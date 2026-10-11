package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ManualTest {
    private val html = File("src/main/assets/manual.html").readText()

    @Test fun `설명서의 차례 링크는 모두 실제 자리를 가리킨다`() {
        val ids = Regex("""id="([^"]+)"""").findAll(html).map { it.groupValues[1] }.toSet()
        val links = Regex("""href="#([^"]+)"""").findAll(html).map { it.groupValues[1] }.toList()
        assertTrue(links.size >= 10)
        assertEquals(emptyList<String>(), links.filter { it !in ids })
    }

    @Test fun `설명서는 인터넷 없이 열린다 - 바깥 주소와 스크립트가 없다`() {
        assertFalse(html.contains("http://") || html.contains("https://"))
        assertFalse(html.contains("<script"))
    }

    @Test fun `설명서에는 숨은 기능과 개발자 전용 창을 적지 않는다`() {
        listOf("헌터", "순서 클릭", "지휘관 관리", "개발자").forEach { assertFalse(it, html.contains(it)) }
    }

    @Test fun `설명서 안의 자리 이동만 따라가고 바깥 주소는 막는다`() {
        assertTrue(ManualActivity.isInside(ManualActivity.PAGE + "#leader"))
        assertTrue(ManualActivity.isInside(ManualActivity.PAGE))
        assertFalse(ManualActivity.isInside("https://example.com/"))
        assertFalse(ManualActivity.isInside(null))
    }
}
