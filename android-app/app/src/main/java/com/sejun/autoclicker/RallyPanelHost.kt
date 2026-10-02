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

    /** 방 번호가 없을 때의 연습용 방. 실제 방과 같은 편집 규칙(RallyRoomEdit)을 쓰고, 이 기기 안에서만 동작한다. */
    class LocalDemoSource(
        teams: List<RallyTeamState>,
        private var myTeamId: String,
        prepSec: Double,
        waitSec: Double,
        override val isAdmin: Boolean = true
    ) : StateSource {
        private var doc = RallyRoomDoc(
            teams.map { RallyTeamDoc(it.id, it.name, it.leaderName, it.marchSec, it.excluded) },
            prepSec, waitSec, "IDLE", 0L
        )
        private var startedAt: Long? = null

        override fun current(): RallyRoomState {
            val s = startedAt
            val elapsed = if (s == null) 0.0 else (SystemClock.elapsedRealtime() - s) / 1000.0
            val run = when (doc.run) {
                "RUNNING" -> RallyRunState.RUNNING
                "CANCELLED" -> RallyRunState.CANCELLED
                else -> RallyRunState.IDLE
            }
            val ts = doc.teams.map { RallyTeamState(it.id, it.name, it.leaderName, it.marchSec, true, it.excluded) }
            return RallyRoomState(ts, myTeamId, doc.prepSec, doc.waitSec, run, elapsed)
        }

        private fun change(op: (RallyRoomDoc) -> RallyRoomDoc) {
            val next = op(doc)
            if (next === doc) return
            if (next.startSeq != doc.startSeq) startedAt = SystemClock.elapsedRealtime()
            if (next.run != "RUNNING") startedAt = if (next.run == "CANCELLED") null else startedAt
            doc = next
        }

        override fun onStart() { if (isAdmin) change(RallyRoomEdit::startOrRegroup) }
        override fun onStop() { if (isAdmin) change(RallyRoomEdit::cancel) }

        override fun onMarchDelta(teamId: String, deltaSec: Double) {
            if (!RallyRoomEdit.canEditMarch(doc, isAdmin, myTeamId, teamId)) return
            val cur = doc.teams.firstOrNull { it.id == teamId } ?: return
            change { RallyRoomEdit.setMarch(it, teamId, cur.marchSec + deltaSec) }
        }

        override fun onToggleExclude(teamId: String) {
            if (!isAdmin) return
            val cur = doc.teams.firstOrNull { it.id == teamId } ?: return
            change { RallyRoomEdit.setExcluded(it, teamId, !cur.excluded) }
        }

        override fun onSelectMine(teamId: String) { myTeamId = teamId }
    }
}
