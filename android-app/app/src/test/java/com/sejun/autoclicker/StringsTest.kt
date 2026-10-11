package com.sejun.autoclicker

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StringsTest {
    @After fun backToKorean() = T.set(StrKo)

    private val hangul = Regex("[가-힣]")

    /** 인자가 없는 글자(val)를 모두 꺼낸다. 표에 글자를 더하면 저절로 검사에 들어온다. */
    private fun plain(s: Str): Map<String, String> = Str::class.java.methods
        .filter { it.parameterCount == 0 && it.returnType == String::class.java }
        .associate { it.name to it.invoke(s) as String }

    @Test fun `영어 표에는 한글이 남아 있지 않다`() {
        val left = plain(StrEn).filter { hangul.containsMatchIn(it.value) }.keys
        assertEquals(emptySet<String>(), left)
        assertTrue(plain(StrEn).size > 100)
        assertTrue(plain(StrEn).values.none { it.isBlank() })
        // 인자를 받는 글자도 몇 개
        listOf(StrEn.wave(2), StrEn.teamCount(3), StrEn.span(1, 10), StrEn.arrivesAfter("1m"), StrEn.setupSummary("5", "15"),
            StrEn.deleteTeam(StrEn.teamName("1군")), StrEn.unassignedTitle("Team 1"), StrEn.moveTo("1111", StrEn.move),
            StrEn.waveStage(2, 2), StrEn.nextWave(3, "1:16"), StrEn.lastChange(StrEn.me, StrEn.minAgo(5)), StrEn.phaseNames.joinToString())
            .forEach { assertFalse(it, hangul.containsMatchIn(it)) }
    }

    private fun plainApp(s: AppStr): Map<String, String> = AppStr::class.java.methods
        .filter { it.parameterCount == 0 && it.returnType == String::class.java }
        .associate { it.name to it.invoke(s) as String }

    @Test fun `앱 화면의 영어 표에도 한글이 남아 있지 않다`() {
        assertEquals(emptySet<String>(), plainApp(AppEn).filter { hangul.containsMatchIn(it.value) }.keys)
        assertTrue(plainApp(AppEn).size > 150)
        listOf(AppEn.groupSet("2000 · WBI"), AppEn.roomDeleted("1111"), AppEn.tapWhere(1, 2, 3, 4, true, false), AppEn.settingsApplied("0.5", AppEn.modeInfinite, 80),
            AppEn.roomSummary("3", "2"), AppEn.pickLine("1111", 3, 2), AppEn.timerDone(1, 30), AppEn.every("0.5"), AppEn.noSuchRoomStatus("2000 · WBI"))
            .forEach { assertFalse(it, hangul.containsMatchIn(it)) }
    }

    @Test fun `앱 화면 표는 집결 패널 표와 같은 언어를 따른다`() {
        assertSame(AppKo, T.a)
        T.set(StrEn)
        assertSame(AppEn, T.a)
        assertEquals("every 0.5s · endless", ClickSummary.text(500, RepeatMode.INFINITE, 0, 0))
        assertEquals("3 teams · 2/3 assigned", RoomChooser.summaryFromLine("1111 · 군단 3개 (배정 2)"))
        T.set(StrKo)
        assertEquals("0.5초마다 · 무한 반복", ClickSummary.text(500, RepeatMode.INFINITE, 0, 0))
        assertEquals("군단 3개 · 배정 2/3", RoomChooser.summaryFromLine("1111 · 군단 3개 (배정 2)"))
    }

    private fun plainMore(s: MoreStr): Map<String, String> = MoreStr::class.java.methods
        .filter { it.parameterCount == 0 && it.returnType == String::class.java }
        .associate { it.name to it.invoke(s) as String }

    @Test fun `지휘관 창과 보내는 글의 영어 표에도 한글이 남아 있지 않다`() {
        assertEquals(emptySet<String>(), plainMore(MoreEn).filter { hangul.containsMatchIn(it.value) }.keys)
        assertTrue(plainMore(MoreEn).size > 100)
        listOf(MoreEn.leaderInvite("me@x.com", "AC-ABC123", "\n" + MoreEn.shareLine("2000", "WBI")), MoreEn.commanderInvite(MoreEn.whoRep, "", "AD-12345678", MoreEn.ttlDay),
            MoreEn.roomShare("", "1111"), MoreEn.approveBody("Moon", "2000 · WBI", ""), MoreEn.detailTeam(1, "Team 1", "Moon", "10", true), MoreEn.seqSaved("A", 3), MoreEn.hunterMarchN(1, 3))
            .forEach { assertFalse(it, hangul.containsMatchIn(it)) }
    }

    @Test fun `영어로 보낸 초대 글도 한국어로 보낸 초대 글도 읽는다`() {
        val en = MoreEn.leaderInvite("me@x.com", "AC-ABC123", "\n" + MoreEn.shareLine("2000", "WBI") + MoreEn.inviteRoomLine("1111"))
        val ko = MoreKo.leaderInvite("me@x.com", "AC-ABC123", "\n" + MoreKo.shareLine("2000", "WBI") + MoreKo.inviteRoomLine("1111"))
        for (text in listOf(en, ko)) {
            val parsed = InviteText.parse(text)!!
            assertEquals("me@x.com", parsed.id)
            assertEquals("AC-ABC123", parsed.code)
            assertEquals(RallyGroup.of("2000", "WBI"), parsed.group)
        }
        // 지휘관 코드 초대 글의 소속 줄도 어느 언어든 읽는다
        assertEquals(RallyGroup.of("2000", "wbi"), InviteText.groupIn(MoreEn.shareLine("2000", "wbi")))
    }

    @Test fun `기간 이름은 언어를 따른다`() {
        assertEquals("24시간", CodeTtl.DAY.label)
        T.set(StrEn)
        assertEquals("24 hours", CodeTtl.DAY.label)
        assertSame(MoreEn, T.x)
    }

    @Test fun `한국어 표는 전과 같은 글자다`() {
        assertEquals("지휘관 시작 대기 중", StrKo.waitingStart)
        assertEquals("2조 행군 중", StrKo.waveStage(2, 2))
        assertEquals("집결 대기 5분 · 이동 준비 15초", StrKo.setupSummary("5", "15"))
        assertEquals("1분 10초", StrKo.span(1, 10))
        assertEquals("1군", StrKo.teamName("1군"))
    }

    @Test fun `언어는 영어만 영어이고 나머지는 한국어다`() {
        assertSame(StrEn, T.forLanguage("en"))
        assertSame(StrKo, T.forLanguage("ko"))
        assertSame(StrKo, T.forLanguage("ja"))
        assertSame(StrKo, T.forLanguage(null))
    }

    @Test fun `서버의 군단 이름은 영어 화면에서 Team N으로 보이고 다른 이름은 그대로다`() {
        assertEquals("Team 3", StrEn.teamName("3군"))
        assertEquals("Team 10", StrEn.teamName("10군"))
        assertEquals("달구지", StrEn.teamName("달구지"))
    }

    @Test fun `영어로 바꾸면 계산하는 쪽의 글자도 영어로 나온다`() {
        T.set(StrEn)
        assertEquals("1m 10s", RallyWaves.span(70.0))
        assertEquals("Arrives first", RallyWaves.delayLabel(1, 0.0))
        assertEquals("1m later", RallyWaves.delayLabel(2, 60.0))
        assertEquals(listOf("Taps first", "+18s"), RallyPanelFormat.lagLabels(listOf(LagInput(51.0, 0, false), LagInput(33.0, 0, false))))
        assertEquals("+0.5s", RallyInputParse.formatCorrection(500))
        assertEquals("0s", RallyInputParse.formatCorrection(0))
        assertEquals("Rally time 5m · Prep 15s", RallyPanelFormat.setupSummary(300.0, 15.0))
    }

    @Test fun `숫자 입력은 어느 언어에서든 초와 s를 모두 받는다`() {
        assertEquals(30.0, RallyInputParse.marchSeconds("30초")!!, 1e-9)
        assertEquals(30.0, RallyInputParse.marchSeconds("30s")!!, 1e-9)
        T.set(StrEn)
        assertEquals(30.0, RallyInputParse.marchSeconds("30초")!!, 1e-9)
        assertEquals(-1500, RallyInputParse.correctionMs("-1.5s"))
    }
}
