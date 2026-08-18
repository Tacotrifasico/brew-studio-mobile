package com.example.domain.repository

import com.example.domain.model.PreparationTechnique
import kotlinx.coroutines.flow.Flow

interface TechniqueRepository {
    fun observeMine(): Flow<List<PreparationTechnique>>
    suspend fun get(id: String): PreparationTechnique?
    suspend fun save(technique: PreparationTechnique)
}
