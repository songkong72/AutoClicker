package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Test

class HunterFirePolicyTest {
    @Test fun firesOnFreshPressWithTargetsSaved() =
        assertEquals(true, HunterFirePolicy.allow(hasTargets = true, repeatCount = 0, nowMs = 10_000, lastFireMs = 0))

    @Test fun neverFiresWithoutSavedTargets() =
        assertEquals(false, HunterFirePolicy.allow(hasTargets = false, repeatCount = 0, nowMs = 10_000, lastFireMs = 0))

    @Test fun heldKeyRepeatsAreIgnored() =
        assertEquals(false, HunterFirePolicy.allow(hasTargets = true, repeatCount = 3, nowMs = 10_000, lastFireMs = 0))

    @Test fun pressesWithinCooldownAreIgnored() {
        assertEquals(false, HunterFirePolicy.allow(true, 0, nowMs = 10_000 + 1_499, lastFireMs = 10_000))
        assertEquals(true, HunterFirePolicy.allow(true, 0, nowMs = 10_000 + 1_500, lastFireMs = 10_000))
    }
}
