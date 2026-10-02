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
        val PHASE_COLORS = listOf("#FBBF24", "#60A5FA", "#A78BFA", "#22C55E")
    }

    interface Callbacks {
        fun onStart()
        fun onStop()
        fun onMinimize()
        fun onClose()
        fun onMarchDelta(teamId: String, deltaSec: Double)
        fun onToggleExclude(teamId: String)
        fun onSelectMine(teamId: String)
        fun onCorrectionDelta(deltaMs: Int)
        fun onEditMarch(teamId: String, currentSec: Double)
        fun onAddTeam()
        fun onEditPrep(currentSec: Double)
        fun onSetWait(sec: Double)
        fun onRemoveTeam(teamId: String)
        fun onSavePosition()
    }

    private val themed = ContextThemeWrapper(context, R.style.Theme_AutoClicker)
    val root: View = LayoutInflater.from(themed).inflate(R.layout.layout_rally_panel, null)

    private var prepShown = 0.0
    private val title = root.findViewById<TextView>(R.id.rallyTitle)
    private val heroLabel = root.findViewById<TextView>(R.id.rallyHeroLabel)
    private val heroTime = root.findViewById<TextView>(R.id.rallyHeroTime)
    private val heroSub = root.findViewById<TextView>(R.id.rallyHeroSub)
    private val heroProgress = root.findViewById<ProgressBar>(R.id.rallyHeroProgress)
    private val warning = root.findViewById<TextView>(R.id.rallyWarning)
    private val rows = root.findViewById<LinearLayout>(R.id.rallyRows)
    private val adminBar = root.findViewById<View>(R.id.rallyAdminBar)
    private val btnStart = root.findViewById<TextView>(R.id.rallyBtnStart)
    private val btnStop = root.findViewById<TextView>(R.id.rallyBtnStop)
    private val blocked = root.findViewById<TextView>(R.id.rallyBlockedReason)

    init {
        root.findViewById<View>(R.id.rallyMinimize).setOnClickListener { callbacks.onMinimize() }
        root.findViewById<View>(R.id.rallyClose).setOnClickListener { callbacks.onClose() }
        // 알약(최소화 상태)의 단계명이나 시간을 탭해도 펼쳐진다
        listOf<View>(heroLabel, title, heroTime).forEach { v -> v.setOnClickListener { if (isMinimized) callbacks.onMinimize() } }
        btnStart.setOnClickListener { callbacks.onStart() }
        btnStop.setOnClickListener { callbacks.onStop() }
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
        root.findViewById<View>(R.id.devMinus).setOnClickListener { callbacks.onCorrectionDelta(-10) }
        root.findViewById<View>(R.id.devPlus).setOnClickListener { callbacks.onCorrectionDelta(10) }
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

    /** "내 기기" 줄: 현재 ms 보정과 저장된 클릭 위치 표시. */
    fun renderDevice(correctionMs: Int, posText: String) {
        root.findViewById<TextView>(R.id.devMs).text = (if (correctionMs > 0) "+" else "") + correctionMs + "ms"
        root.findViewById<TextView>(R.id.devPosStatus).text = posText
    }

    fun render(model: ScreenModel, isAdmin: Boolean, hasStarted: Boolean = false) {
        val hero = model.hero
        title.text = if (isAdmin) "집결 · 관리자" else "집결 · 팀장"
        heroLabel.text = hero.label
        // 대기 중에는 "전원 도착 예정" 총 소요 시간을 흐리게 보여준다
        val previewTotal = hero.kind == HeroKind.IDLE && hero.remainingSec == null && model.arriveAtSec > 0.0
        heroTime.text = when {
            hero.remainingSec != null -> RallyScreenModel.formatMmSs(hero.remainingSec)
            previewTotal -> RallyScreenModel.formatMmSs(model.arriveAtSec)
            else -> ""
        }
        heroTime.visibility = if (heroTime.text.isEmpty()) View.GONE else View.VISIBLE
        heroTime.setTextColor(if (previewTotal) Color.parseColor("#64748B") else heroColor(hero.kind))
        heroSub.text = if (previewTotal) hero.subLabel + " · 전원 ${RallyScreenModel.formatMmSs(model.arriveAtSec)} 후 도착" else hero.subLabel
        renderPhases(hero.kind)
        heroProgress.progress = (hero.progress * 1000).toInt()

        warning.visibility = if (model.warnings.isEmpty()) View.GONE else View.VISIBLE
        warning.text = model.warnings.joinToString("\n") { "⚠ $it" }

        renderRows(model.rows, isAdmin, model.editable, RallyTimelineScale(model.maxMarchSec), model.nowSec)

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

    private fun renderRows(list: List<TeamRowModel>, isAdmin: Boolean, editable: Boolean, scale: RallyTimelineScale, nowSec: Double?) {
        // 팀 수가 적고(≤ 몇 개) 1초 단위 갱신이라, 줄 수가 같으면 재사용한다.
        if (rows.childCount != list.size) {
            rows.removeAllViews()
            val inflater = LayoutInflater.from(themed)
            repeat(list.size) { rows.addView(inflater.inflate(R.layout.item_rally_team_row, rows, false)) }
        }
        list.forEachIndexed { i, r ->
            val v = rows.getChildAt(i)
            v.setBackgroundColor(if (r.isMine) Color.parseColor("#1F3B82F6") else Color.TRANSPARENT)
            v.findViewById<TextView>(R.id.rowDot).setTextColor(if (r.online) Color.parseColor("#22C55E") else Color.parseColor("#64748B"))
            v.findViewById<TextView>(R.id.rowName).apply {
                val base = listOf(r.name, r.leaderName).filter { it.isNotBlank() }.joinToString(" ")
                text = if (r.isMine) "$base ★나" else base
                setTypeface(null, if (r.isMine) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                setTextColor(if (r.isMine) Color.parseColor("#60A5FA") else Color.parseColor("#F1F5F9"))
                alpha = if (r.excluded) 0.45f else 1f
            }
            v.findViewById<TextView>(R.id.rowMarch).text = (if (r.marchSec % 1.0 == 0.0) r.marchSec.toInt().toString() else r.marchSec.toString()) + "s"
            v.findViewById<TextView>(R.id.rowStatus).text = r.statusLabel
            v.findViewById<RallyTimelineBar>(R.id.rowBar).set(scale, r.clickAtSec, r.departAtSec, r.arriveAtSec, nowSec, r.excluded)
            val minus = v.findViewById<View>(R.id.rowMinus)
            val plus = v.findViewById<View>(R.id.rowPlus)
            val canEdit = isAdmin && editable
            val canMarch = editable && !r.excluded && (isAdmin || r.isMine) // 팀장은 내 팀만
            minus.visibility = if (canMarch) View.VISIBLE else View.GONE
            plus.visibility = minus.visibility
            v.findViewById<View>(R.id.rowDel).apply {
                visibility = if (canEdit) View.VISIBLE else View.GONE
                setOnClickListener { callbacks.onRemoveTeam(r.id) }
            }
            v.findViewById<TextView>(R.id.rowMarch).setOnClickListener { if (canMarch) callbacks.onEditMarch(r.id, r.marchSec) }
            minus.setOnClickListener { callbacks.onMarchDelta(r.id, -1.0) }
            plus.setOnClickListener { callbacks.onMarchDelta(r.id, 1.0) }
            v.setOnClickListener { if (canEdit) callbacks.onToggleExclude(r.id) }
            v.setOnLongClickListener { callbacks.onSelectMine(r.id); true }
            v.findViewById<TextView>(R.id.rowRemain).apply {
                text = r.remainingSec?.let { RallyScreenModel.formatMmSs(it) } ?: ""
                visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    /** 대기 · 집결 · 행군 · 도착 중 지금 단계만 밝게 보여준다. */
    private fun renderPhases(kind: HeroKind) {
        val current = when (kind) {
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

    /** 최소화: 카운트다운 한 줄만 남기고 나머지는 숨긴다. */
    private var isMinimized = false

    fun setMinimized(min: Boolean, hero: HeroModel) {
        isMinimized = min
        root.findViewById<TextView>(R.id.rallyMinimize).text = if (min) "▢" else "—"
        val hide = if (min) View.GONE else View.VISIBLE
        heroSub.visibility = hide
        heroProgress.visibility = hide
        rows.visibility = hide
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
        if (min) {
            // 알약: [단계명(단계 색)            — ✕]  아래에 큰 시간. 제목 줄을 단계명으로 바꿔 한 덩어리로 보이게 한다.
            title.text = hero.label
            title.setTextColor(heroColor(hero.kind))
            title.textSize = 13f
            heroLabel.visibility = View.GONE
            heroTime.textSize = 28f
            heroTime.visibility = if (heroTime.text.isEmpty()) View.GONE else View.VISIBLE
        } else {
            title.setTextColor(Color.parseColor("#F8FAFC"))
            title.textSize = 14f
            heroLabel.visibility = View.VISIBLE
            heroLabel.text = hero.label
            heroTime.textSize = 40f
            heroTime.visibility = if (heroTime.text.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}
