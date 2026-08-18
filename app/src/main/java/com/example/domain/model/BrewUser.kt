package com.example.domain.model

data class BrewUser(
    val id: String,
    val displayName: String,
    val handle: String,
    val avatarUrl: String? = null,
    val avatarColor: String? = "#3F7A63"
)
