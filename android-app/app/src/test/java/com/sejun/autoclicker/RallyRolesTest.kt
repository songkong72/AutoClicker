package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RallyRolesTest {
    private val wbi = RallyGroup("2000", "WBI")
    private val uidA = "a".repeat(28)
    private val uidB = "b".repeat(28)
    private val uidC = "c".repeat(28)

    @Test fun `소속 값과 소속을 서로 바꾼다`() {
        assertEquals("2000-WBI", wbi.id)
        assertEquals(wbi, RallyGroup.fromId("2000-WBI"))
        assertNull(RallyGroup.fromId("2000"))
        assertNull(RallyGroup.fromId("-WBI"))
        assertNull(RallyGroup.fromId("2000-W-BI"))
        assertNull(RallyGroup.fromId(null))
        assertEquals("2000 · WBI", RallyRoles.groupLabel("2000-WBI"))
        assertEquals("소속 없음", RallyRoles.groupLabel(""))
    }

    @Test fun `신청 내용을 서버에 쓰고 다시 읽는다`() {
        val body = RallyRoles.encodeRequest(wbi, "  홍길동 ", " WBI 맹주입니다 ", 1000L)!!
        assertEquals("2000-WBI", body["group"])
        assertEquals("pending", body["status"])
        val req = RallyRoles.decodeRequest(uidA, body)!!
        assertEquals(RepRequest(uidA, "2000-WBI", "홍길동", "WBI 맹주입니다", 1000L), req)
        assertTrue(req.pending)
    }

    @Test fun `캐릭터명이 없으면 신청하지 못하고 한마디는 없어도 된다`() {
        assertNull(RallyRoles.encodeRequest(wbi, "  ", "한마디", 1L))
        val body = RallyRoles.encodeRequest(wbi, "홍길동", "", 1L)!!
        assertFalse(body.containsKey("note"))
    }

    @Test fun `한마디는 60자로 줄인다`() {
        assertEquals(60, RallyRoles.cleanNote("가".repeat(100)).length)
        assertEquals("", RallyRoles.cleanNote(null))
    }

    @Test fun `신청 목록은 기다리는 것만 먼저 온 순서로`() {
        val map = mapOf<String, Any?>(
            uidB to mapOf("group" to "2000-WBI", "name" to "둘", "createdAt" to 20, "status" to "pending"),
            uidA to mapOf("group" to "2000-WBI", "name" to "하나", "createdAt" to 10, "status" to "pending"),
            uidC to mapOf("group" to "2000-WBI", "name" to "셋", "createdAt" to 5, "status" to "rejected", "reason" to "이미 있음"),
            "짧은키" to mapOf("group" to "2000-WBI", "name" to "x", "createdAt" to 1, "status" to "pending"),
            "d".repeat(28) to mapOf("group" to "깨짐", "name" to "x", "createdAt" to 1, "status" to "pending")
        )
        assertEquals(listOf("하나", "둘"), RallyRoles.decodeRequests(map).map { it.name })
        assertEquals("이미 있음", RallyRoles.decodeRequest(uidC, map[uidC])!!.reason)
    }

    @Test fun `승인하면 그 소속의 대표로 적는다`() {
        val rec = RallyRoles.approveRecord(RepRequest(uidA, "2000-WBI", "홍길동", "", 1L), 99L)
        assertEquals(mapOf("registeredAt" to 99L, "name" to "홍길동", "group" to "2000-WBI", "rep" to true), rec)
    }

    @Test fun `이미 대표가 있거나 같은 연맹 신청이 겹치면 알려 준다`() {
        val a = RepRequest(uidA, "2000-WBI", "하나", "", 1L)
        val b = RepRequest(uidB, "2000-WBI", "둘", "", 2L)
        val other = RepRequest(uidC, "1873-KOR", "셋", "", 3L)
        val all = listOf(a, b, other)
        assertEquals("같은 연맹에서 신청이 2건이에요. 한 명만 대표가 돼요.", RallyRoles.requestWarning(a, all, emptyList(), 0L))
        assertNull(RallyRoles.requestWarning(other, all, emptyList(), 0L))
        val day = 86_400_000L
        val admins = listOf(
            AdminEntry("x".repeat(28), "", 1L, name = "박철수", lastSeen = day, appVersion = "1.0.200", group = "1873-KOR", rep = true),
            AdminEntry("y".repeat(28), "", 1L, name = "지휘관", group = "2000-WBI", rep = false)
        )
        assertEquals("지금 대표: 박철수 · 마지막 접속 23일 전. 승인하면 대표가 바뀌어요.", RallyRoles.requestWarning(other, all, admins, 24 * day))
        assertEquals("같은 연맹에서 신청이 2건이에요. 한 명만 대표가 돼요.", RallyRoles.requestWarning(a, all, admins, 24 * day))
    }

    @Test fun `대표가 있는 연맹의 신청을 승인하면 예전 대표가 바뀔 사람이다`() {
        val rep = AdminEntry(uidB, "", 1L, name = "옛 대표", group = "2000-WBI", rep = true)
        val plain = AdminEntry(uidC, "", 1L, name = "지휘관", group = "2000-WBI")
        val elsewhere = AdminEntry("z".repeat(28), "", 1L, name = "남", group = "1873-KOR", rep = true)
        val admins = listOf(rep, plain, elsewhere)
        val newcomer = RepRequest(uidA, "2000-WBI", "새 사람", "", 1L)
        assertEquals(rep, RallyRoles.currentRep(newcomer, admins))
        assertEquals(3, RallyRoles.commandersAfterApprove(newcomer, admins))
        // 이미 그 연맹 지휘관인 사람이 신청하면 사람 수는 늘지 않는다
        assertEquals(2, RallyRoles.commandersAfterApprove(RepRequest(uidC, "2000-WBI", "지휘관", "", 1L), admins))
        // 대표 본인이 다시 신청한 경우는 바꿀 사람이 없다
        assertNull(RallyRoles.currentRep(RepRequest(uidB, "2000-WBI", "옛 대표", "", 1L), admins))
        assertNull(RallyRoles.currentRep(RepRequest(uidA, "3000-NEW", "새 연맹", "", 1L), admins))
    }

    @Test fun `연맹의 지휘관은 대기 중인 코드까지 합쳐 5명까지`() {
        assertTrue(RallyRoles.canAddCommander(3, 1))
        assertFalse(RallyRoles.canAddCommander(3, 2))
        assertFalse(RallyRoles.canAddCommander(5, 0))
    }

    @Test fun `지휘관 코드와 등록에 소속과 대표 여부를 적는다`() {
        val plain = AdminRoster.encodeCode("홍길동", 100L)!!
        assertEquals(setOf("name", "createdAt", "expiresAt"), plain.keys)
        val rep = AdminRoster.encodeCode("홍길동", 100L, group = "2000-WBI", rep = true)!!
        assertEquals("2000-WBI", rep["group"]); assertEquals(true, rep["rep"]); assertFalse(rep.containsKey("by"))
        val byRep = AdminRoster.encodeCode("김철수", 100L, group = "2000-WBI", by = uidA)!!
        assertEquals(uidA, byRep["by"]); assertFalse(byRep.containsKey("rep"))
        assertEquals(mapOf("code" to "AD-22222222", "registeredAt" to 5L), AdminRoster.adminRecord("AD-22222222", 5L))
        assertEquals(
            mapOf("code" to "AD-22222222", "registeredAt" to 5L, "group" to "2000-WBI", "rep" to true),
            AdminRoster.adminRecord("AD-22222222", 5L, "2000-WBI", true)
        )
    }

    @Test fun `서버 명단과 코드에서 소속과 대표 여부를 읽는다`() {
        val admins = AdminRoster.decodeAdmins(mapOf(
            uidA to mapOf("registeredAt" to 1, "group" to "2000-WBI", "rep" to true),
            uidB to mapOf("registeredAt" to 2)
        ))
        assertEquals("2000-WBI", admins[0].group); assertTrue(admins[0].rep)
        assertEquals("", admins[1].group); assertFalse(admins[1].rep)
        val codes = AdminRoster.decodeCodes(mapOf(
            "AD-22222222" to mapOf("name" to "n", "createdAt" to 1, "expiresAt" to 999, "group" to "2000-WBI", "by" to uidA)
        ), 10L)
        assertEquals("2000-WBI", codes[0].group); assertEquals(uidA, codes[0].by); assertFalse(codes[0].rep)
    }

    @Test fun `보내는 글에 소속과 대표 여부가 들어간다`() {
        val plain = AdminRoster.shareMessage("AD-22222222", CodeTtl.DAY)
        assertTrue(plain.startsWith("[오토클리커 Pro 지휘관 초대]\n지휘관 코드: AD-22222222"))
        val rep = AdminRoster.shareMessage("AD-22222222", CodeTtl.DAY, "2000-WBI", true)
        assertTrue(rep.startsWith("[오토클리커 Pro 연맹 대표 초대]\n소속: 2000 서버 · WBI\n지휘관 코드: AD-22222222"))
    }
}
