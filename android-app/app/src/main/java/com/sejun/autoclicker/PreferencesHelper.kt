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

    // --- 초대코드 및 관리자 권한 관련 설정 ---
    private const val KEY_IS_VERIFIED = "key_is_verified"
    private const val KEY_VERIFIED_USER_ID = "key_verified_user_id"
    private const val KEY_ADMIN_MASTER_KEY = "key_admin_master_key" // 예전 평문 저장 키(삭제 대상)
    private const val KEY_ADMIN_PW_HASH = "key_admin_pw_hash"

    fun isVerified(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_VERIFIED, false)
    }

    fun setVerified(context: Context, verified: Boolean, userId: String = "") {
        getPrefs(context).edit()
            .putBoolean(KEY_IS_VERIFIED, verified)
            .putString(KEY_VERIFIED_USER_ID, userId)
            .apply()
    }

    private const val KEY_IS_ADMIN_MODE = "key_is_admin_mode"

    /** 관리자 비밀번호로 로그인한 기기인지. 집결 화면에서 관리자/팀장 권한을 가른다. */
    fun isAdminMode(context: Context): Boolean = getPrefs(context).getBoolean(KEY_IS_ADMIN_MODE, false)

    fun setAdminMode(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_ADMIN_MODE, enabled).apply()
    }

    fun getVerifiedUserId(context: Context): String {
        return getPrefs(context).getString(KEY_VERIFIED_USER_ID, "") ?: ""
    }

    /** 관리자 비밀번호 해시. 이 기기에서 바꾼 적이 없으면 빌드에 주입된 값을 쓴다. 예전 평문 비밀번호는 지운다. */
    fun getAdminPasswordHash(context: Context): String {
        val prefs = getPrefs(context)
        if (prefs.contains(KEY_ADMIN_MASTER_KEY)) prefs.edit().remove(KEY_ADMIN_MASTER_KEY).apply()
        return prefs.getString(KEY_ADMIN_PW_HASH, null) ?: BuildConfig.ADMIN_PASSWORD_HASH
    }

    fun setAdminPasswordHash(context: Context, hash: String) {
        getPrefs(context).edit().putString(KEY_ADMIN_PW_HASH, hash).apply()
    }
}
