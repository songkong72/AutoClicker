package com.sejun.autoclicker


/** 패널에 보이는 초 단위 글자 모양(정수면 소수점 없이). */
internal object RallyPanelFormat {
    fun sec(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

    /** 기준 군단과의 행군시간 차이. 예: +18, −5.5, ±0 (단위 s). */
    fun diff(d: Double): String {
        val r = Math.round(d * 10) / 10.0
        return when {
            r == 0.0 -> "±0s"
            r > 0 -> "+" + sec(r) + "s"
            else -> "−" + sec(-r) + "s"
        }
    }
}
