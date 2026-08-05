package com.example.data.repository

import com.example.data.database.*
import com.example.data.engine.RecipeIngredientInput
import kotlinx.coroutines.flow.Flow

class BrewRepository(
    private val ratioPresetDao: RatioPresetDao,
    private val ratioLastUsedDao: RatioLastUsedDao,
    private val beanDao: BeanDao,
    private val instrumentDao: InstrumentDao,
    private val grinderProfileDao: GrinderProfileDao,
    private val grinderMethodSettingDao: GrinderMethodSettingDao,
    private val techniqueDao: TechniqueDao,
    private val techniqueStepDao: TechniqueStepDao,
    private val recipeDao: RecipeDao,
    private val cataDao: CataDao,
    private val cupDao: CupDao,
    private val labExperimentDao: LabExperimentDao,
    private val recipeIngredientDao: RecipeIngredientDao? = null
) {
    // Flows for data lists
    val allPresets: Flow<List<RatioPreset>> = ratioPresetDao.getAllPresets()
    val allBeans: Flow<List<Bean>> = beanDao.getAllBeans()
    val allInstruments: Flow<List<Instrument>> = instrumentDao.getAllInstruments()
    val allGrinders: Flow<List<Instrument>> = instrumentDao.getAllGrinders()
    val allTechniques: Flow<List<Technique>> = techniqueDao.getAllTechniques()
    val allRecipes: Flow<List<Recipe>> = recipeDao.getAllRecipes()
    val allCatas: Flow<List<Cata>> = cataDao.getAllCatas()
    val allCups: Flow<List<Cup>> = cupDao.getAllCups()
    val allExperiments: Flow<List<LabExperiment>> = labExperimentDao.getAllExperiments()

    // Counts
    val beansCount: Flow<Int> = beanDao.getBeansCount()
    val instrumentsCount: Flow<Int> = instrumentDao.getInstrumentsCount()
    val techniquesCount: Flow<Int> = techniqueDao.getTechniquesCount()
    val recipesCount: Flow<Int> = recipeDao.getRecipesCount()
    val cupsCount: Flow<Int> = cupDao.getCupsCount()
    val experimentsCount: Flow<Int> = labExperimentDao.getExperimentsCount()

    // Last Used Methods helper
    suspend fun getLastUsedForMethod(method: String): RatioLastUsed? =
        ratioLastUsedDao.getLastUsedForMethod(method)

    suspend fun insertLastUsed(lastUsed: RatioLastUsed) =
        ratioLastUsedDao.insertLastUsed(lastUsed)

    // Presets
    suspend fun insertPreset(preset: RatioPreset) = ratioPresetDao.insertPreset(preset)
    suspend fun insertPresets(presets: List<RatioPreset>) = ratioPresetDao.insertPresets(presets)
    suspend fun deletePreset(preset: RatioPreset) = ratioPresetDao.deletePreset(preset)

    // Beans
    suspend fun insertBean(bean: Bean) = beanDao.insertBean(bean)
    suspend fun deleteBean(bean: Bean) = beanDao.deleteBean(bean)

    // Instruments
    suspend fun insertInstrument(instrument: Instrument) = instrumentDao.insertInstrument(instrument)
    suspend fun deleteInstrument(instrument: Instrument) = instrumentDao.deleteInstrument(instrument)

    // Techniques & Steps
    suspend fun insertTechnique(technique: Technique, steps: List<TechniqueStep>) {
        techniqueDao.insertTechnique(technique)
        val stepsWithId = steps.map { it.copy(techniqueId = technique.id) }
        techniqueStepDao.insertSteps(stepsWithId)
    }
    suspend fun deleteTechnique(technique: Technique) {
        techniqueDao.deleteTechnique(technique)
        techniqueStepDao.deleteStepsForTechnique(technique.id)
    }
    fun getStepsForTechnique(techId: String): Flow<List<TechniqueStep>> =
        techniqueStepDao.getStepsForTechnique(techId)

    suspend fun getStepsForTechniqueSync(techId: String): List<TechniqueStep> =
        techniqueStepDao.getStepsForTechniqueSync(techId)

    // Recipes
    suspend fun insertRecipe(recipe: Recipe, ingredients: List<RecipeIngredientInput> = emptyList()) {
        recipeDao.insertRecipe(recipe)
        if (ingredients.isNotEmpty() && recipeIngredientDao != null) {
            recipeIngredientDao.deleteIngredientsForRecipe(recipe.id)
            val entities = ingredients.filter { it.name.isNotBlank() }.mapIndexed { idx, ing ->
                RecipeIngredient(
                    recipeId = recipe.id,
                    name = ing.name.trim(),
                    amount = ing.amount.toFloatOrNull() ?: 0f,
                    unit = ing.unit.trim(),
                    orderIndex = idx
                )
            }
            if (entities.isNotEmpty()) {
                recipeIngredientDao.insertIngredients(entities)
            }
        }
    }
    suspend fun deleteRecipe(recipe: Recipe) {
        recipeDao.deleteRecipe(recipe)
        recipeIngredientDao?.deleteIngredientsForRecipe(recipe.id)
    }

    // Catas
    suspend fun insertCata(cata: Cata) = cataDao.insertCata(cata)
    suspend fun deleteCata(cata: Cata) = cataDao.deleteCata(cata)

    // Cups
    suspend fun insertCup(cup: Cup) = cupDao.insertCup(cup)
    suspend fun deleteCup(cup: Cup) = cupDao.deleteCup(cup)

    // Experiments
    suspend fun insertExperiment(experiment: LabExperiment) = labExperimentDao.insertExperiment(experiment)
    suspend fun deleteExperiment(experiment: LabExperiment) = labExperimentDao.deleteExperiment(experiment)
}
