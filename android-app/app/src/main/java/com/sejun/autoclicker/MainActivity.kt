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
            when {
                PreferencesHelper.isUserView(this) -> setUserView(false)
                PreferencesHelper.isAdminMode(this) -> showAdminMenu()
                PreferencesHelper.isRosterAdmin(this) -> enterFromRoster(askCodeIfNot = true)
                else -> showVerificationDialog()
            }
        }

        // 일반 연타 모드 접기/펼치기
        binding.tvGeneralModeToggle.setOnClickListener {
            val open = binding.layoutGeneralModes.visibility != View.VISIBLE
            binding.layoutGeneralModes.visibility = if (open) View.VISIBLE else View.GONE
            updateServiceState()
        }

        // 상단 관리자 설정 아이콘
        binding.btnAdminIcon.setOnClickListener {
            if (PreferencesHelper.isUserView(this)) return@setOnClickListener
            // 이미 관리자면 로그인 창을 다시 띄우지 않고 관리 메뉴를 연다
            if (PreferencesHelper.isAdminMode(this)) showAdminMenu() else showAdminLoginDialog()
        }

        // 관리자로 등록된 기기: 한 번 눌러 관리자 ↔ 집결장 화면을 오간다(코드를 다시 받지 않는다)
        binding.btnUserView.setOnClickListener { enterUserView() }
        binding.btnRoleSwitch.setOnClickListener {
            if (PreferencesHelper.isAdminMode(this)) switchToLeader() else enterFromRoster(askCodeIfNot = true)
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
            Toast.makeText(this, "⚔️ 집결 화면을 띄웠습니다.", Toast.LENGTH_SHORT).show()
            moveTaskToBack(true)
        }

        // Single Smart Toggle Button: [🚀 오토클리커 띄우기] ↔ [✕ 오토클리커 숨기기]
        binding.btnStartService.setOnClickListener {
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
            // 허용된 뒤에는 "허용됨" 배지만 남긴다(누를 수 없는 "완료" 버튼이 같은 뜻으로 한 번 더 나오던 것)
            binding.btnGrantAccessibility.visibility = View.GONE
        } else {
            binding.badgeAccessibility.text = getString(R.string.status_needed)
            binding.badgeAccessibility.setBackgroundResource(R.drawable.bg_badge_warning)
            binding.badgeAccessibility.setTextColor(ContextCompat.getColor(this, R.color.warning))
            binding.btnGrantAccessibility.visibility = View.VISIBLE
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
        val open = binding.layoutGeneralModes.visibility == View.VISIBLE
        binding.tvGeneralModeToggle.text = "일반 연타 모드" + (if (isShowing) " · 떠 있음" else "") + (if (open) "  ▴" else "  ▾")
        updateRallyInfoCard()
    }

    /**
     * 띄우기/숨기기 버튼은 집결을 못 쓰는 사람(일반 사용자·개발자 미리보기)에게만 보인다(연타 영역을 접어도 늘 보인다).
     * 회원·관리자는 "집결 화면 열기"가 조작판까지 띄워 주므로 이 버튼을 두지 않는다(큰 버튼은 늘 하나). 조작판은 조작판의 ✕ 로 끈다.
     */
    private fun updateStartButtonVisibility() {
        val open = binding.layoutGeneralModes.visibility == View.VISIBLE
        binding.btnStartService.visibility = if (!PreferencesHelper.hasAccess(this)) View.VISIBLE else View.GONE
        binding.tvVolumeTip.visibility = if (open) View.VISIBLE else View.GONE
    }

    private fun roomAuth() = FirebaseAuthClient(BuildConfig.FIREBASE_API_KEY,
        load = { roomPrefs().getString("fb_refresh", null) },
        save = { t -> roomPrefs().edit().putString("fb_refresh", t).apply() })

    private fun roomPrefs() = getSharedPreferences("AutoClickerPrefs", MODE_PRIVATE)

    /** 입장 직후 명단 등록 결과(성공/실패 이유). 토스트가 안 보이는 기기가 있어 방 상태 글에 붙여 보여 준다. */
    private var rosterStatus = ""

    private fun updateRallyInfoCard() {
        val room = roomPrefs().getString("cloud_room_number", "") ?: ""
        val admin = PreferencesHelper.isAdminMode(this)
        // 방 번호를 치지 않고 서버의 방 목록에서 고른다. 방이 없으면 큰 "방 선택", 있으면 상태 줄 옆의 "방 바꾸기".
        binding.btnPickRoom.visibility = if (room.isEmpty()) View.VISIBLE else View.GONE
        binding.btnChangeRoom.visibility = if (room.isNotEmpty()) View.VISIBLE else View.GONE
        binding.btnShareRoom.visibility = if (admin && room.isNotEmpty()) View.VISIBLE else View.GONE
        // 방이 이미 있는 관리자에게는 "방을 만들고 공유하세요" 안내를 되풀이하지 않는다
        binding.tvAuthStatusSubtitle.visibility = if (admin && room.isNotEmpty()) View.GONE else View.VISIBLE
        val note = if (rosterStatus.isNotEmpty()) "\n$rosterStatus" else ""
        binding.tvRoomStatus.text = when {
            room.isEmpty() && admin -> "방을 고르거나 만들어 주세요.$note"
            room.isEmpty() -> "방을 선택해 주세요.$note"
            admin -> "방 $room · 관리자$note"
            else -> "방 $room · 집결장$note"
        }
    }

    private fun setupRoomCard() {
        binding.btnPickRoom.setOnClickListener { showRoomChooser() }
        binding.btnChangeRoom.setOnClickListener { showRoomChooser() }
        binding.btnShareRoom.setOnClickListener {
            val code = roomPrefs().getString("cloud_room_number", "") ?: ""
            if (code.isEmpty()) {
                Toast.makeText(this, "먼저 방을 만들어 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "집결 방 번호: $code (오토클릭커 앱 > 집결 방 > 방 선택)")
            }
            startActivity(Intent.createChooser(send, "방 번호 공유"))
        }
    }

    /**
     * 서버의 방 목록을 받아 고르게 한다. 관리자에게는 "+ 새 방 만들기"가 함께 보인다.
     * 목록을 받지 못하면(인터넷·서버 규칙) 이 기기가 들어갔던 방들을 대신 보여 주고, 그때만 번호를 직접 넣을 수 있다.
     */
    private fun showRoomChooser() {
        val current = roomPrefs().getString("cloud_room_number", "") ?: ""
        val admin = PreferencesHelper.isAdminMode(this)
        Toast.makeText(this, "방 목록을 불러오는 중…", Toast.LENGTH_SHORT).show()
        Thread {
            val r = AdminServer(RallyRoomSync.DB_URL, roomAuth()).loadRoomsOnly()
            val server = if (r.error != null) null else RoomList.summarize(r.value, null).map { it.code to RoomList.lineBrief(it) }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (!server.isNullOrEmpty()) RoomListCache.save(roomPrefs(), server) // 집결 화면의 방 선택도 같은 목록으로 바로 뜬다
                val entries = RoomChooser.entries(server, RallyRoomHistory.load(roomPrefs()), current, admin)
                showRoomChooserDialog(RoomChooser.title(server, r.error), entries, current)
            }
        }.start()
    }

    /**
     * 방 선택 창. 방 줄을 누르면 그 방에 들어가고, 관리자에게는 줄 오른쪽에 휴지통이 보여 그 방을 바로 지울 수 있다
     * (지울 방을 고르는 창을 따로 띄우지 않는다). 휴지통은 한 번 더 확인한 뒤에 지운다.
     */
    private fun showRoomChooserDialog(title: String, entries: List<RoomChooser.Entry>, current: String) {
        val builder = AlertDialog.Builder(this).setTitle(title).setNegativeButton("닫기", null)
        val ctx = builder.context
        val dp = resources.displayMetrics.density
        fun px(v: Int) = (v * dp).toInt()
        val ripple = android.util.TypedValue().also { ctx.theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true) }.resourceId
        val list = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(0, px(8), 0, 0)
        }
        // 창 테마(밝은/어두운)의 기본 글자색. 못 얻으면 TextView 기본색을 그대로 쓴다
        val primaryText = ctx.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary)).let { a -> a.getColorStateList(0).also { a.recycle() } }
        var dialog: AlertDialog? = null
        if (entries.isEmpty()) {
            list.addView(TextView(ctx).apply {
                text = "들어갈 수 있는 방이 없어요. 관리자가 방을 만들면 여기에 보여요."
                textSize = 15f
                setPadding(px(24), px(12), px(24), px(12))
            })
        }
        for (e in entries) {
            val row = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            row.addView(TextView(ctx).apply {
                text = e.label
                textSize = 16f
                primaryText?.let { setTextColor(it) }
                setPadding(px(24), px(14), px(8), px(14))
                setBackgroundResource(ripple)
                setOnClickListener {
                    dialog?.dismiss()
                    when (e.kind) {
                        RoomChooser.Kind.ROOM ->
                            if (e.code == current) Toast.makeText(this@MainActivity, "지금 들어와 있는 방이에요.", Toast.LENGTH_SHORT).show()
                            else joinRoom(e.code)
                        RoomChooser.Kind.NEW -> askNewRoom()
                        RoomChooser.Kind.TYPE -> askRoomNumber()
                    }
                }
            }, android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            if (e.deletable) {
                row.addView(TextView(ctx).apply {
                    text = "🗑"
                    textSize = 18f
                    contentDescription = "방 ${e.code} 삭제"
                    gravity = android.view.Gravity.CENTER
                    setBackgroundResource(ripple)
                    setOnClickListener { dialog?.dismiss(); confirmDeleteRoom(e.code, current) }
                }, android.widget.LinearLayout.LayoutParams(px(56), px(48)))
            }
            list.addView(row)
        }
        dialog = builder.setView(android.widget.ScrollView(ctx).apply { addView(list) }).show()
    }

    private fun confirmDeleteRoom(code: String, current: String) {
        val mine = if (code == current) "\n\n지금 들어와 있는 방이에요. 지우면 이 기기도 방에서 나옵니다." else ""
        AlertDialog.Builder(this)
            .setTitle("방 $code 삭제")
            .setMessage("이 방과 방 명단을 서버에서 지워요. 되돌릴 수 없고, 그 방에 들어가 있던 사람들은 다시 입장해야 합니다. 정말 지울까요?$mine")
            .setPositiveButton("삭제") { _, _ ->
                Thread {
                    val err = adminServer().deleteRoom(code)
                    runOnUiThread {
                        if (err != null) { Toast.makeText(this, "방을 지우지 못했어요: $err", Toast.LENGTH_LONG).show(); return@runOnUiThread }
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
                    }
                }.start()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    /** 서버 방 목록을 받지 못했을 때만 쓰는 비상구: 받은 방 번호를 직접 넣어 들어간다. */
    private fun askRoomNumber() {
        val input = EditText(this).apply {
            hint = "방 번호"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(android.text.InputFilter.LengthFilter(8))
            setPadding(50, 40, 50, 40)
        }
        AlertDialog.Builder(this)
            .setTitle("방 번호 입력")
            .setView(input)
            .setPositiveButton("입장") { _, _ ->
                val code = input.text.toString().trim()
                if (code.length < 4) Toast.makeText(this, "방 번호를 4자리 이상 입력해 주세요.", Toast.LENGTH_SHORT).show()
                else joinRoom(code)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    /** 집결장이 처음 입장할 때 한 번 묻는다. 관리자가 군단을 배정할 때 명단에 이 이름으로 보인다. 나중에는 집결 화면의 "내 기기"에서 바꾼다. */
    private fun askCharName(onDone: (String) -> Unit) {
        val input = EditText(this).apply {
            hint = "캐릭터명"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            filters = arrayOf(android.text.InputFilter.LengthFilter(20))
            setSingleLine(true)
            setPadding(50, 40, 50, 40)
        }
        AlertDialog.Builder(this)
            .setTitle("캐릭터명 입력")
            .setMessage("관리자가 군단을 배정할 때 이 이름으로 보여요.")
            .setView(input)
            .setPositiveButton("확인") { _, _ ->
                val name = RallyRoster.cleanName(input.text.toString())
                if (name.isEmpty()) Toast.makeText(this, "캐릭터명을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                else { PreferencesHelper.setRallyCharacterName(this, name); onDone(name) }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    /** 방에 입장한다. 관리자는 없는 번호면 새로 만들고(방 수 상한 확인), 집결장은 서버에 있는 방에만 들어가 명단에 이름을 올린다. */
    private fun joinRoom(code: String) {
        val admin = PreferencesHelper.isAdminMode(this)
        val name = RallyRoster.cleanName(PreferencesHelper.getRallyCharacterName(this))
        if (!admin && name.isEmpty()) { askCharName { joinRoom(code) }; return }

        // 입장 동작. 관리자는 없는 방이면 만들고(첫 입장), 집결장은 아래에서 방이 있다고 확인된 뒤에만 부른다.
        fun enter() {
            roomPrefs().edit().putString("cloud_room_number", code).putString("cloud_room_creatable", code).apply() // 첫 입장이므로 없는 방이면 만들어도 된다
            RallyRoomHistory.record(roomPrefs(), code)
            AutoClickService.instance?.leaveRallyRoom() // 방이 바뀌면 이전 방 연결은 끊는다
            Toast.makeText(this, "방 $code 에 입장했어요.", Toast.LENGTH_SHORT).show()
            if (admin) {
                rosterStatus = ""
            } else {
                // 곧바로 방 명단에 올려, 관리자 목록에 바로 나타나게 한다.
                rosterStatus = "명단에 등록하는 중…"
                val memberId = PreferencesHelper.getRallyMemberId(this)
                Thread {
                    val err = RallyRoomSync.registerMember(RallyRoomSync.DB_URL, roomAuth(), code, memberId, name)
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
                val codes = RallyRoomSync.roomCodes(RallyRoomSync.DB_URL, roomAuth())
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
                val exists = RallyRoomSync.roomExists(RallyRoomSync.DB_URL, roomAuth(), code)
                runOnUiThread {
                    when (exists) {
                        true -> enter()
                        false -> fail("없는 방이에요. 관리자에게 받은 방 번호를 확인해 주세요", "없는 방이에요. 방 번호를 확인해 주세요.")
                        null -> fail("서버에서 방을 확인하지 못했어요. 인터넷 연결을 확인해 주세요", "방을 확인하지 못했어요. 인터넷 연결을 확인해 주세요.")
                    }
                }
            }.start()
        }
    }

    /**
     * 관리자: 새 방을 만든다. 번호를 적으면 그 번호로, 비워 두면 자동 번호로 만든다.
     * 이미 방에 들어와 있으면 같은 창에서 알려 준다(관리자만 옮겨 가고 집결장들은 이전 방에 남기 때문).
     */
    private fun askNewRoom() {
        val current = roomPrefs().getString("cloud_room_number", "") ?: ""
        val input = EditText(this).apply {
            hint = "방 번호 (비워 두면 자동)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(android.text.InputFilter.LengthFilter(8))
            setPadding(50, 40, 50, 40)
        }
        val moving = if (current.isEmpty()) "" else "\n\n지금 방 $current 에서 나가게 돼요. 집결장들은 이전 방에 남으니 새 번호를 다시 공유해야 해요."
        AlertDialog.Builder(this)
            .setTitle("새 방 만들기")
            .setMessage("원하는 방 번호를 4자리 이상 적어 주세요. 비워 두면 번호를 자동으로 정해요.$moving")
            .setView(input)
            .setPositiveButton("만들기") { _, _ -> createNewRoom(input.text.toString()) }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun createNewRoom(typed: String) {
        Thread {
            val codes = RallyRoomSync.roomCodes(RallyRoomSync.DB_URL, roomAuth())
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
                        else RallyRoomSync.createRoom(RallyRoomSync.DB_URL, roomAuth(), code)?.let { "방을 만들지 못했어요: $it" }
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

    // --- 초대코드 및 관리자 모드 관련 기능 ---

    private fun updateAuthUI() {
        val isVerified = PreferencesHelper.isVerified(this)
        val userId = PreferencesHelper.getVerifiedUserId(this)

        if (PreferencesHelper.isUserView(this)) {
            // 개발자가 일반 사용자 화면을 보는 중: 집결 기능은 가려지고 연타만 남는다
            binding.tvAuthStatusTitle.text = "👤 일반 화면 (개발자 미리보기)"
            binding.tvAuthStatusTitle.setTextColor(Color.parseColor("#F59E0B"))
            binding.tvAuthStatusSubtitle.text = "일반 사용자가 보는 화면이에요. 눌러서 원래 화면으로 돌아가세요."
            binding.btnAuthAction.text = "개발자 화면으로 복귀"
            binding.btnAuthAction.setBackgroundColor(Color.parseColor("#2563EB"))
        } else if (PreferencesHelper.isAdminMode(this)) {
            binding.tvAuthStatusTitle.text = "👑 관리자 모드"
            binding.tvAuthStatusTitle.setTextColor(Color.parseColor("#2563EB"))
            binding.tvAuthStatusSubtitle.text = "방을 만들고 집결장에게 방 번호와 초대코드를 공유하세요."
            binding.btnAuthAction.text = "관리"
            binding.btnAuthAction.setBackgroundColor(Color.parseColor("#2563EB"))
        } else if (PreferencesHelper.isRosterAdmin(this)) {
            // 관리자로 등록된 기기가 집결장 화면으로 지내는 중
            binding.tvAuthStatusTitle.text = "🚩 집결장 모드"
            binding.tvAuthStatusTitle.setTextColor(Color.parseColor("#10B981"))
            binding.tvAuthStatusSubtitle.text = "관리자로 등록된 기기예요. 위쪽 버튼으로 코드 없이 관리자로 전환할 수 있어요."
            binding.btnAuthAction.text = "관리자로 전환"
        } else if (isVerified) {
            binding.tvAuthStatusTitle.text = "✅ 정회원 인증 완료"
            binding.tvAuthStatusTitle.setTextColor(Color.parseColor("#10B981"))
            binding.tvAuthStatusSubtitle.text = if (userId.isNotEmpty()) "인증된 회원 ID: $userId" else "정회원 인증이 완료되었습니다."
            binding.btnAuthAction.text = "인증 변경"
            binding.btnAuthAction.setBackgroundColor(Color.parseColor("#475569"))
        } else {
            binding.tvAuthStatusTitle.text = "🔒 집결은 회원 전용"
            binding.tvAuthStatusTitle.setTextColor(Color.parseColor("#F59E0B"))
            binding.tvAuthStatusSubtitle.text = "연타는 바로 쓸 수 있어요. 집결은 초대코드 인증 후 열려요."
            binding.btnAuthAction.text = "인증하기"
            binding.btnAuthAction.setBackgroundColor(Color.parseColor("#3B82F6"))
        }
        // 전환 버튼은 서버 명단에 있다고 확인된 기기에만 보이고, 그때는 열쇠(코드 입력) 아이콘이 필요 없다
        val userView = PreferencesHelper.isUserView(this)
        val roster = PreferencesHelper.isRosterAdmin(this) && !userView
        // 개발자로 확인된 기기에만 "일반 화면" 버튼을 보여 준다(누를 때 서버에 다시 확인한다)
        binding.btnUserView.visibility =
            if (!userView && roomPrefs().getBoolean("is_owner_cached", false)) View.VISIBLE else View.GONE
        binding.btnRoleSwitch.visibility = if (roster) View.VISIBLE else View.GONE
        binding.btnRoleSwitch.text = if (PreferencesHelper.isAdminMode(this)) "집결장으로 전환" else "관리자로 전환"
        binding.btnAdminIcon.visibility = if (roster || userView) View.GONE else View.VISIBLE
        // 집결장 화면에서는 위쪽 전환 버튼 하나만 둔다: 카드 안에 같은 "관리자로 전환"을 또 두지 않는다
        binding.btnAuthAction.visibility =
            if (roster && !PreferencesHelper.isAdminMode(this)) View.GONE else View.VISIBLE
        // 집결 방 카드는 인증한 회원·관리자에게만 보인다
        binding.cardRallyRoom.visibility = if (PreferencesHelper.hasAccess(this)) View.VISIBLE else View.GONE
        updateStartButtonVisibility()
        // 조작판이 떠 있는 채로 인증 상태가 바뀌어도 집결·헌터 아이콘이 바로 맞춰지게 한다
        AutoClickService.instance?.refreshMemberIcons()
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
            rememberOwner(owner)
            val admin = if (owner == Check.YES) Check.YES else server.isAdmin(uid)
            // 서버 명단에 있는 관리자면 마지막 접속 시각과 앱 버전을 적는다(개발자 화면에 보인다)
            val inRoster = if (owner == Check.YES) server.isAdmin(uid) else admin
            if (inRoster == Check.YES) server.reportSelf(uid, System.currentTimeMillis(), BuildConfig.VERSION_NAME)
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
                    PreferencesHelper.setAdminMode(this, false)
                    PreferencesHelper.setAdminViaServer(this, false)
                    AutoClickService.instance?.leaveRallyRoom()
                    Toast.makeText(this, "관리자 권한이 없어서 집결장 화면으로 돌아갑니다. 관리자 코드를 받아 다시 로그인해 주세요.", Toast.LENGTH_LONG).show()
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
        // "관리자 관리"와 "내 기기 ID 보기"는 개발자에게만 보인다(개발자 등록에 쓰는 것이라 관리자와 집결장은 필요 없다)
        val owner = roomPrefs().getBoolean("is_owner_cached", false)
        val items = buildList {
            add("집결장 코드 발급")
            if (owner) add("관리자 관리 (개발자 전용)")
            if (owner) add("내 기기 ID 보기")
            if (!PreferencesHelper.isRosterAdmin(this@MainActivity)) add("집결장으로 전환") // 위쪽 전환 버튼이 없을 때만
            if (owner) add("일반 화면으로 전환 (개발자 전용)")
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("관리자")
            .setItems(items) { _, which ->
                when (items[which]) {
                    "집결장 코드 발급" -> showAdminPanelDialog()
                    "관리자 관리 (개발자 전용)" -> AdminRosterUi(this, adminServer()).showManage()
                    "내 기기 ID 보기" -> AdminRosterUi(this, adminServer()).showMyId()
                    "일반 화면으로 전환 (개발자 전용)" -> enterUserView()
                    else -> switchToLeader()
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
            .setMessage("개발자에게 받은 관리자 코드를 입력해 주세요. 이미 관리자나 개발자로 등록된 기기는 칸을 비워 두고 확인을 누르면 됩니다.")
            .setView(input)
            .setPositiveButton("확인") { d, _ ->
                val typed = input.text.toString().trim()
                if (typed.isEmpty()) {
                    enterFromRoster(askCodeIfNot = false)
                } else if (AdminRoster.looksLikeCode(typed)) {
                    // 관리자 코드: 서버 명단에 올라야 관리자가 된다
                    Toast.makeText(this, "관리자 코드를 확인하는 중…", Toast.LENGTH_SHORT).show()
                    Thread {
                        val err = adminServer().redeem(typed)
                        runOnUiThread {
                            if (err == null) {
                                PreferencesHelper.setAdminMode(this, true)
                                PreferencesHelper.setAdminViaServer(this, true)
                                PreferencesHelper.setRosterAdmin(this, true)
                                AutoClickService.instance?.leaveRallyRoom() // 권한이 바뀌면 패널을 새로 만든다
                                updateAuthUI()
                                updateRallyInfoCard()
                                Toast.makeText(this, "👑 관리자로 등록됐어요. 다음부터는 코드 없이 전환할 수 있어요.", Toast.LENGTH_LONG).show()
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

    /** 관리자 화면을 끄고 집결장 화면으로 간다. 서버 명단에는 그대로 남아 있어, 나중에 코드 없이 다시 관리자로 전환할 수 있다. */
    private fun switchToLeader() {
        PreferencesHelper.setAdminMode(this, false)
        PreferencesHelper.setAdminViaServer(this, false)
        AutoClickService.instance?.leaveRallyRoom() // 권한이 바뀌면 패널을 새로 만든다
        Toast.makeText(this, "집결장으로 전환했어요.", Toast.LENGTH_SHORT).show()
        updateAuthUI()
        updateRallyInfoCard()
    }

    /**
     * 코드 없이 관리자로 들어간다. 서버가 이 기기를 개발자나 관리자로 알고 있을 때만 된다(누를 때마다 서버에 다시 묻는다).
     * 명단에 없으면 들여보내지 않고, [askCodeIfNot]이면 관리자 코드 입력 창을 띄운다. 확인하지 못했을 때도 들여보내지 않는다.
     */
    private fun enterFromRoster(askCodeIfNot: Boolean) {
        Toast.makeText(this, "관리자 명단을 확인하는 중…", Toast.LENGTH_SHORT).show()
        Thread {
            val server = adminServer()
            val uid = server.uid().value
            val owner = if (uid == null) Check.UNKNOWN else server.isOwner(uid)
            rememberOwner(owner)
            val admin = if (uid == null) Check.UNKNOWN else if (owner == Check.YES) Check.YES else server.isAdmin(uid)
            runOnUiThread {
                when (AdminRoster.rosterEntry(owner, admin)) {
                    Check.YES -> {
                        PreferencesHelper.setAdminMode(this, true)
                        PreferencesHelper.setAdminViaServer(this, true)
                        PreferencesHelper.setRosterAdmin(this, true)
                        AutoClickService.instance?.leaveRallyRoom() // 권한이 바뀌면 패널을 새로 만든다
                        updateAuthUI()
                        updateRallyInfoCard()
                        Toast.makeText(this, if (owner == Check.YES) "👑 개발자로 들어왔어요." else "👑 관리자로 전환했어요.", Toast.LENGTH_SHORT).show()
                    }
                    Check.NO -> {
                        PreferencesHelper.setRosterAdmin(this, false)
                        updateAuthUI()
                        updateRallyInfoCard()
                        Toast.makeText(this, "관리자 명단에 없는 기기예요. 관리자 코드를 입력해 주세요.", Toast.LENGTH_LONG).show()
                        if (askCodeIfNot && !isFinishing && !isDestroyed) showAdminLoginDialog()
                    }
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
            // 방 번호를 따로 한 번 더 보내지 않아도 되게, 지금 방이 있으면 같은 메시지에 넣는다
            val room = roomPrefs().getString("cloud_room_number", "") ?: ""
            val roomLine = if (room.isEmpty()) "" else "\n집결 방 번호: $room (인증 후 집결 방에 입력)"
            val shareMsg = "[AutoClicker Pro 정회원 초대]\n회원 ID: $currentMemberId\n초대코드: $currentGeneratedCode\n앱 실행 후 인증창에 입력하시면 정회원으로 등록됩니다.$roomLine"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("AutoClickerInvite", shareMsg)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "📋 카카오톡 전달 메시지가 복사되었습니다!", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }
}




