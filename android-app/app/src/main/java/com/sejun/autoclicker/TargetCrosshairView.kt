package com.sejun.autoclicker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

/**
 * 콤팩트하고 정밀한 38dp 크기의 순수 미니멀 과녁 조준점 뷰.
 * 불필요한 번호 표시 없이, 순수 십자선과 조준점으로 게임 버튼을 가리지 않고 완벽하게 타겟팅합니다.
 */
class TargetCrosshairView(context: Context) : View(context) {

    private val density = resources.displayMetrics.density
    val targetSizePx = (38 * density).toInt()

    private var isDragging = false

    // 1. 외곽 짙은 그림자 (게임 화면에서도 뚜렷하게 도드라짐)
    private val shadowBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#99000000")
        style = Paint.Style.STROKE
        strokeWidth = 3.5f * density
    }

    // 2. 반투명 내부 영역 (게임 버튼 텍스트가 살짝 비쳐서 조준하기 편함)
    private val outerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#332563EB")
        style = Paint.Style.FILL
    }

    // 3. 선명한 메인 원형 테두리 (브랜드 블루 / 드래그 시 청록색)
    private val outerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2563EB")
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * density
    }

    // 4. 정밀 화이트 십자선
    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.6f * density
        strokeCap = Paint.Cap.ROUND
    }

    // 5. 중심 빨간색 타겟 점
    private val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EF4444")
        style = Paint.Style.FILL
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun setDraggingState(dragging: Boolean) {
        if (isDragging != dragging) {
            isDragging = dragging
            if (dragging) {
                outerBorderPaint.color = Color.parseColor("#38BDF8") // 밝은 네온 시안
            } else {
                outerBorderPaint.color = Color.parseColor("#2563EB") // 브랜드 블루
            }
            invalidate()
        }
    }

    fun pulse() {
        animate()
            .scaleX(1.18f)
            .scaleY(1.18f)
            .setDuration(40)
            .withEndAction {
                animate().scaleX(1.0f).scaleY(1.0f).setDuration(40).start()
            }.start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(targetSizePx, targetSizePx)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = (width / 2f) - (3.5f * density)

        // 1. 고대비 섀도우 링
        canvas.drawCircle(cx, cy, radius, shadowBorderPaint)

        // 2. 반투명 배경
        canvas.drawCircle(cx, cy, radius, outerFillPaint)

        // 3. 메인 테두리
        canvas.drawCircle(cx, cy, radius, outerBorderPaint)

        // 4. 정밀 십자선 (상하좌우 4방향)
        val crossGap = radius * 0.32f
        val crossEnd = radius * 0.85f
        // 상
        canvas.drawLine(cx, cy - crossEnd, cx, cy - crossGap, crosshairPaint)
        // 하
        canvas.drawLine(cx, cy + crossGap, cx, cy + crossEnd, crosshairPaint)
        // 좌
        canvas.drawLine(cx - crossEnd, cy, cx - crossGap, cy, crosshairPaint)
        // 우
        canvas.drawLine(cx + crossGap, cy, cx + crossEnd, cy, crosshairPaint)

        // 5. 중심 빨간색 타겟 점
        canvas.drawCircle(cx, cy, 2.5f * density, centerDotPaint)
    }
}
