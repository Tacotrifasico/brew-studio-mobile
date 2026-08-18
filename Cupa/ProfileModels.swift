import CoreData
import Foundation

@objc(UserProfileRecord)
final class UserProfileRecord: NSManagedObject, SyncTrackedRecord {
    @NSManaged var id: UUID; @NSManaged var ownerId: UUID?; @NSManaged var displayName: String; @NSManaged var alias: String
    @NSManaged var biography: String; @NSManaged var avatarColor: String; @NSManaged var favoriteMethods: String; @NSManaged var isPrivate: Bool
    @NSManaged var createdAt: Date; @NSManaged var updatedAt: Date; @NSManaged var version: Int64; @NSManaged var syncStatusRaw: String; @NSManaged var deletedAt: Date?
    func markUpdated() { trackUpdate() }; func markDeleted() { trackDeletion() }
}
extension UserProfileRecord: Identifiable {}

@MainActor
struct ProfileRepository {
    let context: NSManagedObjectContext
    func profile(ownerId: UUID) throws -> UserProfileRecord? {
        let request = NSFetchRequest<UserProfileRecord>(entityName: "UserProfileRecord")
        request.predicate = NSPredicate(format: "ownerId == %@ AND deletedAt == nil", ownerId as CVarArg); request.fetchLimit = 1
        return try context.fetch(request).first
    }
    @discardableResult func save(ownerId: UUID, displayName: String, alias: String, biography: String, avatarColor: String, favoriteMethods: String, isPrivate: Bool) throws -> UserProfileRecord {
        let record = try profile(ownerId: ownerId) ?? UserProfileRecord(context: context)
        if record.value(forKey: "createdAt") == nil {
            record.id = ownerId; record.ownerId = ownerId; record.createdAt = .now; record.version = 1; record.syncStatusRaw = SyncStatus.pendingCreate.rawValue; record.deletedAt = nil
        } else { record.markUpdated() }
        record.displayName = displayName.trimmingCharacters(in: .whitespacesAndNewlines)
        record.alias = alias.trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: "@", with: "")
        record.biography = biography; record.avatarColor = avatarColor; record.favoriteMethods = favoriteMethods; record.isPrivate = isPrivate; record.updatedAt = .now
        try context.save(); return record
    }
    func remoteJSON(_ record: UserProfileRecord) throws -> Data {
        try JSONSerialization.data(withJSONObject: [[
            "id": record.id.uuidString, "display_name": record.displayName, "alias": record.alias,
            "biography": record.biography, "avatar_color": record.avatarColor, "favorite_methods": record.favoriteMethods,
            "preferences": ["avatar_color": record.avatarColor, "favorite_methods": record.favoriteMethods],
            "is_private": record.isPrivate, "updated_at": ISO8601DateFormatter().string(from: record.updatedAt), "version": record.version
        ]])
    }
}
