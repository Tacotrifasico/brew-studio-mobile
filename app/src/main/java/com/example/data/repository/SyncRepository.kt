package com.example.data.repository

import android.util.Log
import com.example.data.database.*
import com.example.data.remote.RecipeRemoteDataSource
import com.example.data.remote.SupabaseClientProvider
import com.example.data.remote.TechniqueRemoteDataSource
import com.example.data.remote.models.RemoteRecipe
import com.example.data.remote.models.RemoteTechnique
import com.example.data.remote.models.RemoteTechniqueStep
import kotlinx.coroutines.flow.first

class SyncRepository(
    private val authRepo: AuthRepository,
    private val recipeDao: RecipeDao,
    private val techniqueDao: TechniqueDao,
    private val techniqueStepDao: TechniqueStepDao,
    private val beanDao: BeanDao,
    private val recipeRemoteSource: RecipeRemoteDataSource,
    private val techniqueRemoteSource: TechniqueRemoteDataSource
) {
    private val TAG = "SyncRepository"

    suspend fun synchronizeAll(): Result<String> {
        if (!SupabaseClientProvider.isConfigured) {
            return Result.failure(Exception("Supabase no configurado o sin variables de entorno"))
        }
        val uid = authRepo.getUserId() ?: return Result.failure(Exception("Inicie sesión para sincronizar"))

        try {
            // 1. Synchronize recipes to remote
            val unsyncedRecipes = recipeDao.getAllRecipes().first().filter { 
                it.syncStatus != "SYNCED" || it.remoteId == null 
            }
            var recipesPushed = 0
            for (localRecipe in unsyncedRecipes) {
                val remoteModel = RemoteRecipe(
                    id = localRecipe.remoteId,
                    userId = uid,
                    ownerUserId = localRecipe.ownerUserId ?: uid,
                    ownerDisplayName = localRecipe.ownerDisplayName ?: authRepo.getCachedDisplayName(),
                    name = localRecipe.name,
                    method = localRecipe.suggestedMethodId,
                    beanId = null,
                    grinderId = null,
                    techniqueId = null,
                    coffeeGrams = 15f,
                    waterMl = 240,
                    ratio = 16f,
                    temperature = 93,
                    clicks = "18",
                    notes = localRecipe.intention,
                    visibility = localRecipe.visibility,
                    isShared = localRecipe.isShared,
                    originalAuthorUserId = localRecipe.originalAuthorUserId,
                    originalAuthorName = localRecipe.originalAuthorName,
                    originalEntityId = localRecipe.originalEntityId,
                    importedFromShareId = localRecipe.importedFromShareId,
                    copyMode = localRecipe.copyMode
                )

                val result = recipeRemoteSource.insertRecipe(remoteModel)
                if (result.isSuccess) {
                    val savedRemote = result.getOrThrow()
                    recipeDao.insertRecipe(localRecipe.copy(
                        remoteId = savedRemote.id,
                        syncStatus = "SYNCED",
                        lastSyncedAt = com.example.data.database.currentIso8601()
                    ))
                    recipesPushed++
                }
            }

            // 2. Synchronize techniques to remote
            val unsyncedTechs = techniqueDao.getAllTechniques().first().filter { 
                it.syncStatus != "SYNCED" || it.remoteId == null 
            }
            var techniquesPushed = 0
            for (localTech in unsyncedTechs) {
                val remoteModel = RemoteTechnique(
                    id = localTech.remoteId,
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
                if (result.isSuccess) {
                    val savedRemote = result.getOrThrow()
                    val remoteTechId = savedRemote.id!!

                    val localSteps = techniqueStepDao.getStepsForTechniqueSync(localTech.id)
                    val remoteSteps = localSteps.map { step ->
                        RemoteTechniqueStep(
                            id = step.remoteId,
                            techniqueId = remoteTechId,
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
                    if (stepsResult.isSuccess) {
                        val savedRemoteSteps = stepsResult.getOrThrow()
                        localSteps.forEachIndexed { index, step ->
                            val parsedId = savedRemoteSteps.getOrNull(index)?.id
                            techniqueStepDao.insertStep(step.copy(
                                remoteId = parsedId,
                                syncStatus = "SYNCED"
                            ))
                        }
                    }

                    techniqueDao.insertTechnique(localTech.copy(
                        remoteId = remoteTechId,
                        syncStatus = "SYNCED",
                        lastSyncedAt = com.example.data.database.currentIso8601()
                    ))
                    techniquesPushed++
                }
            }

            // 3. Pull cloud recipes down to offline cache (Room)
            val pullRecipesRes = recipeRemoteSource.getRecipes(uid)
            var recipesPulled = 0
            if (pullRecipesRes.isSuccess) {
                val remoteRecipes = pullRecipesRes.getOrThrow()
                val localRecipes = recipeDao.getAllRecipes().first()

                for (remote in remoteRecipes) {
                    val matchedLocal = localRecipes.find { it.remoteId == remote.id }
                    if (matchedLocal == null) {
                        val newLocal = Recipe(
                            name = remote.name,
                            recipeKind = "BLACK_COFFEE",
                            intention = remote.notes ?: "",
                            suggestedMethodId = remote.method,
                            ingredientsSummary = "",
                            stepsSummary = "",
                            tags = "",
                            ownerUserId = remote.ownerUserId,
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
                        recipeDao.insertRecipe(newLocal)
                        recipesPulled++
                    }
                }
            }

            // 4. Pull cloud techniques down to offline cache (Room)
            val pullTechsRes = techniqueRemoteSource.getTechniques(uid)
            var techniquesPulled = 0
            if (pullTechsRes.isSuccess) {
                val remoteTechs = pullTechsRes.getOrThrow()
                val localTechs = techniqueDao.getAllTechniques().first()
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
                            ownerUserId = remote.ownerUserId,
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
                        }
                        techniquesPulled++
                    }
                }
            }

            return Result.success("Sincronización completa: $recipesPushed subidas, $techniquesPushed técnicas subidas. $recipesPulled recetas descargadas, $techniquesPulled técnicas descargadas.")
        } catch (e: Exception) {
            Log.e(TAG, "Uncaught error during synchronization", e)
            return Result.failure(e)
        }
    }
}
