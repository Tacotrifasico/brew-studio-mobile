import Foundation

enum TemperatureUnit: String, Codable, CaseIterable {
    case celsius
    case fahrenheit

    var symbol: String { self == .celsius ? "°C" : "°F" }

    func displayValue(celsius: Double) -> Double {
        self == .fahrenheit ? celsius * 9 / 5 + 32 : celsius
    }

    func celsius(displayValue: Double) -> Double {
        self == .fahrenheit ? (displayValue - 32) * 5 / 9 : displayValue
    }

    func text(celsius: Double) -> String {
        "\(displayValue(celsius: celsius).rounded().formatted(.number.precision(.fractionLength(0)))) \(symbol)"
    }
}

struct CoffeeCity: Identifiable, Equatable {
    let label: String
    let altitudeMeters: Int
    let name: String
    var id: String { "\(name)-\(altitudeMeters)" }

    func isSelected(altitudeMeters: Int, cityName: String) -> Bool {
        self.altitudeMeters == altitudeMeters && (cityName == name || cityName.hasPrefix(label))
    }
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
    // Optional keeps old saved drafts readable; Celsius is the canonical value.
    var preciseTemperatureC: Double?
    var effectiveTemperatureC: Double { preciseTemperatureC ?? Double(temperatureC) }
    var grindClicks = 24
    var freshness = "en ventana"
    var timeSeconds = 180
    var notes = ""
    var altitudeMeters = 0
    var cityName = "Nivel del mar (0m)"
    var temperatureUnit = TemperatureUnit.celsius
}

// Mirror of Android LabTemperatureGuide: whole display degrees, canonical Celsius,
// altitude-aware useful window and risk language rather than promised flavors.
struct LabTemperatureGuide {
    let temperatureC: Double
    let altitudeMeters: Int
    var unit: TemperatureUnit = .celsius
    var boilingC: Double { 100 - Double(min(5000, max(0, altitudeMeters))) * 0.0034 }
    var upperC: Double { min(96, boilingC) }
    var lowerC: Double { max(80, min(90, upperC - 6)) }
    func degrees(_ celsius: Double) -> Int { Int(unit.displayValue(celsius: celsius).rounded()) }
    var recommendedRange: ClosedRange<Double> { Double(degrees(lowerC))...Double(degrees(upperC)) }
    var rangeText: String { "\(degrees(lowerC))–\(degrees(upperC)) \(unit.symbol)" }
    var sliderRange: ClosedRange<Double> { unit == .fahrenheit ? 176...208 : 80...98 }
    var leftShare: Double { recommendedRange.lowerBound <= sliderRange.lowerBound ? 0 : recommendedRange.upperBound >= sliderRange.upperBound ? 0.5 : 0.25 }
    // Same 25% / 50% / 25% magnification as Android's calibrated slider.
    func fraction(_ value: Double) -> Double {
        let low = recommendedRange.lowerBound, high = recommendedRange.upperBound
        let value = min(sliderRange.upperBound, max(sliderRange.lowerBound, value))
        if value <= low { return low > sliderRange.lowerBound ? (value - sliderRange.lowerBound) / (low - sliderRange.lowerBound) * leftShare : leftShare }
        if value >= high { return leftShare + 0.5 + (value - high) / (sliderRange.upperBound - high) * (0.5 - leftShare) }
        return leftShare + (value - low) / (high - low) * 0.5
    }
    func value(_ fraction: Double) -> Double {
        let f = min(1, max(0, fraction)), low = recommendedRange.lowerBound, high = recommendedRange.upperBound
        if f <= leftShare { return leftShare > 0 ? (sliderRange.lowerBound + f / leftShare * (low - sliderRange.lowerBound)).rounded() : low }
        if f >= leftShare + 0.5 { return (high + (f - leftShare - 0.5) / (0.5 - leftShare) * (sliderRange.upperBound - high)).rounded() }
        return (low + (f - leftShare) / 0.5 * (high - low)).rounded()
    }
    var warning: Bool { temperatureC > boilingC || !(degrees(lowerC)...degrees(upperC)).contains(degrees(temperatureC)) }
    var headline: String {
        if temperatureC > boilingC { return "Supera el hervor local" }
        if degrees(temperatureC) < degrees(lowerC - 3) { return "Agua demasiado fría" }
        if degrees(temperatureC) < degrees(lowerC) { return "Agua por debajo de la zona útil" }
        if degrees(temperatureC) > degrees(upperC) { return "Calor alto: vigila el amargor" }
        return "En zona útil"
    }
    var detail: String {
        switch headline {
        case "Supera el hervor local": return "A tu altura, el agua hierve antes de alcanzar esa temperatura. Ajusta molienda o tiempo."
        case "Agua demasiado fría": return "Riesgo de subextracción: taza agria o débil. Sube la temperatura hacia la zona útil; valida el resultado en Cata."
        case "Agua por debajo de la zona útil": return "Puede faltar extracción y dulzor. Sube hacia la zona útil o compensa con molienda y tiempo."
        case "Calor alto: vigila el amargor": return "Puede aumentar el amargor o la sequedad. Prueba bajar hacia la zona útil, especialmente con tueste oscuro."
        default: return "Buen punto de partida; molienda, tiempo y grano también definen el sabor."
        }
    }
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
            temperature: state.effectiveTemperatureC,
            grindClicks: state.grindClicks,
            freshnessState: state.freshness,
            altitudeMeters: state.altitudeMeters,
            timeSeconds: state.timeSeconds,
            temperatureUnit: state.temperatureUnit
        )
    }

    // Port literal de calculateLabProfile en LabScreen.kt (commit aff626e).
    static func calculate(
        coffeeGrams: Float,
        waterMl: Int,
        ratio: Float,
        temperature: Double,
        grindClicks: Int,
        freshnessState: String,
        altitudeMeters: Int = 0,
        timeSeconds: Int = 180,
        temperatureUnit: TemperatureUnit = .celsius
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
        let thermal = LabTemperatureGuide(temperatureC: temperature, altitudeMeters: altitudeMeters, unit: temperatureUnit)
        if thermal.warning { labels.insert(thermal.headline, at: 0); labels.removeAll { $0 == "Ventana Óptima" || $0 == "Acidez Brillante" } }
        switch freshnessState {
        case "muy fresco": labels.append("Bloom Largo")
        case "en ventana", "punto ideal": labels.append("Grano en Punto")
        case "viejo": labels.append("Desgasificado")
        default: break
        }

        let summary: String
        if thermal.warning {
            summary = thermal.detail
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
        let effectiveTemp = min(Float(state.effectiveTemperatureC), tBoil)
        let thermal = LabTemperatureGuide(temperatureC: state.effectiveTemperatureC, altitudeMeters: state.altitudeMeters, unit: state.temperatureUnit)
        let extraction: String
        if thermal.warning {
            extraction = thermal.headline
        } else if (effectiveTemp >= 96 && state.grindClicks <= 12) || (state.timeSeconds > 270 && state.grindClicks <= 15) {
            extraction = "Sobre-extracción Extrema (Riesgo amargo/seco)"
        } else if (effectiveTemp < 86 && state.ratio <= 12) || (state.timeSeconds < 100 && state.grindClicks >= 24) {
            extraction = "Sub-extracción (Agria y salada)"
        } else if (88...94.5).contains(effectiveTemp) && (14...26).contains(state.grindClicks) && (120...240).contains(state.timeSeconds) {
            extraction = "Extracción Ideal del Barista"
        } else {
            extraction = "Hipótesis aceptable. Verifique molienda, tiempo y temperatura."
        }

        var risks: [String] = []
        if Float(state.effectiveTemperatureC) > tBoil { risks.append("A \(state.altitudeMeters)m el agua hierve a \(oneDecimal(tBoil))°C; la temperatura real queda limitada.") }
        if effectiveTemp > 95 { risks.append("La temperatura alta puede evaporar notas florales y dejar amargor.") }
        if thermal.warning { risks.insert(thermal.detail, at: 0) }
        if state.grindClicks < 13 { risks.append("La molienda fina puede obstruir el paso y causar astringencia.") }
        if state.grindClicks > 28 { risks.append("La molienda gruesa puede dar canalización y una taza aguada.") }
        if state.timeSeconds > 270 { risks.append("Un tiempo mayor a 4:30 puede saturar amargor y taninos.") }
        if state.timeSeconds < 100 { risks.append("Un tiempo menor a 1:40 puede dejar el café sub-extraído.") }
        if state.freshness == "muy fresco" { risks.append("El grano joven necesita una preinfusión larga de 50 s.") }
        if state.freshness == "viejo" { risks.append("El grano desgasificado puede requerir más temperatura y molienda fina.") }

        let recommendation: String
        if thermal.warning { recommendation = thermal.detail }
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
    private var scopeOwnerId: UUID?
    private let storageKeyBase = "cupa.labState.v1"
    private var storageKey: String { LocalDataScope.scopedKey(storageKeyBase, ownerId: scopeOwnerId) }
    private let temperaturePreferenceKey = "settings.temperature"

    static let cities = [
        CoffeeCity(label: "Costa / Mar", altitudeMeters: 0, name: "Costa (0m)"),
        CoffeeCity(label: "Seattle / Tokio", altitudeMeters: 50, name: "Seattle/Tokio (50m)"),
        CoffeeCity(label: "Roma / Paris", altitudeMeters: 100, name: "Roma/París (100m)"),
        CoffeeCity(label: "São Paulo", altitudeMeters: 760, name: "São Paulo (760m)"),
        CoffeeCity(label: "Medellín", altitudeMeters: 1495, name: "Medellín (1,495m)"),
        CoffeeCity(label: "Guatemala", altitudeMeters: 1500, name: "Guatemala (1,500m)"),
        CoffeeCity(label: "San José CR", altitudeMeters: 1170, name: "San José (1,170m)"),
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
        self.defaults = defaults; scopeOwnerId = LocalDataScope.activeOwnerId
        let baseKey = "cupa.labState.v1"; let scopedKey = LocalDataScope.scopedKey(baseKey, ownerId: scopeOwnerId)
        let stored = defaults.object(forKey: scopedKey) ?? LocalDataScope.migrateLegacyObject(in: defaults, baseKey: baseKey, ownerId: scopeOwnerId)
        var restored = (stored as? Data).flatMap { try? JSONDecoder().decode(LabState.self, from: $0) } ?? LabState()
        Self.normalizeQuantities(&restored)
        if let rawUnit = defaults.string(forKey: temperaturePreferenceKey), let unit = TemperatureUnit(rawValue: rawUnit) {
            restored.temperatureUnit = unit
        } else {
            // Migra la preferencia que versiones anteriores guardaban sólo dentro del estado del Laboratorio.
            defaults.set(restored.temperatureUnit.rawValue, forKey: temperaturePreferenceKey)
        }
        state = restored
    }

    func switchScope(to ownerId: UUID?) {
        guard scopeOwnerId != ownerId else { return }
        scopeOwnerId = ownerId
        let stored = defaults.object(forKey: storageKey) ?? LocalDataScope.migrateLegacyObject(in: defaults, baseKey: storageKeyBase, ownerId: ownerId)
        var restored = (stored as? Data).flatMap { try? JSONDecoder().decode(LabState.self, from: $0) } ?? LabState()
        Self.normalizeQuantities(&restored)
        restored.temperatureUnit = TemperatureUnit(rawValue: defaults.string(forKey: temperaturePreferenceKey) ?? "") ?? .celsius
        state = restored
    }

    func update(_ change: (inout LabState) -> Void) { change(&state) }
    func setCoffeeGrams(_ grams: Float) {
        update {
            $0.coffeeGrams = grams.rounded()
            $0.waterMl = Int(($0.coffeeGrams * $0.ratio).rounded())
            Self.normalizeQuantities(&$0)
        }
    }
    func setWaterMl(_ milliliters: Int) {
        update {
            $0.waterMl = milliliters
            Self.normalizeQuantities(&$0)
        }
    }
    func setRatio(_ ratio: Float) {
        update {
            $0.waterMl = Int(($0.coffeeGrams * ratio.rounded()).rounded())
            Self.normalizeQuantities(&$0)
        }
    }
    func setDisplayedTemperature(_ degrees: Double) {
        let celsius = state.temperatureUnit.celsius(displayValue: degrees.rounded())
        update { $0.preciseTemperatureC = celsius; $0.temperatureC = Int(celsius.rounded()) }
    }
    func setTemperatureUnit(_ unit: TemperatureUnit) {
        defaults.set(unit.rawValue, forKey: temperaturePreferenceKey)
        update { $0.temperatureUnit = unit }
    }
    func selectCity(_ city: CoffeeCity) { update { $0.altitudeMeters = city.altitudeMeters; $0.cityName = city.name } }
    func setManualAltitude(_ meters: Int, city: String? = nil) {
        let clamped = min(5000, max(0, meters))
        update { $0.altitudeMeters = clamped; $0.cityName = city?.isEmpty == false ? "\(city!) (\(clamped)m)" : "Manual (\(clamped)m)" }
    }
    @MainActor func load(calculator: CalculatorModel) {
        update {
            $0.methodId = calculator.selectedMethodId
            $0.recipeId = nil
            $0.techniqueId = nil
            $0.recipeName = nil
            $0.techniqueName = nil
            $0.method = calculator.method
            $0.beanId = calculator.selectedBeanId
            $0.temperatureC = calculator.selectedBeanProfile?.displayDegrees(fahrenheit: false) ?? 93
            $0.preciseTemperatureC = calculator.selectedBeanProfile?.temperatureC ?? 93
            $0.grindClicks = calculator.selectedBeanProfile?.clicks ?? 18
            $0.coffeeGrams = Float(calculator.coffee)
            $0.waterMl = calculator.water
            Self.normalizeQuantities(&$0)
        }
    }

    func load(bean: CoffeeBeanRecord, now: Date = .now, calendar: Calendar = .current) {
        let freshness = CoffeeFreshnessEngine.evaluate(roastDate: bean.roastDate, openedDate: bean.openedDate, now: now, calendar: calendar)
        let freshnessLabel: String = switch freshness.state {
        case .veryFresh: "muy fresco"
        case .inWindow: "en ventana"
        case .ideal: "punto ideal"
        case .declining: "bajando"
        case .old: "viejo"
        case .noDate: "en ventana"
        }
        let details = ["Grano: \(bean.name)", bean.process.isEmpty ? nil : "Proceso: \(bean.process)", bean.notes.isEmpty ? nil : bean.notes]
            .compactMap { $0 }.joined(separator: ". ")
        update {
            $0.beanId = bean.id
            $0.freshness = freshnessLabel
            $0.notes = details
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
            $0.waterMl = Int(technique.waterMl); Self.normalizeQuantities(&$0)
            $0.temperatureC = Int(technique.temperatureC)
            $0.preciseTemperatureC = Double(technique.temperatureC)
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
            Self.normalizeQuantities(&$0); $0.temperatureC = Int(experiment.temperatureC); $0.grindClicks = Int(experiment.grindClicks)
            $0.preciseTemperatureC = experiment.effectiveTemperatureC
            $0.freshness = experiment.freshness; $0.timeSeconds = Int(experiment.timeSeconds); $0.notes = experiment.notes
            // Historical altitude stays in the experiment; live preparation uses Settings.
        }
    }
    func load(tasting: TastingState, brew: BrewSessionRecord?) {
        update {
            if let brew {
                $0.methodId = brew.methodId; $0.recipeId = brew.recipeId; $0.techniqueId = brew.techniqueId
                $0.beanId = brew.beanId; $0.grinderId = brew.grinderId
                $0.recipeName = brew.recipeNameSnapshot.isEmpty ? nil : brew.recipeNameSnapshot
                $0.techniqueName = brew.techniqueNameSnapshot.isEmpty ? nil : brew.techniqueNameSnapshot
                $0.method = brew.methodNameSnapshot; $0.coffeeGrams = Float(brew.doseGrams); $0.waterMl = Int(brew.waterMl)
                Self.normalizeQuantities(&$0); $0.temperatureC = Int(brew.temperatureC)
                $0.preciseTemperatureC = brew.effectiveTemperatureC
                if let clicks = Self.firstInteger(in: brew.grindDescription) { $0.grindClicks = min(50, max(4, clicks)) }
                if brew.elapsedSeconds > 0 { $0.timeSeconds = Int(brew.elapsedSeconds) }
            }
            $0.notes = "Cargado de cata sensorial. Textura: \(tasting.texture), Limpieza: \(tasting.cleanliness)."
        }
    }
    func reset() { state = LabState(altitudeMeters: state.altitudeMeters, cityName: state.cityName, temperatureUnit: state.temperatureUnit) }

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
    private static func firstInteger(in value: String) -> Int? {
        value.split(whereSeparator: { !$0.isNumber }).first.flatMap { Int($0) }
    }
    private static func normalizeQuantities(_ state: inout LabState) {
        guard state.coffeeGrams > 0 else { return }
        state.ratio = Float(state.waterMl) / state.coffeeGrams
    }
}
