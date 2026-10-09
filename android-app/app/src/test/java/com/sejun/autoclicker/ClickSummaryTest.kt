package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Test

class ClickSummaryTest {
    @Test fun 무한_반복() = assertEquals("0.5초마다 · 무한 반복", ClickSummary.text(500, RepeatMode.INFINITE, 100, 60))

    @Test fun 횟수() = assertEquals("0.1초마다 · 300회", ClickSummary.text(100, RepeatMode.COUNT, 300, 60))

    @Test fun 시간은_분과_초로() = assertEquals("1초마다 · 1분 30초 동안", ClickSummary.text(1000, RepeatMode.TIMER, 100, 90))

    @Test fun 딱_떨어지는_분은_초를_적지_않는다() = assertEquals("0.25초마다 · 3분 동안", ClickSummary.text(250, RepeatMode.TIMER, 100, 180))

    @Test fun 일_분이_안_되면_초만() = assertEquals("0.2초마다 · 45초 동안", ClickSummary.text(200, RepeatMode.TIMER, 100, 45))
}
