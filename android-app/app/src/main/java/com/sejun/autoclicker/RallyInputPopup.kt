package com.sejun.autoclicker

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 한 칸 입력 팝업(기본은 숫자, freeText=true면 글자). 키보드가 떠야 해서 포커스를 받는 별도 오버레이 창으로 띄운다.
 * 패널 본체는 포커스를 받지 않는 창이라(게임 터치를 가로채지 않으려고) 입력은 이 창이 맡는다.
 */
class RallyInputPopup(private val context: Context, private val wm: WindowManager) {
    private var view: View? = null

    /** 설정의 투명도를 입력창에 반영한다(최소 40%). */
    fun applyAlpha(a: Float) { view?.alpha = a.coerceAtLeast(0.4f) }

    fun show(title: String, initial: String, signed: Boolean = false, freeText: Boolean = false, errorText: String? = null, onOk: (String) -> Boolean) {
        dismiss()
        fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#121A2C")); cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.parseColor("#33CBD5E1"))
            }
        }
        card.addView(TextView(context).apply {
            text = title; setTextColor(Color.parseColor("#F1F5F9")); textSize = 14f
        })
        val input = EditText(context).apply {
            setText(initial); setSelectAllOnFocus(true)
            inputType = if (freeText) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                else InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or (if (signed) InputType.TYPE_NUMBER_FLAG_SIGNED else 0)
            if (freeText) filters = arrayOf(android.text.InputFilter.LengthFilter(RallyRoster.MAX_NAME))
            imeOptions = EditorInfo.IME_ACTION_DONE
            setTextColor(Color.WHITE); setHintTextColor(Color.parseColor("#64748B"))
            setPadding(dp(4), dp(10), dp(4), dp(10))
        }
        card.addView(input)
        val error = TextView(context).apply {
            setTextColor(Color.parseColor("#F87171")); textSize = 12f; visibility = View.GONE
        }
        card.addView(error)

        fun button(label: String, bg: String, fg: String, action: () -> Unit) = TextView(context).apply {
            text = label; gravity = Gravity.CENTER; setTextColor(Color.parseColor(fg)); textSize = 14f
            background = GradientDrawable().apply { setColor(Color.parseColor(bg)); cornerRadius = dp(12).toFloat() }
            setOnClickListener { action() }
        }
        fun submit() {
            if (onOk(input.text.toString())) dismiss()
            else { error.text = errorText ?: "0 ~ ${RallyInputParse.MAX_MARCH_SEC.toInt()} 사이 숫자를 입력해 주세요"; error.visibility = View.VISIBLE }
        }
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val lp = { m: Int -> LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginStart = m } }
        row.addView(button("취소", "#334155", "#E2E8F0") { dismiss() }, lp(0))
        row.addView(button("확인", "#2563EB", "#FFFFFF") { submit() }, lp(dp(8)))
        card.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
        input.setOnEditorActionListener { _, _, _ -> submit(); true }

        val lpWin = WindowManager.LayoutParams(
            dp(280), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, // 포커스를 받아야 키보드가 뜬다
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN or WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        }
        card.alpha = PreferencesHelper.getOverlayAlpha(context).coerceAtLeast(0.4f)
        wm.addView(card, lpWin)
        view = card
        input.requestFocus()
    }

    fun dismiss() {
        view?.let { try { wm.removeView(it) } catch (_: Exception) { } }
        view = null
    }
}
