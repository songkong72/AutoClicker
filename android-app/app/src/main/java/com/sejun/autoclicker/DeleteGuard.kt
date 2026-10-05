package com.sejun.autoclicker

/**
 * 군단 삭제(✕)를 한 번 잘못 눌러 바로 지워지는 실수를 막는다.
 * 첫 탭은 그 줄을 "삭제?"로 바꾸기만 하고, [windowMs] 안에 같은 줄을 한 번 더 눌러야 지운다.
 */
internal class DeleteGuard(private val windowMs: Long = 3_000L) {
    private var armedId: String? = null
    private var armedAt = -1L

    /** 지워도 되면 true. false면 그 줄이 "삭제?" 확인 상태가 된다. */
    fun onTap(id: String, nowMs: Long): Boolean {
        if (isArmed(id, nowMs)) { armedId = null; return true }
        armedId = id
        armedAt = nowMs
        return false
    }

    /** 이 줄이 지금 "삭제?" 확인을 기다리는 중인지. */
    fun isArmed(id: String, nowMs: Long): Boolean = armedId == id && nowMs - armedAt <= windowMs

    fun reset() { armedId = null }
}
