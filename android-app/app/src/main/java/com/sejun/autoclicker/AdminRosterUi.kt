package com.sejun.autoclicker

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.InputType
import android.widget.Toast
import com.sejun.autoclicker.SheetDialog.Companion.act
import com.sejun.autoclicker.SheetDialog.Kind
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 개발자 전용 "관리자 관리" 화면과 "내 기기 ID" 안내. 화면은 코드로 만든다. 서버 통신은 백그라운드 스레드에서 한다. */
internal class AdminRosterUi(private val activity: Activity, private val server: AdminServer) {

    private var manageDialog: SheetDialog? = null

    private fun ui(block: () -> Unit) = activity.runOnUiThread(block)
    private fun toast(msg: String) = Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()

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
                SheetDialog(
                    activity, "내 기기 ID",
                    "개발자로 등록하려면 이 ID를 Firebase 콘솔의 owners 아래에 적습니다. 앱을 지우고 다시 설치하면 ID가 바뀝니다.\n\n앱 버전: v${BuildConfig.VERSION_NAME}"
                ).also { sheet ->
                    val card = sheet.card(top = 14)
                    sheet.line(card, id, bold = true).apply { setTextIsSelectable(true); textSize = 15f }
                }.actions(act("닫기"), act("복사", Kind.PRIMARY) { copy("기기 ID", id) }).show()
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
        val sheet = SheetDialog(activity, "관리자 관리")

        sheet.wideButton("+ 관리자 코드 만들기", top = 12) { askNames() }
        sheet.equalRow(
            sheet.pill("방 전체 목록") { showRooms() },
            sheet.pill("목록 복사") { copy("관리자 목록", AdminRoster.exportText(admins, codes, now)) },
            sheet.pill("코드 정리") { confirmPurge() },
            top = 10
        )

        sheet.heading("등록된 관리자 (${admins.size}명)")
        if (admins.isEmpty()) sheet.line(sheet.card(), "아직 없어요.")
        for (a in admins) {
            val label = AdminRoster.labelOf(a, codes)
            val card = sheet.card()
            sheet.line(card, "$label · ID …${a.uid.takeLast(6)}", bold = true)
            sheet.line(card, "${AdminRoster.activityText(a.lastSeen, a.appVersion, now)} · 등록 ${day.format(Date(a.registeredAt))}", small = true, top = 2)
            sheet.pillRow(card,
                sheet.pill("이름 수정") { askRename(a, label) },
                sheet.pill("삭제", Kind.DANGER) { confirmRemove(a, label) }
            )
        }

        sheet.heading("대기 중인 코드 (${pending.size}개)")
        if (pending.isEmpty()) sheet.line(sheet.card(), "없어요.")
        for (p in pending) {
            val card = sheet.card()
            sheet.line(card, "${p.code} · ${p.name}", bold = true)
            sheet.line(card, "${remainText(p.expiresAt - now)} 남음", small = true, top = 2)
            sheet.pillRow(card,
                sheet.pill("코드 복사") { copy("관리자 코드", AdminRoster.shareMessage(p.code, ttlOf(p))) },
                sheet.pill("취소", Kind.DANGER) { cancelCode(p) }
            )
        }

        sheet.actions(act("닫기"))
        manageDialog = sheet
        sheet.show()
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
                val sheet = SheetDialog(activity, "방 전체 (${rooms.size}개)")
                rooms.forEach { r ->
                    val card = sheet.card()
                    sheet.line(card, RoomList.line(r))
                    card.setOnClickListener {
                        sheet.dismiss()
                        val room = data.rooms?.get(r.code) as? Map<*, *> ?: return@setOnClickListener
                        showRoomDetail(r.code, room, data.members?.get(r.code) as? Map<*, *>)
                    }
                }
                sheet.actions(act("닫기")).show()
            }
        }.start()
    }

    private fun showRoomDetail(code: String, room: Map<*, *>, members: Map<*, *>?) {
        val text = RoomList.detail(code, room, members)
        val sheet = SheetDialog(activity, "방 $code")
        sheet.line(sheet.card(top = 12), text)
        sheet.actions(
            act("닫기"),
            act("복사") { copy("방 $code", text) },
            act("방 삭제", Kind.DANGER) { confirmDeleteRoom(code) }
        ).show()
    }

    private fun confirmDeleteRoom(code: String) {
        SheetDialog.confirm(
            activity, "방 $code 삭제",
            "이 방과 방 명단을 서버에서 지워요. 되돌릴 수 없고, 그 방에 들어가 있던 사람들은 다시 입장해야 합니다. 정말 지울까요?",
            "삭제", danger = true
        ) {
            Thread {
                val err = server.deleteRoom(code)
                ui { toast(err ?: "방 $code 을(를) 지웠어요") }
            }.start()
        }
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
        val sheet = SheetDialog(activity, "새 관리자 코드", "누구에게 줄 코드인지 이름표를 적어 주세요. 목록에서 알아보는 용도입니다. 코드는 한 번만 쓸 수 있어요.")
        val input = sheet.field(
            hint = "이름표 (여러 명이면 줄바꿈으로, 최대 ${AdminRoster.MAX_BATCH}명)",
            maxLength = 400,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            lines = 3
        )
        sheet.heading("코드를 쓸 수 있는 기간", top = 16)
        var ttl = CodeTtl.DAY
        val chips = mutableListOf<android.widget.TextView>()
        val all = CodeTtl.values()
        fun paint() = chips.forEachIndexed { i, c ->
            val on = all[i] == ttl
            c.setTextColor(if (on) android.graphics.Color.WHITE else SheetDialog.INK)
            c.background = sheet.box(if (on) SheetDialog.BLUE else SheetDialog.FIELD, 14)
        }
        all.forEach { t -> chips.add(sheet.pill(t.label) { ttl = t; paint() }) }
        sheet.equalRow(*chips.toTypedArray(), top = 8)
        paint()

        sheet.actions(
            act("취소"),
            SheetDialog.Action("만들기", Kind.PRIMARY) {
                val names = AdminRoster.parseNames(input.text.toString())
                if (names.isEmpty()) { toast("이름표를 입력해 주세요"); return@Action false }
                val chosen = ttl
                Thread {
                    val made = mutableListOf<Pair<String, String>>()
                    var firstError: String? = null
                    for (n in names) {
                        val r = server.createCode(n, chosen)
                        val code = r.value
                        if (code != null) made.add(n to code) else if (firstError == null) firstError = r.error
                    }
                    ui {
                        if (made.isEmpty()) { toast(firstError ?: "코드를 만들지 못했어요"); return@ui }
                        copy("관리자 코드", AdminRoster.batchShare(made, chosen))
                        val warn = if (firstError != null) "\n\n일부는 만들지 못했어요: $firstError" else ""
                        val done = SheetDialog(
                            activity, "코드를 ${made.size}개 만들었어요",
                            "카카오톡으로 보낼 안내 문구를 사람별로 복사해 두었어요. ${chosen.label} 안에 한 번만 쓸 수 있어요.$warn"
                        )
                        made.forEach { done.line(done.card(), "${it.first}  ${it.second}", bold = true) }
                        done.actions(act("확인", Kind.PRIMARY) { Thread { load() }.start() }).show()
                    }
                }.start()
                true
            }
        ).show()
    }

    private fun askRename(a: AdminEntry, current: String) {
        InputSheet(
            activity, "이름표 수정", "ID …${a.uid.takeLast(6)}",
            fields = listOf(InputSheet.Field(
                hint = "새 이름표", maxLength = AdminRoster.MAX_NAME, inputType = InputType.TYPE_CLASS_TEXT,
                initial = if (current != "직접 등록") current else ""
            )),
            submitLabel = "저장"
        ) { v ->
            Thread {
                val err = server.renameAdmin(a.uid, v[0])
                ui { if (err != null) toast(err) else toast("이름표를 고쳤어요") }
                load()
            }.start()
            true
        }.show()
    }

    private fun confirmPurge() {
        SheetDialog.confirm(
            activity, "코드 정리",
            "이미 쓰였거나 기한이 지난 코드를 서버에서 지워요. 관리자 이름표는 먼저 저장해 두니 목록에 그대로 남아요. 대기 중인 코드는 지우지 않습니다.",
            "정리"
        ) {
            Thread {
                val r = server.purgeCodes()
                ui { val n = r.value; if (n == null) toast(r.error ?: "정리하지 못했어요") else toast("코드 ${n}개를 정리했어요") }
                load()
            }.start()
        }
    }

    private fun confirmRemove(a: AdminEntry, label: String) {
        SheetDialog.confirm(
            activity, "관리자 삭제",
            "$label (ID …${a.uid.takeLast(6)}) 의 관리자 권한을 없앨까요? 그 폰은 다음에 앱을 열 때 집결장 화면으로 돌아갑니다.",
            "삭제", danger = true
        ) {
            Thread {
                val err = server.removeAdmin(a.uid)
                ui { if (err != null) toast(err) else toast("삭제했어요") }
                load()
            }.start()
        }
    }

    private fun cancelCode(p: AdminCode) {
        Thread {
            val err = server.cancelCode(p.code)
            ui { if (err != null) toast(err) else toast("코드를 취소했어요") }
            load()
        }.start()
    }
}
