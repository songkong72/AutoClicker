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

/** 개발자 전용 "지휘관 관리" 화면과 "내 기기 ID" 안내. 화면은 코드로 만든다. 서버 통신은 백그라운드 스레드에서 한다. */
internal class AdminRosterUi(private val activity: Activity, private val server: AdminServer) {

    private var manageDialog: SheetDialog? = null

    companion object {
        /** 이 폰에 연맹 대표 신청이 걸려 있을 때 그 소속("2000-WBI")을 적어 두는 자리. 첫 화면의 대기 카드가 본다. */
        const val KEY_REQUEST = "rep_request_group"
    }

    private fun ui(block: () -> Unit) = activity.runOnUiThread(block)
    private fun toast(msg: String) = Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()

    private fun copy(label: String, text: String) {
        val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        TextShare.copiedNotice(activity)
    }

    /** 지휘관 목록 보내기. 대기 중인 코드가 들어 있으면 한 줄로 알린다. */
    private fun shareList(text: String, pendingCodes: Int) = TextShare.sheet(
        activity, "지휘관 목록 보내기", "지휘관 목록", text,
        if (pendingCodes > 0) "대기 중인 지휘관 코드 ${pendingCodes}개가 함께 보내져요. 받을 사람이 맞는 대화방인지 확인해 주세요." else null
    )

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

    /** 개발자인지 서버에서 확인하고, 맞으면 지휘관 관리 화면을 연다. */
    fun showManage() {
        toast("서버를 확인하는 중…")
        Thread {
            val me = server.uid()
            val uid = me.value
            if (uid == null) { ui { toast(me.error ?: "기기 ID를 받지 못했어요") }; return@Thread }
            when (server.isOwner(uid)) {
                Check.YES -> load()
                Check.NO -> ui { toast("개발자만 쓸 수 있어요. 개발자는 Firebase 콘솔에 이 기기 ID를 적어야 해요 (지휘관 메뉴 → 내 기기 ID 보기)") }
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
        // 연맹 대표 신청. 읽지 못하면(새 서버 규칙을 아직 게시하지 않았을 때 등) 없는 것으로 본다.
        val requests = server.listRequests().value.orEmpty()
        ui {
            val a = admins.value
            val c = codes.value
            if (a == null || c == null) { toast(admins.error ?: codes.error ?: "목록을 불러오지 못했어요"); return@ui }
            render(a, c, requests)
        }
    }

    private fun prefs() = activity.getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE)

    private fun agoText(at: Long, now: Long): String {
        val diff = (now - at).coerceAtLeast(0L)
        return when {
            diff < 60_000L -> "방금 전"
            diff < 3_600_000L -> "${diff / 60_000L}분 전"
            diff < 86_400_000L -> "${diff / 3_600_000L}시간 전"
            else -> "${diff / 86_400_000L}일 전"
        }
    }

    /** 서버 번호와 연맹을 적는 두 칸을 창에 더한다. 대소문자를 구분하므로 키보드가 글자를 바꾸지 않게 한다. */
    private fun groupFields(sheet: SheetDialog, initial: RallyGroup?): Pair<android.widget.EditText, android.widget.EditText> {
        val server = sheet.field("서버 번호 (예: 2000)", RallyGroup.MAX_SERVER, InputType.TYPE_CLASS_NUMBER, initial?.server.orEmpty())
        val alliance = sheet.field(
            "연맹 (예: WBI, 대소문자 구분)", RallyGroup.MAX_ALLIANCE,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
            initial?.alliance.orEmpty(), top = 10
        )
        return server to alliance
    }

    /** 두 칸에서 소속을 읽는다. 틀렸으면 이유를 알리고 null. */
    private fun readGroup(fields: Pair<android.widget.EditText, android.widget.EditText>): RallyGroup? {
        val s = fields.first.text?.toString()
        val a = fields.second.text?.toString()
        RallyGroup.problem(s, a)?.let { toast(it); return null }
        return RallyGroup.of(s, a)
    }

    /** "연맹 대표로 지정" 켜고 끄는 버튼. 지금 값을 돌려주는 함수를 함께 준다. */
    private fun repToggle(sheet: SheetDialog, initial: Boolean): () -> Boolean {
        var on = initial
        lateinit var button: android.widget.TextView
        fun paint() {
            button.text = if (on) "✓ 연맹 대표로 지정함" else "연맹 대표로 지정 안 함 (누르면 지정)"
            button.setTextColor(if (on) android.graphics.Color.WHITE else SheetDialog.BLUE)
            button.background = sheet.box(if (on) SheetDialog.BLUE else android.graphics.Color.WHITE, 16, SheetDialog.BLUE)
        }
        button = sheet.wideButton("", Kind.OUTLINE, top = 12) { on = !on; paint() }
        paint()
        return { on }
    }

    private fun render(admins: List<AdminEntry>, codes: List<AdminCode>, requests: List<RepRequest>) {
        manageDialog?.dismiss()
        val now = System.currentTimeMillis()
        val pending = codes.filter { !it.used && now < it.expiresAt }
        val day = SimpleDateFormat("MM/dd", Locale.KOREA)
        val sheet = SheetDialog(activity, "지휘관 관리")

        sheet.wideButton("+ 지휘관 코드 만들기", top = 12) { askNames() }
        if (requests.isNotEmpty()) {
            sheet.wideButton("연맹 대표 신청 ${requests.size}건", Kind.OUTLINE, top = 10) { showRequests(requests, admins) }
        }
        sheet.equalRow(
            sheet.pill("방 전체 목록") { showRooms() },
            sheet.pill("목록 보내기") { shareList(AdminRoster.exportText(admins, codes, now), codes.count { !it.used && now < it.expiresAt }) },
            sheet.pill("코드 정리") { confirmPurge() },
            top = 10
        )

        sheet.heading("등록된 지휘관 (${admins.size}명)")
        if (admins.isEmpty()) sheet.line(sheet.card(), "아직 없어요.")
        for (a in admins) {
            val label = AdminRoster.labelOf(a, codes)
            val card = sheet.card()
            sheet.line(card, "$label · ID …${a.uid.takeLast(6)}", bold = true)
            // 소속과 대표 여부. 소속이 없으면 눈에 띄게 적는다(전에 등록된 지휘관은 개발자가 정해 준다).
            sheet.line(card, RallyRoles.groupLabel(a.group) + if (a.rep) " · 연맹 대표" else "", small = true, bold = true, top = 2).apply {
                setTextColor(if (a.group.isEmpty()) android.graphics.Color.parseColor("#B45309") else SheetDialog.BLUE)
            }
            sheet.line(card, "${AdminRoster.activityText(a.lastSeen, a.appVersion, now)} · 등록 ${day.format(Date(a.registeredAt))}", small = true, top = 2)
            sheet.pillRow(card,
                sheet.pill(if (a.group.isEmpty()) "소속 정하기" else "소속 바꾸기", if (a.group.isEmpty()) Kind.PRIMARY else Kind.NORMAL) { askGroup(a, label) },
                sheet.pill("이름표 수정") { askRename(a, label) },
                sheet.pill("삭제", Kind.DANGER) { confirmRemove(a, label) }
            )
        }

        sheet.heading("대기 중인 코드 (${pending.size}개)")
        if (pending.isEmpty()) sheet.line(sheet.card(), "없어요.")
        for (p in pending) {
            val card = sheet.card()
            sheet.line(card, "${p.code} · ${p.name}", bold = true)
            sheet.line(card, "${RallyRoles.groupLabel(p.group)}${if (p.rep) " · 연맹 대표" else ""} · ${remainText(p.expiresAt - now)} 남음", small = true, top = 2)
            sheet.pillRow(card,
                sheet.pill("보내기") { TextShare.sheet(activity, "지휘관 코드 보내기", "지휘관 코드", AdminRoster.shareMessage(p.code, ttlOf(p), p.group, p.rep)) },
                sheet.pill("취소", Kind.DANGER) { cancelCode(p) }
            )
        }

        sheet.actions(act("닫기"))
        manageDialog = sheet
        sheet.show()
    }

    /** 개발자: 지휘관의 소속과 대표 여부를 정한다. 정해진 지휘관은 그 소속의 방만 만들고 고칠 수 있다. */
    private fun askGroup(a: AdminEntry, label: String) {
        val sheet = SheetDialog(activity, "소속 정하기", "$label (ID …${a.uid.takeLast(6)}) 을(를) 어느 소속의 지휘관으로 둘지 정해요. 그 폰은 다음에 앱을 열 때 이 소속으로 바뀌어요.")
        val fields = groupFields(sheet, RallyGroup.fromId(a.group))
        val isRep = repToggle(sheet, a.rep)
        sheet.actions(
            act("취소"),
            SheetDialog.Action("저장", Kind.PRIMARY) {
                val group = readGroup(fields) ?: return@Action false
                val rep = isRep()
                Thread {
                    val err = server.setAdminGroup(a.uid, group, rep)
                    ui { toast(err ?: "소속을 ${group.label}(으)로 정했어요") }
                    load()
                }.start()
                true
            }
        ).show()
    }

    /** 개발자: 연맹 대표 신청 목록. 승인하면 그 기기가 신청한 소속의 대표가 된다. */
    private fun showRequests(requests: List<RepRequest>, admins: List<AdminEntry>) {
        val now = System.currentTimeMillis()
        val sheet = SheetDialog(activity, "연맹 대표 신청 (${requests.size}건)", "신청한 사람이 정말 그 연맹 사람인지는 앱이 확인하지 못해요. 한마디를 보고 판단해 주세요.")
        for (r in requests) {
            val card = sheet.card()
            sheet.line(card, "${RallyRoles.groupLabel(r.group)} · ${r.name}", bold = true)
            sheet.line(card, "${agoText(r.createdAt, now)} 신청 · ID …${r.uid.takeLast(6)}", small = true, top = 2)
            sheet.line(card, if (r.note.isEmpty()) "(한마디 없음)" else "\"${r.note}\"", top = 6)
            RallyRoles.requestWarning(r, requests, admins)?.let { warn ->
                sheet.line(card, warn, small = true, bold = true, top = 6).setTextColor(android.graphics.Color.parseColor("#B45309"))
            }
            sheet.pillRow(card,
                sheet.pill("거절", Kind.DANGER) { sheet.dismiss(); askReject(r) },
                sheet.pill("승인", Kind.PRIMARY) { sheet.dismiss(); confirmApprove(r) }
            )
        }
        sheet.actions(act("닫기")).show()
    }

    private fun confirmApprove(r: RepRequest) {
        SheetDialog.confirm(
            activity, "연맹 대표 승인",
            "${r.name} 님을 ${RallyRoles.groupLabel(r.group)}의 연맹 대표로 정할까요? 대표는 그 연맹의 지휘관을 직접 정하고 뺄 수 있어요.",
            "승인"
        ) {
            Thread {
                val err = server.approveRequest(r)
                ui { toast(err ?: "${r.name} 님을 연맹 대표로 정했어요") }
                load()
            }.start()
        }
    }

    private fun askReject(r: RepRequest) {
        InputSheet(
            activity, "신청 거절", "${RallyRoles.groupLabel(r.group)} · ${r.name}\n신청한 사람의 화면에 보일 한 줄을 적어 주세요. 비워 둬도 돼요.",
            fields = listOf(InputSheet.Field(hint = "예: 이미 대표가 있어요", maxLength = RallyRoles.MAX_NOTE, inputType = InputType.TYPE_CLASS_TEXT)),
            submitLabel = "거절"
        ) { v ->
            Thread {
                val err = server.rejectRequest(r.uid, v[0])
                ui { toast(err ?: "신청을 거절했어요") }
                load()
            }.start()
            true
        }.show()
    }

    // ---- 연맹 대표: 우리 연맹 지휘관 ----

    /** 연맹 대표인지 서버에서 확인하고, 맞으면 자기 소속의 지휘관을 정하고 빼는 화면을 연다. */
    fun showMyAlliance() {
        toast("서버를 확인하는 중…")
        Thread {
            val uid = server.uid().value
            if (uid == null) { ui { toast("기기 ID를 받지 못했어요. 인터넷 연결을 확인해 주세요") }; return@Thread }
            val me = server.myAdmin(uid).value
            if (me == null || !me.rep || RallyGroup.fromId(me.group) == null) { ui { toast("연맹 대표만 쓸 수 있어요.") }; return@Thread }
            loadAlliance(uid, me)
        }.start()
    }

    private fun loadAlliance(uid: String, me: AdminEntry) {
        val admins = server.listGroupAdmins(me.group)
        val codes = server.listMyCodes(uid)
        ui {
            val a = admins.value
            val c = codes.value
            if (a == null || c == null) { toast(admins.error ?: codes.error ?: "목록을 불러오지 못했어요"); return@ui }
            renderAlliance(uid, me, a, c)
        }
    }

    private fun renderAlliance(uid: String, me: AdminEntry, admins: List<AdminEntry>, codes: List<AdminCode>) {
        manageDialog?.dismiss()
        val now = System.currentTimeMillis()
        val pending = codes.filter { !it.used && now < it.expiresAt }
        val sheet = SheetDialog(activity, "우리 연맹 지휘관", RallyRoles.groupLabel(me.group))
        if (RallyRoles.canAddCommander(admins.size, pending.size)) {
            sheet.wideButton("+ 지휘관 코드 만들기", top = 12) { askAllianceCode(uid, me) }
            sheet.line(sheet.content, "코드를 받은 사람은 ${RallyRoles.groupLabel(me.group)}의 지휘관이 돼요. 다른 지휘관을 정할 수는 없어요.", small = true, top = 6)
        } else {
            sheet.line(sheet.card(top = 12), "한 연맹의 지휘관은 ${RallyRoles.MAX_COMMANDERS}명까지예요. 더 넣으려면 먼저 한 명을 빼 주세요.")
        }

        sheet.heading("우리 연맹 지휘관 (${admins.size} / ${RallyRoles.MAX_COMMANDERS}명)")
        for (a in admins) {
            val label = AdminRoster.labelOf(a, codes)
            val card = sheet.card()
            sheet.line(card, label + (if (a.uid == uid) " (나)" else "") + (if (a.rep) " · 대표" else ""), bold = true)
            sheet.line(card, AdminRoster.activityText(a.lastSeen, a.appVersion, now), small = true, top = 2)
            if (!a.rep && a.uid != uid) {
                sheet.pillRow(card, sheet.pill("삭제", Kind.DANGER) {
                    SheetDialog.confirm(activity, "지휘관 삭제", "$label 의 지휘관 권한을 없앨까요? 그 폰은 다음에 앱을 열 때 집결장 화면으로 돌아갑니다.", "삭제", danger = true) {
                        Thread {
                            val err = server.removeAdmin(a.uid)
                            ui { toast(err ?: "삭제했어요") }
                            loadAlliance(uid, me)
                        }.start()
                    }
                })
            }
        }

        sheet.heading("대기 중인 코드 (${pending.size}개)")
        if (pending.isEmpty()) sheet.line(sheet.card(), "없어요.")
        for (p in pending) {
            val card = sheet.card()
            sheet.line(card, "${p.code} · ${p.name}", bold = true)
            sheet.line(card, "${remainText(p.expiresAt - now)} 남음", small = true, top = 2)
            sheet.pillRow(card,
                sheet.pill("보내기") { TextShare.sheet(activity, "지휘관 코드 보내기", "지휘관 코드", AdminRoster.shareMessage(p.code, ttlOf(p), p.group, false)) },
                sheet.pill("취소", Kind.DANGER) {
                    Thread {
                        val err = server.cancelCode(p.code)
                        ui { toast(err ?: "코드를 취소했어요") }
                        loadAlliance(uid, me)
                    }.start()
                }
            )
        }
        sheet.actions(act("닫기"))
        manageDialog = sheet
        sheet.show()
    }

    /** 연맹 대표가 자기 소속의 지휘관 코드를 하나 만든다(24시간, 한 번만 쓸 수 있다). */
    private fun askAllianceCode(uid: String, me: AdminEntry) {
        InputSheet(
            activity, "새 지휘관 코드", "${RallyRoles.groupLabel(me.group)}의 지휘관이 될 사람의 이름표를 적어 주세요. 코드는 24시간 안에 한 번만 쓸 수 있어요.",
            fields = listOf(InputSheet.Field(hint = "이름표", maxLength = AdminRoster.MAX_NAME, inputType = InputType.TYPE_CLASS_TEXT)),
            submitLabel = "만들기"
        ) { v ->
            if (AdminRoster.cleanName(v[0]).isEmpty()) { toast("이름표를 입력해 주세요"); false } else {
                Thread {
                    val r = server.createCode(v[0], CodeTtl.DAY, group = me.group, by = uid)
                    val code = r.value
                    ui {
                        if (code == null) toast(r.error ?: "코드를 만들지 못했어요")
                        else TextShare.sheet(activity, "지휘관 코드 보내기", "지휘관 코드", AdminRoster.shareMessage(code, CodeTtl.DAY, me.group, false))
                    }
                    loadAlliance(uid, me)
                }.start()
                true
            }
        }.show()
    }

    // ---- 신청자: 연맹 대표 신청 ----

    /**
     * 연맹 대표 신청 창. 이미 지휘관이 됐으면(승인됨) [onApproved]를, 신청 상태가 바뀌면 [onChanged]를 부른다.
     * 이 폰에 신청이 걸려 있는지는 "rep_request_group"에 소속을 적어 기억한다(첫 화면의 대기 카드가 본다).
     */
    fun showRepRequest(onApproved: () -> Unit, onChanged: () -> Unit) {
        toast("서버를 확인하는 중…")
        Thread {
            val uid = server.uid().value
            if (uid == null) { ui { toast("기기 ID를 받지 못했어요. 인터넷 연결을 확인해 주세요") }; return@Thread }
            if (server.isAdmin(uid) == Check.YES) {
                ui { prefs().edit().remove(KEY_REQUEST).apply(); onApproved() }
                return@Thread
            }
            val r = server.myRequest(uid)
            ui {
                val req = r.value
                when {
                    r.error != null -> toast("${r.error}\n(새 서버 규칙이 아직 게시되지 않았을 수 있어요)")
                    req == null -> { prefs().edit().remove(KEY_REQUEST).apply(); onChanged(); askRequest(uid, onChanged) }
                    req.pending -> { prefs().edit().putString(KEY_REQUEST, req.group).apply(); onChanged(); showPending(uid, req, onChanged) }
                    else -> { prefs().edit().remove(KEY_REQUEST).apply(); onChanged(); showRejected(uid, req, onChanged) }
                }
            }
        }.start()
    }

    private fun askRequest(uid: String, onChanged: () -> Unit) {
        val sheet = SheetDialog(
            activity, "연맹 대표 신청",
            "우리 연맹에서 이 앱의 집결 기능을 쓰려면 대표가 한 명 필요해요. 개발자가 확인한 뒤 승인하면 이 폰이 대표가 되어 지휘관을 정할 수 있어요. 한 폰에서 신청은 하나만 할 수 있어요."
        )
        val fields = groupFields(sheet, RallyGroup.load(prefs()))
        val name = sheet.field("게임 캐릭터명", AdminRoster.MAX_NAME, InputType.TYPE_CLASS_TEXT, PreferencesHelper.getRallyCharacterName(activity), top = 10)
        val note = sheet.field("한마디 (선택, 예: WBI 맹주입니다)", RallyRoles.MAX_NOTE, InputType.TYPE_CLASS_TEXT, top = 10)
        sheet.actions(
            act("취소"),
            SheetDialog.Action("신청하기", Kind.PRIMARY) {
                val group = readGroup(fields) ?: return@Action false
                if (AdminRoster.cleanName(name.text.toString()).isEmpty()) { toast("게임 캐릭터명을 입력해 주세요"); return@Action false }
                Thread {
                    val err = server.submitRequest(uid, group, name.text.toString(), note.text.toString())
                    ui {
                        if (err != null) toast(err) else {
                            prefs().edit().putString(KEY_REQUEST, group.id).apply()
                            toast("${group.label}의 연맹 대표를 신청했어요. 개발자가 승인하면 알 수 있게 가끔 확인해 주세요.")
                            onChanged()
                        }
                    }
                }.start()
                true
            }
        ).show()
    }

    private fun showPending(uid: String, req: RepRequest, onChanged: () -> Unit) {
        SheetDialog(
            activity, "연맹 대표 승인 대기 중",
            "${RallyRoles.groupLabel(req.group)} · ${req.name}\n${agoText(req.createdAt, System.currentTimeMillis())}에 신청했어요. 아직 승인되지 않았어요.\n개발자가 승인한 뒤 이 창을 다시 열면 지휘관 화면으로 바뀌어요."
        ).actions(
            act("닫기"),
            act("신청 취소", Kind.DANGER) {
                Thread {
                    val err = server.cancelRequest(uid)
                    ui {
                        if (err != null) toast(err) else { prefs().edit().remove(KEY_REQUEST).apply(); toast("신청을 취소했어요"); onChanged() }
                    }
                }.start()
            }
        ).show()
    }

    private fun showRejected(uid: String, req: RepRequest, onChanged: () -> Unit) {
        SheetDialog(
            activity, "승인되지 않았어요",
            "${RallyRoles.groupLabel(req.group)} · ${req.name}\n" + if (req.reason.isEmpty()) "개발자가 이유를 적지 않았어요." else "개발자의 말: \"${req.reason}\""
        ).actions(
            act("닫기"),
            act("다시 신청", Kind.PRIMARY) { askRequest(uid, onChanged) }
        ).show()
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
        val sheet = SheetDialog(activity, "새 지휘관 코드", "누구에게, 어느 소속으로 줄 코드인지 적어 주세요. 이 코드로 들어온 지휘관은 그 소속의 방만 만들고 고칠 수 있어요. 코드는 한 번만 쓸 수 있어요.")
        val fields = groupFields(sheet, RallyGroup.load(prefs()))
        val isRep = repToggle(sheet, false)
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
                val group = readGroup(fields) ?: return@Action false
                val rep = isRep()
                val names = AdminRoster.parseNames(input.text.toString())
                if (names.isEmpty()) { toast("이름표를 입력해 주세요"); return@Action false }
                if (rep && names.size > 1) { toast("연맹 대표 코드는 한 번에 한 명만 만들 수 있어요"); return@Action false }
                val chosen = ttl
                Thread {
                    val made = mutableListOf<Pair<String, String>>()
                    var firstError: String? = null
                    for (n in names) {
                        val r = server.createCode(n, chosen, group = group.id, rep = rep)
                        val code = r.value
                        if (code != null) made.add(n to code) else if (firstError == null) firstError = r.error
                    }
                    ui {
                        if (made.isEmpty()) { toast(firstError ?: "코드를 만들지 못했어요"); return@ui }
                        copy("지휘관 코드", AdminRoster.batchShare(made, chosen, group.id, rep))
                        val warn = if (firstError != null) "\n\n일부는 만들지 못했어요: $firstError" else ""
                        val done = SheetDialog(
                            activity, "코드를 ${made.size}개 만들었어요",
                            "${group.label}${if (rep) " · 연맹 대표" else ""}\n카카오톡으로 보낼 안내 문구를 사람별로 복사해 두었어요. ${chosen.label} 안에 한 번만 쓸 수 있어요.$warn"
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
            "이미 쓰였거나 기한이 지난 코드를 서버에서 지워요. 지휘관 이름표는 먼저 저장해 두니 목록에 그대로 남아요. 대기 중인 코드는 지우지 않습니다.",
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
            activity, "지휘관 삭제",
            "$label (ID …${a.uid.takeLast(6)}) 의 지휘관 권한을 없앨까요? 그 폰은 다음에 앱을 열 때 집결장 화면으로 돌아갑니다.",
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
