package com.example.domain.usecase

import com.example.domain.repository.AuthRepository
import com.example.domain.repository.SocialRepository

class ToggleShareLikeUseCase(
    private val authRepository: AuthRepository,
    private val socialRepository: SocialRepository
) {
    suspend operator fun invoke(shareId: String): Result<Boolean> {
        val currentUser = authRepository.getCurrentUserSync()
            ?: return Result.failure(IllegalStateException("Debe estar autenticado para reaccionar"))

        return socialRepository.toggleLike(shareId, currentUser.id)
    }
}
