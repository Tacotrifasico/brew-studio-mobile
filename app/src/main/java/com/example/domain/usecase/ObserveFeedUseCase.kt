package com.example.domain.usecase

import com.example.domain.model.BrewShare
import com.example.domain.model.ShareVisibility
import com.example.domain.repository.SocialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveFeedUseCase(
    private val socialRepository: SocialRepository
) {
    operator fun invoke(): Flow<List<BrewShare>> {
        return socialRepository.observeFeed().map { list ->
            list.filter { it.visibility == ShareVisibility.PUBLIC }
                .sortedByDescending { it.createdAt }
        }
    }
}
