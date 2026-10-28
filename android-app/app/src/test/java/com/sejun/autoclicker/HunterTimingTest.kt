package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HunterTimingTest {
    @Test fun `처음 값은 쉬는 시간 0점05초, 부대에서 출정 0점15초`() {
        val t = HunterTiming()
        assertEquals(50L, t.readyMs)
        assertEquals(150L, t.gapMs)
    }

    @Test fun `0점05초씩 맞추고 0초에서 1초 사이로 묶는다`() {
        assertEquals(0L, HunterTiming.clampReady(-50))
        assertEquals(1000L, HunterTiming.clampReady(1200))
        assertEquals(100L, HunterTiming.clampReady(110))
        // 부대 → 출정은 앞 클릭과 겹치지 않게 0.05초보다 짧게 두지 않는다
        assertEquals(50L, HunterTiming.clampGap(0))
        assertEquals(1000L, HunterTiming.clampGap(1050))
    }

    @Test fun `화면 값은 초로`() {
        assertEquals("0.05", HunterTiming.label(50))
        assertEquals("0.15", HunterTiming.label(150))
        assertEquals("1", HunterTiming.label(1000))
        assertEquals("0", HunterTiming.label(0))
        assertEquals("0.05초", StrKo.sec(HunterTiming.label(50)))
    }

    @Test fun `시간을 길게 잡으면 그만큼 다음 발사를 더 오래 막는다`() {
        val long = HunterTiming(800, 600)
        assertFalse(HunterFirePolicy.allow(true, 0, nowMs = 10_000 + 1_000, lastFireMs = 10_000, busyMs = long.busyMs))
        assertTrue(HunterFirePolicy.allow(true, 0, nowMs = 10_000 + long.busyMs + 60, lastFireMs = 10_000, busyMs = long.busyMs))
        // 짧으면 기본 0.4초
        assertFalse(HunterFirePolicy.allow(true, 0, nowMs = 10_000 + 399, lastFireMs = 10_000, busyMs = HunterTiming().busyMs))
    }
}
