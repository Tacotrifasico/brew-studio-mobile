package com.example.domain.model

data class ExecutionStep(
    val id: String = java.util.UUID.randomUUID().toString(),
    val stepNumber: Int,
    val title: String,
    val durationSeconds: Int,
    val waterAddedMl: Int,
    val waterAccumulatedMl: Int,
    val intensity: String = "MEDIUM",
    val gesture: String = "CIRCULAR_POUR",
    val stepNote: String = ""
)

data class PreparationTechnique(
    val id: String = java.util.UUID.randomUUID().toString(),
    val ownerUserId: String,
    val name: String,
    val method: String? = null,
    val methodId: String? = null,
    val recipeBase: String? = null,

    val coffeeGrams: Double? = 15.0,
    val waterMl: Double? = 240.0,
    val grind: Double? = 18.0,
    val grindDescription: String? = "18 Clicks",
    val temperatureC: Double? = 93.0,
    val totalTimeSeconds: Int? = 180,

    val executionMode: String = "GUIDED",
    val executionSteps: List<ExecutionStep> = emptyList(),

    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    val originalAuthorUserId: String? = null,
    val originalAuthorName: String? = null,
    val originalEntityId: String? = null,
    val attribution: Attribution? = null,
    val socialSource: SocialSource? = null
)
