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
        XCTAssertEqual(Int(roundf(LabEngine.fahrenheit(fromCelsius: LabEngine.boilingPointC(altitudeMeters: 2240)))), 198)
    }

    func testAltitudeCatalogAndBoundariesMatchAndroid() {
        XCTAssertEqual(LabModel.cities.map(\.label), [
            "Costa / Mar", "Seattle / Tokio", "Roma / Paris", "São Paulo", "Medellín", "Guatemala",
            "San José CR", "CDMX / Oaxaca", "Addis Abeba", "Bogotá", "Cusco", "La Paz"
        ])
        XCTAssertEqual(LabModel.cities.map(\.altitudeMeters), [0, 50, 100, 760, 1495, 1500, 1170, 2240, 2355, 2600, 3399, 3640])
        XCTAssertEqual(LabEngine.boilingPointC(altitudeMeters: -1), 100, accuracy: 0.000_1)
        XCTAssertEqual(LabEngine.boilingPointC(altitudeMeters: 5_000), 83, accuracy: 0.000_1)
        XCTAssertEqual(LabEngine.boilingPointC(altitudeMeters: 8_000), 83, accuracy: 0.000_1)

        let cdmx = try! XCTUnwrap(LabModel.cities.first { $0.altitudeMeters == 2240 })
        XCTAssertTrue(cdmx.isSelected(altitudeMeters: 2240, cityName: "CDMX (2,240m)"))
        XCTAssertFalse(cdmx.isSelected(altitudeMeters: 2240, cityName: "Manual (2240m)"))
    }

    func testManualAltitudeClampsToReferenceRange() throws {
        let suite = "LabEngineParityTests.clamp.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let model = LabModel(defaults: defaults)
        model.setManualAltitude(-25)
        XCTAssertEqual(model.state.altitudeMeters, 0)
        model.setManualAltitude(5_025, city: "Quito")
        XCTAssertEqual(model.state.altitudeMeters, 5_000)
        XCTAssertEqual(model.state.cityName, "Quito (5000m)")
    }

    func testLabStatePersistsBetweenModels() throws {
        let suite = "LabEngineParityTests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let first = LabModel(defaults: defaults)
        first.setManualAltitude(2240, city: "CDMX")
        first.update { $0.timeSeconds = 205 }
        first.setTemperatureUnit(.fahrenheit)
        XCTAssertEqual(defaults.string(forKey: "settings.temperature"), TemperatureUnit.fahrenheit.rawValue)
        let restored = LabModel(defaults: defaults)
        XCTAssertEqual(restored.state.altitudeMeters, 2240)
        XCTAssertEqual(restored.state.cityName, "CDMX (2240m)")
        XCTAssertEqual(restored.state.temperatureUnit, .fahrenheit)
        XCTAssertEqual(restored.state.timeSeconds, 205)

        defaults.set(TemperatureUnit.celsius.rawValue, forKey: "settings.temperature")
        XCTAssertEqual(LabModel(defaults: defaults).state.temperatureUnit, .celsius)
    }
}

final class SocialContentPolicyTests: XCTestCase {
    private let safePayload = SharePayloadSnapshot(
        kind: "recipe",
        recipe: SharedRecipeSnapshot(name: "V60 dulce", recipeKind: "Brew", intention: "Balance", suggestedMethodName: "V60", tags: "frutal", ingredients: [], steps: []),
        technique: nil
    )

    func testAllowsCoffeeContent() throws {
        XCTAssertEqual(SocialContentPolicy.messageLimit, 280)
        XCTAssertNoThrow(try SocialContentPolicy.validate(fromName: "Ana", fromHandle: "ana", name: "V60 dulce", subtitle: "Balance", message: "Notas de cacao", payload: safePayload))
    }

    func testRejectsObjectionableContentEvenWithDiacritics() {
        XCTAssertThrowsError(try SocialContentPolicy.validate(fromName: "Ana", fromHandle: "ana", name: "V60", subtitle: "", message: "contenido de violación", payload: safePayload)) { error in
            XCTAssertEqual(error as? SocialValidationError, .objectionableContent)
        }
    }

    func testRejectsMissingProfileIdentity() {
        XCTAssertThrowsError(try SocialContentPolicy.validate(fromName: "", fromHandle: "", name: "V60", subtitle: "", message: "", payload: safePayload)) { error in
            XCTAssertEqual(error as? SocialValidationError, .emptyIdentity)
        }
    }
}

final class CalculatorParityTests: XCTestCase {
    @MainActor func testBidirectionalCalculationsMatchAndroidRules() throws {
        let suite = "CalculatorBidirectionalTests.\(UUID().uuidString)"; let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let calculator = CalculatorModel(defaults: defaults)
        calculator.changeCoffee("18.5")
        XCTAssertEqual(calculator.water, 296)
        calculator.changeRatio("15.5")
        XCTAssertEqual(calculator.water, 286)
        calculator.changeWater("300")
        XCTAssertEqual(calculator.coffee, 19.4, accuracy: 0.000_1)
        XCTAssertEqual(calculator.coffeeInput, "19.4")
    }

    @MainActor func testAllReferenceMethodsUseExpectedRatios() throws {
        let suite = "CalculatorMethodsTests.\(UUID().uuidString)"; let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let calculator = CalculatorModel(defaults: defaults)
        let expected: [(String, Double)] = [
            ("V60", 16), ("AeroPress", 13), ("Prensa francesa", 15),
            ("Chemex", 16), ("Espresso", 2), ("Moka", 10), ("Cold brew", 8)
        ]
        for (method, ratio) in expected {
            calculator.selectMethod(method)
            XCTAssertEqual(calculator.ratio, ratio, "Ratio incorrecto para \(method)")
        }
    }

    @MainActor func testFavoritePersistsAndCanBeRemoved() throws {
        let suite = "CalculatorParityTests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let calculator = CalculatorModel(defaults: defaults)
        calculator.changeCoffee("18")
        calculator.changeRatio("15")
        calculator.toggleFavorite()
        XCTAssertTrue(calculator.isCurrentFavorite)
        calculator.changeCoffee("20")
        let restored = CalculatorModel(defaults: defaults)
        XCTAssertEqual(restored.savedPresets.first?.coffee, 18)
        XCTAssertEqual(restored.coffee, 18)
        XCTAssertEqual(restored.ratio, 15)
        XCTAssertEqual(restored.water, 270)
        restored.toggleFavorite()
        XCTAssertFalse(restored.isCurrentFavorite)
        XCTAssertTrue(CalculatorModel(defaults: defaults).savedPresets.isEmpty)
    }

    @MainActor func testCalculatorDraftContinuouslyFeedsPreparationWithoutReplacingRunningBrew() throws {
        let suite = "CalculatorPreparationContinuityTests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let calculator = CalculatorModel(defaults: defaults)
        let preparation = PreparationModel(defaults: defaults)
        preparation.loadCalculatorIfPristine(calculator)

        calculator.changeCoffee("21")
        calculator.changeRatio("15")
        preparation.loadCalculatorDraftIfPossible(calculator)
        XCTAssertEqual(preparation.state.doseGrams, 21)
        XCTAssertEqual(preparation.state.waterMl, 315)
        XCTAssertEqual(preparation.state.ratio, 15)
        XCTAssertEqual(preparation.state.steps.last?.waterAccumulatedMl, 315)

        preparation.start()
        calculator.changeCoffee("18")
        preparation.loadCalculatorDraftIfPossible(calculator)
        XCTAssertEqual(preparation.state.doseGrams, 21, "Una preparación iniciada no debe sobrescribirse")
    }

    @MainActor func testPinnedMethodsAndCustomEquipmentReferencePersistAndTransfer() throws {
        let suite = "CalculatorMethodPreferencesTests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let calculator = CalculatorModel(defaults: defaults)
        XCTAssertEqual(calculator.pinnedMethodNames, Set(["V60", "AeroPress", "Espresso", "Prensa francesa"]))
        calculator.setMethodPinned("V60", pinned: false)
        let equipmentId = UUID()
        calculator.selectMethod("Origami", methodId: equipmentId)
        XCTAssertEqual(calculator.ratio, 15)
        calculator.toggleFavorite()

        let restored = CalculatorModel(defaults: defaults)
        XCTAssertFalse(restored.isMethodPinned("V60"))
        XCTAssertEqual(restored.method, "Origami")
        XCTAssertEqual(restored.selectedMethodId, equipmentId)
        XCTAssertEqual(restored.savedPresets.first?.methodId, equipmentId)

        let lab = LabModel(defaults: defaults); lab.load(calculator: restored)
        let preparation = PreparationModel(defaults: defaults); preparation.load(calculator: restored)
        XCTAssertEqual(lab.state.methodId, equipmentId)
        XCTAssertEqual(preparation.state.methodId, equipmentId)
    }
}

final class LocalPersistenceTests: XCTestCase {
    @MainActor func testPersistentStoreFailureUsesVisibleTemporaryFallback() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("CupaFallbackTests-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let blockedParent = directory.appendingPathComponent("not-a-directory")
        try Data("blocked".utf8).write(to: blockedParent)
        let persistence = PersistenceController(storeURL: blockedParent.appendingPathComponent("Cupa.sqlite"), enablePersistentHistory: false)
        XCTAssertNotNil(persistence.storageRecoveryMessage)
        XCTAssertEqual(persistence.container.persistentStoreCoordinator.persistentStores.first?.type, NSInMemoryStoreType)
        _ = CoffeeBeanRecord(context: persistence.container.viewContext, name: "Temporal", brand: "", remainingQuantityGrams: 100)
        XCTAssertNoThrow(try persistence.container.viewContext.save())
    }

    @MainActor func testSQLiteStoreSurvivesContainerReopening() throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("CupaSQLiteTests-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let storeURL = directory.appendingPathComponent("Cupa.sqlite"); let beanId = UUID()
        var grinderId: UUID!; var equipmentId: UUID!
        try autoreleasepool {
            let first = PersistenceController(storeURL: storeURL, enablePersistentHistory: false); let context = first.container.viewContext
            _ = CoffeeBeanRecord(context: context, id: beanId, name: "Persistente", brand: "Local", remainingQuantityGrams: 175)
            let grinder = GrinderRecord(context: context, name: "C40", brand: "Comandante", model: "MK4", grinderType: "MANUAL", scaleUnit: "CLICKS", minimumSetting: 0, maximumSetting: 40, calibrationNotes: "Cero real", notes: "Viaje")
            let equipment = EquipmentRecord(context: context, name: "V60 02", equipmentType: "BREWER_METHOD", brand: "Hario", model: "02", capacityMl: 600, configuration: "Plástico", notes: "Con servidor", isFavorite: true, isActive: true)
            grinderId = grinder.id; equipmentId = equipment.id
            XCTAssertNoThrow(try context.save())
            XCTAssertNoThrow(try first.container.persistentStoreCoordinator.remove(first.container.persistentStoreCoordinator.persistentStores[0]))
        }
        try autoreleasepool {
            let reopened = PersistenceController(storeURL: storeURL, enablePersistentHistory: false); let context = reopened.container.viewContext
            let request = NSFetchRequest<CoffeeBeanRecord>(entityName: "CoffeeBeanRecord")
            request.predicate = NSPredicate(format: "id == %@", beanId as CVarArg)
            let bean = try XCTUnwrap(context.fetch(request).first)
            XCTAssertEqual(bean.name, "Persistente"); XCTAssertEqual(bean.remainingQuantityGrams, 175)
            let grinderRequest = NSFetchRequest<GrinderRecord>(entityName: "GrinderRecord")
            grinderRequest.predicate = NSPredicate(format: "id == %@", grinderId as CVarArg)
            let grinder = try XCTUnwrap(context.fetch(grinderRequest).first)
            XCTAssertEqual(grinder.brand, "Comandante"); XCTAssertEqual(grinder.calibrationNotes, "Cero real")
            let equipmentRequest = NSFetchRequest<EquipmentRecord>(entityName: "EquipmentRecord")
            equipmentRequest.predicate = NSPredicate(format: "id == %@", equipmentId as CVarArg)
            let equipment = try XCTUnwrap(context.fetch(equipmentRequest).first)
            XCTAssertEqual(equipment.capacityMl, 600); XCTAssertTrue(equipment.isFavorite)
            try reopened.container.persistentStoreCoordinator.remove(reopened.container.persistentStoreCoordinator.persistentStores[0])
        }
    }

    func testCoffeeFreshnessMatchesAndroidBoundariesAndOpenWarning() {
        var calendar = Calendar(identifier: .gregorian); calendar.timeZone = TimeZone(secondsFromGMT: 0)!
        let now = Date(timeIntervalSince1970: 1_776_643_200)
        let date: (Int) -> Date = { calendar.date(byAdding: .day, value: -$0, to: now)! }
        let expected: [(Int, CoffeeFreshnessState)] = [
            (7, .veryFresh), (8, .inWindow), (21, .inWindow), (22, .ideal),
            (35, .ideal), (36, .declining), (60, .declining), (61, .old)
        ]
        for (days, state) in expected {
            XCTAssertEqual(CoffeeFreshnessEngine.evaluate(roastDate: date(days), openedDate: nil, now: now, calendar: calendar).state, state)
        }
        XCTAssertEqual(CoffeeFreshnessEngine.progress(days: 80), 1, accuracy: 0.000_001)
        let opened = CoffeeFreshnessEngine.evaluate(roastDate: date(15), openedDate: date(15), now: now, calendar: calendar)
        XCTAssertEqual(opened.openWarning, "Abierto hace 15 días. Puede perder aroma más rápido.")
    }

    func testCoffeeInputValidationRejectsCorruptInventoryValues() throws {
        var calendar = Calendar(identifier: .gregorian); calendar.timeZone = TimeZone(secondsFromGMT: 0)!
        let now = Date(timeIntervalSince1970: 1_776_643_200)
        let roast = calendar.date(byAdding: .day, value: -10, to: now)!
        let opened = calendar.date(byAdding: .day, value: -2, to: now)!
        let valid = try CoffeeBeanInputValidator.validate(
            altitudeText: "1850", initialQuantityText: "250,5", remainingQuantityText: "125.25",
            roastDate: roast, openedDate: opened, now: now, calendar: calendar
        )
        XCTAssertEqual(valid, .init(altitudeMeters: 1_850, initialQuantityGrams: 250.5, remainingQuantityGrams: 125.25))

        XCTAssertThrowsError(try CoffeeBeanInputValidator.validate(altitudeText: "alto", initialQuantityText: "250", remainingQuantityText: "100", roastDate: nil, openedDate: nil)) {
            XCTAssertEqual($0 as? CoffeeBeanInputError, .invalidAltitude)
        }
        XCTAssertThrowsError(try CoffeeBeanInputValidator.validate(altitudeText: "", initialQuantityText: "NaN", remainingQuantityText: "0", roastDate: nil, openedDate: nil)) {
            XCTAssertEqual($0 as? CoffeeBeanInputError, .invalidInitialQuantity)
        }
        XCTAssertThrowsError(try CoffeeBeanInputValidator.validate(altitudeText: "", initialQuantityText: "250", remainingQuantityText: "251", roastDate: nil, openedDate: nil)) {
            XCTAssertEqual($0 as? CoffeeBeanInputError, .remainingExceedsInitial)
        }
        XCTAssertThrowsError(try CoffeeBeanInputValidator.validate(altitudeText: "", initialQuantityText: "250", remainingQuantityText: "100", roastDate: roast, openedDate: calendar.date(byAdding: .day, value: -11, to: now), now: now, calendar: calendar)) {
            XCTAssertEqual($0 as? CoffeeBeanInputError, .openedBeforeRoast)
        }
        XCTAssertThrowsError(try CoffeeBeanInputValidator.validate(altitudeText: "", initialQuantityText: "250", remainingQuantityText: "100", roastDate: calendar.date(byAdding: .day, value: 1, to: now), openedDate: nil, now: now, calendar: calendar)) {
            XCTAssertEqual($0 as? CoffeeBeanInputError, .roastDateInFuture)
        }
    }

    func testCoffeeExperimentGrinderAndEquipmentCRUDInMemory() throws {
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
        XCTAssertTrue(equipment.isBrewingMethod)
        XCTAssertTrue(equipment.isFavorite)

        grinder.maximumSetting = 42; grinder.calibrationNotes = "Cero ajustado"; grinder.markUpdated()
        equipment.capacityMl = 700; equipment.configuration = "Cerámica"; equipment.isFavorite = false; equipment.markUpdated()
        try context.save()
        XCTAssertEqual(grinders.first?.maximumSetting, 42)
        XCTAssertEqual(grinder.syncStatus, .pendingUpdate)
        XCTAssertEqual(equipmentItems.first?.configuration, "Cerámica")
        XCTAssertEqual(equipment.syncStatus, .pendingUpdate)

        let preparation = PreparationState(techniqueName: "Prueba histórica", methodId: equipment.id, methodName: equipment.name, grinderId: grinder.id, grindDescription: "22 clicks")
        let brew = BrewSessionRecord(context: context, state: preparation, beanName: "", grinderName: grinder.name)
        try context.save()
        bean.markDeleted()
        grinder.markDeleted()
        equipment.markDeleted()
        try context.save()
        let activeRequest = NSFetchRequest<CoffeeBeanRecord>(entityName: "CoffeeBeanRecord")
        activeRequest.predicate = NSPredicate(format: "deletedAt == nil")
        XCTAssertTrue(try context.fetch(activeRequest).isEmpty)
        let activeGrinders = NSFetchRequest<GrinderRecord>(entityName: "GrinderRecord"); activeGrinders.predicate = NSPredicate(format: "deletedAt == nil")
        let activeEquipment = NSFetchRequest<EquipmentRecord>(entityName: "EquipmentRecord"); activeEquipment.predicate = NSPredicate(format: "deletedAt == nil")
        XCTAssertTrue(try context.fetch(activeGrinders).isEmpty)
        XCTAssertTrue(try context.fetch(activeEquipment).isEmpty)
        XCTAssertEqual(grinder.syncStatus, .pendingDelete)
        XCTAssertEqual(equipment.syncStatus, .pendingDelete)
        XCTAssertEqual(brew.grinderId, grinder.id); XCTAssertEqual(brew.methodId, equipment.id)
        XCTAssertEqual(brew.grinderNameSnapshot, "C40"); XCTAssertEqual(brew.methodNameSnapshot, "V60 02")
    }
}

final class RecipeTechniqueRepositoryTests: XCTestCase {
    func testRecipeTextParserMatchesAndroidImporterContract() {
        let draft = RecipeTextParser.parse("""
        Receta: Espresso Tonic Menta
        Ingredientes:
        - 30 ml Espresso extraído
        - 150 ml Agua tónica
        - 2 unidades Hielo
        Pasos:
        1. Servir tónica y hielo
        2. Verter espresso
        Perfil:
        Refrescante y herbal
        """)
        XCTAssertEqual(draft.name, "Espresso Tonic Menta")
        XCTAssertEqual(draft.recipeKind, "COLD_DRINK")
        XCTAssertEqual(draft.suggestedMethodName, "Espresso")
        XCTAssertEqual(draft.ingredients.map(\.amount), [30, 150, 2])
        XCTAssertEqual(draft.ingredients.map(\.unit), ["MILLILITERS", "MILLILITERS", "UNITS"])
        XCTAssertEqual(draft.steps.map(\.instruction), ["Servir tónica y hielo", "Verter espresso"])
        XCTAssertEqual(draft.intention, "Refrescante y herbal")
    }

    func testRecipeTextParserProvidesAndroidFallbacks() {
        let draft = RecipeTextParser.parse("Mi bebida secreta")
        XCTAssertEqual(draft.name, "Mi bebida secreta")
        XCTAssertEqual(draft.recipeKind, "OTHER")
        XCTAssertEqual(draft.ingredients.count, 2)
        XCTAssertEqual(draft.steps.map(\.instruction), ["Mezclar los ingredientes y servir"])
    }

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
        var draft = TechniqueDraftModel(
            name: "V60 guiada", methodName: "V60", doseGrams: 15, waterMl: 240, ratio: 16,
            temperatureC: 93, executionMode: "GUIDED", grindValue: 24, grindDescription: "Media fina",
            steps: [
                .init(title: "Bloom", durationSeconds: 45, waterAddedMl: 50, intensity: "HIGH", gesture: "BLOOM", note: "Saturar", coverage: 100, flow: 3.2, secondaryAction: "Agitar"),
                .init(title: "Primer vertido", durationSeconds: 40, waterAddedMl: 100, intensity: "MEDIUM", gesture: "CIRCULAR_POUR"),
                .init(title: "Segundo vertido", durationSeconds: 35, waterAddedMl: 90, intensity: "LOW", gesture: "CENTER_POUR")
            ]
        )
        let technique = try repository.saveTechnique(draft)
        let steps = try repository.techniqueSteps(techniqueId: technique.id)
        XCTAssertEqual(steps.map(\.waterAccumulatedMl), [50, 150, 240])
        XCTAssertEqual(steps.map(\.stepNumber), [1, 2, 3])
        XCTAssertEqual(technique.totalTimeSeconds, 120)
        XCTAssertEqual(steps.first?.secondaryAction, "Agitar")
        XCTAssertEqual(steps.first?.coverage, 100)

        draft.steps.swapAt(0, 2)
        draft.steps.remove(at: 1)
        draft.steps[0].flow = 2.5
        _ = try repository.saveTechnique(draft)
        let editedSteps = try repository.techniqueSteps(techniqueId: technique.id)
        XCTAssertEqual(editedSteps.map(\.title), ["Segundo vertido", "Bloom"])
        XCTAssertEqual(editedSteps.map(\.waterAccumulatedMl), [90, 140])
        XCTAssertEqual(editedSteps.first?.flow, 2.5)
        XCTAssertEqual(technique.totalTimeSeconds, 80)

        let suite = "TechniqueDetailPreparationTests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suite)); defer { defaults.removePersistentDomain(forName: suite) }
        let preparation = PreparationModel(defaults: defaults)
        preparation.load(technique: technique, steps: editedSteps)
        XCTAssertEqual(preparation.state.techniqueId, technique.id)
        XCTAssertEqual(preparation.state.steps.map(\.title), ["Segundo vertido", "Bloom"])
        XCTAssertEqual(preparation.state.steps.map(\.waterAccumulatedMl), [90, 140])

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

    @MainActor func testCalculatorQuickStepsAndReset() {
        let suite = "PreparationManualTests.\(UUID().uuidString)"; let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let model = PreparationModel(defaults: defaults)
        let calculator = CalculatorModel(defaults: defaults)
        calculator.selectMethod("V60"); calculator.changeCoffee("15"); calculator.changeRatio("16")
        model.load(calculator: calculator)
        XCTAssertEqual(model.state.executionMode, "GUIDED")
        XCTAssertEqual(model.state.steps.map(\.durationSeconds), [35, 45, 40])
        XCTAssertEqual(model.state.steps.map(\.waterAccumulatedMl), [50, 145, 240])
        model.start(); let tick = model.state.lastTickAt!
        model.synchronizeClock(now: tick.addingTimeInterval(130))
        XCTAssertEqual(model.state.status, .completed)
        XCTAssertEqual(model.state.elapsedSeconds, 120)
        XCTAssertNil(model.state.savedAt)
        XCTAssertEqual(PreparationModel(defaults: defaults).state.status, .completed)
        let savedSessionId = model.state.sessionId
        model.markSaved()
        XCTAssertEqual(PreparationModel(defaults: defaults).state.status, .ready)
        model.reset()
        XCTAssertNotEqual(model.state.sessionId, savedSessionId)
        XCTAssertNil(model.state.savedAt)
        XCTAssertEqual(model.state.status, .ready)
        XCTAssertEqual(model.state.elapsedSeconds, 0)
        XCTAssertEqual(model.state.activeStepIndex, 0)
    }
}

final class TastingModelTests: XCTestCase {
    @MainActor func testCoolingStageBoundariesResetAndCompletedGuards() throws {
        let suite = "TastingCoolingBoundariesTests.\(UUID().uuidString)"; let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let model = TastingModel(defaults: defaults)
        for (seconds, expected) in [(0, "HOT"), (239, "HOT"), (240, "PEAK"), (599, "PEAK"), (600, "DECLINING"), (959, "DECLINING"), (960, "EXHAUSTED")] {
            model.state.coolingElapsedSeconds = seconds
            XCTAssertEqual(model.stageCode, expected)
        }
        model.state.coolingElapsedSeconds = 0
        model.addObservation()
        XCTAssertTrue(model.state.observations.isEmpty)
        model.start(); let tick = try XCTUnwrap(model.state.lastTickAt)
        model.synchronizeClock(now: tick.addingTimeInterval(245))
        model.addObservation(); model.pause()
        XCTAssertEqual(model.state.observations.map(\.stage), ["PEAK"])
        XCTAssertEqual(model.state.coolingStatus, .paused)
        let tastingId = model.state.id
        model.reset()
        XCTAssertEqual(model.state.id, tastingId)
        XCTAssertEqual(model.state.coolingStatus, .ready)
        XCTAssertEqual(model.state.coolingElapsedSeconds, 0)
        XCTAssertTrue(model.state.observations.isEmpty)

        model.start(); model.addObservation(); model.markSaved()
        let completed = model.state
        model.addObservation(); model.removeObservation(id: completed.observations[0].id); model.reset()
        XCTAssertEqual(model.state, completed)
        XCTAssertEqual(model.state.coolingStatus, .completed)
    }

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
        var cups = try context.fetch(NSFetchRequest<CupSessionRecord>(entityName: "CupSessionRecord"))
        XCTAssertEqual(cups.first?.brewSessionId, brew.id)
        XCTAssertEqual(cups.first?.tastingId, tasting.id)

        restored.load(record: tasting, observations: try repository.observations(tastingId: tasting.id))
        restored.state.rating = 5
        restored.state.freeNotes = "Editada desde el historial"
        restored.state.observations = [.init(elapsedSeconds: 960, stage: "EXHAUSTED", notes: "Fría", aroma: 3, acidity: 2, sweetness: 3, body: 2, bitterness: 3, finish: 2)]
        let updated = try repository.save(restored.state, brew: brew)
        XCTAssertEqual(updated.id, tasting.id)
        XCTAssertEqual(updated.rating, 5)
        XCTAssertEqual(try repository.observations(tastingId: tasting.id).map(\.stage), ["EXHAUSTED"])
        cups = try context.fetch(NSFetchRequest<CupSessionRecord>(entityName: "CupSessionRecord"))
        XCTAssertEqual(cups.count, 1)
        XCTAssertEqual(cups.first?.comment, "Editada desde el historial")

        restored.markSaved()
        XCTAssertEqual(restored.state.coolingStatus, .completed)
        let savedId = restored.state.id
        restored.newTasting()
        XCTAssertNotEqual(restored.state.id, savedId)
        XCTAssertEqual(restored.state.coolingStatus, .ready)
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

final class CoffeeUsageHistoryTests: XCTestCase {
    @MainActor func testInventoryStatusAndTransfersMatchAndroidActions() throws {
        let suite = "CoffeeInventoryActions.\(UUID().uuidString)"; let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext
        let roastDate = Date(timeIntervalSince1970: 1_700_000_000)
        let bean = CoffeeBeanRecord(
            context: context, name: "Guji", brand: "Casa", process: "Lavado",
            roastDate: roastDate, initialQuantityGrams: 250, remainingQuantityGrams: 250, notes: "Jazmín"
        )
        XCTAssertEqual(bean.inventoryStatus, .closed)

        let lab = LabModel(defaults: defaults); lab.load(bean: bean, now: roastDate.addingTimeInterval(5 * 86_400))
        XCTAssertEqual(lab.state.beanId, bean.id)
        XCTAssertEqual(lab.state.freshness, "muy fresco")
        XCTAssertEqual(lab.state.notes, "Grano: Guji. Proceso: Lavado. Jazmín")

        let preparation = PreparationModel(defaults: defaults); preparation.selectBean(bean)
        XCTAssertEqual(preparation.state.beanId, bean.id)
        XCTAssertEqual(PreparationModel(defaults: defaults).state.beanId, bean.id)

        bean.openedDate = roastDate.addingTimeInterval(5 * 86_400)
        XCTAssertEqual(bean.inventoryStatus, .open)
        bean.remainingQuantityGrams = 0
        XCTAssertEqual(bean.inventoryStatus, .finished)
    }

    @MainActor func testHistoryIsDerivedFromBeanIdentifierAndSurvivesBeanDeletion() throws {
        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext
        let bean = CoffeeBeanRecord(context: context, name: "Etiopía Guji", brand: "Casa", remainingQuantityGrams: 180)
        let preparation = PreparationState(
            techniqueName: "Tres vertidos", methodName: "V60", beanId: bean.id,
            doseGrams: 18, waterMl: 288, ratio: 16, temperatureC: 93,
            elapsedSeconds: 180, status: .completed
        )
        let brew = BrewSessionRecord(context: context, state: preparation, beanName: bean.name, grinderName: "C40")
        try context.save()
        var tastingState = TastingState(brewSessionId: brew.id)
        tastingState.rating = 4.5; tastingState.freeNotes = "Jazmín al enfriar"
        let tasting = try TastingRepository(context: context).save(tastingState, brew: brew)

        let brews = NSFetchRequest<BrewSessionRecord>(entityName: "BrewSessionRecord")
        brews.predicate = NSPredicate(format: "beanId == %@ AND deletedAt == nil", bean.id as CVarArg)
        let cups = NSFetchRequest<CupSessionRecord>(entityName: "CupSessionRecord")
        cups.predicate = NSPredicate(format: "beanId == %@ AND deletedAt == nil", bean.id as CVarArg)
        XCTAssertEqual(try context.count(for: brews), 1)
        XCTAssertEqual(try context.count(for: cups), 1)
        let cup = try XCTUnwrap(context.fetch(cups).first)
        XCTAssertEqual(cup.rating, 4.5)
        XCTAssertEqual(cup.comment, "Jazmín al enfriar")

        bean.markDeleted(); try context.save()
        XCTAssertEqual(try context.count(for: brews), 1)
        XCTAssertEqual(try context.count(for: cups), 1)
        XCTAssertEqual(brew.beanNameSnapshot, "Etiopía Guji")
        XCTAssertEqual(cup.beanNameSnapshot, "Etiopía Guji")
        XCTAssertEqual(tasting.beanId, bean.id)
        XCTAssertEqual(tasting.evaluatorNotes, "Jazmín al enfriar")
    }
}

private final class MemoryTokenStore: TokenStore {
    var value: AuthTokens?
    func load() throws -> AuthTokens? { value }
    func save(_ tokens: AuthTokens) throws { value = tokens }
    func clear() throws { value = nil }
}

private final class MockTransport: NetworkTransport {
    var requests: [URLRequest] = []; var responseData: Data; var statusCode: Int; var error: Error?
    init(responseData: Data = Data(), statusCode: Int = 200, error: Error? = nil) { self.responseData = responseData; self.statusCode = statusCode; self.error = error }
    func data(for request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        requests.append(request)
        if let error { throw error }
        return (responseData, HTTPURLResponse(url: request.url!, statusCode: statusCode, httpVersion: nil, headerFields: nil)!)
    }
}

final class AccountAndSyncTests: XCTestCase {
    @MainActor func testAccountIsUnavailableWithoutPublicConfiguration() {
        let model = AccountModel(configuration: .init(supabaseURL: nil, supabaseAnonKey: nil), transport: MockTransport(), store: MemoryTokenStore())
        XCTAssertEqual(model.state, .unavailable)
    }

    func testEnvironmentConfigurationRejectsEmptyAndInsecureRemoteValues() {
        let empty = AppConfiguration(bundle: Bundle(for: Self.self), environment: ["SUPABASE_URL": "", "SUPABASE_ANON_KEY": "   "])
        XCTAssertFalse(empty.isSupabaseConfigured)
        XCTAssertNil(empty.supabaseURL)
        XCTAssertNil(empty.supabaseAnonKey)

        let insecure = AppConfiguration(bundle: Bundle(for: Self.self), environment: ["SUPABASE_URL": "http://example.com", "SUPABASE_ANON_KEY": "public-key"])
        XCTAssertFalse(insecure.isSupabaseConfigured)
        XCTAssertNil(insecure.supabaseURL)

        let production = AppConfiguration(bundle: Bundle(for: Self.self), environment: ["SUPABASE_URL": " https://project.supabase.co ", "SUPABASE_ANON_KEY": " public-key "])
        XCTAssertEqual(production.supabaseURL?.absoluteString, "https://project.supabase.co")
        XCTAssertEqual(production.supabaseAnonKey, "public-key")
        XCTAssertEqual(production.authRedirectURL?.absoluteString, "com.tacotrifasico.cupa://auth/recovery")
        XCTAssertTrue(production.isSupabaseConfigured)

        let staging = AppConfiguration(bundle: Bundle(for: Self.self), environment: ["AUTH_URL_SCHEME": "com.tacotrifasico.cupa.staging"])
        XCTAssertEqual(staging.authRedirectURL?.absoluteString, "com.tacotrifasico.cupa.staging://auth/recovery")

        let invalidCallback = AppConfiguration(bundle: Bundle(for: Self.self), environment: ["AUTH_URL_SCHEME": "not a scheme"])
        XCTAssertNil(invalidCallback.authRedirectURL)

        let local = AppConfiguration(bundle: Bundle(for: Self.self), environment: ["SUPABASE_URL": "http://127.0.0.1:54321", "SUPABASE_ANON_KEY": "local-key"])
        XCTAssertTrue(local.isSupabaseConfigured)
    }

    func testSupabaseSignInContractAndTokenMapping() async throws {
        let userId = UUID(); let body = try JSONSerialization.data(withJSONObject: [
            "access_token": "access", "refresh_token": "refresh", "expires_in": 3600,
            "user": ["id": userId.uuidString, "email": "brew@example.com"]
        ])
        let transport = MockTransport(responseData: body)
        let service = SupabaseAuthService(configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"), transport: transport)
        let tokens = try await service.signIn(email: "brew@example.com", password: "password123")
        XCTAssertEqual(tokens.userId, userId); XCTAssertEqual(tokens.email, "brew@example.com")
        XCTAssertEqual(transport.requests.first?.url?.path, "/auth/v1/token")
        XCTAssertEqual(transport.requests.first?.value(forHTTPHeaderField: "apikey"), "public-anon")
        XCTAssertNil(transport.requests.first?.value(forHTTPHeaderField: "Authorization"))
    }

    @MainActor func testSignUpSupportsEmailConfirmationWithoutInventingASession() async throws {
        let userId = UUID()
        let pendingBody = try JSONSerialization.data(withJSONObject: ["id": userId.uuidString, "email": "new@example.com"])
        let pendingTransport = MockTransport(responseData: pendingBody)
        let pendingStore = MemoryTokenStore()
        let pendingModel = AccountModel(
            configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"),
            transport: pendingTransport, store: pendingStore
        )

        await pendingModel.signUp(email: "new@example.com", password: "password123")

        XCTAssertEqual(pendingModel.state, .signedOut)
        XCTAssertNil(pendingStore.value)
        XCTAssertTrue(pendingModel.sessionNotice?.contains("confirma tu correo") == true)
        XCTAssertEqual(pendingTransport.requests.first?.url?.path, "/auth/v1/signup")
        XCTAssertEqual(pendingModel.pendingConfirmationEmail, "new@example.com")

        await pendingModel.resendSignUpConfirmation()
        XCTAssertEqual(pendingModel.state, .signedOut)
        XCTAssertTrue(pendingModel.sessionNotice?.contains("Enviamos de nuevo") == true)
        XCTAssertEqual(pendingTransport.requests.map { $0.url?.path }, ["/auth/v1/signup", "/auth/v1/resend"])
        let resendBody = try XCTUnwrap(JSONSerialization.jsonObject(with: try XCTUnwrap(pendingTransport.requests.last?.httpBody)) as? [String: String])
        XCTAssertEqual(resendBody, ["email": "new@example.com", "type": "signup"])

        let activeBody = try JSONSerialization.data(withJSONObject: [
            "access_token": "access", "refresh_token": "refresh", "expires_in": 3_600,
            "user": ["id": userId.uuidString, "email": "active@example.com"]
        ])
        let activeStore = MemoryTokenStore()
        let activeModel = AccountModel(
            configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"),
            transport: MockTransport(responseData: activeBody), store: activeStore
        )
        await activeModel.signUp(email: "active@example.com", password: "password123")
        XCTAssertEqual(activeModel.tokens?.userId, userId)
        XCTAssertEqual(activeStore.value?.accessToken, "access")
        XCTAssertNil(activeModel.pendingConfirmationEmail)
    }

    @MainActor func testPasswordRecoveryShowsEnumerationSafeConfirmation() async throws {
        let transport = MockTransport(responseData: Data("{}".utf8))
        let model = AccountModel(
            configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"),
            transport: transport, store: MemoryTokenStore()
        )
        await model.recover(email: "maybe@example.com")
        XCTAssertEqual(model.state, .signedOut)
        XCTAssertTrue(model.sessionNotice?.contains("Si existe una cuenta") == true)
        XCTAssertEqual(transport.requests.first?.url?.path, "/auth/v1/recover")
        let redirect = URLComponents(url: try XCTUnwrap(transport.requests.first?.url), resolvingAgainstBaseURL: false)?.queryItems?.first(where: { $0.name == "redirect_to" })?.value
        XCTAssertEqual(redirect, "com.tacotrifasico.cupa://auth/recovery")
    }

    @MainActor func testPasswordRecoveryDeepLinkUpdatesPasswordWithoutCreatingNormalSession() async throws {
        let transport = MockTransport(responseData: Data("{}".utf8))
        let store = MemoryTokenStore()
        let configuration = AppConfiguration(
            supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon",
            authRedirectURL: URL(string: "com.tacotrifasico.cupa://auth/recovery")!
        )
        let model = AccountModel(configuration: configuration, transport: transport, store: store)
        let callback = try XCTUnwrap(URL(string: "com.tacotrifasico.cupa://auth/recovery#access_token=recovery-jwt&refresh_token=recovery-refresh&expires_in=3600&type=recovery"))

        XCTAssertTrue(model.handleAuthCallback(callback))
        XCTAssertEqual(model.passwordRecoveryState, .ready)
        await model.completePasswordRecovery(newPassword: "better-password")

        XCTAssertEqual(model.passwordRecoveryState, .completed)
        XCTAssertNil(store.value)
        XCTAssertEqual(transport.requests.map { $0.url?.path }, ["/auth/v1/user", "/auth/v1/logout"])
        XCTAssertEqual(transport.requests.first?.httpMethod, "PUT")
        XCTAssertEqual(transport.requests.first?.value(forHTTPHeaderField: "Authorization"), "Bearer recovery-jwt")
        let body = try XCTUnwrap(JSONSerialization.jsonObject(with: try XCTUnwrap(transport.requests.first?.httpBody)) as? [String: String])
        XCTAssertEqual(body, ["password": "better-password"])
        XCTAssertTrue(model.sessionNotice?.contains("Contraseña actualizada") == true)
    }

    @MainActor func testPasswordRecoveryRejectsForeignAndInvalidCallbacks() throws {
        let model = AccountModel(
            configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"),
            transport: MockTransport(), store: MemoryTokenStore()
        )
        XCTAssertFalse(model.handleAuthCallback(try XCTUnwrap(URL(string: "other.app://auth/recovery#access_token=stolen&expires_in=3600&type=recovery"))))
        XCTAssertEqual(model.passwordRecoveryState, .idle)

        XCTAssertTrue(model.handleAuthCallback(try XCTUnwrap(URL(string: "com.tacotrifasico.cupa://auth/recovery#error=access_denied&error_description=Expired%20link"))))
        XCTAssertEqual(model.passwordRecoveryState, .error("Expired link"))

        let now = Date(timeIntervalSince1970: 2_000_000_000)
        XCTAssertTrue(model.handleAuthCallback(try XCTUnwrap(URL(string: "com.tacotrifasico.cupa://auth/recovery#access_token=expired&expires_at=1999999999&type=recovery")), now: now))
        XCTAssertEqual(model.passwordRecoveryState, .error("El enlace de recuperación venció. Solicita uno nuevo."))
    }

    func testAccountDeletionUsesAuthenticatedEdgeFunction() async throws {
        let transport = MockTransport(responseData: Data("{\"deleted\":true}".utf8))
        let service = SupabaseAccountService(configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"), transport: transport)
        try await service.deleteAccount(accessToken: "user-jwt", confirmation: "ELIMINAR")
        XCTAssertEqual(transport.requests.first?.url?.path, "/functions/v1/delete-account")
        XCTAssertEqual(transport.requests.first?.value(forHTTPHeaderField: "Authorization"), "Bearer user-jwt")
        XCTAssertFalse(String(data: transport.requests.first?.httpBody ?? Data(), encoding: .utf8)?.contains("service_role") == true)
    }

    @MainActor func testAccountDeletionPurgesOnlyThatUsersLocalDataAndPreferences() async throws {
        let owner = UUID(); let otherOwner = UUID(); let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext
        let suite = "AccountDeletion.\(UUID().uuidString)"; let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        context.activeOwnerId = owner
        _ = CoffeeBeanRecord(context: context, name: "Cuenta eliminada", brand: "Casa", remainingQuantityGrams: 100)
        context.activeOwnerId = otherOwner
        _ = CoffeeBeanRecord(context: context, name: "Otra cuenta", brand: "Casa", remainingQuantityGrams: 100)
        context.activeOwnerId = nil
        _ = CoffeeBeanRecord(context: context, name: "Invitado", brand: "Local", remainingQuantityGrams: 100)
        try context.save()
        defaults.set("A", forKey: LocalDataScope.scopedKey("cupa.labState.v1", ownerId: owner))
        defaults.set("B", forKey: LocalDataScope.scopedKey("cupa.labState.v1", ownerId: otherOwner))
        defaults.set("G", forKey: LocalDataScope.scopedKey("cupa.labState.v1", ownerId: nil))

        let tokens = AuthTokens(accessToken: "active", refreshToken: "refresh", expiresAt: .now.addingTimeInterval(3_600), userId: owner, email: "delete@example.com")
        let store = MemoryTokenStore(); store.value = tokens
        let model = AccountModel(
            configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"),
            transport: MockTransport(responseData: Data("{\"deleted\":true}".utf8)), store: store,
            accountDeletionHandler: { try LocalAccountDataPurger(context: context, defaults: defaults).purge(ownerId: $0) }
        )
        await model.deleteAccount(confirmation: "ELIMINAR")

        let request = NSFetchRequest<CoffeeBeanRecord>(entityName: "CoffeeBeanRecord")
        XCTAssertEqual(Set(try context.fetch(request).map(\.name)), ["Otra cuenta", "Invitado"])
        XCTAssertNil(defaults.object(forKey: LocalDataScope.scopedKey("cupa.labState.v1", ownerId: owner)))
        XCTAssertEqual(defaults.string(forKey: LocalDataScope.scopedKey("cupa.labState.v1", ownerId: otherOwner)), "B")
        XCTAssertEqual(defaults.string(forKey: LocalDataScope.scopedKey("cupa.labState.v1", ownerId: nil)), "G")
        XCTAssertNil(store.value); XCTAssertEqual(model.state, .signedOut)
        XCTAssertEqual(model.sessionNotice, "Tu cuenta y sus datos fueron eliminados.")
    }

    @MainActor func testOfflineRefreshKeepsStoredSessionAndLocalIdentity() async throws {
        let now = Date(timeIntervalSince1970: 2_000_000_000)
        let expired = AuthTokens(accessToken: "old", refreshToken: "refresh", expiresAt: now.addingTimeInterval(-1), userId: UUID(), email: "brew@example.com")
        let store = MemoryTokenStore(); store.value = expired
        let model = AccountModel(
            configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"),
            transport: MockTransport(error: URLError(.notConnectedToInternet)), store: store
        )

        let usableTokens = await model.validTokens(now: now)
        XCTAssertNil(usableTokens)
        XCTAssertEqual(model.tokens, expired)
        XCTAssertEqual(store.value, expired)
        XCTAssertTrue(model.sessionNotice?.contains("Sin conexión") == true)
    }

    @MainActor func testRejectedRefreshClearsTerminalSession() async throws {
        let now = Date(timeIntervalSince1970: 2_000_000_000)
        let expired = AuthTokens(accessToken: "old", refreshToken: "revoked", expiresAt: now.addingTimeInterval(-1), userId: UUID(), email: "brew@example.com")
        let store = MemoryTokenStore(); store.value = expired
        let model = AccountModel(
            configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"),
            transport: MockTransport(responseData: Data("{\"message\":\"Invalid refresh token\"}".utf8), statusCode: 401), store: store
        )

        let usableTokens = await model.validTokens(now: now)
        XCTAssertNil(usableTokens)
        XCTAssertEqual(model.state, .signedOut)
        XCTAssertNil(store.value)
        XCTAssertTrue(model.sessionNotice?.contains("sesión venció") == true)
    }

    @MainActor func testAuthenticatedOperationRefreshesOnceAfterUnauthorized() async throws {
        let userId = UUID()
        let stored = AuthTokens(accessToken: "old", refreshToken: "refresh", expiresAt: .now.addingTimeInterval(3_600), userId: userId, email: "brew@example.com")
        let refreshedBody = try JSONSerialization.data(withJSONObject: [
            "access_token": "new", "refresh_token": "refresh-2", "expires_in": 3_600,
            "user": ["id": userId.uuidString, "email": "brew@example.com"]
        ])
        let store = MemoryTokenStore(); store.value = stored
        let model = AccountModel(
            configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"),
            transport: MockTransport(responseData: refreshedBody), store: store
        )
        var attempts = 0
        let usedToken = try await model.authenticated { token in
            attempts += 1
            if attempts == 1 { throw AuthServiceError.server(401, "expired") }
            return token
        }
        XCTAssertEqual(attempts, 2)
        XCTAssertEqual(usedToken, "new")
        XCTAssertEqual(store.value?.accessToken, "new")
    }

    @MainActor func testConflictResolutionAndPersistentRetryOutbox() throws {
        let owner = UUID(); let older = Date(timeIntervalSince1970: 100); let newer = Date(timeIntervalSince1970: 200)
        XCTAssertEqual(LastWriteWinsResolver.resolve(local: .init(ownerId: owner, updatedAt: newer, version: 2, deletedAt: nil), remote: .init(ownerId: owner, updatedAt: older, version: 9, deletedAt: nil)), .local)
        XCTAssertEqual(LastWriteWinsResolver.resolve(local: .init(ownerId: owner, updatedAt: older, version: 1, deletedAt: nil), remote: .init(ownerId: UUID(), updatedAt: newer, version: 2, deletedAt: nil)), .ownerMismatch)

        let persistence = PersistenceController(inMemory: true); let repository = SyncOutboxRepository(context: persistence.container.viewContext); let entityId = UUID()
        let first = try repository.enqueue(entityName: "recipes", entityId: entityId, ownerId: owner, operation: .pendingCreate, payloadJSON: "{\"name\":\"V60\"}")
        let same = try repository.enqueue(entityName: "recipes", entityId: entityId, ownerId: owner, operation: .pendingUpdate, payloadJSON: "{\"name\":\"V60 editada\"}")
        XCTAssertEqual(first.id, same.id); XCTAssertEqual(try repository.ready().count, 1)
        let now = Date(); try repository.markFailed(same, message: "offline", now: now)
        XCTAssertTrue(same.nextAttemptAt > now); XCTAssertTrue(try repository.ready(now: now).isEmpty)
        try repository.markSucceeded(same); XCTAssertTrue(try repository.ready(now: .distantFuture).isEmpty)
    }

    func testGeminiUsesAuthenticatedEdgeFunctionAndFallsBackLocally() async throws {
        let state = LabState(waterMl: 270, ratio: 18, temperatureC: 84, grindClicks: 32, freshness: "viejo", timeSeconds: 80)
        let input = SuggestionContext(state: state, profile: LabEngine.calculate(state))
        let remote = BrewSuggestion(text: "Ajusta una sola variable.", source: .gemini, promptVersion: "brew-adjustment-v1")
        let transport = MockTransport(responseData: try JSONEncoder().encode(remote))
        let service = GeminiSuggestionService(configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"), transport: transport)
        let result = await service.suggest(input, accessToken: "user-jwt")
        XCTAssertEqual(result, remote)
        XCTAssertEqual(transport.requests.first?.url?.path, "/functions/v1/gemini-suggestions")
        XCTAssertEqual(transport.requests.first?.value(forHTTPHeaderField: "Authorization"), "Bearer user-jwt")

        let fallback = await GeminiSuggestionService(configuration: .init(supabaseURL: nil, supabaseAnonKey: nil), transport: transport).suggest(input, accessToken: nil)
        XCTAssertEqual(fallback.source, .local); XCTAssertTrue(fallback.text.contains("extracción estimada es baja"))
    }
}

final class SettingsTests: XCTestCase {
    @MainActor func testThemeAndUnitPreferencesRestore() throws {
        let suite = "SettingsTests.\(UUID().uuidString)"; let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let first = SettingsModel(defaults: defaults); first.theme = .dark; first.temperatureUnit = .fahrenheit
        let restored = SettingsModel(defaults: defaults)
        XCTAssertEqual(restored.theme, .dark); XCTAssertEqual(restored.temperatureUnit, .fahrenheit)
        XCTAssertEqual(restored.preferredColorScheme, .dark)
    }

    @MainActor func testProfileUpdateOwnerIsolationAndRemoteMapping() throws {
        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext; let repository = ProfileRepository(context: context)
        let ownerA = UUID(); let ownerB = UUID()
        let first = try repository.save(ownerId: ownerA, displayName: "Emiliano", alias: "brewther", biography: "V60", avatarColor: "#3F7A63", favoriteMethods: "V60, AeroPress", isPrivate: true)
        let updated = try repository.save(ownerId: ownerA, displayName: "Emiliano N.", alias: "@brewther", biography: "Café", avatarColor: "#234E3C", favoriteMethods: "V60", isPrivate: false)
        _ = try repository.save(ownerId: ownerB, displayName: "Otra persona", alias: "otra", biography: "", avatarColor: "#000000", favoriteMethods: "", isPrivate: true)
        XCTAssertEqual(first.id, updated.id); XCTAssertEqual(updated.alias, "brewther")
        XCTAssertEqual(try repository.profile(ownerId: ownerA)?.displayName, "Emiliano N.")
        XCTAssertEqual(try context.fetch(NSFetchRequest<UserProfileRecord>(entityName: "UserProfileRecord")).count, 2)
        let json = try XCTUnwrap(String(data: repository.remoteJSON(updated), encoding: .utf8))
        XCTAssertTrue(json.contains("favorite_methods")); XCTAssertFalse(json.contains("email"))
    }

    @MainActor func testProfileValidationNormalizationAndPrivacyPolicy() throws {
        let validated = try ProfileInputValidator.validate(displayName: "  Ana Café  ", alias: " @ana.cafe ", biography: "  V60  ", avatarColor: " #3f7a63 ", favoriteMethods: " V60 ")
        XCTAssertEqual(validated.displayName, "Ana Café"); XCTAssertEqual(validated.alias, "ana.cafe")
        XCTAssertEqual(validated.biography, "V60"); XCTAssertEqual(validated.avatarColor, "#3F7A63"); XCTAssertEqual(validated.favoriteMethods, "V60")
        XCTAssertThrowsError(try ProfileInputValidator.validate(displayName: "Ana", alias: "alias con espacios", biography: "", avatarColor: "#3F7A63", favoriteMethods: ""))
        XCTAssertThrowsError(try ProfileInputValidator.validate(displayName: "Ana", alias: "ana", biography: "", avatarColor: "verde", favoriteMethods: ""))
        XCTAssertEqual(ProfileSharingPolicy.allowedVisibilities(isPrivate: true), ["DIRECT"])
        XCTAssertEqual(ProfileSharingPolicy.allowedVisibilities(isPrivate: false), ["PUBLIC", "DIRECT"])

        let persistence = PersistenceController(inMemory: true); let repository = ProfileRepository(context: persistence.container.viewContext)
        let record = try repository.save(ownerId: UUID(), displayName: " Ana ", alias: "@ana", biography: " Café ", avatarColor: "#3f7a63", favoriteMethods: " V60 ", isPrivate: true)
        XCTAssertEqual(record.displayName, "Ana"); XCTAssertEqual(record.alias, "ana"); XCTAssertEqual(record.avatarColor, "#3F7A63")
    }
}

final class SocialTests: XCTestCase {
    func testDirectRecipientResolvesByPublicAliasWithoutUserFacingUUID() async throws {
        let recipientId = UUID()
        let response = try JSONSerialization.data(withJSONObject: [["user_id": recipientId.uuidString, "alias": "ana.cafe"]])
        let transport = MockTransport(responseData: response)
        let service = SocialService(configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"), transport: transport)

        let recipient = try await service.recipient(alias: "  @Ana.Cafe ", accessToken: "jwt")

        XCTAssertEqual(recipient, .init(userId: recipientId, alias: "ana.cafe"))
        XCTAssertEqual(transport.requests.first?.url?.path, "/rest/v1/rpc/resolve_profile_alias")
        XCTAssertEqual(transport.requests.first?.httpMethod, "POST")
        let body = try XCTUnwrap(JSONSerialization.jsonObject(with: try XCTUnwrap(transport.requests.first?.httpBody)) as? [String: String])
        XCTAssertEqual(body["alias_input"], "Ana.Cafe")
        do {
            _ = try await service.recipient(alias: "alias con espacios", accessToken: "jwt")
            XCTFail("Un alias inválido no debe llegar a la red")
        } catch {
            XCTAssertEqual(error as? SocialRecipientError, .invalidAlias)
        }
    }

    @MainActor func testFeedContractImportAndAttribution() async throws {
        let originalId = UUID(); let ownerId = UUID()
        let recipe = SharedRecipeSnapshot(name: "V60 comunitaria", recipeKind: "BLACK_COFFEE", intention: "Dulzor", suggestedMethodName: "V60", tags: "frutal", ingredients: [.init(name: "Café", amount: 15, unit: "GRAMS")], steps: [.init(instruction: "Bloom", durationSeconds: 45)])
        let share = SocialShare(id: UUID(), ownerId: ownerId, entityType: "recipe", entityId: originalId, fromName: "Barista", fromHandle: "barista", targetUserId: nil, visibility: "PUBLIC", name: recipe.name, subtitle: recipe.intention, message: "Prueba", payloadSnapshot: .init(kind: "recipe", recipe: recipe, technique: nil), originalAuthorUserId: ownerId, originalAuthorName: "Barista original", originalEntityId: originalId, status: "ACTIVE", createdAt: "2026-08-17T00:00:00Z", updatedAt: "2026-08-17T00:00:00Z")
        let transport = MockTransport(responseData: try JSONEncoder().encode([share]))
        let service = SocialService(configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"), transport: transport)
        let feed = try await service.feed(accessToken: "user-jwt")
        XCTAssertEqual(feed, [share]); XCTAssertEqual(transport.requests.first?.url?.path, "/rest/v1/shares")
        XCTAssertTrue(transport.requests.first?.url?.query?.contains("visibility=eq.public") == true)

        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext
        try service.importShare(share, context: context)
        let imported = try context.fetch(NSFetchRequest<RecipeRecord>(entityName: "RecipeRecord"))
        XCTAssertEqual(imported.first?.name, "Copia de V60 comunitaria"); XCTAssertEqual(imported.first?.originalEntityId, originalId); XCTAssertEqual(imported.first?.copyMode, "IMPORT")
        XCTAssertEqual(imported.first?.originalAuthorUserId, ownerId); XCTAssertEqual(imported.first?.originalAuthorName, "Barista original")
        XCTAssertEqual(imported.first?.importedFromShareId, share.id); XCTAssertTrue(imported.first?.isShared == true)
        XCTAssertEqual(try RecipeTechniqueRepository(context: context).ingredients(recipeId: try XCTUnwrap(imported.first?.id)).count, 1)

        let forkPersistence = PersistenceController(inMemory: true); let forkContext = forkPersistence.container.viewContext
        try service.importShare(share, mode: .forked, context: forkContext)
        let fork = try XCTUnwrap(forkContext.fetch(NSFetchRequest<RecipeRecord>(entityName: "RecipeRecord")).first)
        XCTAssertEqual(fork.name, "V60 comunitaria (Variante)"); XCTAssertEqual(fork.copyMode, "FORK"); XCTAssertEqual(fork.originalEntityId, originalId)
    }

    func testPublishReportAndBlockContractsDoNotExposeEmail() async throws {
        let transport = MockTransport(statusCode: 201); let service = SocialService(configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"), transport: transport)
        let entityId = UUID(); let payload = SharePayloadSnapshot(kind: "recipe", recipe: .init(name: "V60", recipeKind: "BLACK_COFFEE", intention: "", suggestedMethodName: "V60", tags: "", ingredients: [], steps: []), technique: nil)
        let originalAuthorId = UUID(); let originalEntityId = UUID()
        try await service.publish(entityType: "recipe", entityId: entityId, fromName: "Barista", fromHandle: "brew", name: "V60", subtitle: "", message: "", payload: payload, originalAuthorUserId: originalAuthorId, originalAuthorName: "Autora original", originalEntityId: originalEntityId, accessToken: "jwt")
        let publishBody = String(data: try XCTUnwrap(transport.requests.first?.httpBody), encoding: .utf8) ?? ""
        XCTAssertFalse(publishBody.contains("email")); XCTAssertTrue(publishBody.contains("payload_snapshot_json"))
        let publishJSON = try XCTUnwrap(JSONSerialization.jsonObject(with: try XCTUnwrap(transport.requests.first?.httpBody)) as? [String: Any])
        let snapshot = try XCTUnwrap(publishJSON["payload_snapshot_json"] as? [String: Any])
        XCTAssertEqual(snapshot["name"] as? String, "V60"); XCTAssertNil(snapshot["recipe"]); XCTAssertNil(snapshot["kind"])
        XCTAssertEqual(publishJSON["original_author_user_id"] as? String, originalAuthorId.uuidString)
        XCTAssertEqual(publishJSON["original_author_name"] as? String, "Autora original")
        XCTAssertEqual(publishJSON["original_entity_id"] as? String, originalEntityId.uuidString)
        try await service.report(shareId: UUID(), reason: .spam, details: "Enlaces engañosos", accessToken: "jwt")
        try await service.block(userId: UUID(), accessToken: "jwt")
        XCTAssertEqual(transport.requests.map { $0.url?.path }, ["/rest/v1/shares", "/rest/v1/content_reports", "/rest/v1/blocked_users"])
        let reportBody = try XCTUnwrap((JSONSerialization.jsonObject(with: try XCTUnwrap(transport.requests[1].httpBody)) as? [String: Any]))
        XCTAssertEqual(reportBody["reason"] as? String, "SPAM_OR_FRAUD: Enlaces engañosos")
    }

    func testAndroidFlatSharePayloadDecodesAndMessageLimitMatchesBackend() throws {
        let id = UUID(); let owner = UUID()
        let androidShare: [String: Any] = [
            "id": id.uuidString, "from_user_id": owner.uuidString, "entity_type": "recipe", "entity_id": id.uuidString,
            "from_name": "Ana", "from_handle": "ana", "visibility": "public", "name": "V60 Android", "subtitle": "Receta", "message": "",
            "payload_snapshot_json": ["name": "V60 Android", "recipeKind": "BLACK_COFFEE", "intention": "Dulzor", "tags": "frutal"],
            "original_entity_id": id.uuidString, "status": "active", "created_at": "2026-09-01T00:00:00Z", "updated_at": "2026-09-01T00:00:00Z"
        ]
        let share = try JSONDecoder().decode(SocialShare.self, from: JSONSerialization.data(withJSONObject: androidShare))
        XCTAssertEqual(share.payloadSnapshot.recipe?.name, "V60 Android"); XCTAssertNil(share.payloadSnapshot.technique)
        XCTAssertEqual(SocialContentPolicy.messageLimit, 280)
        let payload = share.payloadSnapshot
        XCTAssertNoThrow(try SocialContentPolicy.validate(fromName: "Ana", fromHandle: "ana", name: "V60", subtitle: "", message: String(repeating: "a", count: 280), payload: payload))
        XCTAssertThrowsError(try SocialContentPolicy.validate(fromName: "Ana", fromHandle: "ana", name: "V60", subtitle: "", message: String(repeating: "a", count: 281), payload: payload))
    }

    func testBlockListAndUnblockContracts() async throws {
        let userId = UUID(); let blockedId = UUID()
        let listData = try JSONSerialization.data(withJSONObject: [["blocked_user_id": blockedId.uuidString]])
        let listTransport = MockTransport(responseData: listData)
        let configuration = AppConfiguration(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon")
        let ids = try await SocialService(configuration: configuration, transport: listTransport).blockedUserIds(userId: userId, accessToken: "jwt")
        XCTAssertEqual(ids, [blockedId]); XCTAssertEqual(listTransport.requests.first?.httpMethod, "GET")
        XCTAssertTrue(listTransport.requests.first?.url?.query?.contains("blocker_id=eq.\(userId.uuidString)") == true)

        let deleteTransport = MockTransport(statusCode: 204)
        try await SocialService(configuration: configuration, transport: deleteTransport).unblock(userId: blockedId, accessToken: "jwt")
        XCTAssertEqual(deleteTransport.requests.first?.httpMethod, "DELETE")
        XCTAssertTrue(deleteTransport.requests.first?.url?.query?.contains("blocked_user_id=eq.\(blockedId.uuidString)") == true)
    }

    func testDirectInboxActivityAndReadContracts() async throws {
        let owner = UUID(); let target = UUID(); let entity = UUID(); let shareId = UUID(); let inboxId = UUID()
        let payload = SharePayloadSnapshot(kind: "recipe", recipe: .init(name: "V60", recipeKind: "BLACK_COFFEE", intention: "Dulzor", suggestedMethodName: "V60", tags: "", ingredients: [], steps: []), technique: nil)
        let share = SocialShare(id: shareId, ownerId: owner, entityType: "recipe", entityId: entity, fromName: "Ana", fromHandle: "ana", targetUserId: target, visibility: "DIRECT", name: "V60", subtitle: "Dulzor", message: "Para ti", payloadSnapshot: payload, originalEntityId: entity, status: "ACTIVE", createdAt: "2026-08-18T00:00:00Z", updatedAt: "2026-08-18T00:00:00Z")
        let inboxItem = SocialInboxItem(id: inboxId, shareId: shareId, targetUserId: target, readAt: nil, createdAt: "2026-08-18T00:00:00Z", share: share)

        let publishTransport = MockTransport(statusCode: 201)
        let publishService = SocialService(configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"), transport: publishTransport)
        try await publishService.publish(entityType: "recipe", entityId: entity, fromName: "Ana", fromHandle: "ana", name: "V60", subtitle: "Dulzor", message: "Para ti", payload: payload, visibility: "DIRECT", targetUserId: target, accessToken: "jwt")
        let body = try XCTUnwrap((JSONSerialization.jsonObject(with: try XCTUnwrap(publishTransport.requests.first?.httpBody)) as? [String: Any]))
        XCTAssertEqual(body["visibility"] as? String, "direct"); XCTAssertEqual(body["target_user_id"] as? String, target.uuidString)

        let inboxTransport = MockTransport(responseData: try JSONEncoder().encode([inboxItem]))
        let inboxService = SocialService(configuration: publishService.configuration, transport: inboxTransport)
        let receivedInbox = try await inboxService.inbox(userId: target, accessToken: "jwt")
        XCTAssertEqual(receivedInbox, [inboxItem])
        XCTAssertTrue(inboxTransport.requests.first?.url?.query?.contains("target_user_id=eq.\(target.uuidString)") == true)

        let readTransport = MockTransport(statusCode: 204)
        try await SocialService(configuration: publishService.configuration, transport: readTransport).markInboxRead(itemId: inboxId, accessToken: "jwt", date: Date(timeIntervalSince1970: 0))
        XCTAssertEqual(readTransport.requests.first?.httpMethod, "PATCH"); XCTAssertTrue(readTransport.requests.first?.url?.query?.contains("id=eq.\(inboxId.uuidString)") == true)

        let activity = SocialActivity(id: UUID(), userId: target, action: "import_share", entityType: "recipe", entityId: entity, shareId: shareId, note: "Registraste una copia", createdAt: "2026-08-18T00:00:00Z")
        let activityTransport = MockTransport(responseData: try JSONEncoder().encode([activity]))
        let receivedActivity = try await SocialService(configuration: publishService.configuration, transport: activityTransport).activity(userId: target, accessToken: "jwt")
        XCTAssertEqual(receivedActivity, [activity])
    }
}

final class EntitySyncTests: XCTestCase {
    func testAndroidBackendContractUsesOfficialSharedTablesAndOwnership() throws {
        let shared: [String: (String, String)] = [
            "CoffeeBeanRecord": ("beans", "user_id"), "GrinderRecord": ("grinders", "user_id"),
            "EquipmentRecord": ("equipment", "user_id"), "RecipeRecord": ("recipes", "user_id"),
            "TechniqueRecord": ("techniques", "user_id"), "TechniqueStepRecord": ("technique_steps", "user_id"),
            "LabExperimentRecord": ("lab_experiments", "user_id")
        ]
        for (entity, expected) in shared {
            let descriptor = try XCTUnwrap(CoreSyncSchema.descriptors.first { $0.entityName == entity })
            XCTAssertEqual(descriptor.table, expected.0); XCTAssertEqual(descriptor.ownerField, expected.1)
        }
        XCTAssertEqual(CoreSyncSchema.descriptors.first { $0.entityName == "RecipeIngredientRecord" }?.ownerField, "owner_id")

        let attributionFields = ["is_shared", "original_author_user_id", "original_author_name", "original_entity_id", "root_entity_id", "imported_from_share_id", "copy_mode"]
        for entity in ["RecipeRecord", "TechniqueRecord"] {
            let remoteFields = Set(try XCTUnwrap(CoreSyncSchema.descriptors.first { $0.entityName == entity }).fields.map(\.remote))
            XCTAssertTrue(Set(attributionFields).isSubset(of: remoteFields), "\(entity) no conserva toda la atribución Android")
        }

        let base = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent().appendingPathComponent("supabase/migrations")
        let files = ["202608160000_android_schema_preflight.sql", "202608170003_lab_and_brew_references.sql", "202609010007_android_backend_alignment.sql"]
        let sql = try files.map { try String(contentsOf: base.appendingPathComponent($0), encoding: .utf8) }.joined(separator: "\n").lowercased()
        for clause in ["create table if not exists public.beans", "from public.coffee_beans", "create table if not exists public.shares", "references public.shares(id)", "beans_align_clients", "shares_moderate_content", "beans_user_all"] {
            XCTAssertTrue(sql.contains(clause), "Falta contrato SQL Android/iOS: \(clause)")
        }
        XCTAssertFalse(sql.contains("service_role")); XCTAssertFalse(sql.contains("supabase_service"))
    }

    @MainActor func testLocalAccountScopeSeparatesRecordsOutboxAndActiveWork() throws {
        let ownerA = UUID(); let ownerB = UUID(); let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext
        defer { LocalDataScope.activeOwnerId = nil }

        context.activeOwnerId = ownerA
        let beanA = CoffeeBeanRecord(context: context, name: "Sólo A", brand: "A", remainingQuantityGrams: 100)
        context.activeOwnerId = ownerB
        let beanB = CoffeeBeanRecord(context: context, name: "Sólo B", brand: "B", remainingQuantityGrams: 100)
        context.activeOwnerId = nil
        let guest = CoffeeBeanRecord(context: context, name: "Invitado", brand: "Local", remainingQuantityGrams: 100)
        try context.save()
        XCTAssertEqual(beanA.ownerId, ownerA); XCTAssertEqual(beanB.ownerId, ownerB); XCTAssertNil(guest.ownerId)

        func visible(_ owner: UUID?) throws -> Set<String> {
            let request = NSFetchRequest<CoffeeBeanRecord>(entityName: "CoffeeBeanRecord")
            request.predicate = LocalDataScope.visiblePredicate(activeOwnerId: owner)
            return Set(try context.fetch(request).map(\.name))
        }
        XCTAssertEqual(try visible(ownerA), ["Sólo A", "Invitado"])
        XCTAssertEqual(try visible(ownerB), ["Sólo B", "Invitado"])
        XCTAssertEqual(try visible(nil), ["Invitado"])

        let outbox = SyncOutboxRepository(context: context)
        _ = try outbox.enqueue(entityName: "coffee_beans", entityId: beanA.id, ownerId: ownerA, operation: .pendingCreate, payloadJSON: "[]")
        _ = try outbox.enqueue(entityName: "coffee_beans", entityId: beanB.id, ownerId: ownerB, operation: .pendingCreate, payloadJSON: "[]")
        XCTAssertEqual(try outbox.ready(ownerId: ownerA).map(\.ownerId), [ownerA])
        XCTAssertEqual(try outbox.ready(ownerId: ownerB).map(\.ownerId), [ownerB])

        let suiteName = "LocalScopeTests.\(UUID().uuidString)"; let defaults = try XCTUnwrap(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }
        LocalDataScope.activeOwnerId = ownerA
        let calculator = CalculatorModel(defaults: defaults); calculator.changeCoffee("21")
        let preparation = PreparationModel(defaults: defaults); preparation.load(calculator: calculator)
        let lab = LabModel(defaults: defaults); lab.update { $0.waterMl = 333; $0.notes = "Hipótesis A" }
        let tasting = TastingModel(defaults: defaults); tasting.state.freeNotes = "Cata A"
        calculator.switchScope(to: ownerB); preparation.switchScope(to: ownerB); lab.switchScope(to: ownerB); tasting.switchScope(to: ownerB)
        XCTAssertEqual(calculator.coffee, 15); XCTAssertEqual(preparation.state.techniqueName, "Preparación libre")
        XCTAssertEqual(lab.state.waterMl, 240); XCTAssertTrue(tasting.state.freeNotes.isEmpty)
        calculator.changeCoffee("18"); lab.update { $0.waterMl = 280; $0.notes = "Hipótesis B" }; tasting.state.freeNotes = "Cata B"
        calculator.switchScope(to: ownerA); preparation.switchScope(to: ownerA); lab.switchScope(to: ownerA); tasting.switchScope(to: ownerA)
        XCTAssertEqual(calculator.coffee, 21); XCTAssertTrue(preparation.state.techniqueName.contains("V60"))
        XCTAssertEqual(lab.state.waterMl, 333); XCTAssertEqual(lab.state.notes, "Hipótesis A"); XCTAssertEqual(tasting.state.freeNotes, "Cata A")
        calculator.switchScope(to: ownerB); lab.switchScope(to: ownerB); tasting.switchScope(to: ownerB)
        XCTAssertEqual(calculator.coffee, 18); XCTAssertEqual(lab.state.waterMl, 280); XCTAssertEqual(tasting.state.freeNotes, "Cata B")

        defaults.set("legado", forKey: "scope.legacy")
        XCTAssertEqual(LocalDataScope.migrateLegacyObject(in: defaults, baseKey: "scope.legacy", ownerId: ownerA) as? String, "legado")
        XCTAssertNil(defaults.object(forKey: "scope.legacy"))
        XCTAssertEqual(defaults.string(forKey: LocalDataScope.scopedKey("scope.legacy", ownerId: ownerA)), "legado")
    }

    @MainActor func testPendingOwnershipEncodingRemoteMergeAndJSONSnapshots() throws {
        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext; let owner = UUID()
        let bean = CoffeeBeanRecord(context: context, name: "Local", brand: "Tostador", origin: "México", remainingQuantityGrams: 200)
        let brewState = PreparationState(techniqueName: "V60", methodName: "V60", steps: [.init(id: UUID(), number: 1, title: "Bloom", durationSeconds: 45, waterAddedMl: 50, waterAccumulatedMl: 50, gesture: "BLOOM", intensity: "HIGH", note: "")], elapsedSeconds: 45, status: .completed)
        _ = BrewSessionRecord(context: context, state: brewState, beanName: "Local", grinderName: "C40")
        try context.save()
        let coordinator = EntitySyncCoordinator(context: context, configuration: .init(supabaseURL: nil, supabaseAnonKey: nil), defaults: UserDefaults(suiteName: "EntitySyncTests.\(UUID().uuidString)")!)
        try coordinator.enqueuePending(ownerId: owner)
        XCTAssertEqual(bean.ownerId, owner)
        let outbox = try context.fetch(NSFetchRequest<SyncOperationRecord>(entityName: "SyncOperationRecord"))
        XCTAssertEqual(outbox.count, 2)
        let coffeePayload = try XCTUnwrap(outbox.first { $0.entityName == "beans" }?.payloadJSON.data(using: .utf8))
        var coffeeRow = try XCTUnwrap((JSONSerialization.jsonObject(with: coffeePayload) as? [[String: Any]])?.first)
        XCTAssertEqual(coffeeRow["user_id"] as? String, owner.uuidString)
        XCTAssertEqual(coffeeRow["roaster"] as? String, "Tostador"); XCTAssertEqual(coffeeRow["stock_grams"] as? Double, 200)
        XCTAssertNil(coffeeRow["owner_id"]); XCTAssertNil(coffeeRow["brand"]); XCTAssertNil(coffeeRow["remaining_quantity_grams"])
        coffeeRow["name"] = "Remoto"; coffeeRow["updated_at"] = "2099-08-17T00:00:00Z"; coffeeRow["version"] = 8
        let descriptor = try XCTUnwrap(CoreSyncSchema.descriptors.first { $0.entityName == "CoffeeBeanRecord" })
        try coordinator.merge(coffeeRow, descriptor: descriptor, expectedOwner: owner)
        XCTAssertEqual(bean.name, "Remoto"); XCTAssertEqual(bean.syncStatus, .synced); XCTAssertEqual(bean.version, 8)

        let brewPayload = try XCTUnwrap(outbox.first { $0.entityName == "brew_sessions" }?.payloadJSON.data(using: .utf8))
        let brewRow = try XCTUnwrap((JSONSerialization.jsonObject(with: brewPayload) as? [[String: Any]])?.first)
        XCTAssertTrue(brewRow["steps_snapshot"] is [[String: Any]])
        var foreign = coffeeRow; foreign["user_id"] = UUID().uuidString; foreign["name"] = "Intruso"
        try coordinator.merge(foreign, descriptor: descriptor, expectedOwner: owner); XCTAssertEqual(bean.name, "Remoto")
    }

    @MainActor func testRecipeAttributionAndEnumsEncodeForAndroid() throws {
        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext; let owner = UUID()
        let originalAuthor = UUID(); let originalEntity = UUID(); let importedShare = UUID()
        let recipe = RecipeRecord(context: context, name: "Copia atribuida", recipeKind: "BLACK_COFFEE", intention: "Balance", suggestedMethodId: nil, suggestedMethodName: "V60", isFavorite: false, tags: "")
        recipe.ownerId = owner; recipe.visibility = "PRIVATE"; recipe.isShared = true
        recipe.originalAuthorUserId = originalAuthor; recipe.originalAuthorName = "Ana"
        recipe.originalEntityId = originalEntity; recipe.rootEntityId = originalEntity
        recipe.importedFromShareId = importedShare; recipe.copyMode = "FORK"
        try context.save()

        let coordinator = EntitySyncCoordinator(context: context, configuration: .init(supabaseURL: nil, supabaseAnonKey: nil), defaults: UserDefaults(suiteName: "AttributionSync.\(UUID().uuidString)")!)
        let descriptor = try XCTUnwrap(CoreSyncSchema.descriptors.first { $0.entityName == "RecipeRecord" })
        let data = try coordinator.encode(recipe, descriptor: descriptor, ownerId: owner)
        let row = try XCTUnwrap((JSONSerialization.jsonObject(with: data) as? [[String: Any]])?.first)
        XCTAssertEqual(row["visibility"] as? String, "private"); XCTAssertEqual(row["copy_mode"] as? String, "fork")
        XCTAssertEqual(row["is_shared"] as? Bool, true)
        XCTAssertEqual(row["original_author_user_id"] as? String, originalAuthor.uuidString)
        XCTAssertEqual(row["original_author_name"] as? String, "Ana")
        XCTAssertEqual(row["original_entity_id"] as? String, originalEntity.uuidString)
        XCTAssertEqual(row["root_entity_id"] as? String, originalEntity.uuidString)
        XCTAssertEqual(row["imported_from_share_id"] as? String, importedShare.uuidString)
    }

    @MainActor func testEndToEndSyncPushesThenPullsEveryDescriptor() async throws {
        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext; let owner = UUID()
        let bean = CoffeeBeanRecord(context: context, name: "Pendiente", brand: "Tostador"); try context.save()
        let transport = MockTransport(responseData: Data("[]".utf8), statusCode: 200)
        let coordinator = EntitySyncCoordinator(context: context, configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"), transport: transport, defaults: UserDefaults(suiteName: "EndToEndSync.\(UUID().uuidString)")!)
        await coordinator.sync(ownerId: owner, accessToken: "jwt")
        guard case .completed = coordinator.state else { return XCTFail("La sincronización no terminó: \(coordinator.state)") }
        XCTAssertEqual(bean.syncStatus, .synced); XCTAssertEqual(bean.ownerId, owner)
        XCTAssertTrue(transport.requests.contains { $0.httpMethod == "POST" && $0.url?.path == "/rest/v1/beans" })
        let pulledTables = Set(transport.requests.filter { $0.httpMethod == "GET" }.compactMap { $0.url?.lastPathComponent })
        XCTAssertEqual(pulledTables, Set(CoreSyncSchema.descriptors.map(\.table)))
    }

    @MainActor func testOfflineSyncIsRecoverableAndKeepsOutbox() async throws {
        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext; let owner = UUID()
        _ = CoffeeBeanRecord(context: context, name: "Pendiente offline", brand: "Tostador"); try context.save()
        let coordinator = EntitySyncCoordinator(
            context: context,
            configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"),
            transport: MockTransport(error: URLError(.networkConnectionLost)),
            defaults: UserDefaults(suiteName: "OfflineSync.\(UUID().uuidString)")!
        )
        await coordinator.sync(ownerId: owner, accessToken: "jwt")
        XCTAssertEqual(coordinator.state, .offline)
        XCTAssertFalse(try context.fetch(NSFetchRequest<SyncOperationRecord>(entityName: "SyncOperationRecord")).isEmpty)
    }
}

final class NavigationAndThemeTests: XCTestCase {
    func testWarmSpecialtyCanvasUsesCanonicalLightTokens() {
        XCTAssertEqual(CupaPalette.Light.background, 0xF7F5F0)
        XCTAssertEqual(CupaPalette.Light.backgroundAlt, 0xEFECE6)
        XCTAssertEqual(CupaPalette.Light.card, 0xFFFFFF)
        XCTAssertEqual(CupaPalette.Light.border, 0xE6DFD5)
        XCTAssertEqual(CupaPalette.Light.text, 0x1E1A17)
        XCTAssertEqual(CupaPalette.Light.terracotta, 0xC26638)
    }

    @MainActor func testFiveReferenceTabsKeepSharedFeatureState() throws {
        XCTAssertEqual(CupaTab.allCases.map(\.title), ["Taller", "Preparar", "Cata", "Laboratorio", "Almacén"])
        let suite = "NavigationAndThemeTests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let navigation = AppNavigationModel()
        let calculator = CalculatorModel(defaults: defaults)
        calculator.changeCoffee("18")
        for tab in CupaTab.allCases { navigation.select(tab); XCTAssertEqual(navigation.selection, tab) }
        XCTAssertEqual(calculator.coffee, 18)
        XCTAssertEqual(calculator.coffeeInput, "18")
    }

    func testBrandPaletteMeetsWCAGNormalTextContrast() {
        let lightForegrounds = [CupaPalette.Light.text, CupaPalette.Light.secondaryText, CupaPalette.Light.forest, CupaPalette.Light.terracottaText, CupaPalette.Light.gold, CupaPalette.Light.espresso, CupaPalette.Light.clarity]
        let darkForegrounds = [CupaPalette.Dark.text, CupaPalette.Dark.secondaryText, CupaPalette.Dark.forest, CupaPalette.Dark.terracottaText, CupaPalette.Dark.gold, CupaPalette.Dark.espresso, CupaPalette.Dark.clarity]
        for foreground in lightForegrounds {
            XCTAssertGreaterThanOrEqual(contrast(foreground, CupaPalette.Light.background), 4.5)
            XCTAssertGreaterThanOrEqual(contrast(foreground, CupaPalette.Light.card), 4.5)
        }
        for foreground in darkForegrounds {
            XCTAssertGreaterThanOrEqual(contrast(foreground, CupaPalette.Dark.background), 4.5)
            XCTAssertGreaterThanOrEqual(contrast(foreground, CupaPalette.Dark.card), 4.5)
        }
        for accent in [CupaPalette.Light.forest, CupaPalette.Light.gold, CupaPalette.Light.espresso, CupaPalette.Light.clarity] {
            XCTAssertGreaterThanOrEqual(contrast(CupaPalette.Light.onAccent, accent), 4.5)
        }
        XCTAssertGreaterThanOrEqual(contrast(CupaPalette.Light.onTerracotta, CupaPalette.Light.terracotta), 4.5)
        for accent in [CupaPalette.Dark.forest, CupaPalette.Dark.gold, CupaPalette.Dark.espresso, CupaPalette.Dark.clarity] {
            XCTAssertGreaterThanOrEqual(contrast(CupaPalette.Dark.onAccent, accent), 4.5)
        }
        XCTAssertGreaterThanOrEqual(contrast(CupaPalette.Dark.onTerracotta, CupaPalette.Dark.terracotta), 4.5)
    }

    private func contrast(_ first: UInt, _ second: UInt) -> Double {
        let brighter = max(luminance(first), luminance(second))
        let darker = min(luminance(first), luminance(second))
        return (brighter + 0.05) / (darker + 0.05)
    }

    private func luminance(_ hex: UInt) -> Double {
        let channels = [Double((hex >> 16) & 0xff), Double((hex >> 8) & 0xff), Double(hex & 0xff)].map { value -> Double in
            let normalized = value / 255
            return normalized <= 0.04045 ? normalized / 12.92 : pow((normalized + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2]
    }
}
