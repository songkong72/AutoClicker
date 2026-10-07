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

    /** 패널이 화면에서 차지할 수 있는 최대 높이 비율. 넘치는 만큼은 군단 목록이 줄어들어 그 안에서 스크롤된다. */
    var maxScreenRatio = 0.86f
    /** 목록이 한 번에 보여 줄 높이(px)를 돌려준다(예: 군단 4줄). 0 이하면 화면 한도만 쓴다. */
    var visibleListHeight: (() -> Int)? = null

    /**
     * 패널 높이를 화면 안에 맞춘다. 먼저 제한 없이 재서 "목록을 뺀 나머지" 높이를 알아내고,
     * 화면 한도에서 그만큼을 뺀 높이를 목록의 한도로 준 뒤 다시 잰다. 군단 수나 줄 펼침과 상관없이 아래 버튼이 화면에 남는다.
     */
    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val list = scrollArea as? MaxHeightScrollView
        if (list == null || list.visibility != View.VISIBLE) { super.onMeasure(widthSpec, heightSpec); return }
        list.setMaxHeightDuringMeasure(0)
        super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val others = measuredHeight - list.measuredHeight
        val dm = resources.displayMetrics
        val byScreen = Math.max((dm.heightPixels * maxScreenRatio).toInt() - others, (96 * dm.density).toInt())
        val byRows = visibleListHeight?.invoke() ?: 0
        val limit = if (byRows > 0) Math.min(byScreen, byRows) else byScreen
        if (list.measuredHeight > limit) {
            list.setMaxHeightDuringMeasure(limit)
            super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        }
    }

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
