package com.example.feature.social.data

import com.example.data.remote.SessionManager
import com.example.data.repository.AuthRepository as ExistingAuthRepo
import com.example.domain.model.BrewUser
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepositoryImpl(
    private val existingAuthRepo: ExistingAuthRepo,
    private val sessionManager: SessionManager
) : AuthRepository {

    private val _currentUser = MutableStateFlow<BrewUser?>(null)
    override val currentUser: StateFlow<BrewUser?> = _currentUser.asStateFlow()

    init {
        refreshUser()
    }

    private fun refreshUser() {
        if (existingAuthRepo.isLoggedIn()) {
            val uid = existingAuthRepo.getUserId() ?: "local_user"
            val name = existingAuthRepo.getCachedDisplayName()
            val handle = existingAuthRepo.getCachedHandle()
            val color = existingAuthRepo.getCachedAvatarColor()
            _currentUser.value = BrewUser(
                id = uid,
                displayName = name,
                handle = handle,
                avatarUrl = null,
                avatarColor = color
            )
        } else {
            _currentUser.value = null
        }
    }

    override fun getCurrentUserSync(): BrewUser? {
        refreshUser()
        return _currentUser.value
    }

    override suspend fun signIn(email: String, password: String): Result<BrewUser> {
        val result = existingAuthRepo.signIn(email, password)
        return if (result.isSuccess) {
            refreshUser()
            val user = _currentUser.value ?: BrewUser("local_user", "Usuario Brew", "@usuario")
            Result.success(user)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Error al iniciar sesión"))
        }
    }

    override suspend fun signOut() {
        existingAuthRepo.signOut()
        _currentUser.value = null
    }

    fun setCurrentUserDirect(user: BrewUser?) {
        _currentUser.value = user
    }
}
