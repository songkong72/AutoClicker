package com.sejun.autoclicker

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/**
 * 집결 동시 착탄을 위한 작전 그룹 모델
 */
data class RallyGroup(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var leaderName: String = "",         // 집결장 이름 (리더명)
    var members: String = "",            // 집결원 목록 (쉼표 구분 텍스트)
    var targetHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    var targetMinute: Int = Calendar.getInstance().get(Calendar.MINUTE),
    var targetSecond: Int = 0,
    var marchDurationSec: Double = 120.0, // 기본 2분 (120.0초, 0.1초 단위 지원)
    var rallyWaitMinutes: Int = 5,   // 집결 대기 시간 (1분, 3분, 5분, 10분 - 기본 5분)
    var isAutoMode: Boolean = false, // 해당 군단의 자동 대기 모드 활성화 여부
    var lastDepartedTimestamp: Long = 0L // 클릭 출발이 실제로 실행된 departureTimestamp
,
    var isAutoTargetTime: Boolean = false,
    var targetTimeOffsetSec: Int = 90
) {
    /**
     * 해당 출발 시각(depTs)에 실제로 클릭(발사)이 실행되었는지 여부
     */
    fun hasExecutedClick(depTs: Long = calculateDepartureTimestamp()): Boolean {
        return lastDepartedTimestamp != 0L && Math.abs(lastDepartedTimestamp - depTs) < 25_000L
    }

    /**
     * 화면 표기용 이름 (예: "1군 집결 (캡틴김)" 또는 "1군 집결")
     */
    fun getDisplayName(): String {
        return if (leaderName.isNotBlank()) {
            "$name ($leaderName)"
        } else {
            name
        }
    }

    /**
     * 목표 성 도착 시각 포맷 (예: "21:30:00")
     */
    fun getTargetTimeString(): String {
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", targetHour, targetMinute, targetSecond)
    }

    /**
     * 오늘 또는 내일의 성 도착 목표 시각(밀리초 타임스탬프) 계산
     */
    fun calculateTargetTimestamp(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, targetSecond)
            set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()
        // 만약 목표 시각이 현재보다 이미 4시간 이상 지났다면(예: 밤 23시에 새벽 1시 예약 등), 내일 같은 시각으로 간주
        // 진행 중인 작전(집결 대기, 행군, 도착 완료 직후)은 오늘 타임라인으로 유지
        if (cal.timeInMillis < now - 4 * 3600_000L) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    /**
     * 집결 오픈 클릭 시각 (도착 시각 - 행군 시간 - 집결 대기 시간)
     * 즉, 오토클리커가 집결 열기 버튼을 클릭해야 하는 시각 (0.1초 단위 밀리초까지 정밀 계산)
     */
    fun calculateDepartureTimestamp(): Long {
        val totalLeadTimeMs = (marchDurationSec * 1000.0).toLong() + (rallyWaitMinutes * 60_000L)
        return calculateTargetTimestamp() - totalLeadTimeMs
    }

    /**
     * 집결 오픈 시각 포맷 (소수점 초가 있을 시 예: "21:23:00.5", 없을 시 "21:23:00")
     */
    fun getDepartureTimeString(): String {
        val depMs = calculateDepartureTimestamp()
        val cal = Calendar.getInstance().apply {
            timeInMillis = depMs
        }
        val tenth = (((depMs % 1000L) + 1000L) % 1000L) / 100L
        return if (tenth > 0L) {
            String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d.%d",
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                cal.get(Calendar.SECOND),
                tenth
            )
        } else {
            String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d",
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                cal.get(Calendar.SECOND)
            )
        }
    }

    /**
     * 부대가 실제로 출발하는 시각 (집결 오픈 후 rallyWaitMinutes 경과 시점, 즉 도착 시각 - 행군 시간)
     */
    fun getActualDepartureTimeString(): String {
        val actualDepMs = calculateTargetTimestamp() - (marchDurationSec * 1000.0).toLong()
        val cal = Calendar.getInstance().apply {
            timeInMillis = actualDepMs
        }
        val tenth = (((actualDepMs % 1000L) + 1000L) % 1000L) / 100L
        return if (tenth > 0L) {
            String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d.%d",
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                cal.get(Calendar.SECOND),
                tenth
            )
        } else {
            String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d",
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                cal.get(Calendar.SECOND)
            )
        }
    }

    /**
     * 지금부터 집결 오픈 클릭까지 남은 시간 (밀리초)
     */
    fun getRemainingMillis(): Long {
        return calculateDepartureTimestamp() - System.currentTimeMillis()
    }

    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("leaderName", leaderName)
            put("members", members)
            put("targetHour", targetHour)
            put("targetMinute", targetMinute)
            put("targetSecond", targetSecond)
            put("marchDurationSec", marchDurationSec)
            put("rallyWaitMinutes", rallyWaitMinutes)
            put("isAutoMode", isAutoMode)
            put("lastDepartedTimestamp", lastDepartedTimestamp)
            put("isAutoTargetTime", isAutoTargetTime)
            put("targetTimeOffsetSec", targetTimeOffsetSec)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): RallyGroup {
            return RallyGroup(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", "1군 집결"),
                leaderName = json.optString("leaderName", ""),
                members = json.optString("members", ""),
                targetHour = json.optInt("targetHour", 21),
                targetMinute = json.optInt("targetMinute", 30),
                targetSecond = json.optInt("targetSecond", 0),
                marchDurationSec = json.optDouble("marchDurationSec", 120.0),
                rallyWaitMinutes = json.optInt("rallyWaitMinutes", 5),
                isAutoMode = json.optBoolean("isAutoMode", false),
                lastDepartedTimestamp = json.optLong("lastDepartedTimestamp", 0L),
                isAutoTargetTime = json.optBoolean("isAutoTargetTime", false),
                targetTimeOffsetSec = json.optInt("targetTimeOffsetSec", 90)
            )
        }

        fun formatDuration(totalSec: Double): String {
            val m = (totalSec / 60).toInt()
            val s = totalSec % 60
            val sStr = if (s % 1.0 == 0.0) "${s.toInt()}초" else String.format(Locale.US, "%.1f초", s)
            return if (m > 0) "${m}분 $sStr" else sStr
        }
    }
}

/**
 * 클립보드 파싱 결과
 */
data class ParsedOperation(
    val groupName: String?,
    val leaderName: String?,
    val hour: Int,
    val minute: Int,
    val second: Int,
    val isAllOrder: Boolean = false
)

/**
 * 작전 그룹 영구 저장 및 관리 매니저
 */
object RallyGroupManager {
    private const val PREF_NAME = "rally_group_prefs"
    private const val KEY_GROUPS_JSON = "key_rally_groups_json"
    private const val KEY_SELECTED_GROUP_ID = "key_selected_group_id"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getGroups(context: Context): MutableList<RallyGroup> {
        val jsonStr = getPrefs(context).getString(KEY_GROUPS_JSON, null)
        val list = mutableListOf<RallyGroup>()
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    list.add(RallyGroup.fromJsonObject(array.getJSONObject(i)))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 비어있는 경우 기본 작전 프리셋 (1군, 2군, 3군) 자동 생성
        if (list.isEmpty()) {
            val nowCal = Calendar.getInstance()
            val nowH = nowCal.get(Calendar.HOUR_OF_DAY)
            val nowM = nowCal.get(Calendar.MINUTE)
            list.add(
                RallyGroup(
                    name = "1군 집결",
                    leaderName = "1군장",
                    targetHour = nowH,
                    targetMinute = nowM,
                    targetSecond = 0,
                    marchDurationSec = 120.0
                )
            )
            list.add(
                RallyGroup(
                    name = "2군 집결",
                    leaderName = "2군장",
                    targetHour = nowH,
                    targetMinute = nowM,
                    targetSecond = 0,
                    marchDurationSec = 120.0
                )
            )
            list.add(
                RallyGroup(
                    name = "3군 집결",
                    leaderName = "3군장",
                    targetHour = nowH,
                    targetMinute = nowM,
                    targetSecond = 0,
                    marchDurationSec = 120.0
                )
            )
            saveGroups(context, list)
        }
        return list
    }

    fun saveGroups(context: Context, groups: List<RallyGroup>) {
        val sortedGroups = groups.sortedBy { it.name }
        val array = JSONArray()
        sortedGroups.forEach { array.put(it.toJsonObject()) }
        getPrefs(context).edit().putString(KEY_GROUPS_JSON, array.toString()).apply()
    }

    fun getSelectedGroupId(context: Context): String {
        val groups = getGroups(context)
        val savedId = getPrefs(context).getString(KEY_SELECTED_GROUP_ID, null)
        val exists = groups.any { it.id == savedId }
        return if (exists && savedId != null) {
            savedId
        } else {
            val firstId = groups.first().id
            setSelectedGroupId(context, firstId)
            firstId
        }
    }

    fun setSelectedGroupId(context: Context, id: String) {
        getPrefs(context).edit().putString(KEY_SELECTED_GROUP_ID, id).apply()
    }

    fun getSelectedGroup(context: Context): RallyGroup {
        val groups = getGroups(context)
        val selectedId = getSelectedGroupId(context)
        return groups.find { it.id == selectedId } ?: groups.first()
    }

    /**
     * 1군, 2군, 3군 등 특정 이름의 그룹을 조회하거나 없으면 생성하여 선택
     */
    fun getOrCreateGroupByName(context: Context, armyName: String, defaultLeader: String = ""): RallyGroup {
        val groups = getGroups(context)
        val existing = groups.find { it.name.trim() == armyName.trim() }
        if (existing != null) {
            setSelectedGroupId(context, existing.id)
            return existing
        }
        val current = getSelectedGroup(context)
        val newGroup = RallyGroup(
            name = armyName,
            leaderName = defaultLeader,
            targetHour = current.targetHour,
            targetMinute = current.targetMinute,
            targetSecond = current.targetSecond,
            marchDurationSec = current.marchDurationSec,
            rallyWaitMinutes = current.rallyWaitMinutes
        )
        groups.add(newGroup)
        saveGroups(context, groups)
        setSelectedGroupId(context, newGroup.id)
        return newGroup
    }

    /**
     * 전체 오더: 모든 군(1군, 2군, 3군 등)의 도착 목표 시각, 집결 대기시간을 일괄 일치시킴
     */
    fun applyTargetTimeToAllGroups(context: Context, hour: Int, minute: Int, second: Int, rallyWaitMinutes: Int? = null) {
        val groups = getGroups(context)
        groups.forEach {
            it.targetHour = hour
            it.targetMinute = minute
            it.targetSecond = second
            it.lastDepartedTimestamp = 0L // 새 작전 시간이 설정되면 이전 출발 완료 상태 리셋
            if (rallyWaitMinutes != null) {
                it.rallyWaitMinutes = rallyWaitMinutes
            }
        }
        saveGroups(context, groups)
    }

    fun addGroup(
        context: Context,
        name: String,
        leaderName: String,
        targetHour: Int,
        targetMinute: Int,
        targetSecond: Int,
        marchDurationSec: Double = 120.0,
        rallyWaitMinutes: Int = 5
    ): RallyGroup {
        val groups = getGroups(context)
        val newGroup = RallyGroup(
            name = name,
            leaderName = leaderName,
            targetHour = targetHour,
            targetMinute = targetMinute,
            targetSecond = targetSecond,
            marchDurationSec = marchDurationSec,
            rallyWaitMinutes = rallyWaitMinutes
        )
        groups.add(newGroup)
        saveGroups(context, groups)
        setSelectedGroupId(context, newGroup.id)
        return newGroup
    }

    fun updateGroup(context: Context, updated: RallyGroup) {
        val groups = getGroups(context)
        val index = groups.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            groups[index] = updated
            saveGroups(context, groups)
        }
    }

    fun deleteGroup(context: Context, groupId: String): Boolean {
        val groups = getGroups(context)
        if (groups.size <= 1) {
            return false // 최소 1개 그룹은 유지
        }
        val removed = groups.removeAll { it.id == groupId }
        if (removed) {
            saveGroups(context, groups)
            if (getSelectedGroupId(context) == groupId) {
                setSelectedGroupId(context, groups.first().id)
            }
        }
        return removed
    }

    /**
     * 작전 공유 코드 생성
     * 개별: [오토작전:1군 집결|집결장:김철수|21:30:00]
     * 전체오더: [오토작전:전체오더|21:30:00]
     */
    fun exportOperationCode(group: RallyGroup, isAllOrder: Boolean = false): String {
        return if (isAllOrder) {
            "[오토작전:전체오더|${group.getTargetTimeString()}]"
        } else {
            val leaderPart = if (group.leaderName.isNotBlank()) "|집결장:${group.leaderName}" else ""
            "[오토작전:${group.name}${leaderPart}|${group.getTargetTimeString()}]"
        }
    }

    /**
     * 클립보드 텍스트 등에서 작전 코드 파싱
     * 1) [오토작전:전체오더|21:30:00]
     * 2) [오토작전:1군 집결|집결장:김철수|21:30:00]
     * 3) [오토작전:1군 집결|21:30:00]
     * 4) 21:30:00 단순 시간
     */
    fun parseOperationCode(text: String): ParsedOperation? {
        val trimmed = text.trim()

        // 1. 전체 오더 패턴: [오토작전:전체오더|21:30:00]
        val regexAllOrder = Regex("""\[(?:오토)?작전:\s*전체\s*오더\s*\|\s*(\d{1,2}):(\d{1,2}):(\d{1,2})\s*\]""")
        val matchAll = regexAllOrder.find(trimmed)
        if (matchAll != null) {
            val h = matchAll.groupValues[1].toIntOrNull() ?: return null
            val m = matchAll.groupValues[2].toIntOrNull() ?: return null
            val s = matchAll.groupValues[3].toIntOrNull() ?: return null
            if (h in 0..23 && m in 0..59 && s in 0..59) {
                return ParsedOperation("전체오더", null, h, m, s, isAllOrder = true)
            }
        }

        // 2. 개별 군 작전 패턴 (집결장 포함 또는 미포함)
        val regexWithLeader = Regex("""\[(?:오토)?작전:\s*(.*?)\s*\|\s*집결장:\s*(.*?)\s*\|\s*(\d{1,2}):(\d{1,2}):(\d{1,2})\s*\]""")
        val matchWithLeader = regexWithLeader.find(trimmed)
        if (matchWithLeader != null) {
            val name = matchWithLeader.groupValues[1].trim()
            val leader = matchWithLeader.groupValues[2].trim()
            val h = matchWithLeader.groupValues[3].toIntOrNull() ?: return null
            val m = matchWithLeader.groupValues[4].toIntOrNull() ?: return null
            val s = matchWithLeader.groupValues[5].toIntOrNull() ?: return null
            if (h in 0..23 && m in 0..59 && s in 0..59) {
                return ParsedOperation(name.ifBlank { null }, leader.ifBlank { null }, h, m, s, isAllOrder = false)
            }
        }

        // 3. 기존 표준 패턴: [오토작전:작전명|21:30:00]
        val regexSimple = Regex("""\[(?:오토)?작전:\s*(.*?)\s*\|\s*(\d{1,2}):(\d{1,2}):(\d{1,2})\s*\]""")
        val matchSimple = regexSimple.find(trimmed)
        if (matchSimple != null) {
            val name = matchSimple.groupValues[1].trim()
            val h = matchSimple.groupValues[2].toIntOrNull() ?: return null
            val m = matchSimple.groupValues[3].toIntOrNull() ?: return null
            val s = matchSimple.groupValues[4].toIntOrNull() ?: return null
            if (h in 0..23 && m in 0..59 && s in 0..59) {
                val isAll = name.contains("전체")
                return ParsedOperation(name.ifBlank { null }, null, h, m, s, isAllOrder = isAll)
            }
        }

        // 4. 단순 HH:mm:ss 패턴
        val regexTime = Regex("""(\d{1,2}):(\d{1,2}):(\d{1,2})""")
        val matchTime = regexTime.find(trimmed)
        if (matchTime != null) {
            val h = matchTime.groupValues[1].toIntOrNull() ?: return null
            val m = matchTime.groupValues[2].toIntOrNull() ?: return null
            val s = matchTime.groupValues[3].toIntOrNull() ?: return null
            if (h in 0..23 && m in 0..59 && s in 0..59) {
                return ParsedOperation(null, null, h, m, s, isAllOrder = false)
            }
        }

        return null
    }
}
