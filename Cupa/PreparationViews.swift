import CoreData
import SwiftUI

struct PreparationExecutionView: View {
    @Environment(\.managedObjectContext) private var context
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \TechniqueRecord.name, ascending: true)], predicate: LocalDataScope.visiblePredicate()) private var techniques: FetchedResults<TechniqueRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \CoffeeBeanRecord.name, ascending: true)], predicate: LocalDataScope.visiblePredicate()) private var beans: FetchedResults<CoffeeBeanRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \GrinderRecord.name, ascending: true)], predicate: LocalDataScope.visiblePredicate()) private var grinders: FetchedResults<GrinderRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \RecipeRecord.name, ascending: true)], predicate: LocalDataScope.visiblePredicate()) private var recipes: FetchedResults<RecipeRecord>
    @ObservedObject var model: PreparationModel
    let onFinished: ((UUID) -> Void)?
    @State private var selectedTechniqueKey: String?; @State private var errorMessage: String?; @State private var savedConfirmation = false
    @State private var confirmingReset = false; @State private var isSaving = false

    init(model: PreparationModel, onFinished: ((UUID) -> Void)? = nil) {
        self.model = model
        self.onFinished = onFinished
    }

    var body: some View {
        VStack(spacing: 16) {
            executionCard
            if model.state.status == .ready { techniqueLibraryCard }
        }
        .alert("Preparación guardada", isPresented: $savedConfirmation) { Button("Aceptar") {} } message: { Text("La sesión y sus snapshots quedaron disponibles offline.") }
        .alert("Error", isPresented: Binding(get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })) { Button("Aceptar") {} } message: { Text(errorMessage ?? "") }
        .confirmationDialog("¿Reiniciar la preparación?", isPresented: $confirmingReset, titleVisibility: .visible) {
            Button("Reiniciar preparación", role: .destructive, action: model.reset)
            Button("Cancelar", role: .cancel) {}
        } message: {
            Text(model.state.savedAt == nil ? "Se borrarán el tiempo y el avance que todavía no hayas guardado." : "Se iniciará una preparación nueva con otro identificador.")
        }
    }

    private var techniqueLibraryCard: some View {
        CupaCard {
            VStack(alignment: .leading, spacing: 12) {
                Text("TÉCNICAS PARA \(model.state.methodName.uppercased())")
                    .font(.caption2.bold()).tracking(1).foregroundStyle(CupaTheme.secondaryText)
                Text("Todas usarán \(model.state.doseGrams.formatted(.number.precision(.fractionLength(0...1)))) g, \(model.state.waterMl) ml y proporción 1:\(model.state.ratio.formatted(.number.precision(.fractionLength(0...1)))).")
                    .font(.caption).foregroundStyle(CupaTheme.secondaryText)
                Picker("Técnica", selection: $selectedTechniqueKey) {
                    Text("Selecciona una técnica").tag(Optional<String>.none)
                    Section("Incluidas en Cupa") { ForEach(builtInTechniques) { item in Text(item.name).tag(Optional(item.id)) } }
                    if !matchingSavedTechniques.isEmpty {
                        Section("Mis técnicas") { ForEach(matchingSavedTechniques) { Text($0.name).tag(Optional("saved:\($0.id.uuidString)")) } }
                    }
                }
                Button("Cargar técnica", action: loadTechnique).buttonStyle(.bordered).disabled(selectedTechniqueKey == nil)
            }
        }
    }

    private var executionCard: some View {
        CupaCard {
            VStack(spacing: 14) {
                HStack {
                    VStack(alignment: .leading) {
                        Text("TÉCNICA ACTIVA").font(.caption2.bold()).tracking(1.1).opacity(0.82)
                        Text(model.state.techniqueName).font(.title3.bold())
                        Text("\(model.state.methodName) · \(model.state.doseGrams.formatted(.number.precision(.fractionLength(0...1)))) g · \(model.state.waterMl) ml")
                            .font(.caption).opacity(0.9)
                        if let bean = beans.first(where: { $0.id == model.state.beanId }) {
                            Label(bean.name, systemImage: "leaf.fill").font(.caption.bold())
                        }
                    }
                    Spacer()
                    Text("1:\(model.state.ratio.formatted(.number.precision(.fractionLength(0...1))))")
                        .font(.title3.bold().monospacedDigit())
                }
                .foregroundStyle(CupaTheme.onAccent)
                .padding(14)
                .background(LinearGradient(colors: [CupaTheme.forest, CupaTheme.terracottaSurface], startPoint: .topLeading, endPoint: .bottomTrailing))
                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))

                Text(timeString(model.state.elapsedSeconds)).font(.system(.largeTitle, design: .rounded, weight: .black)).monospacedDigit()
                    .minimumScaleFactor(0.6).accessibilityLabel("Tiempo transcurrido").accessibilityValue(timeString(model.state.elapsedSeconds))
                    .foregroundStyle(CupaTheme.espressoText)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(CupaTheme.backgroundAlt.opacity(0.82))
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                if model.state.status == .ready {
                    completeTechniqueOverview
                } else if let step = model.activeStep {
                    activeStepCard(step)
                    executionSequence
                } else {
                    Text("Selecciona una técnica para comenzar.").font(.caption).foregroundStyle(CupaTheme.secondaryText)
                }
                controls
            }.frame(maxWidth: .infinity)
        }
    }

    private var completeTechniqueOverview: some View {
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                Text("TÉCNICA COMPLETA · \(model.state.steps.count) PASOS")
                    .font(.caption2.bold()).tracking(1).foregroundStyle(CupaTheme.forestText)
                Text("Revísala antes de iniciar")
                    .font(.headline).foregroundStyle(CupaTheme.text)
                Text("“Total en báscula” es la suma acumulada al terminar cada paso.")
                    .font(.caption).foregroundStyle(CupaTheme.secondaryText)
            }
            ForEach(Array(model.state.steps.enumerated()), id: \.element.id) { index, step in
                if index > 0 { Divider().overlay(CupaTheme.border) }
                VStack(alignment: .leading, spacing: 8) {
                    HStack(spacing: 9) {
                        Text("\(index + 1)")
                            .font(.caption.bold()).foregroundStyle(CupaTheme.onAccent)
                            .frame(width: 28, height: 28)
                            .background(CupaTheme.forest)
                            .clipShape(RoundedRectangle(cornerRadius: 9, style: .continuous))
                        Text(step.title).font(.subheadline.bold()).foregroundStyle(CupaTheme.text)
                    }
                    stepMetrics(step, timeLabel: "TIEMPO", timeValue: durationString(step.durationSeconds))
                    if !step.note.isEmpty { Text(step.note).font(.caption).foregroundStyle(CupaTheme.secondaryText) }
                }
            }
        }
        .padding(14)
        .background(CupaTheme.card)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay { RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(CupaTheme.border, lineWidth: 1) }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Técnica completa con \(model.state.steps.count) pasos")
    }

    private func activeStepCard(_ step: PreparationStepSnapshot) -> some View {
        VStack(spacing: 10) {
            Text("PASO \(step.number) DE \(model.state.steps.count)").font(.caption2.bold()).tracking(1).foregroundStyle(CupaTheme.secondaryText)
            Text("AHORA · \(step.title)").font(.title3.bold()).multilineTextAlignment(.center)
            stepMetrics(
                step,
                timeLabel: "QUEDAN",
                timeValue: durationString(max(0, step.durationSeconds - model.stepElapsed))
            )
            Text(step.gesture.replacingOccurrences(of: "_", with: " ").capitalized + " · " + step.intensity.capitalized)
                .font(.caption.bold()).foregroundStyle(CupaTheme.forestText)
            if !step.note.isEmpty { Text(step.note).font(.caption).foregroundStyle(CupaTheme.secondaryText).multilineTextAlignment(.center) }
            ProgressView(value: Double(min(model.stepElapsed, max(1, step.durationSeconds))), total: Double(max(1, step.durationSeconds))).tint(CupaTheme.terracotta)
        }
        .padding(12)
        .background(LinearGradient(colors: [CupaTheme.terracotta.opacity(0.12), CupaTheme.forest.opacity(0.08)], startPoint: .leading, endPoint: .trailing))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay { RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(CupaTheme.border, lineWidth: 1) }
    }

    private var executionSequence: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("SECUENCIA COMPLETA").font(.caption2.bold()).tracking(1).foregroundStyle(CupaTheme.secondaryText)
            ForEach(Array(model.state.steps.enumerated()), id: \.element.id) { index, step in
                let isCurrent = index == model.state.activeStepIndex
                let isPast = index < model.state.activeStepIndex
                VStack(alignment: .leading, spacing: 8) {
                    HStack(spacing: 9) {
                        Image(systemName: isPast ? "checkmark" : "\(index + 1).circle.fill")
                            .foregroundStyle(isCurrent ? CupaTheme.terracottaText : isPast ? CupaTheme.forestText : CupaTheme.secondaryText)
                        Text(step.title).font(.subheadline.bold()).foregroundStyle(CupaTheme.text)
                        Spacer()
                        if isCurrent { Text("ACTIVO").font(.caption2.bold()).foregroundStyle(CupaTheme.terracottaText) }
                    }
                    stepMetrics(step, timeLabel: "TIEMPO", timeValue: durationString(step.durationSeconds))
                }
                .padding(10)
                .background(isCurrent ? CupaTheme.terracotta.opacity(0.10) : isPast ? CupaTheme.forest.opacity(0.07) : CupaTheme.backgroundAlt.opacity(0.72))
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .overlay { RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(isCurrent ? CupaTheme.terracotta : CupaTheme.border, lineWidth: isCurrent ? 1.5 : 1) }
            }
        }
    }

    private func stepMetrics(_ step: PreparationStepSnapshot, timeLabel: String, timeValue: String) -> some View {
        PreparationMetricsRow(
            added: "+\(step.waterAddedMl) ml",
            accumulated: "\(step.waterAccumulatedMl) ml",
            timeLabel: timeLabel,
            timeValue: timeValue
        )
    }

    private func durationString(_ seconds: Int) -> String {
        seconds < 60 ? "\(seconds) s" : String(format: "%d:%02d", seconds / 60, seconds % 60)
    }

    @ViewBuilder private var controls: some View {
        HStack {
            Button { model.previousStep() } label: { Image(systemName: "backward.end") }
                .frame(minWidth: 44, minHeight: 44).accessibilityLabel("Paso anterior")
                .disabled(model.state.activeStepIndex == 0)
            switch model.state.status {
            case .ready: Button("Iniciar", action: model.start).buttonStyle(.borderedProminent).tint(CupaTheme.forest).foregroundStyle(CupaTheme.onAccent).disabled(model.state.steps.isEmpty)
            case .running: Button("Pausar", action: model.pause).buttonStyle(.borderedProminent).tint(CupaTheme.terracotta).foregroundStyle(CupaTheme.onTerracotta)
            case .paused: Button("Reanudar", action: model.resume).buttonStyle(.borderedProminent).tint(CupaTheme.forest).foregroundStyle(CupaTheme.onAccent)
            case .completed: Label("Finalizada", systemImage: "checkmark.circle.fill").foregroundStyle(CupaTheme.forestText)
            }
            Button { model.nextStep() } label: { Image(systemName: "forward.end") }
                .frame(minWidth: 44, minHeight: 44).accessibilityLabel("Paso siguiente")
                .disabled(model.state.activeStepIndex >= model.state.steps.count - 1)
            Button(action: requestReset) { Image(systemName: "arrow.counterclockwise") }
                .frame(minWidth: 44, minHeight: 44).accessibilityLabel("Reiniciar preparación")
                .accessibilityIdentifier("preparation.reset")
                .disabled(model.state.steps.isEmpty)
        }
        if model.state.elapsedSeconds > 0 && model.state.savedAt == nil {
            Button(isSaving ? "Guardando…" : (model.state.status == .completed ? "Guardar sesión finalizada" : "Finalizar y guardar sesión"), action: finish)
                .buttonStyle(.bordered).tint(CupaTheme.forest).disabled(isSaving)
                .accessibilityIdentifier("preparation.finish")
        }
    }
    private func requestReset() {
        if model.state.elapsedSeconds > 0 || model.state.savedAt != nil { confirmingReset = true }
        else { model.reset() }
    }

    private func loadTechnique() {
        guard let key = selectedTechniqueKey else { return }
        if let template = builtInTechniques.first(where: { $0.id == key }) { model.load(template: template); return }
        guard key.hasPrefix("saved:"), let id = UUID(uuidString: String(key.dropFirst(6))), let technique = matchingSavedTechniques.first(where: { $0.id == id }) else { return }
        do { model.load(technique: technique, steps: try RecipeTechniqueRepository(context: context).techniqueSteps(techniqueId: id)) }
        catch { errorMessage = error.localizedDescription }
    }
    private var builtInTechniques: [PreparationTechniqueTemplate] { PreparationTechniqueCatalog.techniques(for: model.state.methodName) }
    private var matchingSavedTechniques: [TechniqueRecord] {
        techniques.filter { technique in
            if let selectedId = model.state.methodId, technique.methodId == selectedId { return true }
            return technique.methodName.caseInsensitiveCompare(model.state.methodName) == .orderedSame
        }
    }
    private func finish() {
        guard !isSaving, model.state.savedAt == nil else { return }
        isSaving = true
        if model.state.status == .running { model.pause() }
        let recipeName = recipes.first(where: { $0.id == model.state.recipeId })?.name ?? ""
        let beanName = beans.first(where: { $0.id == model.state.beanId })?.name ?? ""
        let grinderName = grinders.first(where: { $0.id == model.state.grinderId })?.name ?? ""
        let brew = BrewSessionRecord(context: context, state: model.state, recipeName: recipeName, beanName: beanName, grinderName: grinderName)
        do {
            try context.save()
            model.markSaved()
            isSaving = false
            if let onFinished { onFinished(brew.id) } else { savedConfirmation = true }
        } catch {
            context.rollback()
            isSaving = false
            errorMessage = error.localizedDescription
        }
    }
    private func timeString(_ seconds: Int) -> String { String(format: "%02d:%02d", seconds / 60, seconds % 60) }
}

private struct PreparationMetricsRow: View {
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    let added: String
    let accumulated: String
    let timeLabel: String
    let timeValue: String

    var body: some View {
        Group {
            if dynamicTypeSize.isAccessibilitySize {
                VStack(spacing: 7) { tiles }
            } else {
                HStack(spacing: 7) { tiles }
            }
        }
    }

    @ViewBuilder private var tiles: some View {
        PreparationMetricTile(label: "AGREGA AHORA", value: added, color: CupaTheme.terracotta)
        PreparationMetricTile(label: "TOTAL EN BÁSCULA", value: accumulated, color: CupaTheme.forest)
        PreparationMetricTile(label: timeLabel, value: timeValue, color: CupaTheme.gold)
    }
}

private struct PreparationMetricTile: View {
    let label: String
    let value: String
    let color: Color

    var body: some View {
        VStack(spacing: 2) {
            Text(label).font(.system(size: 8, weight: .heavy)).lineLimit(1).minimumScaleFactor(0.72).foregroundStyle(color)
            Text(value).font(.subheadline.bold().monospacedDigit()).lineLimit(1).minimumScaleFactor(0.72).foregroundStyle(CupaTheme.text)
        }
        .frame(maxWidth: .infinity, minHeight: 48)
        .padding(.horizontal, 5).padding(.vertical, 5)
        .background(color.opacity(0.11))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay { RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(color.opacity(0.28), lineWidth: 1) }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(label): \(value)")
    }
}
