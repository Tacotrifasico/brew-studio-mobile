package com.example.data.remote

import org.junit.Assert.*
import org.junit.Test

class AccountAccessTest {
    private val id = "11111111-1111-4111-8111-111111111111"
    @Test fun guestAndDemoCannotEnter() {
        assertFalse(AccountAccess.canEnter(false, null, null))
        assertFalse(AccountAccess.canEnter(true, "local_demo_id_001", "demo@example.com"))
        assertFalse(AccountAccess.canEnter(true, id, " "))
    }
    @Test fun savedAccountDoesNotRequireNetworkForLocalTools() {
        assertTrue(AccountAccess.canEnter(true, id, "barista@example.com"))
    }
    @Test fun signOutOrRevocationClosesAccess() {
        assertFalse(AccountAccess.canEnter(false, id, "barista@example.com"))
    }
}
