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
        assertNull(AdminRoster.normalizeCode("AC-ABCD23"))        // 집결장 초대코드
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

    @Test fun exportTextListsAdminsByLabelAndPendingCodes() {
        val uid = "A".repeat(22) + "xyz789"
        val admins = listOf(AdminEntry(uid, "AD-ABCD2345", 1L), AdminEntry("B".repeat(28), "", 2L))
        val codes = listOf(
            AdminCode("AD-ABCD2345", "김민수", 0L, 100L, used = true),
            AdminCode("AD-EFGH6789", "이영희", 0L, 100L, used = false),
            AdminCode("AD-JKLM2345", "만료", 0L, 10L, used = false)
        )
        val text = AdminRoster.exportText(admins, codes, nowMs = 50L)
        assertTrue(text, text.contains("등록된 관리자 2명"))
        assertTrue(text, text.contains("1. 김민수 · ID …xyz789"))
        assertTrue(text, text.contains("2. 직접 등록 · ID …BBBBBB"))
        assertTrue(text, text.contains("대기 중인 코드 1개"))
        assertTrue(text, text.contains("AD-EFGH6789 · 이영희"))
        assertFalse(text, text.contains("AD-JKLM2345"))
    }

    @Test fun exportTextSaysNoneWhenEmpty() {
        val text = AdminRoster.exportText(emptyList(), emptyList(), nowMs = 0L)
        assertTrue(text, text.contains("등록된 관리자 0명"))
        assertTrue(text, text.contains("대기 중인 코드 0개"))
    }

    // ---- 관리자 모드 점검: 서버가 모른다고 하면 건드리지 않고, 둘 다 아니라고 할 때만 푼다 ----

    @Test fun reconcileKeepsServerAdminAndOwner() {
        assertEquals(AdminModeFix.KEEP, AdminRoster.reconcile(viaServer = true, owner = Check.NO, admin = Check.YES))
        assertEquals(AdminModeFix.KEEP, AdminRoster.reconcile(viaServer = true, owner = Check.YES, admin = Check.NO))
    }

    @Test fun reconcileMarksLegacyDeviceWhenServerKnowsIt() {
        assertEquals(AdminModeFix.MARK_SERVER, AdminRoster.reconcile(viaServer = false, owner = Check.YES, admin = Check.NO))
        assertEquals(AdminModeFix.MARK_SERVER, AdminRoster.reconcile(viaServer = false, owner = Check.NO, admin = Check.YES))
    }

    @Test fun reconcileClearsOnlyWhenServerSaysNeither() {
        assertEquals(AdminModeFix.CLEAR, AdminRoster.reconcile(viaServer = true, owner = Check.NO, admin = Check.NO))
        assertEquals(AdminModeFix.CLEAR, AdminRoster.reconcile(viaServer = false, owner = Check.NO, admin = Check.NO))
    }

    @Test fun reconcileKeepsWhenServerAnswerIsUnknown() {
        assertEquals(AdminModeFix.KEEP, AdminRoster.reconcile(viaServer = false, owner = Check.UNKNOWN, admin = Check.NO))
        assertEquals(AdminModeFix.KEEP, AdminRoster.reconcile(viaServer = true, owner = Check.NO, admin = Check.UNKNOWN))
        assertEquals(AdminModeFix.KEEP, AdminRoster.reconcile(viaServer = false, owner = Check.UNKNOWN, admin = Check.UNKNOWN))
    }

    // ---- 관리 화면 개선: 여러 코드, 유효시간, 이름, 접속 표시, 정리 ----

    @Test fun parseNamesSplitsLinesAndCommasAndCleans() {
        assertEquals(listOf("김민수", "이 영희", "박철수"), AdminRoster.parseNames("  김민수 \n이   영희,박철수\n\n  "))
    }

    @Test fun parseNamesDropsDuplicatesAndLimitsCount() {
        assertEquals(listOf("가", "나"), AdminRoster.parseNames("가\n나\n가"))
        val many = (1..15).joinToString("\n") { "이름$it" }
        assertEquals(AdminRoster.MAX_BATCH, AdminRoster.parseNames(many).size)
    }

    @Test fun parseNamesOfBlankIsEmpty() {
        assertTrue(AdminRoster.parseNames("  \n , ").isEmpty())
    }

    @Test fun encodeCodeUsesChosenLifetime() {
        val m = AdminRoster.encodeCode("김민수", 1_000L, CodeTtl.HOUR.ms)!!
        assertEquals(1_000L + 3_600_000L, m["expiresAt"])
        val d = AdminRoster.encodeCode("김민수", 1_000L)!!
        assertEquals(1_000L + AdminRoster.CODE_TTL_MS, d["expiresAt"])
    }

    @Test fun lifetimeChoicesAreOneHourOneDayOneWeek() {
        assertEquals(listOf(3_600_000L, 86_400_000L, 604_800_000L), CodeTtl.values().map { it.ms })
        assertEquals(CodeTtl.DAY.ms, AdminRoster.CODE_TTL_MS)
    }

    @Test fun shareMessageNamesCodeAndLifetime() {
        val m = AdminRoster.shareMessage("AD-ABCD2345", CodeTtl.WEEK)
        assertTrue(m, m.contains("AD-ABCD2345"))
        assertTrue(m, m.contains("7일"))
    }

    @Test fun batchShareHasOneSectionPerPerson() {
        val text = AdminRoster.batchShare(listOf("김민수" to "AD-ABCD2345", "이영희" to "AD-EFGH6789"), CodeTtl.DAY)
        assertTrue(text, text.contains("김민수") && text.contains("AD-ABCD2345"))
        assertTrue(text, text.contains("이영희") && text.contains("AD-EFGH6789"))
    }

    @Test fun decodeAdminsReadsNameLastSeenAndVersion() {
        val uid = "A".repeat(28)
        val a = AdminRoster.decodeAdmins(mapOf(uid to mapOf("code" to "AD-ABCD2345", "registeredAt" to 5, "name" to " 김  민수 ", "lastSeen" to 99, "appVersion" to "1.0.57"))).single()
        assertEquals("김 민수", a.name)
        assertEquals(99L, a.lastSeen)
        assertEquals("1.0.57", a.appVersion)
    }

    @Test fun decodeAdminsDefaultsWhenFieldsMissing() {
        val a = AdminRoster.decodeAdmins(mapOf("B".repeat(28) to mapOf("code" to "", "registeredAt" to 1))).single()
        assertEquals("", a.name)
        assertEquals(0L, a.lastSeen)
        assertEquals("", a.appVersion)
    }

    @Test fun labelPrefersStoredNameThenCodeNameThenDirect() {
        val codes = listOf(AdminCode("AD-ABCD2345", "코드이름", 0L, 100L, used = true))
        assertEquals("저장이름", AdminRoster.labelOf(AdminEntry("A".repeat(28), "AD-ABCD2345", 1L, name = "저장이름"), codes))
        assertEquals("코드이름", AdminRoster.labelOf(AdminEntry("A".repeat(28), "AD-ABCD2345", 1L), codes))
        assertEquals("직접 등록", AdminRoster.labelOf(AdminEntry("A".repeat(28), "", 1L), codes))
    }

    @Test fun nameBackfillCopiesCodeNameOnlyForUnnamedAdmins() {
        val codes = listOf(
            AdminCode("AD-ABCD2345", "김민수", 0L, 100L, used = true),
            AdminCode("AD-EFGH6789", "이영희", 0L, 100L, used = true)
        )
        val admins = listOf(
            AdminEntry("A".repeat(28), "AD-ABCD2345", 1L),
            AdminEntry("B".repeat(28), "AD-EFGH6789", 2L, name = "이미 있음"),
            AdminEntry("C".repeat(28), "", 3L)
        )
        assertEquals(mapOf("A".repeat(28) to "김민수"), AdminRoster.nameBackfill(admins, codes))
    }

    @Test fun purgeableListsUsedAndExpiredCodesOnly() {
        val raw = mapOf<String, Any?>(
            "AD-ABCD2345" to mapOf("expiresAt" to 100, "usedBy" to "u"),
            "AD-EFGH6789" to mapOf("expiresAt" to 10),
            "AD-JKLM2345" to mapOf("expiresAt" to 100),
            "엉뚱한키" to mapOf("expiresAt" to 10)
        )
        assertEquals(setOf("AD-ABCD2345", "AD-EFGH6789"), AdminRoster.purgeable(raw, nowMs = 50L).toSet())
    }

    @Test fun activityTextShowsRelativeTimeAndVersion() {
        val now = 10L * 24 * 3_600_000
        assertEquals("방금 전 · v1.0.57", AdminRoster.activityText(now - 20_000, "1.0.57", now))
        assertEquals("5분 전 · v1.0.57", AdminRoster.activityText(now - 5 * 60_000, "1.0.57", now))
        assertEquals("3시간 전 · v1.0.57", AdminRoster.activityText(now - 3 * 3_600_000, "1.0.57", now))
        assertEquals("2일 전 · v1.0.57", AdminRoster.activityText(now - 2 * 24 * 3_600_000, "1.0.57", now))
    }

    @Test fun activityTextWhenNeverSeenOrVersionUnknown() {
        assertEquals("접속 기록 없음 · 버전 알 수 없음", AdminRoster.activityText(0L, "", 1_000L))
        assertEquals("방금 전 · 버전 알 수 없음", AdminRoster.activityText(1_000L, "", 1_000L))
    }

    @Test fun exportTextUsesStoredNames() {
        val admins = listOf(AdminEntry("A".repeat(28), "", 1L, name = "저장이름"))
        val text = AdminRoster.exportText(admins, emptyList(), nowMs = 0L)
        assertTrue(text, text.contains("1. 저장이름 · ID …AAAAAA"))
    }
}
