package com.sejun.autoclicker

import android.content.Context

/**
 * 초대코드 발급/검증과 관리자 비밀번호. 비밀 값은 코드에 없고 빌드 때 BuildConfig로 주입된다
 * (android-app/local.properties 의 invite.secret, admin.password.hash).
 */
object InvitationManager {

    private const val MIN_ADMIN_PASSWORD = 6

    private val secret: String get() = BuildConfig.INVITE_SECRET

    /** 회원 식별자 기반 1:1 초대코드. 비밀 문자열이 주입되지 않은 빌드에서는 빈 문자열(발급 불가). */
    fun generateInviteCode(userId: String): String = InviteCodes.generate(userId, secret)

    /** 입력된 초대코드가 해당 회원의 코드이거나, 관리자 비밀번호가 입력됐을 때 통과. */
    fun verifyInviteCode(context: Context, userId: String, inputCode: String): Boolean {
        val code = inputCode.trim()
        if (code.isEmpty() || userId.trim().isEmpty()) return false
        if (checkAdminPassword(context, code)) return true
        return InviteCodes.verify(userId, code, secret)
    }

    fun checkAdminPassword(context: Context, inputPass: String): Boolean =
        AdminAuth.matches(inputPass.trim(), PreferencesHelper.getAdminPasswordHash(context), secret)

    /** 이 기기의 관리자 비밀번호를 바꾼다(해시로 저장). 비밀 문자열이 없는 빌드에서는 바꿀 수 없다. */
    fun updateAdminPassword(context: Context, newPass: String): Boolean {
        val p = newPass.trim()
        if (p.length < MIN_ADMIN_PASSWORD || secret.isEmpty()) return false
        PreferencesHelper.setAdminPasswordHash(context, AdminAuth.hash(p, secret))
        return true
    }

    fun minAdminPasswordLength(): Int = MIN_ADMIN_PASSWORD
}
