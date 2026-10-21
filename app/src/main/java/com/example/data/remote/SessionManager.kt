package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("brew_studio_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ENCRYPTED = "encrypted_session_v1"
        private var memorySession: String? = null // memory-only fallback if Keystore is unavailable
        private var memoryOAuth: String? = null
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_HANDLE = "handle"
        private const val KEY_AVATAR_COLOR = "avatar_color"
    }

    fun saveSession(token: String, userId: String, email: String, displayName: String, handle: String?, avatarColor: String?, refreshToken: String? = null, expiresAt: Long = Long.MAX_VALUE) {
        val bundle = JSONArray().put(token).put(refreshToken.orEmpty()).put(expiresAt).put(userId).toString()
        memorySession = bundle
        val encrypted = runCatching { SessionCipher.encrypt(bundle) }.getOrNull()
        prefs.edit().apply {
            remove(KEY_ACCESS_TOKEN) // retire plaintext legacy credential
            if (encrypted != null) putString(KEY_ENCRYPTED, encrypted) else remove(KEY_ENCRYPTED)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_DISPLAY_NAME, displayName)
            putString(KEY_HANDLE, handle)
            putString(KEY_AVATAR_COLOR, avatarColor ?: "#3F7A63")
            apply()
        }
        // Sync provider
        SupabaseClientProvider.setAuthToken(token)
    }

    fun updateProfileInfo(displayName: String, handle: String?, avatarColor: String?) {
        prefs.edit().apply {
            putString(KEY_DISPLAY_NAME, displayName)
            putString(KEY_HANDLE, handle)
            putString(KEY_AVATAR_COLOR, avatarColor ?: "#3F7A63")
            apply()
        }
    }

    fun clearSession() {
        cancelGoogleOAuth()
        memorySession = null
        prefs.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_ENCRYPTED)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            remove(KEY_DISPLAY_NAME)
            remove(KEY_HANDLE)
            remove(KEY_AVATAR_COLOR)
            apply()
        }
        SupabaseClientProvider.setAuthToken(null)
    }

    fun beginGoogleOAuth(): String {
        val verifier = GoogleOAuthFlow.verifier()
        val id = java.util.UUID.randomUUID().toString()
        val raw = JSONArray().put(id).put(verifier).put(System.currentTimeMillis()).toString()
        memoryOAuth = raw
        val encrypted = runCatching { SessionCipher.encrypt(raw) }.getOrNull()
        prefs.edit().apply {
            if (encrypted == null) remove("oauth_pending") else putString("oauth_pending", encrypted)
            putString("oauth_attempt", id)
        }.commit()
        return verifier
    }

    @Synchronized fun consumeGoogleOAuth(): Pair<String, String>? {
        val raw = prefs.getString("oauth_pending", null)?.let { runCatching { SessionCipher.decrypt(it) }.getOrNull() } ?: memoryOAuth
        memoryOAuth = null
        prefs.edit().remove("oauth_pending").commit()
        val value = raw?.let { runCatching { JSONArray(it) }.getOrNull() } ?: return null
        val age = System.currentTimeMillis() - value.optLong(2)
        return if (age in 0..600_000 && isCurrentGoogleOAuth(value.optString(0))) value.optString(0) to value.optString(1) else null
    }
    fun isCurrentGoogleOAuth(id: String): Boolean = prefs.getString("oauth_attempt", null) == id
    fun cancelGoogleOAuth() {
        memoryOAuth = null
        prefs.edit().remove("oauth_pending").remove("oauth_attempt").commit()
    }

    private fun credentials(): JSONArray? {
        val raw = prefs.getString(KEY_ENCRYPTED, null)?.let { runCatching { SessionCipher.decrypt(it) }.getOrNull() } ?: memorySession
        return raw?.let { runCatching { JSONArray(it) }.getOrNull() }
            ?.takeIf { it.optString(3) == prefs.getString(KEY_USER_ID, null) }
    }
    fun getAccessToken(): String? = credentials()?.optString(0)?.takeIf { it.isNotBlank() }
    fun getRefreshToken(): String? = credentials()?.optString(1)?.takeIf { it.isNotBlank() }
    fun getExpiresAt(): Long = credentials()?.optLong(2, 0) ?: 0
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun replaceTokens(access: String, refresh: String, expiresIn: Long) = saveSession(
        access, requireNotNull(getUserId()), getUserEmail().orEmpty(), getDisplayName(), getHandle(), getAvatarColor(),
        refresh, System.currentTimeMillis() + expiresIn.coerceAtLeast(0) * 1000
    )
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun getDisplayName(): String = prefs.getString(KEY_DISPLAY_NAME, "") ?: ""
    fun getHandle(): String = prefs.getString(KEY_HANDLE, "") ?: ""
    fun getAvatarColor(): String = prefs.getString(KEY_AVATAR_COLOR, "#3F7A63") ?: "#3F7A63"

    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank()

    fun observeUserId(): Flow<String?> = callbackFlow {
        trySend(getUserId())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_USER_ID) trySend(getUserId())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    init {
        // Never reuse a plaintext legacy credential; require login again.
        prefs.edit().remove(KEY_ACCESS_TOKEN).apply()
        // Restore the encrypted credential; refresh runs before community requests.
        val savedToken = getAccessToken()
        if (!savedToken.isNullOrBlank()) {
            SupabaseClientProvider.setAuthToken(savedToken)
        }
    }
}
