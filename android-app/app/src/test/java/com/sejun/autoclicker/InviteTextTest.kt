package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InviteTextTest {
    private val wbi = RallyGroup("2000", "WBI")
    private val full = "[오토클리커 Pro 집결장 초대]\nID: hong@gmail.com\n집결장 코드: AC-8F3K9A\n" +
        "앱을 열고 인증 창에 둘 다 입력하면 집결 기능을 쓸 수 있어요.\n" + wbi.shareLine +
        "\n집결 방 번호: 1111 (인증 후 소속을 정하고 방 선택에서 고르기)"

    @Test fun `초대 글에서 ID와 코드와 소속을 읽는다`() {
        assertEquals(InviteText("hong@gmail.com", "AC-8F3K9A", wbi), InviteText.parse(full))
    }

    @Test fun `연맹의 대문자와 소문자를 그대로 둔다`() {
        assertEquals(RallyGroup("2000", "wBi"), InviteText.groupIn(RallyGroup("2000", "wBi").shareLine))
    }

    @Test fun `소속이 없는 초대 글도 읽는다`() {
        assertEquals(InviteText("길동 1", "AC-ABC123", null), InviteText.parse("ID: 길동 1 \n집결장 코드: AC-ABC123"))
    }

    @Test fun `초대 글이 아니면 읽지 않는다`() {
        assertNull(InviteText.parse(null))
        assertNull(InviteText.parse("안녕하세요"))
        assertNull(InviteText.parse("ID: hong"))
        assertNull(InviteText.parse("집결장 코드: AC-ABC123"))
        assertNull(InviteText.groupIn("소속: 서버 · WBI"))
        assertNull(InviteText.groupIn("소속: 2000 서버 · W-BI!"))
    }

    @Test fun `방 번호만 보낸 글에서도 소속을 읽는다`() {
        assertEquals(wbi, InviteText.groupIn("집결 방 번호: 1111\n" + wbi.shareLine + "\n"))
    }
}
