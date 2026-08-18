package com.example.domain.usecase

import com.example.domain.model.*
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.SocialRepository
import com.example.domain.repository.TechniqueRepository

class ShareTechniqueUseCase(
    private val authRepository: AuthRepository,
    private val techniqueRepository: TechniqueRepository,
    private val socialRepository: SocialRepository
) {
    suspend operator fun invoke(
        techniqueId: String,
        destination: ShareDestination,
        message: String?
    ): Result<BrewShare> {
        val currentUser = authRepository.getCurrentUserSync()
            ?: return Result.failure(IllegalStateException("Debe estar autenticado para compartir"))

        val technique = techniqueRepository.get(techniqueId)
            ?: return Result.failure(IllegalArgumentException("Técnica no encontrada"))

        if (technique.ownerUserId != currentUser.id) {
            return Result.failure(IllegalStateException("No puede publicar como propia una técnica de otro usuario"))
        }

        val origAuthorId = technique.originalAuthorUserId ?: currentUser.id
        val origAuthorName = technique.originalAuthorName ?: currentUser.displayName
        val origEntityId = technique.originalEntityId ?: technique.id

        val attribution = Attribution(
            required = true,
            mode = null,
            originalAuthorUserId = origAuthorId,
            originalAuthorName = origAuthorName,
            originalEntityId = origEntityId
        )

        val metadataList = mutableListOf<String>().apply {
            technique.coffeeGrams?.let { add("${it.toInt()} g") }
            technique.waterMl?.let { add("${it.toInt()} ml") }
            if (technique.executionSteps.isNotEmpty()) add("${technique.executionSteps.size} pasos")
        }

        val targetUserId = if (destination is ShareDestination.Direct) destination.userId else null
        val visibility = if (destination is ShareDestination.Direct) ShareVisibility.DIRECT else ShareVisibility.PUBLIC

        val share = BrewShare(
            entityType = ShareEntityType.TECHNIQUE,
            entityId = technique.id,
            name = technique.name,
            subtitle = technique.method ?: "Técnica de preparación",
            metadata = metadataList,
            fromUserId = currentUser.id,
            fromDisplayName = currentUser.displayName,
            fromHandle = currentUser.handle,
            targetUserId = targetUserId,
            visibility = visibility,
            message = message,
            createdAt = System.currentTimeMillis(),
            attribution = attribution,
            payload = SharedPayload.TechniquePayload(technique)
        )

        return socialRepository.publish(share)
    }
}
