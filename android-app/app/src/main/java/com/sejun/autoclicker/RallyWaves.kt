package com.sejun.autoclicker

/**
 * 조: 한 방의 군단을 몇 묶음으로 나눠, 묶음마다 성에 닿는 때를 다르게 한다(점령전에서 시간 차를 두고 치기).
 * 같은 조의 군단은 지금처럼 동시에 도착하고, 2조부터는 "1조보다 몇 초 뒤에 도착"하는 값(도착 간격)을 하나씩 가진다.
 * 행군 시간은 건드리지 않는다. 시작은 한 번이다. 모든 군단이 1조면 조가 없는 것과 똑같이 움직인다.
 */
object RallyWaves {
    /** 조는 1조부터 5조까지. 한 조의 군단 수는 제한하지 않는다(방 전체 군단 수만 [RallyRoomEdit.MAX_TEAMS]). */
    const val MAX_WAVES = 5
    /** 도착 간격은 10초씩, 10분까지. */
    const val MAX_DELAY_SEC = 600.0
    const val DELAY_STEP_SEC = 10.0

    fun cleanWave(wave: Int): Int = wave.coerceIn(1, MAX_WAVES)
    fun cleanDelay(sec: Double): Double = if (sec.isNaN()) 0.0 else sec.coerceIn(0.0, MAX_DELAY_SEC)

    /** 서버에 쓰는 이름. 숫자만 쓰면 Firebase가 목록으로 바꿔 버려서 글자를 붙인다. */
    fun key(wave: Int): String = "w$wave"
    fun waveOfKey(key: String): Int? = key.removePrefix("w").toIntOrNull()?.takeIf { key.startsWith("w") && it in 2..MAX_WAVES }

    /** 이 조의 도착 간격(초). 1조는 기준이라 항상 0. */
    fun delayOf(delays: Map<Int, Double>, wave: Int): Double = if (wave <= 1) 0.0 else cleanDelay(delays[wave] ?: 0.0)

    /** 군단 이름 속 첫 숫자(2군 → 2). 화면 순서와 조 나누기가 같은 순서를 쓴다. */
    fun teamNumber(name: String): Int? = Regex("\\d+").find(name)?.value?.toIntOrNull()

    /** "40초", "1분", "1분 10초" */
    fun span(sec: Double): String {
        val total = Math.round(Math.max(0.0, sec)).toInt()
        val m = total / 60
        val s = total % 60
        return T.s.span(m, s)분 ${s}초"
        }
    }

    /** 조 제목 줄의 값: "먼저 도착", "같이 도착", "1분 뒤 도착" */
    fun delayLabel(wave: Int, delaySec: Double): String = when {
        wave <= 1 -> T.s.arrivesFirst
        delaySec <= 0.0 -> T.s.arrivesTogether
        else -> T.s.arrivesAfter(span(delaySec))
    }

    /**
     * 조 나누기: 화면 순서의 군단 ID를 [size]개씩 차례로 1조, 2조…에 넣는다. 조가 모자라면 남는 군단은 마지막 조에 넣는다.
     * [size]가 0 이하면 모두 1조로 되돌린다.
     */
    fun split(orderedIds: List<String>, size: Int): Map<String, Int> =
        orderedIds.withIndex().associate { (i, id) -> id to (if (size <= 0) 1 else Math.min(i / size + 1, MAX_WAVES)) }
}
