import SwiftUI

@main
struct CupaApp: App {
    private let persistence: PersistenceController = ProcessInfo.processInfo.arguments.contains("-ui-testing")
        ? PersistenceController(inMemory: true)
        : .shared

    var body: some Scene {
        WindowGroup {
            AppShell(
                storageWarning: persistence.storageRecoveryMessage,
                accountDeletionHandler: { ownerId in
                    try LocalAccountDataPurger(context: persistence.container.viewContext).purge(ownerId: ownerId)
                }
            )
                .preferredColorScheme(nil)
                .environment(\.managedObjectContext, persistence.container.viewContext)
        }
    }
}
