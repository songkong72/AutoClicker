package com.sejun.autoclicker

import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * 게임 위 막대의 "모드" 버튼이 여는 목록과 "순서 클릭" 화면들.
 * 순서 클릭: 번호 동그라미를 누를 자리에 놓아 이름 붙여 저장해 두고, 고른 순서를 실행 버튼으로 1번부터 끝까지 누른다.
 * 헌터 모드 자체는 [AutoClickService]에 있고, 여기서는 목록에서 켜고 끄기만 한다. 헌터와 순서 클릭은 한 번에 하나만 켠다.
 * 시안: 디자인 캔버스 "단순화 시안" 6·7·8번.
 */
internal class SequenceClickController(
    private val service: AutoClickService,
    private val toast: (String) -> Unit,
    private val vibrate: (Long) -> Unit,
    /** 실행하는 동안 과녁이 클릭을 가로채지 않게 한다. */
    private val setTargetTouchable: (Boolean) -> Unit,
    private val hunterAllowed: () -> Boolean,
    private val hunterOn: () -> Boolean,
    private val setHunter: (Boolean) -> Unit,
    private val editHunter: () -> Unit,
    /** 켜진 모드가 바뀌었을 때(막대 버튼의 색과 그림을 맞추라는 신호). */
    private val onModeChanged: () -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val wm: WindowManager? get() = service.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val dp: Float get() = service.resources.displayMetrics.density
    private fun px(v: Int) = (v * dp).toInt()

    private var sets: List<ClickSequence> = emptyList()
    private var loaded = false
    /** 켜진 순서(목록에서의 자리). 꺼져 있으면 -1. 저장하지 않고 이번 실행 중에만 기억한다. */
    private var activeIndex = -1

    val isActive: Boolean get() = activeIndex >= 0

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        sets = ClickSequences.decode(service.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SETS, null))
    }

    private fun persist() {
        service.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_SETS, ClickSequences.encode(sets)).apply()
    }

    // ---------- 공통 모양 ----------

    private fun round(color: String, radiusDp: Int, stroke: String? = null) = GradientDrawable().apply {
        setColor(Color.parseColor(color)); cornerRadius = radiusDp * dp
        if (stroke != null) setStroke(Math.max(1, (1.5f * dp).toInt()), Color.parseColor(stroke))
    }

    private fun label(text: String, size: Float, color: String, bold: Boolean = false) = TextView(service).apply {
        this.text = text
        textSize = size
        setTextColor(Color.parseColor(color))
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun lp(w: Int, h: Int, weight: Float = 0f, top: Int = 0) = LinearLayout.LayoutParams(w, h, weight).apply { topMargin = top }

    private fun overlayParams(w: Int, h: Int, flags: Int) = WindowManager.LayoutParams(
        w, h, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, flags, PixelFormat.TRANSLUCENT
    )

    private fun remove(v: View?) {
        if (v == null) return
        try { wm?.removeView(v) } catch (_: Exception) { }
    }

    // ---------- 모드 목록, 고치기 메뉴, 삭제 확인 (한 번에 하나만 뜬다) ----------

    private var popup: View? = null

    private fun hidePopup() { remove(popup); popup = null }

    /** 목록 바깥을 누르면 닫히는 작은 창으로 [content]를 띄운다. */
    private fun showPopup(content: LinearLayout) {
        hidePopup()
        content.orientation = LinearLayout.VERTICAL
        content.background = round(PANEL, 16, LINE)
        content.setPadding(px(8), px(8), px(8), px(8))
        val root = ScrollView(service).apply {
            addView(content)
            setOnTouchListener { _, e -> if (e.action == MotionEvent.ACTION_OUTSIDE) { hidePopup(); true } else false }
        }
        val params = overlayParams(
            px(280), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        ).apply { gravity = Gravity.CENTER }
        try { wm?.addView(root, params); popup = root } catch (e: Exception) { Log.w(TAG, "모드 목록 추가 실패", e) }
    }

    private fun caption(text: String) = label(text, 12f, SUB).apply { setPadding(px(12), px(6), px(12), px(4)) }

    private fun divider() = View(service).apply { setBackgroundColor(Color.parseColor(LINE)) }

    private fun LinearLayout.addDivider() {
        addView(divider(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, px(1))).apply {
            setMargins(px(8), px(4), px(8), px(4))
        })
    }

    /** 목록 한 줄: 색 점, 이름과 설명, 켜짐 표시, (있으면) 연필. */
    private fun row(
        dot: String, title: String, sub: String?, on: Boolean, titleColor: String = INK,
        onPencil: (() -> Unit)? = null, onClick: () -> Unit,
    ): View = LinearLayout(service).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = px(52)
        setPadding(px(12), 0, px(8), 0)
        background = round(if (on) "#1E293B" else "#00000000", 12)
        addView(View(service).apply { background = round(dot, 5) }, LinearLayout.LayoutParams(px(10), px(10)).apply { rightMargin = px(10) })
        addView(LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(title, 15f, titleColor, bold = true))
            if (!sub.isNullOrEmpty()) addView(label(sub, 12f, SUB))
        }, lp(0, WRAP, 1f))
        if (on) addView(label("켜짐", 12f, SKY_TEXT, bold = true), LinearLayout.LayoutParams(WRAP, WRAP).apply { rightMargin = px(8) })
        if (onPencil != null) {
            addView(ImageView(service).apply {
                setImageResource(R.drawable.ic_rp_edit)
                setColorFilter(Color.parseColor("#CBD5E1"))
                contentDescription = "$title 고치기"
                background = round("#2A3447", 18)
                setPadding(px(9), px(9), px(9), px(9))
                setOnClickListener { vibrate(20); onPencil() }
            }, LinearLayout.LayoutParams(px(36), px(36)))
        }
        setOnClickListener { vibrate(20); onClick() }
    }

    /** 막대의 모드 버튼을 눌렀을 때: 목록이 떠 있으면 닫고, 아니면 연다. */
    fun toggleMenu() { if (popup != null) hidePopup() else showMenu() }

    fun showMenu() {
        ensureLoaded()
        val box = LinearLayout(service)
        box.addView(caption("모드 고르기"))
        if (hunterAllowed()) {
            val sub = if (HunterModeManager.hasTargets) "부대 ${HunterModeManager.troops.size}개" else "위치 저장 전"
            box.addView(row(GREEN, "헌터", sub, on = hunterOn(), onPencil = { hidePopup(); editHunter() }) { hidePopup(); selectHunter() })
            box.addDivider()
        }
        sets.forEachIndexed { i, seq ->
            box.addView(row(SKY, seq.name, ClickSequences.summary(seq), on = activeIndex == i, onPencil = { showActions(i) }) {
                hidePopup(); activate(i)
            })
        }
        if (ClickSequences.canAdd(sets)) {
            box.addView(label("+ 새 순서 만들기", 14f, SKY_TEXT, bold = true).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(px(12), 0, px(12), 0)
                background = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT); cornerRadius = 12 * dp
                    setStroke(Math.max(1, (1.5f * dp).toInt()), Color.parseColor("#475569"), 6 * dp, 4 * dp)
                }
                setOnClickListener { vibrate(20); hidePopup(); openSetup(-1) }
            }, lp(MATCH, px(48), top = px(4)))
        } else {
            box.addView(caption("순서는 ${ClickSequences.MAX_SETS}개까지 저장할 수 있어요"))
        }
        box.addDivider()
        box.addView(row("#475569", "끄기", null, on = false, titleColor = SUB) { hidePopup(); turnOff() })
        showPopup(box)
    }

    private fun actionRow(text: String, color: String = INK, onClick: () -> Unit) = label(text, 15f, color, bold = true).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(px(14), 0, px(14), 0)
        background = round("#0B1220", 12)
        setOnClickListener { vibrate(20); onClick() }
    }

    /** 연필을 눌렀을 때: 자리 다시 잡기 / 이름 바꾸기 / 삭제. */
    private fun showActions(index: Int) {
        val seq = sets.getOrNull(index) ?: return
        val box = LinearLayout(service)
        box.addView(label(seq.name, 16f, INK, bold = true).apply { setPadding(px(8), px(6), px(8), 0) })
        box.addView(label(ClickSequences.summary(seq), 12f, SUB).apply { setPadding(px(8), 0, px(8), px(6)) })
        box.addView(actionRow("자리 다시 잡기") { hidePopup(); openSetup(index) }, lp(MATCH, px(48), top = px(6)))
        box.addView(actionRow("이름 바꾸기") { hidePopup(); openSetup(index, focusName = true) }, lp(MATCH, px(48), top = px(6)))
        box.addView(actionRow("삭제", "#F87171") { showDeleteConfirm(index) }, lp(MATCH, px(48), top = px(6)))
        box.addView(label("닫기", 14f, SUB, bold = true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { hidePopup() }
        }, lp(MATCH, px(44), top = px(2)))
        showPopup(box)
    }

    private fun showDeleteConfirm(index: Int) {
        val seq = sets.getOrNull(index) ?: return
        val box = LinearLayout(service)
        box.addView(label("\"${seq.name}\"을(를) 지울까요?", 15f, INK, bold = true).apply { setPadding(px(8), px(6), px(8), 0) })
        box.addView(label("저장한 자리 ${seq.points.size}곳이 함께 지워져요.", 12f, SUB).apply { setPadding(px(8), px(4), px(8), px(6)) })
        box.addView(LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(label("취소", 14f, INK, bold = true).apply {
                gravity = Gravity.CENTER
                background = round("#2A3447", 12)
                setOnClickListener { hidePopup() }
            }, LinearLayout.LayoutParams(0, px(44), 1f).apply { rightMargin = px(8) })
            addView(label("삭제", 14f, "#FFFFFF", bold = true).apply {
                gravity = Gravity.CENTER
                background = round("#DC2626", 12)
                setOnClickListener { vibrate(30); hidePopup(); delete(index) }
            }, LinearLayout.LayoutParams(0, px(44), 1f))
        }, lp(MATCH, WRAP, top = px(6)))
        showPopup(box)
    }

    private fun delete(index: Int) {
        val seq = sets.getOrNull(index) ?: return
        // 켜져 있던 순서를 지우면 끄고, 그 뒤 순서가 켜져 있었으면 번호를 한 칸 당긴다.
        when {
            activeIndex == index -> deactivate()
            activeIndex > index -> activeIndex -= 1
        }
        sets = ClickSequences.removed(sets, index)
        persist()
        toast("\"${seq.name}\" 순서를 지웠어요")
    }

    // ---------- 모드 켜고 끄기 ----------

    private fun selectHunter() {
        closeSetup()
        deactivate()
        if (!hunterOn()) setHunter(true)
    }

    private fun turnOff() {
        closeSetup()
        if (hunterOn()) setHunter(false)
        deactivate()
    }

    private fun activate(index: Int, quiet: Boolean = false) {
        val seq = sets.getOrNull(index) ?: return
        closeSetup()
        if (hunterOn()) setHunter(false)
        stopRun()
        activeIndex = index
        showRunButton()
        updateRunLabel()
        onModeChanged()
        if (!quiet) toast("\"${seq.name}\" 순서 켜짐 · 하늘색 버튼을 누르면 1번부터 눌러요")
    }

    /** 순서 클릭을 끈다(실행 중이면 멈춘다). 헌터는 건드리지 않는다. */
    fun deactivate() {
        stopRun()
        hideRunButton()
        if (activeIndex >= 0) { activeIndex = -1; onModeChanged() }
    }

    /** 모드 버튼을 길게 눌렀을 때: 켜진 순서의 자리를 다시 잡는다. */
    fun editActive() { if (isActive) openSetup(activeIndex) }

    /** 막대를 닫을 때: 떠 있는 것을 모두 치운다. */
    fun hideAll() {
        hidePopup()
        closeSetup()
        deactivate()
    }

    // ---------- 자리 잡기 창과 번호 동그라미 ----------

    private var setupView: View? = null
    private val markers = ArrayList<TextView>()
    private var countLabel: TextView? = null
    private var gapLabel: TextView? = null
    private var setupCount = ClickSequences.DEFAULT_POINTS
    private var setupGap = ClickSequences.DEFAULT_GAP_MS
    /** 고치는 중인 순서의 자리. 새로 만드는 중이면 -1. */
    private var editingIndex = -1

    private fun stepButton(text: String, desc: String, onClick: () -> Unit) = label(text, 20f, INK, bold = true).apply {
        contentDescription = desc
        gravity = Gravity.CENTER
        background = round("#0B1220", 22, "#475569")
        setOnClickListener { vibrate(15); onClick() }
    }

    private fun stepperRow(title: String, minusDesc: String, plusDesc: String, onStep: (Int) -> Unit): Pair<View, TextView> {
        val value = label("", 16f, INK, bold = true).apply { gravity = Gravity.CENTER }
        val row = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(label(title, 14f, INK, bold = true), lp(0, WRAP, 1f))
            addView(stepButton("−", minusDesc) { onStep(-1) }, LinearLayout.LayoutParams(px(44), px(44)))
            addView(value, LinearLayout.LayoutParams(px(64), WRAP))
            addView(stepButton("+", plusDesc) { onStep(1) }, LinearLayout.LayoutParams(px(44), px(44)))
        }
        return row to value
    }

    private fun openSetup(index: Int, focusName: Boolean = false) {
        ensureLoaded()
        closeSetup()
        // 자리를 잡는 동안에는 다른 모드를 끈다(발사·실행 버튼이 동그라미와 섞이지 않게).
        if (hunterOn()) setHunter(false)
        deactivate()
        val manager = wm ?: return
        val existing = sets.getOrNull(index)
        editingIndex = if (existing != null) index else -1
        setupCount = ClickSequences.clampCount(existing?.points?.size ?: ClickSequences.DEFAULT_POINTS)
        setupGap = ClickSequences.clampGap(existing?.gapMs ?: ClickSequences.DEFAULT_GAP_MS)

        val nameInput = EditText(service).apply {
            setText(existing?.name ?: ClickSequences.defaultName(sets))
            setSingleLine()
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            filters = arrayOf(InputFilter.LengthFilter(ClickSequences.MAX_NAME))
            background = round("#0B1220", 12, "#64748B")
            setPadding(px(12), 0, px(12), 0)
        }
        val root = RallyDragLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            background = round(PANEL, 20, LINE)
            setPadding(px(16), px(12), px(16), px(16))
            // 창이 뜨자마자 이름 칸에 커서가 들어가 키보드가 올라오지 않게 한다.
            isFocusable = true
            isFocusableInTouchMode = true

            addView(View(service).apply { background = round("#475569", 2) },
                LinearLayout.LayoutParams(px(40), px(4)).apply { gravity = Gravity.CENTER_HORIZONTAL })
            addView(LinearLayout(service).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(label(if (existing != null) "순서 고치기" else "새 순서 만들기", 16f, INK, bold = true), lp(0, WRAP, 1f))
                addView(label("✕", 13f, "#CBD5E1").apply {
                    contentDescription = "닫기"
                    gravity = Gravity.CENTER
                    background = round("#2A3447", 15)
                    setOnClickListener { closeSetup() }
                }, LinearLayout.LayoutParams(px(30), px(30)))
            }, lp(MATCH, px(32), top = px(6)))

            addView(LinearLayout(service).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(label("이름", 14f, INK, bold = true), LinearLayout.LayoutParams(WRAP, WRAP).apply { rightMargin = px(10) })
                addView(nameInput, LinearLayout.LayoutParams(0, px(44), 1f))
            }, lp(MATCH, px(44), top = px(10)))

            addView(label("동그라미 1, 2, 3…을 누를 자리에 번호 순서대로 놓고 저장", 12f, SUB), lp(MATCH, WRAP, top = px(8)))

            val (countRow, countValue) = stepperRow("자리 수", "자리 줄이기", "자리 늘리기") { delta ->
                setupCount = ClickSequences.clampCount(setupCount + delta)
                syncMarkers(null)
            }
            countLabel = countValue
            addView(countRow, lp(MATCH, px(44), top = px(10)))

            val (gapRow, gapValue) = stepperRow("자리 사이 쉬는 시간", "쉬는 시간 줄이기", "쉬는 시간 늘리기") { delta ->
                setupGap = ClickSequences.clampGap(setupGap + delta * ClickSequences.GAP_STEP_MS)
                gapLabel?.text = ClickSequences.gapLabel(setupGap)
            }
            gapLabel = gapValue
            gapValue.text = ClickSequences.gapLabel(setupGap)
            addView(gapRow, lp(MATCH, px(44), top = px(10)))

            addView(label("저장", 15f, "#FFFFFF", bold = true).apply {
                gravity = Gravity.CENTER
                background = round("#3B82F6", 14)
                setOnClickListener { vibrate(30); saveSetup(nameInput.text?.toString()) }
            }, lp(MATCH, px(48), top = px(12)))
        }

        // 이름을 적어야 하므로 키보드를 받을 수 있는 창으로 띄운다(창 밖 터치는 그대로 게임과 동그라미로 간다).
        val params = overlayParams(px(320), WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL).apply {
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        }
        var sx = 0; var sy = 0
        root.onDragStart = { sx = params.x; sy = params.y }
        root.onDragMove = { dx, dy ->
            val dm = service.resources.displayMetrics
            val (x, y) = OverlayDragBounds.centered(sx + dx.toInt(), sy + dy.toInt(), dm.widthPixels, dm.heightPixels, root.width, root.height)
            params.x = x; params.y = y
            try { manager.updateViewLayout(root, params) } catch (_: Exception) { }
        }
        try { manager.addView(root, params); setupView = root } catch (e: Exception) { Log.w(TAG, "순서 자리 잡기 창 추가 실패", e); return }

        syncMarkers(existing?.points)

        if (focusName) root.post {
            nameInput.requestFocus()
            nameInput.setSelection(nameInput.text?.length ?: 0)
            (service.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.showSoftInput(nameInput, 0)
        }
    }

    /** 번호 동그라미를 [setupCount]개에 맞춘다. 이미 있는 것은 자리를 그대로 두고, 새로 만드는 것은 [saved]의 자리(있으면)에 놓는다. */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun syncMarkers(saved: List<TroopPoint>?) {
        val manager = wm ?: return
        while (markers.size > setupCount) remove(markers.removeAt(markers.size - 1))
        while (markers.size < setupCount) {
            val i = markers.size
            // 게임 버튼을 가리지 않게 작게. 가운데 점이 눌릴 자리다.
            val size = px(30)
            val marker = label("${i + 1}", 13f, "#0B1220", bold = true).apply {
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#D938BDF8"))
                    setStroke(px(2), Color.WHITE)
                }
            }
            val params = overlayParams(size, size, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE).apply {
                gravity = Gravity.TOP or Gravity.START
                x = px(40 + (i / 5) * 44)
                y = px(100 + (i % 5) * 40)
            }
            var sx = 0; var sy = 0; var tx = 0f; var ty = 0f
            marker.setOnTouchListener { _, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { sx = params.x; sy = params.y; tx = e.rawX; ty = e.rawY }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = sx + (e.rawX - tx).toInt(); params.y = sy + (e.rawY - ty).toInt()
                        try { manager.updateViewLayout(marker, params) } catch (_: Exception) { }
                    }
                }
                true
            }
            try { manager.addView(marker, params) } catch (e: Exception) { Log.w(TAG, "번호 동그라미 추가 실패", e) }
            markers.add(marker)
            saved?.getOrNull(i)?.let { placeCenterAt(marker, params, it.x, it.y) }
        }
        countLabel?.text = "${setupCount}개"
    }

    /** 떠 있는 표시의 가운데가 화면 좌표 ([cx], [cy])에 오도록 옮긴다. 아직 그려지지 않았으면 잠깐 뒤에 다시 해 본다. */
    private fun placeCenterAt(view: View, params: WindowManager.LayoutParams, cx: Int, cy: Int, triesLeft: Int = 5) {
        view.post {
            if (view.width == 0 || view.height == 0) {
                if (triesLeft > 0) handler.postDelayed({ placeCenterAt(view, params, cx, cy, triesLeft - 1) }, 50L)
                return@post
            }
            val loc = IntArray(2)
            view.getLocationOnScreen(loc)
            params.x = HunterTroops.topLeftFor(cx, view.width, loc[0] - params.x)
            params.y = HunterTroops.topLeftFor(cy, view.height, loc[1] - params.y)
            try { wm?.updateViewLayout(view, params) } catch (_: Exception) { }
        }
    }

    private fun centerOnScreen(v: View): TroopPoint? {
        if (v.width == 0 || v.height == 0) return null
        val loc = IntArray(2)
        v.getLocationOnScreen(loc)
        return TroopPoint(loc[0] + v.width / 2, loc[1] + v.height / 2)
    }

    private fun saveSetup(typedName: String?) {
        val points = markers.map { centerOnScreen(it) }
        if (points.isEmpty() || points.any { it == null }) {
            toast("⚠ 자리를 읽지 못했어요. 번호 동그라미가 보이는지 확인하세요")
            return
        }
        val old = sets.getOrNull(editingIndex)
        val name = ClickSequences.cleanName(typedName).ifEmpty { old?.name ?: ClickSequences.defaultName(sets) }
        val seq = ClickSequence(name, points.filterNotNull(), setupGap)
        if (old == null && !ClickSequences.canAdd(sets)) {
            toast("순서는 ${ClickSequences.MAX_SETS}개까지 저장할 수 있어요")
            return
        }
        sets = ClickSequences.saved(sets, editingIndex, seq)
        persist()
        val index = if (old != null) editingIndex else sets.size - 1
        closeSetup()
        activate(index, quiet = true)
        toast("✓ \"$name\" 저장 (${seq.points.size}곳) · 하늘색 버튼을 누르면 1번부터 눌러요")
    }

    private fun closeSetup() {
        remove(setupView); setupView = null
        markers.forEach { remove(it) }
        markers.clear()
        countLabel = null; gapLabel = null
        editingIndex = -1
    }

    // ---------- 실행 버튼과 실행 ----------

    private var runView: TextView? = null
    private var ringView: View? = null
    private var ringParams: WindowManager.LayoutParams? = null
    private var running = false
    /** 실행을 멈추면 값을 올려, 예약해 둔 다음 클릭이 스스로 그만두게 한다. */
    private var runToken = 0

    /** 실행 버튼: 눌러서 실행(실행 중에는 멈춤), 끌어서 이동. 마지막으로 둔 자리에서 연다. */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun showRunButton() {
        if (runView != null) return
        val manager = wm ?: return
        val size = px(56)
        val v = label("", 11f, "#0B1220", bold = true).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor(RUN_READY)) }
        }
        val prefs = service.getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE)
        val dm = service.resources.displayMetrics
        val params = overlayParams(size, size, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt("seq_run_x", px(12)).coerceIn(0, Math.max(0, dm.widthPixels - size))
            y = prefs.getInt("seq_run_y", px(290)).coerceIn(0, Math.max(0, dm.heightPixels - size))
        }
        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var moved = false
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = params.x; sy = params.y; tx = e.rawX; ty = e.rawY; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - tx; val dy = e.rawY - ty
                    if (moved || Math.abs(dx) > 12 * dp || Math.abs(dy) > 12 * dp) {
                        moved = true
                        params.x = sx + dx.toInt(); params.y = sy + dy.toInt()
                        try { manager.updateViewLayout(v, params) } catch (_: Exception) { }
                    }
                }
                MotionEvent.ACTION_UP -> if (moved) {
                    prefs.edit().putInt("seq_run_x", params.x).putInt("seq_run_y", params.y).apply()
                } else {
                    vibrate(30)
                    if (running) stopRun() else startRun()
                }
            }
            true
        }
        try { manager.addView(v, params); runView = v } catch (e: Exception) { Log.w(TAG, "순서 실행 버튼 추가 실패", e) }
    }

    private fun hideRunButton() { remove(runView); runView = null }

    /** 실행 버튼의 글자와 색: 평소에는 "순서 N곳"(하늘색), 실행 중에는 "2 / 4 멈춤"(주황). */
    private fun updateRunLabel(runningIndex: Int = -1) {
        val v = runView ?: return
        val count = sets.getOrNull(activeIndex)?.points?.size ?: 0
        v.text = if (runningIndex >= 0) ClickSequences.runningLabel(runningIndex, count) else ClickSequences.readyLabel(count)
        (v.background as? GradientDrawable)?.setColor(Color.parseColor(if (runningIndex >= 0) RUN_BUSY else RUN_READY))
    }

    private fun startRun() {
        val seq = sets.getOrNull(activeIndex) ?: return
        if (seq.points.isEmpty() || running) return
        running = true
        val token = ++runToken
        setTargetTouchable(false)
        showRing()
        // 과녁이 투과 상태로 바뀔 시간을 잠깐 준 뒤 1번부터 누른다.
        handler.postDelayed({ step(seq, 0, token) }, SETTLE_MS)
    }

    private fun step(seq: ClickSequence, index: Int, token: Int) {
        if (token != runToken) return
        if (index >= seq.points.size) { finishRun(); return }
        val p = seq.points[index]
        updateRunLabel(index)
        moveRing(p)
        tap(p.x.toFloat(), p.y.toFloat())
        handler.postDelayed({ step(seq, index + 1, token) }, seq.gapMs)
    }

    private fun finishRun() {
        running = false
        hideRing()
        setTargetTouchable(true)
        updateRunLabel()
    }

    /** 실행 중이면 멈춘다. 이미 나간 클릭은 되돌리지 못한다. */
    private fun stopRun() {
        runToken++
        if (running) finishRun()
    }

    private fun tap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        try {
            service.dispatchGesture(
                GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 50)).build(), null, null
            )
        } catch (e: Exception) { Log.w(TAG, "순서 클릭 실패", e) }
    }

    /** 지금 누르는 자리를 보여 주는 노란 테두리. 터치를 받지 않아 클릭이 그대로 게임에 닿는다. */
    private fun showRing() {
        if (ringView != null) return
        val manager = wm ?: return
        val size = px(40)
        val v = View(service).apply {
            visibility = View.INVISIBLE
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
                setStroke(px(3), Color.parseColor("#FBBF24"))
            }
        }
        val params = overlayParams(
            size, size, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        ).apply { gravity = Gravity.TOP or Gravity.START }
        try { manager.addView(v, params); ringView = v; ringParams = params } catch (e: Exception) { Log.w(TAG, "자리 표시 추가 실패", e) }
    }

    private fun moveRing(p: TroopPoint) {
        val v = ringView ?: return
        val params = ringParams ?: return
        v.visibility = View.VISIBLE
        placeCenterAt(v, params, p.x, p.y)
    }

    private fun hideRing() { remove(ringView); ringView = null; ringParams = null }

    private companion object {
        const val TAG = "SequenceClick"
        const val PREFS = "SequencePrefs"
        const val KEY_SETS = "sets"
        const val SETTLE_MS = 40L
        const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
        const val PANEL = "#121A2C"
        const val LINE = "#334155"
        const val INK = "#F1F5F9"
        const val SUB = "#A9B4C7"
        const val GREEN = "#10B981"
        const val SKY = "#38BDF8"
        const val SKY_TEXT = "#7DD3FC"
        const val RUN_READY = "#EB38BDF8"
        const val RUN_BUSY = "#F2F59E0B"
    }
}
