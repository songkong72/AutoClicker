package com.sejun.autoclicker

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
        android.app.AlertDialog.Builder(this)
            .setTitle("🚀 새로운 버전 업데이트")
            .setMessage("연맹 필수! 새로운 버전의 오토클리커가 준비되었습니다. 지금 다운로드하시겠습니까?")
            .setPositiveButton("다운로드") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                startActivity(intent)
            }
            .setNegativeButton("나중에", null)
            .setCancelable(false)
            .show()
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
        setupPresets()
        setupRepeatConditionListeners()

        // 미인증 사용자의 경우 실행 시 인증 다이얼로그 즉시 표시
        if (!PreferencesHelper.hasAccess(this)) {
            binding.root.post {
                showVerificationDialog()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAppVersion()
        verifyServerAdmin()
        updateAuthUI()
        loadSettings()
        updateRallyInfoCard()
        updatePermissionStates()
        updateServiceState()
        bindServiceCallbacks()
    }

    override fun onPause() {
        super.onPause()
        saveSettings()
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
            if (PreferencesHelper.isAdminMode(this)) showAdminMenu() else showVerificationDialog()
        }

        // 일반 연타 모드 접기/펼치기
        binding.tvGeneralModeToggle.setOnClickListener {
            val open = binding.layoutGeneralModes.visibility != View.VISIBLE
            binding.layoutGeneralModes.visibility = if (open) View.VISIBLE else View.GONE
            binding.tvGeneralModeToggle.text = if (open) "일반 연타 모드  ▴" else "일반 연타 모드  ▾"
        }

        // 상단 관리자 설정 아이콘
        binding.btnAdminIcon.setOnClickListener {
            showAdminLoginDialog()
        }

        // 상단 타이틀 5회 연속 탭 시 관리자 진입 (히든 제스처)
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
                showAdminLoginDialog()
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
            Toast.makeText(this, "🛑 치료 연타를 즉시 정지했습니다.", Toast.LENGTH_SHORT).show()
            updateServiceState()
        }

        // ⚔️ 집결 동시 착탄 및 그룹 작전 설정 버튼
        binding.btnOpenRallySettings.setOnClickListener {
            if (!PreferencesHelper.hasAccess(this)) {
                Toast.makeText(this, "🔒 정회원 초대코드 인증 후 이용 가능합니다.", Toast.LENGTH_SHORT).show()
                showVerificationDialog()
                return@setOnClickListener
            }

            if (!hasAccessibilityPermission()) {
                Toast.makeText(this, "스위치를 먼저 켜주셔야 게임을 자동으로 터치할 수 있습니다.", Toast.LENGTH_LONG).show()
                openAccessibilitySettings()
                return@setOnClickListener
            }

            val service = AutoClickService.instance
            if (service == null) {
                Toast.makeText(this, "서비스를 준비 중입니다. 잠시 후 다시 눌러주세요.\n(계속 안 되면 접근성을 껐다가 다시 켜주세요)", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if ((roomPrefs().getString("cloud_room_number", "") ?: "").isEmpty()) {
                Toast.makeText(this, "먼저 집결 방에 입장해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            bindServiceCallbacks()

            if (!service.isOverlaysShowing()) {
                saveSettings()
                service.showOverlays()
            }
            service.toggleRallyPanel()
            Toast.makeText(this, "⚔️ 집결 동시 착탄 설정을 띄웠습니다.", Toast.LENGTH_SHORT).show()
            moveTaskToBack(true)
        }

        // Single Smart Toggle Button: [🚀 오토클리커 띄우기] ↔ [✕ 오토클리커 숨기기]
        binding.btnStartService.setOnClickListener {
            if (!PreferencesHelper.hasAccess(this)) {
                Toast.makeText(this, "🔒 정회원 초대코드 인증 후 이용 가능합니다.", Toast.LENGTH_SHORT).show()
                showVerificationDialog()
                return@setOnClickListener
            }

            if (!hasAccessibilityPermission()) {
                Toast.makeText(this, "스위치를 먼저 켜주셔야 게임을 자동으로 터치할 수 있습니다.", Toast.LENGTH_LONG).show()
                openAccessibilitySettings()
                return@setOnClickListener
            }

            val service = AutoClickService.instance
            if (service == null) {
                Toast.makeText(this, "서비스를 준비 중입니다. 잠시 후 다시 눌러주세요.\n(계속 안 되면 접근성을 껐다가 다시 켜주세요)", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            bindServiceCallbacks()

            if (service.isOverlaysShowing()) {
                // 이미 화면에 떠 있으면 숨기기
                service.hideOverlays()
                Toast.makeText(this, "✕ 오토클리커를 숨겼습니다.", Toast.LENGTH_SHORT).show()
            } else {
                // 화면에 없으면 띄우기 (설정값 저장 후 띄움)
                saveSettings()
                service.showOverlays()
                Toast.makeText(this, "🚀 오토클리커를 띄웠습니다! 게임으로 이동합니다.", Toast.LENGTH_SHORT).show()
                moveTaskToBack(true) // Switch to game immediately
            }
            updateServiceState()
        }
    }

    private fun setupRepeatConditionListeners() {
        binding.rgRepeatMode.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbInfinite -> {
                    binding.layoutCountInput.visibility = View.GONE
                    binding.layoutTimerInput.visibility = View.GONE
                }
                R.id.rbCount -> {
                    binding.layoutCountInput.visibility = View.VISIBLE
                    binding.layoutTimerInput.visibility = View.GONE
                }
                R.id.rbTimer -> {
                    binding.layoutCountInput.visibility = View.GONE
                    binding.layoutTimerInput.visibility = View.VISIBLE
                }
            }
            saveSettings()
        }

        // 횟수 프리셋
        binding.chipCount50.setOnClickListener {
            binding.etRepeatCount.setText("50")
            saveSettings()
        }
        binding.chipCount100.setOnClickListener {
            binding.etRepeatCount.setText("100")
            saveSettings()
        }
        binding.chipCount300.setOnClickListener {
            binding.etRepeatCount.setText("300")
            saveSettings()
        }

        // 시간 프리셋
        binding.chipTimer1m.setOnClickListener {
            binding.etTimerMin.setText("1")
            binding.etTimerSec.setText("0")
            saveSettings()
        }
        binding.chipTimer3m.setOnClickListener {
            binding.etTimerMin.setText("3")
            binding.etTimerSec.setText("0")
            saveSettings()
        }
    }

    private fun loadSettings() {
        val interval = PreferencesHelper.getIntervalMs(this)
        binding.etInterval.setText(interval.toString())
        highlightPreset(interval.toInt())

        when (PreferencesHelper.getRepeatMode(this)) {
            RepeatMode.INFINITE -> {
                binding.rbInfinite.isChecked = true
                binding.layoutCountInput.visibility = View.GONE
                binding.layoutTimerInput.visibility = View.GONE
            }
            RepeatMode.COUNT -> {
                binding.rbCount.isChecked = true
                binding.layoutCountInput.visibility = View.VISIBLE
                binding.layoutTimerInput.visibility = View.GONE
            }
            RepeatMode.TIMER -> {
                binding.rbTimer.isChecked = true
                binding.layoutCountInput.visibility = View.GONE
                binding.layoutTimerInput.visibility = View.VISIBLE
            }
        }

        binding.etRepeatCount.setText(PreferencesHelper.getRepeatCount(this).toString())
        val totalSec = PreferencesHelper.getRepeatDurationSec(this)
        binding.etTimerMin.setText((totalSec / 60).toString())
        binding.etTimerSec.setText((totalSec % 60).toString())
    }

    private fun saveSettings() {
        val interval = binding.etInterval.text.toString().toLongOrNull() ?: 500L
        PreferencesHelper.setIntervalMs(this, interval)

        val mode = when (binding.rgRepeatMode.checkedRadioButtonId) {
            R.id.rbCount -> RepeatMode.COUNT
            R.id.rbTimer -> RepeatMode.TIMER
            else -> RepeatMode.INFINITE
        }
        PreferencesHelper.setRepeatMode(this, mode)

        val count = binding.etRepeatCount.text.toString().toIntOrNull() ?: 100
        PreferencesHelper.setRepeatCount(this, count)

        val min = binding.etTimerMin.text.toString().toIntOrNull() ?: 1
        val sec = binding.etTimerSec.text.toString().toIntOrNull() ?: 0
        PreferencesHelper.setRepeatDurationSec(this, (min * 60) + sec)
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

    private fun setupPresets() {
        val presetButtons = listOf(
            Pair(binding.chip100, 100),
            Pair(binding.chip200, 200),
            Pair(binding.chip500, 500),
            Pair(binding.chip1000, 1000)
        )

        presetButtons.forEach { (button, interval) ->
            button.setOnClickListener {
                binding.etInterval.setText(interval.toString())
                highlightPreset(interval)
            }
        }
    }

    private fun highlightPreset(selectedInterval: Int) {
        val map = mapOf(
            100 to binding.chip100,
            200 to binding.chip200,
            500 to binding.chip500,
            1000 to binding.chip1000
        )

        map.forEach { (interval, button) ->
            if (interval == selectedInterval) {
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
                button.setTextColor(ContextCompat.getColor(this, R.color.text_on_primary))
            } else {
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_variant))
                button.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }
        }
    }

    private fun updatePermissionStates() {
        val hasAccessibility = hasAccessibilityPermission()
        if (hasAccessibility) {
            binding.badgeAccessibility.text = getString(R.string.status_granted)
            binding.badgeAccessibility.setBackgroundResource(R.drawable.bg_badge_success)
            binding.badgeAccessibility.setTextColor(ContextCompat.getColor(this, R.color.success))
            binding.btnGrantAccessibility.isEnabled = false
            binding.btnGrantAccessibility.text = "완료"
        } else {
            binding.badgeAccessibility.text = getString(R.string.status_needed)
            binding.badgeAccessibility.setBackgroundResource(R.drawable.bg_badge_warning)
            binding.badgeAccessibility.setTextColor(ContextCompat.getColor(this, R.color.warning))
            binding.btnGrantAccessibility.isEnabled = true
            binding.btnGrantAccessibility.text = getString(R.string.btn_enable)
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
        updateRallyInfoCard()
    }

    private fun roomPrefs() = getSharedPreferences("AutoClickerPrefs", MODE_PRIVATE)

    /** 입장 직후 명단 등록 결과(성공/실패 이유). 토스트가 안 보이는 기기가 있어 방 상태 글에 붙여 보여 준다. */
    private var rosterStatus = ""

    private fun updateRallyInfoCard() {
        val room = roomPrefs().getString("cloud_room_number", "") ?: ""
        val admin = PreferencesHelper.isAdminMode(this)
        binding.layoutRoomAdmin.visibility = if (admin) View.VISIBLE else View.GONE
        binding.etCharName.visibility = if (admin) View.GONE else View.VISIBLE
        if (!admin && binding.etCharName.text.isNullOrEmpty()) binding.etCharName.setText(PreferencesHelper.getRallyCharacterName(this))
        // 패널에서 방을 바꿨을 수 있으니, 입력 중이 아니면 칸을 현재 방 번호에 맞춘다
        if (room.isNotEmpty() && !binding.etRoomCode.hasFocus() && binding.etRoomCode.text.toString() != room) binding.etRoomCode.setText(room)
        binding.tvRoomStatus.text = when {
            room.isEmpty() && admin -> "아직 방이 없어요. 새 방을 만들어 번호를 팀장에게 공유하세요."
            room.isEmpty() -> "관리자에게 받은 방 번호를 입력하고 입장하세요."
            admin -> "방 $room · 관리자"
            else -> "방 $room · 팀장" + if (rosterStatus.isNotEmpty()) "\n$rosterStatus" else ""
        }
    }

    private fun setupRoomCard() {
        // 입장 버튼을 누르지 않고 앱을 나가도 입력한 이름이 남도록, 입력하는 즉시 기기에 저장한다.
        binding.etCharName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (!PreferencesHelper.isAdminMode(this@MainActivity)) {
                    val name = RallyRoster.cleanName(s?.toString() ?: "")
                    if (name != PreferencesHelper.getRallyCharacterName(this@MainActivity)) {
                        PreferencesHelper.setRallyCharacterName(this@MainActivity, name)
                    }
                }
            }
        })
        binding.btnJoinRoom.setOnClickListener {
            val code = binding.etRoomCode.text.toString().trim()
            if (code.length < 4) {
                Toast.makeText(this, "방 번호를 4자리 이상 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val admin = PreferencesHelper.isAdminMode(this)
            val name = RallyRoster.cleanName(binding.etCharName.text.toString())
            if (!admin && name.isEmpty()) {
                Toast.makeText(this, "캐릭터명을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                rosterStatus = "캐릭터명을 입력해야 입장할 수 있어요"
                updateRallyInfoCard()
                return@setOnClickListener
            }
            roomPrefs().edit().putString("cloud_room_number", code).apply()
            AutoClickService.instance?.leaveRallyRoom() // 방이 바뀌면 이전 방 연결은 끊는다
            Toast.makeText(this, "방 $code 에 입장했어요.", Toast.LENGTH_SHORT).show()
            if (admin) {
                rosterStatus = ""
            } else {
                // 이름을 저장하고 곧바로 방 명단에 올려, 관리자 목록에 바로 나타나게 한다.
                PreferencesHelper.setRallyCharacterName(this, name)
                binding.etCharName.setText(name)
                rosterStatus = "명단에 등록하는 중…"
                val memberId = PreferencesHelper.getRallyMemberId(this)
                val auth = FirebaseAuthClient(BuildConfig.FIREBASE_API_KEY,
                    load = { roomPrefs().getString("fb_refresh", null) },
                    save = { t -> roomPrefs().edit().putString("fb_refresh", t).apply() })
                Thread {
                    val err = RallyRoomSync.registerMember(RallyRoomSync.DB_URL, auth, code, memberId, name)
                    runOnUiThread {
                        rosterStatus = if (err == null) "✓ 명단에 등록됐어요 ($name)" else "✗ 명단 등록 실패: $err"
                        updateRallyInfoCard()
                    }
                }.start()
            }
            updateRallyInfoCard()
        }
        binding.btnNewRoom.setOnClickListener {
            val code = (100000..999999).random().toString()
            roomPrefs().edit().putString("cloud_room_number", code).apply()
            AutoClickService.instance?.leaveRallyRoom()
            binding.etRoomCode.setText(code)
            Toast.makeText(this, "새 방 $code 을 만들었어요. 팀장에게 번호를 공유하세요.", Toast.LENGTH_LONG).show()
            updateRallyInfoCard()
        }
        binding.btnShareRoom.setOnClickListener {
            val code = roomPrefs().getString("cloud_room_number", "") ?: ""
            if (code.isEmpty()) {
                Toast.makeText(this, "먼저 방을 만들어 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "집결 방 번호: $code (오토클릭커 앱 > 집결 방에 입력)")
            }
            startActivity(Intent.createChooser(send, "방 번호 공유"))
        }
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

    // --- 초대코드 및 관리자 모드 관련 기능 ---

    private fun updateAuthUI() {
        val isVerified = PreferencesHelper.isVerified(this)
        val userId = PreferencesHelper.getVerifiedUserId(this)

        if (PreferencesHelper.isAdminMode(this)) {
            binding.tvAuthStatusTitle.text = "👑 관리자 모드"
            binding.tvAuthStatusTitle.setTextColor(Color.parseColor("#2563EB"))
            binding.tvAuthStatusSubtitle.text = "방을 만들고 팀장에게 방 번호와 초대코드를 공유하세요."
            binding.btnAuthAction.text = "관리"
            binding.btnAuthAction.setBackgroundColor(Color.parseColor("#2563EB"))
        } else if (isVerified) {
            binding.tvAuthStatusTitle.text = "✅ 정회원 인증 완료"
            binding.tvAuthStatusTitle.setTextColor(Color.parseColor("#10B981"))
            binding.tvAuthStatusSubtitle.text = if (userId.isNotEmpty()) "인증된 회원 ID: $userId" else "정회원 인증이 완료되었습니다."
            binding.btnAuthAction.text = "인증 변경"
            binding.btnAuthAction.setBackgroundColor(Color.parseColor("#475569"))
        } else {
            binding.tvAuthStatusTitle.text = "🔒 회원 전용 인증 필요"
            binding.tvAuthStatusTitle.setTextColor(Color.parseColor("#F59E0B"))
            binding.tvAuthStatusSubtitle.text = "초대코드를 입력하여 정회원 인증을 완료해 주세요."
            binding.btnAuthAction.text = "인증하기"
            binding.btnAuthAction.setBackgroundColor(Color.parseColor("#3B82F6"))
        }
    }

    /** 서버 관리자 명단과 통신하는 객체. 앱과 서비스가 같은 익명 로그인(같은 기기 ID)을 쓴다. */
    private fun adminServer(): AdminServer {
        val auth = FirebaseAuthClient(BuildConfig.FIREBASE_API_KEY,
            load = { roomPrefs().getString("fb_refresh", null) },
            save = { t -> roomPrefs().edit().putString("fb_refresh", t).apply() })
        return AdminServer(RallyRoomSync.DB_URL, auth)
    }

    /**
     * 앱을 열 때 관리자 모드를 서버 명단과 맞춘다. 서버가 개발자도 관리자도 아니라고 분명히 답할 때만 푼다.
     * 네트워크 오류나 서버 규칙 미적용(알 수 없음)일 때는 그대로 둔다. 예전 비밀번호로 들어온 기기도 여기서 정리한다.
     */
    private fun verifyServerAdmin() {
        if (!PreferencesHelper.isAdminMode(this)) return
        val viaServer = PreferencesHelper.isAdminViaServer(this)
        Thread {
            val server = adminServer()
            val uid = server.uid().value ?: return@Thread
            val owner = server.isOwner(uid)
            val admin = if (owner == Check.YES) Check.YES else server.isAdmin(uid)
            when (AdminRoster.reconcile(viaServer, owner, admin)) {
                AdminModeFix.KEEP -> Unit
                AdminModeFix.MARK_SERVER -> PreferencesHelper.setAdminViaServer(this, true)
                AdminModeFix.CLEAR -> runOnUiThread {
                    PreferencesHelper.setAdminMode(this, false)
                    PreferencesHelper.setAdminViaServer(this, false)
                    AutoClickService.instance?.leaveRallyRoom()
                    Toast.makeText(this, "관리자 권한이 없어서 팀장 화면으로 돌아갑니다. 관리자 코드를 받아 다시 로그인해 주세요.", Toast.LENGTH_LONG).show()
                    updateAuthUI()
                    updateRallyInfoCard()
                }
            }
        }.start()
    }

    private fun showAdminMenu() {
        val items = arrayOf("초대코드 발급 · 관리자 패널", "관리자 관리 (개발자 전용)", "내 기기 ID 보기", "관리자 모드 해제 (팀장 화면으로)")
        AlertDialog.Builder(this)
            .setTitle("관리자")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showAdminPanelDialog()
                    1 -> AdminRosterUi(this, adminServer()).showManage()
                    2 -> AdminRosterUi(this, adminServer()).showMyId()
                    else -> {
                        PreferencesHelper.setAdminMode(this, false)
                        PreferencesHelper.setAdminViaServer(this, false)
                        AutoClickService.instance?.leaveRallyRoom() // 권한이 바뀌면 패널을 새로 만든다
                        Toast.makeText(this, "관리자 모드를 해제했어요.", Toast.LENGTH_SHORT).show()
                        updateAuthUI()
                        updateRallyInfoCard()
                    }
                }
            }
            .setNegativeButton("닫기", null)
            .show()
    }

    private fun showVerificationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.layout_dialog_user_verification, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etUserId = dialogView.findViewById<EditText>(R.id.etVerifyUserId)
        val etCode = dialogView.findViewById<EditText>(R.id.etVerifyCode)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmitVerification)
        val btnAdmin = dialogView.findViewById<TextView>(R.id.btnOpenAdminLogin)

        val currentId = PreferencesHelper.getVerifiedUserId(this)
        if (currentId.isNotEmpty()) {
            etUserId.setText(currentId)
        }

        btnSubmit.setOnClickListener {
            val userId = etUserId.text.toString().trim()
            val code = etCode.text.toString().trim()

            if (userId.isEmpty()) {
                Toast.makeText(this, "회원 이메일 또는 식별 ID를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (code.isEmpty()) {
                Toast.makeText(this, "초대코드를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (InvitationManager.verifyInviteCode(this, userId, code)) {
                PreferencesHelper.setVerified(this, true, userId)
                Toast.makeText(this, "🎉 정회원 인증에 성공했습니다! 환영합니다.", Toast.LENGTH_LONG).show()
                dialog.dismiss()
                updateAuthUI()
            } else {
                Toast.makeText(this, "❌ 유효하지 않은 초대코드이거나 일치하지 않는 ID입니다.", Toast.LENGTH_LONG).show()
            }
        }

        btnAdmin.setOnClickListener {
            dialog.dismiss()
            showAdminLoginDialog()
        }

        dialog.show()
    }

    private fun showAdminLoginDialog() {
        val input = EditText(this).apply {
            hint = "관리자 코드 (AD-XXXXXXXX)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            setPadding(50, 40, 50, 40)
        }

        AlertDialog.Builder(this)
            .setTitle("👑 관리자 로그인")
            .setMessage("개발자에게 받은 관리자 코드를 입력해 주세요. 개발자로 등록된 기기는 칸을 비워 두고 확인을 누르면 됩니다.")
            .setView(input)
            .setPositiveButton("확인") { d, _ ->
                val typed = input.text.toString().trim()
                if (typed.isEmpty()) {
                    enterAsOwner()
                } else if (AdminRoster.looksLikeCode(typed)) {
                    // 관리자 코드: 서버 명단에 올라야 관리자가 된다
                    Toast.makeText(this, "관리자 코드를 확인하는 중…", Toast.LENGTH_SHORT).show()
                    Thread {
                        val err = adminServer().redeem(typed)
                        runOnUiThread {
                            if (err == null) {
                                PreferencesHelper.setAdminMode(this, true)
                                PreferencesHelper.setAdminViaServer(this, true)
                                updateAuthUI()
                                updateRallyInfoCard()
                                Toast.makeText(this, "👑 관리자로 등록됐어요.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this, "❌ $err", Toast.LENGTH_LONG).show()
                            }
                        }
                    }.start()
                } else {
                    Toast.makeText(this, "❌ 관리자 코드는 AD- 로 시작해요. 개발자에게 받은 코드를 확인해 주세요.", Toast.LENGTH_LONG).show()
                }
                d.dismiss()
            }
            .setNeutralButton("내 기기 ID") { _, _ -> AdminRosterUi(this, adminServer()).showMyId() }
            .setNegativeButton("취소", null)
            .show()
    }

    /** 서버의 개발자 목록(owners)에 이 기기가 있으면 코드 없이 관리자 모드로 들어간다. */
    private fun enterAsOwner() {
        Toast.makeText(this, "개발자 기기인지 확인하는 중…", Toast.LENGTH_SHORT).show()
        Thread {
            val server = adminServer()
            val uid = server.uid().value
            val owner = if (uid == null) Check.UNKNOWN else server.isOwner(uid)
            runOnUiThread {
                when (owner) {
                    Check.YES -> {
                        PreferencesHelper.setAdminMode(this, true)
                        PreferencesHelper.setAdminViaServer(this, true)
                        updateAuthUI()
                        updateRallyInfoCard()
                        Toast.makeText(this, "👑 개발자로 들어왔어요.", Toast.LENGTH_SHORT).show()
                    }
                    Check.NO -> Toast.makeText(this, "개발자로 등록된 기기가 아니에요. 관리자 코드를 입력해 주세요.", Toast.LENGTH_LONG).show()
                    Check.UNKNOWN -> Toast.makeText(this, "서버에서 확인하지 못했어요. 인터넷 연결을 확인해 주세요.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun showAdminPanelDialog() {
        val dialogView = layoutInflater.inflate(R.layout.layout_dialog_admin_panel, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnClose = dialogView.findViewById<TextView>(R.id.btnAdminClose)
        val etTargetId = dialogView.findViewById<EditText>(R.id.etTargetMemberId)
        val btnGenerate = dialogView.findViewById<Button>(R.id.btnGenerateCode)
        val layoutResult = dialogView.findViewById<View>(R.id.layoutGeneratedResult)
        val tvCode = dialogView.findViewById<TextView>(R.id.tvGeneratedCode)
        val btnCopy = dialogView.findViewById<Button>(R.id.btnCopyShareMessage)

        btnClose.setOnClickListener { dialog.dismiss() }

        var currentGeneratedCode = ""
        var currentMemberId = ""

        btnGenerate.setOnClickListener {
            val memberId = etTargetId.text.toString().trim()
            if (memberId.isEmpty()) {
                Toast.makeText(this, "회원 이메일 또는 ID를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val code = InvitationManager.generateInviteCode(memberId)
            if (code.isEmpty()) {
                Toast.makeText(this, "이 빌드에는 초대코드 비밀 설정이 없어 발급할 수 없어요.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            currentGeneratedCode = code
            currentMemberId = memberId
            tvCode.text = code
            layoutResult.visibility = View.VISIBLE
            Toast.makeText(this, "초대코드가 발급되었습니다.", Toast.LENGTH_SHORT).show()
        }

        btnCopy.setOnClickListener {
            if (currentGeneratedCode.isEmpty()) return@setOnClickListener
            val shareMsg = "[AutoClicker Pro 정회원 초대]\n회원 ID: $currentMemberId\n초대코드: $currentGeneratedCode\n앱 실행 후 인증창에 입력하시면 정회원으로 등록됩니다."
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("AutoClickerInvite", shareMsg)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "📋 카카오톡 전달 메시지가 복사되었습니다!", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }
}




