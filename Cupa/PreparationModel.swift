import Foundation

struct PreparationTechniqueTemplate: Identifiable, Equatable {
    let id: String
    let method: String
    let name: String
    let temperatureC: Int
    let grindDescription: String
    let variant: Int
}

enum PreparationTechniqueCatalog {
    static let techniqueNames: [String: [String]] = [
        "V60": ["Clásica en 3 vertidos", "Pulsos 4:6", "Vertido continuo"],
        "AeroPress": ["Estándar limpia", "Invertida dulce", "Bypass brillante"],
        "Prensa francesa": ["Inmersión clásica", "Costra limpia", "Agitación breve"],
        "Chemex": ["Clásica en pulsos", "Vertido continuo", "Alta claridad"],
        "Espresso": ["Flujo clásico", "Preinfusión suave", "Presión descendente"],
        "Moka": ["Clásica controlada", "Agua precalentada", "Suave para leche"],
        "Cold brew": ["Inmersión balanceada 12 h", "Brillante 8 h", "Concentrado 16 h"]
    ]

    static func techniques(for method: String) -> [PreparationTechniqueTemplate] {
        let names = techniqueNames[method] ?? ["Balanceada en 3 fases", "Flujo continuo", "Pulsos suaves"]
        return names.enumerated().map { index, name in
            .init(
                id: "builtin:\(method):\(index)", method: method, name: name,
                temperatureC: temperature(for: method, variant: index),
                grindDescription: grind(for: method, variant: index), variant: index
            )
        }
    }

    static func steps(for template: PreparationTechniqueTemplate, waterMl: Int) -> [PreparationStepSnapshot] {
        let profile = stepProfile(method: template.method, variant: template.variant)
        let total = max(1, waterMl)
        let waterSteps = profile.filter { $0.weight > 0 }
        let totalWeight = max(1, waterSteps.reduce(0) { $0 + $1.weight })
        var accumulated = 0
        var waterIndex = 0
        return profile.enumerated().map { index, item in
            let added: Int
            if item.weight == 0 { added = 0 }
            else {
                waterIndex += 1
                added = waterIndex == waterSteps.count ? total - accumulated : Int(Double(total * item.weight) / Double(totalWeight))
            }
            accumulated += added
            return .init(id: UUID(), number: index + 1, title: item.title, durationSeconds: item.seconds,
                         waterAddedMl: added, waterAccumulatedMl: accumulated, gesture: item.gesture,
                         intensity: "MEDIUM", note: item.note)
        }
    }

    private struct StepProfile { let title: String; let seconds: Int; let weight: Int; let gesture: String; let note: String }
    private static func p(_ title: String, _ seconds: Int, _ weight: Int, _ gesture: String, _ note: String) -> StepProfile {
        .init(title: title, seconds: seconds, weight: weight, gesture: gesture, note: note)
    }
    private static func temperature(for method: String, variant: Int) -> Int {
        switch method { case "AeroPress": [85, 88, 82][variant]; case "Cold brew": 20; case "Moka": [90, 92, 88][variant]; default: [93, 92, 94][variant] }
    }
    private static func grind(for method: String, variant: Int) -> String {
        switch method { case "Espresso": "Fina"; case "Prensa francesa", "Cold brew": "Gruesa"; case "Chemex": "Media gruesa"; default: variant == 1 ? "Media gruesa" : "Media fina" }
    }
    private static func stepProfile(method: String, variant: Int) -> [StepProfile] {
        switch method {
        case "Espresso":
            if variant == 0 { return [p("Preinfusión", 6, 1, "WAIT", "Satura la pastilla."), p("Extracción", 24, 9, "PRESS", "Detén al alcanzar el rendimiento calculado.")] }
            if variant == 1 { return [p("Saturación", 8, 1, "WAIT", "Inicia con flujo bajo."), p("Pausa", 4, 0, "WAIT", "Permite que se expanda."), p("Flujo principal", 23, 9, "PRESS", "Busca un hilo centrado.")] }
            return [p("Inicio intenso", 10, 3, "PRESS", "Inicia con flujo firme."), p("Flujo estable", 12, 4, "PRESS", "Mantén el color uniforme."), p("Final suave", 10, 3, "PRESS", "Reduce el flujo antes de cortar.")]
        case "AeroPress":
            if variant == 0 { return [p("Carga y mezcla", 30, 10, "STIR", "Mezcla 10 segundos."), p("Inmersión", 45, 0, "WAIT", "Coloca el émbolo."), p("Presión", 30, 0, "PRESS", "Presiona lentamente.")] }
            if variant == 1 { return [p("Preinfusión invertida", 35, 3, "BLOOM", "Mezcla con cuidado."), p("Completar agua", 45, 7, "STIR", "Remueve dos veces."), p("Giro y presión", 35, 0, "PRESS", "Gira y presiona suave.")] }
            return [p("Concentrado", 45, 7, "STIR", "Mezcla 10 segundos."), p("Presión breve", 25, 0, "PRESS", "Presiona constante."), p("Bypass", 15, 3, "CENTER_POUR", "Añade el agua restante a la taza.")]
        case "Prensa francesa":
            if variant == 0 { return [p("Saturar", 30, 10, "STIR", "Añade toda el agua."), p("Inmersión", 210, 0, "WAIT", "Espera sin mover."), p("Prensar", 25, 0, "PRESS", "Baja el émbolo sin forzar.")] }
            if variant == 1 { return [p("Inmersión", 240, 10, "WAIT", "Deja formar la costra."), p("Romper costra", 30, 0, "STIR", "Retira la espuma."), p("Decantar", 300, 0, "WAIT", "Sirve sin hundir hasta el fondo.")] }
            return [p("Llenar", 25, 10, "CIRCULAR_POUR", "Añade toda el agua."), p("Agitar", 15, 0, "STIR", "Mezcla brevemente."), p("Reposar y prensar", 210, 0, "PRESS", "Presiona lentamente.")]
        case "Moka":
            let times = variant == 1 ? 120 : (variant == 2 ? 210 : 180)
            return [p("Cargar y montar", 25, 10, "CENTER_POUR", "Nivela el café sin compactar."), p("Extraer", times, 0, "WAIT", "Usa fuego bajo."), p("Finalizar", 25, 0, "WAIT", "Retira antes del borboteo fuerte.")]
        case "Cold brew":
            let hours = [43200, 28800, 57600][variant]
            return [p("Saturar", 60, 10, "STIR", "Mezcla hasta eliminar zonas secas."), p("Reposar", hours, 0, "WAIT", "Tapa y conserva en refrigeración."), p("Filtrar", 240, 0, "WAIT", "Filtra sin presionar el café.")]
        default:
            if variant == 0 { return [p("Bloom", 40, 2, "BLOOM", "Satura uniformemente."), p("Primer vertido", 45, 4, "CIRCULAR_POUR", "Vierte en círculos."), p("Vertido final", 55, 4, "CENTER_POUR", "Completa y deja drenar.")] }
            if variant == 1 { return [p("Bloom", 45, 2, "BLOOM", "Agita suavemente."), p("Vertido continuo", 100, 8, "CIRCULAR_POUR", "Conserva un flujo constante.")] }
            return [p("Bloom", 45, 2, "BLOOM", "Moja todo el café."), p("Pulso uno", 40, 3, "CIRCULAR_POUR", "Vierte despacio."), p("Pulso dos", 40, 3, "CENTER_POUR", "Espera a que baje el nivel."), p("Pulso final", 50, 2, "CIRCULAR_POUR", "Completa sin agitar.")]
        }
    }
}

struct PreparationStepSnapshot: Identifiable, Codable, Equatable {
    var id: UUID; var number: Int; var title: String; var durationSeconds: Int
    var waterAddedMl: Int; var waterAccumulatedMl: Int; var gesture: String; var intensity: String; var note: String
}

enum PreparationStatus: String, Codable { case ready, running, paused, completed }

struct PreparationState: Codable, Equatable {
    var sessionId = UUID(); var techniqueId: UUID?; var techniqueName = "Preparación libre"; var methodId: UUID?; var methodName = "V60"
    var recipeId: UUID?; var beanId: UUID?; var grinderId: UUID?
    var doseGrams = 15.0; var waterMl = 240; var ratio = 16.0; var temperatureC = 92; var grindDescription = ""
    var executionMode = "MANUAL"; var steps: [PreparationStepSnapshot] = []
    var elapsedSeconds = 0; var activeStepIndex = 0; var status = PreparationStatus.ready
    var startedAt: Date?; var lastTickAt: Date?; var savedAt: Date?; var updatedAt = Date()
}

@MainActor
final class PreparationModel: ObservableObject {
    @Published private(set) var state: PreparationState { didSet { persist() } }
    private let defaults: UserDefaults; private var scopeOwnerId: UUID?; private let keyBase = "cupa.activePreparation.v1"; private var timer: Timer?
    private var key: String { LocalDataScope.scopedKey(keyBase, ownerId: scopeOwnerId) }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults; scopeOwnerId = LocalDataScope.activeOwnerId
        let baseKey = "cupa.activePreparation.v1"; let scopedKey = LocalDataScope.scopedKey(baseKey, ownerId: scopeOwnerId)
        let stored = defaults.object(forKey: scopedKey) ?? LocalDataScope.migrateLegacyObject(in: defaults, baseKey: baseKey, ownerId: scopeOwnerId)
        if let data = stored as? Data, let restored = try? JSONDecoder().decode(PreparationState.self, from: data), restored.status != .completed || restored.savedAt == nil {
            state = restored
        } else { state = PreparationState() }
        if state.status == .running { synchronizeClock(); scheduleTimer() }
    }

    deinit { timer?.invalidate() }

    func switchScope(to ownerId: UUID?) {
        guard scopeOwnerId != ownerId else { return }
        synchronizeClock(); timer?.invalidate(); timer = nil; scopeOwnerId = ownerId
        let stored = defaults.object(forKey: key) ?? LocalDataScope.migrateLegacyObject(in: defaults, baseKey: keyBase, ownerId: ownerId)
        if let data = stored as? Data, let restored = try? JSONDecoder().decode(PreparationState.self, from: data), restored.status != .completed || restored.savedAt == nil { state = restored }
        else { state = PreparationState() }
        if state.status == .running { synchronizeClock(); scheduleTimer() }
    }

    var activeStep: PreparationStepSnapshot? { state.steps.indices.contains(state.activeStepIndex) ? state.steps[state.activeStepIndex] : nil }
    var totalDuration: Int { state.steps.reduce(0) { $0 + $1.durationSeconds } }
    var stepElapsed: Int {
        let prior = state.steps.prefix(state.activeStepIndex).reduce(0) { $0 + $1.durationSeconds }
        return max(0, state.elapsedSeconds - prior)
    }

    func load(technique: TechniqueRecord, steps: [TechniqueStepRecord]) {
        timer?.invalidate(); timer = nil
        let keepCalculatorAmounts = state.methodName.caseInsensitiveCompare(technique.methodName) == .orderedSame && state.status == .ready
        let targetDose = keepCalculatorAmounts ? state.doseGrams : technique.doseGrams
        let targetWater = keepCalculatorAmounts ? state.waterMl : Int(technique.waterMl)
        let targetRatio = keepCalculatorAmounts ? state.ratio : technique.ratio
        let snapshots: [PreparationStepSnapshot] = steps.map { .init(id: $0.id, number: Int($0.stepNumber), title: $0.title, durationSeconds: Int($0.durationSeconds), waterAddedMl: Int($0.waterAddedMl), waterAccumulatedMl: Int($0.waterAccumulatedMl), gesture: $0.gesture, intensity: $0.intensity, note: $0.stepNote) }
        state = PreparationState(
            sessionId: UUID(), techniqueId: technique.id, techniqueName: technique.name,
            methodId: technique.methodId, methodName: technique.methodName,
            recipeId: technique.recipeId, beanId: technique.beanId, grinderId: technique.grinderId,
            doseGrams: targetDose, waterMl: targetWater, ratio: targetRatio,
            temperatureC: Int(technique.temperatureC), grindDescription: technique.grindDescription,
            executionMode: technique.executionMode,
            steps: Self.scaled(snapshots, sourceWater: Int(technique.waterMl), targetWater: targetWater)
        )
    }

    func load(template: PreparationTechniqueTemplate) {
        timer?.invalidate(); timer = nil
        state.techniqueId = nil; state.techniqueName = template.name; state.methodName = template.method
        state.temperatureC = template.temperatureC; state.grindDescription = template.grindDescription
        state.executionMode = "GUIDED"; state.steps = PreparationTechniqueCatalog.steps(for: template, waterMl: state.waterMl)
        state.elapsedSeconds = 0; state.activeStepIndex = 0; state.status = .ready; state.updatedAt = .now
    }

    func load(calculator: CalculatorModel) {
        timer?.invalidate(); timer = nil
        let template = PreparationTechniqueCatalog.techniques(for: calculator.method)[0]
        let keepSelectedTechnique = state.status == .ready && !state.steps.isEmpty &&
            state.methodName.caseInsensitiveCompare(calculator.method) == .orderedSame
        state = PreparationState(
            techniqueId: keepSelectedTechnique ? state.techniqueId : nil,
            techniqueName: keepSelectedTechnique ? state.techniqueName : template.name,
            methodId: calculator.selectedMethodId, methodName: calculator.method,
            doseGrams: calculator.coffee, waterMl: calculator.water, ratio: calculator.ratio,
            temperatureC: keepSelectedTechnique ? state.temperatureC : template.temperatureC,
            grindDescription: keepSelectedTechnique ? state.grindDescription : template.grindDescription,
            executionMode: "GUIDED",
            steps: keepSelectedTechnique
                ? Self.scaled(state.steps, sourceWater: state.waterMl, targetWater: calculator.water)
                : PreparationTechniqueCatalog.steps(for: template, waterMl: calculator.water)
        )
    }

    func loadCalculatorDraftIfPossible(_ calculator: CalculatorModel) {
        guard state.status == .ready, state.elapsedSeconds == 0, state.savedAt == nil else { return }
        load(calculator: calculator)
    }

    func loadCalculatorIfPristine(_ calculator: CalculatorModel) {
        guard state.status == .ready, state.steps.isEmpty, state.elapsedSeconds == 0, state.savedAt == nil else { return }
        load(calculator: calculator)
    }

    func load(lab: LabState) {
        timer?.invalidate(); timer = nil
        state = PreparationState(
            techniqueId: lab.techniqueId, techniqueName: lab.techniqueName ?? "Hipótesis de Laboratorio",
            methodId: lab.methodId, methodName: lab.method, recipeId: lab.recipeId, beanId: lab.beanId, grinderId: lab.grinderId,
            doseGrams: Double(lab.coffeeGrams), waterMl: lab.waterMl, ratio: Double(lab.ratio), temperatureC: lab.temperatureC,
            grindDescription: "\(lab.grindClicks) clicks", executionMode: "MANUAL",
            steps: [.init(id: UUID(), number: 1, title: "Preparar hipótesis", durationSeconds: lab.timeSeconds, waterAddedMl: lab.waterMl, waterAccumulatedMl: lab.waterMl, gesture: "MANUAL", intensity: "MEDIUM", note: lab.notes)]
        )
    }

    func selectBean(_ bean: CoffeeBeanRecord) {
        if state.savedAt != nil { reset() }
        state.beanId = bean.id
        state.updatedAt = .now
    }

    func start() {
        guard !state.steps.isEmpty else { return }
        if state.startedAt == nil { state.startedAt = .now }
        state.status = .running; state.lastTickAt = .now; state.updatedAt = .now; scheduleTimer()
    }
    func pause() { synchronizeClock(); timer?.invalidate(); timer = nil; state.status = .paused; state.lastTickAt = nil; state.updatedAt = .now }
    func resume() { guard state.status == .paused else { return }; state.status = .running; state.lastTickAt = .now; state.updatedAt = .now; scheduleTimer() }
    func reset() {
        timer?.invalidate(); timer = nil
        if state.savedAt != nil { state.sessionId = UUID(); state.savedAt = nil }
        state.elapsedSeconds = 0; state.activeStepIndex = 0; state.status = .ready
        state.startedAt = nil; state.lastTickAt = nil; state.updatedAt = .now
    }
    func nextStep() { guard !state.steps.isEmpty else { return }; state.activeStepIndex = min(state.steps.count - 1, state.activeStepIndex + 1); state.elapsedSeconds = state.steps.prefix(state.activeStepIndex).reduce(0) { $0 + $1.durationSeconds }; state.updatedAt = .now }
    func previousStep() { guard !state.steps.isEmpty else { return }; state.activeStepIndex = max(0, state.activeStepIndex - 1); state.elapsedSeconds = state.steps.prefix(state.activeStepIndex).reduce(0) { $0 + $1.durationSeconds }; state.updatedAt = .now }
    func complete() { synchronizeClock(); timer?.invalidate(); timer = nil; state.status = .completed; state.lastTickAt = nil; state.updatedAt = .now }
    func markSaved() { complete(); state.savedAt = .now; state.updatedAt = .now }

    func synchronizeClock(now: Date = .now) {
        guard state.status == .running, let last = state.lastTickAt else { return }
        let delta = max(0, Int(now.timeIntervalSince(last)))
        if delta > 0 { state.elapsedSeconds += delta; state.lastTickAt = now; updateAutomaticStep() }
    }

    private func scheduleTimer() {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            MainActor.assumeIsolated { self?.synchronizeClock() }
        }
    }
    private func updateAutomaticStep() {
        guard state.executionMode == "GUIDED" || state.executionMode == "AUTOMATED" else { return }
        var accumulated = 0
        for (index, step) in state.steps.enumerated() {
            accumulated += step.durationSeconds
            if state.elapsedSeconds < accumulated { state.activeStepIndex = index; return }
        }
        state.activeStepIndex = max(0, state.steps.count - 1)
        state.elapsedSeconds = accumulated
        state.status = .completed; state.lastTickAt = nil; state.updatedAt = .now
        timer?.invalidate(); timer = nil
    }
    private static func quickSteps(method: String, waterMl: Int) -> [PreparationStepSnapshot] {
        if let template = PreparationTechniqueCatalog.techniques(for: method).first {
            return PreparationTechniqueCatalog.steps(for: template, waterMl: waterMl)
        }
        let total = max(1, waterMl)
        switch method {
        case "Espresso":
            return [.init(id: UUID(), number: 1, title: "Extracción de Presión", durationSeconds: 30, waterAddedMl: total, waterAccumulatedMl: total, gesture: "TAP", intensity: "alta", note: "Mantén la presión uniforme.")]
        case "AeroPress":
            let bloom = min(40, total); let remainder = total - bloom
            return [
                .init(id: UUID(), number: 1, title: "Preinfusión (Bloom)", durationSeconds: 30, waterAddedMl: bloom, waterAccumulatedMl: bloom, gesture: "TAP", intensity: "alta", note: "Remueve por 10 segundos."),
                .init(id: UUID(), number: 2, title: "Vertido de volumen", durationSeconds: 40, waterAddedMl: remainder, waterAccumulatedMl: total, gesture: "TAP", intensity: "media", note: "Pon el émbolo para vacío."),
                .init(id: UUID(), number: 3, title: "Presión continua", durationSeconds: 30, waterAddedMl: 0, waterAccumulatedMl: total, gesture: "TAP", intensity: "alta", note: "Presiona despacio.")
            ]
        default:
            let bloom = min(50, total); let remainder = total - bloom; let firstPour = remainder / 2
            return [
                .init(id: UUID(), number: 1, title: "Preinfusión Bloom", durationSeconds: 35, waterAddedMl: bloom, waterAccumulatedMl: bloom, gesture: "TAP", intensity: "alta", note: "Moja todo el grano uniformemente."),
                .init(id: UUID(), number: 2, title: "Primer Vertido", durationSeconds: 45, waterAddedMl: firstPour, waterAccumulatedMl: bloom + firstPour, gesture: "TAP", intensity: "media", note: "Vierte en círculos suaves."),
                .init(id: UUID(), number: 3, title: "Segundo Vertido final", durationSeconds: 40, waterAddedMl: total - bloom - firstPour, waterAccumulatedMl: total, gesture: "TAP", intensity: "baja", note: "Completa la secuencia.")
            ]
        }
    }
    private static func scaled(_ steps: [PreparationStepSnapshot], sourceWater: Int, targetWater: Int) -> [PreparationStepSnapshot] {
        guard sourceWater > 0, sourceWater != targetWater else { return steps }
        let waterCount = steps.filter { $0.waterAddedMl > 0 }.count
        var index = 0; var accumulated = 0
        return steps.map { step in
            var copy = step
            if step.waterAddedMl > 0 {
                index += 1
                copy.waterAddedMl = index == waterCount ? targetWater - accumulated : Int(Double(step.waterAddedMl) / Double(sourceWater) * Double(targetWater))
            } else { copy.waterAddedMl = 0 }
            accumulated += copy.waterAddedMl; copy.waterAccumulatedMl = accumulated
            return copy
        }
    }
    private func persist() { if let data = try? JSONEncoder().encode(state) { defaults.set(data, forKey: key) } }
}
