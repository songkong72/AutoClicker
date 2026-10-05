package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RallyRoomEditTest {
    private fun room(run: String = "IDLE", seq: Long = 0) = RallyRoomDoc(
        listOf(RallyTeamDoc("t1", "1군", "", 10.0), RallyTeamDoc("t2", "2군", "", 30.0)),
        15.0, 300.0, run, seq
    )

    @Test fun setMarchChangesOnlyThatTeam() {
        val d = RallyRoomEdit.setMarch(room(), "t2", 42.5)
        assertEquals(42.5, d.teams[1].marchSec, 0.0)
        assertEquals(10.0, d.teams[0].marchSec, 0.0)
    }

    @Test fun marchIsClampedAtZero() {
        assertEquals(0.0, RallyRoomEdit.setMarch(room(), "t1", -5.0).teams[0].marchSec, 0.0)
    }

    @Test fun excludeAndReinclude() {
        val ex = RallyRoomEdit.setExcluded(room(), "t1", true)
        assertTrue(ex.teams[0].excluded)
        assertEquals(false, RallyRoomEdit.setExcluded(ex, "t1", false).teams[0].excluded)
    }

    @Test fun addTeamGetsUniqueId() {
        val d = RallyRoomEdit.addTeam(room(), "3군", 50.0)
        assertEquals(3, d.teams.size)
        assertEquals(3, d.teams.map { it.id }.toSet().size)
        assertEquals("3군", d.teams[2].name)
    }

    @Test fun removeTeam() {
        assertEquals(listOf("t2"), RallyRoomEdit.removeTeam(room(), "t1").teams.map { it.id })
    }

    @Test fun editsAreIgnoredWhileRunning() {
        val r = room(run = "RUNNING", seq = 1)
        assertSame(r, RallyRoomEdit.setMarch(r, "t1", 99.0))
        assertSame(r, RallyRoomEdit.setExcluded(r, "t1", true))
        assertSame(r, RallyRoomEdit.addTeam(r, "x", 1.0))
        assertSame(r, RallyRoomEdit.removeTeam(r, "t1"))
    }

    @Test fun startIncrementsSeqAndRuns() {
        val d = RallyRoomEdit.startOrRegroup(room(seq = 4))
        assertEquals("RUNNING", d.run)
        assertEquals(5L, d.startSeq)
    }

    @Test fun regroupWhileRunningRestartsWithNewSeq() {
        val d = RallyRoomEdit.startOrRegroup(room(run = "RUNNING", seq = 5))
        assertEquals("RUNNING", d.run)
        assertEquals(6L, d.startSeq)
    }

    @Test fun startWithNoActiveTeamsDoesNothing() {
        val allOut = room().copy(teams = room().teams.map { it.copy(excluded = true) })
        assertSame(allOut, RallyRoomEdit.startOrRegroup(allOut))
    }

    @Test fun cancelKeepsSeqAndMarksCancelled() {
        val d = RallyRoomEdit.cancel(room(run = "RUNNING", seq = 5))
        assertEquals("CANCELLED", d.run)
        assertEquals(5L, d.startSeq)
    }

    @Test fun cancelWhenNotRunningIsNoop() {
        val r = room()
        assertSame(r, RallyRoomEdit.cancel(r))
    }

    @Test fun leaderCanEditOnlyOwnMarch() {
        assertTrue(RallyRoomEdit.canEditMarch(room(), false, "t1", "t1"))
        assertEquals(false, RallyRoomEdit.canEditMarch(room(), false, "t1", "t2"))
    }

    @Test fun adminCanEditAnyMarch() {
        assertTrue(RallyRoomEdit.canEditMarch(room(), true, "t1", "t2"))
    }

    @Test fun nobodyEditsMarchWhileRunning() {
        val r = room(run = "RUNNING", seq = 1)
        assertEquals(false, RallyRoomEdit.canEditMarch(r, true, "t1", "t1"))
        assertEquals(false, RallyRoomEdit.canEditMarch(r, false, "t1", "t1"))
    }

    @Test fun excludedTeamMarchIsNotEditable() {
        val r = RallyRoomEdit.setExcluded(room(), "t1", true)
        assertEquals(false, RallyRoomEdit.canEditMarch(r, false, "t1", "t1"))
    }

    @Test fun setPrepChangesOnlyPrep() {
        val d = RallyRoomEdit.setPrep(room(), 20.0)
        assertEquals(20.0, d.prepSec, 0.0)
        assertEquals(300.0, d.waitSec, 0.0)
    }

    @Test fun prepIsClampedAtZero() {
        assertEquals(0.0, RallyRoomEdit.setPrep(room(), -3.0).prepSec, 0.0)
    }

    @Test fun waitAcceptsOnlyPresets3_5_10Minutes() {
        assertEquals(180.0, RallyRoomEdit.setWait(room(), 180.0).waitSec, 0.0)
        assertEquals(600.0, RallyRoomEdit.setWait(room(), 600.0).waitSec, 0.0)
        assertEquals(300.0, RallyRoomEdit.setWait(room(), 123.0).waitSec, 0.0) // 프리셋이 아니면 무시
    }

    @Test fun prepAndWaitLockedWhileRunning() {
        val r = room("RUNNING", 1)
        assertSame(r, RallyRoomEdit.setPrep(r, 99.0))
        assertSame(r, RallyRoomEdit.setWait(r, 600.0))
    }

    // 서버 문서가 RUNNING인데 이 기기는 시작 신호를 받은 적이 없으면(예전 시도의 찌꺼기) 화면은 대기로 보이므로 수정도 되어야 한다
    @Test fun staleRunningIsUnstuckWhenThisDeviceNeverSawTheStart() {
        val stale = room("RUNNING", 3)
        val eff = RallyRoomEdit.unstick(stale, startSeen = false)
        assertEquals("IDLE", eff.run)
        assertEquals(60.0, RallyRoomEdit.setMarch(eff, "t1", 60.0).teams[0].marchSec, 0.0)
        assertEquals(600.0, RallyRoomEdit.setWait(eff, 600.0).waitSec, 0.0)
    }

    @Test fun realRunningStaysLockedWhenThisDeviceSawTheStart() {
        val running = room("RUNNING", 3)
        assertSame(running, RallyRoomEdit.unstick(running, startSeen = true))
    }

    @Test fun nonRunningIsUntouchedByUnstick() {
        val idle = room("IDLE", 3)
        assertSame(idle, RallyRoomEdit.unstick(idle, startSeen = false))
    }

    // 전원 도착 뒤에는 서버 문서가 아직 RUNNING이어도 화면처럼 수정할 수 있어야 한다. room(): 준비 15 + 최대 행군 30 + 대기 300 = 345초
    @Test fun finishedOnlyAfterEveryTeamArrived() {
        val running = room("RUNNING", 2)
        assertEquals(false, RallyRoomEdit.finished(running, 344.9))
        assertEquals(true, RallyRoomEdit.finished(running, 345.0))
    }

    @Test fun notRunningIsNeverFinished() {
        assertEquals(false, RallyRoomEdit.finished(room("IDLE", 2), 9999.0))
        assertEquals(false, RallyRoomEdit.finished(room("CANCELLED", 2), 9999.0))
    }

    @Test fun adminAdjustChangesOnlyThatTeamAndIsClamped() {
        val d = RallyRoomEdit.setAdminAdjust(room(), "t2", 800)
        assertEquals(800, d.teams[1].adminAdjustMs)
        assertEquals(0, d.teams[0].adminAdjustMs)
        assertEquals(5000, RallyRoomEdit.setAdminAdjust(room(), "t1", 99999).teams[0].adminAdjustMs)
        assertEquals(-5000, RallyRoomEdit.setAdminAdjust(room(), "t1", -99999).teams[0].adminAdjustMs)
    }

    @Test fun adminAdjustIsLockedWhileRunning() {
        val r = room("RUNNING", 1)
        assertSame(r, RallyRoomEdit.setAdminAdjust(r, "t1", 500))
    }

    private fun gapRoom(vararg nums: Int) = RallyRoomDoc(
        nums.map { RallyTeamDoc("t$it", "${it}군", "", 30.0) }, 15.0, 300.0, "IDLE", 0
    )

    @Test fun addNextTeamFillsTheLowestFreeNumber() {
        val d = RallyRoomEdit.addNextTeam(gapRoom(2, 3), 30.0)
        assertEquals("1군", d.teams.last().name)
        assertEquals(3, d.teams.size)
    }

    @Test fun addNextTeamFillsGapsInOrderThenContinues() {
        var d = gapRoom(2, 4)
        d = RallyRoomEdit.addNextTeam(d, 30.0)
        d = RallyRoomEdit.addNextTeam(d, 30.0)
        d = RallyRoomEdit.addNextTeam(d, 30.0)
        assertEquals(listOf("2군", "4군", "1군", "3군", "5군"), d.teams.map { it.name })
    }

    @Test fun addNextTeamNeverDuplicatesNamesOrIds() {
        var d = gapRoom(1)
        repeat(6) { d = RallyRoomEdit.addNextTeam(d, 30.0) }
        assertEquals(7, d.teams.map { it.name }.toSet().size)
        assertEquals(7, d.teams.map { it.id }.toSet().size)
    }

    @Test fun addNextTeamAvoidsIdOfAnotherTeamWithDifferentName() {
        // id t1은 "2군"이 쓰고 있다. 새 1군은 그 id를 가져가면 안 된다.
        val d = RallyRoomDoc(listOf(RallyTeamDoc("t1", "2군", "", 30.0)), 15.0, 300.0, "IDLE", 0)
        val n = RallyRoomEdit.addNextTeam(d, 30.0)
        assertEquals("1군", n.teams.last().name)
        assertEquals(2, n.teams.map { it.id }.toSet().size)
    }

    @Test fun addNextTeamIsIgnoredWhileRunning() {
        val r = RallyRoomDoc(listOf(RallyTeamDoc("t2", "2군", "", 30.0)), 15.0, 300.0, "RUNNING", 1)
        assertSame(r, RallyRoomEdit.addNextTeam(r, 30.0))
    }

    @Test fun addNextTeamStartsAtOneInAnEmptyRoom() {
        val d = RallyRoomEdit.addNextTeam(gapRoom(), 30.0)
        assertEquals(listOf("1군"), d.teams.map { it.name })
        assertEquals(30.0, d.teams[0].marchSec, 0.0)
    }
}
