package com.sejun.autoclicker

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Firebase REST로 방(rallyRooms/{room})을 1초마다 읽고, 관리자의 시작/취소/다시집결을 반영한다.
 *
 * 시간 모델: 서버 시간을 쓰지 않는다. 기기가 startSeq 변화를 처음 본 순간이 0초이고,
 * 거기서 RallyClickTiming으로 내 클릭 시점을 예약한다. 폴링 지연만큼 기기 간 오차가 생기므로
 * 내 기기의 ms 보정(correctionMs)으로 맞춘다.
 */
class RallyRoomSync(
    private val dbUrl: String,
    private val room: String,
    myTeamIdInit: String,
    private val saveMyTeam: (String) -> Unit,
    override val isAdmin: Boolean,
    private val correctionMs: () -> Long,
    private val setCorrectionMs: (Int) -> Unit,
    private val positionText: () -> String,
    private val savePosition: () -> Unit,
    private val onClickDue: () -> Unit,
    private val onCancel: () -> Unit
) : RallyPanelHost.StateSource {

    private val main = Handler(Looper.getMainLooper())
    @Volatile private var myTeamId: String = myTeamIdInit
    @Volatile private var doc: RallyRoomDoc = RallyRoomCodec.decode(null)
    @Volatile private var online = false
    private var lastSeq = -1L
    private var lastRun = "IDLE"
    private var startedAt: Long? = null
    private var clickTask: Runnable? = null
    @Volatile private var polling = false

    /** 내가 방금 바꾼 값이 서버에 반영되기 전에 도착한 옛 응답이 화면을 되돌리지 않도록, 변경 직후 잠시 서버 응답을 무시한다. */
    @Volatile private var holdRemoteUntil = 0L
    private fun holdRemote() { holdRemoteUntil = SystemClock.elapsedRealtime() + 2500L }

    private val url get() = "$dbUrl/rallyRooms/$room.json"

    fun start() {
        if (polling) return
        polling = true
        Thread {
            while (polling) {
                try {
                    val remote = get()
                    online = true
                    val d = RallyRoomCodec.decode(remote)
                    if (remote == null && isAdmin) put(seedRoom()) // 빈 방이면 기본 팀으로 시작
                    else main.post { if (SystemClock.elapsedRealtime() >= holdRemoteUntil) apply(d) }
                } catch (e: Exception) {
                    online = false
                }
                try { Thread.sleep(1000) } catch (_: InterruptedException) { }
            }
        }.start()
    }

    fun stop() {
        polling = false
        cancelClick()
    }

    // ---- StateSource ----

    override fun current(): RallyRoomState {
        val d = doc
        val s = startedAt
        val elapsed = if (s == null) 0.0 else (SystemClock.elapsedRealtime() - s) / 1000.0
        val run = when (d.run) {
            "RUNNING" -> RallyRunState.RUNNING
            "CANCELLED" -> RallyRunState.CANCELLED
            else -> RallyRunState.IDLE
        }
        val teams = d.teams.map { RallyTeamState(it.id, it.name, it.leaderName, it.marchSec, online, it.excluded) }
        return RallyRoomState(teams, myTeamId, d.prepSec, d.waitSec, run, elapsed)
    }

    override fun deviceCorrectionMs(): Int = correctionMs().toInt()
    override fun onCorrectionDelta(deltaMs: Int) = setCorrectionMs(
        (correctionMs().toInt() + deltaMs).coerceIn(-2000, 2000)
    )
    override fun devicePositionText(): String = positionText()
    override fun onSavePosition() = savePosition()

    override fun onStart() = change(RallyRoomEdit::startOrRegroup)

    override fun onStop() = change(RallyRoomEdit::cancel)

    /** 행군시간은 관리자(모든 팀) 또는 팀장(내 팀)이 고친다. 서버에는 그 팀의 필드 하나만 써서 다른 변경을 덮어쓰지 않는다. */
    override fun onSetMarch(teamId: String, sec: Double) {
        val d = doc
        if (!RallyRoomEdit.canEditMarch(d, isAdmin, myTeamId, teamId)) return
        val idx = d.teams.indexOfFirst { it.id == teamId }
        if (idx < 0) return
        val next = RallyRoomEdit.setMarch(d, teamId, sec)
        if (next === d) return
        holdRemote()
        apply(next)
        val value = next.teams[idx].marchSec
        Thread { try { putField("teams/$idx/marchSec", value) } catch (_: Exception) { } }.start()
    }

    override fun onMarchDelta(teamId: String, deltaSec: Double) {
        val cur = doc.teams.firstOrNull { it.id == teamId } ?: return
        onSetMarch(teamId, cur.marchSec + deltaSec)
    }

    override fun onAddTeam() = change { RallyRoomEdit.addTeam(it, "${it.teams.size + 1}군", 30.0) }

    override fun onSetPrep(sec: Double) = change { RallyRoomEdit.setPrep(it, sec) }

    override fun onSetWait(sec: Double) = change { RallyRoomEdit.setWait(it, sec) }

    override fun onRemoveTeam(teamId: String) = change { RallyRoomEdit.removeTeam(it, teamId) }

    override fun onToggleExclude(teamId: String) {
        val cur = doc.teams.firstOrNull { it.id == teamId } ?: return
        change { RallyRoomEdit.setExcluded(it, teamId, !cur.excluded) }
    }

    /** 내 팀 선택은 기기 로컬 설정이라 방에는 쓰지 않는다. */
    override fun onSelectMine(teamId: String) {
        myTeamId = teamId
        saveMyTeam(teamId)
    }

    /** 관리자만 방을 바꾼다. 내 기기는 즉시 반영하고 서버에는 비동기로 쓴다. */
    private fun change(op: (RallyRoomDoc) -> RallyRoomDoc) {
        if (!isAdmin) return
        val next = op(doc)
        if (next === doc) return
        holdRemote()
        apply(next)
        Thread { try { put(next) } catch (_: Exception) { } }.start()
    }

    // ---- 내부 ----

    private fun apply(d: RallyRoomDoc) {
        doc = d
        if (d.startSeq != lastSeq && d.run == "RUNNING") {
            lastSeq = d.startSeq
            startedAt = SystemClock.elapsedRealtime()
            scheduleMyClick(d)
        } else if (d.startSeq != lastSeq) {
            lastSeq = d.startSeq
        }
        if (d.run == "CANCELLED" && lastRun != "CANCELLED") {
            cancelClick()
            startedAt = null
            onCancel()
        }
        lastRun = d.run
    }

    private fun scheduleMyClick(d: RallyRoomDoc) {
        cancelClick()
        val plan = RallySchedule.plan(
            d.teams.map { RallyTeamInput(it.id, it.marchSec, it.excluded) }, d.prepSec, d.waitSec
        )
        val mine = plan.teamPlan(myTeamId) ?: return // 제외된 팀은 클릭하지 않는다
        val delay = RallyClickTiming.delayUntilClickMs(mine.clickAtSec, 0L, correctionMs())
        val task = Runnable { onClickDue() }
        clickTask = task
        main.postDelayed(task, delay)
    }

    private fun cancelClick() {
        clickTask?.let { main.removeCallbacks(it) }
        clickTask = null
    }

    private fun seedRoom() = RallyRoomDoc(
        teams = listOf(
            RallyTeamDoc("t1", "1군", marchSec = 10.0),
            RallyTeamDoc("t2", "2군", marchSec = 30.0),
            RallyTeamDoc("t3", "3군", marchSec = 50.0)
        ),
        prepSec = 15.0, waitSec = 300.0, run = "IDLE", startSeq = 0L
    )

    private fun get(): Map<String, Any?>? {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 3000; c.readTimeout = 3000
        val text = c.inputStream.bufferedReader().use { it.readText() }.trim()
        if (text == "null" || text.isEmpty()) return null
        @Suppress("UNCHECKED_CAST")
        return toPlain(JSONObject(text)) as Map<String, Any?>
    }

    private fun put(d: RallyRoomDoc) {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "PUT"
        c.setRequestProperty("Content-Type", "application/json")
        c.connectTimeout = 3000; c.readTimeout = 3000
        c.doOutput = true
        c.outputStream.use { it.write(JSONObject(RallyRoomCodec.encode(d)).toString().toByteArray()) }
        c.inputStream.close()
    }

    private fun putField(path: String, value: Double) {
        val c = URL("$dbUrl/rallyRooms/$room/$path.json").openConnection() as HttpURLConnection
        c.requestMethod = "PUT"
        c.setRequestProperty("Content-Type", "application/json")
        c.connectTimeout = 3000; c.readTimeout = 3000
        c.doOutput = true
        c.outputStream.use { it.write(value.toString().toByteArray()) }
        c.inputStream.close()
    }

    private fun toPlain(v: Any?): Any? = when (v) {
        is JSONObject -> v.keys().asSequence().associateWith { toPlain(v.get(it)) }
        is JSONArray -> (0 until v.length()).map { toPlain(v.get(it)) }
        JSONObject.NULL -> null
        else -> v
    }
}
