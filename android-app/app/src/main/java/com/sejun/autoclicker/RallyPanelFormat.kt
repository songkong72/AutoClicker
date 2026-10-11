package com.sejun.autoclicker

/** 군단 한 줄의 시간차이 계산 입력: 행군시간, 지휘관 보정(ms), 제외 여부. */
internal data class LagInput(val marchSec: Double, val adminAdjustMs: Int, val excluded: Boolean)

/** 군단 목록의 한 줄(조 머리띠 또는 군단 줄): 높이(px), 군단 줄인지, 몇 번째 상자에 들어 있는지. 접혀서 안 보이는 줄은 넣지 않는다. */
internal data class ListLine(val heightPx: Int, val isTeam: Boolean, val card: Int)

/** 군단 목록이 한 번에 보여 줄 높이. 조 머리띠는 세지 않고 군단 줄만 센다. */
internal object RallyListHeight {
    /**
     * 위에서부터 군단 [maxTeams]줄이 다 보이는 높이(px)를 돌려준다. 그 사이의 조 머리띠와 상자 사이 간격([gapPx])은 덤으로 들어간다.
     * [extraPx]는 방 설정을 접어서 생긴 자리처럼 더 줄 수 있는 높이. 보이는 군단이 [maxTeams]줄 이하면 0(한도 없음).
     * 어느 한 줄(펼친 군단 줄)이 그보다 크면 그 줄은 다 보이게 한다.
     */
    fun limit(lines: List<ListLine>, maxTeams: Int, gapPx: Int, extraPx: Int = 0): Int {
        if (lines.count { it.isTeam } <= maxTeams) return 0
        var teams = 0; var sum = 0; var lastCard = lines.first().card; var firstCard = lastCard
        for (l in lines) {
            sum += l.heightPx; lastCard = l.card
            if (l.isTeam && ++teams == maxTeams) break
        }
        val gaps = (lastCard - firstCard) * gapPx
        return Math.max(sum + gaps + extraPx, lines.maxOf { it.heightPx })
    }
}

/** 패널에 보이는 초 단위 글자 모양(정수면 소수점 없이). */
internal object RallyPanelFormat {
    fun sec(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

    /** 접힌 방 설정 한 줄의 값: "집결 대기 5분 · 이동 준비 15초" */
    fun setupSummary(waitSec: Double, prepSec: Double): String =
        T.s.setupSummary(sec(waitSec / 60.0), sec(prepSec))

    /**
     * 시작이 막힌 이유(지금은 "참여 군단이 없어요" 하나) 뒤에 어떻게 푸는지를 붙인다.
     * 편집 중이면 체크박스가 보이니 체크하라고, 아니면 연필부터 누르라고 알린다.
     */
    fun blockedNote(reason: String, editing: Boolean): String =
        reason + " · " + (if (editing) T.s.blockedHintEditing else T.s.blockedHintView)

    /**
     * 접힌 "내 기기" 한 줄 요약: "위치 저장됨 · 방 0001 · 문".
     * 줄이 좁아 뒤쪽이 잘리므로 가장 중요한 클릭 위치를 맨 앞에 둔다. 보정은 0초가 아닐 때만 맨 뒤에 붙인다.
     * 빠진 것(클릭 위치·방·캐릭터명)은 "없음"으로 드러내 무엇을 해야 하는지 알 수 있게 한다.
     */
    fun deviceSummary(characterName: String, room: String, correctionText: String, positionSaved: Boolean): String = listOfNotNull(
        if (positionSaved) T.s.posSaved else T.s.posNone,
        if (room.isBlank()) T.s.roomNone else T.s.room(room),
        characterName.ifBlank { T.s.nameNone },
        if (correctionText == T.s.sec("0")) null else T.s.correction(correctionText)
    ).joinToString(" · ")

    /**
     * 군단별 "가장 먼저 누르는 군단보다 몇 초 늦게 누르는지". 지휘관 보정까지 포함해 계산한다.
     * 누르는 시각 = (가장 긴 행군 − 내 행군) + 지휘관 보정. 가장 먼저 누르는 군단은 "먼저 클릭", 나머지는 "20초 뒤", 제외된 군단은 "".
     * (집결장 기기마다 다른 내 보정은 방 데이터에 없어 포함하지 않는다.)
     */
    fun lagLabels(items: List<LagInput>): List<String> {
        val active = items.filter { !it.excluded }
        if (active.isEmpty()) return items.map { "" }
        val maxMarch = active.maxOf { it.marchSec }
        val click = { i: LagInput -> (maxMarch - i.marchSec) + i.adminAdjustMs / 1000.0 }
        val first = active.minOf(click)
        return items.map { i ->
            if (i.excluded) ""
            else {
                val d = Math.round((click(i) - first) * 10) / 10.0
                if (d <= 0.0) T.s.clickFirst else T.s.secLater(sec(d))
            }
        }
    }

    /**
     * 조를 쓰는 방: 같은 조 안에서만 견준다. 조마다 가장 먼저 누르는 군단이 "먼저 클릭"이고 나머지는 그 군단보다 몇 초 뒤인지.
     * 조 사이의 차이는 조 머리띠의 "N분 뒤 도착"이 말해 주므로 여기에 섞지 않는다. [waves]는 [items]와 같은 순서의 조 번호.
     */
    fun lagLabelsByWave(items: List<LagInput>, waves: List<Int>): List<String> {
        val out = MutableList(items.size) { "" }
        items.indices.groupBy { waves.getOrElse(it) { 1 } }.values.forEach { idx ->
            lagLabels(idx.map { items[it] }).forEachIndexed { k, label -> out[idx[k]] = label }
        }
        return out
    }
}
