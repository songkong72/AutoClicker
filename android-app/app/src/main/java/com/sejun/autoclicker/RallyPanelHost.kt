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
    }

    private val handler = Handler(Looper.getMainLooper())
    private var panel: RallyPanelView? = null
    private var params: WindowManager.LayoutParams? = null
    private var minimized = false

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
        })
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
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
        handler.removeCallbacks(tick)
        panel?.root?.let { v ->
            try { wm.removeView(v) } catch (_: Exception) { }
        }
        panel = null
        params = null
        minimized = false
    }

    fun toggle() { if (isShowing) hide() else show() }

    private fun refresh() {
        val p = panel ?: return
        val model = RallyScreenModel.build(stateSource.current())
        p.render(model, stateSource.isAdmin, stateSource.current().runState == RallyRunState.RUNNING)
        if (minimized) p.setMinimized(true, model.hero)
    }

    private fun toggleMinimize() {
        minimized = !minimized
        panel?.setMinimized(minimized, RallyScreenModel.build(stateSource.current()).hero)
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

    /** 앱 내부 로컬 상태: 시작 버튼을 누른 순간부터 경과 시간을 센다 (Firebase 연결 전 임시). */
    class LocalDemoSource(
        private val teams: List<RallyTeamState>,
        private val myTeamId: String,
        private val prepSec: Double,
        private val waitSec: Double,
        override val isAdmin: Boolean = true
    ) : StateSource {
        private var startedAt: Long? = null
        private var cancelled = false

        override fun current(): RallyRoomState {
            val s = startedAt
            val elapsed = if (s == null) 0.0 else (SystemClock.elapsedRealtime() - s) / 1000.0
            val run = when {
                cancelled -> RallyRunState.CANCELLED
                s == null -> RallyRunState.IDLE
                else -> RallyRunState.RUNNING
            }
            return RallyRoomState(teams, myTeamId, prepSec, waitSec, run, elapsed)
        }

        override fun onStart() { startedAt = SystemClock.elapsedRealtime(); cancelled = false }
        override fun onStop() { startedAt = null; cancelled = true }
    }
}
