package com.sejun.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * 곰 사냥 수동 발사. 화면을 분석하지 않는다.
 * 사용자가 집결 목록에서 직접 고른 뒤 볼륨 아래 키를 누르면, 미리 저장한 "쓸 부대" 위치를 누르고 0.15초 뒤 "출정" 위치를 누른다.
 * 저장하는 좌표는 화면 픽셀 기준 중심점이다.
 */
object HunterModeManager {
    var isHunterModeEnabled = false

    // 저장된 위치(화면 픽셀, 중심점). 0,0 이면 아직 저장 전.
    var troopX: Int = 0
    var troopY: Int = 0
    var dispatchX: Int = 0
    var dispatchY: Int = 0

    private var lastFireMs = 0L

    val hasTargets: Boolean get() = (troopX != 0 || troopY != 0) && (dispatchX != 0 || dispatchY != 0)

    fun loadSettings(service: AutoClickService) {
        val p = service.getSharedPreferences("HunterPrefs", Context.MODE_PRIVATE)
        troopX = p.getInt("troopCX", 0)
        troopY = p.getInt("troopCY", 0)
        dispatchX = p.getInt("dispatchCX", 0)
        dispatchY = p.getInt("dispatchCY", 0)
    }

    fun saveSettings(service: AutoClickService) {
        service.getSharedPreferences("HunterPrefs", Context.MODE_PRIVATE).edit()
            .putInt("troopCX", troopX).putInt("troopCY", troopY)
            .putInt("dispatchCX", dispatchX).putInt("dispatchCY", dispatchY)
            .apply()
        Toast.makeText(service, "🐻 헌터 위치 저장 완료!", Toast.LENGTH_SHORT).show()
    }

    /** 헌터 모드를 끈다. */
    fun stop() { isHunterModeEnabled = false }

    /** 볼륨 아래 키가 눌렸을 때 호출. 발사했으면 true. */
    fun fire(service: AutoClickService, repeatCount: Int): Boolean {
        val now = System.currentTimeMillis()
        if (!hasTargets) {
            Toast.makeText(service, "🐻 먼저 헌터 위치를 저장해 주세요 (🐻 버튼을 길게 누르기)", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!HunterFirePolicy.allow(hasTargets, repeatCount, now, lastFireMs)) return false
        lastFireMs = now
        val main = Handler(Looper.getMainLooper())
        main.post { tap(service, troopX.toFloat(), troopY.toFloat()) }
        main.postDelayed({
            tap(service, dispatchX.toFloat(), dispatchY.toFloat())
            Toast.makeText(service, "🐻 헌터 발사!", Toast.LENGTH_SHORT).show()
        }, 150)
        return true
    }

    private fun tap(service: AccessibilityService, x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        service.dispatchGesture(
            GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 50)).build(), null, null
        )
    }
}
