package com.sejun.autoclicker

import android.accessibilityservice.AccessibilityService
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 숨은그림찾기 오버레이 컨트롤러 (숨은 모드 전용).
 * 실행 버튼 [찾기 | ✕] 로 현재 화면을 캡처해 Gemini Vision API로 빠르게 분석하고,
 * 찾은 물건 위치에 터치를 통과하는 동그라미 + 이름표만 띄워 준다.
 */
internal class HiddenObjectController(
    private val service: AutoClickService,
    private val toast: (String) -> Unit,
    private val vibrate: (Long) -> Unit,
    private val onModeChanged: () -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val wm: WindowManager? get() = service.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val dp: Float get() = service.resources.displayMetrics.density
    private fun px(v: Int) = (v * dp).toInt()

    var isActive: Boolean = false
        private set
    private var isAnalyzing: Boolean = false

    private var runBtn: LinearLayout? = null
    private var runFindLabel: TextView? = null
    private var settingsPopup: View? = null
    private val markerViews = ArrayList<View>()

    private var btnX = 0
    private var btnY = 0
    private var btnPosLoaded = false
    private var popupX = 0
    private var popupY = 0

    private fun prefs() = service.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasApiKey(): Boolean = prefs().getString(KEY_API_KEY, "")?.trim().orEmpty().isNotEmpty()
    private fun getApiKey(): String = prefs().getString(KEY_API_KEY, "")?.trim().orEmpty()
    private fun getModel(): String = prefs().getString(KEY_MODEL, HiddenObjectFinder.MODEL_FLASH) ?: HiddenObjectFinder.MODEL_FLASH

    private fun saveSettings(apiKey: String, model: String) {
        prefs().edit()
            .putString(KEY_API_KEY, apiKey.trim())
            .putString(KEY_MODEL, model)
            .apply()
    }

    // ---------- 공통 스타일 ----------

    private fun round(color: String, radiusDp: Int, stroke: String? = null, strokeDp: Float = 1.5f) =
        GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = radiusDp * dp
            if (stroke != null) setStroke(Math.max(1, (strokeDp * dp).toInt()), Color.parseColor(stroke))
        }

    private fun label(text: String, size: Float, color: String, bold: Boolean = false) = TextView(service).apply {
        this.text = text
        textSize = size
        setTextColor(Color.parseColor(color))
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun remove(v: View?) {
        if (v == null) return
        try { wm?.removeView(v) } catch (_: Exception) { }
    }

    // ---------- 모드 켜기 / 끄기 ----------

    fun activate(quiet: Boolean = false) {
        if (isActive) {
            showRunButton()
            return
        }
        isActive = true
        showRunButton()
        onModeChanged()
        if (!quiet) {
            if (hasApiKey()) toast(T.x.hiddenObjOnToast)
            else {
                toast(T.x.hiddenObjNeedKey)
                showSettingsPopup()
            }
        }
    }

    fun deactivate() {
        clearMarkers()
        hideRunButton()
        hideSettingsPopup()
        isAnalyzing = false
        if (isActive) {
            isActive = false
            onModeChanged()
        }
    }

    fun hideAll() {
        deactivate()
    }

    // ---------- 실행 알약 버튼 [ 🔍 찾기 | ✕ ] ----------

    private fun showRunButton() {
        if (runBtn != null) return
        val manager = wm ?: return
        if (!btnPosLoaded) {
            btnPosLoaded = true
            btnX = prefs().getInt(KEY_BTN_X, 0)
            btnY = prefs().getInt(KEY_BTN_Y, px(140))
        }

        val findText = label("🔍 ${T.x.hiddenObjFindBtn}", 14f, "#FFFFFF", bold = true).apply {
            setPadding(px(14), px(10), px(10), px(10))
        }
        runFindLabel = findText

        val clearBtn = label("✕", 14f, "#E2E8F0", bold = true).apply {
            contentDescription = T.x.hiddenObjClearDesc
            gravity = Gravity.CENTER
            setPadding(px(12), px(10), px(14), px(10))
            setOnClickListener {
                vibrate(20)
                clearMarkers()
            }
        }

        val divider = View(service).apply { setBackgroundColor(Color.parseColor("#4DFFFFFF")) }

        val box = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = round("#D97706", 24, "#FCD34D", strokeDp = 1.2f)
            addView(findText)
            addView(divider, LinearLayout.LayoutParams(px(1), px(18)))
            addView(clearBtn)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
            x = btnX
            y = btnY
        }

        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        val slop = px(6)

        val touchHandler = View.OnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = e.rawX
                    downRawY = e.rawY
                    startX = params.x
                    startY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - downRawX).toInt()
                    val dy = (e.rawY - downRawY).toInt()
                    if (!moved && (Math.abs(dx) > slop || Math.abs(dy) > slop)) moved = true
                    if (moved) {
                        val dm = service.resources.displayMetrics
                        val (nx, ny) = OverlayDragBounds.centered(
                            startX + dx, startY + dy,
                            dm.widthPixels, dm.heightPixels,
                            box.width, box.height
                        )
                        params.x = nx
                        params.y = ny
                        btnX = nx
                        btnY = ny
                        try { manager.updateViewLayout(box, params) } catch (_: Exception) { }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        prefs().edit().putInt(KEY_BTN_X, btnX).putInt(KEY_BTN_Y, btnY).apply()
                    } else {
                        vibrate(20)
                        captureAndFind()
                    }
                    true
                }
                else -> false
            }
        }
        findText.setOnTouchListener(touchHandler)
        box.setOnTouchListener(touchHandler)

        runBtn = box
        try { manager.addView(box, params) } catch (_: Exception) { runBtn = null }
    }

    private fun hideRunButton() {
        remove(runBtn)
        runBtn = null
        runFindLabel = null
    }

    private fun setFindingState(finding: Boolean) {
        isAnalyzing = finding
        runFindLabel?.text = if (finding) "⏳ ${T.x.hiddenObjFinding}" else "🔍 ${T.x.hiddenObjFindBtn}"
        runBtn?.background = round(if (finding) "#475569" else "#D97706", 24, if (finding) "#94A3B8" else "#FCD34D", strokeDp = 1.2f)
    }

    // ---------- 화면 캡처 + 빠른 분석 + 마커 표시 ----------

    private fun captureAndFind() {
        if (isAnalyzing) return
        if (!hasApiKey()) {
            toast(T.x.hiddenObjNeedKey)
            showSettingsPopup()
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            toast(T.x.hiddenObjNeedApi30)
            return
        }

        clearMarkers()
        setFindingState(true)
        // 캡처 이미지에 실행 버튼이 겹치지 않게 순간적으로만 숨긴다.
        runBtn?.visibility = View.INVISIBLE

        handler.postDelayed({
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                service.takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    service.mainExecutor,
                    object : AccessibilityService.TakeScreenshotCallback {
                        override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                            runBtn?.visibility = View.VISIBLE
                            val hwBuffer = screenshot.hardwareBuffer
                            val colorSpace = screenshot.colorSpace
                            var swBitmap: Bitmap? = null
                            try {
                                val hwBitmap = Bitmap.wrapHardwareBuffer(hwBuffer, colorSpace)
                                if (hwBitmap != null) {
                                    swBitmap = hwBitmap.copy(Bitmap.Config.ARGB_8888, false)
                                    hwBitmap.recycle()
                                }
                            } finally {
                                hwBuffer.close()
                            }

                            val bmp = swBitmap
                            if (bmp == null) {
                                setFindingState(false)
                                toast(T.x.hiddenObjFailed("capture"))
                                return
                            }

                            val apiKey = getApiKey()
                            val model = getModel()
                            Thread {
                                val res = HiddenObjectFinder.analyzeBitmap(bmp, apiKey, model)
                                bmp.recycle()
                                handler.post {
                                    if (!isActive) return@post
                                    setFindingState(false)
                                    res.onSuccess { list ->
                                        if (list.isEmpty()) {
                                            toast(T.x.hiddenObjNoneFound)
                                        } else {
                                            showMarkers(list)
                                            vibrate(30)
                                            toast(T.x.hiddenObjFoundN(list.size))
                                        }
                                    }.onFailure { err ->
                                        toast(T.x.hiddenObjFailed(err.message ?: "error"))
                                    }
                                }
                            }.start()
                        }

                        override fun onFailure(errorCode: Int) {
                            runBtn?.visibility = View.VISIBLE
                            setFindingState(false)
                            toast(T.x.hiddenObjFailed("code $errorCode"))
                        }
                    }
                )
            }
        }, 40L)
    }

    private fun screenBoundsPx(): Pair<Int, Int> {
        val manager = wm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && manager != null) {
            val b = manager.currentWindowMetrics.bounds
            return b.width() to b.height()
        }
        val dm = service.resources.displayMetrics
        return dm.widthPixels to dm.heightPixels
    }

    private fun showMarkers(items: List<FoundObject>) {
        clearMarkers()
        val manager = wm ?: return
        val (screenW, screenH) = screenBoundsPx()
        val circlePx = px(44)
        val palette = listOf("#22C55E", "#06B6D4", "#F59E0B")

        items.forEachIndexed { idx, obj ->
            val accent = palette[idx % palette.size]
            val cx = HiddenObjectFinder.normToScreenPx(obj.normX, screenW)
            val cy = HiddenObjectFinder.normToScreenPx(obj.normY, screenH)

            val badge = label(obj.label, 11f, "#FFFFFF", bold = true).apply {
                setPadding(px(7), px(2), px(7), px(2))
                background = round("#CC0F172A", 8, accent, strokeDp = 1f)
            }
            val circle = View(service).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#33" + accent.removePrefix("#")))
                    setStroke(px(3), Color.parseColor(accent))
                }
            }
            val container = LinearLayout(service).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                addView(circle, LinearLayout.LayoutParams(circlePx, circlePx))
                addView(badge, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = px(2) })
            }

            container.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            )
            val cw = container.measuredWidth
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                // 터치를 가로채지 않아 표시된 곳을 바로 손으로 누를 수 있다.
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (cx - cw / 2).coerceIn(0, (screenW - cw).coerceAtLeast(0))
                y = (cy - circlePx / 2).coerceIn(0, (screenH - circlePx).coerceAtLeast(0))
            }

            try {
                manager.addView(container, params)
                markerViews.add(container)
            } catch (_: Exception) { }
        }
    }

    fun clearMarkers() {
        markerViews.forEach { remove(it) }
        markerViews.clear()
    }

    // ---------- 설정 창 (Gemini API 키 + 모델 선택) ----------

    fun showSettingsPopup() {
        hideSettingsPopup()
        val manager = wm ?: return
        var selectedModel = getModel()

        val title = label(T.x.hiddenObjKeyTitle, 16f, "#F8FAFC", bold = true).apply {
            setPadding(px(6), px(4), px(6), px(8))
        }

        val keyInput = EditText(service).apply {
            setText(getApiKey())
            hint = T.x.hiddenObjKeyHint
            setHintTextColor(Color.parseColor("#64748B"))
            setTextColor(Color.parseColor("#F8FAFC"))
            textSize = 13f
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
            setPadding(px(12), px(10), px(12), px(10))
            background = round("#0B1220", 10, "#334155", strokeDp = 1f)
        }
        val pasteBtn = label(T.x.hiddenObjPaste, 12f, "#E2E8F0", bold = true).apply {
            gravity = Gravity.CENTER
            setPadding(px(12), px(10), px(12), px(10))
            background = round("#1E293B", 10, "#475569", strokeDp = 1f)
            setOnClickListener {
                vibrate(15)
                val cm = service.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val text = cm?.primaryClip?.getItemAt(0)?.coerceToText(service)?.toString()?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    keyInput.setText(text)
                    keyInput.setSelection(text.length)
                }
            }
        }
        val keyRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(keyInput, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = px(6) })
            addView(pasteBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }

        val modelHeader = label(T.x.hiddenObjModelLabel, 12f, "#94A3B8", bold = true).apply {
            setPadding(px(4), px(10), px(4), px(6))
        }

        val flashBtn = label(T.x.hiddenObjModelFlash, 12f, "#F8FAFC", bold = true).apply {
            gravity = Gravity.CENTER
            setPadding(px(8), px(10), px(8), px(10))
        }
        val liteBtn = label(T.x.hiddenObjModelLite, 12f, "#F8FAFC", bold = true).apply {
            gravity = Gravity.CENTER
            setPadding(px(8), px(10), px(8), px(10))
        }

        fun refreshModelChips() {
            val isLite = selectedModel == HiddenObjectFinder.MODEL_LITE
            flashBtn.background = round(if (!isLite) "#1D4ED8" else "#0B1220", 10, if (!isLite) "#60A5FA" else "#334155")
            liteBtn.background = round(if (isLite) "#1D4ED8" else "#0B1220", 10, if (isLite) "#60A5FA" else "#334155")
        }
        refreshModelChips()

        flashBtn.setOnClickListener { vibrate(15); selectedModel = HiddenObjectFinder.MODEL_FLASH; refreshModelChips() }
        liteBtn.setOnClickListener { vibrate(15); selectedModel = HiddenObjectFinder.MODEL_LITE; refreshModelChips() }

        val modelRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(flashBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = px(6) })
            addView(liteBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        val cancelBtn = label(T.s.cancel, 14f, "#CBD5E1", bold = true).apply {
            gravity = Gravity.CENTER
            background = round("#2A3447", 12)
            setOnClickListener { hideSettingsPopup() }
        }
        val saveBtn = label(T.a.save, 14f, "#FFFFFF", bold = true).apply {
            gravity = Gravity.CENTER
            background = round("#D97706", 12)
            setOnClickListener {
                vibrate(20)
                saveSettings(keyInput.text.toString(), selectedModel)
                hideSettingsPopup()
                toast(T.x.hiddenObjKeySaved)
            }
        }
        val btnRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(cancelBtn, LinearLayout.LayoutParams(0, px(44), 1f).apply { rightMargin = px(8) })
            addView(saveBtn, LinearLayout.LayoutParams(0, px(44), 1f))
        }

        val root = RallyDragLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            background = round("#121A2C", 16, "#475569")
            setPadding(px(14), px(10), px(14), px(14))
            addView(View(service).apply { background = round("#475569", 2) },
                LinearLayout.LayoutParams(px(40), px(4)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = px(6) })
            addView(title)
            addView(keyRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(modelHeader)
            addView(modelRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(btnRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = px(14) })
            setOnTouchListener { _, e ->
                if (e.action == MotionEvent.ACTION_OUTSIDE) { hideSettingsPopup(); true } else false
            }
        }

        val params = WindowManager.LayoutParams(
            px(300),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
            x = popupX
            y = popupY
        }

        var sx = 0
        var sy = 0
        root.onDragStart = { sx = params.x; sy = params.y }
        root.onDragMove = { dx, dy ->
            val dm = service.resources.displayMetrics
            val (nx, ny) = OverlayDragBounds.centered(sx + dx.toInt(), sy + dy.toInt(), dm.widthPixels, dm.heightPixels, root.width, root.height)
            params.x = nx
            params.y = ny
            popupX = nx
            popupY = ny
            try { manager.updateViewLayout(root, params) } catch (_: Exception) { }
        }

        settingsPopup = root
        try {
            manager.addView(root, params)
            keyInput.requestFocus()
            handler.postDelayed({
                val imm = service.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(keyInput, InputMethodManager.SHOW_IMPLICIT)
            }, 120L)
        } catch (_: Exception) {
            settingsPopup = null
        }
    }

    private fun hideSettingsPopup() {
        remove(settingsPopup)
        settingsPopup = null
    }

    companion object {
        private const val PREFS = "AutoClickerPrefs"
        private const val KEY_API_KEY = "hidden_object_api_key"
        private const val KEY_MODEL = "hidden_object_model"
        private const val KEY_BTN_X = "hidden_object_btn_x"
        private const val KEY_BTN_Y = "hidden_object_btn_y"
    }
}
