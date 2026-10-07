package com.sejun.autoclicker

/** 서버에 쓰기가 실패했을 때 화면에 보여 줄 이유. 서버 규칙이 거절하면(401/403) 무엇을 확인할지 알려 준다. */
internal object WriteError {
    fun explain(message: String?): String {
        val m = message.orEmpty()
        return if (m.contains("401") || m.contains("403"))
            "서버가 방 수정을 거절했어요. 이 기기가 관리자 명단에 있는지, 서버 규칙이 맞는지 확인해 주세요"
        else "서버에 저장하지 못했어요: " + m.take(60).ifEmpty { "알 수 없는 오류" }
    }

    /** 집결 시작을 서버에 쓰지 못했을 때: 아무도 시작하지 않았다는 것과 다시 누르라는 것을 알린다. */
    fun explainStart(message: String?): String = "집결을 시작하지 못했어요. 다시 눌러 주세요 · " + explain(message)

    /** 취소를 서버에 쓰지 못했을 때: 이 폰은 멈췄지만 다른 폰은 계속 갈 수 있다는 것을 알린다. */
    fun explainCancel(message: String?): String = "취소를 서버에 전하지 못했어요. 다른 폰은 아직 진행 중일 수 있어요 · " + explain(message)
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
