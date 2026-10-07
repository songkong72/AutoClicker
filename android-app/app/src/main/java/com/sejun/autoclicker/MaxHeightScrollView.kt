package com.sejun.autoclicker

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ScrollView

/** 내용이 [maxHeightPx]보다 길면 그 높이까지만 보이고 스크롤되는 ScrollView. 0이면 제한이 없다. */
class MaxHeightScrollView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : ScrollView(c, a) {
    var maxHeightPx: Int = 0
        set(v) { if (field != v) { field = v; requestLayout() } }

    /** 측정 도중에 부모가 한도를 바꿀 때 쓴다(다시 배치를 요청하지 않는다). */
    fun setMaxHeightDuringMeasure(px: Int) { quiet = px; hasQuiet = true }
    private var quiet = 0
    private var hasQuiet = false

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val max = if (hasQuiet) quiet else maxHeightPx
        val limited = if (max > 0) View.MeasureSpec.makeMeasureSpec(max, View.MeasureSpec.AT_MOST) else heightSpec
        super.onMeasure(widthSpec, limited)
    }
}
