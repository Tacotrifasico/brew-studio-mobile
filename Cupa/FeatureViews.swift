import SwiftUI
import CoreData

struct HomeView: View {
    @Binding var selection: CupaTab
    @ObservedObject var account: AccountModel
    @ObservedObject var settings: SettingsModel
    @State private var showAccount = false
    @State private var showSettings = false
    @State private var showHub = false

    private let shortcuts: [(String, String, CupaTab, Color)] = [
        ("Cata", "heart.text.square", .tasting, CupaTheme.terracotta),
        ("Laboratorio", "flask", .lab, CupaTheme.gold),
        ("Almacén", "shippingbox", .storage, CupaTheme.forest),
        ("Preparar", "mug", .brew, CupaTheme.terracotta)
    ]

    var body: some View {
        ZStack {
            CupaTheme.background.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 20) {
                    HStack {
                        Button { showHub = true } label: {
                            Label(account.tokens?.email ?? "Mi perfil", systemImage: "person.crop.circle").font(.subheadline.weight(.semibold))
                        }
                        .accessibilityIdentifier("home.profile")
                        Spacer()
                        Button { showSettings = true } label: { Image(systemName: "gearshape") }
                            .frame(minWidth: 44, minHeight: 44)
                            .accessibilityLabel("Abrir configuración")
                            .accessibilityIdentifier("home.settings")
                    }
                    .foregroundStyle(CupaTheme.forest)

                    SectionHeader(
                        eyebrow: "Cupa",
                        title: "Taller del Brewther",
                        subtitle: "Calibra, prepara y aprende de cada taza."
                    )

                    CupaCard {
                        VStack(alignment: .leading, spacing: 14) {
                            Label("Calculadora barista", systemImage: "dial.medium")
                                .font(.headline)
                            Text("Empieza con una proporción 1:16 y ajusta según tu café.")
                                .font(.subheadline)
                                .foregroundStyle(CupaTheme.secondaryText)
                            Button("Calcular preparación") { selection = .brew }
                                .buttonStyle(.borderedProminent)
                                .tint(CupaTheme.forest)
                        }
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        Text("Accesos rápidos")
                            .font(.title3.bold())
                        LazyVGrid(columns: [.init(.flexible()), .init(.flexible())], spacing: 12) {
                            ForEach(shortcuts, id: \.0) { item in
                                Button { selection = item.2 } label: {
                                    VStack(alignment: .leading, spacing: 14) {
                                        Image(systemName: item.1)
                                            .font(.title2)
                                            .foregroundStyle(item.3)
                                        Text(item.0)
                                            .font(.subheadline.weight(.semibold))
                                            .foregroundStyle(CupaTheme.text)
                                    }
                                    .frame(maxWidth: .infinity, minHeight: 90, alignment: .leading)
                                    .padding(14)
                                    .background(CupaTheme.card)
                                    .clipShape(RoundedRectangle(cornerRadius: 18))
                                }
                            }
                        }
                    }
                }
                .padding()
            }
        }
        .navigationBarHidden(true)
        .sheet(isPresented: $showAccount) { AccountView(model: account) }
        .sheet(isPresented: $showSettings) { SettingsView(model: settings, account: account) }
        .sheet(isPresented: $showHub) { HubView(account: account) }
    }
}

struct BrewView: View {
    @Binding var selection: CupaTab
    @ObservedObject var calculator: CalculatorModel
    @ObservedObject var lab: LabModel
    @ObservedObject var preparation: PreparationModel

    var body: some View {
        ZStack {
            CupaTheme.background.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 20) {
                    SectionHeader(eyebrow: "Barc", title: "Calculadora barista", subtitle: "Café, proporción y agua se recalculan en ambas direcciones.")

                    CupaCard {
                        VStack(spacing: 16) {
                            HStack {
                                Text(calculator.method).font(.headline)
                                Spacer()
                                Text("1:\(calculator.ratioInput)")
                                    .font(.subheadline.bold())
                                    .foregroundStyle(categoryColor)
                            }

                            VStack(spacing: 2) {
                                Text("AGUA").font(.caption2.bold()).tracking(1.5)
                                Text("\(calculator.water) ml")
                                    .font(.system(size: 44, weight: .black, design: .rounded))
                                Text("\(calculator.coffeeInput) g · 1:\(calculator.ratioInput) · \(calculator.method)")
                                    .font(.caption)
                            }
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 22)
                            .background(LinearGradient(colors: [CupaTheme.forest, categoryColor], startPoint: .topLeading, endPoint: .bottomTrailing))
                            .clipShape(RoundedRectangle(cornerRadius: 22))

                            Text(calculator.category.label)
                                .font(.caption.bold())
                                .foregroundStyle(categoryColor)
                                .frame(maxWidth: .infinity, alignment: .trailing)

                            HStack(spacing: 8) {
                                calculatorInput("CAFÉ (g)", identifier: "calculator.coffee", text: Binding(
                                    get: { calculator.coffeeInput },
                                    set: { calculator.changeCoffee($0) }
                                ), minus: { calculator.adjustCoffee(-1) }, plus: { calculator.adjustCoffee(1) })

                                calculatorInput("RATIO (1:x)", identifier: "calculator.ratio", text: Binding(
                                    get: { calculator.ratioInput },
                                    set: { calculator.changeRatio($0) }
                                ), minus: { calculator.adjustRatio(-0.1) }, plus: { calculator.adjustRatio(0.1) })

                                calculatorInput("AGUA (ml)", identifier: "calculator.water", text: Binding(
                                    get: { calculator.waterInput },
                                    set: { calculator.changeWater($0) }
                                ), minus: { calculator.adjustWater(-10) }, plus: { calculator.adjustWater(10) })
                            }

                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    ForEach(calculator.presets) { preset in
                                        Button { calculator.apply(preset) } label: {
                                            Label(preset.label, systemImage: preset.isCustom ? "star.fill" : "mug")
                                                .font(.caption)
                                                .padding(.horizontal, 10)
                                                .padding(.vertical, 7)
                                                .background(CupaTheme.backgroundAlt)
                                                .clipShape(Capsule())
                                        }
                                        .foregroundStyle(CupaTheme.text)
                                    }
                                }
                            }

                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 7) {
                                    ForEach(calculator.methods, id: \.self) { method in
                                        Button(method) { calculator.selectMethod(method) }
                                            .buttonStyle(.bordered)
                                            .tint(calculator.method == method ? categoryColor : CupaTheme.secondaryText)
                                    }
                                }
                            }

                            HStack {
                                Image(systemName: "info.circle")
                                Text(calculator.microcopy).font(.caption)
                                Spacer()
                                Button { calculator.resetRatio() } label: { Image(systemName: "arrow.counterclockwise") }
                                    .frame(minWidth: 44, minHeight: 44)
                                    .accessibilityLabel("Restablecer proporción")
                                Button { calculator.toggleFavorite() } label: {
                                    Image(systemName: calculator.isCurrentFavorite ? "heart.fill" : "heart")
                                        .foregroundStyle(calculator.isCurrentFavorite ? CupaTheme.terracotta : CupaTheme.secondaryText)
                                }
                                .frame(minWidth: 44, minHeight: 44)
                                .accessibilityLabel(calculator.isCurrentFavorite ? "Quitar de favoritos" : "Guardar como favorito")
                            }
                            .foregroundStyle(CupaTheme.secondaryText)

                            HStack {
                                Button {
                                    lab.load(calculator: calculator)
                                    selection = .lab
                                } label: { Label("Laboratorio", systemImage: "flask") }
                                    .buttonStyle(.bordered)
                                Button("Preparar con estos datos") { preparation.load(calculator: calculator) }
                                    .buttonStyle(.borderedProminent)
                                    .tint(CupaTheme.forest)
                                    .accessibilityIdentifier("calculator.prepare")
                            }
                        }
                    }

                    PreparationExecutionView(model: preparation)
                }
                .padding()
            }
        }
        .navigationTitle("Preparar")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var categoryColor: Color {
        switch calculator.category {
        case .espresso: Color(hex: 0xB85D42)
        case .intense: CupaTheme.gold
        case .balance: CupaTheme.forest
        case .clarity: Color(hex: 0x2E5A44)
        }
    }

    private func calculatorInput(_ title: String, identifier: String, text: Binding<String>, minus: @escaping () -> Void, plus: @escaping () -> Void) -> some View {
        VStack(spacing: 7) {
            Text(title).font(.system(size: 9, weight: .bold)).foregroundStyle(CupaTheme.secondaryText)
            TextField("", text: text)
                .keyboardType(.decimalPad)
                .multilineTextAlignment(.center)
                .font(.headline.monospacedDigit())
                .onSubmit { calculator.validateInputs() }
                .accessibilityLabel(title)
                .accessibilityIdentifier(identifier)
            HStack {
                Button(action: minus) { Image(systemName: "minus.circle.fill") }
                    .frame(minWidth: 44, minHeight: 44)
                    .accessibilityLabel("Disminuir \(title)")
                Spacer()
                Button(action: plus) { Image(systemName: "plus.circle.fill") }
                    .frame(minWidth: 44, minHeight: 44)
                    .accessibilityLabel("Aumentar \(title)")
            }
            .foregroundStyle(categoryColor)
        }
        .padding(9)
        .background(CupaTheme.backgroundAlt.opacity(0.7))
        .clipShape(RoundedRectangle(cornerRadius: 15))
    }

}

private enum LabControlCategory: String, CaseIterable, Identifiable {
    case ratio = "Ratio"
    case extraction = "Calor"
    case bean = "Grano"
    var id: Self { self }
}

struct LabView: View {
    @Environment(\.managedObjectContext) private var modelContext
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \LabExperimentRecord.createdAt, ascending: false)],
        predicate: NSPredicate(format: "deletedAt == nil"),
        animation: .default
    ) private var experiments: FetchedResults<LabExperimentRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \RecipeRecord.name, ascending: true)], predicate: NSPredicate(format: "deletedAt == nil")) private var recipes: FetchedResults<RecipeRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \TechniqueRecord.name, ascending: true)], predicate: NSPredicate(format: "deletedAt == nil")) private var techniques: FetchedResults<TechniqueRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \CoffeeBeanRecord.name, ascending: true)], predicate: NSPredicate(format: "deletedAt == nil")) private var beans: FetchedResults<CoffeeBeanRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \GrinderRecord.name, ascending: true)], predicate: NSPredicate(format: "deletedAt == nil")) private var grinders: FetchedResults<GrinderRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \EquipmentRecord.name, ascending: true)], predicate: NSPredicate(format: "deletedAt == nil AND equipmentType == 'BREWER_METHOD'")) private var methods: FetchedResults<EquipmentRecord>
    @ObservedObject var model: LabModel
    @ObservedObject var preparation: PreparationModel
    @ObservedObject var account: AccountModel
    @Binding var selection: CupaTab
    @State private var category = LabControlCategory.extraction
    @State private var altitudeExpanded = false
    @State private var showCustomCity = false
    @State private var customCity = ""
    @State private var customAltitude = ""
    @State private var saveConfirmation = false
    @State private var suggestion: BrewSuggestion?
    @State private var suggestionLoading = false
    @State private var showGeminiConsent = false
    @State private var errorMessage: String?
    @AppStorage("privacy.geminiConsent.v1") private var geminiConsent = false

    var body: some View {
        ZStack(alignment: .bottom) {
            CupaTheme.background.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 16) {
                    baseDataCard
                    altitudeCard
                    SectionHeader(
                        eyebrow: model.state.method.uppercased(),
                        title: "Laboratorio",
                        subtitle: "Simulación y calibración sensorial determinista."
                    )
                    hypothesisCard
                    sensoryCard
                    suggestionCard
                    Picker("Variables", selection: $category) {
                        ForEach(LabControlCategory.allCases) { Text($0.rawValue).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    controlsCard
                    if !experiments.isEmpty { savedExperimentsCard }
                }
                .padding()
                .padding(.bottom, 72)
            }
            actionBar
        }
        .navigationTitle("Laboratorio")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { model.reset() } label: { Image(systemName: "arrow.counterclockwise") }
                    .accessibilityLabel("Restablecer Laboratorio")
            }
        }
        .sheet(isPresented: $showCustomCity) { customCitySheet }
        .alert("Experimento guardado", isPresented: $saveConfirmation) {
            Button("Aceptar", role: .cancel) {}
        } message: {
            Text("La hipótesis quedó disponible offline en este dispositivo.")
        }
        .alert("Compartir datos con Gemini", isPresented: $showGeminiConsent) {
            Button("Usar sólo sugerencia local", role: .cancel) { requestSuggestion(allowRemote: false) }
            Button("Permitir y continuar") { geminiConsent = true; requestSuggestion(allowRemote: true) }
        } message: {
            Text("Cupa enviará a Google Gemini los parámetros de esta preparación y su perfil sensorial para generar una sugerencia. No se envían tu correo, nombre ni identificador. Puedes retirar este permiso en Configuración.")
        }
        .alert("No se pudo actualizar el experimento", isPresented: Binding(get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })) {
            Button("Aceptar") {}
        } message: { Text(errorMessage ?? "") }
    }

    private var baseDataCard: some View {
        CupaCard {
            VStack(alignment: .leading, spacing: 12) {
                Text("Base e inventario").font(.headline)
                Picker("Receta base", selection: Binding(get: { model.state.recipeId }, set: loadRecipe)) {
                    Text("Sin receta").tag(Optional<UUID>.none)
                    ForEach(recipes) { Text($0.name).tag(Optional($0.id)) }
                }
                Picker("Técnica base", selection: Binding(get: { model.state.techniqueId }, set: loadTechnique)) {
                    Text("Modo libre").tag(Optional<UUID>.none)
                    ForEach(techniques) { Text($0.name).tag(Optional($0.id)) }
                }
                Picker("Método / equipo", selection: Binding(get: { model.state.methodId }, set: loadMethod)) {
                    Text("Método genérico (\(model.state.method))").tag(Optional<UUID>.none)
                    ForEach(methods) { Text($0.name).tag(Optional($0.id)) }
                }
                HStack {
                    Picker("Grano", selection: binding(\.beanId)) { Text("Sin asignar").tag(Optional<UUID>.none); ForEach(beans) { Text($0.name).tag(Optional($0.id)) } }
                    Picker("Molino", selection: binding(\.grinderId)) { Text("Sin asignar").tag(Optional<UUID>.none); ForEach(grinders) { Text($0.name).tag(Optional($0.id)) } }
                }
            }
        }
    }

    private func loadRecipe(_ id: UUID?) {
        guard let id, let recipe = recipes.first(where: { $0.id == id }) else { model.update { $0.recipeId = nil }; return }
        let ingredients = (try? RecipeTechniqueRepository(context: modelContext).ingredients(recipeId: id)) ?? []
        model.load(recipe: recipe, ingredients: ingredients)
    }
    private func loadTechnique(_ id: UUID?) {
        guard let id, let technique = techniques.first(where: { $0.id == id }) else { model.update { $0.techniqueId = nil }; return }
        let recipeName = recipes.first(where: { $0.id == technique.recipeId })?.name
        model.load(technique: technique, recipeName: recipeName)
    }
    private func loadMethod(_ id: UUID?) { model.selectMethod(methods.first(where: { $0.id == id })) }

    private var altitudeCard: some View {
        CupaCard {
            VStack(alignment: .leading, spacing: 12) {
                Button { withAnimation { altitudeExpanded.toggle() } } label: {
                    HStack {
                        Image(systemName: "mountain.2.fill").foregroundStyle(CupaTheme.forest)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(model.state.cityName).font(.subheadline.bold()).foregroundStyle(CupaTheme.text)
                            Text("Hervor: \(boilingText) · \(model.state.altitudeMeters) msnm")
                                .font(.caption).foregroundStyle(isTemperatureCapped ? .orange : CupaTheme.secondaryText)
                        }
                        Spacer()
                        Image(systemName: altitudeExpanded ? "chevron.up" : "chevron.down")
                    }
                }
                .buttonStyle(.plain)

                if isTemperatureCapped {
                    Label("La temperatura real queda limitada al punto de ebullición local.", systemImage: "exclamationmark.triangle.fill")
                        .font(.caption).foregroundStyle(.orange)
                }

                if altitudeExpanded {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack {
                            ForEach(LabModel.cities) { city in
                                Button(city.label) { model.selectCity(city) }
                                    .buttonStyle(.bordered)
                                    .tint(model.state.altitudeMeters == city.altitudeMeters ? CupaTheme.forest : CupaTheme.secondaryText)
                            }
                        }
                    }
                    VStack(spacing: 4) {
                        HStack { Text("Ajuste manual").font(.caption.bold()); Spacer(); Text("\(model.state.altitudeMeters) m").font(.caption) }
                        Slider(value: Binding(
                            get: { Double(model.state.altitudeMeters) },
                            set: { model.setManualAltitude(Int($0.rounded() / 25) * 25) }
                        ), in: 0...4000, step: 25).tint(CupaTheme.gold)
                    }
                    Button { customCity = ""; customAltitude = String(model.state.altitudeMeters); showCustomCity = true } label: {
                        Label("Agregar mi ciudad y altura", systemImage: "location.badge.plus")
                    }
                    .buttonStyle(.bordered)
                }
            }
        }
    }

    private var hypothesisCard: some View {
        let profile = model.profile
        return VStack(alignment: .leading, spacing: 10) {
            HStack {
                VStack(alignment: .leading) {
                    Text("PERFIL ESTIMADO").font(.caption2.bold()).tracking(1)
                    Text(primaryOutcome).font(.title3.bold())
                }
                Spacer()
                Text(String(format: "%.2fx", profile.extractionIndex)).font(.title2.bold())
            }
            Text(profile.summary).font(.subheadline)
            HStack { ForEach(profile.labels, id: \.self) { Text($0).font(.caption2.bold()).padding(.horizontal, 8).padding(.vertical, 4).background(.white.opacity(0.18)).clipShape(Capsule()) } }
        }
        .foregroundStyle(.white)
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(LinearGradient(colors: [CupaTheme.forest, CupaTheme.terracotta], startPoint: .topLeading, endPoint: .bottomTrailing))
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .accessibilityElement(children: .combine)
    }

    private var sensoryCard: some View {
        let values = [
            ("Aroma", model.profile.aroma, CupaTheme.gold), ("Acidez", model.profile.acidity, .yellow),
            ("Dulzor", model.profile.sweetness, .pink), ("Cuerpo", model.profile.body, CupaTheme.terracotta),
            ("Amargor", model.profile.bitterness, .brown), ("Final", model.profile.finish, .cyan)
        ]
        return CupaCard {
            VStack(alignment: .leading, spacing: 14) {
                Text("Ecualizador sensorial").font(.headline)
                HStack(alignment: .bottom, spacing: 8) {
                    ForEach(values, id: \.0) { label, value, color in
                        VStack(spacing: 5) {
                            Text("\(value)%").font(.caption2.bold()).foregroundStyle(color)
                            GeometryReader { proxy in
                                ZStack(alignment: .bottom) {
                                    Capsule().fill(CupaTheme.backgroundAlt)
                                    Capsule().fill(color.gradient).frame(height: proxy.size.height * CGFloat(value) / 100)
                                }
                            }.frame(height: 112)
                            Text(label).font(.system(size: 9, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.7)
                        }.frame(maxWidth: .infinity)
                    }
                }
                Divider()
                Text(model.diagnostic.extraction).font(.subheadline.bold()).foregroundStyle(CupaTheme.forest)
                Text(model.diagnostic.recommendation).font(.caption).foregroundStyle(CupaTheme.secondaryText)
                ForEach(model.diagnostic.risks, id: \.self) { Label($0, systemImage: "exclamationmark.circle").font(.caption).foregroundStyle(.orange) }
            }
        }
    }

    @ViewBuilder private var controlsCard: some View {
        CupaCard {
            VStack(spacing: 16) {
                Picker("Método", selection: binding(\.method)) {
                    ForEach(["V60", "AeroPress", "Prensa francesa", "Chemex", "Espresso", "Moka", "Cold brew"], id: \.self) { Text($0) }
                }
                switch category {
                case .ratio:
                    HStack {
                        Stepper("Café \(model.state.coffeeGrams.formatted()) g", value: bindingFloat(\.coffeeGrams), in: 1...100, step: 1)
                        Divider()
                        Stepper("Agua \(model.state.waterMl) ml", value: binding(\.waterMl), in: 10...2000, step: 10)
                    }
                    labSlider("Ratio", value: bindingFloat(\.ratio), range: 8...22, step: 0.5, display: "1:\(String(format: "%.1f", model.state.ratio))")
                    labSlider("Tiempo", value: bindingInt(\.timeSeconds), range: 60...360, step: 5, display: formattedTime)
                case .extraction:
                    Picker("Unidad", selection: binding(\.temperatureUnit)) {
                        Text("°C").tag(TemperatureUnit.celsius); Text("°F").tag(TemperatureUnit.fahrenheit)
                    }.pickerStyle(.segmented)
                    labSlider("Temperatura", value: bindingInt(\.temperatureC), range: 80...98, step: 1, display: temperatureText)
                    labSlider("Clicks de molienda", value: bindingInt(\.grindClicks), range: 6...36, step: 1, display: "\(model.state.grindClicks) clicks")
                case .bean:
                    Picker("Frescura", selection: binding(\.freshness)) {
                        ForEach(["muy fresco", "en ventana", "punto ideal", "bajando", "viejo"], id: \.self) { Text($0.capitalized) }
                    }
                    TextField("Notas del experimento", text: binding(\.notes), axis: .vertical).lineLimit(3...7)
                }
            }
        }
    }

    private var savedExperimentsCard: some View {
        CupaCard {
            VStack(alignment: .leading, spacing: 8) {
                Text("Experimentos recientes").font(.headline)
                ForEach(Array(experiments.prefix(3))) { experiment in
                    HStack {
                        Button { model.load(experiment: experiment) } label: {
                            VStack(alignment: .leading) {
                                Text(experiment.method).font(.subheadline.bold())
                                Text("1:\(experiment.ratio.formatted(.number.precision(.fractionLength(1)))) · \(experiment.temperatureC)°C · \(experiment.cityName)")
                                    .font(.caption).foregroundStyle(CupaTheme.secondaryText)
                            }
                        }.buttonStyle(.plain).accessibilityLabel("Cargar experimento de \(experiment.method)")
                        Spacer()
                        Text(experiment.createdAt, style: .date).font(.caption2)
                        Button(role: .destructive) { deleteExperiment(experiment) } label: { Image(systemName: "trash") }
                            .frame(minWidth: 44, minHeight: 44).accessibilityLabel("Eliminar experimento")
                    }
                }
            }
        }
    }

    private var suggestionCard: some View {
        CupaCard {
            VStack(alignment: .leading, spacing: 10) {
                HStack { Label("Sugerencia de ajuste", systemImage: "sparkles").font(.headline); Spacer(); if suggestionLoading { ProgressView() } }
                if let suggestion {
                    Text(suggestion.text).font(.subheadline)
                    Text(suggestion.source == .gemini ? "Sugerencia generada por IA · confirma antes de cambiar tu receta" : "Sugerencia local · disponible sin conexión")
                        .font(.caption2).foregroundStyle(CupaTheme.secondaryText)
                } else {
                    Text("Obtén una interpretación sin alterar los cálculos ni tus datos guardados.").font(.caption).foregroundStyle(CupaTheme.secondaryText)
                }
                Button("Analizar este perfil") {
                    if account.configuration.isSupabaseConfigured, account.tokens != nil, !geminiConsent {
                        showGeminiConsent = true
                    } else {
                        requestSuggestion(allowRemote: geminiConsent)
                    }
                }.buttonStyle(.bordered).disabled(suggestionLoading)
            }
        }
    }

    private func requestSuggestion(allowRemote: Bool) {
        suggestionLoading = true
        let input = SuggestionContext(state: model.state, profile: model.profile)
        let token = allowRemote ? account.tokens?.accessToken : nil
        Task {
            suggestion = await GeminiSuggestionService(configuration: account.configuration).suggest(input, accessToken: token)
            suggestionLoading = false
        }
    }

    private var actionBar: some View {
        HStack {
            Button { saveExperiment() } label: { Label("Guardar", systemImage: "square.and.arrow.down") }
                .buttonStyle(.bordered)
            Button {
                saveExperiment()
                preparation.load(lab: model.state)
                selection = .brew
            } label: { Label("Preparar esta idea", systemImage: "play.fill") }
                .buttonStyle(.borderedProminent).tint(CupaTheme.forest)
        }
        .padding().frame(maxWidth: .infinity).background(.ultraThinMaterial)
    }

    private var customCitySheet: some View {
        NavigationStack {
            Form {
                TextField("Ciudad", text: $customCity)
                TextField("Altitud (msnm)", text: $customAltitude).keyboardType(.numberPad)
            }
            .navigationTitle("Tu ciudad y altura")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { showCustomCity = false } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Guardar") { model.setManualAltitude(Int(customAltitude) ?? 0, city: customCity); showCustomCity = false }
                }
            }
        }
    }

    private func saveExperiment() {
        _ = LabExperimentRecord(context: modelContext, state: model.state, profile: model.profile)
        do { try modelContext.save(); saveConfirmation = true }
        catch { modelContext.rollback(); errorMessage = error.localizedDescription }
    }

    private func deleteExperiment(_ experiment: LabExperimentRecord) {
        experiment.markDeleted()
        do { try modelContext.save() }
        catch { modelContext.rollback(); errorMessage = error.localizedDescription }
    }

    private var isTemperatureCapped: Bool { Float(model.state.temperatureC) > model.boilingPointC }
    private var boilingText: String {
        model.state.temperatureUnit == .celsius
            ? String(format: "%.1f °C", model.boilingPointC)
            : "\(Int(roundf(LabEngine.fahrenheit(fromCelsius: model.boilingPointC)))) °F"
    }
    private var temperatureText: String {
        model.state.temperatureUnit == .celsius
            ? "\(model.state.temperatureC) °C"
            : "\(Int(roundf(LabEngine.fahrenheit(fromCelsius: Float(model.state.temperatureC))))) °F"
    }
    private var formattedTime: String { String(format: "%d:%02d min", model.state.timeSeconds / 60, model.state.timeSeconds % 60) }
    private var primaryOutcome: String {
        let p = model.profile
        if p.bitterness > 65 { return "Intensa y con cuerpo" }
        if p.body < 38 { return "Estilo té, alta claridad" }
        if p.sweetness > 68 && p.bitterness < 42 { return "Taza dorada y balanceada" }
        if p.acidity > 68 { return "Acidez brillante y frutal" }
        return "Taza equilibrada clásica"
    }

    private func labSlider(_ title: String, value: Binding<Double>, range: ClosedRange<Double>, step: Double, display: String) -> some View {
        VStack(spacing: 6) {
            HStack { Text(title).font(.subheadline.bold()); Spacer(); Text(display).font(.subheadline.bold()).foregroundStyle(CupaTheme.terracotta) }
            Slider(value: value, in: range, step: step).tint(CupaTheme.forest)
        }
    }
    private func binding<Value>(_ keyPath: WritableKeyPath<LabState, Value>) -> Binding<Value> {
        Binding(get: { model.state[keyPath: keyPath] }, set: { value in model.update { $0[keyPath: keyPath] = value } })
    }
    private func bindingFloat(_ keyPath: WritableKeyPath<LabState, Float>) -> Binding<Double> {
        Binding(get: { Double(model.state[keyPath: keyPath]) }, set: { value in model.update { $0[keyPath: keyPath] = Float(value) } })
    }
    private func bindingInt(_ keyPath: WritableKeyPath<LabState, Int>) -> Binding<Double> {
        Binding(get: { Double(model.state[keyPath: keyPath]) }, set: { value in model.update { $0[keyPath: keyPath] = Int(value.rounded()) } })
    }
}

private enum StorageCategory: String, CaseIterable, Identifiable {
    case coffee = "Cafés"
    case grinders = "Molinos"
    case equipment = "Equipos"
    case recipes = "Recetas"
    case techniques = "Técnicas"
    case cups = "Tazas"
    var id: Self { self }
}

struct StorageView: View {
    @State private var category = StorageCategory.coffee

    var body: some View {
        VStack(spacing: 0) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(StorageCategory.allCases) { item in
                        Button(item.rawValue) { category = item }
                            .buttonStyle(.bordered)
                            .tint(category == item ? CupaTheme.forest : CupaTheme.secondaryText)
                            .accessibilityAddTraits(category == item ? .isSelected : [])
                    }
                }
                .padding(.horizontal)
            }
            .padding(.vertical, 10)
            switch category {
            case .coffee: CoffeeInventoryView()
            case .grinders: GrinderInventoryView()
            case .equipment: EquipmentInventoryView()
            case .recipes: RecipeInventoryView()
            case .techniques: TechniqueInventoryView()
            case .cups: CupHistoryView()
            }
        }
        .background(CupaTheme.background)
        .navigationTitle("Almacén")
    }
}

private struct CupHistoryView: View {
    @Environment(\.managedObjectContext) private var context
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \CupSessionRecord.brewDate, ascending: false)],
        predicate: NSPredicate(format: "deletedAt == nil"),
        animation: .default
    ) private var cups: FetchedResults<CupSessionRecord>
    @State private var errorMessage: String?

    var body: some View {
        List {
            if cups.isEmpty {
                ContentUnavailableView(
                    "Sin tazas guardadas",
                    systemImage: "cup.and.saucer",
                    description: Text("Al guardar una cata aparecerá aquí con los valores ejecutados de la preparación.")
                )
                .listRowBackground(Color.clear)
            } else {
                ForEach(cups) { cup in
                    VStack(alignment: .leading, spacing: 7) {
                        HStack {
                            Text(cup.beanNameSnapshot.isEmpty ? "Café sin registrar" : cup.beanNameSnapshot).font(.headline)
                            Spacer()
                            Text("\(cup.rating.formatted(.number.precision(.fractionLength(0...1)))) ★").foregroundStyle(CupaTheme.gold)
                        }
                        Text("\(cup.techniqueNameSnapshot) · \(cup.executedDoseGrams.formatted(.number.precision(.fractionLength(0...1)))) g → \(cup.executedWaterMl) ml")
                            .font(.subheadline).foregroundStyle(CupaTheme.secondaryText)
                        HStack {
                            Label(cup.cupLifeState.localizedCupLife, systemImage: "thermometer.medium")
                            if !cup.executedGrindSetting.isEmpty { Label(cup.executedGrindSetting, systemImage: "dial.medium") }
                        }
                        .font(.caption).foregroundStyle(CupaTheme.forest)
                        if !cup.comment.isEmpty { Text(cup.comment).font(.caption) }
                        if let date = cup.brewDate { Text(date.formatted(date: .abbreviated, time: .shortened)).font(.caption2).foregroundStyle(.secondary) }
                    }
                    .padding(.vertical, 6)
                    .accessibilityElement(children: .combine)
                }
                .onDelete(perform: delete)
            }
        }
        .scrollContentBackground(.hidden)
        .alert("No se pudo eliminar la taza", isPresented: Binding(get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })) {
            Button("Aceptar") {}
        } message: { Text(errorMessage ?? "") }
    }

    private func delete(at offsets: IndexSet) {
        do { for index in offsets { try TastingRepository(context: context).delete(cups[index]) } }
        catch { context.rollback(); errorMessage = error.localizedDescription }
    }
}

private extension String {
    var localizedCupLife: String {
        switch self { case "FRESH": "Fresca"; case "PEAK": "En su punto"; case "DECLINING": "En descenso"; case "EXHAUSTED": "Agotada"; default: self.capitalized }
    }
}

private struct CoffeeInventoryView: View {
    @Environment(\.managedObjectContext) private var modelContext
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \CoffeeBeanRecord.updatedAt, ascending: false)],
        predicate: NSPredicate(format: "deletedAt == nil"),
        animation: .default
    ) private var beans: FetchedResults<CoffeeBeanRecord>
    @State private var showAddBean = false
    @State private var editingBean: CoffeeBeanRecord?
    @State private var errorMessage: String?

    var body: some View {
        ZStack {
            CupaTheme.background.ignoresSafeArea()
            List {
                Section {
                    if beans.isEmpty {
                        ContentUnavailableView(
                            "Sin cafés guardados",
                            systemImage: "leaf",
                            description: Text("Agrega tu primer café para usarlo en preparaciones, recetas y catas.")
                        )
                        .listRowBackground(Color.clear)
                    } else {
                        ForEach(beans) { bean in
                            let freshness = CoffeeFreshnessEngine.evaluate(roastDate: bean.roastDate, openedDate: bean.openedDate)
                            Button { editingBean = bean } label: {
                                VStack(alignment: .leading, spacing: 6) {
                                    HStack {
                                        Text(bean.name).font(.headline)
                                        Spacer()
                                        CoffeeFreshnessBadge(state: freshness.state)
                                        if bean.syncStatus != .synced {
                                            Image(systemName: "arrow.triangle.2.circlepath")
                                                .font(.caption)
                                                .foregroundStyle(CupaTheme.gold)
                                        }
                                    }
                                    Text("\(bean.brand.isEmpty ? "Sin tostador" : bean.brand) · Tueste \(bean.roastLevel.lowercased())")
                                        .font(.subheadline)
                                        .foregroundStyle(CupaTheme.secondaryText)
                                    if bean.remainingQuantityGrams > 0 {
                                        Text("\(bean.remainingQuantityGrams.formatted(.number.precision(.fractionLength(0...1)))) g disponibles")
                                            .font(.caption)
                                            .foregroundStyle(CupaTheme.forest)
                                    }
                                    CoffeeFreshnessBar(result: freshness)
                                    if let warning = freshness.openWarning {
                                        Label(warning, systemImage: "exclamationmark.triangle.fill")
                                            .font(.caption2).foregroundStyle(CupaTheme.terracotta)
                                    }
                                }
                                .padding(.vertical, 6)
                            }
                            .buttonStyle(.plain)
                        }
                        .onDelete(perform: softDelete)
                    }
                } header: {
                    Text("Cafés")
                }
            }
            .scrollContentBackground(.hidden)
        }
        .toolbar {
            Button { showAddBean = true } label: { Image(systemName: "plus") }
                .accessibilityLabel("Agregar café")
                .accessibilityIdentifier("inventory.addCoffee")
        }
        .sheet(isPresented: $showAddBean) {
            CoffeeBeanEditor(record: nil) { draft in
                _ = draft.makeRecord(in: modelContext)
                return save()
            }
        }
        .sheet(item: $editingBean) { bean in
            CoffeeBeanEditor(record: bean) { draft in
                draft.apply(to: bean)
                bean.markUpdated()
                return save()
            }
        }
        .alert("No se pudo guardar el café", isPresented: Binding(get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })) {
            Button("Aceptar") {}
        } message: { Text(errorMessage ?? "") }
    }

    private func softDelete(at offsets: IndexSet) {
        for index in offsets { beans[index].markDeleted() }
        _ = save()
    }

    @discardableResult private func save() -> Bool {
        do { try modelContext.save(); return true }
        catch { modelContext.rollback(); errorMessage = error.localizedDescription; return false }
    }
}

private struct CoffeeFreshnessBadge: View {
    let state: CoffeeFreshnessState
    var body: some View {
        Text(state.label.uppercased())
            .font(.caption2.bold())
            .foregroundStyle(Color(hex: state.colorHex))
            .padding(.horizontal, 8).padding(.vertical, 4)
            .background(Color(hex: state.colorHex).opacity(0.12), in: Capsule())
    }
}

private struct CoffeeFreshnessBar: View {
    let result: CoffeeFreshnessResult
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(result.daysFromRoast.map { "Día \($0) desde tostado" } ?? "Sin datos de tueste")
                Spacer()
                Text("\(Int((result.progress * 100).rounded(.towardZero)))% est. útil")
            }
            .font(.caption2.bold()).foregroundStyle(CupaTheme.secondaryText)
            ProgressView(value: result.progress)
                .tint(Color(hex: result.state.colorHex))
                .accessibilityLabel("Maduración estimada")
                .accessibilityValue("\(Int((result.progress * 100).rounded())) por ciento")
            Text(result.openStatusDetails).font(.caption2).foregroundStyle(CupaTheme.secondaryText)
        }
    }
}

private struct CoffeeBeanDraft {
    var name: String
    var brand: String
    var origin: String
    var producer: String
    var variety: String
    var process: String
    var altitudeMeters: Int?
    var roastLevel: String
    var roastDate: Date?
    var openedDate: Date?
    var initialQuantityGrams: Double
    var remainingQuantityGrams: Double
    var notes: String

    func makeRecord(in context: NSManagedObjectContext) -> CoffeeBeanRecord {
        CoffeeBeanRecord(
            context: context,
            name: name,
            brand: brand,
            origin: origin,
            producer: producer,
            variety: variety,
            process: process,
            altitudeMeters: altitudeMeters,
            roastLevel: roastLevel,
            roastDate: roastDate,
            openedDate: openedDate,
            initialQuantityGrams: initialQuantityGrams,
            remainingQuantityGrams: remainingQuantityGrams,
            notes: notes
        )
    }

    func apply(to record: CoffeeBeanRecord) {
        record.name = name
        record.brand = brand
        record.origin = origin
        record.producer = producer
        record.variety = variety
        record.process = process
        record.altitudeMeters = altitudeMeters
        record.roastLevel = roastLevel
        record.roastDate = roastDate
        record.openedDate = openedDate
        record.initialQuantityGrams = initialQuantityGrams
        record.remainingQuantityGrams = remainingQuantityGrams
        record.notes = notes
    }
}

private struct CoffeeBeanEditor: View {
    @Environment(\.dismiss) private var dismiss
    private let record: CoffeeBeanRecord?
    private let onSave: (CoffeeBeanDraft) -> Bool
    @State private var name: String
    @State private var brand: String
    @State private var origin: String
    @State private var producer: String
    @State private var variety: String
    @State private var process: String
    @State private var altitude: String
    @State private var roastLevel: String
    @State private var hasRoastDate: Bool
    @State private var roastDate: Date
    @State private var hasOpenedDate: Bool
    @State private var openedDate: Date
    @State private var initialQuantity: String
    @State private var remainingQuantity: String
    @State private var notes: String

    init(record: CoffeeBeanRecord?, onSave: @escaping (CoffeeBeanDraft) -> Bool) {
        self.record = record
        self.onSave = onSave
        _name = State(initialValue: record?.name ?? "")
        _brand = State(initialValue: record?.brand ?? "")
        _origin = State(initialValue: record?.origin ?? "")
        _producer = State(initialValue: record?.producer ?? "")
        _variety = State(initialValue: record?.variety ?? "")
        _process = State(initialValue: record?.process ?? "")
        _altitude = State(initialValue: record?.altitudeMeters.map { String($0) } ?? "")
        _roastLevel = State(initialValue: record?.roastLevel ?? "Medio")
        _hasRoastDate = State(initialValue: record?.roastDate != nil)
        _roastDate = State(initialValue: record?.roastDate ?? .now)
        _hasOpenedDate = State(initialValue: record?.openedDate != nil)
        _openedDate = State(initialValue: record?.openedDate ?? .now)
        _initialQuantity = State(initialValue: record.map { String(format: "%.1f", $0.initialQuantityGrams) } ?? "")
        _remainingQuantity = State(initialValue: record.map { String(format: "%.1f", $0.remainingQuantityGrams) } ?? "")
        _notes = State(initialValue: record?.notes ?? "")
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Identidad") {
                    TextField("Nombre del café", text: $name)
                    TextField("Marca o tostador", text: $brand)
                    TextField("Origen", text: $origin)
                    TextField("Productor", text: $producer)
                    TextField("Variedad", text: $variety)
                    TextField("Proceso", text: $process)
                    TextField("Altitud (msnm)", text: $altitude).keyboardType(.numberPad)
                }
                Section("Tueste y apertura") {
                    Picker("Tueste", selection: $roastLevel) {
                    ForEach(["Claro", "Medio", "Oscuro"], id: \.self) { Text($0) }
                    }
                    Toggle("Con fecha de tueste", isOn: $hasRoastDate)
                    if hasRoastDate { DatePicker("Fecha de tueste", selection: $roastDate, displayedComponents: .date) }
                    Toggle("Bolsa abierta", isOn: $hasOpenedDate)
                    if hasOpenedDate { DatePicker("Fecha de apertura", selection: $openedDate, displayedComponents: .date) }
                }
                Section("Frescura estimada") {
                    HStack { CoffeeFreshnessBadge(state: freshness.state); Spacer(); Text(freshness.openStatusDetails).font(.caption).foregroundStyle(.secondary) }
                    CoffeeFreshnessBar(result: freshness)
                    Text(freshness.recommendation).font(.caption).foregroundStyle(CupaTheme.secondaryText)
                    if let warning = freshness.openWarning {
                        Label(warning, systemImage: "exclamationmark.triangle.fill").font(.caption).foregroundStyle(CupaTheme.terracotta)
                    }
                }
                Section("Inventario") {
                    TextField("Cantidad inicial (g)", text: $initialQuantity).keyboardType(.decimalPad)
                    TextField("Cantidad restante (g)", text: $remainingQuantity).keyboardType(.decimalPad)
                    TextField("Notas", text: $notes, axis: .vertical).lineLimit(3...8)
                }
            }
            .navigationTitle(record == nil ? "Agregar café" : "Editar café")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Guardar") {
                        if onSave(CoffeeBeanDraft(
                            name: name.trimmingCharacters(in: .whitespacesAndNewlines),
                            brand: brand.trimmingCharacters(in: .whitespacesAndNewlines),
                            origin: origin,
                            producer: producer,
                            variety: variety,
                            process: process,
                            altitudeMeters: Int(altitude),
                            roastLevel: roastLevel,
                            roastDate: hasRoastDate ? roastDate : nil,
                            openedDate: hasOpenedDate ? openedDate : nil,
                            initialQuantityGrams: parseDecimal(initialQuantity),
                            remainingQuantityGrams: parseDecimal(remainingQuantity),
                            notes: notes
                        )) { dismiss() }
                    }
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
    }

    private func parseDecimal(_ text: String) -> Double {
        Double(text.replacingOccurrences(of: ",", with: ".")) ?? 0
    }

    private var freshness: CoffeeFreshnessResult {
        CoffeeFreshnessEngine.evaluate(
            roastDate: hasRoastDate ? roastDate : nil,
            openedDate: hasOpenedDate ? openedDate : nil
        )
    }
}
