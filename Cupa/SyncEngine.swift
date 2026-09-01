import CoreData
import Foundation

struct SyncVersion: Equatable {
    let ownerId: UUID; let updatedAt: Date; let version: Int64; let deletedAt: Date?
}

enum ConflictChoice: Equatable { case local, remote, identical, ownerMismatch }

enum LastWriteWinsResolver {
    static func resolve(local: SyncVersion, remote: SyncVersion) -> ConflictChoice {
        guard local.ownerId == remote.ownerId else { return .ownerMismatch }
        if local.updatedAt > remote.updatedAt { return .local }
        if remote.updatedAt > local.updatedAt { return .remote }
        if local.version > remote.version { return .local }
        if remote.version > local.version { return .remote }
        return .identical
    }
}

@objc(SyncOperationRecord)
final class SyncOperationRecord: NSManagedObject, SyncTrackedRecord {
    @NSManaged var id: UUID; @NSManaged var ownerId: UUID?; @NSManaged var entityName: String; @NSManaged var entityId: UUID
    @NSManaged var operation: String; @NSManaged var payloadJSON: String; @NSManaged var attemptCount: Int64
    @NSManaged var nextAttemptAt: Date; @NSManaged var lastError: String; @NSManaged var createdAt: Date; @NSManaged var updatedAt: Date
    @NSManaged var version: Int64; @NSManaged var syncStatusRaw: String; @NSManaged var deletedAt: Date?
    func markUpdated() { trackUpdate() }; func markDeleted() { trackDeletion() }
}
extension SyncOperationRecord: Identifiable {}

@MainActor
struct SyncOutboxRepository {
    let context: NSManagedObjectContext

    @discardableResult func enqueue(entityName: String, entityId: UUID, ownerId: UUID?, operation: SyncStatus, payloadJSON: String) throws -> SyncOperationRecord {
        let request = NSFetchRequest<SyncOperationRecord>(entityName: "SyncOperationRecord")
        if let ownerId { request.predicate = NSPredicate(format: "entityName == %@ AND entityId == %@ AND ownerId == %@ AND deletedAt == nil", entityName, entityId as CVarArg, ownerId as CVarArg) }
        else { request.predicate = NSPredicate(format: "entityName == %@ AND entityId == %@ AND ownerId == nil AND deletedAt == nil", entityName, entityId as CVarArg) }
        let item = try context.fetch(request).first ?? SyncOperationRecord(context: context)
        if item.value(forKey: "createdAt") == nil {
            item.id = UUID(); item.ownerId = ownerId; item.entityName = entityName; item.entityId = entityId
            item.createdAt = .now; item.version = 1; item.attemptCount = 0; item.syncStatusRaw = SyncStatus.pendingCreate.rawValue; item.deletedAt = nil
        } else { item.markUpdated() }
        item.operation = operation.rawValue; item.payloadJSON = payloadJSON; item.nextAttemptAt = .now; item.lastError = ""; item.updatedAt = .now
        try context.save(); return item
    }

    func ready(ownerId: UUID? = nil, now: Date = .now, limit: Int = 100) throws -> [SyncOperationRecord] {
        let request = NSFetchRequest<SyncOperationRecord>(entityName: "SyncOperationRecord")
        if let ownerId { request.predicate = NSPredicate(format: "ownerId == %@ AND deletedAt == nil AND nextAttemptAt <= %@", ownerId as CVarArg, now as NSDate) }
        else { request.predicate = NSPredicate(format: "deletedAt == nil AND nextAttemptAt <= %@", now as NSDate) }
        request.sortDescriptors = [NSSortDescriptor(key: "createdAt", ascending: true)]; request.fetchLimit = limit
        return try context.fetch(request)
    }

    func markSucceeded(_ item: SyncOperationRecord) throws { item.markDeleted(); item.syncStatusRaw = SyncStatus.synced.rawValue; try context.save() }
    func markFailed(_ item: SyncOperationRecord, message: String, now: Date = .now) throws {
        item.attemptCount += 1; item.lastError = message
        let seconds = min(3600.0, pow(2.0, Double(item.attemptCount)) * 5.0)
        item.nextAttemptAt = now.addingTimeInterval(seconds); item.updatedAt = now; item.syncStatusRaw = SyncStatus.error.rawValue
        try context.save()
    }
}

struct SupabaseDataService {
    let configuration: AppConfiguration; let transport: NetworkTransport
    init(configuration: AppConfiguration = AppConfiguration(), transport: NetworkTransport = URLSessionTransport()) { self.configuration = configuration; self.transport = transport }

    func upsert(table: String, json: Data, accessToken: String) async throws {
        _ = try await request(table: table, query: nil, method: "POST", body: json, accessToken: accessToken, prefer: "resolution=merge-duplicates,return=minimal")
    }
    func delete(table: String, id: UUID, updatedAt: Date, accessToken: String) async throws {
        let iso = ISO8601DateFormatter().string(from: updatedAt)
        let body = try JSONSerialization.data(withJSONObject: ["deleted_at": iso, "updated_at": iso])
        _ = try await request(table: table, query: "id=eq.\(id.uuidString)", method: "PATCH", body: body, accessToken: accessToken, prefer: "return=minimal")
    }
    func changes(table: String, since: Date, accessToken: String) async throws -> Data {
        let iso = ISO8601DateFormatter().string(from: since).addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        return try await request(table: table, query: "updated_at=gte.\(iso)&order=updated_at.asc", method: "GET", body: nil, accessToken: accessToken, prefer: nil).0
    }

    private func request(table: String, query: String?, method: String, body: Data?, accessToken: String, prefer: String?) async throws -> (Data, HTTPURLResponse) {
        guard let base = configuration.supabaseURL, let key = configuration.supabaseAnonKey, !key.isEmpty else { throw AuthServiceError.notConfigured }
        var components = URLComponents(url: base.appendingPathComponent("rest/v1/\(table)"), resolvingAgainstBaseURL: false); components?.percentEncodedQuery = query
        guard let url = components?.url else { throw URLError(.badURL) }
        var request = URLRequest(url: url); request.httpMethod = method; request.httpBody = body; request.timeoutInterval = 30
        request.setValue(key, forHTTPHeaderField: "apikey"); request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization"); request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let prefer { request.setValue(prefer, forHTTPHeaderField: "Prefer") }
        let result = try await transport.data(for: request)
        guard (200..<300).contains(result.1.statusCode) else { throw AuthServiceError.server(result.1.statusCode, "No se pudo sincronizar \(table).") }
        return result
    }
}
