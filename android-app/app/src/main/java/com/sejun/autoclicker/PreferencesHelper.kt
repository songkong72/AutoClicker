package com.sejun.autoclicker

import android.content.Context
import android.content.SharedPreferences

enum class RepeatMode {
    INFINITE, // 무한 반복
    COUNT,    // 횟수 지정
    TIMER     // 시간 지정
}

object PreferencesHelper {
    private const val PREF_NAME = "autoclicker_prefs"

    private const val KEY_INTERVAL_MS = "key_interval_ms"
    private const val KEY_REPEAT_MODE = "key_repeat_mode"
    private const val KEY_REPEAT_COUNT = "key_repeat_count"
    private const val KEY_REPEAT_DURATION_SEC = "key_repeat_duration_sec"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getIntervalMs(context: Context): Long {
        return getPrefs(context).getLong(KEY_INTERVAL_MS, 500L)
    }

    fun setIntervalMs(context: Context, ms: Long) {
        getPrefs(context).edit().putLong(KEY_INTERVAL_MS, ms.coerceAtLeast(50L)).apply()
    }

    fun getRepeatMode(context: Context): RepeatMode {
        val name = getPrefs(context).getString(KEY_REPEAT_MODE, RepeatMode.INFINITE.name)
        return try {
            RepeatMode.valueOf(name ?: RepeatMode.INFINITE.name)
        } catch (e: Exception) {
            RepeatMode.INFINITE
        }
    }

    fun setRepeatMode(context: Context, mode: RepeatMode) {
        getPrefs(context).edit().putString(KEY_REPEAT_MODE, mode.name).apply()
    }

    fun getRepeatCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_REPEAT_COUNT, 100)
    }

    fun setRepeatCount(context: Context, count: Int) {
        getPrefs(context).edit().putInt(KEY_REPEAT_COUNT, count.coerceAtLeast(1)).apply()
    }

    fun getRepeatDurationSec(context: Context): Int {
        return getPrefs(context).getInt(KEY_REPEAT_DURATION_SEC, 60)
    }

    private const val KEY_OVERLAY_ALPHA = "key_overlay_alpha"

    fun setRepeatDurationSec(context: Context, sec: Int) {
        getPrefs(context).edit().putInt(KEY_REPEAT_DURATION_SEC, sec.coerceAtLeast(1)).apply()
    }

    fun getOverlayAlpha(context: Context): Float {
        return getPrefs(context).getFloat(KEY_OVERLAY_ALPHA, 1.0f)
    }

    fun setOverlayAlpha(context: Context, alpha: Float) {
        getPrefs(context).edit().putFloat(KEY_OVERLAY_ALPHA, alpha.coerceIn(0.2f, 1.0f)).apply()
    }

    private const val KEY_TARGET_X = "key_target_x"
    private const val KEY_TARGET_Y = "key_target_y"

    fun getTargetPosition(context: Context): Pair<Int, Int>? {
        val prefs = getPrefs(context)
        if (!prefs.contains(KEY_TARGET_X) || !prefs.contains(KEY_TARGET_Y)) return null
        return Pair(prefs.getInt(KEY_TARGET_X, -1), prefs.getInt(KEY_TARGET_Y, -1))
    }

    fun setTargetPosition(context: Context, x: Int, y: Int) {
        getPrefs(context).edit()
            .putInt(KEY_TARGET_X, x)
            .putInt(KEY_TARGET_Y, y)
            .apply()
    }

    private const val KEY_SAVED_RALLY_TARGET_X = "key_saved_rally_target_x"
    private const val KEY_SAVED_RALLY_TARGET_Y = "key_saved_rally_target_y"

    private const val KEY_RALLY_AUTO_MODE = "key_rally_auto_mode"

    /**
     * 유저가 [📍 타겟위치 저장] 버튼으로 명시적으로 지정한 집결 클릭 목표 좌표
     */
    fun getSavedRallyTargetPosition(context: Context): Pair<Int, Int>? {
        val prefs = getPrefs(context)
        if (!prefs.contains(KEY_SAVED_RALLY_TARGET_X) || !prefs.contains(KEY_SAVED_RALLY_TARGET_Y)) return null
        val x = prefs.getInt(KEY_SAVED_RALLY_TARGET_X, -1)
        val y = prefs.getInt(KEY_SAVED_RALLY_TARGET_Y, -1)
        if (x < 0 || y < 0) return null
        return Pair(x, y)
    }

    fun setSavedRallyTargetPosition(context: Context, x: Int, y: Int) {
        getPrefs(context).edit()
            .putInt(KEY_SAVED_RALLY_TARGET_X, x)
            .putInt(KEY_SAVED_RALLY_TARGET_Y, y)
            .apply()
    }

    /**
     * 5초 전 자동 대기 전환 모드 (true: 자동 대기 전환, false: 수동 대기 전용 스킵)
     */
    fun isAutoRallyMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_RALLY_AUTO_MODE, false)
    }

    fun setAutoRallyMode(context: Context, enabled: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_RALLY_AUTO_MODE, enabled)
            .apply()
    }

    private const val KEY_LAST_MARCH_DURATION_SEC = "key_last_march_duration_sec"

    fun getLastMarchDurationSec(context: Context): Double? {
        val prefs = getPrefs(context)
        if (!prefs.contains(KEY_LAST_MARCH_DURATION_SEC)) return null
        return try {
            val f = prefs.getFloat(KEY_LAST_MARCH_DURATION_SEC, -1f)
            if (f >= 0) f.toDouble() else null
        } catch (e: Exception) {
            try {
                val i = prefs.getInt(KEY_LAST_MARCH_DURATION_SEC, -1)
                if (i >= 0) i.toDouble() else null
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun setLastMarchDurationSec(context: Context, sec: Double) {
        getPrefs(context).edit()
            .putFloat(KEY_LAST_MARCH_DURATION_SEC, sec.toFloat())
            .apply()
    }

    private const val KEY_CLICK_OFFSET_MS = "key_click_offset_ms"

    fun getClickOffsetMs(context: Context): Int {
        return getPrefs(context).getInt(KEY_CLICK_OFFSET_MS, 0)
    }

    fun setClickOffsetMs(context: Context, offsetMs: Int) {
        getPrefs(context).edit()
            .putInt(KEY_CLICK_OFFSET_MS, offsetMs)
            .apply()
    }

    private const val KEY_RALLY_DIALOG_ALPHA = "key_rally_dialog_alpha"

    fun getRallyDialogAlpha(context: Context): Float {
        return getPrefs(context).getFloat(KEY_RALLY_DIALOG_ALPHA, 1.0f)
    }

    fun setRallyDialogAlpha(context: Context, alpha: Float) {
        getPrefs(context).edit()
            .putFloat(KEY_RALLY_DIALOG_ALPHA, alpha)
            .apply()
    }

    // --- 초대코드 및 지휘관 권한 관련 설정 ---
    private const val KEY_IS_VERIFIED = "key_is_verified"
    private const val KEY_VERIFIED_USER_ID = "key_verified_user_id"

    /** 기능 사용 권한: 초대코드 인증을 했거나 지휘관으로 로그인한 기기. */
    fun hasAccess(context: Context): Boolean =
        !isUserView(context) && (isVerified(context) || isAdminMode(context) || isRosterAdmin(context))

    private const val KEY_USER_VIEW = "key_user_view"

    /**
     * 개발자가 일반 사용자 화면을 보는 중인지. 켜져 있으면 인증·지휘관 상태는 그대로 두고 집결 기능만 가린다
     * (서버 등록과 초대 인증은 건드리지 않아서 끄면 바로 원래 화면으로 돌아온다).
     */
    fun isUserView(context: Context): Boolean = getPrefs(context).getBoolean(KEY_USER_VIEW, false)

    fun setUserView(context: Context, on: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_USER_VIEW, on).apply()
    }

    fun isVerified(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_VERIFIED, false)
    }

    fun setVerified(context: Context, verified: Boolean, userId: String = "") {
        getPrefs(context).edit()
            .putBoolean(KEY_IS_VERIFIED, verified)
            .putString(KEY_VERIFIED_USER_ID, userId)
            .apply()
    }

    private const val KEY_RALLY_MEMBER_ID = "key_rally_member_id"
    private const val KEY_RALLY_CHARACTER_NAME = "key_rally_character_name"

    /** 이 기기의 집결 명단 ID. 처음 요청할 때 한 번 만들어 계속 쓴다(앱을 지우고 다시 설치하면 새로 만들어진다). */
    fun getRallyMemberId(context: Context): String {
        val prefs = getPrefs(context)
        val cur = prefs.getString(KEY_RALLY_MEMBER_ID, null)
        if (cur != null && RallyRoster.isValidMemberId(cur)) return cur
        val id = RallyRoster.newMemberId()
        prefs.edit().putString(KEY_RALLY_MEMBER_ID, id).apply()
        return id
    }

    /** 게임 캐릭터명. 지휘관이 군단을 배정할 때 명단에 이 이름으로 보인다. */
    fun getRallyCharacterName(context: Context): String = getPrefs(context).getString(KEY_RALLY_CHARACTER_NAME, "") ?: ""

    fun setRallyCharacterName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_RALLY_CHARACTER_NAME, name).apply()
    }

    private const val KEY_IS_ADMIN_MODE = "key_is_admin_mode"

    /** 지휘관 비밀번호로 로그인한 기기인지. 집결 화면에서 지휘관/집결장 권한을 가른다. */
    fun isAdminMode(context: Context): Boolean = getPrefs(context).getBoolean(KEY_IS_ADMIN_MODE, false)

    fun setAdminMode(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_ADMIN_MODE, enabled).apply()
    }

    private const val KEY_ADMIN_VIA_SERVER = "key_admin_via_server"

    /** 지휘관 코드로 서버 명단에 올라 지휘관이 된 기기인지(비밀번호로 들어간 기기는 false). 서버에서 지우면 앱이 지휘관 모드를 풀어 준다. */
    fun isAdminViaServer(context: Context): Boolean = getPrefs(context).getBoolean(KEY_ADMIN_VIA_SERVER, false)

    fun setAdminViaServer(context: Context, viaServer: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ADMIN_VIA_SERVER, viaServer).apply()
    }

    private const val KEY_ROSTER_ADMIN = "key_roster_admin"

    /**
     * 서버가 이 기기를 개발자나 지휘관으로 알고 있다고 마지막으로 확인됐는지. 지휘관 화면을 끄고 집결장으로 지내는 동안에도 남는다.
     * 이 표시가 있으면 "지휘관으로 전환" 버튼을 보여 주고, 누를 때마다 서버 명단을 다시 확인한다(여기 값만 믿고 들여보내지 않는다).
     */
    fun isRosterAdmin(context: Context): Boolean = getPrefs(context).getBoolean(KEY_ROSTER_ADMIN, false)

    fun setRosterAdmin(context: Context, known: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ROSTER_ADMIN, known).apply()
    }

    fun getVerifiedUserId(context: Context): String {
        return getPrefs(context).getString(KEY_VERIFIED_USER_ID, "") ?: ""
    }
}
