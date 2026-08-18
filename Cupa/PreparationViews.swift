import CoreData
import SwiftUI

struct PreparationExecutionView: View {
    @Environment(\.managedObjectContext) private var context
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \TechniqueRecord.name, ascending: true)], predicate: NSPredicate(format: "deletedAt == nil")) private var techniques: FetchedResults<TechniqueRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \CoffeeBeanRecord.name, ascending: true)], predicate: NSPredicate(format: "deletedAt == nil")) private var beans: FetchedResults<CoffeeBeanRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \GrinderRecord.name, ascending: true)], predicate: NSPredicate(format: "deletedAt == nil")) private var grinders: FetchedResults<GrinderRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \RecipeRecord.name, ascending: true)], predicate: NSPredicate(format: "deletedAt == nil")) private var recipes: FetchedResults<RecipeRecord>
    @ObservedObject var model: PreparationModel
    @State private var selectedTechniqueId: UUID?; @State private var errorMessage: String?; @State private var savedConfirmation = false

    var body: some View {
        VStack(spacing: 16) {
            CupaCard {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Técnica de preparación").font(.headline)
                    if techniques.isEmpty {
                        Text("Aún no hay técnicas guardadas. Puedes iniciar una preparación libre desde la calculadora o crear una en Almacén.")
                            .font(.caption).foregroundStyle(CupaTheme.secondaryText)
                    } else {
                        Picker("Técnica", selection: $selectedTechniqueId) {
                            Text("Selecciona una técnica").tag(Optional<UUID>.none)
                            ForEach(techniques) { Text($0.name).tag(Optional($0.id)) }
                        }
                        Button("Cargar técnica", action: loadTechnique).buttonStyle(.bordered).disabled(selectedTechniqueId == nil)
                    }
                }
            }

            CupaCard {
                VStack(spacing: 14) {
                    HStack {
                        VStack(alignment: .leading) {
                            Text(model.state.techniqueName).font(.headline)
                            Text("\(model.state.methodName) · \(model.state.doseGrams.formatted(.number.precision(.fractionLength(0...1)))) g · \(model.state.waterMl) ml")
                                .font(.caption).foregroundStyle(CupaTheme.secondaryText)
                            if let bean = beans.first(where: { $0.id == model.state.beanId }) {
                                Label(bean.name, systemImage: "leaf")
                                    .font(.caption.bold()).foregroundStyle(CupaTheme.forest)
                            }
                        }
                        Spacer()
                        Text("1:\(model.state.ratio.formatted(.number.precision(.fractionLength(0...1))))").font(.subheadline.bold()).foregroundStyle(CupaTheme.forest)
                    }
                    Text(timeString(model.state.elapsedSeconds)).font(.system(size: 52, weight: .bold, design: .rounded)).monospacedDigit()
                        .minimumScaleFactor(0.6)
                        .accessibilityLabel("Tiempo transcurrido")
                        .accessibilityValue(timeString(model.state.elapsedSeconds))
                    if let step = model.activeStep {
                        VStack(spacing: 8) {
                            Text("PASO \(step.number) DE \(model.state.steps.count)").font(.caption2.bold()).tracking(1).foregroundStyle(CupaTheme.secondaryText)
                            Text(step.title).font(.title3.bold()).multilineTextAlignment(.center)
                            HStack { Label("\(step.waterAddedMl) ml", systemImage: "drop"); Label("\(step.waterAccumulatedMl) ml total", systemImage: "sum") }.font(.caption)
                            Text(step.gesture.replacingOccurrences(of: "_", with: " ").capitalized + " · " + step.intensity.capitalized).font(.caption.bold()).foregroundStyle(CupaTheme.forest)
                            if !step.note.isEmpty { Text(step.note).font(.caption).foregroundStyle(CupaTheme.secondaryText).multilineTextAlignment(.center) }
                            ProgressView(value: Double(min(model.stepElapsed, max(1, step.durationSeconds))), total: Double(max(1, step.durationSeconds))).tint(CupaTheme.terracotta)
                        }
                    } else {
                        Text("Carga una técnica o usa los datos de la calculadora.").font(.caption).foregroundStyle(CupaTheme.secondaryText)
                    }
                    controls
                }.frame(maxWidth: .infinity)
            }
        }
        .alert("Preparación guardada", isPresented: $savedConfirmation) { Button("Aceptar") {} } message: { Text("La sesión y sus snapshots quedaron disponibles offline.") }
        .alert("Error", isPresented: Binding(get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })) { Button("Aceptar") {} } message: { Text(errorMessage ?? "") }
    }

    @ViewBuilder private var controls: some View {
        HStack {
            Button { model.previousStep() } label: { Image(systemName: "backward.end") }
                .frame(minWidth: 44, minHeight: 44).accessibilityLabel("Paso anterior")
                .disabled(model.state.activeStepIndex == 0)
            switch model.state.status {
            case .ready: Button("Iniciar", action: model.start).buttonStyle(.borderedProminent).tint(CupaTheme.forest).disabled(model.state.steps.isEmpty)
            case .running: Button("Pausar", action: model.pause).buttonStyle(.borderedProminent).tint(CupaTheme.terracotta)
            case .paused: Button("Reanudar", action: model.resume).buttonStyle(.borderedProminent).tint(CupaTheme.forest)
            case .completed: Label("Finalizada", systemImage: "checkmark.circle.fill").foregroundStyle(CupaTheme.forest)
            }
            Button { model.nextStep() } label: { Image(systemName: "forward.end") }
                .frame(minWidth: 44, minHeight: 44).accessibilityLabel("Paso siguiente")
                .disabled(model.state.activeStepIndex >= model.state.steps.count - 1)
            Button { model.reset() } label: { Image(systemName: "arrow.counterclockwise") }
                .frame(minWidth: 44, minHeight: 44).accessibilityLabel("Reiniciar preparación")
                .disabled(model.state.steps.isEmpty)
        }
        if model.state.elapsedSeconds > 0 && model.state.savedAt == nil {
            Button(model.state.status == .completed ? "Guardar sesión finalizada" : "Finalizar y guardar sesión", action: finish).buttonStyle(.bordered).tint(CupaTheme.forest)
                .accessibilityIdentifier("preparation.finish")
        }
    }

    private func loadTechnique() {
        guard let id = selectedTechniqueId, let technique = techniques.first(where: { $0.id == id }) else { return }
        do { model.load(technique: technique, steps: try RecipeTechniqueRepository(context: context).techniqueSteps(techniqueId: id)) }
        catch { errorMessage = error.localizedDescription }
    }
    private func finish() {
        let recipeName = recipes.first(where: { $0.id == model.state.recipeId })?.name ?? ""
        let beanName = beans.first(where: { $0.id == model.state.beanId })?.name ?? ""
        let grinderName = grinders.first(where: { $0.id == model.state.grinderId })?.name ?? ""
        _ = BrewSessionRecord(context: context, state: model.state, recipeName: recipeName, beanName: beanName, grinderName: grinderName)
        do { try context.save(); model.markSaved(); savedConfirmation = true } catch { context.rollback(); errorMessage = error.localizedDescription }
    }
    private func timeString(_ seconds: Int) -> String { String(format: "%02d:%02d", seconds / 60, seconds % 60) }
}
