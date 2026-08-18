import Foundation

enum RatioCategory {
    case espresso, intense, balance, clarity

    var label: String {
        switch self {
        case .espresso: "Espresso · Corto e intenso"
        case .intense: "Intenso · Dulce y marcado"
        case .balance: "Balance · Redondo y dulce"
        case .clarity: "Claridad · Té y notas limpias"
        }
    }
}

struct BrewPreset: Identifiable, Codable, Equatable {
    let id: String
    let method: String
    let methodId: UUID?
    let coffee: Double
    let ratio: Double
    let isCustom: Bool

    var label: String {
        let coffeeText = coffee.formatted(.number.precision(.fractionLength(0...1)))
        let ratioText = ratio.formatted(.number.precision(.fractionLength(0...1)))
        return "\(method) · \(coffeeText)g · 1:\(ratioText)"
    }
}

private struct CalculatorStateSnapshot: Codable {
    let method: String
    let methodId: UUID?
    let coffee: Double
    let ratio: Double
    let water: Int
}

@MainActor
final class CalculatorModel: ObservableObject {
    @Published var method = "V60"
    @Published private(set) var selectedMethodId: UUID?
    @Published var coffee = 15.0
    @Published var ratio = 16.0
    @Published var water = 240
    @Published var coffeeInput = "15.0"
    @Published var ratioInput = "16.0"
    @Published var waterInput = "240"
    @Published var microcopy = "Listo para preparar."
    @Published private(set) var savedPresets: [BrewPreset] = []
    @Published private(set) var pinnedMethodNames: Set<String>

    let methods = ["V60", "AeroPress", "Prensa francesa", "Chemex", "Espresso", "Moka", "Cold brew"]
    private let defaultPinnedMethods: Set<String> = ["V60", "AeroPress", "Espresso", "Prensa francesa"]

    private let baseRatios: [String: Double] = [
        "V60": 16, "AeroPress": 13, "Prensa francesa": 15,
        "Chemex": 16, "Espresso": 2, "Moka": 10, "Cold brew": 8
    ]

    private let builtInPresets = [
        BrewPreset(id: "default_1", method: "V60", methodId: nil, coffee: 15, ratio: 16, isCustom: false),
        BrewPreset(id: "default_2", method: "AeroPress", methodId: nil, coffee: 18, ratio: 13, isCustom: false),
        BrewPreset(id: "default_3", method: "Prensa francesa", methodId: nil, coffee: 20, ratio: 15, isCustom: false),
        BrewPreset(id: "default_4", method: "Chemex", methodId: nil, coffee: 24, ratio: 16, isCustom: false),
        BrewPreset(id: "default_5", method: "Espresso", methodId: nil, coffee: 18, ratio: 2, isCustom: false),
        BrewPreset(id: "default_6", method: "Moka", methodId: nil, coffee: 18, ratio: 10, isCustom: false),
        BrewPreset(id: "default_7", method: "Cold brew", methodId: nil, coffee: 50, ratio: 8, isCustom: false)
    ]

    private let userDefaults: UserDefaults
    private let stateKey = "cupa.calculatorState.v1"
    private let pinnedMethodsKey = "cupa.pinnedCalculatorMethods.v1"

    var presets: [BrewPreset] { savedPresets + builtInPresets }

    var category: RatioCategory {
        if ratio <= 3 { return .espresso }
        if ratio <= 10 { return .intense }
        if ratio <= 16 { return .balance }
        return .clarity
    }

    var isCurrentFavorite: Bool {
        savedPresets.contains {
            $0.method == method && abs($0.coffee - coffee) < 0.2 && abs($0.ratio - ratio) < 0.2
        }
    }

    init(defaults: UserDefaults = .standard) {
        userDefaults = defaults
        if let stored = defaults.array(forKey: pinnedMethodsKey) as? [String] {
            pinnedMethodNames = Set(stored)
        } else {
            pinnedMethodNames = defaultPinnedMethods
        }
        if let data = defaults.data(forKey: "cupa.savedRatios"),
           let decoded = try? JSONDecoder().decode([BrewPreset].self, from: data) {
            savedPresets = decoded
        }
        if let data = defaults.data(forKey: stateKey),
           let restored = try? JSONDecoder().decode(CalculatorStateSnapshot.self, from: data),
           !restored.method.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
           restored.coffee >= 1, restored.ratio >= 1, restored.water >= 1 {
            method = restored.method; selectedMethodId = restored.methodId
            coffee = restored.coffee; ratio = restored.ratio; water = restored.water
            coffeeInput = format(restored.coffee, forceDecimal: true)
            ratioInput = format(restored.ratio, forceDecimal: true); waterInput = String(restored.water)
            microcopy = "Se restauró tu última preparación."
        }
    }

    func changeCoffee(_ input: String) {
        coffeeInput = input
        guard let parsed = parseDecimal(input), parsed >= 1 else { return }
        coffee = parsed
        water = Int(parsed * ratio)
        waterInput = String(water)
        microcopy = "Listo para preparar con \(format(parsed)) g de café."
        persistState()
    }

    func changeRatio(_ input: String) {
        ratioInput = input
        guard let parsed = parseDecimal(input), parsed >= 1 else { return }
        ratio = parsed
        water = Int(coffee * parsed)
        waterInput = String(water)
        microcopy = "Proporción ajustada a 1:\(format(parsed))."
        persistState()
    }

    func changeWater(_ input: String) {
        waterInput = input
        guard let parsed = Int(input), parsed >= 1 else { return }
        water = parsed
        coffee = ((Double(parsed) / ratio) * 10).rounded() / 10
        coffeeInput = format(coffee, forceDecimal: true)
        microcopy = "Agua total ajustada a \(parsed) ml."
        persistState()
    }

    func selectMethod(_ selected: String, methodId: UUID? = nil) {
        method = selected
        selectedMethodId = methodId
        ratio = baseRatios[selected] ?? 15
        ratioInput = format(ratio, forceDecimal: true)
        water = Int(coffee * ratio)
        waterInput = String(water)
        microcopy = "Método cambiado a \(selected). Proporción sugerida 1:\(format(ratio))."
        persistState()
    }

    func apply(_ preset: BrewPreset) {
        method = preset.method
        selectedMethodId = preset.methodId
        coffee = preset.coffee
        ratio = preset.ratio
        water = Int(coffee * ratio)
        coffeeInput = format(coffee, forceDecimal: true)
        ratioInput = format(ratio, forceDecimal: true)
        waterInput = String(water)
        microcopy = "Se cargó: \(preset.label)."
        persistState()
    }

    func adjustCoffee(_ amount: Double) { changeCoffee(format(max(1, coffee + amount), forceDecimal: true)) }
    func adjustRatio(_ amount: Double) { changeRatio(format(max(1, ratio + amount), forceDecimal: true)) }
    func adjustWater(_ amount: Int) { changeWater(String(max(1, water + amount))) }

    func resetRatio() {
        changeRatio(format(baseRatios[method] ?? 15, forceDecimal: true))
        microcopy = "Se restauró la proporción base de \(method)."
    }

    func validateInputs() {
        if parseDecimal(coffeeInput) == nil { changeCoffee("15.0") }
        if parseDecimal(ratioInput) == nil { changeRatio("15.0") }
        if Int(waterInput) == nil { changeWater("240") }
    }

    func toggleFavorite() {
        if let index = savedPresets.firstIndex(where: {
            $0.method == method && abs($0.coffee - coffee) < 0.2 && abs($0.ratio - ratio) < 0.2
        }) {
            savedPresets.remove(at: index)
            microcopy = "Proporción eliminada de favoritos."
        } else {
            savedPresets.insert(
                BrewPreset(id: UUID().uuidString, method: method, methodId: selectedMethodId, coffee: coffee, ratio: ratio, isCustom: true),
                at: 0
            )
            microcopy = "Proporción guardada en favoritos."
        }
        if let data = try? JSONEncoder().encode(savedPresets) {
            userDefaults.set(data, forKey: "cupa.savedRatios")
        }
    }

    func isMethodPinned(_ name: String) -> Bool { pinnedMethodNames.contains(name) }

    func setMethodPinned(_ name: String, pinned: Bool) {
        if pinned { pinnedMethodNames.insert(name) } else { pinnedMethodNames.remove(name) }
        userDefaults.set(Array(pinnedMethodNames).sorted(), forKey: pinnedMethodsKey)
    }

    private func parseDecimal(_ value: String) -> Double? {
        Double(value.replacingOccurrences(of: ",", with: "."))
    }

    private func persistState() {
        let snapshot = CalculatorStateSnapshot(method: method, methodId: selectedMethodId, coffee: coffee, ratio: ratio, water: water)
        if let data = try? JSONEncoder().encode(snapshot) { userDefaults.set(data, forKey: stateKey) }
    }

    private func format(_ value: Double, forceDecimal: Bool = false) -> String {
        if !forceDecimal && value.rounded() == value { return String(Int(value)) }
        return String(format: "%.1f", value)
    }
}
