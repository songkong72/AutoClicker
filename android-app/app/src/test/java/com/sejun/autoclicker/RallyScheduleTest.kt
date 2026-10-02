package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RallyScheduleTest {

    private val delta = 1e-9
    private val three = RallyTeamInput("3군", 50.0)
    private val two = RallyTeamInput("2군", 30.0)
    private val one = RallyTeamInput("1군", 10.0)

    @Test
    fun `준비 15초 대기 5분 - 3군 2군 1군 예시`() {
        val plan = RallySchedule.plan(listOf(one, two, three), prepSec = 15.0, waitSec = 300.0)
        assertEquals(50.0, plan.maxMarchSec, delta)
        assertEquals(365.0, plan.arriveAtSec, delta)
        assertEquals(15.0, plan.teamPlan("3군")!!.clickAtSec, delta)
        assertEquals(35.0, plan.teamPlan("2군")!!.clickAtSec, delta)
        assertEquals(55.0, plan.teamPlan("1군")!!.clickAtSec, delta)
    }

    @Test
    fun `모든 팀이 같은 시각에 도착한다`() {
        val plan = RallySchedule.plan(listOf(one, two, three), 15.0, 300.0)
        plan.teams.forEach { assertEquals(plan.arriveAtSec, it.arriveAtSec, delta) }
    }

    @Test
    fun `클릭이 빠른 순 - 행군이 긴 팀이 먼저`() {
        val plan = RallySchedule.plan(listOf(one, two, three), 15.0, 300.0)
        assertEquals(listOf("3군", "2군", "1군"), plan.teams.map { it.id })
    }

    @Test
    fun `제외한 팀은 최대 행군 계산에서 빠지고 나머지가 다시 계산된다`() {
        val plan = RallySchedule.plan(listOf(one, two, three.copy(excluded = true)), 15.0, 300.0)
        assertEquals(30.0, plan.maxMarchSec, delta)
        assertEquals(345.0, plan.arriveAtSec, delta)
        assertEquals(null, plan.teamPlan("3군"))
        assertEquals(15.0, plan.teamPlan("2군")!!.clickAtSec, delta)
        assertEquals(35.0, plan.teamPlan("1군")!!.clickAtSec, delta)
    }

    @Test
    fun `팀이 하나면 이동 준비시간 뒤에 클릭한다`() {
        val plan = RallySchedule.plan(listOf(two), 20.0, 180.0)
        assertEquals(20.0, plan.teamPlan("2군")!!.clickAtSec, delta)
        assertEquals(230.0, plan.arriveAtSec, delta)
    }

    @Test
    fun `참여 팀이 없으면 빈 계획`() {
        val empty = RallySchedule.plan(emptyList(), 15.0, 300.0)
        assertTrue(empty.teams.isEmpty())
        val allOut = RallySchedule.plan(listOf(one.copy(excluded = true)), 15.0, 300.0)
        assertTrue(allOut.teams.isEmpty())
        assertEquals(0.0, allOut.arriveAtSec, delta)
    }

    @Test
    fun `0점1초 단위 행군시간을 지원한다`() {
        val plan = RallySchedule.plan(
            listOf(RallyTeamInput("a", 50.5), RallyTeamInput("b", 30.2)), 15.0, 300.0
        )
        assertEquals(15.0, plan.teamPlan("a")!!.clickAtSec, 1e-6)
        assertEquals(35.3, plan.teamPlan("b")!!.clickAtSec, 1e-6)
        assertEquals(365.5, plan.arriveAtSec, 1e-6)
    }

    @Test
    fun `음수 입력은 거부한다`() {
        try {
            RallySchedule.plan(listOf(RallyTeamInput("x", -1.0)), 15.0, 300.0)
            fail("음수 행군시간은 예외여야 함")
        } catch (e: IllegalArgumentException) {
            assertNotNull(e.message)
        }
        try {
            RallySchedule.plan(listOf(one), -1.0, 300.0)
            fail("음수 준비시간은 예외여야 함")
        } catch (e: IllegalArgumentException) {
            assertNotNull(e.message)
        }
    }

    @Test
    fun `단계 전환은 경계 시각에 다음 단계로 넘어간다`() {
        val t = RallySchedule.plan(listOf(two, three), 15.0, 300.0).teamPlan("2군")!!
        // 2군: 클릭 35, 출발 335, 도착 365
        assertEquals(RallyPhase.BEFORE_CLICK, RallySchedule.phaseAt(t, 0.0))
        assertEquals(RallyPhase.BEFORE_CLICK, RallySchedule.phaseAt(t, 34.999))
        assertEquals(RallyPhase.GATHERING, RallySchedule.phaseAt(t, 35.0))
        assertEquals(RallyPhase.GATHERING, RallySchedule.phaseAt(t, 334.999))
        assertEquals(RallyPhase.MARCHING, RallySchedule.phaseAt(t, 335.0))
        assertEquals(RallyPhase.MARCHING, RallySchedule.phaseAt(t, 364.999))
        assertEquals(RallyPhase.ARRIVED, RallySchedule.phaseAt(t, 365.0))
    }

    @Test
    fun `단계별 남은 시간`() {
        val t = RallySchedule.plan(listOf(two, three), 15.0, 300.0).teamPlan("2군")!!
        assertEquals(25.0, RallySchedule.remainingInPhaseSec(t, 10.0), delta)
        assertEquals(290.0, RallySchedule.remainingInPhaseSec(t, 45.0), delta)
        assertEquals(10.0, RallySchedule.remainingInPhaseSec(t, 355.0), delta)
        assertEquals(0.0, RallySchedule.remainingInPhaseSec(t, 400.0), delta)
    }
}
