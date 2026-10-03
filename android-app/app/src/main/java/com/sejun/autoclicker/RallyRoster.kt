package com.sejun.autoclicker

/** 방에 들어온 사람 한 명. [name]은 게임 캐릭터명이다. */
data class RallyMember(val id: String, val name: String)

/**
 * 방 명단(rallyMembers/{방}/{기기ID}) 처리. 방 문서와 따로 저장해서, 관리자가 방 문서를 통째로 쓸 때 명단이 지워지지 않는다.
 * org.json 대신 Map을 써서 JVM 단위 테스트가 가능하다.
 */
object RallyRoster {
    const val MAX_NAME = 20
    private val ID_PATTERN = Regex("^[A-Za-z0-9_-]{1,40}$")

    private const val ID_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789"

    /** 이 기기의 새 ID. 처음 쓸 때 한 번 만들어 저장한다. */
    fun newMemberId(random: java.util.Random = java.security.SecureRandom()): String =
        "m-" + (1..10).map { ID_CHARS[random.nextInt(ID_CHARS.length)] }.joinToString("")

    /** 앞뒤 공백을 지우고 연속 공백을 하나로 줄이고 20자로 자른다. 비면 빈 문자열. */
    fun cleanName(raw: String): String = raw.trim().replace(Regex("\\s+"), " ").take(MAX_NAME).trim()

    /** URL 경로에 그대로 넣어도 안전한 ID인가. */
    fun isValidMemberId(id: String): Boolean = ID_PATTERN.matches(id)

    /** 방에 쓸 내 명단 항목. ID나 이름이 쓸 수 없는 값이면 null. */
    fun encode(id: String, rawName: String): Map<String, Any?>? {
        val name = cleanName(rawName)
        return if (name.isEmpty() || !isValidMemberId(id)) null else mapOf("name" to name)
    }

    /** 서버 명단을 이름순 목록으로. 깨진 항목과 이름 없는 항목은 건너뛴다. */
    fun decode(map: Map<String, Any?>?): List<RallyMember> =
        map.orEmpty().mapNotNull { (id, raw) ->
            val name = cleanName(((raw as? Map<*, *>)?.get("name") as? String).orEmpty())
            if (name.isEmpty() || !isValidMemberId(id)) null else RallyMember(id, name)
        }.sortedWith(compareBy({ it.name }, { it.id }))
}
