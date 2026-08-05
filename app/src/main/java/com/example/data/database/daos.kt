package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RatioPresetDao {
    @Query("SELECT * FROM ratio_presets")
    fun getAllPresets(): Flow<List<RatioPreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: RatioPreset)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<RatioPreset>)

    @Delete
    suspend fun deletePreset(preset: RatioPreset)
}

@Dao
interface RatioLastUsedDao {
    @Query("SELECT * FROM ratio_last_used WHERE methodName = :methodLimit LIMIT 1")
    suspend fun getLastUsedForMethod(methodLimit: String): RatioLastUsed?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLastUsed(lastUsed: RatioLastUsed)
}

@Dao
interface BrewMethodDao {
    @Query("SELECT * FROM brew_methods ORDER BY code ASC")
    fun getAllMethods(): Flow<List<BrewMethod>>

    @Query("SELECT * FROM brew_methods WHERE id = :id LIMIT 1")
    suspend fun getMethodById(id: String): BrewMethod?

    @Query("SELECT * FROM brew_methods WHERE code = :code LIMIT 1")
    suspend fun getMethodByCode(code: String): BrewMethod?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMethod(method: BrewMethod)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMethods(methods: List<BrewMethod>)
}

@Dao
interface BeanDao {
    @Query("SELECT * FROM beans ORDER BY createdAt DESC")
    fun getAllBeans(): Flow<List<Bean>>

    @Query("SELECT * FROM beans WHERE id = :id LIMIT 1")
    suspend fun getBeanById(id: String): Bean?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBean(bean: Bean)

    @Delete
    suspend fun deleteBean(bean: Bean)

    @Query("SELECT COUNT(*) FROM beans")
    fun getBeansCount(): Flow<Int>
}

@Dao
interface InstrumentDao {
    @Query("SELECT * FROM instruments ORDER BY createdAt DESC")
    fun getAllInstruments(): Flow<List<Instrument>>

    @Query("SELECT * FROM instruments WHERE type = 'GRINDER' ORDER BY createdAt DESC")
    fun getAllGrinders(): Flow<List<Instrument>>

    @Query("SELECT * FROM instruments WHERE id = :id LIMIT 1")
    suspend fun getInstrumentById(id: String): Instrument?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstrument(instrument: Instrument)

    @Delete
    suspend fun deleteInstrument(instrument: Instrument)

    @Query("SELECT COUNT(*) FROM instruments")
    fun getInstrumentsCount(): Flow<Int>
}

@Dao
interface GrinderProfileDao {
    @Query("SELECT * FROM grinder_profiles WHERE instrumentId = :instrumentId LIMIT 1")
    fun getProfileForInstrument(instrumentId: String): Flow<GrinderProfile?>

    @Query("SELECT * FROM grinder_profiles WHERE instrumentId = :instrumentId LIMIT 1")
    suspend fun getProfileForInstrumentSync(instrumentId: String): GrinderProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: GrinderProfile)
}

@Dao
interface GrinderMethodSettingDao {
    @Query("SELECT * FROM grinder_method_settings WHERE grinderProfileId = :profileId")
    fun getSettingsForProfile(profileId: String): Flow<List<GrinderMethodSetting>>

    @Query("SELECT * FROM grinder_method_settings WHERE grinderProfileId = :profileId")
    suspend fun getSettingsForProfileSync(profileId: String): List<GrinderMethodSetting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: GrinderMethodSetting)
}

@Dao
interface TechniqueDao {
    @Query("SELECT * FROM techniques ORDER BY createdAt DESC")
    fun getAllTechniques(): Flow<List<Technique>>

    @Query("SELECT * FROM techniques WHERE id = :id LIMIT 1")
    suspend fun getTechniqueById(id: String): Technique?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTechnique(technique: Technique)

    @Delete
    suspend fun deleteTechnique(technique: Technique)

    @Query("SELECT COUNT(*) FROM techniques")
    fun getTechniquesCount(): Flow<Int>
}

@Dao
interface TechniqueStepDao {
    @Query("SELECT * FROM technique_steps WHERE techniqueId = :techId ORDER BY stepNumber ASC")
    fun getStepsForTechnique(techId: String): Flow<List<TechniqueStep>>

    @Query("SELECT * FROM technique_steps WHERE techniqueId = :techId ORDER BY stepNumber ASC")
    suspend fun getStepsForTechniqueSync(techId: String): List<TechniqueStep>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: TechniqueStep)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<TechniqueStep>)

    @Query("DELETE FROM technique_steps WHERE techniqueId = :techId")
    suspend fun deleteStepsForTechnique(techId: String)
}

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    fun getAllRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    suspend fun getRecipeById(id: String): Recipe?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe)

    @Delete
    suspend fun deleteRecipe(recipe: Recipe)

    @Query("SELECT COUNT(*) FROM recipes")
    fun getRecipesCount(): Flow<Int>
}

@Dao
interface RecipeIngredientDao {
    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY orderIndex ASC")
    fun getIngredientsForRecipe(recipeId: String): Flow<List<RecipeIngredient>>

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY orderIndex ASC")
    suspend fun getIngredientsForRecipeSync(recipeId: String): List<RecipeIngredient>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<RecipeIngredient>)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsForRecipe(recipeId: String)
}

@Dao
interface RecipeStepDao {
    @Query("SELECT * FROM recipe_steps WHERE recipeId = :recipeId ORDER BY stepNumber ASC")
    fun getStepsForRecipe(recipeId: String): Flow<List<RecipeStep>>

    @Query("SELECT * FROM recipe_steps WHERE recipeId = :recipeId ORDER BY stepNumber ASC")
    suspend fun getStepsForRecipeSync(recipeId: String): List<RecipeStep>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<RecipeStep>)

    @Query("DELETE FROM recipe_steps WHERE recipeId = :recipeId")
    suspend fun deleteStepsForRecipe(recipeId: String)
}

@Dao
interface RecipeTagDao {
    @Query("SELECT * FROM recipe_tags WHERE recipeId = :recipeId")
    fun getTagsForRecipe(recipeId: String): Flow<List<RecipeTag>>

    @Query("SELECT * FROM recipe_tags WHERE recipeId = :recipeId")
    suspend fun getTagsForRecipeSync(recipeId: String): List<RecipeTag>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<RecipeTag>)

    @Query("DELETE FROM recipe_tags WHERE recipeId = :recipeId")
    suspend fun deleteTagsForRecipe(recipeId: String)
}

@Dao
interface CataDao {
    @Query("SELECT * FROM catas ORDER BY createdAt DESC")
    fun getAllCatas(): Flow<List<Cata>>

    @Query("SELECT * FROM catas WHERE cupId = :cupId LIMIT 1")
    fun getCataForCup(cupId: String): Flow<Cata?>

    @Query("SELECT * FROM catas WHERE cupId = :cupId LIMIT 1")
    suspend fun getCataForCupSync(cupId: String): Cata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCata(cata: Cata)

    @Delete
    suspend fun deleteCata(cata: Cata)
}

@Dao
interface CataFlavorNoteDao {
    @Query("SELECT * FROM cata_flavor_notes WHERE cataId = :cataId")
    fun getNotesForCata(cataId: String): Flow<List<CataFlavorNote>>

    @Query("SELECT * FROM cata_flavor_notes WHERE cataId = :cataId")
    suspend fun getNotesForCataSync(cataId: String): List<CataFlavorNote>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<CataFlavorNote>)

    @Query("DELETE FROM cata_flavor_notes WHERE cataId = :cataId")
    suspend fun deleteNotesForCata(cataId: String)
}

@Dao
interface CupDao {
    @Query("SELECT * FROM cups ORDER BY brewDate DESC")
    fun getAllCups(): Flow<List<Cup>>

    @Query("SELECT * FROM cups WHERE id = :id LIMIT 1")
    suspend fun getCupById(id: String): Cup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCup(cup: Cup)

    @Delete
    suspend fun deleteCup(cup: Cup)

    @Query("SELECT COUNT(*) FROM cups")
    fun getCupsCount(): Flow<Int>
}

@Dao
interface LabExperimentDao {
    @Query("SELECT * FROM lab_experiments ORDER BY createdAt DESC")
    fun getAllExperiments(): Flow<List<LabExperiment>>

    @Query("SELECT * FROM lab_experiments WHERE id = :id LIMIT 1")
    suspend fun getExperimentById(id: String): LabExperiment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExperiment(experiment: LabExperiment)

    @Delete
    suspend fun deleteExperiment(experiment: LabExperiment)

    @Query("SELECT COUNT(*) FROM lab_experiments")
    fun getExperimentsCount(): Flow<Int>
}
