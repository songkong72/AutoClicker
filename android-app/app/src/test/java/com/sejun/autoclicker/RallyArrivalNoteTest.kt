package com.sejun.autoclicker

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class RallyArrivalNoteTest {
    private val utc = TimeZone.getTimeZone("UTC")
    // 2026-10-02 12:34:12 UTC
    private val arrive = 1_790_944_452_000L

    @Test fun noRunNoNote() = assertEquals("", RallyArrivalNote.text(null, arrive, utc))

    @Test fun beforeArrivalShowsExpected() =
        assertEquals("도착 예정 12:34:12", RallyArrivalNote.text(arrive, arrive - 1, utc))

    @Test fun atAndAfterArrivalShowsArrived() {
        assertEquals("12:34:12 도착", RallyArrivalNote.text(arrive, arrive, utc))
        assertEquals("12:34:12 도착", RallyArrivalNote.text(arrive, arrive + 600_000, utc))
    }

    @Test fun wallClockOfArrival() {
        // 시작 시각 + 준비 15 + 최대 행군 30 + 대기 300 = 345초 뒤
        assertEquals(1_000_000L + 345_000L, RallyArrivalNote.arriveAtMs(1_000_000L, 345.0))
    }
}
