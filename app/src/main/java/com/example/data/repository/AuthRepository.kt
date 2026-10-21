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

    fun beginGoogle(): String {
        check(com.example.data.remote.SupabaseClientProvider.isConfigured) { "El acceso no está configurado en esta versión." }
        val verifier = sessionManager.beginGoogleOAuth()
        return com.example.data.remote.GoogleOAuthFlow.authorizationURL(
            com.example.data.remote.SupabaseClientProvider.supabaseUrl,
            com.example.BuildConfig.APPLICATION_ID + "://auth/confirmed", verifier)
    }
    fun cancelGoogle() = sessionManager.cancelGoogleOAuth()
    suspend fun finishGoogle(callback: String): Result<SupabaseUser> = refreshLock.withLock {
        val pending = sessionManager.consumeGoogleOAuth()
            ?: return@withLock Result.failure(Exception("Este acceso terminó. Toca Continuar con Google otra vez."))
        try {
            val code = com.example.data.remote.GoogleOAuthFlow.code(callback, com.example.BuildConfig.APPLICATION_ID + "://auth/confirmed")
            val auth = remoteSource.exchangeGoogle(code, pending.second).getOrThrow()
            val user = requireNotNull(auth.user)
            require(runCatching { java.util.UUID.fromString(user.id) }.isSuccess && user.email.isNotBlank())
            require(auth.expiresIn > 0)
            check(sessionManager.isCurrentGoogleOAuth(pending.first)) { "Acceso cancelado." }
            sessionManager.saveSession(requireNotNull(auth.accessToken), user.id, user.email,
                user.email.substringBefore('@'), null, null, auth.refreshToken,
                System.currentTimeMillis() + auth.expiresIn * 1000)
            sessionManager.cancelGoogleOAuth()
            Result.success(user)
        } catch (error: Exception) {
            if (sessionManager.isCurrentGoogleOAuth(pending.first)) sessionManager.cancelGoogleOAuth()
            Result.failure(error)
        }
    }

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
        cancelGoogle()
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
        cancelGoogle()
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

    fun isLoggedIn(): Boolean = com.example.data.remote.AccountAccess.canEnter(
        sessionManager.isLoggedIn(), getUserId(), getCachedEmail())
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
