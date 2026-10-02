package com.sejun.autoclicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

/**
 * 집결 패널을 오버레이 창으로 띄우고(드래그), 1초마다 RallyScreenModel로 다시 그린다.
 *
 * 주의: 방 상태(팀/행군시간)는 아직 Firebase와 연결 전이라 [stateSource]가 주는 값을 쓴다.
 * 2c단계에서 stateSource를 실제 방 데이터로 교체한다.
 */
class RallyPanelHost(
    private val context: Context,
    private val wm: WindowManager,
    private val stateSource: StateSource,
    private val onSecretUnlock: () -> Unit = {}
) {
    private val secretTap = RallySecretTap()
    interface StateSource {
        /** elapsedSec 등 현재 시각이 반영된 방 상태 */
        fun current(): RallyRoomState
        val isAdmin: Boolean
        fun onStart()
        fun onStop()
        fun onMarchDelta(teamId: String, deltaSec: Double) {}
        fun onToggleExclude(teamId: String) {}
        fun onSelectMine(teamId: String) {}
        fun onSetMarch(teamId: String, sec: Double) {}
        fun onAddTeam() {}
        fun onSetPrep(sec: Double) {}
        fun onSetWait(sec: Double) {}
        /** 관리자가 한 군단에 더해 주는 클릭 보정(ms) */
        fun onSetAdminAdjust(teamId: String, ms: Int) {}
        fun onRemoveTeam(teamId: String) {}
        /** 내 기기 설정(좌표, ms 보정). 방 데이터가 아니라 이 기기에만 저장된다. */
        fun deviceCorrectionMs(): Int = 0
        fun onCorrectionDelta(deltaMs: Int) {}
        fun onSetCorrectionMs(ms: Int) {}
        fun devicePositionText(): String = ""
        /** "도착 예정 12:34:12" / "12:34:12 도착". 진행한 집결이 없으면 빈 문자열. */
        fun arrivalNote(): String = ""
        fun onSavePosition() {}
        /** "✓ 클릭함 12:34:56.789". 이번 집결에서 클릭하지 않았으면 빈 문자열. */
        fun clickNote(): String = ""
        fun connection(): RallyConnection = RallyConnection.LIVE
    }

    private val handler = Handler(Looper.getMainLooper())
    private var panel: RallyPanelView? = null
    private var params: WindowManager.LayoutParams? = null
    private var minimized = false
    private val input = RallyInputPopup(context, wm)

    val isShowing: Boolean get() = panel != null

    private val tick = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 250L) // 표시는 초 단위, 경계에서 밀리지 않게 짧게 갱신
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    /** 설정의 오버레이 투명도를 패널에 반영한다. */
    fun applyAlpha(a: Float) { panel?.root?.alpha = a; input.applyAlpha(a) }

    fun show() {
        if (panel != null) return
        val view = RallyPanelView(context, object : RallyPanelView.Callbacks {
            override fun onStart() { stateSource.onStart(); refresh() }
            override fun onStop() { stateSource.onStop(); refresh() }
            override fun onMinimize() { toggleMinimize() }
            override fun onClose() { hide() }
            override fun onTitleTap() { if (secretTap.tap(System.currentTimeMillis())) onSecretUnlock() } // 제목 5번 연타: 숨은 기능
            override fun onMarchDelta(teamId: String, deltaSec: Double) { stateSource.onMarchDelta(teamId, deltaSec); refresh() }
            override fun onToggleExclude(teamId: String) { stateSource.onToggleExclude(teamId); refresh() }
            override fun onSelectMine(teamId: String) { stateSource.onSelectMine(teamId); refresh() }
            override fun onEditMarch(teamId: String, currentSec: Double) {
                val shown = if (currentSec % 1.0 == 0.0) currentSec.toInt().toString() else currentSec.toString()
                input.show("행군시간(초)", shown) { text ->
                    val sec = RallyInputParse.marchSeconds(text) ?: return@show false
                    stateSource.onSetMarch(teamId, sec); refresh(); true
                }
            }
            override fun onAddTeam() { stateSource.onAddTeam(); refresh() }
            override fun onEditPrep(currentSec: Double) {
                input.show("준비 시간(초)", currentSec.toInt().toString()) { text ->
                    val sec = RallyInputParse.marchSeconds(text) ?: return@show false
                    stateSource.onSetPrep(sec); refresh(); true
                }
            }
            override fun onSetWait(sec: Double) { stateSource.onSetWait(sec); refresh() }
            override fun onEditAdminAdjust(teamId: String, teamName: String, currentMs: Int) {
                val shown = if (currentMs % 1000 == 0) (currentMs / 1000).toString() else (currentMs / 1000.0).toString()
                input.show("$teamName 보정(초) · −는 더 일찍, +는 더 늦게", shown, signed = true,
                    errorText = "−5 ~ +5 사이 숫자를 입력해 주세요 (예: -1.5)") { text ->
                    val ms = RallyInputParse.correctionMs(text) ?: return@show false
                    stateSource.onSetAdminAdjust(teamId, ms); refresh(); true
                }
            }
            override fun onRemoveTeam(teamId: String) { stateSource.onRemoveTeam(teamId); refresh() }
            override fun onCorrectionDelta(deltaMs: Int) { stateSource.onCorrectionDelta(deltaMs); refresh() }
            override fun onSavePosition() { stateSource.onSavePosition(); refresh() }
            override fun onEditCorrection(currentMs: Int) {
                val shown = if (currentMs % 1000 == 0) (currentMs / 1000).toString() else (currentMs / 1000.0).toString()
                input.show("클릭 보정(초) · −는 더 일찍, +는 더 늦게", shown, signed = true,
                    errorText = "−5 ~ +5 사이 숫자를 입력해 주세요 (예: -1.5)") { text ->
                    val ms = RallyInputParse.correctionMs(text) ?: return@show false
                    stateSource.onSetCorrectionMs(ms); refresh(); true
                }
            }
        })
        val lp = WindowManager.LayoutParams(
            panelWidthPx(false),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 120 }
        view.root.setOnTouchListener(dragListener(lp))
        wm.addView(view.root, lp)
        panel = view
        view.root.alpha = PreferencesHelper.getOverlayAlpha(context)
        params = lp
        refresh()
        handler.post(tick)
    }

    fun hide() {
        input.dismiss()
        handler.removeCallbacks(tick)
        panel?.root?.let { v ->
            try { wm.removeView(v) } catch (_: Exception) { }
        }
        panel = null
        params = null
        minimized = false
        lastRun = null
    }

    fun toggle() { if (isShowing) hide() else show() }

    /** 창 폭은 WindowManager가 정한다(레이아웃의 layout_width는 무시됨). 화면이 좁으면 비율로 줄인다. */
    private fun panelWidthPx(compact: Boolean): Int {
        val dm = context.resources.displayMetrics
        val dp = dm.density
        val want = if (compact) 204 * dp else 276 * dp
        return Math.min(want, dm.widthPixels * (if (compact) 0.62f else 0.72f)).toInt()
    }

    private fun applyWidth() {
        val v = panel?.root ?: return
        val lp = params ?: return
        lp.width = if (minimized) WindowManager.LayoutParams.WRAP_CONTENT else panelWidthPx(false) // 알약은 글자 크기에 맞춰 늘어나 잘리지 않는다
        try { wm.updateViewLayout(v, lp) } catch (_: Exception) { }
    }

    private var lastRun: RallyRunState? = null
    private val cue = RallyCountdownCue()

    private fun refresh() {
        val p = panel ?: return
        val state = stateSource.current()
        // 집결이 시작되면 카운트다운만 보이는 알약으로 자동 접고, 끝나거나 취소되면 다시 펼친다.
        if (state.runState != lastRun) {
            val prev = lastRun
            lastRun = state.runState
            if (state.runState == RallyRunState.RUNNING && !minimized) { minimized = true; applyWidth() }
            else if (prev == RallyRunState.RUNNING && state.runState != RallyRunState.RUNNING && minimized) {
                minimized = false; p.setMinimized(false, RallyScreenModel.build(state).hero); applyWidth()
            }
        }
        val model = RallyScreenModel.build(state)
        p.renderDevice(stateSource.deviceCorrectionMs(), stateSource.devicePositionText())
        // 내 클릭을 기다리는 단계에서만: 마지막 5초는 숫자를 붉게, 1초마다 진동(0초 직전은 더 강하게)
        val waiting = model.hero.kind == HeroKind.WAIT_CLICK || model.hero.kind == HeroKind.MOVE
        val remain = model.hero.remainingSec
        cue.onCountdown(waiting, remain ?: 99.0)?.let { n ->
            p.root.performHapticFeedback(if (n == 1) android.view.HapticFeedbackConstants.LONG_PRESS else android.view.HapticFeedbackConstants.CLOCK_TICK)
        }
        val note = listOf(stateSource.arrivalNote(), stateSource.clickNote()).filter { it.isNotEmpty() }.joinToString("\n")
        p.render(model, stateSource.isAdmin, state.runState == RallyRunState.RUNNING, note,
            stateSource.connection(), RallyCountdownCue.urgent(waiting, remain))
        if (minimized) p.setMinimized(true, model.hero)
    }

    private fun toggleMinimize() {
        minimized = !minimized
        panel?.setMinimized(minimized, RallyScreenModel.build(stateSource.current()).hero)
        applyWidth()
        refresh()
    }

    private fun dragListener(lp: WindowManager.LayoutParams) = object : View.OnTouchListener {
        private var sx = 0; private var sy = 0; private var dx = 0f; private var dy = 0f
        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = lp.x; sy = lp.y; dx = e.rawX; dy = e.rawY }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = sx + (e.rawX - dx).toInt()
                    lp.y = sy + (e.rawY - dy).toInt()
                    try { wm.updateViewLayout(v, lp) } catch (_: Exception) { }
                }
            }
            return false // 자식 버튼의 클릭은 그대로 전달
        }
    }
}
