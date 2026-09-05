import CoreData
import SwiftUI

private enum HubShareDraft: Identifiable {
    case recipe(RecipeRecord), technique(TechniqueRecord)
    var id: UUID { switch self { case let .recipe(value): value.id; case let .technique(value): value.id } }
    var title: String { switch self { case let .recipe(value): value.name; case let .technique(value): value.name } }
}

struct HubView: View {
    @Environment(\.managedObjectContext) private var context
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var account: AccountModel
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \RecipeRecord.updatedAt, ascending: false)], predicate: LocalDataScope.visiblePredicate()) private var recipes: FetchedResults<RecipeRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \TechniqueRecord.updatedAt, ascending: false)], predicate: LocalDataScope.visiblePredicate()) private var techniques: FetchedResults<TechniqueRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \BrewSessionRecord.completedAt, ascending: false)], predicate: LocalDataScope.visiblePredicate()) private var brews: FetchedResults<BrewSessionRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \TastingRecord.evaluatedAt, ascending: false)], predicate: LocalDataScope.visiblePredicate()) private var tastings: FetchedResults<TastingRecord>
    @State private var tab = 0; @State private var showAccount = false
    @State private var displayName = ""; @State private var alias = ""; @State private var biography = ""
    @State private var avatarColor = "#3F7A63"; @State private var favoriteMethods = ""; @State private var isPrivate = true
    @State private var message: String?
    @State private var feed: [SocialShare] = []; @State private var feedLoading = false
    @State private var inbox: [SocialInboxItem] = []; @State private var inboxLoading = false
    @State private var socialLoadError: String?
    @State private var activity: [SocialActivity] = []; @State private var communitySection = 0
    @State private var likedShareIds: Set<UUID> = []; @State private var savedShareIds: Set<UUID> = []
    @State private var blockedUserIds: Set<UUID> = []
    @State private var shareDraft: HubShareDraft?
    @State private var reportShare: SocialShare?
    @State private var blockShare: SocialShare?

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Picker("Brew Hub", selection: $tab) { Text("Perfil").tag(0); Text("Bandeja").tag(1); Text("Fórmulas").tag(2); Text("Historial").tag(3) }.pickerStyle(.segmented).padding()
                if account.tokens == nil { signedOut }
                else { Group { switch tab { case 0: profileTab; case 1: communityTab; case 2: formulasTab; default: historyTab } } }
            }
            .background(CupaTheme.background.ignoresSafeArea())
            .navigationTitle("Brew Studio Hub")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) { if account.tokens != nil { Button { synchronize() } label: { Image(systemName: "arrow.triangle.2.circlepath") }.accessibilityLabel("Sincronizar datos") } }
                ToolbarItem(placement: .confirmationAction) { Button("Cerrar") { dismiss() } }
            }
            .sheet(isPresented: $showAccount) { AccountView(model: account) }
            .sheet(item: $shareDraft) { draft in
                ShareComposer(title: draft.title, allowsPublic: !isPrivate) { message, visibility, recipient in
                    publish(draft, message: message, visibility: visibility, targetUserId: recipient)
                }
            }
            .sheet(item: $reportShare) { share in
                ReportComposer(shareName: share.name) { reason, details in
                    socialAction(successMessage: "Reporte enviado para revisión.") { try await $0.report(shareId: share.id, reason: reason, details: details, accessToken: $1) }
                }
            }
            .confirmationDialog("¿Bloquear a este usuario?", isPresented: Binding(get: { blockShare != nil }, set: { if !$0 { blockShare = nil } }), presenting: blockShare) { share in
                Button("Bloquear @\(share.fromHandle)", role: .destructive) { block(share); blockShare = nil }
                Button("Cancelar", role: .cancel) { blockShare = nil }
            } message: { _ in Text("Sus publicaciones dejarán de aparecer. Puedes desbloquearlo desde tu perfil.") }
            .task { loadProfile(); await loadSocialData() }
            .alert("Brew Hub", isPresented: Binding(get: { message != nil }, set: { if !$0 { message = nil } })) { Button("Aceptar") {} } message: { Text(message ?? "") }
        }
    }

    private var signedOut: some View {
        ContentUnavailableView { Label("Inicia sesión para abrir tu Hub", systemImage: "person.2") } description: { Text("Tus datos locales siguen disponibles en Almacén.") } actions: { Button("Abrir cuenta") { showAccount = true }.buttonStyle(.borderedProminent).tint(CupaTheme.forest).foregroundStyle(CupaTheme.onAccent) }
    }

    private var profileTab: some View {
        Form {
            Section("Perfil público") {
                HStack(spacing: 14) {
                    ZStack {
                        Circle().fill(profileAvatarColor)
                        Text(profileInitials).font(.title2.bold()).foregroundStyle(.white)
                    }.frame(width: 64, height: 64).accessibilityLabel("Avatar con iniciales \(profileInitials)")
                    VStack(alignment: .leading) {
                        Text(displayName.isEmpty ? "Tu nombre" : displayName).font(.headline)
                        Text(alias.isEmpty ? "@alias" : "@\(alias.replacingOccurrences(of: "@", with: ""))").font(.subheadline).foregroundStyle(.secondary)
                    }
                }
                TextField("Nombre", text: $displayName)
                Text("\(displayName.count)/80").font(.caption2).foregroundStyle(displayName.count > 80 ? .red : .secondary)
                TextField("Alias", text: $alias).textInputAutocapitalization(.never).autocorrectionDisabled()
                TextField("Biografía", text: $biography, axis: .vertical).lineLimit(2...5)
                Text("\(biography.count)/300").font(.caption2).foregroundStyle(biography.count > 300 ? .red : .secondary)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack {
                        ForEach(["#3F7A63", "#234E3C", "#8C4A32", "#315C8C", "#6C4A8C", "#8C6A24"], id: \.self) { hex in
                            Button { avatarColor = hex } label: {
                                Circle().fill(color(hex)).frame(width: 38, height: 38)
                                    .overlay { if avatarColor.uppercased() == hex { Image(systemName: "checkmark").foregroundStyle(.white).fontWeight(.bold) } }
                            }.buttonStyle(.plain).accessibilityLabel("Usar color \(hex)")
                        }
                    }.padding(.vertical, 2)
                }
                TextField("Color de avatar (#RRGGBB)", text: $avatarColor).textInputAutocapitalization(.characters)
                TextField("Métodos favoritos", text: $favoriteMethods)
                Toggle("Perfil privado", isOn: $isPrivate)
                Text(isPrivate ? "Un perfil privado sólo puede enviar fórmulas directamente; no puede publicarlas en el muro." : "Tu perfil puede publicar fórmulas en el muro público.")
                    .font(.caption).foregroundStyle(.secondary)
                if let profileValidationError { Text(profileValidationError).font(.caption).foregroundStyle(.red) }
                Button("Guardar perfil", action: saveProfile).disabled(profileValidationError != nil)
            }
            Section("Privacidad social") {
                if blockedUserIds.isEmpty { Text("No has bloqueado usuarios.").foregroundStyle(.secondary) }
                else {
                    ForEach(Array(blockedUserIds).sorted(by: { $0.uuidString < $1.uuidString }), id: \.self) { userId in
                        HStack {
                            VStack(alignment: .leading) { Text("Usuario bloqueado"); Text("Referencia ···\(userId.uuidString.suffix(6))").font(.caption2).foregroundStyle(.secondary) }
                            Spacer(); Button("Desbloquear") { unblock(userId) }
                        }
                    }
                }
            }
            Section("Estadísticas reales") {
                LabeledContent("Recetas", value: "\(recipes.count)"); LabeledContent("Técnicas", value: "\(techniques.count)")
                LabeledContent("Preparaciones", value: "\(brews.count)"); LabeledContent("Catas", value: "\(tastings.count)")
            }
            Section("Cuenta") {
                Button("Cerrar sesión", role: .destructive) { Task { await account.signOut() } }
            }
        }.brewScrollableCanvas()
    }

    private var formulasTab: some View {
        List {
            Section("Recetas") {
                if recipes.isEmpty { Text("Sin recetas guardadas") }
                else { ForEach(recipes) { recipe in HStack { VStack(alignment: .leading) { Text(recipe.name); if recipe.isShared { Label("Con atribución de comunidad", systemImage: "person.2").font(.caption2).foregroundStyle(.secondary) } }; Spacer(); Button { shareDraft = .recipe(recipe) } label: { Image(systemName: "square.and.arrow.up") }.frame(minWidth: 44, minHeight: 44).accessibilityLabel("Compartir receta \(recipe.name)") } } }
            }
            Section("Técnicas") {
                if techniques.isEmpty { Text("Sin técnicas guardadas") }
                else { ForEach(techniques) { technique in HStack { VStack(alignment: .leading) { Text(technique.name); Text(technique.methodName).font(.caption).foregroundStyle(.secondary); if technique.isShared { Label("Con atribución de comunidad", systemImage: "person.2").font(.caption2).foregroundStyle(.secondary) } }; Spacer(); Button { shareDraft = .technique(technique) } label: { Image(systemName: "square.and.arrow.up") }.frame(minWidth: 44, minHeight: 44).accessibilityLabel("Compartir técnica \(technique.name)") } } }
            }
        }.brewScrollableCanvas()
    }

    private var historyTab: some View {
        List {
            Section("Actividad social") {
                if activity.isEmpty { Text("Aún no tienes actividad social registrada.").foregroundStyle(.secondary) }
                else { ForEach(activity) { item in
                    Label {
                        VStack(alignment: .leading) {
                            Text(item.note ?? activityTitle(item.action))
                            Text(item.action.uppercased()).font(.caption2).foregroundStyle(.secondary)
                        }
                    } icon: { Image(systemName: item.action.contains("share") ? "square.and.arrow.up" : "arrow.down.doc") }
                } }
            }
            Section("Preparaciones") { if brews.isEmpty { Text("Sin preparaciones") } else { ForEach(brews.prefix(20)) { Text("\($0.techniqueNameSnapshot) · \($0.completedAt.formatted(date: .abbreviated, time: .omitted))") } } }
            Section("Catas") { if tastings.isEmpty { Text("Sin catas") } else { ForEach(tastings.prefix(20)) { Text("\($0.activeFlavorFamily.capitalized) · \(Int($0.rating))/5") } } }
        }.brewScrollableCanvas()
    }

    @ViewBuilder private var communityTab: some View {
        VStack(spacing: 0) {
            Picker("Bandeja social", selection: $communitySection) { Text("Muro público").tag(0); Text("Recibidos").tag(1) }
                .pickerStyle(.segmented).padding(.horizontal).padding(.bottom, 8)
            if let socialLoadError, !(communitySection == 0 ? feed.isEmpty : inbox.isEmpty) {
                HStack(alignment: .top, spacing: 8) {
                    Image(systemName: "exclamationmark.triangle.fill")
                    Text(socialLoadError).font(.caption).lineLimit(3)
                    Spacer()
                    Button("Reintentar") { Task { await loadSocialData() } }.font(.caption.bold())
                }
                .foregroundStyle(CupaTheme.onTerracotta).padding(10).background(CupaTheme.terracotta)
                .accessibilityIdentifier("hub.social.loadWarning")
            }
            if communitySection == 0 {
                if feedLoading { ProgressView("Cargando comunidad…").frame(maxWidth: .infinity, maxHeight: .infinity) }
                else if let socialLoadError, feed.isEmpty { socialFailureView(socialLoadError) }
                else if feed.isEmpty { ContentUnavailableView("Comunidad sin contenido", systemImage: "person.3", description: Text("Cuando haya fórmulas públicas aparecerán aquí.")) }
                else { List(feed) { share in shareCard(share, inboxItem: nil) }.brewScrollableCanvas().refreshable { await loadSocialData() } }
            } else {
                if inboxLoading { ProgressView("Cargando recibidos…").frame(maxWidth: .infinity, maxHeight: .infinity) }
                else if let socialLoadError, inbox.isEmpty { socialFailureView(socialLoadError) }
                else if inbox.isEmpty { ContentUnavailableView("Bandeja vacía", systemImage: "tray", description: Text("Aquí aparecerán las fórmulas que te envíen directamente.")) }
                else {
                    List(inbox) { item in
                        if let share = item.share { shareCard(share, inboxItem: item) }
                        else { removedInboxCard(item) }
                    }
                    .brewScrollableCanvas().refreshable { await loadSocialData() }
                }
            }
        }
    }

    private func socialFailureView(_ error: String) -> some View {
        ContentUnavailableView {
            Label("No se pudo actualizar el Hub", systemImage: "wifi.exclamationmark")
        } description: {
            Text(error)
        } actions: {
            Button("Reintentar") { Task { await loadSocialData() } }
                .buttonStyle(.borderedProminent).tint(CupaTheme.forest).foregroundStyle(CupaTheme.onAccent)
        }
        .accessibilityIdentifier("hub.social.loadError")
    }

    private func removedInboxCard(_ item: SocialInboxItem) -> some View {
        Label {
            VStack(alignment: .leading, spacing: 3) {
                Text("Contenido retirado").font(.headline)
                Text("La fórmula ya no está disponible o fue eliminada por su autor.").font(.caption).foregroundStyle(.secondary)
            }
        } icon: {
            Image(systemName: "doc.badge.ellipsis")
        }
        .padding(.vertical, 6)
        .onAppear { if item.readAt == nil { markRead(item) } }
        .accessibilityIdentifier("hub.inbox.removedContent")
    }

    private func shareCard(_ share: SocialShare, inboxItem: SocialInboxItem?) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(share.name).font(.headline)
                Spacer()
                if inboxItem?.readAt == nil { Circle().fill(CupaTheme.terracotta).frame(width: 9, height: 9).accessibilityLabel("Sin leer") }
            }
            Text("@\(share.fromHandle) · \(share.entityType == "recipe" ? "Receta" : "Técnica")").font(.caption).foregroundStyle(.secondary)
            if !share.subtitle.isEmpty { Text(share.subtitle).font(.subheadline.bold()).foregroundStyle(CupaTheme.forest) }
            if !share.message.isEmpty { Text(share.message).font(.subheadline) }
            shareSnapshotSummary(share.payloadSnapshot)
            if let originalAuthorName = share.originalAuthorName,
               !originalAuthorName.isEmpty,
               originalAuthorName.caseInsensitiveCompare(share.fromName) != .orderedSame {
                Label("Original de \(originalAuthorName)", systemImage: "arrow.triangle.branch")
                    .font(.caption).foregroundStyle(CupaTheme.terracottaText)
            }
            if let date = socialDate(share.createdAt) {
                Text(date.formatted(date: .abbreviated, time: .shortened)).font(.caption2).foregroundStyle(.secondary)
            }
            HStack {
                Menu {
                    Button("Registrar copia") { copyShare(share, mode: .imported) }
                    Button("Crear variante") { copyShare(share, mode: .forked) }
                } label: { Label("Añadir", systemImage: "square.and.arrow.down") }
                if inboxItem == nil {
                    Button { toggleLike(share) } label: { Image(systemName: likedShareIds.contains(share.id) ? "heart.fill" : "heart").accessibilityLabel(likedShareIds.contains(share.id) ? "Quitar Me gusta" : "Me gusta") }
                        .frame(minWidth: 44, minHeight: 44)
                }
                Menu {
                    Button(savedShareIds.contains(share.id) ? "Quitar de guardados" : "Guardar publicación") { toggleSaved(share) }
                    if share.ownerId != account.tokens?.userId {
                        Button("Reportar contenido", role: .destructive) { reportShare = share }
                        Button("Bloquear usuario", role: .destructive) { blockShare = share }
                    }
                } label: { Image(systemName: "ellipsis") }
                    .frame(minWidth: 44, minHeight: 44).accessibilityLabel("Más acciones para \(share.name)")
            }.font(.caption)
        }
        .padding(.vertical, 5)
        .onAppear { if let inboxItem, inboxItem.readAt == nil { markRead(inboxItem) } }
    }

    @ViewBuilder private func shareSnapshotSummary(_ payload: SharePayloadSnapshot) -> some View {
        if let recipe = payload.recipe {
            VStack(alignment: .leading, spacing: 5) {
                if !recipe.suggestedMethodName.isEmpty {
                    Label(recipe.suggestedMethodName, systemImage: "mug").font(.caption.bold())
                }
                Text("\(recipe.ingredients.count) ingredientes · \(recipe.steps.count) pasos")
                    .font(.caption).foregroundStyle(.secondary)
                if !recipe.tags.isEmpty { Text(recipe.tags).font(.caption2).foregroundStyle(CupaTheme.terracottaText).lineLimit(2) }
            }
        } else if let technique = payload.technique {
            VStack(alignment: .leading, spacing: 5) {
                Text("\(technique.doseGrams.formatted(.number.precision(.fractionLength(0...1)))) g · \(technique.waterMl) ml · 1:\(technique.ratio.formatted(.number.precision(.fractionLength(0...1)))) · \(technique.temperatureC) °C")
                    .font(.caption.bold()).foregroundStyle(CupaTheme.forest)
                Text("\(technique.methodName) · \(technique.steps.count) pasos · \(technique.executionMode.replacingOccurrences(of: "_", with: " ").capitalized)")
                    .font(.caption).foregroundStyle(.secondary)
                if !technique.grindDescription.isEmpty { Label(technique.grindDescription, systemImage: "dial.medium").font(.caption2).foregroundStyle(.secondary) }
            }
        }
    }

    private func socialDate(_ text: String) -> Date? {
        let fractional = ISO8601DateFormatter(); fractional.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return fractional.date(from: text) ?? ISO8601DateFormatter().date(from: text)
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
                do {
                    try await account.authenticated { token in
                        try await SupabaseDataService(configuration: account.configuration).upsert(table: "profiles", json: payload, accessToken: token)
                    }
                    record.syncStatusRaw = SyncStatus.synced.rawValue; try? context.save(); message = "Perfil guardado y sincronizado."
                }
                catch { message = "Perfil guardado offline; se sincronizará cuando el backend esté disponible." }
            }
        } catch { context.rollback(); message = error.localizedDescription }
    }
    private func loadSocialData() async {
        guard let userId = account.tokens?.userId else { return }
        feedLoading = true; inboxLoading = true; socialLoadError = nil
        defer { feedLoading = false; inboxLoading = false }
        do {
            let result = try await account.authenticated { token in
                async let feed = SocialService(configuration: account.configuration).feed(accessToken: token)
                async let inbox = SocialService(configuration: account.configuration).inbox(userId: userId, accessToken: token)
                async let activity = SocialService(configuration: account.configuration).activity(userId: userId, accessToken: token)
                async let likes = SocialService(configuration: account.configuration).likedShareIds(userId: userId, accessToken: token)
                async let saves = SocialService(configuration: account.configuration).savedShareIds(userId: userId, accessToken: token)
                async let blocks = SocialService(configuration: account.configuration).blockedUserIds(userId: userId, accessToken: token)
                return try await (feed, inbox, activity, likes, saves, blocks)
            }
            feed = result.0; inbox = result.1; activity = result.2; likedShareIds = result.3; savedShareIds = result.4; blockedUserIds = result.5
        } catch {
            socialLoadError = account.configuration.isSupabaseConfigured
                ? error.localizedDescription
                : "El Hub necesita la conexión de Supabase. Tus fórmulas locales siguen disponibles."
        }
    }

    private func publish(_ draft: HubShareDraft, message: String, visibility: String, targetUserId: UUID?) {
        guard ProfileSharingPolicy.allowedVisibilities(isPrivate: isPrivate).contains(visibility) else {
            self.message = "Tu perfil privado sólo permite compartir directamente."
            return
        }
        switch draft {
        case let .recipe(recipe): publish(recipe: recipe, message: message, visibility: visibility, targetUserId: targetUserId)
        case let .technique(technique): publish(technique: technique, message: message, visibility: visibility, targetUserId: targetUserId)
        }
    }
    private func publish(recipe: RecipeRecord, message: String, visibility: String, targetUserId: UUID?) {
        do {
            let draft = try RecipeTechniqueRepository(context: context).recipeDraft(for: recipe)
            let snapshot = SharedRecipeSnapshot(name: draft.name, recipeKind: draft.recipeKind, intention: draft.intention, suggestedMethodName: draft.suggestedMethodName, tags: draft.tags, ingredients: draft.ingredients.map { .init(name: $0.name, amount: $0.amount, unit: $0.unit) }, steps: draft.steps.map { .init(instruction: $0.instruction, durationSeconds: $0.durationSeconds) })
            let payload = SharePayloadSnapshot(kind: "recipe", recipe: snapshot, technique: nil)
            socialAction(successMessage: visibility == "DIRECT" ? "Fórmula enviada al buzón." : "Receta publicada.") { service, token in
                try await service.publish(entityType: "recipe", entityId: recipe.id, fromName: displayName, fromHandle: alias, name: recipe.name, subtitle: recipe.intention, message: message, payload: payload, visibility: visibility, targetUserId: targetUserId, originalAuthorUserId: recipe.originalAuthorUserId, originalAuthorName: recipe.originalAuthorName, originalEntityId: recipe.originalEntityId, accessToken: token)
                try? await service.logActivity(action: "share_recipe", entityType: "recipe", entityId: recipe.id, note: visibility == "DIRECT" ? "Enviaste \(recipe.name) directamente" : "Publicaste \(recipe.name)", accessToken: token)
            }
        } catch { self.message = error.localizedDescription }
    }
    private func publish(technique: TechniqueRecord, message: String, visibility: String, targetUserId: UUID?) {
        do {
            let draft = try RecipeTechniqueRepository(context: context).techniqueDraft(for: technique)
            let snapshot = SharedTechniqueSnapshot(name: draft.name, methodName: draft.methodName, doseGrams: draft.doseGrams, waterMl: draft.waterMl, ratio: draft.ratio, temperatureC: draft.temperatureC, executionMode: draft.executionMode, grindValue: draft.grindValue, grindDescription: draft.grindDescription, grindUnit: draft.grindUnit, notes: draft.notes, techniqueDescription: draft.techniqueDescription, steps: draft.steps.map { .init(title: $0.title, durationSeconds: $0.durationSeconds, waterAddedMl: $0.waterAddedMl, intensity: $0.intensity, gesture: $0.gesture, note: $0.note) })
            let payload = SharePayloadSnapshot(kind: "technique", recipe: nil, technique: snapshot)
            socialAction(successMessage: visibility == "DIRECT" ? "Técnica enviada al buzón." : "Técnica publicada.") { service, token in
                try await service.publish(entityType: "technique", entityId: technique.id, fromName: displayName, fromHandle: alias, name: technique.name, subtitle: technique.methodName, message: message, payload: payload, visibility: visibility, targetUserId: targetUserId, originalAuthorUserId: technique.originalAuthorUserId, originalAuthorName: technique.originalAuthorName, originalEntityId: technique.originalEntityId, accessToken: token)
                try? await service.logActivity(action: "share_technique", entityType: "technique", entityId: technique.id, note: visibility == "DIRECT" ? "Enviaste \(technique.name) directamente" : "Publicaste \(technique.name)", accessToken: token)
            }
        } catch { self.message = error.localizedDescription }
    }
    private func copyShare(_ share: SocialShare, mode: SocialCopyMode) {
        do {
            try SocialService(configuration: account.configuration).importShare(share, mode: mode, context: context)
            message = mode == .forked ? "Variante creada con atribución." : "Fórmula copiada con atribución."
            Task {
                try? await account.authenticated { token in
                    let service = SocialService(configuration: account.configuration)
                    try await service.save(shareId: share.id, accessToken: token)
                    try await service.logActivity(action: mode == .forked ? "fork_share" : "import_share", entityType: share.entityType, entityId: share.entityId, shareId: share.id, note: mode == .forked ? "Creaste una variante de \(share.name)" : "Registraste una copia de \(share.name)", accessToken: token)
                }
                await loadSocialData()
            }
        } catch { message = error.localizedDescription }
    }
    private func block(_ share: SocialShare) { socialAction(successMessage: "Usuario bloqueado.") { try await $0.block(userId: share.ownerId, accessToken: $1) } }
    private func unblock(_ userId: UUID) { socialAction(successMessage: "Usuario desbloqueado.") { try await $0.unblock(userId: userId, accessToken: $1) } }
    private func toggleLike(_ share: SocialShare) {
        let wasLiked = likedShareIds.contains(share.id)
        socialAction(successMessage: wasLiked ? "Se quitó Me gusta." : "Marcaste Me gusta.") { service, token in
            if wasLiked { try await service.unlike(shareId: share.id, accessToken: token) }
            else { try await service.like(shareId: share.id, accessToken: token) }
        }
    }
    private func toggleSaved(_ share: SocialShare) {
        let wasSaved = savedShareIds.contains(share.id)
        socialAction(successMessage: wasSaved ? "Se quitó de guardados." : "Publicación guardada.") { service, token in
            if wasSaved { try await service.unsave(shareId: share.id, accessToken: token) }
            else { try await service.save(shareId: share.id, accessToken: token) }
        }
    }
    private func socialAction(successMessage: String = "Acción completada.", _ operation: @escaping (SocialService, String) async throws -> Void) {
        Task {
            do {
                try await account.authenticated { token in try await operation(SocialService(configuration: account.configuration), token) }
                message = successMessage; await loadSocialData()
            } catch { message = error.localizedDescription }
        }
    }
    private func markRead(_ item: SocialInboxItem) {
        guard item.readAt == nil else { return }
        Task {
            do {
                try await account.authenticated { token in try await SocialService(configuration: account.configuration).markInboxRead(itemId: item.id, accessToken: token) }
                if let index = inbox.firstIndex(where: { $0.id == item.id }) {
                    inbox[index] = .init(id: item.id, shareId: item.shareId, targetUserId: item.targetUserId, readAt: ISO8601DateFormatter().string(from: .now), createdAt: item.createdAt, share: item.share)
                }
            } catch { /* El servidor conserva el elemento como no leído para reintentar. */ }
        }
    }
    private func activityTitle(_ action: String) -> String {
        switch action {
        case "share_recipe": "Publicaste una receta"
        case "share_technique": "Publicaste una técnica"
        case "import_share": "Registraste una copia"
        case "fork_share": "Creaste una variante"
        default: "Operación social registrada"
        }
    }
    private func synchronize() {
        Task {
            guard let tokens = await account.validTokens() else { return }
            let coordinator = EntitySyncCoordinator(context: context, configuration: account.configuration)
            await coordinator.sync(ownerId: tokens.userId, accessToken: tokens.accessToken)
            if coordinator.authenticationRejected, let refreshed = await account.validTokens(forceRefresh: true) {
                await coordinator.sync(ownerId: refreshed.userId, accessToken: refreshed.accessToken)
            }
            switch coordinator.state {
            case .completed: message = "Datos sincronizados."
            case .offline: message = "Sin backend: los cambios siguen guardados offline."
            case let .failed(error): message = error
            default: break
            }
            loadProfile(); await loadSocialData()
        }
    }

    private var profileValidationError: String? {
        do { _ = try ProfileInputValidator.validate(displayName: displayName, alias: alias, biography: biography, avatarColor: avatarColor, favoriteMethods: favoriteMethods); return nil }
        catch { return error.localizedDescription }
    }
    private var profileInitials: String {
        let parts = displayName.split(whereSeparator: { $0.isWhitespace }).prefix(2)
        let value = parts.compactMap(\.first).map(String.init).joined().uppercased()
        return value.isEmpty ? "C" : value
    }
    private var profileAvatarColor: Color { color(avatarColor) }
    private func color(_ hex: String) -> Color {
        let normalized = hex.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        return Color(hex: UInt(normalized, radix: 16) ?? 0x3F7A63)
    }
}

private struct ShareComposer: View {
    let title: String
    let allowsPublic: Bool
    let onPublish: (String, String, UUID?) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var message = ""
    @State private var visibility: String
    @State private var recipientText = ""

    private var recipient: UUID? { UUID(uuidString: recipientText.trimmingCharacters(in: .whitespacesAndNewlines)) }
    private var canPublish: Bool { visibility == "PUBLIC" || recipient != nil }

    init(title: String, allowsPublic: Bool, onPublish: @escaping (String, String, UUID?) -> Void) {
        self.title = title; self.allowsPublic = allowsPublic; self.onPublish = onPublish
        _visibility = State(initialValue: allowsPublic ? "PUBLIC" : "DIRECT")
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Fórmula") { Text(title).font(.headline) }
                Section("Destino") {
                    Picker("Compartir en", selection: $visibility) {
                        if allowsPublic { Text("Muro público").tag("PUBLIC") }
                        Text("Buzón directo").tag("DIRECT")
                    }.pickerStyle(.segmented)
                    if !allowsPublic { Text("Tu perfil privado sólo permite compartir directamente.").font(.caption).foregroundStyle(.secondary) }
                    if visibility == "DIRECT" {
                        TextField("UUID del destinatario", text: $recipientText)
                            .textInputAutocapitalization(.never).autocorrectionDisabled()
                        Text("El identificador debe ser el UUID exacto de la cuenta destinataria.")
                            .font(.caption).foregroundStyle(.secondary)
                        if !recipientText.isEmpty, recipient == nil { Text("El UUID no es válido.").font(.caption).foregroundStyle(.red) }
                    }
                }
                Section("Mensaje opcional") {
                    TextField("¿Qué debería saber quien la recibe?", text: $message, axis: .vertical).lineLimit(2...6)
                        .onChange(of: message) { _, value in
                            if value.count > SocialContentPolicy.messageLimit { message = String(value.prefix(SocialContentPolicy.messageLimit)) }
                        }
                    Text("\(message.count)/\(SocialContentPolicy.messageLimit)").font(.caption2).foregroundStyle(.secondary)
                }
                Section {
                    Text(visibility == "PUBLIC" ? "Cualquier usuario autenticado podrá ver e importar esta fórmula." : "Sólo la cuenta destinataria y tú podrán verla.")
                        .font(.caption).foregroundStyle(.secondary)
                    if let communityURL = configuredHTTPSURL(for: "COMMUNITY_GUIDELINES_URL") { Link("Consultar normas de la comunidad", destination: communityURL).font(.caption) }
                }
            }
            .brewScrollableCanvas()
            .navigationTitle("Compartir fórmula")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Compartir") { onPublish(message, visibility, visibility == "DIRECT" ? recipient : nil); dismiss() }
                        .disabled(!canPublish)
                }
            }
        }
    }
}

private struct ReportComposer: View {
    let shareName: String
    let onSubmit: (SocialReportReason, String) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var reason = SocialReportReason.harassment
    @State private var details = ""

    var body: some View {
        NavigationStack {
            Form {
                Section("Publicación") { Text(shareName).font(.headline) }
                Section("Motivo") { Picker("Motivo", selection: $reason) { ForEach(SocialReportReason.allCases) { Text($0.label).tag($0) } } }
                Section("Detalles opcionales") {
                    TextField("Explica qué ocurrió", text: $details, axis: .vertical).lineLimit(3...7)
                    Text("\(details.count)/450").font(.caption2).foregroundStyle(details.count > 450 ? .red : .secondary)
                }
                Section { Text("El reporte se enviará para revisión. Si existe peligro inmediato, contacta a los servicios de emergencia locales.").font(.caption).foregroundStyle(.secondary) }
            }
            .brewScrollableCanvas()
            .navigationTitle("Reportar contenido").navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Enviar") { onSubmit(reason, details); dismiss() }.disabled(details.count > 450) }
            }
        }
    }
}

private func configuredHTTPSURL(for key: String) -> URL? {
    guard let value = Bundle.main.object(forInfoDictionaryKey: key) as? String,
          let url = URL(string: value), url.scheme?.lowercased() == "https" else { return nil }
    return url
}
