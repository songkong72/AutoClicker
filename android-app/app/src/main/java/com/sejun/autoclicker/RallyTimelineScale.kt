package com.sejun.autoclicker

/**
 * 시간대 막대용 화면 좌표 변환. 집결 대기(수 분)를 일정 길이로 압축해서 팀 간 행군/클릭 시각 차이가 보이게 한다.
 * 모든 팀의 total()이 같으므로 막대 오른쪽 끝이 항상 맞는다(동시 도착).
 */
class RallyTimelineScale(maxMarchSec: Double) {
    /** 압축된 집결 구간의 화면 길이(초 단위로 환산한 값). */
    val gatherVisual: Double = Math.max(10.0, maxMarchSec * 0.5)

    fun total(clickAt: Double, departAt: Double, arriveAt: Double): Double =
        clickAt + gatherVisual + (arriveAt - departAt)

    /** 실제 경과 시간을 막대 위 위치(0..total)로 옮긴다. */
    fun map(clickAt: Double, departAt: Double, arriveAt: Double, elapsedSec: Double): Double {
        val total = total(clickAt, departAt, arriveAt)
        val e = elapsedSec
        val pos = when {
            e <= clickAt -> e
            e < departAt -> clickAt + (e - clickAt) / (departAt - clickAt) * gatherVisual
            else -> clickAt + gatherVisual + (e - departAt)
        }
        return pos.coerceIn(0.0, total)
    }
}
