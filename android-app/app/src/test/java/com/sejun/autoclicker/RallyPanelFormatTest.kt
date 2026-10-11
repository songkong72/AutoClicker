package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Test

class RallyPanelFormatTest {
    private fun t(march: Double, adj: Int = 0, ex: Boolean = false) = LagInput(march, adj, ex)

    @Test fun 행군이_가장_긴_군단이_기준() =
        assertEquals(listOf("먼저 클릭", "18초 뒤", "21초 뒤"), RallyPanelFormat.lagLabels(listOf(t(51.0), t(33.0), t(30.0))))

    @Test fun 소수점_차이() =
        assertEquals(listOf("먼저 클릭", "5.5초 뒤"), RallyPanelFormat.lagLabels(listOf(t(35.5), t(30.0))))

    @Test fun 지휘관_보정이_시간차이에_들어간다() =
        assertEquals(listOf("먼저 클릭", "19.5초 뒤"), RallyPanelFormat.lagLabels(listOf(t(51.0), t(33.0, 1500))))

    @Test fun 보정이_커서_먼저_누르게_되면_그_군단이_기준() =
        assertEquals(listOf("1초 뒤", "먼저 클릭"), RallyPanelFormat.lagLabels(listOf(t(51.0), t(48.0, -4000))))

    @Test fun 제외된_군단은_계산에서도_표시에서도_빠진다() =
        assertEquals(listOf("먼저 클릭", "18초 뒤", ""), RallyPanelFormat.lagLabels(listOf(t(51.0), t(33.0), t(60.0, 0, true))))

    @Test fun 목록_높이는_군단_줄만_세고_조_머리띠는_덤으로_보인다() {
        fun head(card: Int) = ListLine(44, false, card)
        fun team(card: Int, h: Int = 50) = ListLine(h, true, card)
        // 군단이 5줄 이하면 한도가 없다(머리띠가 몇 개든)
        assertEquals(0, RallyListHeight.limit(listOf(head(0), team(0), team(0), head(1), team(1), team(1), team(1)), 5, 8))
        // 9군단 3조: 머리띠 2개 + 군단 5줄 + 상자 사이 간격 1번까지 보인다
        val nine = listOf(head(0), team(0), team(0), team(0), head(1), team(1), team(1), team(1), head(2), team(2), team(2), team(2))
        assertEquals(44 + 150 + 44 + 100 + 8, RallyListHeight.limit(nine, 5, 8))
        // 방 설정을 접어 생긴 자리만큼 더
        assertEquals(44 + 150 + 44 + 100 + 8 + 104, RallyListHeight.limit(nine, 5, 8, 104))
        // 1조를 접으면(머리띠만 남음) 2조·3조의 군단으로 5줄을 센다
        val folded = listOf(head(0), head(1), team(1), team(1), team(1), head(2), team(2), team(2), team(2))
        assertEquals(44 + 44 + 150 + 44 + 100 + 16, RallyListHeight.limit(folded, 5, 8))
        // 펼친 군단 줄 하나가 그보다 크면 그 줄은 다 보인다
        assertEquals(900, RallyListHeight.limit(listOf(team(0, 900), team(0), team(0), team(0), team(0), team(0)), 1, 8))
    }

    @Test fun 접힌_방_설정은_값을_한_줄로_보여_준다() =
        assertEquals("집결 대기 5분 · 이동 준비 15초", RallyPanelFormat.setupSummary(300.0, 15.0))

    @Test fun 조를_쓰는_방은_같은_조_안에서만_견준다() {
        // 1군(10초)·2군(30초)은 1조, 3군(50초)은 2조. 혼자인 3군은 "먼저 클릭".
        assertEquals(listOf("20초 뒤", "먼저 클릭", "먼저 클릭"), RallyPanelFormat.lagLabelsByWave(listOf(t(10.0), t(30.0), t(50.0)), listOf(1, 1, 2)))
        assertEquals(listOf("먼저 클릭", "5초 뒤", "먼저 클릭", "10초 뒤"), RallyPanelFormat.lagLabelsByWave(listOf(t(40.0), t(35.0), t(40.0), t(30.0)), listOf(1, 1, 2, 2)))
    }

    @Test fun 전부_제외면_모두_빈칸() =
        assertEquals(listOf("", ""), RallyPanelFormat.lagLabels(listOf(t(51.0, 0, true), t(33.0, 0, true))))

    @Test fun 부동소수_오차는_반올림() =
        assertEquals(listOf("먼저 클릭", "0.3초 뒤"), RallyPanelFormat.lagLabels(listOf(t(10.30000000000000004), t(10.0))))

    @Test fun 초_정수는_소수점없이() = assertEquals("51", RallyPanelFormat.sec(51.0))
    @Test fun 초_소수는_그대로() = assertEquals("42.5", RallyPanelFormat.sec(42.5))

    @Test
    fun `내 기기 요약은 이름 방 보정 위치를 한 줄로 보여 준다`() {
        assertEquals("위치 저장됨 · 방 0001 · 문", RallyPanelFormat.deviceSummary("문", "0001", "0초", true))
    }

    @Test
    fun `내 기기 요약은 빠진 항목을 없음으로 드러낸다`() {
        assertEquals("위치 없음 · 방 없음 · 캐릭터명 없음 · 보정 +0.5초", RallyPanelFormat.deviceSummary(" ", "", "+0.5초", false))
    }

    @Test
    fun `시작이 막힌 이유 뒤에 푸는 방법을 붙인다`() {
        assertEquals("참여 군단이 없어요 · 위의 연필을 눌러 군단을 체크하세요", RallyPanelFormat.blockedNote("참여 군단이 없어요", editing = false))
        assertEquals("참여 군단이 없어요 · 참여할 군단을 체크하세요", RallyPanelFormat.blockedNote("참여 군단이 없어요", editing = true))
    }
}
