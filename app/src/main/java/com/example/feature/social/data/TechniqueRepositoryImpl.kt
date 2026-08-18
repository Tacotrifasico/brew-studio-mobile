package com.example.feature.social.data

import com.example.data.database.Technique as RoomTechnique
import com.example.data.database.TechniqueStep as RoomStep
import com.example.data.database.TechniqueDao
import com.example.data.database.TechniqueStepDao
import com.example.domain.model.*
import com.example.domain.repository.TechniqueRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TechniqueRepositoryImpl(
    private val techniqueDao: TechniqueDao,
    private val stepDao: TechniqueStepDao
) : TechniqueRepository {

    override fun observeMine(): Flow<List<PreparationTechnique>> {
        return techniqueDao.getAllTechniques().map { roomTechs ->
            roomTechs.map { roomToDomainSync(it) }
        }
    }

    override suspend fun get(id: String): PreparationTechnique? {
        val roomTech = techniqueDao.getTechniqueById(id) ?: return null
        return roomToDomainSync(roomTech)
    }

    override suspend fun save(technique: PreparationTechnique) {
        val roomTech = RoomTechnique(
            id = technique.id,
            name = technique.name,
            methodId = technique.methodId ?: "11111111-1111-4000-8000-000000000001",
            doseG = (technique.coffeeGrams ?: 15.0).toFloat(),
            waterMl = (technique.waterMl ?: 240.0).toInt(),
            ratio = if ((technique.coffeeGrams ?: 0.0) > 0.0) ((technique.waterMl ?: 240.0) / (technique.coffeeGrams ?: 15.0)).toFloat() else 16f,
            temperatureC = (technique.temperatureC ?: 93.0).toInt(),
            executionMode = technique.executionMode,
            grindValue = technique.grind ?: 18.0,
            grindDescription = technique.grindDescription ?: "18 Clicks",
            notes = technique.notes ?: "",
            totalTimeSeconds = technique.totalTimeSeconds ?: 180,
            ownerUserId = technique.ownerUserId,
            originalAuthorUserId = technique.originalAuthorUserId ?: technique.attribution?.originalAuthorUserId,
            originalAuthorName = technique.originalAuthorName ?: technique.attribution?.originalAuthorName,
            originalEntityId = technique.originalEntityId ?: technique.attribution?.originalEntityId,
            importedFromShareId = technique.attribution?.importedFromShareId ?: technique.socialSource?.shareId,
            copyMode = technique.attribution?.mode?.name ?: technique.socialSource?.copyMode?.name ?: "ORIGINAL"
        )
        techniqueDao.insertTechnique(roomTech)

        if (technique.executionSteps.isNotEmpty()) {
            val roomSteps = technique.executionSteps.mapIndexed { idx, st ->
                RoomStep(
                    id = st.id,
                    techniqueId = technique.id,
                    stepNumber = st.stepNumber,
                    title = st.title,
                    durationSeconds = st.durationSeconds,
                    waterAddedMl = st.waterAddedMl,
                    waterAccumulatedMl = st.waterAccumulatedMl,
                    intensity = st.intensity,
                    gesture = st.gesture,
                    stepNote = st.stepNote
                )
            }
            stepDao.insertSteps(roomSteps)
        }
    }

    private suspend fun roomToDomainSync(room: RoomTechnique): PreparationTechnique {
        val steps = stepDao.getStepsForTechniqueSync(room.id).map {
            ExecutionStep(
                id = it.id,
                stepNumber = it.stepNumber,
                title = it.title,
                durationSeconds = it.durationSeconds,
                waterAddedMl = it.waterAddedMl,
                waterAccumulatedMl = it.waterAccumulatedMl,
                intensity = it.intensity,
                gesture = it.gesture,
                stepNote = it.stepNote
            )
        }

        val attrMode = try {
            room.copyMode.let { AttributionMode.valueOf(it) }
        } catch (_: Exception) {
            null
        }

        val attribution = if (room.originalAuthorUserId != null && room.originalAuthorName != null) {
            Attribution(
                required = true,
                mode = attrMode,
                originalAuthorUserId = room.originalAuthorUserId,
                originalAuthorName = room.originalAuthorName,
                originalEntityId = room.originalEntityId ?: room.id,
                importedFromShareId = room.importedFromShareId
            )
        } else null

        return PreparationTechnique(
            id = room.id,
            ownerUserId = room.ownerUserId ?: "local_user",
            name = room.name,
            method = room.legacyMethodName,
            methodId = room.methodId,
            coffeeGrams = room.doseG.toDouble(),
            waterMl = room.waterMl.toDouble(),
            grind = room.grindValue,
            grindDescription = room.grindDescription,
            temperatureC = room.temperatureC.toDouble(),
            totalTimeSeconds = room.totalTimeSeconds,
            executionMode = room.executionMode,
            executionSteps = steps,
            notes = room.notes,
            originalAuthorUserId = room.originalAuthorUserId,
            originalAuthorName = room.originalAuthorName,
            originalEntityId = room.originalEntityId,
            attribution = attribution
        )
    }
}
