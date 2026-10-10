package com.sejun.autoclicker

import android.content.SharedPreferences

/**
 * 소속: 게임 서버 번호와 연맹. 방은 소속 아래에 둔다(다른 연맹이 같은 방 번호를 써도 겹치지 않는다).
 * 서버에는 방 이름을 "서버-연맹-방번호"(예: 2000-WBI-1111)로 저장하고, 화면에는 방 번호만 보인다.
 * 연맹은 대문자와 소문자를 구분한다(WBI와 wbi는 다른 소속). 적은 글자를 바꾸지 않는다.
 */
data class RallyGroup(val server: String, val alliance: String) {
    /** 이 소속의 방들이 서버에서 갖는 이름의 앞부분. 연맹에는 "-"를 못 쓰게 해서 다른 소속과 섞이지 않는다. */
    val prefix: String get() = "$server-$alliance-"

    /** 서버에 저장되는 방 이름. */
    fun key(room: String): String = prefix + room

    /** 화면의 짧은 표시. 예: "2000 · WBI" */
    val label: String get() = "$server · $alliance"

    /** 보내는 글에 넣는 한 줄. 예: "소속: 2000 서버 · WBI" */
    val shareLine: String get() = "소속: $server 서버 · $alliance"

    companion object {
        const val MAX_SERVER = 6
        const val MAX_ALLIANCE = 8

        /** 입력이 맞으면 소속을, 아니면 null을 돌려준다. 서버는 숫자만(앞의 0은 뗀다), 연맹은 글자·숫자만. */
        fun of(serverRaw: String?, allianceRaw: String?): RallyGroup? =
            if (problem(serverRaw, allianceRaw) != null) null
            else RallyGroup(serverRaw.orEmpty().trim().trimStart('0').ifEmpty { "0" }, allianceRaw.orEmpty().trim())

        /** 입력이 틀렸으면 화면에 보여 줄 이유, 맞으면 null. */
        fun problem(serverRaw: String?, allianceRaw: String?): String? {
            val s = serverRaw.orEmpty().trim()
            val a = allianceRaw.orEmpty().trim()
            return when {
                s.isEmpty() -> "서버 번호를 적어 주세요."
                s.length > MAX_SERVER || !s.all { it in '0'..'9' } -> "서버 번호는 숫자만 ${MAX_SERVER}자리까지 적어 주세요."
                a.isEmpty() -> "연맹을 적어 주세요."
                a.length > MAX_ALLIANCE -> "연맹은 ${MAX_ALLIANCE}자까지 적어 주세요."
                !a.all { it.isLetterOrDigit() } -> "연맹은 글자와 숫자만 적어 주세요. (띄어쓰기·기호 없이)"
                else -> null
            }
        }

        /** 서버에 쓸 방 이름. 소속이 없으면(옛 방식) 방 번호 그대로. */
        fun keyFor(group: RallyGroup?, room: String): String = group?.key(room) ?: room

        /**
         * 서버에서 받은 방 전체([all], 이름 → 내용) 중 이 소속의 방만 골라 방 번호 → 내용으로 바꾼다.
         * 소속이 없으면 그대로 돌려준다(개발자의 방 전체 목록).
         */
        fun scope(all: Map<String, Any?>?, group: RallyGroup?): Map<String, Any?>? {
            if (all == null || group == null) return all
            val out = LinkedHashMap<String, Any?>()
            for ((name, value) in all) {
                if (!name.startsWith(group.prefix)) continue
                val room = RallyRoomCode.normalize(name.substring(group.prefix.length)) ?: continue
                out[room] = value
            }
            return out
        }

        private const val KEY_SERVER = "rally_group_server"
        private const val KEY_ALLIANCE = "rally_group_alliance"

        /** 이 폰에 정해 둔 소속. 아직 없으면 null. */
        fun load(prefs: SharedPreferences): RallyGroup? =
            of(prefs.getString(KEY_SERVER, null), prefs.getString(KEY_ALLIANCE, null))

        fun save(prefs: SharedPreferences, group: RallyGroup) {
            prefs.edit().putString(KEY_SERVER, group.server).putString(KEY_ALLIANCE, group.alliance).apply()
        }
    }
}
