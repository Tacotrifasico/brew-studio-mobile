package com.example.data.repository

import com.example.data.remote.AuthRemoteDataSource
import com.example.data.remote.SessionManager
import com.example.data.remote.models.RemoteProfile
import com.example.data.remote.models.SupabaseUser
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AuthRepository(
    private val remoteSource: AuthRemoteDataSource,
    private val sessionManager: SessionManager
) {
    private val refreshLock = Mutex()

    suspend fun ensureSession(): Result<Unit> = refreshLock.withLock {
        if (!sessionManager.isLoggedIn() || getUserId()?.let { runCatching { java.util.UUID.fromString(it) }.isFailure } != false) return@withLock Result.failure(Exception("Inicia sesión para usar Comunidad."))
        if (sessionManager.getExpiresAt() > System.currentTimeMillis() + 60_000) return@withLock Result.success(Unit)
        val refresh = sessionManager.getRefreshToken() ?: return@withLock Result.failure(Exception("Vuelve a iniciar sesión."))
        val result = remoteSource.refresh(refresh)
        result.fold(onSuccess = { auth ->
            val access = auth.accessToken ?: return@fold Result.failure(Exception("Respuesta de sesión inválida"))
            sessionManager.replaceTokens(access, auth.refreshToken ?: refresh, auth.expiresIn)
            Result.success(Unit)
        }, onFailure = { error ->
            if (error.message == "SESSION_REVOKED") sessionManager.clearSession()
            Result.failure(Exception(if (error.message == "SESSION_REVOKED") "Tu sesión terminó. Vuelve a ingresar." else error.message))
        })
    }

    suspend fun signUp(email: String, password: String, displayName: String, handle: String): Result<SupabaseUser> {
        val result = remoteSource.signUp(email, password, displayName, handle)
        return if (result.isSuccess) {
            val auth = result.getOrThrow()
            val user = auth.user ?: SupabaseUser(auth.id ?: return Result.failure(Exception("Respuesta de registro inválida")), auth.email ?: email)
            if (auth.accessToken.isNullOrBlank()) return Result.success(user) // confirmation required, not a session
            // Pull the generated profile to cache details
            val profileRes = remoteSource.getProfile(user.id)
            val profile = profileRes.getOrNull()
            
            sessionManager.saveSession(
                token = auth.accessToken,
                userId = user.id,
                email = user.email,
                displayName = profile?.displayName ?: displayName,
                handle = profile?.handle ?: handle,
                avatarColor = profile?.avatarColor,
                refreshToken = auth.refreshToken,
                expiresAt = System.currentTimeMillis() + auth.expiresIn * 1000
            )
            Result.success(user)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Fallo en registro"))
        }
    }

    suspend fun signIn(email: String, password: String): Result<SupabaseUser> {
        val result = remoteSource.signIn(email, password)
        return if (result.isSuccess) {
            val auth = result.getOrThrow()
            val user = auth.user ?: return Result.failure(Exception("Respuesta de sesión inválida"))
            if (auth.accessToken.isNullOrBlank()) return Result.failure(Exception("Sesión inválida"))
            val profileRes = remoteSource.getProfile(user.id)
            val profile = profileRes.getOrNull()

            sessionManager.saveSession(
                token = auth.accessToken,
                userId = user.id,
                email = user.email,
                displayName = profile?.displayName ?: user.email.split("@")[0],
                handle = profile?.handle,
                avatarColor = profile?.avatarColor,
                refreshToken = auth.refreshToken,
                expiresAt = System.currentTimeMillis() + auth.expiresIn * 1000
            )
            Result.success(user)
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
        ensureSession().exceptionOrNull()?.let { return Result.failure(it) }
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

    suspend fun updateProfile(displayName: String, handle: String, avatarColor: String, isPrivate: Boolean = true): Result<RemoteProfile> {
        val uid = getUserId() ?: return Result.failure(Exception("Usuario no autenticado"))
        val email = getCachedEmail() ?: ""
        
        val updateModel = RemoteProfile(
            id = uid,
            displayName = displayName,
            handle = handle,
            isPrivate = isPrivate,
            avatarUrl = null,
            avatarColor = avatarColor,
            createdAt = null,
            updatedAt = null
        )

        ensureSession().exceptionOrNull()?.let { return Result.failure(it) }
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
