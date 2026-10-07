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
 * 아래에서 올라오는 시트 창의 공통 틀. 방 선택 창([RoomSheet])·입력 창([InputSheet])과 같은 모양이다.
 * 제목과 설명 아래에 [content]를 채우고, 맨 아래 버튼 줄은 [actions]로 정한다. 내용이 길면 스크롤된다.
 */
internal class SheetDialog(
    private val activity: Activity,
    private val title: String,
    private val message: String = "",
    /** false면 바깥을 눌러도, 뒤로 가기나 아래로 밀어도 닫히지 않는다. 버튼으로만 닫는다. */
    private val cancelable: Boolean = true
) {
    enum class Kind { PRIMARY, NORMAL, DANGER, DANGER_FILL }

    /** 버튼 줄의 버튼 하나. [onClick]이 true를 돌려주면 창을 닫고, false면 열어 둔다. */
    class Action(val label: String, val kind: Kind, val onClick: () -> Boolean)

    companion object {
        val INK = Color.parseColor("#16181D")
        val SUB = Color.parseColor("#5B6270")
        val BLUE = Color.parseColor("#1F3FA6")
        val FIELD = Color.parseColor("#F2F3F5")
        val LINE = Color.parseColor("#C9CDD6")
        val RED = Color.parseColor("#C62828")

        /** 누르면 [run]을 하고 창을 닫는 버튼. */
        fun act(label: String, kind: Kind = Kind.NORMAL, run: () -> Unit = {}) = Action(label, kind) { run(); true }

        /** 확인/취소를 묻는 창. [danger]면 확인 버튼이 빨갛다. */
        fun confirm(activity: Activity, title: String, message: String, confirmLabel: String, danger: Boolean = false, onConfirm: () -> Unit) {
            SheetDialog(activity, title, message).actions(
                act("취소"),
                act(confirmLabel, if (danger) Kind.DANGER_FILL else Kind.PRIMARY, onConfirm)
            ).show()
        }
    }

    private val dp = activity.resources.displayMetrics.density
    private fun px(v: Int) = (v * dp).toInt()
    private val dialog = BottomSheetDialog(activity)
    private var actionList: List<Action> = emptyList()

    /** 제목·설명 아래에 들어갈 내용. 호출하는 쪽이 여기에 뷰를 더한다. */
    val content = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }

    fun actions(vararg list: Action): SheetDialog { actionList = list.toList(); return this }
    fun dismiss() = dialog.dismiss()

    fun box(color: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = radius * dp
        if (stroke != null) setStroke((1.5f * dp).toInt(), stroke)
    }

    private fun lp(w: Int = ViewGroup.LayoutParams.MATCH_PARENT, h: Int = ViewGroup.LayoutParams.WRAP_CONTENT, top: Int = 0) =
        LinearLayout.LayoutParams(w, h).apply { topMargin = px(top) }

    /** 굵은 소제목. */
    fun heading(text: String, top: Int = 18): TextView = TextView(activity).apply {
        this.text = text; textSize = 15f; setTextColor(INK); setTypeface(typeface, Typeface.BOLD)
        setPadding(px(4), 0, px(4), 0)
    }.also { content.addView(it, lp(top = top)) }

    /** 연회색 둥근 상자. 안에 뷰를 더해 쓴다. */
    fun card(top: Int = 8): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        background = box(FIELD, 16)
        setPadding(px(14), px(12), px(14), px(12))
    }.also { content.addView(it, lp(top = top)) }

    fun line(parent: ViewGroup, text: String, small: Boolean = false, bold: Boolean = false, top: Int = 0): TextView = TextView(activity).apply {
        this.text = text; textSize = if (small) 12f else 14f
        setTextColor(if (small) SUB else INK)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(0f, 1.2f)
    }.also { parent.addView(it, lp(top = top)) }

    /** 작은 알약 모양 버튼. [selected]면 남색으로 채운다. */
    fun pill(label: String, kind: Kind = Kind.NORMAL, selected: Boolean = false, onClick: () -> Unit): TextView = TextView(activity).apply {
        text = label; textSize = 13f; gravity = Gravity.CENTER
        val (bg, fg) = colors(if (selected) Kind.PRIMARY else kind)
        setTextColor(fg); setTypeface(typeface, Typeface.BOLD)
        background = box(bg, 14)
        setPadding(px(12), 0, px(12), 0)
        setOnClickListener { onClick() }
    }

    /** 오른쪽 정렬된 작은 버튼 줄. */
    fun pillRow(parent: ViewGroup, vararg pills: TextView, top: Int = 10) {
        parent.addView(LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END
            pills.forEachIndexed { i, p -> addView(p, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(36)).apply { if (i > 0) leftMargin = px(8) }) }
        }, lp(top = top))
    }

    /** 같은 너비로 나란히 놓는 버튼 줄(내용 영역 안). */
    fun equalRow(vararg pills: TextView, top: Int = 10) {
        content.addView(LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            pills.forEachIndexed { i, p -> addView(p, LinearLayout.LayoutParams(0, px(44), 1f).apply { if (i > 0) leftMargin = px(8) }) }
        }, lp(top = top))
    }

    /** 내용 영역 전체 너비의 큰 버튼. */
    fun wideButton(label: String, kind: Kind = Kind.PRIMARY, top: Int = 14, onClick: () -> Unit): TextView = pill(label, kind) { onClick() }.apply {
        textSize = 16f
        background = box(colors(kind).first, 16)
    }.also { content.addView(it, lp(h = px(52), top = top)) }

    /** 입력 칸. 눌렀을 때 남색 테두리가 생긴다. */
    fun field(hint: String, maxLength: Int, inputType: Int = InputType.TYPE_CLASS_TEXT, initial: String = "", lines: Int = 1, top: Int = 14): EditText {
        val idle = box(FIELD, 16)
        val focused = box(FIELD, 16, BLUE)
        val e = EditText(activity).apply {
            this.hint = hint
            this.inputType = inputType
            filters = arrayOf(InputFilter.LengthFilter(maxLength))
            if (lines == 1) setSingleLine(true) else { minLines = lines; gravity = Gravity.TOP or Gravity.START }
            textSize = 17f; setTextColor(INK); setHintTextColor(Color.parseColor("#9AA1AD"))
            background = idle
            setPadding(px(16), px(if (lines == 1) 0 else 14), px(16), px(if (lines == 1) 0 else 14))
            setOnFocusChangeListener { _, has -> background = if (has) focused else idle }
            if (initial.isNotEmpty()) { setText(initial); setSelection(text.length) }
        }
        content.addView(e, if (lines == 1) lp(h = px(56), top = top) else lp(top = top))
        return e
    }

    private fun colors(kind: Kind): Pair<Int, Int> = when (kind) {
        Kind.PRIMARY -> BLUE to Color.WHITE
        Kind.NORMAL -> FIELD to INK
        Kind.DANGER -> Color.parseColor("#FDECEC") to RED
        Kind.DANGER_FILL -> Color.parseColor("#D93025") to Color.WHITE
    }

    fun show() {
        val maxBody = (activity.resources.displayMetrics.heightPixels * 0.55f).toInt()
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(16), px(10), px(16), px(28))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadii = floatArrayOf(28 * dp, 28 * dp, 28 * dp, 28 * dp, 0f, 0f, 0f, 0f)
            }
        }
        root.addView(View(activity).apply { background = box(LINE, 2) },
            LinearLayout.LayoutParams(px(40), px(4)).apply { gravity = Gravity.CENTER_HORIZONTAL })
        root.addView(TextView(activity).apply {
            text = title; textSize = 20f; setTextColor(INK); setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL; setPadding(px(4), 0, px(4), 0)
        }, lp(h = px(44), top = 10))
        if (message.isNotEmpty()) root.addView(TextView(activity).apply {
            text = message; textSize = 14f; setTextColor(SUB); setLineSpacing(0f, 1.25f); setPadding(px(4), 0, px(4), 0)
        }, lp())
        if (content.childCount > 0) root.addView(MaxHeightScrollView(activity).apply {
            maxHeightPx = maxBody
            isVerticalScrollBarEnabled = false
            addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }, lp())

        if (actionList.isNotEmpty()) root.addView(LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            actionList.forEachIndexed { i, a ->
                val (bg, fg) = colors(a.kind)
                addView(TextView(activity).apply {
                    text = a.label; textSize = 16f; setTextColor(fg); setTypeface(typeface, Typeface.BOLD); gravity = Gravity.CENTER
                    background = box(bg, 16)
                    setOnClickListener { if (a.onClick()) dialog.dismiss() }
                }, LinearLayout.LayoutParams(0, px(52), 1f).apply { if (i > 0) leftMargin = px(8) })
            }
        }, lp(top = 16))

        dialog.setContentView(root)
        if (!cancelable) { dialog.setCancelable(false); dialog.setCanceledOnTouchOutside(false) }
        // 자판이 올라와도 입력 칸과 버튼이 가려지지 않게 창을 자판 위로 올린다
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.setOnShowListener {
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
                sheet.setBackgroundColor(Color.TRANSPARENT)
                BottomSheetBehavior.from(sheet).apply { state = BottomSheetBehavior.STATE_EXPANDED; skipCollapsed = true; isHideable = cancelable }
            }
        }
        dialog.show()
    }
}
