package com.sejun.autoclicker

import android.content.SharedPreferences

/**
 * 이 기기가 들어갔던 방 번호 기록(최근 순). 패널에서 방을 바꿀 때 번호를 다시 치지 않고 고르게 한다.
 * 서버의 방 목록은 개발자만 읽을 수 있어서, 누구나 쓸 수 있게 기기에 남긴 기록을 쓴다.
 */
object RallyRoomHistory {
    const val MAX = 8
    private const val KEY = "cloud_room_history"

    /** [code]를 맨 앞에 두고 같은 방은 한 번만, 최근 [MAX]개까지. 형식이 맞지 않는 번호는 무시한다. */
    fun add(list: List<String>, code: String): List<String> {
        val c = RallyRoomCode.normalize(code) ?: return list
        return (listOf(c) + list.filter { it != c }).take(MAX)
    }

    fun encode(list: List<String>): String = list.joinToString(",")

    fun decode(raw: String?): List<String> =
        (raw ?: "").split(",").mapNotNull { RallyRoomCode.normalize(it) }.distinct().take(MAX)

    fun load(prefs: SharedPreferences): List<String> = decode(prefs.getString(KEY, null))

    fun record(prefs: SharedPreferences, code: String) {
        prefs.edit().putString(KEY, encode(add(load(prefs), code))).apply()
    }
}
