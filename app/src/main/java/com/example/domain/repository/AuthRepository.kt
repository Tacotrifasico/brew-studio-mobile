package com.example.domain.repository

import com.example.domain.model.BrewUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<BrewUser?>
    fun getCurrentUserSync(): BrewUser?
    suspend fun signIn(email: String, password: String): Result<BrewUser>
    suspend fun signOut()
}
