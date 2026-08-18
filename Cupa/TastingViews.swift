import CoreData
import SwiftUI

struct TastingView: View {
    @Environment(\.managedObjectContext) private var context
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \TastingRecord.evaluatedAt, ascending: false)], predicate: NSPredicate(format: "deletedAt == nil"), animation: .default) private var tastings: FetchedResults<TastingRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \TastingObservationRecord.elapsedSeconds, ascending: true)], predicate: NSPredicate(format: "deletedAt == nil")) private var persistedObservations: FetchedResults<TastingObservationRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \BrewSessionRecord.completedAt, ascending: false)], predicate: NSPredicate(format: "deletedAt == nil")) private var brews: FetchedResults<BrewSessionRecord>
    @ObservedObject var model: TastingModel
    @ObservedObject var lab: LabModel
    @Binding var selection: CupaTab
    @State private var message: String?; @State private var editingExisting = false
    @State private var selectedTasting: TastingRecord?; @State private var pendingEdit: TastingRecord?
    @State private var confirmingCoolingReset = false

    var body: some View {
        ZStack {
            CupaTheme.background.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 16) {
                    SectionHeader(eyebrow: "Evaluación sensorial", title: "Cata artesanal", subtitle: "Registra cómo cambia la taza mientras se enfría.")
                    coolingCard
                    linksCard
                    flavorCard
                    scoresCard
                    attributesCard
                    finalCard
                    actions
                    if !tastings.isEmpty { historyCard }
                }.padding()
            }
        }
        .navigationTitle("Cata").navigationBarTitleDisplayMode(.inline)
        .sheet(item: $selectedTasting, onDismiss: {
            if let pendingEdit { edit(pendingEdit); self.pendingEdit = nil }
        }) { tasting in
            TastingDetailView(
                tasting: tasting,
                observations: persistedObservations.filter { $0.tastingId == tasting.id },
                brew: brews.first { $0.id == tasting.brewSessionId },
                onEdit: { pendingEdit = tasting; selectedTasting = nil },
                onDelete: { remove(tasting); selectedTasting = nil }
            )
        }
        .confirmationDialog("¿Reiniciar el seguimiento?", isPresented: $confirmingCoolingReset, titleVisibility: .visible) {
            Button("Reiniciar tiempo y observaciones", role: .destructive, action: model.reset)
            Button("Cancelar", role: .cancel) {}
        } message: {
            Text("Se borrarán el tiempo y las observaciones de enfriamiento que todavía no hayas guardado.")
        }
        .alert("Cata", isPresented: Binding(get: { message != nil }, set: { if !$0 { message = nil } })) { Button("Aceptar") {} } message: { Text(message ?? "") }
    }

    private var coolingCard: some View {
        CupaCard {
            VStack(spacing: 12) {
                HStack { Label("LA VIDA DE LA TAZA", systemImage: "mug.fill").font(.caption.bold()); Spacer(); Text(time(model.state.coolingElapsedSeconds)).monospacedDigit().bold() }
                Text(model.stageLabel).font(.headline).multilineTextAlignment(.center)
                ProgressView(value: min(Double(model.state.coolingElapsedSeconds), 960), total: 960).tint(CupaTheme.terracotta)
                HStack {
                    switch model.state.coolingStatus {
                    case .ready: Button("Iniciar", action: model.start).buttonStyle(.borderedProminent).tint(CupaTheme.forest).foregroundStyle(CupaTheme.onAccent).accessibilityIdentifier("tasting.cooling.start")
                    case .running: Button("Pausar", action: model.pause).buttonStyle(.borderedProminent).tint(CupaTheme.terracotta).foregroundStyle(CupaTheme.onAccent).accessibilityIdentifier("tasting.cooling.pause")
                    case .paused: Button("Reanudar", action: model.resume).buttonStyle(.borderedProminent).tint(CupaTheme.forest).foregroundStyle(CupaTheme.onAccent).accessibilityIdentifier("tasting.cooling.resume")
                    case .completed: Label("Guardada", systemImage: "checkmark.circle.fill").foregroundStyle(CupaTheme.forest)
                    }
                    Button("Reiniciar", action: requestCoolingReset).buttonStyle(.bordered)
                        .disabled(model.state.coolingStatus == .completed)
                        .accessibilityIdentifier("tasting.cooling.reset")
                    Button("Registrar etapa", action: model.addObservation).buttonStyle(.bordered)
                        .disabled(model.state.coolingStatus == .ready || model.state.coolingStatus == .completed)
                        .accessibilityIdentifier("tasting.cooling.observe")
                }.font(.caption)
                if !model.state.observations.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("\(model.state.observations.count) observaciones durante el enfriamiento").font(.caption.bold()).foregroundStyle(CupaTheme.secondaryText)
                        ForEach(model.state.observations) { observation in
                            HStack(alignment: .top, spacing: 8) {
                                VStack(alignment: .leading, spacing: 3) {
                                    Text("\(observationStageLabel(observation.stage)) · \(time(observation.elapsedSeconds))").font(.caption.bold())
                                    Text("A \(Int(observation.aroma)) · Ac \(Int(observation.acidity)) · D \(Int(observation.sweetness)) · C \(Int(observation.body)) · Am \(Int(observation.bitterness)) · F \(Int(observation.finish))")
                                        .font(.caption2).foregroundStyle(CupaTheme.forest)
                                    if !observation.notes.isEmpty { Text(observation.notes).font(.caption2).foregroundStyle(CupaTheme.secondaryText) }
                                }
                                Spacer()
                                Button(role: .destructive) { model.removeObservation(id: observation.id) } label: { Image(systemName: "trash") }
                                    .disabled(model.state.coolingStatus == .completed)
                                    .accessibilityLabel("Eliminar observación de \(observationStageLabel(observation.stage))")
                            }
                        }
                    }.frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
    }

    private var linksCard: some View {
        CupaCard {
            VStack(alignment: .leading, spacing: 8) {
                Text("Preparación relacionada").font(.headline)
                Picker("Sesión", selection: $model.state.brewSessionId) {
                    Text("Cata independiente").tag(Optional<UUID>.none)
                    ForEach(brews) { Text("\($0.techniqueNameSnapshot) · \($0.completedAt.formatted(date: .abbreviated, time: .shortened))").tag(Optional($0.id)) }
                }
            }
        }
    }

    private var flavorCard: some View {
        CupaCard {
            VStack(alignment: .leading, spacing: 12) {
                Text("Rueda de sabor").font(.headline)
                Picker("Familia", selection: $model.state.activeFlavorFamily) { ForEach(FlavorFamily.allCases) { Text($0.label).tag($0) } }.pickerStyle(.menu)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack {
                        ForEach(model.state.activeFlavorFamily.suggestions, id: \.self) { note in
                            Button(note) { toggle(note) }.buttonStyle(.bordered).tint(model.state.selectedFlavorNotes.contains(note) ? CupaTheme.forest : CupaTheme.secondaryText)
                        }
                    }
                }
                if !model.state.selectedFlavorNotes.isEmpty { Text(model.state.selectedFlavorNotes.joined(separator: ", ")).font(.caption).foregroundStyle(CupaTheme.forest) }
                TextField("Notas esperadas", text: $model.state.expectedNotes)
                TextField("Notas libres y balance general", text: $model.state.freeNotes, axis: .vertical).lineLimit(3...7)
            }
        }
    }

    private var scoresCard: some View {
        CupaCard {
            VStack(spacing: 12) {
                Text("Perfil sensorial").font(.headline).frame(maxWidth: .infinity, alignment: .leading)
                score("Aroma", $model.state.aroma); score("Acidez", $model.state.acidity); score("Dulzor", $model.state.sweetness)
                score("Cuerpo", $model.state.body); score("Amargor", $model.state.bitterness); score("Final", $model.state.finish)
            }
        }
    }

    private var attributesCard: some View {
        CupaCard {
            VStack(alignment: .leading, spacing: 10) {
                Text("Textura y atributos").font(.headline)
                Picker("Textura", selection: $model.state.texture) { ForEach(["ligera", "sedosa", "jugosa", "densa", "seca"], id: \.self) { Text($0.capitalized) } }
                Picker("Limpieza", selection: $model.state.cleanliness) { ForEach(["baja", "media", "alta", "muy alta"], id: \.self) { Text($0.capitalized) } }
                Picker("Persistencia", selection: $model.state.persistence) { ForEach(["corta", "media", "larga"], id: \.self) { Text($0.capitalized) } }
            }
        }
    }

    private var finalCard: some View {
        CupaCard {
            VStack(spacing: 12) {
                Text("Calificación final").font(.headline).frame(maxWidth: .infinity, alignment: .leading)
                HStack { Text("Estrellas"); Slider(value: $model.state.rating, in: 1...5, step: 1); Text("\(Int(model.state.rating))/5").bold() }
                Stepper("Recomendación (NPS): \(model.state.nps)/10", value: $model.state.nps, in: 0...10)
            }
        }
    }

    private var actions: some View {
        HStack {
            Button(saveButtonTitle, action: save).buttonStyle(.borderedProminent).tint(CupaTheme.forest).foregroundStyle(CupaTheme.onAccent)
                .disabled(!editingExisting && model.state.coolingStatus == .completed)
                .accessibilityIdentifier("tasting.save")
            Button("Nueva") { model.newTasting(); editingExisting = false }.buttonStyle(.bordered)
            Button("Llevar al Laboratorio") {
                lab.load(tasting: model.state, brew: brews.first { $0.id == model.state.brewSessionId })
                selection = .lab
            }.buttonStyle(.bordered)
        }.font(.caption)
    }

    private var historyCard: some View {
        CupaCard {
            VStack(alignment: .leading, spacing: 10) {
                HStack { Text("Historial").font(.headline); Spacer(); Text("\(tastings.count)").font(.caption.bold()).foregroundStyle(CupaTheme.secondaryText) }
                ForEach(tastings) { tasting in
                    HStack {
                        Button { selectedTasting = tasting } label: {
                            VStack(alignment: .leading) {
                                Text("\(FlavorFamily(rawValue: tasting.activeFlavorFamily)?.label ?? "Cata") · \(Int(tasting.rating))/5").font(.subheadline.bold())
                                Text("\(tasting.evaluatedAt.formatted(date: .abbreviated, time: .shortened)) · \(tasting.cupLifeState.capitalized)").font(.caption).foregroundStyle(CupaTheme.secondaryText)
                            }
                        }.buttonStyle(.plain)
                        Spacer(); Image(systemName: "chevron.right").font(.caption).foregroundStyle(CupaTheme.secondaryText)
                    }
                }
            }
        }
    }

    private func score(_ name: String, _ value: Binding<Double>) -> some View {
        ViewThatFits(in: .horizontal) {
            HStack {
                Text(name).frame(minWidth: 70, alignment: .leading)
                Slider(value: value, in: 1...5, step: 1).tint(CupaTheme.terracotta)
                    .accessibilityLabel(name).accessibilityValue("\(Int(value.wrappedValue)) de 5")
                Text("\(Int(value.wrappedValue))/5").monospacedDigit()
            }
            VStack(alignment: .leading, spacing: 4) {
                HStack { Text(name); Spacer(); Text("\(Int(value.wrappedValue))/5").monospacedDigit() }
                Slider(value: value, in: 1...5, step: 1).tint(CupaTheme.terracotta)
                    .accessibilityLabel(name).accessibilityValue("\(Int(value.wrappedValue)) de 5")
            }
        }
    }
    private func toggle(_ note: String) { if let index = model.state.selectedFlavorNotes.firstIndex(of: note) { model.state.selectedFlavorNotes.remove(at: index) } else { model.state.selectedFlavorNotes.append(note) } }
    private func save() {
        if model.state.coolingStatus == .running { model.pause() }
        let brew = brews.first { $0.id == model.state.brewSessionId }
        do { _ = try TastingRepository(context: context).save(model.state, brew: brew); model.markSaved(); editingExisting = false; message = "Cata, observaciones y taza guardadas offline." }
        catch { context.rollback(); message = error.localizedDescription }
    }
    private func edit(_ tasting: TastingRecord) { do { model.load(record: tasting, observations: try TastingRepository(context: context).observations(tastingId: tasting.id)); editingExisting = true } catch { message = error.localizedDescription } }
    private func remove(_ tasting: TastingRecord) { do { try TastingRepository(context: context).delete(tasting) } catch { context.rollback(); message = error.localizedDescription } }
    private var saveButtonTitle: String {
        if editingExisting { return "Actualizar cata" }
        return model.state.coolingStatus == .completed ? "Guardada" : "Guardar cata"
    }
    private func requestCoolingReset() {
        guard model.state.coolingStatus != .completed else { return }
        if model.state.coolingElapsedSeconds > 0 || !model.state.observations.isEmpty { confirmingCoolingReset = true }
        else { model.reset() }
    }
    private func observationStageLabel(_ code: String) -> String {
        switch code { case "HOT": "Caliente"; case "PEAK": "Pico"; case "DECLINING": "Descenso"; default: "Agotada" }
    }
    private func time(_ seconds: Int) -> String { String(format: "%02d:%02d", seconds / 60, seconds % 60) }
}

private struct TastingDetailView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var tasting: TastingRecord
    let observations: [TastingObservationRecord]
    let brew: BrewSessionRecord?
    let onEdit: () -> Void
    let onDelete: () -> Void
    @State private var confirmingDelete = false

    var body: some View {
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 9) {
                        HStack {
                            Image(systemName: "heart.circle.fill").font(.title2).foregroundStyle(CupaTheme.terracotta)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(FlavorFamily(rawValue: tasting.activeFlavorFamily)?.label ?? "Cata").font(.title3.bold())
                                Text(tasting.evaluatedAt.formatted(date: .long, time: .shortened)).font(.caption).foregroundStyle(CupaTheme.secondaryText)
                            }
                            Spacer(); Text("\(Int(tasting.rating))/5 ★").font(.headline).foregroundStyle(CupaTheme.gold)
                        }
                        if !tasting.selectedFlavorNotes.isEmpty { Label(tasting.selectedFlavorNotes.joined(separator: ", "), systemImage: "circle.hexagongrid") }
                        if !tasting.expectedNotes.isEmpty { Text("Esperadas: \(tasting.expectedNotes)").font(.subheadline).foregroundStyle(CupaTheme.secondaryText) }
                    }.padding(.vertical, 4)
                }

                if let brew {
                    Section("Preparación vinculada") {
                        detailRow("Técnica", brew.techniqueNameSnapshot)
                        if !brew.beanNameSnapshot.isEmpty { detailRow("Café", brew.beanNameSnapshot) }
                        detailRow("Extracción", "\(brew.doseGrams.formatted(.number.precision(.fractionLength(0...1)))) g · \(brew.waterMl) ml · 1:\(brew.ratio.formatted(.number.precision(.fractionLength(0...1))))")
                    }
                }

                Section("Perfil sensorial") {
                    sensoryRow("Aroma", tasting.aroma)
                    sensoryRow("Acidez", tasting.acidity)
                    sensoryRow("Dulzor", tasting.sweetness)
                    sensoryRow("Cuerpo", tasting.body)
                    sensoryRow("Amargor", tasting.bitterness)
                    sensoryRow("Final", tasting.finishScore)
                }

                Section("Evaluación") {
                    detailRow("Textura", tasting.texture.capitalized)
                    detailRow("Limpieza", tasting.cleanliness.capitalized)
                    detailRow("Persistencia", tasting.persistence.capitalized)
                    detailRow("Recomendación", "\(tasting.nps)/10")
                    detailRow("Vida de taza", "\(time(Int(tasting.coolingElapsedSeconds))) · \(cupLifeLabel(tasting.cupLifeState))")
                    if !tasting.evaluatorNotes.isEmpty { Text(tasting.evaluatorNotes) }
                }

                Section("Evolución durante el enfriamiento") {
                    if observations.isEmpty {
                        Text("No se registraron observaciones por etapa.").foregroundStyle(CupaTheme.secondaryText)
                    } else {
                        ForEach(observations) { observation in
                            VStack(alignment: .leading, spacing: 5) {
                                HStack { Text(stageLabel(observation.stage)).fontWeight(.semibold); Spacer(); Text(time(Int(observation.elapsedSeconds))).monospacedDigit().font(.caption) }
                                Text("A \(Int(observation.aroma)) · Ac \(Int(observation.acidity)) · D \(Int(observation.sweetness)) · C \(Int(observation.body)) · Am \(Int(observation.bitterness)) · F \(Int(observation.finishScore))")
                                    .font(.caption).foregroundStyle(CupaTheme.forest)
                                if !observation.notes.isEmpty { Text(observation.notes).font(.caption).foregroundStyle(CupaTheme.secondaryText) }
                            }.padding(.vertical, 3)
                        }
                    }
                }

                Section {
                    Button(role: .destructive) { confirmingDelete = true } label: { Label("Eliminar cata", systemImage: "trash") }
                }
            }
            .navigationTitle("Detalle de cata")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cerrar") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Editar", action: onEdit).accessibilityIdentifier("tasting.detail.edit") }
            }
            .confirmationDialog("¿Eliminar esta cata?", isPresented: $confirmingDelete, titleVisibility: .visible) {
                Button("Eliminar cata", role: .destructive, action: onDelete)
                Button("Cancelar", role: .cancel) {}
            } message: {
                Text("También se quitarán sus observaciones y la taza asociada; las preparaciones originales se conservarán.")
            }
        }
    }

    private func sensoryRow(_ name: String, _ value: Double) -> some View {
        HStack { Text(name); Spacer(); Text("\(Int(value))/5").fontWeight(.semibold).foregroundStyle(CupaTheme.forest) }
    }
    private func detailRow(_ name: String, _ value: String) -> some View {
        HStack(alignment: .top) { Text(name); Spacer(); Text(value).multilineTextAlignment(.trailing).fontWeight(.semibold).foregroundStyle(CupaTheme.forest) }
    }
    private func time(_ seconds: Int) -> String { String(format: "%02d:%02d", seconds / 60, seconds % 60) }
    private func stageLabel(_ code: String) -> String {
        switch code { case "HOT": "Caliente"; case "PEAK": "Pico"; case "DECLINING": "Descenso"; default: "Agotada" }
    }
    private func cupLifeLabel(_ code: String) -> String {
        switch code { case "FRESH": "Caliente"; case "PEAK": "Pico"; case "DECLINING": "Descenso"; default: "Agotada" }
    }
}
