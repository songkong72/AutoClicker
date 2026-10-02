package com.sejun.autoclicker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RallyStartDetectorTest {
    @Test fun alreadyRunningRoomSeenForTheFirstTimeIsNotAStart() {
        // 패널을 열거나 방에 늦게 들어왔을 때 옛 시작 신호로 카운트다운이 시작되면 안 된다
        assertFalse(RallyStartDetector().onDoc(startSeq = 5, run = "RUNNING", fromRemote = true))
    }

    @Test fun newerSeqWhileRunningAfterFirstSightIsAStart() {
        val d = RallyStartDetector()
        d.onDoc(5, "RUNNING", true)
        assertTrue(d.onDoc(6, "RUNNING", true))
    }

    @Test fun sameSeqRepeatedIsNotAStartAgain() {
        val d = RallyStartDetector()
        d.onDoc(0, "IDLE", true)
        assertTrue(d.onDoc(1, "RUNNING", true))
        assertFalse(d.onDoc(1, "RUNNING", true))
    }

    @Test fun cancelledRoomWithNewSeqIsNotAStartButIsRemembered() {
        val d = RallyStartDetector()
        d.onDoc(1, "RUNNING", true)
        assertFalse(d.onDoc(2, "CANCELLED", true))
        assertFalse(d.onDoc(2, "RUNNING", true)) // 같은 seq는 이미 본 것
    }

    @Test fun adminStartBeforeAnyRemoteSnapshotStillCounts() {
        val d = RallyStartDetector()
        assertTrue(d.onDoc(1, "RUNNING", fromRemote = false))
        assertFalse(d.onDoc(1, "RUNNING", fromRemote = true)) // 뒤늦게 온 같은 상태
    }
}
