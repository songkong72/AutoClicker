package com.sejun.autoclicker

/** 개발자 화면의 방 목록 한 줄. [assigned]는 팀장이 배정된 군단 수, [members]는 방 명단에 올라온 인원이다. */
internal data class RoomOverview(val code: String, val teamCount: Int, val assigned: Int, val run: String, val members: Int)

/** 서버의 방(rallyRooms)과 방 명단(rallyMembers)을 개발자가 보기 좋게 요약한다. 안드로이드 클래스를 쓰지 않아 단위 테스트가 된다. */
internal object RoomList {

    fun runLabel(run: String): String = when (run) {
        "IDLE" -> "대기 중"
        "RUNNING" -> "진행 중"
        "CANCELLED" -> "취소됨"
        else -> run
    }

    /** 방마다 한 줄 요약. 진행 중인 방이 먼저, 나머지는 코드순. 명단에만 있고 방이 없는 코드는 뺀다. */
    fun summarize(rooms: Map<String, Any?>?, members: Map<String, Any?>?): List<RoomOverview> =
        rooms.orEmpty().mapNotNull { (code, raw) ->
            val m = raw as? Map<*, *> ?: return@mapNotNull null
            val doc = RallyRoomCodec.decode(m.entries.associate { it.key.toString() to it.value })
            val count = (members?.get(code) as? Map<*, *>)?.size ?: 0
            RoomOverview(code, doc.teams.size, doc.teams.count { it.leaderId.isNotEmpty() }, doc.run, count)
        }.sortedWith(compareByDescending<RoomOverview> { it.run == "RUNNING" }.thenBy { it.code })

    /** 방 선택 목록용 한 줄: 명단 인원과 진행 상태는 뺀다. */
    fun lineBrief(o: RoomOverview): String =
        "${o.code} · 군단 ${o.teamCount}개 (배정 ${o.assigned})"

    fun line(o: RoomOverview): String =
        "${o.code} · 군단 ${o.teamCount}개 (배정 ${o.assigned}) · ${runLabel(o.run)} · 명단 ${o.members}명"

    /** 방 하나의 자세한 내용(읽기 전용). 군단별 팀장과 행군시간, 명단의 이름들. */
    fun detail(code: String, room: Map<*, *>, members: Map<*, *>?): String {
        val doc = RallyRoomCodec.decode(room.entries.associate { it.key.toString() to it.value })
        val names = members?.values.orEmpty().mapNotNull { (it as? Map<*, *>)?.get("name") as? String }.sorted()
        return buildString {
            append("방 $code\n")
            append("상태 ${runLabel(doc.run)} · 준비 ${num(doc.prepSec)}초 · 대기 ${num(doc.waitSec)}초\n\n군단\n")
            if (doc.teams.isEmpty()) append("(없음)\n")
            doc.teams.forEachIndexed { i, t ->
                val leader = if (t.leaderId.isNotEmpty() && t.leaderName.isNotEmpty()) t.leaderName else "미배정"
                append("${i + 1}. ${t.name} · 팀장 $leader · 행군 ${num(t.marchSec)}초${if (t.excluded) " · 제외" else ""}\n")
            }
            append("\n명단 ${names.size}명")
            if (names.isNotEmpty()) append(": ${names.joinToString(", ")}")
        }
    }

    private fun num(d: Double): String = if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
}
