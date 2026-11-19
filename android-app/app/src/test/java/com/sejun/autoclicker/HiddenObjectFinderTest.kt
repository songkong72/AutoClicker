package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenObjectFinderTest {

    @Test
    fun `normToScreenPx는 0에서 1000 정규화 좌표를 화면 픽셀로 변환한다`() {
        assertEquals(0, HiddenObjectFinder.normToScreenPx(0, 1080))
        assertEquals(540, HiddenObjectFinder.normToScreenPx(500, 1080))
        assertEquals(1080, HiddenObjectFinder.normToScreenPx(1000, 1080))
        assertEquals(1080, HiddenObjectFinder.normToScreenPx(1500, 1080))
        assertEquals(0, HiddenObjectFinder.normToScreenPx(-10, 1080))
    }

    @Test
    fun `buildRequestJson은 thinkingBudget 0과 application json을 포함한다`() {
        val json = HiddenObjectFinder.buildRequestJson("AAAA")
        assertTrue(json.contains("\"thinkingBudget\":0"))
        assertTrue(json.contains("\"responseMimeType\":\"application/json\""))
        assertTrue(json.contains("\"data\":\"AAAA\""))
    }

    @Test
    fun `parseResponse는 정상 JSON 배열 응답을 파싱하고 좌표를 0에서 1000 사이로 보정한다`() {
        val body = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "[{\"label\":\"통풍구\",\"x\":185,\"y\":105},{\"label\":\"밧줄\",\"x\":1200,\"y\":-5}]"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val list = HiddenObjectFinder.parseResponse(body)
        assertEquals(2, list.size)
        assertEquals(FoundObject("통풍구", 185, 105), list[0])
        assertEquals(FoundObject("밧줄", 1000, 0), list[1])
    }

    @Test
    fun `parseResponse는 마크다운 코드블록이 섞여도 JSON 배열을 추출한다`() {
        val body = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "```json\n[{\"label\":\"포대자루\",\"x\":160,\"y\":290}]\n```"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val list = HiddenObjectFinder.parseResponse(body)
        assertEquals(listOf(FoundObject("포대자루", 160, 290)), list)
    }

    @Test
    fun `parseResponse는 candidates가 비어 있으면 빈 목록을 반환한다`() {
        assertEquals(emptyList<FoundObject>(), HiddenObjectFinder.parseResponse("{}"))
        assertEquals(emptyList<FoundObject>(), HiddenObjectFinder.parseResponse("{\"candidates\":[]}"))
    }
}
