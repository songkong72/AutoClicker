package com.sejun.autoclicker

/** 앱 첫 화면에 보이는 연타 설정 한 줄 요약("0.5초마다 · 무한 반복"). 안드로이드 클래스를 쓰지 않아 단위 테스트가 된다. */
internal object ClickSummary {
    fun text(intervalMs: Long, mode: RepeatMode, count: Int, durationSec: Int): String {
        val every = seconds(intervalMs) + "초마다"
        val stop = when (mode) {
            RepeatMode.INFINITE -> "무한 반복"
            RepeatMode.COUNT -> "${count}회"
            RepeatMode.TIMER -> duration(durationSec) + " 동안"
        }
        return "$every · $stop"
    }

    /** 500 → "0.5", 1000 → "1", 250 → "0.25". 요약 한 줄과 설정 창의 입력 칸이 같은 글자를 쓴다. */
    fun seconds(ms: Long): String =
        java.math.BigDecimal(ms).movePointLeft(3).stripTrailingZeros().toPlainString()

    /** 입력 칸에 적은 초("0.5")를 ms(500)로. 숫자가 아니거나 0 이하면 null. */
    fun parseSeconds(text: String): Long? {
        val sec = text.trim().toBigDecimalOrNull() ?: return null
        val ms = sec.movePointRight(3).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
        return if (ms > 0) ms else null
    }

    /** 90 → "1분 30초", 60 → "1분", 45 → "45초" */
    private fun duration(sec: Int): String {
        val m = sec / 60; val s = sec % 60
        return listOfNotNull(if (m > 0) "${m}분" else null, if (s > 0 || m == 0) "${s}초" else null).joinToString(" ")
    }
}
