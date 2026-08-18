import Foundation
import Security

struct AppConfiguration: Equatable {
    let supabaseURL: URL?; let supabaseAnonKey: String?
    var isSupabaseConfigured: Bool { supabaseURL != nil && !(supabaseAnonKey ?? "").isEmpty }

    init(bundle: Bundle = .main, environment: [String: String] = ProcessInfo.processInfo.environment) {
        let rawURL = environment["SUPABASE_URL"] ?? bundle.object(forInfoDictionaryKey: "SUPABASE_URL") as? String
        let rawKey = environment["SUPABASE_ANON_KEY"] ?? bundle.object(forInfoDictionaryKey: "SUPABASE_ANON_KEY") as? String
        supabaseURL = rawURL.flatMap(URL.init(string:)); supabaseAnonKey = rawKey?.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    init(supabaseURL: URL?, supabaseAnonKey: String?) { self.supabaseURL = supabaseURL; self.supabaseAnonKey = supabaseAnonKey }
}

struct AuthTokens: Codable, Equatable {
    let accessToken: String; let refreshToken: String; let expiresAt: Date; let userId: UUID; let email: String
    var needsRefresh: Bool { expiresAt.timeIntervalSinceNow < 60 }
}

protocol TokenStore {
    func load() throws -> AuthTokens?
    func save(_ tokens: AuthTokens) throws
    func clear() throws
}

enum KeychainError: Error { case unexpectedStatus(OSStatus) }

struct KeychainTokenStore: TokenStore {
    private let service = "com.tacotrifasico.cupa.auth"; private let account = "supabase-session"
    func load() throws -> AuthTokens? {
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: service, kSecAttrAccount as String: account, kSecReturnData as String: true, kSecMatchLimit as String: kSecMatchLimitOne]
        var result: AnyObject?; let status = SecItemCopyMatching(query as CFDictionary, &result)
        if status == errSecItemNotFound { return nil }; guard status == errSecSuccess else { throw KeychainError.unexpectedStatus(status) }
        return try JSONDecoder().decode(AuthTokens.self, from: result as? Data ?? Data())
    }
    func save(_ tokens: AuthTokens) throws {
        let data = try JSONEncoder().encode(tokens)
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: service, kSecAttrAccount as String: account]
        let attributes: [String: Any] = [kSecValueData as String: data, kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly]
        let status = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
        if status == errSecItemNotFound {
            var insert = query; attributes.forEach { insert[$0.key] = $0.value }
            let insertStatus = SecItemAdd(insert as CFDictionary, nil); guard insertStatus == errSecSuccess else { throw KeychainError.unexpectedStatus(insertStatus) }
        } else if status != errSecSuccess { throw KeychainError.unexpectedStatus(status) }
    }
    func clear() throws {
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: service, kSecAttrAccount as String: account]
        let status = SecItemDelete(query as CFDictionary); guard status == errSecSuccess || status == errSecItemNotFound else { throw KeychainError.unexpectedStatus(status) }
    }
}

protocol NetworkTransport {
    func data(for request: URLRequest) async throws -> (Data, HTTPURLResponse)
}

struct URLSessionTransport: NetworkTransport {
    func data(for request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else { throw URLError(.badServerResponse) }
        return (data, http)
    }
}

enum AuthServiceError: LocalizedError, Equatable {
    case notConfigured, invalidResponse, server(Int, String)
    var errorDescription: String? {
        switch self {
        case .notConfigured: "Supabase todavía no está configurado en este build."
        case .invalidResponse: "El servidor devolvió una respuesta inválida."
        case let .server(_, message): message
        }
    }
}

struct SupabaseAuthService {
    let configuration: AppConfiguration; let transport: NetworkTransport
    init(configuration: AppConfiguration = AppConfiguration(), transport: NetworkTransport = URLSessionTransport()) { self.configuration = configuration; self.transport = transport }

    func signIn(email: String, password: String) async throws -> AuthTokens { try await token(path: "/auth/v1/token?grant_type=password", payload: ["email": email, "password": password]) }
    func signUp(email: String, password: String) async throws -> AuthTokens { try await token(path: "/auth/v1/signup", payload: ["email": email, "password": password]) }
    func refresh(_ refreshToken: String) async throws -> AuthTokens { try await token(path: "/auth/v1/token?grant_type=refresh_token", payload: ["refresh_token": refreshToken]) }
    func sendPasswordRecovery(email: String) async throws { _ = try await request(path: "/auth/v1/recover", method: "POST", payload: ["email": email], bearer: nil) }
    func signOut(accessToken: String) async throws { _ = try await request(path: "/auth/v1/logout", method: "POST", payload: nil, bearer: accessToken) }

    private func token(path: String, payload: [String: String]) async throws -> AuthTokens {
        let (data, _) = try await request(path: path, method: "POST", payload: payload, bearer: nil)
        struct Response: Decodable {
            struct User: Decodable { let id: UUID; let email: String? }
            let access_token: String; let refresh_token: String; let expires_in: Double; let user: User
        }
        guard let decoded = try? JSONDecoder().decode(Response.self, from: data) else { throw AuthServiceError.invalidResponse }
        return .init(accessToken: decoded.access_token, refreshToken: decoded.refresh_token, expiresAt: .now.addingTimeInterval(decoded.expires_in), userId: decoded.user.id, email: decoded.user.email ?? payload["email"] ?? "")
    }

    private func request(path: String, method: String, payload: [String: String]?, bearer: String?) async throws -> (Data, HTTPURLResponse) {
        guard let base = configuration.supabaseURL, let key = configuration.supabaseAnonKey, !key.isEmpty,
              let url = URL(string: path, relativeTo: base) else { throw AuthServiceError.notConfigured }
        var request = URLRequest(url: url); request.httpMethod = method; request.timeoutInterval = 20
        request.setValue(key, forHTTPHeaderField: "apikey"); request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let bearer { request.setValue("Bearer \(bearer)", forHTTPHeaderField: "Authorization") }
        if let payload { request.httpBody = try JSONEncoder().encode(payload) }
        let (data, response) = try await transport.data(for: request)
        guard (200..<300).contains(response.statusCode) else {
            let object = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any]
            let message = object?["msg"] as? String ?? object?["message"] as? String ?? object?["error_description"] as? String ?? "Error de autenticación (\(response.statusCode))."
            throw AuthServiceError.server(response.statusCode, message)
        }
        return (data, response)
    }
}

struct SupabaseAccountService {
    let configuration: AppConfiguration; let transport: NetworkTransport
    init(configuration: AppConfiguration = AppConfiguration(), transport: NetworkTransport = URLSessionTransport()) { self.configuration = configuration; self.transport = transport }
    func deleteAccount(accessToken: String, confirmation: String) async throws {
        guard let base = configuration.supabaseURL, let key = configuration.supabaseAnonKey, !key.isEmpty else { throw AuthServiceError.notConfigured }
        var request = URLRequest(url: base.appendingPathComponent("functions/v1/delete-account")); request.httpMethod = "POST"; request.timeoutInterval = 30
        request.setValue(key, forHTTPHeaderField: "apikey"); request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type"); request.httpBody = try JSONEncoder().encode(["confirmation": confirmation])
        let (data, response) = try await transport.data(for: request)
        guard (200..<300).contains(response.statusCode) else {
            let object = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any]
            throw AuthServiceError.server(response.statusCode, object?["error"] as? String ?? "No se pudo eliminar la cuenta.")
        }
    }
}

@MainActor
final class AccountModel: ObservableObject {
    enum State: Equatable { case unavailable, signedOut, loading, signedIn(AuthTokens), error(String) }
    @Published private(set) var state: State = .signedOut
    let configuration: AppConfiguration; private let service: SupabaseAuthService; private let store: TokenStore

    init(configuration: AppConfiguration = AppConfiguration(), transport: NetworkTransport = URLSessionTransport(), store: TokenStore = KeychainTokenStore()) {
        self.configuration = configuration; self.service = SupabaseAuthService(configuration: configuration, transport: transport); self.store = store
        guard configuration.isSupabaseConfigured else { state = .unavailable; return }
        do { state = try store.load().map(State.signedIn) ?? .signedOut } catch { state = .error(error.localizedDescription) }
    }

    var tokens: AuthTokens? { if case let .signedIn(tokens) = state { tokens } else { nil } }
    func signIn(email: String, password: String) async { await perform { try await self.service.signIn(email: email, password: password) } }
    func signUp(email: String, password: String) async { await perform { try await self.service.signUp(email: email, password: password) } }
    func recover(email: String) async {
        state = .loading
        do { try await service.sendPasswordRecovery(email: email); state = .signedOut }
        catch { state = .error(error.localizedDescription) }
    }
    func restoreAndRefreshIfNeeded() async {
        guard let current = tokens, current.needsRefresh else { return }
        await perform { try await self.service.refresh(current.refreshToken) }
    }
    func signOut() async {
        let access = tokens?.accessToken; state = .loading
        if let access { try? await service.signOut(accessToken: access) }
        do { try store.clear(); state = configuration.isSupabaseConfigured ? .signedOut : .unavailable }
        catch { state = .error(error.localizedDescription) }
    }
    func deleteAccount(confirmation: String) async {
        guard let access = tokens?.accessToken else { state = .error("No hay una sesión activa."); return }
        state = .loading
        do {
            try await SupabaseAccountService(configuration: configuration, transport: service.transport).deleteAccount(accessToken: access, confirmation: confirmation)
            try store.clear(); state = .signedOut
        } catch { state = .error(error.localizedDescription) }
    }

    private func perform(_ operation: () async throws -> AuthTokens) async {
        state = .loading
        do { let value = try await operation(); try store.save(value); state = .signedIn(value) }
        catch { state = .error(error.localizedDescription) }
    }
}
