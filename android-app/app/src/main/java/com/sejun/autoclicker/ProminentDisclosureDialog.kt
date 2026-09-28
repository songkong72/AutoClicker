package com.sejun.autoclicker

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object ProminentDisclosureDialog {

    private const val PREFS_NAME = "autoclicker_prefs"
    private const val KEY_DISCLOSURE_AGREED = "key_disclosure_agreed"

    fun isAgreed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DISCLOSURE_AGREED, false)
    }

    private fun setAgreed(context: Context, agreed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DISCLOSURE_AGREED, agreed).apply()
    }

    /**
     * 구글 플레이 접근성 API 정책(Accessibility API Policy) 필수 준수 팝업
     */
    fun showIfNeeded(context: Context, onAgreed: () -> Unit, onDeclined: () -> Unit) {
        if (isAgreed(context)) {
            onAgreed()
            return
        }

        val message = """
            [오토클리커 Pro]는 사용자가 지정한 위치에 자동 탭(터치) 제스처를 수행하기 위해 Android 접근성 서비스(AccessibilityService API)를 사용합니다.

            ■ 사용 목적:
            • 게임 및 화면 위에서 지정된 좌표에 자동 클릭 제스처를 수행합니다.

            ■ 데이터 보호 및 보안:
            • 화면의 텍스트나 입력 내용, 계정 및 금융 정보를 일체 수집하거나 읽지 않습니다.
            • 어떠한 개인정보도 기기 외부에 저장하거나 전송하지 않습니다.

            안전하고 편리한 자동 클릭 기능을 위해 위 내용에 동의해 주시기 바랍니다.
        """.trimIndent()

        MaterialAlertDialogBuilder(context)
            .setTitle("🔒 접근성 API 사용 및 개인정보 보호 고지")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("동의하고 계속하기") { dialog, _ ->
                setAgreed(context, true)
                dialog.dismiss()
                onAgreed()
            }
            .setNegativeButton("종료") { dialog, _ ->
                dialog.dismiss()
                onDeclined()
            }
            .show()
    }
}
