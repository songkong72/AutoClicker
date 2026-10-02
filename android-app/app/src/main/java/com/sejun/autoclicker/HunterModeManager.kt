package com.sejun.autoclicker

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.HardwareBuffer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

object HunterModeManager {
    private const val TAG = "HunterModeManager"
    
    var isHunterModeEnabled = false
    var isAutoScanActive = true // true: 자동 감시, false: 볼륨키 수동
    
    // 저장된 타겟 좌표 (기기 해상도 기준)
    var flag1X: Int = 0
    var flag1Y: Int = 0
    var flag8X: Int = 0
    var flag8Y: Int = 0
    var dispatchX: Int = 0
    var dispatchY: Int = 0
    var dispatchColor: Int = 0

    // 스캔 쿨타임 (더블 클릭 방지용)
    private var lastScanTime = 0L
    private val COOL_DOWN_MS = 3000L

    // 화면 캡처 결과(전체 화면 비트맵 복사와 픽셀 검사)는 메인 스레드가 아니라 전용 스레드에서 처리한다. 앱 멈춤("앱 대기") 방지.
    private val worker = java.util.concurrent.Executors.newSingleThreadExecutor()
    /** 캡처 요청이 아직 끝나지 않았으면 새로 요청하지 않는다(요청이 겹쳐 쌓이는 것을 막는다). */
    @Volatile private var scanning = false

    fun loadSettings(service: AutoClickService) {
        val prefs = service.getSharedPreferences("HunterPrefs", android.content.Context.MODE_PRIVATE)
        flag1X = prefs.getInt("flag1X", 0)
        flag1Y = prefs.getInt("flag1Y", 0)
        flag8X = prefs.getInt("flag8X", 0)
        flag8Y = prefs.getInt("flag8Y", 0)
        dispatchX = prefs.getInt("dispatchX", 0)
        dispatchY = prefs.getInt("dispatchY", 0)
        dispatchColor = prefs.getInt("dispatchColor", 0)
        isAutoScanActive = prefs.getBoolean("isAutoScanActive", true)
    }

    fun saveSettings(service: AutoClickService) {
        val prefs = service.getSharedPreferences("HunterPrefs", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("flag1X", flag1X)
            .putInt("flag1Y", flag1Y)
            .putInt("flag8X", flag8X)
            .putInt("flag8Y", flag8Y)
            .putInt("dispatchX", dispatchX)
            .putInt("dispatchY", dispatchY)
            .putInt("dispatchColor", dispatchColor)
            .putBoolean("isAutoScanActive", isAutoScanActive)
            .apply()
        Toast.makeText(service, "🐻 헌터 타겟 저장 완료!", Toast.LENGTH_SHORT).show()
    }

    /** 헌터 모드를 끄고 진행 중인 감시 요청 표시를 지운다. */
    fun stop() { isHunterModeEnabled = false }

    fun triggerScan(service: AutoClickService) {
        if (!isHunterModeEnabled) return
        if (System.currentTimeMillis() - lastScanTime < COOL_DOWN_MS) return
        if (scanning) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            scanning = true
            try {
                service.takeScreenshot(
                    android.view.Display.DEFAULT_DISPLAY,
                    worker,
                    object : AccessibilityService.TakeScreenshotCallback {
                        override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                            val hwBuffer = screenshot.hardwareBuffer
                            try {
                                val bitmap = Bitmap.wrapHardwareBuffer(hwBuffer, screenshot.colorSpace)
                                val swBitmap = bitmap?.copy(Bitmap.Config.ARGB_8888, false)
                                if (swBitmap != null) {
                                    analyzeAndClick(service, swBitmap)
                                    swBitmap.recycle()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Screenshot processing failed", e)
                            } finally {
                                try { hwBuffer.close() } catch (_: Exception) { }
                                scanning = false
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            scanning = false
                        }
                    }
                )
            } catch (e: Exception) {
                scanning = false
                Log.e(TAG, "takeScreenshot request failed", e)
            }
        } else {
            Toast.makeText(service, "화면 캡처 스캔은 안드로이드 11 이상부터 지원됩니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun analyzeAndClick(service: AutoClickService, bitmap: Bitmap) {
        // 출정 화면 감지 (임시로 타겟 위치가 화면 범위를 벗어나지 않는지 검사)
        if (dispatchX >= bitmap.width || dispatchY >= bitmap.height || flag1X >= bitmap.width || flag1Y >= bitmap.height) {
            return
        }

        // 출정 버튼이 파란색인지 감지 (넓은 범위 스캔으로 텍스트 위에서도 작동하게 함)
        if (isAutoScanActive) {
            var foundBlue = false
            val startX = Math.max(0, dispatchX - 15)
            val endX = Math.min(bitmap.width - 1, dispatchX + 15)
            val startY = Math.max(0, dispatchY - 15)
            val endY = Math.min(bitmap.height - 1, dispatchY + 15)
            
            for (y in startY..endY step 5) {
                for (x in startX..endX step 5) {
                    val color = bitmap.getPixel(x, y)
                    val r = Color.red(color)
                    val g = Color.green(color)
                    val b = Color.blue(color)
                    if (b > r + 20 && b > g && b > 100) {
                        foundBlue = true
                        break
                    }
                }
                if (foundBlue) break
            }
            if (!foundBlue) return
        }
        
        // 1. 깃발 간격 계산
        val totalDx = flag8X - flag1X
        val totalDy = flag8Y - flag1Y
        val dxPerFlag = totalDx / 7.0
        val dyPerFlag = totalDy / 7.0

        var targetFlagX = flag1X
        var targetFlagY = flag1Y

        // 2. 1번부터 8번까지 깃발 스캔
        // 깃발 아이콘 버튼의 크기를 추정하여 좌상단과 우상단 비교
        // 보통 타겟을 깃발 정중앙에 둔다고 가정. 폭은 깃발 간격과 비슷함
        val halfFlagWidth = (dxPerFlag * 0.4).toInt().coerceAtLeast(10)
        val checkOffsetY = -halfFlagWidth // 정중앙에서 위쪽으로

        for (i in 0..7) {
            val cx = (flag1X + dxPerFlag * i).toInt()
            val cy = (flag1Y + dyPerFlag * i).toInt()
            
            val topLeftX = (cx - halfFlagWidth).coerceIn(0, bitmap.width - 1)
            val topLeftY = (cy + checkOffsetY).coerceIn(0, bitmap.height - 1)
            val topRightX = (cx + halfFlagWidth).coerceIn(0, bitmap.width - 1)
            val topRightY = (cy + checkOffsetY).coerceIn(0, bitmap.height - 1)

            val colorTL = bitmap.getPixel(topLeftX, topLeftY)
            val colorTR = bitmap.getPixel(topRightX, topRightY)

            // RGB 값 차이 계산
            val rDiff = Math.abs(Color.red(colorTL) - Color.red(colorTR))
            val gDiff = Math.abs(Color.green(colorTL) - Color.green(colorTR))
            val bDiff = Math.abs(Color.blue(colorTL) - Color.blue(colorTR))

            val isMatch = rDiff < 30 && gDiff < 30 && bDiff < 30

            if (isMatch) {
                // 느낌표가 없는 빈 깃발 발견!
                targetFlagX = cx
                targetFlagY = cy
                Log.d(TAG, "Empty flag found at index $i ($cx, $cy)")
                break
            }
        }

        // 3. 클릭 발사
        lastScanTime = System.currentTimeMillis() // 쿨타임 시작
        
        // 클릭은 메인 스레드에서 보낸다. 1) 깃발, 2) 0.15초 뒤 출정 버튼
        val main = Handler(Looper.getMainLooper())
        main.post { dispatchSingleClick(service, targetFlagX.toFloat(), targetFlagY.toFloat()) }
        main.postDelayed({
            dispatchSingleClick(service, dispatchX.toFloat(), dispatchY.toFloat())
            Toast.makeText(service, "🐻 헌터 발사 완료!", Toast.LENGTH_SHORT).show()
        }, 150)
    }

    private fun dispatchSingleClick(service: AccessibilityService, x: Float, y: Float) {
        val path = android.graphics.Path()
        path.moveTo(x, y)
        val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = android.accessibilityservice.GestureDescription.Builder().addStroke(stroke).build()
        service.dispatchGesture(gesture, null, null)
    }
    fun forceDispatchCombo(service: AutoClickService) {
        // 색상 검사나 화면 캡처 없이 즉시 1번 타겟 클릭 후 출정 타겟 클릭 (스나이퍼 샷)
        val path = android.graphics.Path()
        path.moveTo(flag1X.toFloat(), flag1Y.toFloat())
        val builder = android.accessibilityservice.GestureDescription.Builder()
        builder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 50))
        service.dispatchGesture(builder.build(), null, null)

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val path2 = android.graphics.Path()
            path2.moveTo(dispatchX.toFloat(), dispatchY.toFloat())
            val builder2 = android.accessibilityservice.GestureDescription.Builder()
            builder2.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path2, 0, 50))
            service.dispatchGesture(builder2.build(), null, null)
        }, 150)
    }

}


