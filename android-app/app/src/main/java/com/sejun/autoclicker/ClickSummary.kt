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

    /** 500 → "0.5", 1000 → "1", 250 → "0.25" */
    private fun seconds(ms: Long): String =
        java.math.BigDecimal(ms).movePointLeft(3).stripTrailingZeros().toPlainString()

    /** 90 → "1분 30초", 60 → "1분", 45 → "45초" */
    private fun duration(sec: Int): String {
        val m = sec / 60; val s = sec % 60
        return listOfNotNull(if (m > 0) "${m}분" else null, if (s > 0 || m == 0) "${s}초" else null).joinToString(" ")
    }
}
