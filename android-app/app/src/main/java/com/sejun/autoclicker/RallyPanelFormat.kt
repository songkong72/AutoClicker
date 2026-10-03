package com.sejun.autoclicker

/** 패널에 보이는 초 단위 글자 모양(정수면 소수점 없이). */
internal object RallyPanelFormat {
    fun sec(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

    /**
     * 가장 먼저 출발하는 군단(행군이 가장 긴 군단, [firstMarch])보다 몇 초 늦게 출발하는지.
     * 같으면 "기준", 아니면 "+18s"처럼 늦는 만큼.
     */
    fun laterThanFirst(march: Double, firstMarch: Double): String {
        val d = Math.round((firstMarch - march) * 10) / 10.0
        return if (d <= 0.0) "기준" else "+" + sec(d) + "s"
    }
}
