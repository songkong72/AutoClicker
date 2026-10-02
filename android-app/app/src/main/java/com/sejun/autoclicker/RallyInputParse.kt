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

    const val MAX_CORRECTION_MS = 5000

    /** 클릭 보정을 초로 입력받아 ms로 바꾼다. "-1.5", "+0.3", "2초" 모두 허용, ±5초 밖이거나 숫자가 아니면 null. */
    fun correctionMs(text: String): Int? {
        val cleaned = text.trim().removeSuffix("초").trim().replace(',', '.').replace('−', '-').replace('－', '-')
        if (!cleaned.matches(Regex("""[+-]?\d+(\.\d+)?"""))) return null
        val ms = Math.round((cleaned.toDoubleOrNull() ?: return null) * 1000.0).toInt()
        return if (Math.abs(ms) <= MAX_CORRECTION_MS) ms else null
    }

    /** 보정값을 초 단위 문자열로. 0.1초 단위면 소수 한 자리, 그보다 잘게 맞춰진 값(예전 10ms 단위)은 두 자리까지 보여준다. */
    fun formatCorrection(ms: Int): String {
        if (ms == 0) return "0초"
        val sign = if (ms > 0) "+" else "−"
        val a = Math.abs(ms)
        val body = if (a % 100 == 0) "%d.%d".format(a / 1000, (a % 1000) / 100)
        else "%d.%02d".format(a / 1000, (a % 1000) / 10)
        return sign + body + "초"
    }
}
