package com.example.data.remote

import android.util.Log
import com.example.data.remote.models.*
import java.io.IOException

class AuthRemoteDataSource {
    private val TAG = "AuthRemoteDataSource"
    private val api = SupabaseClientProvider.apiService

    suspend fun signUp(email: String, password: String, displayName: String, handle: String): Result<AuthResponse> {
        if (!SupabaseClientProvider.isConfigured) {
            return Result.failure(Exception("Supabase no configurado"))
        }
        return try {
            val metadata = mapOf("display_name" to displayName, "handle" to handle.lowercase())
            val response = api.signUp(SignUpRequest(email, password, metadata))
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                SupabaseClientProvider.setAuthToken(auth.accessToken)
                Result.success(auth)
            } else {
                val errBody = response.errorBody()?.string() ?: "Error desconocido"
                Log.e(TAG, "Sign up failed: $errBody")
                Result.failure(IOException("Fallo de registro: $errBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed with exception", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<AuthResponse> {
        if (!SupabaseClientProvider.isConfigured) {
            return Result.failure(Exception("Supabase no configurado"))
        }
        return try {
            val response = api.signIn(SignInRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                SupabaseClientProvider.setAuthToken(auth.accessToken)
                Result.success(auth)
            } else {
                val errBody = response.errorBody()?.string() ?: "Credenciales inválidas"
                Log.e(TAG, "Sign in failed: $errBody")
                Result.failure(IOException("Fallo de autenticación: $errBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed with exception", e)
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        SupabaseClientProvider.setAuthToken(null)
        return Result.success(Unit)
    }

    suspend fun getProfile(userId: String): Result<RemoteProfile> {
        if (!SupabaseClientProvider.isConfigured) {
            return Result.failure(Exception("Supabase no configurado"))
        }
        return try {
            val response = api.getProfile("eq.$userId")
            if (response.isSuccessful) {
                val profiles = response.body()
                if (!profiles.isNullOrEmpty()) {
                    Result.success(profiles.first())
                } else {
                    Result.failure(Exception("Perfil no encontrado"))
                }
            } else {
                Result.failure(IOException(response.errorBody()?.string() ?: "Error de servidor"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertProfile(profile: RemoteProfile): Result<RemoteProfile> {
        if (!SupabaseClientProvider.isConfigured) {
            return Result.failure(Exception("Supabase no configurado"))
        }
        return try {
            val response = api.upsertProfile(profile)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(IOException(response.errorBody()?.string() ?: "Error al actualizar perfil"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
