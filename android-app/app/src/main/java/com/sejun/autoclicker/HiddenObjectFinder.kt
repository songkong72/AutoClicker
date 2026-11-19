package com.sejun.autoclicker

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class FoundObject(
    val label: String,
    val normX: Int,
    val normY: Int,
)

/**
 * 숨은그림찾기 화면을 Gemini Vision API로 빠르게 분석해
 * 하단 제시어의 상단 그림 내 중심 좌표(0~1000 정규화)를 추출한다.
 */
internal object HiddenObjectFinder {
    const val MODEL_FLASH = "gemini-flash-latest"
    const val MODEL_LITE = "gemini-flash-lite-latest"

    private const val MAX_IMAGE_DIM = 1024
    private const val JPEG_QUALITY = 80
    private const val CONNECT_TIMEOUT_MS = 4000
    private const val READ_TIMEOUT_MS = 10000

    @Volatile private var cachedLiteModel: String? = null
    @Volatile private var cachedFlashModel: String? = null

    private const val PROMPT =
        "Hidden-object game screen. Look at the target word buttons at the bottom (y > 800). " +
            "Pick up to 3 target words starting from the BACK (the LAST/bottom-most row, or rightmost words if there is only 1 row). " +
            "Carefully inspect the upper illustration ONLY (ymin >= 80, ymax <= 790) to find each exact item—do NOT guess randomly or mark bottom text buttons. " +
            "Return JSON array [{\\\"label\\\":\\\"word\\\",\\\"box_2d\\\":[ymin,xmin,ymax,xmax]}] with 0-1000 coordinates."

    fun buildRequestJson(base64Jpeg: String, includeThinkingZero: Boolean = true): String {
        val cleanB64 = base64Jpeg.replace("\r", "").replace("\n", "")
        val thinkingPart = if (includeThinkingZero) ""","thinkingConfig":{"thinkingBudget":256}""" else ""
        return """{"contents":[{"parts":[{"text":"$PROMPT"},{"inline_data":{"mime_type":"image/jpeg","data":"$cleanB64"}}]}],"generationConfig":{"temperature":0.0,"maxOutputTokens":450,"responseMimeType":"application/json"$thinkingPart}}"""
    }

    /**
     * `v1beta/models` 응답에서 generateContent를 지원하는 가장 적합한 Flash 모델을 고른다.
     */
    fun pickBestModelFromList(modelsJson: String, preferLite: Boolean): String? {
        val blockRegex = Regex("""\{[^{}]*"name"\s*:\s*"models/([^"]+)"[^{}]*"supportedGenerationMethods"\s*:\s*\[([^\]]*)\][^{}]*\}""")
        val candidates = ArrayList<String>()
        for (m in blockRegex.findAll(modelsJson)) {
            val id = m.groupValues[1].trim()
            val methods = m.groupValues[2]
            if (!methods.contains("generateContent")) continue
            val lower = id.lowercase()
            if (!lower.contains("flash")) continue
            // 임베딩·라이브·이미지전용·TTS 모델 제외
            if (lower.contains("tts") || lower.contains("image") || lower.contains("live") || lower.contains("audio")) continue
            candidates.add(id)
        }
        if (candidates.isEmpty()) return null
        val sorted = candidates.sortedDescending()
        return if (preferLite) {
            sorted.firstOrNull { it.lowercase().contains("lite") } ?: sorted.first()
        } else {
            sorted.firstOrNull { !it.lowercase().contains("lite") } ?: sorted.first()
        }
    }

    fun parseResponse(responseBody: String): List<FoundObject> {
        val rawText = extractFirstPartText(responseBody) ?: return emptyList()
        val jsonArray = extractJsonArray(rawText) ?: return emptyList()
        val itemRegex = Regex("""\{[^{}]*\}""")
        val labelRegex = Regex(""""label"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        val boxRegex = Regex(""""box_2d"\s*:\s*\[\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*\]""")
        val xRegex = Regex(""""x"\s*:\s*(-?\d+)""")
        val yRegex = Regex(""""y"\s*:\s*(-?\d+)""")

        val out = ArrayList<FoundObject>()
        for (match in itemRegex.findAll(jsonArray)) {
            val block = match.value
            val labelRaw = labelRegex.find(block)?.groupValues?.get(1) ?: continue
            val label = unescapeJsonString(labelRaw).trim()
            if (label.isEmpty()) continue

            val boxMatch = boxRegex.find(block)
            val (cx, cy) = if (boxMatch != null) {
                val ymin = boxMatch.groupValues[1].toIntOrNull() ?: continue
                val xmin = boxMatch.groupValues[2].toIntOrNull() ?: continue
                val ymax = boxMatch.groupValues[3].toIntOrNull() ?: continue
                val xmax = boxMatch.groupValues[4].toIntOrNull() ?: continue
                // 방 전체 등 지나치게 큰 박스는 오답으로 제외한다.
                if (ymax - ymin > 450 || xmax - xmin > 450) continue
                ((xmin + xmax) / 2) to ((ymin + ymax) / 2)
            } else {
                val x = xRegex.find(block)?.groupValues?.get(1)?.toIntOrNull() ?: continue
                val y = yRegex.find(block)?.groupValues?.get(1)?.toIntOrNull() ?: continue
                x to y
            }

            // 하단 제시어 버튼 영역(y > 800)은 실제 그림이 아니므로 제외한다.
            if (cy > 800) continue
            out.add(FoundObject(label = label, normX = cx.coerceIn(0, 1000), normY = cy.coerceIn(0, 1000)))
        }
        return out
    }

    private fun extractFirstPartText(responseBody: String): String? {
        val textKey = Regex(""""text"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        val raw = textKey.find(responseBody)?.groupValues?.get(1) ?: return null
        return unescapeJsonString(raw).trim()
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
     * 비트맵을 긴 변 [MAX_IMAGE_DIM] 이하로 축소한 뒤 JPEG 바이트로 압축한다.
     * 업로드 용량을 수십 KB로 줄여 응답 속도를 높인다.
     */
    fun encodeBitmapToBase64Jpeg(bitmap: Bitmap): String {
        val w = bitmap.width
        val h = bitmap.height
        val maxSide = maxOf(w, h)
        val scaled = if (maxSide > MAX_IMAGE_DIM && maxSide > 0) {
            val scale = MAX_IMAGE_DIM.toFloat() / maxSide.toFloat()
            val nw = (w * scale).toInt().coerceAtLeast(1)
            val nh = (h * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, nw, nh, false)
        } else {
            bitmap
        }
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos)
        if (scaled !== bitmap) scaled.recycle()
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

    private fun discoverModel(apiKey: String, preferLite: Boolean): String? {
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
                pickBestModelFromList(body, preferLite)
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            null
        }
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

    fun analyzeBitmap(bitmap: Bitmap, apiKey: String, model: String = MODEL_LITE): Result<List<FoundObject>> {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) return Result.failure(IllegalArgumentException("API key is empty"))
        val preferLite = model != MODEL_FLASH
        var activeModel = if (preferLite) (cachedLiteModel ?: MODEL_LITE) else (cachedFlashModel ?: MODEL_FLASH)

        return try {
            val base64 = encodeBitmapToBase64Jpeg(bitmap)
            var (code, body) = postGenerate(cleanKey, activeModel, buildRequestJson(base64, includeThinkingZero = true))

            // 404면 사용 가능한 최신 Flash 모델을 자동 조회해 캐시 후 재시도한다.
            if (code == 404) {
                val discovered = discoverModel(cleanKey, preferLite)
                if (discovered != null && discovered != activeModel) {
                    activeModel = discovered
                    if (preferLite) cachedLiteModel = discovered else cachedFlashModel = discovered
                    val retry = postGenerate(cleanKey, activeModel, buildRequestJson(base64, includeThinkingZero = true))
                    code = retry.first
                    body = retry.second
                }
            }

            // 400이면 thinkingConfig 미지원 모델일 수 있으므로 해당 필드를 빼고 1회 재시도한다.
            if (code == 400) {
                val retry = postGenerate(cleanKey, activeModel, buildRequestJson(base64, includeThinkingZero = false))
                code = retry.first
                body = retry.second
            }

            if (code !in 200..299) {
                val msg = Regex(""""message"\s*:\s*"((?:[^"\\]|\\.)*)"""")
                    .find(body)?.groupValues?.get(1)?.let { unescapeJsonString(it) }.orEmpty()
                Result.failure(RuntimeException("HTTP $code ${msg.ifEmpty { body.take(120) }}"))
            } else {
                Result.success(parseResponse(body))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
