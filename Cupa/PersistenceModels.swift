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

struct PersistenceController {
    static let shared = PersistenceController()
    let container: NSPersistentContainer

    init(inMemory: Bool = false) {
        container = NSPersistentContainer(name: "Cupa", managedObjectModel: Self.makeModel())
        if inMemory { container.persistentStoreDescriptions.first?.url = URL(fileURLWithPath: "/dev/null") }
        container.persistentStoreDescriptions.first?.setOption(true as NSNumber, forKey: NSPersistentHistoryTrackingKey)
        container.persistentStoreDescriptions.first?.setOption(true as NSNumber, forKey: NSPersistentStoreRemoteChangeNotificationPostOptionKey)
        container.loadPersistentStores { _, error in
            if let error { fatalError("No se pudo abrir el almacenamiento local: \(error.localizedDescription)") }
        }
        container.viewContext.automaticallyMergesChangesFromParent = true
        container.viewContext.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy
    }

    private static func makeModel() -> NSManagedObjectModel {
        let model = NSManagedObjectModel()
        let entity = NSEntityDescription()
        entity.name = "CoffeeBeanRecord"
        entity.managedObjectClassName = NSStringFromClass(CoffeeBeanRecord.self)

        func attribute(_ name: String, _ type: NSAttributeType, optional: Bool = false, defaultValue: Any? = nil) -> NSAttributeDescription {
            let item = NSAttributeDescription()
            item.name = name; item.attributeType = type; item.isOptional = optional; item.defaultValue = defaultValue
            return item
        }
        entity.properties = [
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
        entity.uniquenessConstraints = [["id"]]
        model.entities = [entity]
        return model
    }
}
