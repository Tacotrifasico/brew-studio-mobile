package com.example.data.repository

import com.example.data.database.*
import com.example.data.catalog.BrewTechniqueCatalog
import com.example.data.engine.RecipeIngredientInput
import com.example.data.engine.RecipeStepInput
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
    private val brewMethodDao: BrewMethodDao? = null,
    private val userMethodPreferenceDao: UserMethodPreferenceDao? = null
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
    val allBrewMethods: Flow<List<BrewMethod>> = brewMethodDao?.getAllMethods() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    val userMethodPreferences: Flow<List<UserMethodPreference>> = userMethodPreferenceDao?.getAllActivePreferences() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    val pinnedUserMethodPreferences: Flow<List<UserMethodPreference>> = userMethodPreferenceDao?.getPinnedPreferences() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun setMethodPinnedStatus(methodId: String, scopeKey: String, isPinned: Boolean) {
        userMethodPreferenceDao?.setPinnedStatus(methodId, scopeKey, isPinned)
    }

    suspend fun getPreferenceByMethodId(methodId: String, scopeKey: String): UserMethodPreference? {
        return userMethodPreferenceDao?.getPreferenceByMethodId(methodId, scopeKey)
    }

    suspend fun getPreferenceByInstrumentId(instrumentId: String, scopeKey: String): UserMethodPreference? {
        return userMethodPreferenceDao?.getPreferenceByInstrumentId(instrumentId, scopeKey)
    }

    suspend fun insertUserMethodPreference(pref: UserMethodPreference) {
        userMethodPreferenceDao?.insertPreference(pref)
    }

    suspend fun getBrewMethodById(id: String): BrewMethod? {
        return brewMethodDao?.getMethodById(id)
    }

    suspend fun insertBrewMethod(method: BrewMethod) {
        brewMethodDao?.insertMethod(method)
    }

    suspend fun getOrCreateBrewMethodForInstrument(name: String): BrewMethod {
        val code = name.lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "")
        val existing = brewMethodDao?.getMethodByCode(code)
        if (existing != null) return existing
        val newMethod = BrewMethod(
            code = if (code.isBlank()) "method_${System.currentTimeMillis()}" else code,
            nameKey = name,
            category = "POUR_OVER",
            defaultRatio = 16.0f
        )
        brewMethodDao?.insertMethod(newMethod)
        return newMethod
    }

    suspend fun ensureCoreCatalog() {
        if (beanDao.getBeanById(SampleCoffee.ID) == null) beanDao.insertBean(SampleCoffee.bean())
        BrewTechniqueCatalog.methods.forEach { method ->
            if (brewMethodDao?.getMethodById(method.id) == null) brewMethodDao?.insertMethod(method)
        }
        BrewTechniqueCatalog.techniques.forEach { template ->
            if (techniqueDao.getTechniqueById(template.id) == null) {
                val technique = BrewTechniqueCatalog.entity(template)
                insertTechnique(technique, BrewTechniqueCatalog.steps(template, technique.waterMl))
            }
        }
    }

    suspend fun ensureMethodPreferences(scopeKey: String) {
        BrewTechniqueCatalog.preferencesFor(scopeKey).forEach { preference ->
            if (userMethodPreferenceDao?.getPreferenceByMethodId(preference.methodId, scopeKey) == null) {
                userMethodPreferenceDao?.insertPreference(preference)
            }
        }
    }

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
    suspend fun insertBean(bean: Bean) = beanDao.insertBean(if (bean.isSample) bean.copy(
        name = "Ronpotrero", ownerUserId = null, status = "abierto", stockGrams = bean.stockGrams.coerceAtLeast(1f), syncStatus = "SYNCED") else bean)
    suspend fun deleteBean(bean: Bean) { if (!bean.isSample) beanDao.deleteBean(bean) }

    // Instruments
    suspend fun insertInstrument(instrument: Instrument) = instrumentDao.insertInstrument(instrument)
    suspend fun deleteInstrument(instrument: Instrument) {
        instrumentDao.deleteInstrument(instrument)
        userMethodPreferenceDao?.deletePreferenceByInstrumentId(instrument.id)
    }

    // Techniques & Steps
    suspend fun insertTechnique(technique: Technique, steps: List<TechniqueStep>) {
        techniqueDao.insertTechniqueWithSteps(technique, steps)
    }
    suspend fun replaceTechnique(technique: Technique, steps: List<TechniqueStep>) {
        techniqueDao.replaceTechniqueWithSteps(technique, steps)
    }
    suspend fun deleteTechnique(technique: Technique) {
        techniqueDao.deleteTechniqueWithSteps(technique)
    }
    fun getStepsForTechnique(techId: String): Flow<List<TechniqueStep>> =
        techniqueStepDao.getStepsForTechnique(techId)

    suspend fun getStepsForTechniqueSync(techId: String): List<TechniqueStep> =
        techniqueStepDao.getStepsForTechniqueSync(techId)

    // Recipes
    suspend fun insertRecipe(
        recipe: Recipe,
        ingredients: List<RecipeIngredientInput> = emptyList(),
        steps: List<RecipeStepInput> = emptyList(),
        replaceIngredients: Boolean = false
    ) {
        if (!replaceIngredients) {
            recipeDao.insertRecipe(recipe)
            return
        }
        val entities = ingredients.filter { it.name.isNotBlank() }.mapIndexed { idx, ing ->
            RecipeIngredient(
                recipeId = recipe.id,
                name = ing.name.trim(),
                amount = ing.amount.trim().replace(',', '.').toFloatOrNull() ?: 0f,
                unit = ing.unit.trim(),
                orderIndex = idx
            )
        }
        val stepEntities = steps.filter { it.instruction.isNotBlank() }.mapIndexed { idx, step ->
            RecipeStep(
                id = step.id,
                recipeId = recipe.id,
                instruction = step.instruction.trim(),
                stepNumber = idx + 1
            )
        }
        recipeDao.replaceRecipeAggregate(recipe, entities, stepEntities)
    }
    suspend fun deleteRecipe(recipe: Recipe) {
        recipeDao.deleteRecipeWithIngredients(recipe)
    }

    // Catas
    suspend fun insertCata(cata: Cata) = cataDao.insertCata(cata)
    suspend fun deleteCata(cata: Cata) = cataDao.deleteCata(cata)

    // Cups
    suspend fun insertCup(cup: Cup) = cupDao.insertCup(cup)
    suspend fun getCupById(id: String) = cupDao.getCupById(id)
    suspend fun insertCupWithCata(cup: Cup, cata: Cata) = cupDao.insertCupWithCata(cup, cata)
    suspend fun deleteCup(cup: Cup) = cupDao.deleteCup(cup)

    // Experiments
    suspend fun insertExperiment(experiment: LabExperiment) = labExperimentDao.insertExperiment(experiment)
    suspend fun deleteExperiment(experiment: LabExperiment) = labExperimentDao.deleteExperiment(experiment)
}
