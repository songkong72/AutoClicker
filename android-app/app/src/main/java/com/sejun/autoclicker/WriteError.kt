package com.sejun.autoclicker

/** 서버에 쓰기가 실패했을 때 화면에 보여 줄 이유. 서버 규칙이 거절하면(401/403) 무엇을 확인할지 알려 준다. */
internal object WriteError {
    fun explain(message: String?): String {
        val m = message.orEmpty()
        return if (m.contains("401") || m.contains("403"))
            T.s.rejected
        else T.s.saveFailed(m.take(60).ifEmpty { T.s.unknownError })
    }

    /** 조(도착 간격)를 쓰는 방을 저장하지 못했을 때: 거절이면 새 서버 규칙을 게시하지 않은 경우가 대부분이다. */
    fun explainWaves(message: String?): String {
        val m = message.orEmpty()
        return if (m.contains("401") || m.contains("403"))
            T.s.wavesRejected
        else explain(message)
    }

    /** 집결 시작을 서버에 쓰지 못했을 때: 아무도 시작하지 않았다는 것과 다시 누르라는 것을 알린다. */
    fun explainStart(message: String?): String = T.s.startFailed(explain(message))

    /** 취소를 서버에 쓰지 못했을 때: 이 폰은 멈췄지만 다른 폰은 계속 갈 수 있다는 것을 알린다. */
    fun explainCancel(message: String?): String = T.s.cancelFailed(explain(message))
}

/** 몇 번까지 다시 해 보고, 끝내 실패하면 마지막 오류를 돌려준다(성공하면 null). */
internal object Retry {
    fun run(times: Int, pause: () -> Unit = {}, block: () -> Unit): Exception? {
        var last: Exception? = null
        repeat(times.coerceAtLeast(1)) { i ->
            try { block(); return null } catch (e: Exception) { last = e }
            if (i < times - 1) pause()
        }
        return last
    }
}
