import CoreData
import Foundation

struct SharedRecipeIngredient: Codable, Equatable { let name: String; let amount: Double; let unit: String }
struct SharedRecipeStep: Codable, Equatable { let instruction: String; let durationSeconds: Int? }
struct SharedRecipeSnapshot: Codable, Equatable {
    let name: String; let recipeKind: String; let intention: String; let suggestedMethodName: String; let tags: String
    let ingredients: [SharedRecipeIngredient]; let steps: [SharedRecipeStep]
}
struct SharedTechniqueStep: Codable, Equatable {
    let title: String; let durationSeconds: Int; let waterAddedMl: Int; let intensity: String; let gesture: String; let note: String
}
struct SharedTechniqueSnapshot: Codable, Equatable {
    let name: String; let methodName: String; let doseGrams: Double; let waterMl: Int; let ratio: Double; let temperatureC: Int
    let executionMode: String; let grindValue: Double; let grindDescription: String; let grindUnit: String; let notes: String
    let techniqueDescription: String; let steps: [SharedTechniqueStep]
}
struct SharePayloadSnapshot: Codable, Equatable { let kind: String; let recipe: SharedRecipeSnapshot?; let technique: SharedTechniqueSnapshot? }

enum SocialValidationError: LocalizedError, Equatable {
    case emptyIdentity, tooLong, objectionableContent
    var errorDescription: String? {
        switch self {
        case .emptyIdentity: "Completa tu nombre y alias antes de publicar."
        case .tooLong: "La publicación excede la longitud permitida."
        case .objectionableContent: "La publicación contiene texto que no está permitido en la comunidad."
        }
    }
}

enum SocialContentPolicy {
    private static let blockedPhrases = [
        "pornografia", "pornography", "violacion", "rape", "nazi", "terrorista", "terrorist",
        "matarte", "kill yourself", "suicidate", "suicide", "odio racial", "racial hate"
    ]

    static func validate(fromName: String, fromHandle: String, name: String, subtitle: String, message: String, payload: SharePayloadSnapshot) throws {
        guard !fromName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              !fromHandle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { throw SocialValidationError.emptyIdentity }
        guard fromName.count <= 80, fromHandle.count <= 40, name.count <= 160, subtitle.count <= 300, message.count <= 1_000 else { throw SocialValidationError.tooLong }
        let payloadText = (try? String(data: JSONEncoder().encode(payload), encoding: .utf8)) ?? ""
        let combined = [fromName, fromHandle, name, subtitle, message, payloadText].joined(separator: " ")
            .folding(options: [.diacriticInsensitive, .caseInsensitive], locale: Locale(identifier: "es_MX"))
            .lowercased()
        guard !blockedPhrases.contains(where: combined.contains) else { throw SocialValidationError.objectionableContent }
    }
}

struct SocialShare: Codable, Identifiable, Equatable {
    let id: UUID; let ownerId: UUID; let entityType: String; let entityId: UUID; let fromName: String; let fromHandle: String
    let targetUserId: UUID?; let visibility: String; let name: String; let subtitle: String; let message: String
    let payloadSnapshot: SharePayloadSnapshot; let originalEntityId: UUID?; let status: String; let createdAt: String; let updatedAt: String
    enum CodingKeys: String, CodingKey {
        case id, visibility, name, subtitle, message, status
        case ownerId = "owner_id", entityType = "entity_type", entityId = "entity_id", fromName = "from_name", fromHandle = "from_handle"
        case targetUserId = "target_user_id", payloadSnapshot = "payload_snapshot", originalEntityId = "original_entity_id"
        case createdAt = "created_at", updatedAt = "updated_at"
    }
}

struct SocialInboxItem: Codable, Identifiable, Equatable {
    let id: UUID; let shareId: UUID; let targetUserId: UUID; let readAt: String?; let createdAt: String; let share: SocialShare?
    enum CodingKeys: String, CodingKey {
        case id, share
        case shareId = "share_id", targetUserId = "target_user_id", readAt = "read_at", createdAt = "created_at"
    }
}

struct SocialActivity: Codable, Identifiable, Equatable {
    let id: UUID; let userId: UUID; let action: String; let entityType: String?; let entityId: UUID?; let shareId: UUID?; let note: String?; let createdAt: String
    enum CodingKeys: String, CodingKey {
        case id, action, note
        case userId = "user_id", entityType = "entity_type", entityId = "entity_id", shareId = "share_id", createdAt = "created_at"
    }
}

enum SocialCopyMode: String { case imported = "IMPORT", forked = "FORK" }

struct SocialService {
    let configuration: AppConfiguration; let transport: NetworkTransport
    init(configuration: AppConfiguration = AppConfiguration(), transport: NetworkTransport = URLSessionTransport()) { self.configuration = configuration; self.transport = transport }

    func feed(accessToken: String) async throws -> [SocialShare] {
        let (data, _) = try await request(path: "rest/v1/brew_shares", query: "select=*&visibility=eq.PUBLIC&status=eq.ACTIVE&order=created_at.desc&limit=50", method: "GET", body: nil, accessToken: accessToken, prefer: nil)
        return try JSONDecoder().decode([SocialShare].self, from: data)
    }
    func likedShareIds(userId: UUID, accessToken: String) async throws -> Set<UUID> {
        let (data, _) = try await request(path: "rest/v1/share_likes", query: "select=share_id&user_id=eq.\(userId.uuidString)", method: "GET", body: nil, accessToken: accessToken, prefer: nil)
        return try decodeShareIds(data)
    }
    func savedShareIds(userId: UUID, accessToken: String) async throws -> Set<UUID> {
        let (data, _) = try await request(path: "rest/v1/share_saves", query: "select=share_id&user_id=eq.\(userId.uuidString)", method: "GET", body: nil, accessToken: accessToken, prefer: nil)
        return try decodeShareIds(data)
    }
    func inbox(userId: UUID, accessToken: String) async throws -> [SocialInboxItem] {
        let query = "select=id,share_id,target_user_id,read_at,created_at,share:brew_shares(*)&target_user_id=eq.\(userId.uuidString)&order=created_at.desc&limit=50"
        let (data, _) = try await request(path: "rest/v1/inbox_items", query: query, method: "GET", body: nil, accessToken: accessToken, prefer: nil)
        return try JSONDecoder().decode([SocialInboxItem].self, from: data)
    }
    func activity(userId: UUID, accessToken: String) async throws -> [SocialActivity] {
        let query = "select=*&user_id=eq.\(userId.uuidString)&order=created_at.desc&limit=100"
        let (data, _) = try await request(path: "rest/v1/activity_log", query: query, method: "GET", body: nil, accessToken: accessToken, prefer: nil)
        return try JSONDecoder().decode([SocialActivity].self, from: data)
    }
    func markInboxRead(itemId: UUID, accessToken: String, date: Date = .now) async throws {
        let body = try JSONSerialization.data(withJSONObject: ["read_at": ISO8601DateFormatter().string(from: date)])
        _ = try await request(path: "rest/v1/inbox_items", query: "id=eq.\(itemId.uuidString)", method: "PATCH", body: body, accessToken: accessToken, prefer: "return=minimal")
    }
    func logActivity(action: String, entityType: String? = nil, entityId: UUID? = nil, shareId: UUID? = nil, note: String? = nil, accessToken: String) async throws {
        var body: [String: Any] = ["action": action]
        if let entityType { body["entity_type"] = entityType }; if let entityId { body["entity_id"] = entityId.uuidString }
        if let shareId { body["share_id"] = shareId.uuidString }; if let note { body["note"] = note }
        _ = try await request(path: "rest/v1/activity_log", query: nil, method: "POST", body: try JSONSerialization.data(withJSONObject: body), accessToken: accessToken, prefer: "return=minimal")
    }
    func publish(entityType: String, entityId: UUID, fromName: String, fromHandle: String, name: String, subtitle: String, message: String, payload: SharePayloadSnapshot, visibility: String = "PUBLIC", targetUserId: UUID? = nil, accessToken: String) async throws {
        try SocialContentPolicy.validate(fromName: fromName, fromHandle: fromHandle, name: name, subtitle: subtitle, message: message, payload: payload)
        guard visibility == "PUBLIC" || (visibility == "DIRECT" && targetUserId != nil) else { throw AuthServiceError.invalidResponse }
        var body: [String: Any] = ["entity_type": entityType, "entity_id": entityId.uuidString, "from_name": fromName, "from_handle": fromHandle, "visibility": visibility, "name": name, "subtitle": subtitle, "message": message, "payload_snapshot": try jsonObject(payload), "original_entity_id": entityId.uuidString]
        if let targetUserId { body["target_user_id"] = targetUserId.uuidString }
        _ = try await request(path: "rest/v1/brew_shares", query: nil, method: "POST", body: try JSONSerialization.data(withJSONObject: body), accessToken: accessToken, prefer: "return=minimal")
    }
    func like(shareId: UUID, accessToken: String) async throws {
        _ = try await request(path: "rest/v1/share_likes", query: nil, method: "POST", body: try JSONSerialization.data(withJSONObject: ["share_id": shareId.uuidString]), accessToken: accessToken, prefer: "resolution=ignore-duplicates,return=minimal")
    }
    func unlike(shareId: UUID, accessToken: String) async throws {
        _ = try await request(path: "rest/v1/share_likes", query: "share_id=eq.\(shareId.uuidString)", method: "DELETE", body: nil, accessToken: accessToken, prefer: "return=minimal")
    }
    func save(shareId: UUID, accessToken: String) async throws {
        _ = try await request(path: "rest/v1/share_saves", query: nil, method: "POST", body: try JSONSerialization.data(withJSONObject: ["share_id": shareId.uuidString]), accessToken: accessToken, prefer: "resolution=ignore-duplicates,return=minimal")
    }
    func unsave(shareId: UUID, accessToken: String) async throws {
        _ = try await request(path: "rest/v1/share_saves", query: "share_id=eq.\(shareId.uuidString)", method: "DELETE", body: nil, accessToken: accessToken, prefer: "return=minimal")
    }
    func report(shareId: UUID, reason: String, accessToken: String) async throws {
        _ = try await request(path: "rest/v1/content_reports", query: nil, method: "POST", body: try JSONSerialization.data(withJSONObject: ["share_id": shareId.uuidString, "reason": reason]), accessToken: accessToken, prefer: "return=minimal")
    }
    func block(userId: UUID, accessToken: String) async throws {
        _ = try await request(path: "rest/v1/blocked_users", query: nil, method: "POST", body: try JSONSerialization.data(withJSONObject: ["blocked_user_id": userId.uuidString]), accessToken: accessToken, prefer: "resolution=ignore-duplicates,return=minimal")
    }

    @MainActor func importShare(_ share: SocialShare, mode: SocialCopyMode = .imported, context: NSManagedObjectContext) throws {
        let repository = RecipeTechniqueRepository(context: context)
        if let value = share.payloadSnapshot.recipe {
            let draft = RecipeDraftModel(
                id: UUID(), name: mode == .forked ? "\(value.name) (Variante)" : "Copia de \(value.name)", recipeKind: value.recipeKind, intention: value.intention,
                suggestedMethodName: value.suggestedMethodName, tags: value.tags,
                ingredients: value.ingredients.map { .init(name: $0.name, amount: $0.amount, unit: $0.unit) },
                steps: value.steps.map { .init(instruction: $0.instruction, durationSeconds: $0.durationSeconds) }
            )
            let record = try repository.saveRecipe(draft); record.originalEntityId = share.originalEntityId ?? share.entityId; record.rootEntityId = share.originalEntityId ?? share.entityId; record.copyMode = mode.rawValue; record.markUpdated(); try context.save()
        } else if let value = share.payloadSnapshot.technique {
            let draft = TechniqueDraftModel(
                id: UUID(), name: mode == .forked ? "\(value.name) (Variante)" : "Copia de \(value.name)", methodName: value.methodName, doseGrams: value.doseGrams,
                waterMl: value.waterMl, ratio: value.ratio, temperatureC: value.temperatureC, executionMode: value.executionMode,
                grindValue: value.grindValue, grindDescription: value.grindDescription, grindUnit: value.grindUnit,
                notes: value.notes, techniqueDescription: value.techniqueDescription,
                steps: value.steps.map { .init(title: $0.title, durationSeconds: $0.durationSeconds, waterAddedMl: $0.waterAddedMl, intensity: $0.intensity, gesture: $0.gesture, note: $0.note) }
            )
            let record = try repository.saveTechnique(draft); record.originalEntityId = share.originalEntityId ?? share.entityId; record.rootEntityId = share.originalEntityId ?? share.entityId; record.copyMode = mode.rawValue; record.markUpdated(); try context.save()
        } else { throw AuthServiceError.invalidResponse }
    }

    private func request(path: String, query: String?, method: String, body: Data?, accessToken: String, prefer: String?) async throws -> (Data, HTTPURLResponse) {
        guard let base = configuration.supabaseURL, let key = configuration.supabaseAnonKey, !key.isEmpty else { throw AuthServiceError.notConfigured }
        var components = URLComponents(url: base.appendingPathComponent(path), resolvingAgainstBaseURL: false); components?.percentEncodedQuery = query
        guard let url = components?.url else { throw URLError(.badURL) }
        var request = URLRequest(url: url); request.httpMethod = method; request.httpBody = body; request.timeoutInterval = 30
        request.setValue(key, forHTTPHeaderField: "apikey"); request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization"); request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let prefer { request.setValue(prefer, forHTTPHeaderField: "Prefer") }
        let result = try await transport.data(for: request)
        guard (200..<300).contains(result.1.statusCode) else { throw AuthServiceError.server(result.1.statusCode, "No se pudo completar la acción social.") }
        return result
    }
    private func decodeShareIds(_ data: Data) throws -> Set<UUID> {
        struct Reference: Decodable { let shareId: UUID; enum CodingKeys: String, CodingKey { case shareId = "share_id" } }
        return Set(try JSONDecoder().decode([Reference].self, from: data).map(\.shareId))
    }
    private func jsonObject<T: Encodable>(_ value: T) throws -> Any { try JSONSerialization.jsonObject(with: JSONEncoder().encode(value)) }
}
