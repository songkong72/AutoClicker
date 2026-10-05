package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Test

class RallyRoomCheckTest {
    @Test
    fun `서버가 null을 돌려주면 방이 없는 것이다`() {
        assertEquals(false, RallyRoomCheck.exists(200, "null"))
        assertEquals(false, RallyRoomCheck.exists(200, ""))
    }

    @Test
    fun `run 값이 오면 방이 있는 것이다`() {
        assertEquals(true, RallyRoomCheck.exists(200, "\"IDLE\""))
        assertEquals(true, RallyRoomCheck.exists(200, "\"RUNNING\""))
    }

    @Test
    fun `응답 코드가 실패면 알 수 없다`() {
        assertEquals(null, RallyRoomCheck.exists(401, "{\"error\":\"Permission denied\"}"))
        assertEquals(null, RallyRoomCheck.exists(500, ""))
    }
}
