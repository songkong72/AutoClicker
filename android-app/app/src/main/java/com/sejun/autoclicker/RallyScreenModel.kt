package com.sejun.autoclicker

enum class RallyRunState { IDLE, RUNNING, ARRIVED, CANCELLED }

enum class HeroKind { IDLE, MOVE, WAIT_CLICK, GATHERING, MARCHING, ARRIVED, CANCELLED, EXCLUDED,
    /** 내 군단이 없는 사람(지휘관 등)이 진행 중에 보는 전체 상황. 큰 숫자는 전원 도착까지, 단계는 [HeroModel.phase]. */
    OVERVIEW }

data class RallyTeamState(
    val id: String,
    val name: String,
    val leaderName: String = "",
    val marchSec: Double,
    val online: Boolean = true,
    val excluded: Boolean = false,
    /** 지휘관이 이 군단에 더해 준 클릭 보정(ms) */
    val adminAdjustMs: Int = 0,
    /** 이 군단에 배정된 사람의 기기 ID. 배정 전이면 빈 문자열. */
    val leaderId: String = "",
    /** 조(1~5) */
    val wave: Int = 1
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
    /** 이 기기의 캐릭터명이 등록되어 있는지(지휘관은 등록하지 않아도 되므로 true로 넘긴다) */
    val characterNameSet: Boolean = true,
    /** 지휘관 기기인가. 지휘관이 자기 군단을 제외하면 "제외됐어요" 대신 전체 진행을 보여 준다. */
    val isAdmin: Boolean = false,
    /** 조별 도착 간격(초). [RallyWaves] 참고. */
    val waveDelaySec: Map<Int, Double> = emptyMap()
)

data class HeroModel(
    val kind: HeroKind,
    val label: String,
    /** 화면에 크게 보여줄 남은 초. 없으면 null (UI가 "--:--" 또는 "완료"로 표시) */
    val remainingSec: Double?,
    val subLabel: String,
    /** 0.0 ~ 1.0 */
    val progress: Double,
    /** [HeroKind.OVERVIEW]에서만: 전체 단계 0 대기 · 1 집결 · 2 행군. */
    val phase: Int? = null,
    /** 제목 옆에 덧붙일 짧은 표시(예: 자기 군단을 제외한 지휘관의 "참여 안 함"). 없으면 null. */
    val note: String? = null
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
    val adminAdjustMs: Int = 0,
    val wave: Int = 1,
    /** 이 군단의 조가 가진 도착 간격(초). 누르는 순서 안내에 쓴다. */
    val delaySec: Double = 0.0
)

/** 조 한 줄(목록의 조 제목). 군단이 하나라도 들어 있는 조만 만든다. */
data class WaveModel(
    val wave: Int,
    val delaySec: Double,
    /** 참여(제외 아님) 군단 수 */
    val activeCount: Int,
    /** 시작 후 이 조가 성에 도착하는 초. 참여 군단이 없으면 null. */
    val arriveAtSec: Double?,
    /** 진행 중일 때 도착까지 남은 초. 그 밖에는 null. */
    val remainingSec: Double?,
    /** 진행 중일 때 단계: 0 대기 · 1 집결 · 2 행군 · 3 도착. 그 밖에는 null. */
    val phase: Int?
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
    val waitSec: Double = 0.0,
    /** 조를 쓰는 방인가(1조가 아닌 군단이 하나라도 있다). 아니면 화면은 조가 없던 때와 같다. */
    val waveMode: Boolean = false,
    /** 군단이 들어 있는 조들(조 번호 순). 조를 쓰지 않으면 1조 하나다. */
    val waves: List<WaveModel> = emptyList(),
    /** 대기 중 큰 숫자에 흐리게 보일 초: 가장 먼저 닿는 조의 도착까지(조를 쓰지 않으면 전원 도착까지). */
    val previewSec: Double = 0.0,
    /** 조를 쓸 때 대기 중 큰 숫자 아래 한 줄. 없으면 null. */
    val previewNote: String? = null
)

object RallyScreenModel {

    fun build(state: RallyRoomState): ScreenModel {
        val plan = RallySchedule.plan(
            state.teams.map { RallyTeamInput(it.id, it.marchSec, it.excluded, RallyWaves.delayOf(state.waveDelaySec, it.wave)) },
            state.prepSec,
            state.waitSec
        )
        val waveMode = state.teams.any { it.wave != 1 }
        val myPlan = plan.teamPlan(state.myTeamId)
        val elapsed = state.elapsedSec
        val arrivedByTime = state.runState == RallyRunState.RUNNING &&
            plan.teams.isNotEmpty() && elapsed >= plan.arriveAtSec
        val run = if (state.runState == RallyRunState.ARRIVED || arrivedByTime) RallyRunState.ARRIVED else state.runState
        val editable = run != RallyRunState.RUNNING
        val waves = waves(state, plan, run)
        val first = waves.filter { it.arriveAtSec != null }.minByOrNull { it.arriveAtSec ?: 0.0 }

        return ScreenModel(
            hero = hero(state, myPlan, run, plan, if (waveMode) waves else emptyList()),
            rows = rows(state, plan, run, waveMode),
            maxMarchSec = plan.maxMarchSec,
            editable = editable,
            warnings = nameWarnings(state) + positionWarnings(state) + (if (editable) offlineWarnings(state) else emptyList()),
            startBlockedReason = if (plan.teams.isEmpty()) "참여 군단이 없어요" else null,
            arriveAtSec = plan.arriveAtSec,
            nowSec = if (state.runState == RallyRunState.RUNNING) state.elapsedSec else null,
            prepSec = state.prepSec,
            waitSec = state.waitSec,
            waveMode = waveMode,
            waves = waves,
            previewSec = if (waveMode) first?.arriveAtSec ?: 0.0 else plan.arriveAtSec,
            previewNote = if (waveMode && first != null) previewNote(first, waves) else null
        )
    }

    /** "1조 도착까지 · 2조 +1분 · 3조 +2분" */
    private fun previewNote(first: WaveModel, waves: List<WaveModel>): String {
        val base = first.arriveAtSec ?: 0.0
        val rest = waves.filter { it.arriveAtSec != null && it.wave != first.wave }.sortedBy { it.arriveAtSec ?: 0.0 }
            .joinToString("") { " · ${it.wave}조 +${RallyWaves.span((it.arriveAtSec ?: base) - base)}" }
        return "${first.wave}조 도착까지$rest"
    }

    /** 군단이 들어 있는 조마다 한 줄. 같은 조의 참여 군단은 같은 시각에 도착한다. */
    private fun waves(state: RallyRoomState, plan: RallyPlan, run: RallyRunState): List<WaveModel> {
        val planById = plan.teams.associateBy { it.id }
        val e = state.elapsedSec
        return state.teams.groupBy { it.wave }.toSortedMap().map { (wave, teams) ->
            val plans = teams.mapNotNull { planById[it.id] }
            val arrive = plans.maxOfOrNull { it.arriveAtSec }
            val running = run == RallyRunState.RUNNING && arrive != null
            WaveModel(
                wave = wave,
                delaySec = RallyWaves.delayOf(state.waveDelaySec, wave),
                activeCount = plans.size,
                arriveAtSec = arrive,
                remainingSec = if (running) Math.max(0.0, (arrive ?: e) - e) else null,
                phase = if (!running) null else when {
                    e < plans.minOf { it.clickAtSec } -> 0
                    e < plans.maxOf { it.departAtSec } -> 1
                    e < (arrive ?: e) -> 2
                    else -> 3
                }
            )
        }
    }

    private fun hero(state: RallyRoomState, my: RallyTeamPlan?, run: RallyRunState, plan: RallyPlan, waves: List<WaveModel>): HeroModel = when {
        run == RallyRunState.CANCELLED ->
            HeroModel(HeroKind.CANCELLED, "작전 취소됨", null, "예약된 클릭이 모두 멈췄어요", 1.0)
        run == RallyRunState.ARRIVED ->
            HeroModel(HeroKind.ARRIVED, "전원 도착", null, "실패했다면 바로 재집결하세요", 1.0)
        state.teams.isEmpty() && state.isAdmin ->
            HeroModel(HeroKind.IDLE, "이 방에는 군단이 없어요", null, "방 번호가 맞는지 확인하세요 · 새 방은 앱 첫 화면에서 만들어요", 0.0)
        state.teams.isEmpty() ->
            HeroModel(HeroKind.IDLE, "지휘관이 군단을 구성하는 중이에요", null, "방에 군단이 생기면 여기에 표시돼요", 0.0)
        // 군단이 없는 지휘관 등: 내 클릭은 없어도 진행 중에는 전원 도착까지 남은 시간을 보여 준다(숫자가 멈춰 보이지 않게)
        // 집결장이 군단을 못 찾았을 때는 전체 보기가 아니라 아래의 "배정되지 않았어요" 안내를 보여 준다.
        (state.isAdmin && (state.teams.none { it.id == state.myTeamId } || my == null)) && run == RallyRunState.RUNNING ->
            (waveHero(waves, state.elapsedSec) ?: overviewHero(plan, state.elapsedSec, state.prepSec))
                .copy(note = if (state.teams.any { it.id == state.myTeamId }) "참여 안 함" else null)
        state.teams.none { it.id == state.myTeamId } ->
            HeroModel(HeroKind.IDLE, "아직 군단이 배정되지 않았어요", null, "지휘관이 군단을 배정하면 시작할 수 있어요", 0.0)
        state.isAdmin && my == null ->
            HeroModel(HeroKind.IDLE, "이번 작전에는 참여하지 않아요", null, "군단 목록에서 진행을 확인하세요", 0.0)
        my == null ->
            HeroModel(HeroKind.EXCLUDED, "이번 작전에서 제외됐어요", null, "지휘관이 다시 포함하면 참여할 수 있어요", 0.0)
        run == RallyRunState.IDLE ->
            HeroModel(HeroKind.IDLE, "지휘관 시작 대기 중", null, "시작 후 ${Math.ceil(my.clickAtSec).toInt()}초에 내 집결 클릭", 0.0)
        else -> runningHero(state, my)
    }

    /**
     * 집결장 화면처럼 "다음 단계까지 남은 시간"을 센다. 단계 이름은 최소화한 알약과 같은 글자를 쓴다.
     * 첫 클릭 전(준비시간 이내면 이동 준비, 아니면 집결 대기) → 첫 클릭부터 모든 군단이 출발할 때까지 집결 중 → 출발 뒤 행군 중.
     */
    private fun overviewHero(plan: RallyPlan, e: Double, prepSec: Double): HeroModel {
        val firstClick = plan.teams.minOfOrNull { it.clickAtSec } ?: 0.0
        val lastDepart = plan.teams.maxOfOrNull { it.departAtSec } ?: 0.0
        fun frac(from: Double, to: Double) = if (to <= from) 1.0 else ((e - from) / (to - from)).coerceIn(0.0, 1.0)
        return when {
            e < firstClick -> {
                val remaining = firstClick - e
                HeroModel(HeroKind.OVERVIEW, if (remaining <= prepSec) "이동 준비" else "클릭 대기", remaining, "첫 집결 클릭까지", frac(0.0, firstClick), 0)
            }
            e < lastDepart -> HeroModel(HeroKind.OVERVIEW, "집결 중", lastDepart - e, "전원 출발까지", frac(firstClick, lastDepart), 1)
            else -> HeroModel(HeroKind.OVERVIEW, "행군 중", Math.max(0.0, plan.arriveAtSec - e), "전원 도착까지", frac(lastDepart, plan.arriveAtSec), 2)
        }
    }

    /**
     * 조를 쓰는 방에서 군단이 없는 지휘관이 보는 큰 숫자: 다음에 도착할 조의 도착까지 남은 시간.
     * 그 조가 도착하면 다음 조로 넘어간다. 조를 쓰지 않거나 남은 조가 없으면 null.
     */
    private fun waveHero(waves: List<WaveModel>, e: Double): HeroModel? {
        val coming = waves.filter { (it.arriveAtSec ?: -1.0) > e }.sortedBy { it.arriveAtSec ?: 0.0 }
        val next = coming.firstOrNull() ?: return null
        val arrive = next.arriveAtSec ?: return null
        val prevArrive = waves.mapNotNull { it.arriveAtSec }.filter { it <= e }.maxOrNull() ?: 0.0
        val stage = when (next.phase) { 0 -> "클릭 전"; 1 -> "집결 중"; else -> "행군 중" }
        val after = coming.getOrNull(1)?.let { " · 다음 ${it.wave}조 ${formatMmSs((it.arriveAtSec ?: e) - e)}" } ?: ""
        val progress = if (arrive <= prevArrive) 1.0 else ((e - prevArrive) / (arrive - prevArrive)).coerceIn(0.0, 1.0)
        return HeroModel(HeroKind.OVERVIEW, "${next.wave}조 $stage", arrive - e, "${next.wave}조 도착까지$after", progress, (next.phase ?: 0).coerceIn(0, 2))
    }

    private fun runningHero(state: RallyRoomState, my: RallyTeamPlan): HeroModel {
        val e = state.elapsedSec
        return when (RallySchedule.phaseAt(my, e)) {
            RallyPhase.BEFORE_CLICK -> {
                val remaining = my.clickAtSec - e
                val progress = if (my.clickAtSec <= 0.0) 1.0 else (e / my.clickAtSec).coerceIn(0.0, 1.0)
                if (remaining <= state.prepSec) HeroModel(HeroKind.MOVE, "집결 화면으로 이동하세요", remaining, "집결 클릭까지", progress)
                else HeroModel(HeroKind.WAIT_CLICK, "클릭 대기 중", remaining, "집결 클릭까지", progress)
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

    /**
     * 군단 이름(번호) 순서로 보여 준다. 행군시간을 고치거나 제외해도 줄이 움직이지 않는다.
     * 번호는 이름 속 첫 숫자(2군 → 2, 10군 → 10)로 비교하고, 숫자가 없는 이름은 만든 순서대로 뒤에 둔다.
     */
    private fun rows(state: RallyRoomState, plan: RallyPlan, run: RallyRunState, waveMode: Boolean): List<TeamRowModel> {
        val planById = plan.teams.associateBy { it.id }
        // 조를 쓰면 조별로 묶고(1조부터), 조 안에서는 군단 번호 순서 그대로
        val ordered = state.teams.withIndex()
            .sortedWith(compareBy({ if (waveMode) it.value.wave else 0 }, { RallyWaves.teamNumber(it.value.name) ?: Int.MAX_VALUE }, { it.index }))
            .map { it.value }
        return ordered.map { t ->
            val p = planById[t.id]
            val delay = RallyWaves.delayOf(state.waveDelaySec, t.wave)
            if (t.excluded || p == null)
                TeamRowModel(t.id, t.name, t.leaderName, t.marchSec, "제외", null, true, t.online, t.id == state.myTeamId,
                    adminAdjustMs = t.adminAdjustMs, wave = t.wave, delaySec = delay)
            else row(state, t, p, run).copy(wave = t.wave, delaySec = delay)
        }
    }

    private fun row(state: RallyRoomState, t: RallyTeamState, p: RallyTeamPlan, run: RallyRunState): TeamRowModel {
        val e = state.elapsedSec
        val (label, remaining) = when (run) {
            // 대기·취소는 위의 큰 글자와 단계 줄이 이미 말해 주므로 줄마다 되풀이하지 않는다
            RallyRunState.CANCELLED -> "" to null
            RallyRunState.ARRIVED -> "도착" to null
            RallyRunState.IDLE -> "" to null
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

    /** 캐릭터명이 없으면 지휘관이 명단에서 나를 찾아 배정할 수 없다. */
    /** 준비가 덜 됐을 때의 안내. 패널에서 이 줄을 누르면 바로 해당 입력으로 간다. */
    const val WARN_NAME = "캐릭터명을 먼저 등록하세요 (눌러서 입력)"
    const val WARN_POSITION = "클릭 위치를 먼저 저장하세요 (눌러서 열기)"

    private fun nameWarnings(state: RallyRoomState): List<String> =
        if (state.characterNameSet) emptyList() else listOf(WARN_NAME)

    /** 내가 이번 작전에 참여하는데 클릭 위치가 없으면 클릭이 나가지 않는다. 진행 중에도 계속 알린다. */
    private fun positionWarnings(state: RallyRoomState): List<String> {
        val me = state.teams.firstOrNull { it.id == state.myTeamId } ?: return emptyList()
        return if (!state.positionSaved && !me.excluded) listOf(WARN_POSITION) else emptyList()
    }

    /** 대기 중 큰 숫자 아래 한 줄. 큰 숫자가 전체 소요라는 건 자명하니 반복하지 않고, 참여 군단 수를 붙인다. */
    fun idleSub(subLabel: String, activeTeams: Int): String = "$subLabel · 참여 군단 ${activeTeams}개"

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
