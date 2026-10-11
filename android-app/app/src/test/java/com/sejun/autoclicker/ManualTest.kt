package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ManualTest {
    private val ko = File("src/main/assets/manual.html").readText()
    private val en = File("src/main/assets/manual-en.html").readText()
    private val both = listOf(ko, en)

    private fun ids(html: String) = Regex("""id="([^"]+)"""").findAll(html).map { it.groupValues[1] }.toList()
    private fun links(html: String) = Regex("""href="#([^"]+)"""").findAll(html).map { it.groupValues[1] }.toList()

    @Test fun `설명서의 차례 링크는 모두 실제 자리를 가리킨다`() = both.forEach { html ->
        assertTrue(links(html).size >= 10)
        assertEquals(emptyList<String>(), links(html).filter { it !in ids(html) })
    }

    @Test fun `설명서는 인터넷 없이 열린다 - 바깥 주소와 스크립트가 없다`() = both.forEach { html ->
        assertFalse(html.contains("http://") || html.contains("https://"))
        assertFalse(html.contains("<script"))
    }

    @Test fun `설명서에는 숨은 기능과 개발자 전용 창을 적지 않는다`() {
        listOf("헌터", "순서 클릭", "지휘관 관리", "개발자").forEach { assertFalse(it, ko.contains(it)) }
        listOf("Hunter", "sequence", "Commander admin", "developer").forEach { assertFalse(it, en.contains(it, ignoreCase = true)) }
    }

    @Test fun `영어 설명서는 한국어 설명서와 같은 장과 같은 링크를 가진다`() {
        assertEquals(ids(ko), ids(en))
        assertEquals(links(ko), links(en))
        assertFalse(Regex("[가-힣]").containsMatchIn(en))
    }

    @Test fun `폰 언어가 영어일 때만 영어 설명서를 연다`() {
        assertEquals(ManualActivity.PAGE_EN, ManualActivity.pageFor("en"))
        assertEquals(ManualActivity.PAGE, ManualActivity.pageFor("ko"))
        assertEquals(ManualActivity.PAGE, ManualActivity.pageFor("ja"))
        assertEquals(ManualActivity.PAGE, ManualActivity.pageFor(null))
    }

    @Test fun `설명서 안의 자리 이동만 따라가고 바깥 주소는 막는다`() {
        assertTrue(ManualActivity.isInside(ManualActivity.PAGE + "#leader"))
        assertTrue(ManualActivity.isInside(ManualActivity.PAGE_EN + "#leader"))
        assertTrue(ManualActivity.isInside(ManualActivity.PAGE))
        assertFalse(ManualActivity.isInside("https://example.com/"))
        assertFalse(ManualActivity.isInside(null))
    }
}
