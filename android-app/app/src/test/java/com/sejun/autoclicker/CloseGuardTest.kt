package com.sejun.autoclicker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloseGuardTest {
    @Test fun closesAtOnceWhenNotRunning() {
        assertTrue(CloseGuard().onTap(running = false, nowMs = 1_000L))
    }

    @Test fun firstTapWhileRunningOnlyWarns() {
        assertFalse(CloseGuard().onTap(running = true, nowMs = 1_000L))
    }

    @Test fun secondTapWithinWindowCloses() {
        val g = CloseGuard(windowMs = 3_000L)
        assertFalse(g.onTap(true, 1_000L))
        assertTrue(g.onTap(true, 3_900L))
    }

    @Test fun secondTapAfterWindowWarnsAgain() {
        val g = CloseGuard(windowMs = 3_000L)
        assertFalse(g.onTap(true, 1_000L))
        assertFalse(g.onTap(true, 4_500L))
        assertTrue(g.onTap(true, 5_000L))
    }

    @Test fun closingResetsSoNextRunWarnsFirst() {
        val g = CloseGuard(windowMs = 3_000L)
        assertFalse(g.onTap(true, 1_000L))
        assertTrue(g.onTap(true, 2_000L))
        assertFalse(g.onTap(true, 2_500L))
    }

    @Test fun stoppingTheRunClearsAnArmedWarning() {
        val g = CloseGuard(windowMs = 3_000L)
        assertFalse(g.onTap(true, 1_000L))
        assertTrue(g.onTap(false, 1_500L))
        assertFalse(g.onTap(true, 1_800L))
    }
}
