package com.sejun.autoclicker

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * 목록에서 하나를 고르는 팝업(관리자가 군단을 맡을 사람을 고를 때). 키보드가 필요 없어 포커스를 받지 않는다.
 */
class RallyPickPopup(private val context: Context, private val wm: WindowManager) {
    class Item(val label: String, val color: String = "#E2E8F0", val onPick: () -> Unit)

    private var view: View? = null

    fun applyAlpha(a: Float) { view?.alpha = a.coerceAtLeast(0.4f) }

    /** [items]는 스크롤되는 선택 목록, [footer]는 아래에 고정되는 버튼들. 항목을 누르면 팝업이 닫히고 그 동작이 실행된다. */
    fun show(title: String, items: List<Item>, emptyText: String, footer: List<Item>) {
        dismiss()
        val dm = context.resources.displayMetrics
        fun dp(v: Int) = (v * dm.density).toInt()

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F20F172A")); cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.parseColor("#33CBD5E1"))
            }
        }
        card.addView(TextView(context).apply { text = title; setTextColor(Color.parseColor("#F1F5F9")); textSize = 14f })

        fun button(item: Item, bg: String) = TextView(context).apply {
            text = item.label; gravity = Gravity.CENTER_VERTICAL; setTextColor(Color.parseColor(item.color)); textSize = 14f
            setPadding(dp(12), 0, dp(12), 0)
            background = GradientDrawable().apply { setColor(Color.parseColor(bg)); cornerRadius = dp(12).toFloat() }
            setOnClickListener { dismiss(); item.onPick() }
        }
        val itemHeight = dp(44)
        val gap = dp(6)
        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        if (items.isEmpty()) {
            list.addView(TextView(context).apply {
                text = emptyText; setTextColor(Color.parseColor("#94A3B8")); textSize = 13f
                setPadding(0, dp(12), 0, dp(4))
            })
        } else {
            items.forEach { list.addView(button(it, "#1E293B"), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, itemHeight).apply { topMargin = gap }) }
        }
        // 항목이 많으면 화면의 절반까지만 보이고 스크롤한다
        val maxListHeight = (dm.heightPixels * 0.5f).toInt()
        val wanted = if (items.isEmpty()) LinearLayout.LayoutParams.WRAP_CONTENT else Math.min(items.size * (itemHeight + gap) + gap, maxListHeight)
        card.addView(ScrollView(context).apply { addView(list) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, wanted))
        footer.forEach {
            card.addView(button(it, "#334155"), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, itemHeight).apply { topMargin = dp(10) })
        }

        val lpWin = WindowManager.LayoutParams(
            dp(280), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        card.alpha = PreferencesHelper.getOverlayAlpha(context).coerceAtLeast(0.4f)
        wm.addView(card, lpWin)
        view = card
    }

    /**
     * 방 선택 창. 줄을 누르면 고르기만 하고(왼쪽 동그라미·파란 테두리), 아래 큰 버튼을 눌러야 [onMove]가 실행된다.
     * [rooms]는 (번호, "번호 · 설명" 형식의 글). 현재 방([selected])에는 "현재" 배지, 진행 중인 방에는 "집결 중" 배지가 붙는다.
     */
    fun showSelect(title: String, rooms: List<Pair<String, String>>, selected: String, moveLabel: String, onMove: (String) -> Unit) {
        dismiss()
        val dm = context.resources.displayMetrics
        fun dp(v: Int) = (v * dm.density).toInt()
        fun color(hex: String) = Color.parseColor(hex)
        var chosen = selected

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(10), dp(18), dp(18))
            background = GradientDrawable().apply {
                setColor(color("#F2121A2C")); cornerRadius = dp(28).toFloat()
                setStroke(dp(1), color("#2A3550"))
            }
        }
        // 제목 줄: 제목은 왼쪽, 닫기(✕) 아이콘은 오른쪽
        val head = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val titleView = TextView(context).apply {
            text = title; setTextColor(color("#F1F5F9")); textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD); maxLines = 2
        }
        selectTitleView = titleView
        head.addView(titleView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        head.addView(android.widget.ImageView(context).apply {
            setImageResource(R.drawable.ic_rp_close); scaleType = android.widget.ImageView.ScaleType.CENTER
            contentDescription = "닫기"
            setOnClickListener { dismiss() }
        }, LinearLayout.LayoutParams(dp(44), dp(44)).apply { rightMargin = -dp(10) })
        card.addView(head)

        fun badge(text: String, fg: String, bg: String) = TextView(context).apply {
            this.text = text; setTextColor(color(fg)); textSize = 11f; gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dp(8), 0, dp(8), 0)
            background = GradientDrawable().apply { setColor(color(bg)); cornerRadius = dp(10).toFloat() }
        }

        val rowViews = LinkedHashMap<String, Pair<View, View>>() // 번호 → (줄, 동그라미)
        val moveBtn = TextView(context).apply {
            gravity = Gravity.CENTER; setTextColor(Color.WHITE); textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = GradientDrawable().apply { setColor(color("#2F5FE3")); cornerRadius = dp(16).toFloat() }
            setOnClickListener { if (chosen != selected) { dismiss(); onMove(chosen) } }
        }
        fun paint() {
            rowViews.forEach { (code, pair) ->
                val on = code == chosen
                pair.first.background = GradientDrawable().apply {
                    setColor(color(if (on) "#1C2B4D" else "#1E2A44")); cornerRadius = dp(14).toFloat()
                    setStroke(dp(2), if (on) color("#3B82F6") else Color.TRANSPARENT)
                }
                // 고른 줄: 파란 동그라미 안에 흰 점, 나머지: 빈 테두리
                pair.second.background = if (on) android.graphics.drawable.LayerDrawable(arrayOf(
                    GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color("#2F5FE3")) },
                    GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.WHITE) }
                )).apply { setLayerInset(1, dp(7), dp(7), dp(7), dp(7)) }
                else GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.TRANSPARENT); setStroke(dp(2), color("#5B6B8A")) }
            }
            // 현재 방을 고른 상태에서는 옮길 곳이 없으니 버튼을 흐리게 둔다
            val canMove = chosen != selected
            moveBtn.text = if (canMove) "$chosen 방으로 $moveLabel" else "옮길 방을 골라 주세요"
            moveBtn.alpha = if (canMove) 1f else 0.4f
        }
        val itemHeight = dp(60)
        val gap = dp(6)
        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        rooms.forEach { (code, label) ->
            // 첫 줄은 방 번호와 배지, 둘째 줄은 군단·배정 수를 작게
            var detail = label.removePrefix("$code · ").takeIf { it != label }.orEmpty()
            val running = detail.endsWith(RoomList.PICK_RUNNING)
            if (running) detail = detail.removeSuffix(RoomList.PICK_RUNNING)
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), 0, dp(14), 0)
                setOnClickListener { chosen = code; paint() }
            }
            val radio = View(context)
            row.addView(radio, LinearLayout.LayoutParams(dp(22), dp(22)).apply { rightMargin = dp(12) })
            val texts = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            val first = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            first.addView(TextView(context).apply {
                text = code; setTextColor(color("#F1F5F9")); textSize = 18f; setTypeface(null, android.graphics.Typeface.BOLD)
            })
            if (code == selected) first.addView(badge("현재", "#CBD5E1", "#26324A"),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(20)).apply { leftMargin = dp(8) })
            if (running) first.addView(badge("집결 중", "#FBBF24", "#29FBBF24"),
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(20)).apply { leftMargin = dp(8) })
            texts.addView(first)
            if (detail.isNotEmpty()) texts.addView(TextView(context).apply {
                text = detail; setTextColor(color("#A9B4C7")); textSize = 12f; maxLines = 1
            })
            row.addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            rowViews[code] = row to radio
            list.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, itemHeight).apply { topMargin = gap })
        }
        paint()
        // 방이 많으면 화면의 절반까지만 보이고 스크롤한다
        val maxListHeight = (dm.heightPixels * 0.5f).toInt()
        val wanted = Math.min(rooms.size * (itemHeight + gap), maxListHeight)
        card.addView(ScrollView(context).apply { addView(list); isVerticalScrollBarEnabled = false },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, wanted))
        card.addView(moveBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54)).apply { topMargin = dp(14) })

        // 패널과 같은 폭
        val width = Math.min((dm.widthPixels * 0.94f).toInt(), dp(332))
        val lpWin = WindowManager.LayoutParams(
            width, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        card.alpha = PreferencesHelper.getOverlayAlpha(context).coerceAtLeast(0.4f)
        wm.addView(card, lpWin)
        view = card
    }

    private var selectTitleView: TextView? = null

    /** 방 선택 창이 열려 있으면 목록은 그대로 두고 제목만 바꾼다(고른 방이 풀리지 않게). 열려 있지 않으면 false. */
    fun updateSelectTitle(title: String): Boolean {
        val t = selectTitleView ?: return false
        if (view == null) return false
        t.text = title
        return true
    }

    fun dismiss() {
        view?.let { try { wm.removeView(it) } catch (_: Exception) { } }
        view = null
        selectTitleView = null
    }
}
