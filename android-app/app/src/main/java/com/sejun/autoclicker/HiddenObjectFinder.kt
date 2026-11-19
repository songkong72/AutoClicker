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
    const val MODEL_PRO = "gemini-pro-latest"

    private const val MAX_IMAGE_DIM = 1024
    private const val JPEG_QUALITY = 80
    private const val CONNECT_TIMEOUT_MS = 4000
    private const val READ_TIMEOUT_MS = 15000

    @Volatile private var cachedLiteModel: String? = null
    @Volatile private var cachedFlashModel: String? = null
    @Volatile private var cachedProModel: String? = null

    data class CropRect(val x: Int, val y: Int, val w: Int, val h: Int)

    /**
     * 폴드/태블릿처럼 가로가 넓은 화면에서는 양옆의 눈 내리는 배경 여백을 잘라내고
     * 가운데 게임 카드 영역만 남겨 작은 사물 화질을 높인다.
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

    fun remapNormToOriginal(normX: Int, normY: Int, crop: CropRect, origW: Int, origH: Int): Pair<Int, Int> {
        if (origW <= 0 || origH <= 0) return normX to normY
        val ox = ((crop.x + normX.coerceIn(0, 1000) * crop.w / 1000.0) * 1000.0 / origW).toInt().coerceIn(0, 1000)
        val oy = ((crop.y + normY.coerceIn(0, 1000) * crop.h / 1000.0) * 1000.0 / origH).toInt().coerceIn(0, 1000)
        return ox to oy
    }

    private const val PROMPT =
        "Hidden-object game screen. Look at the target word buttons at the bottom (y > 790). " +
            "Pick up to 3 target words starting from the BACK (the LAST/bottom-most row, or rightmost words if there is only 1 row). " +
            "Carefully inspect the upper illustration ONLY (ymin >= 20, ymax <= 785) to find each exact item—do NOT guess randomly or mark bottom text buttons. " +
            "Return JSON array [{\\\"label\\\":\\\"word\\\",\\\"box_2d\\\":[ymin,xmin,ymax,xmax]}] with 0-1000 coordinates."

    fun buildRequestJson(base64Jpeg: String, includeThinkingZero: Boolean = true): String {
        val cleanB64 = base64Jpeg.replace("\r", "").replace("\n", "")
        val thinkingPart = if (includeThinkingZero) ""","thinkingConfig":{"thinkingBudget":256}""" else ""
        return """{"contents":[{"parts":[{"text":"$PROMPT"},{"inline_data":{"mime_type":"image/jpeg","data":"$cleanB64"}}]}],"generationConfig":{"temperature":0.0,"maxOutputTokens":450,"responseMimeType":"application/json"$thinkingPart}}"""
    }

    fun pickBestModelFromList(modelsJson: String, preferLite: Boolean): String? =
        pickBestModelForMode(modelsJson, if (preferLite) MODEL_LITE else MODEL_FLASH)

    /**
     * `v1beta/models` 응답에서 generateContent를 지원하는 가장 적합한 모델(Flash / Lite / Pro)을 고른다.
     */
    fun pickBestModelForMode(modelsJson: String, targetModel: String): String? {
        val blockRegex = Regex("""\{[^{}]*"name"\s*:\s*"models/([^"]+)"[^{}]*"supportedGenerationMethods"\s*:\s*\[([^\]]*)\][^{}]*\}""")
        val wantPro = targetModel == MODEL_PRO
        val wantLite = targetModel == MODEL_LITE
        val candidates = ArrayList<String>()
        for (m in blockRegex.findAll(modelsJson)) {
            val id = m.groupValues[1].trim()
            val methods = m.groupValues[2]
            if (!methods.contains("generateContent")) continue
            val lower = id.lowercase()
            if (lower.contains("tts") || lower.contains("image") || lower.contains("live") || lower.contains("audio")) continue
            if (wantPro) {
                if (lower.contains("pro")) candidates.add(id)
            } else {
                if (lower.contains("flash")) candidates.add(id)
            }
        }
        if (candidates.isEmpty()) return null
        val sorted = candidates.sortedDescending()
        return when {
            wantPro -> sorted.first()
            wantLite -> sorted.firstOrNull { it.lowercase().contains("lite") } ?: sorted.first()
            else -> sorted.firstOrNull { !it.lowercase().contains("lite") } ?: sorted.first()
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
    fun encodeBitmapToBase64Jpeg(bitmap: Bitmap, crop: CropRect? = null): String {
        val src = if (crop != null && (crop.x > 0 || crop.y > 0 || crop.w < bitmap.width || crop.h < bitmap.height)) {
            Bitmap.createBitmap(bitmap, crop.x, crop.y, crop.w, crop.h)
        } else {
            bitmap
        }
        val w = src.width
        val h = src.height
        val maxSide = maxOf(w, h)
        val scaled = if (maxSide > MAX_IMAGE_DIM && maxSide > 0) {
            val scale = MAX_IMAGE_DIM.toFloat() / maxSide.toFloat()
            val nw = (w * scale).toInt().coerceAtLeast(1)
            val nh = (h * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(src, nw, nh, true)
        } else {
            src
        }
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos)
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

    private fun discoverModel(apiKey: String, targetModel: String): String? {
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
                pickBestModelForMode(body, targetModel)
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

    fun analyzeBitmap(bitmap: Bitmap, apiKey: String, model: String = MODEL_FLASH): Result<List<FoundObject>> {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) return Result.failure(IllegalArgumentException("API key is empty"))
        val safeTarget = when (model) {
            MODEL_PRO -> MODEL_PRO
            MODEL_LITE -> MODEL_LITE
            else -> MODEL_FLASH
        }
        var activeModel = when (safeTarget) {
            MODEL_PRO -> cachedProModel ?: MODEL_PRO
            MODEL_LITE -> cachedLiteModel ?: MODEL_LITE
            else -> cachedFlashModel ?: MODEL_FLASH
        }

        return try {
            val origW = bitmap.width
            val origH = bitmap.height
            val crop = computeGameCrop(origW, origH)
            val base64 = encodeBitmapToBase64Jpeg(bitmap, crop)
            var (code, body) = postGenerate(cleanKey, activeModel, buildRequestJson(base64, includeThinkingZero = true))

            if (code == 404) {
                val discovered = discoverModel(cleanKey, safeTarget)
                if (discovered != null && discovered != activeModel) {
                    activeModel = discovered
                    when (safeTarget) {
                        MODEL_PRO -> cachedProModel = discovered
                        MODEL_LITE -> cachedLiteModel = discovered
                        else -> cachedFlashModel = discovered
                    }
                    val retry = postGenerate(cleanKey, activeModel, buildRequestJson(base64, includeThinkingZero = true))
                    code = retry.first
                    body = retry.second
                }
            }

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
                val remapped = parseResponse(body).map { obj ->
                    val (ox, oy) = remapNormToOriginal(obj.normX, obj.normY, crop, origW, origH)
                    obj.copy(normX = ox, normY = oy)
                }
                Result.success(remapped)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
