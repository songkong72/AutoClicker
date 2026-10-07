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

/**
 * 앱 첫 화면의 방 선택 창(아래에서 올라오는 창). 방을 눌러 들어가고, 관리자는 "편집"에서 방을 지운다.
 * 삭제는 같은 창 안에서 한 번 더 묻는다. 무엇을 보여 줄지는 [RoomChooser]가 정하고, 여기서는 그리기만 한다.
 */
internal class RoomSheet(
    private val activity: Activity,
    private var rows: List<Row>,
    private val current: String,
    /** 서버 목록을 받지 못했을 때 등 제목 아래에 보일 안내. 없으면 null. */
    private val note: String?,
    private val showNew: Boolean,
    private val showType: Boolean,
    private val onPick: (String) -> Unit,
    private val onNew: () -> Unit,
    private val onType: () -> Unit,
    /** 방을 지운다. 끝나면 성공 여부로 콜백을 부른다(메인 스레드). */
    private val onDelete: (String, (Boolean) -> Unit) -> Unit
) {
    /** [badge]는 "배정 2/3" 같은 한마디(모르면 null), [deletable]이면 편집에서 휴지통이 나온다. */
    data class Row(val code: String, val badge: String?, val deletable: Boolean)

    private val dp = activity.resources.displayMetrics.density
    private fun px(v: Int) = (v * dp).toInt()
    private val ink = Color.parseColor("#16181D")
    private val sub = Color.parseColor("#5B6270")
    private val blue = Color.parseColor("#1F3FA6")
    private val red = Color.parseColor("#B3261E")

    private var editing = false
    private var pendingDelete: String? = null
    private var deleting = false
    private var dialog: BottomSheetDialog? = null
    private val root = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(px(16), px(10), px(16), px(28))
        background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadii = floatArrayOf(28 * dp, 28 * dp, 28 * dp, 28 * dp, 0f, 0f, 0f, 0f)
        }
    }

    fun show() {
        val d = BottomSheetDialog(activity)
        dialog = d
        render()
        d.setContentView(root)
        d.setOnShowListener {
            // 창의 기본 바탕을 지워 둥근 모서리가 보이게 하고, 처음부터 다 펼친다
            d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
                sheet.setBackgroundColor(Color.TRANSPARENT)
                BottomSheetBehavior.from(sheet).apply { state = BottomSheetBehavior.STATE_EXPANDED; skipCollapsed = true }
            }
        }
        d.show()
    }

    private fun box(color: Int, radius: Int, strokeColor: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = radius * dp
        if (strokeColor != null) setStroke((1.5f * dp).toInt(), strokeColor)
    }

    private fun label(text: String, sizeSp: Float, color: Int, bold: Boolean = false) = TextView(activity).apply {
        this.text = text; textSize = sizeSp; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun icon(res: Int, color: Int, sizeDp: Int) = ImageView(activity).apply {
        setImageResource(res); setColorFilter(color)
        layoutParams = LinearLayout.LayoutParams(px(sizeDp), px(sizeDp))
    }

    private fun render() {
        root.removeAllViews()
        val anyDeletable = rows.any { RoomChooser.canDelete(it.deletable, it.code, current) }
        if (!anyDeletable) { editing = false; pendingDelete = null }

        // 손잡이
        root.addView(View(activity).apply { background = box(Color.parseColor("#C9CDD6"), 2) },
            LinearLayout.LayoutParams(px(40), px(4)).apply { gravity = Gravity.CENTER_HORIZONTAL })

        // 제목 줄: "방 선택 5개" + 편집/완료
        val head = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(px(4), 0, px(4), 0) }
        head.addView(label(if (editing) "방 편집" else "방 선택", 20f, ink, bold = true))
        head.addView(label("${rows.size}개", 14f, sub), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = px(8) })
        if (anyDeletable) {
            head.addView(label(if (editing) "완료" else "편집", 15f, blue, bold = true).apply {
                gravity = Gravity.CENTER; setPadding(px(12), 0, px(12), 0)
                setOnClickListener { if (!deleting) { editing = !editing; pendingDelete = null; render() } }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(44)))
        }
        root.addView(head, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(44)).apply { topMargin = px(10) })

        if (!note.isNullOrEmpty()) root.addView(label(note, 13f, sub).apply { setPadding(px(4), 0, px(4), 0) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(4) })

        // 방 목록
        val list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        if (rows.isEmpty()) list.addView(label("들어갈 수 있는 방이 없어요. 관리자가 방을 만들면 여기에 보여요.", 15f, sub).apply { setPadding(px(4), px(12), px(4), px(12)) })
        rows.forEach { r -> list.addView(if (editing) editRow(r) else pickRow(r), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(if (r.code == current) 60 else 56)).apply { topMargin = px(4) }) }
        root.addView(MaxHeightScrollView(activity).apply {
            maxHeightPx = (activity.resources.displayMetrics.heightPixels * 0.45f).toInt()
            isVerticalScrollBarEnabled = false
            addView(list)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(10) })

        if (editing) {
            pendingDelete?.let { code -> root.addView(confirmBox(code), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(14) }) }
            return
        }
        if (showNew || showType) root.addView(View(activity).apply { setBackgroundColor(Color.parseColor("#E4E6EB")) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(1)).apply { topMargin = px(14) })
        if (showNew) root.addView(LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            background = box(Color.WHITE, 16, blue)
            addView(icon(R.drawable.ic_add, blue, 18))
            addView(label("새 방 만들기", 16f, blue, bold = true).apply { setPadding(px(8), 0, 0, 0) })
            setOnClickListener { dialog?.dismiss(); onNew() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(52)).apply { topMargin = px(14) })
        if (showType) root.addView(label("번호 직접 입력", 15f, blue, bold = true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { dialog?.dismiss(); onType() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(48)).apply { topMargin = px(6) })
    }

    /** 고르는 줄: 현재 방은 파란 바탕에 ✓와 "현재 방", 나머지는 번호와 배정 수. */
    private fun pickRow(r: Row): View {
        val isCurrent = r.code == current
        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(px(14), 0, px(14), 0)
            if (isCurrent) background = box(Color.parseColor("#E9EEFB"), 16)
            // 번호 앞 칸: 현재 방은 ✓, 나머지는 같은 폭의 빈칸(번호가 한 줄로 맞게)
            addView(if (isCurrent) icon(R.drawable.ic_check, blue, 20) else View(activity).apply { layoutParams = LinearLayout.LayoutParams(px(20), px(20)) })
            addView(LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                addView(label(r.code, 18f, ink, bold = isCurrent).apply { letterSpacing = 0.04f })
                if (isCurrent) addView(label("현재 방", 12f, blue, bold = true))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = px(10) })
            if (r.badge != null) addView(label(r.badge, 13f, if (isCurrent) Color.WHITE else sub, bold = isCurrent).apply {
                gravity = Gravity.CENTER; setPadding(px(12), 0, px(12), 0)
                background = box(if (isCurrent) blue else Color.parseColor("#F2F3F5"), 14)
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(28)))
            setOnClickListener { dialog?.dismiss(); onPick(r.code) }
        }
    }

    /** 편집 줄: 현재 방은 잠금, 나머지는 휴지통. 지우려고 고른 줄은 붉은 바탕. */
    private fun editRow(r: Row): View {
        val isCurrent = r.code == current
        val picked = r.code == pendingDelete
        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(px(14), 0, px(if (isCurrent) 14 else 6), 0)
            if (isCurrent) background = box(Color.parseColor("#E9EEFB"), 16) else if (picked) background = box(Color.parseColor("#FCEEEC"), 16)
            addView(LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                addView(label(r.code, 18f, ink, bold = isCurrent || picked).apply { letterSpacing = 0.04f })
                if (isCurrent) addView(label("현재 방", 12f, blue, bold = true))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            if (isCurrent) {
                addView(icon(R.drawable.ic_lock, sub, 16))
                addView(label("사용 중 · 삭제 불가", 13f, sub).apply { setPadding(px(6), 0, 0, 0) })
            } else if (RoomChooser.canDelete(r.deletable, r.code, current)) {
                addView(ImageView(activity).apply {
                    setImageResource(R.drawable.ic_delete); setColorFilter(if (picked) Color.WHITE else red)
                    setPadding(px(12), px(12), px(12), px(12))
                    contentDescription = "방 ${r.code} 삭제"
                    if (picked) background = box(red, 12)
                    setOnClickListener { if (!deleting) { pendingDelete = if (picked) null else r.code; render() } }
                }, LinearLayout.LayoutParams(px(44), px(44)))
            }
        }
    }

    /** 같은 창 안의 삭제 확인: "방 2222를 삭제할까요?" + 취소 / 삭제. */
    private fun confirmBox(code: String): View = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(px(16), px(18), px(16), px(16))
        background = box(Color.parseColor("#F2F3F5"), 20)
        addView(label("방 ${code}를 삭제할까요?", 17f, ink, bold = true))
        addView(label(if (deleting) "지우는 중…" else "방과 방 명단이 서버에서 지워져요. 되돌릴 수 없습니다.", 14f, sub).apply { setPadding(0, px(4), 0, 0) })
        addView(LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(label("취소", 16f, ink, bold = true).apply {
                gravity = Gravity.CENTER; background = box(Color.WHITE, 14, Color.parseColor("#C9CDD6"))
                setOnClickListener { if (!deleting) { pendingDelete = null; render() } }
            }, LinearLayout.LayoutParams(0, px(50), 1f))
            addView(label("삭제", 16f, Color.WHITE, bold = true).apply {
                gravity = Gravity.CENTER; background = box(red, 14)
                alpha = if (deleting) 0.5f else 1f
                setOnClickListener {
                    if (deleting) return@setOnClickListener
                    deleting = true; render()
                    onDelete(code) { ok ->
                        deleting = false
                        if (ok) { rows = rows.filter { it.code != code }; pendingDelete = null }
                        if (dialog?.isShowing == true) render()
                    }
                }
            }, LinearLayout.LayoutParams(0, px(50), 1f).apply { leftMargin = px(8) })
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(14) })
    }
}
