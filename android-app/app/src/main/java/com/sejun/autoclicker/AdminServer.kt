package com.sejun.autoclicker

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

/** 서버에서 받은 값 또는 화면에 보여 줄 실패 이유. */
internal class Reply<T>(val value: T?, val error: String?) {
    val ok: Boolean get() = error == null
}

/** 개발자 화면용으로 읽어 온 방(rallyRooms)과 방 명단(rallyMembers) 원본. */
internal class RoomData(val rooms: Map<String, Any?>?, val members: Map<String, Any?>?)

/** 서버에서 확인한 결과. 모르겠으면(네트워크 오류, 규칙 미적용) [UNKNOWN]이고, 그때는 현재 권한을 건드리지 않는다. */
internal enum class Check { YES, NO, UNKNOWN }

/**
 * 서버 지휘관 명단(owners / admins / adminCodes)과 통신한다(Firebase REST). 네트워크를 쓰므로 메인 스레드에서 부르지 않는다.
 * 개발자(owners)는 Firebase 콘솔에서 사람이 직접 적는다. 앱은 읽기만 한다.
 */
internal class AdminServer(private val dbUrl: String, private val auth: FirebaseAuthClient) {

    /** 이 기기의 서버 ID. */
    fun uid(): Reply<String> {
        val id = try { auth.uid() } catch (e: Exception) { null }
        return if (id != null && AdminRoster.isValidUid(id)) Reply(id, null) else Reply(null, "기기 ID를 받지 못했어요. 인터넷 연결을 확인해 주세요")
    }

    fun isOwner(uid: String): Check = exists("owners/$uid")
    fun isAdmin(uid: String): Check = exists("admins/$uid")

    fun listAdmins(): Reply<List<AdminEntry>> {
        val (code, text) = call("GET", "admins")
        if (code !in 200..299) return Reply(null, explain(code, text))
        return Reply(AdminRoster.decodeAdmins(parseMap(text)), null)
    }

    fun listCodes(now: Long = System.currentTimeMillis()): Reply<List<AdminCode>> {
        val (code, text) = call("GET", "adminCodes")
        if (code !in 200..299) return Reply(null, explain(code, text))
        return Reply(AdminRoster.decodeCodes(parseMap(text), now), null)
    }

    /**
     * 새 지휘관 코드를 만든다. 성공하면 코드를 돌려준다.
     * [group]은 이 코드로 들어온 사람이 묶일 소속("2000-WBI"), [rep]은 연맹 대표 코드인지(개발자만), [by]는 만드는 연맹 대표의 기기 ID다.
     */
    fun createCode(
        rawName: String, ttl: CodeTtl = CodeTtl.DAY, now: Long = System.currentTimeMillis(),
        group: String = "", rep: Boolean = false, by: String = ""
    ): Reply<String> {
        val body = AdminRoster.encodeCode(rawName, now, ttl.ms, group, rep, by) ?: return Reply(null, "이름표를 입력해 주세요")
        val newCode = AdminRoster.newCode()
        val (code, text) = call("PUT", "adminCodes/$newCode", JSONObject(body).toString())
        return if (code in 200..299) Reply(newCode, null) else Reply(null, explain(code, text))
    }

    fun cancelCode(code: String): String? = delete("adminCodes/$code")

    /** 지휘관의 이름표를 고친다. 성공하면 null, 실패하면 이유. 개발자만 된다. */
    fun renameAdmin(uid: String, rawName: String): String? {
        val name = AdminRoster.cleanName(rawName)
        if (name.isEmpty()) return "이름표를 입력해 주세요"
        val (code, text) = call("PUT", "admins/$uid/name", JSONObject.quote(name))
        return if (code in 200..299) null else explain(code, text)
    }

    /** 이름이 없는 지휘관에게 등록에 쓴 코드의 이름표를 복사해 둔다. 코드를 정리해도 이름이 남게 한다. 실패해도 목록 보기에는 영향이 없다. */
    fun backfillNames(admins: List<AdminEntry>, codes: List<AdminCode>) {
        for ((uid, name) in AdminRoster.nameBackfill(admins, codes)) {
            call("PUT", "admins/$uid/name", JSONObject.quote(name))
        }
    }

    /** 이미 쓰였거나 기한이 지난 코드를 서버에서 지운다. 지우기 전에 지휘관 이름표를 먼저 저장한다. 지운 개수를 돌려준다. */
    fun purgeCodes(now: Long = System.currentTimeMillis()): Reply<Int> {
        val admins = listAdmins().let { it.value ?: return Reply(null, it.error) }
        val codes = listCodes(now).let { it.value ?: return Reply(null, it.error) }
        backfillNames(admins, codes)
        val (c, text) = call("GET", "adminCodes")
        if (c !in 200..299) return Reply(null, explain(c, text))
        var removed = 0
        for (code in AdminRoster.purgeable(parseMap(text), now)) {
            if (delete("adminCodes/$code") == null) removed++
        }
        return Reply(removed, null)
    }

    /**
     * 서버의 방(명단 제외)을 읽어 온다. 방 선택 목록에 쓴다.
     * [group]을 주면 그 소속의 방만 방 번호로 돌려주고, 없으면 서버에 저장된 이름 그대로 전부 돌려준다(개발자의 방 전체 목록).
     */
    fun loadRoomsOnly(group: RallyGroup? = null): Reply<Map<String, Any?>?> {
        val (c, t) = call("GET", "rallyRooms")
        return if (c in 200..299) Reply(RallyGroup.scope(parseMap(t), group), null) else Reply(null, explain(c, t))
    }

    /** 개발자 전용: 서버의 모든 방과 방 명단을 읽어 온다. 규칙에서 개발자(owners)만 통째로 읽을 수 있다. */
    fun loadRooms(): Reply<RoomData> {
        val (c1, t1) = call("GET", "rallyRooms")
        if (c1 !in 200..299) return Reply(null, explain(c1, t1))
        val (c2, t2) = call("GET", "rallyMembers")
        if (c2 !in 200..299) return Reply(null, explain(c2, t2))
        return Reply(RoomData(parseMap(t1), parseMap(t2)), null)
    }

    /** 개발자 전용: 방 하나와 그 방의 명단을 서버에서 지운다. 성공하면 null, 실패하면 이유. 규칙에서 개발자(owners)와 지휘관만 방을 쓸 수 있고, 지우기는 개발자 화면에서만 연다. */
    fun deleteRoom(room: String): String? {
        if (room.isEmpty() || room.any { it in "./#$[]" }) return "방 번호가 올바르지 않아요"
        delete("rallyMembers/$room")?.let { return it }
        return delete("rallyRooms/$room")
    }

    /** 내가 서버 명단에 있는 지휘관일 때 마지막 접속 시각과 앱 버전을 적는다. 실패해도 조용히 넘어간다. */
    fun reportSelf(uid: String, now: Long, appVersion: String) {
        call("PUT", "admins/$uid/lastSeen", now.toString())
        call("PUT", "admins/$uid/appVersion", JSONObject.quote(appVersion.take(20)))
    }

    fun removeAdmin(uid: String): String? = delete("admins/$uid")

    /** 코드로 지휘관이 된다. 성공하면 null, 실패하면 이유. 코드를 먼저 내 것으로 잡고(먼저 쓴 사람만 성공), 그다음 명단에 올린다. */
    fun redeem(rawCode: String, now: Long = System.currentTimeMillis()): String? {
        val code = AdminRoster.normalizeCode(rawCode) ?: return "지휘관 코드 모양이 맞지 않아요 (AD-로 시작하는 10자)"
        val me = uid()
        val uid = me.value ?: return me.error
        val (c1, t1) = call("PUT", "adminCodes/$code/usedBy", JSONObject.quote(uid))
        if (c1 !in 200..299) return if (c1 == 401 || c1 == 403) "코드가 없거나, 이미 쓰였거나, 기한(24시간)이 지났어요" else explain(c1, t1)
        // 코드에 적힌 소속과 대표 여부를 읽어 그대로 등록한다(서버 규칙이 똑같은지 본다). 읽지 못하면(옛 규칙) 소속 없이 등록한다.
        val made = parseMap(call("GET", "adminCodes/$code").let { (c, t) -> if (c in 200..299) t else "" })
        val group = (made?.get("group") as? String).orEmpty()
        val rep = made?.get("rep") == true
        val (c2, t2) = call("PUT", "admins/$uid", JSONObject(AdminRoster.adminRecord(code, now, group, rep)).toString())
        return if (c2 in 200..299) null else explain(c2, t2)
    }

    // ---- 소속과 연맹 대표 ----

    /** 서버 명단의 내 항목(소속, 대표 여부). 명단에 없으면 값이 null이고 오류도 없다. */
    fun myAdmin(uid: String): Reply<AdminEntry?> {
        val (c, t) = call("GET", "admins/$uid")
        if (c !in 200..299) return Reply(null, explain(c, t))
        val raw = parseMap(t) ?: return Reply(null, null)
        return Reply(AdminRoster.decodeAdmins(mapOf(uid to raw)).firstOrNull(), null)
    }

    /** 개발자: 지휘관의 소속과 대표 여부를 정한다. 성공하면 null. */
    fun setAdminGroup(uid: String, group: RallyGroup, rep: Boolean): String? {
        val (c1, t1) = call("PUT", "admins/$uid/group", JSONObject.quote(group.id))
        if (c1 !in 200..299) return explain(c1, t1)
        val (c2, t2) = call("PUT", "admins/$uid/rep", rep.toString())
        return if (c2 in 200..299) null else explain(c2, t2)
    }

    /** 연맹 대표: 내 소속의 지휘관들. 서버 규칙이 "내 소속으로 거른 조회"만 허락한다. */
    fun listGroupAdmins(group: String): Reply<List<AdminEntry>> {
        val (c, t) = call("GET", "admins", query = equalTo("group", group))
        return if (c in 200..299) Reply(AdminRoster.decodeAdmins(parseMap(t)), null) else Reply(null, explain(c, t))
    }

    /** 연맹 대표: 내가 만든 지휘관 코드들. */
    fun listMyCodes(uid: String, now: Long = System.currentTimeMillis()): Reply<List<AdminCode>> {
        val (c, t) = call("GET", "adminCodes", query = equalTo("by", uid))
        return if (c in 200..299) Reply(AdminRoster.decodeCodes(parseMap(t), now), null) else Reply(null, explain(c, t))
    }

    // ---- 연맹 대표 신청 ----

    /** 연맹 대표를 신청한다. 한 기기에 신청 하나라, 다시 신청하면 앞의 것을 덮어쓴다. 성공하면 null. */
    fun submitRequest(uid: String, group: RallyGroup, rawName: String, rawNote: String?, now: Long = System.currentTimeMillis()): String? {
        val body = RallyRoles.encodeRequest(group, rawName, rawNote, now) ?: return "게임 캐릭터명을 입력해 주세요"
        val (c, t) = call("PUT", "repRequests/$uid", JSONObject(body).toString())
        return if (c in 200..299) null else explain(c, t)
    }

    /** 내 신청. 없으면 값이 null이고 오류도 없다. */
    fun myRequest(uid: String): Reply<RepRequest?> {
        val (c, t) = call("GET", "repRequests/$uid")
        if (c !in 200..299) return Reply(null, explain(c, t))
        return Reply(RallyRoles.decodeRequest(uid, parseMap(t)), null)
    }

    fun cancelRequest(uid: String): String? = delete("repRequests/$uid")

    /** 개발자: 기다리는 신청 목록. */
    fun listRequests(): Reply<List<RepRequest>> {
        val (c, t) = call("GET", "repRequests")
        return if (c in 200..299) Reply(RallyRoles.decodeRequests(parseMap(t)), null) else Reply(null, explain(c, t))
    }

    /** 개발자: 신청을 승인한다. 그 기기를 신청한 소속의 연맹 대표로 명단에 올리고 신청을 지운다. 성공하면 null. */
    fun approveRequest(req: RepRequest, oldRepUid: String? = null, now: Long = System.currentTimeMillis()): String? {
        val (c, t) = call("PUT", "admins/${req.uid}", JSONObject(RallyRoles.approveRecord(req, now)).toString())
        if (c !in 200..299) return explain(c, t)
        // 대표는 연맹마다 한 명: 예전 대표는 일반 지휘관으로 내린다(지휘관 권한과 소속은 그대로)
        if (oldRepUid != null) {
            val (c2, t2) = call("PUT", "admins/$oldRepUid/rep", "false")
            if (c2 !in 200..299) return "새 대표는 정했지만 예전 대표를 내리지 못했어요. 지휘관 관리에서 확인해 주세요. (${explain(c2, t2)})"
        }
        delete("repRequests/${req.uid}") // 신청이 남아도 권한에는 영향이 없다
        return null
    }

    /** 개발자: 신청을 거절한다. [rawReason]은 신청자 화면에 보일 한 줄. 성공하면 null. */
    fun rejectRequest(uid: String, rawReason: String?): String? {
        val (c, t) = call("PUT", "repRequests/$uid/status", JSONObject.quote(RallyRoles.REJECTED))
        if (c !in 200..299) return explain(c, t)
        val reason = RallyRoles.cleanNote(rawReason)
        if (reason.isNotEmpty()) call("PUT", "repRequests/$uid/reason", JSONObject.quote(reason))
        return null
    }

    /** Firebase REST의 "이 값과 같은 것만" 조회. 서버 규칙의 색인(.indexOn)이 있어야 한다. */
    private fun equalTo(child: String, value: String): String =
        "orderBy=" + java.net.URLEncoder.encode("\"$child\"", "UTF-8") + "&equalTo=" + java.net.URLEncoder.encode(JSONObject.quote(value), "UTF-8")

    // ---- 내부 ----

    private fun exists(path: String): Check {
        val (code, text) = call("GET", path)
        return when {
            code in 200..299 && (text == "null" || text.isEmpty()) -> Check.NO
            code in 200..299 -> Check.YES
            else -> Check.UNKNOWN
        }
    }

    private fun delete(path: String): String? {
        val (code, text) = call("DELETE", path)
        return if (code in 200..299) null else explain(code, text)
    }

    private fun explain(code: Int, text: String): String = when (code) {
        401, 403 -> "서버가 허락하지 않았어요 (개발자 등록이나 서버 규칙을 확인해 주세요)"
        0 -> text.take(80).ifEmpty { "서버에 연결하지 못했어요" }
        else -> "서버 오류 $code"
    }

    private fun call(method: String, path: String, body: String? = null, query: String = ""): Pair<Int, String> = try {
        val target = "$dbUrl/$path.json" + if (query.isEmpty()) "" else "?$query"
        val c = URL(FirebaseAuthCodec.withAuth(target, auth.token())).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 4000; c.readTimeout = 4000
        if (body != null) {
            c.setRequestProperty("Content-Type", "application/json")
            c.doOutput = true
            c.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = c.responseCode
        val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty().trim()
        if (code == 401) auth.invalidate()
        code to text
    } catch (e: Exception) {
        0 to (e.message ?: e.javaClass.simpleName)
    }

    private fun parseMap(text: String): Map<String, Any?>? {
        if (text.isEmpty() || text == "null") return null
        @Suppress("UNCHECKED_CAST")
        return toPlain(JSONObject(text)) as? Map<String, Any?>
    }

    private fun toPlain(v: Any?): Any? = when (v) {
        is JSONObject -> v.keys().asSequence().associateWith { toPlain(v.get(it)) }
        is JSONArray -> (0 until v.length()).map { toPlain(v.get(it)) }
        JSONObject.NULL -> null
        else -> v
    }
}
