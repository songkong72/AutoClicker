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
    val hunterReadyTitle: String
    val hunterReadySub: String
    val hunterGapTitle: String
    val hunterGapSub: String
    val hunterReadyShorter: String
    val hunterReadyLonger: String
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
    override val hunterReadyTitle = "🐻 누른 뒤 쉬는 시간"
    override val hunterReadySub = "부대 누르기 전 · 0.05초씩"
    override val hunterGapTitle = "부대 → 출정 간격"
    override val hunterGapSub = "부대 뒤 출정까지 · 0.05초씩"
    override val hunterReadyShorter = "쉬는 시간 줄이기"
    override val hunterReadyLonger = "쉬는 시간 늘리기"
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
    override val hunterReadyTitle = "🐻 Wait after tap"
    override val hunterReadySub = "Before the squad tap · 0.05s steps"
    override val hunterGapTitle = "Squad → march"
    override val hunterGapSub = "From squad to march · 0.05s steps"
    override val hunterReadyShorter = "Shorter wait"
    override val hunterReadyLonger = "Longer wait"
    override val runIdle = "idle"
    override val runRunning = "running"
    override val runCancelled = "cancelled"
    override fun roomLine(code: String, teams: Int, assigned: Int, run: String, members: Int) = "$code · $teams teams ($assigned assigned) · $run · $members members"
    override fun detailHead(code: String, run: String, prep: String, wait: String) = "Room $code\nStatus $run · prep ${prep}s · rally time ${wait}s\n\nTeams\n"
    override val noneParen = "(none)\n"
    override fun detailTeam(i: Int, team: String, leader: String, march: String, excluded: Boolean) = "$i. $team · leader $leader · march ${march}s${if (excluded) " · out" else ""}\n"
    override fun membersN(n: Int) = "\nMembers: $n"
}

internal object MoreZh : MoreStr {
    override val shareListTitle = "发送指挥官名单"
    override val shareListName = "指挥官名单"
    override fun pendingCodesNote(n: Int) = "会一并发送${n}个等待中的指挥官代码。请确认聊天对象是否正确。"
    override val noDeviceId = "无法获取设备 ID"
    override fun myIdBody(ver: String) = "要登记为开发者，请把这个 ID 写在 Firebase 控制台的 owners 下面。卸载后重新安装应用，ID 会改变。\n\n应用版本：v${ver}"
    override val deviceIdWord = "设备 ID"
    override val checkingServer = "正在确认服务器…"
    override val devOnlyToast = "仅开发者可用。开发者需要在 Firebase 控制台填写这台设备的 ID（指挥官菜单 → 查看我的设备 ID）"
    override val serverCheckRules = "无法向服务器确认。请检查网络连接和服务器规则是否已生效"
    override val listLoadFailed = "无法加载列表"
    override val justAgo = "刚刚"
    override val allianceHintCase = "联盟（例如 WBI，区分大小写）"
    override val repOn = "✓ 设为联盟代表"
    override val repOff = "不设为联盟代表（点击可设定）"
    override val newCommanderCode = "+ 生成指挥官代码"
    override fun repRequestsBtn(n: Int) = "联盟代表申请 ${n}件"
    override val allRooms = "全部房间列表"
    override val sendList = "发送名单"
    override val purge = "清理代码"
    override fun registeredHead(n: Int) = "已登记的指挥官（${n}人）"
    override val noneYetDot = "还没有。"
    override val repSuffix = " · 联盟代表"
    override fun activityRegistered(activity: String, day: String) = "${activity} · 登记于 ${day}"
    override val changeGroup = "更改所属"
    override val rename = "修改备注名"
    override fun pendingHead(n: Int) = "等待中的代码（${n}个）"
    override val noneDot = "没有。"
    override fun remainLeft(t: String) = "剩余 ${t}"
    override val send = "发送"
    override val sendCodeTitle = "发送指挥官代码"
    override val commanderCode = "指挥官代码"
    override fun setGroupBody(label: String, id: String) = "设定 ${label} (ID …${id}) 是哪个所属的指挥官。那台手机下次打开应用时会变成这个所属。"
    override fun groupSetTo(label: String) = "所属已设为 ${label}"
    override fun requestsTitle(n: Int) = "联盟代表申请（${n}件）"
    override val requestsBody = "应用无法确认申请人是否真的属于那个联盟。请根据留言判断。"
    override fun requestedAgo(ago: String, id: String) = "${ago}申请 · ID …${id}"
    override val noNote = "（没有留言）"
    override val reject = "拒绝"
    override val approve = "批准"
    override fun swapNote(name: String) = "\n现任代表 ${name} 将降为普通指挥官。"
    override val noName = "没有名字"
    override fun overNote(after: Int, max: Int) = "\n这个联盟的指挥官将变成${after}人（上限${max}人）。新代表需要先移除一人才能再生成代码。"
    override val approveTitle = "批准联盟代表"
    override val replaceTitle = "更换联盟代表"
    override fun approveBody(name: String, group: String, extra: String) = "要把 ${name} 定为 ${group} 的联盟代表吗？代表可以直接指定和移除该联盟的指挥官。${extra}"
    override fun madeRep(name: String) = "已把 ${name} 定为联盟代表"
    override val rejectTitle = "拒绝申请"
    override fun rejectBody(group: String, name: String) = "${group} · ${name}\n请写一行会显示在申请人画面上的话。可以留空。"
    override val rejectHint = "例如：已经有代表了"
    override val rejected = "已拒绝申请"
    override val noDeviceIdNet = "无法获取设备 ID。请检查网络连接"
    override val repOnlyToast = "仅联盟代表可用。"
    override fun allianceCodeNote(group: String) = "使用这个代码的人会成为 ${group} 的指挥官。他不能指定其他指挥官。"
    override fun allianceFull(max: Int) = "一个联盟最多${max}名指挥官。要再添加请先移除一人。"
    override fun allianceHead(n: Int, max: Int) = "本联盟指挥官（${n} / ${max}人）"
    override val meSuffix = "（我）"
    override val repShort = " · 代表"
    override val removeCommanderTitle = "移除指挥官"
    override fun removeCommanderBody(label: String) = "要取消 ${label} 的指挥官权限吗？那台手机下次打开应用时会回到集结队长画面。"
    override fun removeCommanderBodyId(label: String, id: String) = "要取消 ${label} (ID …${id}) 的指挥官权限吗？那台手机下次打开应用时会回到集结队长画面。"
    override val removed = "已移除"
    override val codeCancelled = "已取消代码"
    override val newCodeTitle = "新的指挥官代码"
    override fun newCodeBodyRep(group: String) = "请填写将成为 ${group} 指挥官的人的备注名。代码在24小时内只能使用一次。"
    override val labelHint = "备注名"
    override val labelRequired = "请输入备注名"
    override val codeMakeFailed = "无法生成代码"
    override fun rulesMaybe(err: String) = "${err}\n(新的服务器规则可能还没有发布)"
    override val approvedPreview = "已批准。如果是真实用户，这里会切换到指挥官画面。（现在是预览，画面不变）"
    override val repRequestTitle = "申请联盟代表"
    override val repRequestBody = "你的联盟要使用这个应用的集结功能，需要有一名代表。开发者确认并批准后，这台手机就成为代表，可以指定指挥官。一台手机只能提交一个申请。"
    override val gameName = "游戏角色名"
    override val noteHint = "留言（可选，例如：我是 WBI 的盟主）"
    override val applyBtn = "申请"
    override val gameNameRequired = "请输入游戏角色名"
    override fun applied(group: String) = "已申请成为 ${group} 的联盟代表。请不时查看开发者是否已批准。"
    override val pendingTitle = "联盟代表等待批准"
    override fun pendingBody(group: String, name: String, ago: String) = "${group} · ${name}\n${ago}提交了申请，还没有批准。\n开发者批准后再打开这个窗口，就会切换到指挥官画面。"
    override val cancelRequest = "取消申请"
    override val requestCancelled = "已取消申请"
    override val notApprovedTitle = "没有获得批准"
    override fun notApprovedHead(group: String, name: String) = "${group} · ${name}\n"
    override val noReason = "开发者没有填写原因。"
    override fun devSaid(reason: String) = "开发者的话：“${reason}”"
    override val reapply = "重新申请"
    override val roomsLoadFailed = "无法加载房间列表"
    override val noRoomsMade = "还没有创建的房间"
    override fun allRoomsTitle(n: Int) = "全部房间（${n}个）"
    override val deleteRoom = "删除房间"
    override val deleteRoomConfirm = "将从服务器上删除这个房间和房间名单。无法恢复，原来在房间里的人需要重新进入。确定要删除吗？"
    override fun roomRemoved(code: String) = "已删除房间 ${code}"
    override fun days(n: Long) = "${n}天"
    override fun hours(n: Long) = "${n}小时"
    override fun minutes(n: Long) = "${n}分钟"
    override val newCodeBodyDev = "请填写代码是给谁的、属于哪个所属。用这个代码进来的指挥官只能创建和修改该所属的房间。代码只能使用一次。"
    override fun labelsHint(max: Int) = "备注名（多人请换行，最多${max}人）"
    override val ttlHead = "代码的有效期"
    override val repOneAtATime = "联盟代表代码一次只能生成一个"
    override fun someFailed(err: String) = "\n\n有一部分没有生成：${err}"
    override fun codesMadeTitle(n: Int) = "已生成${n}个代码"
    override fun codesMadeBody(group: String, ttl: String, warn: String) = "${group}\n已按每个人复制好要发送的说明文字。在 ${ttl} 内只能使用一次。${warn}"
    override val newLabelHint = "新的备注名"
    override val renamed = "已修改备注名"
    override val purgeBody = "将从服务器上删除已使用或已过期的代码。指挥官的备注名会先保存，所以名单保持不变。等待中的代码不会删除。"
    override val purgeBtn = "清理"
    override val purgeFailed = "无法清理"
    override fun purged(n: Int) = "已清理${n}个代码"
    override val ttlHour = "1小时"
    override val ttlDay = "24小时"
    override val ttlWeek = "7天"
    override val whoRep = "联盟代表"
    override fun commanderInvite(who: String, groupLine: String, code: String, ttl: String) = "[AutoClicker Pro ${who}邀请]${groupLine}\n指挥官代码: ${code}\n请在应用的验证画面 → 指挥官登录中输入这个代码。生成后 ${ttl} 内只能使用一次。"
    override fun dear(name: String) = "致 ${name}\n"
    override val directReg = "直接登记"
    override val noSeen = "没有上线记录"
    override val verUnknown = "版本未知"
    override fun exportHead(n: Int) = "已登记的指挥官 ${n}人\n"
    override fun exportPending(n: Int) = "\n等待中的代码 ${n}个\n"
    override val badRoom = "房间号不正确"
    override val badCode = "指挥官代码格式不对（以 AD- 开头的10个字符）"
    override val codeUnusable = "代码不存在、已被使用，或已过期（24小时）"
    override fun demoteFailed(why: String) = "新代表已设定，但原代表没能降级。请在指挥官管理中确认。(${why})"
    override val notAllowed = "服务器不允许（请确认开发者登记或服务器规则）"
    override val noConnection = "无法连接服务器"
    override fun serverError(code: Int) = "服务器错误 ${code}"
    override fun currentRepWarn(name: String, seen: String) = "现任代表：${name} · 最后上线 ${seen}。批准后代表会更换。"
    override fun sameRequests(n: Int) = "同一联盟有${n}件申请。只有一人能成为代表。"
    override val noGroup = "没有所属"
    override val copied = "📋 已复制"
    override fun moreLines(n: Int) = "\n… 另有${n}行"
    override val noKakao = "没有安装 KakaoTalk。请选择要用来发送的应用"
    override val noShareApp = "没有可以发送的应用"
    override fun shareLine(server: String, alliance: String) = "所属: ${server} 服务器 · ${alliance}"
    override val roomShareTitle = "发送房间号"
    override val roomNumber = "房间号"
    override fun roomShare(groupLine: String, code: String) = "[AutoClicker Pro 集结房间]\n${groupLine}集结房间号: ${code}\n(应用 > 集结房间中先设定所属，再选择房间)"
    override val leaderCodeTitle = "集结队长代码"
    override fun inviteRoomLine(room: String) = "\n集结房间号: ${room}（验证后在选择房间中选）"
    override fun leaderInvite(id: String, code: String, roomLine: String) = "[AutoClicker Pro 集结队长邀请]\nID: ${id}\n集结队长代码: ${code}\n把这段文字整段复制后，在应用中点“验证”就会自动填好。这个代码只在下面的所属中有效。${roomLine}"
    override val on = "已开启"
    override fun editDesc(title: String) = "修改 ${title}"
    override fun chooseMode(ver: String) = "选择模式 · ${ver}"
    override fun troopsN(n: Int) = "部队${n}支"
    override val beforeSave = "尚未保存位置"
    override val hunter = "猎人"
    override val newSeq = "+ 新建序列"
    override fun seqLimit(max: Int) = "最多可以保存${max}个序列"
    override val off = "关闭"
    override val reposition = "重新设定位置"
    override val renameSeq = "改名"
    override fun deleteSeqQ(name: String) = "要删除“${name}”吗？"
    override fun deleteSeqBody(n: Int) = "保存的${n}个位置会一起删除。"
    override fun seqDeleted(name: String) = "已删除序列“${name}”"
    override fun seqOn(name: String) = "序列“${name}”已开启 · 点天蓝色按钮从第1个开始点击"
    override val editSeq = "修改序列"
    override val newSeqTitle = "新建序列"
    override val nameWord = "名称"
    override val seqHelp = "把圆圈 1、2、3… 按编号顺序放到要点击的位置，然后保存"
    override val posCount = "位置数"
    override val posFewer = "减少位置"
    override val posMore = "增加位置"
    override val gapTitle = "位置之间的停顿时间"
    override val gapShorter = "缩短停顿时间"
    override val gapLonger = "延长停顿时间"
    override val posReadFailed = "⚠ 无法读取位置。请确认编号圆圈是否显示"
    override fun seqSaved(name: String, n: Int) = "✓ 已保存“${name}”（${n}处） · 点天蓝色按钮从第1个开始点击"
    override fun seqName(n: Int) = "序列 ${n}"
    override val seqWord = "序列"
    override fun seqSummary(n: Int, gap: String) = "${n}处 · 间隔${gap}秒"
    override fun readyLabel(n: Int) = "序列\n${n}处"
    override fun runningLabel(i: Int, n: Int) = "${i} / ${n}\n停止"
    override fun hunterSaved(n: Int) = "🐻 猎人位置保存完成！（部队${n}支）"
    override val hunterSaveFirst = "🐻 请先保存猎人位置（模式列表中猎人旁边的铅笔）"
    override fun hunterMarchN(i: Int, n: Int) = "🐻 第${i}/${n}支部队出征！"
    override val hunterFire = "🐻 猎人发射！"
    override val fireLabel = "🐻\n发射"
    override fun fireNo(n: Int) = "🐻\n${n}号"
    override fun firedLabel(n: Int) = "${n}号\n出征"
    override val hiddenOpened = "🐻 隐藏模式已开启！请在悬浮条的模式按钮中选择猎人或序列点击"
    override val hiddenAlready = "🐻 隐藏模式已经开启。"
    override val hunterOnToast = "🐻 猎人模式已开启 · 选好集结后点 🐻 发射按钮（要重新设定位置请长按模式按钮）"
    override val hunterNeedPos = "🐻 请先设定并保存要用的部队和出征按钮的位置"
    override val hunterOff = "🐻 猎人模式结束！"
    override val hunterSetupTitle = "设定猎人位置"
    override val hunterSetupHelp = "准星 → 放在出征按钮上\n圆圈 1、2、3… → 按编号顺序放在要用的部队旗帜上，然后保存\n(每点一次，从第1支开始依次出征)"
    override val troopCount = "部队数"
    override val troopFewer = "减少部队"
    override val troopMore = "增加部队"
    override val hunterSave = "保存猎人位置"
    override val hunterReadFailed = "⚠ 无法读取位置。请确认准星和部队标记是否显示"
    override val hunterReadyTitle = "🐻 按下后停顿"
    override val hunterReadySub = "点部队之前 · 每次0.05秒"
    override val hunterGapTitle = "部队 → 出征间隔"
    override val hunterGapSub = "部队之后到出征 · 每次0.05秒"
    override val hunterReadyShorter = "缩短停顿"
    override val hunterReadyLonger = "延长停顿"
    override val runIdle = "待机中"
    override val runRunning = "进行中"
    override val runCancelled = "已取消"
    override fun roomLine(code: String, teams: Int, assigned: Int, run: String, members: Int) = "${code} · 队伍${teams}支（已分配 ${assigned}） · ${run} · 名单${members}人"
    override fun detailHead(code: String, run: String, prep: String, wait: String) = "房间 ${code}\n状态 ${run} · 准备${prep}秒 · 集结${wait}秒\n\n队伍\n"
    override val noneParen = "（无）\n"
    override fun detailTeam(i: Int, team: String, leader: String, march: String, excluded: Boolean) = "${i}. ${team} · 集结队长 ${leader} · 行军${march}秒${if (excluded) " · 排除" else ""}\n"
    override fun membersN(n: Int) = "\n名单 ${n}人"
}

internal object MoreZhTw : MoreStr {
    override val shareListTitle = "傳送指揮官名單"
    override val shareListName = "指揮官名單"
    override fun pendingCodesNote(n: Int) = "會一併傳送${n}個等待中的指揮官代碼。請確認聊天物件是否正確。"
    override val noDeviceId = "無法獲取裝置 ID"
    override fun myIdBody(ver: String) = "要登記為開發者，請把這個 ID 寫在 Firebase 主控台的 owners 下面。解除安裝後重新安裝應用程式，ID 會改變。\n\n應用程式版本：v${ver}"
    override val deviceIdWord = "裝置 ID"
    override val checkingServer = "正在確認伺服器…"
    override val devOnlyToast = "僅開發者可用。開發者需要在 Firebase 主控台填寫這台裝置的 ID（指揮官選單 → 檢視我的裝置 ID）"
    override val serverCheckRules = "無法向伺服器確認。請檢查網路連線和伺服器規則是否已生效"
    override val listLoadFailed = "無法載入列表"
    override val justAgo = "剛剛"
    override val allianceHintCase = "聯盟（例如 WBI，區分大小寫）"
    override val repOn = "✓ 設為聯盟代表"
    override val repOff = "不設為聯盟代表（點擊可設定）"
    override val newCommanderCode = "+ 生成指揮官代碼"
    override fun repRequestsBtn(n: Int) = "聯盟代表申請 ${n}件"
    override val allRooms = "全部房間列表"
    override val sendList = "傳送名單"
    override val purge = "清理代碼"
    override fun registeredHead(n: Int) = "已登記的指揮官（${n}人）"
    override val noneYetDot = "還沒有。"
    override val repSuffix = " · 聯盟代表"
    override fun activityRegistered(activity: String, day: String) = "${activity} · 登記於 ${day}"
    override val changeGroup = "更改所屬"
    override val rename = "修改備註名"
    override fun pendingHead(n: Int) = "等待中的代碼（${n}個）"
    override val noneDot = "沒有。"
    override fun remainLeft(t: String) = "剩餘 ${t}"
    override val send = "傳送"
    override val sendCodeTitle = "傳送指揮官代碼"
    override val commanderCode = "指揮官代碼"
    override fun setGroupBody(label: String, id: String) = "設定 ${label} (ID …${id}) 是哪個所屬的指揮官。那台手機下次開啟應用程式時會變成這個所屬。"
    override fun groupSetTo(label: String) = "所屬已設為 ${label}"
    override fun requestsTitle(n: Int) = "聯盟代表申請（${n}件）"
    override val requestsBody = "應用程式無法確認申請人是否真的屬於那個聯盟。請根據留言判斷。"
    override fun requestedAgo(ago: String, id: String) = "${ago}申請 · ID …${id}"
    override val noNote = "（沒有留言）"
    override val reject = "拒絕"
    override val approve = "批准"
    override fun swapNote(name: String) = "\n現任代表 ${name} 將降為普通指揮官。"
    override val noName = "沒有名字"
    override fun overNote(after: Int, max: Int) = "\n這個聯盟的指揮官將變成${after}人（上限${max}人）。新代表需要先移除一人才能再生成代碼。"
    override val approveTitle = "批准聯盟代表"
    override val replaceTitle = "更換聯盟代表"
    override fun approveBody(name: String, group: String, extra: String) = "要把 ${name} 定為 ${group} 的聯盟代表嗎？代表可以直接指定和移除該聯盟的指揮官。${extra}"
    override fun madeRep(name: String) = "已把 ${name} 定為聯盟代表"
    override val rejectTitle = "拒絕申請"
    override fun rejectBody(group: String, name: String) = "${group} · ${name}\n請寫一行會顯示在申請人畫面上的話。可以留空。"
    override val rejectHint = "例如：已經有代表了"
    override val rejected = "已拒絕申請"
    override val noDeviceIdNet = "無法獲取裝置 ID。請檢查網路連線"
    override val repOnlyToast = "僅聯盟代表可用。"
    override fun allianceCodeNote(group: String) = "使用這個代碼的人會成為 ${group} 的指揮官。他不能指定其他指揮官。"
    override fun allianceFull(max: Int) = "一個聯盟最多${max}名指揮官。要再新增請先移除一人。"
    override fun allianceHead(n: Int, max: Int) = "本聯盟指揮官（${n} / ${max}人）"
    override val meSuffix = "（我）"
    override val repShort = " · 代表"
    override val removeCommanderTitle = "移除指揮官"
    override fun removeCommanderBody(label: String) = "要取消 ${label} 的指揮官權限嗎？那台手機下次開啟應用程式時會回到集結隊長畫面。"
    override fun removeCommanderBodyId(label: String, id: String) = "要取消 ${label} (ID …${id}) 的指揮官權限嗎？那台手機下次開啟應用程式時會回到集結隊長畫面。"
    override val removed = "已移除"
    override val codeCancelled = "已取消代碼"
    override val newCodeTitle = "新的指揮官代碼"
    override fun newCodeBodyRep(group: String) = "請填寫將成為 ${group} 指揮官的人的備註名。代碼在24小時內只能使用一次。"
    override val labelHint = "備註名"
    override val labelRequired = "請輸入備註名"
    override val codeMakeFailed = "無法生成代碼"
    override fun rulesMaybe(err: String) = "${err}\n(新的伺服器規則可能還沒有發佈)"
    override val approvedPreview = "已批准。如果是真實使用者，這裡會切換到指揮官畫面。（現在是預覽，畫面不變）"
    override val repRequestTitle = "申請聯盟代表"
    override val repRequestBody = "你的聯盟要使用這個應用程式的集結功能，需要有一名代表。開發者確認並批准後，這台手機就成為代表，可以指定指揮官。一台手機只能提交一個申請。"
    override val gameName = "遊戲角色名"
    override val noteHint = "留言（可選，例如：我是 WBI 的盟主）"
    override val applyBtn = "申請"
    override val gameNameRequired = "請輸入遊戲角色名"
    override fun applied(group: String) = "已申請成為 ${group} 的聯盟代表。請不時檢視開發者是否已批准。"
    override val pendingTitle = "聯盟代表等待批准"
    override fun pendingBody(group: String, name: String, ago: String) = "${group} · ${name}\n${ago}提交了申請，還沒有批准。\n開發者批准後再開啟這個視窗，就會切換到指揮官畫面。"
    override val cancelRequest = "取消申請"
    override val requestCancelled = "已取消申請"
    override val notApprovedTitle = "沒有獲得批准"
    override fun notApprovedHead(group: String, name: String) = "${group} · ${name}\n"
    override val noReason = "開發者沒有填寫原因。"
    override fun devSaid(reason: String) = "開發者的話：「${reason}」"
    override val reapply = "重新申請"
    override val roomsLoadFailed = "無法載入房間列表"
    override val noRoomsMade = "還沒有建立的房間"
    override fun allRoomsTitle(n: Int) = "全部房間（${n}個）"
    override val deleteRoom = "刪除房間"
    override val deleteRoomConfirm = "將從伺服器上刪除這個房間和房間名單。無法恢復，原來在房間裡的人需要重新進入。確定要刪除嗎？"
    override fun roomRemoved(code: String) = "已刪除房間 ${code}"
    override fun days(n: Long) = "${n}天"
    override fun hours(n: Long) = "${n}小時"
    override fun minutes(n: Long) = "${n}分鐘"
    override val newCodeBodyDev = "請填寫代碼是給誰的、屬於哪個所屬。用這個代碼進來的指揮官只能建立和修改該所屬的房間。代碼只能使用一次。"
    override fun labelsHint(max: Int) = "備註名（多人請換行，最多${max}人）"
    override val ttlHead = "代碼的有效期"
    override val repOneAtATime = "聯盟代表代碼一次只能生成一個"
    override fun someFailed(err: String) = "\n\n有一部分沒有生成：${err}"
    override fun codesMadeTitle(n: Int) = "已生成${n}個代碼"
    override fun codesMadeBody(group: String, ttl: String, warn: String) = "${group}\n已按每個人複製好要傳送的說明文字。在 ${ttl} 內只能使用一次。${warn}"
    override val newLabelHint = "新的備註名"
    override val renamed = "已修改備註名"
    override val purgeBody = "將從伺服器上刪除已使用或已過期的代碼。指揮官的備註名會先儲存，所以名單保持不變。等待中的代碼不會刪除。"
    override val purgeBtn = "清理"
    override val purgeFailed = "無法清理"
    override fun purged(n: Int) = "已清理${n}個代碼"
    override val ttlHour = "1小時"
    override val ttlDay = "24小時"
    override val ttlWeek = "7天"
    override val whoRep = "聯盟代表"
    override fun commanderInvite(who: String, groupLine: String, code: String, ttl: String) = "[AutoClicker Pro ${who}邀請]${groupLine}\n指揮官代碼: ${code}\n請在應用程式的驗證畫面 → 指揮官登入中輸入這個代碼。生成後 ${ttl} 內只能使用一次。"
    override fun dear(name: String) = "致 ${name}\n"
    override val directReg = "直接登記"
    override val noSeen = "沒有上線記錄"
    override val verUnknown = "版本未知"
    override fun exportHead(n: Int) = "已登記的指揮官 ${n}人\n"
    override fun exportPending(n: Int) = "\n等待中的代碼 ${n}個\n"
    override val badRoom = "房間號不正確"
    override val badCode = "指揮官代碼格式不對（以 AD- 開頭的10個字元）"
    override val codeUnusable = "代碼不存在、已被使用，或已過期（24小時）"
    override fun demoteFailed(why: String) = "新代表已設定，但原代表沒能降級。請在指揮官管理中確認。(${why})"
    override val notAllowed = "伺服器不允許（請確認開發者登記或伺服器規則）"
    override val noConnection = "無法連線伺服器"
    override fun serverError(code: Int) = "伺服器錯誤 ${code}"
    override fun currentRepWarn(name: String, seen: String) = "現任代表：${name} · 最後上線 ${seen}。批准後代表會更換。"
    override fun sameRequests(n: Int) = "同一聯盟有${n}件申請。只有一人能成為代表。"
    override val noGroup = "沒有所屬"
    override val copied = "📋 已複製"
    override fun moreLines(n: Int) = "\n… 另有${n}行"
    override val noKakao = "沒有安裝 KakaoTalk。請選擇要用來傳送的應用程式"
    override val noShareApp = "沒有可以傳送的應用程式"
    override fun shareLine(server: String, alliance: String) = "所屬: ${server} 伺服器 · ${alliance}"
    override val roomShareTitle = "傳送房間號"
    override val roomNumber = "房間號"
    override fun roomShare(groupLine: String, code: String) = "[AutoClicker Pro 集結房間]\n${groupLine}集結房間號: ${code}\n(應用程式 > 集結房間中先設定所屬，再選擇房間)"
    override val leaderCodeTitle = "集結隊長代碼"
    override fun inviteRoomLine(room: String) = "\n集結房間號: ${room}（驗證後在選擇房間中選）"
    override fun leaderInvite(id: String, code: String, roomLine: String) = "[AutoClicker Pro 集結隊長邀請]\nID: ${id}\n集結隊長代碼: ${code}\n把這段文字整段複製後，在應用程式中點「驗證」就會自動填好。這個代碼只在下面的所屬中有效。${roomLine}"
    override val on = "已開啟"
    override fun editDesc(title: String) = "修改 ${title}"
    override fun chooseMode(ver: String) = "選擇模式 · ${ver}"
    override fun troopsN(n: Int) = "部隊${n}支"
    override val beforeSave = "尚未儲存位置"
    override val hunter = "獵人"
    override val newSeq = "+ 新建序列"
    override fun seqLimit(max: Int) = "最多可以儲存${max}個序列"
    override val off = "關閉"
    override val reposition = "重新設定位置"
    override val renameSeq = "改名"
    override fun deleteSeqQ(name: String) = "要刪除「${name}」嗎？"
    override fun deleteSeqBody(n: Int) = "儲存的${n}個位置會一起刪除。"
    override fun seqDeleted(name: String) = "已刪除序列「${name}」"
    override fun seqOn(name: String) = "序列「${name}」已開啟 · 點天藍色按鈕從第1個開始點擊"
    override val editSeq = "修改序列"
    override val newSeqTitle = "新建序列"
    override val nameWord = "名稱"
    override val seqHelp = "把圓圈 1、2、3… 按編號順序放到要點擊的位置，然後儲存"
    override val posCount = "位置數"
    override val posFewer = "減少位置"
    override val posMore = "增加位置"
    override val gapTitle = "位置之間的停頓時間"
    override val gapShorter = "縮短停頓時間"
    override val gapLonger = "延長停頓時間"
    override val posReadFailed = "⚠ 無法讀取位置。請確認編號圓圈是否顯示"
    override fun seqSaved(name: String, n: Int) = "✓ 已儲存「${name}」（${n}處） · 點天藍色按鈕從第1個開始點擊"
    override fun seqName(n: Int) = "序列 ${n}"
    override val seqWord = "序列"
    override fun seqSummary(n: Int, gap: String) = "${n}處 · 間隔${gap}秒"
    override fun readyLabel(n: Int) = "序列\n${n}處"
    override fun runningLabel(i: Int, n: Int) = "${i} / ${n}\n停止"
    override fun hunterSaved(n: Int) = "🐻 獵人位置儲存完成！（部隊${n}支）"
    override val hunterSaveFirst = "🐻 請先儲存獵人位置（模式列表中獵人旁邊的鉛筆）"
    override fun hunterMarchN(i: Int, n: Int) = "🐻 第${i}/${n}支部隊出征！"
    override val hunterFire = "🐻 獵人發射！"
    override val fireLabel = "🐻\n發射"
    override fun fireNo(n: Int) = "🐻\n${n}號"
    override fun firedLabel(n: Int) = "${n}號\n出征"
    override val hiddenOpened = "🐻 隱藏模式已開啟！請在懸浮條的模式按鈕中選擇獵人或序列點擊"
    override val hiddenAlready = "🐻 隱藏模式已經開啟。"
    override val hunterOnToast = "🐻 獵人模式已開啟 · 選好集結後點 🐻 發射按鈕（要重新設定位置請長按模式按鈕）"
    override val hunterNeedPos = "🐻 請先設定並儲存要用的部隊和出征按鈕的位置"
    override val hunterOff = "🐻 獵人模式結束！"
    override val hunterSetupTitle = "設定獵人位置"
    override val hunterSetupHelp = "準星 → 放在出徵按鈕上\n圓圈 1、2、3… → 按編號順序放在要用的部隊旗幟上，然後儲存\n(每點一次，從第1支開始依次出征)"
    override val troopCount = "部隊數"
    override val troopFewer = "減少部隊"
    override val troopMore = "增加部隊"
    override val hunterSave = "儲存獵人位置"
    override val hunterReadFailed = "⚠ 無法讀取位置。請確認準星和部隊標記是否顯示"
    override val hunterReadyTitle = "🐻 按下後停頓"
    override val hunterReadySub = "點部隊之前 · 每次0.05秒"
    override val hunterGapTitle = "部隊 → 出征間隔"
    override val hunterGapSub = "部隊之後到出征 · 每次0.05秒"
    override val hunterReadyShorter = "縮短停頓"
    override val hunterReadyLonger = "延長停頓"
    override val runIdle = "待機中"
    override val runRunning = "進行中"
    override val runCancelled = "已取消"
    override fun roomLine(code: String, teams: Int, assigned: Int, run: String, members: Int) = "${code} · 隊伍${teams}支（已分配 ${assigned}） · ${run} · 名單${members}人"
    override fun detailHead(code: String, run: String, prep: String, wait: String) = "房間 ${code}\n狀態 ${run} · 準備${prep}秒 · 集結${wait}秒\n\n隊伍\n"
    override val noneParen = "（無）\n"
    override fun detailTeam(i: Int, team: String, leader: String, march: String, excluded: Boolean) = "${i}. ${team} · 集結隊長 ${leader} · 行軍${march}秒${if (excluded) " · 排除" else ""}\n"
    override fun membersN(n: Int) = "\n名單 ${n}人"
}
