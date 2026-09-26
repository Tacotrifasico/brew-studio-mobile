package com.example.data.repository

import com.example.data.database.Technique
import com.example.data.database.TechniqueStep
import com.example.data.remote.models.RemoteTechnique
import com.example.data.remote.models.RemoteTechniqueStep
import com.example.data.validation.BrewInputRules
import java.util.UUID

data class ImportedTechniqueAggregate(
    val technique: Technique,
    val steps: List<TechniqueStep>
)

object RemoteTechniqueImportMapper {
    fun map(
        remote: RemoteTechnique,
        remoteSteps: List<RemoteTechniqueStep>,
        localId: String = UUID.randomUUID().toString(),
        ownerFallback: String
    ): ImportedTechniqueAggregate? {
        val remoteId = remote.id ?: return null
        val methodId = remote.method?.takeIf { it.isNotBlank() } ?: return null
        val orderedSteps = remoteSteps.sortedBy { it.stepOrder }
        val normalized = BrewInputRules.normalizeTechnique(
            name = remote.name,
            coffee = remote.coffeeGrams,
            temperature = remote.temperature,
            stepTitles = orderedSteps.map { it.title.orEmpty() },
            stepDurations = orderedSteps.map { it.durationSec },
            stepWaters = orderedSteps.map { it.waterAddMl }
        ) ?: return null

        val technique = Technique(
            id = localId,
            name = remote.name.trim(),
            methodId = methodId,
            doseG = requireNotNull(remote.coffeeGrams),
            waterMl = normalized.waterMl,
            ratio = normalized.ratio,
            temperatureC = requireNotNull(remote.temperature),
            grindValue = remote.grindClicks?.toDoubleOrNull(),
            grindDescription = remote.grindClicks?.takeIf { it.isNotBlank() }?.let { "$it clics" },
            notes = remote.notes.orEmpty(),
            totalTimeSeconds = normalized.totalTimeSeconds,
            ownerUserId = remote.ownerUserId ?: remote.userId ?: ownerFallback,
            ownerDisplayName = remote.ownerDisplayName,
            visibility = remote.visibility ?: "PRIVATE",
            isShared = remote.isShared ?: false,
            originalAuthorUserId = remote.originalAuthorUserId,
            originalAuthorName = remote.originalAuthorName,
            originalEntityId = remote.originalEntityId,
            importedFromShareId = remote.importedFromShareId,
            copyMode = remote.copyMode ?: "ORIGINAL",
            remoteId = remoteId,
            syncStatus = "SYNCED"
        )
        val steps = orderedSteps.mapIndexed { index, remoteStep ->
            TechniqueStep(
                techniqueId = localId,
                stepNumber = index + 1,
                title = requireNotNull(remoteStep.title).trim(),
                durationSeconds = requireNotNull(remoteStep.durationSec),
                waterAddedMl = requireNotNull(remoteStep.waterAddMl),
                waterAccumulatedMl = normalized.accumulatedWaterMl[index],
                intensity = remoteStep.intensity ?: "MEDIUM",
                gesture = remoteStep.gesture ?: "CIRCULAR_POUR",
                stepNote = remoteStep.note.orEmpty(),
                remoteId = remoteStep.id,
                syncStatus = "SYNCED"
            )
        }
        return ImportedTechniqueAggregate(technique, steps)
    }
}
