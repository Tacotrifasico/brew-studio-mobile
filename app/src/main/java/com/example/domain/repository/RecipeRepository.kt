package com.example.domain.repository

import com.example.domain.model.DomainRecipe
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun observeMine(): Flow<List<DomainRecipe>>
    suspend fun get(id: String): DomainRecipe?
    suspend fun save(recipe: DomainRecipe)
}
