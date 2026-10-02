package com.sejun.autoclicker

/** 방에서 공유되는 팀 정보. 클릭 좌표와 ms 보정은 기기 로컬이라 여기 없다. */
data class RallyTeamDoc(
    val id: String,
    val name: String,
    val leaderName: String = "",
    val marchSec: Double,
    val excluded: Boolean = false
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
                "marchSec" to it.marchSec, "excluded" to it.excluded
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
                excluded = t["excluded"] as? Boolean ?: false
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
    fun delayUntilClickMs(clickAtSec: Double, elapsedMs: Long, correctionMs: Long): Long =
        Math.max(0L, Math.round(clickAtSec * 1000.0) - elapsedMs + correctionMs)
}
