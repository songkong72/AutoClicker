package com.sejun.autoclicker

/** 저장해 둔 "순서 클릭" 한 묶음: 이름, 누를 자리(화면 픽셀 중심점, 번호 순서), 자리 사이 쉬는 시간. */
internal data class ClickSequence(val name: String, val points: List<TroopPoint>, val gapMs: Long)

/**
 * 순서 클릭 묶음의 규칙과 저장 형식. 안드로이드 클래스를 쓰지 않아 단위 테스트가 된다.
 * 실행하면 1번 자리부터 끝까지 차례로 누르고 멈춘다.
 */
internal object ClickSequences {
    /** 저장할 수 있는 묶음 수. 더 많으면 게임 중에 목록에서 고르기 어렵다. */
    const val MAX_SETS = 5
    /** 한 묶음의 자리 수. */
    const val MAX_POINTS = 10
    const val DEFAULT_POINTS = 3
    const val MIN_GAP_MS = 100L
    const val MAX_GAP_MS = 5000L
    const val GAP_STEP_MS = 100L
    const val DEFAULT_GAP_MS = 500L
    const val MAX_NAME = 12

    fun clampCount(n: Int): Int = n.coerceIn(1, MAX_POINTS)

    fun clampGap(ms: Long): Long = ms.coerceIn(MIN_GAP_MS, MAX_GAP_MS)

    fun canAdd(list: List<ClickSequence>): Boolean = list.size < MAX_SETS

    /** 이름에서 저장 형식을 깨는 글자(탭·줄바꿈)를 빼고 [MAX_NAME]자로 줄인다. */
    fun cleanName(text: String?): String =
        (text ?: "").replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim().take(MAX_NAME).trim()

    /** 아직 쓰지 않은 "순서 1", "순서 2"… 중 첫 이름. */
    fun defaultName(existing: List<ClickSequence>): String {
        for (n in 1..MAX_SETS + 1) {
            val candidate = "순서 $n"
            if (existing.none { it.name == candidate }) return candidate
        }
        return "순서"
    }

    /** 목록의 한 줄 설명. 예: "4곳 · 0.5초 간격" */
    fun summary(seq: ClickSequence): String = "${seq.points.size}곳 · ${ClickSummary.seconds(seq.gapMs)}초 간격"

    /** 쉬는 시간 표시. 예: "0.5초" */
    fun gapLabel(ms: Long): String = "${ClickSummary.seconds(ms)}초"

    /** 실행 버튼의 평소 글자. */
    fun readyLabel(count: Int): String = "순서\n${count}곳"

    /** 실행 중 글자: 지금 누르는 자리(0부터)와 전체. 누르면 멈춘다. */
    fun runningLabel(index: Int, count: Int): String = "${index + 1} / $count\n멈춤"

    /** [index] 자리의 묶음을 바꾼다. 없는 자리면 맨 뒤에 더한다(가득 찼으면 그대로). */
    fun saved(list: List<ClickSequence>, index: Int, seq: ClickSequence): List<ClickSequence> = when {
        index in list.indices -> list.toMutableList().also { it[index] = seq }
        canAdd(list) -> list + seq
        else -> list
    }

    fun removed(list: List<ClickSequence>, index: Int): List<ClickSequence> =
        if (index in list.indices) list.filterIndexed { i, _ -> i != index } else list

    /** 한 줄에 한 묶음: "이름<탭>쉬는시간<탭>x,y;x,y" */
    fun encode(list: List<ClickSequence>): String = list.joinToString("\n") { seq ->
        "${cleanName(seq.name)}\t${seq.gapMs}\t" + seq.points.joinToString(";") { "${it.x},${it.y}" }
    }

    /** 깨진 줄과 자리는 건너뛴다. 자리가 하나도 없는 묶음은 버린다. */
    fun decode(text: String?): List<ClickSequence> {
        if (text.isNullOrBlank()) return emptyList()
        val out = ArrayList<ClickSequence>()
        for (line in text.split('\n')) {
            val parts = line.split('\t')
            if (parts.size != 3) continue
            val name = cleanName(parts[0])
            val gap = parts[1].trim().toLongOrNull() ?: continue
            val points = ArrayList<TroopPoint>()
            for (part in parts[2].split(';')) {
                val xy = part.split(',')
                if (xy.size != 2) continue
                val x = xy[0].trim().toIntOrNull() ?: continue
                val y = xy[1].trim().toIntOrNull() ?: continue
                points.add(TroopPoint(x, y))
                if (points.size == MAX_POINTS) break
            }
            if (name.isEmpty() || points.isEmpty()) continue
            out.add(ClickSequence(name, points, clampGap(gap)))
            if (out.size == MAX_SETS) break
        }
        return out
    }
}
