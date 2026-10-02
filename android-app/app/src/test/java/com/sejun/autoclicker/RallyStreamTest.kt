package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RallyStreamTest {
    // ---- SSE 줄 단위 파서 ----

    private fun feedAll(p: SseLineParser, vararg lines: String) = lines.mapNotNull { p.feed(it) }

    @Test fun eventIsEmittedOnBlankLine() {
        val ev = feedAll(SseLineParser(), "event: put", "data: {\"path\":\"/\",\"data\":null}", "")
        assertEquals(1, ev.size)
        assertEquals("put", ev[0].name)
        assertEquals("{\"path\":\"/\",\"data\":null}", ev[0].data)
    }

    @Test fun nothingEmittedBeforeBlankLine() {
        assertEquals(0, feedAll(SseLineParser(), "event: put", "data: 1").size)
    }

    @Test fun multipleDataLinesAreJoinedWithNewline() {
        val ev = feedAll(SseLineParser(), "event: put", "data: a", "data: b", "")
        assertEquals("a\nb", ev[0].data)
    }

    @Test fun commentsAndBlankLinesWithoutFieldsAreIgnored() {
        val ev = feedAll(SseLineParser(), ": hi", "", "event: keep-alive", "data: null", "")
        assertEquals(1, ev.size)
        assertEquals("keep-alive", ev[0].name)
    }

    @Test fun parserResetsBetweenEvents() {
        val p = SseLineParser()
        val ev = feedAll(p, "event: put", "data: x", "", "event: patch", "data: y", "")
        assertEquals(listOf("put" to "x", "patch" to "y"), ev.map { it.name to it.data })
    }

    // ---- 스트림 이벤트를 합쳐 방 전체 상태를 유지하는 트리 ----

    private fun room() = mapOf(
        "prepSec" to 15.0, "waitSec" to 300.0, "run" to "IDLE", "startSeq" to 0.0,
        "teams" to listOf(mapOf("id" to "t1", "marchSec" to 10.0), mapOf("id" to "t2", "marchSec" to 30.0))
    )

    @Test fun putRootReplacesEverything() {
        val t = RallyStreamTree()
        t.put("/", room())
        assertEquals("IDLE", t.snapshot()!!["run"])
    }

    @Test fun putRootNullMeansEmptyRoom() {
        val t = RallyStreamTree()
        t.put("/", room())
        t.put("/", null)
        assertNull(t.snapshot())
    }

    @Test fun putNestedPathUpdatesListElementField() {
        val t = RallyStreamTree()
        t.put("/", room())
        t.put("/teams/1/marchSec", 70.0)
        @Suppress("UNCHECKED_CAST")
        val teams = t.snapshot()!!["teams"] as List<Map<String, Any?>>
        assertEquals(70.0, teams[1]["marchSec"])
        assertEquals(10.0, teams[0]["marchSec"])
    }

    @Test fun patchMergesChildrenWithoutDroppingOthers() {
        val t = RallyStreamTree()
        t.put("/", room())
        t.patch("/", mapOf("run" to "RUNNING", "startSeq" to 1.0))
        val s = t.snapshot()!!
        assertEquals("RUNNING", s["run"])
        assertEquals(1.0, s["startSeq"])
        assertEquals(300.0, s["waitSec"])
    }

    @Test fun putNullAtPathRemovesKey() {
        val t = RallyStreamTree()
        t.put("/", room())
        t.put("/waitSec", null)
        assertNull(t.snapshot()!!["waitSec"])
        assertEquals(false, t.snapshot()!!.containsKey("waitSec"))
    }

    @Test fun putCreatesMissingIntermediateNodes() {
        val t = RallyStreamTree()
        t.put("/a/b", 1.0)
        @Suppress("UNCHECKED_CAST")
        assertEquals(1.0, (t.snapshot()!!["a"] as Map<String, Any?>)["b"])
    }

    @Test fun putAtListEndAppends() {
        val t = RallyStreamTree()
        t.put("/", room())
        t.put("/teams/2", mapOf("id" to "t3", "marchSec" to 50.0))
        @Suppress("UNCHECKED_CAST")
        assertEquals(3, (t.snapshot()!!["teams"] as List<Any?>).size)
    }

    @Test fun snapshotIsADetachedCopy() {
        val t = RallyStreamTree()
        t.put("/", room())
        val before = t.snapshot()!!
        t.put("/run", "RUNNING")
        assertEquals("IDLE", before["run"])
    }
}
