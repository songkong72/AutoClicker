package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Test

class RoomChooserTest {
    private val server = listOf("0001" to "0001 · 군단 3개 (배정 3)", "482913" to "482913 · 군단 0개 (배정 0)")

    @Test
    fun `지휘관은 서버 방 목록 아래에 새 방 만들기만 보이고 방 줄마다 휴지통이 붙는다`() {
        val e = RoomChooser.entries(server, emptyList(), "", admin = true)
        assertEquals(listOf("0001 · 군단 3개 (배정 3)", "482913 · 군단 0개 (배정 0)", RoomChooser.NEW_LABEL), e.map { it.label })
        assertEquals(listOf(RoomChooser.Kind.ROOM, RoomChooser.Kind.ROOM, RoomChooser.Kind.NEW), e.map { it.kind })
        assertEquals(listOf(true, true, false), e.map { it.deletable })
        assertEquals("482913", e[1].code)
    }

    @Test
    fun `집결장에게는 방 목록만 보이고 휴지통도 없다`() {
        val e = RoomChooser.entries(server, emptyList(), "", admin = false)
        assertEquals(listOf(RoomChooser.Kind.ROOM, RoomChooser.Kind.ROOM), e.map { it.kind })
        assertEquals(listOf(false, false), e.map { it.deletable })
    }

    @Test
    fun `서버 목록을 받지 못하면 지휘관에게도 휴지통은 없고 번호 직접 입력이 비상구로 보인다`() {
        val none = RoomChooser.entries(null, listOf("0001"), "", admin = true)
        assertEquals(listOf(RoomChooser.Kind.ROOM, RoomChooser.Kind.NEW, RoomChooser.Kind.TYPE), none.map { it.kind })
        assertEquals(false, none[0].deletable)
        val empty = RoomChooser.entries(emptyList(), emptyList(), "", admin = true).map { it.kind }
        assertEquals(listOf(RoomChooser.Kind.NEW), empty)
    }

    @Test
    fun `지금 들어와 있는 방에는 표시가 붙는다`() {
        val e = RoomChooser.entries(server, emptyList(), "0001", admin = false)
        assertEquals("✓ 0001 · 군단 3개 (배정 3) · 현재", e[0].label)
        assertEquals("482913 · 군단 0개 (배정 0)", e[1].label)
    }

    @Test
    fun `서버 목록을 받지 못하면 들어갔던 방을 대신 보여 준다`() {
        val e = RoomChooser.entries(null, listOf("0001", "7777"), "7777", admin = false)
        assertEquals(listOf("0001", "✓ 7777 · 현재", RoomChooser.TYPE_LABEL), e.map { it.label })
    }

    @Test
    fun `서버 목록을 받았으면 방이 없어도 번호 직접 입력은 보이지 않는다`() {
        assertEquals(emptyList<String>(), RoomChooser.entries(emptyList(), listOf("0001"), "", admin = false).map { it.label })
    }

    @Test
    fun `새 방 번호는 비우면 자동이고 짧거나 이미 있으면 만들지 않는다`() {
        val existing = setOf("1111", "2222")
        assertEquals(RoomChooser.NewRoom.RANDOM, RoomChooser.newRoom("  ", existing))
        assertEquals(RoomChooser.NewRoom.TOO_SHORT, RoomChooser.newRoom("123", existing))
        assertEquals(RoomChooser.NewRoom.EXISTS, RoomChooser.newRoom(" 1111 ", existing))
        assertEquals(RoomChooser.NewRoom.OK, RoomChooser.newRoom("5555", existing))
    }

    @Test
    fun `제목은 방 개수나 못 불러온 이유를 알려 준다`() {
        assertEquals("방 선택 (2개)", RoomChooser.title(server, null))
        assertEquals("방 선택 · 만들어진 방이 없어요", RoomChooser.title(emptyList(), null))
        assertEquals("방 선택 · 서버 방 목록을 불러오지 못했어요 (권한 없음)", RoomChooser.title(null, "권한 없음"))
        assertEquals("방 선택 · 서버 방 목록을 불러오지 못했어요", RoomChooser.title(null, ""))
    }

    @Test fun badgeShowsAssignedOverTeams() = assertEquals("배정 2/3", RoomChooser.badge(2, 3))

    @Test fun currentRoomCannotBeDeleted() {
        assertEquals(false, RoomChooser.canDelete(true, "1111", "1111"))
        assertEquals(true, RoomChooser.canDelete(true, "2222", "1111"))
        assertEquals(false, RoomChooser.canDelete(false, "2222", "1111"))
    }

    @Test fun noteOnlyWhenListCouldNotLoad() {
        assertEquals(null, RoomChooser.note(listOf("1111" to "x"), null))
        assertEquals(null, RoomChooser.note(emptyList(), null))
        assertEquals(true, RoomChooser.note(null, "HTTP 401")!!.contains("HTTP 401"))
    }

    @Test fun summaryIsBuiltFromCachedLine() {
        assertEquals("군단 3개 · 배정 2/3", RoomChooser.summaryFromLine("1111 · 군단 3개 (배정 2)"))
        assertEquals(null, RoomChooser.summaryFromLine("1111"))
        assertEquals(null, RoomChooser.summaryFromLine(null))
    }
}
