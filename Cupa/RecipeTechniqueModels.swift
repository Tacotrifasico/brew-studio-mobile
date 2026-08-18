import CoreData
import Foundation

protocol SyncTrackedRecord: AnyObject {
    var updatedAt: Date { get set }
    var version: Int64 { get set }
    var syncStatusRaw: String { get set }
    var deletedAt: Date? { get set }
}

extension SyncTrackedRecord {
    var trackedSyncStatus: SyncStatus {
        get { SyncStatus(rawValue: syncStatusRaw) ?? .error }
        set { syncStatusRaw = newValue.rawValue }
    }
    func trackUpdate() { updatedAt = .now; version += 1; trackedSyncStatus = .pendingUpdate }
    func trackDeletion() { deletedAt = .now; updatedAt = .now; version += 1; trackedSyncStatus = .pendingDelete }
}

@objc(RecipeRecord)
final class RecipeRecord: NSManagedObject, SyncTrackedRecord {
    @NSManaged var id: UUID; @NSManaged var ownerId: UUID?
    @NSManaged var name: String; @NSManaged var recipeKind: String; @NSManaged var intention: String
    @NSManaged var suggestedMethodId: UUID?; @NSManaged var suggestedMethodName: String
    @NSManaged var isFavorite: Bool; @NSManaged var tags: String; @NSManaged var visibility: String
    @NSManaged var originalEntityId: UUID?; @NSManaged var rootEntityId: UUID?; @NSManaged var copyMode: String
    @NSManaged var createdAt: Date; @NSManaged var updatedAt: Date; @NSManaged var version: Int64
    @NSManaged var syncStatusRaw: String; @NSManaged var deletedAt: Date?

    convenience init(context: NSManagedObjectContext, id: UUID = UUID(), name: String, recipeKind: String, intention: String, suggestedMethodId: UUID?, suggestedMethodName: String, isFavorite: Bool, tags: String, originalEntityId: UUID? = nil, rootEntityId: UUID? = nil, copyMode: String = "ORIGINAL") {
        self.init(context: context); self.id = id; ownerId = nil; self.name = name; self.recipeKind = recipeKind
        self.intention = intention; self.suggestedMethodId = suggestedMethodId; self.suggestedMethodName = suggestedMethodName
        self.isFavorite = isFavorite; self.tags = tags; visibility = "PRIVATE"
        self.originalEntityId = originalEntityId; self.rootEntityId = rootEntityId; self.copyMode = copyMode
        createdAt = .now; updatedAt = .now; version = 1; syncStatusRaw = SyncStatus.pendingCreate.rawValue; deletedAt = nil
    }
    var syncStatus: SyncStatus {
        get { trackedSyncStatus }
        set { trackedSyncStatus = newValue }
    }
    func markUpdated() { trackUpdate() }; func markDeleted() { trackDeletion() }
}
extension RecipeRecord: Identifiable {}

@objc(RecipeIngredientRecord)
final class RecipeIngredientRecord: NSManagedObject, SyncTrackedRecord {
    @NSManaged var id: UUID; @NSManaged var ownerId: UUID?; @NSManaged var recipeId: UUID
    @NSManaged var name: String; @NSManaged var amount: Double; @NSManaged var unit: String; @NSManaged var orderIndex: Int64
    @NSManaged var createdAt: Date; @NSManaged var updatedAt: Date; @NSManaged var version: Int64
    @NSManaged var syncStatusRaw: String; @NSManaged var deletedAt: Date?
    convenience init(context: NSManagedObjectContext, id: UUID = UUID(), recipeId: UUID, name: String, amount: Double, unit: String, orderIndex: Int) {
        self.init(context: context); self.id = id; ownerId = nil; self.recipeId = recipeId; self.name = name
        self.amount = amount; self.unit = unit; self.orderIndex = Int64(orderIndex)
        createdAt = .now; updatedAt = .now; version = 1; syncStatusRaw = SyncStatus.pendingCreate.rawValue; deletedAt = nil
    }
    func markUpdated() { trackUpdate() }; func markDeleted() { trackDeletion() }
}
extension RecipeIngredientRecord: Identifiable {}

@objc(RecipeStepRecord)
final class RecipeStepRecord: NSManagedObject, SyncTrackedRecord {
    @NSManaged var id: UUID; @NSManaged var ownerId: UUID?; @NSManaged var recipeId: UUID
    @NSManaged var instruction: String; @NSManaged var stepNumber: Int64; @NSManaged var durationSecondsValue: NSNumber?
    @NSManaged var createdAt: Date; @NSManaged var updatedAt: Date; @NSManaged var version: Int64
    @NSManaged var syncStatusRaw: String; @NSManaged var deletedAt: Date?
    var durationSeconds: Int? {
        get { durationSecondsValue?.intValue }
        set { durationSecondsValue = newValue.map(NSNumber.init(value:)) }
    }
    convenience init(context: NSManagedObjectContext, id: UUID = UUID(), recipeId: UUID, instruction: String, stepNumber: Int, durationSeconds: Int?) {
        self.init(context: context); self.id = id; ownerId = nil; self.recipeId = recipeId; self.instruction = instruction
        self.stepNumber = Int64(stepNumber); self.durationSeconds = durationSeconds
        createdAt = .now; updatedAt = .now; version = 1; syncStatusRaw = SyncStatus.pendingCreate.rawValue; deletedAt = nil
    }
    func markUpdated() { trackUpdate() }; func markDeleted() { trackDeletion() }
}
extension RecipeStepRecord: Identifiable {}

@objc(TechniqueRecord)
final class TechniqueRecord: NSManagedObject, SyncTrackedRecord {
    @NSManaged var id: UUID; @NSManaged var ownerId: UUID?
    @NSManaged var name: String; @NSManaged var methodId: UUID?; @NSManaged var methodName: String
    @NSManaged var recipeId: UUID?; @NSManaged var beanId: UUID?; @NSManaged var grinderId: UUID?
    @NSManaged var doseGrams: Double; @NSManaged var waterMl: Int64; @NSManaged var ratio: Double; @NSManaged var temperatureC: Int64
    @NSManaged var executionMode: String; @NSManaged var grindValue: Double; @NSManaged var grindDescription: String; @NSManaged var grindUnit: String
    @NSManaged var notes: String; @NSManaged var techniqueDescription: String; @NSManaged var totalTimeSeconds: Int64
    @NSManaged var visibility: String; @NSManaged var originalEntityId: UUID?; @NSManaged var rootEntityId: UUID?; @NSManaged var copyMode: String
    @NSManaged var createdAt: Date; @NSManaged var updatedAt: Date; @NSManaged var version: Int64
    @NSManaged var syncStatusRaw: String; @NSManaged var deletedAt: Date?

    convenience init(context: NSManagedObjectContext, id: UUID = UUID(), name: String, methodId: UUID?, methodName: String, recipeId: UUID?, beanId: UUID?, grinderId: UUID?, doseGrams: Double, waterMl: Int, ratio: Double, temperatureC: Int, executionMode: String, grindValue: Double, grindDescription: String, grindUnit: String, notes: String, techniqueDescription: String, totalTimeSeconds: Int) {
        self.init(context: context); self.id = id; ownerId = nil; self.name = name; self.methodId = methodId; self.methodName = methodName
        self.recipeId = recipeId; self.beanId = beanId; self.grinderId = grinderId
        self.doseGrams = doseGrams; self.waterMl = Int64(waterMl); self.ratio = ratio; self.temperatureC = Int64(temperatureC)
        self.executionMode = executionMode; self.grindValue = grindValue; self.grindDescription = grindDescription; self.grindUnit = grindUnit
        self.notes = notes; self.techniqueDescription = techniqueDescription; self.totalTimeSeconds = Int64(totalTimeSeconds)
        visibility = "PRIVATE"; originalEntityId = nil; rootEntityId = nil; copyMode = "ORIGINAL"
        createdAt = .now; updatedAt = .now; version = 1; syncStatusRaw = SyncStatus.pendingCreate.rawValue; deletedAt = nil
    }
    var syncStatus: SyncStatus {
        get { trackedSyncStatus }
        set { trackedSyncStatus = newValue }
    }
    func markUpdated() { trackUpdate() }; func markDeleted() { trackDeletion() }
}
extension TechniqueRecord: Identifiable {}

@objc(TechniqueStepRecord)
final class TechniqueStepRecord: NSManagedObject, SyncTrackedRecord {
    @NSManaged var id: UUID; @NSManaged var ownerId: UUID?; @NSManaged var techniqueId: UUID
    @NSManaged var stepNumber: Int64; @NSManaged var title: String; @NSManaged var durationSeconds: Int64
    @NSManaged var waterAddedMl: Int64; @NSManaged var waterAccumulatedMl: Int64
    @NSManaged var intensity: String; @NSManaged var gesture: String; @NSManaged var stepNote: String
    @NSManaged var coverageValue: NSNumber?; @NSManaged var flowValue: NSNumber?; @NSManaged var secondaryAction: String?
    @NSManaged var createdAt: Date; @NSManaged var updatedAt: Date; @NSManaged var version: Int64
    @NSManaged var syncStatusRaw: String; @NSManaged var deletedAt: Date?
    var coverage: Double? {
        get { coverageValue?.doubleValue }
        set { coverageValue = newValue.map(NSNumber.init(value:)) }
    }
    var flow: Double? {
        get { flowValue?.doubleValue }
        set { flowValue = newValue.map(NSNumber.init(value:)) }
    }
    convenience init(context: NSManagedObjectContext, id: UUID = UUID(), techniqueId: UUID, stepNumber: Int, title: String, durationSeconds: Int, waterAddedMl: Int, waterAccumulatedMl: Int, intensity: String, gesture: String, stepNote: String, coverage: Double?, flow: Double?, secondaryAction: String?) {
        self.init(context: context); self.id = id; ownerId = nil; self.techniqueId = techniqueId
        self.stepNumber = Int64(stepNumber); self.title = title; self.durationSeconds = Int64(durationSeconds)
        self.waterAddedMl = Int64(waterAddedMl); self.waterAccumulatedMl = Int64(waterAccumulatedMl)
        self.intensity = intensity; self.gesture = gesture; self.stepNote = stepNote; self.coverage = coverage; self.flow = flow; self.secondaryAction = secondaryAction
        createdAt = .now; updatedAt = .now; version = 1; syncStatusRaw = SyncStatus.pendingCreate.rawValue; deletedAt = nil
    }
    func markUpdated() { trackUpdate() }; func markDeleted() { trackDeletion() }
}
extension TechniqueStepRecord: Identifiable {}
