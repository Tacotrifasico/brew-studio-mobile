import SwiftUI

enum ThemePreference: String, CaseIterable, Identifiable { case system, light, dark; var id: Self { self }; var label: String { switch self { case .system: "Sistema"; case .light: "Claro"; case .dark: "Oscuro" } } }

@MainActor
final class SettingsModel: ObservableObject {
    @Published var theme: ThemePreference { didSet { defaults.set(theme.rawValue, forKey: "settings.theme") } }
    @Published var temperatureUnit: TemperatureUnit { didSet { defaults.set(temperatureUnit.rawValue, forKey: "settings.temperature") } }
    @Published var metricUnits: Bool { didSet { defaults.set(metricUnits, forKey: "settings.metric") } }
    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        theme = ThemePreference(rawValue: defaults.string(forKey: "settings.theme") ?? "") ?? .system
        temperatureUnit = TemperatureUnit(rawValue: defaults.string(forKey: "settings.temperature") ?? "") ?? .celsius
        metricUnits = defaults.object(forKey: "settings.metric") as? Bool ?? true
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
                    Toggle("Unidades métricas", isOn: $model.metricUnits)
                }
                Section("Privacidad") { Text("Este build no envía telemetría sensible ni solicita permisos de notificaciones.").font(.caption).foregroundStyle(.secondary) }
                Section("Cuenta") { Button("Abrir cuenta") { showAccount = true } }
                Section("Aplicación") { LabeledContent("Versión", value: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"); LabeledContent("Entorno", value: Bundle.main.object(forInfoDictionaryKey: "APP_ENVIRONMENT") as? String ?? "Development") }
            }
            .navigationTitle("Configuración")
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Cerrar") { dismiss() } } }
            .sheet(isPresented: $showAccount) { AccountView(model: account) }
        }
    }
}
