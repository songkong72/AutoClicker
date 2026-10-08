package com.sejun.autoclicker

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

/** 글을 복사하거나 다른 앱(카카오톡 등)으로 보낸다. */
object TextShare {
    private const val KAKAO = "com.kakao.talk"

    fun copy(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        copiedNotice(context)
    }

    /** 안드로이드 13부터는 복사하면 시스템이 직접 알려 준다. 그 아래 버전에서만 앱이 알린다(알림이 두 번 뜨지 않게). */
    fun copiedNotice(context: Context) {
        if (android.os.Build.VERSION.SDK_INT < 33) Toast.makeText(context, "📋 복사했어요", Toast.LENGTH_SHORT).show()
    }

    private fun sendIntent(text: String) = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }

    /** 카카오톡의 "보낼 대화방 고르기"로 바로 연다. 카카오톡이 없으면 앱 고르기 창을 띄운다. */
    fun toKakao(activity: Activity, title: String, text: String) {
        try {
            activity.startActivity(sendIntent(text).setPackage(KAKAO))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(activity, "카카오톡이 없어요. 보낼 앱을 골라 주세요", Toast.LENGTH_SHORT).show()
            chooser(activity, title, text)
        }
    }

    /** 문자·메일·메모 등 설치된 앱 중에서 골라 보낸다. */
    fun chooser(activity: Activity, title: String, text: String) {
        try { activity.startActivity(Intent.createChooser(sendIntent(text), title)) }
        catch (_: ActivityNotFoundException) { Toast.makeText(activity, "보낼 수 있는 앱이 없어요", Toast.LENGTH_SHORT).show() }
    }
}
