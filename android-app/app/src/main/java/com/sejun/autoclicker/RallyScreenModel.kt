package com.sejun.autoclicker

enum class RallyRunState { IDLE, RUNNING, ARRIVED, CANCELLED }

enum class HeroKind { IDLE, MOVE, WAIT_CLICK, GATHERING, MARCHING, ARRIVED, CANCELLED, EXCLUDED }

data class RallyTeamState(
    val id: String,
    val name: String,
    val leaderName: String = "",
    val marchSec: Double,
    val online: Boolean = true,
    val excluded: Boolean = false,
    /** 관리자가 이 군단에 더해 준 클릭 보정(ms) */
    val adminAdjustMs: Int = 0,
    /** 이 군단에 배정된 사람의 기기 ID. 배정 전이면 빈 문자열. */
    val leaderId: String = ""
)

data class RallyRoomState(
    val teams: List<RallyTeamState>,
    val myTeamId: String,
    val prepSec: Double,
    val waitSec: Double,
    val runState: RallyRunState,
    /** 시작 신호 이후 경과 초. RUNNING일 때만 의미가 있다. */
    val elapsedSec: Double = 0.0,
    /** 이 기기에 클릭 위치가 저장되어 있는지 */
    val positionSaved: Boolean = true,
    /** 이 기기의 캐릭터명이 등록되어 있는지(관리자는 등록하지 않아도 되므로 true로 넘긴다) */
    val characterNameSet: Boolean = true
)

data class HeroModel(
    val kind: HeroKind,
    val label: String,
    /** 화면에 크게 보여줄 남은 초. 없으면 null (UI가 "--:--" 또는 "완료"로 표시) */
    val remainingSec: Double?,
    val subLabel: String,
    /** 0.0 ~ 1.0 */
    val progress: Double
)

data class TeamRowModel(
    val id: String,
    val name: String,
    val leaderName: String,
    val marchSec: Double,
    val statusLabel: String,
    val remainingSec: Double?,
    val excluded: Boolean,
    val online: Boolean,
    val isMine: Boolean,
    val clickAtSec: Double = 0.0,
    val departAtSec: Double = 0.0,
    val arriveAtSec: Double = 0.0,
    val adminAdjustMs: Int = 0
)

data class ScreenModel(
    val hero: HeroModel,
    val rows: List<TeamRowModel>,
    val maxMarchSec: Double,
    /** 팀 추가/삭제/행군시간/제외 수정 가능 여부 (진행 중에는 잠금) */
    val editable: Boolean,
    val warnings: List<String>,
    /** 시작할 수 없는 이유. 시작 가능하면 null */
    val startBlockedReason: String?,
    val arriveAtSec: Double = 0.0,
    val nowSec: Double? = null,
    val prepSec: Double = 0.0,
    val waitSec: Double = 0.0
)

object RallyScreenModel {

    fun build(state: RallyRoomState): ScreenModel {
        val plan = RallySchedule.plan(
            state.teams.map { RallyTeamInput(it.id, it.marchSec, it.excluded) },
            state.prepSec,
            state.waitSec
        )
        val myPlan = plan.teamPlan(state.myTeamId)
        val elapsed = state.elapsedSec
        val arrivedByTime = state.runState == RallyRunState.RUNNING &&
            plan.teams.isNotEmpty() && elapsed >= plan.arriveAtSec
        val run = if (state.runState == RallyRunState.ARRIVED || arrivedByTime) RallyRunState.ARRIVED else state.runState
        val editable = run != RallyRunState.RUNNING

        return ScreenModel(
            hero = hero(state, myPlan, run),
            rows = rows(state, plan, run),
            maxMarchSec = plan.maxMarchSec,
            editable = editable,
            warnings = nameWarnings(state) + positionWarnings(state) + (if (editable) offlineWarnings(state) else emptyList()),
            startBlockedReason = if (plan.teams.isEmpty()) "참여 팀이 없어요" else null,
            arriveAtSec = plan.arriveAtSec,
            nowSec = if (state.runState == RallyRunState.RUNNING) state.elapsedSec else null,
            prepSec = state.prepSec,
            waitSec = state.waitSec
        )
    }

    private fun hero(state: RallyRoomState, my: RallyTeamPlan?, run: RallyRunState): HeroModel = when {
        run == RallyRunState.CANCELLED ->
            HeroModel(HeroKind.CANCELLED, "작전 취소됨", null, "예약된 클릭이 모두 멈췄어요", 1.0)
        run == RallyRunState.ARRIVED ->
            HeroModel(HeroKind.ARRIVED, "전원 도착", null, "실패했다면 바로 재집결하세요", 1.0)
        state.teams.isEmpty() ->
            HeroModel(HeroKind.IDLE, "관리자가 팀을 구성하는 중이에요", null, "방에 팀이 생기면 여기에 표시돼요", 0.0)
        state.teams.none { it.id == state.myTeamId } ->
            HeroModel(HeroKind.IDLE, "아직 군단이 배정되지 않았어요", null, "관리자가 군단을 배정하면 시작할 수 있어요", 0.0)
        my == null ->
            HeroModel(HeroKind.EXCLUDED, "이번 작전에서 제외됐어요", null, "관리자가 다시 포함하면 참여할 수 있어요", 0.0)
        run == RallyRunState.IDLE ->
            HeroModel(HeroKind.IDLE, "관리자 시작 대기중", null, "시작 후 ${Math.ceil(my.clickAtSec).toInt()}초에 내 집결 클릭", 0.0)
        else -> runningHero(state, my)
    }

    private fun runningHero(state: RallyRoomState, my: RallyTeamPlan): HeroModel {
        val e = state.elapsedSec
        return when (RallySchedule.phaseAt(my, e)) {
            RallyPhase.BEFORE_CLICK -> {
                val remaining = my.clickAtSec - e
                val progress = if (my.clickAtSec <= 0.0) 1.0 else (e / my.clickAtSec).coerceIn(0.0, 1.0)
                if (remaining <= state.prepSec) HeroModel(HeroKind.MOVE, "집결 화면으로 이동하세요", remaining, "집결 클릭까지", progress)
                else HeroModel(HeroKind.WAIT_CLICK, "집결 대기중", remaining, "집결 클릭까지", progress)
            }
            RallyPhase.GATHERING -> HeroModel(
                HeroKind.GATHERING, "집결 중", my.departAtSec - e,
                "출발까지 · 이후 행군 ${formatMmSs(my.marchSec)}",
                ((e - my.clickAtSec) / (my.departAtSec - my.clickAtSec)).coerceIn(0.0, 1.0)
            )
            RallyPhase.MARCHING -> HeroModel(
                HeroKind.MARCHING, "행군 중", my.arriveAtSec - e, "성 도착까지",
                if (my.marchSec <= 0.0) 1.0 else ((e - my.departAtSec) / my.marchSec).coerceIn(0.0, 1.0)
            )
            RallyPhase.ARRIVED -> HeroModel(HeroKind.ARRIVED, "전원 도착", null, "실패했다면 바로 재집결하세요", 1.0)
        }
    }

    private fun rows(state: RallyRoomState, plan: RallyPlan, run: RallyRunState): List<TeamRowModel> {
        val byId = state.teams.associateBy { it.id }
        val active = plan.teams.mapNotNull { p -> byId[p.id]?.let { it to p } }
        val excluded = state.teams.filter { it.excluded }
        return active.map { (t, p) -> row(state, t, p, run) } + excluded.map { t ->
            TeamRowModel(t.id, t.name, t.leaderName, t.marchSec, "제외", null, true, t.online, t.id == state.myTeamId,
                adminAdjustMs = t.adminAdjustMs)
        }
    }

    private fun row(state: RallyRoomState, t: RallyTeamState, p: RallyTeamPlan, run: RallyRunState): TeamRowModel {
        val e = state.elapsedSec
        val (label, remaining) = when (run) {
            RallyRunState.CANCELLED -> "취소" to null
            RallyRunState.ARRIVED -> "도착" to null
            RallyRunState.IDLE -> "대기" to null
            RallyRunState.RUNNING -> when (RallySchedule.phaseAt(p, e)) {
                RallyPhase.BEFORE_CLICK -> "클릭 전" to (p.clickAtSec - e)
                RallyPhase.GATHERING -> "집결 중" to (p.departAtSec - e)
                RallyPhase.MARCHING -> "행군" to (p.arriveAtSec - e)
                RallyPhase.ARRIVED -> "도착" to null
            }
        }
        return TeamRowModel(
            t.id, t.name, t.leaderName, t.marchSec, label, remaining, false, t.online, t.id == state.myTeamId,
            p.clickAtSec, p.departAtSec, p.arriveAtSec, t.adminAdjustMs
        )
    }

    /** 캐릭터명이 없으면 관리자가 명단에서 나를 찾아 배정할 수 없다. */
    private fun nameWarnings(state: RallyRoomState): List<String> =
        if (state.characterNameSet) emptyList() else listOf("캐릭터명을 먼저 등록하세요 (내 기기 → 캐릭터명)")

    /** 내가 이번 작전에 참여하는데 클릭 위치가 없으면 클릭이 나가지 않는다. 진행 중에도 계속 알린다. */
    private fun positionWarnings(state: RallyRoomState): List<String> {
        val me = state.teams.firstOrNull { it.id == state.myTeamId } ?: return emptyList()
        return if (!state.positionSaved && !me.excluded) listOf("클릭 위치를 먼저 저장하세요 (내 기기 → 위치 저장)") else emptyList()
    }

    /** 대기 중 큰 숫자 아래 한 줄. 큰 숫자가 전체 소요라는 건 자명하니 반복하지 않고, 참여 팀 수를 붙인다. */
    fun idleSub(subLabel: String, activeTeams: Int): String = "$subLabel · 참여 ${activeTeams}팀"

    private fun offlineWarnings(state: RallyRoomState): List<String> =
        state.teams.filter { !it.excluded && !it.online }
            .map { listOf(it.name, it.leaderName).filter { s -> s.isNotBlank() }.joinToString(" ") + " 연결 없음" }

    /** 남은 초를 m:ss로. 카운트다운은 올림(7.2초 -> 0:08), 음수는 0:00. */
    fun formatMmSs(sec: Double): String {
        val total = Math.ceil(Math.max(0.0, sec)).toLong()
        val ss = total % 60
        return "${total / 60}:${if (ss < 10) "0$ss" else "$ss"}"
    }
}
