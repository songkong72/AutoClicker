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

    @Test fun `소속을 넣은 코드는 그 소속에서만 맞는다`() {
        val wbi = InviteCodes.generate("alice", "s3cret", "2000-WBI")
        assertTrue(wbi.startsWith("AC-") && wbi.length == 9)
        assertTrue(InviteCodes.verify("alice", wbi, "s3cret", "2000-WBI"))
        assertFalse(InviteCodes.verify("alice", wbi, "s3cret", "2000-KOR"))
        assertFalse(InviteCodes.verify("alice", wbi, "s3cret", "2001-WBI"))
        assertFalse(InviteCodes.verify("bob", wbi, "s3cret", "2000-WBI"))
    }

    @Test fun `소속의 대문자와 소문자를 구분한다`() {
        val wbi = InviteCodes.generate("alice", "s3cret", "2000-WBI")
        assertFalse(InviteCodes.verify("alice", wbi, "s3cret", "2000-wbi"))
    }

    @Test fun `소속 없는 예전 코드와 소속을 넣은 코드는 서로 통하지 않는다`() {
        val old = InviteCodes.generate("alice", "s3cret")
        val wbi = InviteCodes.generate("alice", "s3cret", "2000-WBI")
        assertEquals("AC-QP4HZX", old)
        assertFalse(old == wbi)
        assertFalse(InviteCodes.verify("alice", old, "s3cret", "2000-WBI"))
        assertFalse(InviteCodes.verify("alice", wbi, "s3cret"))
    }
}
