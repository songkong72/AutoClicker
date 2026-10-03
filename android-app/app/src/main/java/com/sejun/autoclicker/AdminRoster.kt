package com.sejun.autoclicker

import java.security.SecureRandom
import java.util.Random

/** 서버 관리자 명단(admins/{uid})의 한 사람. [code]는 등록에 쓴 관리자 코드(개발자가 직접 넣은 경우 빈 문자열). */
internal data class AdminEntry(val uid: String, val code: String, val registeredAt: Long)

/** 개발자가 발급한 관리자 코드(adminCodes/{코드}). [name]은 개발자가 붙인 이름표다. */
internal data class AdminCode(val code: String, val name: String, val createdAt: Long, val expiresAt: Long, val used: Boolean)

/**
 * 관리자 코드와 명단 처리. 안드로이드 클래스를 쓰지 않아 단위 테스트가 된다.
 * 코드는 "AD-" + 헷갈리는 글자(0, O, 1, I)를 뺀 8자이고, 만든 지 24시간이 지나면 못 쓴다.
 */
internal object AdminRoster {
    const val CODE_TTL_MS = 24L * 60 * 60 * 1000
    const val MAX_NAME = 20
    private const val POOL = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val CODE_BODY = Regex("^[2-9A-HJ-NP-Z]{8}$")
    private val UID = Regex("^[A-Za-z0-9]{20,40}$")

    fun newCode(random: Random = SecureRandom()): String =
        "AD-" + (1..8).map { POOL[random.nextInt(POOL.length)] }.joinToString("")

    /** 입력을 "AD-XXXXXXXX" 모양으로 맞춘다. 모양이 틀리면 null. 대소문자·공백·하이픈 유무는 봐준다. */
    fun normalizeCode(raw: String): String? {
        val s = raw.trim().uppercase().replace("-", "")
        if (!s.startsWith("AD")) return null
        val body = s.substring(2)
        return if (CODE_BODY.matches(body)) "AD-$body" else null
    }

    /** 관리자 코드를 넣으려는 것으로 보이나. 비밀번호와 팀장 초대코드(AC-)를 가르는 데 쓴다. */
    fun looksLikeCode(raw: String): Boolean = raw.trim().uppercase().startsWith("AD-")

    fun isExpired(nowMs: Long, createdAt: Long): Boolean = nowMs - createdAt >= CODE_TTL_MS

    fun cleanName(raw: String): String = raw.trim().replace(Regex("\\s+"), " ").take(MAX_NAME).trim()

    fun isValidUid(uid: String): Boolean = UID.matches(uid)

    /** 서버 명단을 등록 순서대로. 깨진 항목은 건너뛴다. */
    fun decodeAdmins(map: Map<String, Any?>?): List<AdminEntry> =
        map.orEmpty().mapNotNull { (uid, raw) ->
            val m = raw as? Map<*, *> ?: return@mapNotNull null
            if (!isValidUid(uid)) return@mapNotNull null
            AdminEntry(uid, (m["code"] as? String).orEmpty(), (m["registeredAt"] as? Number)?.toLong() ?: 0L)
        }.sortedWith(compareBy({ it.registeredAt }, { it.uid }))

    /** 코드 목록을 최근 순으로. 기한이 지났는데 쓰이지도 않은 코드는 뺀다(쓰인 코드는 관리자 이름표를 찾는 데 남긴다). */
    fun decodeCodes(map: Map<String, Any?>?, nowMs: Long): List<AdminCode> =
        map.orEmpty().mapNotNull { (code, raw) ->
            val m = raw as? Map<*, *> ?: return@mapNotNull null
            if (normalizeCode(code) != code) return@mapNotNull null
            val expiresAt = (m["expiresAt"] as? Number)?.toLong() ?: return@mapNotNull null
            val used = m["usedBy"] != null
            if (nowMs >= expiresAt && !used) return@mapNotNull null
            AdminCode(code, (m["name"] as? String).orEmpty(), (m["createdAt"] as? Number)?.toLong() ?: 0L, expiresAt, used)
        }.sortedByDescending { it.createdAt }

    /** 새 코드로 서버에 쓸 내용. 이름이 비면 null. */
    fun encodeCode(rawName: String, createdAt: Long): Map<String, Any?>? {
        val name = cleanName(rawName)
        return if (name.isEmpty()) null else mapOf("name" to name, "createdAt" to createdAt, "expiresAt" to createdAt + CODE_TTL_MS)
    }

    /** 코드를 쓴 뒤 서버 명단에 적는 내 항목. 코드를 먼저 내 것으로 잡은(usedBy) 다음에 쓴다. */
    fun adminRecord(code: String, registeredAt: Long): Map<String, Any?> =
        mapOf("code" to code, "registeredAt" to registeredAt)
}
