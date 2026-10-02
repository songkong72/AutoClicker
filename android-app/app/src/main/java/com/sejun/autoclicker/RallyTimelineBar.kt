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
    private var scale: RallyTimelineScale? = null
    private var click = 0.0
    private var depart = 0.0
    private var arrive = 0.0
    private var now: Double? = null
    private var dim = false

    fun set(
        scale: RallyTimelineScale, clickAtSec: Double, departAtSec: Double, arriveAtSec: Double,
        nowSec: Double?, excluded: Boolean
    ) {
        this.scale = scale; click = clickAtSec; depart = departAtSec; arrive = arriveAtSec
        now = nowSec; dim = excluded
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val sc = scale ?: return
        val total = sc.total(click, depart, arrive)
        if (total <= 0.0) return
        val w = width.toFloat()
        val h = height.toFloat()
        val r = h / 2f
        fun x(vis: Double) = (vis / total * w).toFloat().coerceIn(0f, w)
        val gx0 = x(click)
        val gx1 = x(click + sc.gatherVisual)

        paint.alpha = if (dim) 80 else 255
        paint.color = 0xFF334155.toInt() // 대기
        rect.set(0f, 0f, w, h); canvas.drawRoundRect(rect, r, r, paint)
        paint.color = 0xFF3B82F6.toInt() // 집결 (압축)
        rect.set(gx0, 0f, gx1, h); canvas.drawRect(rect, paint)
        paint.color = 0xFFA78BFA.toInt() // 행군 (실제 길이)
        rect.set(gx1, 0f, w, h); canvas.drawRoundRect(rect, r, r, paint)
        rect.set(gx1, 0f, w - r, h); canvas.drawRect(rect, paint)

        now?.let {
            val px = x(sc.map(click, depart, arrive, it))
            paint.alpha = 255
            paint.color = 0xFFFFFFFF.toInt()
            canvas.drawRect(px - 1.5f, -2f, px + 1.5f, h + 2f, paint)
        }
    }
}
