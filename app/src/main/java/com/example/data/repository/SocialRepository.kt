package com.example.data.repository

import com.example.data.database.Recipe
import com.example.data.database.RecipeDao
import com.example.data.database.Technique
import com.example.data.database.TechniqueDao
import com.example.data.database.TechniqueStep
import com.example.data.database.TechniqueStepDao
import com.example.data.remote.SocialRemoteDataSource
import com.example.data.remote.models.RemoteInboxItem
import com.example.data.remote.models.RemoteShare
import com.example.data.remote.models.RemoteActivityLog

class SocialRepository(
    private val remoteSource: SocialRemoteDataSource,
    private val authRepo: AuthRepository,
    private val recipeDao: RecipeDao,
    private val techniqueDao: TechniqueDao,
    private val techniqueStepDao: TechniqueStepDao
) {
    suspend fun getFeed(): Result<List<RemoteShare>> {
        return remoteSource.getFeed()
    }

    suspend fun getInbox(): Result<List<RemoteInboxItem>> {
        val uid = authRepo.getUserId() ?: return Result.failure(Exception("Inicie sesión para ver su buzón"))
        return remoteSource.getInbox(uid)
    }

    suspend fun getActivityTimeline(): Result<List<RemoteActivityLog>> {
        val uid = authRepo.getUserId() ?: return Result.success(emptyList())
        return remoteSource.getActivityTimeline(uid)
    }

    suspend fun likeShare(shareId: String): Result<Unit> {
        val uid = authRepo.getUserId() ?: return Result.failure(Exception("Inicie sesión para interactuar"))
        return remoteSource.likeShare(shareId, uid)
    }

    suspend fun unlikeShare(shareId: String): Result<Unit> {
        val uid = authRepo.getUserId() ?: return Result.failure(Exception("Inicie sesión para interactuar"))
        return remoteSource.unlikeShare(shareId, uid)
    }

    suspend fun saveShare(shareId: String): Result<Unit> {
        val uid = authRepo.getUserId() ?: return Result.failure(Exception("Inicie sesión para guardar"))
        return remoteSource.saveShare(shareId, uid)
    }

    suspend fun shareRecipe(localRecipe: Recipe, message: String, targetUserId: String? = null, visibility: String = "PUBLIC"): Result<RemoteShare> {
        val uid = authRepo.getUserId() ?: return Result.failure(Exception("Inicie sesión para compartir"))
        val userName = authRepo.getCachedDisplayName()
        val userHandle = authRepo.getCachedHandle()

        val payloadMap: Map<String, Any> = mapOf(
            "name" to localRecipe.name,
            "recipeKind" to localRecipe.recipeKind,
            "intention" to localRecipe.intention,
            "suggestedMethodId" to (localRecipe.suggestedMethodId ?: ""),
            "ingredientsSummary" to localRecipe.ingredientsSummary,
            "stepsSummary" to localRecipe.stepsSummary,
            "tags" to localRecipe.tags
        )

        val remoteShare = RemoteShare(
            id = "",
            entityType = "recipe",
            entityId = localRecipe.remoteId ?: "00000000-0000-0000-0000-000000000000",
            fromUserId = uid,
            fromName = userName,
            fromHandle = userHandle,
            targetUserId = targetUserId,
            visibility = visibility,
            name = localRecipe.name,
            subtitle = "Receta de café",
            message = message,
            payloadSnapshotJson = payloadMap,
            originalAuthorUserId = localRecipe.originalAuthorUserId ?: uid,
            originalAuthorName = localRecipe.originalAuthorName ?: userName,
            originalEntityId = localRecipe.originalEntityId ?: localRecipe.remoteId ?: "00000000-0000-0000-0000-000000000000",
            createdAt = "",
            updatedAt = ""
        )

        val result = remoteSource.shareEntity(remoteShare)
        if (result.isSuccess) {
            recipeDao.insertRecipe(localRecipe.copy(
                isShared = true,
                visibility = visibility,
                syncStatus = "SYNCED"
            ))
        }
        return result
    }

    suspend fun shareTechnique(localTech: Technique, steps: List<TechniqueStep>, message: String, targetUserId: String? = null, visibility: String = "PUBLIC"): Result<RemoteShare> {
        val uid = authRepo.getUserId() ?: return Result.failure(Exception("Inicie sesión para compartir"))
        val userName = authRepo.getCachedDisplayName()
        val userHandle = authRepo.getCachedHandle()

        val stepsList = steps.map { step ->
            mapOf(
                "step_order" to step.stepNumber,
                "title" to step.title,
                "duration_sec" to step.durationSeconds,
                "water_add_ml" to step.waterAddedMl,
                "target_water_ml" to step.waterAccumulatedMl,
                "gesture" to step.gesture,
                "intensity" to step.intensity,
                "note" to step.stepNote
            )
        }

        val payloadMap = mapOf(
            "name" to localTech.name,
            "methodId" to localTech.methodId,
            "doseG" to localTech.doseG,
            "waterMl" to localTech.waterMl,
            "ratio" to localTech.ratio,
            "temperatureC" to localTech.temperatureC,
            "grindValue" to (localTech.grindValue ?: 18.0),
            "grindDescription" to (localTech.grindDescription ?: ""),
            "notes" to localTech.notes,
            "steps" to stepsList
        )

        val remoteShare = RemoteShare(
            id = "",
            entityType = "technique",
            entityId = localTech.remoteId ?: "00000000-0000-0000-0000-000000000000",
            fromUserId = uid,
            fromName = userName,
            fromHandle = userHandle,
            targetUserId = targetUserId,
            visibility = visibility,
            name = localTech.name,
            subtitle = "Técnica de preparación",
            message = message,
            payloadSnapshotJson = payloadMap,
            originalAuthorUserId = localTech.originalAuthorUserId ?: uid,
            originalAuthorName = localTech.originalAuthorName ?: userName,
            originalEntityId = localTech.originalEntityId ?: localTech.remoteId ?: "00000000-0000-0000-0000-000000000000",
            createdAt = "",
            updatedAt = ""
        )

        val result = remoteSource.shareEntity(remoteShare)
        if (result.isSuccess) {
            techniqueDao.insertTechnique(localTech.copy(
                isShared = true,
                visibility = visibility,
                syncStatus = "SYNCED"
            ))
        }
        return result
    }

    suspend fun importShare(share: RemoteShare): Result<String> {
        val isRecipe = share.entityType == "recipe"
        val flowResult = remoteSource.importShare(share.id, isRecipe)
        if (flowResult.isSuccess) {
            val copyRemoteId = flowResult.getOrThrow()
            val snap = share.payloadSnapshotJson
            if (isRecipe) {
                val r = Recipe(
                    name = snap["name"] as? String ?: share.name,
                    recipeKind = snap["recipeKind"] as? String ?: "BLACK_COFFEE",
                    intention = snap["intention"] as? String ?: "",
                    suggestedMethodId = snap["suggestedMethodId"] as? String,
                    ingredientsSummary = snap["ingredientsSummary"] as? String ?: "",
                    stepsSummary = snap["stepsSummary"] as? String ?: "",
                    tags = snap["tags"] as? String ?: "",
                    ownerUserId = share.fromUserId,
                    ownerDisplayName = share.fromName,
                    visibility = "PRIVATE",
                    isShared = false,
                    originalAuthorUserId = share.originalAuthorUserId ?: share.fromUserId,
                    originalAuthorName = share.originalAuthorName ?: share.fromName,
                    originalEntityId = share.originalEntityId ?: share.entityId,
                    importedFromShareId = share.id,
                    copyMode = "ORIGINAL",
                    remoteId = copyRemoteId,
                    syncStatus = "SYNCED"
                )
                recipeDao.insertRecipe(r)
            } else {
                val defaultMethodUuid = "11111111-1111-4000-8000-000000000001"
                val t = Technique(
                    name = snap["name"] as? String ?: share.name,
                    methodId = snap["methodId"] as? String ?: defaultMethodUuid,
                    doseG = (snap["doseG"] as? Number)?.toFloat() ?: 15f,
                    waterMl = (snap["waterMl"] as? Number)?.toInt() ?: 240,
                    ratio = (snap["ratio"] as? Number)?.toFloat() ?: 16f,
                    temperatureC = (snap["temperatureC"] as? Number)?.toInt() ?: 93,
                    grindValue = (snap["grindValue"] as? Number)?.toDouble() ?: 18.0,
                    grindDescription = snap["grindDescription"] as? String ?: "18 Clicks",
                    notes = snap["notes"] as? String ?: "Técnica importada",
                    totalTimeSeconds = 180,
                    ownerUserId = share.fromUserId,
                    ownerDisplayName = share.fromName,
                    visibility = "PRIVATE",
                    isShared = false,
                    originalAuthorUserId = share.originalAuthorUserId ?: share.fromUserId,
                    originalAuthorName = share.originalAuthorName ?: share.fromName,
                    originalEntityId = share.originalEntityId ?: share.entityId,
                    importedFromShareId = share.id,
                    copyMode = "ORIGINAL",
                    remoteId = copyRemoteId,
                    syncStatus = "SYNCED"
                )
                techniqueDao.insertTechnique(t)
                val localTechId = t.id

                val stepsObj = snap["steps"] as? List<Map<String, Any>> ?: emptyList()
                val techSteps = stepsObj.mapIndexed { idx, item ->
                    TechniqueStep(
                        techniqueId = localTechId,
                        stepNumber = (item["step_order"] as? Number)?.toInt() ?: (idx + 1),
                        title = item["title"] as? String ?: "Paso",
                        durationSeconds = (item["duration_sec"] as? Number)?.toInt() ?: 30,
                        waterAddedMl = (item["water_add_ml"] as? Number)?.toInt() ?: 50,
                        waterAccumulatedMl = (item["target_water_ml"] as? Number)?.toInt() ?: 50,
                        intensity = item["intensity"] as? String ?: "MEDIUM",
                        gesture = item["gesture"] as? String ?: "CIRCULAR_POUR",
                        stepNote = item["note"] as? String ?: "",
                        syncStatus = "SYNCED"
                    )
                }
                techniqueStepDao.insertSteps(techSteps)
            }
        }
        return flowResult
    }

    suspend fun forkShare(share: RemoteShare): Result<String> {
        val uid = authRepo.getUserId() ?: return Result.failure(Exception("Inicie sesión para forquear"))
        val isRecipe = share.entityType == "recipe"

        val flowResult = remoteSource.forkShare(share.id, isRecipe)
        if (flowResult.isSuccess) {
            val forkRemoteId = flowResult.getOrThrow()
            val snap = share.payloadSnapshotJson
            if (isRecipe) {
                val r = Recipe(
                    name = (snap["name"] as? String ?: share.name) + " (Fork)",
                    recipeKind = snap["recipeKind"] as? String ?: "BLACK_COFFEE",
                    intention = snap["intention"] as? String ?: "",
                    suggestedMethodId = snap["suggestedMethodId"] as? String,
                    ingredientsSummary = snap["ingredientsSummary"] as? String ?: "",
                    stepsSummary = snap["stepsSummary"] as? String ?: "",
                    tags = snap["tags"] as? String ?: "",
                    ownerUserId = uid,
                    ownerDisplayName = authRepo.getCachedDisplayName(),
                    visibility = "PRIVATE",
                    isShared = false,
                    originalAuthorUserId = share.originalAuthorUserId ?: share.fromUserId,
                    originalAuthorName = share.originalAuthorName ?: share.fromName,
                    originalEntityId = share.originalEntityId ?: share.entityId,
                    importedFromShareId = share.id,
                    copyMode = "FORK",
                    remoteId = forkRemoteId,
                    syncStatus = "SYNCED"
                )
                recipeDao.insertRecipe(r)
            } else {
                val defaultMethodUuid = "11111111-1111-4000-8000-000000000001"
                val t = Technique(
                    name = (snap["name"] as? String ?: share.name) + " (Fork)",
                    methodId = snap["methodId"] as? String ?: defaultMethodUuid,
                    doseG = (snap["doseG"] as? Number)?.toFloat() ?: 15f,
                    waterMl = (snap["waterMl"] as? Number)?.toInt() ?: 240,
                    ratio = (snap["ratio"] as? Number)?.toFloat() ?: 16f,
                    temperatureC = (snap["temperatureC"] as? Number)?.toInt() ?: 93,
                    grindValue = (snap["grindValue"] as? Number)?.toDouble() ?: 18.0,
                    grindDescription = snap["grindDescription"] as? String ?: "18 Clicks",
                    notes = snap["notes"] as? String ?: "Fork de rutina",
                    totalTimeSeconds = 180,
                    ownerUserId = uid,
                    ownerDisplayName = authRepo.getCachedDisplayName(),
                    visibility = "PRIVATE",
                    isShared = false,
                    originalAuthorUserId = share.originalAuthorUserId ?: share.fromUserId,
                    originalAuthorName = share.originalAuthorName ?: share.fromName,
                    originalEntityId = share.originalEntityId ?: share.entityId,
                    importedFromShareId = share.id,
                    copyMode = "FORK",
                    remoteId = forkRemoteId,
                    syncStatus = "SYNCED"
                )
                techniqueDao.insertTechnique(t)
                val localTechId = t.id

                val stepsObj = snap["steps"] as? List<Map<String, Any>> ?: emptyList()
                val techSteps = stepsObj.mapIndexed { idx, item ->
                    TechniqueStep(
                        techniqueId = localTechId,
                        stepNumber = (item["step_order"] as? Number)?.toInt() ?: (idx + 1),
                        title = item["title"] as? String ?: "Paso",
                        durationSeconds = (item["duration_sec"] as? Number)?.toInt() ?: 30,
                        waterAddedMl = (item["water_add_ml"] as? Number)?.toInt() ?: 50,
                        waterAccumulatedMl = (item["target_water_ml"] as? Number)?.toInt() ?: 50,
                        intensity = item["intensity"] as? String ?: "MEDIUM",
                        gesture = item["gesture"] as? String ?: "CIRCULAR_POUR",
                        stepNote = item["note"] as? String ?: "",
                        syncStatus = "SYNCED"
                    )
                }
                techniqueStepDao.insertSteps(techSteps)
            }
        }
        return flowResult
    }
}
