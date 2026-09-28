# 구글 플레이 콘솔 등록 & 심사 100% 통과 가이드

구글 플레이 콘솔([play.google.com/console](https://play.google.com/console))에 접속하여 앱을 등록하고 심사를 통과하기 위한 단계별 완벽 가이드입니다. 아래 순서대로 그대로 진행하시면 됩니다.

---

## 📋 [준비물 체크리스트]

모든 필수 파일은 이미 프로젝트 내에 완벽하게 준비되어 있습니다:

| 준비물 항목 | 준비된 파일 위치 | 비고 |
| :--- | :--- | :--- |
| **1. 구글 배포용 앱 파일** | [`AutoClicker-Pro.aab`](file:///e:/project/workspace/AutoClicker/AutoClicker-Pro.aab) | 서명(Sign) 완료된 릴리즈 번들 |
| **2. 앱 아이콘** | [`store_assets/app_icon_512.png`](file:///e:/project/workspace/AutoClicker/store_assets/app_icon_512.png) | 512x512 고해상도 PNG |
| **3. 그래픽 이미지** | [`store_assets/feature_graphic_1024x500.png`](file:///e:/project/workspace/AutoClicker/store_assets/feature_graphic_1024x500.png) | 1024x500 배너 이미지 |
| **4. 개인정보처리방침** | [`store_assets/privacy_policy.html`](file:///e:/project/workspace/AutoClicker/store_assets/privacy_policy.html) | 웹 링크(URL) 변환 필요 |
| **5. 앱 스토어 문구** | [`store_assets/store_listing.md`](file:///e:/project/workspace/AutoClicker/store_assets/store_listing.md) | 복사-붙여넣기 텍스트 |
| **6. 스크린샷 2~4장** | 스마트폰 또는 블루스택 화면 캡처 | 앱 메인 1장 + 게임 위 조준점 1장 |
| **7. 구글 개발자 계정** | [play.google.com/console](https://play.google.com/console) | 1회 등록비 $25 (해외결제 카드) |

---

## 🚀 [단계별 등록 절차]

### STEP 1. 개인정보처리방침 URL 만들기 (3분 소요)
구글은 접근성 권한을 쓰는 앱에 **유효한 인터넷 링크(URL)**의 개인정보처리방침을 의무화하고 있습니다.
- **가장 쉬운 방법 (GitHub Gist)**:
  1. [gist.github.com](https://gist.github.com) 접속 (GitHub 로그인)
  2. 파일 이름에 `privacy_policy.md` 입력
  3. [`store_assets/PRIVACY_POLICY.md`](file:///e:/project/workspace/AutoClicker/store_assets/PRIVACY_POLICY.md) 파일 내용을 복사해서 본문에 붙여넣기
  4. **[Create public gist]** 클릭 후 상단 브라우저 주소 복사 (이 주소가 개인정보처리방침 URL입니다!)
- 또는 본인의 네이버 블로그, 티스토리, 노션(공개 공유 링크)에 해당 내용을 그대로 올려도 됩니다.

---

### STEP 2. 구글 플레이 콘솔에서 앱 만들기
1. [구글 플레이 콘솔](https://play.google.com/console) 로그인 후 우측 상단 **[앱 만들기]** 클릭
2. **앱 이름**: `오토클리커 Pro - 자동 터치 매크로`
3. **기본 언어**: `한국어 - ko-KR`
4. **앱 또는 게임**: `앱`
5. **무료 또는 유료**: `무료`
6. 하단 약관 2개 체크 후 **[앱 만들기]** 클릭

---

### STEP 3. [앱 콘텐츠] 필수 정책 설문 (★심사 통과 핵심!)
좌측 메뉴 **[정책 및 프로그램] > [앱 콘텐츠]**로 이동하여 설문들을 작성합니다:

#### 1) 개인정보처리방침
- STEP 1에서 만든 Gist 또는 블로그 URL 입력

#### 2) 광고
- **"아니요, 내 앱에 광고가 포함되어 있지 않습니다"** 선택

#### 3) 앱 액세스 권한
- **"모든 기능이 특별한 액세스 권한 없이 제공됩니다"** 선택 (접근성은 시스템 권한이라 여기에 해당 없음)

#### 4) 콘텐츠 등급 (IARC)
- 이메일 주소 입력
- 카테고리: **"유틸리티, 생산성, 통신 또는 기타"**
- 모든 질문(폭력성, 성적 내용, 비속어 등): 전부 **"아니요"** 선택
- 결과: **전체이용가 (3세 이상)** 승인

#### 5) 타겟층 및 콘텐츠
- 타겟 연령대: **18세 이상**에 체크 (어린이 대상 심사 지연을 피하기 위해 강력 권장)
- 어린이에게 매력적인가요?: **"아니요"**

#### 6) 데이터 보안 (Data Safety)
- 앱에서 사용자 데이터를 수집하거나 공유하나요?: **"아니요"**

#### 7) 접근성 서비스 API 선언 (Accessibility Tool) ★최중요★
구글이 접근성 권한 사용 앱을 심사할 때 가장 엄격하게 보는 항목입니다. 아래대로 작성하세요:
- "이 앱이 접근성 도구인가요?": **"예"** 또는 **"접근성 API를 사용함"**
- **사용 목적 소명 텍스트 (영문 복사-붙여넣기)**:
  ```text
  This app uses the AccessibilityService API strictly to dispatch automated click and tap gestures on the screen at coordinates designated by the user. The app does not collect, read, store, or transmit any user data, on-screen text, personal credentials, or financial information.
  ```
- **접근성 시연 동영상 URL (필수 요구 시)**:
  스마트폰에서 앱을 켜고 접근성 권한을 켠 뒤 게임 위에서 과녁을 찍어 연타가 되는 30초짜리 화면 녹화 영상을 **YouTube에 [일부 공개]**로 올리고 링크를 첨부합니다.

---

### STEP 4. [스토어 등록정보 설정] 이미지 & 설명 입력
좌측 메뉴 **[성장] > [스토어 등록정보] > [기본 스토어 등록정보]** 이동:

1. **앱 이름**: `오토클리커 Pro - 자동 터치 매크로`
2. **간단한 설명**: `게임 연타를 위한 가장 단순하고 안전한 무한 오토클리커`
3. **자세한 설명**: [`store_assets/store_listing.md`](file:///e:/project/workspace/AutoClicker/store_assets/store_listing.md)의 [자세한 설명] 본문 복사-붙여넣기
4. **그래픽 자산 업로드**:
   - **앱 아이콘 (512x512)**: `store_assets/app_icon_512.png` 업로드
   - **그래픽 이미지 (1024x500)**: `store_assets/feature_graphic_1024x500.png` 업로드
   - **스마트폰 스크린샷 (최소 2장)**: 스마트폰 또는 블루스택에서 캡처한 이미지 업로드

---

### STEP 5. [출시] → AAB 파일 업로드

1. 좌측 메뉴 **[출시] > [프로덕션]** 클릭 (신규 개인 계정인 경우 **[비공개 테스트]**)
2. 우측 상단 **[새 버전 만들기]** 클릭
3. **App Bundle 업로드**:
   - 최상위 폴더의 [`AutoClicker-Pro.aab`](file:///e:/project/workspace/AutoClicker/AutoClicker-Pro.aab) 파일을 드래그하여 업로드합니다.
4. **버전 이름**: `1.0.0`
5. **출시 노트**:
   ```text
   [v1.0.0 정식 출시]
   • 초간편 원스위치 오토클리커 Pro 출시
   • 독립 분리형 과녁 & 옆라인 자석 스냅(Magnetic Snap) 컨트롤러
   • 물리 볼륨 키를 활용한 0.1초 즉시 긴급 정지
   • 3가지 반복 조건 (무한, 횟수 지정, 타이머 지정) 및 인게임 실시간 설정 팝업
   • 세로 5단계 투명도 레벨 조절 지원
   ```
6. **[저장]** 후 **[버전 검토]** 클릭

---

### STEP 6. 신규 개인 개발자 계정 필수 확인 사항 (20인 테스트)
> **💡 참고 (구글 정책 안내)**:
> 2023년 11월 이후 생성된 **신규 개인 개발자 계정**의 경우, 프로덕션 정식 출시 전에 **[비공개 테스트] 트랙에서 최소 20명의 테스터가 14일 동안 연속 참여**해야 프로덕션 출시 신청이 활성화됩니다.
> - **조직/사업자 계정인 경우**: 이 과정 없이 바로 [프로덕션] 출시 검토 가능.
> - **개인 계정인 경우**: 지인, 안드로이드 개발 커뮤니티(오픈채팅방, 디스코드, 레딧 r/AndroidClosedTesting 등)를 통해 20명을 등록하고 14일 후 [프로덕션 신청] 버튼을 누르면 됩니다.

---

### STEP 7. 심사 제출 및 승인
- 모든 설정 및 설문이 초록색 체크(완료)되면 **[검토로 전송]** 버튼이 활성화됩니다.
- 구글 심사는 보통 **24시간 ~ 3일(영업일 기준)** 내에 완료되며, 승인 시 구글 플레이스토어에 전 세계 공개 배포됩니다!
