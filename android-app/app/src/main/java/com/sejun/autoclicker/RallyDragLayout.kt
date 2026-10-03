package com.sejun.autoclicker

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.LinearLayout
import kotlin.math.hypot

/**
 * 집결 패널의 바깥 틀. 버튼이나 글자 위에서 눌러도, 손가락이 어느 정도 움직이면 드래그로 보고 패널을 옮긴다.
 * (그냥 짧게 누르면 안쪽 버튼의 클릭이 그대로 동작한다.)
 */
class RallyDragLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    /** 터치가 시작될 때(패널의 현재 위치를 기억하라는 신호). */
    var onDragStart: (() -> Unit)? = null
    /** 시작점에서 손가락이 이동한 거리(px). */
    var onDragMove: ((dx: Float, dy: Float) -> Unit)? = null

    private val slop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var downX = 0f
    private var downY = 0f
    private var dragging = false

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> begin(e)
            MotionEvent.ACTION_MOVE -> if (!dragging && hypot(e.rawX - downX, e.rawY - downY) > slop) dragging = true
        }
        return dragging
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            // 안쪽에 눌릴 만한 것이 없는 곳을 눌렀을 때는 여기로 바로 들어온다.
            MotionEvent.ACTION_DOWN -> begin(e)
            MotionEvent.ACTION_MOVE -> {
                if (!dragging && hypot(e.rawX - downX, e.rawY - downY) > slop) dragging = true
                if (dragging) onDragMove?.invoke(e.rawX - downX, e.rawY - downY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragging = false
        }
        return true
    }

    private fun begin(e: MotionEvent) {
        downX = e.rawX; downY = e.rawY; dragging = false
        onDragStart?.invoke()
    }
}
