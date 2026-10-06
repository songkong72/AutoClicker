package com.sejun.autoclicker

import java.security.SecureRandom
import java.util.Random

/**
 * 서버 관리자 명단(admins/{uid})의 한 사람. [code]는 등록에 쓴 관리자 코드(개발자가 직접 넣은 경우 빈 문자열).
 * [name]은 개발자가 붙인 이름표, [lastSeen]은 마지막으로 앱을 연 시각(ms), [appVersion]은 그때의 앱 버전. 없으면 기본값이다.
 */
internal data class AdminEntry(
    val uid: String,
    val code: String,
    val registeredAt: Long,
    val name: String = "",
    val lastSeen: Long = 0L,
    val appVersion: String = ""
)

/** 관리자 코드를 쓸 수 있는 기간. 만들 때 고른다. */
internal enum class CodeTtl(val label: String, val ms: Long) {
    HOUR("1시간", 60L * 60 * 1000),
    DAY("24시간", 24L * 60 * 60 * 1000),
    WEEK("7일", 7L * 24 * 60 * 60 * 1000)
}

/** 개발자가 발급한 관리자 코드(adminCodes/{코드}). [name]은 개발자가 붙인 이름표다. */
internal data class AdminCode(val code: String, val name: String, val createdAt: Long, val expiresAt: Long, val used: Boolean)

/**
 * 관리자 코드와 명단 처리. 안드로이드 클래스를 쓰지 않아 단위 테스트가 된다.
 * 코드는 "AD-" + 헷갈리는 글자(0, O, 1, I)를 뺀 8자이고, 만든 지 24시간이 지나면 못 쓴다.
 */
internal object AdminRoster {
    const val CODE_TTL_MS = 24L * 60 * 60 * 1000
    const val MAX_NAME = 20
    const val MAX_BATCH = 10
    private const val MAX_VERSION = 20
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

    /** 관리자 코드를 넣으려는 것으로 보이나. 비밀번호와 집결장 초대코드(AC-)를 가르는 데 쓴다. */
    fun looksLikeCode(raw: String): Boolean = raw.trim().uppercase().startsWith("AD-")

    fun isExpired(nowMs: Long, createdAt: Long): Boolean = nowMs - createdAt >= CODE_TTL_MS

    fun cleanName(raw: String): String = raw.trim().replace(Regex("\\s+"), " ").take(MAX_NAME).trim()

    fun isValidUid(uid: String): Boolean = UID.matches(uid)

    /** 서버 명단을 등록 순서대로. 깨진 항목은 건너뛴다. */
    fun decodeAdmins(map: Map<String, Any?>?): List<AdminEntry> =
        map.orEmpty().mapNotNull { (uid, raw) ->
            val m = raw as? Map<*, *> ?: return@mapNotNull null
            if (!isValidUid(uid)) return@mapNotNull null
            AdminEntry(
                uid, (m["code"] as? String).orEmpty(), (m["registeredAt"] as? Number)?.toLong() ?: 0L,
                name = cleanName((m["name"] as? String).orEmpty()),
                lastSeen = (m["lastSeen"] as? Number)?.toLong() ?: 0L,
                appVersion = (m["appVersion"] as? String).orEmpty().trim().take(MAX_VERSION)
            )
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
    fun encodeCode(rawName: String, createdAt: Long, ttlMs: Long = CODE_TTL_MS): Map<String, Any?>? {
        val name = cleanName(rawName)
        return if (name.isEmpty()) null else mapOf("name" to name, "createdAt" to createdAt, "expiresAt" to createdAt + ttlMs)
    }

    /** 여러 이름을 줄바꿈이나 쉼표로 적은 입력을 이름 목록으로. 비거나 겹친 이름은 빼고 [MAX_BATCH]명까지만 쓴다. */
    fun parseNames(raw: String): List<String> =
        raw.split('\n', ',').map { cleanName(it) }.filter { it.isNotEmpty() }.distinct().take(MAX_BATCH)

    /** 카카오톡으로 보낼 안내 문구 한 사람 몫. */
    fun shareMessage(code: String, ttl: CodeTtl): String =
        "[AutoClicker Pro 관리자 초대]\n관리자 코드: $code\n앱의 인증 화면 → 관리자 로그인에서 이 코드를 입력하세요. 만든 지 ${ttl.label} 안에 한 번만 쓸 수 있어요."

    /** (이름, 코드) 여러 쌍을 사람별 안내 문구로 이어 붙인다. */
    fun batchShare(items: List<Pair<String, String>>, ttl: CodeTtl): String =
        items.joinToString("\n\n────────\n\n") { (name, code) -> "${name}님께\n" + shareMessage(code, ttl) }

    /** 목록에 보일 이름: 저장된 이름표, 없으면 등록에 쓴 코드의 이름표, 그것도 없으면 "직접 등록". */
    fun labelOf(a: AdminEntry, codes: List<AdminCode>): String =
        a.name.takeIf { it.isNotEmpty() } ?: codes.firstOrNull { it.code == a.code }?.name?.takeIf { it.isNotEmpty() } ?: "직접 등록"

    /** 이름이 아직 저장되지 않은 관리자에게, 등록에 쓴 코드의 이름표를 복사할 목록(uid → 이름). 코드를 정리하기 전에 쓴다. */
    fun nameBackfill(admins: List<AdminEntry>, codes: List<AdminCode>): Map<String, String> {
        val labels = codes.associate { it.code to it.name }
        return admins.mapNotNull { a ->
            if (a.name.isNotEmpty()) null else labels[a.code]?.takeIf { it.isNotEmpty() }?.let { a.uid to it }
        }.toMap()
    }

    /** 서버에서 지워도 되는 코드: 이미 쓰였거나 기한이 지난 것. 모양이 이상한 키는 건드리지 않는다. */
    fun purgeable(rawCodes: Map<String, Any?>?, nowMs: Long): List<String> =
        rawCodes.orEmpty().mapNotNull { (code, raw) ->
            val m = raw as? Map<*, *> ?: return@mapNotNull null
            if (normalizeCode(code) != code) return@mapNotNull null
            val used = m["usedBy"] != null
            val expiresAt = (m["expiresAt"] as? Number)?.toLong()
            if (used || (expiresAt != null && nowMs >= expiresAt)) code else null
        }

    /** "5분 전 · v1.0.57" 같은 접속 안내. 접속 기록이나 버전이 없으면 그렇게 적는다. */
    fun activityText(lastSeen: Long, appVersion: String, nowMs: Long): String {
        val seen = if (lastSeen <= 0L) "접속 기록 없음" else {
            val diff = (nowMs - lastSeen).coerceAtLeast(0L)
            when {
                diff < 60_000L -> "방금 전"
                diff < 3_600_000L -> "${diff / 60_000L}분 전"
                diff < 86_400_000L -> "${diff / 3_600_000L}시간 전"
                else -> "${diff / 86_400_000L}일 전"
            }
        }
        val ver = if (appVersion.isBlank()) "버전 알 수 없음" else "v$appVersion"
        return "$seen · $ver"
    }

    /** 코드를 쓴 뒤 서버 명단에 적는 내 항목. 코드를 먼저 내 것으로 잡은(usedBy) 다음에 쓴다. */
    fun adminRecord(code: String, registeredAt: Long): Map<String, Any?> =
        mapOf("code" to code, "registeredAt" to registeredAt)

    /** 관리자 목록 전체를 메모에 붙여 넣기 좋은 글로. 이름표는 쓴 코드에서 찾고, 만료된 코드는 뺀다. */
    fun exportText(admins: List<AdminEntry>, codes: List<AdminCode>, nowMs: Long): String {
        val pending = codes.filter { !it.used && nowMs < it.expiresAt }
        return buildString {
            append("등록된 관리자 ${admins.size}명\n")
            admins.forEachIndexed { i, a ->
                append("${i + 1}. ${labelOf(a, codes)} · ID …${a.uid.takeLast(6)}\n")
            }
            append("\n대기 중인 코드 ${pending.size}개\n")
            pending.forEach { append("${it.code} · ${it.name}\n") }
        }.trimEnd()
    }

    /**
     * 앱을 열 때 이 기기의 관리자 모드를 서버 답과 맞춘다. 비밀번호 로그인이 있던 시절 기기(viaServer=false)도 여기서 정리한다.
     * 서버가 개발자나 관리자라고 하면 유지(옛 기기는 서버 방식으로 표시), 둘 다 아니라고 분명히 답할 때만 푼다.
     * 모르겠으면(네트워크 오류, 규칙 미적용) 건드리지 않는다.
     */
    /**
     * 코드 없이 관리자로 들어가도 되는지. 서버가 개발자나 관리자라고 답하면 YES, 둘 다 아니라고 분명히 답하면 NO.
     * 확인하지 못했으면(네트워크 오류 등) UNKNOWN이고, 그때는 들여보내지 않는다.
     */
    fun rosterEntry(owner: Check, admin: Check): Check = when {
        owner == Check.YES || admin == Check.YES -> Check.YES
        owner == Check.NO && admin == Check.NO -> Check.NO
        else -> Check.UNKNOWN
    }

    fun reconcile(viaServer: Boolean, owner: Check, admin: Check): AdminModeFix = when {
        owner == Check.YES || admin == Check.YES -> if (viaServer) AdminModeFix.KEEP else AdminModeFix.MARK_SERVER
        owner == Check.NO && admin == Check.NO -> AdminModeFix.CLEAR
        else -> AdminModeFix.KEEP
    }
}

internal enum class AdminModeFix { KEEP, MARK_SERVER, CLEAR }
