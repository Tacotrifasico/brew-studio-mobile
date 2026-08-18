import Foundation

enum TemperatureUnit: String, Codable, CaseIterable {
    case celsius
    case fahrenheit

    var symbol: String { self == .celsius ? "°C" : "°F" }
}

struct CoffeeCity: Identifiable, Equatable {
    let label: String
    let altitudeMeters: Int
    let name: String
    var id: String { "\(name)-\(altitudeMeters)" }
}

struct LabFlavorProfile: Equatable {
    let aroma: Int
    let acidity: Int
    let sweetness: Int
    let body: Int
    let bitterness: Int
    let finish: Int
    let extractionIndex: Float
    let labels: [String]
    let summary: String
}

struct LabState: Codable, Equatable {
    var methodId: UUID?
    var recipeId: UUID?
    var techniqueId: UUID?
    var beanId: UUID?
    var grinderId: UUID?
    var recipeName: String?
    var techniqueName: String?
    var method = "V60"
    var coffeeGrams: Float = 15
    var waterMl = 240
    var ratio: Float = 16
    var temperatureC = 92
    var grindClicks = 24
    var freshness = "en ventana"
    var timeSeconds = 180
    var notes = ""
    var altitudeMeters = 0
    var cityName = "Nivel del mar (0m)"
    var temperatureUnit = TemperatureUnit.celsius
}

enum LabEngine {
    static func boilingPointC(altitudeMeters: Int) -> Float {
        min(100, max(80, 100 - Float(min(5000, max(0, altitudeMeters))) * 0.0034))
    }

    static func fahrenheit(fromCelsius value: Float) -> Float { value * 9 / 5 + 32 }
    static func celsius(fromFahrenheit value: Float) -> Float { (value - 32) * 5 / 9 }

    static func calculate(_ state: LabState) -> LabFlavorProfile {
        calculate(
            coffeeGrams: state.coffeeGrams,
            waterMl: state.waterMl,
            ratio: state.ratio,
            temperature: state.temperatureC,
            grindClicks: state.grindClicks,
            freshnessState: state.freshness,
            altitudeMeters: state.altitudeMeters,
            timeSeconds: state.timeSeconds
        )
    }

    // Port literal de calculateLabProfile en LabScreen.kt (commit aff626e).
    static func calculate(
        coffeeGrams: Float,
        waterMl: Int,
        ratio: Float,
        temperature: Int,
        grindClicks: Int,
        freshnessState: String,
        altitudeMeters: Int = 0,
        timeSeconds: Int = 180
    ) -> LabFlavorProfile {
        let effectiveRatio = min(30, max(5, ratio > 0 ? ratio : 16))
        let tBoil = boilingPointC(altitudeMeters: altitudeMeters)
        let tempEffective = min(Float(temperature), tBoil)
        let altitudeFactor = sqrtf(tBoil / 100)
        let clicksActual = Float(min(50, max(4, grindClicks)))
        let timeFactor = min(1.85, max(0.55, Float(min(360, max(60, timeSeconds))) / 180))
        let extRaw = timeFactor * (22 / clicksActual) * ((tempEffective - 35) / 55) * altitudeFactor
        let extractionIndex = min(1.65, max(0.45, extRaw))

        let aromaRaw = 58 + (tempEffective - 88) * 1.4 - max(0, extractionIndex - 1.22) * 16 + max(0, 22 - clicksActual) * 0.9
        let acidityRaw = 54 + (1 - extractionIndex) * 42 + (89 - tempEffective) * 0.5 + max(0, effectiveRatio - 15.5) * 0.8
        let sweetnessRaw = 92 - abs(1 - extractionIndex) * 82 - abs(tempEffective - 91) * 0.9
        let bodyRaw = 40 + (150 / effectiveRatio) + max(0, 24 - clicksActual) * 0.9 + max(0, extractionIndex - 1) * 10
        let bitternessRaw = 32 + max(0, extractionIndex - 1) * 44 + max(0, tempEffective - 92) * 2 + max(0, 18 - clicksActual) * 1.1

        let aroma = clampScore(aromaRaw)
        let acidity = clampScore(acidityRaw)
        let sweetness = clampScore(sweetnessRaw)
        let body = clampScore(bodyRaw)
        let bitterness = clampScore(bitternessRaw)
        let finishRaw = 48 + (Float(sweetness) - 50) * 0.25 + (Float(body) - 50) * 0.18 - max(0, Float(bitterness) - 58) * 0.22
        let finish = clampScore(finishRaw)

        var labels: [String] = []
        if extractionIndex > 1.20 { labels.append("Alta Extracción") }
        else if extractionIndex < 0.85 { labels.append("Sub-Extracción") }
        else { labels.append("Ventana Óptima") }
        if effectiveRatio < 13 { labels.append("Cuerpo Denso") }
        else if effectiveRatio > 17 { labels.append("Alta Claridad") }
        if tempEffective < 88 { labels.append("Acidez Brillante") }
        else if tempEffective > 94 { labels.append("Tono Tostado") }
        if timeSeconds < 105 { labels.append("Paso Rápido") }
        else if timeSeconds > 270 { labels.append("Contacto Prolongado") }
        if Float(temperature) > tBoil { labels.append("Hervor \(oneDecimal(tBoil))°C") }
        else if altitudeMeters >= 1800 { labels.append("Altitud \(altitudeMeters)m") }
        switch freshnessState {
        case "muy fresco": labels.append("Bloom Largo")
        case "en ventana", "punto ideal": labels.append("Grano en Punto")
        case "viejo": labels.append("Desgasificado")
        default: break
        }

        let summary: String
        if Float(temperature) > tBoil {
            summary = "A \(altitudeMeters) msnm el agua hierve a \(oneDecimal(tBoil))°C. La temperatura está acotada al hervor; muele más fino para potenciar extracción."
        } else if bitterness >= 60 {
            summary = "Extracción intensa con perfil seco/amargo pronunciado; disminuye temperatura o engruesa la molienda."
        } else if acidity >= 68 && sweetness < 50 {
            summary = "Acidez dominante con sub-extracción; aumenta temperatura o afina la molienda."
        } else if sweetness >= 65 && bitterness < 45 {
            summary = "Taza balanceada con dulzor redondo y acidez perfectamente integrada."
        } else if body >= 65 {
            summary = "Sensación táctil densa y untuosa con postgusto prolongado."
        } else if body <= 35 {
            summary = "Taza ligera y cristalina con marcada separación aromática."
        } else {
            summary = "Perfil armónico y equilibrado con desarrollo limpio de sabores."
        }

        _ = coffeeGrams
        _ = waterMl
        return LabFlavorProfile(
            aroma: aroma, acidity: acidity, sweetness: sweetness, body: body,
            bitterness: bitterness, finish: finish, extractionIndex: extractionIndex,
            labels: Array(labels.reduce(into: [String]()) { if !$0.contains($1) { $0.append($1) } }.prefix(3)),
            summary: summary
        )
    }

    static func diagnostic(for state: LabState) -> (extraction: String, recommendation: String, risks: [String]) {
        let tBoil = boilingPointC(altitudeMeters: state.altitudeMeters)
        let effectiveTemp = min(Float(state.temperatureC), tBoil)
        let extraction: String
        if (effectiveTemp >= 96 && state.grindClicks <= 12) || (state.timeSeconds > 270 && state.grindClicks <= 15) {
            extraction = "Sobre-extracción Extrema (Riesgo amargo/seco)"
        } else if (effectiveTemp < 86 && state.ratio <= 12) || (state.timeSeconds < 100 && state.grindClicks >= 24) {
            extraction = "Sub-extracción (Agria y salada)"
        } else if (88...94.5).contains(effectiveTemp) && (14...26).contains(state.grindClicks) && (120...240).contains(state.timeSeconds) {
            extraction = "Extracción Ideal del Barista"
        } else {
            extraction = "Hipótesis aceptable. Verifique molienda, tiempo y temperatura."
        }

        var risks: [String] = []
        if Float(state.temperatureC) > tBoil { risks.append("A \(state.altitudeMeters)m el agua hierve a \(oneDecimal(tBoil))°C; la temperatura real queda limitada.") }
        if effectiveTemp > 95 { risks.append("La temperatura alta puede evaporar notas florales y dejar amargor.") }
        if effectiveTemp < 87 { risks.append("La temperatura baja puede acentuar una acidez frágil.") }
        if state.grindClicks < 13 { risks.append("La molienda fina puede obstruir el paso y causar astringencia.") }
        if state.grindClicks > 28 { risks.append("La molienda gruesa puede dar canalización y una taza aguada.") }
        if state.timeSeconds > 270 { risks.append("Un tiempo mayor a 4:30 puede saturar amargor y taninos.") }
        if state.timeSeconds < 100 { risks.append("Un tiempo menor a 1:40 puede dejar el café sub-extraído.") }
        if state.freshness == "muy fresco" { risks.append("El grano joven necesita una preinfusión larga de 50 s.") }
        if state.freshness == "viejo" { risks.append("El grano desgasificado puede requerir más temperatura y molienda fina.") }

        let recommendation: String
        if Float(state.temperatureC) > tBoil { recommendation = "Muele 1 click más fino para compensar la menor energía térmica." }
        else if state.altitudeMeters >= 2000 { recommendation = "Ajusta la molienda fina para retener dulzor en altitud elevada." }
        else if state.ratio <= 3 { recommendation = "Esta hipótesis corta produce alta concentración de aceites." }
        else if state.freshness == "muy fresco" { recommendation = "Aumenta el bloom para drenar dióxido de carbono." }
        else if effectiveTemp >= 94 { recommendation = "Vierte suave para evitar agitación y astringencia." }
        else { recommendation = "Mantén el ciclo preparar → probar → diagnosticar → ajustar." }
        return (extraction, recommendation, risks)
    }

    private static func clampScore(_ value: Float) -> Int { min(96, max(8, Int(roundf(value)))) }
    private static func oneDecimal(_ value: Float) -> String { String(format: "%.1f", locale: Locale(identifier: "en_US_POSIX"), value) }
}

final class LabModel: ObservableObject {
    @Published var state: LabState { didSet { persist() } }
    private let defaults: UserDefaults
    private let storageKey = "cupa.labState.v1"

    static let cities = [
        CoffeeCity(label: "Costa / Mar", altitudeMeters: 0, name: "Costa (0m)"),
        CoffeeCity(label: "Seattle / Tokio", altitudeMeters: 50, name: "Seattle/Tokio (50m)"),
        CoffeeCity(label: "Roma / París", altitudeMeters: 100, name: "Roma/París (100m)"),
        CoffeeCity(label: "São Paulo", altitudeMeters: 760, name: "São Paulo (760m)"),
        CoffeeCity(label: "San José CR", altitudeMeters: 1170, name: "San José (1,170m)"),
        CoffeeCity(label: "Medellín", altitudeMeters: 1495, name: "Medellín (1,495m)"),
        CoffeeCity(label: "Guatemala", altitudeMeters: 1500, name: "Guatemala (1,500m)"),
        CoffeeCity(label: "CDMX / Oaxaca", altitudeMeters: 2240, name: "CDMX (2,240m)"),
        CoffeeCity(label: "Addis Abeba", altitudeMeters: 2355, name: "Addis Abeba (2,355m)"),
        CoffeeCity(label: "Bogotá", altitudeMeters: 2600, name: "Bogotá (2,600m)"),
        CoffeeCity(label: "Cusco", altitudeMeters: 3399, name: "Cusco (3,399m)"),
        CoffeeCity(label: "La Paz", altitudeMeters: 3640, name: "La Paz (3,640m)")
    ]

    var profile: LabFlavorProfile { LabEngine.calculate(state) }
    var diagnostic: (extraction: String, recommendation: String, risks: [String]) { LabEngine.diagnostic(for: state) }
    var boilingPointC: Float { LabEngine.boilingPointC(altitudeMeters: state.altitudeMeters) }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        if let data = defaults.data(forKey: storageKey), let decoded = try? JSONDecoder().decode(LabState.self, from: data) {
            state = decoded
        } else {
            state = LabState()
        }
    }

    func update(_ change: (inout LabState) -> Void) { change(&state) }
    func selectCity(_ city: CoffeeCity) { update { $0.altitudeMeters = city.altitudeMeters; $0.cityName = city.name } }
    func setManualAltitude(_ meters: Int, city: String? = nil) {
        let clamped = min(5000, max(0, meters))
        update { $0.altitudeMeters = clamped; $0.cityName = city?.isEmpty == false ? "\(city!) (\(clamped)m)" : "Manual (\(clamped)m)" }
    }
    @MainActor func load(calculator: CalculatorModel) {
        update {
            $0.methodId = nil
            $0.recipeId = nil
            $0.techniqueId = nil
            $0.recipeName = nil
            $0.techniqueName = nil
            $0.method = calculator.method
            $0.coffeeGrams = Float(calculator.coffee)
            $0.waterMl = calculator.water
            $0.ratio = Float(calculator.ratio)
        }
    }

    func load(recipe: RecipeRecord, ingredients: [RecipeIngredientRecord]) {
        let coffee = (ingredients.first { Self.isCoffee($0) } ?? ingredients.first { Self.isGramUnit($0) })?.amount
        let water = (ingredients.first { Self.isWater($0) } ?? ingredients.first { Self.isMilliliterUnit($0) })?.amount
        update {
            $0.recipeId = recipe.id; $0.techniqueId = nil
            $0.recipeName = recipe.name; $0.techniqueName = nil
            $0.methodId = recipe.suggestedMethodId
            if !recipe.suggestedMethodName.isEmpty { $0.method = recipe.suggestedMethodName }
            if let coffee, coffee > 0 { $0.coffeeGrams = Float(coffee) }
            if let water, water > 0 { $0.waterMl = Int(water.rounded()) }
            if let coffee, let water, coffee > 0 { $0.ratio = Float(water / coffee) }
        }
    }

    func load(technique: TechniqueRecord, recipeName: String? = nil) {
        update {
            $0.techniqueId = technique.id; $0.recipeId = technique.recipeId
            $0.techniqueName = technique.name; $0.recipeName = recipeName
            $0.methodId = technique.methodId; $0.beanId = technique.beanId; $0.grinderId = technique.grinderId
            $0.method = technique.methodName; $0.coffeeGrams = Float(technique.doseGrams)
            $0.waterMl = Int(technique.waterMl); $0.ratio = Float(technique.ratio)
            $0.temperatureC = Int(technique.temperatureC)
            if technique.grindUnit == "CLICKS" { $0.grindClicks = min(50, max(4, Int(technique.grindValue.rounded()))) }
            if technique.totalTimeSeconds > 0 { $0.timeSeconds = Int(technique.totalTimeSeconds) }
        }
    }

    func selectMethod(_ method: EquipmentRecord?) {
        update { $0.methodId = method?.id; if let method { $0.method = method.name } }
    }
    func load(experiment: LabExperimentRecord) {
        update {
            $0.methodId = experiment.methodId; $0.recipeId = experiment.recipeId; $0.techniqueId = experiment.techniqueId
            $0.beanId = experiment.beanId; $0.grinderId = experiment.grinderId; $0.recipeName = nil; $0.techniqueName = nil
            $0.method = experiment.method; $0.coffeeGrams = Float(experiment.coffeeGrams); $0.waterMl = Int(experiment.waterMl)
            $0.ratio = Float(experiment.ratio); $0.temperatureC = Int(experiment.temperatureC); $0.grindClicks = Int(experiment.grindClicks)
            $0.freshness = experiment.freshness; $0.timeSeconds = Int(experiment.timeSeconds); $0.notes = experiment.notes
            $0.altitudeMeters = Int(experiment.altitudeMeters); $0.cityName = experiment.cityName
        }
    }
    func reset() { state = LabState(temperatureUnit: state.temperatureUnit) }

    private func persist() {
        if let data = try? JSONEncoder().encode(state) { defaults.set(data, forKey: storageKey) }
    }

    private static func isCoffee(_ ingredient: RecipeIngredientRecord) -> Bool {
        let value = "\(ingredient.name) \(ingredient.unit)".folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current).lowercased()
        return value.contains("cafe") || value.contains("coffee")
    }
    private static func isWater(_ ingredient: RecipeIngredientRecord) -> Bool {
        let value = "\(ingredient.name) \(ingredient.unit)".folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current).lowercased()
        return value.contains("agua") || value.contains("water")
    }
    private static func isGramUnit(_ ingredient: RecipeIngredientRecord) -> Bool {
        ingredient.unit.uppercased().contains("GRAM")
    }
    private static func isMilliliterUnit(_ ingredient: RecipeIngredientRecord) -> Bool {
        let unit = ingredient.unit.uppercased()
        return unit == "ML" || unit.contains("MILLILIT") || unit.contains("MILILIT")
    }
}
