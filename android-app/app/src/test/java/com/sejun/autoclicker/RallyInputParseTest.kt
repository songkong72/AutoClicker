package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RallyInputParseTest {
    private val d = 1e-9

    @Test fun wholeSeconds() = assertEquals(50.0, RallyInputParse.marchSeconds("50")!!, d)

    @Test fun decimalSeconds() = assertEquals(47.5, RallyInputParse.marchSeconds("47.5")!!, d)

    @Test fun surroundingSpacesAreIgnored() = assertEquals(12.0, RallyInputParse.marchSeconds("  12 ")!!, d)

    @Test fun commaDecimalSeparatorIsAccepted() = assertEquals(47.5, RallyInputParse.marchSeconds("47,5")!!, d)

    @Test fun roundsToOneTenth() = assertEquals(47.5, RallyInputParse.marchSeconds("47.54")!!, d)

    @Test fun trailingSecondsUnitIsAccepted() = assertEquals(30.0, RallyInputParse.marchSeconds("30초")!!, d)

    @Test fun zeroIsValid() = assertEquals(0.0, RallyInputParse.marchSeconds("0")!!, d)

    @Test fun emptyIsRejected() = assertNull(RallyInputParse.marchSeconds(""))

    @Test fun lettersAreRejected() = assertNull(RallyInputParse.marchSeconds("abc"))

    @Test fun negativeIsRejected() = assertNull(RallyInputParse.marchSeconds("-3"))

    @Test fun overMaximumIsRejected() = assertNull(RallyInputParse.marchSeconds("601"))

    @Test fun maximumItselfIsAccepted() = assertEquals(600.0, RallyInputParse.marchSeconds("600")!!, d)

    @Test fun nanAndInfinityAreRejected() {
        assertNull(RallyInputParse.marchSeconds("NaN"))
        assertNull(RallyInputParse.marchSeconds("Infinity"))
    }

    // 클릭 보정: 초 단위로 입력/표시한다. 내부 값은 ms.
    @Test fun correctionSecondsToMs() {
        assertEquals(-1500, RallyInputParse.correctionMs("-1.5"))
        assertEquals(300, RallyInputParse.correctionMs("+0.3"))
        assertEquals(2000, RallyInputParse.correctionMs("2초"))
        assertEquals(-200, RallyInputParse.correctionMs("−0.2")) // 화면에 쓰는 유니코드 마이너스도 받는다
        assertEquals(0, RallyInputParse.correctionMs("0"))
        assertEquals(-5000, RallyInputParse.correctionMs("-5"))
    }

    @Test fun correctionRejectsOutOfRangeOrGarbage() {
        assertNull(RallyInputParse.correctionMs("5.1"))
        assertNull(RallyInputParse.correctionMs("-5.1"))
        assertNull(RallyInputParse.correctionMs("abc"))
        assertNull(RallyInputParse.correctionMs(""))
    }

    @Test fun correctionIsShownInSeconds() {
        assertEquals("+0.3초", RallyInputParse.formatCorrection(300))
        assertEquals("−1.5초", RallyInputParse.formatCorrection(-1500))
        assertEquals("0초", RallyInputParse.formatCorrection(0))
        assertEquals("+0.01초", RallyInputParse.formatCorrection(10)) // 예전 10ms 단위 값도 잘리지 않게
    }
}
