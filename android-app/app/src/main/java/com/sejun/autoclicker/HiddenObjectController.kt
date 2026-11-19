package com.sejun.autoclicker

import android.accessibilityservice.AccessibilityService
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
    private fun getModel(): String = prefs().getString(KEY_MODEL, HiddenObjectFinder.MODEL_LITE) ?: HiddenObjectFinder.MODEL_LITE

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
        if (hasApiKey()) HiddenObjectFinder.prewarm()
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

    private val resetLabelRunnable = Runnable {
        if (!isAnalyzing && runBtn != null) {
            runFindLabel?.text = "🔍 ${T.x.hiddenObjFindBtn}"
            runBtn?.background = round("#D97706", 24, "#FCD34D", strokeDp = 1.2f)
        }
    }

    private fun setFindingState(finding: Boolean) {
        handler.removeCallbacks(resetLabelRunnable)
        isAnalyzing = finding
        runFindLabel?.text = if (finding) "⏳ ${T.x.hiddenObjFinding}" else "🔍 ${T.x.hiddenObjFindBtn}"
        runBtn?.background = round(if (finding) "#475569" else "#D97706", 24, if (finding) "#94A3B8" else "#FCD34D", strokeDp = 1.2f)
    }

    private fun showRunStatus(labelText: String, bgColor: String, strokeColor: String) {
        handler.removeCallbacks(resetLabelRunnable)
        runFindLabel?.text = labelText
        runBtn?.background = round(bgColor, 24, strokeColor, strokeDp = 1.2f)
        handler.postDelayed(resetLabelRunnable, 2200L)
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
        val startedAt = System.currentTimeMillis()
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
                                            showRunStatus("⚠️ 0", "#B45309", "#FCD34D")
                                            toast(T.x.hiddenObjNoneFound)
                                        } else {
                                            showMarkers(list)
                                            vibrate(30)
                                            val sec = String.format("%.1f", (System.currentTimeMillis() - startedAt) / 1000.0)
                                            showRunStatus("✅ ${list.size} (${sec}s)", "#047857", "#34D399")
                                            toast("${T.x.hiddenObjFoundN(list.size)} (${sec}s)")
                                        }
                                    }.onFailure { err ->
                                        val msg = err.message ?: "error"
                                        showRunStatus("❌ ${msg.take(18)}", "#B91C1C", "#F87171")
                                        toast(T.x.hiddenObjFailed(msg))
                                    }
                                }
                            }.start()
                        }

                        override fun onFailure(errorCode: Int) {
                            runBtn?.visibility = View.VISIBLE
                            setFindingState(false)
                            showRunStatus("❌ cap $errorCode", "#B91C1C", "#F87171")
                            toast(T.x.hiddenObjFailed("code $errorCode"))
                        }
                    }
                )
            }
        }, 16L)
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
        val intent = Intent(service, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_OPEN_HIDDEN_OBJ, true)
        }
        try {
            service.startActivity(intent)
        } catch (_: Exception) { }
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
