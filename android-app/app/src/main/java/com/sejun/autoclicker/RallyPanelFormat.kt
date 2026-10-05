package com.sejun.autoclicker

/** 군단 한 줄의 시간차이 계산 입력: 행군시간, 관리자 보정(ms), 제외 여부. */
internal data class LagInput(val marchSec: Double, val adminAdjustMs: Int, val excluded: Boolean)

/** 패널에 보이는 초 단위 글자 모양(정수면 소수점 없이). */
internal object RallyPanelFormat {
    fun sec(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

    /**
     * 군단별 "가장 먼저 누르는 군단보다 몇 초 늦게 누르는지". 관리자 보정까지 포함해 계산한다.
     * 누르는 시각 = (가장 긴 행군 − 내 행군) + 관리자 보정. 가장 먼저 누르는 군단은 "기준", 제외된 군단은 "".
     * (집결장 기기마다 다른 내 보정은 방 데이터에 없어 포함하지 않는다.)
     */
    fun lagLabels(items: List<LagInput>): List<String> {
        val active = items.filter { !it.excluded }
        if (active.isEmpty()) return items.map { "" }
        val maxMarch = active.maxOf { it.marchSec }
        val click = { i: LagInput -> (maxMarch - i.marchSec) + i.adminAdjustMs / 1000.0 }
        val first = active.minOf(click)
        return items.map { i ->
            if (i.excluded) ""
            else {
                val d = Math.round((click(i) - first) * 10) / 10.0
                if (d <= 0.0) "기준" else "+" + sec(d) + "s"
            }
        }
    }
}
