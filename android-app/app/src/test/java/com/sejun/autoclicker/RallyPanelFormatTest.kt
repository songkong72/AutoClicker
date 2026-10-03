package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Test

class RallyPanelFormatTest {
    @Test fun 같으면_플마0() = assertEquals("±0s", RallyPanelFormat.diff(0.0))
    @Test fun 더_길면_플러스() = assertEquals("+18s", RallyPanelFormat.diff(18.0))
    @Test fun 더_짧으면_마이너스() = assertEquals("−5.5s", RallyPanelFormat.diff(-5.5))
    @Test fun 부동소수_오차는_반올림() = assertEquals("+0.3s", RallyPanelFormat.diff(0.30000000000000004))
    @Test fun 초_정수는_소수점없이() = assertEquals("51", RallyPanelFormat.sec(51.0))
    @Test fun 초_소수는_그대로() = assertEquals("42.5", RallyPanelFormat.sec(42.5))
}
