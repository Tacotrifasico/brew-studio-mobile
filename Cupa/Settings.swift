import SwiftUI

enum ThemePreference: String, CaseIterable, Identifiable { case system, light, dark; var id: Self { self }; var label: String { switch self { case .system: "Sistema"; case .light: "Claro"; case .dark: "Oscuro" } } }

@MainActor
final class SettingsModel: ObservableObject {
    @Published var theme: ThemePreference { didSet { defaults.set(theme.rawValue, forKey: "settings.theme") } }
    @Published var temperatureUnit: TemperatureUnit { didSet { defaults.set(temperatureUnit.rawValue, forKey: "settings.temperature") } }
    @Published var altitudeMeters: Int {
        didSet {
            altitudeMeters = min(5000, max(0, altitudeMeters))
            defaults.set(altitudeMeters, forKey: "settings.altitude")
        }
    }
    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        theme = ThemePreference(rawValue: defaults.string(forKey: "settings.theme") ?? "") ?? .system
        temperatureUnit = TemperatureUnit(rawValue: defaults.string(forKey: "settings.temperature") ?? "") ?? .celsius
        altitudeMeters = defaults.object(forKey: "settings.altitude") == nil ? LabModel(defaults: defaults).state.altitudeMeters : defaults.integer(forKey: "settings.altitude")
        altitudeMeters = min(5000, max(0, altitudeMeters))
    }
    var preferredColorScheme: ColorScheme? { theme == .light ? .light : theme == .dark ? .dark : nil }
}

struct SettingsView: View {
    @ObservedObject var model: SettingsModel; @ObservedObject var account: AccountModel
    @Environment(\.dismiss) private var dismiss
    @State private var showAccount = false
    var body: some View {
        NavigationStack {
            Form {
                Section("Apariencia") {
                    Picker("Tema", selection: $model.theme) { ForEach(ThemePreference.allCases) { Text($0.label).tag($0) } }
                    Picker("Temperatura", selection: $model.temperatureUnit) { Text("Celsius").tag(TemperatureUnit.celsius); Text("Fahrenheit").tag(TemperatureUnit.fahrenheit) }
                }
                Section("Altura de preparación") {
                    Stepper("\(model.altitudeMeters) metros", value: $model.altitudeMeters, in: 0...5000, step: 25)
                    TextField("Metros sobre el nivel del mar", value: $model.altitudeMeters, format: .number)
                        .keyboardType(.numberPad)
                        .onChange(of: model.altitudeMeters) { _, value in if !(0...5000).contains(value) { model.altitudeMeters = min(5000, max(0, value)) } }
                    Text("Hervor estimado: \(boilingText)")
                    Text("Se aplica al laboratorio y a sus recomendaciones. Los cálculos se conservan en Celsius.")
                        .font(.caption).foregroundStyle(.secondary)
                }
                Section("Privacidad") {
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
            .brewScrollableCanvas()
            .navigationTitle("Configuración")
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Cerrar") { dismiss() } } }
            .sheet(isPresented: $showAccount) { AccountView(model: account) }
        }
    }

    private var privacyPolicyURL: URL? {
        configuredURL(for: "PRIVACY_POLICY_URL")
    }
    private var boilingText: String {
        let c = LabEngine.boilingPointC(altitudeMeters: model.altitudeMeters)
        let value = model.temperatureUnit == .celsius ? c : LabEngine.fahrenheit(fromCelsius: c)
        return String(format: "%.1f %@", value, model.temperatureUnit == .celsius ? "°C" : "°F")
    }

    private var supportURL: URL? { configuredURL(for: "SUPPORT_URL") }
    private var communityGuidelinesURL: URL? { configuredURL(for: "COMMUNITY_GUIDELINES_URL") }

    private func configuredURL(for key: String) -> URL? {
        guard let value = Bundle.main.object(forInfoDictionaryKey: key) as? String,
              let url = URL(string: value), url.scheme?.lowercased() == "https" else { return nil }
        return url
    }
}
