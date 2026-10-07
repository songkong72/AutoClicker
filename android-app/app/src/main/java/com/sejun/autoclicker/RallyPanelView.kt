package com.sejun.autoclicker

import android.content.Context
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

/**
 * 통합 집결 화면. 계산은 RallyScreenModel이 하고, 이 클래스는 결과를 그리기만 한다.
 * 윈도우 추가/제거와 드래그는 호출하는 쪽(AutoClickService)이 맡는다.
 */
class RallyPanelView(context: Context, private val callbacks: Callbacks) {

    private companion object {
        /** 목록이 화면에서 차지할 수 있는 높이: 화면 높이의 이 비율에서 목록 밖 영역(머리·버튼)을 뺀 만큼. */
        const val PANEL_MAX_SCREEN_RATIO = 0.84f
        const val ADJUST_STEP_MS = 500
        val PHASE_COLORS = listOf("#FBBF24", "#60A5FA", "#A78BFA", "#22C55E")
    }

    interface Callbacks {
        fun onStart()
        fun onStop()
        fun onMinimize()
        fun onClose()
        fun onTitleTap()
        fun onMarchDelta(teamId: String, deltaSec: Double)
        fun onToggleExclude(teamId: String)
        /** 관리자가 군단 이름을 눌렀을 때: 그 군단을 맡을 사람을 고른다. */
        fun onAssignLeader(teamId: String, teamName: String)
        /** "내 기기"의 캐릭터명 줄을 눌렀을 때 */
        fun onEditCharacterName(current: String)
        /** "내 기기"의 방 줄을 눌렀을 때: 다른 방으로 옮긴다. */
        fun onEditRoom(current: String)
        fun onCorrectionDelta(deltaMs: Int)
        fun onEditCorrection(currentMs: Int)
        fun onEditMarch(teamId: String, currentSec: Double)
        fun onAddTeam()
        fun onEditPrep(currentSec: Double)
        fun onSetWait(sec: Double)
        fun onEditAdminAdjust(teamId: String, teamName: String, currentMs: Int)
        /** 보정 −/+ 버튼: 이 군단의 관리자 보정을 ms로 정한다(범위는 서버 쪽 편집 규칙이 맞춘다). */
        fun onSetAdminAdjust(teamId: String, ms: Int)
        fun onRemoveTeam(teamId: String)
        fun onSavePosition()
    }

    private val themed = ContextThemeWrapper(context, R.style.Theme_AutoClicker)
    val root: View = LayoutInflater.from(themed).inflate(R.layout.layout_rally_panel, null)

    private var prepShown = 0.0
    private var correctionShownMs = 0
    private var charNameShown = ""
    private var roomShown = ""
    private val title = root.findViewById<TextView>(R.id.rallyTitle)
    private val heroLabel = root.findViewById<TextView>(R.id.rallyHeroLabel)
    private val heroTime = root.findViewById<TextView>(R.id.rallyHeroTime)
    private val heroSub = root.findViewById<TextView>(R.id.rallyHeroSub)
    private val heroProgress = root.findViewById<ProgressBar>(R.id.rallyHeroProgress)
    private val warning = root.findViewById<TextView>(R.id.rallyWarning)
    private val rows = root.findViewById<LinearLayout>(R.id.rallyRows)
    private val rowsHead = root.findViewById<View>(R.id.rallyRowsHead)
    private val rowsHeadLeft = root.findViewById<TextView>(R.id.rallyRowsHeadLeft)
    private val miniTime = root.findViewById<TextView>(R.id.rallyMiniTime)
    /** 편집 모드에서 펼쳐 둔 군단 줄(한 번에 하나). */
    private var expandedId: String? = null
    private val rowsScroll = root.findViewById<MaxHeightScrollView>(R.id.rallyRowsScroll)
    /** 패널을 끌어 옮기는 틀이 이 목록의 세로 스크롤은 건드리지 않게 알려 주기 위한 참조. */
    val rowsScrollView: View get() = rowsScroll
    private val adminBar = root.findViewById<View>(R.id.rallyAdminBar)
    private val btnStart = root.findViewById<TextView>(R.id.rallyBtnStart)
    private val btnStop = root.findViewById<TextView>(R.id.rallyBtnStop)
    private val blocked = root.findViewById<TextView>(R.id.rallyBlockedReason)

    init {
        // 패널의 다른 부분 높이가 바뀌면(편집 모드, 안내 줄 등) 목록이 쓸 수 있는 높이도 다시 맞춘다.
        root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> fitRowsHeight() }
        root.findViewById<View>(R.id.rallyMinimize).setOnClickListener { callbacks.onMinimize() }
        root.findViewById<View>(R.id.rallyClose).setOnClickListener { callbacks.onClose() }
        // ✎: 관리자 편집 모드. 평소엔 읽기 전용으로 깔끔하게, 켜면 −/+ · ✕ · 밑줄(눌러서 고치기)이 나타난다.
        root.findViewById<View>(R.id.rallyEdit).setOnClickListener { editMode = !editMode; expandedId = null; deleteGuard.reset(); lastRender?.invoke() }
        // 카운트다운 상자(단계명·시간·안내)를 탭하면 축소/확대된다. 작은 —/▢ 버튼 옆의 ✕를 잘못 누르지 않게 큰 영역으로도 누를 수 있다.
        // 알약(최소화 상태)에서는 단계명이나 시간을 탭하면 펼쳐진다. 창을 끄는 일은 ✕만 한다.
        listOf<View>(heroLabel, heroTime, heroSub, miniTime).forEach { v -> v.setOnClickListener { callbacks.onMinimize() } }
        title.setOnClickListener { if (isMinimized) callbacks.onMinimize() else callbacks.onTitleTap() }
        btnStart.setOnClickListener { callbacks.onStart() }
        btnStop.setOnClickListener { callbacks.onStop() }
        listOf<View>(btnStart, btnStop).forEach { pressFeel(it) }
        root.findViewById<TextView>(R.id.devToggle).setOnClickListener { setDeviceOpen(!deviceOpen) }
        // 준비 안내("캐릭터명을 먼저…", "클릭 위치를 먼저…")를 누르면 찾아갈 필요 없이 바로 해당 입력으로 간다
        warning.setOnClickListener {
            when {
                RallyScreenModel.WARN_NAME in warningsShown -> callbacks.onEditCharacterName(charNameShown)
                RallyScreenModel.WARN_POSITION in warningsShown -> setDeviceOpen(true)
            }
        }
        root.findViewById<View>(R.id.rallyAddTeam).setOnClickListener { callbacks.onAddTeam() }
        root.findViewById<View>(R.id.setPrep).setOnClickListener { callbacks.onEditPrep(prepShown) }
        root.findViewById<View>(R.id.setWait3).setOnClickListener { callbacks.onSetWait(180.0) }
        root.findViewById<View>(R.id.setWait5).setOnClickListener { callbacks.onSetWait(300.0) }
        root.findViewById<View>(R.id.setWait10).setOnClickListener { callbacks.onSetWait(600.0) }
        root.findViewById<View>(R.id.devRoom).setOnClickListener { callbacks.onEditRoom(roomShown) }
        root.findViewById<View>(R.id.devCharName).setOnClickListener { callbacks.onEditCharacterName(charNameShown) }
        root.findViewById<View>(R.id.devMs).setOnClickListener { callbacks.onEditCorrection(correctionShownMs) } // 눌러서 초 단위로 직접 입력
        // 보정 버튼은 ±0.5초 둘만 둔다. 그보다 세밀하거나 큰 값은 가운데 숫자를 눌러 직접 입력한다.
        listOf(R.id.devMinus500 to -500, R.id.devPlus500 to 500).forEach { (id, ms) ->
            root.findViewById<View>(id).setOnClickListener { callbacks.onCorrectionDelta(ms) }
        }
        root.findViewById<TextView>(R.id.devSavePos).let { b ->
            val label = b.text
            val color = b.currentTextColor
            val restore = Runnable { b.text = label; b.setTextColor(color) }
            b.setOnClickListener {
                callbacks.onSavePosition()
                // 저장됐다는 걸 눈과 손으로 바로 알 수 있게: 진동 + 버튼이 잠깐 초록 "✓ 저장됨"으로 바뀐다
                b.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                b.text = "✓ 저장됨"
                b.setTextColor(Color.parseColor("#22C55E"))
                b.removeCallbacks(restore)
                b.postDelayed(restore, 1500L)
            }
        }
    }

    /** 눌린 느낌: 누르는 동안 살짝 작아지고 어두워지며, 뗄 때 짧게 진동한다. 비활성 버튼은 반응하지 않는다. */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun pressFeel(v: View) {
        v.setOnTouchListener { view, e ->
            if (view.isEnabled) when (e.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    view.animate().scaleX(0.94f).scaleY(0.94f).alpha(0.75f).setDuration(60).start()
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL ->
                    view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(120).start()
            }
            false // 클릭 이벤트는 그대로 전달
        }
    }

    /** "내 기기" 줄: 캐릭터명, 현재 ms 보정, 저장된 클릭 위치 표시. */
    private var editMode = false
    /** 군단 삭제는 ✕ → "삭제?" 두 번 눌러야 한다(+ 버튼 바로 옆이라 잘못 누르기 쉽다) */
    private val deleteGuard = DeleteGuard()
    private var lastRender: (() -> Unit)? = null
    private var deviceOpen = false
    private var positionSavedShown = true
    private var warningsShown: List<String> = emptyList()

    /** "내 기기"를 펼치거나 접는다. 접혀 있을 때는 제목 줄에 한 줄 요약을 보여 준다. */
    private fun setDeviceOpen(open: Boolean) {
        deviceOpen = open
        root.findViewById<View>(R.id.devSection).visibility = if (open) View.VISIBLE else View.GONE
        showDeviceToggle()
    }

    private fun showDeviceToggle() {
        root.findViewById<TextView>(R.id.devToggle).text =
            if (deviceOpen) "내 기기 ▴"
            else "내 기기 ▾  " + RallyPanelFormat.deviceSummary(charNameShown, roomShown, RallyInputParse.formatCorrection(correctionShownMs), positionSavedShown)
    }

    /** "내 기기"의 현재 방 줄. */
    fun renderRoom(code: String) {
        roomShown = code
        showDeviceToggle()
        root.findViewById<TextView>(R.id.devRoom).apply {
            text = if (code.isEmpty()) "방 없음 (눌러서 입장)" else "방  $code  (눌러서 선택)"
            paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
        }
    }

    private var detailOpen = false
    private var lastPosText = ""
    private var lastDetailText = ""

    private fun showDeviceStatus() {
        val hint = if (detailOpen) "▴ 자세히 접기" else "▾ 자세히 (기기 ID·저장 위치·진단)"
        root.findViewById<TextView>(R.id.devPosStatus).text =
            listOf(lastPosText, if (detailOpen) lastDetailText else "", hint).filter { it.isNotEmpty() }.joinToString("\n")
    }

    fun renderDevice(correctionMs: Int, posText: String, characterName: String, detailText: String = "", positionSaved: Boolean = true) {
        charNameShown = characterName
        positionSavedShown = positionSaved
        root.findViewById<TextView>(R.id.devCharName).apply {
            text = if (characterName.isBlank()) "캐릭터명 등록하기 (눌러서 입력)" else "캐릭터명  $characterName  (눌러서 변경)"
            paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
        }
        correctionShownMs = correctionMs
        root.findViewById<TextView>(R.id.devMs).text = RallyInputParse.formatCorrection(correctionMs)
        lastPosText = posText
        lastDetailText = detailText
        root.findViewById<TextView>(R.id.devPosStatus).setOnClickListener { detailOpen = !detailOpen; showDeviceStatus() }
        showDeviceStatus()
        showDeviceToggle()
    }

    fun render(model: ScreenModel, isAdmin: Boolean, hasStarted: Boolean = false, arrivalNote: String = "",
               conn: RallyConnection = RallyConnection.LIVE, urgent: Boolean = false, starting: Boolean = false) {
        val hero = model.hero
        val name = if (isAdmin) "관리자" else "집결장"
        // 연결 상태 점: 초록=실시간, 주황=1초 확인, 빨강=끊김. 알약(최소화)에서는 단계 표시가 대신 쓴다.
        title.text = if (isMinimized) name else android.text.SpannableString("● $name").apply {
            val c = when (conn) { RallyConnection.LIVE -> "#22C55E"; RallyConnection.POLLING -> "#F59E0B"; RallyConnection.OFFLINE -> "#EF4444" }
            setSpan(android.text.style.ForegroundColorSpan(Color.parseColor(c)), 0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        lastRender = { render(model, isAdmin, hasStarted, arrivalNote, conn, urgent, starting) }
        val editing = isAdmin && model.editable && editMode
        root.findViewById<TextView>(R.id.rallyEdit).apply {
            visibility = if (isAdmin && model.editable) View.VISIBLE else View.GONE
            // 편집 중에는 "완료" 버튼, 평소에는 ✎
            text = if (editMode) "완료" else "✎"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, if (editMode) 15f else 20f)
            setTypeface(null, if (editMode) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            setTextColor(Color.parseColor(if (editMode) "#FFFFFF" else "#A9B4C7"))
            if (editMode) setBackgroundResource(R.drawable.bg_btn_primary) else background = null
        }
        heroLabel.text = if (hero.note == null) hero.label else "${hero.label} · ${hero.note}"
        // 큰 시간이 없는 화면(대기·취소 등)에서는 단계 이름을 알림 모양으로 보여 준다.
        val dpr = root.resources.displayMetrics.density
        if (hero.remainingSec == null && !isMinimized) {
            heroLabel.setBackgroundResource(R.drawable.bg_status_pill)
            heroLabel.setPadding((16 * dpr).toInt(), (6 * dpr).toInt(), (16 * dpr).toInt(), (6 * dpr).toInt())
            heroLabel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 17f)
            heroLabel.setTypeface(null, android.graphics.Typeface.BOLD)
            heroLabel.setTextColor(Color.parseColor("#F1F5F9"))
        } else {
            heroLabel.background = null
            heroLabel.setPadding(0, 0, 0, 0)
            heroLabel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
            heroLabel.setTypeface(null, android.graphics.Typeface.NORMAL)
            heroLabel.setTextColor(Color.parseColor("#CBD5E1"))
        }
        // 대기 중에는 "전원 도착 예정" 총 소요 시간을 흐리게 보여준다
        val previewTotal = hero.kind == HeroKind.IDLE && hero.remainingSec == null && model.arriveAtSec > 0.0
        heroTime.text = when {
            hero.remainingSec != null -> RallyScreenModel.formatMmSs(hero.remainingSec)
            previewTotal -> RallyScreenModel.formatMmSs(model.arriveAtSec)
            else -> ""
        }
        // 편집 모드에서는 시간·안내를 접어 목록과 설정에 높이를 준다(좁은 폰에서 창이 화면을 넘지 않게).
        heroTime.visibility = if (heroTime.text.isEmpty() || editing || isMinimized) View.GONE else View.VISIBLE
        val timeColor = if (previewTotal) Color.parseColor("#64748B") else if (urgent) Color.parseColor("#F87171") else heroColor(hero)
        heroTime.setTextColor(timeColor)
        miniTime.text = heroTime.text
        miniTime.setTextColor(timeColor)
        val sub = if (previewTotal) RallyScreenModel.idleSub(hero.subLabel, model.rows.count { !it.excluded }) else hero.subLabel
        // 집결이 시작된 뒤에는 한 줄만: "도착 예정 15:53:24 · ✓ 클릭함 15:47:56.080" (단계 설명은 큰 숫자·단계 표시가 대신한다)
        heroSub.text = if (arrivalNote.isEmpty()) sub else arrivalNote
        heroSub.visibility = if (editing || isMinimized) View.GONE else View.VISIBLE
        renderPhases(hero.kind, hero.phase)
        heroProgress.progress = (hero.progress * 1000).toInt()

        warningsShown = model.warnings
        warning.visibility = if (model.warnings.isEmpty()) View.GONE else View.VISIBLE
        warning.text = model.warnings.joinToString("\n") { "⚠ $it" }

        renderRows(model.rows, isAdmin, model.editable, editing, hero.kind != HeroKind.IDLE && hero.kind != HeroKind.EXCLUDED, RallyTimelineScale(model.maxMarchSec), model.nowSec)

        adminBar.visibility = if (isAdmin) View.VISIBLE else View.GONE
        // 준비·집결 시간과 팀 추가는 가끔만 고치므로 편집 모드(✎)에서만 보인다. 팀이 하나도 없을 때는 바로 추가할 수 있게 보여 준다.
        val showSetup = if (editing || (isAdmin && model.editable && model.rows.isEmpty())) View.VISIBLE else View.GONE
        // 군단이 최대(10개)면 더 만들 수 없으니 버튼을 숨긴다.
        root.findViewById<View>(R.id.rallyAddTeam).visibility =
            if (model.rows.size >= RallyRoomEdit.MAX_TEAMS) View.GONE else showSetup
        prepShown = model.prepSec
        root.findViewById<View>(R.id.rallySettingsRow).visibility = showSetup
        root.findViewById<View>(R.id.setPrep).visibility = showSetup
        root.findViewById<TextView>(R.id.setPrep).text = "이동 준비 ${model.prepSec.toInt()}초"
        listOf(R.id.setWait3 to 180.0, R.id.setWait5 to 300.0, R.id.setWait10 to 600.0).forEach { (id, sec) ->
            root.findViewById<TextView>(id).setTextColor(Color.parseColor(if (model.waitSec == sec) "#60A5FA" else "#CBD5E1"))
        }
        val canRegroup = model.hero.kind == HeroKind.ARRIVED || model.hero.kind == HeroKind.CANCELLED
        // 서버에 시작을 쓰는 동안은 "시작하는 중…"으로 바꾸고 다시 눌리지 않게 한다.
        btnStart.text = if (starting) "시작하는 중…" else if (canRegroup || hasStarted) "다시 집결" else "집결 시작"
        btnStart.isEnabled = model.startBlockedReason == null && !starting
        // 지금 할 수 있는 버튼 하나만 넓게: 진행 중에는 "집결 취소", 그 밖에는 "집결 시작"/"다시 집결"
        val running = !model.editable
        btnStart.visibility = if (running) View.GONE else View.VISIBLE
        btnStop.visibility = if (running) View.VISIBLE else View.GONE
        blocked.visibility = if (isAdmin && model.startBlockedReason != null) View.VISIBLE else View.GONE
        blocked.text = model.startBlockedReason ?: ""
    }

    private fun renderRows(list: List<TeamRowModel>, isAdmin: Boolean, editable: Boolean, editing: Boolean, showBars: Boolean, scale: RallyTimelineScale, nowSec: Double?) {
        // 팀 수가 적고(≤ 10개) 1초 단위 갱신이라, 줄 수가 같으면 재사용한다.
        if (rows.childCount != list.size) {
            rows.removeAllViews()
            val inflater = LayoutInflater.from(themed)
            repeat(list.size) { rows.addView(inflater.inflate(R.layout.item_rally_team_row, rows, false)) }
        }
        if (expandedId != null && list.none { it.id == expandedId }) expandedId = null
        // 머리글: 줄마다 풀어 쓸 자리가 없어 숫자의 뜻을 한 번만 적는다. 편집 모드에서는 체크박스의 뜻도 적는다.
        rowsHeadLeft.text = if (editing) "☑ 참여 · 줄을 눌러 펼치기" else "군단"
        rowsHead.visibility = if (list.isEmpty() || isMinimized) View.GONE else View.VISIBLE
        // 목록은 화면에 남는 높이까지만 늘어나고, 더 길면 그 안에서 스크롤한다.
        rows.post { fitRowsHeight() }
        val dp = root.resources.displayMetrics.density
        // 내 줄에는 이 폰의 "내 보정"(내 기기)도 더한다. 다른 집결장 폰의 보정은 방 데이터에 없어 알 수 없다.
        val lags = RallyPanelFormat.lagLabels(list.map { LagInput(it.marchSec, it.adminAdjustMs + (if (it.isMine) correctionShownMs else 0), it.excluded) })
        list.forEachIndexed { i, r ->
            val v = rows.getChildAt(i)
            // 행군시간 고치기: 관리자는 편집 모드에서 모든 군단, 집결장은 평소에도 내 군단만
            val canMarch = editable && !r.excluded && (if (isAdmin) editing else r.isMine)
            // 펼침: 관리자는 편집 모드에서 누른 줄 하나, 집결장은 내 군단 줄(행군시간만)
            val leaderOwn = !isAdmin && canMarch
            val open = (editing && !r.excluded && expandedId == r.id) || leaderOwn
            val toggleOpen = { expandedId = if (expandedId == r.id) null else r.id; deleteGuard.reset(); lastRender?.invoke(); Unit }
            when {
                open && editing -> v.setBackgroundResource(R.drawable.bg_row_open)
                r.isMine -> v.setBackgroundColor(Color.parseColor("#1F3B82F6"))
                else -> v.setBackgroundColor(Color.TRANSPARENT)
            }
            v.findViewById<View>(R.id.rowExpand).visibility = if (open) View.VISIBLE else View.GONE
            v.findViewById<TextView>(R.id.rowDot).apply {
                // 편집 모드에서는 ● 대신 체크박스를 보여 준다: ☑ 참여 / ☐ 제외. 눌러서 바꾸는 것이라는 게 보이게.
                text = if (!editing) "●" else if (r.excluded) "☐" else "☑"
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, if (editing) 22f else 10f)
                setTextColor(when {
                    editing -> Color.parseColor(if (r.excluded) "#8190A8" else "#3B82F6")
                    r.online -> Color.parseColor("#22C55E")
                    else -> Color.parseColor("#64748B")
                })
                if (editing) setOnClickListener { callbacks.onToggleExclude(r.id) }
                else { setOnClickListener(null); isClickable = false }
            }
            v.findViewById<TextView>(R.id.rowName).apply {
                // 관리자는 어느 군단이 아직 비었는지 바로 보고, 이름을 눌러 사람을 배정한다.
                // 칸이 좁아 "1군 윈터…"처럼 잘리지 않게, 군단은 윗줄 / 캐릭터명은 아랫줄(조금 작게)로 나눈다.
                val head = if (r.isMine) "${r.name} ★나" else r.name
                text = when {
                    r.leaderName.isNotBlank() -> android.text.SpannableStringBuilder("$head\n${r.leaderName}").apply {
                        setSpan(android.text.style.RelativeSizeSpan(0.85f), head.length + 1, length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    isAdmin -> "$head 미배정"
                    else -> head
                }
                setOnClickListener { if (editing) callbacks.onAssignLeader(r.id, r.name) }
                // 눌러서 배정할 수 있다는 표시: 편집 모드에서만 밑줄
                paintFlags = if (editing) paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                else paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
                setTypeface(null, if (r.isMine) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                setTextColor(if (r.isMine) Color.parseColor("#60A5FA") else Color.parseColor("#F1F5F9"))
                alpha = if (r.excluded) 0.45f else 1f
            }
            v.findViewById<TextView>(R.id.rowMarch).apply {
                // 행군시간 아래에, 가장 먼저 누르는 군단보다 몇 초 늦게 누르는지(관리자 보정 포함) 작게 보여 준다.
                // 가장 먼저 누르는 군단 줄에는 "먼저". 제외된 군단은 표시하지 않는다.
                val main = RallyPanelFormat.sec(r.marchSec) + "초"
                val sub = lags[i]
                gravity = android.view.Gravity.END
                text = if (sub.isEmpty()) android.text.SpannableStringBuilder(main)
                else android.text.SpannableStringBuilder("$main\n$sub").apply {
                    setSpan(android.text.style.RelativeSizeSpan(0.8f), main.length + 1, length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(android.text.style.ForegroundColorSpan(Color.parseColor("#A9B4C7")), main.length + 1, length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(android.text.style.StyleSpan(android.graphics.Typeface.NORMAL), main.length + 1, length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                alpha = if (r.excluded) 0.45f else 1f
                setOnClickListener { if (editing && !r.excluded) toggleOpen() }
            }
            v.findViewById<TextView>(R.id.rowStatus).apply {
                // 평소: 상태 글자. 관리자가 더해 준 보정이 있으면 아래 줄에 작게 보여 준다(줄이 늘어나도 폭은 그대로).
                // 편집 모드: 참여 군단은 펼침 표시(▾/▴), 제외된 군단은 "제외"(눌러서 다시 포함).
                text = when {
                    editing && !r.excluded -> if (open) "▴" else "▾"
                    r.adminAdjustMs == 0 || editing -> r.statusLabel
                    else -> listOf(r.statusLabel, RallyInputParse.formatCorrection(r.adminAdjustMs)).filter { it.isNotEmpty() }.joinToString("\n")
                }
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, if (editing && !r.excluded) 16f else 12f)
                setOnClickListener {
                    if (editing) { if (r.excluded) callbacks.onToggleExclude(r.id) else toggleOpen() }
                }
                paintFlags = paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
            }
            v.findViewById<View>(R.id.rowHead).setOnClickListener { if (editing && !r.excluded) toggleOpen() }
            v.findViewById<View>(R.id.rowHead).isClickable = editing && !r.excluded
            v.findViewById<RallyTimelineBar>(R.id.rowBar).visibility = if (showBars) View.VISIBLE else View.GONE
            v.findViewById<RallyTimelineBar>(R.id.rowBar).set(scale, r.clickAtSec, r.departAtSec, r.arriveAtSec, nowSec, r.excluded)

            // 펼친 부분: 행군 시간(−/+, 숫자를 누르면 직접 입력) · 보정(−/+, 숫자를 누르면 직접 입력) · 삭제
            v.findViewById<TextView>(R.id.rowMarchVal).apply {
                text = RallyPanelFormat.sec(r.marchSec) + "초"
                setOnClickListener { if (canMarch) callbacks.onEditMarch(r.id, r.marchSec) }
            }
            v.findViewById<View>(R.id.rowMinus).setOnClickListener { callbacks.onMarchDelta(r.id, -1.0) }
            v.findViewById<View>(R.id.rowPlus).setOnClickListener { callbacks.onMarchDelta(r.id, 1.0) }
            v.findViewById<View>(R.id.rowAdjCol).visibility = if (editing) View.VISIBLE else View.GONE
            v.findViewById<TextView>(R.id.rowAdjVal).apply {
                text = RallyInputParse.formatCorrection(r.adminAdjustMs)
                setOnClickListener { callbacks.onEditAdminAdjust(r.id, r.name, r.adminAdjustMs) }
            }
            v.findViewById<View>(R.id.rowAdjMinus).setOnClickListener { callbacks.onSetAdminAdjust(r.id, r.adminAdjustMs - ADJUST_STEP_MS) }
            v.findViewById<View>(R.id.rowAdjPlus).setOnClickListener { callbacks.onSetAdminAdjust(r.id, r.adminAdjustMs + ADJUST_STEP_MS) }
            v.findViewById<TextView>(R.id.rowDel).apply {
                visibility = if (editing) View.VISIBLE else View.GONE
                // 첫 탭은 "삭제?"로 바뀌기만 하고, 3초 안에 한 번 더 눌러야 지운다
                text = if (deleteGuard.isArmed(r.id, android.os.SystemClock.elapsedRealtime())) "삭제할까요? 한 번 더 누르세요" else "✕ 이 군단 삭제"
                setOnClickListener {
                    if (deleteGuard.onTap(r.id, android.os.SystemClock.elapsedRealtime())) callbacks.onRemoveTeam(r.id)
                    else lastRender?.invoke()
                }
            }
            v.findViewById<TextView>(R.id.rowRemain).apply {
                text = r.remainingSec?.let { RallyScreenModel.formatMmSs(it) } ?: ""
                setTextColor(Color.parseColor("#FBBF24"))
                visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    /**
     * 목록이 쓸 수 있는 높이를 정한다: 화면 높이의 일정 비율에서, 패널의 나머지(머리·설정·버튼)가 쓰는 높이를 뺀 만큼.
     * 군단이 몇 개든, 줄을 펼쳐도 패널이 화면을 넘지 않고, 넘치는 줄은 목록 안에서 스크롤된다.
     */
    private fun fitRowsHeight() {
        if (root.height <= 0 || rowsScroll.visibility != View.VISIBLE) return
        val dm = root.resources.displayMetrics
        val others = root.height - rowsScroll.height
        val limit = (dm.heightPixels * PANEL_MAX_SCREEN_RATIO).toInt() - others
        rowsScroll.maxHeightPx = Math.max(limit, (96 * dm.density).toInt())
    }

    /** 대기 · 집결 · 행군 · 도착 중 지금 단계만 밝게 보여준다. */
    private fun renderPhases(kind: HeroKind, phase: Int? = null) {
        val current = when (kind) {
            HeroKind.OVERVIEW -> phase ?: 0
            HeroKind.GATHERING -> 1
            HeroKind.MARCHING -> 2
            HeroKind.ARRIVED -> 3
            HeroKind.CANCELLED, HeroKind.EXCLUDED -> -1
            else -> 0
        }
        // 단계마다 고유 색: 대기 노랑 · 집결 파랑 · 행군 보라 · 도착 초록. 지금 단계만 진하게, 나머지는 같은 색을 흐리게.
        listOf(R.id.phase1, R.id.phase2, R.id.phase3, R.id.phase4).forEachIndexed { i, id ->
            val t = root.findViewById<TextView>(id)
            val base = Color.parseColor(PHASE_COLORS[i])
            t.setTextColor(if (i == current) base else (base and 0x00FFFFFF) or (0x66 shl 24))
            t.setTypeface(null, if (i == current) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
    }

    private fun heroColor(h: HeroModel): Int =
        if (h.kind == HeroKind.OVERVIEW) Color.parseColor(PHASE_COLORS[(h.phase ?: 0).coerceIn(0, 3)]) else heroColor(h.kind)

    private fun heroColor(kind: HeroKind): Int = Color.parseColor(
        when (kind) {
            HeroKind.MOVE, HeroKind.WAIT_CLICK -> "#FBBF24"
            HeroKind.GATHERING -> "#60A5FA"
            HeroKind.MARCHING -> "#A78BFA"
            HeroKind.ARRIVED -> "#22C55E"
            HeroKind.CANCELLED -> "#F87171"
            HeroKind.EXCLUDED -> "#64748B"
            else -> "#FFFFFF"
        }
    )

    /** 알약에서는 한 줄에 들어가도록 단계명을 짧게 줄인다. */
    private fun minLabel(h: HeroModel): String = when (h.kind) {
        HeroKind.MOVE -> "이동 준비"
        HeroKind.WAIT_CLICK -> "집결 대기"
        HeroKind.GATHERING -> "집결 중"
        HeroKind.MARCHING -> "행군 중"
        HeroKind.ARRIVED -> "전원 도착"
        HeroKind.CANCELLED -> "작전 취소"
        HeroKind.EXCLUDED -> "참여 안 함"
        HeroKind.OVERVIEW -> h.label
        HeroKind.IDLE -> if (h.label.contains("배정")) "군단 배정 대기" else if (h.label.contains("참여하지")) "참여 안 함" else if (h.label.contains("구성")) "팀 구성 중" else "시작 대기"
    }

    /** 최소화: 카운트다운 한 줄만 남기고 나머지는 숨긴다. */
    private var isMinimized = false

    fun setMinimized(min: Boolean, hero: HeroModel) {
        isMinimized = min
        root.findViewById<TextView>(R.id.rallyMinimize).text = if (min) "▢" else "—"
        val hide = if (min) View.GONE else View.VISIBLE
        heroSub.visibility = hide
        rowsScroll.visibility = hide
        rowsHead.visibility = if (min || rows.childCount == 0) View.GONE else View.VISIBLE
        root.findViewById<View>(R.id.phaseRow).visibility = hide
        root.findViewById<View>(R.id.devToggle).visibility = hide
        if (min) {
            deviceOpen = false
            showDeviceToggle()
            root.findViewById<View>(R.id.devSection).visibility = View.GONE
            root.findViewById<View>(R.id.rallyAddTeam).visibility = View.GONE
            root.findViewById<View>(R.id.rallySettingsRow).visibility = View.GONE
            root.findViewById<View>(R.id.setPrep).visibility = View.GONE
            root.findViewById<View>(R.id.rallyEdit).visibility = View.GONE
            adminBar.visibility = View.GONE
            warning.visibility = View.GONE
            blocked.visibility = View.GONE
        }
        val dp = root.resources.displayMetrics.density
        val bar = heroProgress.layoutParams as LinearLayout.LayoutParams
        if (min) {
            // 알약: [● 단계명 ........ 큰 시간 ▢ ✕] 한 줄, 아래에 얇은 진행 막대. 테두리와 막대는 단계 색.
            val c = heroColor(hero)
            title.text = "● " + minLabel(hero)
            title.setTextColor(c)
            title.textSize = 15f
            title.maxLines = 1
            title.ellipsize = android.text.TextUtils.TruncateAt.END
            (title.layoutParams as? LinearLayout.LayoutParams)?.let { it.width = LinearLayout.LayoutParams.WRAP_CONTENT; it.weight = 0f; it.marginEnd = (12 * dp).toInt(); title.layoutParams = it }
            root.minimumWidth = (236 * dp).toInt()
            heroLabel.visibility = View.GONE
            heroTime.visibility = View.GONE
            miniTime.visibility = if (miniTime.text.isEmpty()) View.GONE else View.VISIBLE
            heroProgress.progressTintList = android.content.res.ColorStateList.valueOf(c)
            bar.height = (4 * dp).toInt(); bar.topMargin = 0
            root.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#F2121A2C"))
                cornerRadius = 32 * dp
                setStroke((2 * dp).toInt(), c)
            }
            root.setPadding((18 * dp).toInt(), (6 * dp).toInt(), (8 * dp).toInt(), (10 * dp).toInt())
        } else {
            root.setBackgroundResource(R.drawable.bg_rally_panel)
            root.setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (14 * dp).toInt())
            title.maxLines = Int.MAX_VALUE
            (title.layoutParams as? LinearLayout.LayoutParams)?.let { it.width = 0; it.weight = 1f; it.marginEnd = 0; title.layoutParams = it }
            root.minimumWidth = 0
            title.setTextColor(Color.parseColor("#F8FAFC"))
            title.textSize = 18f
            heroLabel.visibility = View.VISIBLE
            heroLabel.text = hero.label
            miniTime.visibility = View.GONE
            heroTime.visibility = if (heroTime.text.isEmpty()) View.GONE else View.VISIBLE
            heroProgress.progressTintList = null
            bar.height = (6 * dp).toInt(); bar.topMargin = (6 * dp).toInt()
        }
        heroProgress.layoutParams = bar
    }
}
