package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Test

class RoomChooserTest {
    private val server = listOf("0001" to "0001 · 군단 3개 (배정 3)", "482913" to "482913 · 군단 0개 (배정 0)")

    @Test
    fun `관리자는 서버 방 목록 아래에 새 방 만들기와 번호 직접 입력이 보인다`() {
        val e = RoomChooser.entries(server, emptyList(), "", admin = true)
        assertEquals(listOf("0001 · 군단 3개 (배정 3)", "482913 · 군단 0개 (배정 0)", RoomChooser.NEW_LABEL, RoomChooser.TYPE_LABEL), e.map { it.label })
        assertEquals(listOf(RoomChooser.Kind.ROOM, RoomChooser.Kind.ROOM, RoomChooser.Kind.NEW, RoomChooser.Kind.TYPE), e.map { it.kind })
        assertEquals("482913", e[1].code)
    }

    @Test
    fun `집결장에게는 새 방 만들기가 보이지 않는다`() {
        val e = RoomChooser.entries(server, emptyList(), "", admin = false)
        assertEquals(listOf(RoomChooser.Kind.ROOM, RoomChooser.Kind.ROOM, RoomChooser.Kind.TYPE), e.map { it.kind })
    }

    @Test
    fun `지금 들어와 있는 방에는 표시가 붙는다`() {
        val e = RoomChooser.entries(server, emptyList(), "0001", admin = false)
        assertEquals("✓ 0001 · 군단 3개 (배정 3)", e[0].label)
        assertEquals("482913 · 군단 0개 (배정 0)", e[1].label)
    }

    @Test
    fun `서버 목록을 받지 못하면 들어갔던 방을 대신 보여 준다`() {
        val e = RoomChooser.entries(null, listOf("0001", "7777"), "7777", admin = false)
        assertEquals(listOf("0001", "✓ 7777", RoomChooser.TYPE_LABEL), e.map { it.label })
    }

    @Test
    fun `방이 하나도 없어도 번호 직접 입력은 남는다`() {
        assertEquals(listOf(RoomChooser.TYPE_LABEL), RoomChooser.entries(emptyList(), listOf("0001"), "", admin = false).map { it.label })
    }

    @Test
    fun `제목은 방 개수나 못 불러온 이유를 알려 준다`() {
        assertEquals("방 선택 (2개)", RoomChooser.title(server, null))
        assertEquals("방 선택 · 만들어진 방이 없어요", RoomChooser.title(emptyList(), null))
        assertEquals("방 선택 · 서버 방 목록을 불러오지 못했어요 (권한 없음)", RoomChooser.title(null, "권한 없음"))
        assertEquals("방 선택 · 서버 방 목록을 불러오지 못했어요", RoomChooser.title(null, ""))
    }
}
