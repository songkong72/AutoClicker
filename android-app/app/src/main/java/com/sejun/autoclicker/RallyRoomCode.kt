package com.sejun.autoclicker

/** 집결 방 번호 검사. 서버 경로에 그대로 쓰이므로 영문·숫자·-·_ 만, 4~20자로 제한한다. */
object RallyRoomCode {
    private val OK = Regex("^[A-Za-z0-9_-]{4,20}$")

    /** 앞뒤 공백을 지운 방 번호. 형식이 맞지 않으면 null. */
    fun normalize(raw: String): String? = raw.trim().takeIf { OK.matches(it) }
}
