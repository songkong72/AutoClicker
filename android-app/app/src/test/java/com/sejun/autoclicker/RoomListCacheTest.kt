package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Test

class RoomListCacheTest {
    @Test fun roundTrip() {
        val rooms = listOf("0001" to "0001 · 군단 3개 (배정 2)", "800200" to "800200 · 군단 5개 (배정 5)")
        assertEquals(rooms, RoomListCache.decode(RoomListCache.encode(rooms)))
    }
    @Test fun emptyAndGarbageGiveEmpty() {
        assertEquals(emptyList<Pair<String, String>>(), RoomListCache.decode(null))
        assertEquals(emptyList<Pair<String, String>>(), RoomListCache.decode(""))
        assertEquals(emptyList<Pair<String, String>>(), RoomListCache.decode("noseparator"))
    }
}
