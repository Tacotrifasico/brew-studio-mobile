package com.example.data.remote

import com.example.data.remote.models.RemoteRecipe
import java.io.IOException

class RecipeRemoteDataSource {
    private val api = SupabaseClientProvider.apiService

    suspend fun getRecipes(userId: String): Result<List<RemoteRecipe>> {
        if (!SupabaseClientProvider.isConfigured) return Result.success(emptyList())
        return try {
            val response = api.getRecipes("eq.$userId")
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(IOException(response.errorBody()?.string() ?: "Error de descarga"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertRecipe(recipe: RemoteRecipe): Result<RemoteRecipe> {
        if (!SupabaseClientProvider.isConfigured) return Result.failure(Exception("Supabase no configurado"))
        return try {
            val response = api.insertRecipe(recipe)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(IOException(response.errorBody()?.string() ?: "Error de guardado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
