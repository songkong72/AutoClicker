package com.sejun.autoclicker

/**
 * 지휘관 관리·연맹 대표 창, 보내는 글, 숨은 모드의 글자 표. `T.x.이름`으로 꺼낸다.
 * 보내는 초대 글의 말머리("집결장 코드:", "소속:")는 받는 폰의 [InviteText]가 읽으므로, 바꾸면 그쪽 규칙도 함께 고친다.
 */
internal interface MoreStr {
    val shareListTitle: String
    val shareListName: String
    fun pendingCodesNote(n: Int): String
    val noDeviceId: String
    fun myIdBody(ver: String): String
    val deviceIdWord: String
    val checkingServer: String
    val devOnlyToast: String
    val serverCheckRules: String
    val listLoadFailed: String
    val justAgo: String
    val allianceHintCase: String
    val repOn: String
    val repOff: String
    val newCommanderCode: String
    fun repRequestsBtn(n: Int): String
    val allRooms: String
    val sendList: String
    val purge: String
    fun registeredHead(n: Int): String
    val noneYetDot: String
    val repSuffix: String
    fun activityRegistered(activity: String, day: String): String
    val changeGroup: String
    val rename: String
    fun pendingHead(n: Int): String
    val noneDot: String
    fun remainLeft(t: String): String
    val send: String
    val sendCodeTitle: String
    val commanderCode: String
    fun setGroupBody(label: String, id: String): String
    fun groupSetTo(label: String): String
    fun requestsTitle(n: Int): String
    val requestsBody: String
    fun requestedAgo(ago: String, id: String): String
    val noNote: String
    val reject: String
    val approve: String
    fun swapNote(name: String): String
    val noName: String
    fun overNote(after: Int, max: Int): String
    val approveTitle: String
    val replaceTitle: String
    fun approveBody(name: String, group: String, extra: String): String
    fun madeRep(name: String): String
    val rejectTitle: String
    fun rejectBody(group: String, name: String): String
    val rejectHint: String
    val rejected: String
    val noDeviceIdNet: String
    val repOnlyToast: String
    fun allianceCodeNote(group: String): String
    fun allianceFull(max: Int): String
    fun allianceHead(n: Int, max: Int): String
    val meSuffix: String
    val repShort: String
    val removeCommanderTitle: String
    fun removeCommanderBody(label: String): String
    fun removeCommanderBodyId(label: String, id: String): String
    val removed: String
    val codeCancelled: String
    val newCodeTitle: String
    fun newCodeBodyRep(group: String): String
    val labelHint: String
    val labelRequired: String
    val codeMakeFailed: String
    fun rulesMaybe(err: String): String
    val approvedPreview: String
    val repRequestTitle: String
    val repRequestBody: String
    val gameName: String
    val noteHint: String
    val applyBtn: String
    val gameNameRequired: String
    fun applied(group: String): String
    val pendingTitle: String
    fun pendingBody(group: String, name: String, ago: String): String
    val cancelRequest: String
    val requestCancelled: String
    val notApprovedTitle: String
    fun notApprovedHead(group: String, name: String): String
    val noReason: String
    fun devSaid(reason: String): String
    val reapply: String
    val roomsLoadFailed: String
    val noRoomsMade: String
    fun allRoomsTitle(n: Int): String
    val deleteRoom: String
    val deleteRoomConfirm: String
    fun roomRemoved(code: String): String
    fun days(n: Long): String
    fun hours(n: Long): String
    fun minutes(n: Long): String
    val newCodeBodyDev: String
    fun labelsHint(max: Int): String
    val ttlHead: String
    val repOneAtATime: String
    fun someFailed(err: String): String
    fun codesMadeTitle(n: Int): String
    fun codesMadeBody(group: String, ttl: String, warn: String): String
    val newLabelHint: String
    val renamed: String
    val purgeBody: String
    val purgeBtn: String
    val purgeFailed: String
    fun purged(n: Int): String
    val ttlHour: String
    val ttlDay: String
    val ttlWeek: String
    val whoRep: String
    fun commanderInvite(who: String, groupLine: String, code: String, ttl: String): String
    fun dear(name: String): String
    val directReg: String
    val noSeen: String
    val verUnknown: String
    fun exportHead(n: Int): String
    fun exportPending(n: Int): String
    val badRoom: String
    val badCode: String
    val codeUnusable: String
    fun demoteFailed(why: String): String
    val notAllowed: String
    val noConnection: String
    fun serverError(code: Int): String
    fun currentRepWarn(name: String, seen: String): String
    fun sameRequests(n: Int): String
    val noGroup: String
    val copied: String
    fun moreLines(n: Int): String
    val noKakao: String
    val noShareApp: String
    fun shareLine(server: String, alliance: String): String
    val roomShareTitle: String
    val roomNumber: String
    fun roomShare(groupLine: String, code: String): String
    val leaderCodeTitle: String
    fun inviteRoomLine(room: String): String
    fun leaderInvite(id: String, code: String, roomLine: String): String
    val on: String
    fun editDesc(title: String): String
    fun chooseMode(ver: String): String
    fun troopsN(n: Int): String
    val beforeSave: String
    val hunter: String
    val newSeq: String
    fun seqLimit(max: Int): String
    val off: String
    val reposition: String
    val renameSeq: String
    fun deleteSeqQ(name: String): String
    fun deleteSeqBody(n: Int): String
    fun seqDeleted(name: String): String
    fun seqOn(name: String): String
    val editSeq: String
    val newSeqTitle: String
    val nameWord: String
    val seqHelp: String
    val posCount: String
    val posFewer: String
    val posMore: String
    val gapTitle: String
    val gapShorter: String
    val gapLonger: String
    val posReadFailed: String
    fun seqSaved(name: String, n: Int): String
    fun seqName(n: Int): String
    val seqWord: String
    fun seqSummary(n: Int, gap: String): String
    fun readyLabel(n: Int): String
    fun runningLabel(i: Int, n: Int): String
    fun hunterSaved(n: Int): String
    val hunterSaveFirst: String
    fun hunterMarchN(i: Int, n: Int): String
    val hunterFire: String
    val fireLabel: String
    fun fireNo(n: Int): String
    fun firedLabel(n: Int): String
    val hiddenOpened: String
    val hiddenAlready: String
    val hunterOnToast: String
    val hunterNeedPos: String
    val hunterOff: String
    val hunterSetupTitle: String
    val hunterSetupHelp: String
    val troopCount: String
    val troopFewer: String
    val troopMore: String
    val hunterSave: String
    val hunterReadFailed: String
    val runIdle: String
    val runRunning: String
    val runCancelled: String
    fun roomLine(code: String, teams: Int, assigned: Int, run: String, members: Int): String
    fun detailHead(code: String, run: String, prep: String, wait: String): String
    val noneParen: String
    fun detailTeam(i: Int, team: String, leader: String, march: String, excluded: Boolean): String
    fun membersN(n: Int): String
}

internal object MoreKo : MoreStr {
    override val shareListTitle = "지휘관 목록 보내기"
    override val shareListName = "지휘관 목록"
    override fun pendingCodesNote(n: Int) = "대기 중인 지휘관 코드 ${n}개가 함께 보내져요. 받을 사람이 맞는 대화방인지 확인해 주세요."
    override val noDeviceId = "기기 ID를 받지 못했어요"
    override fun myIdBody(ver: String) = "개발자로 등록하려면 이 ID를 Firebase 콘솔의 owners 아래에 적습니다. 앱을 지우고 다시 설치하면 ID가 바뀝니다.\n\n앱 버전: v$ver"
    override val deviceIdWord = "기기 ID"
    override val checkingServer = "서버를 확인하는 중…"
    override val devOnlyToast = "개발자만 쓸 수 있어요. 개발자는 Firebase 콘솔에 이 기기 ID를 적어야 해요 (지휘관 메뉴 → 내 기기 ID 보기)"
    override val serverCheckRules = "서버에서 확인하지 못했어요. 인터넷 연결과 서버 규칙 적용 여부를 확인해 주세요"
    override val listLoadFailed = "목록을 불러오지 못했어요"
    override val justAgo = "방금 전"
    override val allianceHintCase = "연맹 (예: WBI, 대소문자 구분)"
    override val repOn = "✓ 연맹 대표로 지정함"
    override val repOff = "연맹 대표로 지정 안 함 (누르면 지정)"
    override val newCommanderCode = "+ 지휘관 코드 만들기"
    override fun repRequestsBtn(n: Int) = "연맹 대표 신청 ${n}건"
    override val allRooms = "방 전체 목록"
    override val sendList = "목록 보내기"
    override val purge = "코드 정리"
    override fun registeredHead(n: Int) = "등록된 지휘관 (${n}명)"
    override val noneYetDot = "아직 없어요."
    override val repSuffix = " · 연맹 대표"
    override fun activityRegistered(activity: String, day: String) = "$activity · 등록 $day"
    override val changeGroup = "소속 바꾸기"
    override val rename = "이름표 수정"
    override fun pendingHead(n: Int) = "대기 중인 코드 (${n}개)"
    override val noneDot = "없어요."
    override fun remainLeft(t: String) = "$t 남음"
    override val send = "보내기"
    override val sendCodeTitle = "지휘관 코드 보내기"
    override val commanderCode = "지휘관 코드"
    override fun setGroupBody(label: String, id: String) = "$label (ID …$id) 을(를) 어느 소속의 지휘관으로 둘지 정해요. 그 폰은 다음에 앱을 열 때 이 소속으로 바뀌어요."
    override fun groupSetTo(label: String) = "소속을 ${label}(으)로 정했어요"
    override fun requestsTitle(n: Int) = "연맹 대표 신청 (${n}건)"
    override val requestsBody = "신청한 사람이 정말 그 연맹 사람인지는 앱이 확인하지 못해요. 한마디를 보고 판단해 주세요."
    override fun requestedAgo(ago: String, id: String) = "$ago 신청 · ID …$id"
    override val noNote = "(한마디 없음)"
    override val reject = "거절"
    override val approve = "승인"
    override fun swapNote(name: String) = "\n지금 대표 $name 님은 일반 지휘관으로 내려가요."
    override val noName = "이름 없음"
    override fun overNote(after: Int, max: Int) = "\n이 연맹의 지휘관이 ${after}명이 돼요(한도 ${max}명). 새 대표가 한 명을 빼야 코드를 더 만들 수 있어요."
    override val approveTitle = "연맹 대표 승인"
    override val replaceTitle = "연맹 대표 바꾸기"
    override fun approveBody(name: String, group: String, extra: String) = "$name 님을 ${group}의 연맹 대표로 정할까요? 대표는 그 연맹의 지휘관을 직접 정하고 뺄 수 있어요.$extra"
    override fun madeRep(name: String) = "$name 님을 연맹 대표로 정했어요"
    override val rejectTitle = "신청 거절"
    override fun rejectBody(group: String, name: String) = "$group · $name\n신청한 사람의 화면에 보일 한 줄을 적어 주세요. 비워 둬도 돼요."
    override val rejectHint = "예: 이미 대표가 있어요"
    override val rejected = "신청을 거절했어요"
    override val noDeviceIdNet = "기기 ID를 받지 못했어요. 인터넷 연결을 확인해 주세요"
    override val repOnlyToast = "연맹 대표만 쓸 수 있어요."
    override fun allianceCodeNote(group: String) = "코드를 받은 사람은 ${group}의 지휘관이 돼요. 다른 지휘관을 정할 수는 없어요."
    override fun allianceFull(max: Int) = "한 연맹의 지휘관은 ${max}명까지예요. 더 넣으려면 먼저 한 명을 빼 주세요."
    override fun allianceHead(n: Int, max: Int) = "우리 연맹 지휘관 ($n / ${max}명)"
    override val meSuffix = " (나)"
    override val repShort = " · 대표"
    override val removeCommanderTitle = "지휘관 삭제"
    override fun removeCommanderBody(label: String) = "$label 의 지휘관 권한을 없앨까요? 그 폰은 다음에 앱을 열 때 집결장 화면으로 돌아갑니다."
    override fun removeCommanderBodyId(label: String, id: String) = "$label (ID …$id) 의 지휘관 권한을 없앨까요? 그 폰은 다음에 앱을 열 때 집결장 화면으로 돌아갑니다."
    override val removed = "삭제했어요"
    override val codeCancelled = "코드를 취소했어요"
    override val newCodeTitle = "새 지휘관 코드"
    override fun newCodeBodyRep(group: String) = "${group}의 지휘관이 될 사람의 이름표를 적어 주세요. 코드는 24시간 안에 한 번만 쓸 수 있어요."
    override val labelHint = "이름표"
    override val labelRequired = "이름표를 입력해 주세요"
    override val codeMakeFailed = "코드를 만들지 못했어요"
    override fun rulesMaybe(err: String) = "$err\n(새 서버 규칙이 아직 게시되지 않았을 수 있어요)"
    override val approvedPreview = "승인됐어요. 실제 사용자라면 여기서 지휘관 화면으로 바뀌어요. (미리보기라 화면은 그대로예요)"
    override val repRequestTitle = "연맹 대표 신청"
    override val repRequestBody = "우리 연맹에서 이 앱의 집결 기능을 쓰려면 대표가 한 명 필요해요. 개발자가 확인한 뒤 승인하면 이 폰이 대표가 되어 지휘관을 정할 수 있어요. 한 폰에서 신청은 하나만 할 수 있어요."
    override val gameName = "게임 캐릭터명"
    override val noteHint = "한마디 (선택, 예: WBI 맹주입니다)"
    override val applyBtn = "신청하기"
    override val gameNameRequired = "게임 캐릭터명을 입력해 주세요"
    override fun applied(group: String) = "${group}의 연맹 대표를 신청했어요. 개발자가 승인하면 알 수 있게 가끔 확인해 주세요."
    override val pendingTitle = "연맹 대표 승인 대기 중"
    override fun pendingBody(group: String, name: String, ago: String) = "$group · $name\n${ago}에 신청했어요. 아직 승인되지 않았어요.\n개발자가 승인한 뒤 이 창을 다시 열면 지휘관 화면으로 바뀌어요."
    override val cancelRequest = "신청 취소"
    override val requestCancelled = "신청을 취소했어요"
    override val notApprovedTitle = "승인되지 않았어요"
    override fun notApprovedHead(group: String, name: String) = "$group · $name\n"
    override val noReason = "개발자가 이유를 적지 않았어요."
    override fun devSaid(reason: String) = "개발자의 말: \"$reason\""
    override val reapply = "다시 신청"
    override val roomsLoadFailed = "방 목록을 불러오지 못했어요"
    override val noRoomsMade = "만들어진 방이 없어요"
    override fun allRoomsTitle(n: Int) = "방 전체 (${n}개)"
    override val deleteRoom = "방 삭제"
    override val deleteRoomConfirm = "이 방과 방 명단을 서버에서 지워요. 되돌릴 수 없고, 그 방에 들어가 있던 사람들은 다시 입장해야 합니다. 정말 지울까요?"
    override fun roomRemoved(code: String) = "방 $code 을(를) 지웠어요"
    override fun days(n: Long) = "${n}일"
    override fun hours(n: Long) = "${n}시간"
    override fun minutes(n: Long) = "${n}분"
    override val newCodeBodyDev = "누구에게, 어느 소속으로 줄 코드인지 적어 주세요. 이 코드로 들어온 지휘관은 그 소속의 방만 만들고 고칠 수 있어요. 코드는 한 번만 쓸 수 있어요."
    override fun labelsHint(max: Int) = "이름표 (여러 명이면 줄바꿈으로, 최대 ${max}명)"
    override val ttlHead = "코드를 쓸 수 있는 기간"
    override val repOneAtATime = "연맹 대표 코드는 한 번에 한 명만 만들 수 있어요"
    override fun someFailed(err: String) = "\n\n일부는 만들지 못했어요: $err"
    override fun codesMadeTitle(n: Int) = "코드를 ${n}개 만들었어요"
    override fun codesMadeBody(group: String, ttl: String, warn: String) = "$group\n카카오톡으로 보낼 안내 문구를 사람별로 복사해 두었어요. $ttl 안에 한 번만 쓸 수 있어요.$warn"
    override val newLabelHint = "새 이름표"
    override val renamed = "이름표를 고쳤어요"
    override val purgeBody = "이미 쓰였거나 기한이 지난 코드를 서버에서 지워요. 지휘관 이름표는 먼저 저장해 두니 목록에 그대로 남아요. 대기 중인 코드는 지우지 않습니다."
    override val purgeBtn = "정리"
    override val purgeFailed = "정리하지 못했어요"
    override fun purged(n: Int) = "코드 ${n}개를 정리했어요"
    override val ttlHour = "1시간"
    override val ttlDay = "24시간"
    override val ttlWeek = "7일"
    override val whoRep = "연맹 대표"
    override fun commanderInvite(who: String, groupLine: String, code: String, ttl: String) = "[오토클리커 Pro $who 초대]$groupLine\n지휘관 코드: $code\n앱의 인증 화면 → 지휘관 로그인에서 이 코드를 입력하세요. 만든 지 $ttl 안에 한 번만 쓸 수 있어요."
    override fun dear(name: String) = "${name}님께\n"
    override val directReg = "직접 등록"
    override val noSeen = "접속 기록 없음"
    override val verUnknown = "버전 알 수 없음"
    override fun exportHead(n: Int) = "등록된 지휘관 ${n}명\n"
    override fun exportPending(n: Int) = "\n대기 중인 코드 ${n}개\n"
    override val badRoom = "방 번호가 올바르지 않아요"
    override val badCode = "지휘관 코드 모양이 맞지 않아요 (AD-로 시작하는 10자)"
    override val codeUnusable = "코드가 없거나, 이미 쓰였거나, 기한(24시간)이 지났어요"
    override fun demoteFailed(why: String) = "새 대표는 정했지만 예전 대표를 내리지 못했어요. 지휘관 관리에서 확인해 주세요. ($why)"
    override val notAllowed = "서버가 허락하지 않았어요 (개발자 등록이나 서버 규칙을 확인해 주세요)"
    override val noConnection = "서버에 연결하지 못했어요"
    override fun serverError(code: Int) = "서버 오류 $code"
    override fun currentRepWarn(name: String, seen: String) = "지금 대표: $name · 마지막 접속 $seen. 승인하면 대표가 바뀌어요."
    override fun sameRequests(n: Int) = "같은 연맹에서 신청이 ${n}건이에요. 한 명만 대표가 돼요."
    override val noGroup = "소속 없음"
    override val copied = "📋 복사했어요"
    override fun moreLines(n: Int) = "\n… 외 ${n}줄"
    override val noKakao = "카카오톡이 없어요. 보낼 앱을 골라 주세요"
    override val noShareApp = "보낼 수 있는 앱이 없어요"
    override fun shareLine(server: String, alliance: String) = "소속: $server 서버 · $alliance"
    override val roomShareTitle = "방 번호 보내기"
    override val roomNumber = "방 번호"
    override fun roomShare(groupLine: String, code: String) = "[오토클리커 Pro 집결 방]\n${groupLine}집결 방 번호: $code\n(앱 > 집결 방에서 소속을 먼저 정한 뒤 방 선택)"
    override val leaderCodeTitle = "집결장 코드"
    override fun inviteRoomLine(room: String) = "\n집결 방 번호: $room (인증 후 방 선택에서 고르기)"
    override fun leaderInvite(id: String, code: String, roomLine: String) = "[오토클리커 Pro 집결장 초대]\nID: $id\n집결장 코드: $code\n이 글을 통째로 복사한 뒤 앱에서 \"인증하기\"를 누르면 저절로 채워져요. 이 코드는 아래 소속에서만 맞아요.$roomLine"
    override val on = "켜짐"
    override fun editDesc(title: String) = "$title 고치기"
    override fun chooseMode(ver: String) = "모드 고르기 · $ver"
    override fun troopsN(n: Int) = "부대 ${n}개"
    override val beforeSave = "위치 저장 전"
    override val hunter = "헌터"
    override val newSeq = "+ 새 순서 만들기"
    override fun seqLimit(max: Int) = "순서는 ${max}개까지 저장할 수 있어요"
    override val off = "끄기"
    override val reposition = "자리 다시 잡기"
    override val renameSeq = "이름 바꾸기"
    override fun deleteSeqQ(name: String) = "\"$name\"을(를) 지울까요?"
    override fun deleteSeqBody(n: Int) = "저장한 자리 ${n}곳이 함께 지워져요."
    override fun seqDeleted(name: String) = "\"$name\" 순서를 지웠어요"
    override fun seqOn(name: String) = "\"$name\" 순서 켜짐 · 하늘색 버튼을 누르면 1번부터 눌러요"
    override val editSeq = "순서 고치기"
    override val newSeqTitle = "새 순서 만들기"
    override val nameWord = "이름"
    override val seqHelp = "동그라미 1, 2, 3…을 누를 자리에 번호 순서대로 놓고 저장"
    override val posCount = "자리 수"
    override val posFewer = "자리 줄이기"
    override val posMore = "자리 늘리기"
    override val gapTitle = "자리 사이 쉬는 시간"
    override val gapShorter = "쉬는 시간 줄이기"
    override val gapLonger = "쉬는 시간 늘리기"
    override val posReadFailed = "⚠ 자리를 읽지 못했어요. 번호 동그라미가 보이는지 확인하세요"
    override fun seqSaved(name: String, n: Int) = "✓ \"$name\" 저장 (${n}곳) · 하늘색 버튼을 누르면 1번부터 눌러요"
    override fun seqName(n: Int) = "순서 $n"
    override val seqWord = "순서"
    override fun seqSummary(n: Int, gap: String) = "${n}곳 · ${gap}초 간격"
    override fun readyLabel(n: Int) = "순서\n${n}곳"
    override fun runningLabel(i: Int, n: Int) = "$i / $n\n멈춤"
    override fun hunterSaved(n: Int) = "🐻 헌터 위치 저장 완료! (부대 ${n}개)"
    override val hunterSaveFirst = "🐻 먼저 헌터 위치를 저장해 주세요 (모드 목록에서 헌터 옆 연필)"
    override fun hunterMarchN(i: Int, n: Int) = "🐻 $i/${n}번 부대 출정!"
    override val hunterFire = "🐻 헌터 발사!"
    override val fireLabel = "🐻\n발사"
    override fun fireNo(n: Int) = "🐻\n${n}번"
    override fun firedLabel(n: Int) = "${n}번\n출정"
    override val hiddenOpened = "🐻 숨은 모드가 열렸어요! 막대의 모드 버튼에서 헌터·순서 클릭을 고르세요"
    override val hiddenAlready = "🐻 숨은 모드는 이미 열려 있어요."
    override val hunterOnToast = "🐻 헌터 모드 켜짐 · 집결을 고른 뒤 🐻 발사 버튼을 누르세요 (위치를 다시 잡으려면 모드 버튼 길게 누르기)"
    override val hunterNeedPos = "🐻 먼저 쓸 부대와 출정 버튼 위치를 잡아 저장해 주세요"
    override val hunterOff = "🐻 헌터 모드 종료!"
    override val hunterSetupTitle = "헌터 위치 잡기"
    override val hunterSetupHelp = "과녁 → 출정 버튼 위에\n동그라미 1, 2, 3… → 쓸 부대 깃발 위에 번호 순서대로 놓고 저장\n(누를 때마다 1번부터 차례로 출정)"
    override val troopCount = "부대 수"
    override val troopFewer = "부대 줄이기"
    override val troopMore = "부대 늘리기"
    override val hunterSave = "헌터 위치 저장"
    override val hunterReadFailed = "⚠ 위치를 읽지 못했어요. 과녁과 부대 표시가 보이는지 확인하세요"
    override val runIdle = "대기 중"
    override val runRunning = "진행 중"
    override val runCancelled = "취소됨"
    override fun roomLine(code: String, teams: Int, assigned: Int, run: String, members: Int) = "$code · 군단 ${teams}개 (배정 $assigned) · $run · 명단 ${members}명"
    override fun detailHead(code: String, run: String, prep: String, wait: String) = "방 $code\n상태 $run · 준비 ${prep}초 · 대기 ${wait}초\n\n군단\n"
    override val noneParen = "(없음)\n"
    override fun detailTeam(i: Int, team: String, leader: String, march: String, excluded: Boolean) = "$i. $team · 집결장 $leader · 행군 ${march}초${if (excluded) " · 제외" else ""}\n"
    override fun membersN(n: Int) = "\n명단 ${n}명"
}

internal object MoreEn : MoreStr {
    override val shareListTitle = "Send commander list"
    override val shareListName = "Commander list"
    override fun pendingCodesNote(n: Int) = "$n pending commander codes are included. Make sure this is the right chat."
    override val noDeviceId = "Could not get the device ID"
    override fun myIdBody(ver: String) = "To register as developer, add this ID under owners in the Firebase console. The ID changes if you uninstall and reinstall the app.\n\nApp version: v$ver"
    override val deviceIdWord = "Device ID"
    override val checkingServer = "Checking the server…"
    override val devOnlyToast = "Developer only. A developer must add this device ID in the Firebase console (Commander menu → Show my device ID)"
    override val serverCheckRules = "Could not check with the server. Check the internet connection and that the server rules are applied"
    override val listLoadFailed = "Could not load the list"
    override val justAgo = "just now"
    override val allianceHintCase = "Alliance (e.g. WBI, case-sensitive)"
    override val repOn = "✓ Set as alliance rep"
    override val repOff = "Not an alliance rep (tap to set)"
    override val newCommanderCode = "+ Make commander code"
    override fun repRequestsBtn(n: Int) = "Alliance rep requests: $n"
    override val allRooms = "All rooms"
    override val sendList = "Send list"
    override val purge = "Clean up codes"
    override fun registeredHead(n: Int) = "Registered commanders ($n)"
    override val noneYetDot = "None yet."
    override val repSuffix = " · alliance rep"
    override fun activityRegistered(activity: String, day: String) = "$activity · registered $day"
    override val changeGroup = "Change group"
    override val rename = "Edit label"
    override fun pendingHead(n: Int) = "Pending codes ($n)"
    override val noneDot = "None."
    override fun remainLeft(t: String) = "$t left"
    override val send = "Send"
    override val sendCodeTitle = "Send commander code"
    override val commanderCode = "Commander code"
    override fun setGroupBody(label: String, id: String) = "Choose which group $label (ID …$id) commands. That phone switches to this group the next time the app opens."
    override fun groupSetTo(label: String) = "Group set to $label"
    override fun requestsTitle(n: Int) = "Alliance rep requests ($n)"
    override val requestsBody = "The app cannot check that the applicant really belongs to that alliance. Judge by their note."
    override fun requestedAgo(ago: String, id: String) = "Requested $ago · ID …$id"
    override val noNote = "(no note)"
    override val reject = "Reject"
    override val approve = "Approve"
    override fun swapNote(name: String) = "\nThe current rep $name becomes a regular commander."
    override val noName = "no name"
    override fun overNote(after: Int, max: Int) = "\nThis alliance will have $after commanders (limit $max). The new rep must remove one before making more codes."
    override val approveTitle = "Approve alliance rep"
    override val replaceTitle = "Replace alliance rep"
    override fun approveBody(name: String, group: String, extra: String) = "Make $name the alliance rep of $group? A rep can add and remove that alliance's commanders.$extra"
    override fun madeRep(name: String) = "$name is now the alliance rep"
    override val rejectTitle = "Reject request"
    override fun rejectBody(group: String, name: String) = "$group · $name\nWrite one line the applicant will see. You can leave it empty."
    override val rejectHint = "e.g. There is already a rep"
    override val rejected = "Request rejected"
    override val noDeviceIdNet = "Could not get the device ID. Check your internet connection"
    override val repOnlyToast = "Alliance rep only."
    override fun allianceCodeNote(group: String) = "Whoever uses the code becomes a commander of $group. They cannot appoint other commanders."
    override fun allianceFull(max: Int) = "An alliance can have up to $max commanders. Remove one before adding another."
    override fun allianceHead(n: Int, max: Int) = "Our alliance commanders ($n / $max)"
    override val meSuffix = " (me)"
    override val repShort = " · rep"
    override val removeCommanderTitle = "Remove commander"
    override fun removeCommanderBody(label: String) = "Remove commander rights from $label? That phone goes back to the rally leader screen the next time the app opens."
    override fun removeCommanderBodyId(label: String, id: String) = "Remove commander rights from $label (ID …$id)? That phone goes back to the rally leader screen the next time the app opens."
    override val removed = "Removed"
    override val codeCancelled = "Code cancelled"
    override val newCodeTitle = "New commander code"
    override fun newCodeBodyRep(group: String) = "Enter a label for the person who will command $group. The code works once, within 24 hours."
    override val labelHint = "Label"
    override val labelRequired = "Enter a label"
    override val codeMakeFailed = "Could not make the code"
    override fun rulesMaybe(err: String) = "$err\n(The new server rules may not be published yet)"
    override val approvedPreview = "Approved. For a real user the commander screen would open here. (This is a preview, so the screen stays)"
    override val repRequestTitle = "Apply as alliance rep"
    override val repRequestBody = "Your alliance needs one rep to use the rally feature. After the developer approves, this phone becomes the rep and can appoint commanders. One request per phone."
    override val gameName = "In-game character name"
    override val noteHint = "Note (optional, e.g. I lead WBI)"
    override val applyBtn = "Apply"
    override val gameNameRequired = "Enter your in-game character name"
    override fun applied(group: String) = "Applied as alliance rep of $group. Check back now and then to see whether it was approved."
    override val pendingTitle = "Alliance rep request pending"
    override fun pendingBody(group: String, name: String, ago: String) = "$group · $name\nRequested $ago. Not approved yet.\nAfter the developer approves, open this window again to switch to the commander screen."
    override val cancelRequest = "Cancel request"
    override val requestCancelled = "Request cancelled"
    override val notApprovedTitle = "Not approved"
    override fun notApprovedHead(group: String, name: String) = "$group · $name\n"
    override val noReason = "The developer gave no reason."
    override fun devSaid(reason: String) = "The developer said: \"$reason\""
    override val reapply = "Apply again"
    override val roomsLoadFailed = "Could not load the rooms"
    override val noRoomsMade = "No rooms have been created"
    override fun allRoomsTitle(n: Int) = "All rooms ($n)"
    override val deleteRoom = "Delete room"
    override val deleteRoomConfirm = "This removes the room and its member list from the server. It cannot be undone, and people in the room will have to join again. Delete it?"
    override fun roomRemoved(code: String) = "Room $code deleted"
    override fun days(n: Long) = "${n}d"
    override fun hours(n: Long) = "${n}h"
    override fun minutes(n: Long) = "${n}m"
    override val newCodeBodyDev = "Enter who the code is for and which group. A commander who joins with it can create and edit rooms only in that group. The code works once."
    override fun labelsHint(max: Int) = "Labels (one per line, up to $max)"
    override val ttlHead = "Code valid for"
    override val repOneAtATime = "Alliance rep codes can be made only one at a time"
    override fun someFailed(err: String) = "\n\nSome could not be made: $err"
    override fun codesMadeTitle(n: Int) = "$n codes created"
    override fun codesMadeBody(group: String, ttl: String, warn: String) = "$group\nThe invite text for each person has been copied. Each works once, within $ttl.$warn"
    override val newLabelHint = "New label"
    override val renamed = "Label updated"
    override val purgeBody = "Removes used or expired codes from the server. Commander labels are saved first, so the list stays as it is. Pending codes are kept."
    override val purgeBtn = "Clean up"
    override val purgeFailed = "Could not clean up"
    override fun purged(n: Int) = "$n codes cleaned up"
    override val ttlHour = "1 hour"
    override val ttlDay = "24 hours"
    override val ttlWeek = "7 days"
    override val whoRep = "Alliance rep"
    override fun commanderInvite(who: String, groupLine: String, code: String, ttl: String) = "[AutoClicker Pro $who invite]$groupLine\nCommander code: $code\nEnter this code in the app: verification card → Commander login. It works once, within $ttl of being made."
    override fun dear(name: String) = "To $name\n"
    override val directReg = "Added directly"
    override val noSeen = "never seen"
    override val verUnknown = "version unknown"
    override fun exportHead(n: Int) = "Registered commanders: $n\n"
    override fun exportPending(n: Int) = "\nPending codes: $n\n"
    override val badRoom = "The room number is not valid"
    override val badCode = "That does not look like a commander code (10 characters starting with AD-)"
    override val codeUnusable = "The code does not exist, was already used, or has expired"
    override fun demoteFailed(why: String) = "The new rep is set, but the old rep could not be demoted. Check in Commander admin. ($why)"
    override val notAllowed = "The server did not allow it (check the developer registration or the server rules)"
    override val noConnection = "Could not connect to the server"
    override fun serverError(code: Int) = "Server error $code"
    override fun currentRepWarn(name: String, seen: String) = "Current rep: $name · last seen $seen. Approving replaces the rep."
    override fun sameRequests(n: Int) = "$n requests from the same alliance. Only one becomes the rep."
    override val noGroup = "no group"
    override val copied = "📋 Copied"
    override fun moreLines(n: Int) = "\n… and $n more lines"
    override val noKakao = "KakaoTalk is not installed. Choose an app to send with"
    override val noShareApp = "No app can send this"
    override fun shareLine(server: String, alliance: String) = "Group: Server $server · $alliance"
    override val roomShareTitle = "Send room number"
    override val roomNumber = "Room number"
    override fun roomShare(groupLine: String, code: String) = "[AutoClicker Pro rally room]\n${groupLine}Rally room number: $code\n(In the app: set your group under Rally room, then pick the room)"
    override val leaderCodeTitle = "Rally leader code"
    override fun inviteRoomLine(room: String) = "\nRally room number: $room (pick it from the room list after verifying)"
    override fun leaderInvite(id: String, code: String, roomLine: String) = "[AutoClicker Pro rally leader invite]\nID: $id\nRally leader code: $code\nCopy this whole message, then tap \"Verify\" in the app and it fills in by itself. This code works only for the group below.$roomLine"
    override val on = "On"
    override fun editDesc(title: String) = "Edit $title"
    override fun chooseMode(ver: String) = "Choose mode · $ver"
    override fun troopsN(n: Int) = "$n squads"
    override val beforeSave = "No positions yet"
    override val hunter = "Hunter"
    override val newSeq = "+ New sequence"
    override fun seqLimit(max: Int) = "You can save up to $max sequences"
    override val off = "Off"
    override val reposition = "Set positions again"
    override val renameSeq = "Rename"
    override fun deleteSeqQ(name: String) = "Delete \"$name\"?"
    override fun deleteSeqBody(n: Int) = "Its $n saved positions are deleted too."
    override fun seqDeleted(name: String) = "Sequence \"$name\" deleted"
    override fun seqOn(name: String) = "Sequence \"$name\" on · tap the sky-blue button to run from position 1"
    override val editSeq = "Edit sequence"
    override val newSeqTitle = "New sequence"
    override val nameWord = "Name"
    override val seqHelp = "Put circles 1, 2, 3… on the spots to tap, in order, then save"
    override val posCount = "Positions"
    override val posFewer = "Fewer positions"
    override val posMore = "More positions"
    override val gapTitle = "Pause between positions"
    override val gapShorter = "Shorter pause"
    override val gapLonger = "Longer pause"
    override val posReadFailed = "⚠ Could not read the positions. Check that the numbered circles are visible"
    override fun seqSaved(name: String, n: Int) = "✓ \"$name\" saved ($n positions) · tap the sky-blue button to run from position 1"
    override fun seqName(n: Int) = "Sequence $n"
    override val seqWord = "Sequence"
    override fun seqSummary(n: Int, gap: String) = "$n positions · ${gap}s pause"
    override fun readyLabel(n: Int) = "Run\n$n"
    override fun runningLabel(i: Int, n: Int) = "$i / $n\nStop"
    override fun hunterSaved(n: Int) = "🐻 Hunter positions saved! ($n squads)"
    override val hunterSaveFirst = "🐻 Save the Hunter positions first (pencil next to Hunter in the mode list)"
    override fun hunterMarchN(i: Int, n: Int) = "🐻 Squad $i/$n marching!"
    override val hunterFire = "🐻 Hunter fired!"
    override val fireLabel = "🐻\nFire"
    override fun fireNo(n: Int) = "🐻\n#$n"
    override fun firedLabel(n: Int) = "#$n\nsent"
    override val hiddenOpened = "🐻 Hidden modes unlocked! Choose Hunter or a sequence from the mode button on the bar"
    override val hiddenAlready = "🐻 Hidden modes are already unlocked."
    override val hunterOnToast = "🐻 Hunter mode on · choose a rally, then tap the 🐻 fire button (long-press the mode button to set positions again)"
    override val hunterNeedPos = "🐻 First set and save the squad and march button positions"
    override val hunterOff = "🐻 Hunter mode off!"
    override val hunterSetupTitle = "Set Hunter positions"
    override val hunterSetupHelp = "Target → on the march button\nCircles 1, 2, 3… → on the squad flags, in order, then save\n(each tap sends the next squad, starting from 1)"
    override val troopCount = "Squads"
    override val troopFewer = "Fewer squads"
    override val troopMore = "More squads"
    override val hunterSave = "Save Hunter positions"
    override val hunterReadFailed = "⚠ Could not read the positions. Check that the target and squad markers are visible"
    override val runIdle = "idle"
    override val runRunning = "running"
    override val runCancelled = "cancelled"
    override fun roomLine(code: String, teams: Int, assigned: Int, run: String, members: Int) = "$code · $teams teams ($assigned assigned) · $run · $members members"
    override fun detailHead(code: String, run: String, prep: String, wait: String) = "Room $code\nStatus $run · prep ${prep}s · rally time ${wait}s\n\nTeams\n"
    override val noneParen = "(none)\n"
    override fun detailTeam(i: Int, team: String, leader: String, march: String, excluded: Boolean) = "$i. $team · leader $leader · march ${march}s${if (excluded) " · out" else ""}\n"
    override fun membersN(n: Int) = "\nMembers: $n"
}
