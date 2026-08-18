import SwiftUI

enum CupaTab: Hashable {
    case home, brew, tasting, lab, storage
}

struct AppShell: View {
    @State private var selection: CupaTab = .home
    @StateObject private var calculator = CalculatorModel()
    @StateObject private var lab = LabModel()

    var body: some View {
        TabView(selection: $selection) {
            NavigationStack { HomeView(selection: $selection) }
                .tag(CupaTab.home)
                .tabItem { Label("Taller", systemImage: "house") }

            NavigationStack { BrewView(selection: $selection, calculator: calculator, lab: lab) }
                .tag(CupaTab.brew)
                .tabItem { Label("Preparar", systemImage: "mug") }

            NavigationStack { TastingView() }
                .tag(CupaTab.tasting)
                .tabItem { Label("Cata", systemImage: "heart") }

            NavigationStack { LabView(model: lab, selection: $selection) }
                .tag(CupaTab.lab)
                .tabItem { Label("Laboratorio", systemImage: "flask") }

            NavigationStack { StorageView() }
                .tag(CupaTab.storage)
                .tabItem { Label("Almacén", systemImage: "shippingbox") }
        }
        .tint(CupaTheme.forest)
    }
}
