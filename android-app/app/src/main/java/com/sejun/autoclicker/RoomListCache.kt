package com.sejun.autoclicker

import android.content.SharedPreferences

/** 마지막으로 서버에서 받은 방 목록(번호, 설명)을 기기에 남겨, 방 선택 창이 열리자마자 보여 주게 한다. */
object RoomListCache {
    private const val KEY = "cloud_room_list_cache"
    private const val SEP = "\t"

    fun encode(rooms: List<Pair<String, String>>): String =
        rooms.joinToString("\n") { (c, l) -> c.replace(Regex("[\t\n]"), " ") + SEP + l.replace(Regex("[\t\n]"), " ") }

    fun decode(raw: String?): List<Pair<String, String>> =
        (raw ?: "").split("\n").mapNotNull { line ->
            val i = line.indexOf(SEP)
            if (i <= 0) null else line.substring(0, i) to line.substring(i + 1)
        }

    fun load(prefs: SharedPreferences): List<Pair<String, String>> = decode(prefs.getString(KEY, null))

    fun save(prefs: SharedPreferences, rooms: List<Pair<String, String>>) {
        prefs.edit().putString(KEY, encode(rooms)).apply()
    }
}
