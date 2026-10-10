package com.sejun.autoclicker

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** 초대코드 계산/검증. 비밀 문자열은 코드에 두지 않고 빌드 때 주입한다(안드로이드 의존성 없음). */
object InviteCodes {
    private const val POOL = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ" // 0, O, 1, I 제외

    /**
     * [group]("2000-WBI")을 주면 그 소속에서만 맞는 코드가 된다(집결장이 지휘관의 소속을 물려받는다).
     * 소속의 대문자·소문자는 그대로 구분한다. 비워 두면 소속이 없던 예전 방식의 코드다.
     */
    fun generate(userId: String, secret: String, group: String = ""): String {
        val normalized = userId.trim().lowercase()
        if (normalized.isEmpty() || secret.isEmpty()) return ""
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val message = if (group.isEmpty()) normalized else normalized + "\n" + group
        val hash = mac.doFinal(message.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder("AC-")
        for (i in 0 until 6) sb.append(POOL[(hash[i].toInt() and 0xFF) % POOL.length])
        return sb.toString()
    }

    fun verify(userId: String, input: String, secret: String, group: String = ""): Boolean {
        val expected = generate(userId, secret, group)
        if (expected.isEmpty()) return false
        val a = input.trim().replace("-", "").uppercase()
        val b = expected.replace("-", "").uppercase()
        return a.isNotEmpty() && MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
    }
}
