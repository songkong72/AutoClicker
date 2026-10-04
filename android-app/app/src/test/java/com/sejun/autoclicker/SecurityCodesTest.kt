package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityCodesTest {
    // ---- 초대코드: 파이썬(독립 구현)으로 계산한 기대값과 일치해야 한다 ----

    @Test fun inviteCodeMatchesIndependentHmacImplementation() {
        assertEquals("AC-QP4HZX", InviteCodes.generate("alice", "s3cret"))
    }

    @Test fun inviteCodeIgnoresCaseAndSurroundingSpacesOfUserId() {
        assertEquals(InviteCodes.generate("alice", "s3cret"), InviteCodes.generate("  Alice ", "s3cret"))
    }

    @Test fun differentSecretGivesDifferentCode() {
        assertFalse(InviteCodes.generate("alice", "s3cret") == InviteCodes.generate("alice", "other"))
    }

    @Test fun blankSecretOrUserGivesNoCode() {
        assertEquals("", InviteCodes.generate("alice", ""))
        assertEquals("", InviteCodes.generate("   ", "s3cret"))
    }

    @Test fun verifyAcceptsCodeWithoutDashAndLowercase() {
        assertTrue(InviteCodes.verify("alice", "ac-qp4hzx", "s3cret"))
        assertTrue(InviteCodes.verify("alice", "ACQP4HZX", "s3cret"))
        assertTrue(InviteCodes.verify("alice", "AC-QP4HZX", "s3cret"))
    }

    @Test fun verifyRejectsWrongCodeAndBlankSecret() {
        assertFalse(InviteCodes.verify("alice", "AC-AAAAAA", "s3cret"))
        assertFalse(InviteCodes.verify("alice", "AC-QP4HZX", ""))
    }
}
