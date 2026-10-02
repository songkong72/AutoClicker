package com.sejun.autoclicker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * 한 팀의 시간대 막대: [대기 | 집결 | 행군]을 전체 도착 시각 기준 비율로 그린다.
 * 모든 팀의 막대 오른쪽 끝이 같은 위치에서 끝나는 것이 "동시 도착"이다.
 */
class RallyTimelineBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var total = 0.0
    private var click = 0.0
    private var depart = 0.0
    private var now: Double? = null
    private var dim = false

    fun set(totalSec: Double, clickAtSec: Double, departAtSec: Double, nowSec: Double?, excluded: Boolean) {
        total = totalSec; click = clickAtSec; depart = departAtSec; now = nowSec; dim = excluded
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (total <= 0.0) return
        val w = width.toFloat()
        val h = height.toFloat()
        val r = h / 2f
        fun x(sec: Double) = (sec / total * w).toFloat().coerceIn(0f, w)

        paint.alpha = if (dim) 80 else 255
        paint.color = 0xFF334155.toInt() // 대기
        rect.set(0f, 0f, w, h); canvas.drawRoundRect(rect, r, r, paint)
        paint.color = 0xFF3B82F6.toInt() // 집결
        rect.set(x(click), 0f, x(depart), h); canvas.drawRect(rect, paint)
        paint.color = 0xFFA78BFA.toInt() // 행군
        rect.set(x(depart), 0f, w, h); canvas.drawRoundRect(rect, r, r, paint)
        rect.set(x(depart), 0f, w - r, h); canvas.drawRect(rect, paint)

        now?.let {
            paint.alpha = 255
            paint.color = 0xFFFFFFFF.toInt()
            canvas.drawRect(x(it) - 1.5f, -2f, x(it) + 1.5f, h + 2f, paint)
        }
    }
}
