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

    /** 방 줄 오른쪽의 한마디: "배정 2/3"(집결장이 배정된 군단 수 / 군단 수). */
    fun badge(assigned: Int, teamCount: Int): String = "배정 $assigned/$teamCount"

    /**
     * 저장해 둔 방 목록의 한 줄("1111 · 군단 3개 (배정 2)")에서 첫 화면에 보일 요약("군단 3개 · 배정 2/3")을 만든다.
     * 모양이 다르면 null(요약을 보이지 않는다).
     */
    fun summaryFromLine(line: String?): String? {
        val m = Regex("군단 (\\d+)개 \\(배정 (\\d+)\\)").find(line ?: return null) ?: return null
        val (teams, assigned) = m.destructured
        return "군단 ${teams}개 · 배정 $assigned/$teams"
    }

    /** 편집에서 휴지통을 보일 방인지: 지울 수 있는 목록이고, 지금 들어와 있는 방이 아니어야 한다(사용 중인 방은 잠근다). */
    fun canDelete(deletable: Boolean, code: String, current: String): Boolean = deletable && code != current

    /** 방 선택 창의 제목 아래 안내: 목록을 제대로 받았으면 없다(null). */
    fun note(server: List<Pair<String, String>>?, error: String?): String? = when {
        server == null -> "서버 방 목록을 불러오지 못했어요" + (if (error.isNullOrEmpty()) "" else " ($error)") + " · 이 기기가 들어갔던 방만 보여요"
        else -> null
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
