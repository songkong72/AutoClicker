package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RallyGroupTest {
    private val wbi = RallyGroup("2000", "WBI")

    @Test fun `서버에 쓰는 방 이름과 화면 글자`() {
        assertEquals("2000-WBI-1111", wbi.key("1111"))
        assertEquals("2000 · WBI", wbi.label)
        assertEquals("소속: 2000 서버 · WBI", wbi.shareLine)
        assertEquals("1111", RallyGroup.keyFor(null, "1111"))
        assertEquals("2000-WBI-1111", RallyGroup.keyFor(wbi, "1111"))
    }

    @Test fun `연맹은 대문자와 소문자를 구분한다`() {
        val lower = RallyGroup.of("2000", "wbi")
        assertNotNull(lower)
        assertNotEquals(wbi, lower)
        assertNotEquals(wbi.key("1111"), lower!!.key("1111"))
        assertEquals("wbi", lower.alliance)
    }

    @Test fun `입력 검사`() {
        assertEquals(wbi, RallyGroup.of(" 2000 ", " WBI "))
        assertEquals(RallyGroup("2000", "WBI"), RallyGroup.of("02000", "WBI"))
        assertEquals(RallyGroup("0", "A1"), RallyGroup.of("000", "A1"))
        assertNull(RallyGroup.of("", "WBI"))
        assertNull(RallyGroup.of("20a0", "WBI"))
        assertNull(RallyGroup.of("1234567", "WBI"))
        assertNull(RallyGroup.of("2000", ""))
        assertNull(RallyGroup.of("2000", "W-BI"))
        assertNull(RallyGroup.of("2000", "W BI"))
        assertNull(RallyGroup.of("2000", "ABCDEFGHI"))
        assertNull(RallyGroup.of(null, null))
        assertNotNull(RallyGroup.of("2000", "한국1"))
    }

    @Test fun `틀린 입력에는 이유를 알려 준다`() {
        assertEquals("서버 번호를 적어 주세요.", RallyGroup.problem("", "WBI"))
        assertEquals("연맹을 적어 주세요.", RallyGroup.problem("2000", " "))
        assertNull(RallyGroup.problem("2000", "WBI"))
    }

    @Test fun `내 소속의 방만 방 번호로 골라낸다`() {
        val all = mapOf<String, Any?>(
            "1111" to "옛 방",
            "2000-WBI-1111" to "우리 방",
            "2000-WBI-my-room" to "우리 방 2",
            "2000-wbi-1111" to "소문자 연맹",
            "2000-WBI2-1111" to "다른 연맹",
            "20001-WBI-1111" to "다른 서버",
            "2000-WBI-x" to "방 번호가 너무 짧음"
        )
        assertEquals(mapOf("1111" to "우리 방", "my-room" to "우리 방 2"), RallyGroup.scope(all, wbi))
        assertEquals(all, RallyGroup.scope(all, null))
        assertNull(RallyGroup.scope(null, wbi))
    }

    @Test fun `가장 긴 이름도 서버 규칙의 40자 안에 든다`() {
        val longest = RallyGroup("123456", "ABCDEFGH").key("12345678901234567890")
        assertEquals(36, longest.length)
    }
}
