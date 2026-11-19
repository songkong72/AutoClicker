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
    const val MODEL_FLASH = "gemini-2.5-flash"
    const val MODEL_LITE = "gemini-2.5-flash-lite"

    private const val MAX_IMAGE_DIM = 512
    private const val JPEG_QUALITY = 60
    private const val CONNECT_TIMEOUT_MS = 4000
    private const val READ_TIMEOUT_MS = 8000

    private const val PROMPT =
        "Find bottom target words in the picture above. " +
            "Return JSON array [{\\\"label\\\":\\\"name\\\",\\\"x\\\":0-1000,\\\"y\\\":0-1000}]."

    fun buildRequestJson(base64Jpeg: String): String {
        val cleanB64 = base64Jpeg.replace("\r", "").replace("\n", "")
        // 제한시간이 짧은 게임이므로 thinkingBudget=0, maxOutputTokens=160으로 지연을 최소화한다.
        return """{"contents":[{"parts":[{"text":"$PROMPT"},{"inline_data":{"mime_type":"image/jpeg","data":"$cleanB64"}}]}],"generationConfig":{"temperature":0.0,"maxOutputTokens":160,"responseMimeType":"application/json","thinkingConfig":{"thinkingBudget":0}}}"""
    }

    fun parseResponse(responseBody: String): List<FoundObject> {
        val rawText = extractFirstPartText(responseBody) ?: return emptyList()
        val jsonArray = extractJsonArray(rawText) ?: return emptyList()
        val itemRegex = Regex("""\{[^{}]*\}""")
        val labelRegex = Regex(""""label"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        val xRegex = Regex(""""x"\s*:\s*(-?\d+)""")
        val yRegex = Regex(""""y"\s*:\s*(-?\d+)""")

        val out = ArrayList<FoundObject>()
        for (match in itemRegex.findAll(jsonArray)) {
            val block = match.value
            val labelRaw = labelRegex.find(block)?.groupValues?.get(1) ?: continue
            val label = unescapeJsonString(labelRaw).trim()
            val x = xRegex.find(block)?.groupValues?.get(1)?.toIntOrNull() ?: continue
            val y = yRegex.find(block)?.groupValues?.get(1)?.toIntOrNull() ?: continue
            if (label.isEmpty()) continue
            out.add(FoundObject(label = label, normX = x.coerceIn(0, 1000), normY = y.coerceIn(0, 1000)))
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

    fun analyzeBitmap(bitmap: Bitmap, apiKey: String, model: String = MODEL_LITE): Result<List<FoundObject>> {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) return Result.failure(IllegalArgumentException("API key is empty"))
        val safeModel = if (model == MODEL_FLASH) MODEL_FLASH else MODEL_LITE

        return try {
            val base64 = encodeBitmapToBase64Jpeg(bitmap)
            val reqBody = buildRequestJson(base64)
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$safeModel:generateContent?key=$cleanKey")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            try {
                conn.outputStream.use { it.write(reqBody.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    val msg = Regex(""""message"\s*:\s*"((?:[^"\\]|\\.)*)"""")
                        .find(body)?.groupValues?.get(1)?.let { unescapeJsonString(it) }.orEmpty()
                    Result.failure(RuntimeException("HTTP $code ${msg.ifEmpty { body.take(120) }}"))
                } else {
                    Result.success(parseResponse(body))
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
