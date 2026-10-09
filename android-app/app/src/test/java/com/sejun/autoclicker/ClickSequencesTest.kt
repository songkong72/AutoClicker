package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClickSequencesTest {
    private fun seq(name: String, n: Int = 2, gap: Long = 500L) =
        ClickSequence(name, (1..n).map { TroopPoint(it * 10, it * 20) }, gap)

    @Test fun `저장한 것을 그대로 다시 읽는다`() {
        val list = listOf(seq("보상 받기", 4), seq("훈련", 6, 1000L))
        assertEquals(list, ClickSequences.decode(ClickSequences.encode(list)))
    }

    @Test fun `빈 값과 깨진 줄은 건너뛴다`() {
        assertEquals(emptyList<ClickSequence>(), ClickSequences.decode(null))
        assertEquals(emptyList<ClickSequence>(), ClickSequences.decode("  "))
        val text = "깨짐\n" + ClickSequences.encode(listOf(seq("훈련"))) + "\n이름\tabc\t1,2\n자리없음\t500\tx"
        assertEquals(listOf(seq("훈련")), ClickSequences.decode(text))
    }

    @Test fun `묶음은 5개 자리는 10개까지만 읽는다`() {
        val many = (1..8).map { seq("순서 $it", 12) }
        val read = ClickSequences.decode(ClickSequences.encode(many))
        assertEquals(ClickSequences.MAX_SETS, read.size)
        assertEquals(ClickSequences.MAX_POINTS, read[0].points.size)
    }

    @Test fun `읽을 때 쉬는 시간을 범위 안으로 맞춘다`() {
        assertEquals(100L, ClickSequences.decode("a\t5\t1,2")[0].gapMs)
        assertEquals(5000L, ClickSequences.decode("a\t99999\t1,2")[0].gapMs)
    }

    @Test fun `이름은 탭과 줄바꿈을 빼고 12자로 줄인다`() {
        assertEquals("보상 받기", ClickSequences.cleanName("  보상\t받기\n"))
        assertEquals(12, ClickSequences.cleanName("가나다라마바사아자차카타파하").length)
        assertEquals("", ClickSequences.cleanName(null))
    }

    @Test fun `기본 이름은 안 쓴 번호 중 첫 번째`() {
        assertEquals("순서 1", ClickSequences.defaultName(emptyList()))
        assertEquals("순서 2", ClickSequences.defaultName(listOf(seq("순서 1"), seq("순서 3"))))
    }

    @Test fun `개수와 쉬는 시간 범위`() {
        assertEquals(1, ClickSequences.clampCount(0))
        assertEquals(10, ClickSequences.clampCount(11))
        assertEquals(100L, ClickSequences.clampGap(0L))
        assertEquals(5000L, ClickSequences.clampGap(5100L))
    }

    @Test fun `글자 표시`() {
        assertEquals("4곳 · 0.5초 간격", ClickSequences.summary(seq("a", 4)))
        assertEquals("6곳 · 1초 간격", ClickSequences.summary(seq("a", 6, 1000L)))
        assertEquals("0.1초", ClickSequences.gapLabel(100L))
        assertEquals("순서\n4곳", ClickSequences.readyLabel(4))
        assertEquals("2 / 4\n멈춤", ClickSequences.runningLabel(1, 4))
    }

    @Test fun `고치면 그 자리를 바꾸고 새로 만들면 맨 뒤에 더한다`() {
        val list = listOf(seq("a"), seq("b"))
        assertEquals(listOf(seq("a"), seq("c")), ClickSequences.saved(list, 1, seq("c")))
        assertEquals(listOf(seq("a"), seq("b"), seq("c")), ClickSequences.saved(list, -1, seq("c")))
    }

    @Test fun `가득 차면 더 만들지 못한다`() {
        val full = (1..5).map { seq("순서 $it") }
        assertFalse(ClickSequences.canAdd(full))
        assertTrue(ClickSequences.canAdd(full.dropLast(1)))
        assertEquals(full, ClickSequences.saved(full, -1, seq("x")))
    }

    @Test fun `삭제`() {
        val list = listOf(seq("a"), seq("b"), seq("c"))
        assertEquals(listOf(seq("a"), seq("c")), ClickSequences.removed(list, 1))
        assertEquals(list, ClickSequences.removed(list, 9))
    }
}
