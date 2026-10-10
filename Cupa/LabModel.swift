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

// Recipe references, not universal sensory thresholds. Mirror: Android LabTemperatureGuide.
struct LabTemperatureGuide {
    let temperatureC: Double
    let altitudeMeters: Int
    var unit: TemperatureUnit = .celsius
    var method = "V60"
    var kind: String {
        switch method.lowercased().trimmingCharacters(in: .whitespacesAndNewlines) {
        case "v60": return "filter"; case "chemex": return "chemex"
        case "prensa francesa", "french press": return "press"; case "aeropress": return "aero"
        case "espresso": return "espresso"; case "moka", "moka italiana": return "moka"
        case "cold brew", "coldbrew": return "cold"; default: return "custom"
        }
    }
    // Approximation, not measured pressure. Never cap a pressurized brewer.
    var boilingC: Double { 100 - Double(min(5000, max(0, altitudeMeters))) * 0.0034 }
    var openHotWater: Bool { ["filter", "chemex", "press", "aero"].contains(kind) }
    var lowerC: Double { switch kind { case "filter", "press": return 92; case "chemex": return (200.0 - 32) / 1.8; case "aero": return 80; case "espresso": return 90.5; default: return 0 } }
    var upperC: Double { switch kind { case "filter", "press": return 96; case "chemex": return (200.0 - 32) / 1.8; case "aero": return 85; case "espresso": return 96.1; default: return 100 } }
    var hasReference: Bool { !["moka", "cold", "custom"].contains(kind) }
    var hasReachableBand: Bool { hasReference && (!openHotWater || boilingC >= lowerC) }
    func degrees(_ celsius: Double) -> Int { Int(unit.displayValue(celsius: celsius).rounded()) }
    var sliderRange: ClosedRange<Double> {
        let low: Double = kind == "cold" || kind == "custom" ? 0 : kind == "moka" ? 20 : 70
        return Double(degrees(low))...Double(degrees(kind == "cold" ? 35 : 100))
    }
    var recommendedRange: ClosedRange<Double> {
        hasReachableBand ? Double(degrees(lowerC))...Double(degrees(openHotWater ? min(upperC, boilingC) : upperC)) : sliderRange
    }
    var rangeText: String {
        if !hasReference { return "Sin intervalo universal" }
        if lowerC == upperC { return "≈ \(degrees(lowerC)) \(unit.symbol)" }
        return "\(degrees(lowerC))–\(degrees(upperC)) \(unit.symbol)"
    }
    var workingRangeText: String {
        if !hasReference { return "Según técnica" }
        if !hasReachableBand { return "Sin zona a esta altura" }
        let low = Int(recommendedRange.lowerBound), high = Int(recommendedRange.upperBound)
        if kind == "aero" { return "Puntos · \(low) / \(high) \(unit.symbol)" }
        if low == high { return "Punto · \(low) \(unit.symbol)" }
        return "Zona de trabajo · \(low)–\(high) \(unit.symbol)"
    }
    var sourceName: String {
        switch kind {
        case "filter": return "Hario · receta V60"; case "press": return "Bodum · prensa"
        case "chemex": return "Chemex · punto de partida"; case "aero": return "AeroPress · por tueste"
        case "espresso": return "SCAA · referencia histórica"; case "moka": return "Bialetti · manejo del calor"
        case "cold": return "Toddy · protocolo ambiente"; default: return "Método personalizado"
        }
    }
    var sourceURL: String {
        switch kind {
        case "filter": return "https://www.hario.co.uk/pages/brew-guides-v60-expert"
        case "press": return "https://www.bodum.com/es/es/1918-913-caffettiera"
        case "chemex": return "https://assets.unilogcorp.com/187/ITEM/DOC/CHEMEX_102422786_Instruction_Installation_Manual.pdf"
        case "aero": return "https://aeropress.com/pages/whats-the-optimal-brewing-temperature-for-aeropress-coffee-makers"
        case "espresso": return "https://sca.coffee/sca-news/25-magazine/issue-3/defining-ever-changing-espresso-25-magazine-issue-3-zyx36"
        case "moka": return "https://bialetti-cookware.zendesk.com/hc/en-us/articles/5416235346322-How-to-use-the-Moka-Express"
        case "cold": return "https://toddycafe.com/cold-brew/instruction-manual"; default: return ""
        }
    }
    var warning: Bool { (openHotWater && temperatureC > boilingC) || (hasReference && !(degrees(lowerC)...degrees(upperC)).contains(degrees(temperatureC))) }
    var headline: String {
        if openHotWater && temperatureC > boilingC { return "Supera el hervor local" }
        if !hasReachableBand && hasReference { return "Referencia por encima del hervor" }
        if kind == "moka" { return "Controla la llama, no una zona V60" }
        if kind == "cold" { return "Extracción en frío: manda el tiempo" }
        if kind == "custom" { return "Falta una referencia para este método" }
        if kind == "chemex" { return warning ? "Difiere del punto Chemex" : "Cerca del punto Chemex" }
        if kind == "aero" { return warning ? "Otra receta AeroPress" : "Referencias AeroPress por tueste" }
        if degrees(temperatureC) < degrees(lowerC) { return "Calor bajo para \(method)" }
        if degrees(temperatureC) > degrees(upperC) { return "Calor alto para \(method)" }
        return "Calor en zona de trabajo"
    }
    var compactDetail: String {
        if openHotWater && temperatureC > boilingC { return "Usa el hervor estimado; compensa con molienda o tiempo." }
        if !hasReachableBand && hasReference { return "No inventamos otro rango. Ajusta molienda/tiempo y cata." }
        if kind == "aero" { return "Oscuro: \(degrees(80)); medio/claro: \(degrees(85)) \(unit.symbol). Otras técnicas usan más calor." }
        if kind == "moka" { return "Llama baja/media. Retira al terminar; no es temperatura de vertido." }
        if kind == "cold" { return "Agua ambiente · 8–24 h. Para refrigeración, elige otra técnica." }
        if kind == "custom" { return "Elige una técnica documentada; no heredamos V60." }
        if kind == "chemex" { return "Punto aproximado, no intervalo óptimo. Confirma en Cata." }
        if degrees(temperatureC) < degrees(lowerC) { return "Menos calor puede ralentizar la extracción: prueba menos gruesa o más tiempo." }
        if degrees(temperatureC) > degrees(upperC) { return "Más calor puede acelerar extracción; no garantiza amargor." }
        return "Punto de partida. Confirma el sabor en Cata."
    }
    var detail: String {
        if openHotWater && temperatureC > boilingC { return "A tu altura, el agua hierve antes. Usa el hervor estimado como límite; prueba molienda menos gruesa o más tiempo." }
        if !hasReachableBand && hasReference { return "La altura impide alcanzar esta referencia con agua abierta. No inventamos otro rango: ajusta molienda y tiempo, y compara en Cata." }
        if kind == "aero" { return "Oscuro: \(degrees(80)) \(unit.symbol); medio/claro: \(degrees(85)) \(unit.symbol). Son puntos de partida, no límites; otras técnicas usan más calor." }
        if kind == "moka" { return "Llama baja/media; retira al terminar. La temperatura inicial no describe la extracción interna bajo presión." }
        if kind == "cold" { return "Toddy indica agua ambiente y 8–24 h. En refrigeración usa una técnica específica; no aplican los minutos del filtrado caliente." }
        if kind == "custom" { return "Carga o elige una técnica documentada. No heredamos rangos ni consejos de V60." }
        if kind == "chemex" { return "El manual propone aproximadamente \(degrees(lowerC)) \(unit.symbol); no define un intervalo óptimo universal. Confirma en Cata." }
        if degrees(temperatureC) < degrees(lowerC) { return "Menos calor puede ralentizar la extracción. Prueba subir hacia los puntos orientativos, molienda menos gruesa o más tiempo; confirma en Cata." }
        if degrees(temperatureC) > degrees(upperC) { return "Más calor puede acelerar la extracción; no demuestra amargor. Compara con los puntos orientativos y cambia una variable por vez." }
        return "Punto de partida, no garantía de equilibrio. El sabor real se registra en Cata."
    }
    // Linear scale for absent or single-point references: no division by zero or fake band.
    var magnified: Bool { hasReachableBand && !["aero", "chemex"].contains(kind) && recommendedRange.lowerBound < recommendedRange.upperBound }
    var leftShare: Double { magnified ? (recommendedRange.lowerBound <= sliderRange.lowerBound ? 0 : recommendedRange.upperBound >= sliderRange.upperBound ? 0.5 : 0.25) : 0 }
    func fraction(_ value: Double) -> Double {
        let low = recommendedRange.lowerBound, high = recommendedRange.upperBound
        let v = min(sliderRange.upperBound, max(sliderRange.lowerBound, value))
        if !magnified { return (v - sliderRange.lowerBound) / (sliderRange.upperBound - sliderRange.lowerBound) }
        if v <= low { return low > sliderRange.lowerBound ? (v - sliderRange.lowerBound) / (low - sliderRange.lowerBound) * leftShare : leftShare }
        if v >= high { return high < sliderRange.upperBound ? leftShare + 0.5 + (v - high) / (sliderRange.upperBound - high) * (0.5 - leftShare) : 1 }
        return leftShare + (v - low) / (high - low) * 0.5
    }
    func value(_ fraction: Double) -> Double {
        let f = min(1, max(0, fraction)), low = recommendedRange.lowerBound, high = recommendedRange.upperBound
        if !magnified { return (sliderRange.lowerBound + f * (sliderRange.upperBound - sliderRange.lowerBound)).rounded() }
        if f <= leftShare { return leftShare > 0 ? (sliderRange.lowerBound + f / leftShare * (low - sliderRange.lowerBound)).rounded() : low }
        if f >= leftShare + 0.5 { return 0.5 > leftShare ? (high + (f - leftShare - 0.5) / (0.5 - leftShare) * (sliderRange.upperBound - high)).rounded() : high }
        return (low + (f - leftShare) / 0.5 * (high - low)).rounded()
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
            temperatureUnit: state.temperatureUnit,
            method: state.method
        )
    }

    // Legacy visual heuristic, mirrored in Android. Not experimentally calibrated.
    // Never expose these scores as percentages, extraction yield or documented facts.
    static func calculate(
        coffeeGrams: Float,
        waterMl: Int,
        ratio: Float,
        temperature: Double,
        grindClicks: Int,
        freshnessState: String,
        altitudeMeters: Int = 0,
        timeSeconds: Int = 180,
        temperatureUnit: TemperatureUnit = .celsius,
        method: String = "V60"
    ) -> LabFlavorProfile {
        let effectiveRatio = min(30, max(5, ratio > 0 ? ratio : 16))
        let tBoil = boilingPointC(altitudeMeters: altitudeMeters)
        let reference = LabTemperatureGuide(temperatureC: temperature, altitudeMeters: altitudeMeters, unit: temperatureUnit, method: method)
        let tempEffective = reference.openHotWater ? min(Float(temperature), tBoil) : Float(temperature)
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

        let thermal = reference
        let labels = [thermal.headline, "Hipótesis visual · no medición"]
        let summary = thermal.detail
        // Stored legacy scores remain an unvalidated illustration, never a measured
        // extraction yield or a guaranteed flavor. Neutral outside the hot-water model.
        let supported = ["filter", "chemex", "press", "aero"].contains(reference.kind)
        _ = coffeeGrams
        _ = waterMl
        return LabFlavorProfile(
            aroma: supported ? aroma : 0, acidity: supported ? acidity : 0, sweetness: supported ? sweetness : 0, body: supported ? body : 0,
            bitterness: supported ? bitterness : 0, finish: supported ? finish : 0, extractionIndex: extractionIndex,
            labels: Array(labels.reduce(into: [String]()) { if !$0.contains($1) { $0.append($1) } }.prefix(3)),
            summary: summary
        )
    }

    static func diagnostic(for state: LabState) -> (extraction: String, recommendation: String, risks: [String]) {
        let thermal = LabTemperatureGuide(temperatureC: state.effectiveTemperatureC, altitudeMeters: state.altitudeMeters, unit: state.temperatureUnit, method: state.method)
        return (thermal.headline, thermal.detail, thermal.warning ? [thermal.detail] : [])
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
    func selectMethod(_ method: String) {
        guard method != state.method else { return }
        update {
            $0.method = method; $0.methodId = nil; $0.techniqueId = nil
            let guide = LabTemperatureGuide(temperatureC: $0.effectiveTemperatureC, altitudeMeters: $0.altitudeMeters, method: method)
            if !guide.sliderRange.contains($0.effectiveTemperatureC) {
                $0.temperatureC = guide.kind == "cold" ? 20 : 92
                $0.preciseTemperatureC = Double($0.temperatureC)
            }
            switch guide.kind {
            case "cold": if !(3600...86400).contains($0.timeSeconds) { $0.timeSeconds = 43200 }
            case "espresso": if !(5...90).contains($0.timeSeconds) { $0.timeSeconds = 25 }
            default: if !(30...600).contains($0.timeSeconds) { $0.timeSeconds = 180 }
            }
        }
    }
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
        if let method { selectMethod(method.name) }
        update { $0.methodId = method?.id }
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
