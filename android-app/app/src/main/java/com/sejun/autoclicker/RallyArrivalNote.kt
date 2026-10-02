package com.sejun.autoclicker

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** 전원 도착 시각 안내. 이 기기가 시작 신호를 받은 시계 시각 + 계산된 전원 도착까지의 초로 만든다. */
object RallyArrivalNote {
    fun arriveAtMs(startWallMs: Long, arriveAtSec: Double): Long = startWallMs + Math.round(arriveAtSec * 1000.0)

    /** 도착 전: "도착 예정 HH:mm:ss", 도착 후: "HH:mm:ss 도착", 진행한 집결이 없으면 빈 문자열. */
    fun text(arriveWallMs: Long?, nowMs: Long, zone: TimeZone = TimeZone.getDefault()): String {
        val a = arriveWallMs ?: return ""
        val f = SimpleDateFormat("HH:mm:ss", Locale.US).apply { timeZone = zone }
        val clock = f.format(Date(a))
        return if (nowMs < a) "도착 예정 $clock" else "$clock 도착"
    }
}
