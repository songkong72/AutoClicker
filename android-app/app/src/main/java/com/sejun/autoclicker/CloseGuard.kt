package com.sejun.autoclicker

/**
 * 집결이 진행 중일 때 ✕를 한 번만 눌러 패널이 사라지는 실수를 막는다.
 * 진행 중이면 첫 탭은 경고만 하고, [windowMs] 안에 한 번 더 누르면 닫는다. 진행 중이 아니면 바로 닫는다.
 */
internal class CloseGuard(private val windowMs: Long = 3_000L) {
    private var armedAt = -1L

    /** 닫아도 되면 true. false면 "한 번 더 누르세요" 안내를 보여 준다. */
    fun onTap(running: Boolean, nowMs: Long): Boolean {
        if (!running) { armedAt = -1L; return true }
        if (armedAt >= 0L && nowMs - armedAt <= windowMs) { armedAt = -1L; return true }
        armedAt = nowMs
        return false
    }
}
