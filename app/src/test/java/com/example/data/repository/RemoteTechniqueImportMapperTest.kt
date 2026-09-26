package com.example.data.repository

import com.example.data.remote.models.RemoteTechnique
import com.example.data.remote.models.RemoteTechniqueStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class RemoteTechniqueImportMapperTest {
    @Test
    fun `complete consistent remote technique is imported`() {
        val mapped = RemoteTechniqueImportMapper.map(
            remote = remoteTechnique(),
            remoteSteps = listOf(
                remoteStep(order = 2, title = "Vertido", duration = 90, added = 190, target = 240),
                remoteStep(order = 1, title = "Preinfusión", duration = 30, added = 50, target = 50)
            ),
            localId = "local-technique",
            ownerFallback = "owner"
        )

        assertNotNull(mapped)
        assertEquals(240, mapped!!.technique.waterMl)
        assertEquals(16f, mapped.technique.ratio, 0.001f)
        assertEquals(120, mapped.technique.totalTimeSeconds)
        assertEquals(listOf(50, 240), mapped.steps.map { it.waterAccumulatedMl })
        assertEquals(listOf(1, 2), mapped.steps.map { it.stepNumber })
    }

    @Test
    fun `remote technique without executable steps is rejected`() {
        assertNull(RemoteTechniqueImportMapper.map(remoteTechnique(), emptyList(), ownerFallback = "owner"))
        assertNull(
            RemoteTechniqueImportMapper.map(
                remoteTechnique(),
                listOf(remoteStep(order = 1, title = "", duration = 0, added = 240, target = 240)),
                ownerFallback = "owner"
            )
        )
    }

    @Test
    fun `remote technique with contradictory totals is rejected`() {
        val validSteps = listOf(
            remoteStep(order = 1, title = "Preinfusión", duration = 30, added = 50, target = 50),
            remoteStep(order = 2, title = "Vertido", duration = 90, added = 190, target = 240)
        )
        assertNull(RemoteTechniqueImportMapper.map(remoteTechnique(waterMl = 999), validSteps, ownerFallback = "owner"))
        assertNull(RemoteTechniqueImportMapper.map(remoteTechnique(ratio = 30f), validSteps, ownerFallback = "owner"))
        assertNull(
            RemoteTechniqueImportMapper.map(
                remoteTechnique(),
                validSteps.mapIndexed { index, step -> if (index == 1) step.copy(targetWaterMl = 999) else step },
                ownerFallback = "owner"
            )
        )
    }

    @Test
    fun `remote technique with missing or foreign step sequence is rejected`() {
        assertNull(
            RemoteTechniqueImportMapper.map(
                remoteTechnique(),
                listOf(remoteStep(order = 2, title = "Vertido", duration = 90, added = 240, target = 240)),
                ownerFallback = "owner"
            )
        )
        assertNull(
            RemoteTechniqueImportMapper.map(
                remoteTechnique(),
                listOf(remoteStep(order = 1, title = "Vertido", duration = 90, added = 240, target = 240).copy(techniqueId = "otra-técnica")),
                ownerFallback = "owner"
            )
        )
    }

    private fun remoteTechnique(waterMl: Int = 240, ratio: Float = 16f) = RemoteTechnique(
        id = "remote-technique",
        userId = "owner",
        ownerUserId = "owner",
        ownerDisplayName = "Ana",
        name = "V60 dulce",
        method = "11111111-1111-4000-8000-000000000001",
        coffeeGrams = 15f,
        waterMl = waterMl,
        ratio = ratio,
        temperature = 93,
        grindClicks = "24",
        grinderId = null,
        beanId = null,
        notes = "Balanceada",
        visibility = "PRIVATE",
        isShared = false,
        originalAuthorUserId = null,
        originalAuthorName = null,
        originalEntityId = null,
        importedFromShareId = null,
        copyMode = "ORIGINAL",
        createdAt = null,
        updatedAt = null
    )

    private fun remoteStep(order: Int, title: String, duration: Int, added: Int, target: Int) = RemoteTechniqueStep(
        id = "remote-step-$order",
        techniqueId = "remote-technique",
        userId = "owner",
        stepOrder = order,
        title = title,
        durationSec = duration,
        waterAddMl = added,
        targetWaterMl = target,
        gesture = "CIRCULAR_POUR",
        intensity = "MEDIUM",
        note = ""
    )
}
