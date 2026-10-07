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
        const val ADJUST_STEP_MS = 500
        /** 군단 목록이 한 번에 보여 주는 줄 수. 그보다 많으면 목록 안에서 스크롤한다. */
        const val VISIBLE_ROWS = 6
        val PHASE_COLORS = listOf("#FBBF24", "#3B82F6", "#A78BFA", "#4ADE80")
        val PHASE_NAMES = listOf("대기", "집결", "행군", "도착")
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
    private val connDot = root.findViewById<View>(R.id.rallyConnDot)
    private val rowsHeadRight = root.findViewById<TextView>(R.id.rallyRowsHeadRight)
    private val minimizeBtn = root.findViewById<android.widget.ImageView>(R.id.rallyMinimize)
    private val titleCol = root.findViewById<View>(R.id.rallyTitleCol)
    private val miniSub = root.findViewById<TextView>(R.id.rallyMiniSub)
    private var myTeamShown = ""
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
        // 목록은 군단 6줄 높이까지만 보인다(얇은 제외 줄도 한 줄로 센다). 펼친 줄이 그보다 크면 그 줄은 다 보이게 한다.
        (root as RallyDragLayout).visibleListHeight = {
            var sum = 0; var tallest = 0
            for (i in 0 until rows.childCount) {
                val h = rows.getChildAt(i).measuredHeight
                if (i < VISIBLE_ROWS) sum += h
                if (h > tallest) tallest = h
            }
            if (rows.childCount > VISIBLE_ROWS) Math.max(sum, tallest) else 0
        }
        root.findViewById<View>(R.id.rallyMinimize).setOnClickListener { callbacks.onMinimize() }
        root.findViewById<View>(R.id.rallyClose).setOnClickListener { callbacks.onClose() }
        // ✎: 관리자 편집 모드. 평소엔 읽기 전용으로 깔끔하게, 켜면 −/+ · ✕ · 밑줄(눌러서 고치기)이 나타난다.
        root.findViewById<View>(R.id.rallyEdit).setOnClickListener { editMode = !editMode; expandedId = null; deleteGuard.reset(); lastRender?.invoke() }
        // 카운트다운 상자(단계명·시간·안내)를 탭하면 축소/확대된다. 작은 —/▢ 버튼 옆의 ✕를 잘못 누르지 않게 큰 영역으로도 누를 수 있다.
        // 알약(최소화 상태)에서는 단계명이나 시간을 탭하면 펼쳐진다. 창을 끄는 일은 ✕만 한다.
        listOf<View>(heroLabel, heroTime, heroSub, miniTime, miniSub).forEach { v -> v.setOnClickListener { callbacks.onMinimize() } }
        title.setOnClickListener { if (isMinimized) callbacks.onMinimize() else callbacks.onTitleTap() }
        btnStart.setOnClickListener { callbacks.onStart() }
        btnStop.setOnClickListener { callbacks.onStop() }
        listOf<View>(btnStart, btnStop).forEach { pressFeel(it) }
        root.findViewById<View>(R.id.devToggleRow).setOnClickListener { setDeviceOpen(!deviceOpen) }
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
        // [내 기기 ▾]  ✓ 위치 저장됨 ........ 방 1111 · 문 (보정은 0초가 아닐 때만)
        root.findViewById<TextView>(R.id.devToggle).apply {
            text = if (deviceOpen) "내 기기 ▴" else "내 기기 ▾"
            setBackgroundResource(if (deviceOpen) R.drawable.bg_edit_done else R.drawable.bg_dev_chip)
            setTypeface(null, if (deviceOpen) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
        root.findViewById<TextView>(R.id.devPosBadge).apply {
            text = if (positionSavedShown) "✓ 위치 저장됨" else "⚠ 위치 없음"
            setTextColor(Color.parseColor(if (positionSavedShown) "#4ADE80" else "#FBBF24"))
        }
        val corr = RallyInputParse.formatCorrection(correctionShownMs)
        // 펼쳤을 때는 아래 칸에 같은 내용이 있으니 요약을 숨긴다. "위치 저장됨"은 항상 보인다.
        root.findViewById<View>(R.id.devSummary).visibility = if (deviceOpen) View.INVISIBLE else View.VISIBLE
        root.findViewById<TextView>(R.id.devSummary).text = listOfNotNull(
            if (roomShown.isBlank()) "방 없음" else "방 $roomShown",
            charNameShown.ifBlank { "캐릭터명 없음" },
            if (correctionShownMs == 0) null else "보정 $corr"
        ).joinToString(" · ")
    }

    /** "내 기기"의 현재 방 줄. */
    fun renderRoom(code: String) {
        roomShown = code
        showDeviceToggle()
        root.findViewById<TextView>(R.id.devRoom).text = boxLabel("방", if (code.isEmpty()) "없음" else code)
    }

    /** 칸 안의 글: 작은 회색 이름표 + 굵은 값 ("방 1111", "캐릭터명 문"). */
    private fun boxLabel(label: String, value: String): CharSequence =
        android.text.SpannableStringBuilder("$label  $value").apply {
            setSpan(android.text.style.RelativeSizeSpan(0.8f), 0, label.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(android.text.style.ForegroundColorSpan(Color.parseColor("#8190A8")), 0, label.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), label.length, length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

    private var detailOpen = false
    private var lastPosText = ""
    private var lastDetailText = ""

    /** ⋯(자세히)를 누르면 기기 ID · 저장 위치 · 진단이 아래에 펼쳐진다. */
    private fun showDeviceStatus() {
        root.findViewById<TextView>(R.id.devPosStatus).apply {
            text = listOf(lastPosText, lastDetailText).filter { it.isNotEmpty() }.joinToString("\n")
            visibility = if (detailOpen && text.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    fun renderDevice(correctionMs: Int, posText: String, characterName: String, detailText: String = "", positionSaved: Boolean = true) {
        charNameShown = characterName
        positionSavedShown = positionSaved
        root.findViewById<TextView>(R.id.devCharName).text = boxLabel("캐릭터명", characterName.ifBlank { "등록하기" })
        correctionShownMs = correctionMs
        // "0초" 아래에 작은 글씨로 단위를 적는다(−/+ 한 번에 0.5초)
        root.findViewById<TextView>(R.id.devMs).text = RallyInputParse.formatCorrection(correctionMs).let { main ->
            android.text.SpannableStringBuilder("$main\n보정 · 0.5초씩").apply {
                setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, main.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(android.text.style.RelativeSizeSpan(0.66f), main.length + 1, length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(android.text.style.ForegroundColorSpan(Color.parseColor("#8190A8")), main.length + 1, length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        lastPosText = posText
        lastDetailText = detailText
        root.findViewById<View>(R.id.devMore).setOnClickListener { detailOpen = !detailOpen; showDeviceStatus() }
        showDeviceStatus()
        showDeviceToggle()
    }

    fun render(model: ScreenModel, isAdmin: Boolean, hasStarted: Boolean = false, arrivalNote: String = "",
               conn: RallyConnection = RallyConnection.LIVE, urgent: Boolean = false, starting: Boolean = false) {
        val hero = model.hero
        val name = if (isAdmin) "관리자" else "집결장"
        // 연결 상태 점: 초록=실시간, 주황=1초 확인, 빨강=끊김. 알약(최소화)에서는 단계 표시가 대신 쓴다.
        title.text = name
        connColor = Color.parseColor(when (conn) { RallyConnection.LIVE -> "#22C55E"; RallyConnection.POLLING -> "#F59E0B"; RallyConnection.OFFLINE -> "#EF4444" })
        if (!isMinimized) connDot.background.mutate().setTint(connColor)
        lastRender = { render(model, isAdmin, hasStarted, arrivalNote, conn, urgent, starting) }
        val editing = isAdmin && model.editable && editMode
        root.findViewById<TextView>(R.id.rallyEdit).apply {
            visibility = if (isAdmin && model.editable) View.VISIBLE else View.GONE
            // 편집 중에는 "완료" 버튼, 평소에는 ✎
            text = if (editMode) "완료" else ""
            setCompoundDrawablesWithIntrinsicBounds(if (editMode) 0 else R.drawable.ic_rp_edit, 0, 0, 0)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            val padH = ((if (editMode) 14 else 11) * resources.displayMetrics.density).toInt()
            setPadding(padH, 0, padH, 0)
            if (editMode) setBackgroundResource(R.drawable.bg_edit_done) else background = null
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
        // 알약 오른쪽: 남은 시간. 시간이 없으면 "완료"(전원 도착) 또는 "탭해서 펼치기".
        val idleKind = hero.kind == HeroKind.CANCELLED || hero.kind == HeroKind.IDLE || hero.kind == HeroKind.EXCLUDED
        val hasTime = hero.remainingSec != null
        miniTime.text = when { hasTime -> heroTime.text; hero.kind == HeroKind.ARRIVED -> "완료"; else -> "탭해서 펼치기" }
        miniTime.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, when { hasTime -> 30f; hero.kind == HeroKind.ARRIVED -> 22f; else -> 14f })
        miniTime.setTypeface(null, if (hasTime || hero.kind == HeroKind.ARRIVED) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        miniTime.setTextColor(if (hasTime) timeColor else if (idleKind) Color.parseColor("#A9B4C7") else heroColor(hero))
        myTeamShown = model.rows.firstOrNull { it.isMine }?.name ?: ""
        val sub = if (previewTotal) RallyScreenModel.idleSub(hero.subLabel, model.rows.count { !it.excluded }) else hero.subLabel
        // 집결이 시작된 뒤에는 한 줄만: "도착 예정 15:53:24 · ✓ 클릭함 15:47:56.080" (단계 설명은 큰 숫자·단계 표시가 대신한다)
        heroSub.text = if (arrivalNote.isEmpty()) sub else arrivalNote
        heroSub.visibility = if (editing || isMinimized) View.GONE else View.VISIBLE
        renderPhases(hero.kind, hero.phase)
        // 취소·대기처럼 진행 중이 아닐 때는 막대를 비워 둔다(꽉 찬 빨간 줄이 경고처럼 보이지 않게).
        val idleBar = hero.kind == HeroKind.CANCELLED || hero.kind == HeroKind.IDLE || hero.kind == HeroKind.EXCLUDED
        heroProgress.progress = if (idleBar) 0 else (hero.progress * 1000).toInt()
        heroProgress.progressTintList = android.content.res.ColorStateList.valueOf(heroColor(hero))

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
        root.findViewById<View>(R.id.rallyPrepRow).visibility = showSetup
        root.findViewById<TextView>(R.id.setPrep).text = "${model.prepSec.toInt()}초"
        // 고른 집결 대기 시간은 파란 칩으로 채운다
        listOf(R.id.setWait3 to 180.0, R.id.setWait5 to 300.0, R.id.setWait10 to 600.0).forEach { (id, sec) ->
            val on = model.waitSec == sec
            root.findViewById<TextView>(id).apply {
                setBackgroundResource(if (on) R.drawable.bg_chip_on else R.drawable.bg_chip_soft)
                setTextColor(Color.parseColor(if (on) "#FFFFFF" else "#F1F5F9"))
                setTypeface(null, if (on) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            }
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
        // 머리글: 숫자의 뜻을 한 번만 적는다. 편집 모드에서는 줄 자체가 설명이 되므로 숨겨 높이를 아낀다.
        rowsHeadRight.text = if (editable) "행군 시간 · 위에서부터 차례로 클릭" else "행군 시간 · 클릭까지 남은 시간"
        rowsHead.visibility = if (list.isEmpty() || isMinimized || editing) View.GONE else View.VISIBLE
        // 내 줄에는 이 폰의 "내 보정"(내 기기)도 더한다. 다른 집결장 폰의 보정은 방 데이터에 없어 알 수 없다.
        val lags = RallyPanelFormat.lagLabels(list.map { LagInput(it.marchSec, it.adminAdjustMs + (if (it.isMine) correctionShownMs else 0), it.excluded) })
        val gray = Color.parseColor("#A9B4C7")
        // "50초" 아래에 작은 회색 글씨("먼저", "+20초")
        fun twoLine(main: String, sub: String): CharSequence =
            if (sub.isEmpty()) main else android.text.SpannableStringBuilder("$main\n$sub").apply {
                setSpan(android.text.style.RelativeSizeSpan(0.75f), main.length + 1, length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(android.text.style.ForegroundColorSpan(gray), main.length + 1, length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(android.text.style.StyleSpan(android.graphics.Typeface.NORMAL), main.length + 1, length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        list.forEachIndexed { i, r ->
            val v = rows.getChildAt(i)
            // 행군시간 고치기: 관리자는 편집 모드에서 모든 군단, 집결장은 평소에도 내 군단만
            val canMarch = editable && !r.excluded && (if (isAdmin) editing else r.isMine)
            // 펼침: 관리자는 편집 모드에서 누른 줄 하나, 집결장은 내 군단 줄(행군시간만)
            val leaderOwn = !isAdmin && canMarch
            val editOpen = editing && !r.excluded && expandedId == r.id
            val open = editOpen || leaderOwn
            val toggleOpen = { expandedId = if (expandedId == r.id) null else r.id; deleteGuard.reset(); lastRender?.invoke(); Unit }
            when {
                editOpen -> v.setBackgroundResource(R.drawable.bg_row_open)
                r.isMine && !editing -> v.setBackgroundResource(R.drawable.bg_row_mine)
                else -> v.background = null
            }
            // 제외된 군단은 줄 전체를 흐리게, 그리고 얇게(32dp 한 줄, 막대 없음) 보여 자리를 아낀다. 순서는 그대로.
            v.alpha = if (r.excluded) 0.6f else 1f
            val thin = r.excluded
            val d = v.resources.displayMetrics.density
            v.setPadding(0, 0, (8 * d).toInt(), if (thin) 0 else (6 * d).toInt())
            v.findViewById<View>(R.id.rowHead).minimumHeight = ((if (thin) 32 else 48) * d).toInt()
            v.findViewById<View>(R.id.rowExpand).visibility = if (open) View.VISIBLE else View.GONE
            v.findViewById<android.widget.ImageView>(R.id.rowDot).apply {
                layoutParams = layoutParams.apply { height = ((if (thin) 32 else 44) * d).toInt() }
                // 얇은 줄의 체크박스는 조금 작게(20dp)
                val sc = if (thin && editing) 0.84f else 1f
                scaleX = sc; scaleY = sc
                // 편집 모드에서는 접속 점 대신 체크박스: 파란 ✓ = 참여, 빈 칸 = 제외. 눌러서 바꾼다.
                if (editing) {
                    setImageResource(if (r.excluded) R.drawable.ic_rp_check_off else R.drawable.ic_rp_check_on)
                    clearColorFilter()
                    setOnClickListener { callbacks.onToggleExclude(r.id) }
                } else {
                    setImageResource(R.drawable.ic_rp_dot)
                    setColorFilter(Color.parseColor(if (r.online) "#22C55E" else "#64748B"))
                    setOnClickListener(null); isClickable = false
                }
            }
            v.findViewById<View>(R.id.rowNameWrap).visibility = if (editOpen) View.GONE else View.VISIBLE
            v.findViewById<TextView>(R.id.rowName).apply {
                // "1군 달구지": 군단은 굵게, 맡은 사람은 조금 작고 옅게. 관리자에게는 비어 있는 군단을 "미배정"으로 보여 준다.
                val who = if (r.leaderName.isNotBlank()) r.leaderName else if (isAdmin) "미배정" else ""
                text = if (who.isEmpty()) android.text.SpannableStringBuilder(r.name).apply {
                    setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else android.text.SpannableStringBuilder("${r.name}  $who").apply {
                    setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, r.name.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(android.text.style.RelativeSizeSpan(0.82f), r.name.length, length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(android.text.style.ForegroundColorSpan(Color.parseColor(if (r.leaderName.isNotBlank()) "#CBD5E1" else "#8190A8")), r.name.length, length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, if (thin) 14f else 17f)
                minHeight = if (thin) 0 else (40 * d).toInt()
                // 편집 모드에서 이름을 누르면 맡을 사람을 고른다
                setOnClickListener { if (editing) callbacks.onAssignLeader(r.id, r.name) }
                isClickable = editing
            }
            v.findViewById<View>(R.id.rowMe).visibility = if (r.isMine) View.VISIBLE else View.GONE
            // 펼친 줄의 머리: [3군] [코카콜라 — 눌러서 배정] [✕ 삭제]
            v.findViewById<TextView>(R.id.rowTeamBox).apply {
                visibility = if (editOpen) View.VISIBLE else View.GONE
                text = r.name
            }
            v.findViewById<TextView>(R.id.rowLeaderBox).apply {
                visibility = if (editOpen) View.VISIBLE else View.GONE
                text = if (r.leaderName.isNotBlank()) r.leaderName else "미배정 · 눌러서 배정"
                setTextColor(Color.parseColor(if (r.leaderName.isNotBlank()) "#F1F5F9" else "#8190A8"))
                setOnClickListener { callbacks.onAssignLeader(r.id, r.name) }
            }
            v.findViewById<TextView>(R.id.rowMarch).apply {
                // 행군시간 아래에, 가장 먼저 누르는 군단보다 몇 초 늦게 누르는지(관리자 보정 포함) 작게 보여 준다.
                visibility = if (editOpen || (editing && r.excluded)) View.GONE else View.VISIBLE
                text = twoLine(RallyPanelFormat.sec(r.marchSec) + "초", lags[i])
                setOnClickListener { if (editing && !r.excluded) toggleOpen() }
                isClickable = editing && !r.excluded
            }
            v.findViewById<TextView>(R.id.rowStatus).apply {
                // 상태 글자. 제외된 군단은 테두리 알약 "제외"(편집 중에 누르면 다시 참여).
                // 관리자가 더해 준 보정이 있으면 아래 줄에 작게 보여 준다.
                val dp = resources.displayMetrics.density
                text = when {
                    editing && !r.excluded -> ""
                    r.excluded || r.adminAdjustMs == 0 -> r.statusLabel
                    else -> listOf(r.statusLabel, "보정 " + RallyInputParse.formatCorrection(r.adminAdjustMs)).filter { it.isNotEmpty() && it != "보정 " }.joinToString("\n")
                }
                visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, if (thin) 11f else 12f)
                minHeight = ((if (thin) 20 else 24) * dp).toInt()
                if (r.excluded) { setBackgroundResource(R.drawable.bg_pill_outline); setPadding((10 * dp).toInt(), 0, (10 * dp).toInt(), 0) }
                else { background = null; setPadding(0, 0, 0, 0) }
                setOnClickListener { if (editing && r.excluded) callbacks.onToggleExclude(r.id) }
                isClickable = editing && r.excluded
            }
            v.findViewById<android.widget.ImageView>(R.id.rowChevron).apply {
                // 접힌 줄은 › (펼치기), 펼친 줄은 ▴ (접기)
                visibility = if (editing && !r.excluded) View.VISIBLE else View.GONE
                setImageResource(if (editOpen) R.drawable.ic_rp_chevron_up else R.drawable.ic_rp_chevron)
                contentDescription = if (editOpen) "접기" else "펼치기"
                setOnClickListener { toggleOpen() }
            }
            v.findViewById<View>(R.id.rowHead).apply {
                setOnClickListener { if (editing && !r.excluded) toggleOpen() }
                isClickable = editing && !r.excluded
            }
            v.findViewById<RallyTimelineBar>(R.id.rowBar).visibility = if (showBars && !thin) View.VISIBLE else View.GONE
            v.findViewById<RallyTimelineBar>(R.id.rowBar).set(scale, r.clickAtSec, r.departAtSec, r.arriveAtSec, nowSec, r.excluded)

            // 펼친 부분: 행군 시간(−/+, 숫자를 누르면 직접 입력) · 보정(−/+, 숫자를 누르면 직접 입력)
            v.findViewById<TextView>(R.id.rowMarchVal).apply {
                text = twoLine(RallyPanelFormat.sec(r.marchSec) + "초", lags[i])
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
                visibility = if (editOpen) View.VISIBLE else View.GONE
                // 첫 탭은 빨간 ✕가 "삭제?"로 바뀌기만 하고, 3초 안에 한 번 더 눌러야 지운다
                val armed = deleteGuard.isArmed(r.id, android.os.SystemClock.elapsedRealtime())
                text = if (armed) "삭제?" else ""
                setCompoundDrawablesWithIntrinsicBounds(if (armed) 0 else R.drawable.ic_rp_del, 0, 0, 0)
                val pad = ((if (armed) 6 else 12) * resources.displayMetrics.density).toInt()
                setPadding(pad, 0, pad, 0)
                setOnClickListener {
                    if (deleteGuard.onTap(r.id, android.os.SystemClock.elapsedRealtime())) callbacks.onRemoveTeam(r.id)
                    else lastRender?.invoke()
                }
            }
            v.findViewById<TextView>(R.id.rowRemain).apply {
                text = r.remainingSec?.let { RallyScreenModel.formatMmSs(it) } ?: ""
                visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    /** 대기 · 집결 · 행군 · 도착 중 지금 단계만 밝게 보여준다. */
    private fun renderPhases(kind: HeroKind, phase: Int? = null) {
        val current = when (kind) {
            HeroKind.OVERVIEW -> phase ?: 0
            HeroKind.GATHERING -> 1
            HeroKind.MARCHING -> 2
            HeroKind.ARRIVED -> 3
            HeroKind.CANCELLED, HeroKind.EXCLUDED, HeroKind.IDLE -> -1
            else -> 0
        }
        // 단계마다 색 점: 대기 노랑 · 집결 파랑 · 행군 보라 · 도착 초록. 진행 중에는 지금 단계의 글자만 그 색으로 굵게.
        listOf(R.id.phase1, R.id.phase2, R.id.phase3, R.id.phase4).forEachIndexed { i, id ->
            val t = root.findViewById<TextView>(id)
            val base = Color.parseColor(PHASE_COLORS[i])
            val label = when { i == current -> base; current < 0 -> Color.parseColor("#A9B4C7"); else -> Color.parseColor("#8190A8") }
            t.text = android.text.SpannableString("● " + PHASE_NAMES[i]).apply {
                setSpan(android.text.style.ForegroundColorSpan(base), 0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(android.text.style.RelativeSizeSpan(0.7f), 0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            t.setTextColor(label)
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
    private var connColor = Color.parseColor("#22C55E")

    fun setMinimized(min: Boolean, hero: HeroModel) {
        isMinimized = min
        // 접힌 상태: 펼치기 아이콘만 두고 ✕는 숨긴다(창을 끄려면 펼친 뒤에).
        minimizeBtn.setImageResource(if (min) R.drawable.ic_rp_expand else R.drawable.ic_rp_min)
        minimizeBtn.contentDescription = if (min) "펼치기" else "작게"
        root.findViewById<View>(R.id.rallyClose).visibility = if (min) View.GONE else View.VISIBLE
        val hide = if (min) View.GONE else View.VISIBLE
        heroSub.visibility = hide
        rowsScroll.visibility = hide
        rowsHead.visibility = if (min || rows.childCount == 0) View.GONE else View.VISIBLE
        root.findViewById<View>(R.id.phaseRow).visibility = hide
        root.findViewById<View>(R.id.devToggleRow).visibility = hide
        if (min) {
            deviceOpen = false
            showDeviceToggle()
            root.findViewById<View>(R.id.devSection).visibility = View.GONE
            root.findViewById<View>(R.id.rallyAddTeam).visibility = View.GONE
            root.findViewById<View>(R.id.rallySettingsRow).visibility = View.GONE
            root.findViewById<View>(R.id.rallyPrepRow).visibility = View.GONE
            root.findViewById<View>(R.id.rallyEdit).visibility = View.GONE
            adminBar.visibility = View.GONE
            warning.visibility = View.GONE
            blocked.visibility = View.GONE
        }
        val dp = root.resources.displayMetrics.density
        val bar = heroProgress.layoutParams as LinearLayout.LayoutParams
        if (min) {
            // 알약: [● 단계명 ........ 큰 시간 ▢ ✕] 한 줄, 아래에 얇은 진행 막대. 테두리와 막대는 단계 색.
            // 진행 중이 아닐 때(대기·취소·참여 안 함)는 회색 테두리·점으로 조용하게.
            val quiet = hero.kind == HeroKind.CANCELLED || hero.kind == HeroKind.IDLE || hero.kind == HeroKind.EXCLUDED
            val c = if (quiet) Color.parseColor("#8190A8") else heroColor(hero)
            title.text = minLabel(hero)
            title.setTextColor(Color.parseColor("#F1F5F9"))
            connDot.background.mutate().setTint(c)
            title.textSize = 15f
            title.maxLines = 1
            title.ellipsize = android.text.TextUtils.TruncateAt.END
            miniSub.text = listOf(myTeamShown, if (roomShown.isBlank()) "" else "방 $roomShown").filter { it.isNotEmpty() }.joinToString(" · ")
            miniSub.visibility = if (miniSub.text.isEmpty()) View.GONE else View.VISIBLE
            // 이름 칸이 남는 폭을 가져가서, 시간과 펼치기 아이콘은 어느 단계에서든 오른쪽 끝 같은 자리에 온다.
            (titleCol.layoutParams as? LinearLayout.LayoutParams)?.let { it.width = 0; it.weight = 1f; it.marginEnd = (16 * dp).toInt(); titleCol.layoutParams = it }
            root.minimumWidth = (236 * dp).toInt()
            heroLabel.visibility = View.GONE
            heroTime.visibility = View.GONE
            miniTime.visibility = View.VISIBLE
            // 얇은 진행 막대: 알약 둥근 끝에 닿지 않게 안쪽으로 들인다. 진행 중이 아니면(취소·대기) 막대를 아예 숨긴다.
            heroProgress.visibility = if (quiet) View.GONE else View.VISIBLE
            bar.height = (4 * dp).toInt(); bar.topMargin = 0
            bar.leftMargin = (12 * dp).toInt(); bar.rightMargin = (24 * dp).toInt()
            root.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#F2121A2C"))
                cornerRadius = 32 * dp
                setStroke((2 * dp).toInt(), if (quiet) Color.parseColor("#3A4560") else c)
            }
            root.setPadding((18 * dp).toInt(), (6 * dp).toInt(), (6 * dp).toInt(), (8 * dp).toInt())
        } else {
            root.setBackgroundResource(R.drawable.bg_rally_panel)
            root.setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (14 * dp).toInt())
            title.maxLines = Int.MAX_VALUE
            (titleCol.layoutParams as? LinearLayout.LayoutParams)?.let { it.width = 0; it.weight = 1f; it.marginEnd = 0; titleCol.layoutParams = it }
            miniSub.visibility = View.GONE
            root.minimumWidth = 0
            title.setTextColor(Color.parseColor("#F8FAFC"))
            title.textSize = 20f
            connDot.background.mutate().setTint(connColor)
            heroLabel.visibility = View.VISIBLE
            heroLabel.text = hero.label
            miniTime.visibility = View.GONE
            heroTime.visibility = if (heroTime.text.isEmpty()) View.GONE else View.VISIBLE
            bar.height = (6 * dp).toInt(); bar.topMargin = (6 * dp).toInt()
            bar.leftMargin = 0; bar.rightMargin = 0
            heroProgress.visibility = View.VISIBLE
        }
        heroProgress.layoutParams = bar
    }
}
