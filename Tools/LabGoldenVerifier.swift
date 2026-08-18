import Foundation
import CoreData

@main
struct LabGoldenVerifier {
    static func main() {
        verify(
            name: "nivel-del-mar",
            input: LabState(),
            extraction: 0.95,
            scores: [64, 55, 87, 49, 32, 57]
        )
        verify(
            name: "cdmx-hervor",
            input: LabState(temperatureC: 98, altitudeMeters: 2240, cityName: "CDMX (2,240m)"),
            extraction: 0.919_259_2,
            scores: [64, 56, 84, 49, 33, 56]
        )
        verify(
            name: "sub-extraccion",
            input: LabState(waterMl: 270, ratio: 18, temperatureC: 84, grindClicks: 32, freshness: "viejo", timeSeconds: 80),
            extraction: 0.45,
            scores: [52, 82, 41, 48, 32, 45]
        )
        verify(
            name: "sobre-extraccion",
            input: LabState(coffeeGrams: 20, waterMl: 200, ratio: 10, temperatureC: 98, grindClicks: 8, freshness: "muy fresco", timeSeconds: 320),
            extraction: 1.65,
            scores: [78, 22, 32, 76, 84, 42]
        )
        verifyStateRestoration()
        verifyLocalPersistence()
        print("4 golden tests, restauración de estado y persistencia local aprobados")
    }

    private static func verify(name: String, input: LabState, extraction: Float, scores: [Int]) {
        let output = LabEngine.calculate(input)
        let actual = [output.aroma, output.acidity, output.sweetness, output.body, output.bitterness, output.finish]
        precondition(abs(output.extractionIndex - extraction) <= 0.000_01, "\(name): índice \(output.extractionIndex)")
        precondition(actual == scores, "\(name): esperado \(scores), recibido \(actual)")
    }

    private static func verifyStateRestoration() {
        let suite = "CupaGoldenVerifier.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let first = LabModel(defaults: defaults)
        first.setManualAltitude(2240, city: "CDMX")
        first.update { $0.temperatureUnit = .fahrenheit; $0.timeSeconds = 205 }
        let restored = LabModel(defaults: defaults)
        precondition(restored.state.altitudeMeters == 2240)
        precondition(restored.state.temperatureUnit == .fahrenheit)
        precondition(restored.state.timeSeconds == 205)
    }

    private static func verifyLocalPersistence() {
        let persistence = PersistenceController(inMemory: true)
        let context = persistence.container.viewContext
        let bean = CoffeeBeanRecord(context: context, name: "Prueba", brand: "Tostador", remainingQuantityGrams: 250)
        let state = LabState(altitudeMeters: 1500, cityName: "Guatemala (1,500m)")
        _ = LabExperimentRecord(context: context, state: state, profile: LabEngine.calculate(state))
        let grinder = GrinderRecord(context: context, name: "C40", brand: "Comandante", model: "C40 MK4", grinderType: "MANUAL", scaleUnit: "CLICKS", minimumSetting: 0, maximumSetting: 40, calibrationNotes: "Cero real", notes: "")
        let equipment = EquipmentRecord(context: context, name: "V60 02", equipmentType: "BREWER_METHOD", brand: "Hario", model: "02", capacityMl: 600, configuration: "Plástico", notes: "", isFavorite: true, isActive: true)
        try! context.save()
        let beans = try! context.fetch(NSFetchRequest<CoffeeBeanRecord>(entityName: "CoffeeBeanRecord"))
        let experiments = try! context.fetch(NSFetchRequest<LabExperimentRecord>(entityName: "LabExperimentRecord"))
        let grinders = try! context.fetch(NSFetchRequest<GrinderRecord>(entityName: "GrinderRecord"))
        let equipmentItems = try! context.fetch(NSFetchRequest<EquipmentRecord>(entityName: "EquipmentRecord"))
        precondition(beans.map(\.name) == ["Prueba"])
        precondition(experiments.first?.altitudeMeters == 1500)
        precondition(grinders.first?.maximumSetting == 40)
        precondition(equipmentItems.first?.capacityMl == 600)
        bean.markDeleted()
        grinder.markDeleted()
        equipment.markUpdated()
        try! context.save()
        let active = NSFetchRequest<CoffeeBeanRecord>(entityName: "CoffeeBeanRecord")
        active.predicate = NSPredicate(format: "deletedAt == nil")
        precondition(try! context.fetch(active).isEmpty)
        precondition(grinder.syncStatus == .pendingDelete)
        precondition(equipment.syncStatus == .pendingUpdate)
    }
}
