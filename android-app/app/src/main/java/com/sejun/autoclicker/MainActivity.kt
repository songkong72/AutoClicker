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
        if (!PreferencesHelper.isVerified(this)) {
            binding.root.post {
                showVerificationDialog()
            }
        }
    }

    override fun onResume() {
        super.onResume()
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
        // 회원 인증 관리 버튼
        binding.btnAuthAction.setOnClickListener {
            showVerificationDialog()
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
            if (service?.isRallyReserved == true) {
                service.cancelRallyReservation()
                Toast.makeText(this, "🛑 집결 출발 예약을 취소했습니다.", Toast.LENGTH_SHORT).show()
            } else {
                service?.stopAutoClick()
                Toast.makeText(this, "🛑 치료 연타를 즉시 정지했습니다.", Toast.LENGTH_SHORT).show()
            }
            updateServiceState()
        }

        // ⚔️ 집결 동시 착탄 및 그룹 작전 설정 버튼
        binding.btnOpenRallySettings.setOnClickListener {
            if (!PreferencesHelper.isVerified(this)) {
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

            if (!service.isOverlaysShowing()) {
                saveSettings()
                service.showOverlays()
            }
            service.showRallyDialog()
            Toast.makeText(this, "⚔️ 집결 동시 착탄 설정을 띄웠습니다.", Toast.LENGTH_SHORT).show()
            moveTaskToBack(true)
        }

        // Single Smart Toggle Button: [🚀 오토클리커 띄우기] ↔ [✕ 오토클리커 숨기기]
        binding.btnStartService.setOnClickListener {
            if (!PreferencesHelper.isVerified(this)) {
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
        val isRally = service?.isRallyReserved == true

        if (isRally) {
            binding.btnEmergencyStop.visibility = View.VISIBLE
            binding.btnEmergencyStop.text = "🛑 [${service?.reservedGroupName}] 출발 예약 취소"
        } else if (isClicking) {
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

    private fun updateRallyInfoCard() {
        try {
            val group = RallyGroupManager.getSelectedGroup(this)
            val leaderDesc = if (group.leaderName.isNotBlank()) " (집결장: ${group.leaderName})" else ""
            binding.tvMainRallyGroupInfo.text = "🚩 ${group.name}$leaderDesc"
            val marchStr = if (group.marchDurationSec % 1.0 == 0.0) group.marchDurationSec.toInt().toString() else String.format(java.util.Locale.US, "%.1f", group.marchDurationSec)
            binding.tvMainRallyDetailInfo.text = "목표: ${group.getTargetTimeString()} (행군 ${marchStr}초) ➔ 출발: ${group.getDepartureTimeString()}"
        } catch (e: Exception) {
            e.printStackTrace()
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

        if (isVerified) {
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
            hint = "비밀번호를 입력하세요"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setPadding(50, 40, 50, 40)
        }

        AlertDialog.Builder(this)
            .setTitle("👑 관리자 로그인")
            .setMessage("관리자 비밀번호를 입력해 주세요.")
            .setView(input)
            .setPositiveButton("확인") { d, _ ->
                val pass = input.text.toString().trim()
                if (InvitationManager.checkAdminPassword(this, pass)) {
                    Toast.makeText(this, "👑 관리자 모드로 진입합니다.", Toast.LENGTH_SHORT).show()
                    d.dismiss()
                    showAdminPanelDialog()
                } else {
                    Toast.makeText(this, "❌ 관리자 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
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

        val etNewPass = dialogView.findViewById<EditText>(R.id.etNewMasterPassword)
        etNewPass.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
        val btnSavePass = dialogView.findViewById<Button>(R.id.btnSaveMasterPassword)

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

        btnSavePass.setOnClickListener {
            val newPass = etNewPass.text.toString().trim()
            if (newPass.length < 4) {
                Toast.makeText(this, "비밀번호는 최소 4자 이상이어야 합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (InvitationManager.updateAdminPassword(this, newPass)) {
                Toast.makeText(this, "🔑 관리자 비밀번호가 성공적으로 변경되었습니다.", Toast.LENGTH_LONG).show()
                etNewPass.setText("")
            }
        }

        dialog.show()
    }
}
