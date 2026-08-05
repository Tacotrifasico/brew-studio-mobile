package com.example.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

fun currentIso8601(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}

@Entity(tableName = "ratio_presets")
data class RatioPreset(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val methodName: String,
    val coffeeGrams: Float,
    val ratio: Float,
    val label: String
)

@Entity(tableName = "ratio_last_used")
data class RatioLastUsed(
    @PrimaryKey val methodName: String,
    val coffeeGrams: Float,
    val ratio: Float,
    val waterMl: Int
)

@Entity(tableName = "brew_methods")
data class BrewMethod(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val code: String,
    val nameKey: String,
    val category: String = "POUR_OVER", // POUR_OVER, IMMERSION, PRESSURE, COLD, HYBRID, OTHER
    val defaultRatio: Float = 16.0f,
    val ownerUserId: String? = null,
    val schemaVersion: Int = 1
)

@Entity(tableName = "beans")
data class Bean(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val roaster: String,
    val name: String,
    val origin: String,
    val altitude: String,
    val process: String,
    val roastDate: String, // Format YYYY-MM-DD
    val firstUseDate: String, // Format YYYY-MM-DD
    val notes: String,
    val status: String = "OPEN", // CLOSED, OPEN, FINISHED
    val stockGrams: Float,
    val ownerUserId: String? = null,
    val schemaVersion: Int = 1,
    val remoteId: String? = null,
    val syncStatus: String = "SYNCED",
    val serverVersion: Long = 1L,
    val expectedVersion: Long = 1L,
    val lastSyncedAt: String? = null,
    val createdAt: String = currentIso8601(),
    val updatedAt: String = currentIso8601(),
    val migrationStatus: String = "MIGRATED" // MIGRATED, NEEDS_REVIEW
)

@Entity(tableName = "instruments")
data class Instrument(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String, // GRINDER, BREWER_METHOD, SCALE, KETTLE, FILTERS, SERVER, PRESS, ACCESSORY, OTHER
    val brand: String = "",
    val model: String = "",
    val notes: String = "",
    val ownerUserId: String? = null,
    val schemaVersion: Int = 1,
    val remoteId: String? = null,
    val syncStatus: String = "SYNCED",
    val serverVersion: Long = 1L,
    val expectedVersion: Long = 1L,
    val lastSyncedAt: String? = null,
    val createdAt: String = currentIso8601(),
    val updatedAt: String = currentIso8601(),
    val migrationStatus: String = "MIGRATED"
)

@Entity(
    tableName = "grinder_profiles",
    foreignKeys = [
        ForeignKey(
            entity = Instrument::class,
            parentColumns = ["id"],
            childColumns = ["instrumentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["instrumentId"])]
)
data class GrinderProfile(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val instrumentId: String,
    val clickRange: String = "",
    val calibrationNotes: String = ""
)

@Entity(
    tableName = "grinder_method_settings",
    foreignKeys = [
        ForeignKey(
            entity = GrinderProfile::class,
            parentColumns = ["id"],
            childColumns = ["grinderProfileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["grinderProfileId"]), Index(value = ["methodId"])]
)
data class GrinderMethodSetting(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val grinderProfileId: String,
    val methodId: String,
    val settingValue: String
)

@Entity(tableName = "techniques")
data class Technique(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val methodId: String, // UUID of BrewMethod
    val recipeId: String? = null,
    val beanId: String? = null,
    val grinderId: String? = null,
    val doseG: Float = 15.0f,
    val waterMl: Int = 240,
    val ratio: Float = 16.0f,
    val temperatureC: Int = 93,
    val executionMode: String = "GUIDED", // GUIDED, MANUAL, TIMER_ONLY, AUTOMATED
    val grindValue: Double? = 18.0,
    val grindDescription: String? = "18 Clicks",
    val grindUnit: String = "CLICKS", // CLICKS, MICRONS, SETTING_NUMERIC, DESCRIPTIVE
    val notes: String = "",
    val totalTimeSeconds: Int = 180,
    val author: String? = null,
    val description: String? = null,
    val ownerUserId: String? = null,
    val ownerDisplayName: String? = null,
    val visibility: String = "PRIVATE",
    val isShared: Boolean = false,
    val originalAuthorUserId: String? = null,
    val originalAuthorName: String? = null,
    val originalEntityId: String? = null,
    val rootEntityId: String? = null,
    val importedFromShareId: String? = null,
    val copyMode: String = "ORIGINAL",
    val originCreatedAt: String? = null,
    val legacyMethodName: String? = null,
    val schemaVersion: Int = 1,
    val remoteId: String? = null,
    val syncStatus: String = "SYNCED",
    val serverVersion: Long = 1L,
    val expectedVersion: Long = 1L,
    val lastSyncedAt: String? = null,
    val createdAt: String = currentIso8601(),
    val updatedAt: String = currentIso8601(),
    val migrationStatus: String = "MIGRATED"
)

@Entity(
    tableName = "technique_steps",
    foreignKeys = [
        ForeignKey(
            entity = Technique::class,
            parentColumns = ["id"],
            childColumns = ["techniqueId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["techniqueId"])]
)
data class TechniqueStep(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val techniqueId: String,
    val stepNumber: Int,
    val title: String,
    val durationSeconds: Int,
    val waterAddedMl: Int,
    val waterAccumulatedMl: Int,
    val intensity: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val gesture: String = "CIRCULAR_POUR", // BLOOM, CIRCULAR_POUR, CENTER_POUR, SWIRL, STIR, PRESS, WAIT
    val stepNote: String = "",
    val coverage: Float? = null,
    val flow: Float? = null,
    val secondaryAction: String? = null,
    val position: Int? = null,
    val targetWaterMl: Int? = null,
    val remoteId: String? = null,
    val syncStatus: String = "SYNCED",
    val serverVersion: Long = 1L,
    val expectedVersion: Long = 1L,
    val lastSyncedAt: String? = null,
    val createdAt: String = currentIso8601(),
    val updatedAt: String = currentIso8601()
)

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val recipeKind: String = "BLACK_COFFEE", // BLACK_COFFEE, MILK_DRINK, COLD_DRINK, SIGNATURE, DESSERT, OTHER
    val intention: String = "",
    val suggestedMethodId: String? = null, // UUID
    val isFavorite: Boolean = false,
    val ingredientsSummary: String = "",
    val stepsSummary: String = "",
    val tags: String = "",
    val ownerUserId: String? = null,
    val ownerDisplayName: String? = null,
    val visibility: String = "PRIVATE",
    val isShared: Boolean = false,
    val originalAuthorUserId: String? = null,
    val originalAuthorName: String? = null,
    val originalEntityId: String? = null,
    val rootEntityId: String? = null,
    val importedFromShareId: String? = null,
    val copyMode: String = "ORIGINAL",
    val originCreatedAt: String? = null,
    val legacyMethodName: String? = null,
    val schemaVersion: Int = 1,
    val remoteId: String? = null,
    val syncStatus: String = "SYNCED",
    val serverVersion: Long = 1L,
    val expectedVersion: Long = 1L,
    val lastSyncedAt: String? = null,
    val createdAt: String = currentIso8601(),
    val updatedAt: String = currentIso8601(),
    val migrationStatus: String = "MIGRATED"
)

@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recipeId"])]
)
data class RecipeIngredient(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val recipeId: String,
    val name: String,
    val amount: Float,
    val unit: String = "GRAMS", // GRAMS, MILLILITERS, UNITS, TEASPOONS, TABLESPOONS, OUNCES, OTHER
    val orderIndex: Int = 0
)

@Entity(
    tableName = "recipe_steps",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recipeId"])]
)
data class RecipeStep(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val recipeId: String,
    val instruction: String,
    val stepNumber: Int,
    val durationSeconds: Int? = null
)

@Entity(
    tableName = "recipe_tags",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recipeId"])]
)
data class RecipeTag(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val recipeId: String,
    val tag: String
)

@Entity(tableName = "cups")
data class Cup(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
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
    val cupLifeState: String = "FRESH", // FRESH, PEAK, DECLINING, EXHAUSTED
    val nps: Int? = null, // Range: 0..10
    val rating: Double? = null, // Range: 1.0..5.0
    val comment: String = "",
    val brewDate: String = currentIso8601(),
    val recipeSnapshotJson: String = "{}",
    val techniqueSnapshotJson: String = "{}",
    val beanSnapshotJson: String = "{}",
    val grinderSnapshotJson: String = "{}",
    val ownerUserId: String? = null,
    val schemaVersion: Int = 1,
    val remoteId: String? = null,
    val syncStatus: String = "SYNCED",
    val serverVersion: Long = 1L,
    val expectedVersion: Long = 1L,
    val lastSyncedAt: String? = null,
    val createdAt: String = currentIso8601(),
    val updatedAt: String = currentIso8601(),
    val migrationStatus: String = "MIGRATED"
)

@Entity(
    tableName = "catas",
    indices = [Index(value = ["cupId"], unique = true)]
)
data class Cata(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val cupId: String? = null,
    val recipeId: String? = null,
    val beanId: String? = null,
    val activeFlavorFamily: String? = null, // FLORAL, FRUITY, CITRIC, SWEET, CACAO, NUTTY, SPICED, GREEN
    val selectedFlavorNotesJson: String = "[]",
    val sensoryWheelDescriptorsJson: String = "[]",
    // Qualitative attributes as Enums
    val textureLevel: String? = null,
    val cleanlinessLevel: String? = null,
    val persistenceLevel: String? = null,
    val sweetnessLevel: String? = null,
    val acidityLevel: String? = null,
    val balanceLevel: String? = null,
    // SCA Scores
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
    val evaluatedAt: String = currentIso8601(),
    val ownerUserId: String? = null,
    val schemaVersion: Int = 1,
    val remoteId: String? = null,
    val syncStatus: String = "SYNCED",
    val serverVersion: Long = 1L,
    val expectedVersion: Long = 1L,
    val lastSyncedAt: String? = null,
    val createdAt: String = currentIso8601(),
    val updatedAt: String = currentIso8601(),
    val migrationStatus: String = "MIGRATED"
)

@Entity(
    tableName = "cata_flavor_notes",
    foreignKeys = [
        ForeignKey(
            entity = Cata::class,
            parentColumns = ["id"],
            childColumns = ["cataId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["cataId"])]
)
data class CataFlavorNote(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val cataId: String,
    val note: String,
    val category: String = ""
)

@Entity(tableName = "lab_experiments")
data class LabExperiment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
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
    val syncStatus: String = "SYNCED",
    val serverVersion: Long = 1L,
    val expectedVersion: Long = 1L,
    val lastSyncedAt: String? = null,
    val createdAt: String = currentIso8601(),
    val updatedAt: String = currentIso8601(),
    val migrationStatus: String = "MIGRATED"
)
