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

struct ValidatedProfileInput: Equatable {
    let displayName: String
    let alias: String
    let biography: String
    let avatarColor: String
    let favoriteMethods: String
}

enum ProfileInputError: LocalizedError, Equatable {
    case missingDisplayName, missingAlias, displayNameTooLong, invalidAlias, biographyTooLong, invalidAvatarColor, favoriteMethodsTooLong

    var errorDescription: String? {
        switch self {
        case .missingDisplayName: "Escribe el nombre que quieres mostrar."
        case .missingAlias: "Escribe un alias."
        case .displayNameTooLong: "El nombre debe tener 80 caracteres o menos."
        case .invalidAlias: "El alias debe tener hasta 40 caracteres y usar sólo letras, números, punto, guion o guion bajo."
        case .biographyTooLong: "La biografía debe tener 300 caracteres o menos."
        case .invalidAvatarColor: "El color del avatar debe tener el formato #RRGGBB."
        case .favoriteMethodsTooLong: "Los métodos favoritos deben tener 300 caracteres o menos."
        }
    }
}

enum ProfileInputValidator {
    static func validate(displayName: String, alias: String, biography: String, avatarColor: String, favoriteMethods: String) throws -> ValidatedProfileInput {
        let cleanName = displayName.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanAlias = alias.trimmingCharacters(in: .whitespacesAndNewlines).drop(while: { $0 == "@" })
        let cleanBiography = biography.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanColor = avatarColor.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        let cleanMethods = favoriteMethods.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanName.isEmpty else { throw ProfileInputError.missingDisplayName }
        guard !cleanAlias.isEmpty else { throw ProfileInputError.missingAlias }
        guard cleanName.count <= 80 else { throw ProfileInputError.displayNameTooLong }
        guard cleanAlias.count <= 40,
              cleanAlias.range(of: "^[A-Za-z0-9._-]+$", options: .regularExpression) != nil else { throw ProfileInputError.invalidAlias }
        guard cleanBiography.count <= 300 else { throw ProfileInputError.biographyTooLong }
        guard cleanColor.range(of: "^#[0-9A-F]{6}$", options: .regularExpression) != nil else { throw ProfileInputError.invalidAvatarColor }
        guard cleanMethods.count <= 300 else { throw ProfileInputError.favoriteMethodsTooLong }
        return .init(displayName: cleanName, alias: String(cleanAlias), biography: cleanBiography, avatarColor: cleanColor, favoriteMethods: cleanMethods)
    }
}

enum ProfileSharingPolicy {
    static func allowedVisibilities(isPrivate: Bool) -> [String] { isPrivate ? ["DIRECT"] : ["PUBLIC", "DIRECT"] }
}

@MainActor
struct ProfileRepository {
    let context: NSManagedObjectContext
    func profile(ownerId: UUID) throws -> UserProfileRecord? {
        let request = NSFetchRequest<UserProfileRecord>(entityName: "UserProfileRecord")
        request.predicate = NSPredicate(format: "ownerId == %@ AND deletedAt == nil", ownerId as CVarArg); request.fetchLimit = 1
        return try context.fetch(request).first
    }
    @discardableResult func save(ownerId: UUID, displayName: String, alias: String, biography: String, avatarColor: String, favoriteMethods: String, isPrivate: Bool) throws -> UserProfileRecord {
        let input = try ProfileInputValidator.validate(displayName: displayName, alias: alias, biography: biography, avatarColor: avatarColor, favoriteMethods: favoriteMethods)
        let record = try profile(ownerId: ownerId) ?? UserProfileRecord(context: context)
        if record.value(forKey: "createdAt") == nil {
            record.id = ownerId; record.ownerId = ownerId; record.createdAt = .now; record.version = 1; record.syncStatusRaw = SyncStatus.pendingCreate.rawValue; record.deletedAt = nil
        } else { record.markUpdated() }
        record.displayName = input.displayName; record.alias = input.alias; record.biography = input.biography
        record.avatarColor = input.avatarColor; record.favoriteMethods = input.favoriteMethods; record.isPrivate = isPrivate; record.updatedAt = .now
        try context.save(); return record
    }
    func remoteJSON(_ record: UserProfileRecord) throws -> Data {
        try JSONSerialization.data(withJSONObject: [[
            "id": record.id.uuidString, "display_name": record.displayName, "handle": record.alias,
            "biography": record.biography, "avatar_color": record.avatarColor, "favorite_methods": record.favoriteMethods,
            "preferences": ["avatar_color": record.avatarColor, "favorite_methods": record.favoriteMethods],
            "is_private": record.isPrivate, "updated_at": ISO8601DateFormatter().string(from: record.updatedAt), "version": record.version
        ]])
    }
}
