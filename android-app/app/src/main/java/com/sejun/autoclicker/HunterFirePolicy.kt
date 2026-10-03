package com.sejun.autoclicker

/** 곰 사냥 수동 발사를 허용할지. 저장된 위치가 있고, 누름 반복이 아니며, 직전 발사에서 쿨타임이 지났을 때만. */
object HunterFirePolicy {
    const val COOLDOWN_MS = 1500L

    fun allow(hasTargets: Boolean, repeatCount: Int, nowMs: Long, lastFireMs: Long): Boolean =
        hasTargets && repeatCount == 0 && nowMs - lastFireMs >= COOLDOWN_MS
}
