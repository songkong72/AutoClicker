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
    private val stateSource: StateSource
) {
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
        fun onRemoveTeam(teamId: String) {}
        /** 내 기기 설정(좌표, ms 보정). 방 데이터가 아니라 이 기기에만 저장된다. */
        fun deviceCorrectionMs(): Int = 0
        fun onCorrectionDelta(deltaMs: Int) {}
        fun devicePositionText(): String = ""
        fun onSavePosition() {}
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
    fun show() {
        if (panel != null) return
        val view = RallyPanelView(context, object : RallyPanelView.Callbacks {
            override fun onStart() { stateSource.onStart(); refresh() }
            override fun onStop() { stateSource.onStop(); refresh() }
            override fun onMinimize() { toggleMinimize() }
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
            override fun onRemoveTeam(teamId: String) { stateSource.onRemoveTeam(teamId); refresh() }
            override fun onCorrectionDelta(deltaMs: Int) { stateSource.onCorrectionDelta(deltaMs); refresh() }
            override fun onSavePosition() { stateSource.onSavePosition(); refresh() }
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
        val want = if (compact) 168 * dp else 276 * dp
        return Math.min(want, dm.widthPixels * (if (compact) 0.5f else 0.72f)).toInt()
    }

    private fun applyWidth() {
        val v = panel?.root ?: return
        val lp = params ?: return
        lp.width = panelWidthPx(minimized)
        try { wm.updateViewLayout(v, lp) } catch (_: Exception) { }
    }

    private var lastRun: RallyRunState? = null

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
        p.render(model, stateSource.isAdmin, stateSource.current().runState == RallyRunState.RUNNING)
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
