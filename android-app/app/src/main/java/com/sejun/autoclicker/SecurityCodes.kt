package com.sejun.autoclicker

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** 초대코드 계산/검증. 비밀 문자열은 코드에 두지 않고 빌드 때 주입한다(안드로이드 의존성 없음). */
object InviteCodes {
    private const val POOL = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ" // 0, O, 1, I 제외

    fun generate(userId: String, secret: String): String {
        val normalized = userId.trim().lowercase()
        if (normalized.isEmpty() || secret.isEmpty()) return ""
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val hash = mac.doFinal(normalized.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder("AC-")
        for (i in 0 until 6) sb.append(POOL[(hash[i].toInt() and 0xFF) % POOL.length])
        return sb.toString()
    }

    fun verify(userId: String, input: String, secret: String): Boolean {
        val expected = generate(userId, secret)
        if (expected.isEmpty()) return false
        val a = input.trim().replace("-", "").uppercase()
        val b = expected.replace("-", "").uppercase()
        return a.isNotEmpty() && MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
    }
}
