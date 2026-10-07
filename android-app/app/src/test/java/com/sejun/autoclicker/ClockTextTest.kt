package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class ClockTextTest {
    // 2026-10-07 12:33:05 UTC
    private val ms = 1791376385000L
    private val seoul = TimeZone.getTimeZone("Asia/Seoul")

    @Test fun localTimeUsesGivenZone() = assertEquals("21:33:05", ClockText.format(ms, seoul, utc = false))
    @Test fun utcTimeIsLabelled() = assertEquals("UTC 12:33:05", ClockText.format(ms, seoul, utc = true))
    @Test fun delayReachesNextSecondBoundary() {
        assertEquals(1000L, ClockText.delayToNextSecond(5000L))
        assertEquals(750L, ClockText.delayToNextSecond(5250L))
        assertEquals(1L, ClockText.delayToNextSecond(5999L))
    }
}
