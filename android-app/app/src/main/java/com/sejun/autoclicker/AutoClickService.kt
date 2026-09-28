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

    private var rallyDialogView: View? = null
    private var rallyDialogParams: WindowManager.LayoutParams? = null
    private var rallyDialogTimerRunnable: Runnable? = null

    private var rallyHudView: View? = null
    private var rallyHudParams: WindowManager.LayoutParams? = null

    private var rallyJob: Job? = null
    var isRallyReserved: Boolean = false
        private set
    var reservedGroupName: String = ""
        private set

    private var currentIntervalMs: Long = 500L
    private var lastToggleTime: Long = 0L
    private var currentOverlayAlpha: Float = 1.0f
    
    private var autoStartWatcherJob: Job? = null
    
    private val myDeviceId = java.util.UUID.randomUUID().toString()
    private val firebaseDbUrl = "https://autoclicker-cf5a4-default-rtdb.firebaseio.com"
    private var syncPollingThread: Thread? = null
    private var isPolling = false

    var onStatusChanged: ((Boolean) -> Unit)? = null
    var onClickExecuted: (() -> Unit)? = null
    var onOverlaysVisibilityChanged: ((Boolean) -> Unit)? = null

    // Broadcast receiver for notification action buttons & screen off
    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_STOP_CLICK -> {
                    if (isRallyReserved) {
                        cancelRallyReservation()
                        showToast("✕ 알림창에서 출발 예약을 취소했습니다.")
                    } else {
                        stopAutoClick()
                        showToast("⏹ 알림창에서 연타를 정지했습니다.")
                    }
                }
                ACTION_CLOSE_ALL -> {
                    hideOverlays()
                    showToast("✕ 오토클리커를 완전히 종료했습니다.")
                }
                Intent.ACTION_SCREEN_OFF -> {
                    stopAutoClick()
                    cancelRallyReservation()
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
     * 물리 볼륨 버튼(볼륨 업 또는 다운)을 누르면 즉시 긴급 정지!
     */
    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN) {
            val code = event.keyCode
            if (code == KeyEvent.KEYCODE_VOLUME_DOWN || code == KeyEvent.KEYCODE_VOLUME_UP) {
                if (isRallyReserved) {
                    cancelRallyReservation()
                    vibrate(60)
                    showToast("🛑 볼륨 키로 출발 예약을 취소했습니다!")
                    return true
                }
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
        stopAutoClick()
        cancelRallyReservation()
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
        currentIntervalMs = intervalMs ?: PreferencesHelper.getIntervalMs(this)
        currentOverlayAlpha = PreferencesHelper.getOverlayAlpha(this)
        val wm = windowManager ?: getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        // 기존 뷰가 있다면 리셋
        hideOverlays()

        createTargetView(wm)
        createControlView(wm)
        updateNotification()
        onOverlaysVisibilityChanged?.invoke(true)
        
        startCloudSyncPolling()
        startAutoRallyWatcher()
    }

    private fun startAutoRallyWatcher() {
        if (autoStartWatcherJob?.isActive == true) return
        autoStartWatcherJob = serviceScope.launch {
            while (isActive) {
                delay(500)
                if (isRallyReserved) continue
                
                val now = System.currentTimeMillis()
                val groups = RallyGroupManager.getGroups(this@AutoClickService)
                val selectedGroupId = RallyGroupManager.getSelectedGroupId(this@AutoClickService)
                // 🔥 오직 'isAutoMode == true'로 자동 선택된 군단(예: 1군, 2군)만 감지!
                // 3군, 4군처럼 집결장이 없거나 수동으로 둔 군단은 완전히 스킵됩니다.
                val imminentGroup = groups.firstOrNull { 
                    it.isAutoMode && !it.hasExecutedClick() && it.id == selectedGroupId && (it.calculateDepartureTimestamp() - now) in 200L..6500L
                } ?: groups.firstOrNull { 
                    it.isAutoMode && !it.hasExecutedClick() && (it.calculateDepartureTimestamp() - now) in 200L..6500L
                }
                
                if (imminentGroup != null) {
                    withContext(Dispatchers.Main) {
                        vibrate(100)
                        val savedPos = PreferencesHelper.getSavedRallyTargetPosition(this@AutoClickService)
                        if (savedPos == null) {
                            showToast("⚠️ [${imminentGroup.getDisplayName()}] 자동 대기 불가: 저장된 타겟 위치가 없습니다.\n과녁을 맞춘 후 [📍 타겟위치 저장]을 먼저 해주세요.")
                        } else {
                            moveTargetViewTo(savedPos.first, savedPos.second)
                            showToast("🚨 [${imminentGroup.getDisplayName()}] 출발 5초 전 자동 감지!\n최근 저장된 위치로 이동하여 예약을 시작합니다.")
                            startRallyReservation(imminentGroup, true)
                        }
                    }
                    delay(8000) // 예약 실행 후 중복 감지 방지 대기
                }
            }
        }
    }

    /**
     * 오토클리커 플로팅 위젯들을 화면에서 완전히 닫고 제거합니다.
     */
    fun hideOverlays() {
        autoStartWatcherJob?.cancel()
        autoStartWatcherJob = null
        stopCloudSyncPolling()
        stopAutoClick()
        cancelRallyReservation()
        hideSettingsDialog()
        hideRallyDialog()
        hideOpacityPanel()
        val wm = windowManager ?: return

        controlView?.let {
            if (it.isAttachedToWindow) {
                try { wm.removeView(it) } catch (e: Exception) { Log.e(TAG, "Error removing control", e) }
            }
        }
        targetView?.let {
            if (it.isAttachedToWindow) {
                try { wm.removeView(it) } catch (e: Exception) { Log.e(TAG, "Error removing target", e) }
            }
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
        val btnSettings = control.findViewById<ImageButton>(R.id.btnSettings)
        val btnOpacity = control.findViewById<ImageButton>(R.id.btnOpacity)
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
            showRallyDialog()
        }

        // 4. 인게임 실시간 설정 팝업 버튼 (⚙️)
        btnSettings?.setOnClickListener {
            vibrate(20)
            hideOpacityPanel()
            showSettingsDialog()
        }

        // 5. 세로 5단계 투명도 레벨 창 토글 (🌓) 버튼
        btnOpacity?.setOnClickListener {
            vibrate(20)
            toggleOpacityPanel()
        }

        // 5. 플로팅 컨트롤러 및 타겟 완전 종료 (✕)
        btnClose.setOnClickListener {
            vibrate(20)
            hideOverlays()
            showToast("✕ 오토클리커가 종료되었습니다.")
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
        btnOpacity?.setOnTouchListener(dragTouchListener)
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

    fun toggleOpacityPanel() {
        if (opacityPanelView != null) {
            hideOpacityPanel()
        } else {
            showOpacityPanel()
        }
    }

    @SuppressLint("InflateParams")
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
                control.alpha = a
                targetView?.alpha = a
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
            controlView?.alpha = 1.0f
            targetView?.alpha = 1.0f
        }
        chipAlpha80.setOnClickListener {
            updateAlphaChips(0.8f)
            controlView?.alpha = 0.8f
            targetView?.alpha = 0.8f
        }
        chipAlpha60.setOnClickListener {
            updateAlphaChips(0.6f)
            controlView?.alpha = 0.6f
            targetView?.alpha = 0.6f
        }
        chipAlpha40.setOnClickListener {
            updateAlphaChips(0.4f)
            controlView?.alpha = 0.4f
            targetView?.alpha = 0.4f
        }

        btnClose.setOnClickListener {
            vibrate(15)
            // 취소 시 기존 투명도 복원
            controlView?.alpha = currentOverlayAlpha
            targetView?.alpha = currentOverlayAlpha
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
            controlView?.alpha = selectedAlpha
            targetView?.alpha = selectedAlpha

            showToast("⚙️ 설정 적용 완료! (${newInterval}ms / ${when(selectedMode) {
                RepeatMode.INFINITE -> "무한"
                RepeatMode.COUNT -> "${newCount}회"
                RepeatMode.TIMER -> "${m}분${s}초"
            }} / 투명도 ${(selectedAlpha * 100).toInt()}%)")
            hideSettingsDialog()
        }

        wm.addView(view, params)
    }

    fun hideRallyDialog() {
        rallyDialogTimerRunnable?.let {
            mainHandler.removeCallbacks(it)
        }
        rallyDialogTimerRunnable = null
        rallyDialogView?.let {
            if (it.isAttachedToWindow) {
                try { windowManager?.removeView(it) } catch (e: Exception) { Log.e(TAG, "Error removing rally dialog", e) }
            }
        }
        rallyDialogView = null
        rallyDialogParams = null
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    fun showRallyDialog(skipTimeInit: Boolean = false) {
        if (rallyDialogView != null) {
            hideRallyDialog()
            return
        }
        val wm = windowManager ?: return
        val themedContext = android.view.ContextThemeWrapper(this, R.style.Theme_AutoClicker)
        val inflater = LayoutInflater.from(themedContext)
        val view = inflater.inflate(R.layout.layout_dialog_rally_sync, null)
        this.rallyDialogView = view

        val params = WindowManager.LayoutParams(
            dpToPx(310),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        this.rallyDialogParams = params

        val rallyDialogHeader = view.findViewById<View>(R.id.rallyDialogHeader)
        rallyDialogHeader.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchDownX = 0f
            private var touchDownY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val p = rallyDialogParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = p.x
                        startY = p.y
                        touchDownX = event.rawX
                        touchDownY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchDownX).toInt()
                        val dy = (event.rawY - touchDownY).toInt()
                        p.x = startX + dx
                        p.y = startY + dy
                        try {
                            wm.updateViewLayout(view, p)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error dragging rally dialog", e)
                        }
                        return true
                    }
                }
                return false
            }
        })

        val btnClose = view.findViewById<ImageButton>(R.id.btnRallyClose)
        val btnMinimize = view.findViewById<TextView>(R.id.btnRallyMinimize)
        val rallyDialogContent = view.findViewById<LinearLayout>(R.id.rallyDialogContent)
        val rallyDialogCompactContent = view.findViewById<LinearLayout>(R.id.rallyDialogCompactContent)
        val tvTitle = view.findViewById<TextView>(R.id.tvRallyDialogTitle)

        // ── 윈도우 포커스 동적 제어 (게임 화면 대화창 차단 방지) ─────────────
        fun setDialogFocusable(focusable: Boolean) {
            val p = rallyDialogParams ?: return
            val currentFocusable = (p.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) == 0
            if (currentFocusable != focusable) {
                if (focusable) {
                    p.flags = p.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                } else {
                    p.flags = p.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                }
                try {
                    wm.updateViewLayout(view, p)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating dialog focus", e)
                }
            }
        }

        val rallyDialogRoot = view.findViewById<LinearLayout>(R.id.rallyDialogRoot)

        // ── ─ 최소화 / 복원 상태 플래그 및 함수 ───────────────────────────
        var isMinimized = false
        var savedYBeforeMinimize: Int? = null
        var onUpdateCountdown: (() -> Unit)? = null

        fun minimizeDialog() {
            if (isMinimized) return
            isMinimized = true
            setDialogFocusable(false) // 최소화 시 포커스 해제
            rallyDialogContent.visibility = View.GONE
            rallyDialogCompactContent.visibility = View.VISIBLE
            btnMinimize.text = "▲"
            onUpdateCountdown?.invoke()

            // 화면 정중앙이 아닌 "상단 중앙"으로 팝업 위치 이동
            val p = rallyDialogParams
            if (p != null) {
                if (savedYBeforeMinimize == null) {
                    savedYBeforeMinimize = p.y
                }
                p.y = -dpToPx(180) // 중앙에서 상단 방향으로 이동
                try { wm.updateViewLayout(view, p) } catch (e: Exception) {}
            }
        }
        fun restoreDialog() {
            if (!isMinimized) return
            isMinimized = false
            rallyDialogContent.visibility = View.VISIBLE
            rallyDialogCompactContent.visibility = View.GONE
            btnMinimize.text = "—"
            tvTitle.text = "⚔️ 집결시간 설정"
            tvTitle.setTextColor(Color.parseColor("#FFFFFF"))
            onUpdateCountdown?.invoke()

            // 원래 위치(중앙)로 복귀
            val p = rallyDialogParams
            if (p != null) {
                p.y = savedYBeforeMinimize ?: 0
                savedYBeforeMinimize = null
                try { wm.updateViewLayout(view, p) } catch (e: Exception) {}
            }
        }

        btnMinimize.setOnClickListener {
            vibrate(20)
            if (isMinimized) restoreDialog() else minimizeDialog()
        }
        
        val btnRallyAlpha = view.findViewById<TextView>(R.id.btnRallyAlpha)
        var currentAlpha = PreferencesHelper.getRallyDialogAlpha(this)
        
        fun applyAlpha() {
            view.alpha = currentAlpha
            btnRallyAlpha.text = "${(currentAlpha * 100).toInt()}%"
        }
        
        // 초기 알파 적용
        applyAlpha()
        
        btnRallyAlpha.setOnClickListener {
            vibrate(15)
            currentAlpha = when (currentAlpha) {
                1.0f -> 0.8f
                0.8f -> 0.6f
                0.6f -> 0.4f
                else -> 1.0f
            }
            applyAlpha()
            PreferencesHelper.setRallyDialogAlpha(this@AutoClickService, currentAlpha)
        }

        // 최소화 상태에서 헤더 탭하면 복원
        view.findViewById<LinearLayout>(R.id.rallyDialogHeader).setOnClickListener {
            if (isMinimized) { vibrate(15); restoreDialog() }
        }

        val layoutArmyChipsContainer = view.findViewById<LinearLayout>(R.id.layoutArmyChipsContainer)
        val scrollArmyChips = view.findViewById<HorizontalScrollView>(R.id.scrollArmyChips)
        val btnAdd = view.findViewById<ImageButton>(R.id.btnAddGroup)
        val btnDelete = view.findViewById<ImageButton>(R.id.btnDeleteGroup)

        val etGroupName = view.findViewById<EditText>(R.id.etGroupName)
        val etLeaderName = view.findViewById<EditText>(R.id.etLeaderName)
        val btnApplyAllOrder = view.findViewById<Button>(R.id.btnApplyAllOrder)

        val etTargetHour = view.findViewById<EditText>(R.id.etTargetHour)
        val etTargetMin = view.findViewById<EditText>(R.id.etTargetMin)
        val etTargetSec = view.findViewById<EditText>(R.id.etTargetSec)
        val etMarchDuration = view.findViewById<EditText>(R.id.etMarchDuration)

        // 내 군단 출발 정보 섹터 (한 줄 심플 표기)
        val tvMyDepartureInfo = view.findViewById<TextView>(R.id.tvMyDepartureInfo)

        // 전체 군단 스케줄 리스트
        val btnCopyAllSchedule = view.findViewById<ImageButton>(R.id.btnCopyAllSchedule)
        val layoutAllArmiesList = view.findViewById<LinearLayout>(R.id.layoutAllArmiesList)
        val btnToggleAllArmies = view.findViewById<LinearLayout>(R.id.btnToggleAllArmies)
        val ivToggleArmiesIcon = view.findViewById<ImageView>(R.id.ivToggleArmiesIcon)
        val layoutAllArmiesContainer = view.findViewById<LinearLayout>(R.id.layoutAllArmiesContainer)

        // 집결원 입력란
        val etMembers = view.findViewById<EditText>(R.id.etMembers)

        // 섹션 토글 뷰 참조 (⚙️ 집결 설정 - 집결 대기 + 도착 시간 통합)
        val sectionHeaderRallySettings = view.findViewById<LinearLayout>(R.id.sectionHeaderRallySettings)
        val sectionBodyRallySettings = view.findViewById<LinearLayout>(R.id.sectionBodyRallySettings)
        val ivToggleRallySettings = view.findViewById<ImageView>(R.id.ivToggleRallySettings)

        // ⚙️ 집결 설정 통합 토글 (기본 닫힘)
        sectionHeaderRallySettings.setOnClickListener {
            vibrate(10)
            val expanding = sectionBodyRallySettings.visibility == View.GONE
            sectionBodyRallySettings.visibility = if (expanding) View.VISIBLE else View.GONE
            ivToggleRallySettings.setImageResource(if (expanding) R.drawable.ic_arrow_drop_up else R.drawable.ic_arrow_drop_down)
        }

        var isArmiesListExpanded = false
        btnToggleAllArmies.setOnClickListener {
            isArmiesListExpanded = !isArmiesListExpanded
            if (isArmiesListExpanded) {
                layoutAllArmiesContainer.visibility = View.VISIBLE
                ivToggleArmiesIcon.setImageResource(R.drawable.ic_arrow_drop_up)
            } else {
                layoutAllArmiesContainer.visibility = View.GONE
                ivToggleArmiesIcon.setImageResource(R.drawable.ic_arrow_drop_down)
            }
        }

        val btnStart = view.findViewById<Button>(R.id.btnStartRallyReservation)
        val btnSaveTarget = view.findViewById<Button>(R.id.btnSaveTargetPosition)
        val tvSavedTargetLocation = view.findViewById<TextView>(R.id.tvSavedTargetLocation)
        val btnModeAuto = view.findViewById<TextView>(R.id.btnModeAuto)
        val btnModeManual = view.findViewById<TextView>(R.id.btnModeManual)

        fun updateSavedTargetLocationUi() {
            val saved = PreferencesHelper.getSavedRallyTargetPosition(this)
            if (saved != null) {
                tvSavedTargetLocation.text = "🎯 저장된 타겟 : (${saved.first}, ${saved.second})"
                tvSavedTargetLocation.setTextColor(Color.parseColor("#38BDF8"))
            } else {
                tvSavedTargetLocation.text = "🎯 저장된 타겟 : 위치 미지정"
                tvSavedTargetLocation.setTextColor(Color.parseColor("#94A3B8"))
            }
        }
        updateSavedTargetLocationUi()

        var groups = RallyGroupManager.getGroups(this)
        var selectedGroup = RallyGroupManager.getSelectedGroup(this)

        var renderArmyChipsFunc: (() -> Unit)? = null

        fun updateRallyModeUi(isAuto: Boolean) {
            if (isAuto) {
                btnModeAuto.setBackgroundResource(R.drawable.bg_chip_selected)
                btnModeAuto.setTextColor(Color.WHITE)
                btnModeManual.setBackgroundResource(R.drawable.bg_chip_normal)
                btnModeManual.setTextColor(Color.parseColor("#94A3B8"))
            } else {
                btnModeAuto.setBackgroundResource(R.drawable.bg_chip_normal)
                btnModeAuto.setTextColor(Color.parseColor("#94A3B8"))
                btnModeManual.setBackgroundResource(R.drawable.bg_chip_selected)
                btnModeManual.setTextColor(Color.WHITE)
            }
        }
        updateRallyModeUi(selectedGroup.isAutoMode)

        btnModeAuto.setOnClickListener {
            vibrate(20)
            selectedGroup.isAutoMode = true
            RallyGroupManager.updateGroup(this@AutoClickService, selectedGroup)
            updateRallyModeUi(true)
            renderArmyChipsFunc?.invoke()
            showToast("🤖 [${selectedGroup.name}] 자동 대기 모드 ON\n(출발 5초 전 타겟 위치로 이동하여 자동 대기)")
        }

        btnModeManual.setOnClickListener {
            vibrate(20)
            selectedGroup.isAutoMode = false
            RallyGroupManager.updateGroup(this@AutoClickService, selectedGroup)
            updateRallyModeUi(false)
            renderArmyChipsFunc?.invoke()
            showToast("🖐️ [${selectedGroup.name}] 수동 모드 설정\n(이 군단은 5초 전 자동 감지를 스킵합니다)")
        }
        
        // 창이 열릴 때 목표 시각이 이미 지났을 경우에만 현재 디바이스 시간으로 초기화
        // skipTimeInit=true (싱크 후 재열기) 또는 미래 목표 시각인 경우 그대로 유지
        if (!skipTimeInit && selectedGroup.getRemainingMillis() <= 0) {
            val cal = Calendar.getInstance()
            selectedGroup.targetHour = cal.get(Calendar.HOUR_OF_DAY)
            selectedGroup.targetMinute = cal.get(Calendar.MINUTE)
            RallyGroupManager.saveGroups(this, groups)
        }
        
        var isUpdatingUi = false

        // 휠 숫자 클릭 시 전체 선택 활성화 (키패드로 즉시 수정 편의성)
        etTargetHour.setSelectAllOnFocus(true)
        etTargetMin.setSelectAllOnFocus(true)
        etTargetSec.setSelectAllOnFocus(true)
        etMarchDuration.setSelectAllOnFocus(true)

        class ArmyRowViews(val tvName: TextView, val tvTime: TextView)
        val armyListItems = mutableListOf<ArmyRowViews>()

        fun renderAllArmiesList() {
            layoutAllArmiesList.removeAllViews()
            armyListItems.clear()
            val all = RallyGroupManager.getGroups(this)
            all.forEachIndexed { index, group ->
                val row = LinearLayout(themedContext).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(0, dpToPx(4), 0, dpToPx(4))
                    gravity = Gravity.CENTER_VERTICAL
                }

                val tvName = TextView(themedContext).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    textSize = 10.5f
                    setTextColor(Color.parseColor("#E2E8F0"))
                }

                val tvTime = TextView(themedContext).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    textSize = 10.5f
                    setTextColor(Color.parseColor("#E2E8F0"))
                }

                row.addView(tvName)
                row.addView(tvTime)

                armyListItems.add(ArmyRowViews(tvName, tvTime))
                layoutAllArmiesList.addView(row)
            }
        }

        // 실시간 카운트다운 및 내 군단 상태 업데이트 (20초 전 카운트다운 강조)
        fun updateRealtimeCountdown() {
            if (rallyDialogView == null) return

            val currentG = RallyGroupManager.getGroups(this@AutoClickService).find { it.id == selectedGroup.id } ?: selectedGroup
            val remMs = currentG.getRemainingMillis()
            val waitMs = currentG.rallyWaitMinutes * 60_000L
            val marchMs = (currentG.marchDurationSec * 1000.0).toLong()
            val depTs = currentG.calculateDepartureTimestamp()
            val hasExecutedClick = currentG.hasExecutedClick(depTs)

            // ── 상태 문자열 및 색상 공통 계산 ──
            data class StatusInfo(val text: String, val color: String, val bgRes: Int = 0)

            val statusInfo: StatusInfo = if (remMs > 0) {
                val sec = ((remMs + 999L) / 1000L).coerceAtLeast(0L)
                if (sec > 500L) StatusInfo("⚔️ 집결대기중 (대기)", "#38BDF8")
                else if (sec <= 20L) StatusInfo("⚔️ 집결대기중 (🔥${sec}초)", "#EF4444", R.drawable.bg_countdown_urgent)
                else StatusInfo("⚔️ 집결대기중 (${sec}초)", "#38BDF8")
            } else {
                if (!hasExecutedClick) {
                    StatusInfo("⚠️ 시간 지남", "#EF4444", R.drawable.bg_countdown_urgent)
                } else {
                    val elapsedSinceOpen = -remMs
                    if (elapsedSinceOpen < waitMs) {
                        val waitRemainSec = ((waitMs - elapsedSinceOpen + 999L) / 1000L).coerceAtLeast(0L)
                        StatusInfo("⏳ 집결중 (${waitRemainSec}초)", "#38BDF8", R.drawable.bg_chip_selected)
                    } else if (elapsedSinceOpen < waitMs + marchMs) {
                        val targetTs = currentG.calculateTargetTimestamp()
                        val arriveRemainSec = ((targetTs - System.currentTimeMillis() + 999L) / 1000L).coerceAtLeast(0L)
                        StatusInfo("🚀 행군중 (${arriveRemainSec}초)", "#F59E0B", R.drawable.bg_countdown_urgent)
                    } else {
                        StatusInfo("💥 도착완료", "#10B981")
                    }
                }
            }

            // ── 최소화 모드일 때 컴팩트 뷰 상태/초 표시 ──
            if (rallyDialogContent.visibility == View.GONE) {
                val tvCompactCountdown = rallyDialogView?.findViewById<TextView>(R.id.tvCompactCountdown)
                val tvCompactMode = rallyDialogView?.findViewById<TextView>(R.id.tvCompactMode)
                tvCompactCountdown?.text = "${currentG.name}: ${statusInfo.text}"
                tvCompactCountdown?.setTextColor(Color.parseColor(statusInfo.color))
                tvCompactMode?.text = if (currentG.isAutoMode) "자동" else "수동"
                tvCompactMode?.setBackgroundResource(if (currentG.isAutoMode) R.drawable.bg_chip_selected else R.drawable.bg_chip_normal)
                tvCompactMode?.setTextColor(if (currentG.isAutoMode) Color.WHITE else Color.parseColor("#94A3B8"))
            }

            // ── 내 출발 예정 정보 (한 줄 심플 표기) ──
            val depStr = currentG.getDepartureTimeString()
            val statusPart = statusInfo.text  // 예: "⚔️ 집결대기중 (대기)", "⏳ 집결중 (122초)"
            tvMyDepartureInfo.text = "오픈 $depStr (${currentG.rallyWaitMinutes}분) | $statusPart"
            tvMyDepartureInfo.setTextColor(Color.parseColor(statusInfo.color))

            // 모든 군단 리스트 텍스트 업데이트
            val all = RallyGroupManager.getGroups(this)
            if (all.size != armyListItems.size) {
                renderAllArmiesList()
            }
            all.forEachIndexed { index, g ->
                if (index < armyListItems.size) {
                    val views = armyListItems[index]
                    val isCurrent = g.id == selectedGroup.id
                    val leaderStr = if (g.leaderName.isNotBlank()) "(${g.leaderName})" else ""
                    
                    val nameColor = if (isCurrent) "#38BDF8" else "#E2E8F0"
                    val htmlName = "<font color='$nameColor'>${g.name} $leaderStr</font>"
                    views.tvName.text = android.text.Html.fromHtml(htmlName, android.text.Html.FROM_HTML_MODE_COMPACT)

                    val remMs = g.getRemainingMillis()
                    val waitMs = g.rallyWaitMinutes * 60_000L
                    val marchMs = (g.marchDurationSec * 1000.0).toLong()
                    
                    val statusText: String
                    val statusColor: String
                    
                    val depTs = g.calculateDepartureTimestamp()
                    val hasExecutedClick = g.hasExecutedClick(depTs)

                    if (remMs > 0) {
                        val totalSec = (remMs + 999L) / 1000L
                        val mm = totalSec / 60
                        val ss = totalSec % 60
                        statusText = if (mm > 0) String.format("대기중(%02d:%02d)", mm, ss) else "대기중(${ss}초)"
                        statusColor = if (isCurrent) "#38BDF8" else "#94A3B8"
                    } else {
                        if (!hasExecutedClick) {
                            statusText = "미지정"
                            statusColor = "#EF4444"
                        } else {
                            val elapsedSinceOpen = -remMs
                            if (elapsedSinceOpen < waitMs) {
                                val waitRemainSec = ((waitMs - elapsedSinceOpen + 999L) / 1000L).coerceAtLeast(0L)
                                statusText = "⏳집결중(${waitRemainSec}초)"
                                statusColor = "#38BDF8"
                            } else if (elapsedSinceOpen < waitMs + marchMs) {
                                val targetTs = g.calculateTargetTimestamp()
                                val arriveRemainSec = ((targetTs - System.currentTimeMillis() + 999L) / 1000L).coerceAtLeast(0L)
                                statusText = "🚀행군중(${arriveRemainSec}초)"
                                statusColor = "#F59E0B"
                            } else {
                                statusText = "💥도착완료"
                                statusColor = "#10B981"
                            }
                        }
                    }

                    val timeColor = if (isCurrent) "#38BDF8" else "#E2E8F0"
                    val htmlTime = "<font color='$statusColor'>$statusText</font> <font color='#475569'>|</font> <font color='$timeColor'>${g.getDepartureTimeString()}</font>"
                    views.tvTime.text = android.text.Html.fromHtml(htmlTime, android.text.Html.FROM_HTML_MODE_COMPACT)
                }
            }
        }
        onUpdateCountdown = { updateRealtimeCountdown() }

        fun updateCalculationPreview() {
            val h = etTargetHour.text.toString().toIntOrNull() ?: selectedGroup.targetHour
            val m = etTargetMin.text.toString().toIntOrNull() ?: selectedGroup.targetMinute
            val s = etTargetSec.text.toString().toIntOrNull() ?: selectedGroup.targetSecond
            val march = etMarchDuration.text.toString().toDoubleOrNull() ?: selectedGroup.marchDurationSec

            selectedGroup.targetHour = h.coerceIn(0, 23)
            selectedGroup.targetMinute = m.coerceIn(0, 59)
            selectedGroup.targetSecond = s.coerceIn(0, 59)
            selectedGroup.marchDurationSec = (Math.round(march * 10.0) / 10.0).coerceAtLeast(0.1)
            selectedGroup.name = etGroupName.text.toString().ifBlank { selectedGroup.name }
            selectedGroup.leaderName = etLeaderName.text.toString().trim()
            selectedGroup.members = etMembers.text.toString().trim()

            // 유저가 설정한 행군시간을 로컬 설정에 영구 기억 (오더 동기화 시 덮어쓰기 방지용)
            PreferencesHelper.setLastMarchDurationSec(this@AutoClickService, selectedGroup.marchDurationSec)

            // 팝업에서 사용자가 시간을 수정하는 즉시 내부 DB에 실시간 저장 (백그라운드 자동감지가 즉시 새 시간 감지 가능)
            RallyGroupManager.updateGroup(this@AutoClickService, selectedGroup)

            updateRealtimeCountdown()
        }

        // 집결 대기 시간 칩 뷰 참조
        val btnWait1Min = view.findViewById<TextView>(R.id.btnWait1Min)
        val btnWait3Min = view.findViewById<TextView>(R.id.btnWait3Min)
        val btnWait5Min = view.findViewById<TextView>(R.id.btnWait5Min)
        val btnWait10Min = view.findViewById<TextView>(R.id.btnWait10Min)
        val tvRallyWaitNote = view.findViewById<TextView>(R.id.tvRallyWaitNote)

        fun updateWaitMinutesUi(minutes: Int) {
            btnWait1Min.setBackgroundResource(if (minutes == 1) R.drawable.bg_chip_selected else R.drawable.bg_chip_normal)
            btnWait1Min.setTextColor(if (minutes == 1) Color.WHITE else Color.parseColor("#94A3B8"))

            btnWait3Min.setBackgroundResource(if (minutes == 3) R.drawable.bg_chip_selected else R.drawable.bg_chip_normal)
            btnWait3Min.setTextColor(if (minutes == 3) Color.WHITE else Color.parseColor("#94A3B8"))

            btnWait5Min.setBackgroundResource(if (minutes == 5) R.drawable.bg_chip_selected else R.drawable.bg_chip_normal)
            btnWait5Min.setTextColor(if (minutes == 5) Color.WHITE else Color.parseColor("#94A3B8"))

            btnWait10Min.setBackgroundResource(if (minutes == 10) R.drawable.bg_chip_selected else R.drawable.bg_chip_normal)
            btnWait10Min.setTextColor(if (minutes == 10) Color.WHITE else Color.parseColor("#94A3B8"))

            tvRallyWaitNote.text = "(오픈 후 ${minutes}분 뒤 자동 출발)"
        }

        fun loadGroupToUi(g: RallyGroup) {
            isUpdatingUi = true
            selectedGroup = g

            etGroupName.setText(g.name)
            etLeaderName.setText(g.leaderName)
            etMembers.setText(g.members)
            etTargetHour.setText("%02d".format(g.targetHour))
            etTargetMin.setText("%02d".format(g.targetMinute))
            etTargetSec.setText("%02d".format(g.targetSecond))
            val marchStr = if (g.marchDurationSec % 1.0 == 0.0) g.marchDurationSec.toInt().toString() else String.format(Locale.US, "%.1f", g.marchDurationSec)
            etMarchDuration.setText(marchStr)

            updateWaitMinutesUi(g.rallyWaitMinutes)
            updateRallyModeUi(g.isAutoMode)

            isUpdatingUi = false
            updateCalculationPreview()
        }

        fun renderArmyChips() {
            layoutArmyChipsContainer.removeAllViews()
            groups = RallyGroupManager.getGroups(this)
            groups.forEachIndexed { index, group ->
                val chip = TextView(themedContext).apply {
                    val autoBadge = if (group.isAutoMode) " 🤖" else ""
                    text = "${group.name}$autoBadge"
                    textSize = 11f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setPadding(dpToPx(10), 0, dpToPx(10), 0)

                    val isSelected = group.id == selectedGroup.id
                    setBackgroundResource(if (isSelected) R.drawable.bg_chip_selected else R.drawable.bg_chip_normal)
                    setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#E2E8F0"))

                    val p = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        dpToPx(28)
                    ).apply {
                        if (index > 0) marginStart = dpToPx(5)
                    }
                    layoutParams = p

                    setOnClickListener {
                        vibrate(15)
                        updateCalculationPreview()
                        RallyGroupManager.updateGroup(this@AutoClickService, selectedGroup)
                        RallyGroupManager.setSelectedGroupId(this@AutoClickService, group.id)
                        loadGroupToUi(group)
                        renderArmyChips()
                    }
                }
                layoutArmyChipsContainer.addView(chip)
            }
        }
        renderArmyChipsFunc = { renderArmyChips() }

        loadGroupToUi(selectedGroup)
        renderArmyChips()

        // ── 도착 목표 시각: 분 단위 프리셋 버튼 ──────────────────────────
        // 현재 시:분:초 값을 분 단위로 증감하는 헬퍼
        fun shiftTargetByMinutes(deltaMinutes: Int) {
            val h = etTargetHour.text.toString().toIntOrNull() ?: selectedGroup.targetHour
            val m = etTargetMin.text.toString().toIntOrNull() ?: selectedGroup.targetMinute
            val s = etTargetSec.text.toString().toIntOrNull() ?: selectedGroup.targetSecond
            var totalMinutes = h * 60 + m + deltaMinutes
            // 24시간 범위 내에서 순환
            totalMinutes = ((totalMinutes % (24 * 60)) + 24 * 60) % (24 * 60)
            val newH = totalMinutes / 60
            val newM = totalMinutes % 60
            etTargetHour.setText("%02d".format(newH))
            etTargetMin.setText("%02d".format(newM))
            etTargetSec.setText("%02d".format(s))
            vibrate(20)
        }

        view.findViewById<TextView>(R.id.btnTimeMinus5).setOnClickListener { shiftTargetByMinutes(-5) }
        view.findViewById<TextView>(R.id.btnTimeMinus1).setOnClickListener { shiftTargetByMinutes(-1) }
        view.findViewById<TextView>(R.id.btnTimePlus1).setOnClickListener  { shiftTargetByMinutes(+1) }
        view.findViewById<TextView>(R.id.btnTimePlus5).setOnClickListener  { shiftTargetByMinutes(+5) }

        val etClickOffsetMs = view.findViewById<EditText>(R.id.etClickOffsetMs)
        etClickOffsetMs.setText(PreferencesHelper.getClickOffsetMs(this@AutoClickService).toString())
        etClickOffsetMs.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val offset = s.toString().toIntOrNull() ?: 0
                PreferencesHelper.setClickOffsetMs(this@AutoClickService, offset)
            }
        })



        // 클라우드 동기화 UI 연동
        val etRoomNumber = view.findViewById<EditText>(R.id.etRoomNumber)
        val tvSyncStatus = view.findViewById<TextView>(R.id.tvSyncStatus)
        val prefs = getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE)
        val savedRoom = prefs.getString("cloud_room_number", "") ?: ""
        etRoomNumber.setText(savedRoom)
        
        if (savedRoom.isNotEmpty()) {
            tvSyncStatus.text = "● 연결대기"
            tvSyncStatus.setTextColor(Color.parseColor("#38BDF8"))
        }

        etRoomNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val newRoom = s.toString().trim()
                prefs.edit()
                    .putString("cloud_room_number", newRoom)
                    .putLong("last_cloud_timestamp", 0) // 새 방에 연결 시 즉시 최신 데이터 수신할 수 있도록 리셋
                    .apply()
                if (newRoom.isNotEmpty()) {
                    tvSyncStatus.text = "● 연결대기"
                    tvSyncStatus.setTextColor(Color.parseColor("#38BDF8"))
                } else {
                    tvSyncStatus.text = "● 미사용"
                    tvSyncStatus.setTextColor(Color.parseColor("#94A3B8"))
                }
            }
        })

        // 클라우드로 목표시간 및 대기시간 올리기
        fun uploadTargetTimeToCloud(h: Int, m: Int, s: Int, waitMinutes: Int = selectedGroup.rallyWaitMinutes) {
            val room = prefs.getString("cloud_room_number", "") ?: ""
            if (room.isEmpty()) return
            RallyGroupManager.updateGroup(this@AutoClickService, selectedGroup)
            Thread {
                try {
                    val url = java.net.URL("$firebaseDbUrl/rooms/$room.json")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "PUT"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    
                    val timestamp = System.currentTimeMillis()
                    val json = org.json.JSONObject()
                    json.put("targetHour", h)
                    json.put("targetMinute", m)
                    json.put("targetSecond", s)
                    json.put("rallyWaitMinutes", waitMinutes)
                    json.put("timestamp", timestamp)
                    json.put("senderId", myDeviceId)
                    
                    val groupsArr = org.json.JSONArray()
                    val allGroups = RallyGroupManager.getGroups(this@AutoClickService)
                    for (g in allGroups) {
                        val gJson = org.json.JSONObject()
                        gJson.put("id", g.id)
                        gJson.put("name", g.name)
                        gJson.put("targetHour", g.targetHour)
                        gJson.put("targetMinute", g.targetMinute)
                        gJson.put("targetSecond", g.targetSecond)
                        gJson.put("marchDurationSec", g.marchDurationSec)
                        gJson.put("rallyWaitMinutes", g.rallyWaitMinutes)
                        gJson.put("leaderName", g.leaderName)
                        gJson.put("members", g.members)
                        groupsArr.put(gJson)
                    }
                    json.put("groups", groupsArr)
                    
                    conn.outputStream.write(json.toString().toByteArray())
                    if (conn.responseCode == 200) {
                        prefs.edit().putLong("last_cloud_timestamp", timestamp).apply()
                        Handler(Looper.getMainLooper()).post {
                            tvSyncStatus.text = "● 클라우드 전송됨"
                            tvSyncStatus.setTextColor(Color.parseColor("#10B981"))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }


        // ── 행군 시간: 0.1초 단위 조정 버튼 ──────────────────────────────
        fun shiftMarchBy(deltaSec: Double) {
            val cur = etMarchDuration.text.toString().toDoubleOrNull() ?: selectedGroup.marchDurationSec
            val newVal = (Math.round((cur + deltaSec) * 10.0) / 10.0).coerceAtLeast(0.1)
            val marchStr = if (newVal % 1.0 == 0.0) newVal.toInt().toString() else String.format(Locale.US, "%.1f", newVal)
            etMarchDuration.setText(marchStr)
            vibrate(20)
        }

        view.findViewById<TextView>(R.id.btnMarchMinus10).setOnClickListener { shiftMarchBy(-10.0) }
        view.findViewById<TextView>(R.id.btnMarchMinus1).setOnClickListener  { shiftMarchBy(-1.0) }
        view.findViewById<TextView>(R.id.btnMarchMinus01)?.setOnClickListener { shiftMarchBy(-0.1) }
        view.findViewById<TextView>(R.id.btnMarchPlus01)?.setOnClickListener  { shiftMarchBy(+0.1) }
        view.findViewById<TextView>(R.id.btnMarchPlus1).setOnClickListener   { shiftMarchBy(+1.0) }
        view.findViewById<TextView>(R.id.btnMarchPlus10).setOnClickListener  { shiftMarchBy(+10.0) }

        // ── 집결 대기 시간: 1분 / 3분 / 5분 / 10분 버튼 핸들러 ──────────────
        fun setRallyWaitMinutes(minutes: Int) {
            vibrate(20)
            selectedGroup.rallyWaitMinutes = minutes
            updateWaitMinutesUi(minutes)
            updateCalculationPreview()
            RallyGroupManager.updateGroup(this, selectedGroup)
            // 대기시간 변경 시에도 즉시 클라우드에 자동 동기화 전송!
            val h = etTargetHour.text.toString().toIntOrNull() ?: selectedGroup.targetHour
            val m = etTargetMin.text.toString().toIntOrNull() ?: selectedGroup.targetMinute
            val s = etTargetSec.text.toString().toIntOrNull() ?: selectedGroup.targetSecond
            uploadTargetTimeToCloud(h, m, s, minutes)
        }

        btnWait1Min.setOnClickListener  { setRallyWaitMinutes(1) }
        btnWait3Min.setOnClickListener  { setRallyWaitMinutes(3) }
        btnWait5Min.setOnClickListener  { setRallyWaitMinutes(5) }
        btnWait10Min.setOnClickListener { setRallyWaitMinutes(10) }

        fun copyAllArmiesSchedule() {
            vibrate(35)
            val all = RallyGroupManager.getGroups(this)
            val sb = java.lang.StringBuilder()
            sb.append("📢 [동시 착탄 전체 군단 작전표]\n")
            all.forEachIndexed { i, g ->
                val leaderInfo = if (g.leaderName.isNotBlank()) " (집결장: ${g.leaderName})" else ""
                val marchStr = if (g.marchDurationSec % 1.0 == 0.0) g.marchDurationSec.toInt().toString() else String.format(Locale.US, "%.1f", g.marchDurationSec)
                sb.append("${i + 1}. [${g.name}$leaderInfo] ⚔️집결오픈: ${g.getDepartureTimeString()} (${g.rallyWaitMinutes}분 대기, 행군 ${marchStr}초)\n")
            }
            sb.append("• 최종 착탄: %02d:%02d:%02d".format(selectedGroup.targetHour, selectedGroup.targetMinute, selectedGroup.targetSecond))
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("AllRallySchedule", sb.toString().trim()))
            showToast("📢 [전체 작전표] 복사 완료! 연맹창에 공유하세요.")
        }

        // 전체 리스트 복사 아이콘 클릭
        btnCopyAllSchedule.setOnClickListener {
            updateCalculationPreview()
            RallyGroupManager.updateGroup(this, selectedGroup)
            copyAllArmiesSchedule()
        }

        // 전체 오더 일괄 적용 버튼 (모든 군에 현재 목표 시간 및 대기시간 일괄 적용!)
        btnApplyAllOrder.setOnClickListener {
            vibrate(30)
            val h = etTargetHour.text.toString().toIntOrNull() ?: selectedGroup.targetHour
            val m = etTargetMin.text.toString().toIntOrNull() ?: selectedGroup.targetMinute
            val s = etTargetSec.text.toString().toIntOrNull() ?: selectedGroup.targetSecond
            val waitMin = selectedGroup.rallyWaitMinutes

            RallyGroupManager.applyTargetTimeToAllGroups(this, h, m, s, waitMin)
            selectedGroup.targetHour = h
            selectedGroup.targetMinute = m
            selectedGroup.targetSecond = s
            selectedGroup.rallyWaitMinutes = waitMin
            updateCalculationPreview()
            renderArmyChips()
            
            // 클라우드 업로드!
            uploadTargetTimeToCloud(h, m, s, waitMin)
            
            showToast("📢 [전체 오더] 모든 군의 도착 시각(%02d:%02d:%02d), 대기(%d분)를 일괄 적용했습니다!".format(h, m, s, waitMin))
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isUpdatingUi) updateCalculationPreview()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        etGroupName.addTextChangedListener(watcher)
        etLeaderName.addTextChangedListener(watcher)
        etMembers.addTextChangedListener(watcher)
        etTargetHour.addTextChangedListener(watcher)
        etTargetMin.addTextChangedListener(watcher)
        etTargetSec.addTextChangedListener(watcher)
        etMarchDuration.addTextChangedListener(watcher)

        // 모든 입력창 터치 시에만 일시적으로 키보드/입력 포커스를 획득하고,
        // 입력을 마치면 포커스를 해제하여 뒤의 게임 화면/채팅창 입력이 가능하도록 처리
        val allEditTexts = listOf(etGroupName, etLeaderName, etMembers, etTargetHour, etTargetMin, etTargetSec, etMarchDuration, etRoomNumber, etClickOffsetMs)
        allEditTexts.forEach { et ->
            et.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    setDialogFocusable(true)
                    v.requestFocus()
                }
                false
            }
            et.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    view.post {
                        val anyFocused = allEditTexts.any { it.isFocused }
                        if (!anyFocused) {
                            setDialogFocusable(false)
                            // 모든 입력창에서 포커스가 빠져나갔을 때 클라우드로 변경사항 전송
                            uploadTargetTimeToCloud(selectedGroup.targetHour, selectedGroup.targetMinute, selectedGroup.targetSecond)
                        }
                    }
                }
            }
        }

        // 그룹 추가 (실시간 칩 갱신 & 스크롤 이동 완벽 반영!)
        btnAdd.setOnClickListener {
            vibrate(20)
            groups = RallyGroupManager.getGroups(this)
            val newNum = groups.size + 1
            val newGroup = RallyGroupManager.addGroup(
                this,
                name = "${newNum}군 집결",
                leaderName = "${newNum}군장",
                targetHour = selectedGroup.targetHour,
                targetMinute = selectedGroup.targetMinute,
                targetSecond = 0,
                marchDurationSec = 120.0
            )
            selectedGroup = newGroup
            RallyGroupManager.setSelectedGroupId(this, newGroup.id)
            loadGroupToUi(newGroup)
            renderArmyChips() // 🔥 실시간 칩 즉시 갱신!
            scrollArmyChips.post { scrollArmyChips.fullScroll(View.FOCUS_RIGHT) } // 🔥 맨 오른쪽 끝으로 스크롤 이동!
            
            uploadTargetTimeToCloud(selectedGroup.targetHour, selectedGroup.targetMinute, selectedGroup.targetSecond)
            showToast("➕ 새 그룹 '${newGroup.name}'이 추가되었습니다.")
        }

        // 그룹 삭제 (실시간 칩 갱신 완벽 반영!)
        btnDelete.setOnClickListener {
            vibrate(20)
            groups = RallyGroupManager.getGroups(this)
            if (groups.size <= 1) {
                showToast("⚠️ 최소 1개의 작전 그룹은 유지되어야 합니다.")
                return@setOnClickListener
            }
            val deletedName = selectedGroup.getDisplayName()
            val ok = RallyGroupManager.deleteGroup(this, selectedGroup.id)
            if (ok) {
                selectedGroup = RallyGroupManager.getSelectedGroup(this)
                loadGroupToUi(selectedGroup)
                renderArmyChips() // 🔥 삭제 후 즉시 칩 목록 갱신!
                
                uploadTargetTimeToCloud(selectedGroup.targetHour, selectedGroup.targetMinute, selectedGroup.targetSecond)
                showToast("🗑️ '${deletedName}' 그룹을 삭제했습니다.")
            }
        }

        btnClose.setOnClickListener {
            vibrate(15)
            updateCalculationPreview()
            RallyGroupManager.updateGroup(this, selectedGroup)
            uploadTargetTimeToCloud(selectedGroup.targetHour, selectedGroup.targetMinute, selectedGroup.targetSecond)
            hideRallyDialog()
        }

        // 타겟 위치 저장 (현재 설정된 시간 정보 및 과녁 좌표 영구 보존)
        btnSaveTarget.setOnClickListener {
            vibrate(30)
            updateCalculationPreview()
            RallyGroupManager.updateGroup(this, selectedGroup)
            val curX = targetParams?.x ?: 0
            val curY = targetParams?.y ?: 0
            PreferencesHelper.setSavedRallyTargetPosition(this, curX, curY)
            updateSavedTargetLocationUi()
            tvSavedTargetLocation.setTextColor(Color.parseColor("#10B981"))
            if (!isTargetVisible) {
                toggleTargetVisibility()
            }
            (targetView as? TargetCrosshairView)?.pulse()
            showToast("📍 타겟 위치 저장 완료! (${curX}, ${curY})\n설정 시간과 과녁 위치가 저장되었습니다.")
        }

        // 정시 출발 예약 대기 시작
        btnStart.setOnClickListener {
            vibrate(30)
            updateCalculationPreview()
            RallyGroupManager.updateGroup(this, selectedGroup)
            uploadTargetTimeToCloud(selectedGroup.targetHour, selectedGroup.targetMinute, selectedGroup.targetSecond)

            val remainMs = selectedGroup.getRemainingMillis()
            if (remainMs <= 0) {
                showToast("⚠️ 출발 시각이 이미 지났습니다! 시간을 다시 확인해 주세요.")
                return@setOnClickListener
            }

            // 최근에 저장된 타겟 위치 확인 (방식 A: 저장된 위치가 없으면 대기 시작 차단)
            val savedPos = PreferencesHelper.getSavedRallyTargetPosition(this)
            if (savedPos == null) {
                vibrate(80)
                showToast("⚠️ 저장된 타겟 위치가 없습니다!\n먼저 과녁을 조준하고 [📍 타겟위치 저장]을 눌러주세요.")
                return@setOnClickListener
            }

            // 만약에 그 시점에 타겟이 다른 곳에 있어도 무조건 최근 저장된 곳으로 즉시 이동!
            moveTargetViewTo(savedPos.first, savedPos.second)

            // 예약 클릭 시 팝업 닫지 않고 최소화만 (타겟 확인/이동 가능하게)
            minimizeDialog()
            startRallyReservation(selectedGroup)
        }

        // 다이얼로그 실시간 1초 카운트다운 타이머 등록
        val timerRunnable = object : Runnable {
            override fun run() {
                if (rallyDialogView != null) {
                    updateRealtimeCountdown()
                    mainHandler.postDelayed(this, 1000L)
                }
            }
        }
        rallyDialogTimerRunnable = timerRunnable
        mainHandler.post(timerRunnable)

        wm.addView(view, params)
    }

    fun hideRallyHud() {
        rallyHudView?.let {
            if (it.isAttachedToWindow) {
                try { windowManager?.removeView(it) } catch (e: Exception) { Log.e(TAG, "Error removing rally HUD", e) }
            }
        }
        rallyHudView = null
        rallyHudParams = null
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun showRallyHud(group: RallyGroup) {
        hideRallyHud()
        val wm = windowManager ?: return

        val themedContext = android.view.ContextThemeWrapper(this, R.style.Theme_AutoClicker)
        val inflater = LayoutInflater.from(themedContext)
        val view = inflater.inflate(R.layout.layout_floating_rally_hud, null)
        this.rallyHudView = view

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dpToPx(50)
        }
        this.rallyHudParams = params

        view.findViewById<TextView>(R.id.tvRallyHudText).text = "[${group.getDisplayName()}] 예약 대기 중..."
        view.findViewById<ImageButton>(R.id.btnCancelRallyHud).setOnClickListener {
            vibrate(20)
            cancelRallyReservation()
            showToast("✕ 집결 출발 예약을 취소했습니다.")
        }

        // HUD 드래그 지원
        view.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchDownX = 0f
            private var touchDownY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val p = rallyHudParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = p.x
                        startY = p.y
                        touchDownX = event.rawX
                        touchDownY = event.rawY
                        return false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchDownX).toInt()
                        val dy = (event.rawY - touchDownY).toInt()
                        p.x = startX + dx
                        p.y = startY + dy
                        try {
                            wm.updateViewLayout(view, p)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating rally hud layout", e)
                        }
                        return true
                    }
                }
                return false
            }
        })

        wm.addView(view, params)
    }

    fun startRallyReservation(group: RallyGroup, isAutoReservation: Boolean = false) {
        if (isClicking) {
            stopAutoClick()
        }
        cancelRallyReservation()

        val savedPos = PreferencesHelper.getSavedRallyTargetPosition(this)
        if (savedPos == null) {
            vibrate(80)
            showToast("⚠️ 저장된 타겟 위치가 없습니다!\n먼저 과녁을 조준하고 [📍 타겟위치 저장]을 눌러주세요.")
            return
        }
        val target = targetView

        isRallyReserved = true
        reservedGroupName = group.getDisplayName()

        val departureTimestamp = group.calculateDepartureTimestamp()
        showRallyHud(group)
        updateNotification()

        // 자동예약(T-5초) 시 팝업이 열려 있으면 자동 최소화
        if (isAutoReservation && rallyDialogView != null) {
            mainHandler.post {
                val contentView = rallyDialogView?.findViewById<LinearLayout>(R.id.rallyDialogContent)
                val titleView = rallyDialogView?.findViewById<TextView>(R.id.tvRallyDialogTitle)
                val minBtn = rallyDialogView?.findViewById<TextView>(R.id.btnRallyMinimize)
                if (contentView?.visibility != View.GONE) {
                    minBtn?.performClick()
                }
            }
        }

        val location = IntArray(2)
        target?.getLocationOnScreen(location)
        val targetW = if (target != null && target.width > 0) target.width else dpToPx(38)
        val targetH = if (target != null && target.height > 0) target.height else dpToPx(38)
        
        // 최근에 저장된 타겟 위치를 적용! 타겟이 현재 다른 곳에 있더라도 저장된 곳으로 강제 이동
        val targetPx = savedPos.first
        val targetPy = savedPos.second

        // 예약 시작 즉시 타겟 뷰를 '최근에 저장된 위치'로 이동
        moveTargetViewTo(targetPx, targetPy)

        // 항상 최근에 저장된 좌표(targetPx, targetPy) 중심점을 타격 좌표로 고정
        val clickX = targetPx + targetW / 2f
        val clickY = targetPy + targetH / 2f

        mainHandler.post {
            setTargetTouchable(false) // 과녁이 터치를 가로채지 않고 게임 내 출발 버튼에 100% 닿도록 통과 설정
            setDialogTouchable(false) // 팝업창이 타겟 위치를 가리더라도 100% 뚫고 통과되도록 터치 투과 설정!
        }

        showToast("🚀 [${group.getDisplayName()}] 예약 대기 시작!\n집결 오픈: ${group.getDepartureTimeString()} (${group.rallyWaitMinutes}분 집결 후 자동출발)\n🎯 저장된 위치(${clickX.toInt()}, ${clickY.toInt()}) 정시 자동 클릭 대기")

        rallyJob = serviceScope.launch {
            val pulseRunnable = Runnable {
                (targetView as? TargetCrosshairView)?.pulse()
                onClickExecuted?.invoke()
            }

            var lastCountedSec = -1
            val offsetMs = PreferencesHelper.getClickOffsetMs(this@AutoClickService).toLong()
            val effectiveDepartureTime = departureTimestamp - offsetMs

            while (isActive && isRallyReserved) {
                val now = System.currentTimeMillis()
                val remain = effectiveDepartureTime - now

                if (remain <= 1000L) {
                    // 클릭 1초 전: 타겟이 혹시 다른 곳으로 옮겨졌더라도 무조건 저장된 위치로 다시 이동 보장!
                    moveTargetViewTo(targetPx, targetPy)
                    mainHandler.post {
                        setDialogTouchable(false)
                    }
                }

                if (remain <= 20L) {
                    // 정시 도달! 저장된 타겟 위치로 즉시 재확인 이동 후 딱 1회 정밀 클릭 실행
                    moveTargetViewTo(targetPx, targetPy)
                    mainHandler.post {
                        setDialogTouchable(false)
                    }
                    try {
                        val clickPath = Path().apply {
                            moveTo(clickX, clickY)
                        }
                        val stroke = GestureDescription.StrokeDescription(clickPath, 0L, 35L)
                        val gesture = GestureDescription.Builder().addStroke(stroke).build()
                        dispatchGesture(gesture, null, null)

                        // 🔥 해당 군단이 실제로 정시 클릭(출발)되었음을 영구 기록
                        group.lastDepartedTimestamp = departureTimestamp
                        RallyGroupManager.updateGroup(this@AutoClickService, group)

                        mainHandler.post(pulseRunnable)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error dispatching rally gesture", e)
                    }

                    mainHandler.post {
                        setTargetTouchable(true) // 출발 완료 후 과녁 다시 이동 가능하도록 복구
                        setDialogTouchable(true) // 팝업창 터치 조작 복구
                        vibrate(150) // 발사 완료 강력한 햅틱 진동 피드백
                        val hud = rallyHudView
                        if (hud != null && hud.isAttachedToWindow) {
                            val tv = hud.findViewById<TextView>(R.id.tvRallyHudText)
                            tv?.text = "🚀 [${group.getDisplayName()}] 출발 완료!"
                            tv?.setTextColor(Color.parseColor("#22C55E")) // 초록색 하이라이트
                            tv?.textSize = 13.5f
                        }
                        showToast("📢 [${group.getDisplayName()}] 출발했습니다! (동시 착탄 출정)")
                    }

                    // 2.5초간 화면에 '출발 완료' 메시지를 띄워 유저가 확인하게 한 뒤 HUD 정리
                    delay(2500L)
                    withContext(Dispatchers.Main) {
                        cancelRallyReservation()
                    }
                    break
                }

                // 출발 5초 전부터 화면에 카운트다운 (5... 4... 3... 2... 1...) 및 매초 햅틱 피드백
                if (remain in 21L..5500L) {
                    val countSec = ((remain + 900) / 1000).toInt().coerceIn(1, 5)
                    if (countSec != lastCountedSec) {
                        lastCountedSec = countSec
                        vibrate(30) // 매 초마다 경쾌한 햅틱 틱 피드백
                    }

                    mainHandler.post {
                        val hud = rallyHudView
                        if (hud != null && hud.isAttachedToWindow) {
                            val tv = hud.findViewById<TextView>(R.id.tvRallyHudText)
                            val color = if (countSec <= 2) Color.parseColor("#EF4444") else Color.parseColor("#F59E0B")
                            tv?.text = "🔥 [${group.getDisplayName()}] 출발 ${countSec}초 전! ($countSec)"
                            tv?.setTextColor(color)
                            tv?.textSize = 13.5f
                        }
                    }
                } else {
                    // 일반 대기 상태 HUD 텍스트 업데이트
                    val remainSec = (remain / 1000).coerceAtLeast(0)
                    val m = remainSec / 60
                    val s = remainSec % 60
                    val tenth = ((remain % 1000) / 100).coerceAtLeast(0)
                    val hudText = "[${group.getDisplayName()}] 출발까지: %02d:%02d.%d".format(m, s, tenth)

                    mainHandler.post {
                        val hud = rallyHudView
                        if (hud != null && hud.isAttachedToWindow) {
                            val tv = hud.findViewById<TextView>(R.id.tvRallyHudText)
                            tv?.text = hudText
                            tv?.setTextColor(Color.WHITE)
                            tv?.textSize = 12f
                        }
                    }
                }

                val sleepTime = if (remain > 5500L) 100L else if (remain > 50L) 25L else 2L
                delay(sleepTime)
            }
        }
    }

    fun cancelRallyReservation() {
        if (!isRallyReserved && rallyJob == null && rallyHudView == null) return
        isRallyReserved = false
        rallyJob?.cancel()
        rallyJob = null
        mainHandler.post {
            setTargetTouchable(true)
            setDialogTouchable(true)
        }
        hideRallyHud()
        updateNotification()
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

        val targetPx = targetParams?.x ?: 0
        val targetPy = targetParams?.y ?: 0
        
        val centerX = targetPx + target.width / 2f
        val centerY = targetPy + target.height / 2f

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

    private fun setDialogTouchable(touchable: Boolean) {
        val dialog = rallyDialogView ?: return
        val params = rallyDialogParams ?: return
        val wm = windowManager ?: return

        val oldFlags = params.flags
        params.flags = if (touchable) {
            params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        if (oldFlags != params.flags && dialog.isAttachedToWindow) {
            try {
                wm.updateViewLayout(dialog, params)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update dialog touchable state", e)
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
            isRallyReserved -> "⚔️ [${reservedGroupName}] 출발 예약 대기 중"
            isClicking -> "▶ 치료 연타 실행 중 (클릭중)"
            else -> "⏹ 오토클리커 대기 중"
        }
        val text = when {
            isRallyReserved -> "정시 도달 시 게임의 출발 버튼이 1회 자동 클릭됩니다."
            isClicking -> "정지하려면 아래 [연타 정지]를 누르세요."
            else -> "게임 화면의 컨트롤러로 조작하세요."
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_target)
            .setContentIntent(openIntent)
            .setOngoing(true)

        if (isRallyReserved) {
            builder.addAction(R.drawable.ic_close, "✕ 예약 취소", stopIntent)
        } else if (isClicking) {
            builder.addAction(R.drawable.ic_pause, "⏹ 연타 정지", stopIntent)
        }
        builder.addAction(R.drawable.ic_close, "✕ 위젯 종료", closeIntent)

        manager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun cancelNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }

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

    private fun startCloudSyncPolling() {
        if (isPolling) return
        isPolling = true
        syncPollingThread = Thread {
            val prefs = getSharedPreferences("AutoClickerPrefs", Context.MODE_PRIVATE)
            while (isPolling) {
                try {
                    val room = prefs.getString("cloud_room_number", "") ?: ""
                    if (room.isNotEmpty()) {
                        val url = java.net.URL("$firebaseDbUrl/rooms/$room.json")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "GET"
                        conn.connectTimeout = 3000
                        conn.readTimeout = 3000
                        
                        if (conn.responseCode == 200) {
                            val response = conn.inputStream.bufferedReader().readText()
                            if (response != "null") {
                                val json = org.json.JSONObject(response)
                                val timestamp = json.optLong("timestamp", 0)
                                val senderId = json.optString("senderId", "")
                                
                                val lastTimestamp = prefs.getLong("last_cloud_timestamp", 0)
                                if (timestamp > lastTimestamp && senderId != myDeviceId) {
                                    val targetH = json.optInt("targetHour", -1)
                                    val targetM = json.optInt("targetMinute", -1)
                                    val targetS = json.optInt("targetSecond", -1)
                                    val cloudWaitMinutes = json.optInt("rallyWaitMinutes", 5)
                                    
                                    if (targetH != -1) {
                                        prefs.edit().putLong("last_cloud_timestamp", timestamp).apply()
                                        
                                        val groupsObj = json.opt("groups")
                                        val rawGroupJsons = mutableListOf<org.json.JSONObject>()
                                        if (groupsObj is org.json.JSONArray) {
                                             for (i in 0 until groupsObj.length()) {
                                                 val item = groupsObj.optJSONObject(i)
                                                 if (item != null) rawGroupJsons.add(item)
                                             }
                                        } else if (groupsObj is org.json.JSONObject) {
                                             val keys = groupsObj.keys()
                                             while (keys.hasNext()) {
                                                 val item = groupsObj.optJSONObject(keys.next())
                                                 if (item != null) rawGroupJsons.add(item)
                                             }
                                        }
                                        
                                        if (rawGroupJsons.isNotEmpty()) {
                                            val currentSelectedName = RallyGroupManager.getSelectedGroup(this@AutoClickService).name
                                            val newGroups = mutableListOf<RallyGroup>()
                                            for (gJson in rawGroupJsons) {
                                                newGroups.add(RallyGroup(
                                                    id = gJson.optString("id", java.util.UUID.randomUUID().toString()),
                                                    name = gJson.optString("name", "집결"),
                                                    targetHour = gJson.optInt("targetHour", targetH),
                                                    targetMinute = gJson.optInt("targetMinute", targetM),
                                                    targetSecond = gJson.optInt("targetSecond", targetS),
                                                    marchDurationSec = RallyGroupManager.getGroups(this@AutoClickService).find { it.name.trim() == gJson.optString("name", "").trim() }?.marchDurationSec
                                                        ?: PreferencesHelper.getLastMarchDurationSec(this@AutoClickService)
                                                        ?: gJson.optDouble("marchDurationSec", 30.0),
                                                    rallyWaitMinutes = gJson.optInt("rallyWaitMinutes", cloudWaitMinutes),
                                                    leaderName = gJson.optString("leaderName", ""),
                                                    members = gJson.optString("members", ""),
                                                    isAutoMode = RallyGroupManager.getGroups(this@AutoClickService).find { it.name.trim() == gJson.optString("name", "").trim() }?.isAutoMode ?: false,
                                                    lastDepartedTimestamp = RallyGroupManager.getGroups(this@AutoClickService).find { it.name.trim() == gJson.optString("name", "").trim() }?.lastDepartedTimestamp ?: 0L
                                                ))
                                            }
                                            RallyGroupManager.saveGroups(this@AutoClickService, newGroups)
                                            val matched = newGroups.find { it.name.trim() == currentSelectedName.trim() }
                                            if (matched != null) {
                                                RallyGroupManager.setSelectedGroupId(this@AutoClickService, matched.id)
                                            }
                                        } else {
                                            RallyGroupManager.applyTargetTimeToAllGroups(this@AutoClickService, targetH, targetM, targetS, cloudWaitMinutes)
                                        }
                                        
                                        Handler(Looper.getMainLooper()).post {
                                            // 🔥 클라우드 오더 수신 시 강력한 알림음 및 특수 진동 (Feature 5)
                                            try {
                                                val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                                                android.media.RingtoneManager.getRingtone(this@AutoClickService, uri)?.play()
                                                
                                                // 지징-징 하는 패턴 진동
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                    val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                                                    vibratorManager.defaultVibrator.vibrate(
                                                        VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 150), -1)
                                                    )
                                                } else {
                                                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                                    vibrator.vibrate(longArrayOf(0, 100, 100, 150), -1)
                                                }
                                            } catch (e: Exception) {
                                                vibrate(300)
                                            }

                                            showToast("☁️ 방번호[$room] 새로운 오더 수신! (${cloudWaitMinutes}분 집결)")
                                            
                                            // 다이얼로그 UI 업데이트 - skipTimeInit=true로 싱크받은 시간 보호
                                            if (rallyDialogView != null) {
                                                hideRallyDialog()
                                                showRallyDialog(skipTimeInit = true)
                                            }
                                        }
                                    }
                                } else if (senderId != myDeviceId) {
                                    Handler(Looper.getMainLooper()).post {
                                        rallyDialogView?.findViewById<TextView>(R.id.tvSyncStatus)?.let {
                                            if (it.text.toString().contains("대기") || it.text.toString().contains("오류") || it.text.toString().contains("미사용")) {
                                                it.text = "● 실시간 감시중"
                                                it.setTextColor(Color.parseColor("#3B82F6"))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post {
                        rallyDialogView?.findViewById<TextView>(R.id.tvSyncStatus)?.let {
                            it.text = "● 서버 오류"
                            it.setTextColor(Color.parseColor("#EF4444"))
                        }
                    }
                }
                Thread.sleep(2000)
            }
        }
        syncPollingThread?.start()
    }

    private fun stopCloudSyncPolling() {
        isPolling = false
        syncPollingThread?.interrupt()
        syncPollingThread = null
    }
}
