package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Test

class RallyPanelFormatTest {
    @Test fun 행군이_가장_길면_기준() = assertEquals("기준", RallyPanelFormat.laterThanFirst(51.0, 51.0))
    @Test fun 짧은_군단은_늦게_출발한_만큼() = assertEquals("+18s", RallyPanelFormat.laterThanFirst(33.0, 51.0))
    @Test fun 소수점_차이() = assertEquals("+5.5s", RallyPanelFormat.laterThanFirst(30.0, 35.5))
    @Test fun 부동소수_오차는_반올림() = assertEquals("+0.3s", RallyPanelFormat.laterThanFirst(10.0, 10.30000000000000004))
    @Test fun 초_정수는_소수점없이() = assertEquals("51", RallyPanelFormat.sec(51.0))
    @Test fun 초_소수는_그대로() = assertEquals("42.5", RallyPanelFormat.sec(42.5))
}
