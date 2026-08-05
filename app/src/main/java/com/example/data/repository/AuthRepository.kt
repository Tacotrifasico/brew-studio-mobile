package com.example.data.repository

import com.example.data.remote.AuthRemoteDataSource
import com.example.data.remote.SessionManager
import com.example.data.remote.models.RemoteProfile
import com.example.data.remote.models.SupabaseUser

class AuthRepository(
    private val remoteSource: AuthRemoteDataSource,
    private val sessionManager: SessionManager
) {
    suspend fun signUp(email: String, password: String, displayName: String, handle: String): Result<SupabaseUser> {
        val result = remoteSource.signUp(email, password, displayName, handle)
        return if (result.isSuccess) {
            val auth = result.getOrThrow()
            // Pull the generated profile to cache details
            val profileRes = remoteSource.getProfile(auth.user.id)
            val profile = profileRes.getOrNull()
            
            sessionManager.saveSession(
                token = auth.accessToken,
                userId = auth.user.id,
                email = auth.user.email,
                displayName = profile?.displayName ?: displayName,
                handle = profile?.handle ?: handle,
                avatarColor = profile?.avatarColor
            )
            Result.success(auth.user)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Fallo en registro"))
        }
    }

    suspend fun signIn(email: String, password: String): Result<SupabaseUser> {
        val result = remoteSource.signIn(email, password)
        return if (result.isSuccess) {
            val auth = result.getOrThrow()
            val profileRes = remoteSource.getProfile(auth.user.id)
            val profile = profileRes.getOrNull()

            sessionManager.saveSession(
                token = auth.accessToken,
                userId = auth.user.id,
                email = auth.user.email,
                displayName = profile?.displayName ?: auth.user.email.split("@")[0],
                handle = profile?.handle,
                avatarColor = profile?.avatarColor
            )
            Result.success(auth.user)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Credenciales incorrectas"))
        }
    }

    suspend fun signOut() {
        remoteSource.signOut()
        sessionManager.clearSession()
    }

    fun loginDemo() {
        sessionManager.saveSession(
            token = "local_demo_token_xyz",
            userId = "local_demo_id_001",
            email = "demo@brewstudio.app",
            displayName = "Barista Demo",
            handle = "brewther_local",
            avatarColor = "#3F7A63"
        )
    }

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()
    fun getUserId(): String? = sessionManager.getUserId()
    fun getCachedDisplayName(): String = sessionManager.getDisplayName()
    fun getCachedHandle(): String = sessionManager.getHandle()
    fun getCachedAvatarColor(): String = sessionManager.getAvatarColor()
    fun getCachedEmail(): String? = sessionManager.getUserEmail()

    suspend fun fetchCurrentProfile(): Result<RemoteProfile> {
        val uid = getUserId() ?: return Result.failure(Exception("Usuario no autenticado"))
        val result = remoteSource.getProfile(uid)
        if (result.isSuccess) {
            val profile = result.getOrThrow()
            sessionManager.updateProfileInfo(
                displayName = profile.displayName,
                handle = profile.handle,
                avatarColor = profile.avatarColor
            )
        }
        return result
    }

    suspend fun updateProfile(displayName: String, handle: String, avatarColor: String): Result<RemoteProfile> {
        val uid = getUserId() ?: return Result.failure(Exception("Usuario no autenticado"))
        val email = getCachedEmail() ?: ""
        
        val updateModel = RemoteProfile(
            id = uid,
            email = email,
            displayName = displayName,
            handle = handle,
            avatarUrl = null,
            avatarColor = avatarColor,
            role = "user",
            createdAt = null,
            updatedAt = null
        )

        val result = remoteSource.upsertProfile(updateModel)
        if (result.isSuccess) {
            val updated = result.getOrThrow()
            sessionManager.updateProfileInfo(
                displayName = updated.displayName,
                handle = updated.handle,
                avatarColor = updated.avatarColor
            )
        }
        return result
    }
}
