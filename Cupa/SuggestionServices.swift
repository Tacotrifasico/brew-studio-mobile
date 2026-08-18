import Foundation

struct SuggestionContext: Codable, Equatable {
    let method: String; let coffeeGrams: Double; let waterMl: Int; let ratio: Double; let temperatureC: Int
    let grindClicks: Int; let timeSeconds: Int; let extractionIndex: Double
    let aroma: Int; let acidity: Int; let sweetness: Int; let body: Int; let bitterness: Int; let finish: Int

    init(state: LabState, profile: LabFlavorProfile) {
        method = state.method; coffeeGrams = Double(state.coffeeGrams); waterMl = state.waterMl; ratio = Double(state.ratio)
        temperatureC = state.temperatureC; grindClicks = state.grindClicks; timeSeconds = state.timeSeconds
        extractionIndex = Double(profile.extractionIndex); aroma = profile.aroma; acidity = profile.acidity
        sweetness = profile.sweetness; body = profile.body; bitterness = profile.bitterness; finish = profile.finish
    }

    var isValid: Bool {
        coffeeGrams > 0 && coffeeGrams <= 100 && waterMl > 0 && waterMl <= 2_000 && ratio >= 1 && ratio <= 40 &&
        (0...100).contains(aroma) && (0...100).contains(acidity) && (0...100).contains(sweetness) &&
        (0...100).contains(body) && (0...100).contains(bitterness) && (0...100).contains(finish)
    }
}

struct BrewSuggestion: Codable, Equatable {
    enum Source: String, Codable { case local, gemini }
    let text: String; let source: Source; let promptVersion: String
}

enum LocalSuggestionEngine {
    static func suggest(_ input: SuggestionContext) -> BrewSuggestion {
        let text: String
        if input.extractionIndex < 0.8 {
            text = "La extracción estimada es baja. Prueba una molienda ligeramente más fina o aumenta el tiempo, cambiando una sola variable."
        } else if input.extractionIndex > 1.2 || input.bitterness > 70 {
            text = "La extracción estimada es alta. Prueba una molienda ligeramente más gruesa o reduce el tiempo, sin cambiar la dosis."
        } else if input.acidity > input.sweetness + 15 {
            text = "La acidez domina al dulzor. Un vertido más estable o un pequeño aumento de temperatura puede favorecer el balance."
        } else {
            text = "El perfil está equilibrado. Conserva esta receta como base y ajusta una sola variable por preparación."
        }
        return .init(text: text, source: .local, promptVersion: "local-v1")
    }
}

struct GeminiSuggestionService {
    let configuration: AppConfiguration; let transport: NetworkTransport
    init(configuration: AppConfiguration = AppConfiguration(), transport: NetworkTransport = URLSessionTransport()) { self.configuration = configuration; self.transport = transport }

    func suggest(_ input: SuggestionContext, accessToken: String?) async -> BrewSuggestion {
        let fallback = LocalSuggestionEngine.suggest(input)
        guard input.isValid, let token = accessToken, let base = configuration.supabaseURL,
              let key = configuration.supabaseAnonKey, !key.isEmpty else { return fallback }
        do {
            let url = base.appendingPathComponent("functions/v1/gemini-suggestions")
            var request = URLRequest(url: url); request.httpMethod = "POST"; request.timeoutInterval = 20
            request.setValue(key, forHTTPHeaderField: "apikey"); request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            request.setValue("application/json", forHTTPHeaderField: "Content-Type"); request.httpBody = try JSONEncoder().encode(input)
            let (data, response) = try await transport.data(for: request)
            guard (200..<300).contains(response.statusCode), let decoded = try? JSONDecoder().decode(BrewSuggestion.self, from: data),
                  decoded.source == .gemini, !decoded.text.isEmpty, decoded.text.count <= 800 else { return fallback }
            return decoded
        } catch { return fallback }
    }
}
