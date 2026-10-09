package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RallyChangeNoteTest {
    private val base = RallyRoomDoc(listOf(RallyTeamDoc("t1", "1군", marchSec = 10.0)), 15.0, 300.0, "IDLE", 0L)

    @Test fun labelUsesCharacterNameWhenPresent() {
        assertEquals("윈터", RallyChangeNote.label(" 윈터 ", "abcdef123456"))
    }

    @Test fun labelFallsBackToDeviceIdTail() {
        assertEquals("지휘관 …3456", RallyChangeNote.label("", "abcdef123456"))
    }

    @Test fun stampRecordsWhoAndWhen() {
        val d = RallyChangeNote.stamp(base, "윈터", 1000L)
        assertEquals("윈터", d.lastBy)
        assertEquals(1000L, d.lastAt)
        assertEquals(base.teams, d.teams)
    }

    @Test fun codecRoundTripKeepsStamp() {
        val d = RallyChangeNote.stamp(base, "윈터", 1000L)
        assertEquals(d, RallyRoomCodec.decode(RallyRoomCodec.encode(d)))
    }

    @Test fun codecOmitsStampWhenNeverChanged() {
        val m = RallyRoomCodec.encode(base)
        assertFalse(m.containsKey("lastBy"))
        assertFalse(m.containsKey("lastAt"))
        assertEquals(0L, RallyRoomCodec.decode(m).lastAt)
    }

    @Test fun textIsEmptyWithoutStamp() {
        assertEquals("", RallyChangeNote.text(base, "윈터", 5000L))
    }

    @Test fun textSaysMeAndJustNow() {
        val d = RallyChangeNote.stamp(base, "윈터", 10_000L)
        assertEquals("마지막 변경: 나 · 방금", RallyChangeNote.text(d, "윈터", 20_000L))
    }

    @Test fun textShowsOtherAdminAndMinutes() {
        val d = RallyChangeNote.stamp(base, "쿼드", 0L).copy(lastAt = 1L)
        assertEquals("마지막 변경: 쿼드 · 3분 전", RallyChangeNote.text(d, "윈터", 1L + 3 * 60_000L))
    }

    @Test fun textShowsHoursAndDays() {
        val d = RallyChangeNote.stamp(base, "쿼드", 1L)
        assertEquals("마지막 변경: 쿼드 · 2시간 전", RallyChangeNote.text(d, "윈터", 1L + 2 * 3_600_000L))
        assertEquals("마지막 변경: 쿼드 · 3일 전", RallyChangeNote.text(d, "윈터", 1L + 3 * 86_400_000L))
    }

    @Test fun textHandlesClockBehindAsJustNow() {
        val d = RallyChangeNote.stamp(base, "쿼드", 50_000L)
        assertEquals("마지막 변경: 쿼드 · 방금", RallyChangeNote.text(d, "윈터", 10_000L))
    }

    @Test fun foreignChangeWhenAnotherAdminStampsNewer() {
        val d = RallyChangeNote.stamp(base, "쿼드", 2000L)
        assertTrue(RallyChangeNote.foreignChange(1000L, d, "윈터"))
    }

    @Test fun noForeignChangeForMyOwnStampOrOldOrMissing() {
        assertFalse(RallyChangeNote.foreignChange(1000L, RallyChangeNote.stamp(base, "윈터", 2000L), "윈터"))
        assertFalse(RallyChangeNote.foreignChange(3000L, RallyChangeNote.stamp(base, "쿼드", 2000L), "윈터"))
        assertFalse(RallyChangeNote.foreignChange(0L, base, "윈터"))
    }
}
