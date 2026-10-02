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
}
