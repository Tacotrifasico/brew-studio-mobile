package com.example.data.repository

import android.util.Log
import com.example.data.database.*
import com.example.data.remote.SupabaseClientProvider
import com.example.data.remote.TechniqueRemoteDataSource
import com.example.data.remote.models.RemoteTechnique
import com.example.data.remote.models.RemoteTechniqueStep
import com.example.data.validation.OwnerScopeRules
import kotlinx.coroutines.flow.first

class SyncRepository(
    private val authRepo: AuthRepository,
    private val recipeDao: RecipeDao,
    private val techniqueDao: TechniqueDao,
    private val techniqueStepDao: TechniqueStepDao,
    private val techniqueRemoteSource: TechniqueRemoteDataSource
) {
    private val TAG = "SyncRepository"

    suspend fun synchronizeAll(): Result<String> {
        if (!SupabaseClientProvider.isConfigured) {
            return Result.failure(Exception("Supabase no configurado o sin variables de entorno"))
        }
        val uid = authRepo.getUserId() ?: return Result.failure(Exception("Inicie sesión para sincronizar"))

        try {
            val syncErrors = mutableListOf<String>()

            // AXCIS-ONLINE P0: el contrato remoto heredado de recetas sólo
            // acepta una extracción simple y no representa ingredientes ni
            // pasos. Mantenerlas locales es más seguro que subir cantidades
            // inventadas y declararlas sincronizadas.
            val recipesAwaitingBackend = recipeDao.getAllRecipes().first().count {
                OwnerScopeRules.canSync(it.ownerUserId, uid) && (it.syncStatus != "SYNCED" || it.remoteId == null)
            }

            // 1. Synchronize techniques to remote
            val pendingTechs = techniqueDao.getAllTechniques().first().filter {
                OwnerScopeRules.canSync(it.ownerUserId, uid) && (it.syncStatus != "SYNCED" || it.remoteId == null)
            }
            val techniquesAwaitingBackend = pendingTechs.count {
                AndroidSyncPolicy.techniqueAwaitsBackend(it.syncStatus, it.remoteId)
            }
            val retryableTechs = pendingTechs.filter {
                AndroidSyncPolicy.canRetryTechnique(it.syncStatus, it.remoteId)
            }
            var techniquesPushed = 0
            for (localTech in retryableTechs) {
                var remoteTechId = localTech.remoteId
                if (remoteTechId == null) {
                    val remoteModel = RemoteTechnique(
                        id = null,
                        userId = uid,
                        ownerUserId = localTech.ownerUserId ?: uid,
                        ownerDisplayName = localTech.ownerDisplayName ?: authRepo.getCachedDisplayName(),
                        name = localTech.name,
                        method = localTech.methodId,
                        coffeeGrams = localTech.doseG,
                        waterMl = localTech.waterMl,
                        ratio = localTech.ratio,
                        temperature = localTech.temperatureC,
                        grindClicks = (localTech.grindValue ?: 18.0).toString(),
                        grinderId = localTech.grinderId,
                        beanId = localTech.beanId,
                        notes = localTech.notes,
                        visibility = localTech.visibility,
                        isShared = localTech.isShared,
                        originalAuthorUserId = localTech.originalAuthorUserId,
                        originalAuthorName = localTech.originalAuthorName,
                        originalEntityId = localTech.originalEntityId,
                        importedFromShareId = localTech.importedFromShareId,
                        copyMode = localTech.copyMode
                    )
                    val result = techniqueRemoteSource.insertTechnique(remoteModel)
                    remoteTechId = result.getOrNull()?.id
                    if (remoteTechId == null) {
                        techniqueDao.insertTechnique(localTech.copy(syncStatus = "ERROR"))
                        syncErrors += "No se pudo subir la técnica \"${localTech.name}\""
                        continue
                    }
                }

                val resolvedRemoteTechId = requireNotNull(remoteTechId)
                val localSteps = techniqueStepDao.getStepsForTechniqueSync(localTech.id)
                val pendingSteps = localSteps.filter { it.remoteId == null || it.syncStatus != "SYNCED" }
                val remoteSteps = pendingSteps.map { step ->
                    RemoteTechniqueStep(
                        id = step.remoteId,
                        techniqueId = resolvedRemoteTechId,
                        userId = uid,
                        stepOrder = step.stepNumber,
                        title = step.title,
                        durationSec = step.durationSeconds,
                        waterAddMl = step.waterAddedMl,
                        targetWaterMl = step.waterAccumulatedMl,
                        gesture = step.gesture,
                        intensity = step.intensity,
                        note = step.stepNote
                    )
                }

                val stepsResult = techniqueRemoteSource.insertTechniqueSteps(remoteSteps)
                val allStepsSaved = if (stepsResult.isSuccess) {
                    val savedRemoteSteps = stepsResult.getOrThrow()
                    pendingSteps.forEachIndexed { index, step ->
                        val parsedId = savedRemoteSteps.getOrNull(index)?.id
                        techniqueStepDao.insertStep(step.copy(
                            remoteId = parsedId,
                            syncStatus = if (parsedId != null) "SYNCED" else "ERROR"
                        ))
                    }
                    savedRemoteSteps.size == pendingSteps.size && savedRemoteSteps.all { it.id != null }
                } else {
                    pendingSteps.forEach { step ->
                        techniqueStepDao.insertStep(step.copy(syncStatus = "ERROR"))
                    }
                    false
                }

                techniqueDao.insertTechnique(localTech.copy(
                    remoteId = resolvedRemoteTechId,
                    ownerUserId = localTech.ownerUserId ?: uid,
                    syncStatus = if (allStepsSaved) "SYNCED" else "ERROR",
                    lastSyncedAt = if (allStepsSaved) com.example.data.database.currentIso8601() else localTech.lastSyncedAt
                ))
                if (allStepsSaved) {
                    techniquesPushed++
                } else {
                    syncErrors += "La técnica \"${localTech.name}\" se subió, pero sus pasos quedaron pendientes"
                }
            }

            // 2. Pull cloud techniques down to offline cache (Room)
            val pullTechsRes = techniqueRemoteSource.getTechniques(uid)
            var techniquesPulled = 0
            if (pullTechsRes.isSuccess) {
                val remoteTechs = pullTechsRes.getOrThrow()
                val localTechs = techniqueDao.getAllTechniques().first().filter { OwnerScopeRules.isVisible(it.ownerUserId, uid) }
                val defaultMethodUuid = "11111111-1111-4000-8000-000000000001"

                for (remote in remoteTechs) {
                    val matchedLocal = localTechs.find { it.remoteId == remote.id }
                    if (matchedLocal == null && remote.id != null) {
                        val newLocal = Technique(
                            name = remote.name,
                            methodId = remote.method ?: defaultMethodUuid,
                            doseG = remote.coffeeGrams ?: 15f,
                            waterMl = remote.waterMl ?: 240,
                            ratio = remote.ratio ?: 16f,
                            temperatureC = remote.temperature ?: 93,
                            grindValue = remote.grindClicks?.toDoubleOrNull() ?: 18.0,
                            grindDescription = remote.grindClicks?.let { "$it Clicks" } ?: "18 Clicks",
                            notes = remote.notes ?: "",
                            totalTimeSeconds = 180,
                            ownerUserId = remote.ownerUserId ?: remote.userId ?: uid,
                            ownerDisplayName = remote.ownerDisplayName,
                            visibility = remote.visibility ?: "PRIVATE",
                            isShared = remote.isShared ?: false,
                            originalAuthorUserId = remote.originalAuthorUserId,
                            originalAuthorName = remote.originalAuthorName,
                            originalEntityId = remote.originalEntityId,
                            importedFromShareId = remote.importedFromShareId,
                            copyMode = remote.copyMode ?: "ORIGINAL",
                            remoteId = remote.id,
                            syncStatus = "SYNCED"
                        )
                        techniqueDao.insertTechnique(newLocal)

                        val stepsRes = techniqueRemoteSource.getTechniqueSteps(remote.id)
                        if (stepsRes.isSuccess) {
                            val remoteSteps = stepsRes.getOrThrow()
                            val localMappedSteps = remoteSteps.map { remoteStep ->
                                TechniqueStep(
                                    techniqueId = newLocal.id,
                                    stepNumber = remoteStep.stepOrder,
                                    title = remoteStep.title ?: "Paso",
                                    durationSeconds = remoteStep.durationSec ?: 30,
                                    waterAddedMl = remoteStep.waterAddMl ?: 50,
                                    waterAccumulatedMl = remoteStep.targetWaterMl ?: 50,
                                    intensity = remoteStep.intensity ?: "MEDIUM",
                                    gesture = remoteStep.gesture ?: "CIRCULAR_POUR",
                                    stepNote = remoteStep.note ?: "",
                                    remoteId = remoteStep.id,
                                    syncStatus = "SYNCED"
                                )
                            }
                            techniqueStepDao.insertSteps(localMappedSteps)
                        } else {
                            syncErrors += "No se pudieron descargar los pasos de \"${remote.name}\""
                        }
                        techniquesPulled++
                    }
                }
            } else {
                syncErrors += "No se pudieron descargar las técnicas"
            }

            val backendPending = recipesAwaitingBackend + techniquesAwaitingBackend
            val backendNotice = if (backendPending == 0) "" else
                " $backendPending ${if (backendPending == 1) "elemento permanece" else "elementos permanecen"} seguro${if (backendPending == 1) "" else "s"} en este dispositivo hasta completar su backend."
            val pushedLabel = if (techniquesPushed == 1) "1 técnica sincronizada" else "$techniquesPushed técnicas sincronizadas"
            val pulledLabel = if (techniquesPulled == 1) "1 descargada" else "$techniquesPulled descargadas"
            val summary = "$pushedLabel y $pulledLabel.$backendNotice"
            return if (syncErrors.isEmpty()) {
                Result.success("Sincronización de técnicas completa: $summary")
            } else {
                Result.failure(Exception("Sincronización parcial: $summary ${syncErrors.joinToString(". ")}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Uncaught error during synchronization", e)
            return Result.failure(e)
        }
    }
}
