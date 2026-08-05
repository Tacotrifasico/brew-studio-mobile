package com.example.data.fixtures

import com.example.data.domain.*

object SharedFixtures {

    const val TEST_OWNER_USER_ID = "usr_01h8x9p3z4y2w1v0m9n8b7v6c5"

    val BREW_METHOD_V60 = DomainBrewMethod(
        id = "11111111-1111-4000-8000-000000000001",
        code = "v60",
        nameKey = "brew_method.v60.name",
        category = BrewMethodCategory.POUR_OVER,
        defaultRatio = 16.0f,
        ownerUserId = null,
        schemaVersion = 1
    )

    val BREW_METHOD_AEROPRESS = DomainBrewMethod(
        id = "11111111-1111-4000-8000-000000000002",
        code = "aeropress",
        nameKey = "brew_method.aeropress.name",
        category = BrewMethodCategory.HYBRID,
        defaultRatio = 15.0f,
        ownerUserId = null,
        schemaVersion = 1
    )

    val BREW_METHOD_ESPRESSO = DomainBrewMethod(
        id = "11111111-1111-4000-8000-000000000003",
        code = "espresso",
        nameKey = "brew_method.espresso.name",
        category = BrewMethodCategory.PRESSURE,
        defaultRatio = 2.0f,
        ownerUserId = null,
        schemaVersion = 1
    )

    val SAMPLE_BEAN_V6 = DomainBean(
        id = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        roaster = "Café de Especialidad Taller",
        name = "Geisha San Alberto",
        origin = "Huila, Colombia",
        altitude = "1,850 msnm",
        process = "Lavado Anaeróbico",
        roastDateIso = "2026-07-20",
        firstUseDateIso = "2026-07-25",
        notes = "Notas a jazmín, bergamota y miel de azahar",
        status = BeanStatus.OPEN,
        stockGrams = 220f,
        ownerUserId = TEST_OWNER_USER_ID,
        schemaVersion = 1,
        serverVersion = 1L,
        expectedVersion = 1L,
        createdAtIso = "2026-07-20T10:00:00Z",
        updatedAtIso = "2026-07-25T08:30:00Z",
        migrationStatus = MigrationStatus.MIGRATED
    )

    val SAMPLE_GRINDER_INSTRUMENT_V6 = DomainInstrument(
        id = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
        name = "Comandante C40 MK4",
        type = InstrumentType.GRINDER,
        brand = "Comandante",
        model = "C40 MK4 Nitro Blade",
        notes = "Molino manual de alta precisión con fresas de acero nitrogenado",
        grinderProfile = DomainGrinderProfile(
            id = "11111111-2222-3333-4444-555555555555",
            instrumentId = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
            clickRange = "0 - 45 Clicks",
            calibrationNotes = "Red Clix instalado (calibrado a 0.5 clics)",
            methodSettings = listOf(
                DomainGrinderMethodSetting(
                    id = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                    grinderProfileId = "11111111-2222-3333-4444-555555555555",
                    methodId = BREW_METHOD_V60.id,
                    settingValue = "24 Clicks"
                ),
                DomainGrinderMethodSetting(
                    id = "ffffffff-bbbb-cccc-dddd-eeeeeeeeeeee",
                    grinderProfileId = "11111111-2222-3333-4444-555555555555",
                    methodId = BREW_METHOD_ESPRESSO.id,
                    settingValue = "12 Clicks"
                )
            )
        ),
        ownerUserId = TEST_OWNER_USER_ID,
        schemaVersion = 1,
        serverVersion = 1L,
        expectedVersion = 1L,
        createdAtIso = "2026-07-15T12:00:00Z",
        updatedAtIso = "2026-07-15T12:00:00Z",
        migrationStatus = MigrationStatus.MIGRATED
    )

    val SAMPLE_RECIPE_V6 = DomainRecipe(
        id = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
        name = "Espresso Tonic Artesanal",
        recipeKind = RecipeKind.SIGNATURE,
        intention = "Resaltar la acidez cítrica del café con el amargor refrescante de la tónica",
        suggestedMethodId = BREW_METHOD_ESPRESSO.id,
        isFavorite = true,
        ingredients = listOf(
            DomainRecipeIngredient(
                id = "11111111-1111-4111-8111-111111111111",
                recipeId = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
                name = "Espresso Doble Extraído",
                amount = 36f,
                unit = IngredientUnit.GRAMS,
                orderIndex = 1
            ),
            DomainRecipeIngredient(
                id = "22222222-2222-4222-8222-222222222222",
                recipeId = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
                name = "Agua Tónica Premium",
                amount = 120f,
                unit = IngredientUnit.MILLILITERS,
                orderIndex = 2
            ),
            DomainRecipeIngredient(
                id = "33333333-3333-4333-8333-333333333333",
                recipeId = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
                name = "Hielo Macizo",
                amount = 4f,
                unit = IngredientUnit.UNITS,
                orderIndex = 3
            )
        ),
        steps = listOf(
            DomainRecipeStep(
                id = "44444444-1111-4111-8111-111111111111",
                recipeId = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
                instruction = "Llenar el vaso transparente con cubos de hielo macizo hasta el borde.",
                stepNumber = 1
            ),
            DomainRecipeStep(
                id = "55555555-2222-4222-8222-222222222222",
                recipeId = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
                instruction = "Verter despacio el agua tónica fría sobre los hielos para preservar la carbonatación.",
                stepNumber = 2
            ),
            DomainRecipeStep(
                id = "66666666-3333-4333-8333-333333333333",
                recipeId = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
                instruction = "Verter suavemente el espresso doble recién extraído usando el dorso de una cuchara para crear dos capas diferenciadas.",
                stepNumber = 3,
                durationSeconds = 25
            )
        ),
        tags = listOf("fresco", "verano", "autor", "espresso"),
        ownerUserId = TEST_OWNER_USER_ID,
        schemaVersion = 1,
        serverVersion = 1L,
        expectedVersion = 1L,
        createdAtIso = "2026-07-28T15:00:00Z",
        updatedAtIso = "2026-07-28T15:00:00Z",
        migrationStatus = MigrationStatus.MIGRATED
    )

    val SAMPLE_TECHNIQUE_V6 = DomainTechnique(
        id = "77777777-2222-4333-8444-555555555555",
        name = "V60 Tetsu Kasuya 4:6 Method",
        methodId = BREW_METHOD_V60.id,
        doseG = 20.0f,
        waterMl = 300,
        ratio = 15.0f,
        temperatureC = 92,
        executionMode = ExecutionMode.GUIDED,
        grindValue = 28.0,
        grindDescription = "28 Clicks",
        grindUnit = GrindUnit.CLICKS,
        notes = "Dividido en 5 vertidos de 60ml cada 45 segundos para ajustar dulzor y acidez",
        totalTimeSeconds = 210,
        steps = listOf(
            DomainTechniqueStep(
                id = "88888888-1111-4111-8111-111111111111",
                techniqueId = "77777777-2222-4333-8444-555555555555",
                stepNumber = 1,
                title = "Primer Vertido (Ajuste de Acidez)",
                durationSeconds = 45,
                waterAddedMl = 60,
                waterAccumulatedMl = 60,
                intensity = StepIntensity.HIGH,
                gesture = PreparationGesture.BLOOM,
                stepNote = "Vertido circular rápido para mojar la cama"
            ),
            DomainTechniqueStep(
                id = "99999999-2222-4222-8222-222222222222",
                techniqueId = "77777777-2222-4333-8444-555555555555",
                stepNumber = 2,
                title = "Segundo Vertido (Ajuste de Dulzor)",
                durationSeconds = 45,
                waterAddedMl = 60,
                waterAccumulatedMl = 120,
                intensity = StepIntensity.MEDIUM,
                gesture = PreparationGesture.CIRCULAR_POUR,
                stepNote = "Vertido concéntrico continuo"
            )
        ),
        ownerUserId = TEST_OWNER_USER_ID,
        schemaVersion = 1,
        serverVersion = 1L,
        expectedVersion = 1L,
        createdAtIso = "2026-07-20T09:00:00Z",
        updatedAtIso = "2026-07-20T09:00:00Z",
        migrationStatus = MigrationStatus.MIGRATED
    )

    val SAMPLE_CATA_V6 = DomainCata(
        id = "ca7a1111-2222-4333-8444-555555555555",
        cupId = "c0011111-2222-4333-8444-555555555555",
        recipeId = SAMPLE_RECIPE_V6.id,
        beanId = SAMPLE_BEAN_V6.id,
        activeFlavorFamily = FlavorFamily.FRUITY,
        selectedFlavorNotes = listOf(
            DomainCataFlavorNote(note = "Bergamota", family = FlavorFamily.CITRIC),
            DomainCataFlavorNote(note = "Miel de Azahar", family = FlavorFamily.SWEET)
        ),
        sensoryWheelDescriptors = listOf("Cítrico", "Miel", "Jazmín"),
        textureLevel = SensoryLevel.MEDIUM,
        cleanlinessLevel = SensoryLevel.HIGH,
        persistenceLevel = SensoryLevel.MEDIUM_HIGH,
        sweetnessLevel = SensoryLevel.HIGH,
        acidityLevel = SensoryLevel.MEDIUM_HIGH,
        balanceLevel = SensoryLevel.HIGH,
        fragranceAromaScore = 8.75,
        flavorScore = 8.50,
        aftertasteScore = 8.25,
        acidityScore = 8.50,
        bodyScore = 8.00,
        uniformityScore = 10.0,
        cleanCupScore = 10.0,
        sweetnessScore = 10.0,
        balanceScore = 8.50,
        overallScore = 8.50,
        totalScaScore = 89.50,
        evaluatorNotes = "Café sumamente complejo con notas florales y cítricas muy limpias.",
        evaluatedAtIso = "2026-07-25T10:00:00Z",
        ownerUserId = TEST_OWNER_USER_ID,
        schemaVersion = 1,
        serverVersion = 1L,
        expectedVersion = 1L,
        createdAtIso = "2026-07-25T10:00:00Z",
        updatedAtIso = "2026-07-25T10:00:00Z",
        migrationStatus = MigrationStatus.MIGRATED
    )

    val SAMPLE_CUP_V6 = DomainCup(
        id = "c0011111-2222-4333-8444-555555555555",
        recipeId = SAMPLE_RECIPE_V6.id,
        beanId = SAMPLE_BEAN_V6.id,
        techniqueId = SAMPLE_TECHNIQUE_V6.id,
        methodId = BREW_METHOD_V60.id,
        grinderId = SAMPLE_GRINDER_INSTRUMENT_V6.id,
        executedDoseG = 20.0f,
        executedWaterMl = 300,
        executedRatio = 15.0f,
        executedTemperatureC = 92,
        executedGrindSetting = "28 Clicks",
        executedDurationSeconds = 210,
        beanNameSnapshot = "Geisha San Alberto",
        recipeNameSnapshot = "Espresso Tonic Artesanal",
        techniqueNameSnapshot = "V60 Tetsu Kasuya 4:6 Method",
        methodNameSnapshot = "V60",
        grinderNameSnapshot = "Comandante C40 MK4",
        cupLifeSeconds = 240,
        cupLifeState = CupLifeState.FRESH,
        nps = 9,
        rating = 4.8,
        comment = "Tasa espectacular con acidez limpia y dulzor alto.",
        brewDateIso = "2026-07-25T09:45:00Z",
        cata = SAMPLE_CATA_V6,
        ownerUserId = TEST_OWNER_USER_ID,
        schemaVersion = 1,
        serverVersion = 1L,
        expectedVersion = 1L,
        createdAtIso = "2026-07-25T09:45:00Z",
        updatedAtIso = "2026-07-25T09:45:00Z",
        migrationStatus = MigrationStatus.MIGRATED
    )

    val SAMPLE_LAB_EXPERIMENT_V6 = DomainLabExperiment(
        id = "1ab11111-2222-4333-8444-555555555555",
        methodId = BREW_METHOD_V60.id,
        beanId = SAMPLE_BEAN_V6.id,
        grinderId = SAMPLE_GRINDER_INSTRUMENT_V6.id,
        techniqueId = SAMPLE_TECHNIQUE_V6.id,
        coffeeGrams = 20.0f,
        waterMl = 300,
        ratio = 15.0f,
        temperatureC = 92,
        grindSetting = "28 Clicks",
        beanFreshnessDays = 5,
        estimatedTimeSeconds = 210,
        actualTimeSeconds = 205,
        experimentHypothesis = "Modificar la temperatura a 92C mejorará la claridad aromática.",
        experimentNotes = "Extracción estable sin astringencia.",
        conclusionNotes = "Hipótesis confirmada. La taza resultó más brillante.",
        resultRating = 4.9,
        resultCupId = SAMPLE_CUP_V6.id,
        ownerUserId = TEST_OWNER_USER_ID,
        schemaVersion = 1,
        serverVersion = 1L,
        expectedVersion = 1L,
        createdAtIso = "2026-07-25T09:30:00Z",
        updatedAtIso = "2026-07-25T09:50:00Z",
        migrationStatus = MigrationStatus.MIGRATED
    )
}
