package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class RallyAssignTest {
    private fun room(run: String = "IDLE") = RallyRoomDoc(
        listOf(
            RallyTeamDoc("t1", "1군", marchSec = 10.0),
            RallyTeamDoc("t2", "2군", marchSec = 30.0),
            RallyTeamDoc("t3", "3군", marchSec = 50.0)
        ),
        15.0, 300.0, run, 0L
    )

    // ---- 배정 ----

    @Test fun assignSetsIdAndCharacterNameOnThatTeamOnly() {
        val d = RallyRoomEdit.assignLeader(room(), "t2", "m-aaa", "눈보라")
        assertEquals("m-aaa", d.teams[1].leaderId)
        assertEquals("눈보라", d.teams[1].leaderName)
        assertEquals("", d.teams[0].leaderId)
        assertEquals("", d.teams[2].leaderId)
    }

    @Test fun assigningSamePersonElsewhereMovesThem() {
        val first = RallyRoomEdit.assignLeader(room(), "t1", "m-aaa", "눈보라")
        val moved = RallyRoomEdit.assignLeader(first, "t3", "m-aaa", "눈보라")
        assertEquals("", moved.teams[0].leaderId)
        assertEquals("", moved.teams[0].leaderName)
        assertEquals("m-aaa", moved.teams[2].leaderId)
    }

    @Test fun assigningSomeoneReplacesThePreviousLeaderOfThatTeam() {
        val first = RallyRoomEdit.assignLeader(room(), "t1", "m-aaa", "눈보라")
        val replaced = RallyRoomEdit.assignLeader(first, "t1", "m-bbb", "불꽃")
        assertEquals("m-bbb", replaced.teams[0].leaderId)
        assertEquals("불꽃", replaced.teams[0].leaderName)
    }

    @Test fun unassignClearsIdAndName() {
        val d = RallyRoomEdit.unassignLeader(RallyRoomEdit.assignLeader(room(), "t1", "m-aaa", "눈보라"), "t1")
        assertEquals("", d.teams[0].leaderId)
        assertEquals("", d.teams[0].leaderName)
    }

    @Test fun assignmentIsLockedWhileRunning() {
        val running = room("RUNNING")
        assertSame(running, RallyRoomEdit.assignLeader(running, "t1", "m-aaa", "눈보라"))
        assertSame(running, RallyRoomEdit.unassignLeader(running, "t1"))
    }

    @Test fun assigningToUnknownTeamChangesNothing() {
        val r = room()
        assertEquals(r, RallyRoomEdit.assignLeader(r, "t9", "m-aaa", "눈보라"))
    }

    @Test fun blankMemberIdIsIgnored() {
        val r = room()
        assertEquals(r, RallyRoomEdit.assignLeader(r, "t1", "  ", "눈보라"))
    }

    @Test fun teamIdOfFindsMyAssignedTeam() {
        val d = RallyRoomEdit.assignLeader(room(), "t3", "m-aaa", "눈보라")
        assertEquals("t3", RallyRoomEdit.teamIdOf(d, "m-aaa"))
        assertEquals("", RallyRoomEdit.teamIdOf(d, "m-zzz"))
        assertEquals("", RallyRoomEdit.teamIdOf(d, ""))
    }

    // ---- 문서 코덱 ----

    @Test fun codecRoundTripKeepsLeaderId() {
        val d = RallyRoomEdit.assignLeader(room(), "t2", "m-aaa", "눈보라")
        assertEquals(d, RallyRoomCodec.decode(RallyRoomCodec.encode(d)))
    }

    @Test fun oldDocumentWithoutLeaderIdDecodesAsUnassigned() {
        val old = mapOf<String, Any?>(
            "teams" to listOf(mapOf("id" to "t1", "name" to "1군", "marchSec" to 10, "leaderName" to "옛이름")),
            "prepSec" to 15, "waitSec" to 300, "run" to "IDLE", "startSeq" to 0
        )
        assertEquals("", RallyRoomCodec.decode(old).teams[0].leaderId)
    }

    // ---- 명단 ----

    @Test fun cleanNameTrimsCollapsesAndLimitsLength() {
        assertEquals("눈 보라", RallyRoster.cleanName("  눈   보라  "))
        assertEquals(20, RallyRoster.cleanName("가".repeat(50)).length)
        assertEquals("", RallyRoster.cleanName("   "))
    }

    @Test fun rosterDecodeSortsByNameAndSkipsBrokenEntries() {
        val raw = mapOf<String, Any?>(
            "m-2" to mapOf("name" to "하늘"),
            "m-1" to mapOf("name" to "가람"),
            "m-3" to "깨진 값",
            "m-4" to mapOf("name" to "   ")
        )
        val roster = RallyRoster.decode(raw)
        assertEquals(listOf("가람", "하늘"), roster.map { it.name })
        assertEquals(listOf("m-1", "m-2"), roster.map { it.id })
    }

    @Test fun rosterDecodeOfNullIsEmpty() {
        assertEquals(emptyList<RallyMember>(), RallyRoster.decode(null))
    }

    @Test fun memberIdMustBeSafeForUrlPath() {
        assertEquals(true, RallyRoster.isValidMemberId("m-1a2b3c"))
        assertEquals(false, RallyRoster.isValidMemberId("a/b"))
        assertEquals(false, RallyRoster.isValidMemberId(""))
        assertEquals(false, RallyRoster.isValidMemberId("x".repeat(41)))
        assertNull(RallyRoster.encode("m-1", "   "))
    }

    @Test fun newMemberIdIsValidAndDifferentEachTime() {
        val a = RallyRoster.newMemberId()
        val b = RallyRoster.newMemberId()
        assertEquals(true, RallyRoster.isValidMemberId(a))
        assertEquals(true, a != b)
    }
}
