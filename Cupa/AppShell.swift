import Network
import SwiftUI

@MainActor
final class ConnectivityMonitor: ObservableObject {
    @Published private(set) var isConnected = true
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "com.tacotrifasico.cupa.connectivity")

    init() {
        monitor.pathUpdateHandler = { [weak self] path in
            Task { @MainActor in self?.isConnected = path.status == .satisfied }
        }
        monitor.start(queue: queue)
    }

    deinit { monitor.cancel() }
}

enum CupaTab: String, CaseIterable, Hashable, Identifiable {
    case home, brew, tasting, lab, storage
    var id: Self { self }
    var title: String {
        switch self { case .home: "Taller"; case .brew: "Preparar"; case .tasting: "Cata"; case .lab: "Laboratorio"; case .storage: "Almacén" }
    }
}

@MainActor
final class AppNavigationModel: ObservableObject {
    @Published var selection: CupaTab
    init(selection: CupaTab = .home) { self.selection = selection }
    func select(_ tab: CupaTab) { selection = tab }
}

struct AppShell: View {
    let storageWarning: String?
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.managedObjectContext) private var context
    @StateObject private var account = AccountModel()
    @StateObject private var navigation = AppNavigationModel()
    @StateObject private var calculator = CalculatorModel()
    @StateObject private var lab = LabModel()
    @StateObject private var preparation = PreparationModel()
    @StateObject private var tasting = TastingModel()
    @StateObject private var settings = SettingsModel()
    @StateObject private var connectivity = ConnectivityMonitor()
    @State private var syncInProgress = false
    @State private var syncNotice: String?

    init(storageWarning: String? = nil) { self.storageWarning = storageWarning }

    var body: some View {
        TabView(selection: $navigation.selection) {
            NavigationStack {
                HomeView(
                    selection: $navigation.selection,
                    account: account,
                    settings: settings,
                    calculator: calculator,
                    lab: lab,
                    preparation: preparation
                )
            }
                .tag(CupaTab.home)
                .tabItem { Label("Taller", systemImage: "house") }

            NavigationStack { BrewView(selection: $navigation.selection, calculator: calculator, lab: lab, preparation: preparation) }
                .tag(CupaTab.brew)
                .tabItem { Label("Preparar", systemImage: "mug") }

            NavigationStack { TastingView(model: tasting, lab: lab, selection: $navigation.selection) }
                .tag(CupaTab.tasting)
                .tabItem { Label("Cata", systemImage: "heart") }

            NavigationStack { LabView(model: lab, preparation: preparation, account: account, selection: $navigation.selection) }
                .tag(CupaTab.lab)
                .tabItem { Label("Laboratorio", systemImage: "flask") }

            NavigationStack { StorageView(selection: $navigation.selection, lab: lab, preparation: preparation) }
                .tag(CupaTab.storage)
                .tabItem { Label("Almacén", systemImage: "shippingbox") }
        }
        .id(account.localScopeKey)
        .tint(CupaTheme.forest)
        .safeAreaInset(edge: .top, spacing: 0) {
            VStack(spacing: 0) {
                if let storageWarning {
                    statusBanner(storageWarning, icon: "externaldrive.badge.exclamationmark", color: CupaTheme.terracotta)
                        .accessibilityIdentifier("storage.recoveryWarning")
                }
                if let notice = account.sessionNotice ?? syncNotice {
                    statusBanner(notice, icon: connectivity.isConnected ? "arrow.triangle.2.circlepath" : "wifi.slash", color: CupaTheme.espresso)
                        .accessibilityIdentifier("sync.statusNotice")
                }
            }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active || phase == .background { preparation.synchronizeClock(); tasting.synchronizeClock() }
            if phase == .active { Task { await refreshAndSync() } }
        }
        .task { context.activeOwnerId = account.tokens?.userId; lab.setTemperatureUnit(settings.temperatureUnit); await refreshAndSync() }
        .onChange(of: account.localScopeKey) { _, _ in
            let ownerId = account.tokens?.userId; context.activeOwnerId = ownerId
            calculator.switchScope(to: ownerId); lab.switchScope(to: ownerId); preparation.switchScope(to: ownerId); tasting.switchScope(to: ownerId)
        }
        .onChange(of: settings.temperatureUnit) { _, unit in lab.setTemperatureUnit(unit) }
        .onChange(of: lab.state.temperatureUnit) { _, unit in
            if settings.temperatureUnit != unit { settings.temperatureUnit = unit }
        }
        .onChange(of: connectivity.isConnected) { wasConnected, isConnected in
            guard !wasConnected, isConnected else { return }
            Task { await refreshAndSync() }
        }
        .preferredColorScheme(settings.preferredColorScheme)
    }

    private func statusBanner(_ text: String, icon: String, color: Color) -> some View {
        Label(text, systemImage: icon)
            .font(.caption.bold())
            .foregroundStyle(CupaTheme.onAccent)
            .lineLimit(2)
            .fixedSize(horizontal: false, vertical: true)
            .padding(.horizontal, 12).padding(.vertical, 8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(color)
    }

    private func refreshAndSync() async {
        guard connectivity.isConnected, !syncInProgress else {
            if !connectivity.isConnected { syncNotice = "Sin conexión. Los cambios quedan guardados en este dispositivo." }
            return
        }
        syncInProgress = true
        defer { syncInProgress = false }
        guard let tokens = await account.validTokens() else { return }
        let coordinator = EntitySyncCoordinator(context: context, configuration: account.configuration)
        await coordinator.sync(ownerId: tokens.userId, accessToken: tokens.accessToken)
        if coordinator.authenticationRejected, let refreshed = await account.validTokens(forceRefresh: true) {
            await coordinator.sync(ownerId: refreshed.userId, accessToken: refreshed.accessToken)
        }
        switch coordinator.state {
        case .completed: syncNotice = nil
        case .offline: syncNotice = "Sin conexión. Los cambios se sincronizarán automáticamente al volver internet."
        case let .failed(message): syncNotice = "Sincronización pendiente: \(message)"
        default: break
        }
    }
}
