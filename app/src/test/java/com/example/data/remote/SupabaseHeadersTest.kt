package com.example.data.remote

import org.junit.Assert.*
import org.junit.Test

class SupabaseHeadersTest {
    @Test fun publishableKeyIsNotUsedAsBearerWithoutASession() {
        assertNull(supabaseBearerToken("sb_publishable_test_only", null))
        assertNull(supabaseBearerToken("sb_publishable_test_only", "  "))
    }
    @Test fun signedInUserSuppliesBearerIndependentOfPublicKeyType() {
        assertEquals("user-session-jwt", supabaseBearerToken("sb_publishable_test_only", "user-session-jwt"))
        assertEquals("user-session-jwt", supabaseBearerToken("eyJlegacy.payload.signature", " user-session-jwt "))
    }
    @Test fun legacyAnonJwtRemainsCompatibleAndEmptyKeyDoesNotBecomeBearer() {
        assertEquals("eyJlegacy.payload.signature", supabaseBearerToken("eyJlegacy.payload.signature", null))
        assertNull(supabaseBearerToken("", null))
        assertNull(supabaseBearerToken("placeholder", null))
    }
}
