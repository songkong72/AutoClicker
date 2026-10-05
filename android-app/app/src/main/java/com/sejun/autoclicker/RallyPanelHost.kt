package com.sejun.autoclicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast

/**
 * 집결 패널을 오버레이 창으로 띄우고(드래그), 1초마다 RallyScreenModel로 다시 그린다.
 *
 * 주의: 방 상태(팀/행군시간)는 아직 Firebase와 연결 전이라 [stateSource]가 주는 값을 쓴다.
 * 2c단계에서 stateSource를 실제 방 데이터로 교체한다.
 */
class RallyPanelHost(
    private val context: Context,
    private val wm: WindowManager,
    private val stateSource: StateSource,
    private val onSecretUnlock: () -> Unit = {},
    /** 패널에서 다른 방 번호를 입력했을 때. 새 방으로 옮기는 일은 서비스가 맡는다. */
    private val onSwitchRoom: (String) -> Unit = {}
) {
    private val secretTap = RallySecretTap()
    interface StateSource {
        /** elapsedSec 등 현재 시각이 반영된 방 상태 */
        fun current(): RallyRoomState
        val isAdmin: Boolean
        fun onStart()
        fun onStop()
        fun onMarchDelta(teamId: String, deltaSec: Double) {}
        fun onToggleExclude(teamId: String) {}
        /** 이 기기의 캐릭터명. 등록 전이면 빈 문자열. */
        fun characterName(): String = ""
        /** 지금 들어와 있는 방 번호 */
        fun roomCode(): String = ""
        /** 관리자 화면에 보일 "마지막 변경: 누구 · 언제" 한 줄. 없으면 빈 문자열. */
        fun changeNote(): String = ""
        fun onSetCharacterName(name: String) {}
        /** 관리자가 고를 수 있는 방 명단. 가져오지 못하면 null. */
        fun loadRoster(onLoaded: (List<RallyMember>?) -> Unit) { onLoaded(emptyList()) }
        /** 마지막 명단 불러오기가 실패한 이유(화면에 보여 주는 용도). */
        fun rosterError(): String = ""
        /** 개발자 전용: 서버에 만들어진 모든 방(번호, 요약 한 줄). 읽지 못하면 null과 이유. */
        fun loadAllRooms(onLoaded: (List<Pair<String, String>>?, String) -> Unit) { onLoaded(null, "") }
        fun onAssignLeader(teamId: String, memberId: String, characterName: String) {}
        fun onUnassignLeader(teamId: String) {}
        fun onSetMarch(teamId: String, sec: Double) {}
        fun onAddTeam() {}
        fun onSetPrep(sec: Double) {}
        fun onSetWait(sec: Double) {}
        /** 관리자가 한 군단에 더해 주는 클릭 보정(ms) */
        fun onSetAdminAdjust(teamId: String, ms: Int) {}
        fun onRemoveTeam(teamId: String) {}
        /** 내 기기 설정(좌표, ms 보정). 방 데이터가 아니라 이 기기에만 저장된다. */
        fun deviceCorrectionMs(): Int = 0
        fun onCorrectionDelta(deltaMs: Int) {}
        fun onSetCorrectionMs(ms: Int) {}
        fun devicePositionText(): String = ""
        /** 평소엔 접혀 있고 "자세히"를 누르면 보이는 진단 줄 */
        fun deviceDetailText(): String = ""
        /** "도착 예정 12:34:12" / "12:34:12 도착". 진행한 집결이 없으면 빈 문자열. */
        fun arrivalNote(): String = ""
        fun onSavePosition() {}
        /** "✓ 클릭함 12:34:56.789". 이번 집결에서 클릭하지 않았으면 빈 문자열. */
        fun clickNote(): String = ""
        fun connection(): RallyConnection = RallyConnection.LIVE
    }

    private val handler = Handler(Looper.getMainLooper())
    private var panel: RallyPanelView? = null
    private var params: WindowManager.LayoutParams? = null
    private var minimized = false
    private val closeGuard = CloseGuard()
    private val input = RallyInputPopup(context, wm)
    private val pick = RallyPickPopup(context, wm)

    val isShowing: Boolean get() = panel != null

    private val tick = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 250L) // 표시는 초 단위, 경계에서 밀리지 않게 짧게 갱신
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    /** 설정의 오버레이 투명도를 패널에 반영한다. */
    fun applyAlpha(a: Float) { panel?.root?.alpha = a; input.applyAlpha(a); pick.applyAlpha(a) }

    fun show() {
        if (panel != null) return
        val view = RallyPanelView(context, object : RallyPanelView.Callbacks {
            override fun onStart() { stateSource.onStart(); refresh() }
            override fun onStop() { stateSource.onStop(); refresh() }
            override fun onMinimize() { toggleMinimize() }
            override fun onClose() {
                // 집결 진행 중에는 ✕를 한 번 더 눌러야 패널이 꺼진다(카운트다운이 갑자기 사라지는 실수 방지)
                val running = stateSource.current().runState == RallyRunState.RUNNING
                if (closeGuard.onTap(running, SystemClock.elapsedRealtime())) hide()
                else Toast.makeText(context, "집결이 진행 중이에요. 패널을 끄려면 ✕를 한 번 더 누르세요", Toast.LENGTH_SHORT).show()
            }
            override fun onTitleTap() { if (secretTap.tap(System.currentTimeMillis())) onSecretUnlock() } // 제목 5번 연타: 숨은 기능
            override fun onMarchDelta(teamId: String, deltaSec: Double) { stateSource.onMarchDelta(teamId, deltaSec); refresh() }
            override fun onToggleExclude(teamId: String) { stateSource.onToggleExclude(teamId); refresh() }
            override fun onAssignLeader(teamId: String, teamName: String) { showAssignPicker(teamId, teamName) }
            override fun onEditCharacterName(current: String) { promptCharacterName(current) }
            override fun onEditRoom(current: String) { showRoomPicker(current) }
            override fun onEditMarch(teamId: String, currentSec: Double) {
                val shown = if (currentSec % 1.0 == 0.0) currentSec.toInt().toString() else currentSec.toString()
                input.show("행군시간(초)", shown) { text ->
                    val sec = RallyInputParse.marchSeconds(text) ?: return@show false
                    stateSource.onSetMarch(teamId, sec); refresh(); true
                }
            }
            override fun onAddTeam() { stateSource.onAddTeam(); refresh() }
            override fun onEditPrep(currentSec: Double) {
                input.show("준비 시간(초)", currentSec.toInt().toString()) { text ->
                    val sec = RallyInputParse.marchSeconds(text) ?: return@show false
                    stateSource.onSetPrep(sec); refresh(); true
                }
            }
            override fun onSetWait(sec: Double) { stateSource.onSetWait(sec); refresh() }
            override fun onEditAdminAdjust(teamId: String, teamName: String, currentMs: Int) {
                val shown = if (currentMs % 1000 == 0) (currentMs / 1000).toString() else (currentMs / 1000.0).toString()
                input.show("$teamName 보정(초) · −는 더 일찍, +는 더 늦게", shown, signed = true,
                    errorText = "−5 ~ +5 사이 숫자를 입력해 주세요 (예: -1.5)") { text ->
                    val ms = RallyInputParse.correctionMs(text) ?: return@show false
                    stateSource.onSetAdminAdjust(teamId, ms); refresh(); true
                }
            }
            override fun onRemoveTeam(teamId: String) { stateSource.onRemoveTeam(teamId); refresh() }
            override fun onCorrectionDelta(deltaMs: Int) { stateSource.onCorrectionDelta(deltaMs); refresh() }
            override fun onSavePosition() { stateSource.onSavePosition(); refresh() }
            override fun onEditCorrection(currentMs: Int) {
                val shown = if (currentMs % 1000 == 0) (currentMs / 1000).toString() else (currentMs / 1000.0).toString()
                input.show("클릭 보정(초) · −는 더 일찍, +는 더 늦게", shown, signed = true,
                    errorText = "−5 ~ +5 사이 숫자를 입력해 주세요 (예: -1.5)") { text ->
                    val ms = RallyInputParse.correctionMs(text) ?: return@show false
                    stateSource.onSetCorrectionMs(ms); refresh(); true
                }
            }
        })
        val lp = WindowManager.LayoutParams(
            panelWidthPx(false),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 24; y = 120 }
        (view.root as RallyDragLayout).let { drag ->
            var sx = 0; var sy = 0
            drag.onDragStart = { sx = lp.x; sy = lp.y }
            drag.onDragMove = { dx, dy ->
                lp.x = sx + dx.toInt(); lp.y = sy + dy.toInt()
                try { wm.updateViewLayout(view.root, lp) } catch (_: Exception) { }
            }
        }
        wm.addView(view.root, lp)
        panel = view
        view.root.alpha = PreferencesHelper.getOverlayAlpha(context)
        params = lp
        refresh()
        handler.post(tick)
    }

    /** 방 번호를 직접 입력해 옮긴다. 이미 있는 방으로만 옮겨지고, 없는 번호를 쳐도 방이 새로 만들어지지는 않는다. */
    private fun promptRoomNumber(current: String) {
        input.show("방 번호 (4~20자, 영문·숫자)", "", freeText = true,
            errorText = "방 번호는 영문·숫자 4~20자로 입력해 주세요") { text ->
            val code = RallyRoomCode.normalize(text) ?: return@show false
            if (code != current) handler.post { onSwitchRoom(code) } // 이 창과 패널이 닫힌 뒤에 옮긴다
            true
        }
    }

    /** 들어갔던 방 목록에서 골라 옮긴다. 방 만들기는 앱 첫 화면(첫 입장)에서만 한다. */
    private fun showRoomPicker(current: String) {
        val prefs = context.getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE)
        if (current.isNotEmpty()) RallyRoomHistory.record(prefs, current)
        val items = RallyRoomHistory.load(prefs).map { code ->
            RallyPickPopup.Item(if (code == current) "$code  ·  현재 방" else code, if (code == current) "#60A5FA" else "#E2E8F0") {
                if (code != current) handler.post { onSwitchRoom(code) }
            }
        }
        pick.show("방 선택", items,
            "들어갔던 방이 아직 없어요. 방 번호를 직접 입력하거나, 새 방은 앱 첫 화면에서 만드세요",
            listOf(
                RallyPickPopup.Item("서버의 전체 방 보기 (개발자)") { showAllRooms(current) },
                RallyPickPopup.Item("방 번호 직접 입력") { promptRoomNumber(current) },
                RallyPickPopup.Item("닫기") { }
            ))
    }

    /** 개발자: 서버에 만들어진 모든 방을 목록으로 보고 골라 옮긴다. 관리자와 팀장은 서버 규칙상 읽을 수 없다. */
    private fun showAllRooms(current: String) {
        val back = RallyPickPopup.Item("뒤로") { showRoomPicker(current) }
        pick.show("서버의 전체 방", emptyList(), "방 목록을 불러오는 중…", listOf(back))
        stateSource.loadAllRooms { rooms, error ->
            if (panel == null) return@loadAllRooms
            if (rooms == null) {
                pick.show("서버의 전체 방", emptyList(),
                    "전체 방은 개발자만 볼 수 있어요. 들어갔던 방 목록이나 방 번호 직접 입력을 쓰세요" + if (error.isNotEmpty()) "\n($error)" else "",
                    listOf(back))
                return@loadAllRooms
            }
            val items = rooms.map { (code, line) ->
                RallyPickPopup.Item(if (code == current) "$line  ·  현재 방" else line, if (code == current) "#60A5FA" else "#E2E8F0") {
                    if (code != current) handler.post { onSwitchRoom(code) }
                }
            }
            pick.show("서버의 전체 방 (${rooms.size}개)", items, "만들어진 방이 없어요", listOf(back))
        }
    }

    private fun promptCharacterName(current: String) {
        input.show("게임 캐릭터명 · 관리자가 이 이름을 보고 군단을 배정해요", current, freeText = true,
            errorText = "캐릭터명을 입력해 주세요 (최대 ${RallyRoster.MAX_NAME}자)") { text ->
            val name = RallyRoster.cleanName(text)
            if (name.isEmpty()) return@show false
            stateSource.onSetCharacterName(name); refresh(); true
        }
    }

    /** 관리자: 방에 등록한 사람 목록에서 이 군단을 맡을 사람을 고른다. 이미 다른 군단에 있는 사람을 고르면 그쪽에서 빠진다. */
    private fun showAssignPicker(teamId: String, teamName: String) {
        // 누르자마자 반응이 보이도록 먼저 "불러오는 중" 창을 띄우고, 결과가 오면 바꿔 그린다.
        pick.show("$teamName 을(를) 맡을 사람", emptyList(), "명단을 불러오는 중…", listOf(RallyPickPopup.Item("닫기") { }))
        stateSource.loadRoster { roster ->
            if (panel == null) return@loadRoster
            if (roster == null) {
                // 토스트는 기기에 따라 안 보이므로 목록 창 안에 이유를 적는다.
                pick.show("$teamName 을(를) 맡을 사람", emptyList(),
                    "명단을 불러오지 못했어요. 인터넷 연결과 Firebase 규칙 게시(rallyMembers) 여부를 확인해 주세요\n(${stateSource.rosterError()})",
                    listOf(RallyPickPopup.Item("닫기") { }))
                return@loadRoster
            }
            val teams = stateSource.current().teams
            val assignedTo = teams.filter { it.leaderId.isNotEmpty() }.associate { it.leaderId to it.name }
            val dupNames = roster.groupingBy { it.name }.eachCount().filterValues { it > 1 }.keys
            val items = roster.map { m ->
                // 같은 이름이 둘이면(앱을 다시 설치한 경우) 기기 ID 끝 4자리로 구별한다. 팀장 화면의 "내 기기"에 같은 값이 보인다.
                val label = m.name + (if (m.name in dupNames) " (…${m.id.takeLast(4)})" else "")
                RallyPickPopup.Item(label + (assignedTo[m.id]?.let { "  ·  $it" } ?: "")) {
                    stateSource.onAssignLeader(teamId, m.id, m.name); refresh()
                }
            }
            val footer = mutableListOf<RallyPickPopup.Item>()
            if (teams.firstOrNull { it.id == teamId }?.leaderId?.isNotEmpty() == true) {
                footer += RallyPickPopup.Item("배정 해제", "#F87171") { stateSource.onUnassignLeader(teamId); refresh() }
            }
            footer += RallyPickPopup.Item("닫기") { }
            pick.show("$teamName 을(를) 맡을 사람", items,
                "아직 등록한 사람이 없어요. 팀장이 앱에서 캐릭터명을 등록하면 여기에 나타나요", footer)
        }
    }

    fun hide() {
        input.dismiss()
        pick.dismiss()
        handler.removeCallbacks(tick)
        panel?.root?.let { v ->
            try { wm.removeView(v) } catch (_: Exception) { }
        }
        panel = null
        params = null
        minimized = false
        lastRun = null
    }

    fun toggle() { if (isShowing) hide() else show() }

    /** 창 폭은 WindowManager가 정한다(레이아웃의 layout_width는 무시됨). 화면이 좁으면 비율로 줄인다. */
    private fun panelWidthPx(compact: Boolean): Int {
        val dm = context.resources.displayMetrics
        val dp = dm.density
        val want = if (compact) 204 * dp else 276 * dp
        return Math.min(want, dm.widthPixels * (if (compact) 0.62f else 0.72f)).toInt()
    }

    private fun applyWidth() {
        val v = panel?.root ?: return
        val lp = params ?: return
        lp.width = if (minimized) WindowManager.LayoutParams.WRAP_CONTENT else panelWidthPx(false) // 알약은 글자 크기에 맞춰 늘어나 잘리지 않는다
        try { wm.updateViewLayout(v, lp) } catch (_: Exception) { }
    }

    private var lastRun: RallyRunState? = null
    private val cue = RallyCountdownCue()

    private fun refresh() {
        val p = panel ?: return
        val state = stateSource.current()
        // 집결이 시작되면 카운트다운만 보이는 알약으로 자동 접고, 끝나거나 취소되면 다시 펼친다.
        if (state.runState != lastRun) {
            val prev = lastRun
            lastRun = state.runState
            if (state.runState == RallyRunState.RUNNING && !minimized) { minimized = true; applyWidth() }
            else if (prev == RallyRunState.RUNNING && state.runState != RallyRunState.RUNNING && minimized) {
                minimized = false; p.setMinimized(false, RallyScreenModel.build(state).hero); applyWidth()
            }
        }
        val model = RallyScreenModel.build(state)
        p.renderRoom(stateSource.roomCode())
        p.renderDevice(stateSource.deviceCorrectionMs(), stateSource.devicePositionText(), stateSource.characterName(), stateSource.deviceDetailText())
        // 내 클릭을 기다리는 단계에서만: 마지막 5초는 숫자를 붉게, 1초마다 진동(0초 직전은 더 강하게)
        val waiting = model.hero.kind == HeroKind.WAIT_CLICK || model.hero.kind == HeroKind.MOVE
        val remain = model.hero.remainingSec
        cue.onCountdown(waiting, remain ?: 99.0)?.let { n ->
            p.root.performHapticFeedback(if (n == 1) android.view.HapticFeedbackConstants.LONG_PRESS else android.view.HapticFeedbackConstants.CLOCK_TICK)
        }
        val note = listOf(stateSource.arrivalNote(), stateSource.clickNote(), stateSource.changeNote()).filter { it.isNotEmpty() }.joinToString(" · ")
        p.render(model, stateSource.isAdmin, state.runState == RallyRunState.RUNNING, note,
            stateSource.connection(), RallyCountdownCue.urgent(waiting, remain))
        if (minimized) p.setMinimized(true, model.hero)
    }

    private fun toggleMinimize() {
        minimized = !minimized
        panel?.setMinimized(minimized, RallyScreenModel.build(stateSource.current()).hero)
        applyWidth()
        refresh()
    }
}
