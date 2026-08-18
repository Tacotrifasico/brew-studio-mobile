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

final class SocialContentPolicyTests: XCTestCase {
    private let safePayload = SharePayloadSnapshot(
        kind: "recipe",
        recipe: SharedRecipeSnapshot(name: "V60 dulce", recipeKind: "Brew", intention: "Balance", suggestedMethodName: "V60", tags: "frutal", ingredients: [], steps: []),
        technique: nil
    )

    func testAllowsCoffeeContent() throws {
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
        let restored = CalculatorModel(defaults: defaults)
        XCTAssertEqual(restored.savedPresets.first?.coffee, 18)
        XCTAssertEqual(restored.coffee, 18)
        XCTAssertEqual(restored.ratio, 15)
        XCTAssertEqual(restored.water, 270)
        calculator.toggleFavorite()
        XCTAssertFalse(calculator.isCurrentFavorite)
        XCTAssertTrue(CalculatorModel(defaults: defaults).savedPresets.isEmpty)
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

private final class MemoryTokenStore: TokenStore {
    var value: AuthTokens?
    func load() throws -> AuthTokens? { value }
    func save(_ tokens: AuthTokens) throws { value = tokens }
    func clear() throws { value = nil }
}

private final class MockTransport: NetworkTransport {
    var requests: [URLRequest] = []; var responseData: Data; var statusCode: Int
    init(responseData: Data = Data(), statusCode: Int = 200) { self.responseData = responseData; self.statusCode = statusCode }
    func data(for request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        requests.append(request)
        return (responseData, HTTPURLResponse(url: request.url!, statusCode: statusCode, httpVersion: nil, headerFields: nil)!)
    }
}

final class AccountAndSyncTests: XCTestCase {
    @MainActor func testAccountIsUnavailableWithoutPublicConfiguration() {
        let model = AccountModel(configuration: .init(supabaseURL: nil, supabaseAnonKey: nil), transport: MockTransport(), store: MemoryTokenStore())
        XCTAssertEqual(model.state, .unavailable)
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

    func testAccountDeletionUsesAuthenticatedEdgeFunction() async throws {
        let transport = MockTransport(responseData: Data("{\"deleted\":true}".utf8))
        let service = SupabaseAccountService(configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"), transport: transport)
        try await service.deleteAccount(accessToken: "user-jwt", confirmation: "ELIMINAR")
        XCTAssertEqual(transport.requests.first?.url?.path, "/functions/v1/delete-account")
        XCTAssertEqual(transport.requests.first?.value(forHTTPHeaderField: "Authorization"), "Bearer user-jwt")
        XCTAssertFalse(String(data: transport.requests.first?.httpBody ?? Data(), encoding: .utf8)?.contains("service_role") == true)
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
        let first = SettingsModel(defaults: defaults); first.theme = .dark; first.temperatureUnit = .fahrenheit; first.metricUnits = false
        let restored = SettingsModel(defaults: defaults)
        XCTAssertEqual(restored.theme, .dark); XCTAssertEqual(restored.temperatureUnit, .fahrenheit); XCTAssertFalse(restored.metricUnits)
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
}

final class SocialTests: XCTestCase {
    @MainActor func testFeedContractImportAndAttribution() async throws {
        let originalId = UUID(); let ownerId = UUID()
        let recipe = SharedRecipeSnapshot(name: "V60 comunitaria", recipeKind: "BLACK_COFFEE", intention: "Dulzor", suggestedMethodName: "V60", tags: "frutal", ingredients: [.init(name: "Café", amount: 15, unit: "GRAMS")], steps: [.init(instruction: "Bloom", durationSeconds: 45)])
        let share = SocialShare(id: UUID(), ownerId: ownerId, entityType: "recipe", entityId: originalId, fromName: "Barista", fromHandle: "barista", targetUserId: nil, visibility: "PUBLIC", name: recipe.name, subtitle: recipe.intention, message: "Prueba", payloadSnapshot: .init(kind: "recipe", recipe: recipe, technique: nil), originalEntityId: originalId, status: "ACTIVE", createdAt: "2026-08-17T00:00:00Z", updatedAt: "2026-08-17T00:00:00Z")
        let transport = MockTransport(responseData: try JSONEncoder().encode([share]))
        let service = SocialService(configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"), transport: transport)
        let feed = try await service.feed(accessToken: "user-jwt")
        XCTAssertEqual(feed, [share]); XCTAssertEqual(transport.requests.first?.url?.path, "/rest/v1/brew_shares")
        XCTAssertTrue(transport.requests.first?.url?.query?.contains("visibility=eq.PUBLIC") == true)

        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext
        try service.importShare(share, context: context)
        let imported = try context.fetch(NSFetchRequest<RecipeRecord>(entityName: "RecipeRecord"))
        XCTAssertEqual(imported.first?.name, "Copia de V60 comunitaria"); XCTAssertEqual(imported.first?.originalEntityId, originalId); XCTAssertEqual(imported.first?.copyMode, "IMPORT")
        XCTAssertEqual(try RecipeTechniqueRepository(context: context).ingredients(recipeId: try XCTUnwrap(imported.first?.id)).count, 1)
    }

    func testPublishReportAndBlockContractsDoNotExposeEmail() async throws {
        let transport = MockTransport(statusCode: 201); let service = SocialService(configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"), transport: transport)
        let entityId = UUID(); let payload = SharePayloadSnapshot(kind: "recipe", recipe: .init(name: "V60", recipeKind: "BLACK_COFFEE", intention: "", suggestedMethodName: "V60", tags: "", ingredients: [], steps: []), technique: nil)
        try await service.publish(entityType: "recipe", entityId: entityId, fromName: "Barista", fromHandle: "brew", name: "V60", subtitle: "", message: "", payload: payload, accessToken: "jwt")
        let publishBody = String(data: try XCTUnwrap(transport.requests.first?.httpBody), encoding: .utf8) ?? ""
        XCTAssertFalse(publishBody.contains("email")); XCTAssertTrue(publishBody.contains("payload_snapshot"))
        try await service.report(shareId: UUID(), reason: "USER_REPORTED", accessToken: "jwt")
        try await service.block(userId: UUID(), accessToken: "jwt")
        XCTAssertEqual(transport.requests.map { $0.url?.path }, ["/rest/v1/brew_shares", "/rest/v1/content_reports", "/rest/v1/blocked_users"])
    }
}

final class EntitySyncTests: XCTestCase {
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
        let coffeePayload = try XCTUnwrap(outbox.first { $0.entityName == "coffee_beans" }?.payloadJSON.data(using: .utf8))
        var coffeeRow = try XCTUnwrap((JSONSerialization.jsonObject(with: coffeePayload) as? [[String: Any]])?.first)
        XCTAssertEqual(coffeeRow["owner_id"] as? String, owner.uuidString)
        coffeeRow["name"] = "Remoto"; coffeeRow["updated_at"] = "2099-08-17T00:00:00Z"; coffeeRow["version"] = 8
        let descriptor = try XCTUnwrap(CoreSyncSchema.descriptors.first { $0.entityName == "CoffeeBeanRecord" })
        try coordinator.merge(coffeeRow, descriptor: descriptor, expectedOwner: owner)
        XCTAssertEqual(bean.name, "Remoto"); XCTAssertEqual(bean.syncStatus, .synced); XCTAssertEqual(bean.version, 8)

        let brewPayload = try XCTUnwrap(outbox.first { $0.entityName == "brew_sessions" }?.payloadJSON.data(using: .utf8))
        let brewRow = try XCTUnwrap((JSONSerialization.jsonObject(with: brewPayload) as? [[String: Any]])?.first)
        XCTAssertTrue(brewRow["steps_snapshot"] is [[String: Any]])
        var foreign = coffeeRow; foreign["owner_id"] = UUID().uuidString; foreign["name"] = "Intruso"
        try coordinator.merge(foreign, descriptor: descriptor, expectedOwner: owner); XCTAssertEqual(bean.name, "Remoto")
    }

    @MainActor func testEndToEndSyncPushesThenPullsEveryDescriptor() async throws {
        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext; let owner = UUID()
        let bean = CoffeeBeanRecord(context: context, name: "Pendiente", brand: "Tostador"); try context.save()
        let transport = MockTransport(responseData: Data("[]".utf8), statusCode: 200)
        let coordinator = EntitySyncCoordinator(context: context, configuration: .init(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon"), transport: transport, defaults: UserDefaults(suiteName: "EndToEndSync.\(UUID().uuidString)")!)
        await coordinator.sync(ownerId: owner, accessToken: "jwt")
        guard case .completed = coordinator.state else { return XCTFail("La sincronización no terminó: \(coordinator.state)") }
        XCTAssertEqual(bean.syncStatus, .synced); XCTAssertEqual(bean.ownerId, owner)
        XCTAssertTrue(transport.requests.contains { $0.httpMethod == "POST" && $0.url?.path == "/rest/v1/coffee_beans" })
        let pulledTables = Set(transport.requests.filter { $0.httpMethod == "GET" }.compactMap { $0.url?.lastPathComponent })
        XCTAssertEqual(pulledTables, Set(CoreSyncSchema.descriptors.map(\.table)))
    }
}
