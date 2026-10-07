package com.sejun.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import java.util.Locale
import android.view.animation.DecelerateInterpolator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.content.res.ColorStateList
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.content.ClipData
import android.content.ClipboardManager
import android.text.Editable
import android.text.TextWatcher
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.Calendar
import kotlin.coroutines.resume

class AutoClickService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoClickService"
        private const val CHANNEL_ID = "AutoClickerNotificationChannel"
        private const val NOTIFICATION_ID = 2002

        const val ACTION_STOP_CLICK = "com.sejun.autoclicker.ACTION_STOP_CLICK"
        const val ACTION_CLOSE_ALL = "com.sejun.autoclicker.ACTION_CLOSE_ALL"

        var instance: AutoClickService? = null
            private set

        fun isServiceRunning(): Boolean = instance != null
    }

    private var clickJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    var isClicking: Boolean = false
        private set

    // Independent WindowManager Views
    private var windowManager: WindowManager? = null
    private var controlView: View? = null
    private var controlParams: WindowManager.LayoutParams? = null
    private var targetView: View? = null
    private var targetParams: WindowManager.LayoutParams? = null

    private var isTargetVisible: Boolean = true
    private var isControlCollapsed: Boolean = false

    private var settingsDialogView: View? = null
    private var settingsDialogParams: WindowManager.LayoutParams? = null

    private var opacityPanelView: View? = null
    private var opacityPanelParams: WindowManager.LayoutParams? = null

    private var rallyPanelHost: RallyPanelHost? = null
    private var rallyRoomSync: RallyRoomSync? = null
    private var rallyPanelRoleAdmin = false
    /** 내 클릭 직전에 과녁을 미리 터치 통과로 바꿔 둔 상태인지. */
    @Volatile private var rallyClickArmed = false

    /** 저장된 타겟 위치를 지금 1회 탭한다 (상대시간 집결의 예약 시각에 호출됨). */
    fun performRallyClickNow() {
        val saved = PreferencesHelper.getSavedRallyTargetPosition(this) ?: run {
            rallyRoomSync?.clickResult = "⚠️ 클릭 안 함: 저장된 위치 없음"
            vibrate(80); showToast("⚠️ 저장된 타겟 위치가 없어 클릭하지 못했어요"); return
        }
        val target = targetView
        val loc = IntArray(2)
        target?.getLocationOnScreen(loc)
        // 과녁이 화면에 없으면(오버레이를 숨긴 경우) 창 좌표와 화면 좌표의 차이를 알 수 없으므로 0으로 본다.
        // loc이 [0,0]인 채로 params.x를 빼면 좌표가 화면 왼쪽 위 구석으로 틀어진다.
        val offX = if (target != null) loc[0] - (targetParams?.x ?: 0) else 0
        val offY = if (target != null) loc[1] - (targetParams?.y ?: 0) else 0
        val w = if (target != null && target.width > 0) target.width else dpToPx(38)
        val h = if (target != null && target.height > 0) target.height else dpToPx(38)
        val cx = saved.first + offX + w / 2f
        val cy = saved.second + offY + h / 2f
        val dm = resources.displayMetrics
        val armed = rallyClickArmed
        val where = "탭 좌표 (${cx.toInt()}, ${cy.toInt()}) · 화면 ${dm.widthPixels}x${dm.heightPixels} · 과녁 ${if (target == null) "없음" else "있음"} · 투과 ${if (armed) "사전" else "즉시"}"
        rallyRoomSync?.clickResult = "탭 전송 중 · $where"
        // 과녁 오버레이가 터치를 가로채지 않도록 먼저 투과시킨 뒤 탭한다 (기존 예약 클릭과 동일한 방식).
        setTargetTouchable(false)
        mainHandler.postDelayed({
            val path = Path().apply { moveTo(cx, cy) }
            val stroke = GestureDescription.StrokeDescription(path, 0L, 60L)
            val callback = object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    rallyRoomSync?.clickResult = "✓ 탭 완료 · $where"
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    rallyRoomSync?.clickResult = "✗ 탭 취소됨 · $where"
                }
            }
            val ok = dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), callback, null)
            Log.d(TAG, "rally click at ($cx, $cy) dispatched=$ok")
            if (!ok) rallyRoomSync?.clickResult = "✗ 탭 전송 거부됨 · $where"
            showToast(if (ok) "🎯 집결 클릭! (${cx.toInt()}, ${cy.toInt()})" else "⚠️ 클릭 전송 실패")
            mainHandler.postDelayed({ rallyClickArmed = false; setTargetTouchable(true) }, 500L)
        }, if (armed) 0L else 40L)
    }

    private var rallyRoomCode = ""

    /** 방을 떠나거나 바꿀 때: 패널을 닫고 폴링과 예약된 클릭을 모두 멈춘다. */
    fun leaveRallyRoom() {
        rallyPanelHost?.hide()
        rallyRoomSync?.stop()
        rallyPanelHost = null
        rallyRoomSync = null
        rallyRoomCode = ""
    }

    /**
     * 패널에서 다른 방으로 옮긴다: 이전 방 연결과 예약된 클릭을 끊고, 새 방으로 패널을 다시 연다.
     * 명단 등록은 새 방 연결이 시작될 때 자동으로 된다(registerSelf).
     */
    fun switchRallyRoom(code: String) {
        val prefs = getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE)
        // 패널에서 옮길 때는 방을 만들지 않는다("" 로 표시해 예전 버전 허용 규칙도 끈다)
        prefs.edit().putString("cloud_room_number", code).putString("cloud_room_creatable", "").apply()
        RallyRoomHistory.record(prefs, code)
        leaveRallyRoom()
        showToast("방 $code 로 옮겼어요")
        toggleRallyPanel()
    }

    /**
     * 인증하지 않은 사용자는 연타만 쓴다: 조작판의 집결(깃발)과 헌터(곰) 아이콘을 숨긴다.
     * 조작판을 만들 때와, 앱 화면에서 인증 상태가 바뀌었을 때 부른다.
     */
    fun refreshMemberIcons() {
        val control = controlView ?: return
        val member = PreferencesHelper.hasAccess(this)
        val bearUnlocked = getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE).getBoolean("bear_mode_unlocked", false)
        control.findViewById<ImageButton>(R.id.btnRally)?.visibility = if (member) View.VISIBLE else View.GONE
        control.findViewById<ImageButton>(R.id.btnBearMode)?.visibility = if (member && bearUnlocked) View.VISIBLE else View.GONE
    }

    /** 숨은 곰 사냥 모드를 개방한다. 이미 열려 있으면 안내만 한다. 새 집결 팝업과 옛 대화창 양쪽에서 쓴다. */
    fun unlockBearMode() {
        val prefs = getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("bear_mode_unlocked", false)) {
            prefs.edit().putBoolean("bear_mode_unlocked", true).apply()
            controlView?.findViewById<android.widget.ImageButton>(R.id.btnBearMode)?.visibility = View.VISIBLE
            showToast("🐻 비밀 헌터 모드가 개방되었습니다!")
        } else {
            showToast("🐻 이미 헌터 모드가 열려있습니다!")
        }
    }

    fun toggleRallyPanel() {
        val isAdmin = PreferencesHelper.isAdminMode(this)
        if (!PreferencesHelper.hasAccess(this)) {
            Toast.makeText(this, "🔒 초대코드 인증이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val wm = windowManager ?: return
        val room = getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE)
            .getString("cloud_room_number", "") ?: ""
        if (room.isEmpty()) {
            // 깃발이 아무 반응 없는 것처럼 보이지 않게: 안내와 함께 앱 첫 화면을 열어 바로 방에 입장하게 한다.
            showToast("집결 방에 먼저 입장해 주세요. 앱 화면을 열게요")
            try {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            } catch (e: Exception) {
                Log.w(TAG, "앱 화면을 열지 못했다", e)
            }
            return
        }
        if (rallyPanelHost != null && (rallyPanelRoleAdmin != isAdmin || rallyRoomCode != room)) { // 권한이나 방이 바뀌면 새로 만든다
            leaveRallyRoom()
        }
        rallyPanelRoleAdmin = isAdmin
        rallyRoomCode = room
        val host = rallyPanelHost ?: run {
            val source = RallyRoomSync(
                dbUrl = firebaseDbUrl, room = room, isAdmin = isAdmin,
                auth = FirebaseAuthClient(BuildConfig.FIREBASE_API_KEY,
                    load = { getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE).getString("fb_refresh", null) },
                    save = { t -> getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE).edit().putString("fb_refresh", t).apply() }),
                memberId = PreferencesHelper.getRallyMemberId(this),
                getCharacterName = { PreferencesHelper.getRallyCharacterName(this) },
                saveCharacterName = { name -> PreferencesHelper.setRallyCharacterName(this, name) },
                correctionMs = { PreferencesHelper.getClickOffsetMs(this).toLong() },
                setCorrectionMs = { ms -> PreferencesHelper.setClickOffsetMs(this, ms) },
                positionText = {
                    PreferencesHelper.getSavedRallyTargetPosition(this)?.let { "저장된 클릭 위치 ${it.first}, ${it.second}" }
                        ?: "클릭 위치 없음 · 과녁을 집결 버튼 위에 놓고 저장하세요"
                },
                positionSaved = { PreferencesHelper.getSavedRallyTargetPosition(this) != null },
                savePosition = {
                    PreferencesHelper.setSavedRallyTargetPosition(this, targetParams?.x ?: 0, targetParams?.y ?: 0)
                    showToast("현재 과녁 위치를 클릭 위치로 저장했어요")
                },
                onClickDue = { performRallyClickNow() },
                onClickArm = { on -> rallyClickArmed = on; setTargetTouchable(!on) },
                onRallyStart = { rallyPanelHost?.let { if (!it.isShowing) it.show() } },
                onCancel = { },
                onOtherAdminChange = { who -> showToast("다른 관리자($who)가 방을 바꿨어요") },
                onWriteFailed = { why -> showToast(why) },
                canCreateRoom = {
                    // 앱 첫 화면에서 만든·입장한 방만 새로 만든다. 값이 아예 없으면(예전 버전에서 정한 방) 그대로 허용한다.
                    val p = getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE)
                    val f = p.getString("cloud_room_creatable", null)
                    f == null || f == room
                }
            ).also { it.start(); rallyRoomSync = it }
            RallyPanelHost(this, wm, source, onSecretUnlock = { unlockBearMode() }, onSwitchRoom = { code -> switchRallyRoom(code) }).also { rallyPanelHost = it }
        }
        host.toggle()
    }

    private var currentIntervalMs: Long = 500L
    private var lastToggleTime: Long = 0L
    private var currentOverlayAlpha: Float = 1.0f
    
    
    private val firebaseDbUrl = RallyRoomSync.DB_URL

    var onStatusChanged: ((Boolean) -> Unit)? = null
    var onClickExecuted: (() -> Unit)? = null
    var onOverlaysVisibilityChanged: ((Boolean) -> Unit)? = null

    // Broadcast receiver for notification action buttons & screen off
    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_STOP_CLICK -> {
                    stopAutoClick()
                    showToast("⏹ 알림창에서 연타를 정지했습니다.")
                }
                ACTION_CLOSE_ALL -> {
                    hideOverlays()
                    showToast("✕ 오토클리커를 완전히 종료했습니다.")
                }
                Intent.ACTION_SCREEN_OFF -> {
                    stopAutoClick()
                }
            }
        }
    }


    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        currentOverlayAlpha = PreferencesHelper.getOverlayAlpha(this)
        Log.d(TAG, "AutoClickService connected.")
        HunterModeManager.loadSettings(this)

        serviceInfo = (serviceInfo ?: AccessibilityServiceInfo()).apply {
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }

        val filter = IntentFilter().apply {
            addAction(ACTION_STOP_CLICK)
            addAction(ACTION_CLOSE_ALL)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(actionReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(actionReceiver, filter)
        }

        showToast("⚡ 오토클리커 엔진 준비 완료! 앱에서 [띄우기]를 눌러주세요.")
    }

    /**
     * 연타 중에 물리 볼륨 버튼(볼륨 업 또는 다운)을 누르면 즉시 긴급 정지!
     * 연타 중이 아닐 때는 볼륨 키를 건드리지 않는다(곰 사냥 발사는 🐻 발사 버튼으로만 한다).
     */
    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN) {
            val code = event.keyCode
            if (code == KeyEvent.KEYCODE_VOLUME_DOWN || code == KeyEvent.KEYCODE_VOLUME_UP) {
                if (isClicking) {
                    stopAutoClick()
                    vibrate(60)
                    showToast("🛑 볼륨 키로 연타를 즉시 정지했습니다!")
                    return true
                }
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // Not needed
    }

    override fun onInterrupt() {
        Log.w(TAG, "AutoClickService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        MainThreadWatchdog.stop()
        rallyPanelHost?.hide()
        rallyRoomSync?.stop()
        stopAutoClick()
        hideOverlays()
        serviceScope.cancel()
        try {
            unregisterReceiver(actionReceiver)
        } catch (e: Exception) {
            // ignore
        }
        if (instance == this) {
            instance = null
        }
        Log.d(TAG, "AutoClickService destroyed.")
    }

    /**
     * 화면에 컨트롤 패널과 조준점 과녁을 띄웁니다.
     * 이미 떠 있다면 기존 상태를 안전하게 리셋하고 새로 띄웁니다!
     */
    fun showOverlays(intervalMs: Long? = null) {
        // 연타는 인증 없이도 쓸 수 있다. 집결·헌터만 회원 전용이고, 그 아이콘은 조작판을 만들 때 숨긴다.
        currentIntervalMs = intervalMs ?: PreferencesHelper.getIntervalMs(this)
        currentOverlayAlpha = PreferencesHelper.getOverlayAlpha(this)
        val wm = windowManager ?: getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        // 기존 뷰가 있다면 리셋
        hideOverlays()

        try {
            createTargetView(wm)
            createControlView(wm)
        } catch (e: Exception) {
            // 메뉴 생성이 실패하면 먼저 붙은 조준점만 남지 않게 모두 걷어 낸다.
            Log.e(TAG, "Overlay creation failed", e)
            hideOverlays()
            showToast("화면 위젯을 띄우지 못했어요. 다시 시도해 주세요.")
            return
        }
        updateNotification()
        onOverlaysVisibilityChanged?.invoke(true)

        MainThreadWatchdog.start()
    }

    /**
     * 오토클리커 플로팅 위젯들을 화면에서 완전히 닫고 제거합니다.
     */
    fun hideOverlays() {
        // 오버레이를 끌 때 헌터 감시도 같이 끈다(안 끄면 보이지 않는 채로 계속 캡처하고 클릭할 수 있다)
        HunterModeManager.stop()
        hideHunterFireButton()
        hideBearSetupUi()
        hideClock()
        stopAutoClick()
        hideSettingsDialog()
        rallyPanelHost?.hide() // 새 집결 팝업도 함께 닫는다(방 연결은 유지)
        hideOpacityPanel()
        val wm = windowManager ?: return

        // isAttachedToWindow 로 걸러 내지 않는다: addView 직후 첫 프레임 전에는 false라서, 그때 끄면
        // 제거를 건너뛴 채 참조만 지워져 조준점이 화면에 영원히 남는다. 붙지 않은 뷰는 예외로 걸러진다.
        controlView?.let {
            try { wm.removeView(it) } catch (e: Exception) { Log.w(TAG, "Error removing control", e) }
        }
        targetView?.let {
            try { wm.removeView(it) } catch (e: Exception) { Log.w(TAG, "Error removing target", e) }
        }
        controlView = null
        targetView = null
        cancelNotification()
        onOverlaysVisibilityChanged?.invoke(false)
    }

    fun isOverlaysShowing(): Boolean = controlView != null

    /**
     * 과녁 조준점을 화면 중앙으로 소환(리셋)합니다.
     */
    fun recenterTarget() {
        val target = targetView ?: return
        val wm = windowManager ?: return
        val tParams = targetParams ?: return
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)

        tParams.x = (metrics.widthPixels / 2) - (target.width / 2)
        tParams.y = (metrics.heightPixels / 2) - (target.height / 2)
        PreferencesHelper.setTargetPosition(this, tParams.x, tParams.y)
        isTargetVisible = true
        target.visibility = View.VISIBLE
        try {
            wm.updateViewLayout(target, tParams)
            val btnToggle = controlView?.findViewById<ImageButton>(R.id.btnToggleTarget)
            btnToggle?.setImageResource(R.drawable.ic_visibility_off)
            btnToggle?.setColorFilter(Color.parseColor("#94A3B8"))
            showToast("🎯 조준점을 화면 중앙에 배치했습니다.")
        } catch (e: Exception) {
            Log.e(TAG, "Error recentering target", e)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    @Suppress("DEPRECATION")
    private fun createTargetView(wm: WindowManager) {
        val target = TargetCrosshairView(this)
        this.targetView = target
        val targetSize = target.targetSizePx

        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)

        // 이전에 저장된 게임 출발 버튼 위치 복원 (없으면 화면 중앙 기본값)
        val savedPos = PreferencesHelper.getTargetPosition(this)
        val defaultStartX = savedPos?.first ?: ((metrics.widthPixels / 2) - (targetSize / 2))
        val defaultStartY = savedPos?.second ?: ((metrics.heightPixels / 2) - (targetSize / 2))

        val params = WindowManager.LayoutParams(
            targetSize,
            targetSize,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = defaultStartX
            y = defaultStartY
        }
        this.targetParams = params

        target.visibility = if (isTargetVisible) View.VISIBLE else View.GONE
        target.alpha = currentOverlayAlpha

        // 과녁 조준점 드래그 리스너 (위치 이동 시 자동으로 게임 버튼 위치 영구 저장!)
        target.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchDownX = 0f
            private var touchDownY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                if (isClicking) return false
                val p = targetParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = p.x
                        startY = p.y
                        touchDownX = event.rawX
                        touchDownY = event.rawY
                        target.setDraggingState(true)
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchDownX).toInt()
                        val dy = (event.rawY - touchDownY).toInt()
                        p.x = startX + dx
                        p.y = startY + dy
                        try {
                            wm.updateViewLayout(target, p)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating target layout", e)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        target.setDraggingState(false)
                        val pCurrent = targetParams
                        if (pCurrent != null) {
                            PreferencesHelper.setTargetPosition(this@AutoClickService, pCurrent.x, pCurrent.y)
                        }
                        return true
                    }
                }
                return false
            }
        })

        wm.addView(target, params)
    }

    @Suppress("DEPRECATION")
    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun createControlView(wm: WindowManager) {
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)

        // 초기 위치: 화면 좌측 상단 부근 (가려짐 방지)
        val defaultStartX = dpToPx(16)
        val defaultStartY = (metrics.heightPixels * 0.25f).toInt()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = defaultStartX
            y = defaultStartY
        }
        this.controlParams = params

        val inflater = LayoutInflater.from(this)
        val control = inflater.inflate(R.layout.layout_floating_control, null)
        this.controlView = control

        val btnPlayPause = control.findViewById<ImageButton>(R.id.btnPlayPause)
        val btnToggleTarget = control.findViewById<ImageButton>(R.id.btnToggleTarget)
        val btnRally = control.findViewById<ImageButton>(R.id.btnRally)
        val btnBearMode = control.findViewById<ImageButton>(R.id.btnBearMode)
        refreshMemberIcons()
        btnBearMode?.setOnClickListener {
            vibrate(20)
            hideOpacityPanel()
            toggleBearMode()
        }
        btnBearMode?.setOnLongClickListener {
            vibrate(30)
            hideOpacityPanel()
            if (!HunterModeManager.isHunterModeEnabled) toggleBearMode()
            hideBearSetupUi()
            showBearSetupUi() // 위치 다시 잡기
            true
        }
        val btnSettings = control.findViewById<ImageButton>(R.id.btnSettings)
        val btnClock = control.findViewById<ImageButton>(R.id.btnClock)
        val btnClose = control.findViewById<ImageButton>(R.id.btnClose)
        val btnFoldToggle = control.findViewById<ImageButton>(R.id.btnFoldToggle)
        val collapsibleContainer = control.findViewById<View>(R.id.collapsibleContainer)

        // 1. 재생 / 정지 버튼 (▶ / ⏸)
        btnPlayPause.setOnClickListener {
            vibrate(30)
            hideOpacityPanel()
            toggleAutoClick()
        }

        // 2. 타겟 조준점 숨기기 / 보이기 토글 버튼 (👁)
        btnToggleTarget.setOnClickListener {
            vibrate(20)
            hideOpacityPanel()
            toggleTargetVisibility()
        }
        btnToggleTarget.setOnLongClickListener {
            vibrate(30)
            hideOpacityPanel()
            recenterTarget()
            true // 이벤트 소비
        }

        // 3. 집결 동시 착탄 및 그룹 작전 팝업 (🚩)
        btnRally?.setOnClickListener {
            vibrate(20)
            hideOpacityPanel()
            toggleRallyPanel()
        }

        // 4. 인게임 실시간 설정 팝업 버튼 (⚙️)
        btnSettings?.setOnClickListener {
            vibrate(20)
            hideOpacityPanel()
            showSettingsDialog()
        }

        // 5. 초 단위 시계 켜기/끄기 (투명도는 ⚙ 설정 창에서 바꾼다)
        btnClock?.setColorFilter(Color.parseColor(if (clockView != null) "#38BDF8" else "#CBD5E1"))
        btnClock?.setOnClickListener {
            vibrate(20)
            toggleClock()
        }

        // 5. 플로팅 컨트롤러 및 타겟 완전 종료 (✕)
        btnClose.setOnClickListener {
            vibrate(20)
            hideOverlays()
            // 방에 들어가 있으면 화면만 닫히고 방 연결은 남는다: 완전히 꺼진 것으로 오해하지 않게 알린다.
            showToast(if (rallyRoomSync != null) "화면만 닫았어요. 집결이 시작되면 자동으로 클릭해요." else "✕ 오토클리커가 종료되었습니다.")
        }

        // 6. 접기 / 펼치기 토글 버튼 (^ / v)
        btnFoldToggle.setOnClickListener {
            vibrate(20)
            hideOpacityPanel()
            toggleFold()
        }

        control.alpha = currentOverlayAlpha

        // 플로팅 바 드래그 및 스마트폰 좌/우 옆라인 자석 스냅(Magnetic Snap to Edge) 리스너
        val dragTouchListener = object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchDownRawX = 0f
            private var touchDownRawY = 0f
            private var isDragging = false
            private var snapAnimator: ValueAnimator? = null

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val p = controlParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        snapAnimator?.cancel()
                        startX = p.x
                        startY = p.y
                        touchDownRawX = event.rawX
                        touchDownRawY = event.rawY
                        isDragging = false
                        return false // 일반 버튼 클릭 허용
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchDownRawX).toInt()
                        val dy = (event.rawY - touchDownRawY).toInt()
                        if (!isDragging && (Math.abs(dx) > dpToPx(6) || Math.abs(dy) > dpToPx(6))) {
                            isDragging = true
                            v.isPressed = false
                            hideOpacityPanel() // 드래그 시작 시 열려있던 투명도 패널 닫기
                        }
                        if (isDragging) {
                            val metrics = DisplayMetrics()
                            wm.defaultDisplay.getRealMetrics(metrics)
                            val screenWidth = metrics.widthPixels
                            val screenHeight = metrics.heightPixels
                            val controlWidth = control.width.takeIf { it > 0 } ?: dpToPx(46)
                            val controlHeight = control.height.takeIf { it > 0 } ?: dpToPx(180)
                            p.x = (startX + dx).coerceIn(0, screenWidth - controlWidth)
                            p.y = (startY + dy).coerceIn(dpToPx(40), screenHeight - controlHeight - dpToPx(20))
                            try {
                                wm.updateViewLayout(control, p)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error updating control layout", e)
                            }
                            return true
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            isDragging = false
                            // 휴대폰 좌/우 옆라인으로 자석처럼 찰칵 붙는 애니메이션 (Magnetic Snap to Edge)
                            val metrics = DisplayMetrics()
                            wm.defaultDisplay.getRealMetrics(metrics)
                            val screenWidth = metrics.widthPixels
                            val controlWidth = control.width.takeIf { it > 0 } ?: dpToPx(46)
                            val centerX = p.x + (controlWidth / 2)
                            val targetSnapX = if (centerX < screenWidth / 2) {
                                dpToPx(4) // 왼쪽 옆라인 밀착
                            } else {
                                screenWidth - controlWidth - dpToPx(4) // 오른쪽 옆라인 밀착
                            }

                            snapAnimator = ValueAnimator.ofInt(p.x, targetSnapX).apply {
                                duration = 220L
                                interpolator = DecelerateInterpolator()
                                addUpdateListener { anim ->
                                    p.x = anim.animatedValue as Int
                                    try {
                                        wm.updateViewLayout(control, p)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error animating snap", e)
                                    }
                                }
                                addListener(object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: Animator) {
                                        vibrate(15) // 자석 착 달라붙는 햅틱 피드백!
                                    }
                                })
                            }
                            snapAnimator?.start()
                            return true
                        }
                    }
                }
                return false
            }
        }

        control.setOnTouchListener(dragTouchListener)
        btnPlayPause.setOnTouchListener(dragTouchListener)
        btnToggleTarget.setOnTouchListener(dragTouchListener)
        btnRally?.setOnTouchListener(dragTouchListener)
        btnSettings?.setOnTouchListener(dragTouchListener)
        btnClock?.setOnTouchListener(dragTouchListener)
        btnClose.setOnTouchListener(dragTouchListener)
        btnFoldToggle.setOnTouchListener(dragTouchListener)

        wm.addView(control, params)
    }

    /**
     * 타겟 조준점 숨기기 / 보이기 토글
     */
    fun toggleTargetVisibility() {
        val target = targetView ?: return
        val control = controlView ?: return
        val btnToggle = control.findViewById<ImageButton>(R.id.btnToggleTarget) ?: return

        isTargetVisible = !isTargetVisible
        target.visibility = if (isTargetVisible) View.VISIBLE else View.GONE

        if (isTargetVisible) {
            btnToggle.setImageResource(R.drawable.ic_visibility_off)
            btnToggle.setColorFilter(Color.parseColor("#94A3B8"))
            showToast("👁 조준점을 표시했습니다.")
        } else {
            btnToggle.setImageResource(R.drawable.ic_visibility)
            btnToggle.setColorFilter(Color.parseColor("#38BDF8"))
            showToast("👁 조준점을 숨겼습니다. (연타는 계속 작동)")
        }
    }

    /**
     * 타겟 조준점을 지정된 (x, y) 화면 좌표로 즉시 이동
     */
    fun moveTargetViewTo(x: Int, y: Int) {
        mainHandler.post {
            val target = targetView
            val p = targetParams
            if (target != null && p != null) {
                p.x = x
                p.y = y
                try {
                    windowManager?.updateViewLayout(target, p)
                } catch (e: Exception) {
                    Log.e(TAG, "Error moving target view", e)
                }
                if (!isTargetVisible) {
                    isTargetVisible = true
                    target.visibility = View.VISIBLE
                    controlView?.findViewById<ImageButton>(R.id.btnToggleTarget)?.let {
                        it.setImageResource(R.drawable.ic_visibility_off)
                        it.setColorFilter(Color.parseColor("#94A3B8"))
                    }
                }
                (target as? TargetCrosshairView)?.pulse()
            }
        }
    }

    /**
     * 사이드 바 접기 / 펼치기 토글 (^ / v)
     * 접으면 시작/정지 버튼만 화면 가장자리에 작게 남음!
     */
    fun toggleFold() {
        val control = controlView ?: return
        val wm = windowManager ?: return
        val params = controlParams ?: return
        val container = control.findViewById<View>(R.id.collapsibleContainer) ?: return
        val btnFold = control.findViewById<ImageButton>(R.id.btnFoldToggle) ?: return

        isControlCollapsed = !isControlCollapsed
        if (isControlCollapsed) {
            container.visibility = View.GONE
            btnFold.setImageResource(R.drawable.ic_chevron_down)
            btnFold.setColorFilter(Color.parseColor("#38BDF8"))
        } else {
            container.visibility = View.VISIBLE
            btnFold.setImageResource(R.drawable.ic_chevron_up)
            btnFold.setColorFilter(Color.parseColor("#CBD5E1"))
        }

        try {
            wm.updateViewLayout(control, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating folded control layout", e)
        }
    }

    fun hideOpacityPanel() {
        opacityPanelView?.let {
            if (it.isAttachedToWindow) {
                try {
                    windowManager?.removeView(it)
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing opacity panel", e)
                }
            }
        }
        opacityPanelView = null
        opacityPanelParams = null
    }

    /** 바깥 터치로 투명도 패널이 닫힌 시각. 같은 터치의 버튼 클릭이 패널을 다시 여는 것을 막는다. */
    private var opacityPanelClosedByOutsideAt = 0L

    fun toggleOpacityPanel() {
        if (opacityPanelView != null) {
            hideOpacityPanel()
        } else if (System.currentTimeMillis() - opacityPanelClosedByOutsideAt < 600) {
            // 방금 바깥 터치(🌓 버튼 자신 포함)로 닫힌 것 → 그대로 닫힌 채로 둔다.
            opacityPanelClosedByOutsideAt = 0L
        } else {
            showOpacityPanel()
        }
    }

    @SuppressLint("InflateParams")
    /** 컨트롤 바, 과녁, 집결 패널에 같은 투명도를 적용한다. */
    private fun applyOverlayAlpha(a: Float) {
        controlView?.alpha = a
        targetView?.alpha = a
        rallyPanelHost?.applyAlpha(a)
        // 입력/설정 창은 너무 흐려서 조작 못 하는 일이 없도록 최소 40%까지만 흐려진다.
        val dialogA = a.coerceAtLeast(0.4f)
        settingsDialogView?.alpha = dialogA
        opacityPanelView?.alpha = dialogA
    }

    private fun dialogAlpha() = currentOverlayAlpha.coerceAtLeast(0.4f)

    fun showOpacityPanel() {
        if (opacityPanelView != null) {
            hideOpacityPanel()
            return
        }
        val wm = windowManager ?: return
        val control = controlView ?: return
        val cp = controlParams ?: return
        currentOverlayAlpha = PreferencesHelper.getOverlayAlpha(this)

        val themedContext = android.view.ContextThemeWrapper(this, R.style.Theme_AutoClicker)
        val inflater = LayoutInflater.from(themedContext)
        val view = inflater.inflate(R.layout.layout_floating_opacity_panel, null)
        this.opacityPanelView = view
        view.alpha = dialogAlpha()

        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        val panelWidth = dpToPx(52)
        val margin = dpToPx(6)
        val controlWidth = control.width.takeIf { it > 0 } ?: dpToPx(46)

        // 플로팅 바가 우측에 있으면 패널은 플로팅 바의 왼쪽에, 좌측에 있으면 오른쪽에 배치!
        val posX = if (cp.x + (controlWidth / 2) > screenWidth / 2) {
            (cp.x - panelWidth - margin).coerceAtLeast(dpToPx(4))
        } else {
            (cp.x + controlWidth + margin).coerceAtMost(screenWidth - panelWidth - dpToPx(4))
        }
        val posY = cp.y.coerceIn(dpToPx(40), screenHeight - dpToPx(240))

        val params = WindowManager.LayoutParams(
            panelWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = posX
            y = posY
        }
        this.opacityPanelParams = params

        // 윈도우 생성 직후 발생할 수 있는 가짜 ACTION_OUTSIDE 방지
        val panelCreationTime = System.currentTimeMillis()
        
        // 다른 곳(외부) 클릭 시 투명도 레이어창 닫기!
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                if (System.currentTimeMillis() - panelCreationTime > 200) {
                    // 🌓 버튼을 다시 눌러 닫는 경우: 바깥 터치(DOWN)로 먼저 닫히고 곧이어 버튼 클릭이
                    // 들어오므로, 그 클릭이 패널을 다시 열지 않게 시각을 기록해 둔다.
                    opacityPanelClosedByOutsideAt = System.currentTimeMillis()
                    hideOpacityPanel()
                }
                true
            } else {
                false
            }
        }

        val btn100 = view.findViewById<TextView>(R.id.btnLevel100)
        val btn80 = view.findViewById<TextView>(R.id.btnLevel80)
        val btn60 = view.findViewById<TextView>(R.id.btnLevel60)
        val btn40 = view.findViewById<TextView>(R.id.btnLevel40)
        val btn20 = view.findViewById<TextView>(R.id.btnLevel20)

        val levels = listOf(
            Pair(btn100, 1.0f),
            Pair(btn80, 0.8f),
            Pair(btn60, 0.6f),
            Pair(btn40, 0.4f),
            Pair(btn20, 0.2f)
        )

        fun updateLevelHighlight(selectedAlpha: Float) {
            levels.forEach { (btn, a) ->
                if (Math.abs(a - selectedAlpha) < 0.11f) {
                    btn.setBackgroundResource(R.drawable.bg_chip_selected)
                    btn.setTextColor(Color.WHITE)
                } else {
                    btn.setBackgroundResource(R.drawable.bg_chip_normal)
                    btn.setTextColor(Color.parseColor("#E2E8F0"))
                }
            }
        }

        updateLevelHighlight(currentOverlayAlpha)

        levels.forEach { (btn, a) ->
            btn.setOnClickListener {
                vibrate(20)
                currentOverlayAlpha = a
                applyOverlayAlpha(a)
                PreferencesHelper.setOverlayAlpha(this, a)
                updateLevelHighlight(a)
                showToast("🌓 투명도 ${(a * 100).toInt()}% 적용")
            }
        }

        wm.addView(view, params)
    }

    fun hideSettingsDialog() {
        settingsDialogView?.let {
            if (it.isAttachedToWindow) {
                try { windowManager?.removeView(it) } catch (e: Exception) { Log.e(TAG, "Error removing settings dialog", e) }
            }
        }
        settingsDialogView = null
        settingsDialogParams = null
    }

    @SuppressLint("InflateParams")
    fun showSettingsDialog() {
        if (settingsDialogView != null) {
            hideSettingsDialog()
            return
        }
        val wm = windowManager ?: return
        val themedContext = android.view.ContextThemeWrapper(this, R.style.Theme_AutoClicker)
        val inflater = LayoutInflater.from(themedContext)
        val view = inflater.inflate(R.layout.layout_dialog_floating_settings, null)
        this.settingsDialogView = view
        view.alpha = dialogAlpha()

        // 명시적으로 320dp 폭 지정하여 찌그러짐 완벽 방지!
        val params = WindowManager.LayoutParams(
            dpToPx(320),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        this.settingsDialogParams = params

        val btnClose = view.findViewById<ImageButton>(R.id.btnDialogClose)
        val btnSave = view.findViewById<Button>(R.id.btnDialogSave)
        val etInterval = view.findViewById<EditText>(R.id.dialogEtInterval)

        val chip100 = view.findViewById<TextView>(R.id.dialogChip100)
        val chip200 = view.findViewById<TextView>(R.id.dialogChip200)
        val chip500 = view.findViewById<TextView>(R.id.dialogChip500)
        val chip1000 = view.findViewById<TextView>(R.id.dialogChip1000)

        val tabInfinite = view.findViewById<TextView>(R.id.dialogTabInfinite)
        val tabCount = view.findViewById<TextView>(R.id.dialogTabCount)
        val tabTimer = view.findViewById<TextView>(R.id.dialogTabTimer)

        val tvInfiniteDesc = view.findViewById<TextView>(R.id.dialogTvInfiniteDesc)
        val layoutCount = view.findViewById<View>(R.id.dialogLayoutCountInput)
        val layoutTimer = view.findViewById<View>(R.id.dialogLayoutTimerInput)

        val etCount = view.findViewById<EditText>(R.id.dialogEtCount)
        val etMin = view.findViewById<EditText>(R.id.dialogEtTimerMin)
        val etSec = view.findViewById<EditText>(R.id.dialogEtTimerSec)

        val chipCount50 = view.findViewById<TextView>(R.id.dialogChipCount50)
        val chipCount100 = view.findViewById<TextView>(R.id.dialogChipCount100)
        val chipCount300 = view.findViewById<TextView>(R.id.dialogChipCount300)

        val chipTimer1m = view.findViewById<TextView>(R.id.dialogChipTimer1m)
        val chipTimer3m = view.findViewById<TextView>(R.id.dialogChipTimer3m)

        val tvAlphaPercent = view.findViewById<TextView>(R.id.dialogTvAlphaPercent)
        val chipAlpha100 = view.findViewById<TextView>(R.id.dialogChipAlpha100)
        val chipAlpha80 = view.findViewById<TextView>(R.id.dialogChipAlpha80)
        val chipAlpha60 = view.findViewById<TextView>(R.id.dialogChipAlpha60)
        val chipAlpha40 = view.findViewById<TextView>(R.id.dialogChipAlpha40)

        var selectedMode = PreferencesHelper.getRepeatMode(this)
        var selectedAlpha = currentOverlayAlpha

        fun updateTabs(mode: RepeatMode) {
            selectedMode = mode
            val selBg = R.drawable.bg_chip_selected
            val norBg = R.drawable.bg_chip_normal
            val white = Color.WHITE
            val crispLight = Color.parseColor("#E2E8F0")

            tabInfinite.setBackgroundResource(if (mode == RepeatMode.INFINITE) selBg else norBg)
            tabInfinite.setTextColor(if (mode == RepeatMode.INFINITE) white else crispLight)

            tabCount.setBackgroundResource(if (mode == RepeatMode.COUNT) selBg else norBg)
            tabCount.setTextColor(if (mode == RepeatMode.COUNT) white else crispLight)

            tabTimer.setBackgroundResource(if (mode == RepeatMode.TIMER) selBg else norBg)
            tabTimer.setTextColor(if (mode == RepeatMode.TIMER) white else crispLight)

            tvInfiniteDesc.visibility = if (mode == RepeatMode.INFINITE) View.VISIBLE else View.GONE
            layoutCount.visibility = if (mode == RepeatMode.COUNT) View.VISIBLE else View.GONE
            layoutTimer.visibility = if (mode == RepeatMode.TIMER) View.VISIBLE else View.GONE
        }

        fun updateSpeedChips(selectedMs: Long) {
            val chips = listOf(
                Pair(chip100, 100L),
                Pair(chip200, 200L),
                Pair(chip500, 500L),
                Pair(chip1000, 1000L)
            )
            chips.forEach { (chip, ms) ->
                if (ms == selectedMs) {
                    chip.setBackgroundResource(R.drawable.bg_chip_selected)
                    chip.setTextColor(Color.WHITE)
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip_normal)
                    chip.setTextColor(Color.parseColor("#E2E8F0"))
                }
            }
        }

        fun updateAlphaChips(alpha: Float) {
            selectedAlpha = alpha
            tvAlphaPercent.text = "${(alpha * 100).toInt()}%"
            val chips = listOf(
                Pair(chipAlpha100, 1.0f),
                Pair(chipAlpha80, 0.8f),
                Pair(chipAlpha60, 0.6f),
                Pair(chipAlpha40, 0.4f)
            )
            chips.forEach { (chip, a) ->
                if (Math.abs(a - alpha) < 0.11f) {
                    chip.setBackgroundResource(R.drawable.bg_chip_selected)
                    chip.setTextColor(Color.WHITE)
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip_normal)
                    chip.setTextColor(Color.parseColor("#E2E8F0"))
                }
            }
        }

        // 현재 설정값 로드
        val curInterval = PreferencesHelper.getIntervalMs(this)
        etInterval.setText(curInterval.toString())
        updateSpeedChips(curInterval)

        updateTabs(selectedMode)
        updateAlphaChips(selectedAlpha)

        etCount.setText(PreferencesHelper.getRepeatCount(this).toString())
        val dur = PreferencesHelper.getRepeatDurationSec(this)
        etMin.setText((dur / 60).toString())
        etSec.setText((dur % 60).toString())

        // 탭 클릭 리스너
        tabInfinite.setOnClickListener { updateTabs(RepeatMode.INFINITE) }
        tabCount.setOnClickListener { updateTabs(RepeatMode.COUNT) }
        tabTimer.setOnClickListener { updateTabs(RepeatMode.TIMER) }

        // 속도 프리셋 칩 클릭
        chip100.setOnClickListener { etInterval.setText("100"); updateSpeedChips(100L) }
        chip200.setOnClickListener { etInterval.setText("200"); updateSpeedChips(200L) }
        chip500.setOnClickListener { etInterval.setText("500"); updateSpeedChips(500L) }
        chip1000.setOnClickListener { etInterval.setText("1000"); updateSpeedChips(1000L) }

        // 횟수 칩 클릭
        chipCount50.setOnClickListener { etCount.setText("50") }
        chipCount100.setOnClickListener { etCount.setText("100") }
        chipCount300.setOnClickListener { etCount.setText("300") }

        // 시간 칩 클릭
        chipTimer1m.setOnClickListener { etMin.setText("1"); etSec.setText("0") }
        chipTimer3m.setOnClickListener { etMin.setText("3"); etSec.setText("0") }

        // 투명도 프리셋 칩 클릭
        chipAlpha100.setOnClickListener {
            updateAlphaChips(1.0f)
            applyOverlayAlpha(1.0f)
        }
        chipAlpha80.setOnClickListener {
            updateAlphaChips(0.8f)
            applyOverlayAlpha(0.8f)
        }
        chipAlpha60.setOnClickListener {
            updateAlphaChips(0.6f)
            applyOverlayAlpha(0.6f)
        }
        chipAlpha40.setOnClickListener {
            updateAlphaChips(0.4f)
            applyOverlayAlpha(0.4f)
        }

        btnClose.setOnClickListener {
            vibrate(15)
            // 취소 시 기존 투명도 복원
            applyOverlayAlpha(currentOverlayAlpha)
            hideSettingsDialog()
        }

        btnSave.setOnClickListener {
            vibrate(25)
            val newInterval = etInterval.text.toString().toLongOrNull() ?: 500L
            PreferencesHelper.setIntervalMs(this, newInterval)
            currentIntervalMs = newInterval

            PreferencesHelper.setRepeatMode(this, selectedMode)

            val newCount = etCount.text.toString().toIntOrNull() ?: 100
            PreferencesHelper.setRepeatCount(this, newCount)

            val m = etMin.text.toString().toIntOrNull() ?: 1
            val s = etSec.text.toString().toIntOrNull() ?: 0
            PreferencesHelper.setRepeatDurationSec(this, (m * 60) + s)

            currentOverlayAlpha = selectedAlpha
            PreferencesHelper.setOverlayAlpha(this, selectedAlpha)
            applyOverlayAlpha(selectedAlpha)

            showToast("⚙️ 설정 적용 완료! (${newInterval}ms / ${when(selectedMode) {
                RepeatMode.INFINITE -> "무한"
                RepeatMode.COUNT -> "${newCount}회"
                RepeatMode.TIMER -> "${m}분${s}초"
            }} / 투명도 ${(selectedAlpha * 100).toInt()}%)")
            hideSettingsDialog()
        }

        wm.addView(view, params)
    }

    fun toggleAutoClick() {
        // 정지는 지연/쿨다운 없이 0.001초 만에 즉각 실행!
        if (isClicking) {
            stopAutoClick()
            vibrate(30)
            showToast("⏹ 치료 연타 정지됨")
            return
        }

        // 시작할 때만 더블 클릭 방지
        val now = SystemClock.elapsedRealtime()
        if (now - lastToggleTime < 250L) {
            return
        }
        lastToggleTime = now

        val target = targetView
        if (target == null) {
            showToast("🎯 조준점이 준비되지 않았습니다.")
            return
        }

        // 절대 물리 화면 좌표(상태 표시줄 포함)를 정확히 가져옴
        val location = IntArray(2)
        target.getLocationOnScreen(location)
        
        val centerX = location[0] + target.width / 2f
        val centerY = location[1] + target.height / 2f

        currentIntervalMs = PreferencesHelper.getIntervalMs(this)
        startAutoClick(centerX, centerY, currentIntervalMs)
        vibrate(50)
    }

    private fun setTargetTouchable(touchable: Boolean) {
        val target = targetView ?: return
        val params = targetParams ?: return
        val wm = windowManager ?: return

        val oldFlags = params.flags
        params.flags = if (touchable) {
            params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        if (oldFlags != params.flags && target.isAttachedToWindow) {
            try {
                wm.updateViewLayout(target, params)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update target touchable state", e)
            }
        }
    }

    private fun updateControlState(isClickingState: Boolean) {
        val control = controlView ?: return
        val btnPlay = control.findViewById<ImageButton>(R.id.btnPlayPause) ?: return

        if (isClickingState) {
            btnPlay.setImageResource(R.drawable.ic_pause)
            btnPlay.setColorFilter(Color.parseColor("#EF4444")) // 정지 상태: 빨간색 일시정지 아이콘
        } else {
            btnPlay.setImageResource(R.drawable.ic_play)
            btnPlay.setColorFilter(Color.parseColor("#22C55E")) // 대기 상태: 초록색 재생 아이콘
        }
        updateNotification()
    }

    fun startAutoClick(x: Float, y: Float, intervalMs: Long) {
        if (isClicking) {
            stopAutoClick()
        }

        isClicking = true
        mainHandler.post {
            setTargetTouchable(false) // 연타 중에는 과녁이 터치를 가로채지 않도록 통과 설정!
            updateControlState(true)
            onStatusChanged?.invoke(true)
        }

        val repeatMode = PreferencesHelper.getRepeatMode(this)
        val targetCount = PreferencesHelper.getRepeatCount(this)
        val targetDurationSec = PreferencesHelper.getRepeatDurationSec(this)

        val modeDesc = when (repeatMode) {
            RepeatMode.INFINITE -> "무한 반복"
            RepeatMode.COUNT -> "${targetCount}회 반복"
            RepeatMode.TIMER -> "${targetDurationSec / 60}분 ${targetDurationSec % 60}초 타이머"
        }
        showToast("▶ 연타 시작! ($modeDesc)")
        Log.d(TAG, "Starting robust infinite auto click at ($x, $y) with $intervalMs ms and $modeDesc")

        clickJob = serviceScope.launch {
            val pulseRunnable = Runnable {
                (targetView as? TargetCrosshairView)?.pulse()
                onClickExecuted?.invoke()
            }

            var clickCount = 0
            val startRealtime = SystemClock.elapsedRealtime()

            // 한 번만 클릭되고 멈추는 현상을 100% 방지하는 신뢰성 높은 주기적 무한 연타 엔진
            while (isActive && isClicking) {
                try {
                    val clickPath = Path().apply {
                        moveTo(x, y)
                    }
                    val stroke = GestureDescription.StrokeDescription(clickPath, 0L, 35L)
                    val gesture = GestureDescription.Builder().addStroke(stroke).build()

                    // 콜백 대기로 인한 정지/지연을 원천 차단하는 비동기 디스패치
                    dispatchGesture(gesture, null, null)

                    mainHandler.post(pulseRunnable)
                } catch (e: Exception) {
                    Log.e(TAG, "Error dispatching gesture", e)
                }

                if (!isActive || !isClicking) break

                clickCount++

                // 1) 횟수 지정 모드 체크
                if (repeatMode == RepeatMode.COUNT && clickCount >= targetCount) {
                    mainHandler.post {
                        stopAutoClick()
                        showToast("✅ 설정한 ${targetCount}회 클릭 완료 후 자동 정지되었습니다.")
                    }
                    break
                }

                // 2) 타이머 지정 모드 체크
                if (repeatMode == RepeatMode.TIMER) {
                    val elapsedSec = (SystemClock.elapsedRealtime() - startRealtime) / 1000L
                    if (elapsedSec >= targetDurationSec) {
                        mainHandler.post {
                            stopAutoClick()
                            val m = targetDurationSec / 60
                            val s = targetDurationSec % 60
                            showToast("⏰ 설정한 타이머(${m}분 ${s}초) 완료 후 자동 정지되었습니다.")
                        }
                        break
                    }
                }

                delay(intervalMs.coerceAtLeast(50L))
            }
        }
    }

    fun stopAutoClick() {
        isClicking = false
        clickJob?.cancel()
        clickJob = null
        mainHandler.post {
            setTargetTouchable(true) // 정지되면 다시 과녁을 손으로 드래그할 수 있도록 복구!
            updateControlState(false)
            onStatusChanged?.invoke(false)
        }
        Log.d(TAG, "Stopped auto click.")
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "오토클리커 컨트롤러",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "오토클리커 실행 및 비상 정지 리모컨"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val stopIntent = PendingIntent.getBroadcast(
            this, 1,
            Intent(ACTION_STOP_CLICK),
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val closeIntent = PendingIntent.getBroadcast(
            this, 2,
            Intent(ACTION_CLOSE_ALL),
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val title = when {
            isClicking -> "▶ 치료 연타 실행 중 (클릭중)"
            else -> "⏹ 오토클리커 대기 중"
        }
        val text = when {
            isClicking -> "정지하려면 아래 [연타 정지]를 누르세요."
            else -> "게임 화면의 컨트롤러로 조작하세요."
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_target)
            .setContentIntent(openIntent)
            .setOngoing(true)

        if (isClicking) {
            builder.addAction(R.drawable.ic_pause, "⏹ 연타 정지", stopIntent)
        }
        builder.addAction(R.drawable.ic_close, "✕ 위젯 종료", closeIntent)

        manager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun cancelNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }

        // --- Bear Hunter Mode Restored UI ---
    private var bearSetupView: View? = null
    private var bearSetupParams: WindowManager.LayoutParams? = null
    // 쓸 부대 표시(번호별). 개수만큼 만들어 각자 깃발 위에 놓는다.
    private val troopMarkers = ArrayList<android.widget.TextView>()
    private val troopMarkerParams = ArrayList<WindowManager.LayoutParams>()
    private var troopCountSetting = 1
    private var troopCountLabel: android.widget.TextView? = null

    private var hunterFireView: View? = null

    /** 곰 사냥 발사. 과녁이 출정 버튼 위에 떠 있어서, 투과시키지 않으면 클릭을 과녁이 받아 게임에 닿지 않는다. */
    private fun fireHunter(repeatCount: Int) {
        val count = HunterModeManager.troops.size
        val firedIdx = HunterTroops.current(HunterModeManager.nextTroop, count)
        val fired = HunterModeManager.fire(this, repeatCount,
            beforeTap = { setTargetTouchable(false) },
            afterTap = { setTargetTouchable(true) })
        if (fired && count > 1) showFiredOnButton(firedIdx)
    }

    private val hunterFireColor = "#E610B981"
    private var hunterLabelReset: Runnable? = null

    /** 발사 버튼의 글자를 지금 상태에 맞춘다: 부대가 여럿이면 다음에 나갈 번호. */
    private fun updateHunterFireLabel() {
        val v = hunterFireView as? android.widget.TextView ?: return
        hunterLabelReset?.let { mainHandler.removeCallbacks(it) }
        hunterLabelReset = null
        v.text = HunterTroops.fireLabel(HunterModeManager.nextTroop, HunterModeManager.troops.size)
        (v.background as? android.graphics.drawable.GradientDrawable)?.setColor(Color.parseColor(hunterFireColor))
    }

    /** 누른 직후 1초쯤: 방금 나간 부대 번호를 다른 색으로 보여 주고, 그 뒤 다음 번호로 돌아간다. */
    private fun showFiredOnButton(firedIdx: Int) {
        val v = hunterFireView as? android.widget.TextView ?: return
        hunterLabelReset?.let { mainHandler.removeCallbacks(it) }
        v.text = HunterTroops.firedLabel(firedIdx)
        (v.background as? android.graphics.drawable.GradientDrawable)?.setColor(Color.parseColor("#E6F59E0B"))
        val reset = Runnable { hunterLabelReset = null; updateHunterFireLabel() }
        hunterLabelReset = reset
        mainHandler.postDelayed(reset, 1000L)
    }

    // --- 초 단위 시계 (플로팅 바의 시계 버튼으로 켜고 끈다) ---
    private var clockView: android.widget.TextView? = null
    private var clockUtc = false
    private val clockTick = object : Runnable {
        override fun run() {
            val v = clockView ?: return
            val now = System.currentTimeMillis()
            v.text = ClockText.format(now, java.util.TimeZone.getDefault(), clockUtc)
            mainHandler.postDelayed(this, ClockText.delayToNextSecond(now))
        }
    }

    private fun toggleClock() {
        if (clockView != null) hideClock() else showClock()
        controlView?.findViewById<ImageButton>(R.id.btnClock)
            ?.setColorFilter(Color.parseColor(if (clockView != null) "#38BDF8" else "#CBD5E1"))
    }

    /** 시계 창: 끌어서 옮기고, 눌러서 내 폰 시각 ↔ UTC(게임 화면의 시각)를 바꾼다. */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun showClock() {
        if (clockView != null) return
        val wm = windowManager ?: return
        val dp = resources.displayMetrics.density
        val v = android.widget.TextView(this).apply {
            textSize = 18f
            typeface = android.graphics.Typeface.MONOSPACE
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setPadding((12 * dp).toInt(), (6 * dp).toInt(), (12 * dp).toInt(), (6 * dp).toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#E60F172A")); cornerRadius = 12 * dp
                setStroke((1 * dp).toInt(), Color.parseColor("#3338BDF8"))
            }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = android.view.Gravity.TOP or android.view.Gravity.START; x = (90 * dp).toInt(); y = (40 * dp).toInt() }
        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var moved = false
        v.setOnTouchListener { _, e ->
            when (e.action) {
                android.view.MotionEvent.ACTION_DOWN -> { sx = lp.x; sy = lp.y; tx = e.rawX; ty = e.rawY; moved = false }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - tx; val dy = e.rawY - ty
                    if (moved || Math.abs(dx) > 12 * dp || Math.abs(dy) > 12 * dp) {
                        moved = true
                        lp.x = sx + dx.toInt(); lp.y = sy + dy.toInt()
                        try { wm.updateViewLayout(v, lp) } catch (_: Exception) { }
                    }
                }
                android.view.MotionEvent.ACTION_UP -> if (!moved) {
                    clockUtc = !clockUtc
                    v.text = ClockText.format(System.currentTimeMillis(), java.util.TimeZone.getDefault(), clockUtc)
                }
            }
            true
        }
        try {
            wm.addView(v, lp); clockView = v
            mainHandler.removeCallbacks(clockTick); clockTick.run()
        } catch (e: Exception) { Log.w(TAG, "시계 추가 실패", e) }
    }

    private fun hideClock() {
        mainHandler.removeCallbacks(clockTick)
        clockView?.let { try { windowManager?.removeView(it) } catch (_: Exception) { } }
        clockView = null
    }

    /** 곰 사냥 화면 발사 버튼(발사는 이 버튼으로만 한다). 눌러서 발사, 끌어서 이동한다. */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun showHunterFireButton() {
        if (hunterFireView != null) return
        val wm = windowManager ?: return
        val dp = resources.displayMetrics.density
        val v = android.widget.TextView(this).apply {
            text = HunterTroops.fireLabel(HunterModeManager.nextTroop, HunterModeManager.troops.size)
            textSize = 11f
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.parseColor("#E610B981"))
            }
        }
        val size = (56 * dp).toInt()
        val lp = WindowManager.LayoutParams(
            size, size, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = android.view.Gravity.TOP or android.view.Gravity.START; x = (12 * dp).toInt(); y = (220 * dp).toInt() }
        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var moved = false
        v.setOnTouchListener { _, e ->
            when (e.action) {
                android.view.MotionEvent.ACTION_DOWN -> { sx = lp.x; sy = lp.y; tx = e.rawX; ty = e.rawY; moved = false }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - tx; val dy = e.rawY - ty
                    if (moved || Math.abs(dx) > 12 * dp || Math.abs(dy) > 12 * dp) {
                        moved = true
                        lp.x = sx + dx.toInt(); lp.y = sy + dy.toInt()
                        try { wm.updateViewLayout(v, lp) } catch (_: Exception) { }
                    }
                }
                android.view.MotionEvent.ACTION_UP -> if (!moved) { vibrate(30); fireHunter(0) }
            }
            true
        }
        try { wm.addView(v, lp); hunterFireView = v } catch (e: Exception) { Log.w(TAG, "헌터 발사 버튼 추가 실패", e) }
    }

    private fun hideHunterFireButton() {
        hunterLabelReset?.let { mainHandler.removeCallbacks(it) }
        hunterLabelReset = null
        hunterFireView?.let { try { windowManager?.removeView(it) } catch (_: Exception) { } }
        hunterFireView = null
    }

    private fun toggleBearMode() {
        HunterModeManager.isHunterModeEnabled = !HunterModeManager.isHunterModeEnabled
        val btnBearMode = controlView?.findViewById<android.widget.ImageButton>(R.id.btnBearMode)
        if (HunterModeManager.isHunterModeEnabled) {
            HunterModeManager.resetSequence() // 켤 때마다 1번 부대부터
            btnBearMode?.setColorFilter(android.graphics.Color.parseColor("#10B981")) // 초록: 켜짐
            showHunterFireButton()
            if (HunterModeManager.hasTargets) {
                showToast("🐻 헌터 모드 켜짐 · 집결을 고른 뒤 🐻 발사 버튼을 누르세요 (위치를 다시 잡으려면 🐻 길게 누르기)")
            } else {
                showToast("🐻 먼저 쓸 부대와 출정 버튼 위치를 잡아 저장해 주세요")
                showBearSetupUi()
            }
        } else {
            showToast("🐻 헌터 모드 종료!")
            btnBearMode?.setColorFilter(android.graphics.Color.parseColor("#F59E0B")) // 노랑: 꺼짐
            hideBearSetupUi()
            hideHunterFireButton()
        }
    }

    /** 화면 위에 떠 있는 뷰의 중심을 화면 픽셀 좌표로 구한다. */
    private fun centerOnScreen(v: View?): Pair<Int, Int>? {
        v ?: return null
        if (v.width == 0 || v.height == 0) return null
        val loc = IntArray(2)
        v.getLocationOnScreen(loc)
        return Pair(loc[0] + v.width / 2, loc[1] + v.height / 2)
    }

    private fun showBearSetupUi() {
        troopCountSetting = HunterTroops.clampCount(HunterModeManager.troops.size)
        if (bearSetupView == null) {
            val ctx = this
            bearSetupView = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#E60F172A"))
                setPadding(24, 16, 24, 16)

                addView(android.widget.TextView(ctx).apply {
                    text = "과녁 → 출정 버튼 위에\n동그라미 1, 2, 3… → 쓸 부대 깃발 위에 번호 순서대로 놓고 저장\n(누를 때마다 1번부터 차례로 출정)"
                    setTextColor(Color.WHITE)
                    textSize = 12f
                    gravity = android.view.Gravity.CENTER
                })
                addView(android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER
                    fun stepButton(label: String, delta: Int) = android.widget.Button(ctx).apply {
                        text = label
                        setBackgroundColor(Color.parseColor("#334155"))
                        setTextColor(Color.WHITE)
                        setOnClickListener {
                            troopCountSetting = HunterTroops.clampCount(troopCountSetting + delta)
                            syncTroopMarkers()
                        }
                    }
                    addView(stepButton("−", -1))
                    val countText = android.widget.TextView(ctx).apply {
                        setTextColor(Color.WHITE)
                        textSize = 14f
                        gravity = android.view.Gravity.CENTER
                        setPadding(24, 0, 24, 0)
                    }
                    troopCountLabel = countText
                    addView(countText)
                    addView(stepButton("+", 1))
                })
                addView(android.widget.Button(ctx).apply {
                    text = "헌터 위치 저장"
                    setBackgroundColor(Color.parseColor("#3B82F6"))
                    setTextColor(Color.WHITE)
                    setOnClickListener {
                        val points = troopMarkers.map { centerOnScreen(it) }
                        val dispatch = centerOnScreen(targetView)
                        if (points.isEmpty() || points.any { it == null } || dispatch == null) {
                            showToast("⚠️ 위치를 읽지 못했어요. 과녁과 부대 표시가 보이는지 확인하세요")
                            return@setOnClickListener
                        }
                        HunterModeManager.troops = points.map { TroopPoint(it!!.first, it.second) }
                        HunterModeManager.dispatchX = dispatch.first
                        HunterModeManager.dispatchY = dispatch.second
                        HunterModeManager.saveSettings(ctx)
                        hideBearSetupUi()
                        updateHunterFireLabel() // 부대 수가 바뀌었을 수 있다
                    }
                })
            }

            bearSetupParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                y = 200
            }
        }

        try { windowManager?.addView(bearSetupView, bearSetupParams) } catch (e: Exception) {}

        syncTroopMarkers()

        if (!isTargetVisible) {
            toggleTargetVisibility()
        }
        // 과녁도 저장해 둔 출정 위치에서 시작한다. 닫을 때 원래 자리로 되돌린다(과녁은 집결·연타와 같이 쓴다).
        val tv = targetView
        val tp = targetParams
        if (tv != null && tp != null && bearTargetBackup == null && (HunterModeManager.dispatchX != 0 || HunterModeManager.dispatchY != 0)) {
            bearTargetBackup = Pair(tp.x, tp.y)
            placeCenterAt(tv, tp, HunterModeManager.dispatchX, HunterModeManager.dispatchY)
        }
    }

    /** 위치 잡는 동안 옮겨 둔 과녁의 원래 자리. 위치 잡는 화면이 닫혀 있으면 null. */
    private var bearTargetBackup: Pair<Int, Int>? = null

    /** 떠 있는 표시의 가운데가 화면 좌표 ([cx], [cy])에 오도록 옮긴다. 아직 그려지지 않았으면 잠깐 뒤에 다시 해 본다. */
    private fun placeCenterAt(view: View, params: WindowManager.LayoutParams, cx: Int, cy: Int, triesLeft: Int = 5) {
        view.post {
            if (view.width == 0 || view.height == 0) {
                if (triesLeft > 0) mainHandler.postDelayed({ placeCenterAt(view, params, cx, cy, triesLeft - 1) }, 50L)
                return@post
            }
            val loc = IntArray(2)
            view.getLocationOnScreen(loc)
            params.x = HunterTroops.topLeftFor(cx, view.width, loc[0] - params.x)
            params.y = HunterTroops.topLeftFor(cy, view.height, loc[1] - params.y)
            try { windowManager?.updateViewLayout(view, params) } catch (e: Exception) {}
        }
    }

    /** 부대 표시를 [troopCountSetting]개에 맞춘다. 이미 있는 표시는 위치를 그대로 둔다. */
    private fun syncTroopMarkers() {
        val wm = windowManager ?: return
        val dp = resources.displayMetrics.density
        while (troopMarkers.size > troopCountSetting) {
            val i = troopMarkers.size - 1
            try { wm.removeView(troopMarkers[i]) } catch (e: Exception) {}
            troopMarkers.removeAt(i)
            troopMarkerParams.removeAt(i)
        }
        while (troopMarkers.size < troopCountSetting) {
            val i = troopMarkers.size
            // 게임 속 깃발 아이콘보다 작아야 서로 겹치지 않는다. 가운데 점이 눌릴 위치다.
            val size = (30 * dp).toInt()
            val marker = android.widget.TextView(this).apply {
                text = "${i + 1}"
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = android.view.Gravity.CENTER
                setTextColor(Color.WHITE)
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Color.parseColor("#CC10B981"))
                    setStroke((2 * dp).toInt(), Color.WHITE)
                }
            }
            val lp = WindowManager.LayoutParams(
                size,
                size,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                x = (40 * dp).toInt()
                y = ((100 + i * 36) * dp).toInt()
            }
            setupDrag(marker, lp)
            try { wm.addView(marker, lp) } catch (e: Exception) {}
            troopMarkers.add(marker)
            troopMarkerParams.add(lp)
            // 저장해 둔 부대 위치가 있으면 그 자리에서 시작한다(다시 열 때마다 기본 자리로 돌아가 보이던 문제).
            HunterModeManager.troops.getOrNull(i)?.let { saved -> placeCenterAt(marker, lp, saved.x, saved.y) }
        }
        troopCountLabel?.text = "부대 ${troopCountSetting}개"
    }

    private fun hideBearSetupUi() {
        try { windowManager?.removeView(bearSetupView) } catch (e: Exception) {}
        troopMarkers.forEach { try { windowManager?.removeView(it) } catch (e: Exception) {} }
        troopMarkers.clear()
        troopMarkerParams.clear()
        // 위치를 잡느라 옮겼던 과녁을 원래 자리로 되돌린다.
        bearTargetBackup?.let { (x, y) ->
            bearTargetBackup = null
            val tv = targetView
            val tp = targetParams
            if (tv != null && tp != null) {
                tp.x = x; tp.y = y
                try { windowManager?.updateViewLayout(tv, tp) } catch (e: Exception) {}
            }
        }
    }

    private fun setupDrag(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        view.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }
    // --- End Bear Hunter Mode Restored UI ---

    private fun showToast(msg: String) {
        mainHandler.post {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

}


















