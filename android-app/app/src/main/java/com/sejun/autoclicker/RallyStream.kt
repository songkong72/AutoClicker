package com.sejun.autoclicker

/** SSE(Server-Sent Events) 한 건. */
data class SseEvent(val name: String, val data: String)

/** 줄 단위로 먹이면 빈 줄에서 이벤트 한 건을 돌려준다. 안드로이드 의존성이 없다. */
class SseLineParser {
    private var name = ""
    private val data = StringBuilder()
    private var hasData = false

    fun feed(line: String): SseEvent? {
        if (line.isEmpty()) {
            val ev = if (name.isNotEmpty() || hasData) SseEvent(name, data.toString()) else null
            name = ""; data.setLength(0); hasData = false
            return ev
        }
        if (line.startsWith(":")) return null
        val idx = line.indexOf(':')
        val field = if (idx < 0) line else line.substring(0, idx)
        val value = if (idx < 0) "" else line.substring(idx + 1).removePrefix(" ")
        when (field) {
            "event" -> name = value
            "data" -> { if (hasData) data.append('\n'); data.append(value); hasData = true }
        }
        return null
    }
}

/**
 * Firebase 스트림의 put/patch 이벤트를 합쳐 방 전체 상태를 유지한다.
 * 값은 Map/List/기본형으로만 다룬다(JSON 파싱은 호출 쪽 몫).
 */
class RallyStreamTree {
    private var root: Any? = null

    fun put(path: String, value: Any?) {
        val segs = segments(path)
        if (segs.isEmpty()) { root = mutable(value); return }
        setAt(segs, mutable(value))
    }

    fun patch(path: String, children: Map<String, Any?>) {
        val base = segments(path)
        for ((k, v) in children) setAt(base + k, mutable(v))
    }

    /** 현재 상태의 독립된 복사본. 방이 비었으면 null. */
    fun snapshot(): Map<String, Any?>? {
        @Suppress("UNCHECKED_CAST")
        return (copy(root) as? Map<String, Any?>)
    }

    private fun segments(path: String) = path.split('/').filter { it.isNotEmpty() }

    private fun mutable(v: Any?): Any? = when (v) {
        is Map<*, *> -> LinkedHashMap<String, Any?>().also { m -> v.forEach { (k, x) -> m[k.toString()] = mutable(x) } }
        is List<*> -> v.mapTo(ArrayList()) { mutable(it) }
        else -> v
    }

    private fun copy(v: Any?): Any? = when (v) {
        is Map<*, *> -> LinkedHashMap<String, Any?>().also { m -> v.forEach { (k, x) -> m[k.toString()] = copy(x) } }
        is List<*> -> v.mapTo(ArrayList()) { copy(it) }
        else -> v
    }

    @Suppress("UNCHECKED_CAST")
    private fun setAt(segs: List<String>, value: Any?) {
        if (root !is MutableMap<*, *> && root !is MutableList<*>) root = LinkedHashMap<String, Any?>()
        var node: Any = root!!
        for (i in 0 until segs.size - 1) {
            val key = segs[i]
            val child = child(node, key)
            val next: Any = if (child is MutableMap<*, *> || child is MutableList<*>) child else {
                val created = LinkedHashMap<String, Any?>()
                store(node, key, created)
                created
            }
            node = next
        }
        store(node, segs.last(), value)
    }

    private fun child(node: Any, key: String): Any? = when (node) {
        is Map<*, *> -> node[key]
        is List<*> -> key.toIntOrNull()?.let { node.getOrNull(it) }
        else -> null
    }

    @Suppress("UNCHECKED_CAST")
    private fun store(node: Any, key: String, value: Any?) {
        when (node) {
            is MutableMap<*, *> -> {
                val m = node as MutableMap<String, Any?>
                if (value == null) m.remove(key) else m[key] = value
            }
            is MutableList<*> -> {
                val l = node as MutableList<Any?>
                val i = key.toIntOrNull() ?: return
                while (l.size <= i) l.add(null)
                l[i] = value
            }
        }
    }
}
