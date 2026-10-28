package com.sejun.autoclicker

/**
 * 곰 사냥 발사의 두 시간(사용자가 헌터 위치 잡기 창에서 −/+로 정한다, 그 폰에만 저장).
 * - [ready]: 🐻를 누른 뒤 부대를 누르기까지 쉬는 시간
 * - [gap]: 부대를 누르고 출정을 누르기까지
 * 0.05초씩, 0초~1초. 부대 → 출정은 앞 클릭(누름 [TAP_MS])과 겹치면 앞 클릭이 끊기므로 0.05초보다 짧게 두지 않는다.
 */
data class HunterTiming(val readyMs: Long = DEFAULT_READY_MS, val gapMs: Long = DEFAULT_GAP_MS) {
    /** 한 번 발사가 끝나기까지 걸리는 시간(다음 발사가 겹치지 않게 막는 데 쓴다). */
    val busyMs: Long get() = readyMs + gapMs + TAP_MS

    companion object {
        const val STEP_MS = 50L
        const val MAX_MS = 1000L
        const val MIN_GAP_MS = 50L
        const val DEFAULT_READY_MS = 50L
        const val DEFAULT_GAP_MS = 150L
        /** 한 번 누르는 길이 */
        const val TAP_MS = 40L

        fun clampReady(ms: Long): Long = snap(ms).coerceIn(0L, MAX_MS)
        fun clampGap(ms: Long): Long = snap(ms).coerceIn(MIN_GAP_MS, MAX_MS)
        private fun snap(ms: Long): Long = Math.round(ms / STEP_MS.toDouble()) * STEP_MS

        /** 화면에 보일 값: 0.05, 0.15, 1 … (초) */
        fun label(ms: Long): String {
            val s = java.math.BigDecimal(ms).divide(java.math.BigDecimal(1000)).stripTrailingZeros()
            return if (s.signum() == 0) "0" else s.toPlainString()
        }
    }
}
