package com.sejun.autoclicker

import android.content.Context
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object InvitationManager {

    // 암호화용 비밀키 (앱 내부 및 발급 로직 공통)
    private const val SECRET_SALT = "AutoClickerPro_Rally_Secret_2026#!"

    /**
     * 회원 식별자(이메일, ID, 닉네임 등)를 기반으로 1:1 고유 초대코드를 생성합니다.
     * 예시 결과 형태: AC-8F3K9A (사용자 친화적 6자리 영문대문자/숫자)
     */
    fun generateInviteCode(userId: String): String {
        val normalized = userId.trim().lowercase()
        if (normalized.isEmpty()) return ""

        return try {
            val sha256Hmac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(SECRET_SALT.toByteArray(Charsets.UTF_8), "HmacSHA256")
            sha256Hmac.init(secretKey)
            val hashBytes = sha256Hmac.doFinal(normalized.toByteArray(Charsets.UTF_8))

            // 사람이 읽기 쉬운 문자셋 (헷갈리기 쉬운 0, O, 1, I 제외)
            val charPool = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
            val sb = StringBuilder("AC-")
            for (i in 0 until 6) {
                val byteVal = (hashBytes[i].toInt() and 0xFF)
                sb.append(charPool[byteVal % charPool.length])
            }
            sb.toString()
        } catch (e: Exception) {
            // Fallback: MD5 기반 6자리
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest((normalized + SECRET_SALT).toByteArray())
            "AC-" + digest.take(3).joinToString("") { "%02X".format(it) }
        }
    }

    /**
     * 입력된 초대코드가 해당 회원의 고유 코드와 일치하는지 검증합니다.
     * 관리자 마스터 비밀번호가 입력되었을 경우에도 마스터 통과를 지원합니다.
     */
    fun verifyInviteCode(context: Context, userId: String, inputCode: String): Boolean {
        val trimmedCode = inputCode.trim()
        if (trimmedCode.isEmpty() || userId.trim().isEmpty()) return false

        // 1. 관리자 마스터 키 직접 입력 시 마스터 통과
        val masterKey = PreferencesHelper.getAdminMasterKey(context)
        if (trimmedCode == masterKey) {
            return true
        }

        // 2. 이메일/ID 기반 1:1 전용 코드 일치 여부 확인
        val expectedCode = generateInviteCode(userId)
        val cleanInput = trimmedCode.replace("-", "").uppercase()
        val cleanExpected = expectedCode.replace("-", "").uppercase()

        return cleanInput == cleanExpected
    }

    /**
     * 관리자 마스터 비밀번호 검증
     */
    fun checkAdminPassword(context: Context, inputPass: String): Boolean {
        val currentPass = PreferencesHelper.getAdminMasterKey(context)
        return inputPass.trim() == currentPass
    }

    /**
     * 관리자 마스터 비밀번호 변경
     */
    fun updateAdminPassword(context: Context, newPass: String): Boolean {
        if (newPass.trim().length < 4) return false
        PreferencesHelper.setAdminMasterKey(context, newPass.trim())
        return true
    }
}
