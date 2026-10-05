package com.sejun.autoclicker

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ScrollView

/** 내용이 [maxHeightPx]보다 길면 그 높이까지만 보이고 스크롤되는 ScrollView. 0이면 제한이 없다. */
class MaxHeightScrollView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : ScrollView(c, a) {
    var maxHeightPx: Int = 0
        set(v) { if (field != v) { field = v; requestLayout() } }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val limited = if (maxHeightPx > 0) View.MeasureSpec.makeMeasureSpec(maxHeightPx, View.MeasureSpec.AT_MOST) else heightSpec
        super.onMeasure(widthSpec, limited)
    }
}
