package com.sejun.autoclicker

/**
 * 앱 첫 화면의 "방 선택" 목록을 만든다. 방 번호를 치는 대신 서버의 방 목록에서 고르게 한다.
 * 안드로이드 클래스를 쓰지 않아 단위 테스트가 된다.
 */
internal object RoomChooser {
    enum class Kind { ROOM, NEW, TYPE }

    /** [deletable]이면 그 방 줄 오른쪽에 휴지통을 보여 준다. */
    data class Entry(val label: String, val kind: Kind, val code: String = "", val deletable: Boolean = false)

    const val NEW_LABEL = "+ 새 방 만들기"
    const val TYPE_LABEL = "번호 직접 입력"

    /**
     * [server]는 서버에서 받은 (방 번호, 한 줄 설명) 목록. 받지 못했으면 null이고, 그때는 이 기기가 들어갔던 방([history])을 대신 보여 준다.
     * 지금 들어와 있는 방([current])에는 ✓와 "현재"를 붙인다. 새 방 만들기는 관리자([admin])에게만 보인다.
     * 방 줄의 휴지통도 관리자에게만, 서버 목록을 받았을 때만 나온다(받지 못한 목록으로는 지우지 않는다).
     * 번호 직접 입력은 서버 목록을 받지 못했을 때만 비상구로 보인다(평소에는 목록에서 고르고, 새 번호는 새 방 만들기에서 정한다).
     */
    fun entries(server: List<Pair<String, String>>?, history: List<String>, current: String, admin: Boolean): List<Entry> {
        val rooms = server ?: history.map { it to it }
        val deletable = admin && server != null
        return rooms.map { (code, line) -> Entry(if (code == current) "✓ $line · 현재" else line, Kind.ROOM, code, deletable) } +
            (if (admin) listOf(Entry(NEW_LABEL, Kind.NEW)) else emptyList()) +
            (if (server == null) listOf(Entry(TYPE_LABEL, Kind.TYPE)) else emptyList())
    }

    fun title(server: List<Pair<String, String>>?, error: String?): String = when {
        server == null -> "방 선택 · 서버 방 목록을 불러오지 못했어요" + (if (error.isNullOrEmpty()) "" else " ($error)")
        server.isEmpty() -> "방 선택 · 만들어진 방이 없어요"
        else -> "방 선택 (${server.size}개)"
    }

    enum class NewRoom { RANDOM, OK, TOO_SHORT, EXISTS }

    /** 새 방 만들기에서 적은 번호([typed])를 본다. 비워 두면 자동 번호, 4자리 미만이면 거절, 이미 있는 번호([existing])면 새로 만들지 않는다. */
    fun newRoom(typed: String, existing: Set<String>): NewRoom {
        val code = typed.trim()
        return when {
            code.isEmpty() -> NewRoom.RANDOM
            code.length < 4 -> NewRoom.TOO_SHORT
            code in existing -> NewRoom.EXISTS
            else -> NewRoom.OK
        }
    }
}
