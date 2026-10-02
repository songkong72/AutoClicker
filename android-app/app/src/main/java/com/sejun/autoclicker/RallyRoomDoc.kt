package com.sejun.autoclicker

/**
 * 방에서 공유되는 팀 정보. 클릭 좌표와 기기 ms 보정은 기기 로컬이라 여기 없다.
 * [adminAdjustMs]는 관리자가 그 군단에 더해 주는 보정(+면 더 늦게, -면 더 일찍). 기기 보정과 합산해 적용한다.
 */
data class RallyTeamDoc(
    val id: String,
    val name: String,
    val leaderName: String = "",
    val marchSec: Double,
    val excluded: Boolean = false,
    val adminAdjustMs: Int = 0
)

/**
 * 방 문서. startSeq는 "시작" 또는 "다시 집결"을 누를 때마다 1씩 오른다.
 * 각 기기는 startSeq가 바뀐 것을 처음 본 순간을 0초로 삼아 상대시간으로 센다 (서버 시간 불필요).
 */
data class RallyRoomDoc(
    val teams: List<RallyTeamDoc>,
    val prepSec: Double,
    val waitSec: Double,
    val run: String,          // "IDLE" | "RUNNING" | "CANCELLED"
    val startSeq: Long
)

/** Firebase JSON <-> RallyRoomDoc 변환. org.json 대신 Map/List를 써서 JVM 단위 테스트가 가능하다. */
object RallyRoomCodec {
    fun encode(doc: RallyRoomDoc): Map<String, Any?> = mapOf(
        "teams" to doc.teams.map {
            mapOf(
                "id" to it.id, "name" to it.name, "leaderName" to it.leaderName,
                "marchSec" to it.marchSec, "excluded" to it.excluded,
                "adminAdjustMs" to it.adminAdjustMs
            )
        },
        "prepSec" to doc.prepSec,
        "waitSec" to doc.waitSec,
        "run" to doc.run,
        "startSeq" to doc.startSeq
    )

    /** 값이 없거나 숫자가 Int/Long으로 와도 안전하게 읽는다. (Firebase는 빈 리스트를 아예 빼 버린다) */
    fun decode(map: Map<String, Any?>?): RallyRoomDoc {
        if (map == null) return RallyRoomDoc(emptyList(), 0.0, 0.0, "IDLE", 0L)
        val teams = (map["teams"] as? List<*>).orEmpty().mapNotNull { raw ->
            val t = raw as? Map<*, *> ?: return@mapNotNull null
            val id = t["id"] as? String ?: return@mapNotNull null
            RallyTeamDoc(
                id = id,
                name = t["name"] as? String ?: id,
                leaderName = t["leaderName"] as? String ?: "",
                marchSec = (t["marchSec"] as? Number)?.toDouble() ?: 0.0,
                excluded = t["excluded"] as? Boolean ?: false,
                adminAdjustMs = (t["adminAdjustMs"] as? Number)?.toInt() ?: 0
            )
        }
        return RallyRoomDoc(
            teams = teams,
            prepSec = (map["prepSec"] as? Number)?.toDouble() ?: 0.0,
            waitSec = (map["waitSec"] as? Number)?.toDouble() ?: 0.0,
            run = map["run"] as? String ?: "IDLE",
            startSeq = (map["startSeq"] as? Number)?.toLong() ?: 0L
        )
    }
}

/** 내 클릭까지 기다릴 시간(ms). correctionMs가 +면 더 늦게, -면 더 일찍 클릭한다. */
object RallyClickTiming {
    /** 기기 보정(내가 맞춘 값)과 관리자가 더해 준 보정의 합. */
    fun totalCorrectionMs(deviceMs: Long, adminMs: Int): Long = deviceMs + adminMs

    fun delayUntilClickMs(clickAtSec: Double, elapsedMs: Long, correctionMs: Long): Long =
        Math.max(0L, Math.round(clickAtSec * 1000.0) - elapsedMs + correctionMs)
}

/** 관리자 편집. 진행 중(RUNNING)에는 잠기고, 잠긴 상태의 요청은 문서를 그대로 돌려준다. */
object RallyRoomEdit {
    private fun locked(d: RallyRoomDoc) = d.run == "RUNNING"

    /**
     * 서버 문서가 RUNNING이어도 이 기기가 시작 신호를 직접 본 적이 없으면(예전 시도가 끝나지 않은 찌꺼기)
     * 화면은 "대기"로 보인다. 수정도 같은 기준으로 허용해야 버튼이 눌리는데 아무 일도 안 일어나는 일이 없다.
     */
    /** 진행 중이던 집결이 전원 도착 시각을 지났는가. 이때부터 화면은 "전원 도착"이고 수정도 허용해야 한다. */
    fun finished(doc: RallyRoomDoc, elapsedSec: Double): Boolean {
        if (doc.run != "RUNNING") return false
        val plan = RallySchedule.plan(doc.teams.map { RallyTeamInput(it.id, it.marchSec, it.excluded) }, doc.prepSec, doc.waitSec)
        return plan.teams.isNotEmpty() && elapsedSec >= plan.arriveAtSec
    }

    fun unstick(doc: RallyRoomDoc, startSeen: Boolean): RallyRoomDoc =
        if (doc.run == "RUNNING" && !startSeen) doc.copy(run = "IDLE") else doc

    private fun mapTeam(d: RallyRoomDoc, id: String, f: (RallyTeamDoc) -> RallyTeamDoc): RallyRoomDoc =
        if (locked(d)) d else d.copy(teams = d.teams.map { if (it.id == id) f(it) else it })

    fun setMarch(doc: RallyRoomDoc, teamId: String, marchSec: Double): RallyRoomDoc =
        mapTeam(doc, teamId) { it.copy(marchSec = Math.max(0.0, marchSec)) }

    /** 관리자가 군단별로 더하는 클릭 보정(ms). 범위는 기기 보정과 같다. */
    fun setAdminAdjust(doc: RallyRoomDoc, teamId: String, ms: Int): RallyRoomDoc =
        mapTeam(doc, teamId) { it.copy(adminAdjustMs = ms.coerceIn(-RallyInputParse.MAX_CORRECTION_MS, RallyInputParse.MAX_CORRECTION_MS)) }

    fun setPrep(doc: RallyRoomDoc, prepSec: Double): RallyRoomDoc =
        if (locked(doc)) doc else doc.copy(prepSec = Math.max(0.0, prepSec))

    /** 집결 대기시간은 게임 규칙상 3분/5분/10분만 허용한다. */
    val WAIT_PRESETS_SEC = listOf(180.0, 300.0, 600.0)

    fun setWait(doc: RallyRoomDoc, waitSec: Double): RallyRoomDoc =
        if (locked(doc) || waitSec !in WAIT_PRESETS_SEC) doc else doc.copy(waitSec = waitSec)

    fun setExcluded(doc: RallyRoomDoc, teamId: String, excluded: Boolean): RallyRoomDoc =
        mapTeam(doc, teamId) { it.copy(excluded = excluded) }

    fun addTeam(doc: RallyRoomDoc, name: String, marchSec: Double): RallyRoomDoc {
        if (locked(doc)) return doc
        var n = doc.teams.size + 1
        while (doc.teams.any { it.id == "t$n" }) n++
        return doc.copy(teams = doc.teams + RallyTeamDoc("t$n", name, "", Math.max(0.0, marchSec)))
    }

    fun removeTeam(doc: RallyRoomDoc, teamId: String): RallyRoomDoc =
        if (locked(doc)) doc else doc.copy(teams = doc.teams.filter { it.id != teamId })

    /** 시작 또는 다시 집결: 항상 RUNNING + startSeq 증가. 참여 팀이 없으면 그대로. */
    fun startOrRegroup(doc: RallyRoomDoc): RallyRoomDoc =
        if (doc.teams.none { !it.excluded }) doc else doc.copy(run = "RUNNING", startSeq = doc.startSeq + 1)

    fun cancel(doc: RallyRoomDoc): RallyRoomDoc =
        if (doc.run != "RUNNING") doc else doc.copy(run = "CANCELLED")

    /** 관리자는 모든 팀, 팀장은 내 팀의 행군시간만. 진행 중에는 모두 잠긴다. */
    fun canEditMarch(doc: RallyRoomDoc, isAdmin: Boolean, myTeamId: String, teamId: String): Boolean {
        if (locked(doc)) return false
        val team = doc.teams.firstOrNull { it.id == teamId } ?: return false
        if (team.excluded) return false
        return isAdmin || teamId == myTeamId
    }
}
