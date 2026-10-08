# 오토클리커 Pro — 작업 안내 (Claude용)

안드로이드 앱 `android-app/` (Kotlin, minSdk 24, 패키지 `com.sejun.autoclicker`).
게임에서 여러 군단이 성에 **동시에 도착**하도록 각 폰이 정해진 시각에 집결 버튼을 자동 클릭한다.
방 상태는 Firebase Realtime DB(REST)로 공유한다.

## 사용자와 일하는 방식
- 사용자는 git을 쓰지 않는다. git 처리는 전부 Claude가 한다.
- 순서: 브랜치 만들기 → 고치기 → 푸시 → GitHub CI(`.github/workflows/android-unit-test.yml`, 컴파일+유닛 테스트) 통과 확인 → `main`에 fast-forward로 반영.
- 빌드는 사용자가 PC에서 `build-apk.bat` 실행. 스크립트가 GitHub `main`을 받아 빌드하고 APK를 `G:\내 드라이브\공유`에 복사한다. 끝나면 "build-apk.bat 실행"과 반영된 커밋 번호(앞 7자리)를 알려 준다.
- 보고는 한국어, 쉬운 말로. CI 통과와 "폰에서 직접 확인했는지"를 구분해서 말한다(이 환경에서는 폰 확인 불가).
- 화면 디자인을 바꿀 때는 먼저 시안(디자인 캔버스 "집결 패널 개선안")을 보여 주고 확인받은 뒤 코드에 옮긴다.
- 수정 요청은 몇 개씩 모아 한 번에 처리하는 편이 빌드·CI 횟수가 적다.

## 역할
- 개발자(소유자): 탭 3개(일반 화면·집결장·관리자) 모두.
- 관리자: 방을 만들고 군단 편집·배정, 집결 시작·취소. 관리자 관리 창에서 집결장 코드 발급.
- 집결장: 관리자에게 받은 코드로 인증. 배정된 **내 군단의 행군 시간만** 고친다.
- 일반: 일반 연타 모드만.

## 정해 둔 규칙 (바꾸기 전에 사용자에게 확인)
- 군단은 방마다 **최대 10개**(`RallyRoomEdit.MAX_TEAMS`). 10개 이상이면 "+ 군단 추가"는 흐린 "최대 10개".
- 군단 **순서는 1군부터 고정**. 제외해도 자리를 옮기지 않는다. 제외 줄은 얇게(32dp, 막대·행군 시간 없음).
- 군단 목록은 **5줄**까지 보이고 그 이상은 목록 안에서 스크롤(`RallyPanelView.VISIBLE_ROWS`). 패널은 화면 높이의 86%를 넘지 않는다.
- 보정(내 기기·군단 보정): −/+ **0.5초씩**, 범위 ±5초. 숫자를 누르면 직접 입력.
- 행군 시간: −/+ 1초씩. 이동 준비: −/+ 1초씩, 새 방 기본 15초. 집결 대기: 3·5·10분 중 선택(기본 5분).
- 편집 값은 누르는 즉시 저장·전파된다. "완료"는 편집 화면을 닫을 뿐.
- 참여 군단 중 **미배정**이 있으면 시작 시 확인 창(미배정 제외하고 시작 / 그대로 시작 / 취소). 미배정 글씨는 노랑.
- 취소 직후 1.5초는 같은 자리의 "다시 집결"을 받지 않는다. 늦게 도착한 옛 번호(startSeq)의 상태는 무시한다.
- 접힌 상태(알약): 단계 이름·내 군단·방, 남은 시간, 펼치기 아이콘. ✕ 없음. 취소·대기일 때는 회색, 막대 숨김.
- 색: 대기 노랑 `#FBBF24` · 집결 파랑 `#3B82F6` · 행군 보라 `#A78BFA` · 도착 청록 `#2DD4BF`. 초록은 연결 상태 점과 저장 체크에만. 패널 바탕은 불투명 `#121A2C`(비침은 설정의 오버레이 투명도로만).
- 안드로이드 13 이상은 복사 시 시스템이 알려 주므로 앱 토스트는 그 아래 버전에서만(`TextShare.copiedNotice`).

## 주요 파일 (android-app/app/src/main/java/com/sejun/autoclicker)
- `RallyPanelView.kt` + `res/layout/layout_rally_panel.xml`, `item_rally_team_row.xml`: 인게임 집결 패널 화면.
- `RallyPanelHost.kt`: 패널 창 띄우기·이동·접기, 입력/선택 팝업 연결.
- `RallyDragLayout.kt`: 패널 드래그와 목록 높이 맞춤(목록 위 세로 드래그는 스크롤).
- `RallyRoomSync.kt`: 서버 동기화(실시간 스트림+폴링), 시작·취소, 내 클릭 예약.
- `RallyRoomDoc.kt`: 방 문서와 편집 규칙(`RallyRoomEdit`). `RallySchedule.kt`: 클릭 시각 계산. `RallyScreenModel.kt`: 화면 모델.
- `RallyPickPopup.kt`(방 선택·배정 팝업), `RallyInputPopup.kt`(숫자 입력).
- 앱 화면: `MainActivity.kt`, `RoleSwitch.kt`, 하단 시트 `SheetDialog.kt`, `InputSheet.kt`, 관리자 관리 `AdminRosterUi.kt`, 공유 `TextShare.kt`.
- 서버 규칙: `database.rules.next.json`.
- 테스트: `android-app/app/src/test/java/com/sejun/autoclicker/` (화면 없는 계산·규칙은 테스트를 함께 추가).

## 알아 둘 것
- 커밋 메시지는 한국어, 무엇을 왜 바꿨는지 한두 줄.
- 오래된 서명 키 등 비밀값이 예전 git 기록에 남아 있다는 지적이 있었다. 비밀값은 절대 출력·커밋하지 않는다.
- Firebase 규칙 강화(행군 시간·명단 쓰기 권한 등)는 아직 남은 과제다.
