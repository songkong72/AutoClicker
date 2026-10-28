package com.sejun.autoclicker

/** 곰 사냥 수동 발사를 허용할지. 저장된 위치가 있고, 누름 반복이 아니며, 직전 발사에서 쿨타임이 지났을 때만. */
object HunterFirePolicy {
    /**
     * 연달아 누를 수 있는 간격. 부대·출정 두 번 클릭(약 0.25초)이 끝나기 전에 다음 발사가 겹치지 않을 만큼만 막는다.
     * 예전 1.5초는 "참석 → 🐻 → 다음 참석 → 🐻"를 빠르게 하면 두 번째 발사를 말없이 버려서 참석이 안 됐다.
     */
    const val COOLDOWN_MS = 400L

    fun allow(hasTargets: Boolean, repeatCount: Int, nowMs: Long, lastFireMs: Long): Boolean =
        hasTargets && repeatCount == 0 && nowMs - lastFireMs >= COOLDOWN_MS
}
