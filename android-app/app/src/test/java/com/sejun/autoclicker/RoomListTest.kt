package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomListTest {
    private fun team(id: String, name: String, leaderName: String = "", leaderId: String = "", march: Number = 30, excluded: Boolean = false) =
        mapOf("id" to id, "name" to name, "leaderName" to leaderName, "leaderId" to leaderId, "marchSec" to march, "excluded" to excluded)

    private val rooms: Map<String, Any?> = mapOf(
        "ALPHA" to mapOf(
            "teams" to listOf(team("1", "1군", "김민수", "dev1", 28.5), team("2", "2군"), team("3", "3군", "이영희", "dev3", 35, excluded = true)),
            "prepSec" to 10, "waitSec" to 5, "run" to "IDLE", "startSeq" to 0
        ),
        "BETA" to mapOf("teams" to listOf(team("1", "1군")), "prepSec" to 0, "waitSec" to 0, "run" to "RUNNING", "startSeq" to 2)
    )
    private val members: Map<String, Any?> = mapOf(
        "ALPHA" to mapOf("dev1" to mapOf("name" to "김민수"), "dev3" to mapOf("name" to "이영희"), "x9" to mapOf("name" to "박철수")),
        "GHOST" to mapOf("z1" to mapOf("name" to "유령"))
    )

    @Test fun briefLineHasNoMemberCountAndKeepsOrderRunningFirst() {
        val list = RoomList.summarize(rooms, null)
        assertEquals("BETA", list.first().code)
        assertEquals("ALPHA · 군단 3개 (배정 2) · 대기 중", RoomList.lineBrief(list.first { it.code == "ALPHA" }))
        assertEquals("BETA · 군단 1개 (배정 0) · 진행 중", RoomList.lineBrief(list.first { it.code == "BETA" }))
    }

    @Test fun summarizeCountsTeamsAssignedAndMembers() {
        val a = RoomList.summarize(rooms, members).first { it.code == "ALPHA" }
        assertEquals(3, a.teamCount)
        assertEquals(2, a.assigned)
        assertEquals(3, a.members)
        assertEquals("IDLE", a.run)
    }

    @Test fun summarizePutsRunningRoomsFirstThenByCode() {
        assertEquals(listOf("BETA", "ALPHA"), RoomList.summarize(rooms, members).map { it.code })
    }

    @Test fun summarizeKeepsRoomsThatHaveNoMembersAndSkipsMemberOnlyRooms() {
        val codes = RoomList.summarize(rooms, members).map { it.code }
        assertEquals(0, RoomList.summarize(rooms, members).first { it.code == "BETA" }.members)
        assertTrue(codes.none { it == "GHOST" })
    }

    @Test fun summarizeOfNothingIsEmpty() {
        assertTrue(RoomList.summarize(null, null).isEmpty())
        assertTrue(RoomList.summarize(emptyMap(), emptyMap()).isEmpty())
    }

    @Test fun lineShowsCodeTeamsAssignedStateAndMembers() {
        val a = RoomList.summarize(rooms, members).first { it.code == "ALPHA" }
        assertEquals("ALPHA · 군단 3개 (배정 2) · 대기 중 · 명단 3명", RoomList.line(a))
    }

    @Test fun runLabelsAreKorean() {
        assertEquals("대기 중", RoomList.runLabel("IDLE"))
        assertEquals("진행 중", RoomList.runLabel("RUNNING"))
        assertEquals("취소됨", RoomList.runLabel("CANCELLED"))
        assertEquals("ODD", RoomList.runLabel("ODD"))
    }

    @Test fun detailListsTeamsLeadersMarchTimesAndMemberNames() {
        val text = RoomList.detail("ALPHA", rooms["ALPHA"] as Map<*, *>, members["ALPHA"] as Map<*, *>)
        assertTrue(text, text.contains("방 ALPHA"))
        assertTrue(text, text.contains("상태 대기 중 · 준비 10초 · 대기 5초"))
        assertTrue(text, text.contains("1. 1군 · 팀장 김민수 · 행군 28.5초"))
        assertTrue(text, text.contains("2. 2군 · 팀장 미배정 · 행군 30초"))
        assertTrue(text, text.contains("3. 3군 · 팀장 이영희 · 행군 35초 · 제외"))
        assertTrue(text, text.contains("명단 3명: "))
        assertTrue(text, text.contains("박철수"))
    }

    @Test fun detailOfRoomWithoutMembersSaysZero() {
        val text = RoomList.detail("BETA", rooms["BETA"] as Map<*, *>, null)
        assertTrue(text, text.contains("명단 0명"))
        assertTrue(text, text.contains("상태 진행 중"))
    }
}
