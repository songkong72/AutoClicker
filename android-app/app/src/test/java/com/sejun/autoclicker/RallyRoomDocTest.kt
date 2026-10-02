package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RallyRoomDocTest {
    private val doc = RallyRoomDoc(
        teams = listOf(
            RallyTeamDoc("t1", "1군", "윈터", 10.0),
            RallyTeamDoc("t2", "2군", "쿼드", 30.5, excluded = true)
        ),
        prepSec = 15.0, waitSec = 300.0, run = "RUNNING", startSeq = 3L
    )

    @Test fun roundTripKeepsEverything() {
        assertEquals(doc, RallyRoomCodec.decode(RallyRoomCodec.encode(doc)))
    }

    @Test fun decodeAcceptsIntegerNumbersFromJson() {
        // JSON 파서는 30.0을 Int 30으로 돌려줄 수 있다
        val m = RallyRoomCodec.encode(doc).toMutableMap()
        m["prepSec"] = 15
        m["startSeq"] = 3
        val d = RallyRoomCodec.decode(m)
        assertEquals(15.0, d.prepSec, 0.0)
        assertEquals(3L, d.startSeq)
    }

    @Test fun decodeNullGivesIdleEmptyRoom() {
        val d = RallyRoomCodec.decode(null)
        assertEquals("IDLE", d.run)
        assertTrue(d.teams.isEmpty())
        assertEquals(0L, d.startSeq)
    }

    @Test fun decodeToleratesMissingFieldsWithDefaults() {
        val d = RallyRoomCodec.decode(mapOf("teams" to listOf(mapOf("id" to "a", "marchSec" to 20))))
        assertEquals(1, d.teams.size)
        assertEquals("a", d.teams[0].id)
        assertEquals("", d.teams[0].leaderName)
        assertFalse(d.teams[0].excluded)
        assertEquals("IDLE", d.run)
    }

    @Test fun firebaseDropsEmptyListsSoDecodeHandlesMissingTeams() {
        val d = RallyRoomCodec.decode(mapOf("run" to "IDLE", "startSeq" to 0))
        assertTrue(d.teams.isEmpty())
    }

    @Test fun clickDelayIsClickTimeMinusElapsed() {
        assertEquals(20_000L, RallyClickTiming.delayUntilClickMs(35.0, 15_000L, 0L))
    }

    @Test fun positiveCorrectionDelaysAndNegativeAdvances() {
        assertEquals(20_080L, RallyClickTiming.delayUntilClickMs(35.0, 15_000L, 80L))
        assertEquals(19_920L, RallyClickTiming.delayUntilClickMs(35.0, 15_000L, -80L))
    }

    @Test fun pastClickTimeNeverGoesNegative() {
        assertEquals(0L, RallyClickTiming.delayUntilClickMs(15.0, 20_000L, 0L))
    }
}
