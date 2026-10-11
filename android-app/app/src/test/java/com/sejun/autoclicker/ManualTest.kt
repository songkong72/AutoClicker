package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ManualTest {
    private val ko = File("src/main/assets/manual.html").readText()
    private val en = File("src/main/assets/manual-en.html").readText()
    private val zh = File("src/main/assets/manual-zh.html").readText()
    private val zhTw = File("src/main/assets/manual-zh-hant.html").readText()
    private val both = listOf(ko, en, zh, zhTw)

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
        listOf("猎人", "序列", "指挥官管理", "开发者").forEach { assertFalse(it, zh.contains(it)) }
        listOf("獵人", "序列", "指揮官管理", "開發者").forEach { assertFalse(it, zhTw.contains(it)) }
    }

    @Test fun `영어 설명서는 한국어 설명서와 같은 장과 같은 링크를 가진다`() {
        assertEquals(ids(ko), ids(en))
        assertEquals(links(ko), links(en))
        assertFalse(Regex("[가-힣]").containsMatchIn(en))
    }

    @Test fun `중국어 설명서도 한국어 설명서와 같은 장과 같은 링크를 가진다`() = listOf(zh, zhTw).forEach { html ->
        assertEquals(ids(ko), ids(html))
        assertEquals(links(ko), links(html))
        assertFalse(Regex("[가-힣]").containsMatchIn(html))
    }

    @Test fun `간체 설명서와 번체 설명서는 글자가 다르다`() {
        assertTrue(zh.contains("集结队长") && !zh.contains("集結隊長"))
        assertTrue(zhTw.contains("集結隊長") && !zhTw.contains("集结队长"))
    }

    @Test fun `중국어 폰은 글자 종류에 맞는 설명서를 연다`() {
        assertEquals(ManualActivity.PAGE_ZH, ManualActivity.pageFor("zh"))
        assertEquals(ManualActivity.PAGE_ZH, ManualActivity.pageFor("zh", "Hans", "CN"))
        assertEquals(ManualActivity.PAGE_ZH_TW, ManualActivity.pageFor("zh", "Hant", "TW"))
        assertEquals(ManualActivity.PAGE_ZH_TW, ManualActivity.pageFor("zh", "", "HK"))
        assertTrue(ManualActivity.isInside(ManualActivity.PAGE_ZH + "#leader"))
        assertTrue(ManualActivity.isInside(ManualActivity.PAGE_ZH_TW + "#faq"))
    }

    @Test fun `번역이 없는 언어의 폰은 한국어 설명서를 연다`() {
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
