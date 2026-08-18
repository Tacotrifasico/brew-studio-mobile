import CoreData
import Foundation

enum SyncStatus: String, Codable, CaseIterable {
    case synced, pendingCreate, pendingUpdate, pendingDelete, conflict, error
}

enum CoffeeFreshnessState: String, CaseIterable {
    case noDate, veryFresh, inWindow, ideal, declining, old

    var label: String {
        switch self {
        case .noDate: "Sin fecha"
        case .veryFresh: "Muy fresco"
        case .inWindow: "En ventana"
        case .ideal: "Puntal ideal"
        case .declining: "Bajando"
        case .old: "Viejo"
        }
    }

    var colorHex: UInt {
        switch self {
        case .noDate: 0x60756A
        case .veryFresh: 0x84AD92
        case .inWindow: 0x3F7A63
        case .ideal: 0xC28B46
        case .declining: 0xB76545
        case .old: 0x8C5A2B
        }
    }
}

struct CoffeeFreshnessResult: Equatable {
    let daysFromRoast: Int?
    let daysFromOpen: Int?
    let state: CoffeeFreshnessState
    let progress: Double
    let recommendation: String
    let openStatusDetails: String
    let openWarning: String?
}

enum CoffeeFreshnessEngine {
    static func evaluate(
        roastDate: Date?,
        openedDate: Date?,
        now: Date = .now,
        calendar: Calendar = .current
    ) -> CoffeeFreshnessResult {
        let daysFromRoast = roastDate.map { dayDifference(from: $0, to: now, calendar: calendar) }
        let daysFromOpen = openedDate.map { dayDifference(from: $0, to: now, calendar: calendar) }
        guard let daysFromRoast else {
            return .init(
                daysFromRoast: nil, daysFromOpen: daysFromOpen, state: .noDate, progress: 0,
                recommendation: "Agrega fecha de tostado para calcular frescura.",
                openStatusDetails: openDetails(daysFromOpen), openWarning: nil
            )
        }

        let state: CoffeeFreshnessState = switch daysFromRoast {
        case ..<0: .noDate
        case ...7: .veryFresh
        case ...21: .inWindow
        case ...35: .ideal
        case ...60: .declining
        default: .old
        }
        let recommendation = switch state {
        case .noDate: "Agrega fecha de tostado para calcular frescura."
        case .veryFresh: "Puede tener mucho gas; se sugiere preinfusión larga de 45–50 segundos (bloom)."
        case .inWindow: "Excelente ventana de uso. El perfil de sabor es más estable y dulce."
        case .ideal: "Punto ideal para filtrados. Buena retención de aromas y extracción equilibrada."
        case .declining: "Va perdiendo expresión. Ajusta molienda un poco más fina o sube temperatura 1°C."
        case .old: "Perfil más plano. Úsalo pronto o para recetas con leche/frías donde resalte intensidad."
        }
        return .init(
            daysFromRoast: daysFromRoast, daysFromOpen: daysFromOpen, state: state,
            progress: progress(days: daysFromRoast), recommendation: recommendation,
            openStatusDetails: openDetails(daysFromOpen),
            openWarning: daysFromOpen.map { $0 > 14 ? "Abierto hace \($0) días. Puede perder aroma más rápido." : nil } ?? nil
        )
    }

    static func progress(days: Int) -> Double {
        if days < 0 { return 0 }
        switch days {
        case ...7: return (Double(days) / 7) * 0.2
        case ...21: return 0.2 + (Double(days - 7) / 14) * 0.2
        case ...35: return 0.4 + (Double(days - 21) / 14) * 0.2
        case ...60: return 0.6 + (Double(days - 35) / 25) * 0.2
        default: return min(1, 0.8 + (Double(days - 60) / 20) * 0.2)
        }
    }

    private static func dayDifference(from date: Date, to now: Date, calendar: Calendar) -> Int {
        let seconds = calendar.startOfDay(for: now).timeIntervalSince(calendar.startOfDay(for: date))
        return Int((seconds / 86_400).rounded(.towardZero))
    }

    private static func openDetails(_ days: Int?) -> String {
        days.map { "Abierto hace \($0) días" } ?? "Sin abrir (Hermético)"
    }
}

@objc(CoffeeBeanRecord)
final class CoffeeBeanRecord: NSManagedObject {
    @NSManaged var id: UUID
    @NSManaged var ownerId: UUID?
    @NSManaged var name: String
    @NSManaged var brand: String
    @NSManaged var origin: String
    @NSManaged var producer: String
    @NSManaged var variety: String
    @NSManaged var process: String
    @NSManaged var altitudeMetersValue: NSNumber?
    @NSManaged var roastLevel: String
    @NSManaged var roastDate: Date?
    @NSManaged var openedDate: Date?
    @NSManaged var initialQuantityGrams: Double
    @NSManaged var remainingQuantityGrams: Double
    @NSManaged var notes: String
    @NSManaged var createdAt: Date
    @NSManaged var updatedAt: Date
    @NSManaged var version: Int64
    @NSManaged var syncStatusRaw: String
    @NSManaged var deletedAt: Date?

    var altitudeMeters: Int? {
        get { altitudeMetersValue?.intValue }
        set { altitudeMetersValue = newValue.map(NSNumber.init(value:)) }
    }

    var syncStatus: SyncStatus {
        get { SyncStatus(rawValue: syncStatusRaw) ?? .error }
        set { syncStatusRaw = newValue.rawValue }
    }

    convenience init(
        context: NSManagedObjectContext,
        id: UUID = UUID(), ownerId: UUID? = nil,
        name: String, brand: String, origin: String = "", producer: String = "",
        variety: String = "", process: String = "", altitudeMeters: Int? = nil,
        roastLevel: String = "Medio", roastDate: Date? = nil, openedDate: Date? = nil,
        initialQuantityGrams: Double = 0, remainingQuantityGrams: Double = 0,
        notes: String = "", createdAt: Date = .now, updatedAt: Date = .now,
        version: Int64 = 1, syncStatus: SyncStatus = .pendingCreate, deletedAt: Date? = nil
    ) {
        self.init(context: context)
        self.id = id; self.ownerId = ownerId; self.name = name; self.brand = brand
        self.origin = origin; self.producer = producer; self.variety = variety; self.process = process
        self.altitudeMeters = altitudeMeters; self.roastLevel = roastLevel
        self.roastDate = roastDate; self.openedDate = openedDate
        self.initialQuantityGrams = initialQuantityGrams; self.remainingQuantityGrams = remainingQuantityGrams
        self.notes = notes; self.createdAt = createdAt; self.updatedAt = updatedAt
        self.version = version; self.syncStatus = syncStatus; self.deletedAt = deletedAt
    }

    func markUpdated() {
        updatedAt = .now; version += 1; syncStatus = .pendingUpdate
    }

    func markDeleted() {
        deletedAt = .now; updatedAt = .now; version += 1; syncStatus = .pendingDelete
    }
}

extension CoffeeBeanRecord: Identifiable {}

@objc(LabExperimentRecord)
final class LabExperimentRecord: NSManagedObject {
    @NSManaged var id: UUID
    @NSManaged var ownerId: UUID?
    @NSManaged var methodId: UUID?
    @NSManaged var recipeId: UUID?
    @NSManaged var techniqueId: UUID?
    @NSManaged var beanId: UUID?
    @NSManaged var grinderId: UUID?
    @NSManaged var method: String
    @NSManaged var coffeeGrams: Double
    @NSManaged var waterMl: Int64
    @NSManaged var ratio: Double
    @NSManaged var temperatureC: Int64
    @NSManaged var grindClicks: Int64
    @NSManaged var freshness: String
    @NSManaged var timeSeconds: Int64
    @NSManaged var altitudeMeters: Int64
    @NSManaged var cityName: String
    @NSManaged var notes: String
    @NSManaged var extractionIndex: Double
    @NSManaged var summary: String
    @NSManaged var createdAt: Date
    @NSManaged var updatedAt: Date
    @NSManaged var version: Int64
    @NSManaged var syncStatusRaw: String
    @NSManaged var deletedAt: Date?

    convenience init(context: NSManagedObjectContext, state: LabState, profile: LabFlavorProfile) {
        self.init(context: context)
        id = UUID(); ownerId = nil; methodId = state.methodId; recipeId = state.recipeId
        techniqueId = state.techniqueId; beanId = state.beanId; grinderId = state.grinderId; method = state.method
        coffeeGrams = Double(state.coffeeGrams); waterMl = Int64(state.waterMl); ratio = Double(state.ratio)
        temperatureC = Int64(state.temperatureC); grindClicks = Int64(state.grindClicks)
        freshness = state.freshness; timeSeconds = Int64(state.timeSeconds)
        altitudeMeters = Int64(state.altitudeMeters); cityName = state.cityName; notes = state.notes
        extractionIndex = Double(profile.extractionIndex); summary = profile.summary
        createdAt = .now; updatedAt = .now; version = 1
        syncStatusRaw = SyncStatus.pendingCreate.rawValue; deletedAt = nil
    }

    var syncStatus: SyncStatus {
        get { SyncStatus(rawValue: syncStatusRaw) ?? .error }
        set { syncStatusRaw = newValue.rawValue }
    }

    func markDeleted() {
        deletedAt = .now; updatedAt = .now; version += 1; syncStatus = .pendingDelete
    }
}

extension LabExperimentRecord: Identifiable {}

@objc(GrinderRecord)
final class GrinderRecord: NSManagedObject {
    @NSManaged var id: UUID
    @NSManaged var ownerId: UUID?
    @NSManaged var name: String
    @NSManaged var brand: String
    @NSManaged var model: String
    @NSManaged var grinderType: String
    @NSManaged var scaleUnit: String
    @NSManaged var minimumSetting: Int64
    @NSManaged var maximumSetting: Int64
    @NSManaged var calibrationNotes: String
    @NSManaged var notes: String
    @NSManaged var createdAt: Date
    @NSManaged var updatedAt: Date
    @NSManaged var version: Int64
    @NSManaged var syncStatusRaw: String
    @NSManaged var deletedAt: Date?

    convenience init(context: NSManagedObjectContext, name: String, brand: String, model: String, grinderType: String, scaleUnit: String, minimumSetting: Int, maximumSetting: Int, calibrationNotes: String, notes: String) {
        self.init(context: context)
        id = UUID(); ownerId = nil; self.name = name; self.brand = brand; self.model = model
        self.grinderType = grinderType; self.scaleUnit = scaleUnit
        self.minimumSetting = Int64(minimumSetting); self.maximumSetting = Int64(maximumSetting)
        self.calibrationNotes = calibrationNotes; self.notes = notes
        createdAt = .now; updatedAt = .now; version = 1; syncStatusRaw = SyncStatus.pendingCreate.rawValue; deletedAt = nil
    }

    var syncStatus: SyncStatus {
        get { SyncStatus(rawValue: syncStatusRaw) ?? .error }
        set { syncStatusRaw = newValue.rawValue }
    }
    func markUpdated() { updatedAt = .now; version += 1; syncStatus = .pendingUpdate }
    func markDeleted() { deletedAt = .now; updatedAt = .now; version += 1; syncStatus = .pendingDelete }
}

extension GrinderRecord: Identifiable {}

@objc(EquipmentRecord)
final class EquipmentRecord: NSManagedObject {
    @NSManaged var id: UUID
    @NSManaged var ownerId: UUID?
    @NSManaged var name: String
    @NSManaged var equipmentType: String
    @NSManaged var brand: String
    @NSManaged var model: String
    @NSManaged var capacityMlValue: NSNumber?
    @NSManaged var configuration: String
    @NSManaged var notes: String
    @NSManaged var isFavorite: Bool
    @NSManaged var isActive: Bool
    @NSManaged var createdAt: Date
    @NSManaged var updatedAt: Date
    @NSManaged var version: Int64
    @NSManaged var syncStatusRaw: String
    @NSManaged var deletedAt: Date?

    var capacityMl: Int? {
        get { capacityMlValue?.intValue }
        set { capacityMlValue = newValue.map(NSNumber.init(value:)) }
    }
    convenience init(context: NSManagedObjectContext, name: String, equipmentType: String, brand: String, model: String, capacityMl: Int?, configuration: String, notes: String, isFavorite: Bool, isActive: Bool) {
        self.init(context: context)
        id = UUID(); ownerId = nil; self.name = name; self.equipmentType = equipmentType
        self.brand = brand; self.model = model; self.capacityMl = capacityMl
        self.configuration = configuration; self.notes = notes; self.isFavorite = isFavorite; self.isActive = isActive
        createdAt = .now; updatedAt = .now; version = 1; syncStatusRaw = SyncStatus.pendingCreate.rawValue; deletedAt = nil
    }

    var syncStatus: SyncStatus {
        get { SyncStatus(rawValue: syncStatusRaw) ?? .error }
        set { syncStatusRaw = newValue.rawValue }
    }
    func markUpdated() { updatedAt = .now; version += 1; syncStatus = .pendingUpdate }
    func markDeleted() { deletedAt = .now; updatedAt = .now; version += 1; syncStatus = .pendingDelete }
}

extension EquipmentRecord: Identifiable {}

struct PersistenceController {
    static let shared = PersistenceController()
    private static let sharedModel = makeModel()
    let container: NSPersistentContainer

    init(inMemory: Bool = false, storeURL: URL? = nil, enablePersistentHistory: Bool = true) {
        container = NSPersistentContainer(name: "Cupa", managedObjectModel: Self.sharedModel)
        let description = NSPersistentStoreDescription()
        if inMemory {
            description.type = NSInMemoryStoreType
        } else {
            description.type = NSSQLiteStoreType
            description.url = storeURL ?? NSPersistentContainer.defaultDirectoryURL().appendingPathComponent("Cupa.sqlite")
        }
        description.shouldMigrateStoreAutomatically = true
        description.shouldInferMappingModelAutomatically = true
        if enablePersistentHistory {
            description.setOption(true as NSNumber, forKey: NSPersistentHistoryTrackingKey)
            description.setOption(true as NSNumber, forKey: NSPersistentStoreRemoteChangeNotificationPostOptionKey)
        }
        container.persistentStoreDescriptions = [description]
        container.loadPersistentStores { _, error in
            if let error { fatalError("No se pudo abrir el almacenamiento local: \(error.localizedDescription)") }
        }
        container.viewContext.automaticallyMergesChangesFromParent = true
        container.viewContext.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy
    }

    private static func makeModel() -> NSManagedObjectModel {
        let model = NSManagedObjectModel()
        let coffeeEntity = NSEntityDescription()
        coffeeEntity.name = "CoffeeBeanRecord"
        coffeeEntity.managedObjectClassName = NSStringFromClass(CoffeeBeanRecord.self)

        func attribute(_ name: String, _ type: NSAttributeType, optional: Bool = false, defaultValue: Any? = nil) -> NSAttributeDescription {
            let item = NSAttributeDescription()
            item.name = name; item.attributeType = type; item.isOptional = optional; item.defaultValue = defaultValue
            return item
        }
        coffeeEntity.properties = [
            attribute("id", .UUIDAttributeType), attribute("ownerId", .UUIDAttributeType, optional: true),
            attribute("name", .stringAttributeType, defaultValue: ""), attribute("brand", .stringAttributeType, defaultValue: ""),
            attribute("origin", .stringAttributeType, defaultValue: ""), attribute("producer", .stringAttributeType, defaultValue: ""),
            attribute("variety", .stringAttributeType, defaultValue: ""), attribute("process", .stringAttributeType, defaultValue: ""),
            attribute("altitudeMetersValue", .integer64AttributeType, optional: true), attribute("roastLevel", .stringAttributeType, defaultValue: "Medio"),
            attribute("roastDate", .dateAttributeType, optional: true), attribute("openedDate", .dateAttributeType, optional: true),
            attribute("initialQuantityGrams", .doubleAttributeType, defaultValue: 0), attribute("remainingQuantityGrams", .doubleAttributeType, defaultValue: 0),
            attribute("notes", .stringAttributeType, defaultValue: ""), attribute("createdAt", .dateAttributeType),
            attribute("updatedAt", .dateAttributeType), attribute("version", .integer64AttributeType, defaultValue: 1),
            attribute("syncStatusRaw", .stringAttributeType, defaultValue: SyncStatus.pendingCreate.rawValue),
            attribute("deletedAt", .dateAttributeType, optional: true)
        ]
        coffeeEntity.uniquenessConstraints = [["id"]]

        let experimentEntity = NSEntityDescription()
        experimentEntity.name = "LabExperimentRecord"
        experimentEntity.managedObjectClassName = NSStringFromClass(LabExperimentRecord.self)
        experimentEntity.properties = [
            attribute("id", .UUIDAttributeType), attribute("ownerId", .UUIDAttributeType, optional: true),
            attribute("methodId", .UUIDAttributeType, optional: true), attribute("recipeId", .UUIDAttributeType, optional: true),
            attribute("techniqueId", .UUIDAttributeType, optional: true), attribute("beanId", .UUIDAttributeType, optional: true),
            attribute("grinderId", .UUIDAttributeType, optional: true),
            attribute("method", .stringAttributeType, defaultValue: "V60"), attribute("coffeeGrams", .doubleAttributeType, defaultValue: 15),
            attribute("waterMl", .integer64AttributeType, defaultValue: 240), attribute("ratio", .doubleAttributeType, defaultValue: 16),
            attribute("temperatureC", .integer64AttributeType, defaultValue: 92), attribute("grindClicks", .integer64AttributeType, defaultValue: 24),
            attribute("freshness", .stringAttributeType, defaultValue: "en ventana"), attribute("timeSeconds", .integer64AttributeType, defaultValue: 180),
            attribute("altitudeMeters", .integer64AttributeType, defaultValue: 0), attribute("cityName", .stringAttributeType, defaultValue: "Nivel del mar (0m)"),
            attribute("notes", .stringAttributeType, defaultValue: ""), attribute("extractionIndex", .doubleAttributeType, defaultValue: 1),
            attribute("summary", .stringAttributeType, defaultValue: ""), attribute("createdAt", .dateAttributeType),
            attribute("updatedAt", .dateAttributeType), attribute("version", .integer64AttributeType, defaultValue: 1),
            attribute("syncStatusRaw", .stringAttributeType, defaultValue: SyncStatus.pendingCreate.rawValue),
            attribute("deletedAt", .dateAttributeType, optional: true)
        ]
        experimentEntity.uniquenessConstraints = [["id"]]

        let grinderEntity = NSEntityDescription()
        grinderEntity.name = "GrinderRecord"
        grinderEntity.managedObjectClassName = NSStringFromClass(GrinderRecord.self)
        grinderEntity.properties = syncProperties(attribute: attribute) + [
            attribute("name", .stringAttributeType, defaultValue: ""), attribute("brand", .stringAttributeType, defaultValue: ""),
            attribute("model", .stringAttributeType, defaultValue: ""), attribute("grinderType", .stringAttributeType, defaultValue: "MANUAL"),
            attribute("scaleUnit", .stringAttributeType, defaultValue: "CLICKS"), attribute("minimumSetting", .integer64AttributeType, defaultValue: 0),
            attribute("maximumSetting", .integer64AttributeType, defaultValue: 40), attribute("calibrationNotes", .stringAttributeType, defaultValue: ""),
            attribute("notes", .stringAttributeType, defaultValue: "")
        ]
        grinderEntity.uniquenessConstraints = [["id"]]

        let equipmentEntity = NSEntityDescription()
        equipmentEntity.name = "EquipmentRecord"
        equipmentEntity.managedObjectClassName = NSStringFromClass(EquipmentRecord.self)
        equipmentEntity.properties = syncProperties(attribute: attribute) + [
            attribute("name", .stringAttributeType, defaultValue: ""), attribute("equipmentType", .stringAttributeType, defaultValue: "BREWER_METHOD"),
            attribute("brand", .stringAttributeType, defaultValue: ""), attribute("model", .stringAttributeType, defaultValue: ""),
            attribute("capacityMlValue", .integer64AttributeType, optional: true), attribute("configuration", .stringAttributeType, defaultValue: ""),
            attribute("notes", .stringAttributeType, defaultValue: ""), attribute("isFavorite", .booleanAttributeType, defaultValue: false),
            attribute("isActive", .booleanAttributeType, defaultValue: true)
        ]
        equipmentEntity.uniquenessConstraints = [["id"]]

        let recipeEntity = NSEntityDescription()
        recipeEntity.name = "RecipeRecord"; recipeEntity.managedObjectClassName = NSStringFromClass(RecipeRecord.self)
        recipeEntity.properties = syncProperties(attribute: attribute) + [
            attribute("name", .stringAttributeType, defaultValue: ""), attribute("recipeKind", .stringAttributeType, defaultValue: "BLACK_COFFEE"),
            attribute("intention", .stringAttributeType, defaultValue: ""), attribute("suggestedMethodId", .UUIDAttributeType, optional: true),
            attribute("suggestedMethodName", .stringAttributeType, defaultValue: ""), attribute("isFavorite", .booleanAttributeType, defaultValue: false),
            attribute("tags", .stringAttributeType, defaultValue: ""), attribute("visibility", .stringAttributeType, defaultValue: "PRIVATE"),
            attribute("originalEntityId", .UUIDAttributeType, optional: true), attribute("rootEntityId", .UUIDAttributeType, optional: true),
            attribute("copyMode", .stringAttributeType, defaultValue: "ORIGINAL")
        ]
        recipeEntity.uniquenessConstraints = [["id"]]

        let ingredientEntity = NSEntityDescription()
        ingredientEntity.name = "RecipeIngredientRecord"; ingredientEntity.managedObjectClassName = NSStringFromClass(RecipeIngredientRecord.self)
        ingredientEntity.properties = syncProperties(attribute: attribute) + [
            attribute("recipeId", .UUIDAttributeType), attribute("name", .stringAttributeType, defaultValue: ""),
            attribute("amount", .doubleAttributeType, defaultValue: 0), attribute("unit", .stringAttributeType, defaultValue: "GRAMS"),
            attribute("orderIndex", .integer64AttributeType, defaultValue: 0)
        ]
        ingredientEntity.uniquenessConstraints = [["id"]]

        let recipeStepEntity = NSEntityDescription()
        recipeStepEntity.name = "RecipeStepRecord"; recipeStepEntity.managedObjectClassName = NSStringFromClass(RecipeStepRecord.self)
        recipeStepEntity.properties = syncProperties(attribute: attribute) + [
            attribute("recipeId", .UUIDAttributeType), attribute("instruction", .stringAttributeType, defaultValue: ""),
            attribute("stepNumber", .integer64AttributeType, defaultValue: 1), attribute("durationSecondsValue", .integer64AttributeType, optional: true)
        ]
        recipeStepEntity.uniquenessConstraints = [["id"]]

        let techniqueEntity = NSEntityDescription()
        techniqueEntity.name = "TechniqueRecord"; techniqueEntity.managedObjectClassName = NSStringFromClass(TechniqueRecord.self)
        techniqueEntity.properties = syncProperties(attribute: attribute) + [
            attribute("name", .stringAttributeType, defaultValue: ""), attribute("methodId", .UUIDAttributeType, optional: true),
            attribute("methodName", .stringAttributeType, defaultValue: "V60"), attribute("recipeId", .UUIDAttributeType, optional: true),
            attribute("beanId", .UUIDAttributeType, optional: true), attribute("grinderId", .UUIDAttributeType, optional: true),
            attribute("doseGrams", .doubleAttributeType, defaultValue: 15), attribute("waterMl", .integer64AttributeType, defaultValue: 240),
            attribute("ratio", .doubleAttributeType, defaultValue: 16), attribute("temperatureC", .integer64AttributeType, defaultValue: 93),
            attribute("executionMode", .stringAttributeType, defaultValue: "GUIDED"), attribute("grindValue", .doubleAttributeType, defaultValue: 18),
            attribute("grindDescription", .stringAttributeType, defaultValue: "18 Clicks"), attribute("grindUnit", .stringAttributeType, defaultValue: "CLICKS"),
            attribute("notes", .stringAttributeType, defaultValue: ""), attribute("techniqueDescription", .stringAttributeType, defaultValue: ""),
            attribute("totalTimeSeconds", .integer64AttributeType, defaultValue: 180), attribute("visibility", .stringAttributeType, defaultValue: "PRIVATE"),
            attribute("originalEntityId", .UUIDAttributeType, optional: true), attribute("rootEntityId", .UUIDAttributeType, optional: true),
            attribute("copyMode", .stringAttributeType, defaultValue: "ORIGINAL")
        ]
        techniqueEntity.uniquenessConstraints = [["id"]]

        let techniqueStepEntity = NSEntityDescription()
        techniqueStepEntity.name = "TechniqueStepRecord"; techniqueStepEntity.managedObjectClassName = NSStringFromClass(TechniqueStepRecord.self)
        techniqueStepEntity.properties = syncProperties(attribute: attribute) + [
            attribute("techniqueId", .UUIDAttributeType), attribute("stepNumber", .integer64AttributeType, defaultValue: 1),
            attribute("title", .stringAttributeType, defaultValue: ""), attribute("durationSeconds", .integer64AttributeType, defaultValue: 30),
            attribute("waterAddedMl", .integer64AttributeType, defaultValue: 0), attribute("waterAccumulatedMl", .integer64AttributeType, defaultValue: 0),
            attribute("intensity", .stringAttributeType, defaultValue: "MEDIUM"), attribute("gesture", .stringAttributeType, defaultValue: "CIRCULAR_POUR"),
            attribute("stepNote", .stringAttributeType, defaultValue: ""), attribute("coverageValue", .doubleAttributeType, optional: true),
            attribute("flowValue", .doubleAttributeType, optional: true), attribute("secondaryAction", .stringAttributeType, optional: true)
        ]
        techniqueStepEntity.uniquenessConstraints = [["id"]]

        let brewSessionEntity = NSEntityDescription()
        brewSessionEntity.name = "BrewSessionRecord"; brewSessionEntity.managedObjectClassName = NSStringFromClass(BrewSessionRecord.self)
        brewSessionEntity.properties = syncProperties(attribute: attribute) + [
            attribute("techniqueId", .UUIDAttributeType, optional: true), attribute("recipeId", .UUIDAttributeType, optional: true),
            attribute("methodId", .UUIDAttributeType, optional: true),
            attribute("beanId", .UUIDAttributeType, optional: true), attribute("grinderId", .UUIDAttributeType, optional: true),
            attribute("techniqueNameSnapshot", .stringAttributeType, defaultValue: ""), attribute("recipeNameSnapshot", .stringAttributeType, defaultValue: ""),
            attribute("methodNameSnapshot", .stringAttributeType, defaultValue: ""),
            attribute("beanNameSnapshot", .stringAttributeType, defaultValue: ""), attribute("grinderNameSnapshot", .stringAttributeType, defaultValue: ""),
            attribute("doseGrams", .doubleAttributeType, defaultValue: 15), attribute("waterMl", .integer64AttributeType, defaultValue: 240),
            attribute("ratio", .doubleAttributeType, defaultValue: 16), attribute("temperatureC", .integer64AttributeType, defaultValue: 92),
            attribute("grindDescription", .stringAttributeType, defaultValue: ""), attribute("elapsedSeconds", .integer64AttributeType, defaultValue: 0),
            attribute("completedAt", .dateAttributeType), attribute("stepsSnapshotJSON", .stringAttributeType, defaultValue: "[]")
        ]
        brewSessionEntity.uniquenessConstraints = [["id"]]

        let tastingEntity = NSEntityDescription()
        tastingEntity.name = "TastingRecord"; tastingEntity.managedObjectClassName = NSStringFromClass(TastingRecord.self)
        tastingEntity.properties = syncProperties(attribute: attribute) + [
            attribute("brewSessionId", .UUIDAttributeType, optional: true), attribute("recipeId", .UUIDAttributeType, optional: true),
            attribute("techniqueId", .UUIDAttributeType, optional: true), attribute("beanId", .UUIDAttributeType, optional: true),
            attribute("activeFlavorFamily", .stringAttributeType, defaultValue: "FRUITY"), attribute("selectedFlavorNotesJSON", .stringAttributeType, defaultValue: "[]"),
            attribute("expectedNotes", .stringAttributeType, defaultValue: ""), attribute("texture", .stringAttributeType, defaultValue: "sedosa"),
            attribute("cleanliness", .stringAttributeType, defaultValue: "alta"), attribute("persistence", .stringAttributeType, defaultValue: "media"),
            attribute("aroma", .doubleAttributeType, defaultValue: 3), attribute("acidity", .doubleAttributeType, defaultValue: 3),
            attribute("sweetness", .doubleAttributeType, defaultValue: 3), attribute("body", .doubleAttributeType, defaultValue: 3),
            attribute("bitterness", .doubleAttributeType, defaultValue: 3), attribute("finishScore", .doubleAttributeType, defaultValue: 3),
            attribute("rating", .doubleAttributeType, defaultValue: 4), attribute("nps", .integer64AttributeType, defaultValue: 8),
            attribute("evaluatorNotes", .stringAttributeType, defaultValue: ""), attribute("coolingElapsedSeconds", .integer64AttributeType, defaultValue: 0),
            attribute("cupLifeState", .stringAttributeType, defaultValue: "FRESH"), attribute("evaluatedAt", .dateAttributeType)
        ]
        tastingEntity.uniquenessConstraints = [["id"]]

        let tastingObservationEntity = NSEntityDescription()
        tastingObservationEntity.name = "TastingObservationRecord"; tastingObservationEntity.managedObjectClassName = NSStringFromClass(TastingObservationRecord.self)
        tastingObservationEntity.properties = syncProperties(attribute: attribute) + [
            attribute("tastingId", .UUIDAttributeType), attribute("elapsedSeconds", .integer64AttributeType, defaultValue: 0),
            attribute("stage", .stringAttributeType, defaultValue: "HOT"), attribute("notes", .stringAttributeType, defaultValue: ""),
            attribute("aroma", .doubleAttributeType, defaultValue: 3), attribute("acidity", .doubleAttributeType, defaultValue: 3),
            attribute("sweetness", .doubleAttributeType, defaultValue: 3), attribute("body", .doubleAttributeType, defaultValue: 3),
            attribute("bitterness", .doubleAttributeType, defaultValue: 3), attribute("finishScore", .doubleAttributeType, defaultValue: 3)
        ]
        tastingObservationEntity.uniquenessConstraints = [["id"]]

        let cupSessionEntity = NSEntityDescription()
        cupSessionEntity.name = "CupSessionRecord"; cupSessionEntity.managedObjectClassName = NSStringFromClass(CupSessionRecord.self)
        cupSessionEntity.properties = syncProperties(attribute: attribute) + [
            attribute("brewSessionId", .UUIDAttributeType, optional: true), attribute("tastingId", .UUIDAttributeType),
            attribute("recipeId", .UUIDAttributeType, optional: true), attribute("beanId", .UUIDAttributeType, optional: true),
            attribute("techniqueId", .UUIDAttributeType, optional: true), attribute("methodId", .UUIDAttributeType, optional: true), attribute("grinderId", .UUIDAttributeType, optional: true),
            attribute("executedDoseGrams", .doubleAttributeType, defaultValue: 0), attribute("executedWaterMl", .integer64AttributeType, defaultValue: 0),
            attribute("executedRatio", .doubleAttributeType, defaultValue: 0), attribute("executedTemperatureC", .integer64AttributeType, defaultValue: 0),
            attribute("executedGrindSetting", .stringAttributeType, defaultValue: ""), attribute("executedDurationSeconds", .integer64AttributeType, defaultValue: 0),
            attribute("beanNameSnapshot", .stringAttributeType, defaultValue: ""), attribute("recipeNameSnapshot", .stringAttributeType, defaultValue: ""),
            attribute("techniqueNameSnapshot", .stringAttributeType, defaultValue: ""), attribute("methodNameSnapshot", .stringAttributeType, defaultValue: ""),
            attribute("grinderNameSnapshot", .stringAttributeType, defaultValue: ""), attribute("cupLifeSeconds", .integer64AttributeType, defaultValue: 0),
            attribute("cupLifeState", .stringAttributeType, defaultValue: "FRESH"), attribute("nps", .integer64AttributeType, defaultValue: 0),
            attribute("rating", .doubleAttributeType, defaultValue: 0), attribute("comment", .stringAttributeType, defaultValue: ""), attribute("brewDate", .dateAttributeType, optional: true),
            attribute("recipeSnapshotJSON", .stringAttributeType, defaultValue: "{}"), attribute("techniqueSnapshotJSON", .stringAttributeType, defaultValue: "{}"),
            attribute("beanSnapshotJSON", .stringAttributeType, defaultValue: "{}"), attribute("grinderSnapshotJSON", .stringAttributeType, defaultValue: "{}")
        ]
        cupSessionEntity.uniquenessConstraints = [["id"], ["tastingId"]]

        let syncOperationEntity = NSEntityDescription()
        syncOperationEntity.name = "SyncOperationRecord"; syncOperationEntity.managedObjectClassName = NSStringFromClass(SyncOperationRecord.self)
        syncOperationEntity.properties = syncProperties(attribute: attribute) + [
            attribute("entityName", .stringAttributeType, defaultValue: ""), attribute("entityId", .UUIDAttributeType),
            attribute("operation", .stringAttributeType, defaultValue: SyncStatus.pendingCreate.rawValue), attribute("payloadJSON", .stringAttributeType, defaultValue: "{}"),
            attribute("attemptCount", .integer64AttributeType, defaultValue: 0), attribute("nextAttemptAt", .dateAttributeType),
            attribute("lastError", .stringAttributeType, defaultValue: "")
        ]
        syncOperationEntity.uniquenessConstraints = [["id"], ["entityName", "entityId"]]

        let profileEntity = NSEntityDescription()
        profileEntity.name = "UserProfileRecord"; profileEntity.managedObjectClassName = NSStringFromClass(UserProfileRecord.self)
        profileEntity.properties = syncProperties(attribute: attribute) + [
            attribute("displayName", .stringAttributeType, defaultValue: ""), attribute("alias", .stringAttributeType, defaultValue: ""),
            attribute("biography", .stringAttributeType, defaultValue: ""), attribute("avatarColor", .stringAttributeType, defaultValue: "#3F7A63"),
            attribute("favoriteMethods", .stringAttributeType, defaultValue: ""), attribute("isPrivate", .booleanAttributeType, defaultValue: true)
        ]
        profileEntity.uniquenessConstraints = [["id"], ["ownerId"]]

        model.entities = [
            coffeeEntity, experimentEntity, grinderEntity, equipmentEntity,
            recipeEntity, ingredientEntity, recipeStepEntity, techniqueEntity, techniqueStepEntity, brewSessionEntity,
            tastingEntity, tastingObservationEntity, cupSessionEntity, syncOperationEntity, profileEntity
        ]
        return model
    }

    private static func syncProperties(attribute: (String, NSAttributeType, Bool, Any?) -> NSAttributeDescription) -> [NSAttributeDescription] {
        [
            attribute("id", .UUIDAttributeType, false, nil), attribute("ownerId", .UUIDAttributeType, true, nil),
            attribute("createdAt", .dateAttributeType, false, nil), attribute("updatedAt", .dateAttributeType, false, nil),
            attribute("version", .integer64AttributeType, false, 1), attribute("syncStatusRaw", .stringAttributeType, false, SyncStatus.pendingCreate.rawValue),
            attribute("deletedAt", .dateAttributeType, true, nil)
        ]
    }
}
