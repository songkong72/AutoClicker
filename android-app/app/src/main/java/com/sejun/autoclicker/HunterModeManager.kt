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
 * 사용자가 집결 목록에서 직접 고른 뒤 🐻 발사 버튼을 누르면, 미리 저장한 "쓸 부대" 위치를 곧바로 누르고 [GAP_MS] 뒤 "출정" 위치를 누른다.
 * 부대는 최대 7곳까지 저장하고, 누를 때마다 1번 → 2번 → … 순서로 쓴다. 저장한 마지막 부대 다음에는 다시 1번이다.
 * 저장하는 좌표는 화면 픽셀 기준 중심점이다.
 */
object HunterModeManager {
    var isHunterModeEnabled = false

    // 저장된 위치(화면 픽셀, 중심점). 부대가 비었거나 출정이 0,0 이면 아직 저장 전.
    internal var troops: List<TroopPoint> = emptyList()
    var dispatchX: Int = 0
    var dispatchY: Int = 0

    private var lastFireMs = 0L

    /** 다음 발사에 쓸 부대 번호(0부터). 저장 값이 아니라 이번 실행 중에만 기억한다. */
    var nextTroop = 0
        private set

    val hasTargets: Boolean get() = troops.isNotEmpty() && (dispatchX != 0 || dispatchY != 0)

    /** 다음 발사를 1번 부대부터 다시 시작한다. */
    fun resetSequence() { nextTroop = 0 }

    fun loadSettings(service: AutoClickService) {
        val p = service.getSharedPreferences("HunterPrefs", Context.MODE_PRIVATE)
        troops = HunterTroops.decode(p.getString("troops", null))
        if (troops.isEmpty()) {
            // 예전 버전은 부대를 1곳만 저장했다. 그대로 1개짜리 목록으로 읽는다.
            val ox = p.getInt("troopCX", 0)
            val oy = p.getInt("troopCY", 0)
            if (ox != 0 || oy != 0) troops = listOf(TroopPoint(ox, oy))
        }
        nextTroop = 0
        dispatchX = p.getInt("dispatchCX", 0)
        dispatchY = p.getInt("dispatchCY", 0)
    }

    fun saveSettings(service: AutoClickService) {
        service.getSharedPreferences("HunterPrefs", Context.MODE_PRIVATE).edit()
            .putString("troops", HunterTroops.encode(troops))
            .putInt("dispatchCX", dispatchX).putInt("dispatchCY", dispatchY)
            .apply()
        nextTroop = 0
        Toast.makeText(service, T.x.hunterSaved(troops.size), Toast.LENGTH_SHORT).show()
    }

    /** 헌터 모드를 끈다. */
    fun stop() { isHunterModeEnabled = false; resetSequence() }

    /**
     * 화면 발사 버튼이 눌렸을 때 호출. 발사했으면 true.
     * [beforeTap]은 첫 클릭 전에, [afterTap]은 모든 클릭이 끝난 뒤에 부른다. 과녁 오버레이가 클릭을 가로채지 않게 투과시키는 데 쓴다.
     */
    fun fire(service: AutoClickService, repeatCount: Int, beforeTap: () -> Unit = {}, afterTap: () -> Unit = {}): Boolean {
        val now = System.currentTimeMillis()
        if (!hasTargets) {
            Toast.makeText(service, T.x.hunterSaveFirst, Toast.LENGTH_SHORT).show()
            return false
        }
        if (!HunterFirePolicy.allow(hasTargets, repeatCount, now, lastFireMs)) return false
        lastFireMs = now
        val list = troops
        val idx = HunterTroops.current(nextTroop, list.size)
        val troop = list[idx]
        nextTroop = HunterTroops.next(idx, list.size) // 다음 발사는 다음 부대. 마지막이면 1번으로.
        beforeTap()
        val main = Handler(Looper.getMainLooper())
        val first = SETTLE_MS
        main.postDelayed({ tap(service, troop.x.toFloat(), troop.y.toFloat()) }, first)
        main.postDelayed({
            tap(service, dispatchX.toFloat(), dispatchY.toFloat())
            val label = if (list.size > 1) T.x.hunterMarchN(idx + 1, list.size) else T.x.hunterFire
            Toast.makeText(service, label, Toast.LENGTH_SHORT).show()
        }, first + GAP_MS)
        main.postDelayed({ afterTap() }, first + GAP_MS + 150L)
        return true
    }

    /** 오버레이가 투과 상태로 바뀔 시간. */
    private const val SETTLE_MS = 40L
    /**
     * 부대 클릭과 출정 클릭 사이 간격. 사용자 요청(2026-10-28): 부대를 누르면 출정은 바로 누른다.
     * 부대 클릭(누름 0.05초)이 끝나자마자 출정을 누르도록 0.06초로 둔다(0.15초에서 줄임).
     * (2026-10-24에 첫 클릭 전 0.4초 대기와 간격 0.25초로 늘렸더니 한 박자 늦고 실패가 더 늘어 되돌렸다.
     *  클릭이 늦어질수록 사용자의 다음 손가락이 게임을 누르는 때와 겹쳐, 앱이 보낸 클릭이 끊긴다.)
     */
    const val GAP_MS = 60L

    private fun tap(service: AccessibilityService, x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        service.dispatchGesture(
            GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 50)).build(), null, null
        )
    }
}
