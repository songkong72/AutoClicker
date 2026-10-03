package com.sejun.autoclicker

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Firebase REST로 방(rallyRooms/{room})을 실시간 스트림(SSE)으로 받아 관리자의 시작/취소/다시집결을 즉시 반영한다.
 * 스트림이 끊기거나 실패하면 1초 폴링으로 버티다가 다시 스트림에 연결한다.
 *
 * 시간 모델: 서버 시간을 쓰지 않는다. 기기가 startSeq 변화를 처음 본 순간이 0초이고,
 * 거기서 RallyClickTiming으로 내 클릭 시점을 예약한다. 수신 지연만큼 기기 간 오차가 생기므로
 * (스트림이면 수십 ms, 폴링이면 최대 1초) 내 기기의 ms 보정(correctionMs)으로 맞춘다.
 */
class RallyRoomSync(
    private val dbUrl: String,
    private val auth: FirebaseAuthClient? = null,
    private val room: String,
    /** 이 기기의 고유 ID. 관리자가 군단을 배정할 때 사람을 가리키는 키다. */
    private val memberId: String,
    private val getCharacterName: () -> String,
    private val saveCharacterName: (String) -> Unit,
    override val isAdmin: Boolean,
    private val correctionMs: () -> Long,
    private val setCorrectionMs: (Int) -> Unit,
    private val positionText: () -> String,
    private val positionSaved: () -> Boolean = { true },
    private val savePosition: () -> Unit,
    private val onClickDue: () -> Unit,
    /** 클릭 약 1.5초 전에 true, 예약이 취소되면 false로 호출된다. 과녁 오버레이를 미리 터치 통과로 바꿔 두어, 느린 기기에서 탭이 오버레이에 걸리지 않게 한다. */
    private val onClickArm: (Boolean) -> Unit = {},
    /** 시작 신호를 처음 받은 순간. 패널이 닫혀 있으면 다시 띄워 카운트다운이 보이게 한다. */
    private val onRallyStart: () -> Unit = {},
    private val onCancel: () -> Unit
) : RallyPanelHost.StateSource {

    private val main = Handler(Looper.getMainLooper())
    /** 내 군단은 관리자의 배정에서 정해진다. 배정이 없으면 빈 문자열. */
    private val myTeamId: String get() = RallyRoomEdit.teamIdOf(doc, memberId, getCharacterName())
    @Volatile private var doc: RallyRoomDoc = RallyRoomCodec.decode(null)
    @Volatile private var online = false
    private val startDetector = RallyStartDetector()
    private var lastRun = "IDLE"
    private var startedAt: Long? = null
    @Volatile private var clickWallMs: Long? = null // 이 기기가 내 클릭을 실행한 시계 시각
    @Volatile private var arriveWallMs: Long? = null // 이 기기 시계 기준 전원 도착 시각(안내용)
    private var clickTask: Runnable? = null
    private var armTask: Runnable? = null
    @Volatile private var polling = false
    @Volatile private var streaming = false
    @Volatile private var streamConn: HttpURLConnection? = null
    private var pendingDoc: RallyRoomDoc? = null
    private var lastDiag = ""
    /** 마지막 탭의 좌표와 제스처 결과. 토스트가 막히는 기기에서도 패널에서 확인할 수 있게 한다. */
    @Volatile var clickResult = ""

    /** 내가 방금 바꾼 값이 서버에 반영되기 전에 도착한 옛 응답이 화면을 되돌리지 않도록, 변경 직후 잠시 서버 응답을 무시한다. */
    @Volatile private var holdRemoteUntil = 0L
    private fun holdRemote() { holdRemoteUntil = SystemClock.elapsedRealtime() + 2500L }

    private val url get() = FirebaseAuthCodec.withAuth("$dbUrl/rallyRooms/$room.json", auth?.token())

    fun start() {
        if (polling) return
        polling = true
        Thread {
            registerSelf()
            while (polling) {
                try { runStream() } catch (e: Exception) { dropTokenIf401(e) }
                streaming = false
                if (!polling) break
                // 스트림이 끊기면 몇 번은 폴링으로 버티고 다시 연결을 시도한다.
                repeat(5) {
                    if (polling) {
                        pollOnce()
                        try { Thread.sleep(1000) } catch (_: InterruptedException) { }
                    }
                }
            }
        }.start()
    }

    fun stop() {
        polling = false
        cancelClick()
        val c = streamConn
        if (c != null) Thread { try { c.disconnect() } catch (_: Exception) { } }.start()
    }

    /** 서버 응답(수신 시점의 방 전체 상태)을 화면에 반영한다. 내가 방금 바꾼 직후에는 잠깐 미뤘다가 최신 상태를 반영한다. */
    private fun deliver(remote: Map<String, Any?>?) {
        val d = RallyRoomCodec.decode(remote)
        if (remote == null && isAdmin) { Thread { try { put(seedRoom()) } catch (_: Exception) { } }.start(); return } // 빈 방이면 기본 팀으로 시작
        main.post {
            val wait = holdRemoteUntil - SystemClock.elapsedRealtime()
            if (wait <= 0) { pendingDoc = null; apply(d, fromRemote = true) }
            else { pendingDoc = d; main.postDelayed({ pendingDoc?.let { p -> pendingDoc = null; apply(p, fromRemote = true) } }, wait + 50) }
        }
    }

    private fun pollOnce() {
        try {
            val remote = get()
            online = true
            deliver(remote)
        } catch (e: Exception) {
            online = false
            dropTokenIf401(e)
        }
    }

    /** 서버가 토큰을 거절하면(401/403) 다음 요청에서 새 토큰을 받는다. */
    private fun dropTokenIf401(e: Exception) {
        val m = e.message ?: return
        if (m.contains("401") || m.contains("403")) auth?.invalidate()
    }

    /** 실시간 연결. 끊기거나 오류가 나면 반환/예외로 빠져나가 폴링으로 넘어간다. */
    private fun runStream() {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("Accept", "text/event-stream")
        c.connectTimeout = 5000
        c.readTimeout = 70000 // 서버가 30초마다 keep-alive를 보낸다
        streamConn = c
        val tree = RallyStreamTree()
        val parser = SseLineParser()
        c.inputStream.bufferedReader().use { r ->
            online = true
            streaming = true
            while (polling) {
                val line = r.readLine() ?: break
                val ev = parser.feed(line) ?: continue
                when (ev.name) {
                    "put", "patch" -> {
                        val obj = JSONObject(ev.data)
                        val path = obj.optString("path", "/")
                        val data = if (obj.isNull("data")) null else toPlain(obj.get("data"))
                        if (ev.name == "put") tree.put(path, data)
                        else (data as? Map<*, *>)?.let { m -> tree.patch(path, m.entries.associate { it.key.toString() to it.value }) }
                        deliver(tree.snapshot())
                    }
                    "cancel", "auth_revoked" -> return
                }
            }
        }
    }

    // ---- StateSource ----

    override fun current(): RallyRoomState {
        val d = doc
        val s = startedAt
        val elapsed = if (s == null) 0.0 else (SystemClock.elapsedRealtime() - s) / 1000.0
        val run = when (d.run) {
            // 이 기기가 시작 신호를 직접 받은 적이 없으면(늦게 입장 등) 진행 중으로 보지 않는다
            "RUNNING" -> if (s == null) RallyRunState.IDLE else RallyRunState.RUNNING
            "CANCELLED" -> RallyRunState.CANCELLED
            else -> RallyRunState.IDLE
        }
        val teams = d.teams.map { RallyTeamState(it.id, it.name, it.leaderName, it.marchSec, online, it.excluded, it.adminAdjustMs, it.leaderId) }
        return RallyRoomState(teams, RallyRoomEdit.teamIdOf(d, memberId), d.prepSec, d.waitSec, run, elapsed, positionSaved(),
            characterNameSet = isAdmin || getCharacterName().isNotBlank())
    }

    override fun deviceCorrectionMs(): Int = correctionMs().toInt()
    override fun clickNote(): String = RallyClickNote.text(clickWallMs)
    override fun connection(): RallyConnection = RallyConnection.of(streaming, online)
    override fun arrivalNote(): String = RallyArrivalNote.text(arriveWallMs, System.currentTimeMillis())
    override fun onSetCorrectionMs(ms: Int) = setCorrectionMs(ms.coerceIn(-RallyInputParse.MAX_CORRECTION_MS, RallyInputParse.MAX_CORRECTION_MS))
    override fun onCorrectionDelta(deltaMs: Int) = setCorrectionMs(
        (correctionMs().toInt() + deltaMs).coerceIn(-RallyInputParse.MAX_CORRECTION_MS, RallyInputParse.MAX_CORRECTION_MS)
    )
    private fun myAdminAdjustMs(d: RallyRoomDoc = doc): Int =
        RallyRoomEdit.teamIdOf(d, memberId, getCharacterName()).let { mine -> d.teams.firstOrNull { it.id == mine }?.adminAdjustMs ?: 0 }

    override fun devicePositionText(): String =
        myAdminAdjustMs().let { a -> if (a == 0) "" else "관리자 보정 ${RallyInputParse.formatCorrection(a)} (내 보정에 더해 적용)\n" } +
        positionText() + (if (lastDiag.isNotEmpty()) "\n$lastDiag" else "") + (if (clickResult.isNotEmpty()) "\n$clickResult" else "") + "\n수신 방식: " + (if (streaming) "실시간" else "1초 확인") +
        (MainThreadWatchdog.lastStall.let { if (it.isEmpty()) "" else "\n$it" })
    override fun onSavePosition() = savePosition()

    override fun onStart() = change(RallyRoomEdit::startOrRegroup)

    override fun onStop() = change(RallyRoomEdit::cancel)

    /** 행군시간은 관리자(모든 팀) 또는 팀장(내 팀)이 고친다. 서버에는 그 팀의 필드 하나만 써서 다른 변경을 덮어쓰지 않는다. */
    override fun onSetMarch(teamId: String, sec: Double) {
        val d = effectiveDoc()
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

    /** 관리자만: 한 군단의 클릭 보정을 정한다. 그 팀의 필드 하나만 서버에 쓴다. */
    override fun onSetAdminAdjust(teamId: String, ms: Int) {
        if (!isAdmin) return
        val d = effectiveDoc()
        val idx = d.teams.indexOfFirst { it.id == teamId }
        if (idx < 0) return
        val next = RallyRoomEdit.setAdminAdjust(d, teamId, ms)
        if (next === d) return
        holdRemote()
        apply(next)
        val value = next.teams[idx].adminAdjustMs
        Thread { try { putField("teams/$idx/adminAdjustMs", value.toDouble()) } catch (_: Exception) { } }.start()
    }

    override fun onAddTeam() = change { RallyRoomEdit.addTeam(it, "${it.teams.size + 1}군", 30.0) }

    override fun onSetPrep(sec: Double) = change { RallyRoomEdit.setPrep(it, sec) }

    override fun onSetWait(sec: Double) = change { RallyRoomEdit.setWait(it, sec) }

    override fun onRemoveTeam(teamId: String) = change { RallyRoomEdit.removeTeam(it, teamId) }

    override fun onToggleExclude(teamId: String) {
        val cur = doc.teams.firstOrNull { it.id == teamId } ?: return
        change { RallyRoomEdit.setExcluded(it, teamId, !cur.excluded) }
    }

    // ---- 캐릭터명과 군단 배정 ----

    override fun characterName(): String = getCharacterName()

    /** 내 캐릭터명을 이 기기에 저장하고 방 명단에 올린다. 관리자는 이 명단에서 사람을 골라 군단에 배정한다. */
    override fun onSetCharacterName(name: String) {
        val clean = RallyRoster.cleanName(name)
        if (clean.isEmpty()) return
        saveCharacterName(clean)
        Thread { registerSelf() }.start()
    }

    /** 관리자가 고를 수 있는 방 명단을 가져온다. 실패하면 null을 돌려준다. 결과는 메인 스레드로 전달한다. */
    @Volatile private var rosterErr = ""
    override fun rosterError(): String = rosterErr

    override fun loadRoster(onLoaded: (List<RallyMember>?) -> Unit) {
        Thread {
            val result = try { rosterErr = ""; RallyRoster.decode(getMembers()) } catch (e: Exception) { rosterErr = (e.message ?: e.javaClass.simpleName).take(80); dropTokenIf401(e); null }
            main.post { onLoaded(result) }
        }.start()
    }

    override fun onAssignLeader(teamId: String, memberId: String, characterName: String) =
        change { RallyRoomEdit.assignLeader(it, teamId, memberId, characterName) }

    override fun onUnassignLeader(teamId: String) = change { RallyRoomEdit.unassignLeader(it, teamId) }

    private fun membersUrl(path: String) =
        FirebaseAuthCodec.withAuth("$dbUrl/rallyMembers/$room$path.json", auth?.token())

    /** 방 명단에 내 항목(rallyMembers/{방}/{내 ID})을 쓴다. 이름이 없으면 아무것도 하지 않는다. */
    private fun registerSelf() {
        if (getCharacterName().isBlank()) return
        registerMember(dbUrl, auth, room, memberId, getCharacterName())
    }

    companion object {
        const val DB_URL = "https://autoclicker-cf5a4-default-rtdb.firebaseio.com"

        /** 방 명단에 이 기기를 올린다. 성공하면 null, 실패하면 화면에 보여 줄 이유를 돌려준다. 네트워크를 쓰므로 메인 스레드에서 부르지 않는다. */
        fun registerMember(dbUrl: String, auth: FirebaseAuthClient?, room: String, memberId: String, rawName: String): String? {
            val body = RallyRoster.encode(memberId, rawName) ?: return "캐릭터명이 비어 있어요"
            return try {
                val url = FirebaseAuthCodec.withAuth("$dbUrl/rallyMembers/$room/$memberId.json", auth?.token())
                val c = URL(url).openConnection() as HttpURLConnection
                c.requestMethod = "PUT"
                c.setRequestProperty("Content-Type", "application/json")
                c.connectTimeout = 3000; c.readTimeout = 3000
                c.doOutput = true
                c.outputStream.use { it.write(JSONObject(body).toString().toByteArray()) }
                c.inputStream.close()
                null
            } catch (e: Exception) {
                val m = e.message ?: e.javaClass.simpleName
                if (m.contains("401") || m.contains("403")) auth?.invalidate()
                m.take(80)
            }
        }
    }

    private fun getMembers(): Map<String, Any?>? {
        val c = URL(membersUrl("")).openConnection() as HttpURLConnection
        c.connectTimeout = 3000; c.readTimeout = 3000
        val text = c.inputStream.bufferedReader().use { it.readText() }.trim()
        if (text == "null" || text.isEmpty()) return null
        @Suppress("UNCHECKED_CAST")
        return toPlain(JSONObject(text)) as Map<String, Any?>
    }

    /** 관리자만 방을 바꾼다. 내 기기는 즉시 반영하고 서버에는 비동기로 쓴다. */
    private fun change(op: (RallyRoomDoc) -> RallyRoomDoc) {
        if (!isAdmin) return
        val cur = effectiveDoc()
        val next = op(cur)
        if (next === cur) return
        holdRemote()
        apply(next)
        Thread { try { put(next) } catch (_: Exception) { } }.start()
    }

    // ---- 내부 ----

    /** 화면이 보여주는 상태와 같은 기준의 문서. 끝나지 않은 예전 RUNNING 찌꺼기 때문에 수정이 막히지 않게 한다. */
    private fun effectiveDoc(): RallyRoomDoc {
        // 전원 도착 뒤에는 이 기기에서 그 집결을 끝난 것으로 보고 잊는다(화면도 이미 "전원 도착"이라 수정 가능으로 보인다)
        val s = startedAt
        if (s != null && RallyRoomEdit.finished(doc, (SystemClock.elapsedRealtime() - s) / 1000.0)) startedAt = null
        return RallyRoomEdit.unstick(doc, startedAt != null)
    }

    private fun apply(d: RallyRoomDoc, fromRemote: Boolean = false) {
        doc = d
        if (startDetector.onDoc(d.startSeq, d.run, fromRemote)) {
            startedAt = SystemClock.elapsedRealtime()
            clickWallMs = null
            val plan = RallySchedule.plan(d.teams.map { RallyTeamInput(it.id, it.marchSec, it.excluded) }, d.prepSec, d.waitSec)
            arriveWallMs = RallyArrivalNote.arriveAtMs(System.currentTimeMillis(), plan.arriveAtSec)
            scheduleMyClick(d)
            onRallyStart()
        }
        if (d.run == "CANCELLED" && lastRun != "CANCELLED") {
            cancelClick()
            startedAt = null
            arriveWallMs = null
            clickWallMs = null
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
        val totalMs = RallyClickTiming.totalCorrectionMs(correctionMs(), myAdminAdjustMs(d))
        val delay = RallyClickTiming.delayUntilClickMs(mine.clickAtSec, 0L, totalMs)
        val startAtMs = startedAt
        val task = Runnable {
            val actual = if (startAtMs == null) 0.0 else (SystemClock.elapsedRealtime() - startAtMs) / 1000.0
            val wall = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
            lastDiag = "마지막 집결: 신호 수신 ${"%.3f".format(actual)}초 뒤 클릭(목표 ${"%.2f".format(mine.clickAtSec)}초 ${totalMs}ms 보정) · $wall"
            clickWallMs = System.currentTimeMillis()
            clickResult = ""
            onClickDue()
        }
        clickTask = task
        main.postDelayed(task, delay)
        val arm = Runnable { onClickArm(true) }
        armTask = arm
        main.postDelayed(arm, Math.max(0L, delay - CLICK_ARM_LEAD_MS))
    }

    private fun cancelClick() {
        clickTask?.let { main.removeCallbacks(it) }
        clickTask = null
        armTask?.let { main.removeCallbacks(it) }
        armTask = null
        main.post { onClickArm(false) }
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
        val c = URL(FirebaseAuthCodec.withAuth("$dbUrl/rallyRooms/$room/$path.json", auth?.token())).openConnection() as HttpURLConnection
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

/** 내 클릭 몇 ms 전에 과녁을 터치 통과로 바꿔 둘지. */
private const val CLICK_ARM_LEAD_MS = 1500L
