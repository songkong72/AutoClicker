package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Test

class RallyTimelineScaleTest {
    private val d = 1e-9
    // prep 15, wait 300, marches 50/30/10 -> click 15/35/55, depart 315/335/355, arrive 365
    private val s = RallyTimelineScale(50.0)

    @Test fun gatherIsCompressedToHalfOfMaxMarch() {
        assertEquals(25.0, s.gatherVisual, d)
    }

    @Test fun gatherHasAMinimumLength() {
        assertEquals(10.0, RallyTimelineScale(4.0).gatherVisual, d)
    }

    @Test fun allTeamsShareTheSameTotalSoBarsEndTogether() {
        val t3 = s.total(15.0, 315.0, 365.0)
        val t2 = s.total(35.0, 335.0, 365.0)
        val t1 = s.total(55.0, 355.0, 365.0)
        assertEquals(90.0, t3, d)
        assertEquals(t3, t2, d)
        assertEquals(t3, t1, d)
    }

    @Test fun beforeClickMapsOneToOne() {
        assertEquals(7.5, s.map(15.0, 315.0, 365.0, 7.5), d)
    }

    @Test fun duringGatherMapsProportionallyIntoCompressedSpan() {
        assertEquals(27.5, s.map(15.0, 315.0, 365.0, 165.0), d) // 중간 지점
    }

    @Test fun duringMarchMapsOneToOneAfterGather() {
        assertEquals(65.0, s.map(15.0, 315.0, 365.0, 340.0), d)
    }

    @Test fun pastArrivalClampsToTotal() {
        assertEquals(90.0, s.map(15.0, 315.0, 365.0, 999.0), d)
    }

    @Test fun negativeElapsedClampsToZero() {
        assertEquals(0.0, s.map(15.0, 315.0, 365.0, -3.0), d)
    }

    @Test fun zeroLengthGatherDoesNotDivideByZero() {
        assertEquals(15.0, s.map(15.0, 15.0, 65.0, 15.0), d)
    }
}
