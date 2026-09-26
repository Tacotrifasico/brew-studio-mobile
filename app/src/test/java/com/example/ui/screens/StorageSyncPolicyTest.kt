package com.example.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageSyncPolicyTest {
    @Test
    fun `only recipes and techniques are offered for retry until Axcis completes backend`() {
        val result = storagePendingBreakdown(
            beans = 1,
            recipes = 2,
            techniques = 3,
            grinders = 4,
            equipment = 5,
            cups = 6,
            tastings = 7,
            experiments = 8
        )

        assertEquals(5, result.retryableNow)
        assertEquals(31, result.awaitingBackend)
    }

    @Test
    fun `empty local store never invents pending work`() {
        assertEquals(
            StoragePendingBreakdown(retryableNow = 0, awaitingBackend = 0),
            storagePendingBreakdown(0, 0, 0, 0, 0, 0, 0, 0)
        )
    }
}
