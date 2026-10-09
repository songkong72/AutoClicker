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

    /**
     * 보내기 창: 보낼 내용을 미리 보여 주고, 카카오톡으로 바로 보내기를 크게, 복사·다른 앱은 그 아래 작게 둔다.
     * 방 번호·집결장 코드·관리자 코드·관리자 목록이 모두 이 창 하나로 보낸다. [note]는 미리보기 아래의 한 줄 안내.
     */
    fun sheet(activity: Activity, sheetTitle: String, shareTitle: String, text: String, note: String? = null) {
        val sheet = SheetDialog(activity, sheetTitle)
        val lines = text.lines()
        val shown = lines.take(6).joinToString("\n") + if (lines.size > 6) "\n… 외 ${lines.size - 6}줄" else ""
        sheet.line(sheet.card(top = 4), shown, small = true)
        if (note != null) sheet.line(sheet.content, note, small = true, top = 8)
        sheet.wideButton("카카오톡으로 보내기", top = 14) { sheet.dismiss(); toKakao(activity, shareTitle, text) }
        sheet.equalRow(
            sheet.pill("복사") { sheet.dismiss(); copy(activity, shareTitle, text) },
            sheet.pill("다른 앱으로 보내기") { sheet.dismiss(); chooser(activity, shareTitle, text) }
        )
        sheet.actions(SheetDialog.act("닫기")).show()
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
