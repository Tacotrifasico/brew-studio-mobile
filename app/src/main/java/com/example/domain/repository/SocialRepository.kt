package com.example.domain.repository

import com.example.domain.model.BrewShare
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    fun observeFeed(): Flow<List<BrewShare>>
    fun observeInbox(userId: String): Flow<List<BrewShare>>
    suspend fun publish(share: BrewShare): Result<BrewShare>
    suspend fun toggleLike(shareId: String, userId: String): Result<Boolean>
    suspend fun importShare(shareId: String, currentUserId: String): Result<BrewShare>
    suspend fun forkShare(shareId: String, currentUserId: String): Result<BrewShare>
    suspend fun getShare(shareId: String): BrewShare?
}
