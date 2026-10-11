package com.sejun.autoclicker

import android.content.Context

/**
 * 화면에 보이는 글자를 한곳에 모은다(코드 안에서 만드는 글자용. 레이아웃의 고정 글자는 `res/values/strings.xml`과 `res/values-en/strings.xml`, `values-zh`(간체), `values-b+zh+Hant`(번체)).
 * 계산하는 쪽([RallyScreenModel] 등)은 안드로이드 없이 테스트하므로 리소스 대신 이 표를 쓴다.
 * 언어를 더하려면 [Str]를 구현한 표를 하나 더 만들고 [T.forLanguage]에 잇는다.
 * 중국어 번체 표([StrZhTw], [AppZhTw], [MoreZhTw])는 간체 표를 변환해 만든 것이다. 간체를 고치면 번체도 같은 뜻으로 고친다.
 * 기본은 한국어다: 번역이 없는 언어의 폰은 한국어로 보인다.
 */
internal object T {
    @Volatile var s: Str = StrKo
        private set

    /**
     * 언어(와 중국어일 때 글자 종류)에 맞는 표.
     * 중국어는 번체(글자 종류 Hant, 또는 지역이 대만·홍콩·마카오)와 간체(그 밖의 중국어)를 따로 둔다.
     */
    fun forLanguage(lang: String?, script: String? = null, region: String? = null): Str = when (lang) {
        "en" -> StrEn
        "zh" -> if (isTraditional(script, region)) StrZhTw else StrZh
        else -> StrKo
    }

    /** 중국어 번체인가. 글자 종류가 적혀 있으면 그것을 따르고, 없으면 지역으로 본다. */
    fun isTraditional(script: String?, region: String?): Boolean = when {
        script.equals("Hant", ignoreCase = true) -> true
        script.equals("Hans", ignoreCase = true) -> false
        else -> region?.uppercase() in setOf("TW", "HK", "MO")
    }

    /** 폰(또는 이 앱)의 언어에 맞춘다. 화면을 그리기 직전에 부른다. */
    fun use(context: Context) {
        val l = context.resources.configuration.locales[0]
        val want = forLanguage(l?.language, l?.script, l?.country)
        if (want !== s) s = want
    }

    /** 앱 화면(첫 화면·창·게임 위 막대)의 글자 표. [s]와 같은 언어를 따른다. */
    val a: AppStr get() = when (s) { StrEn -> AppEn; StrZh -> AppZh; StrZhTw -> AppZhTw; else -> AppKo }

    /** 지휘관·대표 창, 보내는 글, 숨은 모드의 글자 표. */
    val x: MoreStr get() = when (s) { StrEn -> MoreEn; StrZh -> MoreZh; StrZhTw -> MoreZhTw; else -> MoreKo }

    /** 테스트용: 언어를 직접 고른다. */
    fun set(str: Str) { s = str }
}

internal interface Str {
    // ── 단위와 이름 ─────────────────────────────
    /** "10초" */
    fun sec(text: String): String
    /** 입력에서 떼어 낼 초 단위 글자들 */
    val secSuffixes: List<String>
    /** "40초", "1분", "1분 10초" */
    fun span(min: Int, sec: Int): String
    /** 서버에 저장된 군단 이름("1군")을 이 언어로 */
    fun teamName(raw: String): String
    /** "2조" */
    fun wave(n: Int): String
    val waveWord: String
    /** "군단 3개" */
    fun teamCount(n: Int): String
    val teams: String
    fun teamsOf(n: Int, max: Int): String
    val commander: String
    val leader: String
    val me: String
    val myTeamDesc: String
    val none: String
    val roomWord: String
    fun room(code: String): String
    val charName: String

    // ── 단계 ─────────────────────────────
    val phaseNames: List<String>
    val beforeClick: String
    val gathering: String
    val marching: String
    val statusMarch: String
    val statusArrived: String
    val statusExcluded: String
    val prepMove: String
    val waitClick: String

    // ── 위 칸(큰 숫자와 안내) ─────────────────────────────
    val noTeamsJoining: String
    fun untilWaveArrives(wave: Int): String
    fun wavePlus(wave: Int, span: String): String
    val cancelled: String
    val cancelledSub: String
    val allArrived: String
    val arrivedSub: String
    val roomEmptyAdmin: String
    val roomEmptyAdminSub: String
    val roomEmptyLeader: String
    val roomEmptyLeaderSub: String
    val notJoining: String
    val notAssigned: String
    val notAssignedSub: String
    val adminOut: String
    val adminOutSub: String
    val excluded: String
    val excludedSub: String
    val waitingStart: String
    fun myClickAt(sec: Int): String
    val untilFirstClick: String
    val untilAllDepart: String
    val untilAllArrive: String
    /** "2조 행군 중" · phase 0 클릭 전, 1 집결 중, 2 행군 중 */
    fun waveStage(wave: Int, phase: Int): String
    fun nextWave(wave: Int, mmss: String): String
    val goToRally: String
    val untilMyClick: String
    val waitingClick: String
    fun untilDepartThenMarch(mmss: String): String
    val untilCastle: String
    fun allArriveIn(mmss: String): String
    val myArrived: String
    val warnName: String
    val warnPosition: String
    fun idleSub(sub: String, activeTeams: Int): String
    fun offline(who: String): String
    fun eta(clock: String): String
    fun arrivedAt(clock: String): String
    fun clicked(time: String): String
    fun commanderFallback(last4: String): String
    fun lastChange(who: String, ago: String): String
    val justNow: String
    fun minAgo(n: Long): String
    fun hourAgo(n: Long): String
    fun dayAgo(n: Long): String

    // ── 군단 목록과 조 ─────────────────────────────
    val clickFirst: String
    fun secLater(text: String): String
    val arrivesFirst: String
    val arrivesTogether: String
    fun arrivesAfter(span: String): String
    fun base(label: String): String
    fun untilArrive(mmss: String): String
    fun afterStart(mmss: String): String
    val rowsHeadEdit: String
    val rowsHeadRun: String
    val unassigned: String
    val unassignedTapAssign: String
    fun unassignedNote(names: String): String
    val tapAgainToDelete: String
    fun deleteTeam(name: String): String
    val deleteQ: String
    val expand: String
    val collapse: String
    val splitWaves: String
    val addTeam: String
    fun maxTeams(n: Int): String

    // ── 방 설정, 내 기기, 버튼 ─────────────────────────────
    val setup: String
    val setupExpand: String
    val setupCollapse: String
    fun setupSummary(waitMin: String, prepSec: String): String
    val blockedHintEditing: String
    val blockedHintView: String
    val myDevice: String
    val posSaved: String
    val posNone: String
    val roomNone: String
    val nameNone: String
    fun correction(text: String): String
    val savedFlash: String
    val saveAgain: String
    val savePos: String
    val register: String
    val offsetStepNote: String
    val done: String
    val tapToExpand: String
    val minimize: String
    val starting: String
    val regroup: String
    val start: String
    val miniCancelled: String
    val miniAssignWait: String
    val miniBuilding: String
    val miniStartWait: String

    // ── 묻는 창 ─────────────────────────────
    val close: String
    val cancel: String
    val ok: String
    val move: String
    fun moveTo(room: String, action: String): String
    val pickRoomFirst: String
    val badgeCurrent: String
    val badgeRunning: String
    fun rangeError(max: Int): String
    fun rangeErrorEx(max: Int, example: String): String
    val offsetError: String
    fun unassignedTitle(names: String): String
    val unassignedBody: String
    val startWithoutUnassigned: String
    val startAnyway: String
    val closeGuardToast: String
    val askMarch: String
    val askPrep: String
    fun askWaveDelay(wave: Int): String
    val splitTitle: String
    val splitBody: String
    fun perWave(n: Int): String
    val allOneWave: String
    fun askTeamOffset(team: String): String
    val askMyOffset: String
    val noRoomsToast: String
    fun roomsLoading(n: Int): String
    val roomsLoadingNone: String
    fun roomsFailed(error: String): String
    fun roomsTitle(n: Int): String
    val askCharName: String
    fun charNameError(max: Int): String
    fun assignTitle(team: String): String
    val rosterLoading: String
    fun rosterFailed(error: String): String
    val unassign: String
    val rosterEmpty: String

    // ── 서버와 진단 ─────────────────────────────
    val rejected: String
    fun saveFailed(message: String): String
    val unknownError: String
    val wavesRejected: String
    fun startFailed(why: String): String
    fun cancelFailed(why: String): String
    fun adminOffsetNote(text: String): String
    val pollingNote: String
    fun deviceLine(last4: String, team: String): String
    val noneYet: String
    fun syncMode(live: Boolean): String
    val needLogin: String
    fun serverRefused(code: Int): String
    val checkInternet: String
    val nameEmpty: String
    fun lastDiag(actualSec: String, targetSec: String, offsetMs: Int, wall: String): String
}

internal object StrKo : Str {
    override fun sec(text: String) = "${text}초"
    override val secSuffixes = listOf("초", "s")
    override fun span(min: Int, sec: Int) = when { min == 0 -> "${sec}초"; sec == 0 -> "${min}분"; else -> "${min}분 ${sec}초" }
    override fun teamName(raw: String) = raw
    override fun wave(n: Int) = "${n}조"
    override val waveWord = "조"
    override fun teamCount(n: Int) = "군단 ${n}개"
    override val teams = "군단"
    override fun teamsOf(n: Int, max: Int) = "군단 $n/$max"
    override val commander = "지휘관"
    override val leader = "집결장"
    override val me = "나"
    override val myTeamDesc = "내 군단"
    override val none = "없음"
    override val roomWord = "방"
    override fun room(code: String) = "방 $code"
    override val charName = "캐릭터명"

    override val phaseNames = listOf("대기", "집결", "행군", "도착")
    override val beforeClick = "클릭 전"
    override val gathering = "집결 중"
    override val marching = "행군 중"
    override val statusMarch = "행군"
    override val statusArrived = "도착"
    override val statusExcluded = "제외"
    override val prepMove = "이동 준비"
    override val waitClick = "클릭 대기"

    override val noTeamsJoining = "참여 군단이 없어요"
    override fun untilWaveArrives(wave: Int) = "${wave}조 도착까지"
    override fun wavePlus(wave: Int, span: String) = "${wave}조 +$span"
    override val cancelled = "작전 취소됨"
    override val cancelledSub = "예약된 클릭이 모두 멈췄어요"
    override val allArrived = "전원 도착"
    override val arrivedSub = "실패했다면 바로 재집결하세요"
    override val roomEmptyAdmin = "이 방에는 군단이 없어요"
    override val roomEmptyAdminSub = "방 번호가 맞는지 확인하세요 · 새 방은 앱 첫 화면에서 만들어요"
    override val roomEmptyLeader = "지휘관이 군단을 구성하는 중이에요"
    override val roomEmptyLeaderSub = "방에 군단이 생기면 여기에 표시돼요"
    override val notJoining = "참여 안 함"
    override val notAssigned = "아직 군단이 배정되지 않았어요"
    override val notAssignedSub = "지휘관이 군단을 배정하면 시작할 수 있어요"
    override val adminOut = "이번 작전에는 참여하지 않아요"
    override val adminOutSub = "군단 목록에서 진행을 확인하세요"
    override val excluded = "이번 작전에서 제외됐어요"
    override val excludedSub = "지휘관이 다시 포함하면 참여할 수 있어요"
    override val waitingStart = "지휘관 시작 대기 중"
    override fun myClickAt(sec: Int) = "시작 후 ${sec}초에 내 집결 클릭"
    override val untilFirstClick = "첫 집결 클릭까지"
    override val untilAllDepart = "전원 출발까지"
    override val untilAllArrive = "전원 도착까지"
    override fun waveStage(wave: Int, phase: Int) = "${wave}조 " + when (phase) { 0 -> "클릭 전"; 1 -> "집결 중"; else -> "행군 중" }
    override fun nextWave(wave: Int, mmss: String) = " · 다음 ${wave}조 $mmss"
    override val goToRally = "집결 화면으로 이동하세요"
    override val untilMyClick = "집결 클릭까지"
    override val waitingClick = "클릭 대기 중"
    override fun untilDepartThenMarch(mmss: String) = "출발까지 · 이후 행군 $mmss"
    override val untilCastle = "성 도착까지"
    override fun allArriveIn(mmss: String) = "전원 도착까지 $mmss"
    override val myArrived = "내 군단 도착"
    override val warnName = "캐릭터명을 먼저 등록하세요 (눌러서 입력)"
    override val warnPosition = "클릭 위치를 먼저 저장하세요 (눌러서 열기)"
    override fun idleSub(sub: String, activeTeams: Int) = "$sub · 참여 군단 ${activeTeams}개"
    override fun offline(who: String) = "$who 연결 없음"
    override fun eta(clock: String) = "도착 예정 $clock"
    override fun arrivedAt(clock: String) = "$clock 도착"
    override fun clicked(time: String) = "✓ 클릭함 $time"
    override fun commanderFallback(last4: String) = "지휘관 …$last4"
    override fun lastChange(who: String, ago: String) = "마지막 변경: $who · $ago"
    override val justNow = "방금"
    override fun minAgo(n: Long) = "${n}분 전"
    override fun hourAgo(n: Long) = "${n}시간 전"
    override fun dayAgo(n: Long) = "${n}일 전"

    override val clickFirst = "먼저 클릭"
    override fun secLater(text: String) = "${text}초 뒤"
    override val arrivesFirst = "먼저 도착"
    override val arrivesTogether = "같이 도착"
    override fun arrivesAfter(span: String) = "$span 뒤 도착"
    override fun base(label: String) = "$label (기준)"
    override fun untilArrive(mmss: String) = "도착까지 $mmss"
    override fun afterStart(mmss: String) = "시작 후 $mmss"
    override val rowsHeadEdit = "행군 시간 · 위에서부터 차례로 클릭"
    override val rowsHeadRun = "행군 시간 · 단계가 끝날 때까지"
    override val unassigned = "미배정"
    override val unassignedTapAssign = "미배정 · 눌러서 배정"
    override fun unassignedNote(names: String) = "$names 미배정 · 맡은 사람이 없으면 클릭하지 않아요"
    override val tapAgainToDelete = "한 번 더 누르면 삭제"
    override fun deleteTeam(name: String) = "$name 삭제"
    override val deleteQ = "삭제?"
    override val expand = "펼치기"
    override val collapse = "접기"
    override val splitWaves = "조 나누기"
    override val addTeam = "+ 군단 추가"
    override fun maxTeams(n: Int) = "최대 ${n}개"

    override val setup = "방 설정"
    override val setupExpand = "방 설정 펼치기"
    override val setupCollapse = "방 설정 접기"
    override fun setupSummary(waitMin: String, prepSec: String) = "집결 대기 ${waitMin}분 · 이동 준비 ${prepSec}초"
    override val blockedHintEditing = "참여할 군단을 체크하세요"
    override val blockedHintView = "위의 연필을 눌러 군단을 체크하세요"
    override val myDevice = "내 기기"
    override val posSaved = "위치 저장됨"
    override val posNone = "위치 없음"
    override val roomNone = "방 없음"
    override val nameNone = "캐릭터명 없음"
    override fun correction(text: String) = "보정 $text"
    override val savedFlash = "✓ 저장됨"
    override val saveAgain = "다시 저장"
    override val savePos = "위치 저장"
    override val register = "등록하기"
    override val offsetStepNote = "보정 · 0.5초씩"
    override val done = "완료"
    override val tapToExpand = "탭해서 펼치기"
    override val minimize = "작게"
    override val starting = "시작하는 중…"
    override val regroup = "다시 집결"
    override val start = "집결 시작"
    override val miniCancelled = "작전 취소"
    override val miniAssignWait = "군단 배정 대기"
    override val miniBuilding = "군단 구성 중"
    override val miniStartWait = "시작 대기"

    override val close = "닫기"
    override val cancel = "취소"
    override val ok = "확인"
    override val move = "이동"
    override fun moveTo(room: String, action: String) = "$room 방으로 $action"
    override val pickRoomFirst = "옮길 방을 골라 주세요"
    override val badgeCurrent = "현재"
    override val badgeRunning = "집결 중"
    override fun rangeError(max: Int) = "0 ~ $max 사이 숫자를 입력해 주세요"
    override fun rangeErrorEx(max: Int, example: String) = "0 ~ $max 사이 숫자를 입력해 주세요 (예: $example)"
    override val offsetError = "−5 ~ +5 사이 숫자를 입력해 주세요 (예: -1.5)"
    override fun unassignedTitle(names: String) = "${names}이(가) 미배정이에요"
    override val unassignedBody = "맡은 사람이 없어 이 군단은 클릭하지 않아요. 어떻게 할까요?"
    override val startWithoutUnassigned = "미배정 제외하고 시작"
    override val startAnyway = "그대로 시작"
    override val closeGuardToast = "집결이 진행 중이에요. 패널을 끄려면 ✕를 한 번 더 누르세요"
    override val askMarch = "행군시간(초)"
    override val askPrep = "이동 준비 시간(초)"
    override fun askWaveDelay(wave: Int) = "${wave}조 · 1조보다 몇 초 뒤에 도착할까요"
    override val splitTitle = "조 나누기"
    override val splitBody = "군단 번호 순서대로 나눠요. 조마다 1조보다 몇 분 뒤에 도착할지 따로 정할 수 있어요."
    override fun perWave(n: Int) = "${n}개씩"
    override val allOneWave = "모두 1조로 (조 없애기)"
    override fun askTeamOffset(team: String) = "$team 보정(초) · −는 더 일찍, +는 더 늦게"
    override val askMyOffset = "클릭 보정(초) · −는 더 일찍, +는 더 늦게"
    override val noRoomsToast = "들어갔던 방이 없어요. 앱 첫 화면에서 방을 만들거나 입장해 주세요"
    override fun roomsLoading(n: Int) = "방 선택 (${n}개) · 불러오는 중…"
    override val roomsLoadingNone = "방 선택 · 불러오는 중…"
    override fun roomsFailed(error: String) = "방 선택 · 서버 방 목록을 불러오지 못했어요" + (if (error.isNotEmpty()) " ($error)" else "")
    override fun roomsTitle(n: Int) = "방 선택 (${n}개)"
    override val askCharName = "게임 캐릭터명 · 지휘관이 이 이름을 보고 군단을 배정해요"
    override fun charNameError(max: Int) = "캐릭터명을 입력해 주세요 (최대 ${max}자)"
    override fun assignTitle(team: String) = "$team 을(를) 맡을 사람"
    override val rosterLoading = "명단을 불러오는 중…"
    override fun rosterFailed(error: String) = "명단을 불러오지 못했어요. 인터넷 연결과 Firebase 규칙 게시(rallyMembers) 여부를 확인해 주세요\n($error)"
    override val unassign = "배정 해제"
    override val rosterEmpty = "아직 등록한 사람이 없어요. 집결장이 앱에서 캐릭터명을 등록하면 여기에 나타나요"

    override val rejected = "서버가 방 수정을 거절했어요. 이 기기가 지휘관 명단에 있는지, 서버 규칙이 맞는지 확인해 주세요"
    override fun saveFailed(message: String) = "서버에 저장하지 못했어요: $message"
    override val unknownError = "알 수 없는 오류"
    override val wavesRejected = "조를 저장하지 못했어요. 서버 규칙을 새로 게시해야 조를 쓸 수 있어요 (Firebase 콘솔 → 규칙)"
    override fun startFailed(why: String) = "집결을 시작하지 못했어요. 다시 눌러 주세요 · $why"
    override fun cancelFailed(why: String) = "취소를 서버에 전하지 못했어요. 다른 폰은 아직 진행 중일 수 있어요 · $why"
    override fun adminOffsetNote(text: String) = "지휘관 보정 $text (내 보정에 더해 적용)"
    override val pollingNote = "수신 방식: 1초 확인 (실시간보다 최대 1초 늦을 수 있음)"
    override fun deviceLine(last4: String, team: String) = "기기 ID …$last4 · 배정: $team"
    override val noneYet = "아직 없음"
    override fun syncMode(live: Boolean) = "수신 방식: " + (if (live) "실시간" else "1초 확인")
    override val needLogin = "로그인이 필요해요"
    override fun serverRefused(code: Int) = "서버가 거절했어요 ($code)"
    override val checkInternet = "인터넷 연결을 확인해 주세요"
    override val nameEmpty = "캐릭터명이 비어 있어요"
    override fun lastDiag(actualSec: String, targetSec: String, offsetMs: Int, wall: String) =
        "마지막 집결: 신호 수신 ${actualSec}초 뒤 클릭(목표 ${targetSec}초 ${offsetMs}ms 보정) · $wall"
}

internal object StrEn : Str {
    private val teamNo = Regex("""^(\d+)군$""")

    override fun sec(text: String) = "${text}s"
    override val secSuffixes = listOf("s", "초")
    override fun span(min: Int, sec: Int) = when { min == 0 -> "${sec}s"; sec == 0 -> "${min}m"; else -> "${min}m ${sec}s" }
    override fun teamName(raw: String) = teamNo.matchEntire(raw)?.let { "Team ${it.groupValues[1]}" } ?: raw
    override fun wave(n: Int) = "Wave $n"
    override val waveWord = "Wave"
    override fun teamCount(n: Int) = if (n == 1) "1 team" else "$n teams"
    override val teams = "Teams"
    override fun teamsOf(n: Int, max: Int) = "Teams $n/$max"
    override val commander = "Commander"
    override val leader = "Rally leader"
    override val me = "Me"
    override val myTeamDesc = "my team"
    override val none = "None"
    override val roomWord = "Room"
    override fun room(code: String) = "Room $code"
    override val charName = "Name"

    override val phaseNames = listOf("Wait", "Rally", "March", "Arrive")
    override val beforeClick = "Before tap"
    override val gathering = "Rallying"
    override val marching = "Marching"
    override val statusMarch = "March"
    override val statusArrived = "Arrived"
    override val statusExcluded = "Out"
    override val prepMove = "Get ready"
    override val waitClick = "Waiting to tap"

    override val noTeamsJoining = "No teams are joining"
    override fun untilWaveArrives(wave: Int) = "Until Wave $wave arrives"
    override fun wavePlus(wave: Int, span: String) = "Wave $wave +$span"
    override val cancelled = "Rally cancelled"
    override val cancelledSub = "All scheduled taps were stopped"
    override val allArrived = "All arrived"
    override val arrivedSub = "If it failed, rally again right away"
    override val roomEmptyAdmin = "This room has no teams"
    override val roomEmptyAdminSub = "Check the room number · Create new rooms on the app's home screen"
    override val roomEmptyLeader = "The commander is setting up teams"
    override val roomEmptyLeaderSub = "Teams will appear here once they are added"
    override val notJoining = "Not joining"
    override val notAssigned = "No team assigned to you yet"
    override val notAssignedSub = "You can start once the commander assigns you a team"
    override val adminOut = "You are not joining this rally"
    override val adminOutSub = "Follow the progress in the team list"
    override val excluded = "You were left out of this rally"
    override val excludedSub = "You can join when the commander adds you back"
    override val waitingStart = "Waiting for the commander"
    override fun myClickAt(sec: Int) = "My rally tap ${sec}s after start"
    override val untilFirstClick = "Until the first rally tap"
    override val untilAllDepart = "Until all depart"
    override val untilAllArrive = "Until all arrive"
    override fun waveStage(wave: Int, phase: Int) = "Wave $wave · " + when (phase) { 0 -> "before tap"; 1 -> "rallying"; else -> "marching" }
    override fun nextWave(wave: Int, mmss: String) = " · next Wave $wave $mmss"
    override val goToRally = "Go to the rally screen"
    override val untilMyClick = "Until rally tap"
    override val waitingClick = "Waiting to tap"
    override fun untilDepartThenMarch(mmss: String) = "Until departure · then march $mmss"
    override val untilCastle = "Until arrival at the castle"
    override fun allArriveIn(mmss: String) = "All arrive in $mmss"
    override val myArrived = "My team arrived"
    override val warnName = "Set your character name first (tap to enter)"
    override val warnPosition = "Save the tap position first (tap to open)"
    override fun idleSub(sub: String, activeTeams: Int) = "$sub · " + (if (activeTeams == 1) "1 team joining" else "$activeTeams teams joining")
    override fun offline(who: String) = "$who offline"
    override fun eta(clock: String) = "ETA $clock"
    override fun arrivedAt(clock: String) = "Arrived $clock"
    override fun clicked(time: String) = "✓ Tapped $time"
    override fun commanderFallback(last4: String) = "Commander …$last4"
    override fun lastChange(who: String, ago: String) = "Last change: $who · $ago"
    override val justNow = "just now"
    override fun minAgo(n: Long) = "${n}m ago"
    override fun hourAgo(n: Long) = "${n}h ago"
    override fun dayAgo(n: Long) = "${n}d ago"

    override val clickFirst = "Taps first"
    override fun secLater(text: String) = "+${text}s"
    override val arrivesFirst = "Arrives first"
    override val arrivesTogether = "Same time"
    override fun arrivesAfter(span: String) = "$span later"
    override fun base(label: String) = "$label (base)"
    override fun untilArrive(mmss: String) = "Arrives in $mmss"
    override fun afterStart(mmss: String) = "$mmss after start"
    override val rowsHeadEdit = "March time · taps in order from the top"
    override val rowsHeadRun = "March time · until the stage ends"
    override val unassigned = "Unassigned"
    override val unassignedTapAssign = "Unassigned · tap to assign"
    override fun unassignedNote(names: String) = "$names unassigned · a team with no one assigned will not tap"
    override val tapAgainToDelete = "Tap again to delete"
    override fun deleteTeam(name: String) = "Delete $name"
    override val deleteQ = "Delete?"
    override val expand = "Expand"
    override val collapse = "Collapse"
    override val splitWaves = "Split waves"
    override val addTeam = "+ Add team"
    override fun maxTeams(n: Int) = "Max $n"

    override val setup = "Room settings"
    override val setupExpand = "Expand room settings"
    override val setupCollapse = "Collapse room settings"
    override fun setupSummary(waitMin: String, prepSec: String) = "Rally time ${waitMin}m · Prep ${prepSec}s"
    override val blockedHintEditing = "Check the teams that will join"
    override val blockedHintView = "Tap the pencil above and check teams"
    override val myDevice = "My device"
    override val posSaved = "Position saved"
    override val posNone = "No position"
    override val roomNone = "No room"
    override val nameNone = "No name"
    override fun correction(text: String) = "Offset $text"
    override val savedFlash = "✓ Saved"
    override val saveAgain = "Save again"
    override val savePos = "Save position"
    override val register = "Set name"
    override val offsetStepNote = "Offset · 0.5s steps"
    override val done = "Done"
    override val tapToExpand = "Tap to expand"
    override val minimize = "Minimize"
    override val starting = "Starting…"
    override val regroup = "Rally again"
    override val start = "Start rally"
    override val miniCancelled = "Cancelled"
    override val miniAssignWait = "Awaiting team"
    override val miniBuilding = "Setting up teams"
    override val miniStartWait = "Waiting to start"

    override val close = "Close"
    override val cancel = "Cancel"
    override val ok = "OK"
    override val move = "Move"
    override fun moveTo(room: String, action: String) = "$action to room $room"
    override val pickRoomFirst = "Pick a room to move to"
    override val badgeCurrent = "Current"
    override val badgeRunning = "Rallying"
    override fun rangeError(max: Int) = "Enter a number from 0 to $max"
    override fun rangeErrorEx(max: Int, example: String) = "Enter a number from 0 to $max (e.g. $example)"
    override val offsetError = "Enter a number from −5 to +5 (e.g. -1.5)"
    override fun unassignedTitle(names: String) = "$names: no one assigned"
    override val unassignedBody = "A team with no one assigned will not tap. What would you like to do?"
    override val startWithoutUnassigned = "Start without unassigned teams"
    override val startAnyway = "Start anyway"
    override val closeGuardToast = "A rally is running. Tap ✕ once more to close the panel"
    override val askMarch = "March time (seconds)"
    override val askPrep = "Prep time (seconds)"
    override fun askWaveDelay(wave: Int) = "Wave $wave · seconds after Wave 1"
    override val splitTitle = "Split into waves"
    override val splitBody = "Teams are split in number order. Each wave gets its own delay after Wave 1."
    override fun perWave(n: Int) = "$n per wave"
    override val allOneWave = "All in Wave 1 (no waves)"
    override fun askTeamOffset(team: String) = "$team offset (seconds) · − earlier, + later"
    override val askMyOffset = "Tap offset (seconds) · − earlier, + later"
    override val noRoomsToast = "No rooms joined yet. Create or join a room on the app's home screen"
    override fun roomsLoading(n: Int) = "Rooms ($n) · loading…"
    override val roomsLoadingNone = "Rooms · loading…"
    override fun roomsFailed(error: String) = "Rooms · could not load the room list" + (if (error.isNotEmpty()) " ($error)" else "")
    override fun roomsTitle(n: Int) = "Rooms ($n)"
    override val askCharName = "In-game character name · the commander assigns teams by this name"
    override fun charNameError(max: Int) = "Enter a character name (up to $max characters)"
    override fun assignTitle(team: String) = "Who leads $team"
    override val rosterLoading = "Loading the list…"
    override fun rosterFailed(error: String) = "Could not load the list. Check the internet connection and that the Firebase rules (rallyMembers) are published\n($error)"
    override val unassign = "Remove assignment"
    override val rosterEmpty = "No one has registered yet. Rally leaders appear here after they set a character name in the app"

    override val rejected = "The server refused the room change. Check that this device is on the commander list and that the server rules are correct"
    override fun saveFailed(message: String) = "Could not save to the server: $message"
    override val unknownError = "unknown error"
    override val wavesRejected = "Could not save waves. Publish the new server rules to use waves (Firebase console → Rules)"
    override fun startFailed(why: String) = "Could not start the rally. Tap again · $why"
    override fun cancelFailed(why: String) = "Could not send the cancel to the server. Other phones may still be running · $why"
    override fun adminOffsetNote(text: String) = "Commander offset $text (added to my offset)"
    override val pollingNote = "Sync: checking every 1s (can be up to 1s behind live)"
    override fun deviceLine(last4: String, team: String) = "Device ID …$last4 · Team: $team"
    override val noneYet = "none yet"
    override fun syncMode(live: Boolean) = "Sync: " + (if (live) "live" else "every 1s")
    override val needLogin = "Sign-in required"
    override fun serverRefused(code: Int) = "The server refused ($code)"
    override val checkInternet = "Check your internet connection"
    override val nameEmpty = "The character name is empty"
    override fun lastDiag(actualSec: String, targetSec: String, offsetMs: Int, wall: String) =
        "Last rally: tapped ${actualSec}s after the signal (target ${targetSec}s, offset ${offsetMs}ms) · $wall"
}

internal object StrZh : Str {
    private val teamNo = Regex("""^(\d+)군$""")

    override fun sec(text: String) = "${text}秒"
    override val secSuffixes = listOf("秒", "s", "초")
    override fun span(min: Int, sec: Int) = when { min == 0 -> "${sec}秒"; sec == 0 -> "${min}分"; else -> "${min}分${sec}秒" }
    override fun teamName(raw: String) = teamNo.matchEntire(raw)?.let { "${it.groupValues[1]}队" } ?: raw
    override fun wave(n: Int) = "第${n}波"
    override val waveWord = "波"
    override fun teamCount(n: Int) = "${n}支队伍"
    override val teams = "队伍"
    override fun teamsOf(n: Int, max: Int) = "队伍 ${n}/${max}"
    override val commander = "指挥官"
    override val leader = "集结队长"
    override val me = "我"
    override val myTeamDesc = "我的队伍"
    override val none = "无"
    override val roomWord = "房间"
    override fun room(code: String) = "房间 ${code}"
    override val charName = "角色名"

    override val phaseNames = listOf("等待", "集结", "行军", "到达")
    override val beforeClick = "点击前"
    override val gathering = "集结中"
    override val marching = "行军中"
    override val statusMarch = "行军"
    override val statusArrived = "到达"
    override val statusExcluded = "排除"
    override val prepMove = "准备时间"
    override val waitClick = "等待点击"

    override val noTeamsJoining = "没有参加的队伍"
    override fun untilWaveArrives(wave: Int) = "距第${wave}波到达"
    override fun wavePlus(wave: Int, span: String) = "第${wave}波 +${span}"
    override val cancelled = "行动已取消"
    override val cancelledSub = "已预约的点击全部停止"
    override val allArrived = "全员到达"
    override val arrivedSub = "如果失败，请马上重新集结"
    override val roomEmptyAdmin = "这个房间没有队伍"
    override val roomEmptyAdminSub = "请确认房间号 · 新房间在应用首页创建"
    override val roomEmptyLeader = "指挥官正在编排队伍"
    override val roomEmptyLeaderSub = "房间里有了队伍就会显示在这里"
    override val notJoining = "不参加"
    override val notAssigned = "还没有给你分配队伍"
    override val notAssignedSub = "指挥官分配队伍后就可以开始"
    override val adminOut = "这次行动你不参加"
    override val adminOutSub = "请在队伍列表中查看进度"
    override val excluded = "这次行动你被排除了"
    override val excludedSub = "指挥官重新加入后就可以参加"
    override val waitingStart = "等待指挥官开始"
    override fun myClickAt(sec: Int) = "开始后${sec}秒点击我的集结"
    override val untilFirstClick = "距第一次集结点击"
    override val untilAllDepart = "距全员出发"
    override val untilAllArrive = "距全员到达"
    override fun waveStage(wave: Int, phase: Int) = "第${wave}波 " + when (phase) { 0 -> "点击前"; 1 -> "集结中"; else -> "行军中" }
    override fun nextWave(wave: Int, mmss: String) = " · 下一波 第${wave}波 ${mmss}"
    override val goToRally = "请切换到集结画面"
    override val untilMyClick = "距集结点击"
    override val waitingClick = "等待点击中"
    override fun untilDepartThenMarch(mmss: String) = "距出发 · 之后行军 ${mmss}"
    override val untilCastle = "距到达城堡"
    override fun allArriveIn(mmss: String) = "距全员到达 ${mmss}"
    override val myArrived = "我的队伍已到达"
    override val warnName = "请先登记角色名（点击输入）"
    override val warnPosition = "请先保存点击位置（点击打开）"
    override fun idleSub(sub: String, activeTeams: Int) = "${sub} · 参加队伍${activeTeams}支"
    override fun offline(who: String) = "${who} 未连接"
    override fun eta(clock: String) = "预计到达 ${clock}"
    override fun arrivedAt(clock: String) = "${clock} 到达"
    override fun clicked(time: String) = "✓ 已点击 ${time}"
    override fun commanderFallback(last4: String) = "指挥官 …${last4}"
    override fun lastChange(who: String, ago: String) = "最后修改：${who} · ${ago}"
    override val justNow = "刚刚"
    override fun minAgo(n: Long) = "${n}分钟前"
    override fun hourAgo(n: Long) = "${n}小时前"
    override fun dayAgo(n: Long) = "${n}天前"

    override val clickFirst = "最先点击"
    override fun secLater(text: String) = "${text}秒后"
    override val arrivesFirst = "最先到达"
    override val arrivesTogether = "同时到达"
    override fun arrivesAfter(span: String) = "${span}后到达"
    override fun base(label: String) = "${label}（基准）"
    override fun untilArrive(mmss: String) = "距到达 ${mmss}"
    override fun afterStart(mmss: String) = "开始后 ${mmss}"
    override val rowsHeadEdit = "行军时间 · 从上到下依次点击"
    override val rowsHeadRun = "行军时间 · 到本阶段结束"
    override val unassigned = "未分配"
    override val unassignedTapAssign = "未分配 · 点击分配"
    override fun unassignedNote(names: String) = "${names} 未分配 · 没有负责人的队伍不会点击"
    override val tapAgainToDelete = "再点一次删除"
    override fun deleteTeam(name: String) = "删除${name}"
    override val deleteQ = "删除？"
    override val expand = "展开"
    override val collapse = "收起"
    override val splitWaves = "分波"
    override val addTeam = "+ 添加队伍"
    override fun maxTeams(n: Int) = "最多${n}支"

    override val setup = "房间设置"
    override val setupExpand = "展开房间设置"
    override val setupCollapse = "收起房间设置"
    override fun setupSummary(waitMin: String, prepSec: String) = "集结时间${waitMin}分 · 准备时间${prepSec}秒"
    override val blockedHintEditing = "请勾选要参加的队伍"
    override val blockedHintView = "点击上方的铅笔，勾选队伍"
    override val myDevice = "我的设备"
    override val posSaved = "位置已保存"
    override val posNone = "没有位置"
    override val roomNone = "没有房间"
    override val nameNone = "没有角色名"
    override fun correction(text: String) = "校正 ${text}"
    override val savedFlash = "✓ 已保存"
    override val saveAgain = "重新保存"
    override val savePos = "保存位置"
    override val register = "登记"
    override val offsetStepNote = "校正 · 每次0.5秒"
    override val done = "完成"
    override val tapToExpand = "点击展开"
    override val minimize = "缩小"
    override val starting = "正在开始…"
    override val regroup = "重新集结"
    override val start = "开始集结"
    override val miniCancelled = "行动取消"
    override val miniAssignWait = "等待分配队伍"
    override val miniBuilding = "编排队伍中"
    override val miniStartWait = "等待开始"

    override val close = "关闭"
    override val cancel = "取消"
    override val ok = "确定"
    override val move = "移动"
    override fun moveTo(room: String, action: String) = "${action}到房间 ${room}"
    override val pickRoomFirst = "请选择要去的房间"
    override val badgeCurrent = "当前"
    override val badgeRunning = "集结中"
    override fun rangeError(max: Int) = "请输入 0 ~ ${max} 之间的数字"
    override fun rangeErrorEx(max: Int, example: String) = "请输入 0 ~ ${max} 之间的数字（例如 ${example}）"
    override val offsetError = "请输入 −5 ~ +5 之间的数字（例如 -1.5）"
    override fun unassignedTitle(names: String) = "${names} 还没有分配"
    override val unassignedBody = "没有负责人的队伍不会点击。要怎么做？"
    override val startWithoutUnassigned = "排除未分配后开始"
    override val startAnyway = "直接开始"
    override val closeGuardToast = "集结正在进行。要关闭面板请再点一次 ✕"
    override val askMarch = "行军时间（秒）"
    override val askPrep = "准备时间（秒）"
    override fun askWaveDelay(wave: Int) = "第${wave}波 · 比第1波晚几秒到达"
    override val splitTitle = "分波"
    override val splitBody = "按队伍编号顺序分。每一波可以分别设定比第1波晚多久到达。"
    override fun perWave(n: Int) = "每波${n}支"
    override val allOneWave = "全部归入第1波（不分波）"
    override fun askTeamOffset(team: String) = "${team} 校正（秒） · −更早，+更晚"
    override val askMyOffset = "点击校正（秒） · −更早，+更晚"
    override val noRoomsToast = "还没有进入过房间。请在应用首页创建或进入房间"
    override fun roomsLoading(n: Int) = "选择房间（${n}个） · 加载中…"
    override val roomsLoadingNone = "选择房间 · 加载中…"
    override fun roomsFailed(error: String) = "选择房间 · 无法加载服务器上的房间列表" + (if (error.isNotEmpty()) " ($error)" else "")
    override fun roomsTitle(n: Int) = "选择房间（${n}个）"
    override val askCharName = "游戏角色名 · 指挥官会根据这个名字分配队伍"
    override fun charNameError(max: Int) = "请输入角色名（最多${max}个字）"
    override fun assignTitle(team: String) = "${team} 的负责人"
    override val rosterLoading = "正在加载名单…"
    override fun rosterFailed(error: String) = "无法加载名单。请检查网络连接，以及 Firebase 规则（rallyMembers）是否已发布\n($error)"
    override val unassign = "取消分配"
    override val rosterEmpty = "还没有人登记。集结队长在应用中登记角色名后会显示在这里"

    override val rejected = "服务器拒绝了房间修改。请确认这台设备在指挥官名单中，并且服务器规则正确"
    override fun saveFailed(message: String) = "无法保存到服务器：${message}"
    override val unknownError = "未知错误"
    override val wavesRejected = "无法保存分波。需要重新发布服务器规则才能使用分波（Firebase 控制台 → 规则）"
    override fun startFailed(why: String) = "无法开始集结。请再点一次 · ${why}"
    override fun cancelFailed(why: String) = "取消没有传到服务器。其他手机可能还在进行 · ${why}"
    override fun adminOffsetNote(text: String) = "指挥官校正 ${text}（与我的校正相加）"
    override val pollingNote = "接收方式：每1秒检查（最多比实时晚1秒）"
    override fun deviceLine(last4: String, team: String) = "设备 ID …${last4} · 分配：${team}"
    override val noneYet = "还没有"
    override fun syncMode(live: Boolean) = "接收方式：" + (if (live) "实时" else "每1秒检查")
    override val needLogin = "需要登录"
    override fun serverRefused(code: Int) = "服务器拒绝了 (${code})"
    override val checkInternet = "请检查网络连接"
    override val nameEmpty = "角色名为空"
    override fun lastDiag(actualSec: String, targetSec: String, offsetMs: Int, wall: String) =
        "上次集结：收到信号${actualSec}秒后点击（目标${targetSec}秒，校正${offsetMs}ms） · ${wall}"
}

internal object StrZhTw : Str {
    private val teamNo = Regex("""^(\d+)군$""")

    override fun sec(text: String) = "${text}秒"
    override val secSuffixes = listOf("秒", "s", "초")
    override fun span(min: Int, sec: Int) = when { min == 0 -> "${sec}秒"; sec == 0 -> "${min}分"; else -> "${min}分${sec}秒" }
    override fun teamName(raw: String) = teamNo.matchEntire(raw)?.let { "${it.groupValues[1]}隊" } ?: raw
    override fun wave(n: Int) = "第${n}波"
    override val waveWord = "波"
    override fun teamCount(n: Int) = "${n}支隊伍"
    override val teams = "隊伍"
    override fun teamsOf(n: Int, max: Int) = "隊伍 ${n}/${max}"
    override val commander = "指揮官"
    override val leader = "集結隊長"
    override val me = "我"
    override val myTeamDesc = "我的隊伍"
    override val none = "無"
    override val roomWord = "房間"
    override fun room(code: String) = "房間 ${code}"
    override val charName = "角色名"

    override val phaseNames = listOf("等待", "集結", "行軍", "到達")
    override val beforeClick = "點擊前"
    override val gathering = "集結中"
    override val marching = "行軍中"
    override val statusMarch = "行軍"
    override val statusArrived = "到達"
    override val statusExcluded = "排除"
    override val prepMove = "準備時間"
    override val waitClick = "等待點擊"

    override val noTeamsJoining = "沒有參加的隊伍"
    override fun untilWaveArrives(wave: Int) = "距第${wave}波到達"
    override fun wavePlus(wave: Int, span: String) = "第${wave}波 +${span}"
    override val cancelled = "行動已取消"
    override val cancelledSub = "已預約的點擊全部停止"
    override val allArrived = "全員到達"
    override val arrivedSub = "如果失敗，請馬上重新集結"
    override val roomEmptyAdmin = "這個房間沒有隊伍"
    override val roomEmptyAdminSub = "請確認房間號 · 新房間在應用程式首頁建立"
    override val roomEmptyLeader = "指揮官正在編排隊伍"
    override val roomEmptyLeaderSub = "房間裡有了隊伍就會顯示在這裡"
    override val notJoining = "不參加"
    override val notAssigned = "還沒有給你分配隊伍"
    override val notAssignedSub = "指揮官分配隊伍後就可以開始"
    override val adminOut = "這次行動你不參加"
    override val adminOutSub = "請在隊伍列表中檢視進度"
    override val excluded = "這次行動你被排除了"
    override val excludedSub = "指揮官重新加入後就可以參加"
    override val waitingStart = "等待指揮官開始"
    override fun myClickAt(sec: Int) = "開始後${sec}秒點擊我的集結"
    override val untilFirstClick = "距第一次集結點擊"
    override val untilAllDepart = "距全員出發"
    override val untilAllArrive = "距全員到達"
    override fun waveStage(wave: Int, phase: Int) = "第${wave}波 " + when (phase) { 0 -> "點擊前"; 1 -> "集結中"; else -> "行軍中" }
    override fun nextWave(wave: Int, mmss: String) = " · 下一波 第${wave}波 ${mmss}"
    override val goToRally = "請切換到集結畫面"
    override val untilMyClick = "距集結點擊"
    override val waitingClick = "等待點擊中"
    override fun untilDepartThenMarch(mmss: String) = "距出發 · 之後行軍 ${mmss}"
    override val untilCastle = "距到達城堡"
    override fun allArriveIn(mmss: String) = "距全員到達 ${mmss}"
    override val myArrived = "我的隊伍已到達"
    override val warnName = "請先登記角色名（點擊輸入）"
    override val warnPosition = "請先儲存點擊位置（點擊開啟）"
    override fun idleSub(sub: String, activeTeams: Int) = "${sub} · 參加隊伍${activeTeams}支"
    override fun offline(who: String) = "${who} 未連線"
    override fun eta(clock: String) = "預計到達 ${clock}"
    override fun arrivedAt(clock: String) = "${clock} 到達"
    override fun clicked(time: String) = "✓ 已點擊 ${time}"
    override fun commanderFallback(last4: String) = "指揮官 …${last4}"
    override fun lastChange(who: String, ago: String) = "最後修改：${who} · ${ago}"
    override val justNow = "剛剛"
    override fun minAgo(n: Long) = "${n}分鐘前"
    override fun hourAgo(n: Long) = "${n}小時前"
    override fun dayAgo(n: Long) = "${n}天前"

    override val clickFirst = "最先點擊"
    override fun secLater(text: String) = "${text}秒後"
    override val arrivesFirst = "最先到達"
    override val arrivesTogether = "同時到達"
    override fun arrivesAfter(span: String) = "${span}後到達"
    override fun base(label: String) = "${label}（基準）"
    override fun untilArrive(mmss: String) = "距到達 ${mmss}"
    override fun afterStart(mmss: String) = "開始後 ${mmss}"
    override val rowsHeadEdit = "行軍時間 · 從上到下依次點擊"
    override val rowsHeadRun = "行軍時間 · 到本階段結束"
    override val unassigned = "未分配"
    override val unassignedTapAssign = "未分配 · 點擊分配"
    override fun unassignedNote(names: String) = "${names} 未分配 · 沒有負責人的隊伍不會點擊"
    override val tapAgainToDelete = "再點一次刪除"
    override fun deleteTeam(name: String) = "刪除${name}"
    override val deleteQ = "刪除？"
    override val expand = "展開"
    override val collapse = "收起"
    override val splitWaves = "分波"
    override val addTeam = "+ 新增隊伍"
    override fun maxTeams(n: Int) = "最多${n}支"

    override val setup = "房間設定"
    override val setupExpand = "展開房間設定"
    override val setupCollapse = "收起房間設定"
    override fun setupSummary(waitMin: String, prepSec: String) = "集結時間${waitMin}分 · 準備時間${prepSec}秒"
    override val blockedHintEditing = "請勾選要參加的隊伍"
    override val blockedHintView = "點擊上方的鉛筆，勾選隊伍"
    override val myDevice = "我的裝置"
    override val posSaved = "位置已儲存"
    override val posNone = "沒有位置"
    override val roomNone = "沒有房間"
    override val nameNone = "沒有角色名"
    override fun correction(text: String) = "校正 ${text}"
    override val savedFlash = "✓ 已儲存"
    override val saveAgain = "重新儲存"
    override val savePos = "儲存位置"
    override val register = "登記"
    override val offsetStepNote = "校正 · 每次0.5秒"
    override val done = "完成"
    override val tapToExpand = "點擊展開"
    override val minimize = "縮小"
    override val starting = "正在開始…"
    override val regroup = "重新集結"
    override val start = "開始集結"
    override val miniCancelled = "行動取消"
    override val miniAssignWait = "等待分配隊伍"
    override val miniBuilding = "編排隊伍中"
    override val miniStartWait = "等待開始"

    override val close = "關閉"
    override val cancel = "取消"
    override val ok = "確定"
    override val move = "移動"
    override fun moveTo(room: String, action: String) = "${action}到房間 ${room}"
    override val pickRoomFirst = "請選擇要去的房間"
    override val badgeCurrent = "當前"
    override val badgeRunning = "集結中"
    override fun rangeError(max: Int) = "請輸入 0 ~ ${max} 之間的數字"
    override fun rangeErrorEx(max: Int, example: String) = "請輸入 0 ~ ${max} 之間的數字（例如 ${example}）"
    override val offsetError = "請輸入 −5 ~ +5 之間的數字（例如 -1.5）"
    override fun unassignedTitle(names: String) = "${names} 還沒有分配"
    override val unassignedBody = "沒有負責人的隊伍不會點擊。要怎麼做？"
    override val startWithoutUnassigned = "排除未分配後開始"
    override val startAnyway = "直接開始"
    override val closeGuardToast = "集結正在進行。要關閉面板請再點一次 ✕"
    override val askMarch = "行軍時間（秒）"
    override val askPrep = "準備時間（秒）"
    override fun askWaveDelay(wave: Int) = "第${wave}波 · 比第1波晚幾秒到達"
    override val splitTitle = "分波"
    override val splitBody = "按隊伍編號順序分。每一波可以分別設定比第1波晚多久到達。"
    override fun perWave(n: Int) = "每波${n}支"
    override val allOneWave = "全部歸入第1波（不分波）"
    override fun askTeamOffset(team: String) = "${team} 校正（秒） · −更早，+更晚"
    override val askMyOffset = "點擊校正（秒） · −更早，+更晚"
    override val noRoomsToast = "還沒有進入過房間。請在應用程式首頁建立或進入房間"
    override fun roomsLoading(n: Int) = "選擇房間（${n}個） · 載入中…"
    override val roomsLoadingNone = "選擇房間 · 載入中…"
    override fun roomsFailed(error: String) = "選擇房間 · 無法載入伺服器上的房間列表" + (if (error.isNotEmpty()) " ($error)" else "")
    override fun roomsTitle(n: Int) = "選擇房間（${n}個）"
    override val askCharName = "遊戲角色名 · 指揮官會根據這個名字分配隊伍"
    override fun charNameError(max: Int) = "請輸入角色名（最多${max}個字）"
    override fun assignTitle(team: String) = "${team} 的負責人"
    override val rosterLoading = "正在載入名單…"
    override fun rosterFailed(error: String) = "無法載入名單。請檢查網路連線，以及 Firebase 規則（rallyMembers）是否已發佈\n($error)"
    override val unassign = "取消分配"
    override val rosterEmpty = "還沒有人登記。集結隊長在應用程式中登記角色名後會顯示在這裡"

    override val rejected = "伺服器拒絕了房間修改。請確認這台裝置在指揮官名單中，並且伺服器規則正確"
    override fun saveFailed(message: String) = "無法儲存到伺服器：${message}"
    override val unknownError = "未知錯誤"
    override val wavesRejected = "無法儲存分波。需要重新發布伺服器規則才能使用分波（Firebase 主控台 → 規則）"
    override fun startFailed(why: String) = "無法開始集結。請再點一次 · ${why}"
    override fun cancelFailed(why: String) = "取消沒有傳到伺服器。其他手機可能還在進行 · ${why}"
    override fun adminOffsetNote(text: String) = "指揮官校正 ${text}（與我的校正相加）"
    override val pollingNote = "接收方式：每1秒檢查（最多比實時晚1秒）"
    override fun deviceLine(last4: String, team: String) = "裝置 ID …${last4} · 分配：${team}"
    override val noneYet = "還沒有"
    override fun syncMode(live: Boolean) = "接收方式：" + (if (live) "實時" else "每1秒檢查")
    override val needLogin = "需要登入"
    override fun serverRefused(code: Int) = "伺服器拒絕了 (${code})"
    override val checkInternet = "請檢查網路連線"
    override val nameEmpty = "角色名為空"
    override fun lastDiag(actualSec: String, targetSec: String, offsetMs: Int, wall: String) =
        "上次集結：收到訊號${actualSec}秒後點擊（目標${targetSec}秒，校正${offsetMs}ms） · ${wall}"
}
