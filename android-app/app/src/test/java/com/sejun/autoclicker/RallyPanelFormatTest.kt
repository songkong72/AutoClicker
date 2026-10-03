package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Test

class RallyPanelFormatTest {
    private fun t(march: Double, adj: Int = 0, ex: Boolean = false) = LagInput(march, adj, ex)

    @Test fun 행군이_가장_긴_군단이_기준() =
        assertEquals(listOf("기준", "+18s", "+21s"), RallyPanelFormat.lagLabels(listOf(t(51.0), t(33.0), t(30.0))))

    @Test fun 소수점_차이() =
        assertEquals(listOf("기준", "+5.5s"), RallyPanelFormat.lagLabels(listOf(t(35.5), t(30.0))))

    @Test fun 관리자_보정이_시간차이에_들어간다() =
        assertEquals(listOf("기준", "+19.5s"), RallyPanelFormat.lagLabels(listOf(t(51.0), t(33.0, 1500))))

    @Test fun 보정이_커서_먼저_누르게_되면_그_군단이_기준() =
        assertEquals(listOf("+1s", "기준"), RallyPanelFormat.lagLabels(listOf(t(51.0), t(48.0, -4000))))

    @Test fun 제외된_군단은_계산에서도_표시에서도_빠진다() =
        assertEquals(listOf("기준", "+18s", ""), RallyPanelFormat.lagLabels(listOf(t(51.0), t(33.0), t(60.0, 0, true))))

    @Test fun 전부_제외면_모두_빈칸() =
        assertEquals(listOf("", ""), RallyPanelFormat.lagLabels(listOf(t(51.0, 0, true), t(33.0, 0, true))))

    @Test fun 부동소수_오차는_반올림() =
        assertEquals(listOf("기준", "+0.3s"), RallyPanelFormat.lagLabels(listOf(t(10.30000000000000004), t(10.0))))

    @Test fun 초_정수는_소수점없이() = assertEquals("51", RallyPanelFormat.sec(51.0))
    @Test fun 초_소수는_그대로() = assertEquals("42.5", RallyPanelFormat.sec(42.5))
}
