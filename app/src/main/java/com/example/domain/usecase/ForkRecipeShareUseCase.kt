package com.example.domain.usecase

import com.example.domain.model.*
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.RecipeRepository
import com.example.domain.repository.SocialRepository
import java.util.UUID

class ForkRecipeShareUseCase(
    private val authRepository: AuthRepository,
    private val recipeRepository: RecipeRepository,
    private val socialRepository: SocialRepository
) {
    suspend operator fun invoke(shareId: String): Result<DomainRecipe> {
        val currentUser = authRepository.getCurrentUserSync()
            ?: return Result.failure(IllegalStateException("Debe estar autenticado para crear un fork"))

        val share = socialRepository.getShare(shareId)
            ?: return Result.failure(IllegalArgumentException("Publicación no encontrada"))

        if (share.entityType != ShareEntityType.RECIPE) {
            return Result.failure(IllegalArgumentException("La publicación no es una receta"))
        }

        val payload = share.payload as? SharedPayload.RecipePayload
            ?: return Result.failure(IllegalStateException("Payload de receta no válido"))

        val sourceRecipe = payload.recipe

        val origAuthorId = sourceRecipe.originalAuthorUserId ?: share.attribution.originalAuthorUserId
        val origAuthorName = sourceRecipe.originalAuthorName ?: share.attribution.originalAuthorName
        val origEntityId = sourceRecipe.originalEntityId ?: share.attribution.originalEntityId

        val newRecipeId = UUID.randomUUID().toString()
        val forkedName = "${sourceRecipe.name} · fork"

        val forkedRecipe = sourceRecipe.copy(
            id = newRecipeId,
            ownerUserId = currentUser.id,
            name = forkedName,
            ingredients = sourceRecipe.ingredients.map { it.copy(id = UUID.randomUUID().toString()) },
            steps = sourceRecipe.steps.map { it.copy(id = UUID.randomUUID().toString()) },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            originalAuthorUserId = origAuthorId,
            originalAuthorName = origAuthorName,
            originalEntityId = origEntityId,
            attribution = Attribution(
                required = true,
                mode = AttributionMode.FORK,
                originalAuthorUserId = origAuthorId,
                originalAuthorName = origAuthorName,
                originalEntityId = origEntityId,
                importedFromShareId = share.id
            ),
            socialSource = SocialSource(
                shareId = share.id,
                copiedAt = System.currentTimeMillis(),
                copyMode = SocialCopyMode.FORK,
                fromUserId = share.fromUserId,
                fromDisplayName = share.fromDisplayName
            )
        )

        recipeRepository.save(forkedRecipe)
        socialRepository.forkShare(shareId, currentUser.id)

        return Result.success(forkedRecipe)
    }
}
