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
    @State private var feed: [SocialShare] = []; @State private var feedLoading = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Picker("Brew Hub", selection: $tab) { Text("Perfil").tag(0); Text("Fórmulas").tag(1); Text("Historial").tag(2); Text("Comunidad").tag(3) }.pickerStyle(.segmented).padding()
                if account.tokens == nil { signedOut }
                else { Group { switch tab { case 0: profileTab; case 1: formulasTab; case 2: historyTab; default: communityTab } } }
            }
            .background(CupaTheme.background.ignoresSafeArea())
            .navigationTitle("Brew Studio Hub")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) { if account.tokens != nil { Button { synchronize() } label: { Image(systemName: "arrow.triangle.2.circlepath") }.accessibilityLabel("Sincronizar datos") } }
                ToolbarItem(placement: .confirmationAction) { Button("Cerrar") { dismiss() } }
            }
            .sheet(isPresented: $showAccount) { AccountView(model: account) }
            .task { loadProfile(); await loadFeed() }
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
            Section("Recetas") {
                if recipes.isEmpty { Text("Sin recetas guardadas") }
                else { ForEach(recipes) { recipe in HStack { Text(recipe.name); Spacer(); Button { publish(recipe: recipe) } label: { Image(systemName: "square.and.arrow.up") }.frame(minWidth: 44, minHeight: 44).accessibilityLabel("Publicar receta \(recipe.name)") } } }
            }
            Section("Técnicas") {
                if techniques.isEmpty { Text("Sin técnicas guardadas") }
                else { ForEach(techniques) { technique in HStack { VStack(alignment: .leading) { Text(technique.name); Text(technique.methodName).font(.caption).foregroundStyle(.secondary) }; Spacer(); Button { publish(technique: technique) } label: { Image(systemName: "square.and.arrow.up") }.frame(minWidth: 44, minHeight: 44).accessibilityLabel("Publicar técnica \(technique.name)") } } }
            }
        }.scrollContentBackground(.hidden)
    }

    private var historyTab: some View {
        List {
            Section("Preparaciones") { if brews.isEmpty { Text("Sin preparaciones") } else { ForEach(brews.prefix(20)) { Text("\($0.techniqueNameSnapshot) · \($0.completedAt.formatted(date: .abbreviated, time: .omitted))") } } }
            Section("Catas") { if tastings.isEmpty { Text("Sin catas") } else { ForEach(tastings.prefix(20)) { Text("\($0.activeFlavorFamily.capitalized) · \(Int($0.rating))/5") } } }
        }.scrollContentBackground(.hidden)
    }

    @ViewBuilder private var communityTab: some View {
        if feedLoading { ProgressView("Cargando comunidad…").frame(maxWidth: .infinity, maxHeight: .infinity) }
        else if feed.isEmpty { ContentUnavailableView("Comunidad sin contenido", systemImage: "person.3", description: Text("El feed muestra únicamente publicaciones reales permitidas por RLS. No hay datos demostrativos.")) }
        else {
            List(feed) { share in
                VStack(alignment: .leading, spacing: 8) {
                    Text(share.name).font(.headline); Text("@\(share.fromHandle) · \(share.entityType.capitalized)").font(.caption).foregroundStyle(.secondary)
                    if !share.message.isEmpty { Text(share.message).font(.subheadline) }
                    HStack {
                        Button("Importar") { importShare(share) }
                        Button { socialAction { try await $0.like(shareId: share.id, accessToken: account.tokens!.accessToken) } } label: { Label("Me gusta", systemImage: "heart") }
                        Menu { Button("Reportar contenido", role: .destructive) { socialAction { try await $0.report(shareId: share.id, reason: "USER_REPORTED", accessToken: account.tokens!.accessToken) } }; Button("Bloquear usuario", role: .destructive) { block(share) } } label: { Image(systemName: "ellipsis") }
                            .frame(minWidth: 44, minHeight: 44).accessibilityLabel("Más acciones para \(share.name)")
                    }.font(.caption)
                }.padding(.vertical, 5)
            }.scrollContentBackground(.hidden).refreshable { await loadFeed() }
        }
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
    private func loadFeed() async {
        guard let token = account.tokens?.accessToken else { return }; feedLoading = true
        do { feed = try await SocialService(configuration: account.configuration).feed(accessToken: token) }
        catch { if account.configuration.isSupabaseConfigured { message = error.localizedDescription } }
        feedLoading = false
    }
    private func publish(recipe: RecipeRecord) {
        guard let tokens = account.tokens else { return }
        do {
            let draft = try RecipeTechniqueRepository(context: context).recipeDraft(for: recipe)
            let snapshot = SharedRecipeSnapshot(name: draft.name, recipeKind: draft.recipeKind, intention: draft.intention, suggestedMethodName: draft.suggestedMethodName, tags: draft.tags, ingredients: draft.ingredients.map { .init(name: $0.name, amount: $0.amount, unit: $0.unit) }, steps: draft.steps.map { .init(instruction: $0.instruction, durationSeconds: $0.durationSeconds) })
            let payload = SharePayloadSnapshot(kind: "recipe", recipe: snapshot, technique: nil)
            socialAction { try await $0.publish(entityType: "recipe", entityId: recipe.id, fromName: displayName, fromHandle: alias, name: recipe.name, subtitle: recipe.intention, message: "", payload: payload, accessToken: tokens.accessToken) }
        } catch { message = error.localizedDescription }
    }
    private func publish(technique: TechniqueRecord) {
        guard let tokens = account.tokens else { return }
        do {
            let draft = try RecipeTechniqueRepository(context: context).techniqueDraft(for: technique)
            let snapshot = SharedTechniqueSnapshot(name: draft.name, methodName: draft.methodName, doseGrams: draft.doseGrams, waterMl: draft.waterMl, ratio: draft.ratio, temperatureC: draft.temperatureC, executionMode: draft.executionMode, grindValue: draft.grindValue, grindDescription: draft.grindDescription, grindUnit: draft.grindUnit, notes: draft.notes, techniqueDescription: draft.techniqueDescription, steps: draft.steps.map { .init(title: $0.title, durationSeconds: $0.durationSeconds, waterAddedMl: $0.waterAddedMl, intensity: $0.intensity, gesture: $0.gesture, note: $0.note) })
            let payload = SharePayloadSnapshot(kind: "technique", recipe: nil, technique: snapshot)
            socialAction { try await $0.publish(entityType: "technique", entityId: technique.id, fromName: displayName, fromHandle: alias, name: technique.name, subtitle: technique.methodName, message: "", payload: payload, accessToken: tokens.accessToken) }
        } catch { message = error.localizedDescription }
    }
    private func importShare(_ share: SocialShare) { do { try SocialService(configuration: account.configuration).importShare(share, context: context); message = "Fórmula importada con atribución." } catch { message = error.localizedDescription } }
    private func block(_ share: SocialShare) { socialAction { try await $0.block(userId: share.ownerId, accessToken: account.tokens!.accessToken) }; feed.removeAll { $0.ownerId == share.ownerId } }
    private func socialAction(_ operation: @escaping (SocialService) async throws -> Void) {
        Task { do { try await operation(SocialService(configuration: account.configuration)); message = "Acción completada."; await loadFeed() } catch { message = error.localizedDescription } }
    }
    private func synchronize() {
        guard let tokens = account.tokens else { return }
        Task {
            let coordinator = EntitySyncCoordinator(context: context, configuration: account.configuration)
            await coordinator.sync(ownerId: tokens.userId, accessToken: tokens.accessToken)
            switch coordinator.state {
            case .completed: message = "Datos sincronizados."
            case .offline: message = "Sin backend: los cambios siguen guardados offline."
            case let .failed(error): message = error
            default: break
            }
            loadProfile(); await loadFeed()
        }
    }
}
