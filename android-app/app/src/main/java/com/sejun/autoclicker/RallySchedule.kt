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

    /**
     * 제외(excluded)된 팀은 최대 행군 계산과 클릭 예약에서 빠진다.
     * 참여 팀이 없으면 빈 계획을 돌려준다.
     */
    fun plan(teams: List<RallyTeamInput>, prepSec: Double, waitSec: Double): RallyPlan {
        require(prepSec >= 0.0) { "이동 준비시간은 0 이상이어야 합니다: $prepSec" }
        require(waitSec >= 0.0) { "집결 대기시간은 0 이상이어야 합니다: $waitSec" }
        teams.forEach { require(it.marchSec >= 0.0) { "행군시간은 0 이상이어야 합니다: ${it.id}=${it.marchSec}" } }

        val active = teams.filter { !it.excluded }
        if (active.isEmpty()) return RallyPlan(0.0, 0.0, emptyList())

        val maxMarch = active.maxOf { it.marchSec }
        val arrive = prepSec + maxMarch + waitSec
        val plans = active.map {
            val click = prepSec + (maxMarch - it.marchSec)
            val depart = click + waitSec
            RallyTeamPlan(it.id, it.marchSec, click, depart, depart + it.marchSec)
        }.sortedBy { it.clickAtSec }
        return RallyPlan(maxMarch, arrive, plans)
    }

    /** 시작 신호 후 elapsedSec 시점에 해당 팀이 어느 단계인지. 경계 시각은 다음 단계로 넘어간다. */
    fun phaseAt(team: RallyTeamPlan, elapsedSec: Double): RallyPhase = when {
        elapsedSec < team.clickAtSec -> RallyPhase.BEFORE_CLICK
        elapsedSec < team.departAtSec -> RallyPhase.GATHERING
        elapsedSec < team.arriveAtSec -> RallyPhase.MARCHING
        else -> RallyPhase.ARRIVED
    }

    /** 현재 단계가 끝날 때까지 남은 초. 도착 후에는 0. */
    fun remainingInPhaseSec(team: RallyTeamPlan, elapsedSec: Double): Double = when (phaseAt(team, elapsedSec)) {
        RallyPhase.BEFORE_CLICK -> team.clickAtSec - elapsedSec
        RallyPhase.GATHERING -> team.departAtSec - elapsedSec
        RallyPhase.MARCHING -> team.arriveAtSec - elapsedSec
        RallyPhase.ARRIVED -> 0.0
    }
}
