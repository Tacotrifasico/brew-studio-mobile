import CoreData
import SwiftUI

struct TastingView: View {
    @Environment(\.managedObjectContext) private var context
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \TastingRecord.evaluatedAt, ascending: false)], predicate: NSPredicate(format: "deletedAt == nil"), animation: .default) private var tastings: FetchedResults<TastingRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \BrewSessionRecord.completedAt, ascending: false)], predicate: NSPredicate(format: "deletedAt == nil")) private var brews: FetchedResults<BrewSessionRecord>
    @ObservedObject var model: TastingModel
    @Binding var selection: CupaTab
    @State private var message: String?; @State private var editingExisting = false

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
                    case .ready: Button("Iniciar", action: model.start).buttonStyle(.borderedProminent).tint(CupaTheme.forest)
                    case .running: Button("Pausar", action: model.pause).buttonStyle(.borderedProminent).tint(CupaTheme.terracotta)
                    case .paused: Button("Reanudar", action: model.resume).buttonStyle(.borderedProminent).tint(CupaTheme.forest)
                    case .completed: Label("Guardada", systemImage: "checkmark.circle.fill").foregroundStyle(CupaTheme.forest)
                    }
                    Button("Reiniciar", action: model.reset).buttonStyle(.bordered)
                    Button("Registrar etapa", action: model.addObservation).buttonStyle(.bordered).disabled(model.state.coolingStatus == .ready)
                }.font(.caption)
                if !model.state.observations.isEmpty { Text("\(model.state.observations.count) observaciones durante el enfriamiento").font(.caption).foregroundStyle(CupaTheme.secondaryText) }
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
            Button(editingExisting ? "Actualizar cata" : "Guardar cata", action: save).buttonStyle(.borderedProminent).tint(CupaTheme.forest)
            Button("Nueva") { model.newTasting(); editingExisting = false }.buttonStyle(.bordered)
            Button("Llevar al Laboratorio") { selection = .lab }.buttonStyle(.bordered)
        }.font(.caption)
    }

    private var historyCard: some View {
        CupaCard {
            VStack(alignment: .leading, spacing: 10) {
                Text("Historial").font(.headline)
                ForEach(tastings.prefix(8)) { tasting in
                    HStack {
                        Button { edit(tasting) } label: {
                            VStack(alignment: .leading) {
                                Text("\(FlavorFamily(rawValue: tasting.activeFlavorFamily)?.label ?? "Cata") · \(Int(tasting.rating))/5").font(.subheadline.bold())
                                Text("\(tasting.evaluatedAt.formatted(date: .abbreviated, time: .shortened)) · \(tasting.cupLifeState.capitalized)").font(.caption).foregroundStyle(CupaTheme.secondaryText)
                            }
                        }.buttonStyle(.plain)
                        Spacer(); Button(role: .destructive) { remove(tasting) } label: { Image(systemName: "trash") }
                    }
                }
            }
        }
    }

    private func score(_ name: String, _ value: Binding<Double>) -> some View { HStack { Text(name).frame(width: 70, alignment: .leading); Slider(value: value, in: 1...5, step: 1).tint(CupaTheme.terracotta); Text("\(Int(value.wrappedValue))/5").monospacedDigit() } }
    private func toggle(_ note: String) { if let index = model.state.selectedFlavorNotes.firstIndex(of: note) { model.state.selectedFlavorNotes.remove(at: index) } else { model.state.selectedFlavorNotes.append(note) } }
    private func save() {
        let brew = brews.first { $0.id == model.state.brewSessionId }
        do { _ = try TastingRepository(context: context).save(model.state, brew: brew); model.markSaved(); editingExisting = false; message = "Cata, observaciones y taza guardadas offline." }
        catch { context.rollback(); message = error.localizedDescription }
    }
    private func edit(_ tasting: TastingRecord) { do { model.load(record: tasting, observations: try TastingRepository(context: context).observations(tastingId: tasting.id)); editingExisting = true } catch { message = error.localizedDescription } }
    private func remove(_ tasting: TastingRecord) { do { try TastingRepository(context: context).delete(tasting) } catch { context.rollback(); message = error.localizedDescription } }
    private func time(_ seconds: Int) -> String { String(format: "%02d:%02d", seconds / 60, seconds % 60) }
}
