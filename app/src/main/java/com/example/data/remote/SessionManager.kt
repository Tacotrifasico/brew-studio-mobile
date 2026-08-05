package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("brew_studio_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_HANDLE = "handle"
        private const val KEY_AVATAR_COLOR = "avatar_color"
    }

    fun saveSession(token: String, userId: String, email: String, displayName: String, handle: String?, avatarColor: String?) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, token)
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
        prefs.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            remove(KEY_DISPLAY_NAME)
            remove(KEY_HANDLE)
            remove(KEY_AVATAR_COLOR)
            apply()
        }
        SupabaseClientProvider.setAuthToken(null)
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun getDisplayName(): String = prefs.getString(KEY_DISPLAY_NAME, "") ?: ""
    fun getHandle(): String = prefs.getString(KEY_HANDLE, "") ?: ""
    fun getAvatarColor(): String = prefs.getString(KEY_AVATAR_COLOR, "#3F7A63") ?: "#3F7A63"

    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank()

    init {
        // Automatically restore token in provider on init
        val savedToken = getAccessToken()
        if (!savedToken.isNullOrBlank()) {
            SupabaseClientProvider.setAuthToken(savedToken)
        }
    }
}
