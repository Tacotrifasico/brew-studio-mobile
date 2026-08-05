package com.example.data.remote

import android.util.Log
import com.example.data.remote.models.*
import java.io.IOException

class SocialRemoteDataSource {
    private val TAG = "SocialRemoteDataSource"
    private val api = SupabaseClientProvider.apiService

    suspend fun getFeed(): Result<List<RemoteShare>> {
        if (!SupabaseClientProvider.isConfigured) return Result.success(emptyList())
        return try {
            val response = api.getPublicFeed()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                val err = response.errorBody()?.string() ?: "Error de feed"
                Result.failure(IOException(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInbox(userId: String): Result<List<RemoteInboxItem>> {
        if (!SupabaseClientProvider.isConfigured) return Result.success(emptyList())
        return try {
            val response = api.getInbox("eq.$userId")
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(IOException(response.errorBody()?.string() ?: "Error de recibidos"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun shareEntity(share: RemoteShare): Result<RemoteShare> {
        if (!SupabaseClientProvider.isConfigured) return Result.failure(Exception("Supabase no configurado"))
        return try {
            val response = api.shareEntity(share)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(IOException(response.errorBody()?.string() ?: "Error al publicar"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun likeShare(shareId: String, userId: String): Result<Unit> {
        if (!SupabaseClientProvider.isConfigured) return Result.failure(Exception("Supabase no configurado"))
        return try {
            val response = api.likeShare(mapOf("share_id" to shareId, "user_id" to userId))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException(response.errorBody()?.string() ?: "Error al dar like"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlikeShare(shareId: String, userId: String): Result<Unit> {
        if (!SupabaseClientProvider.isConfigured) return Result.failure(Exception("Supabase no configurado"))
        return try {
            val response = api.unlikeShare("eq.$shareId", "eq.$userId")
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException(response.errorBody()?.string() ?: "Error al quitar like"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveShare(shareId: String, userId: String): Result<Unit> {
        if (!SupabaseClientProvider.isConfigured) return Result.failure(Exception("Supabase no configurado"))
        return try {
            val response = api.saveShare(mapOf("share_id" to shareId, "user_id" to userId))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(IOException(response.errorBody()?.string() ?: "Error al bookmarkear"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importShare(shareId: String, isRecipe: Boolean): Result<String> {
        if (!SupabaseClientProvider.isConfigured) return Result.failure(Exception("Supabase no configurado"))
        return try {
            val payload = RpcShareIdPayload(shareId)
            val response = if (isRecipe) {
                api.rpcImportAsRecipe(payload)
            } else {
                api.rpcImportAsTechnique(payload)
            }
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(IOException(response.errorBody()?.string() ?: "Error al importar"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun forkShare(shareId: String, isRecipe: Boolean): Result<String> {
        if (!SupabaseClientProvider.isConfigured) return Result.failure(Exception("Supabase no configurado"))
        return try {
            val payload = RpcShareIdPayload(shareId)
            val response = if (isRecipe) {
                api.rpcForkAsRecipe(payload)
            } else {
                api.rpcForkAsTechnique(payload)
            }
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(IOException(response.errorBody()?.string() ?: "Error en fork"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActivityTimeline(userId: String): Result<List<RemoteActivityLog>> {
        if (!SupabaseClientProvider.isConfigured) return Result.success(emptyList())
        return try {
            val response = api.getActivityTimeline("eq.$userId")
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(IOException(response.errorBody()?.string() ?: "Error de historial"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logActivity(log: RemoteActivityLog): Result<Unit> {
        if (!SupabaseClientProvider.isConfigured) return Result.success(Unit)
        return try {
            api.logActivity(log)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
