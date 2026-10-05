package com.sejun.autoclicker

/** 방이 서버에 실제로 있는지 가리는 규칙. 방 문서는 항상 run 값을 갖고 있어서, 그 값 하나만 읽어 본다. 안드로이드 클래스를 쓰지 않아 단위 테스트가 된다. */
object RallyRoomCheck {
    /** 있으면 true, 없으면 false, 서버가 제대로 답하지 않았으면(네트워크·권한 오류) null. */
    fun exists(httpCode: Int, body: String): Boolean? {
        if (httpCode !in 200..299) return null
        val t = body.trim()
        return !(t.isEmpty() || t == "null")
    }
}
