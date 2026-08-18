import CoreData
import Foundation

enum SyncStatus: String, Codable, CaseIterable {
    case synced, pendingCreate, pendingUpdate, pendingDelete, conflict, error
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
        id = UUID(); ownerId = nil; method = state.method
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
    let container: NSPersistentContainer

    init(inMemory: Bool = false) {
        container = NSPersistentContainer(name: "Cupa", managedObjectModel: Self.makeModel())
        let description = NSPersistentStoreDescription()
        if inMemory {
            description.type = NSInMemoryStoreType
        } else {
            description.type = NSSQLiteStoreType
            description.url = NSPersistentContainer.defaultDirectoryURL().appendingPathComponent("Cupa.sqlite")
        }
        description.shouldMigrateStoreAutomatically = true
        description.shouldInferMappingModelAutomatically = true
        description.setOption(true as NSNumber, forKey: NSPersistentHistoryTrackingKey)
        description.setOption(true as NSNumber, forKey: NSPersistentStoreRemoteChangeNotificationPostOptionKey)
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

        model.entities = [coffeeEntity, experimentEntity, grinderEntity, equipmentEntity]
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
