import SwiftUI

enum CupaTab: Hashable {
    case home, brew, tasting, lab, storage
}

struct AppShell: View {
    let storageWarning: String?
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.managedObjectContext) private var context
    @State private var selection: CupaTab = .home
    @StateObject private var calculator = CalculatorModel()
    @StateObject private var lab = LabModel()
    @StateObject private var preparation = PreparationModel()
    @StateObject private var tasting = TastingModel()
    @StateObject private var account = AccountModel()
    @StateObject private var settings = SettingsModel()

    init(storageWarning: String? = nil) { self.storageWarning = storageWarning }

    var body: some View {
        TabView(selection: $selection) {
            NavigationStack { HomeView(selection: $selection, account: account, settings: settings) }
                .tag(CupaTab.home)
                .tabItem { Label("Taller", systemImage: "house") }

            NavigationStack { BrewView(selection: $selection, calculator: calculator, lab: lab, preparation: preparation) }
                .tag(CupaTab.brew)
                .tabItem { Label("Preparar", systemImage: "mug") }

            NavigationStack { TastingView(model: tasting, lab: lab, selection: $selection) }
                .tag(CupaTab.tasting)
                .tabItem { Label("Cata", systemImage: "heart") }

            NavigationStack { LabView(model: lab, preparation: preparation, account: account, selection: $selection) }
                .tag(CupaTab.lab)
                .tabItem { Label("Laboratorio", systemImage: "flask") }

            NavigationStack { StorageView(selection: $selection, lab: lab, preparation: preparation) }
                .tag(CupaTab.storage)
                .tabItem { Label("Almacén", systemImage: "shippingbox") }
        }
        .tint(CupaTheme.forest)
        .safeAreaInset(edge: .top, spacing: 0) {
            if let storageWarning {
                Label(storageWarning, systemImage: "externaldrive.badge.exclamationmark")
                    .font(.caption.bold())
                    .foregroundStyle(.white)
                    .padding(.horizontal, 12).padding(.vertical, 8)
                    .frame(maxWidth: .infinity)
                    .background(CupaTheme.terracotta)
                    .accessibilityIdentifier("storage.recoveryWarning")
            }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active || phase == .background { preparation.synchronizeClock(); tasting.synchronizeClock() }
        }
        .task { lab.setTemperatureUnit(settings.temperatureUnit); await account.restoreAndRefreshIfNeeded() }
        .onChange(of: settings.temperatureUnit) { _, unit in lab.setTemperatureUnit(unit) }
        .onChange(of: lab.state.temperatureUnit) { _, unit in
            if settings.temperatureUnit != unit { settings.temperatureUnit = unit }
        }
        .onChange(of: account.tokens) { _, tokens in
            guard let tokens else { return }
            Task { await EntitySyncCoordinator(context: context, configuration: account.configuration).sync(ownerId: tokens.userId, accessToken: tokens.accessToken) }
        }
        .preferredColorScheme(settings.preferredColorScheme)
    }
}
