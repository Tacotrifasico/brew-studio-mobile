package com.example.data.remote.dtos

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BrewMethodDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "code") val code: String,
    @field:Json(name = "name_key") val nameKey: String,
    @field:Json(name = "category") val category: String = "POUR_OVER",
    @field:Json(name = "default_ratio") val defaultRatio: Float = 16.0f,
    @field:Json(name = "owner_user_id") val ownerUserId: String? = null,
    @field:Json(name = "schema_version") val schemaVersion: Int = 1
)

@JsonClass(generateAdapter = true)
data class BeanDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "roaster") val roaster: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "origin") val origin: String,
    @field:Json(name = "altitude") val altitude: String,
    @field:Json(name = "process") val process: String,
    @field:Json(name = "roast_date") val roastDate: String, // YYYY-MM-DD
    @field:Json(name = "first_use_date") val firstUseDate: String, // YYYY-MM-DD
    @field:Json(name = "notes") val notes: String,
    @field:Json(name = "status") val status: String, // CLOSED, OPEN, FINISHED
    @field:Json(name = "stock_grams") val stockGrams: Float,
    @field:Json(name = "owner_user_id") val ownerUserId: String? = null,
    @field:Json(name = "schema_version") val schemaVersion: Int = 1,
    @field:Json(name = "server_version") val serverVersion: Long = 1L,
    @field:Json(name = "expected_version") val expectedVersion: Long = 1L,
    @field:Json(name = "last_synced_at") val lastSyncedAt: String? = null,
    @field:Json(name = "created_at") val createdAt: String,
    @field:Json(name = "updated_at") val updatedAt: String,
    @field:Json(name = "migration_status") val migrationStatus: String = "MIGRATED"
)

@JsonClass(generateAdapter = true)
data class InstrumentDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "type") val type: String, // GRINDER, BREWER_METHOD, etc.
    @field:Json(name = "brand") val brand: String = "",
    @field:Json(name = "model") val model: String = "",
    @field:Json(name = "notes") val notes: String = "",
    @field:Json(name = "grinder_profile") val grinderProfile: GrinderProfileDto? = null,
    @field:Json(name = "owner_user_id") val ownerUserId: String? = null,
    @field:Json(name = "schema_version") val schemaVersion: Int = 1,
    @field:Json(name = "server_version") val serverVersion: Long = 1L,
    @field:Json(name = "expected_version") val expectedVersion: Long = 1L,
    @field:Json(name = "last_synced_at") val lastSyncedAt: String? = null,
    @field:Json(name = "created_at") val createdAt: String,
    @field:Json(name = "updated_at") val updatedAt: String,
    @field:Json(name = "migration_status") val migrationStatus: String = "MIGRATED"
)

@JsonClass(generateAdapter = true)
data class GrinderProfileDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "instrument_id") val instrumentId: String,
    @field:Json(name = "click_range") val clickRange: String = "",
    @field:Json(name = "calibration_notes") val calibrationNotes: String = "",
    @field:Json(name = "method_settings") val methodSettings: List<GrinderMethodSettingDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GrinderMethodSettingDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "grinder_profile_id") val grinderProfileId: String,
    @field:Json(name = "method_id") val methodId: String, // UUID
    @field:Json(name = "setting_value") val settingValue: String
)

@JsonClass(generateAdapter = true)
data class TechniqueStepDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "technique_id") val techniqueId: String,
    @field:Json(name = "step_number") val stepNumber: Int,
    @field:Json(name = "title") val title: String,
    @field:Json(name = "duration_seconds") val durationSeconds: Int,
    @field:Json(name = "water_added_ml") val waterAddedMl: Int,
    @field:Json(name = "water_accumulated_ml") val waterAccumulatedMl: Int,
    @field:Json(name = "intensity") val intensity: String = "MEDIUM",
    @field:Json(name = "gesture") val gesture: String = "CIRCULAR_POUR",
    @field:Json(name = "step_note") val stepNote: String = "",
    @field:Json(name = "coverage") val coverage: Float? = null,
    @field:Json(name = "flow") val flow: Float? = null,
    @field:Json(name = "secondary_action") val secondaryAction: String? = null,
    @field:Json(name = "position") val position: Int? = null,
    @field:Json(name = "target_water_ml") val targetWaterMl: Int? = null,
    @field:Json(name = "server_version") val serverVersion: Long = 1L,
    @field:Json(name = "expected_version") val expectedVersion: Long = 1L,
    @field:Json(name = "last_synced_at") val lastSyncedAt: String? = null,
    @field:Json(name = "created_at") val createdAt: String,
    @field:Json(name = "updated_at") val updatedAt: String
)

@JsonClass(generateAdapter = true)
data class TechniqueDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "method_id") val methodId: String, // UUID
    @field:Json(name = "recipe_id") val recipeId: String? = null,
    @field:Json(name = "bean_id") val beanId: String? = null,
    @field:Json(name = "grinder_id") val grinderId: String? = null,
    @field:Json(name = "dose_g") val doseG: Float = 15.0f,
    @field:Json(name = "water_ml") val waterMl: Int = 240,
    @field:Json(name = "ratio") val ratio: Float = 16.0f,
    @field:Json(name = "temperature_c") val temperatureC: Int = 93,
    @field:Json(name = "execution_mode") val executionMode: String = "GUIDED",
    @field:Json(name = "grind_value") val grindValue: Double? = 18.0,
    @field:Json(name = "grind_description") val grindDescription: String? = "18 Clicks",
    @field:Json(name = "grind_unit") val grindUnit: String = "CLICKS",
    @field:Json(name = "notes") val notes: String = "",
    @field:Json(name = "total_time_seconds") val totalTimeSeconds: Int = 180,
    @field:Json(name = "author") val author: String? = null,
    @field:Json(name = "description") val description: String? = null,
    @field:Json(name = "steps") val steps: List<TechniqueStepDto> = emptyList(),
    @field:Json(name = "owner_user_id") val ownerUserId: String? = null,
    @field:Json(name = "owner_display_name") val ownerDisplayName: String? = null,
    @field:Json(name = "visibility") val visibility: String = "PRIVATE",
    @field:Json(name = "is_shared") val isShared: Boolean = false,
    @field:Json(name = "original_author_user_id") val originalAuthorUserId: String? = null,
    @field:Json(name = "original_author_name") val originalAuthorName: String? = null,
    @field:Json(name = "original_entity_id") val originalEntityId: String? = null,
    @field:Json(name = "root_entity_id") val rootEntityId: String? = null,
    @field:Json(name = "imported_from_share_id") val importedFromShareId: String? = null,
    @field:Json(name = "copy_mode") val copyMode: String = "ORIGINAL",
    @field:Json(name = "origin_created_at") val originCreatedAt: String? = null,
    @field:Json(name = "schema_version") val schemaVersion: Int = 1,
    @field:Json(name = "server_version") val serverVersion: Long = 1L,
    @field:Json(name = "expected_version") val expectedVersion: Long = 1L,
    @field:Json(name = "last_synced_at") val lastSyncedAt: String? = null,
    @field:Json(name = "created_at") val createdAt: String,
    @field:Json(name = "updated_at") val updatedAt: String,
    @field:Json(name = "migration_status") val migrationStatus: String = "MIGRATED"
)

@JsonClass(generateAdapter = true)
data class RecipeIngredientDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "recipe_id") val recipeId: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "amount") val amount: Float,
    @field:Json(name = "unit") val unit: String = "GRAMS",
    @field:Json(name = "order_index") val orderIndex: Int = 0
)

@JsonClass(generateAdapter = true)
data class RecipeStepDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "recipe_id") val recipeId: String,
    @field:Json(name = "instruction") val instruction: String,
    @field:Json(name = "step_number") val stepNumber: Int,
    @field:Json(name = "duration_seconds") val durationSeconds: Int? = null
)

@JsonClass(generateAdapter = true)
data class RecipeDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "recipe_kind") val recipeKind: String = "BLACK_COFFEE",
    @field:Json(name = "intention") val intention: String = "",
    @field:Json(name = "suggested_method_id") val suggestedMethodId: String? = null,
    @field:Json(name = "is_favorite") val isFavorite: Boolean = false,
    @field:Json(name = "ingredients") val ingredients: List<RecipeIngredientDto> = emptyList(),
    @field:Json(name = "steps") val steps: List<RecipeStepDto> = emptyList(),
    @field:Json(name = "tags") val tags: List<String> = emptyList(),
    @field:Json(name = "owner_user_id") val ownerUserId: String? = null,
    @field:Json(name = "owner_display_name") val ownerDisplayName: String? = null,
    @field:Json(name = "visibility") val visibility: String = "PRIVATE",
    @field:Json(name = "is_shared") val isShared: Boolean = false,
    @field:Json(name = "original_author_user_id") val originalAuthorUserId: String? = null,
    @field:Json(name = "original_author_name") val originalAuthorName: String? = null,
    @field:Json(name = "original_entity_id") val originalEntityId: String? = null,
    @field:Json(name = "root_entity_id") val rootEntityId: String? = null,
    @field:Json(name = "imported_from_share_id") val importedFromShareId: String? = null,
    @field:Json(name = "copy_mode") val copyMode: String = "ORIGINAL",
    @field:Json(name = "origin_created_at") val originCreatedAt: String? = null,
    @field:Json(name = "schema_version") val schemaVersion: Int = 1,
    @field:Json(name = "server_version") val serverVersion: Long = 1L,
    @field:Json(name = "expected_version") val expectedVersion: Long = 1L,
    @field:Json(name = "last_synced_at") val lastSyncedAt: String? = null,
    @field:Json(name = "created_at") val createdAt: String,
    @field:Json(name = "updated_at") val updatedAt: String,
    @field:Json(name = "migration_status") val migrationStatus: String = "MIGRATED"
)

@JsonClass(generateAdapter = true)
data class CupDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "recipe_id") val recipeId: String? = null,
    @field:Json(name = "bean_id") val beanId: String? = null,
    @field:Json(name = "technique_id") val techniqueId: String? = null,
    @field:Json(name = "method_id") val methodId: String? = null,
    @field:Json(name = "grinder_id") val grinderId: String? = null,
    @field:Json(name = "executed_dose_g") val executedDoseG: Float = 15.0f,
    @field:Json(name = "executed_water_ml") val executedWaterMl: Int = 240,
    @field:Json(name = "executed_ratio") val executedRatio: Float = 16.0f,
    @field:Json(name = "executed_temperature_c") val executedTemperatureC: Int = 93,
    @field:Json(name = "executed_grind_setting") val executedGrindSetting: String = "18",
    @field:Json(name = "executed_duration_seconds") val executedDurationSeconds: Int = 180,
    @field:Json(name = "bean_name_snapshot") val beanNameSnapshot: String = "",
    @field:Json(name = "recipe_name_snapshot") val recipeNameSnapshot: String = "",
    @field:Json(name = "technique_name_snapshot") val techniqueNameSnapshot: String = "",
    @field:Json(name = "method_name_snapshot") val methodNameSnapshot: String = "",
    @field:Json(name = "grinder_name_snapshot") val grinderNameSnapshot: String = "",
    @field:Json(name = "cup_life_seconds") val cupLifeSeconds: Int = 180,
    @field:Json(name = "cup_life_state") val cupLifeState: String = "FRESH",
    @field:Json(name = "nps") val nps: Int? = null,
    @field:Json(name = "rating") val rating: Double? = null,
    @field:Json(name = "comment") val comment: String = "",
    @field:Json(name = "brew_date") val brewDate: String,
    @field:Json(name = "cata") val cata: CataDto? = null,
    @field:Json(name = "owner_user_id") val ownerUserId: String? = null,
    @field:Json(name = "schema_version") val schemaVersion: Int = 1,
    @field:Json(name = "server_version") val serverVersion: Long = 1L,
    @field:Json(name = "expected_version") val expectedVersion: Long = 1L,
    @field:Json(name = "last_synced_at") val lastSyncedAt: String? = null,
    @field:Json(name = "created_at") val createdAt: String,
    @field:Json(name = "updated_at") val updatedAt: String,
    @field:Json(name = "migration_status") val migrationStatus: String = "MIGRATED"
)

@JsonClass(generateAdapter = true)
data class CataFlavorNoteDto(
    @field:Json(name = "note") val note: String,
    @field:Json(name = "family") val family: String
)

@JsonClass(generateAdapter = true)
data class CataDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "cup_id") val cupId: String? = null,
    @field:Json(name = "recipe_id") val recipeId: String? = null,
    @field:Json(name = "bean_id") val beanId: String? = null,
    @field:Json(name = "active_flavor_family") val activeFlavorFamily: String? = null, // FLORAL, FRUITY, CITRIC, SWEET, CACAO, NUTTY, SPICED, GREEN
    @field:Json(name = "selected_flavor_notes") val selectedFlavorNotes: List<CataFlavorNoteDto> = emptyList(),
    @field:Json(name = "sensory_wheel_descriptors") val sensoryWheelDescriptors: List<String> = emptyList(),
    // Qualitative attributes as Enums (LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH, HIGH)
    @field:Json(name = "texture_level") val textureLevel: String? = null,
    @field:Json(name = "cleanliness_level") val cleanlinessLevel: String? = null,
    @field:Json(name = "persistence_level") val persistenceLevel: String? = null,
    @field:Json(name = "sweetness_level") val sweetnessLevel: String? = null,
    @field:Json(name = "acidity_level") val acidityLevel: String? = null,
    @field:Json(name = "balance_level") val balanceLevel: String? = null,
    // SCA Scores
    @field:Json(name = "fragrance_aroma_score") val fragranceAromaScore: Double? = null,
    @field:Json(name = "flavor_score") val flavorScore: Double? = null,
    @field:Json(name = "aftertaste_score") val aftertasteScore: Double? = null,
    @field:Json(name = "acidity_score") val acidityScore: Double? = null,
    @field:Json(name = "body_score") val bodyScore: Double? = null,
    @field:Json(name = "uniformity_score") val uniformityScore: Double? = null,
    @field:Json(name = "clean_cup_score") val cleanCupScore: Double? = null,
    @field:Json(name = "sweetness_score") val sweetnessScore: Double? = null,
    @field:Json(name = "balance_score") val balanceScore: Double? = null,
    @field:Json(name = "overall_score") val overallScore: Double? = null,
    @field:Json(name = "total_sca_score") val totalScaScore: Double? = null,
    @field:Json(name = "evaluator_notes") val evaluatorNotes: String = "",
    @field:Json(name = "evaluated_at") val evaluatedAt: String,
    @field:Json(name = "owner_user_id") val ownerUserId: String? = null,
    @field:Json(name = "schema_version") val schemaVersion: Int = 1,
    @field:Json(name = "server_version") val serverVersion: Long = 1L,
    @field:Json(name = "expected_version") val expectedVersion: Long = 1L,
    @field:Json(name = "last_synced_at") val lastSyncedAt: String? = null,
    @field:Json(name = "created_at") val createdAt: String,
    @field:Json(name = "updated_at") val updatedAt: String,
    @field:Json(name = "migration_status") val migrationStatus: String = "MIGRATED"
)

@JsonClass(generateAdapter = true)
data class LabExperimentDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "method_id") val methodId: String? = null,
    @field:Json(name = "bean_id") val beanId: String? = null,
    @field:Json(name = "grinder_id") val grinderId: String? = null,
    @field:Json(name = "technique_id") val techniqueId: String? = null,
    @field:Json(name = "coffee_grams") val coffeeGrams: Float = 15.0f,
    @field:Json(name = "water_ml") val waterMl: Int = 240,
    @field:Json(name = "ratio") val ratio: Float = 16.0f,
    @field:Json(name = "temperature_c") val temperatureC: Int = 93,
    @field:Json(name = "grind_setting") val grindSetting: String = "18",
    @field:Json(name = "bean_freshness_days") val beanFreshnessDays: Int? = null,
    @field:Json(name = "estimated_time_seconds") val estimatedTimeSeconds: Int = 180,
    @field:Json(name = "actual_time_seconds") val actualTimeSeconds: Int? = null,
    @field:Json(name = "experiment_hypothesis") val experimentHypothesis: String = "",
    @field:Json(name = "experiment_notes") val experimentNotes: String = "",
    @field:Json(name = "conclusion_notes") val conclusionNotes: String = "",
    @field:Json(name = "result_rating") val resultRating: Double? = null,
    @field:Json(name = "result_cup_id") val resultCupId: String? = null,
    @field:Json(name = "owner_user_id") val ownerUserId: String? = null,
    @field:Json(name = "schema_version") val schemaVersion: Int = 1,
    @field:Json(name = "server_version") val serverVersion: Long = 1L,
    @field:Json(name = "expected_version") val expectedVersion: Long = 1L,
    @field:Json(name = "last_synced_at") val lastSyncedAt: String? = null,
    @field:Json(name = "created_at") val createdAt: String,
    @field:Json(name = "updated_at") val updatedAt: String,
    @field:Json(name = "migration_status") val migrationStatus: String = "MIGRATED"
)
