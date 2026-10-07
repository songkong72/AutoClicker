package com.sejun.autoclicker

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** 화면에 띄우는 초 단위 시계의 글자와 갱신 간격. 안드로이드 클래스를 쓰지 않아 단위 테스트가 된다. */
internal object ClockText {
    /** "21:33:05" 모양. [utc]면 앞에 "UTC "를 붙여 게임 화면의 UTC 시각과 견줄 수 있게 한다. */
    fun format(nowMs: Long, zone: TimeZone, utc: Boolean): String {
        val f = SimpleDateFormat("HH:mm:ss", Locale.US)
        f.timeZone = if (utc) TimeZone.getTimeZone("UTC") else zone
        val t = f.format(Date(nowMs))
        return if (utc) "UTC $t" else t
    }

    /** 다음 초가 바뀌는 순간까지 남은 ms(1~1000). 초 경계에 맞춰 갱신해 숫자가 밀려 보이지 않게 한다. */
    fun delayToNextSecond(nowMs: Long): Long = 1000L - (((nowMs % 1000L) + 1000L) % 1000L)
}
