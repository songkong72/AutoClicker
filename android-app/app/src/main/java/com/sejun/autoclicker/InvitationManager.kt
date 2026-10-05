package com.sejun.autoclicker

import android.content.Context

/**
 * 초대코드 발급/검증. 비밀 값은 코드에 없고 빌드 때 BuildConfig로 주입된다
 * (android-app/local.properties 의 invite.secret). 관리자는 비밀번호가 아니라 서버 명단(AdminRoster)으로 정한다.
 */
object InvitationManager {

    private val secret: String get() = BuildConfig.INVITE_SECRET

    /** 회원 식별자 기반 1:1 초대코드. 비밀 문자열이 주입되지 않은 빌드에서는 빈 문자열(발급 불가). */
    fun generateInviteCode(userId: String): String = InviteCodes.generate(userId, secret)

    /** 입력된 초대코드가 해당 회원의 코드일 때만 통과. */
    fun verifyInviteCode(context: Context, userId: String, inputCode: String): Boolean {
        val code = inputCode.trim()
        if (code.isEmpty() || userId.trim().isEmpty()) return false
        return InviteCodes.verify(userId, code, secret)
    }
}
