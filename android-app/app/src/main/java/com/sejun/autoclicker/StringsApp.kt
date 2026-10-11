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
