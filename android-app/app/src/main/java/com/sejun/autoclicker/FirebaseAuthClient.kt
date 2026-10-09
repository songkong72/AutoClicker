package com.sejun.autoclicker

import java.net.HttpURLConnection
import java.net.URL

/**
 * Firebase 익명 로그인(REST). 사용자가 로그인하는 것이 아니라, "이 앱이 보낸 요청"임을 증명하는 토큰을 받는다.
 * apiKey가 비어 있으면 토큰 없이(null) 동작한다. 이 경우 DB 규칙이 열려 있어야 한다.
 * 갱신 토큰은 [load]/[save]로 기기에 보관해 실행할 때마다 새 계정이 쌓이지 않게 한다.
 */
class FirebaseAuthClient(
    private val apiKey: String,
    private val load: () -> String?,
    private val save: (String) -> Unit
) {
    @Volatile private var session: AuthSession? = null
    val enabled: Boolean get() = apiKey.isNotBlank()

    /** 유효한 idToken. 불가능하면 null(그대로 인증 없이 시도). 네트워크를 쓰므로 백그라운드 스레드에서 부른다. */
    @Synchronized fun token(nowMs: Long = System.currentTimeMillis()): String? {
        if (!enabled) return null
        val s = session
        if (s != null && !s.needsRefresh(nowMs)) return s.idToken
        val fresh = try {
            val saved = s?.refreshToken ?: load()
            (if (!saved.isNullOrEmpty()) refresh(saved, nowMs) else null) ?: signUp(nowMs)
        } catch (e: Exception) { null }
        if (fresh != null) { session = fresh; save(fresh.refreshToken) }
        return fresh?.idToken ?: s?.idToken
    }

    /** 이 기기의 Firebase 익명 사용자 ID. 서버 지휘관 명단에서 이 폰을 가리키는 값이다. 받지 못하면 null. 백그라운드 스레드에서 부른다. */
    fun uid(): String? {
        token()
        return session?.uid?.takeIf { it.isNotEmpty() }
    }

    /** 서버가 토큰을 거절(401)하면 다음 요청에서 새로 받게 한다. */
    @Synchronized fun invalidate() { session = null }

    private fun signUp(nowMs: Long): AuthSession? =
        post("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey",
            "application/json", """{"returnSecureToken":true}""", nowMs)

    private fun refresh(refreshToken: String, nowMs: Long): AuthSession? =
        post("https://securetoken.googleapis.com/v1/token?key=$apiKey",
            "application/x-www-form-urlencoded",
            "grant_type=refresh_token&refresh_token=" + java.net.URLEncoder.encode(refreshToken, "UTF-8"), nowMs)

    private fun post(url: String, type: String, body: String, nowMs: Long): AuthSession? {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.setRequestProperty("Content-Type", type)
        c.connectTimeout = 4000; c.readTimeout = 4000
        c.doOutput = true
        c.outputStream.use { it.write(body.toByteArray()) }
        if (c.responseCode !in 200..299) return null
        return FirebaseAuthCodec.parseSession(c.inputStream.bufferedReader().use { it.readText() }, nowMs)
    }
}
