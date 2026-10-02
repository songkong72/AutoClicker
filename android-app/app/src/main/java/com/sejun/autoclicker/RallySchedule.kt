package com.sejun.autoclicker

/**
 * 동시 도착 집결 일정 계산 (안드로이드 의존성 없음).
 *
 * 모든 시간은 "관리자가 시작을 누른 시점"을 0으로 하는 상대 시간(초)이다.
 * 절대 시각이나 서버 시각은 쓰지 않는다.
 *
 * 타임라인(팀별): 집결 클릭 -> [집결 대기] -> 행군 출발 -> [행군시간] -> 성 도착
 *
 *   최대 행군      = 참여 팀 행군시간의 최댓값
 *   집결 클릭 시각 = 이동 준비시간 + (최대 행군 - 내 행군)
 *   출발 시각      = 집결 클릭 시각 + 집결 대기
 *   도착 시각      = 출발 시각 + 내 행군  (= 이동 준비 + 최대 행군 + 집결 대기, 전 팀 동일)
 */
data class RallyTeamInput(
    val id: String,
    val marchSec: Double,
    val excluded: Boolean = false
)

data class RallyTeamPlan(
    val id: String,
    val marchSec: Double,
    val clickAtSec: Double,
    val departAtSec: Double,
    val arriveAtSec: Double
)

data class RallyPlan(
    val maxMarchSec: Double,
    val arriveAtSec: Double,
    /** 집결 클릭이 빠른 순 (행군이 긴 팀이 먼저) */
    val teams: List<RallyTeamPlan>
) {
    fun teamPlan(id: String): RallyTeamPlan? = teams.firstOrNull { it.id == id }
}

enum class RallyPhase { BEFORE_CLICK, GATHERING, MARCHING, ARRIVED }

object RallySchedule {
    // RED 확인용 임시 스텁: 구현 전 상태. 테스트가 실패해야 한다.
    fun plan(teams: List<RallyTeamInput>, prepSec: Double, waitSec: Double): RallyPlan =
        RallyPlan(0.0, 0.0, emptyList())

    fun phaseAt(team: RallyTeamPlan, elapsedSec: Double): RallyPhase = RallyPhase.BEFORE_CLICK

    fun remainingInPhaseSec(team: RallyTeamPlan, elapsedSec: Double): Double = 0.0
}
