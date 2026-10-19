package com.example.data.repository

import com.example.data.database.*
import com.example.data.remote.TechniqueRemoteDataSource

/** Libraries stay local. Only explicit publication sends a social snapshot. */
class SyncRepository(
    private val authRepo: AuthRepository,
    private val recipeDao: RecipeDao,
    private val techniqueDao: TechniqueDao,
    private val techniqueStepDao: TechniqueStepDao,
    private val techniqueRemoteSource: TechniqueRemoteDataSource
) {
    suspend fun synchronizeAll(): Result<String> = Result.success(
        "Biblioteca guardada en este dispositivo. Sólo se sube lo que compartes."
    )
}
