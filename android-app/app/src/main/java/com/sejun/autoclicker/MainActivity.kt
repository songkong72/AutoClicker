package com.sejun.autoclicker

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.sejun.autoclicker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private fun checkAppVersion() {
        Thread {
            try {
                val url = java.net.URL("https://autoclicker-6f8d7-default-rtdb.asia-southeast1.firebasedatabase.app/appInfo.json")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 3000
                if (conn.responseCode == 200) {
                    val res = conn.inputStream.bufferedReader().readText()
                    if (res != "null") {
                        val json = org.json.JSONObject(res)
                        val latestVersion = json.optInt("latestVersionCode", packageManager.getPackageInfo(packageName, 0).versionCode)
                        val downloadUrl = json.optString("downloadUrl", "")
                        if (latestVersion > packageManager.getPackageInfo(packageName, 0).versionCode && downloadUrl.isNotEmpty()) {
                            runOnUiThread {
                                showUpdateDialog(downloadUrl)
                            }
                        }
                    }
                }
            } catch (e: Exception) {}
        }.start()
    }

    private fun showUpdateDialog(url: String) {
        // 중복 방지
        if (isFinishing) return
        SheetDialog(
            this, "새로운 버전 업데이트",
            "연맹 필수 업데이트예요. 새 버전의 오토클리커 Pro를 지금 받을까요?",
            cancelable = false
        ).actions(
            SheetDialog.act("나중에"),
            SheetDialog.act("다운로드", SheetDialog.Kind.PRIMARY) {
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
            }
        ).show()
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 구글 플레이 필수 심사 요건: 접근성 API 사용 사전 고지 팝업
        ProminentDisclosureDialog.showIfNeeded(
            context = this,
            onAgreed = {
                // 정상 진행
            },
            onDeclined = {
                finish() // 거절 시 종료
            }
        )

        setupListeners()

    }

    override fun onResume() {
        super.onResume()
        checkAppVersion()
        verifyServerAdmin()
        updateAuthUI()
        updateRallyInfoCard()
        updatePermissionStates()
        updateServiceState()
        bindServiceCallbacks()
    }

    override fun onPause() {
        super.onPause()
        AutoClickService.instance?.onStatusChanged = null
        AutoClickService.instance?.onOverlaysVisibilityChanged = null
    }

    private fun bindServiceCallbacks() {
        val service = AutoClickService.instance ?: return
        service.onStatusChanged = {
            runOnUiThread {
                updateServiceState()
            }
        }
        service.onOverlaysVisibilityChanged = {
            runOnUiThread {
                updateServiceState()
            }
        }
    }

    private fun setupListeners() {
        setupRoomCard()

        // 회원 인증 관리 버튼
        binding.btnAuthAction.setOnClickListener {
            when {
                // 일반 화면 미리보기에서는 일반 사용자와 똑같이 움직인다(개발자가 인증·대표 신청 화면을 시험해 볼 수 있게)
                PreferencesHelper.isUserView(this) ->
                    if (roomPrefs().getString(AdminRosterUi.KEY_REQUEST, null) != null) openRepRequest() else showVerificationDialog()
                PreferencesHelper.isAdminMode(this) -> showAdminMenu()
                PreferencesHelper.isRosterAdmin(this) -> enterFromRoster(askCodeIfNot = true)
                roomPrefs().getString(AdminRosterUi.KEY_REQUEST, null) != null -> openRepRequest()
                else -> showVerificationDialog()
            }
        }

        // 연타 설정 바꾸기: 앱의 다른 창처럼 밝은 아래 창으로 연다(값은 게임 위 설정 창과 같은 곳에 저장한다)
        binding.cardClickSettings.setOnClickListener { showClickSettingsSheet() }

        // 상단 지휘관 설정 아이콘
        binding.btnAdminIcon.setOnClickListener {
            // 이미 지휘관이면 로그인 창을 다시 띄우지 않고 관리 메뉴를 연다. 일반 화면 미리보기에서는 일반 사용자처럼 로그인 창을 보여 준다.
            if (PreferencesHelper.isAdminMode(this) && !PreferencesHelper.isUserView(this)) showAdminMenu() else showAdminLoginDialog()
        }

        // 지휘관으로 등록된 기기: 한 번 눌러 지휘관 ↔ 집결장 화면을 오간다(코드를 다시 받지 않는다)
        // 화면 전환 막대: 지금 화면이 아닌 칸을 누르면 그 화면으로 간다(지휘관은 누를 때마다 서버에 다시 확인한다)
        binding.segUser.setOnClickListener { if (!PreferencesHelper.isUserView(this)) enterUserView() }
        binding.segLeader.setOnClickListener {
            if (PreferencesHelper.isUserView(this)) setUserView(false)
            if (PreferencesHelper.isAdminMode(this)) switchToLeader()
        }
        binding.segAdmin.setOnClickListener {
            if (PreferencesHelper.isUserView(this)) setUserView(false)
            if (!PreferencesHelper.isAdminMode(this)) enterFromRoster(askCodeIfNot = true)
        }

        // 상단 타이틀 5회 연속 탭 시 지휘관 진입 (히든 제스처)
        var titleTapCount = 0
        var lastTitleTapTime = 0L
        binding.tvAppTitle.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastTitleTapTime > 1500L) {
                titleTapCount = 0
            }
            lastTitleTapTime = now
            titleTapCount++
            if (titleTapCount >= 5) {
                titleTapCount = 0
                if (PreferencesHelper.isUserView(this)) return@setOnClickListener
                if (PreferencesHelper.isAdminMode(this)) showAdminMenu() else showAdminLoginDialog()
            }
        }

        // Accessibility Permission Button (Only 1 setting required!)
        binding.btnGrantAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }

        // Emergency Stop Button
        binding.btnEmergencyStop.setOnClickListener {
            val service = AutoClickService.instance
            service?.stopAutoClick()
            Toast.makeText(this, "치료 연타를 바로 멈췄어요.", Toast.LENGTH_SHORT).show()
            updateServiceState()
        }

        // ⚔️ 집결 동시 착탄 및 그룹 작전 설정 버튼
        binding.btnOpenRallySettings.setOnClickListener {
            if (!PreferencesHelper.hasAccess(this)) {
                Toast.makeText(this, "집결장 코드로 인증한 뒤에 쓸 수 있어요.", Toast.LENGTH_SHORT).show()
                showVerificationDialog()
                return@setOnClickListener
            }

            if (!hasAccessibilityPermission()) {
                Toast.makeText(this, "스위치를 먼저 켜야 게임을 자동으로 터치할 수 있어요.", Toast.LENGTH_LONG).show()
                openAccessibilitySettings()
                return@setOnClickListener
            }

            val service = AutoClickService.instance
            if (service == null) {
                Toast.makeText(this, "아직 준비 중이에요. 잠시 후 다시 눌러 주세요.\n(계속 안 되면 접근성을 껐다가 다시 켜 주세요)", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (group() == null) { showGroupSheet(); return@setOnClickListener }
            if ((roomPrefs().getString("cloud_room_number", "") ?: "").isEmpty()) {
                Toast.makeText(this, "먼저 집결 방에 입장해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            bindServiceCallbacks()

            if (!service.isOverlaysShowing()) {
                service.showOverlays()
            }
            service.toggleRallyPanel()
            Toast.makeText(this, "집결 화면을 띄웠어요.", Toast.LENGTH_SHORT).show()
            moveTaskToBack(true)
        }

        // Single Smart Toggle Button: [🚀 오토클리커 띄우기] ↔ [✕ 오토클리커 숨기기]
        binding.btnStartService.setOnClickListener {
            if (!hasAccessibilityPermission()) {
                Toast.makeText(this, "스위치를 먼저 켜야 게임을 자동으로 터치할 수 있어요.", Toast.LENGTH_LONG).show()
                openAccessibilitySettings()
                return@setOnClickListener
            }

            val service = AutoClickService.instance
            if (service == null) {
                Toast.makeText(this, "아직 준비 중이에요. 잠시 후 다시 눌러 주세요.\n(계속 안 되면 접근성을 껐다가 다시 켜 주세요)", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            bindServiceCallbacks()

            if (service.isOverlaysShowing()) {
                // 이미 화면에 떠 있으면 숨기기
                service.hideOverlays()
                Toast.makeText(this, "오토클리커를 숨겼어요.", Toast.LENGTH_SHORT).show()
            } else {
                // 화면에 없으면 띄우기
                service.showOverlays()
                Toast.makeText(this, "오토클리커를 띄웠어요. 게임으로 이동할게요.", Toast.LENGTH_SHORT).show()
                moveTaskToBack(true) // Switch to game immediately
            }
            updateServiceState()
        }
    }

    /**
     * 앱 첫 화면의 연타 설정 창. 게임 위 설정 창과 같은 값을 고치되, 모양은 앱의 다른 창처럼 밝은 아래 창이다.
     * 버튼 투명도는 게임 위 막대에만 쓰이는 값이라 여기에는 두지 않는다.
     */
    private fun showClickSettingsSheet() {
        val sheet = SheetDialog(this, "연타 설정")
        val density = resources.displayMetrics.density
        fun px(v: Int) = (v * density).toInt()
        val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
        fun lp(w: Int, h: Int, weight: Float = 0f, left: Int = 0) = LinearLayout.LayoutParams(w, h, weight).apply { leftMargin = px(left) }
        fun label(text: String, bold: Boolean = false) = TextView(this).apply {
            this.text = text; textSize = if (bold) 15f else 14f
            setTextColor(if (bold) SheetDialog.INK else SheetDialog.SUB)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
        fun numberBox(initial: String, decimal: Boolean = false) = EditText(this).apply {
            setText(initial); gravity = Gravity.CENTER; textSize = 16f; setTextColor(SheetDialog.INK); setTypeface(typeface, Typeface.BOLD)
            inputType = InputType.TYPE_CLASS_NUMBER or (if (decimal) InputType.TYPE_NUMBER_FLAG_DECIMAL else 0)
            filters = arrayOf(InputFilter.LengthFilter(5)); setSingleLine(true)
            background = sheet.box(SheetDialog.FIELD, 14); setPadding(0, 0, 0, 0)
        }
        /** 가로 한 줄을 만들어 창에 넣는다. */
        fun row(top: Int, vararg views: Pair<View, LinearLayout.LayoutParams>) = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(px(4), 0, px(4), 0)
            views.forEach { (v, p) -> addView(v, p) }
        }.also { sheet.content.addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, wrap).apply { topMargin = px(top) }) }
        /** 고른 칩만 남색으로 채운다. [on]이 -1이면 아무것도 고르지 않은 것. */
        fun paint(pills: List<TextView>, on: Int) = pills.forEachIndexed { i, pill ->
            pill.setTextColor(if (i == on) Color.WHITE else SheetDialog.INK)
            pill.background = sheet.box(if (i == on) SheetDialog.BLUE else SheetDialog.FIELD, 14)
        }

        // 클릭 주기: 초로 적는다. 칩을 누르면 입력 칸에 그 값이 들어간다.
        val etInterval = numberBox(ClickSummary.seconds(PreferencesHelper.getIntervalMs(this)), decimal = true)
        row(6, label("클릭 주기", bold = true) to lp(0, wrap, 1f), etInterval to lp(px(96), px(44)), label("초") to lp(wrap, wrap, left = 8))
        val presets = listOf(100L to "0.1초", 200L to "0.2초", 500L to "0.5초", 1000L to "1.0초")
        val speedPills = presets.map { (ms, text) -> sheet.pill(text) { etInterval.setText(ClickSummary.seconds(ms)) } }
        sheet.equalRow(*speedPills.toTypedArray())
        fun paintSpeed() = paint(speedPills, presets.indexOfFirst { it.first == ClickSummary.parseSeconds(etInterval.text.toString()) })
        etInterval.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) = paintSpeed()
        })
        paintSpeed()

        // 반복 조건: 고른 것에 따라 아래 한 줄이 바뀐다(무한 = 안내, 횟수·시간 = 입력 줄).
        sheet.heading("반복 조건")
        var mode = PreferencesHelper.getRepeatMode(this)
        val modes = listOf(RepeatMode.INFINITE to "무한", RepeatMode.COUNT to "횟수", RepeatMode.TIMER to "시간")
        val modePills = modes.map { (_, text) -> sheet.pill(text) { } }
        sheet.equalRow(*modePills.toTypedArray())
        val infiniteNote = sheet.line(sheet.content, "정지 버튼이나 볼륨 키를 누를 때까지 계속 연타해요.", small = true, top = 8).apply { setPadding(px(4), 0, px(4), 0) }
        val etCount = numberBox(PreferencesHelper.getRepeatCount(this).toString())
        val countRow = row(8, etCount to lp(px(84), px(44)), label("회") to lp(wrap, wrap, left = 8),
            *listOf("50", "100", "300").map { n -> sheet.pill(n) { etCount.setText(n) } to lp(0, px(44), 1f, left = 8) }.toTypedArray())
        val totalSec = PreferencesHelper.getRepeatDurationSec(this)
        val etMin = numberBox((totalSec / 60).toString())
        val etSec = numberBox((totalSec % 60).toString())
        val timerRow = row(8, etMin to lp(px(64), px(44)), label("분") to lp(wrap, wrap, left = 6),
            etSec to lp(px(64), px(44), left = 8), label("초") to lp(wrap, wrap, left = 6),
            *listOf(1, 3).map { m -> sheet.pill("${m}분") { etMin.setText(m.toString()); etSec.setText("0") } to lp(0, px(44), 1f, left = 8) }.toTypedArray())
        fun showMode() {
            paint(modePills, modes.indexOfFirst { it.first == mode })
            infiniteNote.visibility = if (mode == RepeatMode.INFINITE) View.VISIBLE else View.GONE
            countRow.visibility = if (mode == RepeatMode.COUNT) View.VISIBLE else View.GONE
            timerRow.visibility = if (mode == RepeatMode.TIMER) View.VISIBLE else View.GONE
        }
        modePills.forEachIndexed { i, pill -> pill.setOnClickListener { mode = modes[i].first; showMode() } }
        showMode()

        sheet.actions(
            SheetDialog.act("취소"),
            SheetDialog.Action("적용", SheetDialog.Kind.PRIMARY) {
                val intervalMs = ClickSummary.parseSeconds(etInterval.text.toString())
                if (intervalMs == null) {
                    Toast.makeText(this, "클릭 주기를 0보다 큰 숫자로 입력해 주세요 (예: 0.5)", Toast.LENGTH_SHORT).show()
                    return@Action false
                }
                PreferencesHelper.setIntervalMs(this, intervalMs)
                PreferencesHelper.setRepeatMode(this, mode)
                PreferencesHelper.setRepeatCount(this, etCount.text.toString().toIntOrNull() ?: 100)
                PreferencesHelper.setRepeatDurationSec(this, (etMin.text.toString().toIntOrNull() ?: 1) * 60 + (etSec.text.toString().toIntOrNull() ?: 0))
                updateServiceState() // 첫 화면의 요약 한 줄을 새 값으로
                true
            }
        ).show()
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(
            this,
            "목록에서 [오토클리커 Pro]를 찾아 켜주세요.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun updatePermissionStates() {
        val hasAccessibility = hasAccessibilityPermission()
        if (hasAccessibility) {
            binding.badgeAccessibility.text = getString(R.string.status_granted)
            binding.badgeAccessibility.setBackgroundResource(R.drawable.bg_badge_success)
            binding.badgeAccessibility.setTextColor(ContextCompat.getColor(this, R.color.success))
            // 허용된 뒤에는 "허용됨" 배지만 남긴다(누를 수 없는 "완료" 버튼이 같은 뜻으로 한 번 더 나오던 것)
            binding.btnGrantAccessibility.visibility = View.GONE
            binding.tvAccessibilityHint.visibility = View.GONE
            // 허용된 뒤에는 카드 대신 "화면 터치 허용됨" 한 줄만 보인다
            binding.cardAccessibility.visibility = View.GONE
            binding.rowAccessibilityOk.visibility = View.VISIBLE
        } else {
            binding.cardAccessibility.visibility = View.VISIBLE
            binding.rowAccessibilityOk.visibility = View.GONE
            binding.badgeAccessibility.text = getString(R.string.status_needed)
            binding.badgeAccessibility.setBackgroundResource(R.drawable.bg_badge_warning)
            binding.badgeAccessibility.setTextColor(ContextCompat.getColor(this, R.color.warning))
            binding.btnGrantAccessibility.visibility = View.VISIBLE
            binding.tvAccessibilityHint.visibility = View.VISIBLE
        }
        updateServiceState()
    }

    private fun updateServiceState() {
        val service = AutoClickService.instance
        val isShowing = service?.isOverlaysShowing() == true
        val isClicking = service?.isClicking == true

        if (isClicking) {
            binding.btnEmergencyStop.visibility = View.VISIBLE
            binding.btnEmergencyStop.text = "🛑 치료 연타 즉시 정지"
        } else {
            binding.btnEmergencyStop.visibility = View.GONE
        }

        if (isShowing) {
            // 이미 화면에 떠 있는 경우: 빨간색 [✕ 오토클리커 숨기기]
            binding.btnStartService.text = "✕ 오토클리커 숨기기"
            binding.btnStartService.setBackgroundColor(ContextCompat.getColor(this, R.color.danger))
        } else {
            // 화면에 없는 경우: 파란색 [🚀 오토클리커 띄우기]
            binding.btnStartService.text = "🚀 오토클리커 띄우기"
            binding.btnStartService.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
        }
        updateStartButtonVisibility()
        // 회원 화면에는 띄우기 버튼이 없으므로, 조작판이 떠 있는지는 제목에서 알 수 있게 한다
        binding.tvGeneralModeTitle.text = "일반 연타 모드" + (if (isShowing) " · 떠 있음" else "")
        binding.tvClickSummary.text = ClickSummary.text(
            PreferencesHelper.getIntervalMs(this), PreferencesHelper.getRepeatMode(this),
            PreferencesHelper.getRepeatCount(this), PreferencesHelper.getRepeatDurationSec(this)
        )
        updateRallyInfoCard()
    }

    /**
     * 띄우기/숨기기 버튼은 집결을 못 쓰는 사람(일반 사용자·개발자 미리보기)에게만 보인다.
     * 회원·지휘관은 "집결 화면 열기"가 조작판까지 띄워 주므로 이 버튼을 두지 않는다(큰 버튼은 늘 하나). 조작판은 조작판의 ✕ 로 끈다.
     */
    private fun updateStartButtonVisibility() {
        binding.btnStartService.visibility = if (!PreferencesHelper.hasAccess(this)) View.VISIBLE else View.GONE
    }

    private fun roomAuth() = FirebaseAuthClient(BuildConfig.FIREBASE_API_KEY,
        load = { roomPrefs().getString("fb_refresh", null) },
        save = { t -> roomPrefs().edit().putString("fb_refresh", t).apply() })

    private fun roomPrefs() = getSharedPreferences("AutoClickerPrefs", MODE_PRIVATE)

    /** 이 폰의 소속(서버·연맹). 아직 정하지 않았으면 null. */
    private fun group(): RallyGroup? = RallyGroup.load(roomPrefs())

    /** 서버에 쓰는 방 이름(소속 + 방 번호). 화면과 저장해 둔 값은 방 번호만 쓴다. */
    private fun roomKey(code: String): String = RallyGroup.keyFor(group(), code)

    /**
     * 소속을 정하는 창. 처음 한 번, 그리고 방 카드의 소속 표시를 눌렀을 때 뜬다. 저장하면 [then]을 부른다.
     * 소속이 바뀌면 들어와 있던 방에서 나오고, 이 폰이 기억하던 방 목록도 비운다(다른 소속의 방이라 더는 보이지 않는다).
     */
    private fun showGroupSheet(then: () -> Unit = {}) {
        if (groupLocked()) {
            Toast.makeText(this, "지휘관의 소속은 개발자나 연맹 대표가 정해요. 바꾸려면 그분께 말씀해 주세요.", Toast.LENGTH_LONG).show()
            return
        }
        if (leaderLocked()) {
            // 집결장의 소속은 받은 코드에 묶여 있다. 다른 소속으로 가려면 그 소속의 지휘관에게 새 코드를 받아 다시 인증한다.
            Toast.makeText(this, "소속은 지휘관에게 받은 집결장 코드로 정해져요. 바꾸려면 새 코드를 받아 다시 인증해 주세요.", Toast.LENGTH_LONG).show()
            showVerificationDialog()
            return
        }
        val current = group()
        // 아직 소속이 없을 때, 복사해 둔 초대 글에 소속이 있으면 그대로 채워 둔다(손으로 옮기다 대소문자를 틀리지 않게)
        val copied = if (current == null) InviteText.groupIn(clipboardText()) else null
        val shown = current ?: copied
        val sheet = SheetDialog(
            this, "소속 정하기",
            (if (copied != null) "복사해 둔 글에서 소속을 가져왔어요. 맞는지 보고 저장을 눌러 주세요.\n" else "") +
                "같은 소속끼리만 방이 보여요. 지휘관에게 받은 그대로 적어 주세요.\n연맹은 대문자와 소문자를 구분해요 (WBI와 wbi는 다른 소속)."
        )
        val server = sheet.field("서버 번호 (예: 2000)", RallyGroup.MAX_SERVER, InputType.TYPE_CLASS_NUMBER, shown?.server.orEmpty())
        // 키보드가 첫 글자를 대문자로 바꾸거나 자동 고침을 하지 않게 한다(대소문자를 구분하므로 적은 그대로 들어가야 한다).
        val alliance = sheet.field(
            "연맹 (예: WBI)", RallyGroup.MAX_ALLIANCE,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
            shown?.alliance.orEmpty(), top = 10
        )
        sheet.actions(
            SheetDialog.act("취소"),
            SheetDialog.Action("저장", SheetDialog.Kind.PRIMARY) {
                val problem = RallyGroup.problem(server.text?.toString(), alliance.text?.toString())
                val next = RallyGroup.of(server.text?.toString(), alliance.text?.toString())
                if (problem != null || next == null) {
                    Toast.makeText(this, problem ?: "소속을 다시 확인해 주세요.", Toast.LENGTH_SHORT).show()
                    false
                } else {
                    if (next != current) applyGroup(next, "소속을 ${next.label}(으)로 정했어요. 방을 골라 주세요.")
                    updateRallyInfoCard()
                    then()
                    true
                }
            }
        ).show()
    }

    /** 지금 복사해 둔 글. 없거나 읽지 못하면 빈 글. (안드로이드는 앱 화면이 앞에 있을 때만 읽게 해 준다) */
    private fun clipboardText(): String = try {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
    } catch (e: Exception) { "" }

    /** 소속을 바꾼다. 들어와 있던 방에서 나오고, 이 폰이 기억하던 방 목록도 비운다(다른 소속의 방이라 더는 보이지 않는다). */
    private fun applyGroup(next: RallyGroup, notice: String) {
        RallyGroup.save(roomPrefs(), next)
        roomPrefs().edit().remove("cloud_room_number").remove("cloud_room_creatable").apply()
        RallyRoomHistory.load(roomPrefs()).forEach { RallyRoomHistory.forget(roomPrefs(), it) }
        RoomListCache.save(roomPrefs(), emptyList())
        AutoClickService.instance?.leaveRallyRoom()
        rosterStatus = ""
        Toast.makeText(this, notice, Toast.LENGTH_LONG).show()
    }

    /**
     * 지휘관의 소속이 서버에서 정해져 있어 이 폰에서 바꿀 수 없는지. 개발자는 모든 소속을 다루므로 잠기지 않는다.
     * 값은 [syncServerGroup]이 서버 명단을 보고 적어 둔다.
     */
    /**
     * 집결장의 소속이 인증한 코드에 묶여 있어 직접 바꿀 수 없는지. 소속이 묶인 코드로 인증한 폰만 해당한다
     * (그 전에 인증한 집결장은 전처럼 직접 바꾼다). 지휘관은 [groupLocked]가 따로 정하고, 개발자는 잠기지 않는다.
     */
    private fun leaderLocked(): Boolean =
        roomPrefs().getBoolean("leader_group_locked", false) && !PreferencesHelper.isRosterAdmin(this) &&
            !roomPrefs().getBoolean("is_owner_cached", false)

    private fun groupLocked(): Boolean =
        roomPrefs().getBoolean("group_locked", false) && PreferencesHelper.isRosterAdmin(this) &&
            !roomPrefs().getBoolean("is_owner_cached", false)

    /**
     * 서버 명단의 내 항목에서 소속과 대표 여부를 읽어 이 폰에 맞춘다. 네트워크를 쓰므로 백그라운드 스레드에서 부른다.
     * 소속이 정해진 지휘관은 그 소속으로 바뀌고 잠긴다. 읽지 못하면 아무것도 바꾸지 않는다.
     */
    private fun syncServerGroup(server: AdminServer, uid: String, owner: Boolean) {
        val me = server.myAdmin(uid)
        if (me.error != null) return
        val entry = me.value
        val serverGroup = RallyGroup.fromId(entry?.group)
        val locked = serverGroup != null && !owner
        roomPrefs().edit().putBoolean("group_locked", locked).putBoolean("is_rep_cached", entry?.rep == true).apply()
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            if (locked && serverGroup != null && serverGroup != group()) {
                applyGroup(serverGroup, "소속이 ${serverGroup.label}(으)로 정해졌어요. 방을 골라 주세요.")
            }
            updateRallyInfoCard()
        }
    }

    /** 연맹 대표 신청 창을 연다. 승인돼 있으면 곧바로 지휘관 화면으로 들어간다. */
    private fun openRepRequest() {
        AdminRosterUi(this, adminServer()).showRepRequest(
            onApproved = { enterFromRoster(askCodeIfNot = false) },
            onChanged = { updateAuthUI() },
            preview = PreferencesHelper.isUserView(this)
        )
    }

    /** 입장 직후 명단 등록 결과(성공/실패 이유). 토스트가 안 보이는 기기가 있어 방 상태 글에 붙여 보여 준다. */
    private var rosterStatus = ""

    private fun updateRallyInfoCard() {
        val room = roomPrefs().getString("cloud_room_number", "") ?: ""
        val admin = PreferencesHelper.isAdminMode(this)
        val hasRoom = room.isNotEmpty()
        // 방 번호를 치지 않고 서버의 방 목록에서 고른다. 방이 없으면 큰 "방 선택", 있으면 "방 바꾸기".
        binding.btnPickRoom.visibility = if (hasRoom) View.GONE else View.VISIBLE
        binding.btnChangeRoom.visibility = if (hasRoom) View.VISIBLE else View.GONE
        binding.btnShareRoom.visibility = if (admin && hasRoom) View.VISIBLE else View.GONE
        binding.btnManage.visibility = if (admin) View.VISIBLE else View.GONE
        binding.rowRoomActions.visibility = if (hasRoom || admin) View.VISIBLE else View.GONE
        // 방이 이미 있는 지휘관에게는 "방을 만들고 공유하세요" 안내를 되풀이하지 않는다
        binding.tvAuthStatusSubtitle.visibility = if (admin && hasRoom) View.GONE else View.VISIBLE
        // 방 번호는 크게, 옆에 내 역할과 군단·배정 수(마지막으로 받은 방 목록 기준이라 없으면 비운다)
        binding.rowRoomNumber.visibility = if (hasRoom) View.VISIBLE else View.GONE
        binding.tvRoomNumber.text = room
        binding.tvRoleChip.text = if (admin) "지휘관" else "집결장"
        // 소속 표시: 정해 둔 소속, 없으면 정하라는 안내. 누르면 소속 정하기 창이 뜬다.
        binding.tvRoomGroup.text = (group()?.label ?: "소속 정하기") + if (groupLocked() || leaderLocked()) " 🔒" else " ›"
        binding.tvRoomSummary.text = RoomChooser.summaryFromLine(RoomListCache.load(roomPrefs()).firstOrNull { it.first == room }?.second).orEmpty()
        val status = listOf(
            when {
                hasRoom -> ""
                admin -> "방을 고르거나 만들어 주세요."
                else -> "방을 선택해 주세요."
            },
            rosterStatus
        ).filter { it.isNotEmpty() }.joinToString("\n")
        binding.tvRoomStatus.text = status
        binding.tvRoomStatus.visibility = if (status.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setupRoomCard() {
        binding.btnPickRoom.setOnClickListener { showRoomChooser() }
        binding.btnManage.setOnClickListener { showAdminMenu() }
        binding.btnRoomHelp.setOnClickListener {
            SheetDialog(this, "집결 방", "같은 방에 들어온 군단들이 동시에 성에 도착하도록 집결 클릭 시각이 자동 계산돼요.")
                .actions(SheetDialog.act("확인", SheetDialog.Kind.PRIMARY)).show()
        }
        binding.btnChangeRoom.setOnClickListener { showRoomChooser() }
        binding.tvRoomGroup.setOnClickListener { showGroupSheet() }
        binding.btnShareRoom.setOnClickListener {
            val code = roomPrefs().getString("cloud_room_number", "") ?: ""
            if (code.isEmpty()) {
                Toast.makeText(this, "먼저 방을 만들어 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val groupLine = group()?.let { it.shareLine + "\n" }.orEmpty()
            TextShare.sheet(this, "방 번호 보내기", "방 번호", "[오토클리커 Pro 집결 방]\n${groupLine}집결 방 번호: $code\n(앱 > 집결 방에서 소속을 먼저 정한 뒤 방 선택)")
        }
    }

    /**
     * 서버의 방 목록을 받아 고르게 한다. 지휘관에게는 "+ 새 방 만들기"가 함께 보인다.
     * 목록을 받지 못하면(인터넷·서버 규칙) 이 기기가 들어갔던 방들을 대신 보여 주고, 그때만 번호를 직접 넣을 수 있다.
     */
    private fun showRoomChooser() {
        // 방은 소속 아래에 있다. 소속을 아직 정하지 않았으면 먼저 정하게 한 뒤 다시 연다.
        if (group() == null) { showGroupSheet { showRoomChooser() }; return }
        val current = roomPrefs().getString("cloud_room_number", "") ?: ""
        val admin = PreferencesHelper.isAdminMode(this)
        Toast.makeText(this, "방 목록을 불러오는 중…", Toast.LENGTH_SHORT).show()
        Thread {
            val r = AdminServer(RallyRoomSync.DB_URL, roomAuth()).loadRoomsOnly(group())
            val overviews = if (r.error != null) null else RoomList.summarize(r.value, null)
            val server = overviews?.map { it.code to RoomList.lineBrief(it) }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (!server.isNullOrEmpty()) RoomListCache.save(roomPrefs(), server) // 집결 화면의 방 선택도 같은 목록으로 바로 뜬다
                val entries = RoomChooser.entries(server, RallyRoomHistory.load(roomPrefs()), current, admin)
                val badges = overviews.orEmpty().associate { it.code to RoomChooser.badge(it.assigned, it.teamCount) }
                RoomSheet(
                    this,
                    rows = entries.filter { it.kind == RoomChooser.Kind.ROOM }.map { RoomSheet.Row(it.code, badges[it.code], it.deletable) },
                    current = current,
                    note = RoomChooser.note(server, r.error),
                    showNew = entries.any { it.kind == RoomChooser.Kind.NEW },
                    showType = entries.any { it.kind == RoomChooser.Kind.TYPE },
                    onPick = { code ->
                        if (code == current) Toast.makeText(this, "지금 들어와 있는 방이에요.", Toast.LENGTH_SHORT).show()
                        else joinRoom(code)
                    },
                    onNew = { askNewRoom() },
                    onType = { askRoomNumber() },
                    onDelete = { code, done -> deleteRoom(code, done) }
                ).show()
            }
        }.start()
    }

    /** 방과 방 명단을 서버에서 지운다. 방 선택 창의 편집에서 확인을 받은 뒤에 부른다. 끝나면 성공 여부를 알린다. */
    private fun deleteRoom(code: String, done: (Boolean) -> Unit) {
        Thread {
            val err = adminServer().deleteRoom(roomKey(code))
            runOnUiThread {
                if (err != null) { Toast.makeText(this, "방을 지우지 못했어요: $err", Toast.LENGTH_LONG).show(); done(false); return@runOnUiThread }
                RallyRoomHistory.forget(roomPrefs(), code)
                RoomListCache.save(roomPrefs(), RoomListCache.load(roomPrefs()).filter { it.first != code })
                // 지운 방에 들어와 있었다면 방에서 나온다. 번호를 남겨 두면 집결 화면이 그 방을 다시 만들어 버린다.
                if (code == (roomPrefs().getString("cloud_room_number", "") ?: "")) {
                    roomPrefs().edit().remove("cloud_room_number").remove("cloud_room_creatable").apply()
                    AutoClickService.instance?.leaveRallyRoom()
                    rosterStatus = ""
                }
                Toast.makeText(this, "방 $code 을(를) 지웠어요.", Toast.LENGTH_SHORT).show()
                updateRallyInfoCard()
                done(true)
            }
        }.start()
    }

    /** 서버 방 목록을 받지 못했을 때만 쓰는 비상구: 받은 방 번호를 직접 넣어 들어간다. */
    private fun askRoomNumber() {
        InputSheet(
            this,
            title = "방 번호 입력",
            message = "집결장에게 받은 방 번호를 입력해 주세요.",
            fields = listOf(InputSheet.Field(hint = "방 번호 (4자리 이상)", maxLength = 8)),
            submitLabel = "입장"
        ) { v ->
            val code = v[0].trim()
            if (code.length < 4) { Toast.makeText(this, "방 번호를 4자리 이상 입력해 주세요.", Toast.LENGTH_SHORT).show(); false }
            else { joinRoom(code); true }
        }.show()
    }

    /** 집결장이 처음 입장할 때 한 번 묻는다. 지휘관이 군단을 배정할 때 명단에 이 이름으로 보인다. 나중에는 집결 화면의 "내 기기"에서 바꾼다. */
    private fun askCharName(onDone: (String) -> Unit) {
        InputSheet(
            this,
            title = "캐릭터명 입력",
            message = "지휘관이 군단을 배정할 때 이 이름으로 보여요.",
            fields = listOf(InputSheet.Field(hint = "캐릭터명", maxLength = 20, inputType = android.text.InputType.TYPE_CLASS_TEXT)),
            submitLabel = "확인"
        ) { v ->
            val name = RallyRoster.cleanName(v[0])
            if (name.isEmpty()) { Toast.makeText(this, "캐릭터명을 입력해 주세요.", Toast.LENGTH_SHORT).show(); false }
            else { PreferencesHelper.setRallyCharacterName(this, name); onDone(name); true }
        }.show()
    }

    /** 방에 입장한다. 지휘관은 없는 번호면 새로 만들고(방 수 상한 확인), 집결장은 서버에 있는 방에만 들어가 명단에 이름을 올린다. */
    private fun joinRoom(code: String) {
        val admin = PreferencesHelper.isAdminMode(this)
        val name = RallyRoster.cleanName(PreferencesHelper.getRallyCharacterName(this))
        if (!admin && name.isEmpty()) { askCharName { joinRoom(code) }; return }

        // 입장 동작. 지휘관은 없는 방이면 만들고(첫 입장), 집결장은 아래에서 방이 있다고 확인된 뒤에만 부른다.
        fun enter() {
            roomPrefs().edit().putString("cloud_room_number", code).putString("cloud_room_creatable", code).apply() // 첫 입장이므로 없는 방이면 만들어도 된다
            RallyRoomHistory.record(roomPrefs(), code)
            AutoClickService.instance?.leaveRallyRoom() // 방이 바뀌면 이전 방 연결은 끊는다
            Toast.makeText(this, "방 $code 에 입장했어요.", Toast.LENGTH_SHORT).show()
            if (admin) {
                rosterStatus = ""
            } else {
                // 곧바로 방 명단에 올려, 지휘관 목록에 바로 나타나게 한다.
                rosterStatus = "명단에 등록하는 중…"
                val memberId = PreferencesHelper.getRallyMemberId(this)
                Thread {
                    val err = RallyRoomSync.registerMember(RallyRoomSync.DB_URL, roomAuth(), roomKey(code), memberId, name)
                    runOnUiThread {
                        rosterStatus = if (err == null) "✓ 명단에 등록됐어요 ($name)" else "✗ 명단 등록 실패: $err · 방 바꾸기에서 다시 골라 주세요"
                        updateRallyInfoCard()
                    }
                }.start()
            }
            updateRallyInfoCard()
        }
        fun fail(status: String, toast: String) {
            rosterStatus = "✗ $status"
            Toast.makeText(this, toast, Toast.LENGTH_LONG).show()
            updateRallyInfoCard()
        }
        rosterStatus = "방을 확인하는 중…"
        updateRallyInfoCard()
        if (admin) {
            // 새 번호로 방을 만들 때는 서버의 방 수 상한(10개)을 본다. 이미 있는 방이면 그냥 들어간다.
            Thread {
                val codes = RallyRoomSync.roomCodes(RallyRoomSync.DB_URL, roomAuth(), group())
                runOnUiThread {
                    when (RoomLimit.decide(codes, code)) {
                        RoomLimit.Verdict.ALLOW -> enter()
                        RoomLimit.Verdict.FULL -> fail(RoomLimit.fullMessage(), RoomLimit.fullMessage())
                        RoomLimit.Verdict.UNKNOWN -> fail("서버에서 방을 확인하지 못했어요. 인터넷 연결을 확인해 주세요", "방을 확인하지 못했어요. 인터넷 연결을 확인해 주세요.")
                    }
                }
            }.start()
        } else {
            // 집결장은 방을 만들 수 없다. 없는 번호면 입장도 명단 등록도 하지 않고, 입장 목록에도 남기지 않는다.
            Thread {
                val exists = RallyRoomSync.roomExists(RallyRoomSync.DB_URL, roomAuth(), roomKey(code))
                runOnUiThread {
                    when (exists) {
                        true -> enter()
                        false -> fail("없는 방이에요. 소속(${group()?.label ?: "없음"})과 방 번호를 확인해 주세요", "없는 방이에요. 소속과 방 번호를 확인해 주세요.")
                        null -> fail("서버에서 방을 확인하지 못했어요. 인터넷 연결을 확인해 주세요", "방을 확인하지 못했어요. 인터넷 연결을 확인해 주세요.")
                    }
                }
            }.start()
        }
    }

    /**
     * 지휘관: 새 방을 만든다. 번호를 적으면 그 번호로, 비워 두면 자동 번호로 만든다.
     * 이미 방에 들어와 있으면 같은 창에서 알려 준다(지휘관만 옮겨 가고 집결장들은 이전 방에 남기 때문).
     */
    private fun askNewRoom() {
        val current = roomPrefs().getString("cloud_room_number", "") ?: ""
        val moving = if (current.isEmpty()) "" else "\n\n지금 방 $current 에서 나가게 돼요. 집결장들은 이전 방에 남으니 새 번호를 다시 공유해야 해요."
        InputSheet(
            this,
            title = "새 방 만들기",
            message = "원하는 방 번호를 4자리 이상 적어 주세요. 비워 두면 번호를 자동으로 정해요.$moving",
            fields = listOf(InputSheet.Field(hint = "방 번호 (비워 두면 자동)", maxLength = 8)),
            submitLabel = "만들기"
        ) { v -> createNewRoom(v[0]); true }.show()
    }

    private fun createNewRoom(typed: String) {
        Thread {
            val codes = RallyRoomSync.roomCodes(RallyRoomSync.DB_URL, roomAuth(), group())
            // 화면에 보여 줄 실패 이유. 방을 만들었으면 null
            var code = ""
            val problem: String? = when {
                codes == null -> "방을 확인하지 못했어요. 인터넷 연결을 확인해 주세요."
                else -> when (RoomChooser.newRoom(typed, codes)) {
                    RoomChooser.NewRoom.TOO_SHORT -> "방 번호를 4자리 이상 입력해 주세요."
                    RoomChooser.NewRoom.EXISTS -> "이미 있는 방이에요. 방 목록에서 골라 주세요."
                    else -> {
                        code = typed.trim().ifEmpty { generateSequence { (100000..999999).random().toString() }.first { it !in codes } }
                        if (RoomLimit.decide(codes, code) == RoomLimit.Verdict.FULL) RoomLimit.fullMessage()
                        // 서버에 바로 만들어, 집결 화면을 열기 전에도 방 목록에 보이게 한다
                        else RallyRoomSync.createRoom(RallyRoomSync.DB_URL, roomAuth(), roomKey(code))?.let { "방을 만들지 못했어요: $it" }
                    }
                }
            }
            runOnUiThread {
                if (problem != null) { Toast.makeText(this, problem, Toast.LENGTH_LONG).show(); return@runOnUiThread }
                roomPrefs().edit().putString("cloud_room_number", code).putString("cloud_room_creatable", code).apply()
                RallyRoomHistory.record(roomPrefs(), code)
                AutoClickService.instance?.leaveRallyRoom()
                rosterStatus = ""
                Toast.makeText(this, "새 방 $code 을 만들었어요. 집결장에게 번호를 공유하세요.", Toast.LENGTH_LONG).show()
                updateRallyInfoCard()
            }
        }.start()
    }

    private fun hasAccessibilityPermission(): Boolean {
        if (AutoClickService.isServiceRunning()) return true

        try {
            val enabledServices = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            val expectedService = "${packageName}/${AutoClickService::class.java.name}"
            val expectedShort = "${packageName}/.AutoClickService"
            val colonSplitter = enabledServices.split(":")
            for (service in colonSplitter) {
                if (service.equals(expectedService, ignoreCase = true) ||
                    service.equals(expectedShort, ignoreCase = true) ||
                    (service.contains(packageName) && service.contains("AutoClickService"))
                ) {
                    return true
                }
            }
        } catch (e: Exception) {
            // fallback
        }

        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledList = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (service in enabledList) {
            if (service.id.contains(packageName)) {
                return true
            }
        }
        return false
    }

    // --- 초대코드 및 지휘관 모드 관련 기능 ---

    private fun updateAuthUI() {
        val isVerified = PreferencesHelper.isVerified(this)
        val userId = PreferencesHelper.getVerifiedUserId(this)

        if (PreferencesHelper.isUserView(this)) {
            // 개발자가 일반 사용자 화면을 보는 중: 집결 기능은 가려지고 연타만 남는다
            binding.tvAuthStatusTitle.text = "👤 일반 화면 (개발자 미리보기)"
            binding.tvAuthStatusTitle.setTextColor(Color.parseColor("#F59E0B"))
            if (roomPrefs().getString(AdminRosterUi.KEY_REQUEST, null) != null) {
                // 미리보기에서 연맹 대표를 신청해 본 상태: 일반 사용자의 대기 화면과 같게 보인다
                binding.tvAuthStatusSubtitle.text = "연맹 대표 승인 대기 중 · " + RallyRoles.groupLabel(roomPrefs().getString(AdminRosterUi.KEY_REQUEST, null).orEmpty()) +
                    "\n위쪽 탭을 눌러 돌아가서 '지휘관 관리'에서 승인하거나 거절해 볼 수 있어요."
                binding.btnAuthAction.text = "승인됐는지 확인"
            } else {
                binding.tvAuthStatusSubtitle.text = "일반 사용자가 보는 화면이에요. 인증·지휘관 로그인·연맹 대표 신청을 시험해 볼 수 있어요. 위쪽 탭을 눌러 돌아가세요."
                binding.btnAuthAction.text = "인증하기"
            }
            binding.btnAuthAction.setBackgroundColor(Color.parseColor("#3B82F6"))
        } else if (PreferencesHelper.isAdminMode(this)) {
            binding.tvAuthStatusTitle.text = "👑 지휘관 모드"
            binding.tvAuthStatusTitle.setTextColor(Color.parseColor("#2563EB"))
            binding.tvAuthStatusSubtitle.text = "방을 만들고 집결장에게 방 번호와 집결장 코드를 공유하세요."
            binding.btnAuthAction.text = "관리"
            binding.btnAuthAction.setBackgroundColor(Color.parseColor("#2563EB"))
        } else if (PreferencesHelper.isRosterAdmin(this)) {
            // 지휘관으로 등록된 기기가 집결장 화면으로 지내는 중
            binding.tvAuthStatusTitle.text = "🚩 집결장 모드"
            binding.tvAuthStatusTitle.setTextColor(Color.parseColor("#10B981"))
            binding.tvAuthStatusSubtitle.text = "지휘관으로 등록된 기기예요. 위쪽 버튼으로 코드 없이 지휘관으로 전환할 수 있어요."
            binding.btnAuthAction.text = "지휘관으로 전환"
        } else if (roomPrefs().getString(AdminRosterUi.KEY_REQUEST, null) != null) {
            // 연맹 대표를 신청하고 개발자의 승인을 기다리는 중
            binding.tvAuthStatusTitle.text = "⏳ 연맹 대표 승인 대기 중"
            binding.tvAuthStatusTitle.setTextColor(Color.parseColor("#B45309"))
            binding.tvAuthStatusSubtitle.text = RallyRoles.groupLabel(roomPrefs().getString(AdminRosterUi.KEY_REQUEST, null).orEmpty()) +
                " · 개발자가 승인하면 지휘관 화면이 열려요. 아래 버튼으로 확인하거나 신청을 취소할 수 있어요."
            binding.btnAuthAction.text = "승인됐는지 확인"
            binding.btnAuthAction.setBackgroundColor(Color.parseColor("#475569"))
        } else if (isVerified) {
            binding.tvAuthStatusTitle.text = "✅ 집결장 인증 완료"
            binding.tvAuthStatusTitle.setTextColor(Color.parseColor("#10B981"))
            binding.tvAuthStatusSubtitle.text = if (userId.isNotEmpty()) "인증한 ID: $userId" else "집결장 인증을 마쳤어요."
            binding.btnAuthAction.text = "인증 변경"
            binding.btnAuthAction.setBackgroundColor(Color.parseColor("#475569"))
        } else {
            binding.tvAuthStatusTitle.text = "🔒 집결은 인증 후 사용"
            binding.tvAuthStatusTitle.setTextColor(Color.parseColor("#F59E0B"))
            binding.tvAuthStatusSubtitle.text = "연타는 바로 쓸 수 있어요. 집결은 집결장 코드로 인증하면 열려요."
            binding.btnAuthAction.text = "인증하기"
            binding.btnAuthAction.setBackgroundColor(Color.parseColor("#3B82F6"))
        }
        // 전환 버튼은 서버 명단에 있다고 확인된 기기에만 보이고, 그때는 열쇠(코드 입력) 아이콘이 필요 없다
        val userView = PreferencesHelper.isUserView(this)
        val roster = PreferencesHelper.isRosterAdmin(this) && !userView
        // 화면 전환 막대: 역할이 여럿인 기기에만 보이고, 지금 화면인 칸이 밝게 표시된다("일반 화면" 칸은 개발자에게만)
        val adminMode = PreferencesHelper.isAdminMode(this)
        val seg = RoleSwitch.model(roomPrefs().getBoolean("is_owner_cached", false), PreferencesHelper.isRosterAdmin(this), userView, adminMode)
        binding.roleSegments.visibility = if (seg.visible) View.VISIBLE else View.GONE
        binding.segUser.visibility = if (seg.showUser) View.VISIBLE else View.GONE
        listOf(binding.segUser to RoleSwitch.Seg.USER, binding.segLeader to RoleSwitch.Seg.LEADER, binding.segAdmin to RoleSwitch.Seg.ADMIN).forEach { (v, s) ->
            val on = s == seg.selected
            if (on) v.setBackgroundResource(R.drawable.bg_segment_on) else v.background = null
            v.setTextColor(ContextCompat.getColor(this, if (on) R.color.text_primary else R.color.text_secondary))
            v.setTypeface(null, if (on) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
        // 지휘관 화면과 (명단에 있는) 집결장 화면에서는 상태 카드를 두지 않는다: 전환은 위 막대, 관리는 집결 방 카드의 "관리"
        binding.cardAuthStatus.visibility = if (!userView && (adminMode || roster)) View.GONE else View.VISIBLE
        binding.btnAdminIcon.visibility = if (roster) View.GONE else View.VISIBLE // 미리보기에서는 일반 사용자처럼 보인다
        // 집결장 화면에서는 위쪽 전환 버튼 하나만 둔다: 카드 안에 같은 "지휘관으로 전환"을 또 두지 않는다
        // 일반 화면 미리보기에서는 일반 사용자와 같은 버튼(인증하기)이 보인다. 돌아가는 것은 위쪽 막대의 집결장·지휘관 칸이다.
        binding.btnAuthAction.visibility =
            if (!userView && roster && !PreferencesHelper.isAdminMode(this)) View.GONE else View.VISIBLE
        // 집결 방 카드는 인증한 회원·지휘관에게만 보인다
        binding.cardRallyRoom.visibility = if (PreferencesHelper.hasAccess(this)) View.VISIBLE else View.GONE
        binding.bottomBar.visibility = binding.cardRallyRoom.visibility // "집결 화면 열기"는 화면 맨 아래에 고정
        updateStartButtonVisibility()
        // 조작판이 떠 있는 채로 인증 상태가 바뀌어도 집결·헌터 아이콘이 바로 맞춰지게 한다
        AutoClickService.instance?.refreshMemberIcons()
    }

    /** 서버 지휘관 명단과 통신하는 객체. 앱과 서비스가 같은 익명 로그인(같은 기기 ID)을 쓴다. */
    private fun adminServer(): AdminServer {
        val auth = FirebaseAuthClient(BuildConfig.FIREBASE_API_KEY,
            load = { roomPrefs().getString("fb_refresh", null) },
            save = { t -> roomPrefs().edit().putString("fb_refresh", t).apply() })
        return AdminServer(RallyRoomSync.DB_URL, auth)
    }

    /**
     * 앱을 열 때 지휘관 모드를 서버 명단과 맞춘다. 서버가 개발자도 지휘관도 아니라고 분명히 답할 때만 푼다.
     * 네트워크 오류나 서버 규칙 미적용(알 수 없음)일 때는 그대로 둔다. 예전 비밀번호로 들어온 기기도 여기서 정리한다.
     */
    private fun verifyServerAdmin() {
        if (!PreferencesHelper.isAdminMode(this)) return
        val viaServer = PreferencesHelper.isAdminViaServer(this)
        Thread {
            val server = adminServer()
            val uid = server.uid().value ?: return@Thread
            val owner = server.isOwner(uid)
            rememberOwner(owner)
            val admin = if (owner == Check.YES) Check.YES else server.isAdmin(uid)
            // 서버 명단에 있는 지휘관이면 마지막 접속 시각과 앱 버전을 적는다(개발자 화면에 보인다)
            val inRoster = if (owner == Check.YES) server.isAdmin(uid) else admin
            if (inRoster == Check.YES) server.reportSelf(uid, System.currentTimeMillis(), BuildConfig.VERSION_NAME)
            // 소속이 서버에서 정해진 지휘관이면 그 소속으로 맞추고 잠근다(대표 여부도 여기서 안다)
            if (owner != Check.UNKNOWN && admin == Check.YES) syncServerGroup(server, uid, owner == Check.YES)
            // 서버가 분명히 답했을 때만 "등록된 기기" 표시를 고친다(전환 버튼이 이 표시를 본다)
            AdminRoster.rosterEntry(owner, admin).let { known ->
                if (known != Check.UNKNOWN && PreferencesHelper.isRosterAdmin(this) != (known == Check.YES)) {
                    PreferencesHelper.setRosterAdmin(this, known == Check.YES)
                    runOnUiThread { updateAuthUI() }
                }
            }
            when (AdminRoster.reconcile(viaServer, owner, admin)) {
                AdminModeFix.KEEP -> Unit
                AdminModeFix.MARK_SERVER -> PreferencesHelper.setAdminViaServer(this, true)
                AdminModeFix.CLEAR -> runOnUiThread {
                    roomPrefs().edit().putBoolean("group_locked", false).putBoolean("is_rep_cached", false).apply()
                    PreferencesHelper.setAdminMode(this, false)
                    PreferencesHelper.setAdminViaServer(this, false)
                    AutoClickService.instance?.leaveRallyRoom()
                    Toast.makeText(this, "지휘관 권한이 없어서 집결장 화면으로 돌아가요. 지휘관 코드를 받아 다시 로그인해 주세요.", Toast.LENGTH_LONG).show()
                    updateAuthUI()
                    updateRallyInfoCard()
                }
            }
        }.start()
    }

    /** 서버가 알려 준 개발자 여부를 기억해 둔다. 확인하지 못했을 때(네트워크 오류)는 이전 값을 그대로 둔다. */
    private fun rememberOwner(owner: Check) {
        if (owner == Check.UNKNOWN) return
        roomPrefs().edit().putBoolean("is_owner_cached", owner == Check.YES).apply()
    }

    private fun showAdminMenu() {
        // "지휘관 관리"와 "내 기기 ID 보기"는 개발자에게만 보인다(개발자 등록에 쓰는 것이라 지휘관과 집결장은 필요 없다)
        val owner = roomPrefs().getBoolean("is_owner_cached", false)
        val items = buildList {
            add(MenuSheet.Item("집결장 코드 발급") { showAdminPanelDialog() })
            if (owner) add(MenuSheet.Item("지휘관 관리", "개발자 전용") { AdminRosterUi(this@MainActivity, adminServer()).showManage() })
            if (owner) add(MenuSheet.Item("내 기기 ID 보기") { AdminRosterUi(this@MainActivity, adminServer()).showMyId() })
            // 연맹 대표는 자기 소속의 지휘관을 정하고 뺀다(개발자는 "지휘관 관리"에서 모든 소속을 다룬다)
            if (!owner && roomPrefs().getBoolean("is_rep_cached", false)) {
                add(MenuSheet.Item("우리 연맹 지휘관", "대표 전용") { AdminRosterUi(this@MainActivity, adminServer()).showMyAlliance() })
            }
            if (!PreferencesHelper.isRosterAdmin(this@MainActivity)) add(MenuSheet.Item("집결장으로 전환") { switchToLeader() }) // 위쪽 전환 버튼이 없을 때만
            // "일반 화면으로 전환"은 두지 않는다: 위쪽 막대의 "일반 화면" 칸이 같은 일을 한다.
        }
        MenuSheet(this, "지휘관", items).show()
    }

    private fun showVerificationDialog() {
        // 집결장 코드는 지휘관의 소속에 묶여 있다. 받은 초대 글을 통째로 복사해 두었으면 ID·코드·소속을 채워 둔다.
        val invite = InviteText.parse(clipboardText())
        val knownGroup = invite?.group ?: group()
        InputSheet(
            this,
            title = "집결장 코드 인증",
            message = if (invite != null) "복사해 둔 초대 글에서 채웠어요. 맞는지 보고 아래 버튼을 눌러 주세요."
                else "지휘관에게 받은 글을 통째로 복사한 뒤 이 창을 열면 저절로 채워져요. 직접 적을 때는 ID, 집결장 코드, 소속을 받은 그대로 적어 주세요.",
            fields = listOf(
                InputSheet.Field(
                    hint = "예: user@gmail.com", maxLength = 100, label = "이메일 또는 ID",
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                    initial = invite?.id ?: PreferencesHelper.getVerifiedUserId(this)
                ),
                InputSheet.Field(
                    hint = "예: AC-8F3K9A", maxLength = 20, label = "집결장 코드 (AC- 뒤 6자리)",
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS,
                    initial = invite?.code.orEmpty()
                ),
                InputSheet.Field(
                    hint = "예: 2000", maxLength = RallyGroup.MAX_SERVER, label = "서버 번호",
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER, initial = knownGroup?.server.orEmpty()
                ),
                // 대문자·소문자를 구분하므로 키보드가 글자를 바꾸지 않게 한다
                InputSheet.Field(
                    hint = "예: WBI", maxLength = RallyGroup.MAX_ALLIANCE, label = "연맹 (대문자·소문자 구분)",
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
                    initial = knownGroup?.alliance.orEmpty()
                )
            ),
            submitLabel = "인증 완료 및 시작하기",
            showCancel = false,
            link = InputSheet.Link("지휘관 로그인 (코드 발급 및 관리)") { showAdminLoginDialog() }
        ) { v ->
            val userId = v[0].trim()
            val code = v[1].trim()
            if (userId.isEmpty()) {
                Toast.makeText(this, "이메일 또는 ID를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                false
            } else if (code.isEmpty()) {
                Toast.makeText(this, "집결장 코드를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                false
            } else if (RallyGroup.problem(v[2], v[3]) != null) {
                Toast.makeText(this, RallyGroup.problem(v[2], v[3]), Toast.LENGTH_SHORT).show()
                false
            } else {
                val codeGroup = RallyGroup.of(v[2], v[3])
                if (codeGroup != null && InvitationManager.verifyInviteCode(this, userId, code, codeGroup)) {
                    PreferencesHelper.setVerified(this, true, userId)
                    // 코드가 그 소속에서만 맞으므로 소속도 함께 정하고 잠근다(지휘관의 소속을 물려받는다)
                    roomPrefs().edit().putBoolean("leader_group_locked", true).apply()
                    if (codeGroup != group() && !groupLocked()) {
                        applyGroup(codeGroup, "집결장 인증을 마쳤어요. 소속은 ${codeGroup.label}이에요. 방을 골라 주세요.")
                    } else {
                        Toast.makeText(this, "집결장 인증을 마쳤어요. 환영해요!", Toast.LENGTH_LONG).show()
                    }
                    updateAuthUI()
                    updateRallyInfoCard() // 방 카드의 소속 표시도 바로 맞춘다
                    true
                } else if (InvitationManager.isOldInviteCode(userId, code)) {
                    Toast.makeText(this, "예전 방식의 코드예요. 지휘관에게 새 집결장 코드를 받아 주세요. (지휘관도 앱을 최신으로 올려야 해요)", Toast.LENGTH_LONG).show()
                    false
                } else {
                    Toast.makeText(this, "코드가 맞지 않아요. ID, 코드, 소속(대문자·소문자)을 받은 그대로 적었는지 봐 주세요.", Toast.LENGTH_LONG).show()
                    false
                }
            }
        }.show()
    }

    private fun showAdminLoginDialog() {
        // "개발자"라는 말은 개발자 폰에서만 보인다. 다른 사람에게는 연맹 대표만 알면 된다.
        val owner = roomPrefs().getBoolean("is_owner_cached", false)
        val intro = if (owner) "개발자나 연맹 대표에게 받은 지휘관 코드를 입력해 주세요. 이미 지휘관이나 개발자로 등록된 기기는 칸을 비워 두고 확인을 누르면 됩니다."
            else "연맹 대표에게 받은 지휘관 코드를 입력해 주세요. 이미 지휘관으로 등록된 기기는 칸을 비워 두고 확인을 누르면 됩니다."
        InputSheet(
            this,
            title = "지휘관 로그인",
            message = "$intro 우리 연맹에 대표가 아직 없다면 아래에서 신청할 수 있어요.",
            fields = listOf(InputSheet.Field(
                hint = "지휘관 코드 (AD-XXXXXXXX)", maxLength = 40,
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            )),
            submitLabel = "확인",
            link = InputSheet.Link("내 기기 ID") { AdminRosterUi(this, adminServer()).showMyId() },
            // 대표가 없는 연맹 사람이 찾아야 하는 길이라 작은 글자 단추가 아니라 버튼으로 둔다
            extra = InputSheet.Link("우리 연맹 대표 신청하기") { openRepRequest() }
        ) { v ->
            val typed = v[0].trim()
            if (typed.isEmpty()) {
                enterFromRoster(askCodeIfNot = false)
            } else if (AdminRoster.looksLikeCode(typed)) {
                // 지휘관 코드: 서버 명단에 올라야 지휘관이 된다
                Toast.makeText(this, "지휘관 코드를 확인하는 중…", Toast.LENGTH_SHORT).show()
                Thread {
                    val server = adminServer()
                    val err = server.redeem(typed)
                    if (err == null) server.uid().value?.let { syncServerGroup(server, it, owner = false) }
                    runOnUiThread {
                        if (err == null) {
                            PreferencesHelper.setAdminMode(this, true)
                            PreferencesHelper.setAdminViaServer(this, true)
                            PreferencesHelper.setRosterAdmin(this, true)
                            AutoClickService.instance?.leaveRallyRoom() // 권한이 바뀌면 패널을 새로 만든다
                            updateAuthUI()
                            updateRallyInfoCard()
                            Toast.makeText(this, "지휘관으로 등록됐어요. 다음부터는 코드 없이 전환할 수 있어요.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "$err", Toast.LENGTH_LONG).show()
                        }
                    }
                }.start()
            } else {
                Toast.makeText(this, "지휘관 코드는 AD- 로 시작해요. 개발자에게 받은 코드를 확인해 주세요.", Toast.LENGTH_LONG).show()
            }
            true
        }.show()
    }

    /** 서버의 개발자 목록(owners)에 이 기기가 있으면 코드 없이 지휘관 모드로 들어간다. */
    /** 개발자만: 서버에 개발자인지 다시 확인한 뒤 일반 사용자 화면으로 바꾼다. 확인하지 못하면 바꾸지 않는다. */
    private fun enterUserView() {
        Toast.makeText(this, "개발자 여부를 확인하는 중…", Toast.LENGTH_SHORT).show()
        Thread {
            val server = adminServer()
            val uid = server.uid().value
            val owner = if (uid == null) Check.UNKNOWN else server.isOwner(uid)
            rememberOwner(owner)
            runOnUiThread {
                when (owner) {
                    Check.YES -> setUserView(true)
                    Check.NO -> {
                        Toast.makeText(this, "개발자로 등록된 기기만 쓸 수 있어요.", Toast.LENGTH_LONG).show()
                        updateAuthUI()
                    }
                    Check.UNKNOWN -> Toast.makeText(this, "서버에서 확인하지 못했어요. 네트워크를 확인하고 다시 눌러 주세요.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun setUserView(on: Boolean) {
        PreferencesHelper.setUserView(this, on)
        AutoClickService.instance?.leaveRallyRoom() // 보이는 권한이 바뀌면 집결 패널을 닫는다
        Toast.makeText(this, if (on) "일반 화면으로 바꿨어요." else "개발자 화면으로 돌아왔어요.", Toast.LENGTH_SHORT).show()
        updateAuthUI()
        updateServiceState() // 펼침 상태가 바뀌었으니 제목의 ▴/▾ 와 띄우기 버튼 글자를 다시 맞춘다
        updateRallyInfoCard()
    }

    /** 지휘관 화면을 끄고 집결장 화면으로 간다. 서버 명단에는 그대로 남아 있어, 나중에 코드 없이 다시 지휘관으로 전환할 수 있다. */
    private fun switchToLeader() {
        PreferencesHelper.setAdminMode(this, false)
        PreferencesHelper.setAdminViaServer(this, false)
        AutoClickService.instance?.leaveRallyRoom() // 권한이 바뀌면 패널을 새로 만든다
        Toast.makeText(this, "집결장으로 전환했어요.", Toast.LENGTH_SHORT).show()
        updateAuthUI()
        updateRallyInfoCard()
    }

    /**
     * 코드 없이 지휘관으로 들어간다. 서버가 이 기기를 개발자나 지휘관으로 알고 있을 때만 된다(누를 때마다 서버에 다시 묻는다).
     * 명단에 없으면 들여보내지 않고, [askCodeIfNot]이면 지휘관 코드 입력 창을 띄운다. 확인하지 못했을 때도 들여보내지 않는다.
     */
    private fun enterFromRoster(askCodeIfNot: Boolean) {
        Toast.makeText(this, "지휘관 명단을 확인하는 중…", Toast.LENGTH_SHORT).show()
        Thread {
            val server = adminServer()
            val uid = server.uid().value
            val owner = if (uid == null) Check.UNKNOWN else server.isOwner(uid)
            rememberOwner(owner)
            val admin = if (uid == null) Check.UNKNOWN else if (owner == Check.YES) Check.YES else server.isAdmin(uid)
            if (uid != null && owner != Check.UNKNOWN && admin == Check.YES) syncServerGroup(server, uid, owner == Check.YES)
            runOnUiThread {
                when (AdminRoster.rosterEntry(owner, admin)) {
                    Check.YES -> {
                        roomPrefs().edit().remove(AdminRosterUi.KEY_REQUEST).apply() // 대표 신청이 승인돼 들어온 경우 대기 표시를 지운다
                        if (PreferencesHelper.isUserView(this)) PreferencesHelper.setUserView(this, false) // 미리보기의 로그인 창으로 들어온 경우
                        PreferencesHelper.setAdminMode(this, true)
                        PreferencesHelper.setAdminViaServer(this, true)
                        PreferencesHelper.setRosterAdmin(this, true)
                        AutoClickService.instance?.leaveRallyRoom() // 권한이 바뀌면 패널을 새로 만든다
                        updateAuthUI()
                        updateRallyInfoCard()
                        Toast.makeText(this, if (owner == Check.YES) "개발자로 들어왔어요." else "지휘관으로 전환했어요.", Toast.LENGTH_SHORT).show()
                    }
                    Check.NO -> {
                        PreferencesHelper.setRosterAdmin(this, false)
                        updateAuthUI()
                        updateRallyInfoCard()
                        Toast.makeText(this, "지휘관 명단에 없는 기기예요. 지휘관 코드를 입력해 주세요.", Toast.LENGTH_LONG).show()
                        if (askCodeIfNot && !isFinishing && !isDestroyed) showAdminLoginDialog()
                    }
                    Check.UNKNOWN -> Toast.makeText(this, "서버에서 확인하지 못했어요. 인터넷 연결을 확인해 주세요.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun showAdminPanelDialog() {
        val sheet = SheetDialog(this, "집결장 코드 발급", "집결장의 이메일이나 ID를 입력하면 그 사람만 쓸 수 있는 코드를 만들어요.")
        val etTargetId = sheet.field(
            hint = "집결장 이메일 또는 ID (예: friend@gmail.com)", maxLength = 100,
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )

        val shareTitle = "집결장 코드"
        var shareMsg = ""
        // 코드를 만든 뒤에만 보이는 것들: 코드와 보낼 내용 미리보기, 보내기 버튼들
        val afterMade = mutableListOf<View>()

        // 파란 버튼은 "카카오톡으로 보내기" 하나만 되게, 만들기는 테두리 버튼으로 둔다
        val btnGenerate = sheet.wideButton("집결장 코드 만들기", SheetDialog.Kind.OUTLINE, top = 12) { }

        val resultCard = sheet.card(top = 14)
        val tvCode = sheet.line(resultCard, "", bold = true).apply {
            textSize = 22f; setTextColor(SheetDialog.BLUE); gravity = android.view.Gravity.CENTER
            setTextIsSelectable(true)
        }
        val tvPreview = sheet.line(resultCard, "", small = true, top = 8)
        afterMade += resultCard
        afterMade += sheet.wideButton("카카오톡으로 보내기", top = 14) { TextShare.toKakao(this, shareTitle, shareMsg) }
        afterMade += sheet.equalRow(
            sheet.pill("복사") { TextShare.copy(this, shareTitle, shareMsg) },
            sheet.pill("다른 앱으로 보내기") { TextShare.chooser(this, shareTitle, shareMsg) }
        )
        afterMade.forEach { it.visibility = View.GONE }

        btnGenerate.setOnClickListener {
            val memberId = etTargetId.text.toString().trim()
            if (memberId.isEmpty()) {
                Toast.makeText(this, "이메일 또는 ID를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val codeGroup = group()
            if (codeGroup == null) {
                Toast.makeText(this, "집결장 코드는 소속에 묶여요. 먼저 소속을 정해 주세요.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val code = InvitationManager.generateInviteCode(memberId, codeGroup)
            if (code.isEmpty()) {
                Toast.makeText(this, "이 앱에는 집결장 코드 설정이 없어 만들 수 없어요.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            // 방 번호를 따로 한 번 더 보내지 않아도 되게, 지금 방이 있으면 같은 메시지에 넣는다
            val room = roomPrefs().getString("cloud_room_number", "") ?: ""
            val roomLine = "\n" + codeGroup.shareLine + if (room.isEmpty()) "" else "\n집결 방 번호: $room (인증 후 방 선택에서 고르기)"
            shareMsg = "[오토클리커 Pro 집결장 초대]\nID: $memberId\n집결장 코드: $code\n이 글을 통째로 복사한 뒤 앱에서 \"인증하기\"를 누르면 저절로 채워져요. 이 코드는 아래 소속에서만 맞아요.$roomLine"
            tvCode.text = code
            tvPreview.text = shareMsg
            afterMade.forEach { it.visibility = View.VISIBLE }
            Toast.makeText(this, "집결장 코드를 만들었어요.", Toast.LENGTH_SHORT).show()
        }

        sheet.actions(SheetDialog.act("닫기")).show()
    }
}
