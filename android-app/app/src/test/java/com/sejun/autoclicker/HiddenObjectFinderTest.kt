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
    fun `buildRequestJson은 thinkingBudget 256과 application json을 포함한다`() {
        val json = HiddenObjectFinder.buildRequestJson("AAAA")
        assertTrue(json.contains("\"thinkingBudget\":256"))
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

    @Test
    fun `buildRequestJson에서 thinkingConfig를 끌 수 있다`() {
        val json = HiddenObjectFinder.buildRequestJson("AAAA", includeThinkingZero = false)
        assertTrue(!json.contains("thinkingConfig"))
    }

    @Test
    fun `pickBestModelFromList는 generateContent를 지원하는 최신 Flash 모델을 고른다`() {
        val json = """
            {"models":[
              {"name":"models/gemini-3.5-flash","supportedGenerationMethods":["generateContent"]},
              {"name":"models/gemini-3.5-flash-lite","supportedGenerationMethods":["generateContent"]},
              {"name":"models/gemini-3.5-flash-tts","supportedGenerationMethods":["generateContent"]},
              {"name":"models/embedding-001","supportedGenerationMethods":["embedContent"]}
            ]}
        """.trimIndent()
        assertEquals("gemini-3.5-flash-lite", HiddenObjectFinder.pickBestModelFromList(json, preferLite = true))
        assertEquals("gemini-3.5-flash", HiddenObjectFinder.pickBestModelFromList(json, preferLite = false))
    }

    @Test
    fun `parseResponse는 하단 제시어 영역(y 800 초과) 좌표를 걸러낸다`() {
        val body = """
            {"candidates":[{"content":{"parts":[{"text":"[{\"label\":\"가면\",\"x\":740,\"y\":440},{\"label\":\"스툴\",\"x\":800,\"y\":920}]"}]}}]}
        """.trimIndent()
        val list = HiddenObjectFinder.parseResponse(body)
        assertEquals(listOf(FoundObject("가면", 740, 440)), list)
    }

    @Test
    fun `parseResponse는 box_2d 바운딩 박스의 중심 좌표를 계산한다`() {
        val body = """
            {"candidates":[{"content":{"parts":[{"text":"[{\"label\":\"우편함\",\"box_2d\":[500,600,560,680]}]"}]}}]}
        """.trimIndent()
        val list = HiddenObjectFinder.parseResponse(body)
        assertEquals(listOf(FoundObject("우편함", 640, 530)), list)
    }

    @Test
    fun `computeGameCrop과 remapNormToOriginal은 넓은 화면의 가운데 영역을 잘라내고 원본 좌표로 정확히 복원한다`() {
        val crop = HiddenObjectFinder.computeGameCrop(1800, 2000)
        // 2000 * 0.62 = 1240 -> cropX = (1800 - 1240) / 2 = 280, cropY = 100
        assertEquals(HiddenObjectFinder.CropRect(280, 100, 1240, 1900), crop)
        val (ox, oy) = HiddenObjectFinder.remapNormToOriginal(500, 500, crop, 1800, 2000)
        assertEquals(500, ox)
        assertEquals(525, oy)
    }

    @Test
    fun `pickBestModelForMode는 MODEL_PRO 요청 시 최신 Pro 모델을 고른다`() {
        val json = """
            {"models":[
              {"name":"models/gemini-3.5-flash","supportedGenerationMethods":["generateContent"]},
              {"name":"models/gemini-3.5-pro","supportedGenerationMethods":["generateContent"]}
            ]}
        """.trimIndent()
        assertEquals("gemini-3.5-pro", HiddenObjectFinder.pickBestModelForMode(json, HiddenObjectFinder.MODEL_PRO))
    }
}
