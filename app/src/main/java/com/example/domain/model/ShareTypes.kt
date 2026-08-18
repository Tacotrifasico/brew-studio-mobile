package com.example.domain.model

enum class ShareEntityType {
    RECIPE,
    TECHNIQUE
}

enum class ShareVisibility {
    PUBLIC,
    DIRECT
}

sealed interface ShareDestination {
    data object Feed : ShareDestination
    data class Direct(
        val userId: String,
        val displayName: String? = null
    ) : ShareDestination
}

sealed interface SharedPayload {
    data class RecipePayload(
        val recipe: DomainRecipe
    ) : SharedPayload

    data class TechniquePayload(
        val technique: PreparationTechnique
    ) : SharedPayload
}
