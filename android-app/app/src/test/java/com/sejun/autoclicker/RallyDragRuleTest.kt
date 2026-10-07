package com.sejun.autoclicker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RallyDragRuleTest {
    @Test fun 스크롤_목록_위의_세로_움직임은_패널_이동이_아니다() =
        assertFalse(RallyDragRule.isPanelDrag(moved = 40f, dx = 3f, dy = 40f, slop = 8f, startedInScrollable = true))

    @Test fun 스크롤_목록_위의_가로_움직임은_패널_이동이다() =
        assertTrue(RallyDragRule.isPanelDrag(moved = 40f, dx = 40f, dy = 3f, slop = 8f, startedInScrollable = true))

    @Test fun 목록_밖에서는_세로여도_패널_이동이다() =
        assertTrue(RallyDragRule.isPanelDrag(moved = 40f, dx = 3f, dy = 40f, slop = 8f, startedInScrollable = false))

    @Test fun 슬롭_이하의_움직임은_이동이_아니다() =
        assertFalse(RallyDragRule.isPanelDrag(moved = 5f, dx = 5f, dy = 0f, slop = 8f, startedInScrollable = false))
}
