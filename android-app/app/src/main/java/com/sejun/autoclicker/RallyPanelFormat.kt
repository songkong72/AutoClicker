package com.sejun.autoclicker

/** 군단 한 줄의 시간차이 계산 입력: 행군시간, 관리자 보정(ms), 제외 여부. */
internal data class LagInput(val marchSec: Double, val adminAdjustMs: Int, val excluded: Boolean)

/** 패널에 보이는 초 단위 글자 모양(정수면 소수점 없이). */
internal object RallyPanelFormat {
    fun sec(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

    /**
     * 시작이 막힌 이유(지금은 "참여 군단이 없어요" 하나) 뒤에 어떻게 푸는지를 붙인다.
     * 편집 중이면 체크박스가 보이니 체크하라고, 아니면 연필부터 누르라고 알린다.
     */
    fun blockedNote(reason: String, editing: Boolean): String =
        reason + " · " + (if (editing) "참여할 군단을 체크하세요" else "위의 연필을 눌러 군단을 체크하세요")

    /**
     * 접힌 "내 기기" 한 줄 요약: "위치 저장됨 · 방 0001 · 문".
     * 줄이 좁아 뒤쪽이 잘리므로 가장 중요한 클릭 위치를 맨 앞에 둔다. 보정은 0초가 아닐 때만 맨 뒤에 붙인다.
     * 빠진 것(클릭 위치·방·캐릭터명)은 "없음"으로 드러내 무엇을 해야 하는지 알 수 있게 한다.
     */
    fun deviceSummary(characterName: String, room: String, correctionText: String, positionSaved: Boolean): String = listOfNotNull(
        if (positionSaved) "위치 저장됨" else "위치 없음",
        if (room.isBlank()) "방 없음" else "방 $room",
        characterName.ifBlank { "캐릭터명 없음" },
        if (correctionText == "0초") null else "보정 $correctionText"
    ).joinToString(" · ")

    /**
     * 군단별 "가장 먼저 누르는 군단보다 몇 초 늦게 누르는지". 관리자 보정까지 포함해 계산한다.
     * 누르는 시각 = (가장 긴 행군 − 내 행군) + 관리자 보정. 가장 먼저 누르는 군단은 "먼저 클릭", 나머지는 "20초 뒤", 제외된 군단은 "".
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
                if (d <= 0.0) "먼저 클릭" else sec(d) + "초 뒤"
            }
        }
    }
}
