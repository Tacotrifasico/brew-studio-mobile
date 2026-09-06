import CoreData
import Foundation

enum RemoteFieldKind { case string, localUppercaseString, integer, double, boolean, uuid, date, dateOnly, json }
struct RemoteField { let local: String; let remote: String; let kind: RemoteFieldKind }
struct SyncEntityDescriptor {
    let entityName: String
    let table: String
    let fields: [RemoteField]
    var profileIdentity = false
    var ownerField = "owner_id"
}

enum CoreSyncSchema {
    static let descriptors: [SyncEntityDescriptor] = [
        .init(entityName: "CoffeeBeanRecord", table: "beans", fields: [f("name"),f("brand","roaster"),f("origin"),f("producer"),f("variety"),f("process"),f("altitudeMetersValue","altitude_meters",.integer),f("roastLevel","roast_level"),f("roastDate","roast_date",.dateOnly),f("openedDate","first_use_date",.dateOnly),f("initialQuantityGrams","initial_quantity_grams",.double),f("remainingQuantityGrams","stock_grams",.double),f("notes")], ownerField: "user_id"),
        .init(entityName: "GrinderRecord", table: "grinders", fields: [f("name"),f("brand"),f("model"),f("grinderType","grinder_type"),f("scaleUnit","scale_unit"),f("minimumSetting","minimum_setting",.integer),f("maximumSetting","maximum_setting",.integer),f("calibrationNotes","calibration_notes"),f("notes")], ownerField: "user_id"),
        .init(entityName: "EquipmentRecord", table: "equipment", fields: [f("equipmentType","equipment_type"),f("name"),f("brand"),f("model"),f("capacityMlValue","capacity_ml",.integer),f("configuration"),f("notes"),f("isFavorite","is_favorite",.boolean),f("isActive","is_active",.boolean)], ownerField: "user_id"),
        .init(entityName: "RecipeRecord", table: "recipes", fields: [f("name"),f("recipeKind","recipe_kind"),f("intention"),f("suggestedMethodId","suggested_method_id",.uuid),f("suggestedMethodName","suggested_method_name"),f("isFavorite","is_favorite",.boolean),f("tags"),f("visibility",kind:.localUppercaseString),f("isShared","is_shared",.boolean),f("originalAuthorUserId","original_author_user_id",.uuid),f("originalAuthorName","original_author_name"),f("originalEntityId","original_entity_id",.uuid),f("rootEntityId","root_entity_id",.uuid),f("importedFromShareId","imported_from_share_id",.uuid),f("copyMode","copy_mode",.localUppercaseString)], ownerField: "user_id"),
        .init(entityName: "RecipeIngredientRecord", table: "recipe_ingredients", fields: [f("recipeId","recipe_id",.uuid),f("name"),f("amount",kind:.double),f("unit"),f("orderIndex","order_index",.integer)]),
        .init(entityName: "RecipeStepRecord", table: "recipe_steps", fields: [f("recipeId","recipe_id",.uuid),f("instruction"),f("stepNumber","step_number",.integer),f("durationSecondsValue","duration_seconds",.integer)]),
        .init(entityName: "TechniqueRecord", table: "techniques", fields: [f("name"),f("methodId","method_id",.uuid),f("methodName","method_name"),f("recipeId","recipe_id",.uuid),f("beanId","bean_id",.uuid),f("grinderId","grinder_id",.uuid),f("doseGrams","dose_grams",.double),f("waterMl","water_ml",.integer),f("ratio",kind:.double),f("temperatureC","temperature_c",.integer),f("executionMode","execution_mode"),f("grindValue","grind_value",.double),f("grindDescription","grind_description"),f("grindUnit","grind_unit"),f("notes"),f("techniqueDescription","description"),f("totalTimeSeconds","total_time_seconds",.integer),f("visibility",kind:.localUppercaseString),f("isShared","is_shared",.boolean),f("originalAuthorUserId","original_author_user_id",.uuid),f("originalAuthorName","original_author_name"),f("originalEntityId","original_entity_id",.uuid),f("rootEntityId","root_entity_id",.uuid),f("importedFromShareId","imported_from_share_id",.uuid),f("copyMode","copy_mode",.localUppercaseString)], ownerField: "user_id"),
        .init(entityName: "TechniqueStepRecord", table: "technique_steps", fields: [f("techniqueId","technique_id",.uuid),f("stepNumber","step_number",.integer),f("title"),f("durationSeconds","duration_seconds",.integer),f("waterAddedMl","water_added_ml",.integer),f("waterAccumulatedMl","water_accumulated_ml",.integer),f("intensity"),f("gesture"),f("stepNote","step_note"),f("coverageValue","coverage",.double),f("flowValue","flow",.double),f("secondaryAction","secondary_action")], ownerField: "user_id"),
        .init(entityName: "BrewSessionRecord", table: "brew_sessions", fields: [f("techniqueId","technique_id",.uuid),f("recipeId","recipe_id",.uuid),f("methodId","method_id",.uuid),f("beanId","bean_id",.uuid),f("grinderId","grinder_id",.uuid),f("techniqueNameSnapshot","technique_name_snapshot"),f("recipeNameSnapshot","recipe_name_snapshot"),f("methodNameSnapshot","method_name_snapshot"),f("beanNameSnapshot","bean_name_snapshot"),f("grinderNameSnapshot","grinder_name_snapshot"),f("doseGrams","dose_grams",.double),f("waterMl","water_ml",.integer),f("ratio",kind:.double),f("temperatureC","temperature_c",.integer),f("grindDescription","grind_description"),f("elapsedSeconds","elapsed_seconds",.integer),f("completedAt","completed_at",.date),f("stepsSnapshotJSON","steps_snapshot",.json)]),
        .init(entityName: "TastingRecord", table: "tastings", fields: [f("brewSessionId","brew_session_id",.uuid),f("recipeId","recipe_id",.uuid),f("techniqueId","technique_id",.uuid),f("beanId","bean_id",.uuid),f("activeFlavorFamily","active_flavor_family"),f("selectedFlavorNotesJSON","selected_flavor_notes",.json),f("expectedNotes","expected_notes"),f("texture"),f("cleanliness"),f("persistence"),f("aroma",kind:.double),f("acidity",kind:.double),f("sweetness",kind:.double),f("body",kind:.double),f("bitterness",kind:.double),f("finishScore","finish",.double),f("rating",kind:.double),f("nps",kind:.integer),f("evaluatorNotes","evaluator_notes"),f("coolingElapsedSeconds","cooling_elapsed_seconds",.integer),f("cupLifeState","cup_life_state"),f("evaluatedAt","evaluated_at",.date)]),
        .init(entityName: "TastingObservationRecord", table: "tasting_observations", fields: [f("tastingId","tasting_id",.uuid),f("elapsedSeconds","elapsed_seconds",.integer),f("stage"),f("notes"),f("aroma",kind:.double),f("acidity",kind:.double),f("sweetness",kind:.double),f("body",kind:.double),f("bitterness",kind:.double),f("finishScore","finish",.double)]),
        .init(entityName: "CupSessionRecord", table: "cup_sessions", fields: [f("brewSessionId","brew_session_id",.uuid),f("tastingId","tasting_id",.uuid),f("recipeId","recipe_id",.uuid),f("beanId","bean_id",.uuid),f("techniqueId","technique_id",.uuid),f("methodId","method_id",.uuid),f("grinderId","grinder_id",.uuid),f("executedDoseGrams","executed_dose_g",.double),f("executedWaterMl","executed_water_ml",.integer),f("executedRatio","executed_ratio",.double),f("executedTemperatureC","executed_temperature_c",.integer),f("executedGrindSetting","executed_grind_setting"),f("executedDurationSeconds","executed_duration_seconds",.integer),f("beanNameSnapshot","bean_name_snapshot"),f("recipeNameSnapshot","recipe_name_snapshot"),f("techniqueNameSnapshot","technique_name_snapshot"),f("methodNameSnapshot","method_name_snapshot"),f("grinderNameSnapshot","grinder_name_snapshot"),f("cupLifeSeconds","cup_life_seconds",.integer),f("cupLifeState","cup_life_state"),f("nps",kind:.integer),f("rating",kind:.double),f("comment"),f("brewDate","brew_date",.date),f("recipeSnapshotJSON","recipe_snapshot",.json),f("techniqueSnapshotJSON","technique_snapshot",.json),f("beanSnapshotJSON","bean_snapshot",.json),f("grinderSnapshotJSON","grinder_snapshot",.json)]),
        .init(entityName: "LabExperimentRecord", table: "lab_experiments", fields: [f("methodId","method_id",.uuid),f("recipeId","recipe_id",.uuid),f("techniqueId","technique_id",.uuid),f("beanId","bean_id",.uuid),f("grinderId","grinder_id",.uuid),f("method"),f("coffeeGrams","coffee_grams",.double),f("waterMl","water_ml",.integer),f("ratio",kind:.double),f("temperatureC","temperature_c",.integer),f("grindClicks","grind_clicks",.integer),f("freshness"),f("timeSeconds","time_seconds",.integer),f("altitudeMeters","altitude_meters",.integer),f("cityName","city_name"),f("notes"),f("extractionIndex","extraction_index",.double),f("summary")], ownerField: "user_id"),
        .init(entityName: "UserProfileRecord", table: "profiles", fields: [f("displayName","display_name"),f("alias","handle"),f("biography"),f("avatarColor","avatar_color"),f("favoriteMethods","favorite_methods"),f("isPrivate","is_private",.boolean)], profileIdentity: true)
    ]
    private static func f(_ local: String, _ remote: String? = nil, _ kind: RemoteFieldKind = .string) -> RemoteField { .init(local: local, remote: remote ?? snake(local), kind: kind) }
    private static func f(_ local: String, kind: RemoteFieldKind) -> RemoteField { f(local, nil, kind) }
    private static func snake(_ value: String) -> String { value.reduce(into: "") { result, char in if char.isUppercase { result.append("_"); result.append(char.lowercased()) } else { result.append(char) } } }
}

@MainActor
final class EntitySyncCoordinator: ObservableObject {
    enum State: Equatable { case idle, syncing, offline, completed(Date), failed(String) }
    @Published private(set) var state: State = .idle
    @Published private(set) var authenticationRejected = false
    private let context: NSManagedObjectContext; private let configuration: AppConfiguration; private let transport: NetworkTransport
    private let defaults: UserDefaults; private let now: () -> Date
    private let checkpointKeyPrefix = "sync.lastSuccessfulAt.v2"; private let fallbackOverlap: TimeInterval = 300
    init(context: NSManagedObjectContext, configuration: AppConfiguration = AppConfiguration(), transport: NetworkTransport = URLSessionTransport(), defaults: UserDefaults = .standard, now: @escaping () -> Date = Date.init) {
        self.context = context; self.configuration = configuration; self.transport = transport; self.defaults = defaults; self.now = now
    }

    func sync(ownerId: UUID, accessToken: String) async {
        guard configuration.isSupabaseConfigured else { state = .offline; return }; state = .syncing; authenticationRejected = false
        let startedAt = now()
        do {
            try enqueuePending(ownerId: ownerId)
            try await push(ownerId: ownerId, accessToken: accessToken)
            let safeCheckpoint = try await pull(ownerId: ownerId, accessToken: accessToken, fallbackCheckpoint: startedAt.addingTimeInterval(-fallbackOverlap))
            defaults.set(safeCheckpoint, forKey: checkpointKey(ownerId)); state = .completed(now())
        } catch {
            authenticationRejected = RemoteFailureClassifier.isUnauthorized(error)
            state = RemoteFailureClassifier.isOffline(error) ? .offline : .failed(error.localizedDescription)
        }
    }

    func enqueuePending(ownerId: UUID) throws {
        let outbox = SyncOutboxRepository(context: context)
        for descriptor in CoreSyncSchema.descriptors {
            let request = NSFetchRequest<NSManagedObject>(entityName: descriptor.entityName)
            request.predicate = NSPredicate(format: "(ownerId == nil OR ownerId == %@) AND syncStatusRaw != %@", ownerId as CVarArg, SyncStatus.synced.rawValue)
            for record in try context.fetch(request) {
                if descriptor.profileIdentity { record.setValue(ownerId, forKey: "id") }
                if record.value(forKey: "ownerId") == nil { record.setValue(ownerId, forKey: "ownerId") }
                guard record.value(forKey: "ownerId") as? UUID == ownerId, let id = record.value(forKey: "id") as? UUID else { continue }
                let payload = try encode(record, descriptor: descriptor, ownerId: ownerId)
                let operation = SyncStatus(rawValue: record.value(forKey: "syncStatusRaw") as? String ?? "") ?? .pendingUpdate
                try outbox.enqueue(entityName: descriptor.table, entityId: id, ownerId: ownerId, operation: operation, payloadJSON: String(data: payload, encoding: .utf8) ?? "[]")
            }
        }
    }

    private func push(ownerId: UUID, accessToken: String) async throws {
        let outbox = SyncOutboxRepository(context: context); let service = SupabaseDataService(configuration: configuration, transport: transport)
        for item in try outbox.ready(ownerId: ownerId) {
            guard let data = item.payloadJSON.data(using: .utf8) else { continue }
            do {
                try await service.upsert(table: item.entityName, json: data, accessToken: accessToken)
                if let descriptor = CoreSyncSchema.descriptors.first(where: { $0.table == item.entityName }), let local = try localObject(descriptor.entityName, id: item.entityId, expectedOwner: ownerId) { local.setValue(SyncStatus.synced.rawValue, forKey: "syncStatusRaw") }
                try outbox.markSucceeded(item)
            } catch { try outbox.markFailed(item, message: error.localizedDescription); throw error }
        }
    }

    private func pull(ownerId: UUID, accessToken: String, fallbackCheckpoint: Date) async throws -> Date {
        let service = SupabaseDataService(configuration: configuration, transport: transport)
        let since = defaults.object(forKey: checkpointKey(ownerId)) as? Date ?? Date(timeIntervalSince1970: 0)
        var safeCheckpoint = fallbackCheckpoint
        for (index, descriptor) in CoreSyncSchema.descriptors.enumerated() {
            let batch = try await service.changes(table: descriptor.table, since: since, accessToken: accessToken)
            if index == 0, let serverDate = batch.serverDate { safeCheckpoint = serverDate.addingTimeInterval(-fallbackOverlap) }
            let rows = (try JSONSerialization.jsonObject(with: batch.data)) as? [[String: Any]] ?? []
            for row in rows { try merge(row, descriptor: descriptor, expectedOwner: ownerId) }
        }
        if context.hasChanges { try context.save() }
        return safeCheckpoint
    }
    private func checkpointKey(_ ownerId: UUID) -> String { "\(checkpointKeyPrefix).\(ownerId.uuidString.lowercased())" }

    func encode(_ record: NSManagedObject, descriptor: SyncEntityDescriptor, ownerId: UUID) throws -> Data {
        guard let id = record.value(forKey: "id") as? UUID else { throw AuthServiceError.invalidResponse }
        var row: [String: Any] = ["id": id.uuidString]
        if !descriptor.profileIdentity { row[descriptor.ownerField] = ownerId.uuidString }
        for field in descriptor.fields { row[field.remote] = remoteValue(record.value(forKey: field.local), kind: field.kind) }
        row["created_at"] = remoteValue(record.value(forKey: "createdAt"), kind: .date)
        row["updated_at"] = remoteValue(record.value(forKey: "updatedAt"), kind: .date)
        row["version"] = record.value(forKey: "version") ?? 1
        row["deleted_at"] = remoteValue(record.value(forKey: "deletedAt"), kind: .date)
        return try JSONSerialization.data(withJSONObject: [row])
    }

    func merge(_ row: [String: Any], descriptor: SyncEntityDescriptor, expectedOwner: UUID) throws {
        guard let idText = row["id"] as? String, let id = UUID(uuidString: idText) else { throw AuthServiceError.invalidResponse }
        let remoteOwner = descriptor.profileIdentity ? id : (row[descriptor.ownerField] as? String).flatMap(UUID.init(uuidString:))
        guard remoteOwner == expectedOwner else { return }
        let remoteUpdated = parseDate(row["updated_at"]) ?? .distantPast; let remoteVersion = (row["version"] as? NSNumber)?.int64Value ?? 1
        let object = try localObject(descriptor.entityName, id: id) ?? NSEntityDescription.insertNewObject(forEntityName: descriptor.entityName, into: context)
        if let localOwner = object.value(forKey: "ownerId") as? UUID, localOwner != expectedOwner { return }
        if let localUpdated = object.value(forKey: "updatedAt") as? Date {
            let localVersion = (object.value(forKey: "version") as? NSNumber)?.int64Value ?? 1
            let choice = LastWriteWinsResolver.resolve(local: .init(ownerId: expectedOwner, updatedAt: localUpdated, version: localVersion, deletedAt: object.value(forKey: "deletedAt") as? Date), remote: .init(ownerId: expectedOwner, updatedAt: remoteUpdated, version: remoteVersion, deletedAt: parseDate(row["deleted_at"])))
            if choice == .local { return }
        }
        object.setValue(id, forKey: "id"); object.setValue(expectedOwner, forKey: "ownerId")
        for field in descriptor.fields { object.setValue(localValue(row[field.remote], kind: field.kind), forKey: field.local) }
        object.setValue(parseDate(row["created_at"]) ?? remoteUpdated, forKey: "createdAt"); object.setValue(remoteUpdated, forKey: "updatedAt")
        object.setValue(remoteVersion, forKey: "version"); object.setValue(parseDate(row["deleted_at"]), forKey: "deletedAt"); object.setValue(SyncStatus.synced.rawValue, forKey: "syncStatusRaw")
    }

    private func localObject(_ entity: String, id: UUID, expectedOwner: UUID? = nil) throws -> NSManagedObject? {
        let request = NSFetchRequest<NSManagedObject>(entityName: entity)
        if let expectedOwner { request.predicate = NSPredicate(format: "id == %@ AND (ownerId == nil OR ownerId == %@)", id as CVarArg, expectedOwner as CVarArg) }
        else { request.predicate = NSPredicate(format: "id == %@", id as CVarArg) }
        request.fetchLimit = 1
        return try context.fetch(request).first
    }
    private func remoteValue(_ value: Any?, kind: RemoteFieldKind) -> Any {
        guard let value else { return NSNull() }
        switch kind {
        case .localUppercaseString: return (value as? String)?.lowercased() ?? NSNull()
        case .uuid: return (value as? UUID)?.uuidString ?? NSNull()
        case .date: return (value as? Date).map { ISO8601DateFormatter().string(from: $0) } ?? NSNull()
        case .dateOnly:
            guard let date = value as? Date else { return NSNull() }; let formatter = DateFormatter(); formatter.locale = Locale(identifier: "en_US_POSIX"); formatter.dateFormat = "yyyy-MM-dd"; return formatter.string(from: date)
        case .json:
            guard let text = value as? String, let data = text.data(using: .utf8), let object = try? JSONSerialization.jsonObject(with: data) else { return NSNull() }; return object
        default: return value
        }
    }
    private func localValue(_ value: Any?, kind: RemoteFieldKind) -> Any? {
        guard let value, !(value is NSNull) else { return nil }
        switch kind {
        case .localUppercaseString: return (value as? String)?.uppercased()
        case .uuid: return (value as? String).flatMap(UUID.init(uuidString:))
        case .date: return parseDate(value)
        case .dateOnly:
            let formatter = DateFormatter(); formatter.locale = Locale(identifier: "en_US_POSIX"); formatter.dateFormat = "yyyy-MM-dd"; return (value as? String).flatMap(formatter.date(from:))
        case .json: guard let data = try? JSONSerialization.data(withJSONObject: value) else { return "[]" }; return String(data: data, encoding: .utf8) ?? "[]"
        default: return value
        }
    }
    private func parseDate(_ value: Any?) -> Date? {
        guard let text = value as? String else { return value as? Date }
        let fractional = ISO8601DateFormatter(); fractional.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return fractional.date(from: text) ?? ISO8601DateFormatter().date(from: text)
    }
}
