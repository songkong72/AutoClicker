package com.sejun.autoclicker

import android.content.Context
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

/**
 * 통합 집결 화면. 계산은 RallyScreenModel이 하고, 이 클래스는 결과를 그리기만 한다.
 * 윈도우 추가/제거와 드래그는 호출하는 쪽(AutoClickService)이 맡는다.
 */
class RallyPanelView(context: Context, private val callbacks: Callbacks) {

    interface Callbacks {
        fun onStart()
        fun onStop()
        fun onMinimize()
        fun onMarchDelta(teamId: String, deltaSec: Double)
        fun onToggleExclude(teamId: String)
        fun onSelectMine(teamId: String)
    }

    private val themed = ContextThemeWrapper(context, R.style.Theme_AutoClicker)
    val root: View = LayoutInflater.from(themed).inflate(R.layout.layout_rally_panel, null)

    private val heroLabel = root.findViewById<TextView>(R.id.rallyHeroLabel)
    private val heroTime = root.findViewById<TextView>(R.id.rallyHeroTime)
    private val heroSub = root.findViewById<TextView>(R.id.rallyHeroSub)
    private val heroProgress = root.findViewById<ProgressBar>(R.id.rallyHeroProgress)
    private val warning = root.findViewById<TextView>(R.id.rallyWarning)
    private val rows = root.findViewById<LinearLayout>(R.id.rallyRows)
    private val adminBar = root.findViewById<View>(R.id.rallyAdminBar)
    private val btnStart = root.findViewById<Button>(R.id.rallyBtnStart)
    private val btnStop = root.findViewById<Button>(R.id.rallyBtnStop)
    private val blocked = root.findViewById<TextView>(R.id.rallyBlockedReason)

    init {
        root.findViewById<View>(R.id.rallyMinimize).setOnClickListener { callbacks.onMinimize() }
        btnStart.setOnClickListener { callbacks.onStart() }
        btnStop.setOnClickListener { callbacks.onStop() }
    }

    fun render(model: ScreenModel, isAdmin: Boolean, hasStarted: Boolean = false) {
        val hero = model.hero
        heroLabel.text = hero.label
        heroTime.text = hero.remainingSec?.let { RallyScreenModel.formatMmSs(it) } ?: "--:--"
        heroTime.setTextColor(heroColor(hero.kind))
        heroSub.text = hero.subLabel
        heroProgress.progress = (hero.progress * 1000).toInt()

        warning.visibility = if (model.warnings.isEmpty()) View.GONE else View.VISIBLE
        warning.text = model.warnings.joinToString("\n") { "⚠ $it" }

        renderRows(model.rows, isAdmin && model.editable)

        adminBar.visibility = if (isAdmin) View.VISIBLE else View.GONE
        val canRegroup = model.hero.kind == HeroKind.ARRIVED || model.hero.kind == HeroKind.CANCELLED
        btnStart.text = if (canRegroup || hasStarted) "다시 집결" else "집결 시작"
        btnStart.isEnabled = model.startBlockedReason == null
        btnStop.isEnabled = !model.editable
        blocked.visibility = if (isAdmin && model.startBlockedReason != null) View.VISIBLE else View.GONE
        blocked.text = model.startBlockedReason ?: ""
    }

    private fun renderRows(list: List<TeamRowModel>, canEdit: Boolean) {
        // 팀 수가 적고(≤ 몇 개) 1초 단위 갱신이라, 줄 수가 같으면 재사용한다.
        if (rows.childCount != list.size) {
            rows.removeAllViews()
            val inflater = LayoutInflater.from(themed)
            repeat(list.size) { rows.addView(inflater.inflate(R.layout.item_rally_team_row, rows, false)) }
        }
        list.forEachIndexed { i, r ->
            val v = rows.getChildAt(i)
            v.findViewById<TextView>(R.id.rowDot).setTextColor(if (r.online) Color.parseColor("#22C55E") else Color.parseColor("#64748B"))
            v.findViewById<TextView>(R.id.rowName).apply {
                text = listOf(r.name, r.leaderName).filter { it.isNotBlank() }.joinToString(" ")
                setTextColor(if (r.isMine) Color.parseColor("#60A5FA") else Color.parseColor("#F1F5F9"))
                alpha = if (r.excluded) 0.45f else 1f
            }
            v.findViewById<TextView>(R.id.rowMarch).text = "행군 ${r.marchSec.toInt()}s"
            v.findViewById<TextView>(R.id.rowStatus).text = r.statusLabel
            val minus = v.findViewById<View>(R.id.rowMinus)
            val plus = v.findViewById<View>(R.id.rowPlus)
            minus.visibility = if (canEdit && !r.excluded) View.VISIBLE else View.GONE
            plus.visibility = minus.visibility
            minus.setOnClickListener { callbacks.onMarchDelta(r.id, -1.0) }
            plus.setOnClickListener { callbacks.onMarchDelta(r.id, 1.0) }
            v.setOnClickListener { if (canEdit) callbacks.onToggleExclude(r.id) }
            v.setOnLongClickListener { callbacks.onSelectMine(r.id); true }
            v.findViewById<TextView>(R.id.rowRemain).text = r.remainingSec?.let { RallyScreenModel.formatMmSs(it) } ?: ""
        }
    }

    private fun heroColor(kind: HeroKind): Int = Color.parseColor(
        when (kind) {
            HeroKind.MOVE -> "#FBBF24"
            HeroKind.GATHERING -> "#60A5FA"
            HeroKind.MARCHING -> "#A78BFA"
            HeroKind.ARRIVED -> "#22C55E"
            HeroKind.CANCELLED -> "#F87171"
            HeroKind.EXCLUDED -> "#64748B"
            else -> "#FFFFFF"
        }
    )

    /** 최소화: 카운트다운 한 줄만 남기고 나머지는 숨긴다. */
    fun setMinimized(min: Boolean, hero: HeroModel) {
        val hide = if (min) View.GONE else View.VISIBLE
        heroSub.visibility = hide
        heroProgress.visibility = hide
        rows.visibility = hide
        if (min) {
            adminBar.visibility = View.GONE
            warning.visibility = View.GONE
            blocked.visibility = View.GONE
        }
        heroLabel.text = if (min) "${hero.label}  ${hero.remainingSec?.let { RallyScreenModel.formatMmSs(it) } ?: ""}" else hero.label
        heroTime.visibility = if (min) View.GONE else View.VISIBLE
    }
}
