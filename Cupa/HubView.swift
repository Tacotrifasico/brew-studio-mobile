import CoreData
import SwiftUI

struct HubView: View {
    @Environment(\.managedObjectContext) private var context
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var account: AccountModel
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \RecipeRecord.updatedAt, ascending: false)], predicate: NSPredicate(format: "deletedAt == nil")) private var recipes: FetchedResults<RecipeRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \TechniqueRecord.updatedAt, ascending: false)], predicate: NSPredicate(format: "deletedAt == nil")) private var techniques: FetchedResults<TechniqueRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \BrewSessionRecord.completedAt, ascending: false)], predicate: NSPredicate(format: "deletedAt == nil")) private var brews: FetchedResults<BrewSessionRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \TastingRecord.evaluatedAt, ascending: false)], predicate: NSPredicate(format: "deletedAt == nil")) private var tastings: FetchedResults<TastingRecord>
    @State private var tab = 0; @State private var showAccount = false
    @State private var displayName = ""; @State private var alias = ""; @State private var biography = ""
    @State private var avatarColor = "#3F7A63"; @State private var favoriteMethods = ""; @State private var isPrivate = true
    @State private var message: String?

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Picker("Brew Hub", selection: $tab) { Text("Perfil").tag(0); Text("Fórmulas").tag(1); Text("Historial").tag(2); Text("Comunidad").tag(3) }.pickerStyle(.segmented).padding()
                if account.tokens == nil { signedOut }
                else { Group { switch tab { case 0: profileTab; case 1: formulasTab; case 2: historyTab; default: communityTab } } }
            }
            .background(CupaTheme.background.ignoresSafeArea())
            .navigationTitle("Brew Studio Hub")
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Cerrar") { dismiss() } } }
            .sheet(isPresented: $showAccount) { AccountView(model: account) }
            .task { loadProfile() }
            .alert("Perfil", isPresented: Binding(get: { message != nil }, set: { if !$0 { message = nil } })) { Button("Aceptar") {} } message: { Text(message ?? "") }
        }
    }

    private var signedOut: some View {
        ContentUnavailableView { Label("Inicia sesión para abrir tu Hub", systemImage: "person.2") } description: { Text("Tus datos locales siguen disponibles en Almacén.") } actions: { Button("Abrir cuenta") { showAccount = true }.buttonStyle(.borderedProminent).tint(CupaTheme.forest) }
    }

    private var profileTab: some View {
        Form {
            Section("Perfil público") {
                TextField("Nombre", text: $displayName); TextField("Alias", text: $alias).textInputAutocapitalization(.never)
                TextField("Biografía", text: $biography, axis: .vertical).lineLimit(2...5)
                TextField("Color de avatar (#RRGGBB)", text: $avatarColor).textInputAutocapitalization(.characters)
                TextField("Métodos favoritos", text: $favoriteMethods)
                Toggle("Perfil privado", isOn: $isPrivate)
                Button("Guardar perfil", action: saveProfile).disabled(displayName.trimmingCharacters(in: .whitespaces).isEmpty || alias.trimmingCharacters(in: .whitespaces).isEmpty)
            }
            Section("Estadísticas reales") {
                LabeledContent("Recetas", value: "\(recipes.count)"); LabeledContent("Técnicas", value: "\(techniques.count)")
                LabeledContent("Preparaciones", value: "\(brews.count)"); LabeledContent("Catas", value: "\(tastings.count)")
            }
        }.scrollContentBackground(.hidden)
    }

    private var formulasTab: some View {
        List {
            Section("Recetas") { if recipes.isEmpty { Text("Sin recetas guardadas") } else { ForEach(recipes) { Text($0.name) } } }
            Section("Técnicas") {
                if techniques.isEmpty { Text("Sin técnicas guardadas") }
                else { ForEach(techniques) { technique in VStack(alignment: .leading) { Text(technique.name); Text(technique.methodName).font(.caption).foregroundStyle(.secondary) } } }
            }
        }.scrollContentBackground(.hidden)
    }

    private var historyTab: some View {
        List {
            Section("Preparaciones") { if brews.isEmpty { Text("Sin preparaciones") } else { ForEach(brews.prefix(20)) { Text("\($0.techniqueNameSnapshot) · \($0.completedAt.formatted(date: .abbreviated, time: .omitted))") } } }
            Section("Catas") { if tastings.isEmpty { Text("Sin catas") } else { ForEach(tastings.prefix(20)) { Text("\($0.activeFlavorFamily.capitalized) · \(Int($0.rating))/5") } } }
        }.scrollContentBackground(.hidden)
    }

    private var communityTab: some View {
        ContentUnavailableView("Comunidad sin contenido", systemImage: "person.3", description: Text("El feed mostrará únicamente publicaciones reales permitidas por RLS. No hay datos demostrativos."))
    }

    private func loadProfile() {
        guard let owner = account.tokens?.userId, let record = try? ProfileRepository(context: context).profile(ownerId: owner) else { return }
        displayName = record.displayName; alias = record.alias; biography = record.biography; avatarColor = record.avatarColor; favoriteMethods = record.favoriteMethods; isPrivate = record.isPrivate
    }
    private func saveProfile() {
        guard let tokens = account.tokens else { showAccount = true; return }
        do {
            let repository = ProfileRepository(context: context)
            let record = try repository.save(ownerId: tokens.userId, displayName: displayName, alias: alias, biography: biography, avatarColor: avatarColor, favoriteMethods: favoriteMethods, isPrivate: isPrivate)
            let payload = try repository.remoteJSON(record)
            Task {
                do { try await SupabaseDataService(configuration: account.configuration).upsert(table: "profiles", json: payload, accessToken: tokens.accessToken); record.syncStatusRaw = SyncStatus.synced.rawValue; try? context.save(); message = "Perfil guardado y sincronizado." }
                catch { message = "Perfil guardado offline; se sincronizará cuando el backend esté disponible." }
            }
        } catch { context.rollback(); message = error.localizedDescription }
    }
}
