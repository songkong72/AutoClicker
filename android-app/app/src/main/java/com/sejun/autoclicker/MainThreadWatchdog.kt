package com.sejun.autoclicker

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * 메인 스레드가 얼마나 늦게 응답했는지 기록한다. "앱 대기(ANR)" 팝업이 떴을 때 원인을 찾기 위한 진단용.
 * 0.5초마다 메인 스레드에 작업을 던지고, 실행되기까지 1초 넘게 걸리면 그 지연을 남긴다.
 */
object MainThreadWatchdog {
    private const val STALL_MS = 1000L
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var running = false
    @Volatile var lastStall: String = ""
        private set

    fun start() {
        if (running) return
        running = true
        Thread {
            while (running) {
                val posted = SystemClock.elapsedRealtime()
                main.post {
                    val delay = SystemClock.elapsedRealtime() - posted
                    if (delay >= STALL_MS) {
                        val wall = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
                        lastStall = "화면 멈춤 감지: ${"%.1f".format(delay / 1000.0)}초 ($wall)"
                    }
                }
                try { Thread.sleep(500) } catch (_: InterruptedException) { }
            }
        }.apply { isDaemon = true }.start()
    }

    fun stop() { running = false }
}
