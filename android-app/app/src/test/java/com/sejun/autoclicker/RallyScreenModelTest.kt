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
        assertEquals("관리자 시작 대기 중", m.hero.label)
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
        assertEquals("클릭 대기 중", m.hero.label)
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
    fun `취소되면 취소 문구를 보여 주고 팀 상태 글자는 비우며 다시 편집할 수 있다`() {
        val m = build(room(RallyRunState.CANCELLED))
        assertEquals(HeroKind.CANCELLED, m.hero.kind)
        assertEquals("작전 취소됨", m.hero.label)
        assertEquals("예약된 클릭이 모두 멈췄어요", m.hero.subLabel)
        assertTrue(m.editable)
        assertEquals(setOf(""), m.rows.map { it.statusLabel }.toSet())
    }

    @Test
    fun `팀 목록은 클릭이 빠른 순이고 제외된 팀은 맨 아래`() {
        // 정렬은 클릭 순서가 아니라 군단 이름(번호) 순서다
        assertEquals(listOf("1군", "2군", "3군"), build(room()).rows.map { it.id })
        val m = build(room(teams = listOf(t3.copy(excluded = true), t2, t1), mine = "1군"))
        assertEquals(listOf("1군", "2군", "3군"), m.rows.map { it.id }) // 제외돼도 제자리
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
    fun `내 팀 표시와 시작 전에는 팀 상태 글자가 비어 있다`() {
        val rows = build(room()).rows
        assertEquals(listOf("3군"), rows.filter { it.isMine }.map { it.id })
        assertEquals(setOf(""), rows.map { it.statusLabel }.toSet())
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
        assertEquals("참여 군단이 없어요", none.startBlockedReason)
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

    @Test
    fun `방에 팀이 하나도 없으면 제외가 아니라 관리자 구성 대기 안내를 보여준다`() {
        val m = build(room(teams = emptyList()))
        assertEquals(HeroKind.IDLE, m.hero.kind)
        assertEquals("관리자가 군단을 구성하는 중이에요", m.hero.label)
        assertNull(m.hero.remainingSec)
    }

    @Test
    fun `팀이 하나도 없는 방에서 관리자에게는 방 번호 확인과 새 방 만드는 곳을 안내한다`() {
        val m = build(room(teams = emptyList()).copy(isAdmin = true))
        assertEquals(HeroKind.IDLE, m.hero.kind)
        assertEquals("이 방에는 군단이 없어요", m.hero.label)
        assertEquals("방 번호가 맞는지 확인하세요 · 새 방은 앱 첫 화면에서 만들어요", m.hero.subLabel)
    }

    @Test
    fun `내 군단이 배정되지 않았으면 관리자의 배정을 기다리라고 안내한다`() {
        val m = build(room(teams = listOf(t2, t1), mine = ""))
        assertEquals(HeroKind.IDLE, m.hero.kind)
        assertEquals("아직 군단이 배정되지 않았어요", m.hero.label)
        assertEquals("관리자가 군단을 배정하면 시작할 수 있어요", m.hero.subLabel)
    }

    @Test
    fun `캐릭터명을 등록하지 않았으면 경고하고 등록하면 사라진다`() {
        val missing = build(room().copy(characterNameSet = false))
        assertTrue(missing.warnings.any { it.contains("캐릭터명") })
        assertFalse(build(room()).warnings.any { it.contains("캐릭터명") })
    }

    @Test
    fun `내 팀이 목록에 있고 제외된 경우에만 제외 안내가 나온다`() {
        val m = build(room(teams = listOf(t3.copy(excluded = true), t2, t1)))
        assertEquals(HeroKind.EXCLUDED, m.hero.kind)
    }

    @Test
    fun `전원 도착 예정 시각과 팀별 구간 시각을 알려준다`() {
        val m = build(room())
        assertEquals(365.0, m.arriveAtSec, d)
        val r3 = m.rows.first { it.id == "3군" }
        assertEquals(15.0, r3.clickAtSec, d)
        assertEquals(315.0, r3.departAtSec, d)
        assertEquals(365.0, r3.arriveAtSec, d)
        val r1 = m.rows.first { it.id == "1군" }
        assertEquals(55.0, r1.clickAtSec, d)
        assertEquals(365.0, r1.arriveAtSec, d)
    }

    @Test
    fun `진행 중에만 현재 경과 시각을 알려준다`() {
        assertNull(build(room()).nowSec)
        assertEquals(20.0, build(room(RallyRunState.RUNNING, 20.0)).nowSec!!, d)
    }

    @Test fun screenModelExposesPrepAndWaitForSettingsRow() {
        val m = build(room())
        assertEquals(15.0, m.prepSec, 0.0)
        assertEquals(300.0, m.waitSec, 0.0)
    }

    @Test
    fun `관리자 보정이 행에 실리고 제외된 팀도 값을 유지한다`() {
        val adj = t1.copy(adminAdjustMs = 700)
        val ex = t2.copy(excluded = true, adminAdjustMs = -300)
        val m = build(room(teams = listOf(t3, ex, adj)))
        assertEquals(700, m.rows.first { it.id == "1군" }.adminAdjustMs)
        assertEquals(-300, m.rows.first { it.id == "2군" }.adminAdjustMs)
        assertEquals(0, m.rows.first { it.id == "3군" }.adminAdjustMs)
    }

    @Test
    fun `내 클릭 위치를 저장하지 않았으면 경고하고 진행 중에도 유지한다`() {
        val idle = build(room().copy(positionSaved = false))
        assertTrue(idle.warnings.any { it.contains("클릭 위치") })
        val running = build(room(run = RallyRunState.RUNNING, elapsed = 3.0).copy(positionSaved = false))
        assertTrue(running.warnings.any { it.contains("클릭 위치") })
    }

    @Test
    fun `위치를 저장했거나 이번 작전에서 제외됐으면 위치 경고가 없다`() {
        assertFalse(build(room()).warnings.any { it.contains("클릭 위치") })
        val excluded = room(teams = listOf(t3.copy(excluded = true), t2, t1)).copy(positionSaved = false)
        assertFalse(build(excluded).warnings.any { it.contains("클릭 위치") })
    }

    // 방: 3군 50s, 2군 30s, 1군 10s / 준비 15s, 대기 300s → 클릭 시각 3군 15s, 2군 35s, 1군 55s, 출발은 클릭+300s(첫 315s, 마지막 355s), 전원 도착 365s
    // 군단이 없는 관리자의 큰 숫자는 집결장처럼 "다음 단계까지 남은 시간"이다.
    @Test
    fun `군단이 없는 관리자는 시작 직후 첫 클릭까지 이동 준비로 센다`() {
        val m = build(room(RallyRunState.RUNNING, elapsed = 0.0, mine = "").copy(isAdmin = true))
        assertEquals(HeroKind.OVERVIEW, m.hero.kind)
        assertEquals("이동 준비", m.hero.label)
        assertEquals(0, m.hero.phase)
        assertEquals("첫 집결 클릭까지", m.hero.subLabel)
        assertEquals(15.0, m.hero.remainingSec!!, d)
    }

    @Test
    fun `군단이 없는 관리자의 첫 클릭까지 숫자는 시간이 갈수록 줄어든다`() {
        val a = build(room(RallyRunState.RUNNING, elapsed = 3.0, mine = "").copy(isAdmin = true))
        val b = build(room(RallyRunState.RUNNING, elapsed = 10.0, mine = "").copy(isAdmin = true))
        assertEquals(12.0, a.hero.remainingSec!!, d)
        assertEquals(5.0, b.hero.remainingSec!!, d)
    }

    @Test
    fun `군단이 없는 관리자는 첫 클릭 뒤 전원 출발까지를 집결 중으로 센다`() {
        val m = build(room(RallyRunState.RUNNING, elapsed = 100.0, mine = "").copy(isAdmin = true))
        assertEquals("집결 중", m.hero.label)
        assertEquals(1, m.hero.phase)
        assertEquals("전원 출발까지", m.hero.subLabel)
        assertEquals(355.0 - 100.0, m.hero.remainingSec!!, d)
    }

    @Test
    fun `군단이 없는 관리자는 전원 출발 뒤 전원 도착까지를 행군 중으로 센다`() {
        val m = build(room(RallyRunState.RUNNING, elapsed = 360.0, mine = "").copy(isAdmin = true))
        assertEquals("행군 중", m.hero.label)
        assertEquals(2, m.hero.phase)
        assertEquals("전원 도착까지", m.hero.subLabel)
        assertEquals(5.0, m.hero.remainingSec!!, d)
    }

    @Test
    fun `군단이 배정되지 않은 집결장은 진행 중에도 전체 보기 대신 배정 안내를 본다`() {
        val m = build(room(RallyRunState.RUNNING, elapsed = 20.0, mine = ""))
        assertEquals(HeroKind.IDLE, m.hero.kind)
        assertEquals("아직 군단이 배정되지 않았어요", m.hero.label)
        assertNull(m.hero.remainingSec)
    }

    @Test
    fun `자기 군단을 제외한 관리자의 전체 보기에는 참여 안 함 표시가 붙고 군단이 없는 관리자에는 안 붙는다`() {
        val excluded = build(room(RallyRunState.RUNNING, elapsed = 3.0, teams = listOf(t3.copy(excluded = true), t2, t1)).copy(isAdmin = true))
        assertEquals(HeroKind.OVERVIEW, excluded.hero.kind)
        assertEquals("참여 안 함", excluded.hero.note)
        val none = build(room(RallyRunState.RUNNING, elapsed = 3.0, mine = "").copy(isAdmin = true))
        assertNull(none.hero.note)
    }

    @Test
    fun `군단이 없는 관리자도 시작 전에는 배정 안내를 그대로 보여준다`() {
        val m = build(room(RallyRunState.IDLE, mine = ""))
        assertEquals("아직 군단이 배정되지 않았어요", m.hero.label)
        assertNull(m.hero.remainingSec)
    }

    @Test
    fun `제외한 군단은 맨 아래로 내려가지 않고 제 번호 자리에 흐리게 남는다`() {
        val m = build(room(teams = listOf(t3, t2.copy(excluded = true), t1), mine = "1군"))
        assertEquals(listOf("1군", "2군", "3군"), m.rows.map { it.id })
        assertEquals(listOf(false, true, false), m.rows.map { it.excluded })
        assertEquals("제외", m.rows[1].statusLabel)
    }

    @Test
    fun `행군시간을 바꿔도 줄 순서는 그대로다`() {
        val a = build(room()).rows.map { it.id }
        val b = build(room(teams = listOf(t3.copy(marchSec = 5.0), t2, t1.copy(marchSec = 99.0)))).rows.map { it.id }
        assertEquals(a, b)
    }

    @Test
    fun `군단 번호는 숫자로 정렬하고 번호 없는 이름은 만든 순서대로 뒤에 둔다`() {
        fun team(id: String, name: String) = RallyTeamState(id, name, "", 30.0)
        val m = build(room(teams = listOf(team("a", "10군"), team("b", "별동대"), team("c", "9군"), team("d", "2군"), team("e", "예비")), mine = "a"))
        assertEquals(listOf("2군", "9군", "10군", "별동대", "예비"), m.rows.map { it.name })
    }

    private fun adminExcluded(run: RallyRunState, elapsed: Double = 0.0) =
        room(run, elapsed, mine = "1군", teams = listOf(t3, t2, t1.copy(excluded = true))).copy(isAdmin = true)

    @Test
    fun `자기 군단을 제외한 관리자도 진행 중에는 전체 진행을 센다`() {
        val m = build(adminExcluded(RallyRunState.RUNNING, 100.0))
        assertEquals(HeroKind.OVERVIEW, m.hero.kind)
        assertEquals("집결 중", m.hero.label)
        // 1군이 제외돼 마지막 출발은 2군(335초)이다
        assertEquals(235.0, m.hero.remainingSec!!, d)
    }

    @Test
    fun `자기 군단을 제외한 관리자의 시작 전 안내는 제외됐다가 아니라 참여하지 않는다`() {
        val m = build(adminExcluded(RallyRunState.IDLE))
        assertEquals(HeroKind.IDLE, m.hero.kind)
        assertEquals("이번 작전에는 참여하지 않아요", m.hero.label)
    }

    @Test
    fun `제외된 집결장은 관리자가 아니면 계속 제외됐다고 보인다`() {
        val s = adminExcluded(RallyRunState.RUNNING, 100.0).copy(isAdmin = false)
        assertEquals(HeroKind.EXCLUDED, build(s).hero.kind)
    }
}
