package com.example.domain.usecase

import com.example.domain.model.*
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.RecipeRepository
import com.example.domain.repository.SocialRepository

class ShareRecipeUseCase(
    private val authRepository: AuthRepository,
    private val recipeRepository: RecipeRepository,
    private val socialRepository: SocialRepository
) {
    suspend operator fun invoke(
        recipeId: String,
        destination: ShareDestination,
        message: String?
    ): Result<BrewShare> {
        val currentUser = authRepository.getCurrentUserSync()
            ?: return Result.failure(IllegalStateException("Debe estar autenticado para compartir"))

        val recipe = recipeRepository.get(recipeId)
            ?: return Result.failure(IllegalArgumentException("Receta no encontrada"))

        if (recipe.ownerUserId != currentUser.id) {
            return Result.failure(IllegalStateException("No puede publicar como propia una receta de otro usuario"))
        }

        val origAuthorId = recipe.originalAuthorUserId ?: currentUser.id
        val origAuthorName = recipe.originalAuthorName ?: currentUser.displayName
        val origEntityId = recipe.originalEntityId ?: recipe.id

        val attribution = Attribution(
            required = true,
            mode = null,
            originalAuthorUserId = origAuthorId,
            originalAuthorName = origAuthorName,
            originalEntityId = origEntityId
        )

        val metadataList = mutableListOf<String>().apply {
            if (recipe.ingredients.isNotEmpty()) add("${recipe.ingredients.size} ingredientes")
            if (recipe.steps.isNotEmpty()) add("${recipe.steps.size} pasos")
        }

        val targetUserId = if (destination is ShareDestination.Direct) destination.userId else null
        val visibility = if (destination is ShareDestination.Direct) ShareVisibility.DIRECT else ShareVisibility.PUBLIC

        val share = BrewShare(
            entityType = ShareEntityType.RECIPE,
            entityId = recipe.id,
            name = recipe.name,
            subtitle = recipe.method ?: "Receta de café",
            metadata = metadataList,
            fromUserId = currentUser.id,
            fromDisplayName = currentUser.displayName,
            fromHandle = currentUser.handle,
            targetUserId = targetUserId,
            visibility = visibility,
            message = message,
            createdAt = System.currentTimeMillis(),
            attribution = attribution,
            payload = SharedPayload.RecipePayload(recipe)
        )

        return socialRepository.publish(share)
    }
}
