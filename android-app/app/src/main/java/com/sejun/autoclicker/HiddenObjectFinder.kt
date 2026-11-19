package com.sejun.autoclicker

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

data class FoundObject(
    val label: String,
    val normX: Int,
    val normY: Int,
)

/**
 * 숨은그림찾기 화면을 Gemini Vision API로 분석해
 * 하단 제시어의 상단 그림 내 중심 좌표(0~1000 정규화)를 추출한다.
 */
internal object HiddenObjectFinder {
    const val MODEL_FLASH = "gemini-flash-latest"
    const val MODEL_LITE = "gemini-flash-lite-latest"
    const val MODEL_PRO = "gemini-pro-latest"

    private const val MAX_SCENE_DIM = 832
    private const val MAX_WORDS_DIM = 512
    private const val JPEG_QUALITY = 80
    private const val WORDS_JPEG_QUALITY = 75
    private const val CONNECT_TIMEOUT_MS = 4000
    private const val READ_TIMEOUT_MS = 12000
    private const val MAX_OUTPUT_TOKENS = 2048
    private const val MODELS_CACHE_TTL_MS = 10 * 60 * 1000L
    private const val COOLDOWN_RATE_LIMIT_MS = 45 * 1000L
    private const val COOLDOWN_DAILY_QUOTA_MS = 5 * 60 * 1000L
    private const val COOLDOWN_ZERO_QUOTA_MS = 10 * 60 * 1000L
    private const val FALLBACK_BUDGET_MS = 10 * 1000L

    @Volatile private var cachedLiteModel: String? = null
    @Volatile private var cachedFlashModel: String? = null
    @Volatile private var cachedProModel: String? = null
    @Volatile private var cachedModelsJson: String? = null
    @Volatile private var cachedModelsFetchedAt: Long = 0L

    private val quotaCooldownUntil = ConcurrentHashMap<String, Long>()
    private val lastWorkingModelByKey = ConcurrentHashMap<String, String>()

    data class CropRect(val x: Int, val y: Int, val w: Int, val h: Int)

    /**
     * 폴드/태블릿처럼 가로가 넓은 화면에서는 양옆의 눈 내리는 배경 여백을 잘라내고
     * 가운데 게임 카드 영역만 남겨 작은 사물 화질을 높인다. (테두리 자동 감지 실패 시 폴백용)
     */
    fun computeGameCrop(origW: Int, origH: Int): CropRect {
        if (origW <= 0 || origH <= 0) return CropRect(0, 0, origW.coerceAtLeast(1), origH.coerceAtLeast(1))
        val targetW = if (origW > origH * 0.62f) {
            (origH * 0.62f).toInt().coerceIn(1, origW)
        } else {
            origW
        }
        val cropX = (origW - targetW) / 2
        val cropY = (origH * 0.05f).toInt().coerceIn(0, origH - 1)
        val cropH = (origH - cropY).coerceAtLeast(1)
        return CropRect(cropX, cropY, targetW, cropH)
    }

    /**
     * 숨은그림찾기 게임의 흰색 사각형 그림 액자 테두리를 픽셀에서 자동 감지한다.
     * 바깥쪽에서 안쪽으로 스캔해 그림 안의 눈/하늘에 속지 않고 정확한 그림 영역만 분리하며,
     * 좁은 화면에서 왼쪽 플로팅 막대가 테두리 중간을 가려도 위·아래 노출 구간으로 안정적으로 감지한다.
     */
    fun detectIllustrationRect(
        width: Int,
        height: Int,
        isBrightWhite: (x: Int, y: Int) -> Boolean,
    ): CropRect? {
        if (width < 100 || height < 100) return null

        val sampleYs = IntArray(25) { i ->
            (height * (0.10 + i * 0.029)).toInt().coerceIn(0, height - 1)
        }

        val leftLimit = (width * 0.45).toInt()
        var left = -1
        for (x in 0 until leftLimit) {
            var cnt = 0
            for (y in sampleYs) if (isBrightWhite(x, y)) cnt++
            if (cnt >= 10) {
                left = x
                break
            }
        }

        val rightLimit = (width * 0.55).toInt()
        var right = -1
        for (x in (width - 1) downTo (rightLimit + 1)) {
            var cnt = 0
            for (y in sampleYs) if (isBrightWhite(x, y)) cnt++
            if (cnt >= 10) {
                right = x
                break
            }
        }

        if (left < 0 || right < 0 || right - left < (width * 0.35).toInt()) return null

        val span = right - left
        val sampleXs = IntArray(20) { i ->
            (left + span * (0.10 + i * 0.04)).toInt().coerceIn(left, right)
        }

        val topStart = (height * 0.02).toInt()
        val topEnd = (height * 0.30).toInt()
        var top = -1
        for (y in topStart until topEnd) {
            var cnt = 0
            for (x in sampleXs) if (isBrightWhite(x, y)) cnt++
            if (cnt >= 13) {
                top = y
                break
            }
        }

        val botStart = (height * 0.90).toInt().coerceAtMost(height - 1)
        val botEnd = (height * 0.60).toInt()
        var bottom = -1
        for (y in botStart downTo (botEnd + 1)) {
            var cnt = 0
            for (x in sampleXs) if (isBrightWhite(x, y)) cnt++
            if (cnt >= 13) {
                bottom = y
                break
            }
        }

        if (top < 0 || bottom < 0) return null
        val rectW = right - left
        val rectH = bottom - top
        if (rectH < (height * 0.45).toInt() || bottom > (height * 0.92).toInt()) return null

        // 순백색 화면 오감지 방지: 내부 5개 지점이 모두 흰색이면 실제 게임 그림이 아니다.
        val interiorWhite = listOf(
            0.3 to 0.3, 0.7 to 0.3, 0.5 to 0.5, 0.3 to 0.7, 0.7 to 0.7,
        ).count { (fx, fy) ->
            isBrightWhite(
                (left + rectW * fx).toInt().coerceIn(0, width - 1),
                (top + rectH * fy).toInt().coerceIn(0, height - 1),
            )
        }
        if (interiorWhite == 5) return null

        val inset = (minOf(rectW, rectH) * 0.005).toInt().coerceIn(2, 6)
        val ix = (left + inset).coerceIn(0, width - 2)
        val iy = (top + inset).coerceIn(0, height - 2)
        val iw = (right - inset - ix).coerceAtLeast(1)
        val ih = (bottom - inset - iy).coerceAtLeast(1)
        return CropRect(ix, iy, iw, ih)
    }

    /**
     * Bitmap에서 행/열 캐시(총 ~45회 getPixels)를 사용해 1ms 이내에 그림 액자 영역을 찾는다.
     */
    fun detectIllustrationRect(bitmap: Bitmap): CropRect? {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 100 || h < 100) return null
        val rowCache = HashMap<Int, IntArray>(32)
        val colCache = HashMap<Int, IntArray>(24)

        fun isWhite(p: Int): Boolean {
            val r = (p ushr 16) and 0xFF
            val g = (p ushr 8) and 0xFF
            val b = p and 0xFF
            return r >= 230 && g >= 230 && b >= 230
        }

        return detectIllustrationRect(w, h) { x, y ->
            val cachedRow = rowCache[y]
            if (cachedRow != null) {
                isWhite(cachedRow[x])
            } else if (rowCache.size < 26) {
                val row = IntArray(w)
                bitmap.getPixels(row, 0, w, 0, y, w, 1)
                rowCache[y] = row
                isWhite(row[x])
            } else {
                val cachedCol = colCache[x]
                if (cachedCol != null) {
                    isWhite(cachedCol[y])
                } else if (colCache.size < 22) {
                    val col = IntArray(h)
                    bitmap.getPixels(col, 0, 1, x, 0, 1, h)
                    colCache[x] = col
                    isWhite(col[y])
                } else {
                    isWhite(bitmap.getPixel(x, y))
                }
            }
        }
    }

    /**
     * 감지된 그림 액자 바로 아래의 하단 제시어 버튼 영역을 계산한다.
     */
    fun computeWordsCrop(illustrationRect: CropRect, origW: Int, origH: Int): CropRect? {
        if (origW <= 0 || origH <= 0) return null
        val yStart = (illustrationRect.y + illustrationRect.h).coerceIn(0, origH)
        val h = origH - yStart
        if (h < (origH * 0.06f).toInt()) return null
        val padX = (illustrationRect.w * 0.10f).toInt()
        val xStart = (illustrationRect.x - padX).coerceAtLeast(0)
        val xEnd = (illustrationRect.x + illustrationRect.w + padX).coerceAtMost(origW)
        val w = (xEnd - xStart).coerceAtLeast(1)
        return CropRect(xStart, yStart, w, h)
    }

    fun remapNormToOriginal(normX: Int, normY: Int, crop: CropRect, origW: Int, origH: Int): Pair<Int, Int> {
        if (origW <= 0 || origH <= 0) return normX to normY
        val ox = ((crop.x + normX.coerceIn(0, 1000) * crop.w / 1000.0) * 1000.0 / origW).toInt().coerceIn(0, 1000)
        val oy = ((crop.y + normY.coerceIn(0, 1000) * crop.h / 1000.0) * 1000.0 / origH).toInt().coerceIn(0, 1000)
        return ox to oy
    }

    private const val RESPONSE_SCHEMA_JSON =
        """{"type":"ARRAY","items":{"type":"OBJECT","properties":{"label":{"type":"STRING"},"where":{"type":"STRING"},"box_2d":{"type":"ARRAY","items":{"type":"INTEGER"}}},"required":["label","where","box_2d"],"propertyOrdering":["label","where","box_2d"]}}"""

    private const val SINGLE_PROMPT =
        "Hidden-object game screen. Look at the Korean target word buttons at the bottom (y > 830). " +
            "Pick up to 3 target words starting from the BACK (the LAST/bottom-most row first, right-to-left). " +
            "Carefully inspect the upper illustration ONLY (ymax <= 845) to find each exact object—only include an item if you clearly see it, never guess randomly or mark text buttons. " +
            "Return ONLY a JSON array [{\\\"label\\\":\\\"Korean word\\\",\\\"where\\\":\\\"3-6 word visual description and nearby landmark\\\",\\\"box_2d\\\":[ymin,xmin,ymax,xmax]}] with tight 0-1000 coordinates."

    private const val DUAL_WORDS_PROMPT =
        "Image 1 shows the Korean target word buttons at the bottom of a hidden-object game. " +
            "Read ALL visible Korean target words in Image 1. Choose up to 3 target words starting from the BACK (the LAST/bottom-most row first, right-to-left)."

    private const val DUAL_SCENE_PROMPT =
        "Image 2 is the pure game illustration ONLY (no UI buttons). " +
            "Carefully scan Image 2 to locate each chosen target object from Image 1. Objects may be small or partially hidden in the background. " +
            "ONLY include an item if you clearly see that EXACT object in Image 2—never guess a random background spot or mark an unrelated similar item. " +
            "Return ONLY a raw JSON array: [{\\\"label\\\":\\\"exact Korean word from Image 1\\\",\\\"where\\\":\\\"3-6 word description of the exact object and nearby landmark in Image 2\\\",\\\"box_2d\\\":[ymin,xmin,ymax,xmax]}] where box_2d is a tight 0-1000 bounding box strictly inside Image 2."

    private fun buildGenerationConfig(
        includeThinking: Boolean,
        thinkingBudget: Int,
        includeMediaResolution: Boolean,
        includeSchema: Boolean,
        includeResponseMimeType: Boolean = true,
    ): String {
        val sb = StringBuilder()
        sb.append(""""temperature":0.0,"maxOutputTokens":$MAX_OUTPUT_TOKENS""")
        if (includeResponseMimeType) sb.append(""","responseMimeType":"application/json"""")
        if (includeSchema && includeResponseMimeType) sb.append(""","responseSchema":$RESPONSE_SCHEMA_JSON""")
        if (includeMediaResolution) sb.append(""","mediaResolution":"MEDIA_RESOLUTION_HIGH"""")
        if (includeThinking) sb.append(""","thinkingConfig":{"thinkingBudget":$thinkingBudget}""")
        return sb.toString()
    }

    fun buildRequestJson(
        base64Jpeg: String,
        includeThinkingZero: Boolean = true,
        thinkingBudget: Int = 256,
        includeMediaResolution: Boolean = true,
        includeSchema: Boolean = true,
        includeResponseMimeType: Boolean = true,
    ): String {
        val cleanB64 = base64Jpeg.replace("\r", "").replace("\n", "")
        val genCfg = buildGenerationConfig(
            includeThinkingZero,
            thinkingBudget,
            includeMediaResolution,
            includeSchema,
            includeResponseMimeType,
        )
        return """{"contents":[{"parts":[{"text":"$SINGLE_PROMPT"},{"inline_data":{"mime_type":"image/jpeg","data":"$cleanB64"}}]}],"generationConfig":{$genCfg}}"""
    }

    /**
     * 하단 제시어 이미지(Image 1)와 순수 그림 이미지(Image 2)를 한 번의 요청으로 분리 전송한다.
     * 제시어 버튼 오표시를 원천 차단하고, 그림 해상도를 극대화해 작은 사물 인식률을 높인다.
     */
    fun buildDualCropRequestJson(
        wordsBase64Jpeg: String,
        sceneBase64Jpeg: String,
        includeThinking: Boolean = true,
        thinkingBudget: Int = 256,
        includeMediaResolution: Boolean = true,
        includeSchema: Boolean = true,
        includeResponseMimeType: Boolean = true,
    ): String {
        val cleanWords = wordsBase64Jpeg.replace("\r", "").replace("\n", "")
        val cleanScene = sceneBase64Jpeg.replace("\r", "").replace("\n", "")
        val genCfg = buildGenerationConfig(
            includeThinking,
            thinkingBudget,
            includeMediaResolution,
            includeSchema,
            includeResponseMimeType,
        )
        return """{"contents":[{"parts":[{"text":"$DUAL_WORDS_PROMPT"},{"inline_data":{"mime_type":"image/jpeg","data":"$cleanWords"}},{"text":"$DUAL_SCENE_PROMPT"},{"inline_data":{"mime_type":"image/jpeg","data":"$cleanScene"}}]}],"generationConfig":{$genCfg}}"""
    }

    /**
     * API 키 입력칸에 쉼표나 줄바꿈으로 여러 프로젝트 키가 들어 있으면 순서대로 분리해
     * 429 한도 초과 시 다음 키로 자동 순환한다.
     */
    fun parseApiKeys(raw: String): List<String> =
        raw.split(',', '\n', '\r', ';', ' ', '\t')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    fun buildFallbackChain(targetModel: String): List<String> = when (targetModel) {
        MODEL_PRO -> listOf(MODEL_PRO, MODEL_FLASH, MODEL_LITE)
        MODEL_LITE -> listOf(MODEL_LITE, MODEL_FLASH)
        else -> listOf(MODEL_FLASH, MODEL_LITE)
    }

    fun isZeroQuotaError(errorBody: String): Boolean {
        val lower = errorBody.lowercase()
        return lower.contains("limit: 0") || lower.contains("limit:0") ||
            (lower.contains("free_tier") && lower.contains("quota"))
    }

    fun isDailyQuotaError(errorBody: String): Boolean {
        val lower = errorBody.lowercase()
        return lower.contains("perday") || lower.contains("per_day") ||
            lower.contains("daily") || lower.contains("requestsperday")
    }

    fun classifyQuotaError(errorBody: String): String {
        val lower = errorBody.lowercase()
        return when {
            isDailyQuotaError(errorBody) -> "일일 무료한도 소진 (오후5시 초기화)"
            lower.contains("limit: 0") || lower.contains("limit:0") -> "무료 한도 0 (새 프로젝트 키 필요)"
            lower.contains("perminute") || lower.contains("per_minute") || lower.contains("rpm") -> "분당 한도 초과 (1분 후 재시도)"
            else -> "HTTP 429 한도초과 (잠시 후 재시도)"
        }
    }

    fun pickBestModelFromList(modelsJson: String, preferLite: Boolean): String? =
        pickBestModelForMode(modelsJson, if (preferLite) MODEL_LITE else MODEL_FLASH)

    private fun extractVisionCandidates(modelsJson: String, includeGemma: Boolean = false): List<String> {
        val blockRegex = Regex("""\{[^{}]*"name"\s*:\s*"models/([^"]+)"[^{}]*"supportedGenerationMethods"\s*:\s*\[([^\]]*)\][^{}]*\}""")
        val excluded = listOf(
            "tts", "image", "live", "audio", "embedding", "robotics",
            "computer-use", "deep-research", "veo", "imagen", "lyria", "aqa", "thinking-exp",
        )
        val out = ArrayList<String>()
        for (m in blockRegex.findAll(modelsJson)) {
            val id = m.groupValues[1].trim()
            val methods = m.groupValues[2]
            if (!methods.contains("generateContent")) continue
            val lower = id.lowercase()
            if (excluded.any { lower.contains(it) }) continue
            val isGemini = lower.startsWith("gemini-")
            val isVisionGemma = includeGemma && (
                lower.startsWith("gemma-4-") ||
                    (lower.startsWith("gemma-3-") && (lower.contains("27b") || lower.contains("12b")))
                )
            if (!isGemini && !isVisionGemma) continue
            out.add(id)
        }
        return out
    }

    /**
     * `v1beta/models` 응답에서 generateContent를 지원하는 가장 적합한 모델(Flash / Lite / Pro)을 고른다.
     */
    fun pickBestModelForMode(modelsJson: String, targetModel: String): String? {
        val wantPro = targetModel == MODEL_PRO
        val wantLite = targetModel == MODEL_LITE
        val candidates = extractVisionCandidates(modelsJson, includeGemma = false).filter { id ->
            val lower = id.lowercase()
            if (wantPro) lower.contains("pro") else lower.contains("flash")
        }
        if (candidates.isEmpty()) return null
        val sorted = candidates.sortedDescending()
        return when {
            wantPro -> sorted.first()
            wantLite -> sorted.firstOrNull { it.lowercase().contains("lite") } ?: sorted.first()
            else -> sorted.firstOrNull { !it.lowercase().contains("lite") } ?: sorted.first()
        }
    }

    private fun modelFamilyKey(id: String): String {
        val lower = id.lowercase()
        val gemMatch = Regex("""^(gemini-\d+(?:\.\d+)?)-(flash-lite|flash|pro)""").find(lower)
        if (gemMatch != null) return "${gemMatch.groupValues[1]}-${gemMatch.groupValues[2]}"
        val gemmaMatch = Regex("""^(gemma-\d+-\d+b)""").find(lower)
        if (gemmaMatch != null) return gemmaMatch.groupValues[1]
        return lower
    }

    /**
     * 이미 시도해 429/503을 받은 모델들을 제외하고, 서로 다른 할당량 버킷을 가진
     * 대체 Vision 모델들(Gemini 3/2.5/2.0 Flash·Lite, Gemma 4/3 멀티모달)을 우선순위대로 반환한다.
     */
    fun pickAlternateModels(modelsJson: String, triedModels: Set<String>, maxCount: Int = 6): List<String> {
        val triedLower = triedModels.map { it.lowercase() }.toSet()
        val all = extractVisionCandidates(modelsJson, includeGemma = true)
            .filter { id ->
                val lower = id.lowercase()
                !lower.contains("latest") &&
                    lower !in triedLower &&
                    (lower.contains("flash") || lower.startsWith("gemma-"))
            }

        fun sortWithinGroup(list: List<String>): List<String> = list.sortedWith(
            compareByDescending<String> { !it.lowercase().contains("preview") && !it.lowercase().contains("exp") }
                .thenByDescending { it },
        )

        val geminiFlash = sortWithinGroup(all.filter { it.lowercase().startsWith("gemini-") && !it.lowercase().contains("lite") })
        val geminiLite = sortWithinGroup(all.filter { it.lowercase().startsWith("gemini-") && it.lowercase().contains("lite") })
        val gemmaVision = sortWithinGroup(all.filter { it.lowercase().startsWith("gemma-") })

        val picked = ArrayList<String>()
        val seenFamilies = HashSet<String>()
        for (id in geminiFlash + geminiLite + gemmaVision) {
            val fam = modelFamilyKey(id)
            if (seenFamilies.add(fam)) {
                picked.add(id)
                if (picked.size >= maxCount) break
            }
        }
        return picked
    }

    /**
     * 이미 시도해 429/503을 받은 모델들을 제외하고 별도 할당량 버킷을 가진 다른 Flash/Lite 모델을 고른다.
     */
    fun pickAlternateModel(modelsJson: String, triedModels: Set<String>): String? =
        pickAlternateModels(modelsJson, triedModels, maxCount = 1).firstOrNull()

    fun parseResponse(responseBody: String, filterBottomButtons: Boolean = true): List<FoundObject> {
        val rawText = extractBestPartText(responseBody) ?: return emptyList()
        val jsonArray = extractJsonArray(rawText) ?: return emptyList()
        val itemRegex = Regex("""\{[^{}]*\}""")
        val labelRegex = Regex(""""label"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        val num = """-?\d+(?:\.\d+)?"""
        val boxRegex = Regex(""""box_2d"\s*:\s*\[\s*($num)\s*,\s*($num)\s*,\s*($num)\s*,\s*($num)\s*\]""")
        val xRegex = Regex(""""x"\s*:\s*($num)""")
        val yRegex = Regex(""""y"\s*:\s*($num)""")

        val out = ArrayList<FoundObject>()
        val seenLabels = HashSet<String>()
        for (match in itemRegex.findAll(jsonArray)) {
            val block = match.value
            val labelRaw = labelRegex.find(block)?.groupValues?.get(1) ?: continue
            val label = unescapeJsonString(labelRaw).trim()
            if (label.isEmpty()) continue
            val dedupeKey = label.lowercase().replace(" ", "")
            if (!seenLabels.add(dedupeKey)) continue

            val boxMatch = boxRegex.find(block)
            val (cx, cy) = if (boxMatch != null) {
                val rYmin = boxMatch.groupValues[1].toDoubleOrNull()?.toInt() ?: continue
                val rXmin = boxMatch.groupValues[2].toDoubleOrNull()?.toInt() ?: continue
                val rYmax = boxMatch.groupValues[3].toDoubleOrNull()?.toInt() ?: continue
                val rXmax = boxMatch.groupValues[4].toDoubleOrNull()?.toInt() ?: continue
                val ymin = minOf(rYmin, rYmax)
                val ymax = maxOf(rYmin, rYmax)
                val xmin = minOf(rXmin, rXmax)
                val xmax = maxOf(rXmin, rXmax)
                val bh = ymax - ymin
                val bw = xmax - xmin
                // 방 전체 등 지나치게 큰 박스는 오답으로 제외한다.
                if (bh > 500 || bw > 500 || bh.toLong() * bw.toLong() > 220_000L) continue
                ((xmin + xmax) / 2) to ((ymin + ymax) / 2)
            } else {
                val x = xRegex.find(block)?.groupValues?.get(1)?.toDoubleOrNull()?.toInt() ?: continue
                val y = yRegex.find(block)?.groupValues?.get(1)?.toDoubleOrNull()?.toInt() ?: continue
                x to y
            }

            // 단일 이미지 폴백 모드에서만 하단 제시어 버튼 영역(y > 850)을 제외한다.
            if (filterBottomButtons && cy > 850) continue
            out.add(FoundObject(label = label, normX = cx.coerceIn(0, 1000), normY = cy.coerceIn(0, 1000)))
        }
        return out
    }

    /**
     * Gemini thinking 응답에 thought 파트가 앞에 섞여 나와도 실제 JSON 배열이 든 마지막 text 파트를 추출한다.
     */
    private fun extractBestPartText(responseBody: String): String? {
        val textKey = Regex(""""text"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        var fallback: String? = null
        var bestWithArray: String? = null
        for (m in textKey.findAll(responseBody)) {
            val unescaped = unescapeJsonString(m.groupValues[1]).trim()
            if (fallback == null) fallback = unescaped
            val start = unescaped.indexOf('[')
            val end = unescaped.lastIndexOf(']')
            if (start >= 0 && end > start) {
                bestWithArray = unescaped
            }
        }
        return bestWithArray ?: fallback
    }

    private fun unescapeJsonString(s: String): String {
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val n = s[i + 1]) {
                    '"', '\\', '/' -> { sb.append(n); i += 2 }
                    'n' -> { sb.append('\n'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    'u' -> {
                        if (i + 5 < s.length) {
                            val code = s.substring(i + 2, i + 6).toIntOrNull(16)
                            if (code != null) { sb.append(code.toChar()); i += 6 }
                            else { sb.append(c); i++ }
                        } else { sb.append(c); i++ }
                    }
                    else -> { sb.append(n); i += 2 }
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun extractJsonArray(raw: String): String? {
        if (raw.startsWith("[")) return raw
        val start = raw.indexOf('[')
        val end = raw.lastIndexOf(']')
        if (start >= 0 && end > start) return raw.substring(start, end + 1)
        return null
    }

    fun normToScreenPx(norm: Int, totalPx: Int): Int =
        ((norm.coerceIn(0, 1000).toLong() * totalPx.coerceAtLeast(0)) / 1000L).toInt()

    /**
     * 비트맵을 긴 변 [maxDim] 이하로 축소한 뒤 JPEG 바이트로 압축한다.
     */
    fun encodeBitmapToBase64Jpeg(
        bitmap: Bitmap,
        crop: CropRect? = null,
        maxDim: Int = MAX_SCENE_DIM,
        quality: Int = JPEG_QUALITY,
    ): String {
        val src = if (crop != null && (crop.x > 0 || crop.y > 0 || crop.w < bitmap.width || crop.h < bitmap.height)) {
            Bitmap.createBitmap(bitmap, crop.x, crop.y, crop.w, crop.h)
        } else {
            bitmap
        }
        val w = src.width
        val h = src.height
        val maxSide = maxOf(w, h)
        val scaled = if (maxSide > maxDim && maxSide > 0) {
            val scale = maxDim.toFloat() / maxSide.toFloat()
            val nw = (w * scale).toInt().coerceAtLeast(1)
            val nh = (h * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(src, nw, nh, true)
        } else {
            src
        }
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        if (scaled !== src) scaled.recycle()
        if (src !== bitmap) src.recycle()
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    /** 모드 진입 시 TLS 핸드셰이크를 미리 맺어 첫 검색 지연을 줄인다. */
    fun prewarm() {
        Thread {
            try {
                val conn = (URL("https://generativelanguage.googleapis.com/").openConnection() as HttpURLConnection).apply {
                    requestMethod = "HEAD"
                    connectTimeout = 3000
                    readTimeout = 3000
                }
                try { conn.responseCode } finally { conn.disconnect() }
            } catch (_: Exception) { }
        }.start()
    }

    private fun fetchModelsJson(apiKey: String): String? {
        val now = System.currentTimeMillis()
        val cached = cachedModelsJson
        if (cached != null && now - cachedModelsFetchedAt < MODELS_CACHE_TTL_MS) return cached
        return try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }
            try {
                if (conn.responseCode !in 200..299) return null
                val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                cachedModelsJson = body
                cachedModelsFetchedAt = now
                body
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun discoverModel(apiKey: String, targetModel: String): String? {
        val body = fetchModelsJson(apiKey) ?: return null
        return pickBestModelForMode(body, targetModel)
    }

    private fun postGenerate(apiKey: String, model: String, reqBody: String): Pair<Int, String> {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        return try {
            conn.outputStream.use { it.write(reqBody.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            code to body
        } finally {
            conn.disconnect()
        }
    }

    private fun resolveTierModel(tier: String): String = when (tier) {
        MODEL_PRO -> cachedProModel ?: MODEL_PRO
        MODEL_LITE -> cachedLiteModel ?: MODEL_LITE
        else -> cachedFlashModel ?: MODEL_FLASH
    }

    private fun cacheTierModel(tier: String, modelId: String) {
        when (tier) {
            MODEL_PRO -> cachedProModel = modelId
            MODEL_LITE -> cachedLiteModel = modelId
            else -> cachedFlashModel = modelId
        }
    }

    private fun thinkingBudgetFor(tier: String): Int = when (tier) {
        MODEL_PRO -> 256
        MODEL_LITE -> 0
        else -> 64
    }

    // 순수 그림 영역만 잘라 보내므로 Flash/Lite는 기본 해상도(~258토큰/장)로도 선명하며 토큰과 지연이 4배 줄어든다.
    private fun highResFor(tier: String): Boolean = tier == MODEL_PRO

    fun analyzeBitmap(bitmap: Bitmap, apiKey: String, model: String = MODEL_FLASH): Result<List<FoundObject>> {
        val keys = parseApiKeys(apiKey)
        if (keys.isEmpty()) return Result.failure(IllegalArgumentException("API key is empty"))
        val safeTarget = when (model) {
            MODEL_PRO -> MODEL_PRO
            MODEL_LITE -> MODEL_LITE
            else -> MODEL_FLASH
        }

        return try {
            val origW = bitmap.width
            val origH = bitmap.height

            // 1. 흰색 그림 액자를 자동 감지해 제시어(Image 1)와 순수 그림(Image 2)을 분리한다.
            val illustrationRect = detectIllustrationRect(bitmap)
            val wordsRect = illustrationRect?.let { computeWordsCrop(it, origW, origH) }
            val useDualCrop = illustrationRect != null && wordsRect != null

            val coordCrop: CropRect
            val buildBody: (includeThinking: Boolean, budget: Int, highRes: Boolean, schema: Boolean, mimeJson: Boolean) -> String
            if (useDualCrop) {
                coordCrop = illustrationRect!!
                val wordsB64 = encodeBitmapToBase64Jpeg(bitmap, wordsRect, MAX_WORDS_DIM, WORDS_JPEG_QUALITY)
                val sceneB64 = encodeBitmapToBase64Jpeg(bitmap, illustrationRect, MAX_SCENE_DIM, JPEG_QUALITY)
                buildBody = { incThink, budget, highRes, schema, mimeJson ->
                    buildDualCropRequestJson(wordsB64, sceneB64, incThink, budget, highRes, schema, mimeJson)
                }
            } else {
                coordCrop = computeGameCrop(origW, origH)
                val singleB64 = encodeBitmapToBase64Jpeg(bitmap, coordCrop, MAX_SCENE_DIM, JPEG_QUALITY)
                buildBody = { incThink, budget, highRes, schema, mimeJson ->
                    buildRequestJson(singleB64, incThink, budget, highRes, schema, mimeJson)
                }
            }

            fun cooldownKey(key: String, modelId: String): String = "${key.hashCode()}:$modelId"

            fun isCooledDown(key: String, modelId: String, nowMs: Long): Boolean =
                (quotaCooldownUntil[cooldownKey(key, modelId)] ?: 0L) > nowMs

            fun markCooldown(key: String, modelId: String, tier: String, code: Int, body: String) {
                val ms = when {
                    code == 429 && (tier == MODEL_PRO || isZeroQuotaError(body)) -> COOLDOWN_ZERO_QUOTA_MS
                    code == 429 && isDailyQuotaError(body) -> COOLDOWN_DAILY_QUOTA_MS
                    else -> COOLDOWN_RATE_LIMIT_MS
                }
                quotaCooldownUntil[cooldownKey(key, modelId)] = System.currentTimeMillis() + ms
            }

            fun executeModel(
                cleanKey: String,
                modelId: String,
                tier: String,
                allowDiscover: Boolean,
            ): Triple<Int, String, String> {
                var active = modelId
                val isGemma = active.lowercase().startsWith("gemma-")
                val budget = if (isGemma) 0 else thinkingBudgetFor(tier)
                val highRes = !isGemma && highResFor(tier)
                var (code, body) = if (isGemma) {
                    postGenerate(cleanKey, active, buildBody(false, 0, false, false, false))
                } else {
                    postGenerate(cleanKey, active, buildBody(true, budget, highRes, true, true))
                }

                if (code == 404 && allowDiscover) {
                    val discovered = discoverModel(cleanKey, tier)
                    if (discovered != null && discovered != active) {
                        active = discovered
                        cacheTierModel(tier, discovered)
                        val retry = postGenerate(cleanKey, active, buildBody(true, budget, highRes, true, true))
                        code = retry.first
                        body = retry.second
                    }
                }

                if (code == 400 && !isGemma) {
                    // mediaResolution만 미지원하는 모델이면 thinking/schema는 유지한 채 먼저 재시도한다.
                    if (highRes && body.lowercase().contains("resolution")) {
                        val retryNoRes = postGenerate(cleanKey, active, buildBody(true, budget, false, true, true))
                        code = retryNoRes.first
                        body = retryNoRes.second
                    }
                    if (code == 400) {
                        val retryPlainJson = postGenerate(cleanKey, active, buildBody(false, 0, false, false, true))
                        code = retryPlainJson.first
                        body = retryPlainJson.second
                    }
                    if (code == 400) {
                        val retryBare = postGenerate(cleanKey, active, buildBody(false, 0, false, false, false))
                        code = retryBare.first
                        body = retryBare.second
                    }
                }
                return Triple(code, body, active)
            }

            fun finishSuccess(cleanKey: String, usedModel: String, body: String): Result<List<FoundObject>> {
                quotaCooldownUntil.remove(cooldownKey(cleanKey, usedModel))
                lastWorkingModelByKey[cleanKey] = usedModel
                val remapped = parseResponse(body, filterBottomButtons = !useDualCrop).map { obj ->
                    val (ox, oy) = remapNormToOriginal(obj.normX, obj.normY, coordCrop, origW, origH)
                    obj.copy(normX = ox, normY = oy)
                }
                return Result.success(remapped)
            }

            val startedAt = System.currentTimeMillis()
            val chain = buildFallbackChain(safeTarget)
            var lastCode = 0
            var lastBody = ""
            var lastQuotaBody: String? = null

            for (cleanKey in keys) {
                val now = System.currentTimeMillis()
                val triedForKey = LinkedHashSet<String>()
                var attemptsOnKey = 0
                var keyAuthFailed = false

                // 직전에 성공했던 모델이 있고 기본 선택 모델이 쿨다운 중이면 성공 모델부터 바로 호출한다.
                val primaryModel = resolveTierModel(chain.first())
                val sticky = lastWorkingModelByKey[cleanKey]
                if (sticky != null && !isCooledDown(cleanKey, sticky, now) && isCooledDown(cleanKey, primaryModel, now)) {
                    triedForKey.add(sticky)
                    attemptsOnKey++
                    val stickyTier = when {
                        sticky.lowercase().contains("pro") -> MODEL_PRO
                        sticky.lowercase().contains("lite") -> MODEL_LITE
                        else -> MODEL_FLASH
                    }
                    val (code, body, usedModel) = executeModel(cleanKey, sticky, stickyTier, allowDiscover = false)
                    triedForKey.add(usedModel)
                    lastCode = code
                    lastBody = body
                    if (code in 200..299) return finishSuccess(cleanKey, usedModel, body)
                    if (code == 429 || code == 503) {
                        if (code == 429) lastQuotaBody = body
                        markCooldown(cleanKey, usedModel, stickyTier, code, body)
                        lastWorkingModelByKey.remove(cleanKey)
                    } else if (code == 401 || code == 403) {
                        keyAuthFailed = true
                    }
                }

                // 기본 모델 체인(Pro -> Flash -> Lite) 중 쿨다운이 아닌 것부터 시도한다.
                if (!keyAuthFailed) {
                    for (tier in chain) {
                        if (attemptsOnKey > 0 && System.currentTimeMillis() - startedAt > FALLBACK_BUDGET_MS) break
                        val active = resolveTierModel(tier)
                        if (isCooledDown(cleanKey, active, System.currentTimeMillis())) continue
                        if (!triedForKey.add(active)) continue
                        attemptsOnKey++

                        val (code, body, usedModel) = executeModel(cleanKey, active, tier, allowDiscover = true)
                        triedForKey.add(usedModel)
                        lastCode = code
                        lastBody = body

                        if (code in 200..299) return finishSuccess(cleanKey, usedModel, body)
                        if (code == 429 || code == 503) {
                            if (code == 429) lastQuotaBody = body
                            markCooldown(cleanKey, usedModel, tier, code, body)
                            markCooldown(cleanKey, active, tier, code, body)
                            continue
                        }
                        if (code == 401 || code == 403) {
                            keyAuthFailed = true
                            break
                        }
                    }
                }

                // 기본 체인이 429/503 또는 모두 쿨다운이면 별도 할당량 버킷(Gemini 3/2.5/2.0 Flash·Lite, Gemma 4/3)을 순서대로 시도한다.
                if (!keyAuthFailed && (attemptsOnKey == 0 || lastCode == 429 || lastCode == 503)) {
                    val modelsJson = fetchModelsJson(cleanKey)
                    val alts = modelsJson?.let { pickAlternateModels(it, triedForKey, maxCount = 6) }.orEmpty()
                    for (alt in alts) {
                        if (attemptsOnKey > 0 && System.currentTimeMillis() - startedAt > FALLBACK_BUDGET_MS) break
                        if (isCooledDown(cleanKey, alt, System.currentTimeMillis())) continue
                        if (!triedForKey.add(alt)) continue
                        attemptsOnKey++

                        val altTier = if (alt.lowercase().contains("lite")) MODEL_LITE else MODEL_FLASH
                        val (code, body, usedModel) = executeModel(cleanKey, alt, altTier, allowDiscover = false)
                        triedForKey.add(usedModel)
                        if (code in 200..299) return finishSuccess(cleanKey, usedModel, body)
                        if (code == 429 || code == 503) {
                            lastCode = code
                            lastBody = body
                            if (code == 429) lastQuotaBody = body
                            markCooldown(cleanKey, usedModel, altTier, code, body)
                        } else if (code == 401 || code == 403) {
                            lastCode = code
                            lastBody = body
                            keyAuthFailed = true
                            break
                        } else if (lastQuotaBody == null) {
                            lastCode = code
                            lastBody = body
                        }
                    }
                }

                // 모든 모델이 이미 쿨다운이라 1번도 못 불렀으면 가장 가벼운 모델 1개만 빠르게 프로브한다(연속 10초 지연 방지).
                if (!keyAuthFailed && attemptsOnKey == 0) {
                    val probeTier = if (safeTarget == MODEL_PRO) MODEL_FLASH else safeTarget
                    val probeModel = sticky ?: resolveTierModel(probeTier)
                    val (code, body, usedModel) = executeModel(cleanKey, probeModel, probeTier, allowDiscover = false)
                    lastCode = code
                    lastBody = body
                    if (code in 200..299) return finishSuccess(cleanKey, usedModel, body)
                    if (code == 429 || code == 503) {
                        if (code == 429) lastQuotaBody = body
                        markCooldown(cleanKey, usedModel, probeTier, code, body)
                    }
                }
            }

            val friendlyErr = when {
                lastQuotaBody != null -> classifyQuotaError(lastQuotaBody)
                lastCode == 429 -> classifyQuotaError(lastBody)
                lastCode == 401 || lastCode == 403 -> "HTTP $lastCode API 키 확인 필요"
                lastCode == 503 -> "HTTP 503 서버 혼잡 (재시도)"
                else -> {
                    val msg = Regex(""""message"\s*:\s*"((?:[^"\\]|\\.)*)"""")
                        .find(lastBody)?.groupValues?.get(1)?.let { unescapeJsonString(it) }.orEmpty()
                    "HTTP $lastCode ${msg.ifEmpty { lastBody.take(100) }}"
                }
            }
            Result.failure(RuntimeException(friendlyErr))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


