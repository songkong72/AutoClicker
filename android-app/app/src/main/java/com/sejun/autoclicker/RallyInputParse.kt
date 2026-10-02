package com.sejun.autoclicker

/** 입력창 문자열 해석. 안드로이드 의존성이 없어 JVM에서 테스트한다. */
object RallyInputParse {
    const val MAX_MARCH_SEC = 600.0

    /** 행군시간(초). 0.1초 단위로 반올림하고, 숫자가 아니거나 범위(0~600) 밖이면 null. */
    fun marchSeconds(text: String): Double? {
        val cleaned = text.trim().removeSuffix("초").trim().replace(',', '.')
        if (!cleaned.matches(Regex("""\d+(\.\d+)?"""))) return null
        val v = cleaned.toDoubleOrNull() ?: return null
        val rounded = Math.round(v * 10.0) / 10.0
        return if (rounded in 0.0..MAX_MARCH_SEC) rounded else null
    }
}
