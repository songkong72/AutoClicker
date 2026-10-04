package com.sejun.autoclicker

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 개발자 전용 "관리자 관리" 화면과 "내 기기 ID" 안내. 화면은 코드로 만든다. 서버 통신은 백그라운드 스레드에서 한다. */
internal class AdminRosterUi(private val activity: Activity, private val server: AdminServer) {

    private var manageDialog: AlertDialog? = null

    private fun ui(block: () -> Unit) = activity.runOnUiThread(block)
    private fun toast(msg: String) = Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
    private fun dp(v: Int) = (v * activity.resources.displayMetrics.density).toInt()

    /** 밝은/어두운 테마에 맞는 글자색을 테마에서 가져온다. 색을 고정하면 어두운 화면에서 글자가 묻힌다. */
    private fun themeColor(attr: Int): Int {
        val tv = TypedValue()
        activity.theme.resolveAttribute(attr, tv, true)
        return if (tv.resourceId != 0) activity.getColor(tv.resourceId) else tv.data
    }

    private fun copy(label: String, text: String) {
        val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        toast("📋 복사했어요")
    }

    /** 이 기기의 서버 ID를 보여 준다. 개발자로 등록할 때 Firebase 콘솔에 적는 값이다. */
    fun showMyId() {
        Thread {
            val r = server.uid()
            ui {
                val id = r.value
                if (id == null) { toast(r.error ?: "기기 ID를 받지 못했어요"); return@ui }
                val tv = TextView(activity).apply {
                    text = id
                    textSize = 15f
                    setTextColor(themeColor(android.R.attr.textColorPrimary))
                    setTextIsSelectable(true)
                    setPadding(dp(20), dp(8), dp(20), dp(8))
                }
                AlertDialog.Builder(activity)
                    .setTitle("내 기기 ID")
                    .setMessage("개발자로 등록하려면 이 ID를 Firebase 콘솔의 owners 아래에 적습니다. 앱을 지우고 다시 설치하면 ID가 바뀝니다.")
                    .setView(tv)
                    .setPositiveButton("복사") { _, _ -> copy("기기 ID", id) }
                    .setNegativeButton("닫기", null)
                    .show()
            }
        }.start()
    }

    /** 개발자인지 서버에서 확인하고, 맞으면 관리자 관리 화면을 연다. */
    fun showManage() {
        toast("서버를 확인하는 중…")
        Thread {
            val me = server.uid()
            val uid = me.value
            if (uid == null) { ui { toast(me.error ?: "기기 ID를 받지 못했어요") }; return@Thread }
            when (server.isOwner(uid)) {
                Check.YES -> load()
                Check.NO -> ui { toast("개발자만 쓸 수 있어요. 개발자는 Firebase 콘솔에 이 기기 ID를 적어야 해요 (관리자 메뉴 → 내 기기 ID 보기)") }
                Check.UNKNOWN -> ui { toast("서버에서 확인하지 못했어요. 인터넷 연결과 서버 규칙 적용 여부를 확인해 주세요") }
            }
        }.start()
    }

    private fun load() {
        val admins = server.listAdmins()
        val codes = server.listCodes()
        ui {
            val a = admins.value
            val c = codes.value
            if (a == null || c == null) { toast(admins.error ?: codes.error ?: "목록을 불러오지 못했어요"); return@ui }
            render(a, c)
        }
    }

    private fun render(admins: List<AdminEntry>, codes: List<AdminCode>) {
        manageDialog?.dismiss()
        val now = System.currentTimeMillis()
        val labels = codes.associate { it.code to it.name }
        val pending = codes.filter { !it.used && now < it.expiresAt }
        val day = SimpleDateFormat("MM/dd", Locale.KOREA)

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        fun heading(text: String) = TextView(activity).apply {
            this.text = text; textSize = 15f; setTypeface(typeface, Typeface.BOLD)
            setTextColor(themeColor(android.R.attr.textColorPrimary))
            setPadding(0, dp(16), 0, dp(6))
        }.also { root.addView(it) }
        fun line(text: String) = TextView(activity).apply {
            this.text = text; textSize = 14f
            setTextColor(themeColor(android.R.attr.textColorPrimary))
            setPadding(0, dp(4), 0, 0)
        }
        fun buttons(vararg b: Button) = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END
            b.forEach { addView(it) }
        }
        fun button(text: String, onClick: () -> Unit) = Button(activity).apply {
            this.text = text; textSize = 12f; setOnClickListener { onClick() }
        }

        root.addView(button("+ 관리자 코드 만들기") { askName() })

        heading("등록된 관리자 (${admins.size}명)")
        if (admins.isEmpty()) root.addView(line("아직 없어요."))
        for (a in admins) {
            val label = labels[a.code]?.takeIf { it.isNotEmpty() } ?: "직접 등록"
            root.addView(line("$label · ID …${a.uid.takeLast(6)} · 등록 ${day.format(Date(a.registeredAt))}"))
            root.addView(buttons(button("삭제") { confirmRemove(a, label) }))
        }

        heading("대기 중인 코드 (${pending.size}개)")
        if (pending.isEmpty()) root.addView(line("없어요."))
        for (p in pending) {
            val hours = ((p.expiresAt - now) / 3_600_000L).coerceAtLeast(0)
            root.addView(line("${p.code} · ${p.name} · ${hours}시간 남음"))
            root.addView(buttons(
                button("코드 복사") { copy("관리자 코드", shareMessage(p.code)) },
                button("취소") { cancelCode(p) }
            ))
        }

        manageDialog = AlertDialog.Builder(activity)
            .setTitle("👑 관리자 관리")
            .setView(ScrollView(activity).apply { addView(root) })
            .setNegativeButton("닫기", null)
            .show()
    }

    private fun shareMessage(code: String) =
        "[AutoClicker Pro 관리자 초대]\n관리자 코드: $code\n앱의 인증 화면 → 관리자 로그인에서 이 코드를 입력하세요. 만든 지 24시간 안에 한 번만 쓸 수 있어요."

    private fun askName() {
        val input = EditText(activity).apply { hint = "이름표 (예: 김민수)"; setPadding(dp(20), dp(16), dp(20), dp(16)) }
        AlertDialog.Builder(activity)
            .setTitle("새 관리자 코드")
            .setMessage("누구에게 줄 코드인지 이름표를 적어 주세요. 목록에서 알아보는 용도입니다.")
            .setView(input)
            .setPositiveButton("만들기") { _, _ ->
                val name = input.text.toString()
                Thread {
                    val r = server.createCode(name)
                    ui {
                        val code = r.value
                        if (code == null) { toast(r.error ?: "코드를 만들지 못했어요"); return@ui }
                        copy("관리자 코드", shareMessage(code))
                        AlertDialog.Builder(activity)
                            .setTitle("코드를 만들었어요")
                            .setMessage("$code\n\n카카오톡으로 보낼 안내 문구를 복사해 두었어요. 24시간 안에 한 번만 쓸 수 있어요.")
                            .setPositiveButton("확인") { _, _ -> Thread { load() }.start() }
                            .show()
                    }
                }.start()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun confirmRemove(a: AdminEntry, label: String) {
        AlertDialog.Builder(activity)
            .setTitle("관리자 삭제")
            .setMessage("$label (ID …${a.uid.takeLast(6)}) 의 관리자 권한을 없앨까요? 그 폰은 다음에 앱을 열 때 팀장 화면으로 돌아갑니다.")
            .setPositiveButton("삭제") { _, _ ->
                Thread {
                    val err = server.removeAdmin(a.uid)
                    ui { if (err != null) toast(err) else toast("삭제했어요") }
                    load()
                }.start()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun cancelCode(p: AdminCode) {
        Thread {
            val err = server.cancelCode(p.code)
            ui { if (err != null) toast(err) else toast("코드를 취소했어요") }
            load()
        }.start()
    }
}
