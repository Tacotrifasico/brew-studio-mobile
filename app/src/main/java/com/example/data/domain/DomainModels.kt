package com.example.data.domain

import java.time.Instant
import java.util.UUID

// CANONICAL ENUMS IN ENGLISH (PERSISTED AS UPPERCASE STRINGS)
enum class BeanStatus { CLOSED, OPEN, FINISHED }

enum class RecipeKind { BLACK_COFFEE, MILK_DRINK, COLD_DRINK, SIGNATURE, DESSERT, OTHER }

enum class IngredientUnit { GRAMS, MILLILITERS, UNITS, TEASPOONS, TABLESPOONS, OUNCES, OTHER }

enum class InstrumentType { GRINDER, BREWER_METHOD, SCALE, KETTLE, FILTERS, SERVER, PRESS, ACCESSORY, OTHER }

enum class GrindUnit { CLICKS, MICRONS, SETTING_NUMERIC, DESCRIPTIVE }

enum class ExecutionMode { GUIDED, MANUAL, TIMER_ONLY, AUTOMATED }

enum class CupLifeState { FRESH, PEAK, DECLINING, EXHAUSTED }

enum class PreparationGesture { BLOOM, CIRCULAR_POUR, CENTER_POUR, SWIRL, STIR, PRESS, WAIT }

enum class StepIntensity { LOW, MEDIUM, HIGH }

enum class SyncStatus { SYNCED, PENDING_CREATE, PENDING_UPDATE, PENDING_DELETE, CONFLICT, ERROR }

enum class MigrationStatus { MIGRATED, NEEDS_REVIEW }

enum class Visibility { PRIVATE, PUBLIC, UNLISTED }

enum class CopyMode { ORIGINAL, FORK, SHARED_COPY }

enum class BrewMethodCategory { POUR_OVER, IMMERSION, PRESSURE, COLD, HYBRID, OTHER }

enum class FlavorFamily { FLORAL, FRUITY, CITRIC, SWEET, CACAO, NUTTY, SPICED, GREEN }

enum class SensoryLevel { LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH, HIGH }

// DOMAIN MODELS

data class DomainBrewMethod(
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    val nameKey: String,
    val category: BrewMethodCategory = BrewMethodCategory.POUR_OVER,
    val defaultRatio: Float = 16.0f,
    val ownerUserId: String? = null,
    val schemaVersion: Int = 1
)

data class DomainBean(
    val id: String = UUID.randomUUID().toString(),
    val roaster: String,
    val name: String,
    val origin: String,
    val altitude: String,
    val process: String,
    val roastDateIso: String, // Format: YYYY-MM-DD
    val firstUseDateIso: String, // Format: YYYY-MM-DD
    val notes: String,
    val status: BeanStatus,
    val stockGrams: Float,
    val ownerUserId: String? = null,
    val schemaVersion: Int = 1,
    val remoteId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val serverVersion: Long = 1L,
    val expectedVersion: Long = 1L,
    val lastSyncedAtIso: String? = null,
    val createdAtIso: String = Instant.now().toString(),
    val updatedAtIso: String = Instant.now().toString(),
    val migrationStatus: MigrationStatus = MigrationStatus.MIGRATED
)

data class DomainInstrument(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: InstrumentType,
    val brand: String = "",
    val model: String = "",
    val notes: String = "",
    val grinderProfile: DomainGrinderProfile? = null,
    val ownerUserId: String? = null,
    val schemaVersion: Int = 1,
    val remoteId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val serverVersion: Long = 1L,
    val expectedVersion: Long = 1L,
    val lastSyncedAtIso: String? = null,
    val createdAtIso: String = Instant.now().toString(),
    val updatedAtIso: String = Instant.now().toString(),
    val migrationStatus: MigrationStatus = MigrationStatus.MIGRATED
)

data class DomainGrinderProfile(
    val id: String = UUID.randomUUID().toString(),
    val instrumentId: String,
    val clickRange: String = "",
    val calibrationNotes: String = "",
    val methodSettings: List<DomainGrinderMethodSetting> = emptyList()
)

data class DomainGrinderMethodSetting(
    val id: String = UUID.randomUUID().toString(),
    val grinderProfileId: String,
    val methodId: String, // UUID
    val settingValue: String
)

data class DomainTechniqueStep(
    val id: String = UUID.randomUUID().toString(),
    val techniqueId: String, // Parent technique UUID (Aggregate child)
    val stepNumber: Int,
    val title: String,
    val durationSeconds: Int,
    val waterAddedMl: Int,
    val waterAccumulatedMl: Int,
    val intensity: StepIntensity = StepIntensity.MEDIUM,
    val gesture: PreparationGesture = PreparationGesture.CIRCULAR_POUR,
    val stepNote: String = "",
    val coverage: Float? = null,
    val flow: Float? = null,
    val secondaryAction: String? = null,
    val position: Int? = null,
    val targetWaterMl: Int? = null,
    val remoteId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val serverVersion: Long = 1L,
    val expectedVersion: Long = 1L,
    val lastSyncedAtIso: String? = null,
    val createdAtIso: String = Instant.now().toString(),
    val updatedAtIso: String = Instant.now().toString()
)

data class DomainTechnique(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val methodId: String, // UUID of BrewMethod
    val recipeId: String? = null,
    val beanId: String? = null,
    val grinderId: String? = null,
    val doseG: Float = 15.0f,
    val waterMl: Int = 240,
    val ratio: Float = 16.0f,
    val temperatureC: Int = 93,
    val executionMode: ExecutionMode = ExecutionMode.GUIDED,
    val grindValue: Double? = 18.0,
    val grindDescription: String? = "18 Clicks",
    val grindUnit: GrindUnit = GrindUnit.CLICKS,
    val notes: String = "",
    val totalTimeSeconds: Int = 180,
    val author: String? = null,
    val description: String? = null,
    val steps: List<DomainTechniqueStep> = emptyList(),
    val ownerUserId: String? = null,
    val ownerDisplayName: String? = null,
    val visibility: Visibility = Visibility.PRIVATE,
    val isShared: Boolean = false,
    val originalAuthorUserId: String? = null,
    val originalAuthorName: String? = null,
    val originalEntityId: String? = null,
    val rootEntityId: String? = null,
    val importedFromShareId: String? = null,
    val copyMode: CopyMode = CopyMode.ORIGINAL,
    val originCreatedAtIso: String? = null,
    val schemaVersion: Int = 1,
    val remoteId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val serverVersion: Long = 1L,
    val expectedVersion: Long = 1L,
    val lastSyncedAtIso: String? = null,
    val createdAtIso: String = Instant.now().toString(),
    val updatedAtIso: String = Instant.now().toString(),
    val migrationStatus: MigrationStatus = MigrationStatus.MIGRATED
)

data class DomainRecipeIngredient(
    val id: String = UUID.randomUUID().toString(),
    val recipeId: String, // Parent recipe UUID (Aggregate child)
    val name: String,
    val amount: Float,
    val unit: IngredientUnit = IngredientUnit.GRAMS,
    val orderIndex: Int = 0
)

data class DomainRecipeStep(
    val id: String = UUID.randomUUID().toString(),
    val recipeId: String, // Parent recipe UUID (Aggregate child)
    val instruction: String,
    val stepNumber: Int,
    val durationSeconds: Int? = null
)

data class DomainRecipe(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val recipeKind: RecipeKind = RecipeKind.BLACK_COFFEE,
    val intention: String = "",
    val suggestedMethodId: String? = null, // UUID
    val isFavorite: Boolean = false,
    val ingredients: List<DomainRecipeIngredient> = emptyList(),
    val steps: List<DomainRecipeStep> = emptyList(),
    val tags: List<String> = emptyList(),
    val ownerUserId: String? = null,
    val ownerDisplayName: String? = null,
    val visibility: Visibility = Visibility.PRIVATE,
    val isShared: Boolean = false,
    val originalAuthorUserId: String? = null,
    val originalAuthorName: String? = null,
    val originalEntityId: String? = null,
    val rootEntityId: String? = null,
    val importedFromShareId: String? = null,
    val copyMode: CopyMode = CopyMode.ORIGINAL,
    val originCreatedAtIso: String? = null,
    val schemaVersion: Int = 1,
    val remoteId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val serverVersion: Long = 1L,
    val expectedVersion: Long = 1L,
    val lastSyncedAtIso: String? = null,
    val createdAtIso: String = Instant.now().toString(),
    val updatedAtIso: String = Instant.now().toString(),
    val migrationStatus: MigrationStatus = MigrationStatus.MIGRATED
) {
    val ingredientsSummary: String
        get() = ingredients.joinToString(", ") { "${it.name} (${it.amount}${it.unit.name.take(2)})" }

    val stepsSummary: String
        get() = steps.sortedBy { it.stepNumber }.joinToString("\n") { "${it.stepNumber}. ${it.instruction}" }
}

data class DomainCataFlavorNote(
    val note: String,
    val family: FlavorFamily
)

data class DomainCata(
    val id: String = UUID.randomUUID().toString(),
    val cupId: String? = null, // UNIQUE relationship to Cup
    val recipeId: String? = null,
    val beanId: String? = null,
    val activeFlavorFamily: FlavorFamily? = null, // Exactly one active family allowed
    val selectedFlavorNotes: List<DomainCataFlavorNote> = emptyList(),
    val sensoryWheelDescriptors: List<String> = emptyList(),
    // Qualitative attributes as Enums
    val textureLevel: SensoryLevel? = null,
    val cleanlinessLevel: SensoryLevel? = null,
    val persistenceLevel: SensoryLevel? = null,
    val sweetnessLevel: SensoryLevel? = null,
    val acidityLevel: SensoryLevel? = null,
    val balanceLevel: SensoryLevel? = null,
    // SCA Scores as Double
    val fragranceAromaScore: Double? = null,
    val flavorScore: Double? = null,
    val aftertasteScore: Double? = null,
    val acidityScore: Double? = null,
    val bodyScore: Double? = null,
    val uniformityScore: Double? = null,
    val cleanCupScore: Double? = null,
    val sweetnessScore: Double? = null,
    val balanceScore: Double? = null,
    val overallScore: Double? = null,
    val totalScaScore: Double? = null,
    val evaluatorNotes: String = "",
    val evaluatedAtIso: String = Instant.now().toString(),
    val ownerUserId: String? = null,
    val schemaVersion: Int = 1,
    val remoteId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val serverVersion: Long = 1L,
    val expectedVersion: Long = 1L,
    val lastSyncedAtIso: String? = null,
    val createdAtIso: String = Instant.now().toString(),
    val updatedAtIso: String = Instant.now().toString(),
    val migrationStatus: MigrationStatus = MigrationStatus.MIGRATED
)

data class DomainCup(
    val id: String = UUID.randomUUID().toString(),
    val recipeId: String? = null,
    val beanId: String? = null,
    val techniqueId: String? = null,
    val methodId: String? = null,
    val grinderId: String? = null,
    val executedDoseG: Float = 15.0f,
    val executedWaterMl: Int = 240,
    val executedRatio: Float = 16.0f,
    val executedTemperatureC: Int = 93,
    val executedGrindSetting: String = "18",
    val executedDurationSeconds: Int = 180,
    val beanNameSnapshot: String = "",
    val recipeNameSnapshot: String = "",
    val techniqueNameSnapshot: String = "",
    val methodNameSnapshot: String = "",
    val grinderNameSnapshot: String = "",
    val cupLifeSeconds: Int = 180,
    val cupLifeState: CupLifeState = deriveCupLifeState(cupLifeSeconds),
    val nps: Int? = null, // Range: 0..10
    val rating: Double? = null, // Range: 1.0..5.0
    val comment: String = "",
    val brewDateIso: String = Instant.now().toString(),
    val recipeSnapshotJson: String = "{}",
    val techniqueSnapshotJson: String = "{}",
    val beanSnapshotJson: String = "{}",
    val grinderSnapshotJson: String = "{}",
    val cata: DomainCata? = null,
    val ownerUserId: String? = null,
    val schemaVersion: Int = 1,
    val remoteId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val serverVersion: Long = 1L,
    val expectedVersion: Long = 1L,
    val lastSyncedAtIso: String? = null,
    val createdAtIso: String = Instant.now().toString(),
    val updatedAtIso: String = Instant.now().toString(),
    val migrationStatus: MigrationStatus = MigrationStatus.MIGRATED
)

fun deriveCupLifeState(seconds: Int): CupLifeState {
    return when {
        seconds <= 300 -> CupLifeState.FRESH
        seconds <= 900 -> CupLifeState.PEAK
        seconds <= 1800 -> CupLifeState.DECLINING
        else -> CupLifeState.EXHAUSTED
    }
}

data class DomainLabExperiment(
    val id: String = UUID.randomUUID().toString(),
    val methodId: String? = null,
    val beanId: String? = null,
    val grinderId: String? = null,
    val techniqueId: String? = null,
    val coffeeGrams: Float = 15.0f,
    val waterMl: Int = 240,
    val ratio: Float = 16.0f,
    val temperatureC: Int = 93,
    val grindSetting: String = "18",
    val beanFreshnessDays: Int? = null,
    val estimatedTimeSeconds: Int = 180,
    val actualTimeSeconds: Int? = null,
    val experimentHypothesis: String = "",
    val experimentNotes: String = "",
    val conclusionNotes: String = "",
    val resultRating: Double? = null,
    val resultCupId: String? = null,
    val ownerUserId: String? = null,
    val schemaVersion: Int = 1,
    val remoteId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val serverVersion: Long = 1L,
    val expectedVersion: Long = 1L,
    val lastSyncedAtIso: String? = null,
    val createdAtIso: String = Instant.now().toString(),
    val updatedAtIso: String = Instant.now().toString(),
    val migrationStatus: MigrationStatus = MigrationStatus.MIGRATED
)
