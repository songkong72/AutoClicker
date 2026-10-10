package com.sejun.autoclicker

/**
 * 지휘관이 보낸 초대 글(집결장 코드 보내기)에서 ID·집결장 코드·소속을 읽어 낸다.
 * 받은 사람이 글을 통째로 복사해 두면 인증 창과 소속 정하기 창이 손으로 적지 않아도 채워진다.
 * 연맹은 대문자와 소문자를 구분하므로 손으로 옮겨 적다 틀리는 일을 막는 것이 목적이다.
 */
data class InviteText(val id: String, val code: String, val group: RallyGroup?) {
    companion object {
        private val ID = Regex("""(?m)^\s*ID:[ \t]*(\S.*?)\s*$""")
        private val CODE = Regex("""집결장 코드:[ \t]*(AC-[A-Za-z0-9]+)""")
        private val GROUP = Regex("""소속:[ \t]*(\d+)[ \t]*서버[ \t]*·[ \t]*([^\s()]+)""")

        /** 글에 적힌 소속. 없거나 모양이 틀리면 null. */
        fun groupIn(text: String?): RallyGroup? =
            GROUP.find(text.orEmpty())?.let { RallyGroup.of(it.groupValues[1], it.groupValues[2]) }

        /** 초대 글이면 그 내용을, ID나 집결장 코드가 없으면 null을 돌려준다. */
        fun parse(text: String?): InviteText? {
            val t = text.orEmpty()
            val id = ID.find(t)?.groupValues?.get(1)?.trim().orEmpty()
            val code = CODE.find(t)?.groupValues?.get(1).orEmpty()
            return if (id.isEmpty() || code.isEmpty()) null else InviteText(id, code, groupIn(t))
        }
    }
}
