package com.sejun.autoclicker

/**
 * 앱 첫 화면의 "방 선택" 목록을 만든다. 방 번호를 치는 대신 서버의 방 목록에서 고르게 한다.
 * 안드로이드 클래스를 쓰지 않아 단위 테스트가 된다.
 */
internal object RoomChooser {
    enum class Kind { ROOM, NEW, TYPE }

    data class Entry(val label: String, val kind: Kind, val code: String = "")

    const val NEW_LABEL = "+ 새 방 만들기"
    const val TYPE_LABEL = "번호 직접 입력"

    /**
     * [server]는 서버에서 받은 (방 번호, 한 줄 설명) 목록. 받지 못했으면 null이고, 그때는 이 기기가 들어갔던 방([history])을 대신 보여 준다.
     * 지금 들어와 있는 방([current])에는 ✓를 붙인다. 새 방 만들기는 관리자([admin])에게만, 번호 직접 입력은 누구에게나 맨 아래에 둔다.
     */
    fun entries(server: List<Pair<String, String>>?, history: List<String>, current: String, admin: Boolean): List<Entry> {
        val rooms = server ?: history.map { it to it }
        return rooms.map { (code, line) -> Entry(if (code == current) "✓ $line" else line, Kind.ROOM, code) } +
            (if (admin) listOf(Entry(NEW_LABEL, Kind.NEW)) else emptyList()) +
            Entry(TYPE_LABEL, Kind.TYPE)
    }

    fun title(server: List<Pair<String, String>>?, error: String?): String = when {
        server == null -> "방 선택 · 서버 방 목록을 불러오지 못했어요" + (if (error.isNullOrEmpty()) "" else " ($error)")
        server.isEmpty() -> "방 선택 · 만들어진 방이 없어요"
        else -> "방 선택 (${server.size}개)"
    }
}
