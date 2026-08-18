package com.example.feature.social.data

import com.example.data.database.Recipe as RoomRecipe
import com.example.data.database.RecipeIngredient as RoomIngredient
import com.example.data.database.RecipeStep as RoomStep
import com.example.data.database.RecipeDao
import com.example.data.database.RecipeIngredientDao
import com.example.data.database.RecipeStepDao
import com.example.domain.model.*
import com.example.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecipeRepositoryImpl(
    private val recipeDao: RecipeDao,
    private val ingredientDao: RecipeIngredientDao,
    private val stepDao: RecipeStepDao
) : RecipeRepository {

    override fun observeMine(): Flow<List<DomainRecipe>> {
        return recipeDao.getAllRecipes().map { roomRecipes ->
            roomRecipes.map { roomToDomainSync(it) }
        }
    }

    override suspend fun get(id: String): DomainRecipe? {
        val roomRecipe = recipeDao.getRecipeById(id) ?: return null
        return roomToDomainSync(roomRecipe)
    }

    override suspend fun save(recipe: DomainRecipe) {
        val roomRecipe = RoomRecipe(
            id = recipe.id,
            name = recipe.name,
            recipeKind = recipe.recipeKind,
            intention = recipe.intention,
            ingredientsSummary = recipe.ingredientsSummary,
            stepsSummary = recipe.stepsSummary,
            tags = recipe.tags,
            ownerUserId = recipe.ownerUserId,
            originalAuthorUserId = recipe.originalAuthorUserId ?: recipe.attribution?.originalAuthorUserId,
            originalAuthorName = recipe.originalAuthorName ?: recipe.attribution?.originalAuthorName,
            originalEntityId = recipe.originalEntityId ?: recipe.attribution?.originalEntityId,
            importedFromShareId = recipe.attribution?.importedFromShareId ?: recipe.socialSource?.shareId,
            copyMode = recipe.attribution?.mode?.name ?: recipe.socialSource?.copyMode?.name ?: "ORIGINAL"
        )
        recipeDao.insertRecipe(roomRecipe)

        if (recipe.ingredients.isNotEmpty()) {
            val roomIngredients = recipe.ingredients.mapIndexed { idx, ing ->
                RoomIngredient(
                    id = ing.id,
                    recipeId = recipe.id,
                    name = ing.name,
                    amount = ing.amount,
                    unit = ing.unit,
                    orderIndex = idx
                )
            }
            ingredientDao.insertIngredients(roomIngredients)
        }

        if (recipe.steps.isNotEmpty()) {
            val roomSteps = recipe.steps.mapIndexed { idx, st ->
                RoomStep(
                    id = st.id,
                    recipeId = recipe.id,
                    instruction = st.instruction,
                    stepNumber = st.stepNumber,
                    durationSeconds = st.durationSeconds
                )
            }
            stepDao.insertSteps(roomSteps)
        }
    }

    private suspend fun roomToDomainSync(room: RoomRecipe): DomainRecipe {
        val ingredients = ingredientDao.getIngredientsForRecipeSync(room.id).map {
            RecipeIngredient(
                id = it.id,
                name = it.name,
                amount = it.amount,
                unit = it.unit,
                orderIndex = it.orderIndex
            )
        }
        val steps = stepDao.getStepsForRecipeSync(room.id).map {
            RecipeStepItem(
                id = it.id,
                instruction = it.instruction,
                stepNumber = it.stepNumber,
                durationSeconds = it.durationSeconds
            )
        }

        val attrMode = try {
            room.copyMode.let { AttributionMode.valueOf(it) }
        } catch (_: Exception) {
            null
        }

        val attribution = if (room.originalAuthorUserId != null && room.originalAuthorName != null) {
            Attribution(
                required = true,
                mode = attrMode,
                originalAuthorUserId = room.originalAuthorUserId,
                originalAuthorName = room.originalAuthorName,
                originalEntityId = room.originalEntityId ?: room.id,
                importedFromShareId = room.importedFromShareId
            )
        } else null

        return DomainRecipe(
            id = room.id,
            ownerUserId = room.ownerUserId ?: "local_user",
            name = room.name,
            method = room.legacyMethodName,
            recipeKind = room.recipeKind,
            intention = room.intention,
            ingredients = ingredients,
            steps = steps,
            ingredientsSummary = room.ingredientsSummary,
            stepsSummary = room.stepsSummary,
            tags = room.tags,
            originalAuthorUserId = room.originalAuthorUserId,
            originalAuthorName = room.originalAuthorName,
            originalEntityId = room.originalEntityId,
            attribution = attribution
        )
    }
}
