import Foundation

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
    private let defaults: UserDefaults; private let key = "cupa.activePreparation.v1"; private var timer: Timer?

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        if let data = defaults.data(forKey: key), let restored = try? JSONDecoder().decode(PreparationState.self, from: data), restored.status != .completed || restored.savedAt == nil {
            state = restored
        } else { state = PreparationState() }
        if state.status == .running { synchronizeClock(); scheduleTimer() }
    }

    deinit { timer?.invalidate() }

    var activeStep: PreparationStepSnapshot? { state.steps.indices.contains(state.activeStepIndex) ? state.steps[state.activeStepIndex] : nil }
    var totalDuration: Int { state.steps.reduce(0) { $0 + $1.durationSeconds } }
    var stepElapsed: Int {
        let prior = state.steps.prefix(state.activeStepIndex).reduce(0) { $0 + $1.durationSeconds }
        return max(0, state.elapsedSeconds - prior)
    }

    func load(technique: TechniqueRecord, steps: [TechniqueStepRecord]) {
        timer?.invalidate(); timer = nil
        state = PreparationState(
            sessionId: UUID(), techniqueId: technique.id, techniqueName: technique.name,
            methodId: technique.methodId, methodName: technique.methodName,
            recipeId: technique.recipeId, beanId: technique.beanId, grinderId: technique.grinderId,
            doseGrams: technique.doseGrams, waterMl: Int(technique.waterMl), ratio: technique.ratio,
            temperatureC: Int(technique.temperatureC), grindDescription: technique.grindDescription,
            executionMode: technique.executionMode,
            steps: steps.map { .init(id: $0.id, number: Int($0.stepNumber), title: $0.title, durationSeconds: Int($0.durationSeconds), waterAddedMl: Int($0.waterAddedMl), waterAccumulatedMl: Int($0.waterAccumulatedMl), gesture: $0.gesture, intensity: $0.intensity, note: $0.stepNote) }
        )
    }

    func load(calculator: CalculatorModel) {
        timer?.invalidate(); timer = nil
        state = PreparationState(
            techniqueName: "\(calculator.method) Estándar", methodId: calculator.selectedMethodId, methodName: calculator.method,
            doseGrams: calculator.coffee, waterMl: calculator.water, ratio: calculator.ratio,
            temperatureC: 93, executionMode: "GUIDED",
            steps: Self.quickSteps(method: calculator.method, waterMl: calculator.water)
        )
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
    private func persist() { if let data = try? JSONEncoder().encode(state) { defaults.set(data, forKey: key) } }
}
