package com.sejun.autoclicker

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class WriteErrorTest {
    @Test fun permissionDeniedExplainsRosterAndRules() {
        val t = WriteError.explain("Server returned HTTP response code: 401 for URL: https://x")
        assertTrue(t.contains("거절"))
        assertTrue(t.contains("관리자 명단"))
    }

    @Test fun forbiddenIsTreatedTheSame() {
        assertEquals(WriteError.explain("HTTP 401"), WriteError.explain("Server returned HTTP response code: 403 for URL"))
    }

    @Test fun otherErrorsShowShortReason() {
        val t = WriteError.explain("Unable to resolve host \"firebaseio.com\"")
        assertTrue(t.contains("저장하지 못했어요"))
        assertTrue(t.contains("Unable to resolve"))
    }

    @Test fun missingMessageStillSaysSomething() {
        assertTrue(WriteError.explain(null).contains("저장하지 못했어요"))
    }

    @Test fun reasonIsShortened() {
        assertTrue(WriteError.explain("x".repeat(500)).length < 120)
    }
}
