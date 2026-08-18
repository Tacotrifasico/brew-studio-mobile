import Foundation
import CoreData

@main
struct LabGoldenVerifier {
    @MainActor static func main() async {
        verify(
            name: "nivel-del-mar",
            input: LabState(),
            extraction: 0.95,
            scores: [64, 55, 87, 49, 32, 57]
        )
        verify(
            name: "cdmx-hervor",
            input: LabState(temperatureC: 98, altitudeMeters: 2240, cityName: "CDMX (2,240m)"),
            extraction: 0.919_259_2,
            scores: [64, 56, 84, 49, 33, 56]
        )
        verify(
            name: "sub-extraccion",
            input: LabState(waterMl: 270, ratio: 18, temperatureC: 84, grindClicks: 32, freshness: "viejo", timeSeconds: 80),
            extraction: 0.45,
            scores: [52, 82, 41, 48, 32, 45]
        )
        verify(
            name: "sobre-extraccion",
            input: LabState(coffeeGrams: 20, waterMl: 200, ratio: 10, temperatureC: 98, grindClicks: 8, freshness: "muy fresco", timeSeconds: 320),
            extraction: 1.65,
            scores: [78, 22, 32, 76, 84, 42]
        )
        verifyStateRestoration()
        verifyAltitudeCatalogAndTemperaturePreference()
        verifyCoffeeFreshnessParity()
        verifyCoffeeInputValidation()
        verifyCoffeeInventoryActions()
        verifySQLiteReopening()
        verifyPersistentStoreRecovery()
        verifyCalculatorFavorites()
        verifyCalculatorQuickPreparation()
        verifyTransfersAndHistoricalSnapshots()
        verifyLocalPersistence()
        verifyRecipeTechniqueAggregates()
        verifyRecipeTextImport()
        verifyPreparationRecovery()
        verifyTastingCoolingAndPersistence()
        verifyAppConfiguration()
        verifySyncConflictAndOutbox()
        verifyLocalSuggestionFallback()
        verifyProfilePersistence()
        verifySocialContentPolicy()
        verifySocialImportAttribution()
        verifyEntitySyncMapping()
        await verifySessionRecovery()
        print("4 golden tests, altitud/unidades, frescura, inventario, reapertura SQLite, agregados, importación de recetas, preparación, cata, ambientes, sesión offline, sincronización, IA, perfil y social aprobados")
    }

    private static func verify(name: String, input: LabState, extraction: Float, scores: [Int]) {
        let output = LabEngine.calculate(input)
        let actual = [output.aroma, output.acidity, output.sweetness, output.body, output.bitterness, output.finish]
        precondition(abs(output.extractionIndex - extraction) <= 0.000_01, "\(name): índice \(output.extractionIndex)")
        precondition(actual == scores, "\(name): esperado \(scores), recibido \(actual)")
    }

    private static func verifyAppConfiguration() {
        let empty = AppConfiguration(environment: ["SUPABASE_URL": "", "SUPABASE_ANON_KEY": "   "])
        precondition(!empty.isSupabaseConfigured && empty.supabaseURL == nil && empty.supabaseAnonKey == nil)
        let insecure = AppConfiguration(environment: ["SUPABASE_URL": "http://example.com", "SUPABASE_ANON_KEY": "public-key"])
        precondition(!insecure.isSupabaseConfigured && insecure.supabaseURL == nil)
        let production = AppConfiguration(environment: ["SUPABASE_URL": " https://project.supabase.co ", "SUPABASE_ANON_KEY": " public-key "])
        precondition(production.supabaseURL?.absoluteString == "https://project.supabase.co" && production.supabaseAnonKey == "public-key")
        let local = AppConfiguration(environment: ["SUPABASE_URL": "http://127.0.0.1:54321", "SUPABASE_ANON_KEY": "local-key"])
        precondition(local.isSupabaseConfigured)
    }

    @MainActor private static func verifyCoffeeInventoryActions() {
        let suite = "CupaCoffeeActionsVerifier.\(UUID().uuidString)"; let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext
        let roastDate = Date(timeIntervalSince1970: 1_700_000_000)
        let bean = CoffeeBeanRecord(
            context: context, name: "Guji", brand: "Casa", process: "Lavado",
            roastDate: roastDate, initialQuantityGrams: 250, remainingQuantityGrams: 250, notes: "Jazmín"
        )
        precondition(bean.inventoryStatus == .closed)
        let lab = LabModel(defaults: defaults); lab.load(bean: bean, now: roastDate.addingTimeInterval(5 * 86_400))
        precondition(lab.state.beanId == bean.id && lab.state.freshness == "muy fresco")
        precondition(lab.state.notes == "Grano: Guji. Proceso: Lavado. Jazmín")
        let preparation = PreparationModel(defaults: defaults); preparation.selectBean(bean)
        precondition(preparation.state.beanId == bean.id && PreparationModel(defaults: defaults).state.beanId == bean.id)
        bean.openedDate = roastDate.addingTimeInterval(5 * 86_400); precondition(bean.inventoryStatus == .open)
        bean.remainingQuantityGrams = 0; precondition(bean.inventoryStatus == .finished)
    }

    private static func verifyCoffeeInputValidation() {
        var calendar = Calendar(identifier: .gregorian); calendar.timeZone = TimeZone(secondsFromGMT: 0)!
        let now = Date(timeIntervalSince1970: 1_776_643_200)
        let roast = calendar.date(byAdding: .day, value: -10, to: now)!
        let opened = calendar.date(byAdding: .day, value: -2, to: now)!
        let valid = try! CoffeeBeanInputValidator.validate(
            altitudeText: "1850", initialQuantityText: "250,5", remainingQuantityText: "125.25",
            roastDate: roast, openedDate: opened, now: now, calendar: calendar
        )
        precondition(valid == .init(altitudeMeters: 1_850, initialQuantityGrams: 250.5, remainingQuantityGrams: 125.25))
        do {
            _ = try CoffeeBeanInputValidator.validate(altitudeText: "alto", initialQuantityText: "250", remainingQuantityText: "100", roastDate: nil, openedDate: nil)
            preconditionFailure("La altitud inválida debió rechazarse")
        } catch { precondition(error as? CoffeeBeanInputError == .invalidAltitude) }
        do {
            _ = try CoffeeBeanInputValidator.validate(altitudeText: "", initialQuantityText: "250", remainingQuantityText: "251", roastDate: nil, openedDate: nil)
            preconditionFailure("El inventario incoherente debió rechazarse")
        } catch { precondition(error as? CoffeeBeanInputError == .remainingExceedsInitial) }
        do {
            _ = try CoffeeBeanInputValidator.validate(altitudeText: "", initialQuantityText: "250", remainingQuantityText: "100", roastDate: roast, openedDate: calendar.date(byAdding: .day, value: -11, to: now), now: now, calendar: calendar)
            preconditionFailure("La apertura anterior al tueste debió rechazarse")
        } catch { precondition(error as? CoffeeBeanInputError == .openedBeforeRoast) }
    }

    private static func verifyStateRestoration() {
        let suite = "CupaGoldenVerifier.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let first = LabModel(defaults: defaults)
        first.setManualAltitude(2240, city: "CDMX")
        first.update { $0.timeSeconds = 205 }
        first.setTemperatureUnit(.fahrenheit)
        let restored = LabModel(defaults: defaults)
        precondition(restored.state.altitudeMeters == 2240)
        precondition(restored.state.temperatureUnit == .fahrenheit)
        precondition(restored.state.timeSeconds == 205)
        precondition(defaults.string(forKey: "settings.temperature") == TemperatureUnit.fahrenheit.rawValue)
    }

    private static func verifyAltitudeCatalogAndTemperaturePreference() {
        precondition(LabModel.cities.map(\.altitudeMeters) == [0, 50, 100, 760, 1495, 1500, 1170, 2240, 2355, 2600, 3399, 3640])
        precondition(LabModel.cities.map(\.label) == [
            "Costa / Mar", "Seattle / Tokio", "Roma / Paris", "São Paulo", "Medellín", "Guatemala",
            "San José CR", "CDMX / Oaxaca", "Addis Abeba", "Bogotá", "Cusco", "La Paz"
        ])
        precondition(abs(LabEngine.boilingPointC(altitudeMeters: -1) - 100) < 0.000_1)
        precondition(abs(LabEngine.boilingPointC(altitudeMeters: 5_000) - 83) < 0.000_1)
        precondition(Int(roundf(LabEngine.fahrenheit(fromCelsius: LabEngine.boilingPointC(altitudeMeters: 2240)))) == 198)
        let cdmx = LabModel.cities.first { $0.altitudeMeters == 2240 }!
        precondition(cdmx.isSelected(altitudeMeters: 2240, cityName: "CDMX (2,240m)"))
        precondition(!cdmx.isSelected(altitudeMeters: 2240, cityName: "Manual (2240m)"))
    }

    private static func verifyCoffeeFreshnessParity() {
        var calendar = Calendar(identifier: .gregorian); calendar.timeZone = TimeZone(secondsFromGMT: 0)!
        let now = Date(timeIntervalSince1970: 1_776_643_200)
        func daysAgo(_ days: Int) -> Date { calendar.date(byAdding: .day, value: -days, to: now)! }
        let cases: [(Int, CoffeeFreshnessState, Double)] = [
            (0, .veryFresh, 0), (7, .veryFresh, 0.2), (8, .inWindow, 0.2 + 0.2 / 14),
            (21, .inWindow, 0.4), (22, .ideal, 0.4 + 0.2 / 14), (35, .ideal, 0.6),
            (36, .declining, 0.608), (60, .declining, 0.8), (61, .old, 0.81), (80, .old, 1)
        ]
        for item in cases {
            let result = CoffeeFreshnessEngine.evaluate(roastDate: daysAgo(item.0), openedDate: nil, now: now, calendar: calendar)
            precondition(result.daysFromRoast == item.0 && result.state == item.1)
            precondition(abs(result.progress - item.2) < 0.000_001)
        }
        let future = CoffeeFreshnessEngine.evaluate(roastDate: daysAgo(-1), openedDate: nil, now: now, calendar: calendar)
        precondition(future.state == .noDate && future.progress == 0)
        let opened = CoffeeFreshnessEngine.evaluate(roastDate: daysAgo(15), openedDate: daysAgo(15), now: now, calendar: calendar)
        precondition(opened.openWarning == "Abierto hace 15 días. Puede perder aroma más rápido.")
        let missing = CoffeeFreshnessEngine.evaluate(roastDate: nil, openedDate: daysAgo(20), now: now, calendar: calendar)
        precondition(missing.state == .noDate && missing.openStatusDetails == "Abierto hace 20 días" && missing.openWarning == nil)
    }

    @MainActor private static func verifySQLiteReopening() {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("CupaReopenVerifier-\(UUID().uuidString)", isDirectory: true)
        try! FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let storeURL = directory.appendingPathComponent("Cupa.sqlite")
        let beanId = UUID(); let roastDate = Date(timeIntervalSince1970: 1_775_952_000)
        var grinderId: UUID!; var equipmentId: UUID!
        autoreleasepool {
            let first = PersistenceController(storeURL: storeURL, enablePersistentHistory: false); let context = first.container.viewContext
            _ = CoffeeBeanRecord(
                context: context, id: beanId, name: "Reapertura", brand: "Tostador", origin: "Chiapas",
                roastDate: roastDate, initialQuantityGrams: 250, remainingQuantityGrams: 232
            )
            let grinder = GrinderRecord(context: context, name: "C40", brand: "Comandante", model: "MK4", grinderType: "MANUAL", scaleUnit: "CLICKS", minimumSetting: 0, maximumSetting: 40, calibrationNotes: "Cero real", notes: "Viaje")
            let equipment = EquipmentRecord(context: context, name: "V60 02", equipmentType: "BREWER_METHOD", brand: "Hario", model: "02", capacityMl: 600, configuration: "Plástico", notes: "Con servidor", isFavorite: true, isActive: true)
            grinderId = grinder.id; equipmentId = equipment.id
            try! context.save()
            try! first.container.persistentStoreCoordinator.remove(first.container.persistentStoreCoordinator.persistentStores[0])
        }
        autoreleasepool {
            let reopened = PersistenceController(storeURL: storeURL, enablePersistentHistory: false); let context = reopened.container.viewContext
            let request = NSFetchRequest<CoffeeBeanRecord>(entityName: "CoffeeBeanRecord")
            request.predicate = NSPredicate(format: "id == %@", beanId as CVarArg)
            let bean = try! context.fetch(request).first!
            precondition(bean.name == "Reapertura" && bean.origin == "Chiapas")
            precondition(bean.roastDate == roastDate && bean.remainingQuantityGrams == 232)
            let grinderRequest = NSFetchRequest<GrinderRecord>(entityName: "GrinderRecord"); grinderRequest.predicate = NSPredicate(format: "id == %@", grinderId as CVarArg)
            let grinder = try! context.fetch(grinderRequest).first!
            precondition(grinder.brand == "Comandante" && grinder.calibrationNotes == "Cero real")
            let equipmentRequest = NSFetchRequest<EquipmentRecord>(entityName: "EquipmentRecord"); equipmentRequest.predicate = NSPredicate(format: "id == %@", equipmentId as CVarArg)
            let equipment = try! context.fetch(equipmentRequest).first!
            precondition(equipment.capacityMl == 600 && equipment.isFavorite)
            try! reopened.container.persistentStoreCoordinator.remove(reopened.container.persistentStoreCoordinator.persistentStores[0])
        }
    }

    @MainActor private static func verifyPersistentStoreRecovery() {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("CupaFallbackVerifier-\(UUID().uuidString)", isDirectory: true)
        try! FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: directory) }
        let blockedParent = directory.appendingPathComponent("not-a-directory")
        try! Data("blocked".utf8).write(to: blockedParent)
        let persistence = PersistenceController(storeURL: blockedParent.appendingPathComponent("Cupa.sqlite"), enablePersistentHistory: false)
        precondition(persistence.storageRecoveryMessage != nil)
        precondition(persistence.container.persistentStoreCoordinator.persistentStores.first?.type == NSInMemoryStoreType)
        _ = CoffeeBeanRecord(context: persistence.container.viewContext, name: "Temporal", brand: "", remainingQuantityGrams: 100)
        try! persistence.container.viewContext.save()
    }

    @MainActor private static func verifyCalculatorFavorites() {
        let suite = "CupaCalculatorVerifier.\(UUID().uuidString)"; let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let calculator = CalculatorModel(defaults: defaults)
        calculator.changeCoffee("18"); calculator.changeRatio("15"); calculator.toggleFavorite()
        precondition(calculator.isCurrentFavorite)
        let restored = CalculatorModel(defaults: defaults)
        precondition(restored.savedPresets.first?.coffee == 18)
        precondition(restored.method == "V60" && restored.coffee == 18 && restored.ratio == 15 && restored.water == 270)
        precondition(restored.pinnedMethodNames == Set(["V60", "AeroPress", "Espresso", "Prensa francesa"]))
        calculator.toggleFavorite()
        precondition(CalculatorModel(defaults: defaults).savedPresets.isEmpty)
        calculator.setMethodPinned("V60", pinned: false)
        let methodId = UUID(); calculator.selectMethod("Origami", methodId: methodId)
        let customRestored = CalculatorModel(defaults: defaults)
        precondition(!customRestored.isMethodPinned("V60") && customRestored.selectedMethodId == methodId)
        let lab = LabModel(defaults: defaults); lab.load(calculator: customRestored)
        let preparation = PreparationModel(defaults: defaults); preparation.load(calculator: customRestored)
        precondition(lab.state.methodId == methodId && preparation.state.methodId == methodId)
    }

    @MainActor private static func verifyCalculatorQuickPreparation() {
        let suite = "CupaQuickPreparationVerifier.\(UUID().uuidString)"; let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let calculator = CalculatorModel(defaults: defaults); let preparation = PreparationModel(defaults: defaults)
        calculator.changeCoffee("15"); calculator.changeRatio("16"); preparation.load(calculator: calculator)
        precondition(preparation.state.techniqueName == "V60 Estándar" && preparation.state.temperatureC == 93)
        precondition(preparation.state.executionMode == "GUIDED" && preparation.state.steps.map(\.durationSeconds) == [35, 45, 40])
        precondition(preparation.state.steps.map(\.waterAddedMl) == [50, 95, 95])
        precondition(preparation.state.steps.map(\.waterAccumulatedMl) == [50, 145, 240])
        preparation.start(); let tick = preparation.state.lastTickAt!
        preparation.synchronizeClock(now: tick.addingTimeInterval(130))
        precondition(preparation.state.status == .completed && preparation.state.elapsedSeconds == 120 && preparation.state.savedAt == nil)
        precondition(PreparationModel(defaults: defaults).state.status == .completed)
        let savedSessionId = preparation.state.sessionId
        preparation.markSaved(); precondition(PreparationModel(defaults: defaults).state.status == .ready)
        preparation.reset(); precondition(preparation.state.savedAt == nil && preparation.state.sessionId != savedSessionId)
        calculator.selectMethod("AeroPress"); preparation.load(calculator: calculator)
        precondition(preparation.state.steps.map(\.title) == ["Preinfusión (Bloom)", "Vertido de volumen", "Presión continua"])
        precondition(preparation.state.steps.map(\.waterAccumulatedMl) == [40, 195, 195])
        calculator.selectMethod("Espresso"); preparation.load(calculator: calculator)
        precondition(preparation.state.steps.count == 1 && preparation.state.steps[0].durationSeconds == 30)
    }

    @MainActor private static func verifyTransfersAndHistoricalSnapshots() {
        let suite = "CupaTransferVerifier.\(UUID().uuidString)"; let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext
        let method = EquipmentRecord(context: context, name: "V60 02", equipmentType: "BREWER_METHOD", brand: "Hario", model: "02", capacityMl: 600, configuration: "Plástico", notes: "", isFavorite: true, isActive: true)
        let bean = CoffeeBeanRecord(context: context, name: "Etiopía Guji", brand: "Casa", remainingQuantityGrams: 250)
        let grinder = GrinderRecord(context: context, name: "C40", brand: "Comandante", model: "MK4", grinderType: "MANUAL", scaleUnit: "CLICKS", minimumSetting: 0, maximumSetting: 40, calibrationNotes: "", notes: "")
        let recipe = RecipeRecord(context: context, name: "V60 floral", recipeKind: "BLACK_COFFEE", intention: "Claridad", suggestedMethodId: method.id, suggestedMethodName: method.name, isFavorite: true, tags: "floral")
        let coffee = RecipeIngredientRecord(context: context, recipeId: recipe.id, name: "Café", amount: 18, unit: "GRAMS", orderIndex: 0)
        let water = RecipeIngredientRecord(context: context, recipeId: recipe.id, name: "Agua", amount: 270, unit: "MILLILITERS", orderIndex: 1)
        let technique = TechniqueRecord(context: context, name: "Tres vertidos", methodId: method.id, methodName: method.name, recipeId: recipe.id, beanId: bean.id, grinderId: grinder.id, doseGrams: 18, waterMl: 270, ratio: 15, temperatureC: 94, executionMode: "GUIDED", grindValue: 22, grindDescription: "22 clicks", grindUnit: "CLICKS", notes: "Bloom largo", techniqueDescription: "Tres pulsos", totalTimeSeconds: 210)
        try! context.save()

        let calculator = CalculatorModel(defaults: defaults); calculator.selectMethod("AeroPress"); calculator.changeCoffee("17"); calculator.changeRatio("13")
        let lab = LabModel(defaults: defaults); lab.load(calculator: calculator)
        precondition(lab.state.method == "AeroPress" && lab.state.coffeeGrams == 17 && lab.state.waterMl == 221 && lab.state.ratio == 13)
        lab.load(recipe: recipe, ingredients: [coffee, water])
        precondition(lab.state.recipeId == recipe.id && lab.state.methodId == method.id && lab.state.coffeeGrams == 18 && lab.state.waterMl == 270 && lab.state.ratio == 15)
        lab.load(technique: technique, recipeName: recipe.name)
        precondition(lab.state.techniqueId == technique.id && lab.state.beanId == bean.id && lab.state.grinderId == grinder.id)
        precondition(lab.state.temperatureC == 94 && lab.state.grindClicks == 22 && lab.state.timeSeconds == 210)
        let reopenedLab = LabModel(defaults: defaults)
        precondition(reopenedLab.state.techniqueId == technique.id && reopenedLab.state.recipeName == recipe.name)

        let preparation = PreparationModel(defaults: defaults); preparation.load(lab: reopenedLab.state)
        precondition(preparation.state.techniqueId == technique.id && preparation.state.methodId == method.id)
        precondition(preparation.state.doseGrams == 18 && preparation.state.waterMl == 270 && preparation.state.temperatureC == 94)
        let experiment = LabExperimentRecord(context: context, state: reopenedLab.state, profile: reopenedLab.profile)
        let brew = BrewSessionRecord(context: context, state: preparation.state, recipeName: recipe.name, beanName: bean.name, grinderName: grinder.name)
        try! context.save()
        var tastingState = TastingState(brewSessionId: brew.id)
        tastingState.rating = 4.5; tastingState.freeNotes = "Jazmín al enfriar"
        let tasting = try! TastingRepository(context: context).save(tastingState, brew: brew)
        let linkedBrews = NSFetchRequest<BrewSessionRecord>(entityName: "BrewSessionRecord")
        linkedBrews.predicate = NSPredicate(format: "beanId == %@ AND deletedAt == nil", bean.id as CVarArg)
        let linkedCups = NSFetchRequest<CupSessionRecord>(entityName: "CupSessionRecord")
        linkedCups.predicate = NSPredicate(format: "beanId == %@ AND deletedAt == nil", bean.id as CVarArg)
        precondition(try! context.count(for: linkedBrews) == 1 && (try! context.count(for: linkedCups)) == 1)
        let linkedCup = try! context.fetch(linkedCups).first!
        precondition(linkedCup.rating == 4.5 && linkedCup.comment == "Jazmín al enfriar")
        precondition(experiment.methodId == method.id && experiment.recipeId == recipe.id && experiment.techniqueId == technique.id)
        precondition(brew.methodId == method.id && brew.recipeNameSnapshot == "V60 floral" && brew.beanNameSnapshot == "Etiopía Guji")
        let loadedExperiment = LabModel(defaults: defaults); loadedExperiment.reset(); loadedExperiment.load(experiment: experiment)
        precondition(loadedExperiment.state.methodId == method.id && loadedExperiment.state.techniqueId == technique.id)
        precondition(loadedExperiment.state.coffeeGrams == 18 && loadedExperiment.state.waterMl == 270 && loadedExperiment.state.timeSeconds == 210)

        recipe.markDeleted(); technique.markDeleted(); bean.markDeleted(); grinder.markDeleted(); method.markDeleted(); try! context.save()
        precondition(brew.recipeId == recipe.id && brew.methodId == method.id && brew.beanId == bean.id && brew.grinderId == grinder.id)
        precondition(brew.techniqueNameSnapshot == "Tres vertidos" && brew.methodNameSnapshot == "V60 02" && brew.grinderNameSnapshot == "C40")
        precondition(try! context.count(for: linkedBrews) == 1 && (try! context.count(for: linkedCups)) == 1)
        precondition(linkedCup.beanNameSnapshot == "Etiopía Guji")
        precondition(tasting.beanId == bean.id && tasting.evaluatorNotes == "Jazmín al enfriar")
        experiment.markDeleted(); try! context.save(); precondition(experiment.syncStatus == .pendingDelete && experiment.deletedAt != nil)
    }

    private static func verifyLocalPersistence() {
        let persistence = PersistenceController(inMemory: true)
        let context = persistence.container.viewContext
        let bean = CoffeeBeanRecord(context: context, name: "Prueba", brand: "Tostador", remainingQuantityGrams: 250)
        let state = LabState(altitudeMeters: 1500, cityName: "Guatemala (1,500m)")
        _ = LabExperimentRecord(context: context, state: state, profile: LabEngine.calculate(state))
        let grinder = GrinderRecord(context: context, name: "C40", brand: "Comandante", model: "C40 MK4", grinderType: "MANUAL", scaleUnit: "CLICKS", minimumSetting: 0, maximumSetting: 40, calibrationNotes: "Cero real", notes: "")
        let equipment = EquipmentRecord(context: context, name: "V60 02", equipmentType: "BREWER_METHOD", brand: "Hario", model: "02", capacityMl: 600, configuration: "Plástico", notes: "", isFavorite: true, isActive: true)
        try! context.save()
        let beans = try! context.fetch(NSFetchRequest<CoffeeBeanRecord>(entityName: "CoffeeBeanRecord"))
        let experiments = try! context.fetch(NSFetchRequest<LabExperimentRecord>(entityName: "LabExperimentRecord"))
        let grinders = try! context.fetch(NSFetchRequest<GrinderRecord>(entityName: "GrinderRecord"))
        let equipmentItems = try! context.fetch(NSFetchRequest<EquipmentRecord>(entityName: "EquipmentRecord"))
        precondition(beans.map(\.name) == ["Prueba"])
        precondition(experiments.first?.altitudeMeters == 1500)
        precondition(grinders.first?.maximumSetting == 40)
        precondition(equipmentItems.first?.capacityMl == 600)
        precondition(equipment.isBrewingMethod && equipment.isFavorite)
        grinder.maximumSetting = 42; grinder.calibrationNotes = "Cero ajustado"; grinder.markUpdated()
        equipment.capacityMl = 700; equipment.configuration = "Cerámica"; equipment.isFavorite = false; equipment.markUpdated()
        try! context.save()
        precondition(grinder.maximumSetting == 42 && grinder.syncStatus == .pendingUpdate)
        precondition(equipment.capacityMl == 700 && equipment.syncStatus == .pendingUpdate)
        let preparation = PreparationState(techniqueName: "Prueba histórica", methodId: equipment.id, methodName: equipment.name, grinderId: grinder.id, grindDescription: "22 clicks")
        let brew = BrewSessionRecord(context: context, state: preparation, beanName: "", grinderName: grinder.name)
        try! context.save()
        bean.markDeleted()
        grinder.markDeleted()
        equipment.markDeleted()
        try! context.save()
        let active = NSFetchRequest<CoffeeBeanRecord>(entityName: "CoffeeBeanRecord")
        active.predicate = NSPredicate(format: "deletedAt == nil")
        precondition(try! context.fetch(active).isEmpty)
        let activeGrinders = NSFetchRequest<GrinderRecord>(entityName: "GrinderRecord"); activeGrinders.predicate = NSPredicate(format: "deletedAt == nil")
        let activeEquipment = NSFetchRequest<EquipmentRecord>(entityName: "EquipmentRecord"); activeEquipment.predicate = NSPredicate(format: "deletedAt == nil")
        precondition(try! context.fetch(activeGrinders).isEmpty && (try! context.fetch(activeEquipment)).isEmpty)
        precondition(grinder.syncStatus == .pendingDelete && equipment.syncStatus == .pendingDelete)
        precondition(brew.grinderId == grinder.id && brew.methodId == equipment.id)
        precondition(brew.grinderNameSnapshot == "C40" && brew.methodNameSnapshot == "V60 02")
    }

    @MainActor private static func verifyRecipeTechniqueAggregates() {
        let persistence = PersistenceController(inMemory: true)
        let context = persistence.container.viewContext
        let repository = RecipeTechniqueRepository(context: context)
        var recipeDraft = RecipeDraftModel(
            name: "V60 frutal", recipeKind: "BLACK_COFFEE", intention: "Acidez brillante",
            ingredients: [.init(name: "Café", amount: 15, unit: "GRAMS"), .init(name: "Agua", amount: 240, unit: "MILLILITERS")],
            steps: [.init(instruction: "Bloom", durationSeconds: 45), .init(instruction: "Vertido", durationSeconds: 120)]
        )
        let recipe = try! repository.saveRecipe(recipeDraft)
        precondition(try! repository.ingredients(recipeId: recipe.id).count == 2)
        try! repository.toggleFavorite(recipe)
        precondition(recipe.isFavorite)
        recipeDraft.ingredients.removeFirst(); recipeDraft.steps.swapAt(0, 1)
        _ = try! repository.saveRecipe(recipeDraft)
        precondition(try! repository.ingredients(recipeId: recipe.id).map(\.name) == ["Agua"])
        precondition(try! repository.recipeSteps(recipeId: recipe.id).map(\.instruction) == ["Vertido", "Bloom"])
        let copy = try! repository.duplicateRecipe(recipe)
        precondition(copy.originalEntityId == recipe.id)
        precondition(try! repository.ingredients(recipeId: copy.id).count == 1)

        var techniqueDraft = TechniqueDraftModel(
            name: "V60 guiada", methodName: "V60", doseGrams: 15, waterMl: 240, ratio: 16, temperatureC: 93,
            steps: [.init(title: "Bloom", durationSeconds: 45, waterAddedMl: 50, note: "Saturar", coverage: 100, flow: 3.2, secondaryAction: "Agitar"), .init(title: "Vertido 1", durationSeconds: 40, waterAddedMl: 100), .init(title: "Vertido 2", durationSeconds: 35, waterAddedMl: 90)]
        )
        let technique = try! repository.saveTechnique(techniqueDraft)
        var techniqueSteps = try! repository.techniqueSteps(techniqueId: technique.id)
        precondition(techniqueSteps.map(\.waterAccumulatedMl) == [50, 150, 240])
        precondition(technique.totalTimeSeconds == 120 && techniqueSteps.first?.secondaryAction == "Agitar")
        techniqueDraft.steps.swapAt(0, 2); techniqueDraft.steps.remove(at: 1); techniqueDraft.steps[0].flow = 2.5
        _ = try! repository.saveTechnique(techniqueDraft)
        techniqueSteps = try! repository.techniqueSteps(techniqueId: technique.id)
        precondition(techniqueSteps.map(\.title) == ["Vertido 2", "Bloom"])
        precondition(techniqueSteps.map(\.waterAccumulatedMl) == [90, 140] && technique.totalTimeSeconds == 80)
        let suite = "CupaTechniqueFlowVerifier.\(UUID().uuidString)"; let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let preparation = PreparationModel(defaults: defaults); preparation.load(technique: technique, steps: techniqueSteps)
        precondition(preparation.state.techniqueId == technique.id && preparation.state.steps.map(\.title) == ["Vertido 2", "Bloom"])
        try! repository.deleteTechnique(technique)
        precondition(try! repository.techniqueSteps(techniqueId: technique.id).isEmpty)
    }

    private static func verifyRecipeTextImport() {
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
        precondition(draft.name == "Espresso Tonic Menta" && draft.recipeKind == "COLD_DRINK")
        precondition(draft.suggestedMethodName == "Espresso")
        precondition(draft.ingredients.map(\.amount) == [30, 150, 2])
        precondition(draft.ingredients.map(\.unit) == ["MILLILITERS", "MILLILITERS", "UNITS"])
        precondition(draft.steps.map(\.instruction) == ["Servir tónica y hielo", "Verter espresso"])
        precondition(draft.intention == "Refrescante y herbal")
        let fallback = RecipeTextParser.parse("Mi bebida secreta")
        precondition(fallback.recipeKind == "OTHER" && fallback.ingredients.count == 2 && fallback.steps.count == 1)
    }

    @MainActor private static func verifyPreparationRecovery() {
        let suite = "CupaPreparationVerifier.\(UUID().uuidString)"; let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext
        let repository = RecipeTechniqueRepository(context: context)
        let technique = try! repository.saveTechnique(TechniqueDraftModel(
            name: "Guiada", methodName: "V60", executionMode: "GUIDED",
            steps: [.init(title: "Bloom", durationSeconds: 45, waterAddedMl: 50), .init(title: "Vertido", durationSeconds: 75, waterAddedMl: 190)]
        ))
        let model = PreparationModel(defaults: defaults)
        model.load(technique: technique, steps: try! repository.techniqueSteps(techniqueId: technique.id))
        model.start(); let tick = model.state.lastTickAt!
        model.synchronizeClock(now: tick.addingTimeInterval(46)); model.pause()
        precondition(model.state.activeStepIndex == 1)
        let restored = PreparationModel(defaults: defaults)
        precondition(restored.state.status == .paused && restored.state.elapsedSeconds == 46)
        _ = BrewSessionRecord(context: context, state: restored.state, beanName: "Café", grinderName: "Molino")
        try! context.save()
        let sessions = try! context.fetch(NSFetchRequest<BrewSessionRecord>(entityName: "BrewSessionRecord"))
        precondition(sessions.first?.stepsSnapshotJSON.contains("Bloom") == true)
    }

    @MainActor private static func verifyTastingCoolingAndPersistence() {
        let suite = "CupaTastingVerifier.\(UUID().uuidString)"; let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext
        let recipeId = UUID(); let techniqueId = UUID(); let methodId = UUID(); let beanId = UUID(); let grinderId = UUID()
        let brewState = PreparationState(
            techniqueId: techniqueId, techniqueName: "V60 Dulce", methodId: methodId, methodName: "V60",
            recipeId: recipeId, beanId: beanId, grinderId: grinderId, doseGrams: 18, waterMl: 288, ratio: 16,
            temperatureC: 93, grindDescription: "22 clicks", elapsedSeconds: 180, status: .completed
        )
        let brew = BrewSessionRecord(context: context, state: brewState, recipeName: "Mora limpia", beanName: "Etiopía", grinderName: "C40")
        try! context.save()
        let model = TastingModel(defaults: defaults); model.state.brewSessionId = brew.id
        for (seconds, expected) in [(0, "HOT"), (239, "HOT"), (240, "PEAK"), (599, "PEAK"), (600, "DECLINING"), (959, "DECLINING"), (960, "EXHAUSTED")] {
            model.state.coolingElapsedSeconds = seconds; precondition(model.stageCode == expected)
        }
        model.addObservation(); precondition(model.state.observations.isEmpty)
        model.reset(); let originalId = model.state.id
        model.start(); let boundaryTick = model.state.lastTickAt!; model.synchronizeClock(now: boundaryTick.addingTimeInterval(245)); model.addObservation(); model.pause()
        precondition(model.state.observations.map(\.stage) == ["PEAK"] && model.state.coolingStatus == .paused)
        model.removeObservation(id: model.state.observations[0].id); precondition(model.state.observations.isEmpty)
        model.reset(); precondition(model.state.id == originalId && model.state.coolingElapsedSeconds == 0)
        model.state.selectedFlavorNotes = ["Mora", "Jazmín"]; model.state.freeNotes = "Muy dulce"; model.start(); let tick = model.state.lastTickAt!
        model.synchronizeClock(now: tick.addingTimeInterval(601)); model.state.freeNotes = "Cacao"; model.addObservation(); model.pause()
        precondition(model.stageCode == "DECLINING")
        let restored = TastingModel(defaults: defaults); precondition(restored.state.coolingElapsedSeconds == 601)
        let repository = TastingRepository(context: context); let tasting = try! repository.save(restored.state, brew: brew)
        precondition(tasting.id != brew.id && tasting.selectedFlavorNotes == ["Mora", "Jazmín"])
        precondition(try! repository.observations(tastingId: tasting.id).count == 1)
        let cups = try! context.fetch(NSFetchRequest<CupSessionRecord>(entityName: "CupSessionRecord"))
        let cup = cups.first!
        precondition(cup.brewSessionId == brew.id && cup.tastingId == tasting.id)
        precondition(cup.recipeId == recipeId && cup.techniqueId == techniqueId && cup.methodId == methodId && cup.beanId == beanId && cup.grinderId == grinderId)
        precondition(cup.executedDoseGrams == 18 && cup.executedWaterMl == 288 && cup.executedRatio == 16 && cup.executedTemperatureC == 93)
        precondition(cup.executedGrindSetting == "22 clicks" && cup.executedDurationSeconds == 180)
        precondition(cup.beanNameSnapshot == "Etiopía" && cup.recipeNameSnapshot == "Mora limpia" && cup.techniqueNameSnapshot == "V60 Dulce")
        precondition(cup.methodNameSnapshot == "V60" && cup.grinderNameSnapshot == "C40" && cup.cupLifeState == "DECLINING")
        precondition(cup.comment == "Cacao" && cup.rating == restored.state.rating && cup.nps == Int64(restored.state.nps))
        let labSuite = "CupaTastingLabVerifier.\(UUID().uuidString)"; let labDefaults = UserDefaults(suiteName: labSuite)!
        defer { labDefaults.removePersistentDomain(forName: labSuite) }
        let lab = LabModel(defaults: labDefaults); lab.load(tasting: restored.state, brew: brew)
        precondition(lab.state.method == "V60" && lab.state.coffeeGrams == 18 && lab.state.waterMl == 288)
        precondition(lab.state.notes == "Cargado de cata sensorial. Textura: sedosa, Limpieza: alta.")
        let editor = TastingModel(defaults: defaults)
        editor.load(record: tasting, observations: try! repository.observations(tastingId: tasting.id))
        editor.state.rating = 5; editor.state.freeNotes = "Editada desde el historial"
        editor.state.observations = [.init(elapsedSeconds: 960, stage: "EXHAUSTED", notes: "Fría", aroma: 3, acidity: 2, sweetness: 3, body: 2, bitterness: 3, finish: 2)]
        let updated = try! repository.save(editor.state, brew: brew)
        precondition(updated.id == tasting.id && updated.rating == 5)
        precondition((try! repository.observations(tastingId: tasting.id)).map(\.stage) == ["EXHAUSTED"])
        precondition((try! context.fetch(NSFetchRequest<CupSessionRecord>(entityName: "CupSessionRecord"))).count == 1)
        editor.markSaved(); let savedId = editor.state.id; precondition(editor.state.coolingStatus == .completed)
        let completed = editor.state; editor.addObservation(); editor.removeObservation(id: completed.observations[0].id); editor.reset(); precondition(editor.state == completed)
        editor.newTasting(); precondition(editor.state.id != savedId && editor.state.coolingStatus == .ready)
        try! repository.delete(tasting)
        precondition(tasting.syncStatusRaw == SyncStatus.pendingDelete.rawValue && cup.syncStatusRaw == SyncStatus.pendingDelete.rawValue)
    }

    @MainActor private static func verifySyncConflictAndOutbox() {
        let owner = UUID(); let older = Date(timeIntervalSince1970: 10); let newer = Date(timeIntervalSince1970: 20)
        let choice = LastWriteWinsResolver.resolve(
            local: .init(ownerId: owner, updatedAt: newer, version: 1, deletedAt: nil),
            remote: .init(ownerId: owner, updatedAt: older, version: 9, deletedAt: nil)
        )
        precondition(choice == .local)
        let persistence = PersistenceController(inMemory: true); let repository = SyncOutboxRepository(context: persistence.container.viewContext)
        let entityId = UUID(); let first = try! repository.enqueue(entityName: "recipes", entityId: entityId, ownerId: owner, operation: .pendingCreate, payloadJSON: "{}")
        let same = try! repository.enqueue(entityName: "recipes", entityId: entityId, ownerId: owner, operation: .pendingUpdate, payloadJSON: "{\"edited\":true}")
        precondition(first.id == same.id && (try! repository.ready().count) == 1)
        let now = Date(); try! repository.markFailed(same, message: "offline", now: now)
        precondition((try! repository.ready(now: now)).isEmpty && same.nextAttemptAt > now)
        try! repository.markSucceeded(same); precondition((try! repository.ready(now: .distantFuture)).isEmpty)
    }

    private static func verifyLocalSuggestionFallback() {
        let state = LabState(waterMl: 270, ratio: 18, temperatureC: 84, grindClicks: 32, freshness: "viejo", timeSeconds: 80)
        let suggestion = LocalSuggestionEngine.suggest(.init(state: state, profile: LabEngine.calculate(state)))
        precondition(suggestion.source == .local && suggestion.text.contains("extracción estimada es baja"))
    }

    @MainActor private static func verifyProfilePersistence() {
        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext; let repository = ProfileRepository(context: context)
        let ownerA = UUID(); let ownerB = UUID()
        let first = try! repository.save(ownerId: ownerA, displayName: "Emiliano", alias: "brewther", biography: "V60", avatarColor: "#3F7A63", favoriteMethods: "V60", isPrivate: true)
        let updated = try! repository.save(ownerId: ownerA, displayName: "Emiliano N.", alias: "@brewther", biography: "Café", avatarColor: "#234E3C", favoriteMethods: "V60", isPrivate: false)
        _ = try! repository.save(ownerId: ownerB, displayName: "Otra", alias: "otra", biography: "", avatarColor: "#000000", favoriteMethods: "", isPrivate: true)
        precondition(first.id == updated.id && updated.alias == "brewther")
        precondition(try! context.fetch(NSFetchRequest<UserProfileRecord>(entityName: "UserProfileRecord")).count == 2)
    }

    @MainActor private static func verifySocialImportAttribution() {
        let originalId = UUID(); let share = SocialShare(
            id: UUID(), ownerId: UUID(), entityType: "recipe", entityId: originalId, fromName: "Barista", fromHandle: "brew",
            targetUserId: nil, visibility: "PUBLIC", name: "V60 comunitaria", subtitle: "Dulzor", message: "",
            payloadSnapshot: .init(kind: "recipe", recipe: .init(name: "V60 comunitaria", recipeKind: "BLACK_COFFEE", intention: "Dulzor", suggestedMethodName: "V60", tags: "", ingredients: [.init(name: "Café", amount: 15, unit: "GRAMS")], steps: [.init(instruction: "Bloom", durationSeconds: 45)]), technique: nil),
            originalEntityId: originalId, status: "ACTIVE", createdAt: "2026-08-17T00:00:00Z", updatedAt: "2026-08-17T00:00:00Z"
        )
        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext
        try! SocialService(configuration: .init(supabaseURL: nil, supabaseAnonKey: nil)).importShare(share, context: context)
        let imported = try! context.fetch(NSFetchRequest<RecipeRecord>(entityName: "RecipeRecord"))
        precondition(imported.first?.originalEntityId == originalId && imported.first?.copyMode == "IMPORT")
    }

    private static func verifySocialContentPolicy() {
        let payload = SharePayloadSnapshot(
            kind: "recipe",
            recipe: .init(name: "V60 dulce", recipeKind: "BLACK_COFFEE", intention: "Balance", suggestedMethodName: "V60", tags: "", ingredients: [], steps: []),
            technique: nil
        )
        try! SocialContentPolicy.validate(fromName: "Ana", fromHandle: "ana", name: "V60 dulce", subtitle: "Balance", message: "Notas de cacao", payload: payload)
        do {
            try SocialContentPolicy.validate(fromName: "Ana", fromHandle: "ana", name: "V60", subtitle: "", message: "contenido de violación", payload: payload)
            preconditionFailure("El filtro social aceptó contenido prohibido")
        } catch SocialValidationError.objectionableContent {
            // Resultado esperado.
        } catch {
            preconditionFailure("Error inesperado del filtro social: \(error)")
        }
    }

    @MainActor private static func verifyEntitySyncMapping() {
        let persistence = PersistenceController(inMemory: true); let context = persistence.container.viewContext; let owner = UUID()
        let bean = CoffeeBeanRecord(context: context, name: "Local", brand: "Tostador", remainingQuantityGrams: 200); try! context.save()
        let defaults = UserDefaults(suiteName: "CupaEntitySyncVerifier.\(UUID().uuidString)")!
        let coordinator = EntitySyncCoordinator(context: context, configuration: .init(supabaseURL: nil, supabaseAnonKey: nil), defaults: defaults)
        try! coordinator.enqueuePending(ownerId: owner); precondition(bean.ownerId == owner)
        let outbox = try! context.fetch(NSFetchRequest<SyncOperationRecord>(entityName: "SyncOperationRecord")); precondition(outbox.count == 1)
        let data = outbox[0].payloadJSON.data(using: .utf8)!; var row = (try! JSONSerialization.jsonObject(with: data) as! [[String: Any]])[0]
        precondition(row["owner_id"] as? String == owner.uuidString)
        row["name"] = "Remoto"; row["updated_at"] = "2099-08-17T00:00:00Z"; row["version"] = 8
        let descriptor = CoreSyncSchema.descriptors.first { $0.entityName == "CoffeeBeanRecord" }!
        try! coordinator.merge(row, descriptor: descriptor, expectedOwner: owner)
        precondition(bean.name == "Remoto" && bean.syncStatus == .synced && bean.version == 8)
    }

    @MainActor private static func verifySessionRecovery() async {
        let configuration = AppConfiguration(supabaseURL: URL(string: "https://project.supabase.co")!, supabaseAnonKey: "public-anon")
        let now = Date(timeIntervalSince1970: 2_000_000_000)
        let expired = AuthTokens(accessToken: "old", refreshToken: "refresh", expiresAt: now.addingTimeInterval(-1), userId: UUID(), email: "brew@example.com")

        let offlineStore = VerifierTokenStore(expired)
        let offline = AccountModel(configuration: configuration, transport: VerifierTransport(error: URLError(.notConnectedToInternet)), store: offlineStore)
        let offlineTokens = await offline.validTokens(now: now)
        precondition(offlineTokens == nil)
        precondition(offline.tokens == expired && offlineStore.value == expired)
        precondition(offline.sessionNotice?.contains("Sin conexión") == true)

        let rejectedStore = VerifierTokenStore(expired)
        let rejected = AccountModel(configuration: configuration, transport: VerifierTransport(statusCode: 401), store: rejectedStore)
        let rejectedTokens = await rejected.validTokens(now: now)
        precondition(rejectedTokens == nil)
        precondition(rejected.state == .signedOut && rejectedStore.value == nil)
    }
}

private final class VerifierTokenStore: TokenStore {
    var value: AuthTokens?
    init(_ value: AuthTokens?) { self.value = value }
    func load() throws -> AuthTokens? { value }
    func save(_ tokens: AuthTokens) throws { value = tokens }
    func clear() throws { value = nil }
}

private final class VerifierTransport: NetworkTransport {
    let statusCode: Int; let error: Error?
    init(statusCode: Int = 200, error: Error? = nil) { self.statusCode = statusCode; self.error = error }
    func data(for request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        if let error { throw error }
        return (Data("{\"message\":\"Invalid refresh token\"}".utf8), HTTPURLResponse(url: request.url!, statusCode: statusCode, httpVersion: nil, headerFields: nil)!)
    }
}
