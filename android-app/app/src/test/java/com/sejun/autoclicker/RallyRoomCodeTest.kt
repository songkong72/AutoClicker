package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RallyRoomCodeTest {
    @Test fun 여섯자리_숫자는_그대로() = assertEquals("123456", RallyRoomCode.normalize("123456"))
    @Test fun 앞뒤_공백은_지운다() = assertEquals("123456", RallyRoomCode.normalize("  123456 \n"))
    @Test fun 영문과_하이픈도_허용() = assertEquals("ab-12_Z", RallyRoomCode.normalize("ab-12_Z"))
    @Test fun 너무_짧으면_거부() = assertNull(RallyRoomCode.normalize("123"))
    @Test fun 너무_길면_거부() = assertNull(RallyRoomCode.normalize("1".repeat(21)))
    @Test fun 경로를_깨는_글자는_거부() {
        for (bad in listOf("12/3456", "12.3456", "12#3456", "12\$3456", "12[34]56", "12 3456", "")) assertNull(bad, RallyRoomCode.normalize(bad))
    }
}
