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
        activity, T.x.shareListTitle, T.x.shareListName, text,
        if (pendingCodes > 0) T.x.pendingCodesNote(pendingCodes) else null
    )

    /** 이 기기의 서버 ID를 보여 준다. 개발자로 등록할 때 Firebase 콘솔에 적는 값이다. */
    fun showMyId() {
        Thread {
            val r = server.uid()
            ui {
                val id = r.value
                if (id == null) { toast(r.error ?: T.x.noDeviceId); return@ui }
                SheetDialog(
                    activity, T.a.linkMyId,
                    T.x.myIdBody(BuildConfig.VERSION_NAME)
                ).also { sheet ->
                    val card = sheet.card(top = 14)
                    sheet.line(card, id, bold = true).apply { setTextIsSelectable(true); textSize = 15f }
                }.actions(act(T.s.close), act(T.a.copy, Kind.PRIMARY) { copy(T.x.deviceIdWord, id) }).show()
            }
        }.start()
    }

    /** 개발자인지 서버에서 확인하고, 맞으면 지휘관 관리 화면을 연다. */
    fun showManage() {
        toast(T.x.checkingServer)
        Thread {
            val me = server.uid()
            val uid = me.value
            if (uid == null) { ui { toast(me.error ?: T.x.noDeviceId) }; return@Thread }
            when (server.isOwner(uid)) {
                Check.YES -> load()
                Check.NO -> ui { toast(T.x.devOnlyToast) }
                Check.UNKNOWN -> ui { toast(T.x.serverCheckRules) }
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
            if (a == null || c == null) { toast(admins.error ?: codes.error ?: T.x.listLoadFailed); return@ui }
            render(a, c, requests)
        }
    }

    private fun prefs() = activity.getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE)

    private fun agoText(at: Long, now: Long): String {
        val diff = (now - at).coerceAtLeast(0L)
        return when {
            diff < 60_000L -> T.x.justAgo
            diff < 3_600_000L -> T.s.minAgo(diff / 60_000L)
            diff < 86_400_000L -> T.s.hourAgo(diff / 3_600_000L)
            else -> T.s.dayAgo(diff / 86_400_000L)
        }
    }

    /** 서버 번호와 연맹을 적는 두 칸을 창에 더한다. 대소문자를 구분하므로 키보드가 글자를 바꾸지 않게 한다. */
    private fun groupFields(sheet: SheetDialog, initial: RallyGroup?): Pair<android.widget.EditText, android.widget.EditText> {
        val server = sheet.field(T.a.serverHint, RallyGroup.MAX_SERVER, InputType.TYPE_CLASS_NUMBER, initial?.server.orEmpty())
        val alliance = sheet.field(
            T.x.allianceHintCase, RallyGroup.MAX_ALLIANCE,
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
            button.text = if (on) T.x.repOn else T.x.repOff
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
        val sheet = SheetDialog(activity, T.a.menuAdmin)

        sheet.wideButton(T.x.newCommanderCode, top = 12) { askNames() }
        if (requests.isNotEmpty()) {
            sheet.wideButton(T.x.repRequestsBtn(requests.size), Kind.OUTLINE, top = 10) { showRequests(requests, admins) }
        }
        sheet.equalRow(
            sheet.pill(T.x.allRooms) { showRooms() },
            sheet.pill(T.x.sendList) { shareList(AdminRoster.exportText(admins, codes, now), codes.count { !it.used && now < it.expiresAt }) },
            sheet.pill(T.x.purge) { confirmPurge() },
            top = 10
        )

        sheet.heading(T.x.registeredHead(admins.size))
        if (admins.isEmpty()) sheet.line(sheet.card(), T.x.noneYetDot)
        for (a in admins) {
            val label = AdminRoster.labelOf(a, codes)
            val card = sheet.card()
            sheet.line(card, "$label · ID …${a.uid.takeLast(6)}", bold = true)
            // 소속과 대표 여부. 소속이 없으면 눈에 띄게 적는다(전에 등록된 지휘관은 개발자가 정해 준다).
            sheet.line(card, RallyRoles.groupLabel(a.group) + if (a.rep) T.x.repSuffix else "", small = true, bold = true, top = 2).apply {
                setTextColor(if (a.group.isEmpty()) android.graphics.Color.parseColor("#B45309") else SheetDialog.BLUE)
            }
            sheet.line(card, T.x.activityRegistered(AdminRoster.activityText(a.lastSeen, a.appVersion, now), day.format(Date(a.registeredAt))), small = true, top = 2)
            sheet.pillRow(card,
                sheet.pill(if (a.group.isEmpty()) T.a.setGroup else T.x.changeGroup, if (a.group.isEmpty()) Kind.PRIMARY else Kind.NORMAL) { askGroup(a, label) },
                sheet.pill(T.x.rename) { askRename(a, label) },
                sheet.pill(T.a.delete, Kind.DANGER) { confirmRemove(a, label) }
            )
        }

        sheet.heading(T.x.pendingHead(pending.size))
        if (pending.isEmpty()) sheet.line(sheet.card(), T.x.noneDot)
        for (p in pending) {
            val card = sheet.card()
            sheet.line(card, "${p.code} · ${p.name}", bold = true)
            sheet.line(card, RallyRoles.groupLabel(p.group) + (if (p.rep) T.x.repSuffix else "") + " · " + T.x.remainLeft(remainText(p.expiresAt - now)), small = true, top = 2)
            sheet.pillRow(card,
                sheet.pill(T.x.send) { TextShare.sheet(activity, T.x.sendCodeTitle, T.x.commanderCode, AdminRoster.shareMessage(p.code, ttlOf(p), p.group, p.rep)) },
                sheet.pill(T.s.cancel, Kind.DANGER) { cancelCode(p) }
            )
        }

        sheet.actions(act(T.s.close))
        manageDialog = sheet
        sheet.show()
    }

    /** 개발자: 지휘관의 소속과 대표 여부를 정한다. 정해진 지휘관은 그 소속의 방만 만들고 고칠 수 있다. */
    private fun askGroup(a: AdminEntry, label: String) {
        val sheet = SheetDialog(activity, T.a.setGroup, T.x.setGroupBody(label, a.uid.takeLast(6)))
        val fields = groupFields(sheet, RallyGroup.fromId(a.group))
        val isRep = repToggle(sheet, a.rep)
        sheet.actions(
            act(T.s.cancel),
            SheetDialog.Action(T.a.save, Kind.PRIMARY) {
                val group = readGroup(fields) ?: return@Action false
                val rep = isRep()
                Thread {
                    val err = server.setAdminGroup(a.uid, group, rep)
                    ui { toast(err ?: T.x.groupSetTo(group.label)) }
                    load()
                }.start()
                true
            }
        ).show()
    }

    /** 개발자: 연맹 대표 신청 목록. 승인하면 그 기기가 신청한 소속의 대표가 된다. */
    private fun showRequests(requests: List<RepRequest>, admins: List<AdminEntry>) {
        val now = System.currentTimeMillis()
        val sheet = SheetDialog(activity, T.x.requestsTitle(requests.size), T.x.requestsBody)
        for (r in requests) {
            val card = sheet.card()
            sheet.line(card, "${RallyRoles.groupLabel(r.group)} · ${r.name}", bold = true)
            sheet.line(card, T.x.requestedAgo(agoText(r.createdAt, now), r.uid.takeLast(6)), small = true, top = 2)
            sheet.line(card, if (r.note.isEmpty()) T.x.noNote else "\"${r.note}\"", top = 6)
            RallyRoles.requestWarning(r, requests, admins, now)?.let { warn ->
                sheet.line(card, warn, small = true, bold = true, top = 6).setTextColor(android.graphics.Color.parseColor("#B45309"))
            }
            sheet.pillRow(card,
                sheet.pill(T.x.reject, Kind.DANGER) { sheet.dismiss(); askReject(r) },
                sheet.pill(T.x.approve, Kind.PRIMARY) { sheet.dismiss(); confirmApprove(r, admins) }
            )
        }
        sheet.actions(act(T.s.close)).show()
    }

    private fun confirmApprove(r: RepRequest, admins: List<AdminEntry>) {
        val old = RallyRoles.currentRep(r, admins)
        val swap = if (old == null) "" else T.x.swapNote(old.name.ifEmpty { T.x.noName })
        val after = RallyRoles.commandersAfterApprove(r, admins)
        val over = if (after <= RallyRoles.MAX_COMMANDERS) "" else T.x.overNote(after, RallyRoles.MAX_COMMANDERS)
        SheetDialog.confirm(
            activity, if (old == null) T.x.approveTitle else T.x.replaceTitle,
            T.x.approveBody(r.name, RallyRoles.groupLabel(r.group), swap + over),
            T.x.approve
        ) {
            Thread {
                val err = server.approveRequest(r, old?.uid)
                ui { toast(err ?: T.x.madeRep(r.name)) }
                load()
            }.start()
        }
    }

    private fun askReject(r: RepRequest) {
        InputSheet(
            activity, T.x.rejectTitle, T.x.rejectBody(RallyRoles.groupLabel(r.group), r.name),
            fields = listOf(InputSheet.Field(hint = T.x.rejectHint, maxLength = RallyRoles.MAX_NOTE, inputType = InputType.TYPE_CLASS_TEXT)),
            submitLabel = T.x.reject
        ) { v ->
            Thread {
                val err = server.rejectRequest(r.uid, v[0])
                ui { toast(err ?: T.x.rejected) }
                load()
            }.start()
            true
        }.show()
    }

    // ---- 연맹 대표: 우리 연맹 지휘관 ----

    /** 연맹 대표인지 서버에서 확인하고, 맞으면 자기 소속의 지휘관을 정하고 빼는 화면을 연다. */
    fun showMyAlliance() {
        toast(T.x.checkingServer)
        Thread {
            val uid = server.uid().value
            if (uid == null) { ui { toast(T.x.noDeviceIdNet) }; return@Thread }
            val me = server.myAdmin(uid).value
            if (me == null || !me.rep || RallyGroup.fromId(me.group) == null) { ui { toast(T.x.repOnlyToast) }; return@Thread }
            loadAlliance(uid, me)
        }.start()
    }

    private fun loadAlliance(uid: String, me: AdminEntry) {
        val admins = server.listGroupAdmins(me.group)
        val codes = server.listMyCodes(uid)
        ui {
            val a = admins.value
            val c = codes.value
            if (a == null || c == null) { toast(admins.error ?: codes.error ?: T.x.listLoadFailed); return@ui }
            renderAlliance(uid, me, a, c)
        }
    }

    private fun renderAlliance(uid: String, me: AdminEntry, admins: List<AdminEntry>, codes: List<AdminCode>) {
        manageDialog?.dismiss()
        val now = System.currentTimeMillis()
        val pending = codes.filter { !it.used && now < it.expiresAt }
        val sheet = SheetDialog(activity, T.a.menuAlliance, RallyRoles.groupLabel(me.group))
        if (RallyRoles.canAddCommander(admins.size, pending.size)) {
            sheet.wideButton(T.x.newCommanderCode, top = 12) { askAllianceCode(uid, me) }
            sheet.line(sheet.content, T.x.allianceCodeNote(RallyRoles.groupLabel(me.group)), small = true, top = 6)
        } else {
            sheet.line(sheet.card(top = 12), T.x.allianceFull(RallyRoles.MAX_COMMANDERS))
        }

        sheet.heading(T.x.allianceHead(admins.size, RallyRoles.MAX_COMMANDERS))
        for (a in admins) {
            val label = AdminRoster.labelOf(a, codes)
            val card = sheet.card()
            sheet.line(card, label + (if (a.uid == uid) T.x.meSuffix else "") + (if (a.rep) T.x.repShort else ""), bold = true)
            sheet.line(card, AdminRoster.activityText(a.lastSeen, a.appVersion, now), small = true, top = 2)
            if (!a.rep && a.uid != uid) {
                sheet.pillRow(card, sheet.pill(T.a.delete, Kind.DANGER) {
                    SheetDialog.confirm(activity, T.x.removeCommanderTitle, T.x.removeCommanderBody(label), T.a.delete, danger = true) {
                        Thread {
                            val err = server.removeAdmin(a.uid)
                            ui { toast(err ?: T.x.removed) }
                            loadAlliance(uid, me)
                        }.start()
                    }
                })
            }
        }

        sheet.heading(T.x.pendingHead(pending.size))
        if (pending.isEmpty()) sheet.line(sheet.card(), T.x.noneDot)
        for (p in pending) {
            val card = sheet.card()
            sheet.line(card, "${p.code} · ${p.name}", bold = true)
            sheet.line(card, T.x.remainLeft(remainText(p.expiresAt - now)), small = true, top = 2)
            sheet.pillRow(card,
                sheet.pill(T.x.send) { TextShare.sheet(activity, T.x.sendCodeTitle, T.x.commanderCode, AdminRoster.shareMessage(p.code, ttlOf(p), p.group, false)) },
                sheet.pill(T.s.cancel, Kind.DANGER) {
                    Thread {
                        val err = server.cancelCode(p.code)
                        ui { toast(err ?: T.x.codeCancelled) }
                        loadAlliance(uid, me)
                    }.start()
                }
            )
        }
        sheet.actions(act(T.s.close))
        manageDialog = sheet
        sheet.show()
    }

    /** 연맹 대표가 자기 소속의 지휘관 코드를 하나 만든다(24시간, 한 번만 쓸 수 있다). */
    private fun askAllianceCode(uid: String, me: AdminEntry) {
        InputSheet(
            activity, T.x.newCodeTitle, T.x.newCodeBodyRep(RallyRoles.groupLabel(me.group)),
            fields = listOf(InputSheet.Field(hint = T.x.labelHint, maxLength = AdminRoster.MAX_NAME, inputType = InputType.TYPE_CLASS_TEXT)),
            submitLabel = T.a.create
        ) { v ->
            if (AdminRoster.cleanName(v[0]).isEmpty()) { toast(T.x.labelRequired); false } else {
                Thread {
                    val r = server.createCode(v[0], CodeTtl.DAY, group = me.group, by = uid)
                    val code = r.value
                    ui {
                        if (code == null) toast(r.error ?: T.x.codeMakeFailed)
                        else TextShare.sheet(activity, T.x.sendCodeTitle, T.x.commanderCode, AdminRoster.shareMessage(code, CodeTtl.DAY, me.group, false))
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
    /** [preview]는 개발자가 일반 화면 미리보기에서 시험해 보는 경우다. 이미 등록된 기기라도 신청 화면을 그대로 보여 준다. */
    fun showRepRequest(onApproved: () -> Unit, onChanged: () -> Unit, preview: Boolean = false) {
        toast(T.x.checkingServer)
        Thread {
            val uid = server.uid().value
            if (uid == null) { ui { toast(T.x.noDeviceIdNet) }; return@Thread }
            if (!preview && server.isAdmin(uid) == Check.YES) {
                ui { prefs().edit().remove(KEY_REQUEST).apply(); onApproved() }
                return@Thread
            }
            val r = server.myRequest(uid)
            ui {
                val req = r.value
                when {
                    r.error != null -> toast(T.x.rulesMaybe(r.error.orEmpty()))
                    // 미리보기에서 신청해 둔 것이 사라졌으면 승인된 것이다(승인하면 신청이 지워진다). 실제 사용자는 여기서 지휘관 화면으로 넘어간다.
                    req == null && preview && prefs().getString(KEY_REQUEST, null) != null -> {
                        prefs().edit().remove(KEY_REQUEST).apply(); onChanged()
                        toast(T.x.approvedPreview)
                    }
                    req == null -> { prefs().edit().remove(KEY_REQUEST).apply(); onChanged(); askRequest(uid, onChanged) }
                    req.pending -> { prefs().edit().putString(KEY_REQUEST, req.group).apply(); onChanged(); showPending(uid, req, onChanged) }
                    else -> { prefs().edit().remove(KEY_REQUEST).apply(); onChanged(); showRejected(uid, req, onChanged) }
                }
            }
        }.start()
    }

    private fun askRequest(uid: String, onChanged: () -> Unit) {
        val sheet = SheetDialog(
            activity, T.x.repRequestTitle,
            T.x.repRequestBody
        )
        val fields = groupFields(sheet, RallyGroup.load(prefs()))
        val name = sheet.field(T.x.gameName, AdminRoster.MAX_NAME, InputType.TYPE_CLASS_TEXT, PreferencesHelper.getRallyCharacterName(activity), top = 10)
        val note = sheet.field(T.x.noteHint, RallyRoles.MAX_NOTE, InputType.TYPE_CLASS_TEXT, top = 10)
        sheet.actions(
            act(T.s.cancel),
            SheetDialog.Action(T.x.applyBtn, Kind.PRIMARY) {
                val group = readGroup(fields) ?: return@Action false
                if (AdminRoster.cleanName(name.text.toString()).isEmpty()) { toast(T.x.gameNameRequired); return@Action false }
                Thread {
                    val err = server.submitRequest(uid, group, name.text.toString(), note.text.toString())
                    ui {
                        if (err != null) toast(err) else {
                            prefs().edit().putString(KEY_REQUEST, group.id).apply()
                            toast(T.x.applied(group.label))
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
            activity, T.x.pendingTitle,
            T.x.pendingBody(RallyRoles.groupLabel(req.group), req.name, agoText(req.createdAt, System.currentTimeMillis()))
        ).actions(
            act(T.s.close),
            act(T.x.cancelRequest, Kind.DANGER) {
                Thread {
                    val err = server.cancelRequest(uid)
                    ui {
                        if (err != null) toast(err) else { prefs().edit().remove(KEY_REQUEST).apply(); toast(T.x.requestCancelled); onChanged() }
                    }
                }.start()
            }
        ).show()
    }

    private fun showRejected(uid: String, req: RepRequest, onChanged: () -> Unit) {
        SheetDialog(
            activity, T.x.notApprovedTitle,
            "${RallyRoles.groupLabel(req.group)} · ${req.name}\n" + if (req.reason.isEmpty()) T.x.noReason else T.x.devSaid(req.reason)
        ).actions(
            act(T.s.close),
            act(T.x.reapply, Kind.PRIMARY) { askRequest(uid, onChanged) }
        ).show()
    }

    /** 서버의 모든 방을 목록으로 보여 준다. 방을 누르면 군단과 명단을 읽기 전용으로 보여 준다. */
    private fun showRooms() {
        toast(T.a.loadingRooms)
        Thread {
            val r = server.loadRooms()
            ui {
                val data = r.value
                if (data == null) { toast(r.error ?: T.x.roomsLoadFailed); return@ui }
                val rooms = RoomList.summarize(data.rooms, data.members)
                if (rooms.isEmpty()) { toast(T.x.noRoomsMade); return@ui }
                val sheet = SheetDialog(activity, T.x.allRoomsTitle(rooms.size))
                rooms.forEach { r ->
                    val card = sheet.card()
                    sheet.line(card, RoomList.line(r))
                    card.setOnClickListener {
                        sheet.dismiss()
                        val room = data.rooms?.get(r.code) as? Map<*, *> ?: return@setOnClickListener
                        showRoomDetail(r.code, room, data.members?.get(r.code) as? Map<*, *>)
                    }
                }
                sheet.actions(act(T.s.close)).show()
            }
        }.start()
    }

    private fun showRoomDetail(code: String, room: Map<*, *>, members: Map<*, *>?) {
        val text = RoomList.detail(code, room, members)
        val sheet = SheetDialog(activity, T.s.room(code))
        sheet.line(sheet.card(top = 12), text)
        sheet.actions(
            act(T.s.close),
            act(T.a.copy) { copy(T.s.room(code), text) },
            act(T.x.deleteRoom, Kind.DANGER) { confirmDeleteRoom(code) }
        ).show()
    }

    private fun confirmDeleteRoom(code: String) {
        SheetDialog.confirm(
            activity, T.a.deleteRoomDesc(code),
            T.x.deleteRoomConfirm,
            T.a.delete, danger = true
        ) {
            Thread {
                val err = server.deleteRoom(code)
                ui { toast(err ?: T.x.roomRemoved(code)) }
            }.start()
        }
    }

    /** 코드를 만들 때 고른 기간에 가장 가까운 것. 안내 문구에 "24시간 안에"처럼 적는 데 쓴다. */
    private fun ttlOf(p: AdminCode): CodeTtl =
        CodeTtl.values().minByOrNull { kotlin.math.abs(it.ms - (p.expiresAt - p.createdAt)) } ?: CodeTtl.DAY

    private fun remainText(ms: Long): String = when {
        ms >= 86_400_000L -> T.x.days(ms / 86_400_000L)
        ms >= 3_600_000L -> T.x.hours(ms / 3_600_000L)
        else -> T.x.minutes((ms / 60_000L).coerceAtLeast(0))
    }

    /** 이름표를 한 줄에 한 명씩 적으면 사람마다 코드를 만든다. 코드를 쓸 수 있는 기간도 고른다. */
    private fun askNames() {
        val sheet = SheetDialog(activity, T.x.newCodeTitle, T.x.newCodeBodyDev)
        val fields = groupFields(sheet, RallyGroup.load(prefs()))
        val isRep = repToggle(sheet, false)
        val input = sheet.field(
            hint = T.x.labelsHint(AdminRoster.MAX_BATCH),
            maxLength = 400,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            lines = 3
        )
        sheet.heading(T.x.ttlHead, top = 16)
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
            act(T.s.cancel),
            SheetDialog.Action(T.a.create, Kind.PRIMARY) {
                val group = readGroup(fields) ?: return@Action false
                val rep = isRep()
                val names = AdminRoster.parseNames(input.text.toString())
                if (names.isEmpty()) { toast(T.x.labelRequired); return@Action false }
                if (rep && names.size > 1) { toast(T.x.repOneAtATime); return@Action false }
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
                        if (made.isEmpty()) { toast(firstError ?: T.x.codeMakeFailed); return@ui }
                        copy(T.x.commanderCode, AdminRoster.batchShare(made, chosen, group.id, rep))
                        val warn = if (firstError != null) T.x.someFailed(firstError.orEmpty()) else ""
                        val done = SheetDialog(
                            activity, T.x.codesMadeTitle(made.size),
                            T.x.codesMadeBody(group.label + (if (rep) T.x.repSuffix else ""), chosen.label, warn)
                        )
                        made.forEach { done.line(done.card(), "${it.first}  ${it.second}", bold = true) }
                        done.actions(act(T.s.ok, Kind.PRIMARY) { Thread { load() }.start() }).show()
                    }
                }.start()
                true
            }
        ).show()
    }

    private fun askRename(a: AdminEntry, current: String) {
        InputSheet(
            activity, T.x.rename, "ID …${a.uid.takeLast(6)}",
            fields = listOf(InputSheet.Field(
                hint = T.x.newLabelHint, maxLength = AdminRoster.MAX_NAME, inputType = InputType.TYPE_CLASS_TEXT,
                initial = if (current != T.x.directReg) current else ""
            )),
            submitLabel = T.a.save
        ) { v ->
            Thread {
                val err = server.renameAdmin(a.uid, v[0])
                ui { if (err != null) toast(err) else toast(T.x.renamed) }
                load()
            }.start()
            true
        }.show()
    }

    private fun confirmPurge() {
        SheetDialog.confirm(
            activity, T.x.purge,
            T.x.purgeBody,
            T.x.purgeBtn
        ) {
            Thread {
                val r = server.purgeCodes()
                ui { val n = r.value; if (n == null) toast(r.error ?: T.x.purgeFailed) else toast(T.x.purged(n)) }
                load()
            }.start()
        }
    }

    private fun confirmRemove(a: AdminEntry, label: String) {
        SheetDialog.confirm(
            activity, T.x.removeCommanderTitle,
            T.x.removeCommanderBodyId(label, a.uid.takeLast(6)),
            T.a.delete, danger = true
        ) {
            Thread {
                val err = server.removeAdmin(a.uid)
                ui { if (err != null) toast(err) else toast(T.x.removed) }
                load()
            }.start()
        }
    }

    private fun cancelCode(p: AdminCode) {
        Thread {
            val err = server.cancelCode(p.code)
            ui { if (err != null) toast(err) else toast(T.x.codeCancelled) }
            load()
        }.start()
    }
}
