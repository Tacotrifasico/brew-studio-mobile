import Foundation
import Security

struct AppConfiguration: Equatable {
    let supabaseURL: URL?; let supabaseAnonKey: String?
    var isSupabaseConfigured: Bool { supabaseURL != nil && !(supabaseAnonKey ?? "").isEmpty }

    init(bundle: Bundle = .main, environment: [String: String] = ProcessInfo.processInfo.environment) {
        let rawURL = environment["SUPABASE_URL"] ?? bundle.object(forInfoDictionaryKey: "SUPABASE_URL") as? String
        let rawKey = environment["SUPABASE_ANON_KEY"] ?? bundle.object(forInfoDictionaryKey: "SUPABASE_ANON_KEY") as? String
        supabaseURL = Self.validatedSupabaseURL(rawURL)
        let cleanKey = rawKey?.trimmingCharacters(in: .whitespacesAndNewlines)
        supabaseAnonKey = cleanKey?.isEmpty == false ? cleanKey : nil
    }

    init(supabaseURL: URL?, supabaseAnonKey: String?) { self.supabaseURL = supabaseURL; self.supabaseAnonKey = supabaseAnonKey }

    private static func validatedSupabaseURL(_ rawValue: String?) -> URL? {
        guard let value = rawValue?.trimmingCharacters(in: .whitespacesAndNewlines),
              !value.isEmpty,
              let url = URL(string: value),
              let scheme = url.scheme?.lowercased(),
              let host = url.host?.lowercased() else { return nil }
        let isLocalDevelopment = scheme == "http" && (host == "localhost" || host == "127.0.0.1")
        return scheme == "https" || isLocalDevelopment ? url : nil
    }
}

struct AuthTokens: Codable, Equatable {
    let accessToken: String; let refreshToken: String; let expiresAt: Date; let userId: UUID; let email: String
    var needsRefresh: Bool { needsRefresh(at: .now) }
    func needsRefresh(at date: Date) -> Bool { expiresAt.timeIntervalSince(date) < 60 }
    func hasValidAccessToken(at date: Date = .now) -> Bool { expiresAt > date }
}

enum SignUpResult: Equatable {
    case signedIn(AuthTokens)
    case confirmationRequired(email: String)
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

enum RemoteFailureClassifier {
    static func isUnauthorized(_ error: Error) -> Bool {
        guard case let AuthServiceError.server(status, _) = error else { return false }
        return status == 401
    }

    static func isTerminalSessionFailure(_ error: Error) -> Bool {
        guard case let AuthServiceError.server(status, _) = error else { return false }
        return status == 400 || status == 401
    }

    static func isOffline(_ error: Error) -> Bool {
        let code = (error as? URLError)?.code
        return [.notConnectedToInternet, .networkConnectionLost, .cannotFindHost, .cannotConnectToHost,
                .dnsLookupFailed, .timedOut, .internationalRoamingOff, .dataNotAllowed].contains(code)
    }
}

struct SupabaseAuthService {
    let configuration: AppConfiguration; let transport: NetworkTransport
    init(configuration: AppConfiguration = AppConfiguration(), transport: NetworkTransport = URLSessionTransport()) { self.configuration = configuration; self.transport = transport }

    func signIn(email: String, password: String) async throws -> AuthTokens { try await token(path: "/auth/v1/token?grant_type=password", payload: ["email": email, "password": password]) }
    func signUp(email: String, password: String) async throws -> SignUpResult {
        let payload = ["email": email, "password": password]
        let (data, _) = try await request(path: "/auth/v1/signup", method: "POST", payload: payload, bearer: nil)
        if let tokens = try? decodeTokens(data, fallbackEmail: email) { return .signedIn(tokens) }

        struct PendingUser: Decodable { let id: UUID; let email: String? }
        guard let user = try? JSONDecoder().decode(PendingUser.self, from: data) else { throw AuthServiceError.invalidResponse }
        let confirmedEmail = user.email.flatMap { $0.isEmpty ? nil : $0 } ?? email
        return .confirmationRequired(email: confirmedEmail)
    }
    func refresh(_ refreshToken: String) async throws -> AuthTokens { try await token(path: "/auth/v1/token?grant_type=refresh_token", payload: ["refresh_token": refreshToken]) }
    func sendPasswordRecovery(email: String) async throws { _ = try await request(path: "/auth/v1/recover", method: "POST", payload: ["email": email], bearer: nil) }
    func resendSignUpConfirmation(email: String) async throws { _ = try await request(path: "/auth/v1/resend", method: "POST", payload: ["email": email, "type": "signup"], bearer: nil) }
    func signOut(accessToken: String) async throws { _ = try await request(path: "/auth/v1/logout", method: "POST", payload: nil, bearer: accessToken) }

    private func token(path: String, payload: [String: String]) async throws -> AuthTokens {
        let (data, _) = try await request(path: path, method: "POST", payload: payload, bearer: nil)
        return try decodeTokens(data, fallbackEmail: payload["email"] ?? "")
    }

    private func decodeTokens(_ data: Data, fallbackEmail: String) throws -> AuthTokens {
        struct Response: Decodable {
            struct User: Decodable { let id: UUID; let email: String? }
            let access_token: String; let refresh_token: String; let expires_in: Double; let user: User
        }
        guard let decoded = try? JSONDecoder().decode(Response.self, from: data) else { throw AuthServiceError.invalidResponse }
        return .init(accessToken: decoded.access_token, refreshToken: decoded.refresh_token, expiresAt: .now.addingTimeInterval(decoded.expires_in), userId: decoded.user.id, email: decoded.user.email ?? fallbackEmail)
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
    @Published private(set) var state: State = .signedOut {
        didSet { LocalDataScope.activeOwnerId = tokens?.userId }
    }
    @Published private(set) var sessionNotice: String?
    @Published private(set) var pendingConfirmationEmail: String?
    @Published private(set) var isRefreshing = false
    let configuration: AppConfiguration; private let service: SupabaseAuthService; private let store: TokenStore
    private let accountDeletionHandler: (UUID) throws -> Void

    init(configuration: AppConfiguration = AppConfiguration(), transport: NetworkTransport = URLSessionTransport(), store: TokenStore = KeychainTokenStore(), accountDeletionHandler: @escaping (UUID) throws -> Void = { _ in }) {
        self.configuration = configuration; self.service = SupabaseAuthService(configuration: configuration, transport: transport); self.store = store
        self.accountDeletionHandler = accountDeletionHandler
        LocalDataScope.activeOwnerId = nil
        guard configuration.isSupabaseConfigured else { state = .unavailable; return }
        do { state = try store.load().map(State.signedIn) ?? .signedOut } catch { state = .error(error.localizedDescription) }
    }

    var tokens: AuthTokens? { if case let .signedIn(tokens) = state { tokens } else { nil } }
    var localScopeKey: String { tokens?.userId.uuidString ?? "guest" }
    func signIn(email: String, password: String) async { await perform { try await self.service.signIn(email: email, password: password) } }
    func signUp(email: String, password: String) async {
        state = .loading; sessionNotice = nil
        do {
            switch try await service.signUp(email: email, password: password) {
            case let .signedIn(tokens):
                try store.save(tokens); pendingConfirmationEmail = nil; state = .signedIn(tokens)
            case let .confirmationRequired(confirmedEmail):
                pendingConfirmationEmail = confirmedEmail; state = .signedOut
                sessionNotice = "Revisa \(confirmedEmail) y confirma tu correo. Después vuelve para iniciar sesión."
            }
        } catch { state = .error(error.localizedDescription) }
    }
    func resendSignUpConfirmation() async {
        guard let email = pendingConfirmationEmail else { return }
        state = .loading; sessionNotice = nil
        do {
            try await service.resendSignUpConfirmation(email: email); state = .signedOut
            sessionNotice = "Enviamos de nuevo la confirmación a \(email)."
        } catch { state = .error(error.localizedDescription) }
    }
    func recover(email: String) async {
        state = .loading
        do {
            try await service.sendPasswordRecovery(email: email); state = .signedOut
            sessionNotice = "Si existe una cuenta con ese correo, recibirás instrucciones para restablecer la contraseña."
        }
        catch { state = .error(error.localizedDescription) }
    }
    @discardableResult
    func validTokens(forceRefresh: Bool = false, now: Date = .now) async -> AuthTokens? {
        guard let current = tokens else { return nil }
        guard forceRefresh || current.needsRefresh(at: now) else { return current }
        guard !isRefreshing else { return current.hasValidAccessToken(at: now) ? current : nil }
        isRefreshing = true
        defer { isRefreshing = false }
        do {
            let refreshed = try await service.refresh(current.refreshToken)
            try store.save(refreshed)
            state = .signedIn(refreshed)
            sessionNotice = nil
            return refreshed
        } catch {
            if RemoteFailureClassifier.isTerminalSessionFailure(error) {
                try? store.clear()
                state = .signedOut
                sessionNotice = "Tu sesión venció. Inicia sesión otra vez para sincronizar."
            } else {
                state = .signedIn(current)
                sessionNotice = RemoteFailureClassifier.isOffline(error)
                    ? "Sin conexión. Puedes seguir trabajando; sincronizaremos al volver internet."
                    : "No pudimos renovar la sesión. Tus datos locales siguen seguros."
            }
            return current.hasValidAccessToken(at: now) ? current : nil
        }
    }
    func restoreAndRefreshIfNeeded() async { _ = await validTokens() }
    func authenticated<Value>(_ operation: (String) async throws -> Value) async throws -> Value {
        guard let current = await validTokens() else { throw URLError(.userAuthenticationRequired) }
        do { return try await operation(current.accessToken) }
        catch {
            guard RemoteFailureClassifier.isUnauthorized(error),
                  let refreshed = await validTokens(forceRefresh: true) else { throw error }
            return try await operation(refreshed.accessToken)
        }
    }
    func signOut() async {
        let access = tokens?.accessToken; state = .loading; sessionNotice = nil; pendingConfirmationEmail = nil
        if let access { try? await service.signOut(accessToken: access) }
        do { try store.clear(); state = configuration.isSupabaseConfigured ? .signedOut : .unavailable }
        catch { state = .error(error.localizedDescription) }
    }
    func deleteAccount(confirmation: String) async {
        guard let ownerId = tokens?.userId else { sessionNotice = "Necesitas una sesión activa y conexión para eliminar la cuenta."; return }
        do {
            try await authenticated { access in
                try await SupabaseAccountService(configuration: self.configuration, transport: self.service.transport).deleteAccount(accessToken: access, confirmation: confirmation)
            }
        } catch {
            sessionNotice = error.localizedDescription
            return
        }

        var cleanupMessages: [String] = []
        do { try accountDeletionHandler(ownerId) }
        catch { cleanupMessages.append("No fue posible limpiar por completo la caché local: \(error.localizedDescription)") }
        do { try store.clear() }
        catch { cleanupMessages.append("No fue posible borrar las credenciales locales: \(error.localizedDescription)") }
        state = .signedOut
        sessionNotice = cleanupMessages.isEmpty ? "Tu cuenta y sus datos fueron eliminados." : cleanupMessages.joined(separator: " ")
    }

    private func perform(_ operation: () async throws -> AuthTokens) async {
        state = .loading; sessionNotice = nil
        do { let value = try await operation(); try store.save(value); pendingConfirmationEmail = nil; state = .signedIn(value); sessionNotice = nil }
        catch { state = .error(error.localizedDescription) }
    }
}
