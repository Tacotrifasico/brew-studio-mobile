package com.example.data.remote

import com.example.data.remote.models.RemoteTechnique
import com.example.data.remote.models.RemoteTechniqueStep
import java.io.IOException

class TechniqueRemoteDataSource {
    private val api = SupabaseClientProvider.apiService

    suspend fun getTechniques(userId: String): Result<List<RemoteTechnique>> {
        if (!SupabaseClientProvider.isConfigured) return Result.success(emptyList())
        return try {
            val response = api.getTechniques("eq.$userId")
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(IOException(response.errorBody()?.string() ?: "Error de descarga"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertTechnique(technique: RemoteTechnique): Result<RemoteTechnique> {
        if (!SupabaseClientProvider.isConfigured) return Result.failure(Exception("Supabase no configurado"))
        return try {
            val response = api.insertTechnique(technique)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(IOException(response.errorBody()?.string() ?: "Error de guardado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTechniqueSteps(techniqueId: String): Result<List<RemoteTechniqueStep>> {
        if (!SupabaseClientProvider.isConfigured) return Result.success(emptyList())
        return try {
            val response = api.getTechniqueSteps("eq.$techniqueId")
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(IOException(response.errorBody()?.string() ?: "Error al descargar pasos"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertTechniqueSteps(steps: List<RemoteTechniqueStep>): Result<List<RemoteTechniqueStep>> {
        if (!SupabaseClientProvider.isConfigured) return Result.failure(Exception("Supabase no configurado"))
        if (steps.isEmpty()) return Result.success(emptyList())
        return try {
            val response = api.insertTechniqueSteps(steps)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(IOException(response.errorBody()?.string() ?: "Error al guardar pasos"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
