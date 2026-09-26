package com.example.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidSyncPolicyTest {
    @Test
    fun `new and partially uploaded techniques can retry`() {
        assertTrue(AndroidSyncPolicy.canRetryTechnique("PENDING_CREATE", null))
        assertTrue(AndroidSyncPolicy.canRetryTechnique("ERROR", null))
        assertTrue(AndroidSyncPolicy.canRetryTechnique("ERROR", "remote-technique"))
    }

    @Test
    fun `remote edits deletes and conflicts wait for Axcis endpoints`() {
        assertTrue(AndroidSyncPolicy.techniqueAwaitsBackend("PENDING_UPDATE", "remote-technique"))
        assertTrue(AndroidSyncPolicy.techniqueAwaitsBackend("PENDING_DELETE", "remote-technique"))
        assertTrue(AndroidSyncPolicy.techniqueAwaitsBackend("CONFLICT", "remote-technique"))
        assertFalse(AndroidSyncPolicy.canRetryTechnique("SYNCED", "remote-technique"))
    }
}
