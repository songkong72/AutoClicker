package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Test

class HunterTroopsTest {
    @Test fun encodeThenDecodeKeepsOrderAndPositions() {
        val list = listOf(TroopPoint(100, 200), TroopPoint(300, 400), TroopPoint(5, 6))
        assertEquals(list, HunterTroops.decode(HunterTroops.encode(list)))
    }

    @Test fun decodeOfNullOrBlankIsEmpty() {
        assertEquals(emptyList<TroopPoint>(), HunterTroops.decode(null))
        assertEquals(emptyList<TroopPoint>(), HunterTroops.decode(""))
    }

    @Test fun decodeSkipsBrokenEntries() {
        assertEquals(listOf(TroopPoint(1, 2), TroopPoint(7, 8)), HunterTroops.decode("1,2;abc;3;7,8;,"))
    }

    @Test fun decodeKeepsOnlyTheFirstSeven() {
        val text = (1..9).joinToString(";") { "$it,$it" }
        assertEquals(7, HunterTroops.decode(text).size)
        assertEquals(TroopPoint(7, 7), HunterTroops.decode(text).last())
    }

    @Test fun nextMovesOneForward() = assertEquals(1, HunterTroops.next(0, 3))

    @Test fun nextAfterTheLastSavedTroopGoesBackToTheFirst() = assertEquals(0, HunterTroops.next(2, 3))

    @Test fun nextWithOneTroopStaysOnIt() = assertEquals(0, HunterTroops.next(0, 1))

    @Test fun currentFallsBackToFirstWhenIndexIsOutOfRange() {
        assertEquals(0, HunterTroops.current(5, 3))
        assertEquals(0, HunterTroops.current(-1, 3))
        assertEquals(2, HunterTroops.current(2, 3))
    }

    @Test fun clampCountStaysBetweenOneAndSeven() {
        assertEquals(1, HunterTroops.clampCount(0))
        assertEquals(7, HunterTroops.clampCount(99))
        assertEquals(4, HunterTroops.clampCount(4))
    }

    @Test fun topLeftPutsCentreOnSavedPoint() {
        // 가운데 500, 크기 60 → 왼쪽 위 470. 창이 화면보다 48 아래에서 시작하면 그만큼 뺀다.
        assertEquals(470, HunterTroops.topLeftFor(500, 60, 0))
        assertEquals(422, HunterTroops.topLeftFor(500, 60, 48))
    }
}
