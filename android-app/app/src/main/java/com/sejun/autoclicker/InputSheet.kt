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

/**
 * 아래에서 올라오는 입력 창. 방 선택 창([RoomSheet])과 같은 모양이다.
 * 입력 칸은 [fields]로 하나 이상 받는다. [onSubmit]이 true를 돌려주면 창을 닫고, false면 열어 둔다(입력이 잘못됐을 때).
 */
internal class InputSheet(
    private val activity: Activity,
    private val title: String,
    private val message: String,
    private val fields: List<Field>,
    private val submitLabel: String,
    private val showCancel: Boolean = true,
    private val link: Link? = null,
    private val onSubmit: (List<String>) -> Boolean
) {
    /** 입력 칸 하나. [label]이 있으면 칸 위에 이름을 붙인다. [inputType]은 android.text.InputType 값. */
    data class Field(
        val hint: String,
        val maxLength: Int,
        val inputType: Int = InputType.TYPE_CLASS_NUMBER,
        val label: String = "",
        val initial: String = ""
    )

    /** 버튼 아래의 작은 글자 단추. 누르면 창을 닫고 [onClick]을 한다. */
    data class Link(val label: String, val onClick: () -> Unit)

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

        val inputs = fields.map { f ->
            val numeric = f.inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_NUMBER
            val idle = box(Color.parseColor("#F2F3F5"), 16)
            val focused = box(Color.parseColor("#F2F3F5"), 16, blue)
            EditText(activity).apply {
                this.hint = f.hint
                inputType = f.inputType
                filters = arrayOf(InputFilter.LengthFilter(f.maxLength))
                setSingleLine(true)
                textSize = if (numeric) 20f else 17f
                setTextColor(ink); setHintTextColor(Color.parseColor("#9AA1AD"))
                if (numeric) letterSpacing = 0.04f
                background = idle
                setPadding(px(16), 0, px(16), 0)
                setOnFocusChangeListener { _, has -> background = if (has) focused else idle }
                if (f.initial.isNotEmpty()) setText(f.initial)
            }
        }
        fields.forEachIndexed { i, f ->
            if (f.label.isNotEmpty()) {
                root.addView(TextView(activity).apply {
                    text = f.label; textSize = 13f; setTextColor(ink); setTypeface(typeface, Typeface.BOLD)
                    setPadding(px(4), 0, px(4), 0)
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(if (i == 0) 14 else 12) })
            }
            root.addView(inputs[i], LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(56)).apply {
                topMargin = px(if (f.label.isNotEmpty()) 6 else if (i == 0) 14 else 10)
            })
        }

        root.addView(LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            if (showCancel) addView(TextView(activity).apply {
                text = "취소"; textSize = 16f; setTextColor(ink); setTypeface(typeface, Typeface.BOLD); gravity = Gravity.CENTER
                background = box(Color.WHITE, 16, Color.parseColor("#C9CDD6"))
                setOnClickListener { d.dismiss() }
            }, LinearLayout.LayoutParams(0, px(52), 1f))
            addView(TextView(activity).apply {
                text = submitLabel; textSize = 16f; setTextColor(Color.WHITE); setTypeface(typeface, Typeface.BOLD); gravity = Gravity.CENTER
                background = box(blue, 16)
                setOnClickListener { if (onSubmit(inputs.map { it.text.toString() })) d.dismiss() }
            }, LinearLayout.LayoutParams(0, px(52), 1f).apply { if (showCancel) leftMargin = px(8) })
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(14) })

        link?.let { l ->
            root.addView(TextView(activity).apply {
                text = l.label; textSize = 13f; setTextColor(sub); gravity = Gravity.CENTER
                setPadding(px(8), px(10), px(8), px(10))
                setOnClickListener { d.dismiss(); l.onClick() }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL; topMargin = px(6)
            })
        }

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
