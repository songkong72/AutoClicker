package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RallyWavesTest {
    private fun team(n: Int, march: Double, wave: Int = 1, excluded: Boolean = false) =
        RallyTeamDoc("t$n", "${n}군", "", march, excluded, wave = wave)

    private val nine = RallyRoomDoc(
        teams = listOf(40.0, 35.0, 28.0, 40.0, 30.0, 25.0, 45.0, 30.0, 20.0).mapIndexed { i, m -> team(i + 1, m) },
        prepSec = 15.0, waitSec = 300.0, run = "IDLE", startSeq = 0L
    )

    @Test fun `같은 조는 동시에 도착하고 다음 조는 간격만큼 뒤에 도착한다`() {
        val doc = RallyRoomEdit.setWaveDelay(RallyRoomEdit.setWaveDelay(RallyRoomEdit.splitWaves(nine, 3), 2, 60.0), 3, 120.0)
        val plan = RallySchedule.plan(doc.inputs(), doc.prepSec, doc.waitSec)
        fun arrive(n: Int) = plan.teamPlan("t$n")!!.arriveAtSec
        // 가장 긴 행군은 7군의 45초: 1조 도착 = 15 + 45 + 300 = 360초
        listOf(1, 2, 3).forEach { assertEquals(360.0, arrive(it), 1e-9) }
        listOf(4, 5, 6).forEach { assertEquals(420.0, arrive(it), 1e-9) }
        listOf(7, 8, 9).forEach { assertEquals(480.0, arrive(it), 1e-9) }
        assertEquals(480.0, plan.arriveAtSec, 1e-9)
        // 누르는 시각도 간격만큼 늦어진다: 4군(40초, 2조) = 15 + (45 - 40) + 60
        assertEquals(80.0, plan.teamPlan("t4")!!.clickAtSec, 1e-9)
        assertEquals(20.0, plan.teamPlan("t1")!!.clickAtSec, 1e-9)
        assertEquals(135.0, plan.teamPlan("t7")!!.clickAtSec, 1e-9)
    }

    @Test fun `모두 1조면 조가 없던 때와 같다`() {
        val plan = RallySchedule.plan(nine.inputs(), nine.prepSec, nine.waitSec)
        plan.teams.forEach { assertEquals(360.0, it.arriveAtSec, 1e-9) }
        // 저장 모양도 예전과 같다(새 서버 규칙 없이도 쓸 수 있다)
        val saved = RallyRoomCodec.encode(nine)
        assertFalse(saved.containsKey("waveDelay"))
        @Suppress("UNCHECKED_CAST")
        (saved["teams"] as List<Map<String, Any?>>).forEach { assertFalse(it.containsKey("wave")) }
    }

    @Test fun `조와 도착 간격을 저장하고 다시 읽는다`() {
        val doc = RallyRoomEdit.setWaveDelay(RallyRoomEdit.setWave(nine, "t4", 2), 2, 70.0)
        val back = RallyRoomCodec.decode(RallyRoomCodec.encode(doc))
        assertEquals(2, back.teams.first { it.id == "t4" }.wave)
        assertEquals(1, back.teams.first { it.id == "t1" }.wave)
        assertEquals(mapOf(2 to 70.0), back.waveDelaySec)
        assertEquals(mapOf("w2" to 70.0), RallyRoomCodec.encode(doc)["waveDelay"])
    }

    @Test fun `이상한 값은 범위 안으로 맞춘다`() {
        val raw = mapOf(
            "teams" to listOf(mapOf("id" to "t1", "marchSec" to 10, "wave" to 9), mapOf("id" to "t2", "marchSec" to 10, "wave" to 0)),
            "waveDelay" to mapOf("w2" to 9999, "w1" to 30, "x" to 5, "w6" to 10, "w3" to "많이")
        )
        val doc = RallyRoomCodec.decode(raw)
        assertEquals(listOf(5, 1), doc.teams.map { it.wave })
        assertEquals(mapOf(2 to 600.0), doc.waveDelaySec)
    }

    @Test fun `도착 간격은 2조부터 0초에서 10분까지`() {
        assertSame(nine, RallyRoomEdit.setWaveDelay(nine, 1, 60.0))
        assertSame(nine, RallyRoomEdit.setWaveDelay(nine, 6, 60.0))
        assertSame(nine, RallyRoomEdit.setWaveDelay(nine, 2, -10.0))
        assertEquals(600.0, RallyRoomEdit.setWaveDelay(nine, 2, 900.0).waveDelaySec[2]!!, 1e-9)
        val set = RallyRoomEdit.setWaveDelay(nine, 3, 40.0)
        assertTrue(RallyRoomEdit.setWaveDelay(set, 3, 0.0).waveDelaySec.isEmpty())
    }

    @Test fun `진행 중에는 조를 바꿀 수 없다`() {
        val running = nine.copy(run = "RUNNING")
        assertSame(running, RallyRoomEdit.setWave(running, "t1", 2))
        assertSame(running, RallyRoomEdit.setWaveDelay(running, 2, 60.0))
        assertSame(running, RallyRoomEdit.splitWaves(running, 3))
    }

    @Test fun `조 나누기는 번호 순서대로 넣고 남는 군단은 마지막 조에 넣는다`() {
        val ten = nine.copy(teams = (listOf(team(10, 30.0)) + nine.teams)) // 10군이 목록 맨 앞에 있어도 번호 순으로
        fun waves(d: RallyRoomDoc) = RallyRoomEdit.ordered(d).map { it.wave }
        assertEquals(listOf(1, 1, 1, 2, 2, 2, 3, 3, 3, 4), waves(RallyRoomEdit.splitWaves(ten, 3)))
        assertEquals(listOf(1, 1, 2, 2, 3, 3, 4, 4, 5, 5), waves(RallyRoomEdit.splitWaves(ten, 2)))
        assertEquals(listOf(1, 2, 3, 4, 5, 5, 5, 5, 5, 5), waves(RallyRoomEdit.splitWaves(ten, 1)))
        assertEquals(List(10) { 1 }, waves(RallyRoomEdit.splitWaves(RallyRoomEdit.splitWaves(ten, 3), 0)))
    }

    @Test fun `간격을 읽기 쉬운 말로 적는다`() {
        assertEquals("먼저 도착", RallyWaves.delayLabel(1, 0.0))
        assertEquals("같이 도착", RallyWaves.delayLabel(2, 0.0))
        assertEquals("40초 뒤 도착", RallyWaves.delayLabel(2, 40.0))
        assertEquals("1분 뒤 도착", RallyWaves.delayLabel(3, 60.0))
        assertEquals("1분 10초 뒤 도착", RallyWaves.delayLabel(3, 70.0))
        assertEquals(2, RallyWaves.waveOfKey("w2"))
        assertNull(RallyWaves.waveOfKey("w1"))
        assertNull(RallyWaves.waveOfKey("2"))
    }

    private fun state(doc: RallyRoomDoc, run: RallyRunState, elapsed: Double, myTeamId: String = "", admin: Boolean = true) = RallyRoomState(
        doc.teams.map { RallyTeamState(it.id, it.name, it.leaderName, it.marchSec, true, it.excluded, it.adminAdjustMs, it.leaderId, it.wave) },
        myTeamId, doc.prepSec, doc.waitSec, run, elapsed, isAdmin = admin, waveDelaySec = doc.waveDelaySec
    )

    private val split = RallyRoomEdit.setWaveDelay(RallyRoomEdit.setWaveDelay(RallyRoomEdit.splitWaves(nine, 3), 2, 60.0), 3, 120.0)

    @Test fun `대기 중에는 가장 먼저 닿는 조의 도착까지를 미리 보여 준다`() {
        val m = RallyScreenModel.build(state(split, RallyRunState.IDLE, 0.0))
        assertTrue(m.waveMode)
        assertEquals(listOf(1, 2, 3), m.waves.map { it.wave })
        assertEquals(360.0, m.previewSec, 1e-9)
        assertEquals("1조 도착까지 · 2조 +1분 · 3조 +2분", m.previewNote)
        assertEquals(listOf(360.0, 420.0, 480.0), m.waves.map { it.arriveAtSec })
        assertEquals(480.0, m.arriveAtSec, 1e-9)
    }

    @Test fun `진행 중 지휘관의 큰 숫자는 다음에 도착할 조이고 도착하면 다음 조로 넘어간다`() {
        val at88 = RallyScreenModel.build(state(split, RallyRunState.RUNNING, 88.0))
        assertEquals(HeroKind.OVERVIEW, at88.hero.kind)
        assertEquals("1조 집결 중", at88.hero.label)
        assertEquals(272.0, at88.hero.remainingSec!!, 1e-9)
        assertEquals("1조 도착까지 · 다음 2조 5:32", at88.hero.subLabel)
        assertEquals(listOf(272.0, 332.0, 392.0), at88.waves.map { it.remainingSec })
        assertEquals(listOf(1, 1, 0), at88.waves.map { it.phase })

        val at400 = RallyScreenModel.build(state(split, RallyRunState.RUNNING, 400.0))
        assertEquals("2조 행군 중", at400.hero.label)
        assertEquals(20.0, at400.hero.remainingSec!!, 1e-9)
        assertEquals("2조 도착까지 · 다음 3조 1:20", at400.hero.subLabel)
        assertEquals(3, at400.waves.first().phase)
        assertFalse(at400.editable)

        val done = RallyScreenModel.build(state(split, RallyRunState.RUNNING, 481.0))
        assertEquals(HeroKind.ARRIVED, done.hero.kind)
    }

    @Test fun `내 군단이 먼저 닿아도 뒤 조가 가는 중이면 전원 도착이 아니다`() {
        // 지휘관이 1군(1조)을 맡았을 때: 1조가 닿은 뒤(360초)에는 다음 조를 이어서 본다.
        val admin = RallyScreenModel.build(state(split, RallyRunState.RUNNING, 400.0, myTeamId = "t1"))
        assertEquals(HeroKind.OVERVIEW, admin.hero.kind)
        assertEquals("2조 행군 중", admin.hero.label)
        assertEquals(20.0, admin.hero.remainingSec!!, 1e-9)
        // 집결장: "내 군단 도착"과 남은 조.
        val leader = RallyScreenModel.build(state(split, RallyRunState.RUNNING, 400.0, myTeamId = "t1", admin = false))
        assertEquals(HeroKind.ARRIVED, leader.hero.kind)
        assertEquals("내 군단 도착", leader.hero.label)
        assertEquals("2조 행군 중 0:20", leader.hero.note)
        // 마지막 조까지 닿으면 그때 전원 도착.
        val done = RallyScreenModel.build(state(split, RallyRunState.RUNNING, 481.0, myTeamId = "t1"))
        assertEquals("전원 도착", done.hero.label)
    }

    @Test fun `집결장의 큰 숫자는 내 군단 기준 그대로다`() {
        val m = RallyScreenModel.build(state(split, RallyRunState.RUNNING, 70.0, myTeamId = "t5", admin = false))
        // 5군(30초, 2조): 누르는 때 = 15 + (45 - 30) + 60 = 90초
        assertEquals(20.0, m.hero.remainingSec!!, 1e-9)
        assertEquals(listOf("1군", "2군", "3군", "4군", "5군", "6군", "7군", "8군", "9군"), m.rows.map { it.name })
        assertEquals(listOf(1, 1, 1, 2, 2, 2, 3, 3, 3), m.rows.map { it.wave })
    }

    @Test fun `조를 쓰지 않는 방의 화면 모델은 그대로다`() {
        val m = RallyScreenModel.build(state(nine, RallyRunState.IDLE, 0.0))
        assertFalse(m.waveMode)
        assertNull(m.previewNote)
        assertEquals(360.0, m.previewSec, 1e-9)
        assertEquals(1, m.waves.size)
    }

    @Test fun `군단을 뒤 조로 옮기면 목록에서 그 조 아래로 간다`() {
        val moved = RallyRoomEdit.setWave(nine, "t2", 3)
        val m = RallyScreenModel.build(state(moved, RallyRunState.IDLE, 0.0))
        assertEquals("2군", m.rows.last().name)
        assertEquals(listOf(1, 3), m.waves.map { it.wave })
    }
}
