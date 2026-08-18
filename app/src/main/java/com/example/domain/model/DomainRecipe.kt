package com.example.domain.model

data class RecipeIngredient(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val amount: Float,
    val unit: String = "GRAMS",
    val orderIndex: Int = 0
)

data class RecipeStepItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val instruction: String,
    val stepNumber: Int,
    val durationSeconds: Int? = null
)

data class DomainRecipe(
    val id: String = java.util.UUID.randomUUID().toString(),
    val ownerUserId: String,
    val name: String,
    val method: String? = null,
    val recipeKind: String = "BLACK_COFFEE",
    val intention: String = "",
    val ingredients: List<RecipeIngredient> = emptyList(),
    val steps: List<RecipeStepItem> = emptyList(),
    val ingredientsSummary: String = "",
    val stepsSummary: String = "",
    val tags: String = "",
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    val originalAuthorUserId: String? = null,
    val originalAuthorName: String? = null,
    val originalEntityId: String? = null,
    val attribution: Attribution? = null,
    val socialSource: SocialSource? = null
)
