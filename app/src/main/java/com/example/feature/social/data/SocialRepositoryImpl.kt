package com.example.feature.social.data

import com.example.domain.model.*
import com.example.domain.repository.SocialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class SocialRepositoryImpl : SocialRepository {

    private val sharesState = MutableStateFlow<List<BrewShare>>(emptyList())

    override fun observeFeed(): Flow<List<BrewShare>> {
        return sharesState.asStateFlow().map { list ->
            list.filter { it.visibility == ShareVisibility.PUBLIC }
        }
    }

    override fun observeInbox(userId: String): Flow<List<BrewShare>> {
        return sharesState.asStateFlow().map { list ->
            list.filter { it.visibility == ShareVisibility.DIRECT && it.targetUserId == userId }
        }
    }

    override suspend fun publish(share: BrewShare): Result<BrewShare> {
        cache(share)
        return Result.success(share)
    }

    /** Stages a snapshot for import/fork use cases without causing any network mutation. */
    fun cache(share: BrewShare) {
        val current = sharesState.value.toMutableList()
        current.removeAll { it.id == share.id }
        current.add(0, share)
        sharesState.value = current
    }

    override suspend fun toggleLike(shareId: String, userId: String): Result<Boolean> {
        val current = sharesState.value.toMutableList()
        val index = current.indexOfFirst { it.id == shareId }
        if (index == -1) return Result.failure(IllegalArgumentException("Publicación no encontrada"))

        val targetShare = current[index]
        val likes = targetShare.likes.toMutableSet()
        val isLikedNow: Boolean

        if (likes.contains(userId)) {
            likes.remove(userId)
            isLikedNow = false
        } else {
            likes.add(userId)
            isLikedNow = true
        }

        current[index] = targetShare.copy(likes = likes)
        sharesState.value = current

        return Result.success(isLikedNow)
    }

    override suspend fun importShare(shareId: String, currentUserId: String): Result<BrewShare> {
        val current = sharesState.value.toMutableList()
        val index = current.indexOfFirst { it.id == shareId }
        if (index == -1) return Result.failure(IllegalArgumentException("Publicación no encontrada"))

        val targetShare = current[index]
        val saves = targetShare.saves.toMutableSet()
        saves.add(currentUserId)

        val updatedShare = targetShare.copy(saves = saves)
        current[index] = updatedShare
        sharesState.value = current

        return Result.success(updatedShare)
    }

    override suspend fun forkShare(shareId: String, currentUserId: String): Result<BrewShare> {
        val current = sharesState.value.toMutableList()
        val index = current.indexOfFirst { it.id == shareId }
        if (index == -1) return Result.failure(IllegalArgumentException("Publicación no encontrada"))

        val targetShare = current[index]
        val saves = targetShare.saves.toMutableSet()
        saves.add(currentUserId)

        val updatedShare = targetShare.copy(saves = saves)
        current[index] = updatedShare
        sharesState.value = current

        return Result.success(updatedShare)
    }

    override suspend fun getShare(shareId: String): BrewShare? {
        return sharesState.value.find { it.id == shareId }
    }

    fun addMockShareForTesting(share: BrewShare) {
        val current = sharesState.value.toMutableList()
        current.add(0, share)
        sharesState.value = current
    }
}
