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
        const val MAX_VISIBLE_ROWS = 5
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
    private val rowsScroll = root.findViewById<MaxHeightScrollView>(R.id.rallyRowsScroll)
    private val adminBar = root.findViewById<View>(R.id.rallyAdminBar)
    private val btnStart = root.findViewById<TextView>(R.id.rallyBtnStart)
    private val btnStop = root.findViewById<TextView>(R.id.rallyBtnStop)
    private val blocked = root.findViewById<TextView>(R.id.rallyBlockedReason)

    init {
        root.findViewById<View>(R.id.rallyMinimize).setOnClickListener { callbacks.onMinimize() }
        root.findViewById<View>(R.id.rallyClose).setOnClickListener { callbacks.onClose() }
        // ✎: 관리자 편집 모드. 평소엔 읽기 전용으로 깔끔하게, 켜면 −/+ · ✕ · 밑줄(눌러서 고치기)이 나타난다.
        root.findViewById<View>(R.id.rallyEdit).setOnClickListener { editMode = !editMode; deleteGuard.reset(); lastRender?.invoke() }
        // 카운트다운 상자(단계명·시간·안내)를 탭하면 축소/확대된다. 작은 —/▢ 버튼 옆의 ✕를 잘못 누르지 않게 큰 영역으로도 누를 수 있다.
        // 알약(최소화 상태)에서는 단계명이나 시간을 탭하면 펼쳐진다. 창을 끄는 일은 ✕만 한다.
        listOf<View>(heroLabel, heroTime, heroSub).forEach { v -> v.setOnClickListener { callbacks.onMinimize() } }
        title.setOnClickListener { if (isMinimized) callbacks.onMinimize() else callbacks.onTitleTap() }
        btnStart.setOnClickListener { callbacks.onStart() }
        btnStop.setOnClickListener { callbacks.onStop() }
        listOf<View>(btnStart, btnStop).forEach { pressFeel(it) }
        root.findViewById<TextView>(R.id.devToggle).setOnClickListener { t ->
            val sec = root.findViewById<View>(R.id.devSection)
            val open = sec.visibility != View.VISIBLE
            sec.visibility = if (open) View.VISIBLE else View.GONE
            (t as TextView).text = if (open) "내 기기 ▴" else "내 기기 ▾"
        }
        root.findViewById<View>(R.id.rallyAddTeam).setOnClickListener { callbacks.onAddTeam() }
        root.findViewById<View>(R.id.setPrep).setOnClickListener { callbacks.onEditPrep(prepShown) }
        root.findViewById<View>(R.id.setWait3).setOnClickListener { callbacks.onSetWait(180.0) }
        root.findViewById<View>(R.id.setWait5).setOnClickListener { callbacks.onSetWait(300.0) }
        root.findViewById<View>(R.id.setWait10).setOnClickListener { callbacks.onSetWait(600.0) }
        root.findViewById<View>(R.id.devRoom).setOnClickListener { callbacks.onEditRoom(roomShown) }
        root.findViewById<View>(R.id.devCharName).setOnClickListener { callbacks.onEditCharacterName(charNameShown) }
        root.findViewById<View>(R.id.devMs).setOnClickListener { callbacks.onEditCorrection(correctionShownMs) } // 눌러서 초 단위로 직접 입력
        root.findViewById<View>(R.id.devMinus).setOnClickListener { callbacks.onCorrectionDelta(-100) }
        root.findViewById<View>(R.id.devPlus).setOnClickListener { callbacks.onCorrectionDelta(100) }
        listOf(R.id.devMinus1s to -1000, R.id.devMinus500 to -500, R.id.devPlus500 to 500, R.id.devPlus1s to 1000).forEach { (id, ms) ->
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
    /** "내 기기"의 현재 방 줄. */
    fun renderRoom(code: String) {
        roomShown = code
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

    fun renderDevice(correctionMs: Int, posText: String, characterName: String, detailText: String = "") {
        charNameShown = characterName
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
    }

    fun render(model: ScreenModel, isAdmin: Boolean, hasStarted: Boolean = false, arrivalNote: String = "",
               conn: RallyConnection = RallyConnection.LIVE, urgent: Boolean = false) {
        val hero = model.hero
        val name = if (isAdmin) "관리자" else "집결장"
        // 연결 상태 점: 초록=실시간, 주황=1초 확인, 빨강=끊김. 알약(최소화)에서는 단계 표시가 대신 쓴다.
        title.text = if (isMinimized) name else android.text.SpannableString("● $name").apply {
            val c = when (conn) { RallyConnection.LIVE -> "#22C55E"; RallyConnection.POLLING -> "#F59E0B"; RallyConnection.OFFLINE -> "#EF4444" }
            setSpan(android.text.style.ForegroundColorSpan(Color.parseColor(c)), 0, 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        lastRender = { render(model, isAdmin, hasStarted, arrivalNote, conn, urgent) }
        val editing = isAdmin && model.editable && editMode
        root.findViewById<TextView>(R.id.rallyEdit).apply {
            visibility = if (isAdmin && model.editable) View.VISIBLE else View.GONE
            text = if (editMode) "✓" else "✎"
            setTextColor(Color.parseColor(if (editMode) "#60A5FA" else "#94A3B8"))
        }
        heroLabel.text = if (hero.note == null) hero.label else "${hero.label} · ${hero.note}"
        // 대기 중에는 "전원 도착 예정" 총 소요 시간을 흐리게 보여준다
        val previewTotal = hero.kind == HeroKind.IDLE && hero.remainingSec == null && model.arriveAtSec > 0.0
        heroTime.text = when {
            hero.remainingSec != null -> RallyScreenModel.formatMmSs(hero.remainingSec)
            previewTotal -> RallyScreenModel.formatMmSs(model.arriveAtSec)
            else -> ""
        }
        heroTime.visibility = if (heroTime.text.isEmpty()) View.GONE else View.VISIBLE
        heroTime.setTextColor(if (previewTotal) Color.parseColor("#64748B") else if (urgent) Color.parseColor("#F87171") else heroColor(hero))
        val sub = if (previewTotal) RallyScreenModel.idleSub(hero.subLabel, model.rows.count { !it.excluded }) else hero.subLabel
        // 집결이 시작된 뒤에는 한 줄만: "도착 예정 15:53:24 · ✓ 클릭함 15:47:56.080" (단계 설명은 큰 숫자·단계 표시가 대신한다)
        heroSub.text = if (arrivalNote.isEmpty()) sub else arrivalNote
        renderPhases(hero.kind, hero.phase)
        heroProgress.progress = (hero.progress * 1000).toInt()

        warning.visibility = if (model.warnings.isEmpty()) View.GONE else View.VISIBLE
        warning.text = model.warnings.joinToString("\n") { "⚠ $it" }

        renderRows(model.rows, isAdmin, model.editable, editing, hero.kind != HeroKind.IDLE && hero.kind != HeroKind.EXCLUDED, RallyTimelineScale(model.maxMarchSec), model.nowSec)

        adminBar.visibility = if (isAdmin) View.VISIBLE else View.GONE
        root.findViewById<View>(R.id.rallyAddTeam).visibility = if (isAdmin && model.editable) View.VISIBLE else View.GONE
        prepShown = model.prepSec
        root.findViewById<View>(R.id.rallySettingsRow).visibility = if (isAdmin && model.editable) View.VISIBLE else View.GONE
        root.findViewById<TextView>(R.id.setPrep).text = "준비 ${model.prepSec.toInt()}초"
        listOf(R.id.setWait3 to 180.0, R.id.setWait5 to 300.0, R.id.setWait10 to 600.0).forEach { (id, sec) ->
            root.findViewById<TextView>(id).setTextColor(Color.parseColor(if (model.waitSec == sec) "#60A5FA" else "#CBD5E1"))
        }
        val canRegroup = model.hero.kind == HeroKind.ARRIVED || model.hero.kind == HeroKind.CANCELLED
        btnStart.text = if (canRegroup || hasStarted) "다시 집결" else "집결 시작"
        btnStart.isEnabled = model.startBlockedReason == null
        btnStop.isEnabled = !model.editable
        blocked.visibility = if (isAdmin && model.startBlockedReason != null) View.VISIBLE else View.GONE
        blocked.text = model.startBlockedReason ?: ""
    }

    private fun renderRows(list: List<TeamRowModel>, isAdmin: Boolean, editable: Boolean, editing: Boolean, showBars: Boolean, scale: RallyTimelineScale, nowSec: Double?) {
        // 팀 수가 적고(≤ 몇 개) 1초 단위 갱신이라, 줄 수가 같으면 재사용한다.
        if (rows.childCount != list.size) {
            rows.removeAllViews()
            val inflater = LayoutInflater.from(themed)
            repeat(list.size) { rows.addView(inflater.inflate(R.layout.item_rally_team_row, rows, false)) }
        }
        // 군단이 많으면 5줄 높이까지만 보이고 나머지는 스크롤한다(줄 높이는 첫 줄 기준).
        rows.post {
            val h = rows.getChildAt(0)?.height ?: 0
            rowsScroll.maxHeightPx = if (list.size > MAX_VISIBLE_ROWS && h > 0) h * MAX_VISIBLE_ROWS else 0
        }
        // 내 줄에는 이 폰의 "내 보정"(내 기기)도 더한다. 다른 집결장 폰의 보정은 방 데이터에 없어 알 수 없다.
        val lags = RallyPanelFormat.lagLabels(list.map { LagInput(it.marchSec, it.adminAdjustMs + (if (it.isMine) correctionShownMs else 0), it.excluded) })
        list.forEachIndexed { i, r ->
            val v = rows.getChildAt(i)
            v.setBackgroundColor(if (r.isMine) Color.parseColor("#1F3B82F6") else Color.TRANSPARENT)
            v.findViewById<TextView>(R.id.rowDot).apply {
                setTextColor(if (r.online) Color.parseColor("#22C55E") else Color.parseColor("#64748B"))
                // 관리자는 ● 를 눌러 이 군단을 제외하거나 다시 포함한다(눌러도 되는 크기로 여백을 준다)
                val pad = (10 * resources.displayMetrics.density).toInt()
                setPadding(pad / 2, pad, pad, pad)
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
                // 가장 먼저 누르는 군단 줄에는 "기준". 제외된 군단은 표시하지 않는다.
                val main = RallyPanelFormat.sec(r.marchSec) + "s"
                val sub = lags[i]
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                text = if (sub.isEmpty()) android.text.SpannableStringBuilder(main)
                else android.text.SpannableStringBuilder("$main\n$sub").apply {
                    setSpan(android.text.style.RelativeSizeSpan(0.8f), main.length + 1, length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            v.findViewById<TextView>(R.id.rowStatus).apply {
                // 평소: 상태 글자. 관리자가 더해 준 보정이 있으면 아래 줄에 작게 보여 준다(줄이 늘어나도 폭은 그대로).
                // 편집 모드: 상태 대신 "보정 / 0초"를 보여 준다. 상태 글자("취소됨" 등)에 밑줄이 붙어 버튼처럼 보이던 혼동을 없앤다.
                text = when {
                    editing && !r.excluded -> "보정\n" + RallyInputParse.formatCorrection(r.adminAdjustMs)
                    r.adminAdjustMs == 0 -> r.statusLabel
                    else -> r.statusLabel + "\n" + RallyInputParse.formatCorrection(r.adminAdjustMs)
                }
                // 편집 모드에서 누르면: 제외된 군단의 "제외"는 다시 포함, 그 밖에는 보정 입력(진행 중에는 잠김)
                setOnClickListener {
                    if (editing) {
                        if (r.excluded) callbacks.onToggleExclude(r.id) else callbacks.onEditAdminAdjust(r.id, r.name, r.adminAdjustMs)
                    }
                }
                // 눌러서 보정을 정할 수 있다는 표시: 편집 모드에서만 밑줄
                paintFlags = if (editing) paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                else paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
            }
            v.findViewById<RallyTimelineBar>(R.id.rowBar).visibility = if (showBars) View.VISIBLE else View.GONE
            v.findViewById<RallyTimelineBar>(R.id.rowBar).set(scale, r.clickAtSec, r.departAtSec, r.arriveAtSec, nowSec, r.excluded)
            val minus = v.findViewById<View>(R.id.rowMinus)
            val plus = v.findViewById<View>(R.id.rowPlus)
            val canEdit = editing
            // 행군시간 고치기: 관리자는 편집 모드에서 모든 군단, 집결장은 평소에도 내 군단만
            val canMarch = editable && !r.excluded && (if (isAdmin) editing else r.isMine)
            minus.visibility = if (canMarch) View.VISIBLE else View.GONE
            plus.visibility = minus.visibility
            v.findViewById<TextView>(R.id.rowDel).apply {
                visibility = if (canEdit) View.VISIBLE else View.GONE
                // 첫 탭은 "삭제?"로 바뀌기만 하고, 3초 안에 한 번 더 눌러야 지운다
                text = if (deleteGuard.isArmed(r.id, android.os.SystemClock.elapsedRealtime())) "삭제?" else "✕"
                setOnClickListener {
                    if (deleteGuard.onTap(r.id, android.os.SystemClock.elapsedRealtime())) callbacks.onRemoveTeam(r.id)
                    else lastRender?.invoke()
                }
            }
            v.findViewById<TextView>(R.id.rowMarch).setOnClickListener { if (canMarch) callbacks.onEditMarch(r.id, r.marchSec) }
            minus.setOnClickListener { callbacks.onMarchDelta(r.id, -1.0) }
            plus.setOnClickListener { callbacks.onMarchDelta(r.id, 1.0) }
            v.setOnClickListener(null); v.isClickable = false // 줄 빈 곳을 눌러도 아무 일도 없게 한다(실수로 제외되던 문제)
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
        heroProgress.visibility = hide
        rowsScroll.visibility = hide
        root.findViewById<View>(R.id.phaseRow).visibility = hide
        root.findViewById<View>(R.id.devToggle).visibility = hide
        if (min) {
            root.findViewById<View>(R.id.devSection).visibility = View.GONE
            root.findViewById<View>(R.id.rallyAddTeam).visibility = View.GONE
            root.findViewById<View>(R.id.rallySettingsRow).visibility = View.GONE
        }
        if (min) {
            adminBar.visibility = View.GONE
            warning.visibility = View.GONE
            blocked.visibility = View.GONE
        }
        val dp = root.resources.displayMetrics.density
        if (min) {
            // 알약: 단계 색 점 + 짧은 단계명 한 줄, 아래에 큰 시간. 테두리도 단계 색으로 은은하게 칠한다.
            val c = heroColor(hero)
            title.text = "● " + minLabel(hero)
            title.setTextColor(c)
            title.textSize = 13f
            title.maxLines = 1
            title.ellipsize = android.text.TextUtils.TruncateAt.END
            (title.layoutParams as? LinearLayout.LayoutParams)?.let { it.width = LinearLayout.LayoutParams.WRAP_CONTENT; it.weight = 0f; it.marginEnd = (10 * dp).toInt(); title.layoutParams = it }
            root.minimumWidth = (112 * dp).toInt()
            heroLabel.visibility = View.GONE
            heroTime.textSize = 28f
            heroTime.visibility = if (heroTime.text.isEmpty()) View.GONE else View.VISIBLE
            root.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#F20F172A"))
                cornerRadius = 22 * dp
                setStroke((1.5f * dp).toInt(), (c and 0x00FFFFFF) or (0x99 shl 24))
            }
            root.setPadding((12 * dp).toInt(), (6 * dp).toInt(), (6 * dp).toInt(), (8 * dp).toInt())
        } else {
            root.setBackgroundResource(R.drawable.bg_rally_panel)
            root.setPadding((12 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt())
            title.maxLines = Int.MAX_VALUE
            (title.layoutParams as? LinearLayout.LayoutParams)?.let { it.width = 0; it.weight = 1f; it.marginEnd = 0; title.layoutParams = it }
            root.minimumWidth = 0
            title.setTextColor(Color.parseColor("#F8FAFC"))
            title.textSize = 14f
            heroLabel.visibility = View.VISIBLE
            heroLabel.text = hero.label
            heroTime.textSize = 40f
            heroTime.visibility = if (heroTime.text.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}

