package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RallyScreenModelTest {

    private val d = 1e-9
    private val t3 = RallyTeamState("3군", "3군", "야곱", 50.0)
    private val t2 = RallyTeamState("2군", "2군", "쿼드", 30.0, online = false)
    private val t1 = RallyTeamState("1군", "1군", "윈터", 10.0)

    private fun room(
        run: RallyRunState = RallyRunState.IDLE,
        elapsed: Double = 0.0,
        mine: String = "3군",
        teams: List<RallyTeamState> = listOf(t3, t2, t1)
    ) = RallyRoomState(teams, mine, prepSec = 15.0, waitSec = 300.0, runState = run, elapsedSec = elapsed)

    private fun build(s: RallyRoomState) = RallyScreenModel.build(s)

    @Test
    fun `시작 전에는 관리자 시작 대기 문구와 내 클릭 시각을 보여준다`() {
        val m = build(room())
        assertEquals(HeroKind.IDLE, m.hero.kind)
        assertEquals("관리자 시작 대기중", m.hero.label)
        assertNull(m.hero.remainingSec)
        assertEquals("시작 후 15초에 내 집결 클릭", m.hero.subLabel)
        assertTrue(m.editable)
    }

    @Test
    fun `접속이 끊긴 참여 팀은 경고로 알려준다`() {
        assertEquals(listOf("2군 쿼드 연결 없음"), build(room()).warnings)
    }

    @Test
    fun `끊긴 팀을 제외하면 경고가 사라지고 최대 행군은 남은 팀 기준이다`() {
        val m = build(room(teams = listOf(t3, t2.copy(excluded = true), t1)))
        assertTrue(m.warnings.isEmpty())
        assertEquals(50.0, m.maxMarchSec, d)
        val m2 = build(room(teams = listOf(t3.copy(excluded = true), t2.copy(excluded = true), t1), mine = "1군"))
        assertEquals(10.0, m2.maxMarchSec, d)
    }

    @Test
    fun `클릭 시각이 준비시간 이내로 남으면 이동 문구를 보여준다`() {
        val m = build(room(RallyRunState.RUNNING, 0.0))
        assertEquals(HeroKind.MOVE, m.hero.kind)
        assertEquals("집결 화면으로 이동하세요", m.hero.label)
        assertEquals(15.0, m.hero.remainingSec!!, d)
    }

    @Test
    fun `클릭까지 준비시간보다 많이 남으면 대기 문구를 보여준다`() {
        val m = build(room(RallyRunState.RUNNING, 0.0, mine = "1군"))
        assertEquals(HeroKind.WAIT_CLICK, m.hero.kind)
        assertEquals("집결 대기중", m.hero.label)
        assertEquals(55.0, m.hero.remainingSec!!, d)
    }

    @Test
    fun `클릭 전 진행률과 보조 문구와 편집 잠금`() {
        val m = build(room(RallyRunState.RUNNING, 7.5))
        assertEquals(7.5, m.hero.remainingSec!!, d)
        assertEquals(0.5, m.hero.progress, d)
        assertEquals("집결 클릭까지", m.hero.subLabel)
        assertFalse(m.editable)
    }

    @Test
    fun `집결 중에는 출발까지 남은 시간과 이후 행군시간을 보여준다`() {
        val m = build(room(RallyRunState.RUNNING, 45.0))
        assertEquals(HeroKind.GATHERING, m.hero.kind)
        assertEquals("집결 중", m.hero.label)
        assertEquals(270.0, m.hero.remainingSec!!, d)
        assertEquals("출발까지 · 이후 행군 0:50", m.hero.subLabel)
        assertEquals(0.1, m.hero.progress, d)
    }

    @Test
    fun `행군 중에는 도착까지 남은 시간을 보여준다`() {
        val m = build(room(RallyRunState.RUNNING, 340.0))
        assertEquals(HeroKind.MARCHING, m.hero.kind)
        assertEquals("행군 중", m.hero.label)
        assertEquals(25.0, m.hero.remainingSec!!, d)
        assertEquals("성 도착까지", m.hero.subLabel)
        assertEquals(0.5, m.hero.progress, d)
    }

    @Test
    fun `도착 시각이 지나면 전원 도착 상태이고 다시 편집할 수 있다`() {
        val byTime = build(room(RallyRunState.RUNNING, 365.0))
        assertEquals(HeroKind.ARRIVED, byTime.hero.kind)
        assertEquals("전원 도착", byTime.hero.label)
        assertNull(byTime.hero.remainingSec)
        assertEquals(1.0, byTime.hero.progress, d)
        val byState = build(room(RallyRunState.ARRIVED))
        assertEquals(HeroKind.ARRIVED, byState.hero.kind)
        assertTrue(byState.editable)
    }

    @Test
    fun `취소되면 취소 문구와 팀 상태를 취소로 바꾸고 다시 편집할 수 있다`() {
        val m = build(room(RallyRunState.CANCELLED))
        assertEquals(HeroKind.CANCELLED, m.hero.kind)
        assertEquals("작전 취소됨", m.hero.label)
        assertEquals("예약된 클릭이 모두 멈췄어요", m.hero.subLabel)
        assertTrue(m.editable)
        assertEquals(setOf("취소"), m.rows.map { it.statusLabel }.toSet())
    }

    @Test
    fun `팀 목록은 클릭이 빠른 순이고 제외된 팀은 맨 아래`() {
        assertEquals(listOf("3군", "2군", "1군"), build(room()).rows.map { it.id })
        val m = build(room(teams = listOf(t3.copy(excluded = true), t2, t1), mine = "1군"))
        assertEquals(listOf("2군", "1군", "3군"), m.rows.map { it.id })
        assertEquals("제외", m.rows.last().statusLabel)
        assertTrue(m.rows.last().excluded)
    }

    @Test
    fun `진행 중 팀별 상태와 남은 시간`() {
        val rows = build(room(RallyRunState.RUNNING, 20.0)).rows.associateBy { it.id }
        assertEquals("집결 중", rows["3군"]!!.statusLabel)
        assertEquals(295.0, rows["3군"]!!.remainingSec!!, d)
        assertEquals("클릭 전", rows["2군"]!!.statusLabel)
        assertEquals(15.0, rows["2군"]!!.remainingSec!!, d)
        assertEquals("클릭 전", rows["1군"]!!.statusLabel)
        assertEquals(35.0, rows["1군"]!!.remainingSec!!, d)
    }

    @Test
    fun `내 팀 표시와 시작 전 대기 상태`() {
        val rows = build(room()).rows
        assertEquals(listOf("3군"), rows.filter { it.isMine }.map { it.id })
        assertEquals(setOf("대기"), rows.map { it.statusLabel }.toSet())
    }

    @Test
    fun `내 팀이 제외되면 제외 안내를 보여준다`() {
        val m = build(room(RallyRunState.RUNNING, 10.0, teams = listOf(t3.copy(excluded = true), t2, t1)))
        assertEquals(HeroKind.EXCLUDED, m.hero.kind)
        assertEquals("이번 작전에서 제외됐어요", m.hero.label)
        assertNull(m.hero.remainingSec)
    }

    @Test
    fun `참여 팀이 없으면 시작할 수 없다`() {
        assertNull(build(room()).startBlockedReason)
        val none = build(room(teams = listOf(t3.copy(excluded = true))))
        assertEquals("참여 팀이 없어요", none.startBlockedReason)
        assertEquals(0.0, none.maxMarchSec, d)
    }

    @Test
    fun `남은 시간 표기는 올림 m 콜론 ss`() {
        assertEquals("0:08", RallyScreenModel.formatMmSs(7.2))
        assertEquals("0:00", RallyScreenModel.formatMmSs(0.0))
        assertEquals("1:05", RallyScreenModel.formatMmSs(65.0))
        assertEquals("1:00", RallyScreenModel.formatMmSs(59.01))
        assertEquals("0:00", RallyScreenModel.formatMmSs(-3.0))
        assertEquals("5:00", RallyScreenModel.formatMmSs(300.0))
    }
}
