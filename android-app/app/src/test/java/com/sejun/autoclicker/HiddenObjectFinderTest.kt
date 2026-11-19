package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun `buildRequestJson은 thinkingBudget 256과 고해상도·충분한 출력 토큰을 포함한다`() {
        val json = HiddenObjectFinder.buildRequestJson("AAAA")
        assertTrue(json.contains("\"thinkingBudget\":256"))
        assertTrue(json.contains("\"maxOutputTokens\":2048"))
        assertTrue(json.contains("\"mediaResolution\":\"MEDIA_RESOLUTION_HIGH\""))
        assertTrue(json.contains("\"responseMimeType\":\"application/json\""))
        assertTrue(json.contains("\"responseSchema\""))
        assertTrue(json.contains("\"data\":\"AAAA\""))
    }

    @Test
    fun `buildDualCropRequestJson은 제시어와 순수 그림 이미지를 분리해 함께 보낸다`() {
        val json = HiddenObjectFinder.buildDualCropRequestJson("WORDS_B64", "SCENE_B64", thinkingBudget = 256)
        assertTrue(json.contains("\"data\":\"WORDS_B64\""))
        assertTrue(json.contains("\"data\":\"SCENE_B64\""))
        assertTrue(json.contains("\"propertyOrdering\":[\"label\",\"where\",\"box_2d\"]"))
        assertTrue(json.contains("\"mediaResolution\":\"MEDIA_RESOLUTION_HIGH\""))
        assertTrue(json.contains("\"maxOutputTokens\":2048"))
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
    fun `parseResponse는 thinking thought 파트가 앞에 와도 실제 JSON 파트를 파싱한다`() {
        val body = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {"thought": true, "text": "Let me carefully inspect Image 1 and Image 2 for toolbox."},
                      {"text": "[{\"label\":\"공구함\",\"where\":\"gray toolbox on porch\",\"box_2d\":[600,340,660,420]}]"}
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val list = HiddenObjectFinder.parseResponse(body, filterBottomButtons = false)
        assertEquals(listOf(FoundObject("공구함", 380, 630)), list)
    }

    @Test
    fun `parseResponse는 candidates가 비어 있으면 빈 목록을 반환한다`() {
        assertEquals(emptyList<FoundObject>(), HiddenObjectFinder.parseResponse("{}"))
        assertEquals(emptyList<FoundObject>(), HiddenObjectFinder.parseResponse("{\"candidates\":[]}"))
    }

    @Test
    fun `buildRequestJson에서 thinkingConfig와 mediaResolution을 끌 수 있다`() {
        val json = HiddenObjectFinder.buildRequestJson(
            "AAAA",
            includeThinkingZero = false,
            includeMediaResolution = false,
            includeSchema = false,
        )
        assertFalse(json.contains("thinkingConfig"))
        assertFalse(json.contains("mediaResolution"))
        assertFalse(json.contains("responseSchema"))
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
    fun `parseResponse는 단일 이미지 모드에서 하단 제시어 영역(y 850 초과)을 걸러내고 이중 크롭 모드에서는 하단 사물도 유지한다`() {
        val body = """
            {"candidates":[{"content":{"parts":[{"text":"[{\"label\":\"가면\",\"x\":740,\"y\":440},{\"label\":\"말\",\"x\":550,\"y\":920}]"}]}}]}
        """.trimIndent()
        assertEquals(listOf(FoundObject("가면", 740, 440)), HiddenObjectFinder.parseResponse(body, filterBottomButtons = true))
        assertEquals(
            listOf(FoundObject("가면", 740, 440), FoundObject("말", 550, 920)),
            HiddenObjectFinder.parseResponse(body, filterBottomButtons = false),
        )
    }

    @Test
    fun `parseResponse는 box_2d 바운딩 박스의 중심 좌표를 계산하고 중복·과대 박스를 제외한다`() {
        val body = """
            {"candidates":[{"content":{"parts":[{"text":"[{\"label\":\"우편함\",\"where\":\"brown mailbox\",\"box_2d\":[500,600,560,680]},{\"label\":\"우편함\",\"where\":\"dup\",\"box_2d\":[100,100,120,120]},{\"label\":\"배경\",\"where\":\"whole room\",\"box_2d\":[50,50,650,700]}]"}]}}]}
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
    fun `detectIllustrationRect는 흰색 사각형 그림 액자와 하단 제시어 영역을 정확히 분리한다`() {
        val w = 852
        val h = 1024
        val rect = HiddenObjectFinder.detectIllustrationRect(w, h) { x, y ->
            val onVert = (x in 184..186 || x in 666..668) && y in 60..849
            val onHoriz = (y in 60..62 || y in 847..849) && x in 184..668
            // 왼쪽 하단 오류 토스트(y=720..770)가 테두리를 일부 가려도 감지되어야 한다
            val coveredByToast = x in 184..186 && y in 720..770
            (onVert || onHoriz) && !coveredByToast
        }
        assertNotNull(rect)
        // inset=2 적용 -> x=186, y=62, w=480, h=785
        assertEquals(HiddenObjectFinder.CropRect(186, 62, 480, 785), rect)

        val words = HiddenObjectFinder.computeWordsCrop(rect!!, w, h)
        assertNotNull(words)
        assertEquals(847, words!!.y)
        assertEquals(1024 - 847, words.h)
        assertTrue(words.x < rect.x && words.x + words.w > rect.x + rect.w)
    }

    @Test
    fun `detectIllustrationRect는 순백색 화면이나 테두리 없는 화면에서 null을 반환한다`() {
        assertNull(HiddenObjectFinder.detectIllustrationRect(852, 1024) { _, _ -> true })
        assertNull(HiddenObjectFinder.detectIllustrationRect(852, 1024) { _, _ -> false })
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

    @Test
    fun `buildFallbackChain은 Pro와 Flash 한도 초과 시 하위 모델로 자동 폴백 순서를 구성한다`() {
        assertEquals(
            listOf(HiddenObjectFinder.MODEL_PRO, HiddenObjectFinder.MODEL_FLASH, HiddenObjectFinder.MODEL_LITE),
            HiddenObjectFinder.buildFallbackChain(HiddenObjectFinder.MODEL_PRO),
        )
        assertEquals(
            listOf(HiddenObjectFinder.MODEL_FLASH, HiddenObjectFinder.MODEL_LITE),
            HiddenObjectFinder.buildFallbackChain(HiddenObjectFinder.MODEL_FLASH),
        )
        assertEquals(
            listOf(HiddenObjectFinder.MODEL_LITE, HiddenObjectFinder.MODEL_FLASH),
            HiddenObjectFinder.buildFallbackChain(HiddenObjectFinder.MODEL_LITE),
        )
    }

    @Test
    fun `pickAlternateModel은 이미 시도한 모델을 제외하고 다른 Flash 모델 버킷을 고른다`() {
        val json = """
            {"models":[
              {"name":"models/gemini-flash-latest","supportedGenerationMethods":["generateContent"]},
              {"name":"models/gemini-2.5-flash","supportedGenerationMethods":["generateContent"]},
              {"name":"models/gemini-2.5-flash-lite","supportedGenerationMethods":["generateContent"]}
            ]}
        """.trimIndent()
        assertEquals(
            "gemini-2.5-flash",
            HiddenObjectFinder.pickAlternateModel(json, setOf("gemini-flash-latest", "gemini-flash-lite-latest")),
        )
        assertEquals(
            "gemini-2.5-flash-lite",
            HiddenObjectFinder.pickAlternateModel(json, setOf("gemini-flash-latest", "gemini-2.5-flash")),
        )
    }

    @Test
    fun `isZeroQuotaError는 무료 한도 0 오류 메시지를 판별한다`() {
        assertTrue(HiddenObjectFinder.isZeroQuotaError("""{"error":{"message":"Quota exceeded for metric: generate_content_free_tier_requests, limit: 0"}}"""))
        assertFalse(HiddenObjectFinder.isZeroQuotaError("""{"error":{"message":"Resource has been exhausted (e.g. check quota)."}}"""))
    }

    @Test
    fun `detectIllustrationRect는 좁은 화면에서 왼쪽 플로팅 막대가 테두리 중간을 가려도 감지한다`() {
        val w = 540
        val h = 1170
        val rect = HiddenObjectFinder.detectIllustrationRect(w, h) { x, y ->
            val onVert = (x in 52..54 || x in 486..488) && y in 70..960
            val onHoriz = (y in 70..72 || y in 958..960) && x in 52..488
            // 왼쪽 플로팅 막대가 y=25%~58% 구간에서 왼쪽 흰색 테두리를 완전히 가린 상황 재현
            val coveredBySidebar = x in 52..54 && y in 290..680
            (onVert || onHoriz) && !coveredBySidebar
        }
        assertNotNull(rect)
        assertEquals(54, rect!!.x)
        assertEquals(72, rect.y)
    }

    @Test
    fun `pickAlternateModels는 서로 다른 할당량 버킷(Gemini 3·2_5·2_0 Flash·Lite 및 Gemma 멀티모달)을 가족별 1개씩 반환한다`() {
        val json = """
            {"models":[
              {"name":"models/gemini-flash-latest","supportedGenerationMethods":["generateContent"]},
              {"name":"models/gemini-3-flash-preview","supportedGenerationMethods":["generateContent"]},
              {"name":"models/gemini-2.5-flash","supportedGenerationMethods":["generateContent"]},
              {"name":"models/gemini-2.5-flash-preview-05-20","supportedGenerationMethods":["generateContent"]},
              {"name":"models/gemini-2.5-flash-lite","supportedGenerationMethods":["generateContent"]},
              {"name":"models/gemini-2.0-flash","supportedGenerationMethods":["generateContent"]},
              {"name":"models/gemma-3-27b-it","supportedGenerationMethods":["generateContent"]},
              {"name":"models/gemma-3-1b-it","supportedGenerationMethods":["generateContent"]}
            ]}
        """.trimIndent()
        val alts = HiddenObjectFinder.pickAlternateModels(json, setOf("gemini-flash-latest", "gemini-flash-lite-latest"))
        assertEquals(
            listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-3-flash-preview", "gemini-2.5-flash-lite", "gemma-3-27b-it"),
            alts,
        )
    }

    @Test
    fun `parseApiKeys는 쉼표와 줄바꿈으로 구분된 여러 키를 중복 없이 분리한다`() {
        assertEquals(listOf("AIza111", "AIza222", "AIza333"), HiddenObjectFinder.parseApiKeys(" AIza111, AIza222\nAIza111\r\nAIza333 "))
        assertTrue(HiddenObjectFinder.parseApiKeys("   ").isEmpty())
    }

    @Test
    fun `classifyQuotaError는 일일 한도·무료한도 0·분당 한도를 구분해 한국어로 안내한다`() {
        assertTrue(HiddenObjectFinder.classifyQuotaError("GenerateRequestsPerDayPerProjectPerModel-FreeTier").contains("일일 무료한도"))
        assertTrue(HiddenObjectFinder.classifyQuotaError("limit: 0 for free_tier").contains("무료 한도 0"))
        assertTrue(HiddenObjectFinder.classifyQuotaError("GenerateRequestsPerMinutePerProject").contains("분당 한도"))
    }

    @Test
    fun `buildDualCropRequestJson은 Gemma용 일반 모드에서 responseMimeType과 schema를 제외한다`() {
        val json = HiddenObjectFinder.buildDualCropRequestJson(
            "W", "S",
            includeThinking = false,
            thinkingBudget = 0,
            includeMediaResolution = false,
            includeSchema = false,
            includeResponseMimeType = false,
        )
        assertFalse(json.contains("responseMimeType"))
        assertFalse(json.contains("responseSchema"))
        assertFalse(json.contains("thinkingConfig"))
        assertFalse(json.contains("mediaResolution"))
    }
}


