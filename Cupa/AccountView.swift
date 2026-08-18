import SwiftUI

struct AccountView: View {
    @ObservedObject var model: AccountModel
    @Environment(\.dismiss) private var dismiss
    @State private var email = ""; @State private var password = ""; @State private var confirmation = ""

    var body: some View {
        NavigationStack {
            Form {
                switch model.state {
                case .unavailable:
                    ContentUnavailableView("Cuenta aún no conectada", systemImage: "person.crop.circle.badge.exclamationmark", description: Text("El build está listo, pero faltan SUPABASE_URL y SUPABASE_ANON_KEY."))
                case let .signedIn(tokens):
                    Section("Sesión") { LabeledContent("Correo", value: tokens.email); LabeledContent("Usuario", value: tokens.userId.uuidString) }
                    Button("Cerrar sesión", role: .destructive) { Task { await model.signOut() } }
                    Section { Text("La eliminación de cuenta se habilitará mediante una función segura de servidor para borrar o anonimizar los datos asociados.").font(.caption).foregroundStyle(.secondary) }
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
            .navigationTitle("Cuenta")
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Cerrar") { dismiss() } } }
        }
    }
}
