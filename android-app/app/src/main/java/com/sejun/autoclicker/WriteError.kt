package com.sejun.autoclicker

/** 서버에 쓰기가 실패했을 때 화면에 보여 줄 이유. 서버 규칙이 거절하면(401/403) 무엇을 확인할지 알려 준다. */
internal object WriteError {
    fun explain(message: String?): String {
        val m = message.orEmpty()
        return if (m.contains("401") || m.contains("403"))
            "서버가 방 수정을 거절했어요. 이 기기가 관리자 명단에 있는지, 서버 규칙이 맞는지 확인해 주세요"
        else "서버에 저장하지 못했어요: " + m.take(60).ifEmpty { "알 수 없는 오류" }
    }
}
