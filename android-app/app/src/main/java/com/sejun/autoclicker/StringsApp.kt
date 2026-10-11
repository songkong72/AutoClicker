package com.sejun.autoclicker

/**
 * 앱 화면(첫 화면·창·게임 위 막대)의 글자 표. 집결 패널의 글자는 [Str]에 있다.
 * `T.a.이름`으로 꺼낸다. 이 파일은 표라서 한 줄이 길다.
 */
internal interface AppStr {
    val later: String
    val download: String
    val apply: String
    val save: String
    val verify: String
    val manage: String
    val edit: String
    val delete: String
    val create: String
    val enter: String
    val copy: String
    val manual: String
    fun secs(v: String): String
    fun mins(n: Int): String
    val unitSec: String
    val unitMin: String
    val unitTimes: String
    val updTitle: String
    val updBody: String
    val stoppedNow: String
    val needAuthToast: String
    val needSwitch: String
    val notReady: String
    val joinRoomFirst: String
    val panelShown: String
    val floaterHidden: String
    val floaterShown: String
    val clickSettings: String
    val interval: String
    val repeatCond: String
    val modeInfinite: String
    val modeCount: String
    val modeTimer: String
    val infiniteNote: String
    val intervalError: String
    val findInList: String
    val emergencyStop: String
    val hideFloater: String
    val showFloater: String
    val generalMode: String
    val floatingNow: String
    val groupByOthers: String
    val groupByCode: String
    val setGroup: String
    val groupFromCopied: String
    val groupBody: String
    val serverHint: String
    val allianceHint: String
    val groupCheck: String
    fun groupSet(label: String): String
    fun groupAssigned(label: String): String
    val pickOrCreate: String
    val pickRoom: String
    val roomHelpTitle: String
    val roomHelpBody: String
    val createFirst: String
    val loadingRooms: String
    val alreadyHere: String
    fun deleteFailed(err: String): String
    fun roomDeleted(code: String): String
    val enterCodeTitle: String
    val enterCodeBody: String
    val codeHint: String
    val codeTooShort: String
    val nameTitle: String
    val nameBody: String
    val nameHint: String
    val nameRequired: String
    fun joined(code: String): String
    val registering: String
    fun registered(name: String): String
    fun registerFailed(err: String): String
    val checkingRoom: String
    val roomCheckFailedStatus: String
    val roomCheckFailed: String
    fun noSuchRoomStatus(group: String): String
    val noSuchRoom: String
    fun movingNote(current: String): String
    val newRoomTitle: String
    fun newRoomBody(moving: String): String
    val newRoomHint: String
    val roomExists: String
    fun createFailed(err: String): String
    fun roomCreated(code: String): String
    val previewTitle: String
    val repPendingPrefix: String
    val previewPendingTail: String
    val checkApproved: String
    val previewBody: String
    val adminModeTitle: String
    val adminModeBody: String
    val leaderModeTitle: String
    val leaderModeBody: String
    val switchToCommander: String
    val repPendingTitle: String
    val repPendingTail: String
    val verifiedTitle: String
    fun verifiedId(id: String): String
    val verifiedBody: String
    val changeAuth: String
    val lockedTitle: String
    val lockedBody: String
    val demoted: String
    val menuIssueCode: String
    val menuAdmin: String
    val devOnly: String
    val menuMyId: String
    val menuAlliance: String
    val repOnly: String
    val menuToLeader: String
    val verifyTitle: String
    val verifyFilled: String
    val verifyBody: String
    val hintEmail: String
    val labelEmail: String
    val hintCode: String
    val labelCode: String
    val hintServer: String
    val labelServer: String
    val hintAlliance: String
    val labelAlliance: String
    val verifySubmit: String
    val linkAdminLogin: String
    val needEmail: String
    val needCode: String
    fun verifiedGroup(label: String): String
    val verifiedWelcome: String
    val oldCode: String
    val codeMismatch: String
    val loginIntroOwner: String
    val loginIntro: String
    val loginTitle: String
    val loginTail: String
    val loginHint: String
    val linkMyId: String
    val linkRepRequest: String
    val checkingCode: String
    val commanderRegistered: String
    val codePrefixError: String
    val checkingOwner: String
    val ownerOnly: String
    val serverCheckFailedNet: String
    val toUserView: String
    val toDevView: String
    val toLeader: String
    val checkingRoster: String
    val enteredOwner: String
    val toCommander: String
    val notOnRoster: String
    val serverCheckFailed: String
    val issueBody: String
    val issueHint: String
    val makeCode: String
    val sendKakao: String
    val sendOther: String
    val needGroupForCode: String
    val noCodeSecret: String
    val codeMade: String
    val editRooms: String
    val pickRooms: String
    fun countN(n: Int): String
    val noRooms: String
    val typeNumber: String
    val currentRoom: String
    val inUse: String
    fun deleteRoomDesc(code: String): String
    fun deleteRoomQ(code: String): String
    val deleting: String
    val deleteRoomBody: String
    fun badge(assigned: Int, teams: Int): String
    fun roomSummary(teams: String, assigned: String): String
    fun roomsFallbackNote(error: String): String
    val roomsNone: String
    fun pickLine(code: String, teams: Int, assigned: Int): String
    val pickRunning: String
    val gServerEmpty: String
    fun gServerBad(max: Int): String
    val gAllianceEmpty: String
    fun gAllianceLong(max: Int): String
    val gAllianceChars: String
    fun roomsFull(max: Int): String
    fun every(sec: String): String
    val repInfinite: String
    fun repCount(n: Int): String
    fun forDuration(d: String): String
    val discTitle: String
    val quit: String
    val agree: String
    val discBody: String
    fun stall(sec: String, wall: String): String
    val clickNoPos: String
    val toastNoPos: String
    fun tapWhere(x: Int, y: Int, w: Int, h: Int, hasTarget: Boolean, armed: Boolean): String
    fun tapSending(where: String): String
    fun tapDone(where: String): String
    fun tapCancelled(where: String): String
    fun tapRefused(where: String): String
    fun rallyClickToast(x: Int, y: Int): String
    val clickSendFailed: String
    fun movedRoom(code: String): String
    val needAuth: String
    val openAppJoin: String
    val openAppGroup: String
    fun savedPos(x: Int, y: Int): String
    val noPosHelp: String
    val posSavedToast: String
    fun otherAdminChanged(who: String): String
    val stoppedFromNoti: String
    val fullyOff: String
    val serviceReady: String
    val stoppedByVolume: String
    val widgetFailed: String
    val targetCentered: String
    val closedKeepRally: String
    val turnedOff: String
    val targetShown: String
    val targetHidden: String
    fun opacityToast(pct: Int): String
    fun countTimes(n: Int): String
    fun minSec(m: Int, s: Int): String
    fun settingsApplied(interval: String, mode: String, pct: Int): String
    val stoppedTap: String
    val targetNotReady: String
    fun repeatN(n: Int): String
    fun timerDesc(m: Int, s: Int): String
    fun tapStarted(desc: String): String
    fun countDone(n: Int): String
    fun timerDone(m: Int, s: Int): String
    val notiChannel: String
    val notiChannelDesc: String
    val notiRunning: String
    val notiIdle: String
    val notiRunningSub: String
    val notiIdleSub: String
    val notiStop: String
    val notiClose: String
}

internal object AppKo : AppStr {
    override val later = "나중에"
    override val download = "다운로드"
    override val apply = "적용"
    override val save = "저장"
    override val verify = "인증하기"
    override val manage = "관리"
    override val edit = "편집"
    override val delete = "삭제"
    override val create = "만들기"
    override val enter = "입장"
    override val copy = "복사"
    override val manual = "사용 설명서"
    override fun secs(v: String) = "${v}초"
    override fun mins(n: Int) = "${n}분"
    override val unitSec = "초"
    override val unitMin = "분"
    override val unitTimes = "회"
    override val updTitle = "새로운 버전 업데이트"
    override val updBody = "연맹 필수 업데이트예요. 새 버전의 오토클리커 Pro를 지금 받을까요?"
    override val stoppedNow = "치료 연타를 바로 멈췄어요."
    override val needAuthToast = "집결장 코드로 인증한 뒤에 쓸 수 있어요."
    override val needSwitch = "스위치를 먼저 켜야 게임을 자동으로 터치할 수 있어요."
    override val notReady = "아직 준비 중이에요. 잠시 후 다시 눌러 주세요.\n(계속 안 되면 접근성을 껐다가 다시 켜 주세요)"
    override val joinRoomFirst = "먼저 집결 방에 입장해 주세요."
    override val panelShown = "집결 화면을 띄웠어요."
    override val floaterHidden = "오토클리커를 숨겼어요."
    override val floaterShown = "오토클리커를 띄웠어요. 게임으로 이동할게요."
    override val clickSettings = "연타 설정"
    override val interval = "클릭 주기"
    override val repeatCond = "반복 조건"
    override val modeInfinite = "무한"
    override val modeCount = "횟수"
    override val modeTimer = "시간"
    override val infiniteNote = "정지 버튼이나 볼륨 키를 누를 때까지 계속 연타해요."
    override val intervalError = "클릭 주기를 0보다 큰 숫자로 입력해 주세요 (예: 0.5)"
    override val findInList = "목록에서 [오토클리커 Pro]를 찾아 켜주세요."
    override val emergencyStop = "🛑 치료 연타 즉시 정지"
    override val hideFloater = "✕ 오토클리커 숨기기"
    override val showFloater = "🚀 오토클리커 띄우기"
    override val generalMode = "일반 연타 모드"
    override val floatingNow = " · 떠 있음"
    override val groupByOthers = "지휘관의 소속은 개발자나 연맹 대표가 정해요. 바꾸려면 그분께 말씀해 주세요."
    override val groupByCode = "소속은 지휘관에게 받은 집결장 코드로 정해져요. 바꾸려면 새 코드를 받아 다시 인증해 주세요."
    override val setGroup = "소속 정하기"
    override val groupFromCopied = "복사해 둔 글에서 소속을 가져왔어요. 맞는지 보고 저장을 눌러 주세요.\n"
    override val groupBody = "같은 소속끼리만 방이 보여요. 지휘관에게 받은 그대로 적어 주세요.\n연맹은 대문자와 소문자를 구분해요 (WBI와 wbi는 다른 소속)."
    override val serverHint = "서버 번호 (예: 2000)"
    override val allianceHint = "연맹 (예: WBI)"
    override val groupCheck = "소속을 다시 확인해 주세요."
    override fun groupSet(label: String) = "소속을 ${label}(으)로 정했어요. 방을 골라 주세요."
    override fun groupAssigned(label: String) = "소속이 ${label}(으)로 정해졌어요. 방을 골라 주세요."
    override val pickOrCreate = "방을 고르거나 만들어 주세요."
    override val pickRoom = "방을 선택해 주세요."
    override val roomHelpTitle = "집결 방"
    override val roomHelpBody = "같은 방에 들어온 군단들이 동시에 성에 도착하도록 집결 클릭 시각이 자동 계산돼요."
    override val createFirst = "먼저 방을 만들어 주세요."
    override val loadingRooms = "방 목록을 불러오는 중…"
    override val alreadyHere = "지금 들어와 있는 방이에요."
    override fun deleteFailed(err: String) = "방을 지우지 못했어요: $err"
    override fun roomDeleted(code: String) = "방 $code 을(를) 지웠어요."
    override val enterCodeTitle = "방 번호 입력"
    override val enterCodeBody = "집결장에게 받은 방 번호를 입력해 주세요."
    override val codeHint = "방 번호 (4자리 이상)"
    override val codeTooShort = "방 번호를 4자리 이상 입력해 주세요."
    override val nameTitle = "캐릭터명 입력"
    override val nameBody = "지휘관이 군단을 배정할 때 이 이름으로 보여요."
    override val nameHint = "캐릭터명"
    override val nameRequired = "캐릭터명을 입력해 주세요."
    override fun joined(code: String) = "방 $code 에 입장했어요."
    override val registering = "명단에 등록하는 중…"
    override fun registered(name: String) = "✓ 명단에 등록됐어요 ($name)"
    override fun registerFailed(err: String) = "✗ 명단 등록 실패: $err · 방 바꾸기에서 다시 골라 주세요"
    override val checkingRoom = "방을 확인하는 중…"
    override val roomCheckFailedStatus = "서버에서 방을 확인하지 못했어요. 인터넷 연결을 확인해 주세요"
    override val roomCheckFailed = "방을 확인하지 못했어요. 인터넷 연결을 확인해 주세요."
    override fun noSuchRoomStatus(group: String) = "없는 방이에요. 소속(${group})과 방 번호를 확인해 주세요"
    override val noSuchRoom = "없는 방이에요. 소속과 방 번호를 확인해 주세요."
    override fun movingNote(current: String) = "\n\n지금 방 $current 에서 나가게 돼요. 집결장들은 이전 방에 남으니 새 번호를 다시 공유해야 해요."
    override val newRoomTitle = "새 방 만들기"
    override fun newRoomBody(moving: String) = "원하는 방 번호를 4자리 이상 적어 주세요. 비워 두면 번호를 자동으로 정해요.$moving"
    override val newRoomHint = "방 번호 (비워 두면 자동)"
    override val roomExists = "이미 있는 방이에요. 방 목록에서 골라 주세요."
    override fun createFailed(err: String) = "방을 만들지 못했어요: $err"
    override fun roomCreated(code: String) = "새 방 $code 을 만들었어요. 집결장에게 번호를 공유하세요."
    override val previewTitle = "👤 일반 화면 (개발자 미리보기)"
    override val repPendingPrefix = "연맹 대표 승인 대기 중 · "
    override val previewPendingTail = "\n위쪽 탭을 눌러 돌아가서 '지휘관 관리'에서 승인하거나 거절해 볼 수 있어요."
    override val checkApproved = "승인됐는지 확인"
    override val previewBody = "일반 사용자가 보는 화면이에요. 인증·지휘관 로그인·연맹 대표 신청을 시험해 볼 수 있어요. 위쪽 탭을 눌러 돌아가세요."
    override val adminModeTitle = "👑 지휘관 모드"
    override val adminModeBody = "방을 만들고 집결장에게 방 번호와 집결장 코드를 공유하세요."
    override val leaderModeTitle = "🚩 집결장 모드"
    override val leaderModeBody = "지휘관으로 등록된 기기예요. 위쪽 버튼으로 코드 없이 지휘관으로 전환할 수 있어요."
    override val switchToCommander = "지휘관으로 전환"
    override val repPendingTitle = "⏳ 연맹 대표 승인 대기 중"
    override val repPendingTail = " · 개발자가 승인하면 지휘관 화면이 열려요. 아래 버튼으로 확인하거나 신청을 취소할 수 있어요."
    override val verifiedTitle = "✅ 집결장 인증 완료"
    override fun verifiedId(id: String) = "인증한 ID: $id"
    override val verifiedBody = "집결장 인증을 마쳤어요."
    override val changeAuth = "인증 변경"
    override val lockedTitle = "🔒 집결은 인증 후 사용"
    override val lockedBody = "연타는 바로 쓸 수 있어요. 집결은 집결장 코드로 인증하면 열려요."
    override val demoted = "지휘관 권한이 없어서 집결장 화면으로 돌아가요. 지휘관 코드를 받아 다시 로그인해 주세요."
    override val menuIssueCode = "집결장 코드 발급"
    override val menuAdmin = "지휘관 관리"
    override val devOnly = "개발자 전용"
    override val menuMyId = "내 기기 ID 보기"
    override val menuAlliance = "우리 연맹 지휘관"
    override val repOnly = "대표 전용"
    override val menuToLeader = "집결장으로 전환"
    override val verifyTitle = "집결장 코드 인증"
    override val verifyFilled = "복사해 둔 초대 글에서 채웠어요. 맞는지 보고 아래 버튼을 눌러 주세요."
    override val verifyBody = "지휘관에게 받은 글을 통째로 복사한 뒤 이 창을 열면 저절로 채워져요. 직접 적을 때는 ID, 집결장 코드, 소속을 받은 그대로 적어 주세요."
    override val hintEmail = "예: user@gmail.com"
    override val labelEmail = "이메일 또는 ID"
    override val hintCode = "예: AC-8F3K9A"
    override val labelCode = "집결장 코드 (AC- 뒤 6자리)"
    override val hintServer = "예: 2000"
    override val labelServer = "서버 번호"
    override val hintAlliance = "예: WBI"
    override val labelAlliance = "연맹 (대문자·소문자 구분)"
    override val verifySubmit = "인증 완료 및 시작하기"
    override val linkAdminLogin = "지휘관 로그인 (코드 발급 및 관리)"
    override val needEmail = "이메일 또는 ID를 입력해 주세요."
    override val needCode = "집결장 코드를 입력해 주세요."
    override fun verifiedGroup(label: String) = "집결장 인증을 마쳤어요. 소속은 ${label}이에요. 방을 골라 주세요."
    override val verifiedWelcome = "집결장 인증을 마쳤어요. 환영해요!"
    override val oldCode = "예전 방식의 코드예요. 지휘관에게 새 집결장 코드를 받아 주세요. (지휘관도 앱을 최신으로 올려야 해요)"
    override val codeMismatch = "코드가 맞지 않아요. ID, 코드, 소속(대문자·소문자)을 받은 그대로 적었는지 봐 주세요."
    override val loginIntroOwner = "개발자나 연맹 대표에게 받은 지휘관 코드를 입력해 주세요. 이미 지휘관이나 개발자로 등록된 기기는 칸을 비워 두고 확인을 누르면 됩니다."
    override val loginIntro = "연맹 대표에게 받은 지휘관 코드를 입력해 주세요. 이미 지휘관으로 등록된 기기는 칸을 비워 두고 확인을 누르면 됩니다."
    override val loginTitle = "지휘관 로그인"
    override val loginTail = " 우리 연맹에 대표가 아직 없다면 아래에서 신청할 수 있어요."
    override val loginHint = "지휘관 코드 (AD-XXXXXXXX)"
    override val linkMyId = "내 기기 ID"
    override val linkRepRequest = "우리 연맹 대표 신청하기"
    override val checkingCode = "지휘관 코드를 확인하는 중…"
    override val commanderRegistered = "지휘관으로 등록됐어요. 다음부터는 코드 없이 전환할 수 있어요."
    override val codePrefixError = "지휘관 코드는 AD- 로 시작해요. 개발자에게 받은 코드를 확인해 주세요."
    override val checkingOwner = "개발자 여부를 확인하는 중…"
    override val ownerOnly = "개발자로 등록된 기기만 쓸 수 있어요."
    override val serverCheckFailedNet = "서버에서 확인하지 못했어요. 네트워크를 확인하고 다시 눌러 주세요."
    override val toUserView = "일반 화면으로 바꿨어요."
    override val toDevView = "개발자 화면으로 돌아왔어요."
    override val toLeader = "집결장으로 전환했어요."
    override val checkingRoster = "지휘관 명단을 확인하는 중…"
    override val enteredOwner = "개발자로 들어왔어요."
    override val toCommander = "지휘관으로 전환했어요."
    override val notOnRoster = "지휘관 명단에 없는 기기예요. 지휘관 코드를 입력해 주세요."
    override val serverCheckFailed = "서버에서 확인하지 못했어요. 인터넷 연결을 확인해 주세요."
    override val issueBody = "집결장의 이메일이나 ID를 입력하면 그 사람만 쓸 수 있는 코드를 만들어요."
    override val issueHint = "집결장 이메일 또는 ID (예: friend@gmail.com)"
    override val makeCode = "집결장 코드 만들기"
    override val sendKakao = "카카오톡으로 보내기"
    override val sendOther = "다른 앱으로 보내기"
    override val needGroupForCode = "집결장 코드는 소속에 묶여요. 먼저 소속을 정해 주세요."
    override val noCodeSecret = "이 앱에는 집결장 코드 설정이 없어 만들 수 없어요."
    override val codeMade = "집결장 코드를 만들었어요."
    override val editRooms = "방 편집"
    override val pickRooms = "방 선택"
    override fun countN(n: Int) = "${n}개"
    override val noRooms = "들어갈 수 있는 방이 없어요. 지휘관이 방을 만들면 여기에 보여요."
    override val typeNumber = "번호 직접 입력"
    override val currentRoom = "현재 방"
    override val inUse = "사용 중 · 삭제 불가"
    override fun deleteRoomDesc(code: String) = "방 $code 삭제"
    override fun deleteRoomQ(code: String) = "방 ${code}를 삭제할까요?"
    override val deleting = "지우는 중…"
    override val deleteRoomBody = "방과 방 명단이 서버에서 지워져요. 되돌릴 수 없습니다."
    override fun badge(assigned: Int, teams: Int) = "배정 $assigned/$teams"
    override fun roomSummary(teams: String, assigned: String) = "군단 ${teams}개 · 배정 $assigned/$teams"
    override fun roomsFallbackNote(error: String) = "서버 방 목록을 불러오지 못했어요$error · 이 기기가 들어갔던 방만 보여요"
    override val roomsNone = "방 선택 · 만들어진 방이 없어요"
    override fun pickLine(code: String, teams: Int, assigned: Int) = "$code · 군단 $teams · 배정 $assigned"
    override val pickRunning = " · 집결 중"
    override val gServerEmpty = "서버 번호를 적어 주세요."
    override fun gServerBad(max: Int) = "서버 번호는 숫자만 ${max}자리까지 적어 주세요."
    override val gAllianceEmpty = "연맹을 적어 주세요."
    override fun gAllianceLong(max: Int) = "연맹은 ${max}자까지 적어 주세요."
    override val gAllianceChars = "연맹은 글자와 숫자만 적어 주세요. (띄어쓰기·기호 없이)"
    override fun roomsFull(max: Int) = "방은 최대 ${max}개까지 만들 수 있어요. 안 쓰는 방을 먼저 삭제해 주세요."
    override fun every(sec: String) = "${sec}초마다"
    override val repInfinite = "무한 반복"
    override fun repCount(n: Int) = "${n}회"
    override fun forDuration(d: String) = "$d 동안"
    override val discTitle = "접근성 API 사용 및 개인정보 보호 고지"
    override val quit = "종료"
    override val agree = "동의하고 계속하기"
    override val discBody = "[오토클리커 Pro]는 사용자가 지정한 위치에 자동 탭(터치) 제스처를 수행하기 위해 Android 접근성 서비스(AccessibilityService API)를 사용합니다.\n\n■ 사용 목적:\n• 게임 및 화면 위에서 지정된 좌표에 자동 클릭 제스처를 수행합니다.\n\n■ 데이터 보호 및 보안:\n• 화면의 텍스트나 입력 내용, 계정 및 금융 정보를 일체 수집하거나 읽지 않습니다.\n• 어떠한 개인정보도 기기 외부에 저장하거나 전송하지 않습니다.\n\n안전하고 편리한 자동 클릭 기능을 위해 위 내용에 동의해 주시기 바랍니다."
    override fun stall(sec: String, wall: String) = "화면 멈춤 감지: ${sec}초 ($wall)"
    override val clickNoPos = "⚠️ 클릭 안 함: 저장된 위치 없음"
    override val toastNoPos = "⚠ 저장된 클릭 위치가 없어 클릭하지 못했어요"
    override fun tapWhere(x: Int, y: Int, w: Int, h: Int, hasTarget: Boolean, armed: Boolean) = "탭 좌표 ($x, $y) · 화면 ${w}x$h · 과녁 ${if (hasTarget) "있음" else "없음"} · 투과 ${if (armed) "사전" else "즉시"}"
    override fun tapSending(where: String) = "탭 전송 중 · $where"
    override fun tapDone(where: String) = "✓ 탭 완료 · $where"
    override fun tapCancelled(where: String) = "✗ 탭 취소됨 · $where"
    override fun tapRefused(where: String) = "✗ 탭 전송 거부됨 · $where"
    override fun rallyClickToast(x: Int, y: Int) = "집결 클릭! ($x, $y)"
    override val clickSendFailed = "⚠ 클릭을 보내지 못했어요"
    override fun movedRoom(code: String) = "방 $code 로 옮겼어요"
    override val needAuth = "집결장 코드 인증이 필요해요."
    override val openAppJoin = "집결 방에 먼저 입장해 주세요. 앱 화면을 열게요"
    override val openAppGroup = "소속(서버·연맹)을 먼저 정해 주세요. 앱 화면을 열게요"
    override fun savedPos(x: Int, y: Int) = "저장된 클릭 위치 $x, $y"
    override val noPosHelp = "클릭 위치 없음 · 과녁을 집결 버튼 위에 놓고 저장하세요"
    override val posSavedToast = "현재 과녁 위치를 클릭 위치로 저장했어요"
    override fun otherAdminChanged(who: String) = "다른 지휘관($who)가 방을 바꿨어요"
    override val stoppedFromNoti = "알림창에서 연타를 멈췄어요."
    override val fullyOff = "오토클리커를 완전히 껐어요."
    override val serviceReady = "오토클리커 준비가 끝났어요. 앱에서 [띄우기]를 눌러 주세요."
    override val stoppedByVolume = "볼륨 키로 연타를 바로 멈췄어요."
    override val widgetFailed = "화면 위젯을 띄우지 못했어요. 다시 시도해 주세요."
    override val targetCentered = "과녁을 화면 가운데에 놓았어요."
    override val closedKeepRally = "화면만 닫았어요. 집결이 시작되면 자동으로 클릭해요."
    override val turnedOff = "오토클리커를 껐어요."
    override val targetShown = "과녁을 보이게 했어요."
    override val targetHidden = "과녁을 숨겼어요. (연타는 계속돼요)"
    override fun opacityToast(pct: Int) = "투명도 ${pct}%"
    override fun countTimes(n: Int) = "${n}회"
    override fun minSec(m: Int, s: Int) = "${m}분${s}초"
    override fun settingsApplied(interval: String, mode: String, pct: Int) = "⚙️ 설정 적용 완료! (${interval}초 / $mode / 투명도 ${pct}%)"
    override val stoppedTap = "치료 연타를 멈췄어요."
    override val targetNotReady = "과녁이 아직 준비되지 않았어요."
    override fun repeatN(n: Int) = "${n}회 반복"
    override fun timerDesc(m: Int, s: Int) = "${m}분 ${s}초 타이머"
    override fun tapStarted(desc: String) = "연타 시작 ($desc)"
    override fun countDone(n: Int) = "✓ ${n}회를 다 눌러서 자동으로 멈췄어요."
    override fun timerDone(m: Int, s: Int) = "✓ 타이머(${m}분 ${s}초)가 끝나서 자동으로 멈췄어요."
    override val notiChannel = "오토클리커 컨트롤러"
    override val notiChannelDesc = "오토클리커 실행 및 비상 정지 리모컨"
    override val notiRunning = "▶ 치료 연타 실행 중 (클릭중)"
    override val notiIdle = "⏹ 오토클리커 대기 중"
    override val notiRunningSub = "정지하려면 아래 [연타 정지]를 누르세요."
    override val notiIdleSub = "게임 화면의 컨트롤러로 조작하세요."
    override val notiStop = "⏹ 연타 정지"
    override val notiClose = "✕ 위젯 종료"
}

internal object AppEn : AppStr {
    override val later = "Later"
    override val download = "Download"
    override val apply = "Apply"
    override val save = "Save"
    override val verify = "Verify"
    override val manage = "Manage"
    override val edit = "Edit"
    override val delete = "Delete"
    override val create = "Create"
    override val enter = "Join"
    override val copy = "Copy"
    override val manual = "User guide"
    override fun secs(v: String) = "${v}s"
    override fun mins(n: Int) = "$n min"
    override val unitSec = "s"
    override val unitMin = "min"
    override val unitTimes = "times"
    override val updTitle = "New version available"
    override val updBody = "This update is required for the alliance. Download the new AutoClicker Pro now?"
    override val stoppedNow = "Tapping stopped."
    override val needAuthToast = "Available after you verify with a rally leader code."
    override val needSwitch = "Turn the switch on first so the app can tap the game for you."
    override val notReady = "Still getting ready. Try again in a moment.\n(If it keeps failing, turn Accessibility off and on again)"
    override val joinRoomFirst = "Join a rally room first."
    override val panelShown = "Rally panel opened."
    override val floaterHidden = "AutoClicker hidden."
    override val floaterShown = "AutoClicker is showing. Switching to the game."
    override val clickSettings = "Auto-tap settings"
    override val interval = "Tap interval"
    override val repeatCond = "Repeat"
    override val modeInfinite = "Endless"
    override val modeCount = "Count"
    override val modeTimer = "Timer"
    override val infiniteNote = "Keeps tapping until you press stop or a volume key."
    override val intervalError = "Enter a tap interval greater than 0 (e.g. 0.5)"
    override val findInList = "Find [AutoClicker Pro] in the list and turn it on."
    override val emergencyStop = "🛑 Stop tapping now"
    override val hideFloater = "✕ Hide AutoClicker"
    override val showFloater = "🚀 Show AutoClicker"
    override val generalMode = "Auto-tap mode"
    override val floatingNow = " · showing"
    override val groupByOthers = "A commander's group is set by the developer or the alliance rep. Ask them to change it."
    override val groupByCode = "Your group comes from the rally leader code you received. To change it, get a new code and verify again."
    override val setGroup = "Set group"
    override val groupFromCopied = "Group filled in from the text you copied. Check it and tap Save.\n"
    override val groupBody = "Rooms are visible only within the same group. Enter it exactly as your commander gave it.\nAlliance is case-sensitive (WBI and wbi are different groups)."
    override val serverHint = "Server number (e.g. 2000)"
    override val allianceHint = "Alliance (e.g. WBI)"
    override val groupCheck = "Check the group again."
    override fun groupSet(label: String) = "Group set to $label. Pick a room."
    override fun groupAssigned(label: String) = "Your group is now $label. Pick a room."
    override val pickOrCreate = "Pick or create a room."
    override val pickRoom = "Pick a room."
    override val roomHelpTitle = "Rally room"
    override val roomHelpBody = "Tap times are calculated so that all teams in the same room arrive at the castle together."
    override val createFirst = "Create a room first."
    override val loadingRooms = "Loading rooms…"
    override val alreadyHere = "You are already in this room."
    override fun deleteFailed(err: String) = "Could not delete the room: $err"
    override fun roomDeleted(code: String) = "Room $code deleted."
    override val enterCodeTitle = "Enter room number"
    override val enterCodeBody = "Enter the room number you were given."
    override val codeHint = "Room number (4+ digits)"
    override val codeTooShort = "Enter a room number with at least 4 digits."
    override val nameTitle = "Enter character name"
    override val nameBody = "The commander sees this name when assigning teams."
    override val nameHint = "Character name"
    override val nameRequired = "Enter a character name."
    override fun joined(code: String) = "Joined room $code."
    override val registering = "Registering on the list…"
    override fun registered(name: String) = "✓ Registered on the list ($name)"
    override fun registerFailed(err: String) = "✗ Registration failed: $err · pick the room again from Change room"
    override val checkingRoom = "Checking the room…"
    override val roomCheckFailedStatus = "Could not check the room on the server. Check your internet connection"
    override val roomCheckFailed = "Could not check the room. Check your internet connection."
    override fun noSuchRoomStatus(group: String) = "No such room. Check the group ($group) and the room number"
    override val noSuchRoom = "No such room. Check the group and the room number."
    override fun movingNote(current: String) = "\n\nYou will leave room $current. Rally leaders stay in the old room, so share the new number again."
    override val newRoomTitle = "Create room"
    override fun newRoomBody(moving: String) = "Enter a room number with at least 4 digits. Leave it empty to get one automatically.$moving"
    override val newRoomHint = "Room number (empty = automatic)"
    override val roomExists = "That room already exists. Pick it from the list."
    override fun createFailed(err: String) = "Could not create the room: $err"
    override fun roomCreated(code: String) = "Room $code created. Share the number with your rally leaders."
    override val previewTitle = "👤 User view (developer preview)"
    override val repPendingPrefix = "Alliance rep request pending · "
    override val previewPendingTail = "\nGo back with the tabs above to approve or reject it in Commander admin."
    override val checkApproved = "Check approval"
    override val previewBody = "This is what a regular user sees. You can try verification, commander login and the rep request. Use the tabs above to go back."
    override val adminModeTitle = "👑 Commander mode"
    override val adminModeBody = "Create a room and share the room number and rally leader codes."
    override val leaderModeTitle = "🚩 Rally leader mode"
    override val leaderModeBody = "This device is registered as a commander. Use the button above to switch without a code."
    override val switchToCommander = "Switch to commander"
    override val repPendingTitle = "⏳ Alliance rep request pending"
    override val repPendingTail = " · The commander screen opens once the developer approves. Use the button below to check or cancel."
    override val verifiedTitle = "✅ Rally leader verified"
    override fun verifiedId(id: String) = "Verified ID: $id"
    override val verifiedBody = "Rally leader verification is done."
    override val changeAuth = "Change"
    override val lockedTitle = "🔒 Rally needs verification"
    override val lockedBody = "Auto-tap works right away. Rally opens after you verify with a rally leader code."
    override val demoted = "You no longer have commander rights, so this goes back to the rally leader screen. Get a commander code and log in again."
    override val menuIssueCode = "Issue rally leader code"
    override val menuAdmin = "Commander admin"
    override val devOnly = "Developer only"
    override val menuMyId = "Show my device ID"
    override val menuAlliance = "Our alliance commanders"
    override val repOnly = "Rep only"
    override val menuToLeader = "Switch to rally leader"
    override val verifyTitle = "Verify rally leader code"
    override val verifyFilled = "Filled in from the invite you copied. Check it and tap the button below."
    override val verifyBody = "Copy the whole message from your commander, then open this window and it fills in by itself. If you type it, enter the ID, code and group exactly as given."
    override val hintEmail = "e.g. user@gmail.com"
    override val labelEmail = "Email or ID"
    override val hintCode = "e.g. AC-8F3K9A"
    override val labelCode = "Rally leader code (AC- plus 6 characters)"
    override val hintServer = "e.g. 2000"
    override val labelServer = "Server number"
    override val hintAlliance = "e.g. WBI"
    override val labelAlliance = "Alliance (case-sensitive)"
    override val verifySubmit = "Verify and start"
    override val linkAdminLogin = "Commander login (codes and management)"
    override val needEmail = "Enter your email or ID."
    override val needCode = "Enter the rally leader code."
    override fun verifiedGroup(label: String) = "Verified. Your group is $label. Pick a room."
    override val verifiedWelcome = "Verified. Welcome!"
    override val oldCode = "This is an old-style code. Ask your commander for a new one. (The commander must update the app too)"
    override val codeMismatch = "The code does not match. Check that the ID, code and group (upper/lower case) are exactly as given."
    override val loginIntroOwner = "Enter the commander code from the developer or your alliance rep. If this device is already a commander or developer, leave it empty and tap OK."
    override val loginIntro = "Enter the commander code from your alliance rep. If this device is already a commander, leave it empty and tap OK."
    override val loginTitle = "Commander login"
    override val loginTail = " If your alliance has no rep yet, you can apply below."
    override val loginHint = "Commander code (AD-XXXXXXXX)"
    override val linkMyId = "My device ID"
    override val linkRepRequest = "Apply as our alliance rep"
    override val checkingCode = "Checking the commander code…"
    override val commanderRegistered = "Registered as a commander. Next time you can switch without a code."
    override val codePrefixError = "Commander codes start with AD-. Check the code you were given."
    override val checkingOwner = "Checking developer status…"
    override val ownerOnly = "Only devices registered as developer can use this."
    override val serverCheckFailedNet = "Could not check with the server. Check the network and try again."
    override val toUserView = "Switched to user view."
    override val toDevView = "Back to developer view."
    override val toLeader = "Switched to rally leader."
    override val checkingRoster = "Checking the commander list…"
    override val enteredOwner = "Signed in as developer."
    override val toCommander = "Switched to commander."
    override val notOnRoster = "This device is not on the commander list. Enter a commander code."
    override val serverCheckFailed = "Could not check with the server. Check your internet connection."
    override val issueBody = "Enter the rally leader's email or ID to make a code only they can use."
    override val issueHint = "Rally leader's email or ID (e.g. friend@gmail.com)"
    override val makeCode = "Make rally leader code"
    override val sendKakao = "Send with KakaoTalk"
    override val sendOther = "Send with another app"
    override val needGroupForCode = "Rally leader codes are tied to a group. Set your group first."
    override val noCodeSecret = "This build has no rally leader code setup, so codes cannot be made."
    override val codeMade = "Rally leader code created."
    override val editRooms = "Edit rooms"
    override val pickRooms = "Rooms"
    override fun countN(n: Int) = "$n"
    override val noRooms = "No rooms to join yet. Rooms appear here once a commander creates them."
    override val typeNumber = "Enter a number"
    override val currentRoom = "Current room"
    override val inUse = "In use · cannot delete"
    override fun deleteRoomDesc(code: String) = "Delete room $code"
    override fun deleteRoomQ(code: String) = "Delete room $code?"
    override val deleting = "Deleting…"
    override val deleteRoomBody = "The room and its member list are removed from the server. This cannot be undone."
    override fun badge(assigned: Int, teams: Int) = "$assigned/$teams assigned"
    override fun roomSummary(teams: String, assigned: String) = "$teams teams · $assigned/$teams assigned"
    override fun roomsFallbackNote(error: String) = "Could not load the room list$error · showing only rooms this device has joined"
    override val roomsNone = "Rooms · none created yet"
    override fun pickLine(code: String, teams: Int, assigned: Int) = "$code · $teams teams · $assigned assigned"
    override val pickRunning = " · rallying"
    override val gServerEmpty = "Enter the server number."
    override fun gServerBad(max: Int) = "Server number: digits only, up to $max."
    override val gAllianceEmpty = "Enter the alliance."
    override fun gAllianceLong(max: Int) = "Alliance: up to $max characters."
    override val gAllianceChars = "Alliance: letters and digits only (no spaces or symbols)."
    override fun roomsFull(max: Int) = "You can have up to $max rooms. Delete an unused room first."
    override fun every(sec: String) = "every ${sec}s"
    override val repInfinite = "endless"
    override fun repCount(n: Int) = "$n times"
    override fun forDuration(d: String) = "for $d"
    override val discTitle = "Accessibility API use and privacy notice"
    override val quit = "Exit"
    override val agree = "Agree and continue"
    override val discBody = "[AutoClicker Pro] uses the Android accessibility service (AccessibilityService API) to perform automatic tap gestures at the positions you choose.\n\n■ Purpose:\n• Performs automatic tap gestures at the chosen coordinates on top of games and other screens.\n\n■ Data protection and security:\n• It does not collect or read any on-screen text, typed input, account or financial information.\n• No personal information is stored or sent outside the device.\n\nPlease agree to the above to use the automatic tap feature safely and conveniently."
    override fun stall(sec: String, wall: String) = "UI stall detected: ${sec}s ($wall)"
    override val clickNoPos = "⚠️ No tap: no saved position"
    override val toastNoPos = "⚠ No saved tap position, so nothing was tapped"
    override fun tapWhere(x: Int, y: Int, w: Int, h: Int, hasTarget: Boolean, armed: Boolean) = "Tap at ($x, $y) · screen ${w}x$h · target ${if (hasTarget) "yes" else "no"} · pass-through ${if (armed) "early" else "instant"}"
    override fun tapSending(where: String) = "Sending tap · $where"
    override fun tapDone(where: String) = "✓ Tap done · $where"
    override fun tapCancelled(where: String) = "✗ Tap cancelled · $where"
    override fun tapRefused(where: String) = "✗ Tap refused · $where"
    override fun rallyClickToast(x: Int, y: Int) = "Rally tap! ($x, $y)"
    override val clickSendFailed = "⚠ Could not send the tap"
    override fun movedRoom(code: String) = "Moved to room $code"
    override val needAuth = "Rally leader verification is required."
    override val openAppJoin = "Join a rally room first. Opening the app"
    override val openAppGroup = "Set your group (server · alliance) first. Opening the app"
    override fun savedPos(x: Int, y: Int) = "Saved tap position $x, $y"
    override val noPosHelp = "No tap position · put the target on the rally button and save"
    override val posSavedToast = "Saved the target's position as the tap position"
    override fun otherAdminChanged(who: String) = "Another commander ($who) changed the room"
    override val stoppedFromNoti = "Tapping stopped from the notification."
    override val fullyOff = "AutoClicker turned off completely."
    override val serviceReady = "AutoClicker is ready. Tap [Show AutoClicker] in the app."
    override val stoppedByVolume = "Tapping stopped with the volume key."
    override val widgetFailed = "Could not show the overlay. Try again."
    override val targetCentered = "Target moved to the center of the screen."
    override val closedKeepRally = "Only the overlay was closed. The rally tap will still happen automatically."
    override val turnedOff = "AutoClicker turned off."
    override val targetShown = "Target is visible."
    override val targetHidden = "Target hidden. (Tapping continues)"
    override fun opacityToast(pct: Int) = "Opacity $pct%"
    override fun countTimes(n: Int) = "$n times"
    override fun minSec(m: Int, s: Int) = "${m}m ${s}s"
    override fun settingsApplied(interval: String, mode: String, pct: Int) = "⚙️ Settings applied! (${interval}s / $mode / opacity $pct%)"
    override val stoppedTap = "Tapping stopped."
    override val targetNotReady = "The target is not ready yet."
    override fun repeatN(n: Int) = "$n times"
    override fun timerDesc(m: Int, s: Int) = "${m}m ${s}s timer"
    override fun tapStarted(desc: String) = "Tapping started ($desc)"
    override fun countDone(n: Int) = "✓ Stopped automatically after $n taps."
    override fun timerDone(m: Int, s: Int) = "✓ Stopped automatically when the timer (${m}m ${s}s) ended."
    override val notiChannel = "AutoClicker controller"
    override val notiChannelDesc = "Run and emergency-stop control for AutoClicker"
    override val notiRunning = "▶ Tapping in progress"
    override val notiIdle = "⏹ AutoClicker idle"
    override val notiRunningSub = "Tap [Stop tapping] below to stop."
    override val notiIdleSub = "Use the controller on the game screen."
    override val notiStop = "⏹ Stop tapping"
    override val notiClose = "✕ Close overlay"
}

internal object AppZh : AppStr {
    override val later = "以后再说"
    override val download = "下载"
    override val apply = "应用"
    override val save = "保存"
    override val verify = "验证"
    override val manage = "管理"
    override val edit = "编辑"
    override val delete = "删除"
    override val create = "创建"
    override val enter = "进入"
    override val copy = "复制"
    override val manual = "使用说明"
    override fun secs(v: String) = "${v}秒"
    override fun mins(n: Int) = "${n}分钟"
    override val unitSec = "秒"
    override val unitMin = "分"
    override val unitTimes = "次"
    override val updTitle = "有新版本"
    override val updBody = "这是联盟必须的更新。现在下载新版 AutoClicker Pro 吗？"
    override val stoppedNow = "已立即停止连点。"
    override val needAuthToast = "用集结队长代码验证后才能使用。"
    override val needSwitch = "请先打开开关，应用才能自动点击游戏。"
    override val notReady = "还在准备中，请稍后再点。\n(如果一直不行，请把无障碍关掉再打开)"
    override val joinRoomFirst = "请先进入集结房间。"
    override val panelShown = "已打开集结面板。"
    override val floaterHidden = "已隐藏 AutoClicker。"
    override val floaterShown = "已显示 AutoClicker，即将切换到游戏。"
    override val clickSettings = "连点设置"
    override val interval = "点击间隔"
    override val repeatCond = "重复条件"
    override val modeInfinite = "无限"
    override val modeCount = "次数"
    override val modeTimer = "时间"
    override val infiniteNote = "一直连点，直到按下停止按钮或音量键。"
    override val intervalError = "请输入大于 0 的点击间隔（例如 0.5）"
    override val findInList = "请在列表中找到 [AutoClicker Pro] 并打开。"
    override val emergencyStop = "🛑 立即停止连点"
    override val hideFloater = "✕ 隐藏 AutoClicker"
    override val showFloater = "🚀 显示 AutoClicker"
    override val generalMode = "普通连点模式"
    override val floatingNow = " · 显示中"
    override val groupByOthers = "指挥官的所属由开发者或联盟代表决定。要更改请联系他们。"
    override val groupByCode = "所属由指挥官给你的集结队长代码决定。要更改请拿到新代码后重新验证。"
    override val setGroup = "设定所属"
    override val groupFromCopied = "已从复制的文字中读取所属。确认无误后请点保存。\n"
    override val groupBody = "只能看到相同所属的房间。请按指挥官给的原样填写。\n联盟区分大小写（WBI 和 wbi 是不同的所属）。"
    override val serverHint = "服务器编号（例如 2000）"
    override val allianceHint = "联盟（例如 WBI）"
    override val groupCheck = "请再确认一下所属。"
    override fun groupSet(label: String) = "所属已设为 ${label}。请选择房间。"
    override fun groupAssigned(label: String) = "你的所属已定为 ${label}。请选择房间。"
    override val pickOrCreate = "请选择或创建房间。"
    override val pickRoom = "请选择房间。"
    override val roomHelpTitle = "集结房间"
    override val roomHelpBody = "系统会自动计算集结点击时间，让同一房间的队伍同时到达城堡。"
    override val createFirst = "请先创建房间。"
    override val loadingRooms = "正在加载房间列表…"
    override val alreadyHere = "你现在就在这个房间。"
    override fun deleteFailed(err: String) = "无法删除房间：${err}"
    override fun roomDeleted(code: String) = "已删除房间 ${code}。"
    override val enterCodeTitle = "输入房间号"
    override val enterCodeBody = "请输入你收到的房间号。"
    override val codeHint = "房间号（4位以上）"
    override val codeTooShort = "请输入4位以上的房间号。"
    override val nameTitle = "输入角色名"
    override val nameBody = "指挥官分配队伍时会看到这个名字。"
    override val nameHint = "角色名"
    override val nameRequired = "请输入角色名。"
    override fun joined(code: String) = "已进入房间 ${code}。"
    override val registering = "正在登记到名单…"
    override fun registered(name: String) = "✓ 已登记到名单 (${name})"
    override fun registerFailed(err: String) = "✗ 登记名单失败：${err} · 请在“更换房间”中重新选择"
    override val checkingRoom = "正在确认房间…"
    override val roomCheckFailedStatus = "无法在服务器上确认房间。请检查网络连接"
    override val roomCheckFailed = "无法确认房间。请检查网络连接。"
    override fun noSuchRoomStatus(group: String) = "没有这个房间。请确认所属（${group}）和房间号"
    override val noSuchRoom = "没有这个房间。请确认所属和房间号。"
    override fun movingNote(current: String) = "\n\n你将离开现在的房间 ${current}。集结队长会留在原来的房间，需要重新分享新的房间号。"
    override val newRoomTitle = "创建新房间"
    override fun newRoomBody(moving: String) = "请填写4位以上的房间号。留空则自动生成。${moving}"
    override val newRoomHint = "房间号（留空则自动）"
    override val roomExists = "这个房间已经存在。请在房间列表中选择。"
    override fun createFailed(err: String) = "无法创建房间：${err}"
    override fun roomCreated(code: String) = "已创建新房间 ${code}。请把房间号分享给集结队长。"
    override val previewTitle = "👤 普通画面（开发者预览）"
    override val repPendingPrefix = "联盟代表等待批准 · "
    override val previewPendingTail = "\n点上方的标签返回，可以在“指挥官管理”中批准或拒绝。"
    override val checkApproved = "查看是否已批准"
    override val previewBody = "这是普通用户看到的画面。可以试用验证、指挥官登录和联盟代表申请。点上方的标签返回。"
    override val adminModeTitle = "👑 指挥官模式"
    override val adminModeBody = "创建房间，并把房间号和集结队长代码分享给集结队长。"
    override val leaderModeTitle = "🚩 集结队长模式"
    override val leaderModeBody = "这台设备已登记为指挥官。用上方的按钮可以不输入代码直接切换为指挥官。"
    override val switchToCommander = "切换为指挥官"
    override val repPendingTitle = "⏳ 联盟代表等待批准"
    override val repPendingTail = " · 开发者批准后会打开指挥官画面。可以用下面的按钮查看或取消申请。"
    override val verifiedTitle = "✅ 集结队长验证完成"
    override fun verifiedId(id: String) = "已验证的 ID：${id}"
    override val verifiedBody = "集结队长验证已完成。"
    override val changeAuth = "更改验证"
    override val lockedTitle = "🔒 集结需验证后使用"
    override val lockedBody = "连点可以直接使用。集结在用集结队长代码验证后开放。"
    override val demoted = "你没有指挥官权限，将回到集结队长画面。请拿到指挥官代码后重新登录。"
    override val menuIssueCode = "发放集结队长代码"
    override val menuAdmin = "指挥官管理"
    override val devOnly = "开发者专用"
    override val menuMyId = "查看我的设备 ID"
    override val menuAlliance = "本联盟指挥官"
    override val repOnly = "代表专用"
    override val menuToLeader = "切换为集结队长"
    override val verifyTitle = "验证集结队长代码"
    override val verifyFilled = "已从复制的邀请文字中填好。确认无误后请点下面的按钮。"
    override val verifyBody = "把指挥官发来的文字整段复制后再打开这个窗口，就会自动填好。手动填写时，请按原样填写 ID、集结队长代码和所属。"
    override val hintEmail = "例如 user@gmail.com"
    override val labelEmail = "邮箱或 ID"
    override val hintCode = "例如 AC-8F3K9A"
    override val labelCode = "集结队长代码（AC- 后6位）"
    override val hintServer = "例如 2000"
    override val labelServer = "服务器编号"
    override val hintAlliance = "例如 WBI"
    override val labelAlliance = "联盟（区分大小写）"
    override val verifySubmit = "完成验证并开始"
    override val linkAdminLogin = "指挥官登录（发放代码与管理）"
    override val needEmail = "请输入邮箱或 ID。"
    override val needCode = "请输入集结队长代码。"
    override fun verifiedGroup(label: String) = "集结队长验证已完成。你的所属是 ${label}。请选择房间。"
    override val verifiedWelcome = "集结队长验证已完成。欢迎！"
    override val oldCode = "这是旧版的代码。请向指挥官索取新的集结队长代码。（指挥官也需要把应用更新到最新版）"
    override val codeMismatch = "代码不正确。请确认 ID、代码和所属（大小写）是否按原样填写。"
    override val loginIntroOwner = "请输入开发者或联盟代表给你的指挥官代码。已登记为指挥官或开发者的设备，留空后点确定即可。"
    override val loginIntro = "请输入联盟代表给你的指挥官代码。已登记为指挥官的设备，留空后点确定即可。"
    override val loginTitle = "指挥官登录"
    override val loginTail = " 如果你的联盟还没有代表，可以在下面申请。"
    override val loginHint = "指挥官代码 (AD-XXXXXXXX)"
    override val linkMyId = "我的设备 ID"
    override val linkRepRequest = "申请成为本联盟代表"
    override val checkingCode = "正在确认指挥官代码…"
    override val commanderRegistered = "已登记为指挥官。下次可以不输入代码直接切换。"
    override val codePrefixError = "指挥官代码以 AD- 开头。请确认你收到的代码。"
    override val checkingOwner = "正在确认是否为开发者…"
    override val ownerOnly = "只有登记为开发者的设备才能使用。"
    override val serverCheckFailedNet = "无法向服务器确认。请检查网络后再点一次。"
    override val toUserView = "已切换到普通画面。"
    override val toDevView = "已回到开发者画面。"
    override val toLeader = "已切换为集结队长。"
    override val checkingRoster = "正在确认指挥官名单…"
    override val enteredOwner = "已以开发者身份进入。"
    override val toCommander = "已切换为指挥官。"
    override val notOnRoster = "这台设备不在指挥官名单中。请输入指挥官代码。"
    override val serverCheckFailed = "无法向服务器确认。请检查网络连接。"
    override val issueBody = "输入集结队长的邮箱或 ID，就会生成只有那个人能用的代码。"
    override val issueHint = "集结队长的邮箱或 ID（例如 friend@gmail.com）"
    override val makeCode = "生成集结队长代码"
    override val sendKakao = "用 KakaoTalk 发送"
    override val sendOther = "用其他应用发送"
    override val needGroupForCode = "集结队长代码与所属绑定。请先设定所属。"
    override val noCodeSecret = "这个应用没有集结队长代码的设置，无法生成。"
    override val codeMade = "已生成集结队长代码。"
    override val editRooms = "编辑房间"
    override val pickRooms = "选择房间"
    override fun countN(n: Int) = "${n}个"
    override val noRooms = "没有可以进入的房间。指挥官创建房间后会显示在这里。"
    override val typeNumber = "直接输入房间号"
    override val currentRoom = "当前房间"
    override val inUse = "使用中 · 不能删除"
    override fun deleteRoomDesc(code: String) = "删除房间 ${code}"
    override fun deleteRoomQ(code: String) = "要删除房间 ${code} 吗？"
    override val deleting = "正在删除…"
    override val deleteRoomBody = "房间和房间名单会从服务器上删除，无法恢复。"
    override fun badge(assigned: Int, teams: Int) = "已分配 ${assigned}/${teams}"
    override fun roomSummary(teams: String, assigned: String) = "队伍${teams}支 · 已分配 ${assigned}/${teams}"
    override fun roomsFallbackNote(error: String) = "无法加载服务器上的房间列表${error} · 只显示这台设备进入过的房间"
    override val roomsNone = "选择房间 · 还没有创建的房间"
    override fun pickLine(code: String, teams: Int, assigned: Int) = "${code} · 队伍 ${teams} · 已分配 ${assigned}"
    override val pickRunning = " · 集结中"
    override val gServerEmpty = "请填写服务器编号。"
    override fun gServerBad(max: Int) = "服务器编号只能是数字，最多${max}位。"
    override val gAllianceEmpty = "请填写联盟。"
    override fun gAllianceLong(max: Int) = "联盟最多${max}个字符。"
    override val gAllianceChars = "联盟只能填写字母和数字。（不能有空格和符号）"
    override fun roomsFull(max: Int) = "最多只能创建${max}个房间。请先删除不用的房间。"
    override fun every(sec: String) = "每${sec}秒"
    override val repInfinite = "无限重复"
    override fun repCount(n: Int) = "${n}次"
    override fun forDuration(d: String) = "持续 ${d}"
    override val discTitle = "无障碍 API 使用及隐私保护说明"
    override val quit = "退出"
    override val agree = "同意并继续"
    override val discBody = "[AutoClicker Pro] 使用 Android 无障碍服务（AccessibilityService API），在你指定的位置执行自动点击（触摸）手势。\n\n■ 使用目的：\n• 在游戏及其他画面上，对指定的坐标执行自动点击手势。\n\n■ 数据保护与安全：\n• 完全不收集、不读取画面上的文字、输入内容、账号及金融信息。\n• 不会把任何个人信息保存或传送到设备之外。\n\n为了安全、方便地使用自动点击功能，请同意以上内容。"
    override fun stall(sec: String, wall: String) = "检测到画面卡顿：${sec}秒 (${wall})"
    override val clickNoPos = "⚠️ 未点击：没有保存的位置"
    override val toastNoPos = "⚠ 没有保存的点击位置，无法点击"
    override fun tapWhere(x: Int, y: Int, w: Int, h: Int, hasTarget: Boolean, armed: Boolean) = "点击坐标 (${x}, ${y}) · 屏幕 ${w}x${h} · 准星 ${if (hasTarget) "有" else "无"} · 穿透 ${if (armed) "预先" else "即时"}"
    override fun tapSending(where: String) = "正在发送点击 · ${where}"
    override fun tapDone(where: String) = "✓ 点击完成 · ${where}"
    override fun tapCancelled(where: String) = "✗ 点击被取消 · ${where}"
    override fun tapRefused(where: String) = "✗ 点击被拒绝 · ${where}"
    override fun rallyClickToast(x: Int, y: Int) = "集结点击！(${x}, ${y})"
    override val clickSendFailed = "⚠ 无法发送点击"
    override fun movedRoom(code: String) = "已移到房间 ${code}"
    override val needAuth = "需要验证集结队长代码。"
    override val openAppJoin = "请先进入集结房间。即将打开应用"
    override val openAppGroup = "请先设定所属（服务器·联盟）。即将打开应用"
    override fun savedPos(x: Int, y: Int) = "已保存的点击位置 ${x}, ${y}"
    override val noPosHelp = "没有点击位置 · 请把准星放在集结按钮上再保存"
    override val posSavedToast = "已把当前准星位置保存为点击位置"
    override fun otherAdminChanged(who: String) = "其他指挥官（${who}）更换了房间"
    override val stoppedFromNoti = "已从通知栏停止连点。"
    override val fullyOff = "已完全关闭 AutoClicker。"
    override val serviceReady = "AutoClicker 已准备好。请在应用中点 [显示 AutoClicker]。"
    override val stoppedByVolume = "已用音量键立即停止连点。"
    override val widgetFailed = "无法显示悬浮窗。请再试一次。"
    override val targetCentered = "已把准星放到屏幕中央。"
    override val closedKeepRally = "只关闭了画面。集结开始时仍会自动点击。"
    override val turnedOff = "已关闭 AutoClicker。"
    override val targetShown = "已显示准星。"
    override val targetHidden = "已隐藏准星。（连点仍在继续）"
    override fun opacityToast(pct: Int) = "透明度 ${pct}%"
    override fun countTimes(n: Int) = "${n}次"
    override fun minSec(m: Int, s: Int) = "${m}分${s}秒"
    override fun settingsApplied(interval: String, mode: String, pct: Int) = "⚙️ 设置已应用！(${interval}秒 / ${mode} / 透明度 ${pct}%)"
    override val stoppedTap = "已停止连点。"
    override val targetNotReady = "准星还没有准备好。"
    override fun repeatN(n: Int) = "重复${n}次"
    override fun timerDesc(m: Int, s: Int) = "${m}分${s}秒计时"
    override fun tapStarted(desc: String) = "开始连点 (${desc})"
    override fun countDone(n: Int) = "✓ 已点满${n}次，自动停止。"
    override fun timerDone(m: Int, s: Int) = "✓ 计时（${m}分${s}秒）结束，自动停止。"
    override val notiChannel = "AutoClicker 控制器"
    override val notiChannelDesc = "AutoClicker 运行与紧急停止遥控"
    override val notiRunning = "▶ 正在连点（点击中）"
    override val notiIdle = "⏹ AutoClicker 待机中"
    override val notiRunningSub = "要停止请点下面的 [停止连点]。"
    override val notiIdleSub = "请用游戏画面上的控制器操作。"
    override val notiStop = "⏹ 停止连点"
    override val notiClose = "✕ 关闭悬浮窗"
}

internal object AppZhTw : AppStr {
    override val later = "以後再說"
    override val download = "下載"
    override val apply = "套用"
    override val save = "儲存"
    override val verify = "驗證"
    override val manage = "管理"
    override val edit = "編輯"
    override val delete = "刪除"
    override val create = "建立"
    override val enter = "進入"
    override val copy = "複製"
    override val manual = "使用說明"
    override fun secs(v: String) = "${v}秒"
    override fun mins(n: Int) = "${n}分鐘"
    override val unitSec = "秒"
    override val unitMin = "分"
    override val unitTimes = "次"
    override val updTitle = "有新版本"
    override val updBody = "這是聯盟必須的更新。現在下載新版 AutoClicker Pro 嗎？"
    override val stoppedNow = "已立即停止連點。"
    override val needAuthToast = "用集結隊長代碼驗證後才能使用。"
    override val needSwitch = "請先開啟開關，應用程式才能自動點擊遊戲。"
    override val notReady = "還在準備中，請稍後再點。\n(如果一直不行，請把無障礙關掉再開啟)"
    override val joinRoomFirst = "請先進入集結房間。"
    override val panelShown = "已開啟集結面板。"
    override val floaterHidden = "已隱藏 AutoClicker。"
    override val floaterShown = "已顯示 AutoClicker，即將切換到遊戲。"
    override val clickSettings = "連點設定"
    override val interval = "點擊間隔"
    override val repeatCond = "重複條件"
    override val modeInfinite = "無限"
    override val modeCount = "次數"
    override val modeTimer = "時間"
    override val infiniteNote = "一直連點，直到按下停止按鈕或音量鍵。"
    override val intervalError = "請輸入大於 0 的點擊間隔（例如 0.5）"
    override val findInList = "請在列表中找到 [AutoClicker Pro] 並開啟。"
    override val emergencyStop = "🛑 立即停止連點"
    override val hideFloater = "✕ 隱藏 AutoClicker"
    override val showFloater = "🚀 顯示 AutoClicker"
    override val generalMode = "普通連點模式"
    override val floatingNow = " · 顯示中"
    override val groupByOthers = "指揮官的所屬由開發者或聯盟代表決定。要更改請聯絡他們。"
    override val groupByCode = "所屬由指揮官給你的集結隊長代碼決定。要更改請拿到新代碼後重新驗證。"
    override val setGroup = "設定所屬"
    override val groupFromCopied = "已從複製的文字中讀取所屬。確認無誤後請點儲存。\n"
    override val groupBody = "只能看到相同所屬的房間。請按指揮官給的原樣填寫。\n聯盟區分大小寫（WBI 和 wbi 是不同的所屬）。"
    override val serverHint = "伺服器編號（例如 2000）"
    override val allianceHint = "聯盟（例如 WBI）"
    override val groupCheck = "請再確認一下所屬。"
    override fun groupSet(label: String) = "所屬已設為 ${label}。請選擇房間。"
    override fun groupAssigned(label: String) = "你的所屬已定為 ${label}。請選擇房間。"
    override val pickOrCreate = "請選擇或建立房間。"
    override val pickRoom = "請選擇房間。"
    override val roomHelpTitle = "集結房間"
    override val roomHelpBody = "系統會自動計算集結點擊時間，讓同一房間的隊伍同時到達城堡。"
    override val createFirst = "請先建立房間。"
    override val loadingRooms = "正在載入房間列表…"
    override val alreadyHere = "你現在就在這個房間。"
    override fun deleteFailed(err: String) = "無法刪除房間：${err}"
    override fun roomDeleted(code: String) = "已刪除房間 ${code}。"
    override val enterCodeTitle = "輸入房間號"
    override val enterCodeBody = "請輸入你收到的房間號。"
    override val codeHint = "房間號（4位以上）"
    override val codeTooShort = "請輸入4位以上的房間號。"
    override val nameTitle = "輸入角色名"
    override val nameBody = "指揮官分配隊伍時會看到這個名字。"
    override val nameHint = "角色名"
    override val nameRequired = "請輸入角色名。"
    override fun joined(code: String) = "已進入房間 ${code}。"
    override val registering = "正在登記到名單…"
    override fun registered(name: String) = "✓ 已登記到名單 (${name})"
    override fun registerFailed(err: String) = "✗ 登記名單失敗：${err} · 請在「更換房間」中重新選擇"
    override val checkingRoom = "正在確認房間…"
    override val roomCheckFailedStatus = "無法在伺服器上確認房間。請檢查網路連線"
    override val roomCheckFailed = "無法確認房間。請檢查網路連線。"
    override fun noSuchRoomStatus(group: String) = "沒有這個房間。請確認所屬（${group}）和房間號"
    override val noSuchRoom = "沒有這個房間。請確認所屬和房間號。"
    override fun movingNote(current: String) = "\n\n你將離開現在的房間 ${current}。集結隊長會留在原來的房間，需要重新分享新的房間號。"
    override val newRoomTitle = "建立新房間"
    override fun newRoomBody(moving: String) = "請填寫4位以上的房間號。留空則自動生成。${moving}"
    override val newRoomHint = "房間號（留空則自動）"
    override val roomExists = "這個房間已經存在。請在房間列表中選擇。"
    override fun createFailed(err: String) = "無法建立房間：${err}"
    override fun roomCreated(code: String) = "已建立新房間 ${code}。請把房間號分享給集結隊長。"
    override val previewTitle = "👤 普通畫面（開發者預覽）"
    override val repPendingPrefix = "聯盟代表等待批准 · "
    override val previewPendingTail = "\n點上方的標籤返回，可以在「指揮官管理」中批准或拒絕。"
    override val checkApproved = "檢視是否已批准"
    override val previewBody = "這是普通使用者看到的畫面。可以試用驗證、指揮官登入和聯盟代表申請。點上方的標籤返回。"
    override val adminModeTitle = "👑 指揮官模式"
    override val adminModeBody = "建立房間，並把房間號和集結隊長代碼分享給集結隊長。"
    override val leaderModeTitle = "🚩 集結隊長模式"
    override val leaderModeBody = "這台裝置已登記為指揮官。用上方的按鈕可以不輸入代碼直接切換為指揮官。"
    override val switchToCommander = "切換為指揮官"
    override val repPendingTitle = "⏳ 聯盟代表等待批准"
    override val repPendingTail = " · 開發者批准後會開啟指揮官畫面。可以用下面的按鈕檢視或取消申請。"
    override val verifiedTitle = "✅ 集結隊長驗證完成"
    override fun verifiedId(id: String) = "已驗證的 ID：${id}"
    override val verifiedBody = "集結隊長驗證已完成。"
    override val changeAuth = "更改驗證"
    override val lockedTitle = "🔒 集結需驗證後使用"
    override val lockedBody = "連點可以直接使用。集結在用集結隊長代碼驗證後開放。"
    override val demoted = "你沒有指揮官權限，將回到集結隊長畫面。請拿到指揮官代碼後重新登入。"
    override val menuIssueCode = "發放集結隊長代碼"
    override val menuAdmin = "指揮官管理"
    override val devOnly = "開發者專用"
    override val menuMyId = "檢視我的裝置 ID"
    override val menuAlliance = "本聯盟指揮官"
    override val repOnly = "代表專用"
    override val menuToLeader = "切換為集結隊長"
    override val verifyTitle = "驗證集結隊長代碼"
    override val verifyFilled = "已從複製的邀請文字中填好。確認無誤後請點下面的按鈕。"
    override val verifyBody = "把指揮官發來的文字整段複製後再開啟這個視窗，就會自動填好。手動填寫時，請按原樣填寫 ID、集結隊長代碼和所屬。"
    override val hintEmail = "例如 user@gmail.com"
    override val labelEmail = "電子郵件或 ID"
    override val hintCode = "例如 AC-8F3K9A"
    override val labelCode = "集結隊長代碼（AC- 後6位）"
    override val hintServer = "例如 2000"
    override val labelServer = "伺服器編號"
    override val hintAlliance = "例如 WBI"
    override val labelAlliance = "聯盟（區分大小寫）"
    override val verifySubmit = "完成驗證並開始"
    override val linkAdminLogin = "指揮官登入（發放代碼與管理）"
    override val needEmail = "請輸入電子郵件或 ID。"
    override val needCode = "請輸入集結隊長代碼。"
    override fun verifiedGroup(label: String) = "集結隊長驗證已完成。你的所屬是 ${label}。請選擇房間。"
    override val verifiedWelcome = "集結隊長驗證已完成。歡迎！"
    override val oldCode = "這是舊版的代碼。請向指揮官索取新的集結隊長代碼。（指揮官也需要把應用程式更新到最新版）"
    override val codeMismatch = "代碼不正確。請確認 ID、代碼和所屬（大小寫）是否按原樣填寫。"
    override val loginIntroOwner = "請輸入開發者或聯盟代表給你的指揮官代碼。已登記為指揮官或開發者的裝置，留空後點確定即可。"
    override val loginIntro = "請輸入聯盟代表給你的指揮官代碼。已登記為指揮官的裝置，留空後點確定即可。"
    override val loginTitle = "指揮官登入"
    override val loginTail = " 如果你的聯盟還沒有代表，可以在下面申請。"
    override val loginHint = "指揮官代碼 (AD-XXXXXXXX)"
    override val linkMyId = "我的裝置 ID"
    override val linkRepRequest = "申請成為本聯盟代表"
    override val checkingCode = "正在確認指揮官代碼…"
    override val commanderRegistered = "已登記為指揮官。下次可以不輸入代碼直接切換。"
    override val codePrefixError = "指揮官代碼以 AD- 開頭。請確認你收到的代碼。"
    override val checkingOwner = "正在確認是否為開發者…"
    override val ownerOnly = "只有登記為開發者的裝置才能使用。"
    override val serverCheckFailedNet = "無法向伺服器確認。請檢查網路後再點一次。"
    override val toUserView = "已切換到普通畫面。"
    override val toDevView = "已回到開發者畫面。"
    override val toLeader = "已切換為集結隊長。"
    override val checkingRoster = "正在確認指揮官名單…"
    override val enteredOwner = "已以開發者身份進入。"
    override val toCommander = "已切換為指揮官。"
    override val notOnRoster = "這台裝置不在指揮官名單中。請輸入指揮官代碼。"
    override val serverCheckFailed = "無法向伺服器確認。請檢查網路連線。"
    override val issueBody = "輸入集結隊長的電子郵件或 ID，就會生成只有那個人能用的代碼。"
    override val issueHint = "集結隊長的電子郵件或 ID（例如 friend@gmail.com）"
    override val makeCode = "生成集結隊長代碼"
    override val sendKakao = "用 KakaoTalk 傳送"
    override val sendOther = "用其他應用程式傳送"
    override val needGroupForCode = "集結隊長代碼與所屬綁定。請先設定所屬。"
    override val noCodeSecret = "這個應用程式沒有集結隊長代碼的設定，無法生成。"
    override val codeMade = "已生成集結隊長代碼。"
    override val editRooms = "編輯房間"
    override val pickRooms = "選擇房間"
    override fun countN(n: Int) = "${n}個"
    override val noRooms = "沒有可以進入的房間。指揮官建立房間後會顯示在這裡。"
    override val typeNumber = "直接輸入房間號"
    override val currentRoom = "當前房間"
    override val inUse = "使用中 · 不能刪除"
    override fun deleteRoomDesc(code: String) = "刪除房間 ${code}"
    override fun deleteRoomQ(code: String) = "要刪除房間 ${code} 嗎？"
    override val deleting = "正在刪除…"
    override val deleteRoomBody = "房間和房間名單會從伺服器上刪除，無法恢復。"
    override fun badge(assigned: Int, teams: Int) = "已分配 ${assigned}/${teams}"
    override fun roomSummary(teams: String, assigned: String) = "隊伍${teams}支 · 已分配 ${assigned}/${teams}"
    override fun roomsFallbackNote(error: String) = "無法載入伺服器上的房間列表${error} · 只顯示這台裝置進入過的房間"
    override val roomsNone = "選擇房間 · 還沒有建立的房間"
    override fun pickLine(code: String, teams: Int, assigned: Int) = "${code} · 隊伍 ${teams} · 已分配 ${assigned}"
    override val pickRunning = " · 集結中"
    override val gServerEmpty = "請填寫伺服器編號。"
    override fun gServerBad(max: Int) = "伺服器編號只能是數字，最多${max}位。"
    override val gAllianceEmpty = "請填寫聯盟。"
    override fun gAllianceLong(max: Int) = "聯盟最多${max}個字元。"
    override val gAllianceChars = "聯盟只能填寫字母和數字。（不能有空格和符號）"
    override fun roomsFull(max: Int) = "最多隻能建立${max}個房間。請先刪除不用的房間。"
    override fun every(sec: String) = "每${sec}秒"
    override val repInfinite = "無限重複"
    override fun repCount(n: Int) = "${n}次"
    override fun forDuration(d: String) = "持續 ${d}"
    override val discTitle = "無障礙 API 使用及隱私保護說明"
    override val quit = "退出"
    override val agree = "同意並繼續"
    override val discBody = "[AutoClicker Pro] 使用 Android 無障礙服務（AccessibilityService API），在你指定的位置執行自動點擊（觸控）手勢。\n\n■ 使用目的：\n• 在遊戲及其他畫面上，對指定的座標執行自動點擊手勢。\n\n■ 資料保護與安全：\n• 完全不收集、不讀取畫面上的文字、輸入內容、賬號及金融資訊。\n• 不會把任何個人資訊儲存或傳送到裝置之外。\n\n為了安全、方便地使用自動點擊功能，請同意以上內容。"
    override fun stall(sec: String, wall: String) = "檢測到畫面卡頓：${sec}秒 (${wall})"
    override val clickNoPos = "⚠️ 未點擊：沒有儲存的位置"
    override val toastNoPos = "⚠ 沒有儲存的點擊位置，無法點擊"
    override fun tapWhere(x: Int, y: Int, w: Int, h: Int, hasTarget: Boolean, armed: Boolean) = "點擊座標 (${x}, ${y}) · 螢幕 ${w}x${h} · 準星 ${if (hasTarget) "有" else "無"} · 穿透 ${if (armed) "預先" else "即時"}"
    override fun tapSending(where: String) = "正在傳送點擊 · ${where}"
    override fun tapDone(where: String) = "✓ 點擊完成 · ${where}"
    override fun tapCancelled(where: String) = "✗ 點擊被取消 · ${where}"
    override fun tapRefused(where: String) = "✗ 點擊被拒絕 · ${where}"
    override fun rallyClickToast(x: Int, y: Int) = "集結點擊！(${x}, ${y})"
    override val clickSendFailed = "⚠ 無法傳送點擊"
    override fun movedRoom(code: String) = "已移到房間 ${code}"
    override val needAuth = "需要驗證集結隊長代碼。"
    override val openAppJoin = "請先進入集結房間。即將開啟應用程式"
    override val openAppGroup = "請先設定所屬（伺服器·聯盟）。即將開啟應用程式"
    override fun savedPos(x: Int, y: Int) = "已儲存的點擊位置 ${x}, ${y}"
    override val noPosHelp = "沒有點擊位置 · 請把準星放在集結按鈕上再儲存"
    override val posSavedToast = "已把當前準星位置儲存為點擊位置"
    override fun otherAdminChanged(who: String) = "其他指揮官（${who}）更換了房間"
    override val stoppedFromNoti = "已從通知欄停止連點。"
    override val fullyOff = "已完全關閉 AutoClicker。"
    override val serviceReady = "AutoClicker 已準備好。請在應用程式中點 [顯示 AutoClicker]。"
    override val stoppedByVolume = "已用音量鍵立即停止連點。"
    override val widgetFailed = "無法顯示懸浮窗。請再試一次。"
    override val targetCentered = "已把準星放到螢幕中央。"
    override val closedKeepRally = "只關閉了畫面。集結開始時仍會自動點擊。"
    override val turnedOff = "已關閉 AutoClicker。"
    override val targetShown = "已顯示準星。"
    override val targetHidden = "已隱藏準星。（連點仍在繼續）"
    override fun opacityToast(pct: Int) = "透明度 ${pct}%"
    override fun countTimes(n: Int) = "${n}次"
    override fun minSec(m: Int, s: Int) = "${m}分${s}秒"
    override fun settingsApplied(interval: String, mode: String, pct: Int) = "⚙️ 設定已套用！(${interval}秒 / ${mode} / 透明度 ${pct}%)"
    override val stoppedTap = "已停止連點。"
    override val targetNotReady = "準星還沒有準備好。"
    override fun repeatN(n: Int) = "重複${n}次"
    override fun timerDesc(m: Int, s: Int) = "${m}分${s}秒計時"
    override fun tapStarted(desc: String) = "開始連點 (${desc})"
    override fun countDone(n: Int) = "✓ 已點滿${n}次，自動停止。"
    override fun timerDone(m: Int, s: Int) = "✓ 計時（${m}分${s}秒）結束，自動停止。"
    override val notiChannel = "AutoClicker 控制器"
    override val notiChannelDesc = "AutoClicker 執行與緊急停止遙控"
    override val notiRunning = "▶ 正在連點（點擊中）"
    override val notiIdle = "⏹ AutoClicker 待機中"
    override val notiRunningSub = "要停止請點下面的 [停止連點]。"
    override val notiIdleSub = "請用遊戲畫面上的控制器操作。"
    override val notiStop = "⏹ 停止連點"
    override val notiClose = "✕ 關閉懸浮窗"
}
