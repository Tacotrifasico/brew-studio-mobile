package com.example.domain.usecase

import com.example.domain.model.*
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.SocialRepository
import com.example.domain.repository.TechniqueRepository
import java.util.UUID

class ImportTechniqueShareUseCase(
    private val authRepository: AuthRepository,
    private val techniqueRepository: TechniqueRepository,
    private val socialRepository: SocialRepository
) {
    suspend operator fun invoke(shareId: String): Result<PreparationTechnique> {
        val currentUser = authRepository.getCurrentUserSync()
            ?: return Result.failure(IllegalStateException("Debe estar autenticado para guardar una copia de la técnica"))

        val share = socialRepository.getShare(shareId)
            ?: return Result.failure(IllegalArgumentException("Publicación no encontrada"))

        if (share.entityType != ShareEntityType.TECHNIQUE) {
            return Result.failure(IllegalArgumentException("La publicación no es una técnica"))
        }

        val payload = share.payload as? SharedPayload.TechniquePayload
            ?: return Result.failure(IllegalStateException("Payload de técnica no válido"))

        val sourceTech = payload.technique

        val origAuthorId = sourceTech.originalAuthorUserId ?: share.attribution.originalAuthorUserId
        val origAuthorName = sourceTech.originalAuthorName ?: share.attribution.originalAuthorName
        val origEntityId = sourceTech.originalEntityId ?: share.attribution.originalEntityId

        val newTechId = UUID.randomUUID().toString()

        val newTechnique = sourceTech.copy(
            id = newTechId,
            ownerUserId = currentUser.id,
            name = sourceTech.name,
            executionSteps = sourceTech.executionSteps.map { it.copy(id = UUID.randomUUID().toString()) },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            originalAuthorUserId = origAuthorId,
            originalAuthorName = origAuthorName,
            originalEntityId = origEntityId,
            attribution = Attribution(
                required = true,
                mode = AttributionMode.IMPORT,
                originalAuthorUserId = origAuthorId,
                originalAuthorName = origAuthorName,
                originalEntityId = origEntityId,
                importedFromShareId = share.id
            ),
            socialSource = SocialSource(
                shareId = share.id,
                copiedAt = System.currentTimeMillis(),
                copyMode = SocialCopyMode.IMPORT,
                fromUserId = share.fromUserId,
                fromDisplayName = share.fromDisplayName
            )
        )

        techniqueRepository.save(newTechnique)
        socialRepository.importShare(shareId, currentUser.id)

        return Result.success(newTechnique)
    }
}
