package com.sejun.autoclicker

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
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
                    .setMessage("개발자로 등록하려면 이 ID를 Firebase 콘솔의 owners 아래에 적습니다. 앱을 지우고 다시 설치하면 ID가 바뀝니다.\n\n앱 버전: v${BuildConfig.VERSION_NAME}")
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
        val adminList = admins.value
        val codeList = codes.value
        if (adminList != null && codeList != null) server.backfillNames(adminList, codeList)
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
        fun line(text: String, small: Boolean = false) = TextView(activity).apply {
            this.text = text; textSize = if (small) 12f else 14f
            setTextColor(themeColor(if (small) android.R.attr.textColorSecondary else android.R.attr.textColorPrimary))
            setPadding(0, dp(4), 0, 0)
        }
        fun buttons(vararg b: Button) = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END
            b.forEach { addView(it) }
        }
        fun button(text: String, onClick: () -> Unit) = Button(activity).apply {
            this.text = text; textSize = 12f; setOnClickListener { onClick() }
        }

        root.addView(button("+ 관리자 코드 만들기") { askNames() })
        root.addView(button("🗂 방 전체 목록 (개발자 전용)") { showRooms() })
        root.addView(buttons(
            button("📋 목록 전체 복사") { copy("관리자 목록", AdminRoster.exportText(admins, codes, now)) },
            button("🧹 쓰인·만료 코드 정리") { confirmPurge() }
        ))

        heading("등록된 관리자 (${admins.size}명)")
        if (admins.isEmpty()) root.addView(line("아직 없어요."))
        for (a in admins) {
            val label = AdminRoster.labelOf(a, codes)
            root.addView(line("$label · ID …${a.uid.takeLast(6)}"))
            root.addView(line("${AdminRoster.activityText(a.lastSeen, a.appVersion, now)} · 등록 ${day.format(Date(a.registeredAt))}", small = true))
            root.addView(buttons(button("이름 수정") { askRename(a, label) }, button("삭제") { confirmRemove(a, label) }))
        }

        heading("대기 중인 코드 (${pending.size}개)")
        if (pending.isEmpty()) root.addView(line("없어요."))
        for (p in pending) {
            root.addView(line("${p.code} · ${p.name} · ${remainText(p.expiresAt - now)} 남음"))
            root.addView(buttons(
                button("코드 복사") { copy("관리자 코드", AdminRoster.shareMessage(p.code, ttlOf(p))) },
                button("취소") { cancelCode(p) }
            ))
        }

        manageDialog = AlertDialog.Builder(activity)
            .setTitle("👑 관리자 관리")
            .setView(ScrollView(activity).apply { addView(root) })
            .setNegativeButton("닫기", null)
            .show()
    }

    /** 서버의 모든 방을 목록으로 보여 준다. 방을 누르면 군단과 명단을 읽기 전용으로 보여 준다. */
    private fun showRooms() {
        toast("방 목록을 불러오는 중…")
        Thread {
            val r = server.loadRooms()
            ui {
                val data = r.value
                if (data == null) { toast(r.error ?: "방 목록을 불러오지 못했어요"); return@ui }
                val rooms = RoomList.summarize(data.rooms, data.members)
                if (rooms.isEmpty()) { toast("만들어진 방이 없어요"); return@ui }
                AlertDialog.Builder(activity)
                    .setTitle("🗂 방 전체 (${rooms.size}개)")
                    .setItems(rooms.map { RoomList.line(it) }.toTypedArray()) { _, i ->
                        val code = rooms[i].code
                        val room = data.rooms?.get(code) as? Map<*, *> ?: return@setItems
                        showRoomDetail(code, room, data.members?.get(code) as? Map<*, *>)
                    }
                    .setNegativeButton("닫기", null)
                    .show()
            }
        }.start()
    }

    private fun showRoomDetail(code: String, room: Map<*, *>, members: Map<*, *>?) {
        val text = RoomList.detail(code, room, members)
        AlertDialog.Builder(activity)
            .setTitle("방 $code")
            .setMessage(text)
            .setPositiveButton("복사") { _, _ -> copy("방 $code", text) }
            .setNegativeButton("닫기", null)
            .show()
    }

    /** 코드를 만들 때 고른 기간에 가장 가까운 것. 안내 문구에 "24시간 안에"처럼 적는 데 쓴다. */
    private fun ttlOf(p: AdminCode): CodeTtl =
        CodeTtl.values().minByOrNull { kotlin.math.abs(it.ms - (p.expiresAt - p.createdAt)) } ?: CodeTtl.DAY

    private fun remainText(ms: Long): String = when {
        ms >= 86_400_000L -> "${ms / 86_400_000L}일"
        ms >= 3_600_000L -> "${ms / 3_600_000L}시간"
        else -> "${(ms / 60_000L).coerceAtLeast(0)}분"
    }

    /** 이름표를 한 줄에 한 명씩 적으면 사람마다 코드를 만든다. 코드를 쓸 수 있는 기간도 고른다. */
    private fun askNames() {
        val input = EditText(activity).apply {
            hint = "이름표 (여러 명이면 줄바꿈으로, 최대 ${AdminRoster.MAX_BATCH}명)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
            gravity = Gravity.TOP
        }
        val ids = CodeTtl.values().associateWith { View.generateViewId() }
        val group = RadioGroup(activity).apply {
            orientation = RadioGroup.HORIZONTAL
            CodeTtl.values().forEach { t -> addView(RadioButton(activity).apply { text = t.label; id = ids.getValue(t) }) }
            check(ids.getValue(CodeTtl.DAY))
        }
        val box = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
            addView(input)
            addView(TextView(activity).apply { text = "코드를 쓸 수 있는 기간"; textSize = 13f; setPadding(0, dp(12), 0, dp(4)) })
            addView(group)
        }
        AlertDialog.Builder(activity)
            .setTitle("새 관리자 코드")
            .setMessage("누구에게 줄 코드인지 이름표를 적어 주세요. 목록에서 알아보는 용도입니다. 코드는 한 번만 쓸 수 있어요.")
            .setView(box)
            .setPositiveButton("만들기") { _, _ ->
                val names = AdminRoster.parseNames(input.text.toString())
                val ttl = ids.entries.firstOrNull { it.value == group.checkedRadioButtonId }?.key ?: CodeTtl.DAY
                if (names.isEmpty()) { toast("이름표를 입력해 주세요"); return@setPositiveButton }
                Thread {
                    val made = mutableListOf<Pair<String, String>>()
                    var firstError: String? = null
                    for (n in names) {
                        val r = server.createCode(n, ttl)
                        val code = r.value
                        if (code != null) made.add(n to code) else if (firstError == null) firstError = r.error
                    }
                    ui {
                        if (made.isEmpty()) { toast(firstError ?: "코드를 만들지 못했어요"); return@ui }
                        copy("관리자 코드", AdminRoster.batchShare(made, ttl))
                        val summary = made.joinToString("\n") { "${it.first}  ${it.second}" }
                        val warn = if (firstError != null) "\n\n일부는 만들지 못했어요: $firstError" else ""
                        AlertDialog.Builder(activity)
                            .setTitle("코드를 ${made.size}개 만들었어요")
                            .setMessage("$summary\n\n카카오톡으로 보낼 안내 문구를 사람별로 복사해 두었어요. ${ttl.label} 안에 한 번만 쓸 수 있어요.$warn")
                            .setPositiveButton("확인") { _, _ -> Thread { load() }.start() }
                            .show()
                    }
                }.start()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun askRename(a: AdminEntry, current: String) {
        val input = EditText(activity).apply {
            hint = "새 이름표"
            if (current != "직접 등록") setText(current)
            setSelection(text.length)
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }
        AlertDialog.Builder(activity)
            .setTitle("이름표 수정")
            .setMessage("ID …${a.uid.takeLast(6)}")
            .setView(input)
            .setPositiveButton("저장") { _, _ ->
                Thread {
                    val err = server.renameAdmin(a.uid, input.text.toString())
                    ui { if (err != null) toast(err) else toast("이름표를 고쳤어요") }
                    load()
                }.start()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun confirmPurge() {
        AlertDialog.Builder(activity)
            .setTitle("코드 정리")
            .setMessage("이미 쓰였거나 기한이 지난 코드를 서버에서 지워요. 관리자 이름표는 먼저 저장해 두니 목록에 그대로 남아요. 대기 중인 코드는 지우지 않습니다.")
            .setPositiveButton("정리") { _, _ ->
                Thread {
                    val r = server.purgeCodes()
                    ui { val n = r.value; if (n == null) toast(r.error ?: "정리하지 못했어요") else toast("코드 ${n}개를 정리했어요") }
                    load()
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
