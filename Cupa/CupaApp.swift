import SwiftUI

@main
struct CupaApp: App {
    private let persistence = PersistenceController.shared

    var body: some Scene {
        WindowGroup {
            AppShell()
                .preferredColorScheme(nil)
                .environment(\.managedObjectContext, persistence.container.viewContext)
        }
    }
}
