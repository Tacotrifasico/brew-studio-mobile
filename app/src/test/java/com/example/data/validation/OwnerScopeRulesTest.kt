package com.example.data.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerScopeRulesTest {
    @Test fun guestAndAccountScopesNeverExposeAnotherAccount() {
        assertTrue(OwnerScopeRules.isVisible(null, null))
        assertFalse(OwnerScopeRules.isVisible("owner-a", null))
        assertTrue(OwnerScopeRules.isVisible(null, "owner-a"))
        assertTrue(OwnerScopeRules.isVisible("owner-a", "owner-a"))
        assertFalse(OwnerScopeRules.isVisible("owner-b", "owner-a"))
        assertTrue(OwnerScopeRules.canSync(null, "owner-a"))
        assertTrue(OwnerScopeRules.canSync("owner-a", "owner-a"))
        assertFalse(OwnerScopeRules.canSync("owner-b", "owner-a"))
    }
}
