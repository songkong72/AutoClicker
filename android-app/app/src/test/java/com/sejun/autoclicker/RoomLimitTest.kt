package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Test

class RoomLimitTest {
    private fun rooms(n: Int) = (1..n).map { "R$it" }.toSet()

    @Test fun newRoomAllowedBelowLimit() = assertEquals(RoomLimit.Verdict.ALLOW, RoomLimit.decide(rooms(9), "NEW"))
    @Test fun newRoomRefusedAtLimit() = assertEquals(RoomLimit.Verdict.FULL, RoomLimit.decide(rooms(10), "NEW"))
    @Test fun existingRoomAlwaysAllowedEvenWhenFull() = assertEquals(RoomLimit.Verdict.ALLOW, RoomLimit.decide(rooms(10), "R3"))
    @Test fun unreadableListIsUnknown() = assertEquals(RoomLimit.Verdict.UNKNOWN, RoomLimit.decide(null, "NEW"))
}
