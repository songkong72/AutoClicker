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
 * 서버 관리자 명단(owners / admins / adminCodes)과 통신한다(Firebase REST). 네트워크를 쓰므로 메인 스레드에서 부르지 않는다.
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

    /** 새 관리자 코드를 만든다. 성공하면 코드를 돌려준다. */
    fun createCode(rawName: String, ttl: CodeTtl = CodeTtl.DAY, now: Long = System.currentTimeMillis()): Reply<String> {
        val body = AdminRoster.encodeCode(rawName, now, ttl.ms) ?: return Reply(null, "이름표를 입력해 주세요")
        val newCode = AdminRoster.newCode()
        val (code, text) = call("PUT", "adminCodes/$newCode", JSONObject(body).toString())
        return if (code in 200..299) Reply(newCode, null) else Reply(null, explain(code, text))
    }

    fun cancelCode(code: String): String? = delete("adminCodes/$code")

    /** 관리자의 이름표를 고친다. 성공하면 null, 실패하면 이유. 개발자만 된다. */
    fun renameAdmin(uid: String, rawName: String): String? {
        val name = AdminRoster.cleanName(rawName)
        if (name.isEmpty()) return "이름표를 입력해 주세요"
        val (code, text) = call("PUT", "admins/$uid/name", JSONObject.quote(name))
        return if (code in 200..299) null else explain(code, text)
    }

    /** 이름이 없는 관리자에게 등록에 쓴 코드의 이름표를 복사해 둔다. 코드를 정리해도 이름이 남게 한다. 실패해도 목록 보기에는 영향이 없다. */
    fun backfillNames(admins: List<AdminEntry>, codes: List<AdminCode>) {
        for ((uid, name) in AdminRoster.nameBackfill(admins, codes)) {
            call("PUT", "admins/$uid/name", JSONObject.quote(name))
        }
    }

    /** 이미 쓰였거나 기한이 지난 코드를 서버에서 지운다. 지우기 전에 관리자 이름표를 먼저 저장한다. 지운 개수를 돌려준다. */
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

    /** 개발자 전용: 서버의 모든 방과 방 명단을 읽어 온다. 규칙에서 개발자(owners)만 통째로 읽을 수 있다. */
    fun loadRooms(): Reply<RoomData> {
        val (c1, t1) = call("GET", "rallyRooms")
        if (c1 !in 200..299) return Reply(null, explain(c1, t1))
        val (c2, t2) = call("GET", "rallyMembers")
        if (c2 !in 200..299) return Reply(null, explain(c2, t2))
        return Reply(RoomData(parseMap(t1), parseMap(t2)), null)
    }

    /** 내가 서버 명단에 있는 관리자일 때 마지막 접속 시각과 앱 버전을 적는다. 실패해도 조용히 넘어간다. */
    fun reportSelf(uid: String, now: Long, appVersion: String) {
        call("PUT", "admins/$uid/lastSeen", now.toString())
        call("PUT", "admins/$uid/appVersion", JSONObject.quote(appVersion.take(20)))
    }

    fun removeAdmin(uid: String): String? = delete("admins/$uid")

    /** 코드로 관리자가 된다. 성공하면 null, 실패하면 이유. 코드를 먼저 내 것으로 잡고(먼저 쓴 사람만 성공), 그다음 명단에 올린다. */
    fun redeem(rawCode: String, now: Long = System.currentTimeMillis()): String? {
        val code = AdminRoster.normalizeCode(rawCode) ?: return "관리자 코드 모양이 맞지 않아요 (AD-로 시작하는 10자)"
        val me = uid()
        val uid = me.value ?: return me.error
        val (c1, t1) = call("PUT", "adminCodes/$code/usedBy", JSONObject.quote(uid))
        if (c1 !in 200..299) return if (c1 == 401 || c1 == 403) "코드가 없거나, 이미 쓰였거나, 기한(24시간)이 지났어요" else explain(c1, t1)
        val (c2, t2) = call("PUT", "admins/$uid", JSONObject(AdminRoster.adminRecord(code, now)).toString())
        return if (c2 in 200..299) null else explain(c2, t2)
    }

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

    private fun call(method: String, path: String, body: String? = null): Pair<Int, String> = try {
        val c = URL(FirebaseAuthCodec.withAuth("$dbUrl/$path.json", auth.token())).openConnection() as HttpURLConnection
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
