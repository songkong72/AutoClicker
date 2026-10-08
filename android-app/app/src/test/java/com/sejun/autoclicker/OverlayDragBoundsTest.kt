package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayDragBoundsTest {
    // 화면 1080x2000, 창 800x1000 → 가운데에서 좌우 140, 위아래 500까지 움직일 수 있다
    private fun at(x: Int, y: Int) = OverlayDragBounds.centered(x, y, 1080, 2000, 800, 1000)

    @Test fun 화면_안에서는_끈_만큼_움직인다() = assertEquals(100 to -300, at(100, -300))

    @Test fun 오른쪽_아래로_넘기면_가장자리에서_멈춘다() = assertEquals(140 to 500, at(900, 5000))

    @Test fun 왼쪽_위로_넘기면_가장자리에서_멈춘다() = assertEquals(-140 to -500, at(-900, -5000))

    @Test fun 창이_화면보다_크면_가운데에_둔다() =
        assertEquals(0 to 0, OverlayDragBounds.centered(50, 70, 300, 400, 320, 600))
}
