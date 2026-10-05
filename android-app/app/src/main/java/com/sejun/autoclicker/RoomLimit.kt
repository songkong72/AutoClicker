package com.sejun.autoclicker

/** 서버에 만들 수 있는 방 수의 상한. 이미 있는 방에 들어가는 것은 세지 않는다. */
object RoomLimit {
    const val MAX_ROOMS = 10

    enum class Verdict { ALLOW, FULL, UNKNOWN }

    /** [existing]은 서버에 있는 방 번호들(읽지 못했으면 null). 새 번호를 만들 때만 상한을 본다. */
    fun decide(existing: Set<String>?, code: String): Verdict = when {
        existing == null -> Verdict.UNKNOWN
        code in existing -> Verdict.ALLOW
        existing.size >= MAX_ROOMS -> Verdict.FULL
        else -> Verdict.ALLOW
    }

    fun fullMessage(): String = "방은 최대 ${MAX_ROOMS}개까지 만들 수 있어요. 안 쓰는 방을 먼저 삭제해 주세요."
}
