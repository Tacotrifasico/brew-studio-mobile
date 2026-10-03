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
    @State private var showingTechniques = false
    @State private var showingSteps = false
    @State private var pour = "Original"

    init(model: PreparationModel, onFinished: ((UUID) -> Void)? = nil) {
        self.model = model
        self.onFinished = onFinished
    }

    var body: some View {
        VStack(spacing: 16) {
            executionCard
        }
        .sheet(isPresented: $showingTechniques) {
            NavigationStack {
                ScrollView { techniqueLibraryCard.padding() }
                    .background(CupaTheme.background)
                    .navigationTitle("Técnica")
                    .toolbar { Button("Cerrar") { showingTechniques = false } }
            }
        }
        .alert("Preparación guardada", isPresented: $savedConfirmation) { Button("Aceptar") {} } message: { Text("La sesión y sus snapshots quedaron disponibles offline.") }
        .alert("Error", isPresented: Binding(get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })) { Button("Aceptar") {} } message: { Text(errorMessage ?? "") }
        .confirmationDialog("¿Cancelar esta preparación?", isPresented: $confirmingReset, titleVisibility: .visible) {
            Button("Cancelar preparación", role: .destructive, action: model.reset)
            Button("Cancelar", role: .cancel) {}
        } message: {
            Text("Se reiniciarán el tiempo y el avance. Conservaremos la técnica y las cantidades para empezar de nuevo, sin enviarte a Cata. Las sesiones ya guardadas no se borran.")
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

                if model.state.status == .ready {
                    Button("Técnica") { showingTechniques = true }
                    Picker("Vertido", selection: $pour) {
                        ForEach(["Original", "Circular", "Al centro", "En pulsos"], id: \.self) { Text($0) }
                    }
                    .onChange(of: pour) { _, value in model.setPreparationPour(value) }
                    Button(showingSteps ? "Ocultar pasos" : "Ver todos los pasos") { showingSteps.toggle() }
                    if showingSteps { completeTechniqueOverview }
                } else if let step = model.activeStep {
                    Text(timeString(model.state.elapsedSeconds)).font(.system(.largeTitle, design: .rounded, weight: .black)).monospacedDigit()
                        .minimumScaleFactor(0.6).accessibilityLabel("Tiempo total transcurrido").accessibilityValue(timeString(model.state.elapsedSeconds))
                        .foregroundStyle(CupaTheme.espressoText)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background(CupaTheme.backgroundAlt.opacity(0.82))
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                    activeStepCard(step)
                    Button(showingSteps ? "Ocultar pasos" : "Ver todos los pasos") { showingSteps.toggle() }
                    if showingSteps { executionSequence }
                } else {
                    Text("Selecciona una técnica para comenzar.").font(.caption).foregroundStyle(CupaTheme.secondaryText)
                }
                controls
            }.frame(maxWidth: .infinity)
        }
    }

    private var completeTechniqueOverview: some View {
        VStack(alignment: .leading, spacing: 4) {
            notebookStepHeader
            ForEach(Array(model.state.steps.enumerated()), id: \.element.id) { index, step in
                if index > 0 { Divider().overlay(CupaTheme.border) }
                notebookStepRow(step, number: index + 1)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(stepAccessibilityLabel(step, position: index + 1, status: "Por revisar"))
            }
        }
        .padding(10)
        .background(CupaTheme.card)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay { RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(CupaTheme.border, lineWidth: 1) }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Técnica completa con \(model.state.steps.count) pasos")
    }

    private func notebookStepRow(_ step: PreparationStepSnapshot, number: Int) -> some View {
        HStack(spacing: 6) {
            Text("\(number).").foregroundStyle(CupaTheme.secondaryText)
            Text(step.title).foregroundStyle(CupaTheme.text).frame(maxWidth: .infinity, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)
            Text(step.waterAddedMl > 0 ? "+\(step.waterAddedMl)" : "—").bold().foregroundStyle(CupaTheme.terracottaText).frame(width: 42, alignment: .leading)
            Text("\(step.waterAccumulatedMl)").bold().foregroundStyle(CupaTheme.forestText).frame(width: 54, alignment: .leading)
            Text(durationString(step.durationSeconds)).bold().foregroundStyle(CupaTheme.goldText).frame(width: 48, alignment: .leading)
        }
        .font(.caption).monospacedDigit()
        .padding(.vertical, 3)
    }

    private var notebookStepHeader: some View {
        HStack(spacing: 6) {
            Text("Paso").frame(maxWidth: .infinity, alignment: .leading)
            Text("+ ml").frame(width: 42, alignment: .leading)
            Text("Total ml").frame(width: 54, alignment: .leading)
            Text("Tiempo").frame(width: 48, alignment: .leading)
        }.font(.caption2).foregroundStyle(CupaTheme.secondaryText)
    }

    private func activeStepCard(_ step: PreparationStepSnapshot) -> some View {
        VStack(spacing: 10) {
            Text("PASO \(step.number) DE \(model.state.steps.count)").font(.caption2.bold()).tracking(1).foregroundStyle(CupaTheme.secondaryText)
            Text("AHORA · \(step.title)").font(.title3.bold()).multilineTextAlignment(.center)
            activeStepInstructions(step, remainingSeconds: max(0, step.durationSeconds - model.stepElapsed))
            Text(step.gesture.replacingOccurrences(of: "_", with: " ").capitalized + " · " + step.intensity.capitalized)
                .font(.caption.bold()).foregroundStyle(CupaTheme.forestText)
            if !step.note.isEmpty { Text(step.note).font(.caption).foregroundStyle(CupaTheme.secondaryText).multilineTextAlignment(.center) }
            ProgressView(value: Double(min(model.stepElapsed, max(1, step.durationSeconds))), total: Double(max(1, step.durationSeconds))).tint(CupaTheme.terracotta)
        }
        .padding(12)
        .background(LinearGradient(colors: [CupaTheme.terracotta.opacity(0.12), CupaTheme.forest.opacity(0.08)], startPoint: .leading, endPoint: .trailing))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay { RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(CupaTheme.border, lineWidth: 1) }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            stepAccessibilityLabel(
                step,
                position: model.state.activeStepIndex + 1,
                status: "Activo",
                timeLabel: "Tiempo restante",
                timeValue: durationString(max(0, step.durationSeconds - model.stepElapsed))
            )
        )
    }

    private var executionSequence: some View {
        VStack(alignment: .leading, spacing: 4) {
            notebookStepHeader
            ForEach(Array(model.state.steps.enumerated()), id: \.element.id) { index, step in
                let isCurrent = index == model.state.activeStepIndex
                let isPast = index < model.state.activeStepIndex
                let status = isPast ? "Completado" : (isCurrent ? "Activo" : "Pendiente")
                notebookStepRow(step, number: index + 1)
                .padding(.horizontal, 6)
                .background(isCurrent ? CupaTheme.terracotta.opacity(0.10) : isPast ? CupaTheme.forest.opacity(0.07) : CupaTheme.backgroundAlt.opacity(0.72))
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .overlay { RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(isCurrent ? CupaTheme.terracotta : CupaTheme.border, lineWidth: isCurrent ? 1.5 : 1) }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(stepAccessibilityLabel(step, position: index + 1, status: status))
            }
        }
    }

    private func stepMetrics(_ step: PreparationStepSnapshot, timeLabel: String, timeValue: String) -> some View {
        PreparationMetricsRow(
            addedLabel: step.waterAddedMl > 0 ? "1 · VIERTE" : "1 · ESPERA",
            added: step.waterAddedMl > 0 ? "+\(step.waterAddedMl) ml" : "0 ml",
            addedHelper: step.waterAddedMl > 0 ? "agrega ahora" : "sin verter",
            accumulated: "\(step.waterAccumulatedMl) ml",
            timeLabel: "3 · TIEMPO",
            timeHelper: timeLabel == "TIEMPO RESTANTE" ? "restante" : "del paso",
            timeValue: timeValue
        )
    }

    private func activeStepInstructions(_ step: PreparationStepSnapshot, remainingSeconds: Int) -> some View {
        VStack(spacing: 8) {
            ActiveInstructionBand(
                label: step.waterAddedMl > 0 ? "1 · AGREGA AHORA" : "1 · NO AGREGUES AGUA",
                value: step.waterAddedMl > 0 ? "+\(step.waterAddedMl) ml" : "0 ml",
                helper: step.waterAddedMl > 0 ? "Cantidad de este vertido" : "Espera sin verter",
                color: CupaTheme.terracotta,
                systemImage: "drop.fill"
            )
            ActiveInstructionBand(
                label: "2 · LA BÁSCULA DEBE MARCAR",
                value: "\(step.waterAccumulatedMl) ml",
                helper: "Total acumulado desde el inicio",
                color: CupaTheme.forest,
                systemImage: "scalemass.fill"
            )
            ActiveInstructionBand(
                label: "3 · TIEMPO DE ESTE PASO",
                value: durationString(remainingSeconds),
                helper: "Tiempo restante",
                color: CupaTheme.gold,
                systemImage: "timer"
            )
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            step.waterAddedMl > 0
                ? "Agrega ahora \(step.waterAddedMl) mililitros. La báscula debe marcar \(step.waterAccumulatedMl) mililitros. Tiempo restante \(durationString(remainingSeconds))."
                : "No agregues agua. Mantén la báscula en \(step.waterAccumulatedMl) mililitros. Tiempo restante \(durationString(remainingSeconds))."
        )
    }

    private func durationString(_ seconds: Int) -> String {
        seconds < 60 ? "\(seconds) s" : String(format: "%d:%02d", seconds / 60, seconds % 60)
    }

    private func stepAccessibilityLabel(
        _ step: PreparationStepSnapshot,
        position: Int,
        status: String,
        timeLabel: String = "Tiempo del paso",
        timeValue: String? = nil
    ) -> String {
        let waterAction = step.waterAddedMl > 0
            ? "Agrega \(step.waterAddedMl) mililitros"
            : "Sin agua nueva"
        return "Paso \(position) de \(model.state.steps.count). \(status). \(step.title). \(waterAction). " +
            "Total en báscula \(step.waterAccumulatedMl) mililitros. \(timeLabel) \(timeValue ?? durationString(step.durationSeconds))."
    }

    @ViewBuilder private var controls: some View {
        if model.state.status == .ready {
            Button(action: model.start) {
                Label("Iniciar preparación", systemImage: "play.fill")
                    .font(.subheadline.bold())
                    .frame(maxWidth: .infinity, minHeight: 38)
            }
            .buttonStyle(.borderedProminent).tint(CupaTheme.forest).foregroundStyle(CupaTheme.onAccent)
            .disabled(model.state.steps.isEmpty)
        } else {
            HStack {
                Button { model.previousStep() } label: { Image(systemName: "backward.end") }
                    .frame(minWidth: 44, minHeight: 44).accessibilityLabel("Paso anterior")
                    .disabled(model.state.activeStepIndex == 0)
                switch model.state.status {
                case .running: Button("Pausar", action: model.pause).buttonStyle(.borderedProminent).tint(CupaTheme.terracotta).foregroundStyle(CupaTheme.onTerracotta)
                case .paused: Button("Reanudar", action: model.resume).buttonStyle(.borderedProminent).tint(CupaTheme.forest).foregroundStyle(CupaTheme.onAccent)
                case .completed: Label("Finalizada", systemImage: "checkmark.circle.fill").foregroundStyle(CupaTheme.forestText)
                case .ready: EmptyView()
                }
                Button { model.nextStep() } label: { Image(systemName: "forward.end") }
                    .frame(minWidth: 44, minHeight: 44).accessibilityLabel("Paso siguiente")
                    .disabled(model.state.activeStepIndex >= model.state.steps.count - 1)
            }
        }
        if model.state.status != .ready {
            Button(model.state.status == .completed ? "Reiniciar preparación" : "Cancelar preparación", action: requestReset)
                .font(.caption.bold()).foregroundStyle(CupaTheme.secondaryText)
                .disabled(model.state.steps.isEmpty)
                .accessibilityIdentifier("preparation.reset")
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
        if let template = builtInTechniques.first(where: { $0.id == key }) { model.load(template: template); pour = "Original"; showingTechniques = false; return }
        guard key.hasPrefix("saved:"), let id = UUID(uuidString: String(key.dropFirst(6))), let technique = matchingSavedTechniques.first(where: { $0.id == id }) else { return }
        do { model.load(technique: technique, steps: try RecipeTechniqueRepository(context: context).techniqueSteps(techniqueId: id)); pour = "Original"; showingTechniques = false }
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
    let addedLabel: String
    let added: String
    let addedHelper: String
    let accumulated: String
    let timeLabel: String
    let timeHelper: String
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
        PreparationMetricTile(label: addedLabel, value: added, helper: addedHelper, color: CupaTheme.terracotta, systemImage: "drop.fill")
        PreparationMetricTile(label: "2 · BÁSCULA", value: accumulated, helper: "total acumulado", color: CupaTheme.forest, systemImage: "scalemass.fill")
        PreparationMetricTile(label: timeLabel, value: timeValue, helper: timeHelper, color: CupaTheme.gold, systemImage: "timer")
    }
}

private struct PreparationMetricTile: View {
    let label: String
    let value: String
    let helper: String
    let color: Color
    let systemImage: String

    var body: some View {
        VStack(spacing: 2) {
            HStack(spacing: 3) {
                Image(systemName: systemImage).font(.system(size: 8, weight: .bold))
                Text(label).font(.system(size: 8, weight: .heavy)).lineLimit(1).minimumScaleFactor(0.65)
            }
            .foregroundStyle(color)
            Text(value).font(.headline.bold().monospacedDigit()).lineLimit(1).minimumScaleFactor(0.72).foregroundStyle(CupaTheme.text)
            Text(helper).font(.system(size: 8, weight: .bold)).lineLimit(1).minimumScaleFactor(0.7).foregroundStyle(color)
        }
        .frame(maxWidth: .infinity, minHeight: 58)
        .padding(.horizontal, 5).padding(.vertical, 5)
        .background(color.opacity(0.11))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay { RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(color.opacity(0.28), lineWidth: 1) }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(label): \(value), \(helper)")
    }
}

private struct ActiveInstructionBand: View {
    let label: String
    let value: String
    let helper: String
    let color: Color
    let systemImage: String

    var body: some View {
        HStack(spacing: 11) {
            Image(systemName: systemImage)
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(color)
                .frame(width: 38, height: 38)
                .background(color.opacity(0.17))
                .clipShape(RoundedRectangle(cornerRadius: 11, style: .continuous))
            VStack(alignment: .leading, spacing: 1) {
                Text(label).font(.caption2.bold()).tracking(0.4).foregroundStyle(color)
                    .lineLimit(1).minimumScaleFactor(0.72)
                Text(helper).font(.caption2).foregroundStyle(CupaTheme.secondaryText)
            }
            Spacer(minLength: 6)
            Text(value)
                .font(.title3.bold().monospacedDigit())
                .foregroundStyle(CupaTheme.text)
                .lineLimit(1)
                .minimumScaleFactor(0.75)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(color.opacity(0.11))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay { RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(color.opacity(0.30), lineWidth: 1) }
    }
}
