package com.sejun.autoclicker

import java.net.URLEncoder

/** Firebase 익명 로그인으로 받은 토큰. */
data class AuthSession(val idToken: String, val refreshToken: String, val expiresAtMs: Long) {
    /** 만료 5분 전부터 갱신한다. */
    fun needsRefresh(nowMs: Long) = nowMs >= expiresAtMs - 300_000L
}

/** REST 응답 해석과 URL 만들기(네트워크 없음). */
object FirebaseAuthCodec {
    /** signUp 응답(idToken…)과 토큰 갱신 응답(id_token…)을 모두 받는다. 실패면 null. */
    fun parseSession(text: String, nowMs: Long): AuthSession? {
        val id = field(text, "idToken") ?: field(text, "id_token")
        val rf = field(text, "refreshToken") ?: field(text, "refresh_token")
        if (id.isNullOrEmpty() || rf.isNullOrEmpty()) return null
        val sec = (field(text, "expiresIn") ?: field(text, "expires_in"))?.toLongOrNull() ?: 3600L
        return AuthSession(id, rf, nowMs + sec * 1000L)
    }

    /** 평평한 JSON에서 문자열 값 하나를 꺼낸다(토큰은 따옴표/역슬래시를 포함하지 않는다). */
    private fun field(text: String, key: String): String? =
        Regex("\"" + key + "\"\\s*:\\s*\"([^\"\\\\]*)\"").find(text)?.groupValues?.get(1)

    fun withAuth(url: String, token: String?): String {
        if (token == null) return url
        val sep = if (url.contains('?')) "&" else "?"
        return url + sep + "auth=" + URLEncoder.encode(token, "UTF-8")
    }
}
