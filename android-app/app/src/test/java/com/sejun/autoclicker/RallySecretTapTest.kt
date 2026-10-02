package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Test

class RallySecretTapTest {
    @Test fun fiveQuickTapsTrigger() {
        val s = RallySecretTap()
        val hits = (0 until 5).map { s.tap(1000L + it * 300L) }
        assertEquals(listOf(false, false, false, false, true), hits)
    }

    @Test fun slowTapsRestartTheCount() {
        val s = RallySecretTap()
        listOf(0L, 300L, 600L, 900L).forEach { s.tap(it) }
        assertEquals(false, s.tap(900L + 900L)) // 800ms를 넘기면 다시 1번째
        assertEquals(false, s.tap(1800L + 300L))
    }

    @Test fun countResetsAfterTriggering() {
        val s = RallySecretTap()
        (0 until 5).forEach { s.tap(it * 100L) }
        assertEquals(false, s.tap(600L))
    }
}
