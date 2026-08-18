import CoreData
import XCTest
@testable import Cupa

final class LabEngineParityTests: XCTestCase {
    func testSeaLevelGoldenProfileMatchesAndroid() {
        let profile = LabEngine.calculate(
            coffeeGrams: 15, waterMl: 240, ratio: 16, temperature: 92,
            grindClicks: 24, freshnessState: "en ventana", altitudeMeters: 0, timeSeconds: 180
        )
        XCTAssertEqual(profile.extractionIndex, 0.95, accuracy: 0.000_01)
        XCTAssertEqual([profile.aroma, profile.acidity, profile.sweetness, profile.body, profile.bitterness, profile.finish], [64, 55, 87, 49, 32, 57])
        XCTAssertEqual(profile.labels, ["Ventana Óptima", "Grano en Punto"])
    }

    func testAltitudeCapsTemperatureAndMatchesAndroid() {
        let profile = LabEngine.calculate(
            coffeeGrams: 15, waterMl: 240, ratio: 16, temperature: 98,
            grindClicks: 24, freshnessState: "en ventana", altitudeMeters: 2240, timeSeconds: 180
        )
        XCTAssertEqual(LabEngine.boilingPointC(altitudeMeters: 2240), 92.384, accuracy: 0.000_1)
        XCTAssertEqual(profile.extractionIndex, 0.919_259_2, accuracy: 0.000_01)
        XCTAssertEqual([profile.aroma, profile.acidity, profile.sweetness, profile.body, profile.bitterness, profile.finish], [64, 56, 84, 49, 33, 56])
        XCTAssertEqual(profile.labels, ["Ventana Óptima", "Hervor 92.4°C", "Grano en Punto"])
        XCTAssertTrue(profile.summary.contains("92.4°C"))
    }

    func testUnderExtractionBoundaryMatchesAndroid() {
        let profile = LabEngine.calculate(
            coffeeGrams: 15, waterMl: 270, ratio: 18, temperature: 84,
            grindClicks: 32, freshnessState: "viejo", altitudeMeters: 0, timeSeconds: 80
        )
        XCTAssertEqual(profile.extractionIndex, 0.45, accuracy: 0.000_01)
        XCTAssertEqual([profile.aroma, profile.acidity, profile.sweetness, profile.body, profile.bitterness, profile.finish], [52, 82, 41, 48, 32, 45])
        XCTAssertEqual(profile.labels, ["Sub-Extracción", "Alta Claridad", "Acidez Brillante"])
    }

    func testOverExtractionBoundaryMatchesAndroid() {
        let profile = LabEngine.calculate(
            coffeeGrams: 20, waterMl: 200, ratio: 10, temperature: 98,
            grindClicks: 8, freshnessState: "muy fresco", altitudeMeters: 0, timeSeconds: 320
        )
        XCTAssertEqual(profile.extractionIndex, 1.65, accuracy: 0.000_01)
        XCTAssertEqual([profile.aroma, profile.acidity, profile.sweetness, profile.body, profile.bitterness, profile.finish], [78, 22, 32, 76, 84, 42])
        XCTAssertEqual(profile.labels, ["Alta Extracción", "Cuerpo Denso", "Tono Tostado"])
    }

    func testUnitConversionsRoundTrip() {
        let fahrenheit = LabEngine.fahrenheit(fromCelsius: 92)
        XCTAssertEqual(fahrenheit, 197.6, accuracy: 0.000_1)
        XCTAssertEqual(LabEngine.celsius(fromFahrenheit: fahrenheit), 92, accuracy: 0.000_1)
    }

    func testLabStatePersistsBetweenModels() throws {
        let suite = "LabEngineParityTests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let first = LabModel(defaults: defaults)
        first.setManualAltitude(2240, city: "CDMX")
        first.update { $0.temperatureUnit = .fahrenheit; $0.timeSeconds = 205 }
        let restored = LabModel(defaults: defaults)
        XCTAssertEqual(restored.state.altitudeMeters, 2240)
        XCTAssertEqual(restored.state.cityName, "CDMX (2240m)")
        XCTAssertEqual(restored.state.temperatureUnit, .fahrenheit)
        XCTAssertEqual(restored.state.timeSeconds, 205)
    }
}

final class CalculatorParityTests: XCTestCase {
    @MainActor func testBidirectionalCalculationsMatchAndroidRules() {
        let calculator = CalculatorModel()
        calculator.changeCoffee("18.5")
        XCTAssertEqual(calculator.water, 296)
        calculator.changeRatio("15.5")
        XCTAssertEqual(calculator.water, 286)
        calculator.changeWater("300")
        XCTAssertEqual(calculator.coffee, 19.4, accuracy: 0.000_1)
        XCTAssertEqual(calculator.coffeeInput, "19.4")
    }

    @MainActor func testAllReferenceMethodsUseExpectedRatios() {
        let calculator = CalculatorModel()
        let expected: [(String, Double)] = [
            ("V60", 16), ("AeroPress", 13), ("Prensa francesa", 15),
            ("Chemex", 16), ("Espresso", 2), ("Moka", 10), ("Cold brew", 8)
        ]
        for (method, ratio) in expected {
            calculator.selectMethod(method)
            XCTAssertEqual(calculator.ratio, ratio, "Ratio incorrecto para \(method)")
        }
    }
}

final class LocalPersistenceTests: XCTestCase {
    func testCoffeeAndExperimentCRUDInMemory() throws {
        let persistence = PersistenceController(inMemory: true)
        let context = persistence.container.viewContext
        let bean = CoffeeBeanRecord(context: context, name: "Prueba", brand: "Tostador", remainingQuantityGrams: 250)
        let state = LabState(altitudeMeters: 1500, cityName: "Guatemala (1,500m)")
        _ = LabExperimentRecord(context: context, state: state, profile: LabEngine.calculate(state))
        let grinder = GrinderRecord(context: context, name: "C40", brand: "Comandante", model: "C40 MK4", grinderType: "MANUAL", scaleUnit: "CLICKS", minimumSetting: 0, maximumSetting: 40, calibrationNotes: "Cero real", notes: "")
        let equipment = EquipmentRecord(context: context, name: "V60 02", equipmentType: "BREWER_METHOD", brand: "Hario", model: "02", capacityMl: 600, configuration: "Plástico", notes: "", isFavorite: true, isActive: true)
        try context.save()

        let beans = try context.fetch(NSFetchRequest<CoffeeBeanRecord>(entityName: "CoffeeBeanRecord"))
        let experiments = try context.fetch(NSFetchRequest<LabExperimentRecord>(entityName: "LabExperimentRecord"))
        let grinders = try context.fetch(NSFetchRequest<GrinderRecord>(entityName: "GrinderRecord"))
        let equipmentItems = try context.fetch(NSFetchRequest<EquipmentRecord>(entityName: "EquipmentRecord"))
        XCTAssertEqual(beans.map(\.name), ["Prueba"])
        XCTAssertEqual(experiments.first?.altitudeMeters, 1500)
        XCTAssertEqual(grinders.first?.maximumSetting, 40)
        XCTAssertEqual(equipmentItems.first?.capacityMl, 600)

        bean.markDeleted()
        grinder.markDeleted()
        equipment.markUpdated()
        try context.save()
        let activeRequest = NSFetchRequest<CoffeeBeanRecord>(entityName: "CoffeeBeanRecord")
        activeRequest.predicate = NSPredicate(format: "deletedAt == nil")
        XCTAssertTrue(try context.fetch(activeRequest).isEmpty)
        XCTAssertEqual(grinder.syncStatus, .pendingDelete)
        XCTAssertEqual(equipment.syncStatus, .pendingUpdate)
    }
}

final class RecipeTechniqueRepositoryTests: XCTestCase {
    @MainActor func testRecipeAggregateCreateEditDuplicateAndDelete() throws {
        let persistence = PersistenceController(inMemory: true)
        let context = persistence.container.viewContext
        let repository = RecipeTechniqueRepository(context: context)
        var draft = RecipeDraftModel(
            name: "V60 frutal", recipeKind: "BLACK_COFFEE", intention: "Acidez brillante", tags: "v60,frutal",
            ingredients: [
                .init(name: "Café", amount: 15, unit: "GRAMS"),
                .init(name: "Agua", amount: 240, unit: "MILLILITERS")
            ],
            steps: [
                .init(instruction: "Bloom", durationSeconds: 45),
                .init(instruction: "Vertido principal", durationSeconds: 120)
            ]
        )
        let recipe = try repository.saveRecipe(draft)
        XCTAssertEqual(try repository.ingredients(recipeId: recipe.id).map(\.name), ["Café", "Agua"])
        XCTAssertEqual(try repository.recipeSteps(recipeId: recipe.id).map(\.stepNumber), [1, 2])

        draft.ingredients.removeFirst()
        draft.steps.swapAt(0, 1)
        _ = try repository.saveRecipe(draft)
        XCTAssertEqual(try repository.ingredients(recipeId: recipe.id).map(\.name), ["Agua"])
        XCTAssertEqual(try repository.recipeSteps(recipeId: recipe.id).map(\.instruction), ["Vertido principal", "Bloom"])

        let copy = try repository.duplicateRecipe(recipe)
        XCTAssertNotEqual(copy.id, recipe.id)
        XCTAssertEqual(copy.originalEntityId, recipe.id)
        XCTAssertEqual(try repository.ingredients(recipeId: copy.id).map(\.name), ["Agua"])
        XCTAssertNotEqual(try repository.ingredients(recipeId: copy.id).first?.id, try repository.ingredients(recipeId: recipe.id).first?.id)

        try repository.deleteRecipe(recipe)
        XCTAssertNotNil(recipe.deletedAt)
        XCTAssertTrue(try repository.ingredients(recipeId: recipe.id).isEmpty)
        XCTAssertTrue(try repository.recipeSteps(recipeId: recipe.id).isEmpty)
    }

    @MainActor func testTechniqueOrderingAccumulationAndSoftDelete() throws {
        let persistence = PersistenceController(inMemory: true)
        let context = persistence.container.viewContext
        let repository = RecipeTechniqueRepository(context: context)
        let draft = TechniqueDraftModel(
            name: "V60 guiada", methodName: "V60", doseGrams: 15, waterMl: 240, ratio: 16,
            temperatureC: 93, executionMode: "GUIDED", grindValue: 24, grindDescription: "Media fina",
            steps: [
                .init(title: "Bloom", durationSeconds: 45, waterAddedMl: 50, intensity: "HIGH", gesture: "BLOOM"),
                .init(title: "Primer vertido", durationSeconds: 40, waterAddedMl: 100, intensity: "MEDIUM", gesture: "CIRCULAR_POUR"),
                .init(title: "Segundo vertido", durationSeconds: 35, waterAddedMl: 90, intensity: "LOW", gesture: "CENTER_POUR")
            ]
        )
        let technique = try repository.saveTechnique(draft)
        let steps = try repository.techniqueSteps(techniqueId: technique.id)
        XCTAssertEqual(steps.map(\.waterAccumulatedMl), [50, 150, 240])
        XCTAssertEqual(steps.map(\.stepNumber), [1, 2, 3])
        XCTAssertEqual(technique.totalTimeSeconds, 120)

        try repository.deleteTechnique(technique)
        XCTAssertEqual(technique.syncStatus, .pendingDelete)
        XCTAssertTrue(try repository.techniqueSteps(techniqueId: technique.id).isEmpty)
    }
}

final class PreparationModelTests: XCTestCase {
    @MainActor func testGuidedTimingRecoveryAndSessionSnapshot() throws {
        let suite = "PreparationModelTests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suite)); defer { defaults.removePersistentDomain(forName: suite) }
        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext
        let repository = RecipeTechniqueRepository(context: context)
        let technique = try repository.saveTechnique(TechniqueDraftModel(
            name: "Guiada", methodName: "V60", executionMode: "GUIDED",
            steps: [.init(title: "Bloom", durationSeconds: 45, waterAddedMl: 50), .init(title: "Vertido", durationSeconds: 75, waterAddedMl: 190)]
        ))
        let model = PreparationModel(defaults: defaults)
        model.load(technique: technique, steps: try repository.techniqueSteps(techniqueId: technique.id))
        model.start()
        let tick = try XCTUnwrap(model.state.lastTickAt)
        model.synchronizeClock(now: tick.addingTimeInterval(46))
        XCTAssertEqual(model.state.elapsedSeconds, 46)
        XCTAssertEqual(model.state.activeStepIndex, 1)
        model.pause()

        let restored = PreparationModel(defaults: defaults)
        XCTAssertEqual(restored.state.status, .paused)
        XCTAssertEqual(restored.state.elapsedSeconds, 46)
        XCTAssertEqual(restored.activeStep?.title, "Vertido")

        _ = BrewSessionRecord(context: context, state: restored.state, beanName: "Café prueba", grinderName: "Molino prueba")
        try context.save()
        let sessions = try context.fetch(NSFetchRequest<BrewSessionRecord>(entityName: "BrewSessionRecord"))
        XCTAssertEqual(sessions.first?.techniqueNameSnapshot, "Guiada")
        XCTAssertEqual(sessions.first?.elapsedSeconds, 46)
        XCTAssertTrue(sessions.first?.stepsSnapshotJSON.contains("Bloom") == true)
    }

    @MainActor func testManualNavigationAndReset() {
        let suite = "PreparationManualTests.\(UUID().uuidString)"; let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let model = PreparationModel(defaults: defaults)
        model.load(calculator: CalculatorModel())
        XCTAssertEqual(model.state.executionMode, "MANUAL")
        model.start(); model.pause(); model.reset()
        XCTAssertEqual(model.state.status, .ready)
        XCTAssertEqual(model.state.elapsedSeconds, 0)
        XCTAssertEqual(model.state.activeStepIndex, 0)
    }
}

final class TastingModelTests: XCTestCase {
    @MainActor func testCoolingRecoveryObservationAndIndependentAggregates() throws {
        let suite = "TastingModelTests.\(UUID().uuidString)"; let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext
        let preparation = PreparationState(techniqueName: "V60 guiada", methodName: "V60", elapsedSeconds: 180, status: .completed)
        let brew = BrewSessionRecord(context: context, state: preparation, beanName: "Etiopía", grinderName: "C40")
        try context.save()

        let model = TastingModel(defaults: defaults)
        model.state.brewSessionId = brew.id; model.state.selectedFlavorNotes = ["Mora", "Jazmín"]
        model.start(); let tick = try XCTUnwrap(model.state.lastTickAt)
        model.synchronizeClock(now: tick.addingTimeInterval(601))
        XCTAssertEqual(model.stageCode, "DECLINING")
        model.state.freeNotes = "Aparece cacao al enfriar"; model.addObservation(); model.pause()

        let restored = TastingModel(defaults: defaults)
        XCTAssertEqual(restored.state.coolingElapsedSeconds, 601)
        XCTAssertEqual(restored.state.observations.first?.stage, "DECLINING")
        let repository = TastingRepository(context: context)
        let tasting = try repository.save(restored.state, brew: brew)
        XCTAssertNotEqual(tasting.id, brew.id)
        XCTAssertEqual(tasting.techniqueId, brew.techniqueId)
        XCTAssertEqual(tasting.selectedFlavorNotes, ["Mora", "Jazmín"])
        XCTAssertEqual(try repository.observations(tastingId: tasting.id).count, 1)
        let cups = try context.fetch(NSFetchRequest<CupSessionRecord>(entityName: "CupSessionRecord"))
        XCTAssertEqual(cups.first?.brewSessionId, brew.id)
        XCTAssertEqual(cups.first?.tastingId, tasting.id)
    }

    @MainActor func testTastingSoftDeletePreservesCupSnapshot() throws {
        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext
        let repository = TastingRepository(context: context); var state = TastingState()
        state.observations = [.init(elapsedSeconds: 90, stage: "HOT", notes: "Floral", aroma: 4, acidity: 3, sweetness: 4, body: 2, bitterness: 1, finish: 4)]
        let tasting = try repository.save(state, brew: nil)
        try repository.delete(tasting)
        XCTAssertEqual(tasting.trackedSyncStatus, .pendingDelete)
        XCTAssertTrue(try repository.observations(tastingId: tasting.id).isEmpty)
        let cups = try context.fetch(NSFetchRequest<CupSessionRecord>(entityName: "CupSessionRecord"))
        XCTAssertEqual(cups.first?.techniqueNameSnapshot, "Cata independiente")
    }
}
