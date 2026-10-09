package com.sejun.autoclicker

/**
 * 지휘관이 방을 바꿀 때 "누가, 언제" 남기는 표시. 지휘관 둘이 동시에 만져도 서로 알아볼 수 있게 한다.
 * 안드로이드 클래스를 쓰지 않아 단위 테스트가 된다. 시각은 바꾼 기기의 시계라 몇 초쯤 어긋날 수 있다.
 */
internal object RallyChangeNote {
    private const val MAX_LABEL = 20

    /** 표시에 쓸 이름: 캐릭터명이 있으면 그것, 없으면 기기 ID 끝 4자리. */
    fun label(characterName: String, memberId: String): String =
        characterName.trim().ifEmpty { "지휘관 …" + memberId.takeLast(4) }.take(MAX_LABEL)

    fun stamp(doc: RallyRoomDoc, label: String, nowMs: Long): RallyRoomDoc = doc.copy(lastBy = label, lastAt = nowMs)

    /** 패널에 보이는 한 줄. 아직 아무도 바꾸지 않았으면 빈 문자열. */
    fun text(doc: RallyRoomDoc, myLabel: String, nowMs: Long): String {
        if (doc.lastAt <= 0L || doc.lastBy.isEmpty()) return ""
        val who = if (doc.lastBy == myLabel) "나" else doc.lastBy
        return "마지막 변경: $who · ${ago(nowMs - doc.lastAt)}"
    }

    /** 내가 마지막으로 본 변경([prevAt]) 뒤에 다른 지휘관이 바꾼 것이 도착했는가. */
    fun foreignChange(prevAt: Long, doc: RallyRoomDoc, myLabel: String): Boolean =
        doc.lastAt > prevAt && doc.lastAt > 0L && doc.lastBy.isNotEmpty() && doc.lastBy != myLabel

    private fun ago(ms: Long): String = when {
        ms < 60_000L -> "방금"
        ms < 3_600_000L -> "${ms / 60_000L}분 전"
        ms < 86_400_000L -> "${ms / 3_600_000L}시간 전"
        else -> "${ms / 86_400_000L}일 전"
    }
}
