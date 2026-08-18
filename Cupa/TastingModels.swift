import CoreData
import Foundation

enum CoolingStatus: String, Codable { case ready, running, paused, completed }

enum FlavorFamily: String, CaseIterable, Codable, Identifiable {
    case floral = "FLORAL", fruity = "FRUITY", citric = "CITRIC", sweet = "SWEET"
    case cacao = "CACAO", nutty = "NUTTY", spiced = "SPICED", green = "GREEN"
    var id: Self { self }
    var label: String {
        switch self {
        case .floral: "Floral"; case .fruity: "Frutal"; case .citric: "Cítrica"; case .sweet: "Dulce"
        case .cacao: "Cacao"; case .nutty: "Nueces"; case .spiced: "Especiada"; case .green: "Verde"
        }
    }
    var suggestions: [String] {
        switch self {
        case .floral: ["Jazmín", "Rosa", "Té negro"]
        case .fruity: ["Mora", "Fresa", "Durazno"]
        case .citric: ["Limón", "Naranja", "Toronja"]
        case .sweet: ["Panela", "Caramelo", "Miel"]
        case .cacao: ["Chocolate", "Cacao", "Nibs"]
        case .nutty: ["Almendra", "Avellana", "Nuez"]
        case .spiced: ["Canela", "Clavo", "Pimienta"]
        case .green: ["Herbal", "Té verde", "Vegetal"]
        }
    }
}

struct TastingObservationSnapshot: Identifiable, Codable, Equatable {
    var id = UUID(); var elapsedSeconds: Int; var stage: String; var notes: String
    var aroma: Double; var acidity: Double; var sweetness: Double; var body: Double; var bitterness: Double; var finish: Double
}

struct TastingState: Codable, Equatable {
    var id = UUID(); var brewSessionId: UUID?; var activeFlavorFamily = FlavorFamily.fruity
    var selectedFlavorNotes: [String] = []; var expectedNotes = ""; var freeNotes = ""
    var texture = "sedosa"; var cleanliness = "alta"; var persistence = "media"
    var aroma = 3.0; var acidity = 3.0; var sweetness = 3.0; var body = 3.0; var bitterness = 3.0; var finish = 3.0
    var rating = 4.0; var nps = 8
    var coolingStatus = CoolingStatus.ready; var coolingElapsedSeconds = 0
    var startedAt: Date?; var lastTickAt: Date?; var observations: [TastingObservationSnapshot] = []
    var evaluatedAt = Date(); var updatedAt = Date()
}

@MainActor
final class TastingModel: ObservableObject {
    @Published var state: TastingState { didSet { persist() } }
    private let defaults: UserDefaults; private let key = "cupa.activeTasting.v1"; private var timer: Timer?

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        if let data = defaults.data(forKey: key), let restored = try? JSONDecoder().decode(TastingState.self, from: data), restored.coolingStatus != .completed {
            state = restored
        } else { state = TastingState() }
        if state.coolingStatus == .running { synchronizeClock(); scheduleTimer() }
    }

    var stageCode: String {
        switch state.coolingElapsedSeconds {
        case ..<240: "HOT"
        case ..<600: "PEAK"
        case ..<960: "DECLINING"
        default: "EXHAUSTED"
        }
    }
    var stageLabel: String {
        switch stageCode {
        case "HOT": "Abierta · calor y aromas volátiles"
        case "PEAK": "Ideal · dulzor y claridad"
        case "DECLINING": "En descenso · cambia la complejidad"
        default: "Fría · acidez más plana"
        }
    }

    func start() {
        guard state.coolingStatus == .ready || state.coolingStatus == .paused else { return }
        if state.startedAt == nil { state.startedAt = .now }
        state.coolingStatus = .running; state.lastTickAt = .now; state.updatedAt = .now; scheduleTimer()
    }
    func pause() { synchronizeClock(); timer?.invalidate(); timer = nil; state.coolingStatus = .paused; state.lastTickAt = nil; state.updatedAt = .now }
    func resume() { start() }
    func reset() {
        timer?.invalidate(); timer = nil; state.coolingStatus = .ready; state.coolingElapsedSeconds = 0
        state.startedAt = nil; state.lastTickAt = nil; state.observations = []; state.updatedAt = .now
    }
    func synchronizeClock(now: Date = .now) {
        guard state.coolingStatus == .running, let last = state.lastTickAt else { return }
        let delta = max(0, Int(now.timeIntervalSince(last)))
        if delta > 0 { state.coolingElapsedSeconds += delta; state.lastTickAt = now; state.updatedAt = now }
    }
    func addObservation() {
        let observation = TastingObservationSnapshot(
            elapsedSeconds: state.coolingElapsedSeconds, stage: stageCode, notes: state.freeNotes,
            aroma: state.aroma, acidity: state.acidity, sweetness: state.sweetness,
            body: state.body, bitterness: state.bitterness, finish: state.finish
        )
        state.observations.append(observation); state.updatedAt = .now
    }
    func load(record: TastingRecord, observations: [TastingObservationRecord]) {
        timer?.invalidate(); timer = nil
        state = record.snapshot(observations: observations)
    }
    func newTasting(linkedBrewSessionId: UUID? = nil) {
        timer?.invalidate(); timer = nil; state = TastingState(brewSessionId: linkedBrewSessionId)
    }
    func markSaved() { timer?.invalidate(); timer = nil; state.coolingStatus = .completed; state.lastTickAt = nil; state.updatedAt = .now; defaults.removeObject(forKey: key) }

    private func scheduleTimer() {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in Task { @MainActor in self?.synchronizeClock() } }
    }
    private func persist() { if let data = try? JSONEncoder().encode(state) { defaults.set(data, forKey: key) } }
}

@objc(TastingRecord)
final class TastingRecord: NSManagedObject, SyncTrackedRecord {
    @NSManaged var id: UUID; @NSManaged var ownerId: UUID?; @NSManaged var brewSessionId: UUID?
    @NSManaged var recipeId: UUID?; @NSManaged var techniqueId: UUID?; @NSManaged var beanId: UUID?
    @NSManaged var activeFlavorFamily: String; @NSManaged var selectedFlavorNotesJSON: String; @NSManaged var expectedNotes: String
    @NSManaged var texture: String; @NSManaged var cleanliness: String; @NSManaged var persistence: String
    @NSManaged var aroma: Double; @NSManaged var acidity: Double; @NSManaged var sweetness: Double; @NSManaged var body: Double
    @NSManaged var bitterness: Double; @NSManaged var finishScore: Double; @NSManaged var rating: Double; @NSManaged var nps: Int64
    @NSManaged var evaluatorNotes: String; @NSManaged var coolingElapsedSeconds: Int64; @NSManaged var cupLifeState: String
    @NSManaged var evaluatedAt: Date; @NSManaged var createdAt: Date; @NSManaged var updatedAt: Date; @NSManaged var version: Int64
    @NSManaged var syncStatusRaw: String; @NSManaged var deletedAt: Date?

    var selectedFlavorNotes: [String] {
        get { (try? JSONDecoder().decode([String].self, from: Data(selectedFlavorNotesJSON.utf8))) ?? [] }
        set { selectedFlavorNotesJSON = String(data: (try? JSONEncoder().encode(newValue)) ?? Data("[]".utf8), encoding: .utf8) ?? "[]" }
    }
    func apply(_ state: TastingState, brew: BrewSessionRecord?) {
        brewSessionId = state.brewSessionId; recipeId = brew?.recipeId; techniqueId = brew?.techniqueId; beanId = brew?.beanId
        activeFlavorFamily = state.activeFlavorFamily.rawValue; selectedFlavorNotes = state.selectedFlavorNotes; expectedNotes = state.expectedNotes
        texture = state.texture; cleanliness = state.cleanliness; persistence = state.persistence
        aroma = state.aroma; acidity = state.acidity; sweetness = state.sweetness; body = state.body; bitterness = state.bitterness; finishScore = state.finish
        rating = state.rating; nps = Int64(state.nps); evaluatorNotes = state.freeNotes
        coolingElapsedSeconds = Int64(state.coolingElapsedSeconds)
        cupLifeState = state.coolingElapsedSeconds < 240 ? "FRESH" : state.coolingElapsedSeconds < 600 ? "PEAK" : state.coolingElapsedSeconds < 960 ? "DECLINING" : "EXHAUSTED"
        evaluatedAt = state.evaluatedAt; updatedAt = .now
    }
    func snapshot(observations: [TastingObservationRecord]) -> TastingState {
        TastingState(
            id: id, brewSessionId: brewSessionId, activeFlavorFamily: FlavorFamily(rawValue: activeFlavorFamily) ?? .fruity,
            selectedFlavorNotes: selectedFlavorNotes, expectedNotes: expectedNotes, freeNotes: evaluatorNotes,
            texture: texture, cleanliness: cleanliness, persistence: persistence,
            aroma: aroma, acidity: acidity, sweetness: sweetness, body: body, bitterness: bitterness, finish: finishScore,
            rating: rating, nps: Int(nps), coolingStatus: .paused, coolingElapsedSeconds: Int(coolingElapsedSeconds),
            startedAt: createdAt, observations: observations.map(\.snapshot), evaluatedAt: evaluatedAt, updatedAt: updatedAt
        )
    }
    func markUpdated() { trackUpdate() }; func markDeleted() { trackDeletion() }
}
extension TastingRecord: Identifiable {}

@objc(TastingObservationRecord)
final class TastingObservationRecord: NSManagedObject, SyncTrackedRecord {
    @NSManaged var id: UUID; @NSManaged var ownerId: UUID?; @NSManaged var tastingId: UUID
    @NSManaged var elapsedSeconds: Int64; @NSManaged var stage: String; @NSManaged var notes: String
    @NSManaged var aroma: Double; @NSManaged var acidity: Double; @NSManaged var sweetness: Double
    @NSManaged var body: Double; @NSManaged var bitterness: Double; @NSManaged var finishScore: Double
    @NSManaged var createdAt: Date; @NSManaged var updatedAt: Date; @NSManaged var version: Int64
    @NSManaged var syncStatusRaw: String; @NSManaged var deletedAt: Date?
    var snapshot: TastingObservationSnapshot { .init(id: id, elapsedSeconds: Int(elapsedSeconds), stage: stage, notes: notes, aroma: aroma, acidity: acidity, sweetness: sweetness, body: body, bitterness: bitterness, finish: finishScore) }
    func apply(_ value: TastingObservationSnapshot, tastingId: UUID) {
        id = value.id; self.tastingId = tastingId; elapsedSeconds = Int64(value.elapsedSeconds); stage = value.stage; notes = value.notes
        aroma = value.aroma; acidity = value.acidity; sweetness = value.sweetness; body = value.body; bitterness = value.bitterness; finishScore = value.finish
    }
    func markUpdated() { trackUpdate() }; func markDeleted() { trackDeletion() }
}
extension TastingObservationRecord: Identifiable {}

@objc(CupSessionRecord)
final class CupSessionRecord: NSManagedObject, SyncTrackedRecord {
    @NSManaged var id: UUID; @NSManaged var ownerId: UUID?; @NSManaged var brewSessionId: UUID?; @NSManaged var tastingId: UUID
    @NSManaged var techniqueNameSnapshot: String; @NSManaged var beanNameSnapshot: String; @NSManaged var rating: Double
    @NSManaged var createdAt: Date; @NSManaged var updatedAt: Date; @NSManaged var version: Int64
    @NSManaged var syncStatusRaw: String; @NSManaged var deletedAt: Date?
    func markUpdated() { trackUpdate() }; func markDeleted() { trackDeletion() }
}
extension CupSessionRecord: Identifiable {}

@MainActor
struct TastingRepository {
    let context: NSManagedObjectContext

    func observations(tastingId: UUID) throws -> [TastingObservationRecord] {
        let request = NSFetchRequest<TastingObservationRecord>(entityName: "TastingObservationRecord")
        request.predicate = NSPredicate(format: "tastingId == %@ AND deletedAt == nil", tastingId as CVarArg)
        request.sortDescriptors = [NSSortDescriptor(key: "elapsedSeconds", ascending: true)]
        return try context.fetch(request)
    }

    @discardableResult func save(_ state: TastingState, brew: BrewSessionRecord?) throws -> TastingRecord {
        let request = NSFetchRequest<TastingRecord>(entityName: "TastingRecord"); request.predicate = NSPredicate(format: "id == %@", state.id as CVarArg)
        let record = try context.fetch(request).first ?? TastingRecord(context: context)
        let isNew = record.managedObjectContext != nil && record.value(forKey: "createdAt") == nil
        if isNew { record.id = state.id; record.ownerId = nil; record.createdAt = state.startedAt ?? .now; record.version = 1; record.syncStatusRaw = SyncStatus.pendingCreate.rawValue; record.deletedAt = nil }
        else { record.markUpdated() }
        record.apply(state, brew: brew)

        let existing = try observations(tastingId: state.id); let byId = Dictionary(uniqueKeysWithValues: existing.map { ($0.id, $0) })
        for item in state.observations {
            let child = byId[item.id] ?? TastingObservationRecord(context: context)
            if byId[item.id] == nil { child.ownerId = nil; child.createdAt = .now; child.version = 1; child.syncStatusRaw = SyncStatus.pendingCreate.rawValue; child.deletedAt = nil }
            else { child.markUpdated() }
            child.apply(item, tastingId: state.id); child.updatedAt = .now
        }
        let retained = Set(state.observations.map(\.id)); existing.filter { !retained.contains($0.id) }.forEach { $0.markDeleted() }

        let cupRequest = NSFetchRequest<CupSessionRecord>(entityName: "CupSessionRecord"); cupRequest.predicate = NSPredicate(format: "tastingId == %@", state.id as CVarArg)
        let cup = try context.fetch(cupRequest).first ?? CupSessionRecord(context: context)
        if cup.value(forKey: "createdAt") == nil { cup.id = UUID(); cup.ownerId = nil; cup.tastingId = state.id; cup.createdAt = .now; cup.version = 1; cup.syncStatusRaw = SyncStatus.pendingCreate.rawValue; cup.deletedAt = nil }
        else { cup.markUpdated() }
        cup.brewSessionId = state.brewSessionId; cup.techniqueNameSnapshot = brew?.techniqueNameSnapshot ?? "Cata independiente"
        cup.beanNameSnapshot = brew?.beanNameSnapshot ?? ""; cup.rating = state.rating; cup.updatedAt = .now
        try context.save(); return record
    }

    func delete(_ record: TastingRecord) throws {
        record.markDeleted(); try observations(tastingId: record.id).forEach { $0.markDeleted() }; try context.save()
    }
}
