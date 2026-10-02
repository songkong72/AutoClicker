package com.sejun.autoclicker

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** 내 집결 클릭 5초 전부터 1초마다 한 번씩 알려 준다(진동용). 같은 초 안에서는 다시 알리지 않는다. */
class RallyCountdownCue {
    private var last: Int? = null

    /** [waitingMyClick]: 지금 단계가 "내 클릭을 기다리는 중"인지. 새 초가 시작되면 그 초(5..1)를, 아니면 null. */
    fun onCountdown(waitingMyClick: Boolean, remainingSec: Double): Int? {
        val n = Math.ceil(remainingSec).toInt()
        if (!waitingMyClick || n > 5 || n < 1) { last = null; return null }
        if (last == n) return null
        last = n
        return n
    }

    companion object {
        /** 마지막 5초는 숫자를 붉게 보여 눈에 띄게 한다. */
        fun urgent(waitingMyClick: Boolean, remainingSec: Double?): Boolean =
            waitingMyClick && remainingSec != null && remainingSec >= 0.0 && remainingSec <= 5.0
    }
}

/** 내 클릭이 실행된 시각 안내(ms까지). 보정이 맞았는지 팀장끼리 비교할 때 쓴다. */
object RallyClickNote {
    fun text(clickWallMs: Long?, zone: TimeZone = TimeZone.getDefault()): String {
        val ms = clickWallMs ?: return ""
        val f = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).apply { timeZone = zone }
        return "✓ 클릭함 " + f.format(Date(ms))
    }
}

enum class RallyConnection {
    LIVE, POLLING, OFFLINE;
    companion object {
        fun of(streaming: Boolean, online: Boolean) = when {
            streaming -> LIVE
            online -> POLLING
            else -> OFFLINE
        }
    }
}
