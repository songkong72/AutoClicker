package com.sejun.autoclicker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteGuardTest {
    @Test
    fun `첫 탭은 지우지 않고 확인 상태가 된다`() {
        val g = DeleteGuard()
        assertFalse(g.onTap("a", 1_000))
        assertTrue(g.isArmed("a", 1_500))
    }

    @Test
    fun `시간 안에 같은 줄을 한 번 더 누르면 지운다`() {
        val g = DeleteGuard()
        g.onTap("a", 1_000)
        assertTrue(g.onTap("a", 3_000))
        assertFalse(g.isArmed("a", 3_000))
    }

    @Test
    fun `시간이 지나면 다시 처음부터 확인한다`() {
        val g = DeleteGuard()
        g.onTap("a", 1_000)
        assertFalse(g.isArmed("a", 4_001))
        assertFalse(g.onTap("a", 4_001))
    }

    @Test
    fun `다른 줄을 누르면 그 줄이 새로 확인 상태가 된다`() {
        val g = DeleteGuard()
        g.onTap("a", 1_000)
        assertFalse(g.onTap("b", 1_500))
        assertFalse(g.isArmed("a", 1_600))
        assertTrue(g.isArmed("b", 1_600))
    }

    @Test
    fun `reset 하면 확인 상태가 풀린다`() {
        val g = DeleteGuard()
        g.onTap("a", 1_000)
        g.reset()
        assertFalse(g.isArmed("a", 1_100))
    }
}
