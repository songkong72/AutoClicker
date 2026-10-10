package com.sejun.autoclicker

/**
 * 연맹 대표 신청 한 건(repRequests/{기기 ID}). [group]은 "2000-WBI" 모양, [status]는 "pending"(기다리는 중) 또는 "rejected"(거절),
 * [reason]은 거절할 때 개발자가 적은 한 줄이다.
 */
internal data class RepRequest(
    val uid: String,
    val group: String,
    val name: String,
    val note: String,
    val createdAt: Long,
    val status: String = RallyRoles.PENDING,
    val reason: String = ""
) {
    val pending: Boolean get() = status == RallyRoles.PENDING
}

/**
 * 지휘관을 정하는 길의 규칙. 개발자 → 연맹 대표 → 지휘관, 한 방향 한 단계다(지휘관은 다른 지휘관을 정하지 못한다).
 * 안드로이드 클래스를 쓰지 않아 단위 테스트가 된다. 서버 쪽 강제는 `database.rules.next.json`이 한다.
 */
internal object RallyRoles {
    const val PENDING = "pending"
    const val REJECTED = "rejected"
    /** 한 연맹의 지휘관 수(대표 포함). 앱에서만 세는 한도다. */
    const val MAX_COMMANDERS = 5
    const val MAX_NOTE = 60

    fun cleanNote(raw: String?): String = raw.orEmpty().trim().replace(Regex("\\s+"), " ").take(MAX_NOTE).trim()

    /** 신청으로 서버에 쓸 내용. 캐릭터명이 비면 null. */
    fun encodeRequest(group: RallyGroup, rawName: String, rawNote: String?, now: Long): Map<String, Any?>? {
        val name = AdminRoster.cleanName(rawName)
        if (name.isEmpty()) return null
        val out = linkedMapOf<String, Any?>("group" to group.id, "name" to name, "createdAt" to now, "status" to PENDING)
        cleanNote(rawNote).takeIf { it.isNotEmpty() }?.let { out["note"] = it }
        return out
    }

    fun decodeRequest(uid: String, raw: Any?): RepRequest? {
        val m = raw as? Map<*, *> ?: return null
        val group = (m["group"] as? String).orEmpty()
        if (RallyGroup.fromId(group) == null) return null
        return RepRequest(
            uid, group, AdminRoster.cleanName((m["name"] as? String).orEmpty()), cleanNote(m["note"] as? String),
            (m["createdAt"] as? Number)?.toLong() ?: 0L,
            status = if (m["status"] == REJECTED) REJECTED else PENDING,
            reason = cleanNote(m["reason"] as? String)
        )
    }

    /** 기다리는 신청만, 먼저 온 것부터. 거절된 것과 깨진 항목은 뺀다. */
    fun decodeRequests(map: Map<String, Any?>?): List<RepRequest> =
        map.orEmpty().mapNotNull { (uid, raw) -> if (AdminRoster.isValidUid(uid)) decodeRequest(uid, raw) else null }
            .filter { it.pending }
            .sortedWith(compareBy({ it.createdAt }, { it.uid }))

    /** 승인할 때 지휘관 명단에 적는 내용: 신청한 소속의 연맹 대표. */
    fun approveRecord(req: RepRequest, now: Long): Map<String, Any?> =
        linkedMapOf("registeredAt" to now, "name" to req.name, "group" to req.group, "rep" to true)

    /** 개발자의 신청 목록에서 한 줄 아래에 보일 주의. 없으면 null. 이미 대표가 있는 연맹이 먼저다. */
    fun requestWarning(req: RepRequest, all: List<RepRequest>, admins: List<AdminEntry>): String? {
        val rep = admins.firstOrNull { it.rep && it.group == req.group }
        if (rep != null) return "이미 대표가 있어요: ${rep.name.ifEmpty { "이름 없음" }}"
        val same = all.count { it.pending && it.group == req.group }
        return if (same > 1) "같은 연맹에서 신청이 ${same}건이에요. 한 명만 대표가 돼요." else null
    }

    /** 연맹 대표가 지휘관 코드를 더 만들어도 되는지: 지금 지휘관(대표 포함)과 대기 중인 코드를 합쳐 [MAX_COMMANDERS] 미만일 때. */
    fun canAddCommander(commanders: Int, pendingCodes: Int): Boolean = commanders + pendingCodes < MAX_COMMANDERS

    /** 화면에 보일 소속. 모양이 틀리거나 비었으면 "소속 없음". */
    fun groupLabel(groupId: String): String = RallyGroup.fromId(groupId)?.label ?: "소속 없음"
}
