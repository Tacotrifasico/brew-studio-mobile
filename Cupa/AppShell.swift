import SwiftUI

enum CupaTab: Hashable {
    case home, brew, tasting, lab, storage
}

struct AppShell: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var selection: CupaTab = .home
    @StateObject private var calculator = CalculatorModel()
    @StateObject private var lab = LabModel()
    @StateObject private var preparation = PreparationModel()
    @StateObject private var tasting = TastingModel()
    @StateObject private var account = AccountModel()

    var body: some View {
        TabView(selection: $selection) {
            NavigationStack { HomeView(selection: $selection, account: account) }
                .tag(CupaTab.home)
                .tabItem { Label("Taller", systemImage: "house") }

            NavigationStack { BrewView(selection: $selection, calculator: calculator, lab: lab, preparation: preparation) }
                .tag(CupaTab.brew)
                .tabItem { Label("Preparar", systemImage: "mug") }

            NavigationStack { TastingView(model: tasting, selection: $selection) }
                .tag(CupaTab.tasting)
                .tabItem { Label("Cata", systemImage: "heart") }

            NavigationStack { LabView(model: lab, account: account, selection: $selection) }
                .tag(CupaTab.lab)
                .tabItem { Label("Laboratorio", systemImage: "flask") }

            NavigationStack { StorageView() }
                .tag(CupaTab.storage)
                .tabItem { Label("Almacén", systemImage: "shippingbox") }
        }
        .tint(CupaTheme.forest)
        .onChange(of: scenePhase) { _, phase in
            if phase == .active || phase == .background { preparation.synchronizeClock(); tasting.synchronizeClock() }
        }
        .task { await account.restoreAndRefreshIfNeeded() }
    }
}
