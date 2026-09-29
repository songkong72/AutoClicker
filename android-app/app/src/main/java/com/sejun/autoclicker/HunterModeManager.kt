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

    fun triggerScan(service: AutoClickService) {
        if (!isHunterModeEnabled) return
        if (System.currentTimeMillis() - lastScanTime < COOL_DOWN_MS) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val executor = service.mainExecutor
            service.takeScreenshot(
                android.view.Display.DEFAULT_DISPLAY,
                executor,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                        try {
                            val hwBuffer = screenshot.hardwareBuffer
                            val colorSpace = screenshot.colorSpace
                            val bitmap = Bitmap.wrapHardwareBuffer(hwBuffer, colorSpace)
                            
                            val swBitmap = bitmap?.copy(Bitmap.Config.ARGB_8888, false)
                            hwBuffer.close()
                            
                            if (swBitmap != null) {
                                analyzeAndClick(service, swBitmap)
                                swBitmap.recycle()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Screenshot processing failed", e)
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        Log.e(TAG, "Screenshot failed with error code: $errorCode")
                    }
                }
            )
        } else {
            Toast.makeText(service, "화면 캡처 스캔은 안드로이드 11 이상부터 지원됩니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun analyzeAndClick(service: AutoClickService, bitmap: Bitmap) {
        // 출정 화면 감지 (임시로 타겟 위치가 화면 범위를 벗어나지 않는지 검사)
        if (dispatchX >= bitmap.width || dispatchY >= bitmap.height || flag1X >= bitmap.width || flag1Y >= bitmap.height) {
            return
        }

        // 출정 화면인지 1차 검증: 출정 버튼 위치의 색상 비교 (자동 모드일 때만 검증)
        val currentColor = bitmap.getPixel(dispatchX, dispatchY)
        if (isAutoScanActive && dispatchColor != 0) {
            val rDiffBtn = Math.abs(Color.red(currentColor) - Color.red(dispatchColor))
            val gDiffBtn = Math.abs(Color.green(currentColor) - Color.green(dispatchColor))
            val bDiffBtn = Math.abs(Color.blue(currentColor) - Color.blue(dispatchColor))
            if (rDiffBtn > 30 || gDiffBtn > 30 || bDiffBtn > 30) {
                // 출정 화면이 아님! 스캔 중단
                return
            }
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
        
        // 클릭 1: 깃발
        dispatchSingleClick(service, targetFlagX.toFloat(), targetFlagY.toFloat())
        
        // 클릭 2: 출정 버튼 (0.15초 뒤)
        Handler(Looper.getMainLooper()).postDelayed({
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
}
