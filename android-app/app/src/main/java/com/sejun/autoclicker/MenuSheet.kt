package com.sejun.autoclicker

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/** 아래에서 올라오는 메뉴 창. 방 선택 창([RoomSheet])과 같은 모양으로, 제목 아래에 누를 수 있는 줄들을 보여 준다. */
internal class MenuSheet(private val activity: Activity, private val title: String, private val items: List<Item>) {
    /** [badge]는 줄 오른쪽의 작은 꼬리표(예: "개발자 전용"). 없으면 null. */
    data class Item(val label: String, val badge: String? = null, val onClick: () -> Unit)

    private val dp = activity.resources.displayMetrics.density
    private fun px(v: Int) = (v * dp).toInt()

    fun show() {
        val d = BottomSheetDialog(activity)
        val ink = Color.parseColor("#16181D")
        val sub = Color.parseColor("#5B6270")
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(16), px(10), px(16), px(28))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadii = floatArrayOf(28 * dp, 28 * dp, 28 * dp, 28 * dp, 0f, 0f, 0f, 0f)
            }
        }
        root.addView(View(activity).apply {
            background = GradientDrawable().apply { setColor(Color.parseColor("#C9CDD6")); cornerRadius = 2 * dp }
        }, LinearLayout.LayoutParams(px(40), px(4)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        root.addView(TextView(activity).apply {
            text = title; textSize = 20f; setTextColor(ink); setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL; setPadding(px(4), 0, px(4), 0)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(44)).apply { topMargin = px(10) })

        items.forEachIndexed { i, item ->
            if (i > 0) root.addView(View(activity).apply { setBackgroundColor(Color.parseColor("#EEF0F3")) },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(1)).apply { leftMargin = px(14); rightMargin = px(14) })
            root.addView(LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(px(14), 0, px(8), 0)
                addView(TextView(activity).apply { text = item.label; textSize = 17f; setTextColor(ink) },
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                if (item.badge != null) addView(TextView(activity).apply {
                    text = item.badge; textSize = 12f; setTextColor(sub); gravity = Gravity.CENTER
                    setPadding(px(10), 0, px(10), 0)
                    background = GradientDrawable().apply { setColor(Color.parseColor("#F2F3F5")); cornerRadius = 13 * dp }
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(26)))
                addView(ImageView(activity).apply { setImageResource(R.drawable.ic_chevron_right); setColorFilter(Color.parseColor("#9AA1AD")) },
                    LinearLayout.LayoutParams(px(20), px(20)).apply { leftMargin = px(6) })
                setOnClickListener { d.dismiss(); item.onClick() }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(56)))
        }

        d.setContentView(root)
        d.setOnShowListener {
            d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
                sheet.setBackgroundColor(Color.TRANSPARENT)
                BottomSheetBehavior.from(sheet).apply { state = BottomSheetBehavior.STATE_EXPANDED; skipCollapsed = true }
            }
        }
        d.show()
    }
}
