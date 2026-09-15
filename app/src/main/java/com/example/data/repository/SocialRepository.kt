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

    suspend fun syncImportedShare(share: RemoteShare, localEntityId: String): Result<String> {
        val uid = authRepo.getUserId() ?: return Result.failure(Exception("Inicie sesión para importar"))
        val isRecipe = share.entityType == "recipe"
        val flowResult = remoteSource.importShare(share.id, isRecipe)
        flowResult.getOrNull()?.let { remoteId ->
            if (!markLocalCopySynced(localEntityId, remoteId, isRecipe, uid)) {
                return Result.failure(Exception("La copia remota se creó, pero el registro local no pertenece a la sesión actual"))
            }
        }
        return flowResult
    }

    suspend fun syncForkedShare(share: RemoteShare, localEntityId: String): Result<String> {
        val uid = authRepo.getUserId() ?: return Result.failure(Exception("Inicie sesión para crear una variante"))
        val isRecipe = share.entityType == "recipe"
        val flowResult = remoteSource.forkShare(share.id, isRecipe)
        flowResult.getOrNull()?.let { remoteId ->
            if (!markLocalCopySynced(localEntityId, remoteId, isRecipe, uid)) {
                return Result.failure(Exception("La variante remota se creó, pero el registro local no pertenece a la sesión actual"))
            }
        }
        return flowResult
    }

    private suspend fun markLocalCopySynced(localEntityId: String, remoteId: String, isRecipe: Boolean, uid: String): Boolean {
        if (isRecipe) {
            val local = recipeDao.getRecipeById(localEntityId) ?: return false
            if (local.ownerUserId != uid) return false
            recipeDao.insertRecipe(local.copy(remoteId = remoteId, syncStatus = "SYNCED", lastSyncedAt = com.example.data.database.currentIso8601()))
        } else {
            val local = techniqueDao.getTechniqueById(localEntityId) ?: return false
            if (local.ownerUserId != uid) return false
            techniqueDao.insertTechnique(local.copy(remoteId = remoteId, syncStatus = "SYNCED", lastSyncedAt = com.example.data.database.currentIso8601()))
        }
        return true
    }
}
