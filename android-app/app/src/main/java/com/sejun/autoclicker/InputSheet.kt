package com.sejun.autoclicker

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/** 아래에서 올라오는 한 줄 입력 창. 방 선택 창([RoomSheet])과 같은 모양이다. 숫자만 받는다. */
internal class InputSheet(
    private val activity: Activity,
    private val title: String,
    private val message: String,
    private val hint: String,
    private val maxLength: Int,
    private val submitLabel: String,
    private val onSubmit: (String) -> Unit
) {
    private val dp = activity.resources.displayMetrics.density
    private fun px(v: Int) = (v * dp).toInt()

    fun show() {
        val d = BottomSheetDialog(activity)
        val ink = Color.parseColor("#16181D")
        val sub = Color.parseColor("#5B6270")
        val blue = Color.parseColor("#1F3FA6")
        fun box(color: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
            setColor(color); cornerRadius = radius * dp
            if (stroke != null) setStroke((1.5f * dp).toInt(), stroke)
        }
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(16), px(10), px(16), px(28))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadii = floatArrayOf(28 * dp, 28 * dp, 28 * dp, 28 * dp, 0f, 0f, 0f, 0f)
            }
        }
        root.addView(View(activity).apply { background = box(Color.parseColor("#C9CDD6"), 2) },
            LinearLayout.LayoutParams(px(40), px(4)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        root.addView(TextView(activity).apply {
            text = title; textSize = 20f; setTextColor(ink); setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL; setPadding(px(4), 0, px(4), 0)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(44)).apply { topMargin = px(10) })
        root.addView(TextView(activity).apply {
            text = message; textSize = 14f; setTextColor(sub); setLineSpacing(0f, 1.25f); setPadding(px(4), 0, px(4), 0)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val input = EditText(activity).apply {
            this.hint = hint
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(maxLength))
            setSingleLine(true)
            textSize = 20f; setTextColor(ink); setHintTextColor(Color.parseColor("#9AA1AD"))
            letterSpacing = 0.04f
            background = box(Color.parseColor("#F2F3F5"), 16)
            setPadding(px(16), 0, px(16), 0)
        }
        root.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(56)).apply { topMargin = px(14) })

        root.addView(LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(TextView(activity).apply {
                text = "취소"; textSize = 16f; setTextColor(ink); setTypeface(typeface, Typeface.BOLD); gravity = Gravity.CENTER
                background = box(Color.WHITE, 16, Color.parseColor("#C9CDD6"))
                setOnClickListener { d.dismiss() }
            }, LinearLayout.LayoutParams(0, px(52), 1f))
            addView(TextView(activity).apply {
                text = submitLabel; textSize = 16f; setTextColor(Color.WHITE); setTypeface(typeface, Typeface.BOLD); gravity = Gravity.CENTER
                background = box(blue, 16)
                setOnClickListener { d.dismiss(); onSubmit(input.text.toString()) }
            }, LinearLayout.LayoutParams(0, px(52), 1f).apply { leftMargin = px(8) })
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(14) })

        d.setContentView(root)
        // 자판이 올라와도 입력 칸과 버튼이 가려지지 않게 창을 자판 위로 올린다
        d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        d.setOnShowListener {
            d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
                sheet.setBackgroundColor(Color.TRANSPARENT)
                BottomSheetBehavior.from(sheet).apply { state = BottomSheetBehavior.STATE_EXPANDED; skipCollapsed = true }
            }
        }
        d.show()
    }
}
