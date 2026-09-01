import SwiftUI

enum ThemePreference: String, CaseIterable, Identifiable { case system, light, dark; var id: Self { self }; var label: String { switch self { case .system: "Sistema"; case .light: "Claro"; case .dark: "Oscuro" } } }

@MainActor
final class SettingsModel: ObservableObject {
    @Published var theme: ThemePreference { didSet { defaults.set(theme.rawValue, forKey: "settings.theme") } }
    @Published var temperatureUnit: TemperatureUnit { didSet { defaults.set(temperatureUnit.rawValue, forKey: "settings.temperature") } }
    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        theme = ThemePreference(rawValue: defaults.string(forKey: "settings.theme") ?? "") ?? .system
        temperatureUnit = TemperatureUnit(rawValue: defaults.string(forKey: "settings.temperature") ?? "") ?? .celsius
    }
    var preferredColorScheme: ColorScheme? { theme == .light ? .light : theme == .dark ? .dark : nil }
}

struct SettingsView: View {
    @ObservedObject var model: SettingsModel; @ObservedObject var account: AccountModel
    @Environment(\.dismiss) private var dismiss
    @State private var showAccount = false
    @AppStorage("privacy.geminiConsent.v1") private var geminiConsent = false
    var body: some View {
        NavigationStack {
            Form {
                Section("Apariencia") {
                    Picker("Tema", selection: $model.theme) { ForEach(ThemePreference.allCases) { Text($0.label).tag($0) } }
                    Picker("Temperatura", selection: $model.temperatureUnit) { Text("Celsius").tag(TemperatureUnit.celsius); Text("Fahrenheit").tag(TemperatureUnit.fahrenheit) }
                }
                Section("Privacidad") {
                    Toggle("Permitir sugerencias con Google Gemini", isOn: $geminiConsent)
                    Text("Al activarlo, sólo se envían los parámetros de preparación y el perfil sensorial que solicites analizar. No se envían tu correo, nombre ni identificador.")
                        .font(.caption).foregroundStyle(.secondary)
                    Text("Cupa no envía telemetría sensible ni solicita permisos de notificaciones.")
                        .font(.caption).foregroundStyle(.secondary)
                    if let privacyPolicyURL {
                        Link("Consultar política de privacidad", destination: privacyPolicyURL)
                    }
                    if let supportURL {
                        Link("Contactar soporte y moderación", destination: supportURL)
                    }
                    if let communityGuidelinesURL {
                        Link("Normas de la comunidad", destination: communityGuidelinesURL)
                    }
                }
                Section("Cuenta") { Button("Abrir cuenta") { showAccount = true } }
                Section("Aplicación") { LabeledContent("Versión", value: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"); LabeledContent("Entorno", value: Bundle.main.object(forInfoDictionaryKey: "APP_ENVIRONMENT") as? String ?? "Development") }
            }
            .navigationTitle("Configuración")
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Cerrar") { dismiss() } } }
            .sheet(isPresented: $showAccount) { AccountView(model: account) }
        }
    }

    private var privacyPolicyURL: URL? {
        configuredURL(for: "PRIVACY_POLICY_URL")
    }

    private var supportURL: URL? { configuredURL(for: "SUPPORT_URL") }
    private var communityGuidelinesURL: URL? { configuredURL(for: "COMMUNITY_GUIDELINES_URL") }

    private func configuredURL(for key: String) -> URL? {
        guard let value = Bundle.main.object(forInfoDictionaryKey: key) as? String,
              let url = URL(string: value), url.scheme?.lowercased() == "https" else { return nil }
        return url
    }
}
