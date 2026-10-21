import SwiftUI
import AuthenticationServices
import UIKit

@MainActor
private final class GoogleLoginBrowser: NSObject, ObservableObject, ASWebAuthenticationPresentationContextProviding {
    @Published private(set) var isRunning = false
    private var session: ASWebAuthenticationSession?
    private var attemptID: UUID?
    func start(_ request: GooglePKCERequest, completion: @escaping @MainActor (URL?) -> Void) {
        guard !isRunning else { return }
        isRunning = true
        attemptID = request.id
        let auth = ASWebAuthenticationSession(url: request.authorizationURL, callbackURLScheme: request.redirectURL.scheme) { [weak self] callback, _ in
            Task { @MainActor in
                guard let self, self.attemptID == request.id else { return }
                self.attemptID = nil; self.session = nil; self.isRunning = false; completion(callback)
            }
        }
        session = auth; auth.presentationContextProvider = self
        if !auth.start() { attemptID = nil; session = nil; isRunning = false; completion(nil) }
    }
    func cancel() {
        attemptID = nil // A late cancellation from an old browser must not cancel a new login.
        session?.cancel(); session = nil; isRunning = false
    }
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows).first(where: \.isKeyWindow) ?? ASPresentationAnchor()
    }
}

struct AccountView: View {
    @ObservedObject var model: AccountModel
    var requiresLogin = false
    @Environment(\.dismiss) private var dismiss
    @State private var email = ""; @State private var password = ""; @State private var confirmation = ""
    @State private var deleteConfirmation = ""
    @State private var isSignUp = false
    @StateObject private var googleBrowser = GoogleLoginBrowser()

    var body: some View {
        NavigationStack {
            Group {
                switch model.state {
                case .signedOut, .error: loginPage
                default: accountForm
                }
            }
            .navigationTitle(model.tokens == nil ? "Bienvenido" : "Cuenta")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if !requiresLogin { ToolbarItem(placement: .confirmationAction) { Button("Cerrar") { dismiss() } } }
            }
            .interactiveDismissDisabled(requiresLogin)
        }
        // Cancel only when the whole account screen closes. Group applies its
        // modifiers to conditional children; switching login → loading is not exit.
        .onDisappear { googleBrowser.cancel(); model.cancelGoogleSignIn() }
    }

    private var accountForm: some View {
        Form {
                if let notice = model.sessionNotice {
                    Label(notice, systemImage: "exclamationmark.arrow.triangle.2.circlepath")
                        .font(.caption).foregroundStyle(.secondary)
                }
                switch model.state {
                case .unavailable:
                    ContentUnavailableView("Cuenta no disponible", systemImage: "person.crop.circle.badge.exclamationmark", description: Text("El servicio de cuenta todavía no está disponible en esta versión. Tus datos locales permanecen en el dispositivo."))
                case let .signedIn(tokens):
                    Section("Sesión") { LabeledContent("Correo", value: tokens.email) }
                    Button("Cerrar sesión", role: .destructive) { Task { await model.signOut() } }
                    Section("Eliminar cuenta y datos") {
                        Text("Esta acción elimina la cuenta y los datos asociados. Escribe ELIMINAR para confirmarla.").font(.caption).foregroundStyle(.secondary)
                        TextField("ELIMINAR", text: $deleteConfirmation).textInputAutocapitalization(.characters)
                        Button("Eliminar definitivamente", role: .destructive) { Task { await model.deleteAccount(confirmation: deleteConfirmation) } }
                            .disabled(deleteConfirmation != "ELIMINAR")
                    }
                case .loading:
                    HStack { ProgressView(); Text("Conectando…") }
                case .signedOut, .error:
                    EmptyView() // Presented by the dedicated login page instead.
                }
            }
            .brewScrollableCanvas()
    }

    private var loginPage: some View {
        ScrollView {
            VStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text("cUpa").font(.system(size: 48, weight: .bold, design: .serif)).foregroundStyle(CupaTheme.forestText)
                        Spacer(minLength: 12)
                        // Reserved for the future rectangular mascot, not the app icon.
                        RoundedRectangle(cornerRadius: 18).fill(CupaTheme.terracotta.opacity(0.10))
                            .frame(width: 84, height: 84).accessibilityHidden(true)
                    }
                    Text("Hecha por compitas del café.").font(.subheadline.weight(.semibold))
                    Text("Sin cargos extras.").font(.caption).foregroundStyle(CupaTheme.secondaryText)
                }.frame(minHeight: 140)

                VStack(alignment: .leading, spacing: 12) {
                    Text(isSignUp ? "Crear cuenta" : "Iniciar sesión").font(.title3.bold())
                    Button {
                        do {
                            let request = try model.beginGoogleSignIn()
                            googleBrowser.start(request) { callback in
                                if let callback { Task { await model.completeGoogleSignIn(callback) } }
                                else { model.cancelGoogleSignIn() }
                            }
                        } catch { model.cancelGoogleSignIn() }
                    } label: {
                        HStack { Spacer(); Text("Continuar con Google"); Spacer() }
                            .frame(maxWidth: .infinity, minHeight: 32)
                    }.buttonStyle(.bordered).disabled(googleBrowser.isRunning || !model.configuration.isSupabaseConfigured)
                    HStack { Rectangle().frame(height: 1); Text("o con tu correo").font(.caption); Rectangle().frame(height: 1) }
                        .foregroundStyle(CupaTheme.secondaryText.opacity(0.6))
                    TextField("Correo", text: $email)
                        .textInputAutocapitalization(.never).autocorrectionDisabled()
                        .keyboardType(.emailAddress).textContentType(.emailAddress)
                        .textFieldStyle(.roundedBorder).accessibilityLabel("Correo")
                    SecureField("Contraseña", text: $password)
                        .textContentType(isSignUp ? .newPassword : .password)
                        .textFieldStyle(.roundedBorder).accessibilityLabel("Contraseña")
                    if isSignUp {
                        SecureField("Confirmar contraseña", text: $confirmation).textContentType(.newPassword)
                            .textFieldStyle(.roundedBorder)
                        Text("Usa al menos 8 caracteres.").font(.caption).foregroundStyle(CupaTheme.secondaryText)
                    }
                    if let notice = model.sessionNotice { Text(notice).font(.caption).foregroundStyle(CupaTheme.forestText) }
                    if case let .error(message) = model.state { Text(message).font(.caption).foregroundStyle(.red) }
                    Button {
                        Task {
                            if isSignUp { await model.signUp(email: email.trimmingCharacters(in: .whitespacesAndNewlines), password: password) }
                            else { await model.signIn(email: email.trimmingCharacters(in: .whitespacesAndNewlines), password: password) }
                        }
                    } label: {
                        Text(isSignUp ? "Crear cuenta" : "Iniciar sesión").frame(maxWidth: .infinity, minHeight: 32)
                    }
                    .buttonStyle(.borderedProminent).tint(CupaTheme.forest)
                    .frame(maxWidth: .infinity)
                    .disabled(email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || password.isEmpty || (isSignUp && (password.count < 8 || password != confirmation)))
                    if !isSignUp {
                        Button("Recuperar contraseña") { Task { await model.recover(email: email.trimmingCharacters(in: .whitespacesAndNewlines)) } }
                            .font(.caption).disabled(email.isEmpty)
                    }
                    if model.pendingConfirmationEmail != nil {
                        Button("Reenviar confirmación") { Task { await model.resendSignUpConfirmation() } }.font(.caption)
                    }
                }
                .padding(20).background(CupaTheme.card, in: RoundedRectangle(cornerRadius: 20))
                .overlay { RoundedRectangle(cornerRadius: 20).stroke(CupaTheme.border, lineWidth: 1) }
                Button(isSignUp ? "Ya tengo cuenta" : "Crear una cuenta") { isSignUp.toggle(); confirmation = "" }
                if !requiresLogin {
                    Button("Continuar sin cuenta") { dismiss() }.font(.subheadline).foregroundStyle(CupaTheme.secondaryText)
                }
                Text("Inicia sesión para entrar. Tus granos y preparaciones se guardan en este dispositivo.")
                    .font(.caption).foregroundStyle(CupaTheme.secondaryText).multilineTextAlignment(.center)
            }.padding(.horizontal, 24).padding(.vertical, 16).frame(maxWidth: 480)
                .frame(maxWidth: .infinity)
        }.background(BrewOrganicCanvas().ignoresSafeArea())
    }
}

struct PasswordResetView: View {
    @ObservedObject var model: AccountModel
    @Environment(\.dismiss) private var dismiss
    @State private var password = ""
    @State private var confirmation = ""

    var body: some View {
        NavigationStack {
            Form {
                switch model.passwordRecoveryState {
                case .ready, .updating:
                    Section("Nueva contraseña") {
                        SecureField("Contraseña", text: $password).textContentType(.newPassword)
                        SecureField("Confirmar contraseña", text: $confirmation).textContentType(.newPassword)
                        Text("Usa al menos 8 caracteres.").font(.caption).foregroundStyle(.secondary)
                    }
                    Button(model.passwordRecoveryState == .updating ? "Actualizando…" : "Guardar contraseña") {
                        Task { await model.completePasswordRecovery(newPassword: password) }
                    }
                    .disabled(model.passwordRecoveryState == .updating || password.count < 8 || password != confirmation)
                case .completed:
                    ContentUnavailableView("Contraseña actualizada", systemImage: "checkmark.shield", description: Text("Cierra esta pantalla e inicia sesión con tu nueva contraseña."))
                case let .error(message):
                    ContentUnavailableView("No se pudo recuperar la contraseña", systemImage: "link.badge.plus", description: Text(message))
                    if model.canRetryPasswordRecovery {
                        Button("Intentar de nuevo") { model.retryPasswordRecovery() }
                    }
                case .idle:
                    EmptyView()
                }
            }
            .brewScrollableCanvas()
            .navigationTitle("Recuperar cuenta")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Cerrar") { model.dismissPasswordRecovery(); dismiss() }
                        .disabled(model.passwordRecoveryState == .updating)
                }
            }
            .interactiveDismissDisabled(model.passwordRecoveryState == .updating)
        }
    }
}
