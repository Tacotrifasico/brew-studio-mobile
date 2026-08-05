package com.example.data.mappers

import com.example.data.database.*
import com.example.data.domain.*
import com.example.data.remote.dtos.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object EntityMappers {

    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val stringListAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )
    private val flavorNotesAdapter = moshi.adapter<List<CataFlavorNoteDto>>(
        Types.newParameterizedType(List::class.java, CataFlavorNoteDto::class.java)
    )

    // BREW METHOD MAPPERS
    fun BrewMethod.toDomain(): DomainBrewMethod {
        val cat = try {
            BrewMethodCategory.valueOf(category)
        } catch (e: Exception) {
            BrewMethodCategory.POUR_OVER
        }
        return DomainBrewMethod(
            id = id,
            code = code,
            nameKey = nameKey,
            category = cat,
            defaultRatio = defaultRatio,
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion
        )
    }

    fun DomainBrewMethod.toEntity(): BrewMethod {
        return BrewMethod(
            id = id,
            code = code,
            nameKey = nameKey,
            category = category.name,
            defaultRatio = defaultRatio,
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion
        )
    }

    fun DomainBrewMethod.toDto(): BrewMethodDto {
        return BrewMethodDto(
            id = id,
            code = code,
            nameKey = nameKey,
            category = category.name,
            defaultRatio = defaultRatio,
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion
        )
    }

    fun BrewMethodDto.toDomain(): DomainBrewMethod {
        val cat = try {
            BrewMethodCategory.valueOf(category)
        } catch (e: Exception) {
            BrewMethodCategory.POUR_OVER
        }
        return DomainBrewMethod(
            id = id,
            code = code,
            nameKey = nameKey,
            category = cat,
            defaultRatio = defaultRatio,
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion
        )
    }

    // BEAN MAPPERS
    fun Bean.toDomain(): DomainBean {
        val mappedStatus = try {
            when (status.uppercase()) {
                "ABIERTO", "OPEN" -> BeanStatus.OPEN
                "CERRADO", "CLOSED" -> BeanStatus.CLOSED
                "TERMINADO", "FINISHED" -> BeanStatus.FINISHED
                else -> BeanStatus.OPEN
            }
        } catch (e: Exception) {
            BeanStatus.OPEN
        }

        val mappedMigrationStatus = try {
            MigrationStatus.valueOf(migrationStatus)
        } catch (e: Exception) {
            MigrationStatus.NEEDS_REVIEW
        }

        return DomainBean(
            id = id,
            roaster = roaster,
            name = name,
            origin = origin,
            altitude = altitude,
            process = process,
            roastDateIso = roastDate,
            firstUseDateIso = firstUseDate,
            notes = notes,
            status = mappedStatus,
            stockGrams = stockGrams,
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            remoteId = remoteId,
            syncStatus = try { SyncStatus.valueOf(syncStatus) } catch (e: Exception) { SyncStatus.SYNCED },
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAtIso = lastSyncedAt,
            createdAtIso = createdAt,
            updatedAtIso = updatedAt,
            migrationStatus = mappedMigrationStatus
        )
    }

    fun DomainBean.toEntity(): Bean {
        return Bean(
            id = id,
            roaster = roaster,
            name = name,
            origin = origin,
            altitude = altitude,
            process = process,
            roastDate = roastDateIso,
            firstUseDate = firstUseDateIso,
            notes = notes,
            status = status.name,
            stockGrams = stockGrams,
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            remoteId = remoteId,
            syncStatus = syncStatus.name,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAt = lastSyncedAtIso,
            createdAt = createdAtIso,
            updatedAt = updatedAtIso,
            migrationStatus = migrationStatus.name
        )
    }

    fun DomainBean.toDto(): BeanDto {
        return BeanDto(
            id = id,
            roaster = roaster,
            name = name,
            origin = origin,
            altitude = altitude,
            process = process,
            roastDate = roastDateIso,
            firstUseDate = firstUseDateIso,
            notes = notes,
            status = status.name,
            stockGrams = stockGrams,
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAt = lastSyncedAtIso,
            createdAt = createdAtIso,
            updatedAt = updatedAtIso,
            migrationStatus = migrationStatus.name
        )
    }

    fun BeanDto.toDomain(): DomainBean {
        return DomainBean(
            id = id,
            roaster = roaster,
            name = name,
            origin = origin,
            altitude = altitude,
            process = process,
            roastDateIso = roastDate,
            firstUseDateIso = firstUseDate,
            notes = notes,
            status = try { BeanStatus.valueOf(status) } catch (e: Exception) { BeanStatus.OPEN },
            stockGrams = stockGrams,
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAtIso = lastSyncedAt,
            createdAtIso = createdAt,
            updatedAtIso = updatedAt,
            migrationStatus = try { MigrationStatus.valueOf(migrationStatus) } catch (e: Exception) { MigrationStatus.MIGRATED }
        )
    }

    // INSTRUMENT MAPPERS
    fun Instrument.toDomain(profile: DomainGrinderProfile? = null): DomainInstrument {
        val mappedType = try {
            when (type.uppercase()) {
                "MOLINO", "GRINDER" -> InstrumentType.GRINDER
                "MÉTODO", "BREWER_METHOD" -> InstrumentType.BREWER_METHOD
                "BÁSCULA", "SCALE" -> InstrumentType.SCALE
                "TETERA", "KETTLE" -> InstrumentType.KETTLE
                "FILTROS", "FILTERS" -> InstrumentType.FILTERS
                "SERVIDOR", "SERVER" -> InstrumentType.SERVER
                "PRENSA", "PRESS" -> InstrumentType.PRESS
                "ACCESORIOS", "ACCESSORY" -> InstrumentType.ACCESSORY
                else -> InstrumentType.OTHER
            }
        } catch (e: Exception) {
            InstrumentType.OTHER
        }

        return DomainInstrument(
            id = id,
            name = name,
            type = mappedType,
            brand = brand,
            model = model,
            notes = notes,
            grinderProfile = profile,
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            remoteId = remoteId,
            syncStatus = try { SyncStatus.valueOf(syncStatus) } catch (e: Exception) { SyncStatus.SYNCED },
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAtIso = lastSyncedAt,
            createdAtIso = createdAt,
            updatedAtIso = updatedAt,
            migrationStatus = try { MigrationStatus.valueOf(migrationStatus) } catch (e: Exception) { MigrationStatus.NEEDS_REVIEW }
        )
    }

    fun DomainInstrument.toEntity(): Instrument {
        return Instrument(
            id = id,
            name = name,
            type = type.name,
            brand = brand,
            model = model,
            notes = notes,
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            remoteId = remoteId,
            syncStatus = syncStatus.name,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAt = lastSyncedAtIso,
            createdAt = createdAtIso,
            updatedAt = updatedAtIso,
            migrationStatus = migrationStatus.name
        )
    }

    fun DomainInstrument.toDto(): InstrumentDto {
        return InstrumentDto(
            id = id,
            name = name,
            type = type.name,
            brand = brand,
            model = model,
            notes = notes,
            grinderProfile = grinderProfile?.let { p ->
                GrinderProfileDto(
                    id = p.id,
                    instrumentId = p.instrumentId,
                    clickRange = p.clickRange,
                    calibrationNotes = p.calibrationNotes,
                    methodSettings = p.methodSettings.map { s ->
                        GrinderMethodSettingDto(
                            id = s.id,
                            grinderProfileId = s.grinderProfileId,
                            methodId = s.methodId,
                            settingValue = s.settingValue
                        )
                    }
                )
            },
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAt = lastSyncedAtIso,
            createdAt = createdAtIso,
            updatedAt = updatedAtIso,
            migrationStatus = migrationStatus.name
        )
    }

    fun InstrumentDto.toDomain(): DomainInstrument {
        return DomainInstrument(
            id = id,
            name = name,
            type = try { InstrumentType.valueOf(type) } catch (e: Exception) { InstrumentType.OTHER },
            brand = brand,
            model = model,
            notes = notes,
            grinderProfile = grinderProfile?.let { p ->
                DomainGrinderProfile(
                    id = p.id,
                    instrumentId = p.instrumentId,
                    clickRange = p.clickRange,
                    calibrationNotes = p.calibrationNotes,
                    methodSettings = p.methodSettings.map { s ->
                        DomainGrinderMethodSetting(
                            id = s.id,
                            grinderProfileId = s.grinderProfileId,
                            methodId = s.methodId,
                            settingValue = s.settingValue
                        )
                    }
                )
            },
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAtIso = lastSyncedAt,
            createdAtIso = createdAt,
            updatedAtIso = updatedAt,
            migrationStatus = try { MigrationStatus.valueOf(migrationStatus) } catch (e: Exception) { MigrationStatus.MIGRATED }
        )
    }

    // TECHNIQUE MAPPERS
    fun Technique.toDomain(stepsList: List<TechniqueStep> = emptyList()): DomainTechnique {
        return DomainTechnique(
            id = id,
            name = name,
            methodId = methodId,
            recipeId = recipeId,
            beanId = beanId,
            grinderId = grinderId,
            doseG = doseG,
            waterMl = waterMl,
            ratio = ratio,
            temperatureC = temperatureC,
            executionMode = try { ExecutionMode.valueOf(executionMode) } catch (e: Exception) { ExecutionMode.GUIDED },
            grindValue = grindValue,
            grindDescription = grindDescription,
            grindUnit = try { GrindUnit.valueOf(grindUnit) } catch (e: Exception) { GrindUnit.CLICKS },
            notes = notes,
            totalTimeSeconds = totalTimeSeconds,
            author = author,
            description = description,
            steps = stepsList.map { st ->
                DomainTechniqueStep(
                    id = st.id,
                    techniqueId = st.techniqueId,
                    stepNumber = st.stepNumber,
                    title = st.title,
                    durationSeconds = st.durationSeconds,
                    waterAddedMl = st.waterAddedMl,
                    waterAccumulatedMl = st.waterAccumulatedMl,
                    intensity = try { StepIntensity.valueOf(st.intensity) } catch (e: Exception) { StepIntensity.MEDIUM },
                    gesture = try { PreparationGesture.valueOf(st.gesture) } catch (e: Exception) { PreparationGesture.CIRCULAR_POUR },
                    stepNote = st.stepNote,
                    coverage = st.coverage,
                    flow = st.flow,
                    secondaryAction = st.secondaryAction,
                    position = st.position,
                    targetWaterMl = st.targetWaterMl,
                    remoteId = st.remoteId,
                    syncStatus = try { SyncStatus.valueOf(st.syncStatus) } catch (e: Exception) { SyncStatus.SYNCED },
                    serverVersion = st.serverVersion,
                    expectedVersion = st.expectedVersion,
                    lastSyncedAtIso = st.lastSyncedAt,
                    createdAtIso = st.createdAt,
                    updatedAtIso = st.updatedAt
                )
            },
            ownerUserId = ownerUserId,
            ownerDisplayName = ownerDisplayName,
            visibility = try { Visibility.valueOf(visibility) } catch (e: Exception) { Visibility.PRIVATE },
            isShared = isShared,
            originalAuthorUserId = originalAuthorUserId,
            originalAuthorName = originalAuthorName,
            originalEntityId = originalEntityId,
            rootEntityId = rootEntityId,
            importedFromShareId = importedFromShareId,
            copyMode = try { CopyMode.valueOf(copyMode) } catch (e: Exception) { CopyMode.ORIGINAL },
            originCreatedAtIso = originCreatedAt,
            schemaVersion = schemaVersion,
            remoteId = remoteId,
            syncStatus = try { SyncStatus.valueOf(syncStatus) } catch (e: Exception) { SyncStatus.SYNCED },
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAtIso = lastSyncedAt,
            createdAtIso = createdAt,
            updatedAtIso = updatedAt,
            migrationStatus = try { MigrationStatus.valueOf(migrationStatus) } catch (e: Exception) { MigrationStatus.MIGRATED }
        )
    }

    fun DomainTechnique.toEntity(): Technique {
        return Technique(
            id = id,
            name = name,
            methodId = methodId,
            recipeId = recipeId,
            beanId = beanId,
            grinderId = grinderId,
            doseG = doseG,
            waterMl = waterMl,
            ratio = ratio,
            temperatureC = temperatureC,
            executionMode = executionMode.name,
            grindValue = grindValue,
            grindDescription = grindDescription,
            grindUnit = grindUnit.name,
            notes = notes,
            totalTimeSeconds = totalTimeSeconds,
            author = author,
            description = description,
            ownerUserId = ownerUserId,
            ownerDisplayName = ownerDisplayName,
            visibility = visibility.name,
            isShared = isShared,
            originalAuthorUserId = originalAuthorUserId,
            originalAuthorName = originalAuthorName,
            originalEntityId = originalEntityId,
            rootEntityId = rootEntityId,
            importedFromShareId = importedFromShareId,
            copyMode = copyMode.name,
            originCreatedAt = originCreatedAtIso,
            schemaVersion = schemaVersion,
            remoteId = remoteId,
            syncStatus = syncStatus.name,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAt = lastSyncedAtIso,
            createdAt = createdAtIso,
            updatedAt = updatedAtIso,
            migrationStatus = migrationStatus.name
        )
    }

    fun DomainTechnique.toDto(): TechniqueDto {
        return TechniqueDto(
            id = id,
            name = name,
            methodId = methodId,
            recipeId = recipeId,
            beanId = beanId,
            grinderId = grinderId,
            doseG = doseG,
            waterMl = waterMl,
            ratio = ratio,
            temperatureC = temperatureC,
            executionMode = executionMode.name,
            grindValue = grindValue,
            grindDescription = grindDescription,
            grindUnit = grindUnit.name,
            notes = notes,
            totalTimeSeconds = totalTimeSeconds,
            author = author,
            description = description,
            steps = steps.map { st ->
                TechniqueStepDto(
                    id = st.id,
                    techniqueId = st.techniqueId,
                    stepNumber = st.stepNumber,
                    title = st.title,
                    durationSeconds = st.durationSeconds,
                    waterAddedMl = st.waterAddedMl,
                    waterAccumulatedMl = st.waterAccumulatedMl,
                    intensity = st.intensity.name,
                    gesture = st.gesture.name,
                    stepNote = st.stepNote,
                    coverage = st.coverage,
                    flow = st.flow,
                    secondaryAction = st.secondaryAction,
                    position = st.position,
                    targetWaterMl = st.targetWaterMl,
                    serverVersion = st.serverVersion,
                    expectedVersion = st.expectedVersion,
                    lastSyncedAt = st.lastSyncedAtIso,
                    createdAt = st.createdAtIso,
                    updatedAt = st.updatedAtIso
                )
            },
            ownerUserId = ownerUserId,
            ownerDisplayName = ownerDisplayName,
            visibility = visibility.name,
            isShared = isShared,
            originalAuthorUserId = originalAuthorUserId,
            originalAuthorName = originalAuthorName,
            originalEntityId = originalEntityId,
            rootEntityId = rootEntityId,
            importedFromShareId = importedFromShareId,
            copyMode = copyMode.name,
            originCreatedAt = originCreatedAtIso,
            schemaVersion = schemaVersion,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAt = lastSyncedAtIso,
            createdAt = createdAtIso,
            updatedAt = updatedAtIso,
            migrationStatus = migrationStatus.name
        )
    }

    fun TechniqueDto.toDomain(): DomainTechnique {
        return DomainTechnique(
            id = id,
            name = name,
            methodId = methodId,
            recipeId = recipeId,
            beanId = beanId,
            grinderId = grinderId,
            doseG = doseG,
            waterMl = waterMl,
            ratio = ratio,
            temperatureC = temperatureC,
            executionMode = try { ExecutionMode.valueOf(executionMode) } catch (e: Exception) { ExecutionMode.GUIDED },
            grindValue = grindValue,
            grindDescription = grindDescription,
            grindUnit = try { GrindUnit.valueOf(grindUnit) } catch (e: Exception) { GrindUnit.CLICKS },
            notes = notes,
            totalTimeSeconds = totalTimeSeconds,
            author = author,
            description = description,
            steps = steps.map { st ->
                DomainTechniqueStep(
                    id = st.id,
                    techniqueId = st.techniqueId,
                    stepNumber = st.stepNumber,
                    title = st.title,
                    durationSeconds = st.durationSeconds,
                    waterAddedMl = st.waterAddedMl,
                    waterAccumulatedMl = st.waterAccumulatedMl,
                    intensity = try { StepIntensity.valueOf(st.intensity) } catch (e: Exception) { StepIntensity.MEDIUM },
                    gesture = try { PreparationGesture.valueOf(st.gesture) } catch (e: Exception) { PreparationGesture.CIRCULAR_POUR },
                    stepNote = st.stepNote,
                    coverage = st.coverage,
                    flow = st.flow,
                    secondaryAction = st.secondaryAction,
                    position = st.position,
                    targetWaterMl = st.targetWaterMl,
                    serverVersion = st.serverVersion,
                    expectedVersion = st.expectedVersion,
                    lastSyncedAtIso = st.lastSyncedAt,
                    createdAtIso = st.createdAt,
                    updatedAtIso = st.updatedAt
                )
            },
            ownerUserId = ownerUserId,
            ownerDisplayName = ownerDisplayName,
            visibility = try { Visibility.valueOf(visibility) } catch (e: Exception) { Visibility.PRIVATE },
            isShared = isShared,
            originalAuthorUserId = originalAuthorUserId,
            originalAuthorName = originalAuthorName,
            originalEntityId = originalEntityId,
            rootEntityId = rootEntityId,
            importedFromShareId = importedFromShareId,
            copyMode = try { CopyMode.valueOf(copyMode) } catch (e: Exception) { CopyMode.ORIGINAL },
            originCreatedAtIso = originCreatedAt,
            schemaVersion = schemaVersion,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAtIso = lastSyncedAt,
            createdAtIso = createdAt,
            updatedAtIso = updatedAt,
            migrationStatus = try { MigrationStatus.valueOf(migrationStatus) } catch (e: Exception) { MigrationStatus.MIGRATED }
        )
    }

    // RECIPE MAPPERS
    fun Recipe.toDomain(
        ingredientsList: List<RecipeIngredient> = emptyList(),
        stepsList: List<RecipeStep> = emptyList(),
        tagsList: List<RecipeTag> = emptyList()
    ): DomainRecipe {
        val mappedKind = try {
            RecipeKind.valueOf(recipeKind)
        } catch (e: Exception) {
            RecipeKind.BLACK_COFFEE
        }

        return DomainRecipe(
            id = id,
            name = name,
            recipeKind = mappedKind,
            intention = intention,
            suggestedMethodId = suggestedMethodId,
            isFavorite = isFavorite,
            ingredients = ingredientsList.map { ing ->
                DomainRecipeIngredient(
                    id = ing.id,
                    recipeId = ing.recipeId,
                    name = ing.name,
                    amount = ing.amount,
                    unit = try { IngredientUnit.valueOf(ing.unit) } catch (e: Exception) { IngredientUnit.GRAMS },
                    orderIndex = ing.orderIndex
                )
            },
            steps = stepsList.map { st ->
                DomainRecipeStep(
                    id = st.id,
                    recipeId = st.recipeId,
                    instruction = st.instruction,
                    stepNumber = st.stepNumber,
                    durationSeconds = st.durationSeconds
                )
            },
            tags = tagsList.map { it.tag },
            ownerUserId = ownerUserId,
            ownerDisplayName = ownerDisplayName,
            visibility = try { Visibility.valueOf(visibility) } catch (e: Exception) { Visibility.PRIVATE },
            isShared = isShared,
            originalAuthorUserId = originalAuthorUserId,
            originalAuthorName = originalAuthorName,
            originalEntityId = originalEntityId,
            rootEntityId = rootEntityId,
            importedFromShareId = importedFromShareId,
            copyMode = try { CopyMode.valueOf(copyMode) } catch (e: Exception) { CopyMode.ORIGINAL },
            originCreatedAtIso = originCreatedAt,
            schemaVersion = schemaVersion,
            remoteId = remoteId,
            syncStatus = try { SyncStatus.valueOf(syncStatus) } catch (e: Exception) { SyncStatus.SYNCED },
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAtIso = lastSyncedAt,
            createdAtIso = createdAt,
            updatedAtIso = updatedAt,
            migrationStatus = try { MigrationStatus.valueOf(migrationStatus) } catch (e: Exception) { MigrationStatus.NEEDS_REVIEW }
        )
    }

    fun DomainRecipe.toEntity(): Recipe {
        return Recipe(
            id = id,
            name = name,
            recipeKind = recipeKind.name,
            intention = intention,
            suggestedMethodId = suggestedMethodId,
            isFavorite = isFavorite,
            ingredientsSummary = ingredientsSummary,
            stepsSummary = stepsSummary,
            tags = tags.joinToString(","),
            ownerUserId = ownerUserId,
            ownerDisplayName = ownerDisplayName,
            visibility = visibility.name,
            isShared = isShared,
            originalAuthorUserId = originalAuthorUserId,
            originalAuthorName = originalAuthorName,
            originalEntityId = originalEntityId,
            rootEntityId = rootEntityId,
            importedFromShareId = importedFromShareId,
            copyMode = copyMode.name,
            originCreatedAt = originCreatedAtIso,
            schemaVersion = schemaVersion,
            remoteId = remoteId,
            syncStatus = syncStatus.name,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAt = lastSyncedAtIso,
            createdAt = createdAtIso,
            updatedAt = updatedAtIso,
            migrationStatus = migrationStatus.name
        )
    }

    fun DomainRecipe.toDto(): RecipeDto {
        return RecipeDto(
            id = id,
            name = name,
            recipeKind = recipeKind.name,
            intention = intention,
            suggestedMethodId = suggestedMethodId,
            isFavorite = isFavorite,
            ingredients = ingredients.map { ing ->
                RecipeIngredientDto(
                    id = ing.id,
                    recipeId = ing.recipeId,
                    name = ing.name,
                    amount = ing.amount,
                    unit = ing.unit.name,
                    orderIndex = ing.orderIndex
                )
            },
            steps = steps.map { st ->
                RecipeStepDto(
                    id = st.id,
                    recipeId = st.recipeId,
                    instruction = st.instruction,
                    stepNumber = st.stepNumber,
                    durationSeconds = st.durationSeconds
                )
            },
            tags = tags,
            ownerUserId = ownerUserId,
            ownerDisplayName = ownerDisplayName,
            visibility = visibility.name,
            isShared = isShared,
            originalAuthorUserId = originalAuthorUserId,
            originalAuthorName = originalAuthorName,
            originalEntityId = originalEntityId,
            rootEntityId = rootEntityId,
            importedFromShareId = importedFromShareId,
            copyMode = copyMode.name,
            originCreatedAt = originCreatedAtIso,
            schemaVersion = schemaVersion,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAt = lastSyncedAtIso,
            createdAt = createdAtIso,
            updatedAt = updatedAtIso,
            migrationStatus = migrationStatus.name
        )
    }

    fun RecipeDto.toDomain(): DomainRecipe {
        return DomainRecipe(
            id = id,
            name = name,
            recipeKind = try { RecipeKind.valueOf(recipeKind) } catch (e: Exception) { RecipeKind.BLACK_COFFEE },
            intention = intention,
            suggestedMethodId = suggestedMethodId,
            isFavorite = isFavorite,
            ingredients = ingredients.map { ing ->
                DomainRecipeIngredient(
                    id = ing.id,
                    recipeId = ing.recipeId,
                    name = ing.name,
                    amount = ing.amount,
                    unit = try { IngredientUnit.valueOf(ing.unit) } catch (e: Exception) { IngredientUnit.GRAMS },
                    orderIndex = ing.orderIndex
                )
            },
            steps = steps.map { st ->
                DomainRecipeStep(
                    id = st.id,
                    recipeId = st.recipeId,
                    instruction = st.instruction,
                    stepNumber = st.stepNumber,
                    durationSeconds = st.durationSeconds
                )
            },
            tags = tags,
            ownerUserId = ownerUserId,
            ownerDisplayName = ownerDisplayName,
            visibility = try { Visibility.valueOf(visibility) } catch (e: Exception) { Visibility.PRIVATE },
            isShared = isShared,
            originalAuthorUserId = originalAuthorUserId,
            originalAuthorName = originalAuthorName,
            originalEntityId = originalEntityId,
            rootEntityId = rootEntityId,
            importedFromShareId = importedFromShareId,
            copyMode = try { CopyMode.valueOf(copyMode) } catch (e: Exception) { CopyMode.ORIGINAL },
            originCreatedAtIso = originCreatedAt,
            schemaVersion = schemaVersion,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAtIso = lastSyncedAt,
            createdAtIso = createdAt,
            updatedAtIso = updatedAt,
            migrationStatus = try { MigrationStatus.valueOf(migrationStatus) } catch (e: Exception) { MigrationStatus.MIGRATED }
        )
    }

    // CATA & CUP MAPPERS
    fun Cata.toDomain(): DomainCata {
        val mappedFamily = activeFlavorFamily?.let {
            try { FlavorFamily.valueOf(it) } catch (e: Exception) { null }
        }
        val flavorNotes = try {
            flavorNotesAdapter.fromJson(selectedFlavorNotesJson)?.map {
                DomainCataFlavorNote(
                    note = it.note,
                    family = try { FlavorFamily.valueOf(it.family) } catch (e: Exception) { FlavorFamily.GREEN }
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val descriptors = try {
            stringListAdapter.fromJson(sensoryWheelDescriptorsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return DomainCata(
            id = id,
            cupId = cupId,
            recipeId = recipeId,
            beanId = beanId,
            activeFlavorFamily = mappedFamily,
            selectedFlavorNotes = flavorNotes,
            sensoryWheelDescriptors = descriptors,
            textureLevel = textureLevel?.let { try { SensoryLevel.valueOf(it) } catch (e: Exception) { null } },
            cleanlinessLevel = cleanlinessLevel?.let { try { SensoryLevel.valueOf(it) } catch (e: Exception) { null } },
            persistenceLevel = persistenceLevel?.let { try { SensoryLevel.valueOf(it) } catch (e: Exception) { null } },
            sweetnessLevel = sweetnessLevel?.let { try { SensoryLevel.valueOf(it) } catch (e: Exception) { null } },
            acidityLevel = acidityLevel?.let { try { SensoryLevel.valueOf(it) } catch (e: Exception) { null } },
            balanceLevel = balanceLevel?.let { try { SensoryLevel.valueOf(it) } catch (e: Exception) { null } },
            fragranceAromaScore = fragranceAromaScore,
            flavorScore = flavorScore,
            aftertasteScore = aftertasteScore,
            acidityScore = acidityScore,
            bodyScore = bodyScore,
            uniformityScore = uniformityScore,
            cleanCupScore = cleanCupScore,
            sweetnessScore = sweetnessScore,
            balanceScore = balanceScore,
            overallScore = overallScore,
            totalScaScore = totalScaScore,
            evaluatorNotes = evaluatorNotes,
            evaluatedAtIso = evaluatedAt,
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            remoteId = remoteId,
            syncStatus = try { SyncStatus.valueOf(syncStatus) } catch (e: Exception) { SyncStatus.SYNCED },
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAtIso = lastSyncedAt,
            createdAtIso = createdAt,
            updatedAtIso = updatedAt,
            migrationStatus = try { MigrationStatus.valueOf(migrationStatus) } catch (e: Exception) { MigrationStatus.MIGRATED }
        )
    }

    fun DomainCata.toEntity(): Cata {
        val flavorNotesDtos = selectedFlavorNotes.map {
            CataFlavorNoteDto(note = it.note, family = it.family.name)
        }
        return Cata(
            id = id,
            cupId = cupId,
            recipeId = recipeId,
            beanId = beanId,
            activeFlavorFamily = activeFlavorFamily?.name,
            selectedFlavorNotesJson = flavorNotesAdapter.toJson(flavorNotesDtos),
            sensoryWheelDescriptorsJson = stringListAdapter.toJson(sensoryWheelDescriptors),
            textureLevel = textureLevel?.name,
            cleanlinessLevel = cleanlinessLevel?.name,
            persistenceLevel = persistenceLevel?.name,
            sweetnessLevel = sweetnessLevel?.name,
            acidityLevel = acidityLevel?.name,
            balanceLevel = balanceLevel?.name,
            fragranceAromaScore = fragranceAromaScore,
            flavorScore = flavorScore,
            aftertasteScore = aftertasteScore,
            acidityScore = acidityScore,
            bodyScore = bodyScore,
            uniformityScore = uniformityScore,
            cleanCupScore = cleanCupScore,
            sweetnessScore = sweetnessScore,
            balanceScore = balanceScore,
            overallScore = overallScore,
            totalScaScore = totalScaScore,
            evaluatorNotes = evaluatorNotes,
            evaluatedAt = evaluatedAtIso,
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            remoteId = remoteId,
            syncStatus = syncStatus.name,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAt = lastSyncedAtIso,
            createdAt = createdAtIso,
            updatedAt = updatedAtIso,
            migrationStatus = migrationStatus.name
        )
    }

    fun DomainCata.toDto(): CataDto {
        return CataDto(
            id = id,
            cupId = cupId,
            recipeId = recipeId,
            beanId = beanId,
            activeFlavorFamily = activeFlavorFamily?.name,
            selectedFlavorNotes = selectedFlavorNotes.map {
                CataFlavorNoteDto(note = it.note, family = it.family.name)
            },
            sensoryWheelDescriptors = sensoryWheelDescriptors,
            textureLevel = textureLevel?.name,
            cleanlinessLevel = cleanlinessLevel?.name,
            persistenceLevel = persistenceLevel?.name,
            sweetnessLevel = sweetnessLevel?.name,
            acidityLevel = acidityLevel?.name,
            balanceLevel = balanceLevel?.name,
            fragranceAromaScore = fragranceAromaScore,
            flavorScore = flavorScore,
            aftertasteScore = aftertasteScore,
            acidityScore = acidityScore,
            bodyScore = bodyScore,
            uniformityScore = uniformityScore,
            cleanCupScore = cleanCupScore,
            sweetnessScore = sweetnessScore,
            balanceScore = balanceScore,
            overallScore = overallScore,
            totalScaScore = totalScaScore,
            evaluatorNotes = evaluatorNotes,
            evaluatedAt = evaluatedAtIso,
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAt = lastSyncedAtIso,
            createdAt = createdAtIso,
            updatedAt = updatedAtIso,
            migrationStatus = migrationStatus.name
        )
    }

    fun CataDto.toDomain(): DomainCata {
        return DomainCata(
            id = id,
            cupId = cupId,
            recipeId = recipeId,
            beanId = beanId,
            activeFlavorFamily = activeFlavorFamily?.let { try { FlavorFamily.valueOf(it) } catch (e: Exception) { null } },
            selectedFlavorNotes = selectedFlavorNotes.map {
                DomainCataFlavorNote(
                    note = it.note,
                    family = try { FlavorFamily.valueOf(it.family) } catch (e: Exception) { FlavorFamily.GREEN }
                )
            },
            sensoryWheelDescriptors = sensoryWheelDescriptors,
            textureLevel = textureLevel?.let { try { SensoryLevel.valueOf(it) } catch (e: Exception) { null } },
            cleanlinessLevel = cleanlinessLevel?.let { try { SensoryLevel.valueOf(it) } catch (e: Exception) { null } },
            persistenceLevel = persistenceLevel?.let { try { SensoryLevel.valueOf(it) } catch (e: Exception) { null } },
            sweetnessLevel = sweetnessLevel?.let { try { SensoryLevel.valueOf(it) } catch (e: Exception) { null } },
            acidityLevel = acidityLevel?.let { try { SensoryLevel.valueOf(it) } catch (e: Exception) { null } },
            balanceLevel = balanceLevel?.let { try { SensoryLevel.valueOf(it) } catch (e: Exception) { null } },
            fragranceAromaScore = fragranceAromaScore,
            flavorScore = flavorScore,
            aftertasteScore = aftertasteScore,
            acidityScore = acidityScore,
            bodyScore = bodyScore,
            uniformityScore = uniformityScore,
            cleanCupScore = cleanCupScore,
            sweetnessScore = sweetnessScore,
            balanceScore = balanceScore,
            overallScore = overallScore,
            totalScaScore = totalScaScore,
            evaluatorNotes = evaluatorNotes,
            evaluatedAtIso = evaluatedAt,
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAtIso = lastSyncedAt,
            createdAtIso = createdAt,
            updatedAtIso = updatedAt,
            migrationStatus = try { MigrationStatus.valueOf(migrationStatus) } catch (e: Exception) { MigrationStatus.MIGRATED }
        )
    }

    fun Cup.toDomain(cataEntity: Cata? = null): DomainCup {
        return DomainCup(
            id = id,
            recipeId = recipeId,
            beanId = beanId,
            techniqueId = techniqueId,
            methodId = methodId,
            grinderId = grinderId,
            executedDoseG = executedDoseG,
            executedWaterMl = executedWaterMl,
            executedRatio = executedRatio,
            executedTemperatureC = executedTemperatureC,
            executedGrindSetting = executedGrindSetting,
            executedDurationSeconds = executedDurationSeconds,
            beanNameSnapshot = beanNameSnapshot,
            recipeNameSnapshot = recipeNameSnapshot,
            techniqueNameSnapshot = techniqueNameSnapshot,
            methodNameSnapshot = methodNameSnapshot,
            grinderNameSnapshot = grinderNameSnapshot,
            cupLifeSeconds = cupLifeSeconds,
            cupLifeState = try { CupLifeState.valueOf(cupLifeState) } catch (e: Exception) { deriveCupLifeState(cupLifeSeconds) },
            nps = nps,
            rating = rating,
            comment = comment,
            brewDateIso = brewDate,
            recipeSnapshotJson = recipeSnapshotJson,
            techniqueSnapshotJson = techniqueSnapshotJson,
            beanSnapshotJson = beanSnapshotJson,
            grinderSnapshotJson = grinderSnapshotJson,
            cata = cataEntity?.toDomain(),
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            remoteId = remoteId,
            syncStatus = try { SyncStatus.valueOf(syncStatus) } catch (e: Exception) { SyncStatus.SYNCED },
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAtIso = lastSyncedAt,
            createdAtIso = createdAt,
            updatedAtIso = updatedAt,
            migrationStatus = try { MigrationStatus.valueOf(migrationStatus) } catch (e: Exception) { MigrationStatus.MIGRATED }
        )
    }

    fun DomainCup.toEntity(): Cup {
        return Cup(
            id = id,
            recipeId = recipeId,
            beanId = beanId,
            techniqueId = techniqueId,
            methodId = methodId,
            grinderId = grinderId,
            executedDoseG = executedDoseG,
            executedWaterMl = executedWaterMl,
            executedRatio = executedRatio,
            executedTemperatureC = executedTemperatureC,
            executedGrindSetting = executedGrindSetting,
            executedDurationSeconds = executedDurationSeconds,
            beanNameSnapshot = beanNameSnapshot,
            recipeNameSnapshot = recipeNameSnapshot,
            techniqueNameSnapshot = techniqueNameSnapshot,
            methodNameSnapshot = methodNameSnapshot,
            grinderNameSnapshot = grinderNameSnapshot,
            cupLifeSeconds = cupLifeSeconds,
            cupLifeState = cupLifeState.name,
            nps = nps,
            rating = rating,
            comment = comment,
            brewDate = brewDateIso,
            recipeSnapshotJson = recipeSnapshotJson,
            techniqueSnapshotJson = techniqueSnapshotJson,
            beanSnapshotJson = beanSnapshotJson,
            grinderSnapshotJson = grinderSnapshotJson,
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            remoteId = remoteId,
            syncStatus = syncStatus.name,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAt = lastSyncedAtIso,
            createdAt = createdAtIso,
            updatedAt = updatedAtIso,
            migrationStatus = migrationStatus.name
        )
    }

    fun DomainCup.toDto(): CupDto {
        return CupDto(
            id = id,
            recipeId = recipeId,
            beanId = beanId,
            techniqueId = techniqueId,
            methodId = methodId,
            grinderId = grinderId,
            executedDoseG = executedDoseG,
            executedWaterMl = executedWaterMl,
            executedRatio = executedRatio,
            executedTemperatureC = executedTemperatureC,
            executedGrindSetting = executedGrindSetting,
            executedDurationSeconds = executedDurationSeconds,
            beanNameSnapshot = beanNameSnapshot,
            recipeNameSnapshot = recipeNameSnapshot,
            techniqueNameSnapshot = techniqueNameSnapshot,
            methodNameSnapshot = methodNameSnapshot,
            grinderNameSnapshot = grinderNameSnapshot,
            cupLifeSeconds = cupLifeSeconds,
            cupLifeState = cupLifeState.name,
            nps = nps,
            rating = rating,
            comment = comment,
            brewDate = brewDateIso,
            cata = cata?.toDto(),
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAt = lastSyncedAtIso,
            createdAt = createdAtIso,
            updatedAt = updatedAtIso,
            migrationStatus = migrationStatus.name
        )
    }

    fun CupDto.toDomain(): DomainCup {
        return DomainCup(
            id = id,
            recipeId = recipeId,
            beanId = beanId,
            techniqueId = techniqueId,
            methodId = methodId,
            grinderId = grinderId,
            executedDoseG = executedDoseG,
            executedWaterMl = executedWaterMl,
            executedRatio = executedRatio,
            executedTemperatureC = executedTemperatureC,
            executedGrindSetting = executedGrindSetting,
            executedDurationSeconds = executedDurationSeconds,
            beanNameSnapshot = beanNameSnapshot,
            recipeNameSnapshot = recipeNameSnapshot,
            techniqueNameSnapshot = techniqueNameSnapshot,
            methodNameSnapshot = methodNameSnapshot,
            grinderNameSnapshot = grinderNameSnapshot,
            cupLifeSeconds = cupLifeSeconds,
            cupLifeState = try { CupLifeState.valueOf(cupLifeState) } catch (e: Exception) { deriveCupLifeState(cupLifeSeconds) },
            nps = nps,
            rating = rating,
            comment = comment,
            brewDateIso = brewDate,
            cata = cata?.toDomain(),
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAtIso = lastSyncedAt,
            createdAtIso = createdAt,
            updatedAtIso = updatedAt,
            migrationStatus = try { MigrationStatus.valueOf(migrationStatus) } catch (e: Exception) { MigrationStatus.MIGRATED }
        )
    }

    // LAB EXPERIMENT MAPPERS
    fun LabExperiment.toDomain(): DomainLabExperiment {
        return DomainLabExperiment(
            id = id,
            methodId = methodId,
            beanId = beanId,
            grinderId = grinderId,
            techniqueId = techniqueId,
            coffeeGrams = coffeeGrams,
            waterMl = waterMl,
            ratio = ratio,
            temperatureC = temperatureC,
            grindSetting = grindSetting,
            beanFreshnessDays = beanFreshnessDays,
            estimatedTimeSeconds = estimatedTimeSeconds,
            actualTimeSeconds = actualTimeSeconds,
            experimentHypothesis = experimentHypothesis,
            experimentNotes = experimentNotes,
            conclusionNotes = conclusionNotes,
            resultRating = resultRating,
            resultCupId = resultCupId,
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            remoteId = remoteId,
            syncStatus = try { SyncStatus.valueOf(syncStatus) } catch (e: Exception) { SyncStatus.SYNCED },
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAtIso = lastSyncedAt,
            createdAtIso = createdAt,
            updatedAtIso = updatedAt,
            migrationStatus = try { MigrationStatus.valueOf(migrationStatus) } catch (e: Exception) { MigrationStatus.MIGRATED }
        )
    }

    fun DomainLabExperiment.toEntity(): LabExperiment {
        return LabExperiment(
            id = id,
            methodId = methodId,
            beanId = beanId,
            grinderId = grinderId,
            techniqueId = techniqueId,
            coffeeGrams = coffeeGrams,
            waterMl = waterMl,
            ratio = ratio,
            temperatureC = temperatureC,
            grindSetting = grindSetting,
            beanFreshnessDays = beanFreshnessDays,
            estimatedTimeSeconds = estimatedTimeSeconds,
            actualTimeSeconds = actualTimeSeconds,
            experimentHypothesis = experimentHypothesis,
            experimentNotes = experimentNotes,
            conclusionNotes = conclusionNotes,
            resultRating = resultRating,
            resultCupId = resultCupId,
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            remoteId = remoteId,
            syncStatus = syncStatus.name,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAt = lastSyncedAtIso,
            createdAt = createdAtIso,
            updatedAt = updatedAtIso,
            migrationStatus = migrationStatus.name
        )
    }

    fun DomainLabExperiment.toDto(): LabExperimentDto {
        return LabExperimentDto(
            id = id,
            methodId = methodId,
            beanId = beanId,
            grinderId = grinderId,
            techniqueId = techniqueId,
            coffeeGrams = coffeeGrams,
            waterMl = waterMl,
            ratio = ratio,
            temperatureC = temperatureC,
            grindSetting = grindSetting,
            beanFreshnessDays = beanFreshnessDays,
            estimatedTimeSeconds = estimatedTimeSeconds,
            actualTimeSeconds = actualTimeSeconds,
            experimentHypothesis = experimentHypothesis,
            experimentNotes = experimentNotes,
            conclusionNotes = conclusionNotes,
            resultRating = resultRating,
            resultCupId = resultCupId,
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAt = lastSyncedAtIso,
            createdAt = createdAtIso,
            updatedAt = updatedAtIso,
            migrationStatus = migrationStatus.name
        )
    }

    fun LabExperimentDto.toDomain(): DomainLabExperiment {
        return DomainLabExperiment(
            id = id,
            methodId = methodId,
            beanId = beanId,
            grinderId = grinderId,
            techniqueId = techniqueId,
            coffeeGrams = coffeeGrams,
            waterMl = waterMl,
            ratio = ratio,
            temperatureC = temperatureC,
            grindSetting = grindSetting,
            beanFreshnessDays = beanFreshnessDays,
            estimatedTimeSeconds = estimatedTimeSeconds,
            actualTimeSeconds = actualTimeSeconds,
            experimentHypothesis = experimentHypothesis,
            experimentNotes = experimentNotes,
            conclusionNotes = conclusionNotes,
            resultRating = resultRating,
            resultCupId = resultCupId,
            ownerUserId = ownerUserId,
            schemaVersion = schemaVersion,
            serverVersion = serverVersion,
            expectedVersion = expectedVersion,
            lastSyncedAtIso = lastSyncedAt,
            createdAtIso = createdAt,
            updatedAtIso = updatedAt,
            migrationStatus = try { MigrationStatus.valueOf(migrationStatus) } catch (e: Exception) { MigrationStatus.MIGRATED }
        )
    }
}
