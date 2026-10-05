package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Test

class RallyRoomHistoryTest {
    @Test
    fun `새로 들어간 방이 맨 앞에 오고 같은 방은 중복되지 않는다`() {
        val l = RallyRoomHistory.add(listOf("111111", "222222"), "333333")
        assertEquals(listOf("333333", "111111", "222222"), l)
        assertEquals(listOf("222222", "111111"), RallyRoomHistory.add(listOf("111111", "222222"), "222222"))
    }

    @Test
    fun `기록은 최근 8개까지만 남긴다`() {
        var l = emptyList<String>()
        for (i in 1..12) l = RallyRoomHistory.add(l, "room${1000 + i}")
        assertEquals(RallyRoomHistory.MAX, l.size)
        assertEquals("room1012", l.first())
        assertEquals("room1005", l.last())
    }

    @Test
    fun `형식이 맞지 않는 방 번호는 기록하지 않는다`() {
        assertEquals(listOf("111111"), RallyRoomHistory.add(listOf("111111"), "ab"))
        assertEquals(listOf("111111"), RallyRoomHistory.add(listOf("111111"), "방 번호!"))
    }

    @Test
    fun `저장하고 읽으면 같은 목록이고 깨진 값은 걸러진다`() {
        val l = listOf("800200", "abcd-1")
        assertEquals(l, RallyRoomHistory.decode(RallyRoomHistory.encode(l)))
        assertEquals(listOf("800200"), RallyRoomHistory.decode("800200,,x, 800200 ,bad value"))
        assertEquals(emptyList<String>(), RallyRoomHistory.decode(null))
    }
}
