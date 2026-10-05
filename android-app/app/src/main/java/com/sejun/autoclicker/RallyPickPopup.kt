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

    /** 목록에서 하나를 눌러 고른 뒤(강조 표시), 옆의 이동 버튼을 눌러야 [onMove]가 실행된다. [rooms]는 (번호, 보여 줄 글). */
    fun showSelect(title: String, rooms: List<Pair<String, String>>, selected: String, moveLabel: String, onMove: (String) -> Unit) {
        dismiss()
        val dm = context.resources.displayMetrics
        fun dp(v: Int) = (v * dm.density).toInt()
        var chosen = selected

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F20F172A")); cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.parseColor("#33CBD5E1"))
            }
        }
        card.addView(TextView(context).apply { text = title; setTextColor(Color.parseColor("#F1F5F9")); textSize = 14f })

        val rows = LinkedHashMap<String, TextView>()
        fun paint() {
            rows.forEach { (code, tv) ->
                val on = code == chosen
                tv.setTextColor(Color.parseColor(if (on) "#FFFFFF" else "#E2E8F0"))
                tv.background = GradientDrawable().apply {
                    setColor(Color.parseColor(if (on) "#2563EB" else "#1E293B")); cornerRadius = dp(12).toFloat()
                }
            }
        }
        val itemHeight = dp(44)
        val gap = dp(6)
        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        rooms.forEach { (code, label) ->
            val tv = TextView(context).apply {
                text = if (code == selected) "$label  ·  현재" else label
                gravity = Gravity.CENTER_VERTICAL; textSize = 14f; setPadding(dp(12), 0, dp(12), 0)
                setOnClickListener { chosen = code; paint() }
            }
            rows[code] = tv
            list.addView(tv, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, itemHeight).apply { topMargin = gap })
        }
        paint()
        val maxListHeight = (dm.heightPixels * 0.5f).toInt()
        val wanted = Math.min(rooms.size * (itemHeight + gap) + gap, maxListHeight)

        // 목록은 왼쪽, 이동 버튼은 그 옆
        val body = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        body.addView(ScrollView(context).apply { addView(list) }, LinearLayout.LayoutParams(0, wanted, 1f))
        body.addView(TextView(context).apply {
            text = moveLabel; gravity = Gravity.CENTER; setTextColor(Color.parseColor("#F1F5F9")); textSize = 14f
            setPadding(dp(16), 0, dp(16), 0)
            background = GradientDrawable().apply { setColor(Color.parseColor("#334155")); cornerRadius = dp(12).toFloat() }
            setOnClickListener { dismiss(); if (chosen != selected) onMove(chosen) }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, itemHeight).apply { topMargin = gap; leftMargin = dp(8) })
        card.addView(body)

        val lpWin = WindowManager.LayoutParams(
            dp(300), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        card.alpha = PreferencesHelper.getOverlayAlpha(context).coerceAtLeast(0.4f)
        wm.addView(card, lpWin)
        view = card
    }

    fun dismiss() {
        view?.let { try { wm.removeView(it) } catch (_: Exception) { } }
        view = null
    }
}
