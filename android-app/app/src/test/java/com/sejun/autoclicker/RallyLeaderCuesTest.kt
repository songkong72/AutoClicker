package com.sejun.autoclicker

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RallyLeaderCuesTest {
    // ---- 2. 내 클릭 임박 ----
    private fun tracker() = RallyCountdownCue()

    @Test fun ticksOncePerSecondInLastFiveSeconds() {
        val t = tracker()
        assertNull(t.onCountdown(true, 12.0))
        assertNull(t.onCountdown(true, 5.9))     // 아직 6초대
        assertEquals(5, t.onCountdown(true, 5.0))
        assertNull(t.onCountdown(true, 4.8))     // 같은 5초 구간
        assertEquals(4, t.onCountdown(true, 4.0))
        assertEquals(1, t.onCountdown(true, 0.9))
    }

    @Test fun neverTicksWhenNotWaitingForMyClick() {
        val t = tracker()
        assertNull(t.onCountdown(false, 3.0))
    }

    @Test fun resetsWhenCountdownRestarts() {
        val t = tracker()
        t.onCountdown(true, 2.0)
        assertNull(t.onCountdown(true, 20.0))
        assertEquals(5, t.onCountdown(true, 4.6))
    }

    @Test fun urgentOnlyInLastFiveSecondsBeforeMyClick() {
        assertTrue(RallyCountdownCue.urgent(true, 4.9))
        assertTrue(RallyCountdownCue.urgent(true, 0.2))
        assertFalse(RallyCountdownCue.urgent(true, 5.5))
        assertFalse(RallyCountdownCue.urgent(false, 2.0))
        assertFalse(RallyCountdownCue.urgent(true, null))
    }

    // ---- 3. 클릭 완료 ----
    @Test fun clickNoteShowsMillisecondClock() {
        val utc = TimeZone.getTimeZone("UTC")
        assertEquals("", RallyClickNote.text(null, utc))
        assertEquals("✓ 클릭함 12:34:12.345", RallyClickNote.text(1_790_944_452_345L, utc))
    }

    // ---- 4. 연결 상태 ----
    @Test fun connectionStates() {
        assertEquals(RallyConnection.LIVE, RallyConnection.of(streaming = true, online = true))
        assertEquals(RallyConnection.POLLING, RallyConnection.of(streaming = false, online = true))
        assertEquals(RallyConnection.OFFLINE, RallyConnection.of(streaming = false, online = false))
    }

    // ---- 5. 대기 중 한 줄 ----
    @Test fun idleSubLabelShowsClickTimeAndTeamCount() {
        assertEquals("시작 후 15초에 내 집결 클릭 · 참여 군단 2개",
            RallyScreenModel.idleSub("시작 후 15초에 내 집결 클릭", 2))
    }
}
