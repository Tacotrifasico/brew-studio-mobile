package com.example.data.repository

import com.example.data.database.Recipe
import com.example.data.database.RecipeDao
import com.example.data.database.RecipeIngredientDao
import com.example.data.database.RecipeStepDao
import com.example.data.database.BrewMethodDao
import com.example.data.database.Technique
import com.example.data.database.TechniqueDao
import com.example.data.database.TechniqueStep
import com.example.data.database.TechniqueStepDao
import com.example.data.remote.SocialRemoteDataSource
import com.example.data.remote.models.RemoteInboxItem
import com.example.data.remote.models.RemoteShare
import com.example.data.remote.models.RemoteActivityLog
import java.util.UUID

class SocialRepository(
    private val remoteSource: SocialRemoteDataSource,
    private val authRepo: AuthRepository,
    private val recipeDao: RecipeDao,
    private val recipeIngredientDao: RecipeIngredientDao,
    private val recipeStepDao: RecipeStepDao,
    private val brewMethodDao: BrewMethodDao,
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
        val publication = SocialPublicationPolicy.validate(localRecipe.remoteId, visibility, targetUserId).getOrElse { return Result.failure(it) }
        val userName = authRepo.getCachedDisplayName()
        val userHandle = authRepo.getCachedHandle()
        val timestamp = com.example.data.database.currentIso8601()
        val ingredients = recipeIngredientDao.getIngredientsForRecipeSync(localRecipe.id)
        val steps = recipeStepDao.getStepsForRecipeSync(localRecipe.id)
        val methodName = localRecipe.suggestedMethodId?.let { brewMethodDao.getMethodById(it)?.nameKey }
            ?: localRecipe.legacyMethodName.orEmpty()
        val payloadMap = SocialSnapshotFactory.recipe(localRecipe, ingredients, steps, methodName)
            .getOrElse { return Result.failure(it) }

        val remoteShare = RemoteShare(
            id = UUID.randomUUID().toString(),
            entityType = "recipe",
            entityId = publication.entityId,
            fromUserId = uid,
            fromName = userName,
            fromHandle = userHandle,
            targetUserId = publication.targetUserId,
            visibility = publication.visibility,
            name = localRecipe.name,
            subtitle = "Receta de café",
            message = message,
            payloadSnapshotJson = payloadMap,
            originalAuthorUserId = localRecipe.originalAuthorUserId ?: uid,
            originalAuthorName = localRecipe.originalAuthorName ?: userName,
            originalEntityId = localRecipe.originalEntityId ?: publication.entityId,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val result = remoteSource.shareEntity(remoteShare)
        if (result.isSuccess) {
            recipeDao.insertRecipe(localRecipe.copy(
                isShared = true,
                visibility = publication.visibility
            ))
        }
        return result
    }

    suspend fun shareTechnique(localTech: Technique, steps: List<TechniqueStep>, message: String, targetUserId: String? = null, visibility: String = "PUBLIC"): Result<RemoteShare> {
        val uid = authRepo.getUserId() ?: return Result.failure(Exception("Inicie sesión para compartir"))
        val publication = SocialPublicationPolicy.validate(localTech.remoteId, visibility, targetUserId).getOrElse { return Result.failure(it) }
        val userName = authRepo.getCachedDisplayName()
        val userHandle = authRepo.getCachedHandle()
        val timestamp = com.example.data.database.currentIso8601()
        val methodName = brewMethodDao.getMethodById(localTech.methodId)?.nameKey
            ?: localTech.legacyMethodName.orEmpty()
        val payloadMap = SocialSnapshotFactory.technique(localTech, steps, methodName)
            .getOrElse { return Result.failure(it) }

        val remoteShare = RemoteShare(
            id = UUID.randomUUID().toString(),
            entityType = "technique",
            entityId = publication.entityId,
            fromUserId = uid,
            fromName = userName,
            fromHandle = userHandle,
            targetUserId = publication.targetUserId,
            visibility = publication.visibility,
            name = localTech.name,
            subtitle = "Técnica de preparación",
            message = message,
            payloadSnapshotJson = payloadMap,
            originalAuthorUserId = localTech.originalAuthorUserId ?: uid,
            originalAuthorName = localTech.originalAuthorName ?: userName,
            originalEntityId = localTech.originalEntityId ?: publication.entityId,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val result = remoteSource.shareEntity(remoteShare)
        if (result.isSuccess) {
            techniqueDao.insertTechnique(localTech.copy(
                isShared = true,
                visibility = publication.visibility
            ))
        }
        return result
    }

    suspend fun syncImportedShare(share: RemoteShare, localEntityId: String): Result<String> {
        val uid = authRepo.getUserId() ?: return Result.failure(Exception("Inicie sesión para importar"))
        val isRecipe = share.entityType.equals("recipe", ignoreCase = true)
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
        val isRecipe = share.entityType.equals("recipe", ignoreCase = true)
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
