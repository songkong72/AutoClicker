package com.sejun.autoclicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseAuthTest {
    @Test fun parsesAnonymousSignUp() {
        val s = FirebaseAuthCodec.parseSession(
            """{"idToken":"ID1","refreshToken":"RF1","expiresIn":"3600","localId":"u"}""", nowMs = 1_000L)!!
        assertEquals("ID1", s.idToken); assertEquals("RF1", s.refreshToken)
        assertEquals(1_000L + 3_600_000L, s.expiresAtMs)
    }

    @Test fun parsesTokenRefreshResponse() {
        val s = FirebaseAuthCodec.parseSession(
            """{"id_token":"ID2","refresh_token":"RF2","expires_in":"3600"}""", nowMs = 0L)!!
        assertEquals("ID2", s.idToken); assertEquals("RF2", s.refreshToken)
    }

    @Test fun badResponseGivesNull() {
        assertNull(FirebaseAuthCodec.parseSession("""{"error":{"message":"CONFIGURATION_NOT_FOUND"}}""", 0L))
        assertNull(FirebaseAuthCodec.parseSession("not json", 0L))
    }

    @Test fun refreshesFiveMinutesBeforeExpiry() {
        val s = AuthSession("a", "r", expiresAtMs = 10_000_000L)
        assertEquals(false, s.needsRefresh(nowMs = 10_000_000L - 301_000L))
        assertEquals(true, s.needsRefresh(nowMs = 10_000_000L - 299_000L))
    }

    @Test fun addsAuthToUrlWithAndWithoutQuery() {
        assertEquals("https://x/a.json?auth=T", FirebaseAuthCodec.withAuth("https://x/a.json", "T"))
        assertEquals("https://x/a.json?shallow=true&auth=T", FirebaseAuthCodec.withAuth("https://x/a.json?shallow=true", "T"))
        assertEquals("https://x/a.json", FirebaseAuthCodec.withAuth("https://x/a.json", null))
    }

    @Test fun tokenIsUrlEncoded() {
        assertTrue(FirebaseAuthCodec.withAuth("https://x/a.json", "a b+c").endsWith("auth=a+b%2Bc"))
    }

    @Test fun signUpKeepsTheAnonymousUserId() {
        val s = FirebaseAuthCodec.parseSession(
            """{"idToken":"ID1","refreshToken":"RF1","expiresIn":"3600","localId":"UID123"}""", nowMs = 0L)!!
        assertEquals("UID123", s.uid)
    }

    @Test fun refreshResponseKeepsTheUserId() {
        val s = FirebaseAuthCodec.parseSession(
            """{"id_token":"ID2","refresh_token":"RF2","expires_in":"3600","user_id":"UID456"}""", nowMs = 0L)!!
        assertEquals("UID456", s.uid)
    }

    @Test fun missingUserIdIsEmpty() {
        val s = FirebaseAuthCodec.parseSession("""{"id_token":"I","refresh_token":"R","expires_in":"3600"}""", 0L)!!
        assertEquals("", s.uid)
    }
}
