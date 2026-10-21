package com.example.data.remote

import org.junit.Assert.*
import org.junit.Test

class GoogleOAuthFlowTest {
    private val redirect = "com.aistudio.brewstudio.tfcqwy://auth/confirmed"
    @Test fun rfc7636Challenge() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            GoogleOAuthFlow.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
    }
    @Test fun randomAndUrlSafe() {
        val values = (1..100).map { GoogleOAuthFlow.verifier() }
        assertEquals(100, values.distinct().size)
        assertTrue(values.all { it.length == 43 && it.matches(Regex("[A-Za-z0-9_-]+")) })
    }
    @Test fun browserOnlyCarriesChallenge() {
        val verifier = GoogleOAuthFlow.verifier()
        val url = GoogleOAuthFlow.authorizationURL("https://example.supabase.co", redirect, verifier)
        assertTrue(url.contains("provider=google") && url.contains("code_challenge_method=s256"))
        assertFalse(url.contains(verifier)); assertFalse(url.contains("secret"))
    }
    @Test fun exactReturnAndCode() {
        assertEquals("one-time-code", GoogleOAuthFlow.code("$redirect?code=one-time-code", redirect))
        for (invalid in listOf("$redirect?code=a&code=b", "$redirect?code=a#access_token=forged",
            "$redirect?access_token=forged", "$redirect?error=access_denied", "$redirect?code=",
            "com.other://auth/confirmed?code=a", "${redirect}extra?code=a",
            "com.aistudio.brewstudio.tfcqwy://evil/confirmed?code=a")) {
            assertTrue("Rejected $invalid", runCatching { GoogleOAuthFlow.code(invalid, redirect) }.isFailure)
        }
    }
    @Test fun browserMarkersDoNotBecomeCredentials() {
        for (suffix in listOf("?code=once#", "?code=once#_", "?code=once#sb=", "/?code=once")) {
            assertEquals("once", GoogleOAuthFlow.code(redirect + suffix, redirect))
        }
        assertTrue(runCatching { GoogleOAuthFlow.code("$redirect?code=a#sb=&access_token=forged", redirect) }.isFailure)
    }
    @Test fun knownErrorsAreActionableAndDoNotEchoProviderData() {
        val error = runCatching { GoogleOAuthFlow.code("$redirect?error=access_denied&error_code=flow_state_expired&error_description=SECRET", redirect) }.exceptionOrNull()!!
        assertTrue(error.message!!.contains("G05"))
        assertFalse(error.message!!.contains("SECRET"))
    }
}
