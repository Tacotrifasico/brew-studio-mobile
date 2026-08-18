package com.example.domain.usecase

import com.example.domain.model.BrewShare
import com.example.domain.model.ShareVisibility
import com.example.domain.repository.SocialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveInboxUseCase(
    private val socialRepository: SocialRepository
) {
    operator fun invoke(userId: String): Flow<List<BrewShare>> {
        return socialRepository.observeInbox(userId).map { list ->
            list.filter { it.visibility == ShareVisibility.DIRECT && it.targetUserId == userId }
                .sortedByDescending { it.createdAt }
        }
    }
}
