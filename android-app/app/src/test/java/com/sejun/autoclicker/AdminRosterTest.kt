package com.sejun.autoclicker

import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminRosterTest {
    @Test fun newCodeHasPrefixAndEightSafeCharacters() {
        val code = AdminRoster.newCode(Random(1))
        assertTrue(code, Regex("^AD-[2-9A-HJ-NP-Z]{8}$").matches(code))
    }

    @Test fun newCodesDiffer() {
        val r = Random(7)
        assertTrue(AdminRoster.newCode(r) != AdminRoster.newCode(r))
    }

    @Test fun normalizeAcceptsLowercaseSpacesAndMissingDash() {
        assertEquals("AD-ABCD2345", AdminRoster.normalizeCode(" ad-abcd2345 "))
        assertEquals("AD-ABCD2345", AdminRoster.normalizeCode("ADABCD2345"))
    }

    @Test fun normalizeRejectsWrongShapeOrConfusingCharacters() {
        assertNull(AdminRoster.normalizeCode(""))
        assertNull(AdminRoster.normalizeCode("AC-ABCD23"))        // 팀장 초대코드
        assertNull(AdminRoster.normalizeCode("AD-ABCD234"))       // 7자
        assertNull(AdminRoster.normalizeCode("AD-ABCD23456"))     // 9자
        assertNull(AdminRoster.normalizeCode("AD-ABCD23O1"))      // 0, O, 1, I 는 쓰지 않는다
    }

    @Test fun isLooksLikeAdminCodeOnlyForAdPrefix() {
        assertTrue(AdminRoster.looksLikeCode("ad-abcd2345"))
        assertFalse(AdminRoster.looksLikeCode("AC-ABCDEF"))
        assertFalse(AdminRoster.looksLikeCode("비밀번호123"))
    }

    @Test fun codeExpiresAfterTwentyFourHours() {
        val t0 = 1_000_000L
        assertFalse(AdminRoster.isExpired(t0 + AdminRoster.CODE_TTL_MS - 1, t0))
        assertTrue(AdminRoster.isExpired(t0 + AdminRoster.CODE_TTL_MS, t0))
    }

    @Test fun cleanNameTrimsCollapsesAndCapsAtTwenty() {
        assertEquals("김 민수", AdminRoster.cleanName("  김   민수 "))
        assertEquals(20, AdminRoster.cleanName("가".repeat(30)).length)
        assertEquals("", AdminRoster.cleanName("   "))
    }

    @Test fun validUidIsFirebaseStyle() {
        assertTrue(AdminRoster.isValidUid("aB3dEf6hIj9kLm2nOp5qRs8tUv1w"))
        assertFalse(AdminRoster.isValidUid(""))
        assertFalse(AdminRoster.isValidUid("a/b"))
        assertFalse(AdminRoster.isValidUid("x".repeat(80)))
    }

    @Test fun decodeAdminsSortsByRegistrationAndSkipsBrokenOnes() {
        val uidA = "a".repeat(28); val uidB = "b".repeat(28)
        val map = mapOf<String, Any?>(
            uidB to mapOf("registeredAt" to 200L, "code" to "AD-ABCD2345"),
            uidA to mapOf("registeredAt" to 100L),
            "bad/id" to mapOf("registeredAt" to 1L),
            "c".repeat(28) to "not a map"
        )
        val list = AdminRoster.decodeAdmins(map)
        assertEquals(listOf(uidA, uidB), list.map { it.uid })
        assertEquals("AD-ABCD2345", list[1].code)
        assertEquals("", list[0].code)
    }

    @Test fun decodeAdminsOfNullIsEmpty() = assertEquals(emptyList<AdminEntry>(), AdminRoster.decodeAdmins(null))

    @Test fun decodeCodesDropsExpiredUnusedKeepsUsedOnes() {
        val now = 10_000_000L
        val map = mapOf<String, Any?>(
            "AD-ABCD2345" to mapOf("name" to "김민수", "createdAt" to now - 1000, "expiresAt" to now + 1000),
            "AD-WXYZ6789" to mapOf("name" to "이영희", "createdAt" to now - 5000, "expiresAt" to now - 1, "usedBy" to "u"),
            "AD-HJKM2345" to mapOf("name" to "박철수", "createdAt" to now - 100, "expiresAt" to now + 500, "usedBy" to "u1"),
            "AD-QRST2345" to mapOf("name" to "최지우", "createdAt" to now - 9000, "expiresAt" to now - 1)
        )
        val list = AdminRoster.decodeCodes(map, now)
        assertEquals(listOf("AD-HJKM2345", "AD-ABCD2345", "AD-WXYZ6789"), list.map { it.code }) // 최근 만든 순, 기한 지난 미사용 코드는 없음
        assertEquals(true, list.first { it.code == "AD-HJKM2345" }.used)
        assertEquals(false, list.first { it.code == "AD-ABCD2345" }.used)
        assertEquals("이영희", list.first { it.code == "AD-WXYZ6789" }.name)
    }

    @Test fun encodeCodeCarriesNameAndExpiry() {
        val m = AdminRoster.encodeCode("  김민수 ", createdAt = 1_000L)!!
        assertEquals("김민수", m["name"])
        assertEquals(1_000L, m["createdAt"])
        assertEquals(1_000L + AdminRoster.CODE_TTL_MS, m["expiresAt"])
    }

    @Test fun encodeCodeRejectsEmptyName() = assertNull(AdminRoster.encodeCode("   ", 1L))

    @Test fun adminRecordCarriesTheCodeAndTime() {
        val rec = AdminRoster.adminRecord("AD-ABCD2345", registeredAt = 5L)
        assertEquals("AD-ABCD2345", rec["code"])
        assertEquals(5L, rec["registeredAt"])
        assertEquals(2, rec.size)
    }
}
