package com.sejun.autoclicker

/** 화면 픽셀 기준 중심점. */
internal data class TroopPoint(val x: Int, val y: Int)

/**
 * 곰 사냥에서 쓸 부대 위치 목록과 순서 계산. 안드로이드 클래스를 쓰지 않아 단위 테스트가 된다.
 * 발사할 때마다 1번 → 2번 → … 순서로 쓰고, 저장한 마지막 부대 다음에는 다시 1번으로 돌아간다.
 */
internal object HunterTroops {
    const val MAX = 7

    fun clampCount(n: Int): Int = n.coerceIn(1, MAX)

    /** "x,y;x,y" 형태로 저장한다. */
    fun encode(list: List<TroopPoint>): String = list.joinToString(";") { "${it.x},${it.y}" }

    /** 깨진 항목은 건너뛰고, 앞에서부터 [MAX]개까지만 읽는다. */
    fun decode(text: String?): List<TroopPoint> {
        if (text.isNullOrBlank()) return emptyList()
        val out = ArrayList<TroopPoint>()
        for (part in text.split(';')) {
            val xy = part.split(',')
            if (xy.size != 2) continue
            val x = xy[0].trim().toIntOrNull() ?: continue
            val y = xy[1].trim().toIntOrNull() ?: continue
            out.add(TroopPoint(x, y))
            if (out.size == MAX) break
        }
        return out
    }

    /**
     * 표시의 가운데가 화면의 [center]에 오게 하려면 창 좌표(왼쪽 위)를 얼마로 둬야 하는지.
     * [windowOffset]은 창 좌표 0이 화면에서 놓이는 위치(상태바·컷아웃만큼 밀린 값)다.
     */
    fun topLeftFor(center: Int, size: Int, windowOffset: Int): Int = center - size / 2 - windowOffset

    /** 이번에 쓸 부대 번호(0부터). 범위를 벗어나면 첫 부대. */
    fun current(index: Int, count: Int): Int = if (index in 0 until count) index else 0

    /** 다음에 쓸 부대 번호. 마지막 다음은 첫 부대. */
    fun next(index: Int, count: Int): Int = if (count <= 0) 0 else (index + 1) % count
}
