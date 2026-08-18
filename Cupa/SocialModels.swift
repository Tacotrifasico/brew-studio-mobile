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

struct SocialService {
    let configuration: AppConfiguration; let transport: NetworkTransport
    init(configuration: AppConfiguration = AppConfiguration(), transport: NetworkTransport = URLSessionTransport()) { self.configuration = configuration; self.transport = transport }

    func feed(accessToken: String) async throws -> [SocialShare] {
        let (data, _) = try await request(path: "rest/v1/brew_shares", query: "select=*&visibility=eq.PUBLIC&status=eq.ACTIVE&order=created_at.desc&limit=50", method: "GET", body: nil, accessToken: accessToken, prefer: nil)
        return try JSONDecoder().decode([SocialShare].self, from: data)
    }
    func publish(entityType: String, entityId: UUID, fromName: String, fromHandle: String, name: String, subtitle: String, message: String, payload: SharePayloadSnapshot, accessToken: String) async throws {
        try SocialContentPolicy.validate(fromName: fromName, fromHandle: fromHandle, name: name, subtitle: subtitle, message: message, payload: payload)
        let body: [String: Any] = ["entity_type": entityType, "entity_id": entityId.uuidString, "from_name": fromName, "from_handle": fromHandle, "visibility": "PUBLIC", "name": name, "subtitle": subtitle, "message": message, "payload_snapshot": try jsonObject(payload), "original_entity_id": entityId.uuidString]
        _ = try await request(path: "rest/v1/brew_shares", query: nil, method: "POST", body: try JSONSerialization.data(withJSONObject: body), accessToken: accessToken, prefer: "return=minimal")
    }
    func like(shareId: UUID, accessToken: String) async throws {
        _ = try await request(path: "rest/v1/share_likes", query: nil, method: "POST", body: try JSONSerialization.data(withJSONObject: ["share_id": shareId.uuidString]), accessToken: accessToken, prefer: "resolution=ignore-duplicates,return=minimal")
    }
    func report(shareId: UUID, reason: String, accessToken: String) async throws {
        _ = try await request(path: "rest/v1/content_reports", query: nil, method: "POST", body: try JSONSerialization.data(withJSONObject: ["share_id": shareId.uuidString, "reason": reason]), accessToken: accessToken, prefer: "return=minimal")
    }
    func block(userId: UUID, accessToken: String) async throws {
        _ = try await request(path: "rest/v1/blocked_users", query: nil, method: "POST", body: try JSONSerialization.data(withJSONObject: ["blocked_user_id": userId.uuidString]), accessToken: accessToken, prefer: "resolution=ignore-duplicates,return=minimal")
    }

    @MainActor func importShare(_ share: SocialShare, context: NSManagedObjectContext) throws {
        let repository = RecipeTechniqueRepository(context: context)
        if let value = share.payloadSnapshot.recipe {
            let draft = RecipeDraftModel(
                id: UUID(), name: "Copia de \(value.name)", recipeKind: value.recipeKind, intention: value.intention,
                suggestedMethodName: value.suggestedMethodName, tags: value.tags,
                ingredients: value.ingredients.map { .init(name: $0.name, amount: $0.amount, unit: $0.unit) },
                steps: value.steps.map { .init(instruction: $0.instruction, durationSeconds: $0.durationSeconds) }
            )
            let record = try repository.saveRecipe(draft); record.originalEntityId = share.originalEntityId ?? share.entityId; record.rootEntityId = share.originalEntityId ?? share.entityId; record.copyMode = "IMPORT"; record.markUpdated(); try context.save()
        } else if let value = share.payloadSnapshot.technique {
            let draft = TechniqueDraftModel(
                id: UUID(), name: "Copia de \(value.name)", methodName: value.methodName, doseGrams: value.doseGrams,
                waterMl: value.waterMl, ratio: value.ratio, temperatureC: value.temperatureC, executionMode: value.executionMode,
                grindValue: value.grindValue, grindDescription: value.grindDescription, grindUnit: value.grindUnit,
                notes: value.notes, techniqueDescription: value.techniqueDescription,
                steps: value.steps.map { .init(title: $0.title, durationSeconds: $0.durationSeconds, waterAddedMl: $0.waterAddedMl, intensity: $0.intensity, gesture: $0.gesture, note: $0.note) }
            )
            let record = try repository.saveTechnique(draft); record.originalEntityId = share.originalEntityId ?? share.entityId; record.rootEntityId = share.originalEntityId ?? share.entityId; record.copyMode = "IMPORT"; record.markUpdated(); try context.save()
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
    private func jsonObject<T: Encodable>(_ value: T) throws -> Any { try JSONSerialization.jsonObject(with: JSONEncoder().encode(value)) }
}
