package com.example.ui.viewmodel

import com.example.data.database.Technique
import com.example.data.database.TechniqueStep
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
            ownerDisplayName = "Propietaria anterior",
            visibility = "PUBLIC",
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

        val copy = source.forkedCopy("copy-technique", "current-owner", "Emiliano", "2026-09-16T00:00:00Z")

        assertEquals("copy-technique", copy.id)
        assertEquals("Copia de V60 comunitaria", copy.name)
        assertEquals("current-owner", copy.ownerUserId)
        assertEquals("Emiliano", copy.ownerDisplayName)
        assertEquals("PRIVATE", copy.visibility)
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

    @Test
    fun `forked technique step keeps brewing data and resets remote identity`() {
        val source = TechniqueStep(
            id = "source-step",
            techniqueId = "source-technique",
            stepNumber = 4,
            title = "Vertido final",
            durationSeconds = 55,
            waterAddedMl = 90,
            waterAccumulatedMl = 240,
            targetWaterMl = null,
            remoteId = "remote-step",
            syncStatus = "SYNCED",
            serverVersion = 7,
            expectedVersion = 7,
            lastSyncedAt = "2026-09-01T00:00:00Z"
        )

        val copy = source.forkedStepCopy(
            copyId = "copy-step",
            targetTechniqueId = "copy-technique",
            targetStepNumber = 2,
            timestamp = "2026-09-27T00:00:00Z"
        )

        assertEquals("copy-step", copy.id)
        assertEquals("copy-technique", copy.techniqueId)
        assertEquals(2, copy.stepNumber)
        assertEquals("Vertido final", copy.title)
        assertEquals(55, copy.durationSeconds)
        assertEquals(90, copy.waterAddedMl)
        assertEquals(240, copy.waterAccumulatedMl)
        assertEquals(240, copy.targetWaterMl)
        assertNull(copy.remoteId)
        assertEquals("PENDING_CREATE", copy.syncStatus)
        assertEquals(1L, copy.serverVersion)
        assertEquals(1L, copy.expectedVersion)
        assertNull(copy.lastSyncedAt)
    }
}
