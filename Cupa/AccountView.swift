import SwiftUI

struct AccountView: View {
    @ObservedObject var model: AccountModel
    @Environment(\.dismiss) private var dismiss
    @State private var email = ""; @State private var password = ""; @State private var confirmation = ""
    @State private var deleteConfirmation = ""

    var body: some View {
        NavigationStack {
            Form {
                if let notice = model.sessionNotice {
                    Label(notice, systemImage: "exclamationmark.arrow.triangle.2.circlepath")
                        .font(.caption).foregroundStyle(.secondary)
                }
                switch model.state {
                case .unavailable:
                    ContentUnavailableView("Cuenta no disponible", systemImage: "person.crop.circle.badge.exclamationmark", description: Text("El servicio de cuenta todavía no está disponible en esta versión. Tus datos locales permanecen en el dispositivo."))
                case let .signedIn(tokens):
                    Section("Sesión") { LabeledContent("Correo", value: tokens.email); LabeledContent("Usuario", value: tokens.userId.uuidString) }
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
                    Section("Acceso") {
                        TextField("Correo", text: $email).textInputAutocapitalization(.never).keyboardType(.emailAddress).textContentType(.emailAddress)
                        SecureField("Contraseña", text: $password).textContentType(.password)
                        SecureField("Confirmar contraseña", text: $confirmation).textContentType(.newPassword)
                    }
                    if case let .error(message) = model.state { Text(message).foregroundStyle(.red).font(.caption) }
                    Button("Iniciar sesión") { Task { await model.signIn(email: email, password: password) } }.disabled(email.isEmpty || password.isEmpty)
                    Button("Crear cuenta") { Task { await model.signUp(email: email, password: password) } }.disabled(email.isEmpty || password.count < 8 || password != confirmation)
                    Button("Recuperar contraseña") { Task { await model.recover(email: email) } }.disabled(email.isEmpty)
                }
            }
            .brewScrollableCanvas()
            .navigationTitle("Cuenta")
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Cerrar") { dismiss() } } }
        }
    }
}
