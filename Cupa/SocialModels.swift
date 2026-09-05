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

private enum SnapshotKeys: String, CodingKey {
    case kind, recipe, technique, name, recipeKind, recipe_kind, intention, suggestedMethodName, suggested_method_name, method, methodName, method_name
    case tags, ingredients, steps, doseG, dose_g, coffeeGrams, coffee_grams, waterMl, water_ml, ratio, temperatureC, temperature_c, temperature
    case executionMode, execution_mode, grindValue, grind_value, grindDescription, grind_description, grindClicks, grind_clicks, grindUnit, grind_unit, notes
    case description, techniqueDescription, ingredientsSummary, stepsSummary
}

extension SharePayloadSnapshot {
    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: SnapshotKeys.self)
        if values.contains(.recipe) || values.contains(.technique) {
            kind = try values.decodeIfPresent(String.self, forKey: .kind) ?? (values.contains(.recipe) ? "recipe" : "technique")
            recipe = try values.decodeIfPresent(SharedRecipeSnapshot.self, forKey: .recipe)
            technique = try values.decodeIfPresent(SharedTechniqueSnapshot.self, forKey: .technique)
            return
        }
        let name = try values.decodeIfPresent(String.self, forKey: .name) ?? "Publicación"
        let isRecipe = values.contains(.recipeKind) || values.contains(.recipe_kind) || values.contains(.intention) || values.contains(.ingredientsSummary)
        if isRecipe {
            kind = "recipe"
            recipe = SharedRecipeSnapshot(
                name: name,
                recipeKind: try values.decodeIfPresent(String.self, forKey: .recipeKind) ?? values.decodeIfPresent(String.self, forKey: .recipe_kind) ?? "BLACK_COFFEE",
                intention: try values.decodeIfPresent(String.self, forKey: .intention) ?? "",
                suggestedMethodName: try values.decodeIfPresent(String.self, forKey: .suggestedMethodName) ?? values.decodeIfPresent(String.self, forKey: .suggested_method_name) ?? values.decodeIfPresent(String.self, forKey: .method) ?? "",
                tags: try values.decodeIfPresent(String.self, forKey: .tags) ?? "",
                ingredients: try values.decodeIfPresent([SharedRecipeIngredient].self, forKey: .ingredients) ?? [],
                steps: try values.decodeIfPresent([SharedRecipeStep].self, forKey: .steps) ?? []
            )
            technique = nil
        } else {
            kind = "technique"; recipe = nil
            technique = SharedTechniqueSnapshot(
                name: name,
                methodName: try values.decodeIfPresent(String.self, forKey: .methodName) ?? values.decodeIfPresent(String.self, forKey: .method_name) ?? values.decodeIfPresent(String.self, forKey: .method) ?? "",
                doseGrams: try values.decodeIfPresent(Double.self, forKey: .doseG) ?? values.decodeIfPresent(Double.self, forKey: .dose_g) ?? values.decodeIfPresent(Double.self, forKey: .coffeeGrams) ?? values.decodeIfPresent(Double.self, forKey: .coffee_grams) ?? 15,
                waterMl: try values.decodeIfPresent(Int.self, forKey: .waterMl) ?? values.decodeIfPresent(Int.self, forKey: .water_ml) ?? 240,
                ratio: try values.decodeIfPresent(Double.self, forKey: .ratio) ?? 16,
                temperatureC: try values.decodeIfPresent(Int.self, forKey: .temperatureC) ?? values.decodeIfPresent(Int.self, forKey: .temperature_c) ?? values.decodeIfPresent(Int.self, forKey: .temperature) ?? 93,
                executionMode: try values.decodeIfPresent(String.self, forKey: .executionMode) ?? values.decodeIfPresent(String.self, forKey: .execution_mode) ?? "GUIDED",
                grindValue: try values.decodeIfPresent(Double.self, forKey: .grindValue) ?? values.decodeIfPresent(Double.self, forKey: .grind_value) ?? 0,
                grindDescription: try values.decodeIfPresent(String.self, forKey: .grindDescription) ?? values.decodeIfPresent(String.self, forKey: .grind_description) ?? values.decodeIfPresent(String.self, forKey: .grindClicks) ?? values.decodeIfPresent(String.self, forKey: .grind_clicks) ?? "",
                grindUnit: try values.decodeIfPresent(String.self, forKey: .grindUnit) ?? values.decodeIfPresent(String.self, forKey: .grind_unit) ?? "CLICKS",
                notes: try values.decodeIfPresent(String.self, forKey: .notes) ?? "",
                techniqueDescription: try values.decodeIfPresent(String.self, forKey: .techniqueDescription) ?? values.decodeIfPresent(String.self, forKey: .description) ?? "",
                steps: try values.decodeIfPresent([SharedTechniqueStep].self, forKey: .steps) ?? []
            )
        }
    }

    func androidJSONObject() throws -> Any {
        if let recipe {
            return try JSONSerialization.jsonObject(with: JSONEncoder().encode(recipe))
        }
        guard let technique else { throw AuthServiceError.invalidResponse }
        var value = try JSONSerialization.jsonObject(with: JSONEncoder().encode(technique)) as? [String: Any] ?? [:]
        value["doseG"] = technique.doseGrams; value["coffeeGrams"] = technique.doseGrams
        value["temperature"] = technique.temperatureC; value["grindClicks"] = technique.grindDescription
        value["method"] = technique.methodName
        value["steps"] = technique.steps.enumerated().map { index, step in
            ["step_order": index + 1, "title": step.title, "duration_sec": step.durationSeconds, "water_add_ml": step.waterAddedMl,
             "target_water_ml": 0, "gesture": step.gesture, "intensity": step.intensity, "note": step.note] as [String: Any]
        }
        return value
    }
}

extension SharedTechniqueStep {
    private enum Keys: String, CodingKey { case title, durationSeconds, duration_sec, waterAddedMl, water_add_ml, intensity, gesture, note, stepNote, step_note }
    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: Keys.self)
        title = try values.decodeIfPresent(String.self, forKey: .title) ?? "Paso"
        durationSeconds = try values.decodeIfPresent(Int.self, forKey: .durationSeconds) ?? values.decodeIfPresent(Int.self, forKey: .duration_sec) ?? 0
        waterAddedMl = try values.decodeIfPresent(Int.self, forKey: .waterAddedMl) ?? values.decodeIfPresent(Int.self, forKey: .water_add_ml) ?? 0
        intensity = try values.decodeIfPresent(String.self, forKey: .intensity) ?? "MEDIUM"
        gesture = try values.decodeIfPresent(String.self, forKey: .gesture) ?? "CIRCULAR_POUR"
        note = try values.decodeIfPresent(String.self, forKey: .note) ?? values.decodeIfPresent(String.self, forKey: .stepNote) ?? values.decodeIfPresent(String.self, forKey: .step_note) ?? ""
    }
}

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
    static let messageLimit = 280
    private static let blockedPhrases = [
        "pornografia", "pornography", "violacion", "rape", "nazi", "terrorista", "terrorist",
        "matarte", "kill yourself", "suicidate", "suicide", "odio racial", "racial hate"
    ]

    static func validate(fromName: String, fromHandle: String, name: String, subtitle: String, message: String, payload: SharePayloadSnapshot) throws {
        guard !fromName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              !fromHandle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { throw SocialValidationError.emptyIdentity }
        guard fromName.count <= 80, fromHandle.count <= 40, name.count <= 160, subtitle.count <= 300, message.count <= messageLimit else { throw SocialValidationError.tooLong }
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
    let payloadSnapshot: SharePayloadSnapshot; let originalAuthorUserId: UUID?; let originalAuthorName: String?; let originalEntityId: UUID?
    let status: String; let createdAt: String; let updatedAt: String
    enum CodingKeys: String, CodingKey {
        case id, visibility, name, subtitle, message, status
        case ownerId = "from_user_id", entityType = "entity_type", entityId = "entity_id", fromName = "from_name", fromHandle = "from_handle"
        case targetUserId = "target_user_id", payloadSnapshot = "payload_snapshot_json"
        case originalAuthorUserId = "original_author_user_id", originalAuthorName = "original_author_name", originalEntityId = "original_entity_id"
        case createdAt = "created_at", updatedAt = "updated_at"
    }

    init(id: UUID, ownerId: UUID, entityType: String, entityId: UUID, fromName: String, fromHandle: String,
         targetUserId: UUID?, visibility: String, name: String, subtitle: String, message: String,
         payloadSnapshot: SharePayloadSnapshot, originalAuthorUserId: UUID? = nil, originalAuthorName: String? = nil,
         originalEntityId: UUID?, status: String, createdAt: String, updatedAt: String) {
        self.id = id; self.ownerId = ownerId; self.entityType = entityType; self.entityId = entityId
        self.fromName = fromName; self.fromHandle = fromHandle; self.targetUserId = targetUserId; self.visibility = visibility
        self.name = name; self.subtitle = subtitle; self.message = message; self.payloadSnapshot = payloadSnapshot
        self.originalAuthorUserId = originalAuthorUserId; self.originalAuthorName = originalAuthorName; self.originalEntityId = originalEntityId
        self.status = status; self.createdAt = createdAt; self.updatedAt = updatedAt
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

enum SocialReportReason: String, CaseIterable, Identifiable {
    case harassment = "HARASSMENT"
    case hate = "HATE_OR_VIOLENCE"
    case sexual = "SEXUAL_CONTENT"
    case spam = "SPAM_OR_FRAUD"
    case dangerous = "DANGEROUS_CONTENT"
    case other = "OTHER"

    var id: Self { self }
    var label: String {
        switch self {
        case .harassment: "Acoso o intimidación"
        case .hate: "Odio o violencia"
        case .sexual: "Contenido sexual"
        case .spam: "Spam o fraude"
        case .dangerous: "Contenido peligroso"
        case .other: "Otro motivo"
        }
    }
}

enum SocialReportValidationError: LocalizedError, Equatable {
    case detailsTooLong
    var errorDescription: String? { "Los detalles del reporte deben tener 450 caracteres o menos." }
}

struct SocialService {
    let configuration: AppConfiguration; let transport: NetworkTransport
    init(configuration: AppConfiguration = AppConfiguration(), transport: NetworkTransport = URLSessionTransport()) { self.configuration = configuration; self.transport = transport }

    func feed(accessToken: String) async throws -> [SocialShare] {
        let (data, _) = try await request(path: "rest/v1/shares", query: "select=*&visibility=eq.public&status=eq.active&order=created_at.desc&limit=50", method: "GET", body: nil, accessToken: accessToken, prefer: nil)
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
    func blockedUserIds(userId: UUID, accessToken: String) async throws -> Set<UUID> {
        struct Reference: Decodable {
            let blockedUserId: UUID
            enum CodingKeys: String, CodingKey { case blockedUserId = "blocked_user_id" }
        }
        let query = "select=blocked_user_id&blocker_id=eq.\(userId.uuidString)"
        let (data, _) = try await request(path: "rest/v1/blocked_users", query: query, method: "GET", body: nil, accessToken: accessToken, prefer: nil)
        return Set(try JSONDecoder().decode([Reference].self, from: data).map(\.blockedUserId))
    }
    func inbox(userId: UUID, accessToken: String) async throws -> [SocialInboxItem] {
        let query = "select=id,share_id,target_user_id,read_at,created_at,share:shares(*)&target_user_id=eq.\(userId.uuidString)&order=created_at.desc&limit=50"
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
    func publish(entityType: String, entityId: UUID, fromName: String, fromHandle: String, name: String, subtitle: String, message: String, payload: SharePayloadSnapshot, visibility: String = "PUBLIC", targetUserId: UUID? = nil, originalAuthorUserId: UUID? = nil, originalAuthorName: String? = nil, originalEntityId: UUID? = nil, accessToken: String) async throws {
        try SocialContentPolicy.validate(fromName: fromName, fromHandle: fromHandle, name: name, subtitle: subtitle, message: message, payload: payload)
        guard visibility == "PUBLIC" || (visibility == "DIRECT" && targetUserId != nil) else { throw AuthServiceError.invalidResponse }
        let remoteVisibility = visibility == "DIRECT" ? "direct" : "public"
        var body: [String: Any] = ["entity_type": entityType, "entity_id": entityId.uuidString, "from_name": fromName, "from_handle": fromHandle, "visibility": remoteVisibility, "name": name, "subtitle": subtitle, "message": message, "payload_snapshot_json": try payload.androidJSONObject(), "original_entity_id": (originalEntityId ?? entityId).uuidString]
        if let targetUserId { body["target_user_id"] = targetUserId.uuidString }
        if let originalAuthorUserId { body["original_author_user_id"] = originalAuthorUserId.uuidString }
        if let originalAuthorName, !originalAuthorName.isEmpty { body["original_author_name"] = originalAuthorName }
        _ = try await request(path: "rest/v1/shares", query: nil, method: "POST", body: try JSONSerialization.data(withJSONObject: body), accessToken: accessToken, prefer: "return=minimal")
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
    func report(shareId: UUID, reason: SocialReportReason, details: String = "", accessToken: String) async throws {
        let cleanDetails = details.trimmingCharacters(in: .whitespacesAndNewlines)
        guard cleanDetails.count <= 450 else { throw SocialReportValidationError.detailsTooLong }
        let value = cleanDetails.isEmpty ? reason.rawValue : "\(reason.rawValue): \(cleanDetails)"
        _ = try await request(path: "rest/v1/content_reports", query: nil, method: "POST", body: try JSONSerialization.data(withJSONObject: ["share_id": shareId.uuidString, "reason": value]), accessToken: accessToken, prefer: "return=minimal")
    }
    func block(userId: UUID, accessToken: String) async throws {
        _ = try await request(path: "rest/v1/blocked_users", query: nil, method: "POST", body: try JSONSerialization.data(withJSONObject: ["blocked_user_id": userId.uuidString]), accessToken: accessToken, prefer: "resolution=ignore-duplicates,return=minimal")
    }
    func unblock(userId: UUID, accessToken: String) async throws {
        _ = try await request(path: "rest/v1/blocked_users", query: "blocked_user_id=eq.\(userId.uuidString)", method: "DELETE", body: nil, accessToken: accessToken, prefer: "return=minimal")
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
            let record = try repository.saveRecipe(draft)
            record.isShared = true; record.originalAuthorUserId = share.originalAuthorUserId ?? share.ownerId
            record.originalAuthorName = share.originalAuthorName ?? share.fromName; record.originalEntityId = share.originalEntityId ?? share.entityId
            record.rootEntityId = share.originalEntityId ?? share.entityId; record.importedFromShareId = share.id; record.copyMode = mode.rawValue
            record.markUpdated(); try context.save()
        } else if let value = share.payloadSnapshot.technique {
            let draft = TechniqueDraftModel(
                id: UUID(), name: mode == .forked ? "\(value.name) (Variante)" : "Copia de \(value.name)", methodName: value.methodName, doseGrams: value.doseGrams,
                waterMl: value.waterMl, ratio: value.ratio, temperatureC: value.temperatureC, executionMode: value.executionMode,
                grindValue: value.grindValue, grindDescription: value.grindDescription, grindUnit: value.grindUnit,
                notes: value.notes, techniqueDescription: value.techniqueDescription,
                steps: value.steps.map { .init(title: $0.title, durationSeconds: $0.durationSeconds, waterAddedMl: $0.waterAddedMl, intensity: $0.intensity, gesture: $0.gesture, note: $0.note) }
            )
            let record = try repository.saveTechnique(draft)
            record.isShared = true; record.originalAuthorUserId = share.originalAuthorUserId ?? share.ownerId
            record.originalAuthorName = share.originalAuthorName ?? share.fromName; record.originalEntityId = share.originalEntityId ?? share.entityId
            record.rootEntityId = share.originalEntityId ?? share.entityId; record.importedFromShareId = share.id; record.copyMode = mode.rawValue
            record.markUpdated(); try context.save()
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
