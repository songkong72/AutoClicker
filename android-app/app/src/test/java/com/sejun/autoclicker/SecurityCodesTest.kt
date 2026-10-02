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

    // ---- 관리자 비밀번호: 해시만 저장·비교한다 ----

    @Test fun adminHashMatchesIndependentSha256() {
        assertEquals("6f0f66e8b89655798ddee03104317c52cfe6aa71e7937f01f4744398cb4c32c2", AdminAuth.hash("pw1234", "salt1"))
    }

    @Test fun adminMatchesOnlyCorrectPassword() {
        val h = AdminAuth.hash("pw1234", "salt1")
        assertTrue(AdminAuth.matches("pw1234", h, "salt1"))
        assertFalse(AdminAuth.matches("pw1235", h, "salt1"))
    }

    @Test fun adminDeniedWhenNoHashConfiguredOrBlankInput() {
        assertFalse(AdminAuth.matches("anything", "", "salt1"))
        assertFalse(AdminAuth.matches("", AdminAuth.hash("", "salt1"), "salt1"))
    }

    @Test fun adminOldDefaultPasswordIsNotAccepted() {
        val h = AdminAuth.hash("pw1234", "salt1")
        assertFalse(AdminAuth.matches("admin1234!", h, "salt1"))
    }
}
