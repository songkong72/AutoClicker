package com.sejun.autoclicker

import android.content.Context

/**
 * 화면에 보이는 글자를 한곳에 모은다(코드 안에서 만드는 글자용. 레이아웃의 고정 글자는 `res/values*/strings.xml`).
 * 계산하는 쪽([RallyScreenModel] 등)은 안드로이드 없이 테스트하므로 리소스 대신 이 표를 쓴다.
 * 언어를 더하려면 [Str]를 구현한 표를 하나 더 만들고 [T.forLanguage]에 잇는다.
 * 기본은 한국어다: 번역이 없는 언어의 폰은 한국어로 보인다.
 */
internal object T {
    @Volatile var s: Str = StrKo
        private set

    fun forLanguage(lang: String?): Str = when (lang) {
        "en" -> StrEn
        else -> StrKo
    }

    /** 폰(또는 이 앱)의 언어에 맞춘다. 화면을 그리기 직전에 부른다. */
    fun use(context: Context) {
        val want = forLanguage(context.resources.configuration.locales[0]?.language)
        if (want !== s) s = want
    }

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
