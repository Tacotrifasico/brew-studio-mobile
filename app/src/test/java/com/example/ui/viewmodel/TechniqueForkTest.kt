package com.example.ui.viewmodel

import com.example.data.database.Technique
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TechniqueForkTest {
    @Test
    fun `local technique fork preserves attribution and resets sync identity`() {
        val source = Technique(
            id = "source-technique",
            name = "V60 comunitaria",
            methodId = "method-v60",
            ownerUserId = "original-owner",
            isShared = true,
            originalAuthorUserId = "original-author",
            originalAuthorName = "Ana",
            originalEntityId = "original-technique",
            rootEntityId = "root-technique",
            importedFromShareId = "share-123",
            copyMode = "IMPORT",
            remoteId = "remote-source",
            syncStatus = "SYNCED",
            serverVersion = 8,
            expectedVersion = 8,
            lastSyncedAt = "2026-09-01T00:00:00Z"
        )

        val copy = source.forkedCopy("copy-technique", "current-owner", "2026-09-16T00:00:00Z")

        assertEquals("copy-technique", copy.id)
        assertEquals("Copia de V60 comunitaria", copy.name)
        assertEquals("current-owner", copy.ownerUserId)
        assertFalse(copy.isShared)
        assertEquals("original-author", copy.originalAuthorUserId)
        assertEquals("original-technique", copy.originalEntityId)
        assertEquals("root-technique", copy.rootEntityId)
        assertEquals("share-123", copy.importedFromShareId)
        assertEquals("FORK", copy.copyMode)
        assertNull(copy.remoteId)
        assertEquals("PENDING_CREATE", copy.syncStatus)
        assertEquals(1L, copy.serverVersion)
        assertEquals(1L, copy.expectedVersion)
        assertNull(copy.lastSyncedAt)
    }
}
