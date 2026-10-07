package com.sejun.autoclicker

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.LinearLayout
import kotlin.math.abs
import kotlin.math.hypot

internal object RallyDragRule {
    /** 스크롤되는 목록 위에서 시작한 세로 움직임은 목록이 가져가고, 그 밖의 움직임만 패널 이동으로 본다. */
    fun isPanelDrag(moved: Float, dx: Float, dy: Float, slop: Float, startedInScrollable: Boolean): Boolean =
        moved > slop && !(startedInScrollable && abs(dy) >= abs(dx))
}

/**
 * 집결 패널의 바깥 틀. 버튼이나 글자 위에서 눌러도, 손가락이 어느 정도 움직이면 드래그로 보고 패널을 옮긴다.
 * (그냥 짧게 누르면 안쪽 버튼의 클릭이 그대로 동작한다.)
 */
class RallyDragLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    /** 터치가 시작될 때(패널의 현재 위치를 기억하라는 신호). */
    var onDragStart: (() -> Unit)? = null
    /** 시작점에서 손가락이 이동한 거리(px). */
    var onDragMove: ((dx: Float, dy: Float) -> Unit)? = null

    /** 군단 목록. 이 목록이 실제로 스크롤될 수 있을 때, 그 위의 세로 드래그는 패널을 옮기지 않고 목록을 스크롤한다. */
    var scrollArea: View? = null

    private val slop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var downX = 0f
    private var downY = 0f
    private var dragging = false
    private var inScrollable = false

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> begin(e)
            MotionEvent.ACTION_MOVE -> if (!dragging && isDrag(e)) dragging = true
        }
        return dragging
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            // 안쪽에 눌릴 만한 것이 없는 곳을 눌렀을 때는 여기로 바로 들어온다.
            MotionEvent.ACTION_DOWN -> begin(e)
            MotionEvent.ACTION_MOVE -> {
                if (!dragging && isDrag(e)) dragging = true
                if (dragging) onDragMove?.invoke(e.rawX - downX, e.rawY - downY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragging = false
        }
        return true
    }

    private fun isDrag(e: MotionEvent): Boolean {
        val dx = e.rawX - downX; val dy = e.rawY - downY
        return RallyDragRule.isPanelDrag(hypot(dx, dy), dx, dy, slop, inScrollable)
    }

    private fun begin(e: MotionEvent) {
        downX = e.rawX; downY = e.rawY; dragging = false
        inScrollable = scrollArea?.let { v ->
            v.visibility == View.VISIBLE && (v.canScrollVertically(1) || v.canScrollVertically(-1)) &&
                IntArray(2).also { v.getLocationOnScreen(it) }.let { (x, y) ->
                    downX >= x && downX < x + v.width && downY >= y && downY < y + v.height
                }
        } ?: false
        onDragStart?.invoke()
    }
}
