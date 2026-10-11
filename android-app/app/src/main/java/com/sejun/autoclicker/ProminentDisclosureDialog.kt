package com.sejun.autoclicker

import android.app.Activity
import android.content.Context

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
    fun showIfNeeded(context: Activity, onAgreed: () -> Unit, onDeclined: () -> Unit) {
        if (isAgreed(context)) {
            onAgreed()
            return
        }

        val message = T.a.discBody

        // 동의하거나 종료를 눌러야만 닫힌다(바깥, 뒤로 가기, 아래로 밀기로는 닫히지 않는다).
        val sheet = SheetDialog(context, T.a.discTitle, cancelable = false)
        sheet.line(sheet.content, message).apply { textSize = 14f; setLineSpacing(0f, 1.3f) }
        sheet.actions(
            SheetDialog.act(T.a.quit) { onDeclined() },
            SheetDialog.act(T.a.agree, SheetDialog.Kind.PRIMARY) {
                setAgreed(context, true)
                onAgreed()
            }
        ).show()
    }
}
