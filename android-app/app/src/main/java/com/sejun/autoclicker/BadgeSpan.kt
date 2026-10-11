package com.sejun.autoclicker

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.style.ReplacementSpan

/**
 * 글자 안에 그리는 작은 둥근 딱지(예: 군단 줄의 "나").
 * 따로 놓은 뷰는 줄이 붐비면 이름에 밀려 찌그러지거나 사라졌다. 글자의 일부로 그리면 이름과 함께 줄바꿈되어 늘 보인다.
 */
internal class BadgeSpan(private val bg: Int, private val fg: Int, private val density: Float, private val scale: Float = 0.72f) : ReplacementSpan() {
    private val padH get() = 7f * density
    private val gap get() = 6f * density

    private fun textPaint(src: Paint) = Paint(src).apply {
        textSize = src.textSize * scale
        typeface = Typeface.DEFAULT_BOLD
        color = fg
    }

    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int =
        Math.round(gap + padH * 2 + textPaint(paint).measureText(text, start, end))

    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        val tp = textPaint(paint)
        val w = tp.measureText(text, start, end) + padH * 2
        val h = tp.textSize + 6f * density
        // 글자 줄의 가운데 높이에 맞춘다
        val fm = paint.fontMetrics
        val cy = y + (fm.ascent + fm.descent) / 2f
        val rect = RectF(x + gap, cy - h / 2f, x + gap + w, cy + h / 2f)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg }
        canvas.drawRoundRect(rect, h / 2f, h / 2f, fill)
        val tfm = tp.fontMetrics
        canvas.drawText(text, start, end, rect.left + padH, cy - (tfm.ascent + tfm.descent) / 2f, tp)
    }
}
