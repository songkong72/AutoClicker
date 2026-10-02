package com.sejun.autoclicker

/** 숨은 기능 개방용 연타 감지. 간격이 [gapMs] 이내인 탭이 [needed]번 이어지면 true를 한 번 돌려주고 처음부터 센다. */
class RallySecretTap(private val needed: Int = 5, private val gapMs: Long = 800L) {
    private var count = 0
    private var last = Long.MIN_VALUE

    fun tap(nowMs: Long): Boolean {
        count = if (last != Long.MIN_VALUE && nowMs - last < gapMs) count + 1 else 1
        last = nowMs
        if (count >= needed) { count = 0; return true }
        return false
    }
}
