import SwiftUI
import CoreData
import UIKit

struct HomeView: View {
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \CoffeeBeanRecord.updatedAt, ascending: false)], predicate: LocalDataScope.visiblePredicate()) private var beans: FetchedResults<CoffeeBeanRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \RecipeRecord.updatedAt, ascending: false)], predicate: LocalDataScope.visiblePredicate()) private var recipes: FetchedResults<RecipeRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \CupSessionRecord.updatedAt, ascending: false)], predicate: LocalDataScope.visiblePredicate()) private var cups: FetchedResults<CupSessionRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \TechniqueRecord.updatedAt, ascending: false)], predicate: LocalDataScope.visiblePredicate()) private var techniques: FetchedResults<TechniqueRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \EquipmentRecord.updatedAt, ascending: false)], predicate: LocalDataScope.visiblePredicate()) private var equipment: FetchedResults<EquipmentRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \GrinderRecord.updatedAt, ascending: false)], predicate: LocalDataScope.visiblePredicate()) private var grinders: FetchedResults<GrinderRecord>
    @Binding var selection: CupaTab
    @ObservedObject var account: AccountModel
    @ObservedObject var settings: SettingsModel
    @ObservedObject var calculator: CalculatorModel
    @ObservedObject var lab: LabModel
    @ObservedObject var preparation: PreparationModel
    @State private var showAccount = false
    @State private var showSettings = false
    @State private var showNotifications = false
    @State private var showHub = false

    private let shortcuts: [(String, String, CupaTab, Color)] = [
        ("Cata", "heart.text.square", .tasting, CupaTheme.terracotta),
        ("Laboratorio", "flask", .lab, CupaTheme.gold),
        ("Almacén", "shippingbox", .storage, CupaTheme.forest),
        ("Preparar", "mug", .brew, CupaTheme.terracotta)
    ]

    var body: some View {
        ZStack {
            BrewOrganicCanvas().ignoresSafeArea()
            ScrollView {
                VStack(spacing: 20) {
                    HStack {
                        Button { showHub = true } label: {
                            Label(account.tokens?.email ?? "Mi perfil", systemImage: "person.crop.circle").font(.subheadline.weight(.semibold))
                        }
                        .accessibilityIdentifier("home.profile")
                        Spacer()
                        Menu {
                            Button { showSettings = true } label: { Label("Configuración", systemImage: "gearshape") }
                            Button { showNotifications = true } label: { Label("Notificaciones", systemImage: "bell") }
                        } label: { Image(systemName: "line.3.horizontal").frame(width: 44, height: 44) }
                        .accessibilityLabel("Menú")
                    }
                    .foregroundStyle(CupaTheme.forestText)

                    SectionHeader(
                        eyebrow: "Cupa",
                        title: "Taller del Brewther",
                        subtitle: "Calibra, prepara y aprende de cada taza."
                    )

                    BaristaCalculatorCard(
                        calculator: calculator,
                        settings: settings,
                        onLab: {
                            lab.load(calculator: calculator)
                            selection = .lab
                        },
                        onPrepare: {
                            preparation.load(calculator: calculator)
                            selection = .brew
                        }
                    )

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

                    workshopStatusCard
                    if !beans.isEmpty || !recipes.isEmpty { recentResourcesCard }
                }
                .padding()
            }
        }
        .navigationBarHidden(true)
        .sheet(isPresented: $showAccount) { AccountView(model: account) }
        .sheet(isPresented: $showSettings) { SettingsView(model: settings, account: account) }
        .alert("Notificaciones", isPresented: $showNotifications) { Button("Cerrar", role: .cancel) {} } message: { Text("Sin notificaciones nuevas") }
        .sheet(isPresented: $showHub) { HubView(account: account) }
    }

    private var workshopStatusCard: some View {
        CupaCard {
            VStack(alignment: .leading, spacing: 12) {
                Text("Estado del taller").font(.headline)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        WorkshopMetric(title: "Granos", count: beans.count, icon: "leaf", color: CupaTheme.terracotta)
                        WorkshopMetric(title: "Recetas", count: recipes.count, icon: "book.closed", color: CupaTheme.forest)
                        WorkshopMetric(title: "Tazas", count: cups.count, icon: "mug", color: CupaTheme.gold)
                        WorkshopMetric(title: "Técnicas", count: techniques.count, icon: "list.number", color: CupaTheme.clarity)
                        WorkshopMetric(title: "Equipo", count: equipment.count + grinders.count, icon: "wrench.and.screwdriver", color: CupaTheme.secondaryText)
                    }
                }
            }
        }
        .accessibilityIdentifier("home.workshopStatus")
    }

    private var recentResourcesCard: some View {
        CupaCard {
            VStack(alignment: .leading, spacing: 12) {
                Text("Últimos recursos usados").font(.headline)
                if let bean = beans.first {
                    LabeledContent {
                        Text("\(bean.remainingQuantityGrams.formatted(.number.precision(.fractionLength(0...1)))) g")
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Último grano").font(.caption).foregroundStyle(CupaTheme.secondaryText)
                            Text(bean.name).font(.subheadline.bold())
                            if !bean.origin.isEmpty { Text(bean.origin).font(.caption).foregroundStyle(CupaTheme.secondaryText) }
                        }
                    }
                }
                if let recipe = recipes.first {
                    LabeledContent {
                        Text(recipe.recipeKind.replacingOccurrences(of: "_", with: " ").capitalized)
                            .font(.caption).foregroundStyle(CupaTheme.secondaryText)
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Última receta").font(.caption).foregroundStyle(CupaTheme.secondaryText)
                            Text(recipe.name).font(.subheadline.bold())
                            if !recipe.intention.isEmpty { Text(recipe.intention).font(.caption).foregroundStyle(CupaTheme.secondaryText) }
                        }
                    }
                }
            }
        }
        .accessibilityIdentifier("home.recentResources")
    }
}

private struct WorkshopMetric: View {
    let title: String
    let count: Int
    let icon: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Image(systemName: icon).foregroundStyle(color).font(.title3)
            Text(count.formatted()).font(.title2.bold().monospacedDigit())
            Text(title).font(.caption).foregroundStyle(CupaTheme.secondaryText)
        }
        .frame(width: 88, alignment: .leading)
        .padding(12)
        .background(CupaTheme.backgroundAlt.opacity(0.7))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(title): \(count)")
    }
}

struct BrewView: View {
    @ObservedObject var preparation: PreparationModel
    @ObservedObject var tasting: TastingModel
    @Binding var selection: CupaTab

    var body: some View {
        ZStack {
            BrewOrganicCanvas(warmTop: true).ignoresSafeArea()
            ScrollView {
                VStack(spacing: 20) {
                    SectionHeader(eyebrow: "Secuencia de extracción", title: "Preparar café", subtitle: "Elige una técnica para los datos calculados y sigue cada paso.")
                    PreparationExecutionView(model: preparation) { brewSessionId in
                        tasting.linkToPreparation(brewSessionId)
                        selection = .tasting
                    }
                }
                .padding()
            }
        }
        .navigationBarHidden(true)
    }
}

/// Swaps faces at the midpoint, keeping text upright during a true whole-card flip.
private struct CalculatorFlip: AnimatableModifier {
    var rotation: Double
    let back: AnyView
    var animatableData: Double { get { rotation } set { rotation = newValue } }
    func body(content: Content) -> some View {
        Group {
            if rotation > 90 { back.rotation3DEffect(.degrees(180), axis: (x: 0, y: 1, z: 0)) }
            else { content }
        }
        .rotation3DEffect(.degrees(rotation), axis: (x: 0, y: 1, z: 0), perspective: 0.35)
    }
}

private struct BeanMethodSettings: View {
    @Environment(\.managedObjectContext) private var context
    @ObservedObject var bean: CoffeeBeanRecord
    let method: String
    let unit: TemperatureUnit
    @State private var saveError: String?
    private var profile: BeanBrewProfile { bean.brewProfile(for: method) ?? .init(methodName: method) }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Clics de molino").font(.caption.bold())
                    Text("\(profile.clicks)").font(.title3.monospacedDigit().bold())
                    Stepper("Clics de molino", value: Binding(get: { profile.clicks }, set: { save(clicks: $0, temperature: profile.temperatureC) }), in: 1...200).labelsHidden()
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                VStack(alignment: .leading, spacing: 6) {
                    Text("Temperatura").font(.caption.bold())
                    Text(unit == .celsius ? "\(profile.temperatureC) °C" : "\(Int((Double(profile.temperatureC) * 1.8 + 32).rounded())) °F")
                        .font(.title3.monospacedDigit().bold())
                    Stepper("Temperatura", value: Binding(get: { profile.temperatureC }, set: { save(clicks: profile.clicks, temperature: $0) }), in: 1...100).labelsHidden()
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(12).background(CupaTheme.backgroundAlt).clipShape(RoundedRectangle(cornerRadius: 14))
            Text(saveError ?? (bean.brewProfile(for: method) == nil ? "Ajusta para guardar tu punto favorito." : "Guardado para este grano · \(method)"))
                .font(.caption).foregroundStyle(CupaTheme.secondaryText)
        }
    }

    private func save(clicks: Int, temperature: Int) {
        let previous = bean.brewProfilesJSON
        let version = bean.version; let status = bean.syncStatusRaw; let updated = bean.updatedAt
        do {
            try bean.setBrewProfile(.init(methodName: method, clicks: clicks, temperatureC: temperature))
            try context.save(); saveError = nil
        } catch {
            bean.brewProfilesJSON = previous; bean.version = version; bean.syncStatusRaw = status; bean.updatedAt = updated
            saveError = "No se guardó. Vuelve a ajustar para reintentar."
        }
    }
}

private struct InventoryBeanBrewSettings: View {
    @ObservedObject var bean: CoffeeBeanRecord
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \EquipmentRecord.name, ascending: true)], predicate: LocalDataScope.visiblePredicate()) private var equipment: FetchedResults<EquipmentRecord>
    @AppStorage("settings.temperature") private var rawUnit = TemperatureUnit.celsius.rawValue
    @State private var method = "V60"
    private var methods: [String] {
        Array(Set(["V60", "AeroPress", "Prensa francesa", "Chemex", "Espresso", "Moka", "Cold brew"] + equipment.filter { $0.isActive && $0.isBrewingMethod }.map(\.name))).sorted()
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Picker("Método", selection: $method) { ForEach(methods, id: \.self) { Text($0).tag($0) } }
            BeanMethodSettings(bean: bean, method: method, unit: TemperatureUnit(rawValue: rawUnit) ?? .celsius)
        }
    }
}

private struct BaristaCalculatorCard: View {
    @Environment(\.managedObjectContext) private var context
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \EquipmentRecord.createdAt, ascending: true)],
        predicate: LocalDataScope.visiblePredicate(additional: NSPredicate(format: "isActive == YES"))
    ) private var activeEquipment: FetchedResults<EquipmentRecord>
    @ObservedObject var calculator: CalculatorModel
    @ObservedObject var settings: SettingsModel
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \CoffeeBeanRecord.name, ascending: true)], predicate: LocalDataScope.visiblePredicate()) private var beans: FetchedResults<CoffeeBeanRecord>
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var flipped = false
    let onLab: () -> Void
    let onPrepare: () -> Void
    @State private var showingMethodManager = false
    @State private var methodError: String?
    @State private var coffeeDragStep = 0
    @State private var ratioDragStep = 0
    @State private var waterDragStep = 0

    var body: some View {
        frontCard
            .modifier(CalculatorFlip(rotation: flipped ? 180 : 0, back: AnyView(backCard)))
            .animation(reduceMotion ? nil : .easeInOut(duration: 0.42), value: flipped)
            .onAppear { recallBean() }
            .onChange(of: calculator.method) { _, _ in recallBean() }
            .onChange(of: beans.map { $0.id.uuidString + $0.brewProfilesJSON }) { _, _ in recallBean() }
    }

    private func recallBean() {
        if calculator.selectedBeanId != nil {
            calculator.selectBean(beans.first { $0.id == calculator.selectedBeanId })
        } else if calculator.needsInitialBeanSelection, let sample = beans.first(where: \.isSample) {
            calculator.selectBean(sample)
        }
    }

    // Matches Android: one compact touch target, with the next face explicitly named.
    private func flipHeader(back: Bool) -> some View {
        Button { flipped = !back } label: {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Calculadora barista")
                        .font(.system(size: 17, weight: .semibold, design: .serif))
                        .foregroundStyle(CupaTheme.text)
                    Text(back ? "Volver al cálculo" : "Ver reverso")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(CupaTheme.terracottaText)
                }
                Spacer(minLength: 0)
                Image(systemName: "arrow.triangle.2.circlepath")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(CupaTheme.terracottaText)
                    .frame(width: 36, height: 36)
                    .background(CupaTheme.terracottaText.opacity(0.08), in: Circle())
                    .overlay(Circle().stroke(CupaTheme.terracottaText.opacity(0.18), lineWidth: 1))
            }
            .frame(maxWidth: .infinity, minHeight: 48, alignment: .leading)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(back ? "Calculadora barista. Volver al cálculo" : "Calculadora barista. Ver ajustes del grano")
    }

    private var backCard: some View {
        CupaCard {
            VStack(alignment: .leading, spacing: 12) {
                flipHeader(back: true)
                Text("Método · \(calculator.method)").font(.caption.bold()).foregroundStyle(CupaTheme.forestText)
                Picker("Grano", selection: Binding(get: { calculator.selectedBeanId }, set: { id in calculator.selectBean(beans.first { $0.id == id }) })) {
                    Text("Elegir grano del Almacén").tag(Optional<UUID>.none)
                    ForEach(beans) { bean in Text(bean.name).tag(Optional(bean.id)) }
                }
                .tint(CupaTheme.forestText)
                if let bean = beans.first(where: { $0.id == calculator.selectedBeanId }) {
                    BeanMethodSettings(bean: bean, method: calculator.method, unit: settings.temperatureUnit)
                } else {
                    Text(beans.isEmpty ? "Agrega un grano en Almacén para guardar sus ajustes." : "Cada grano recuerda sus ajustes por método.")
                        .font(.caption).foregroundStyle(CupaTheme.secondaryText)
                }
            }
        }
    }

    private var frontCard: some View {
        CupaCard {
            VStack(spacing: 10) {
                flipHeader(back: false)

                VStack(spacing: 2) {
                    Text("AGUA").font(.caption2.bold()).tracking(1.5)
                    Text("\(calculator.water) ml")
                        .font(.system(.largeTitle, design: .rounded, weight: .black))
                    Text("\(calculator.coffeeInput) g · 1:\(calculator.ratioInput) · \(calculator.method)")
                        .font(.caption)
                }
                .foregroundStyle(CupaTheme.onAccent)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(LinearGradient(colors: [CupaTheme.forest, categorySurfaceColor], startPoint: .topLeading, endPoint: .bottomTrailing))
                .clipShape(RoundedRectangle(cornerRadius: 22))
                .accessibilityElement(children: .combine)
                .accessibilityLabel("Resultado: \(calculator.water) mililitros de agua, \(calculator.coffeeInput) gramos de café, proporción uno a \(calculator.ratioInput), método \(calculator.method)")

                Text(calculator.category.label)
                    .font(.caption.bold())
                    .foregroundStyle(categoryTextColor)
                    .frame(maxWidth: .infinity, alignment: .trailing)

                calculatorInputs(axis: .horizontal)

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

                HStack(spacing: 8) {
                    Button { showingMethodManager = true } label: { Image(systemName: "slider.horizontal.3") }
                        .frame(minWidth: 44, minHeight: 44)
                        .accessibilityLabel("Gestionar métodos de la calculadora")
                        .accessibilityIdentifier("calculator.manageMethods")
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 7) {
                            ForEach(quickMethodOptions) { option in
                                Button(option.name) { calculator.selectMethod(option.name, methodId: option.equipmentId) }
                                    .buttonStyle(.bordered)
                                    .tint(calculator.method.caseInsensitiveCompare(option.name) == .orderedSame ? categoryTextColor : CupaTheme.secondaryText)
                            }
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
                            .foregroundStyle(calculator.isCurrentFavorite ? CupaTheme.terracottaText : CupaTheme.secondaryText)
                    }
                    .frame(minWidth: 44, minHeight: 44)
                    .accessibilityLabel(calculator.isCurrentFavorite ? "Quitar de favoritos" : "Guardar como favorito")
                }
                .foregroundStyle(CupaTheme.secondaryText)

                ViewThatFits(in: .horizontal) {
                    calculatorActions
                    VStack(alignment: .leading) {
                        Button(action: onLab) { Label("Laboratorio", systemImage: "flask") }
                            .buttonStyle(.bordered)
                        Button(action: onPrepare) { Label("Preparar con estos datos", systemImage: "play.fill") }
                            .buttonStyle(.borderedProminent)
                            .tint(CupaTheme.forest)
                            .foregroundStyle(CupaTheme.onAccent)
                            .accessibilityIdentifier("calculator.prepare")
                    }
                }
            }
        }
        .brewKeyboardDismissToolbar()
        .sheet(isPresented: $showingMethodManager) {
            CalculatorMethodManager(
                calculator: calculator,
                equipment: methodEquipment,
                onEquipmentPinnedChange: setEquipmentPinned
            )
        }
        .alert("No se pudo actualizar el método", isPresented: Binding(get: { methodError != nil }, set: { if !$0 { methodError = nil } })) {
            Button("Aceptar") {}
        } message: { Text(methodError ?? "") }
    }

    private func calculatorInputs(axis: Axis) -> some View {
        Group {
            if axis == .horizontal {
                HStack(spacing: 8) { calculatorInputContent }
            } else {
                VStack(spacing: 8) { calculatorInputContent }
            }
        }
    }

    @ViewBuilder private var calculatorInputContent: some View {
        calculatorInput("CAFÉ (g)", identifier: "calculator.coffee", text: Binding(
            get: { calculator.coffeeInput },
            set: { calculator.changeCoffee($0) }
        ), dragAxis: .vertical, dragStep: $coffeeDragStep,
        adjust: { adjustCoffee(Double($0)) })

        calculatorInput("PROPORCIÓN", identifier: "calculator.ratio", text: Binding(
            get: { calculator.ratioInput },
            set: { calculator.changeRatio($0) }
        ), dragAxis: .horizontal, dragStep: $ratioDragStep,
        adjust: { adjustRatio(Double($0) * 0.1) })

        calculatorInput("AGUA (ml)", identifier: "calculator.water", text: Binding(
            get: { calculator.waterInput },
            set: { calculator.changeWater($0) }
        ), dragAxis: .vertical, dragStep: $waterDragStep,
        adjust: { adjustWater($0 * 10) })
    }

    private var calculatorActions: some View {
        HStack {
            Button(action: onLab) { Label("Laboratorio", systemImage: "flask") }
                .buttonStyle(.bordered)
            Button(action: onPrepare) { Label("Preparar", systemImage: "play.fill") }
                .buttonStyle(.borderedProminent)
                .tint(CupaTheme.forest)
                .foregroundStyle(CupaTheme.onAccent)
                .accessibilityIdentifier("calculator.prepare")
        }
    }

    private var methodEquipment: [EquipmentRecord] { activeEquipment.filter(\.isBrewingMethod) }

    private var allMethodOptions: [CalculatorMethodOption] {
        var seen = Set<String>()
        var result = calculator.methods.map { name in
            seen.insert(name.folding(options: [.caseInsensitive, .diacriticInsensitive], locale: .current))
            return CalculatorMethodOption(name: name, equipmentId: nil)
        }
        for item in methodEquipment {
            let key = item.name.folding(options: [.caseInsensitive, .diacriticInsensitive], locale: .current)
            if !item.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, seen.insert(key).inserted {
                result.append(.init(name: item.name, equipmentId: item.id))
            }
        }
        return result
    }

    private var quickMethodOptions: [CalculatorMethodOption] {
        let pinned = allMethodOptions.filter { option in
            if let id = option.equipmentId { return methodEquipment.first(where: { $0.id == id })?.isFavorite == true }
            return calculator.isMethodPinned(option.name)
        }
        return pinned.isEmpty ? allMethodOptions : pinned
    }

    private func setEquipmentPinned(_ equipment: EquipmentRecord, _ pinned: Bool) {
        equipment.isFavorite = pinned
        equipment.markUpdated()
        do { try context.save() }
        catch { context.rollback(); methodError = error.localizedDescription }
    }

    private var categorySurfaceColor: Color {
        switch calculator.category {
        case .espresso: CupaTheme.espresso
        case .intense: CupaTheme.gold
        case .balance: CupaTheme.forest
        case .clarity: CupaTheme.clarity
        }
    }

    private var categoryTextColor: Color {
        switch calculator.category {
        case .espresso: CupaTheme.espressoText
        case .intense: CupaTheme.goldText
        case .balance: CupaTheme.forestText
        case .clarity: CupaTheme.clarityText
        }
    }

    private func calculatorInput(
        _ title: String,
        identifier: String,
        text: Binding<String>,
        dragAxis: CalculatorDragAxis,
        dragStep: Binding<Int>,
        adjust: @escaping (Int) -> Void
    ) -> some View {
        VStack(spacing: 7) {
            Text(title).font(.caption2.bold()).foregroundStyle(CupaTheme.secondaryText)
                .lineLimit(1).minimumScaleFactor(0.8)
            TextField("", text: text)
                .keyboardType(.decimalPad)
                .multilineTextAlignment(.center)
                .font(.headline.monospacedDigit())
                .onSubmit { calculator.validateInputs() }
                .accessibilityLabel(title)
                .accessibilityIdentifier(identifier)
            HStack(spacing: 0) {
                Button { adjust(-1) } label: { Image(systemName: "minus.circle.fill") }
                    .frame(minWidth: 44, minHeight: 44)
                    .accessibilityLabel("Disminuir \(title)")
                Spacer(minLength: 0)
                Button { adjust(1) } label: { Image(systemName: "plus.circle.fill") }
                    .frame(minWidth: 44, minHeight: 44)
                    .accessibilityLabel("Aumentar \(title)")
            }
            .foregroundStyle(categoryTextColor)
        }
        .padding(.horizontal, 4).padding(.vertical, 8)
        .frame(minWidth: 0, maxWidth: .infinity)
        .background(CupaTheme.backgroundAlt.opacity(0.7))
        .clipShape(RoundedRectangle(cornerRadius: 15))
        .contentShape(RoundedRectangle(cornerRadius: 15))
        .highPriorityGesture(
            DragGesture(minimumDistance: 10)
                .onChanged { value in
                    let distance = dragAxis == .vertical ? -value.translation.height : value.translation.width
                    let threshold: CGFloat = dragAxis == .vertical ? 34 : 24
                    let nextStep = Int(distance / threshold)
                    let delta = nextStep - dragStep.wrappedValue
                    guard delta != 0 else { return }
                    dragStep.wrappedValue = nextStep
                    adjust(delta)
                }
                .onEnded { _ in dragStep.wrappedValue = 0 }
        )
        .accessibilityAdjustableAction { direction in
            switch direction {
            case .increment: adjust(1)
            case .decrement: adjust(-1)
            @unknown default: break
            }
        }
    }

    private func adjustCoffee(_ amount: Double) {
        calculator.adjustCoffee(amount)
        CalculatorHaptics.play(.coffee, milestone: calculator.coffee.truncatingRemainder(dividingBy: 5) == 0)
    }

    private func adjustRatio(_ amount: Double) {
        calculator.adjustRatio(amount)
        let nearestInteger = calculator.ratio.rounded()
        CalculatorHaptics.play(.ratio, milestone: abs(calculator.ratio - nearestInteger) < 0.001)
    }

    private func adjustWater(_ amount: Int) {
        calculator.adjustWater(amount)
        CalculatorHaptics.play(.water, milestone: calculator.water.isMultiple(of: 50))
    }
}

private enum CalculatorDragAxis { case vertical, horizontal }

private enum CalculatorHapticKind { case coffee, ratio, water }

private enum CalculatorHaptics {
    static func play(_ kind: CalculatorHapticKind, milestone: Bool) {
        if milestone {
            let generator = UIImpactFeedbackGenerator(style: .medium)
            generator.prepare()
            generator.impactOccurred(intensity: 0.82)
            return
        }
        switch kind {
        case .coffee:
            let generator = UIImpactFeedbackGenerator(style: .soft)
            generator.prepare()
            generator.impactOccurred(intensity: 0.55)
        case .ratio:
            let generator = UISelectionFeedbackGenerator()
            generator.prepare()
            generator.selectionChanged()
        case .water:
            let generator = UIImpactFeedbackGenerator(style: .rigid)
            generator.prepare()
            generator.impactOccurred(intensity: 0.62)
        }
    }
}

private struct CalculatorMethodOption: Identifiable {
    let name: String
    let equipmentId: UUID?
    var id: String { equipmentId?.uuidString ?? "builtin:\(name)" }
}

private struct CalculatorMethodManager: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.managedObjectContext) private var context
    @ObservedObject var calculator: CalculatorModel
    let equipment: [EquipmentRecord]
    let onEquipmentPinnedChange: (EquipmentRecord, Bool) -> Void
    @State private var showingAddMethod = false
    @State private var newMethodName = ""
    @State private var newMethodRatio = "16"
    @State private var saveError: String?

    var body: some View {
        NavigationStack {
            List {
                Section("Métodos base") {
                    ForEach(calculator.methods, id: \.self) { method in
                        Toggle(method, isOn: Binding(
                            get: { calculator.isMethodPinned(method) },
                            set: { calculator.setMethodPinned(method, pinned: $0) }
                        ))
                    }
                }
                Section("Métodos del Almacén") {
                    if equipment.isEmpty {
                        Text("Registra un equipo de tipo Método para añadirlo a la calculadora.")
                            .foregroundStyle(CupaTheme.secondaryText)
                    } else {
                        ForEach(equipment) { item in
                            Toggle(item.name, isOn: Binding(
                                get: { item.isFavorite },
                                set: { onEquipmentPinnedChange(item, $0) }
                            ))
                        }
                    }
                }
                Section {
                    Button { showingAddMethod = true } label: { Label("Agregar método", systemImage: "plus.circle.fill") }
                } footer: {
                    Text("El nuevo método quedará seleccionado en la calculadora y tendrá tres técnicas iniciales en Preparar café.")
                }
            }
            .brewScrollableCanvas()
            .navigationTitle("Gestionar métodos")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Listo") { dismiss() } } }
            .sheet(isPresented: $showingAddMethod) {
                NavigationStack {
                    Form {
                        TextField("Nombre del método", text: $newMethodName)
                        TextField("Proporción inicial 1:", text: $newMethodRatio).keyboardType(.decimalPad)
                    }
                    .navigationTitle("Nuevo método")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { showingAddMethod = false } }
                        ToolbarItem(placement: .confirmationAction) { Button("Guardar", action: saveMethod).disabled(newMethodName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) }
                    }
                }
                .presentationDetents([.medium])
            }
            .alert("No se pudo guardar", isPresented: Binding(get: { saveError != nil }, set: { if !$0 { saveError = nil } })) {
                Button("Aceptar") {}
            } message: { Text(saveError ?? "") }
        }
    }

    private func saveMethod() {
        let name = newMethodName.trimmingCharacters(in: .whitespacesAndNewlines)
        let ratio = Double(newMethodRatio.replacingOccurrences(of: ",", with: ".")) ?? 16
        let method = EquipmentRecord(context: context, name: name, equipmentType: "BREWER_METHOD", brand: "", model: "", capacityMl: nil, configuration: "", notes: "Método personalizado", isFavorite: true, isActive: true)
        do {
            try context.save()
            calculator.registerCustomMethod(name, methodId: method.id, defaultRatio: max(1, ratio))
            showingAddMethod = false; newMethodName = ""; newMethodRatio = "16"
        } catch { context.rollback(); saveError = error.localizedDescription }
    }
}

private enum LabControlCategory: String, CaseIterable, Identifiable {
    case ratio = "Proporción"
    case extraction = "Calor"
    case bean = "Grano"
    var id: Self { self }
}

struct LabView: View {
    @Environment(\.managedObjectContext) private var modelContext
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \LabExperimentRecord.createdAt, ascending: false)],
        predicate: LocalDataScope.visiblePredicate(),
        animation: .default
    ) private var experiments: FetchedResults<LabExperimentRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \RecipeRecord.name, ascending: true)], predicate: LocalDataScope.visiblePredicate()) private var recipes: FetchedResults<RecipeRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \TechniqueRecord.name, ascending: true)], predicate: LocalDataScope.visiblePredicate()) private var techniques: FetchedResults<TechniqueRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \CoffeeBeanRecord.name, ascending: true)], predicate: LocalDataScope.visiblePredicate()) private var beans: FetchedResults<CoffeeBeanRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \GrinderRecord.name, ascending: true)], predicate: LocalDataScope.visiblePredicate()) private var grinders: FetchedResults<GrinderRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \EquipmentRecord.name, ascending: true)], predicate: LocalDataScope.visiblePredicate(additional: NSPredicate(format: "equipmentType == 'BREWER_METHOD'"))) private var methods: FetchedResults<EquipmentRecord>
    @ObservedObject var model: LabModel
    @ObservedObject var preparation: PreparationModel
    @ObservedObject var account: AccountModel
    @Binding var selection: CupaTab
    @State private var category = LabControlCategory.extraction
    @State private var confirmingLabReset = false
    @State private var deletingExperiment: LabExperimentRecord?
    @State private var saveConfirmationMessage: String?
    @State private var showLabDetails = false
    @State private var errorMessage: String?
    @State private var isSavingExperiment = false
    @State private var isSavingTechnique = false
    @State private var profileExpanded = false

    var body: some View {
        ZStack(alignment: .bottom) {
            BrewOrganicCanvas().ignoresSafeArea()
            ScrollView {
            VStack(spacing: 8) {
                HStack {
                    VStack(alignment: .leading, spacing: 1) {
                        Text("Laboratorio").font(.title3.bold()).foregroundStyle(CupaTheme.text)
                    }
                    Spacer()
                    Button { showLabDetails = true } label: {
                        Label("Contexto", systemImage: "slider.horizontal.3")
                            .font(.caption.bold())
                    }
                    .buttonStyle(.bordered)
                }
                calibrationWorkspaceCard
            }
            .padding(.horizontal, 12).padding(.vertical, 6)
            }
            .scrollBounceBehavior(.basedOnSize)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) { actionBar }
        .navigationBarHidden(true)
        .sheet(isPresented: $showLabDetails) {
            NavigationStack {
                ScrollView {
                    VStack(spacing: 14) {
                        SectionHeader(eyebrow: "Contexto", title: "Base del experimento", subtitle: "Inventario e historial.")
                        baseDataCard
                        if !experiments.isEmpty { savedExperimentsCard }
                    }.padding()
                }
                .background(BrewOrganicCanvas().ignoresSafeArea())
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) { Button("Cerrar") { showLabDetails = false } }
                    ToolbarItem(placement: .primaryAction) {
                        Button { confirmingLabReset = true } label: { Image(systemName: "arrow.counterclockwise") }
                            .accessibilityLabel("Restablecer Laboratorio")
                    }
                }
            }
        }
        .alert("Guardado", isPresented: Binding(get: { saveConfirmationMessage != nil }, set: { if !$0 { saveConfirmationMessage = nil } })) {
            Button("Aceptar", role: .cancel) {}
        } message: {
            Text(saveConfirmationMessage ?? "")
        }
        .alert("No se pudo actualizar el experimento", isPresented: Binding(get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })) {
            Button("Aceptar") {}
        } message: { Text(errorMessage ?? "") }
        .confirmationDialog("¿Restablecer el Laboratorio?", isPresented: $confirmingLabReset, titleVisibility: .visible) {
            Button("Restablecer variables", role: .destructive, action: model.reset)
            Button("Cancelar", role: .cancel) {}
        } message: { Text("Se perderán los ajustes que no hayas guardado como experimento.") }
        .confirmationDialog("¿Eliminar este experimento?", isPresented: Binding(get: { deletingExperiment != nil }, set: { if !$0 { deletingExperiment = nil } }), titleVisibility: .visible) {
            Button("Eliminar experimento", role: .destructive) { if let experiment = deletingExperiment { deleteExperiment(experiment) }; deletingExperiment = nil }
            Button("Cancelar", role: .cancel) { deletingExperiment = nil }
        } message: { Text("Se retirará del historial local y la eliminación se sincronizará cuando haya conexión.") }
    }

    private var baseDataCard: some View {
        CupaCard {
            VStack(alignment: .leading, spacing: 12) {
                Text("Base e inventario").font(.headline)
                Picker("Receta base", selection: Binding(get: { model.state.recipeId }, set: loadRecipe)) {
                    Text("Sin receta base").tag(Optional<UUID>.none)
                    ForEach(recipes) { Text($0.name).tag(Optional($0.id)) }
                }
                Picker("Técnica base", selection: Binding(get: { model.state.techniqueId }, set: loadTechnique)) {
                    Text("Sin técnica base · modo libre").tag(Optional<UUID>.none)
                    ForEach(techniques) { Text($0.name).tag(Optional($0.id)) }
                }
                Picker("Método / equipo", selection: Binding(get: { model.state.methodId }, set: loadMethod)) {
                    Text("Sin equipo asignado · \(model.state.method)").tag(Optional<UUID>.none)
                    ForEach(methods) { Text($0.name).tag(Optional($0.id)) }
                }
                HStack {
                    Picker("Café", selection: binding(\.beanId)) { Text("Sin café seleccionado").tag(Optional<UUID>.none); ForEach(beans) { Text($0.name).tag(Optional($0.id)) } }
                    Picker("Molino", selection: binding(\.grinderId)) { Text("Sin molino seleccionado").tag(Optional<UUID>.none); ForEach(grinders) { Text($0.name).tag(Optional($0.id)) } }
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
        .foregroundStyle(CupaTheme.onAccent)
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(LinearGradient(colors: [CupaTheme.forest, CupaTheme.terracottaSurface], startPoint: .topLeading, endPoint: .bottomTrailing))
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .accessibilityElement(children: .combine)
        .accessibilityHint("El índice 1 punto 00 es la referencia; un valor menor indica subextracción y uno mayor indica más extracción")
    }

    private var calibrationWorkspaceCard: some View {
        CupaCard {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Text("Método").font(.caption).foregroundStyle(CupaTheme.secondaryText)
                    Spacer()
                    Menu {
                        ForEach(["V60", "AeroPress", "Prensa francesa", "Chemex", "Espresso", "Moka", "Cold brew"], id: \.self) { method in
                            Button(method) { model.update { $0.method = method } }
                        }
                    } label: {
                        HStack(spacing: 4) { Text(model.state.method); Image(systemName: "chevron.down") }
                            .font(.caption.bold()).foregroundStyle(CupaTheme.terracottaText)
                    }
                }
                VStack(spacing: 4) {
                    sensoryContent
                    variableTabs
                }
                Text("AJUSTES · \(category.rawValue.uppercased())")
                    .font(.caption2.bold()).tracking(0.8).foregroundStyle(CupaTheme.secondaryText)
                controlsContent
                compactHypothesis
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Variables de preparación y perfil sensorial en vivo")
    }


    private var variableTabs: some View {
        HStack(spacing: 4) {
            ForEach(LabControlCategory.allCases) { item in
                let selected = category == item
                Button {
                    withAnimation(.easeInOut(duration: 0.2)) { category = item }
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: item == .ratio ? "scalemass" : item == .extraction ? "thermometer.medium" : "leaf")
                        if selected { Text(item.rawValue).font(.caption.bold()) }
                    }
                    .frame(width: selected ? nil : 44, height: 44)
                    .frame(maxWidth: selected ? .infinity : nil)
                    .foregroundStyle(selected ? CupaTheme.onAccent : CupaTheme.secondaryText)
                    .background(selected ? CupaTheme.forest : CupaTheme.backgroundAlt, in: RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
                .accessibilityLabel(item.rawValue)
                .accessibilityAddTraits(selected ? .isSelected : [])
            }
        }
    }

    private var compactHypothesis: some View {
        DisclosureGroup(isExpanded: $profileExpanded) {
            VStack(alignment: .leading, spacing: 5) {
                Text(model.profile.summary).font(.caption)
                Text("1.00× es la referencia: menos = menor extracción; más = mayor extracción.")
                    .font(.caption2)
            }.padding(.top, 4)
        } label: {
            VStack(alignment: .leading, spacing: 3) {
            Text("PERFIL ESTIMADO").font(.caption2.bold()).tracking(0.8)
            HStack(alignment: .firstTextBaseline, spacing: 10) {
                Text(primaryOutcome).font(.caption.bold()).fixedSize(horizontal: false, vertical: true)
                Spacer()
                Text(String(format: "%.2f×", model.profile.extractionIndex)).font(.headline.monospacedDigit())
            }
            }
        }
        .tint(CupaTheme.onAccent)
        .foregroundStyle(CupaTheme.onAccent)
        .padding(.horizontal, 12).padding(.vertical, 8)
        .background(LinearGradient(colors: [CupaTheme.forest, CupaTheme.terracottaSurface], startPoint: .leading, endPoint: .trailing))
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .accessibilityElement(children: .combine)
        .accessibilityHint("El índice 1 punto 00 es la referencia; un valor menor indica subextracción y uno mayor indica más extracción")
    }

    private var sensoryContent: some View {
        let values = [
            ("Aroma", model.profile.aroma, Color(hex: 0xC59A5A)), ("Acidez", model.profile.acidity, Color(hex: 0xF2C14E)),
            ("Dulzor", model.profile.sweetness, Color(hex: 0xD98BB3)), ("Cuerpo", model.profile.body, Color(hex: 0x8B6B5C)),
            ("Amargor", model.profile.bitterness, Color(hex: 0x5C5641)), ("Final", model.profile.finish, Color(hex: 0x74BFE0))
        ]
        return VStack(alignment: .leading, spacing: 5) {
                Text("SABOR ESTIMADO").font(.caption2.bold()).tracking(0.8).foregroundStyle(CupaTheme.secondaryText)
                HStack(alignment: .bottom, spacing: 8) {
                    ForEach(values, id: \.0) { label, value, color in
                        VStack(spacing: 2) {
                            Text("\(value)").font(.system(size: 9, weight: .bold)).foregroundStyle(color).frame(height: 16)
                            GeometryReader { proxy in
                                ZStack(alignment: .bottom) {
                                    Rectangle().fill(CupaTheme.backgroundAlt)
                                    Rectangle().fill(color.gradient).frame(height: proxy.size.height * CGFloat(value) / 100)
                                }
                            }.frame(width: 12, height: 70)
                            Text(label).font(.system(size: 9, weight: .semibold)).lineLimit(2).multilineTextAlignment(.center).frame(height: 26)
                        }
                        .frame(maxWidth: .infinity)
                        .accessibilityElement(children: .ignore)
                        .accessibilityLabel(label)
                        .accessibilityValue("\(value) de 100")
                    }
                }
                HStack(spacing: 5) {
                    Circle().fill(CupaTheme.forest).frame(width: 6, height: 6)
                    Text(model.diagnostic.extraction).font(.caption2.bold()).foregroundStyle(CupaTheme.forestText).fixedSize(horizontal: false, vertical: true)
                    Spacer()
                }
        }
    }

    @ViewBuilder private var controlsContent: some View {
        VStack(spacing: 9) {
                switch category {
                case .ratio:
                    HStack {
                        Stepper("Café \(model.state.coffeeGrams.formatted()) g", value: labCoffeeBinding, in: 1...100, step: 1)
                        Divider()
                        Stepper("Agua \(model.state.waterMl) ml", value: labWaterBinding, in: 10...2000, step: 10)
                    }
                    labSlider("Proporción", value: labRatioBinding, range: 8...22, step: 0.5, display: "1:\(String(format: "%.1f", model.state.ratio))")
                    labSlider("Tiempo", value: bindingInt(\.timeSeconds), range: 60...360, step: 5, display: formattedTime)
                case .extraction:
                    labSlider("Temperatura", value: bindingInt(\.temperatureC), range: 80...98, step: 1, display: temperatureText)
                    temperatureCalibrationBand
                    labSlider("Clics de molienda", value: bindingInt(\.grindClicks), range: 6...36, step: 1, display: "\(model.state.grindClicks) clics")
                case .bean:
                    Picker("Frescura", selection: binding(\.freshness)) {
                        ForEach(["muy fresco", "en ventana", "punto ideal", "bajando", "viejo"], id: \.self) { Text($0.capitalized) }
                    }
                    TextField("Notas del experimento", text: binding(\.notes), axis: .vertical).lineLimit(1...3)
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
                                Text("1:\(experiment.ratio.formatted(.number.precision(.fractionLength(1)))) · \(experimentTemperatureText(experiment)) · \(experiment.cityName)")
                                    .font(.caption).foregroundStyle(CupaTheme.secondaryText)
                            }
                        }.buttonStyle(.plain).accessibilityLabel("Cargar experimento de \(experiment.method)")
                        Spacer()
                        Text(experiment.createdAt, style: .date).font(.caption2)
                        Button(role: .destructive) { deletingExperiment = experiment } label: { Image(systemName: "trash") }
                            .frame(minWidth: 44, minHeight: 44).accessibilityLabel("Eliminar experimento")
                    }
                }
            }
        }
    }


    private var actionBar: some View {
        HStack {
            Menu {
                Button { saveExperiment() } label: { Label(isSavingExperiment ? "Guardando experimento…" : "Guardar experimento", systemImage: "flask") }
                    .disabled(isSavingExperiment)
                Button { saveTechniqueFromLab() } label: { Label(isSavingTechnique ? "Guardando técnica…" : "Guardar como técnica", systemImage: "list.bullet.clipboard") }
                    .disabled(isSavingTechnique)
            } label: { Label("Guardar", systemImage: "square.and.arrow.down") }
                .buttonStyle(.bordered)
            Button {
                if saveExperiment() {
                    preparation.load(lab: model.state)
                    selection = .brew
                }
            } label: { Label("Preparar", systemImage: "play.fill") }
                .buttonStyle(.borderedProminent).tint(CupaTheme.forest).foregroundStyle(CupaTheme.onAccent)
                .disabled(isSavingExperiment)
        }
        .padding(.horizontal, 12).padding(.vertical, 8)
        .frame(maxWidth: .infinity).background(CupaTheme.card)
    }


    @discardableResult private func saveExperiment() -> Bool {
        guard !isSavingExperiment else { return false }
        isSavingExperiment = true
        _ = LabExperimentRecord(context: modelContext, state: model.state, profile: model.profile)
        do {
            try modelContext.save()
            isSavingExperiment = false
            saveConfirmationMessage = "La hipótesis quedó disponible offline en este dispositivo."
            return true
        } catch {
            modelContext.rollback()
            isSavingExperiment = false
            errorMessage = error.localizedDescription
            return false
        }
    }

    private func saveTechniqueFromLab() {
        guard !isSavingTechnique else { return }
        isSavingTechnique = true
        do {
            let technique = try RecipeTechniqueRepository(context: modelContext).saveTechnique(.fromLab(model.state))
            model.update { $0.techniqueId = technique.id; $0.techniqueName = technique.name }
            isSavingTechnique = false
            saveConfirmationMessage = "La técnica quedó en Almacén → Técnicas y está lista para preparar."
        } catch {
            modelContext.rollback()
            isSavingTechnique = false
            errorMessage = error.localizedDescription
        }
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
    private func experimentTemperatureText(_ experiment: LabExperimentRecord) -> String {
        model.state.temperatureUnit == .celsius
            ? "\(experiment.temperatureC) °C"
            : "\(Int(roundf(LabEngine.fahrenheit(fromCelsius: Float(experiment.temperatureC))))) °F"
    }
    private var formattedTime: String { String(format: "%d:%02d min", model.state.timeSeconds / 60, model.state.timeSeconds % 60) }
    private var temperatureCalibrationBand: some View {
        VStack(spacing: 3) {
            GeometryReader { proxy in
                let usableWidth = max(0, proxy.size.width - 4)
                HStack(spacing: 2) {
                    Capsule().fill(Color.blue.opacity(0.55)).frame(width: usableWidth * 10 / 18)
                    Capsule().fill(CupaTheme.forest.opacity(0.82)).frame(width: usableWidth * 6 / 18)
                    Capsule().fill(CupaTheme.terracotta.opacity(0.78)).frame(width: usableWidth * 2 / 18)
                }
                .overlay(alignment: .topLeading) {
                    HStack(spacing: 0) {
                        Color.clear.frame(width: proxy.size.width * 10 / 18)
                        Rectangle().fill(CupaTheme.text.opacity(0.6)).frame(width: 1, height: 10)
                        Color.clear.frame(width: proxy.size.width * 6 / 18)
                        Rectangle().fill(CupaTheme.text.opacity(0.6)).frame(width: 1, height: 10)
                    }
                }
            }
            .frame(height: 10)
            HStack(alignment: .top) {
                Text("80–89°\nMás acidez").frame(maxWidth: .infinity, alignment: .leading)
                Text("90–96°\nZona útil").frame(maxWidth: .infinity)
                Text("97–98°\nMás amargor").frame(maxWidth: .infinity, alignment: .trailing)
            }
            .font(.system(size: 9, weight: .semibold)).foregroundStyle(CupaTheme.secondaryText)
            if isTemperatureCapped {
                Text("En \(model.state.cityName), el límite físico es \(boilingText).")
                    .font(.caption2.bold()).foregroundStyle(.orange).frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Guía de temperatura: de 80 a 89 grados favorece acidez; de 90 a 96 es la zona útil; de 97 a 98 aumenta el amargor")
    }
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
            HStack { Text(title).font(.subheadline.bold()); Spacer(); Text(display).font(.subheadline.bold()).foregroundStyle(CupaTheme.terracottaText) }
            Slider(value: value, in: range, step: step).tint(CupaTheme.forest)
                .accessibilityLabel(title)
                .accessibilityValue(display)
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
    private var labCoffeeBinding: Binding<Double> {
        Binding(get: { Double(model.state.coffeeGrams) }, set: { model.setCoffeeGrams(Float($0)) })
    }
    private var labWaterBinding: Binding<Int> {
        Binding(get: { model.state.waterMl }, set: model.setWaterMl)
    }
    private var labRatioBinding: Binding<Double> {
        Binding(get: { Double(model.state.ratio) }, set: { model.setRatio(Float($0)) })
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
    @Binding var selection: CupaTab
    @ObservedObject var lab: LabModel
    @ObservedObject var preparation: PreparationModel
    @ObservedObject var account: AccountModel
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
            case .coffee: CoffeeInventoryView(selection: $selection, lab: lab, preparation: preparation)
            case .grinders: GrinderInventoryView()
            case .equipment: EquipmentInventoryView()
            case .recipes: RecipeInventoryView()
            case .techniques: TechniqueInventoryView(selection: $selection, preparation: preparation, account: account)
            case .cups: CupHistoryView()
            }
        }
        .background(BrewOrganicCanvas(warmTop: true).ignoresSafeArea())
        .navigationTitle("Almacén")
    }
}

private struct CupHistoryView: View {
    @Environment(\.managedObjectContext) private var context
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \CupSessionRecord.brewDate, ascending: false)],
        predicate: LocalDataScope.visiblePredicate(),
        animation: .default
    ) private var cups: FetchedResults<CupSessionRecord>
    @State private var errorMessage: String?
    @State private var selectedCup: CupSessionRecord?

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
                ForEach(Array(cups), id: \.objectID) { (cup: CupSessionRecord) in
                    Button { selectedCup = cup } label: {
                        CupHistoryRow(cup: cup)
                    }
                    .buttonStyle(.plain)
                    .accessibilityElement(children: .combine)
                    .accessibilityHint("Abre todos los datos de la taza")
                }
            }
        }
        .brewScrollableCanvas()
        .sheet(item: $selectedCup) { cup in
            CupSessionDetailView(cup: cup, onDelete: { if delete(cup) { selectedCup = nil } })
        }
        .alert("No se pudo eliminar la taza", isPresented: Binding(get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })) {
            Button("Aceptar") {}
        } message: { Text(errorMessage ?? "") }
    }

    private func delete(_ cup: CupSessionRecord) -> Bool {
        do { try TastingRepository(context: context).delete(cup); return true }
        catch { context.rollback(); errorMessage = error.localizedDescription; return false }
    }
}

private struct CupHistoryRow: View {
    @ObservedObject var cup: CupSessionRecord
    private var rating: String { cup.rating.formatted(.number.precision(.fractionLength(0...1))) + " ★" }
    private var quantities: String {
        let dose = cup.executedDoseGrams.formatted(.number.precision(.fractionLength(0...1)))
        return "\(cup.techniqueNameSnapshot) · \(dose) g → \(cup.executedWaterMl) ml"
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            HStack {
                Text(cup.beanNameSnapshot.isEmpty ? "Café sin registrar" : cup.beanNameSnapshot).font(.headline)
                Spacer()
                Text(rating).foregroundStyle(CupaTheme.goldText)
            }
            if cup.syncStatus != .synced { inventorySyncBadge(cup.syncStatus) }
            Text(quantities).font(.subheadline).foregroundStyle(CupaTheme.secondaryText)
            HStack {
                Label(cup.cupLifeState.localizedCupLife, systemImage: "thermometer.medium")
                if !cup.executedGrindSetting.isEmpty { Label(cup.executedGrindSetting, systemImage: "dial.medium") }
            }.font(.caption).foregroundStyle(CupaTheme.forestText)
            if !cup.comment.isEmpty { Text(cup.comment).font(.caption) }
            if let date = cup.brewDate { Text(date.formatted(date: .abbreviated, time: .shortened)).font(.caption2).foregroundStyle(.secondary) }
        }.padding(.vertical, 6)
    }
}

private struct CupSessionDetailView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var cup: CupSessionRecord
    let onDelete: () -> Void
    @State private var confirmingDelete = false

    var body: some View {
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            Label(cup.beanNameSnapshot.isEmpty ? "Café sin registrar" : cup.beanNameSnapshot, systemImage: "cup.and.saucer.fill").font(.title3.bold())
                            Spacer(); Text("\(cup.rating.formatted(.number.precision(.fractionLength(0...1)))) ★").font(.headline).foregroundStyle(CupaTheme.goldText)
                        }
                        if let date = cup.brewDate { Text(date.formatted(date: .long, time: .shortened)).font(.caption).foregroundStyle(CupaTheme.secondaryText) }
                    }.padding(.vertical, 4)
                }
                Section("Preparación ejecutada") {
                    cupDetailRow("Dosis", "\(cup.executedDoseGrams.formatted(.number.precision(.fractionLength(0...1)))) g")
                    cupDetailRow("Agua", "\(cup.executedWaterMl) ml")
                    cupDetailRow("Proporción", "1:\(cup.executedRatio.formatted(.number.precision(.fractionLength(0...1))))")
                    cupDetailRow("Temperatura", "\(cup.executedTemperatureC) °C")
                    cupDetailRow("Duración", String(format: "%02d:%02d", Int(cup.executedDurationSeconds) / 60, Int(cup.executedDurationSeconds) % 60))
                    if !cup.executedGrindSetting.isEmpty { cupDetailRow("Molienda", cup.executedGrindSetting) }
                }
                Section("Referencias históricas") {
                    if !cup.recipeNameSnapshot.isEmpty { cupDetailRow("Receta", cup.recipeNameSnapshot) }
                    cupDetailRow("Técnica", cup.techniqueNameSnapshot.isEmpty ? "Cata independiente" : cup.techniqueNameSnapshot)
                    if !cup.methodNameSnapshot.isEmpty { cupDetailRow("Método", cup.methodNameSnapshot) }
                    if !cup.grinderNameSnapshot.isEmpty { cupDetailRow("Molino", cup.grinderNameSnapshot) }
                }
                Section("Resultado de cata") {
                    cupDetailRow("Vida de taza", "\(cup.cupLifeState.localizedCupLife) · \(String(format: "%02d:%02d", Int(cup.cupLifeSeconds) / 60, Int(cup.cupLifeSeconds) % 60))")
                    cupDetailRow("Recomendación", "\(cup.nps)/10")
                    if !cup.comment.isEmpty { Text(cup.comment) }
                }
                Section {
                    Button(role: .destructive) { confirmingDelete = true } label: { Label("Eliminar taza y cata", systemImage: "trash") }
                }
            }
            .brewScrollableCanvas()
            .navigationTitle("Detalle de taza")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Cerrar") { dismiss() } } }
            .confirmationDialog("¿Eliminar esta taza?", isPresented: $confirmingDelete, titleVisibility: .visible) {
                Button("Eliminar taza y cata", role: .destructive, action: onDelete)
                Button("Cancelar", role: .cancel) {}
            } message: {
                Text("También se ocultarán la cata y sus observaciones asociadas. La preparación original se conservará.")
            }
        }
    }

    private func cupDetailRow(_ title: String, _ value: String) -> some View {
        HStack(alignment: .top) { Text(title); Spacer(); Text(value).multilineTextAlignment(.trailing).fontWeight(.semibold).foregroundStyle(CupaTheme.forestText) }
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
        predicate: LocalDataScope.visiblePredicate(),
        animation: .default
    ) private var beans: FetchedResults<CoffeeBeanRecord>
    @Binding var selection: CupaTab
    @ObservedObject var lab: LabModel
    @ObservedObject var preparation: PreparationModel
    @State private var showAddBean = false
    @State private var selectedBean: CoffeeBeanRecord?
    @State private var editedBean: CoffeeBeanRecord?
    @State private var pendingDeleteBean: CoffeeBeanRecord?
    @State private var errorMessage: String?

    var body: some View {
        ZStack {
            BrewOrganicCanvas(warmTop: true).ignoresSafeArea()
            List {
                Section("Cafés activos") {
                    if activeBeans.isEmpty {
                        ContentUnavailableView(
                            "Sin cafés activos",
                            systemImage: "leaf",
                            description: Text("Agrega tu primer café para usarlo en preparaciones, recetas y catas.")
                        )
                        .listRowBackground(Color.clear)
                    } else {
                        ForEach(activeBeans) { bean in coffeeRow(bean) }
                    }
                }
                if !finishedBeans.isEmpty {
                    Section("Lotes históricos / terminados") {
                        ForEach(finishedBeans) { bean in coffeeRow(bean) }
                    }
                }
            }
            .brewScrollableCanvas(warmTop: true)
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
        .sheet(item: $selectedBean) { bean in
            CoffeeBeanDetail(
                record: bean,
                onPrepare: { preparation.selectBean(bean); selection = .brew },
                onLab: { lab.load(bean: bean); selection = .lab },
                onDelete: { delete(bean) }
            )
        }
        .sheet(item: $editedBean) { bean in
            CoffeeBeanEditor(record: bean) { draft in
                draft.apply(to: bean)
                bean.markUpdated()
                return save()
            }
        }
        .confirmationDialog("¿Eliminar este café?", isPresented: Binding(
            get: { pendingDeleteBean != nil }, set: { if !$0 { pendingDeleteBean = nil } }), titleVisibility: .visible) {
            Button("Eliminar café", role: .destructive) {
                if let bean = pendingDeleteBean { errorMessage = delete(bean) }
                pendingDeleteBean = nil
            }
            Button("Cancelar", role: .cancel) { pendingDeleteBean = nil }
        } message: { Text("Las preparaciones y tazas conservarán sus datos históricos.") }
        .alert("No se pudo guardar el café", isPresented: Binding(get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })) {
            Button("Aceptar") {}
        } message: { Text(errorMessage ?? "") }
    }

    private var activeBeans: [CoffeeBeanRecord] { beans.filter { $0.inventoryStatus != .finished } }
    private var finishedBeans: [CoffeeBeanRecord] { beans.filter { $0.inventoryStatus == .finished } }

    private func coffeeRow(_ bean: CoffeeBeanRecord) -> some View {
        let freshness = CoffeeFreshnessEngine.evaluate(roastDate: bean.roastDate, openedDate: bean.openedDate)
        let accent = CupaTheme.coffeeAccent(bean.id)
        return VStack(alignment: .leading, spacing: 10) {
            Button { selectedBean = bean } label: {
                VStack(alignment: .leading, spacing: 5) {
                    HStack(spacing: 10) {
                        RoundedRectangle(cornerRadius: 2).fill(accent).frame(width: 4, height: 28)
                        Text(bean.name).font(.system(size: 18, weight: .semibold, design: .serif))
                            .foregroundStyle(CupaTheme.text).lineLimit(2)
                        Spacer(minLength: 0)
                    }
                    HStack(alignment: .top, spacing: 8) {
                        Text(bean.origin.isEmpty ? "Origen sin registrar" : bean.origin)
                            .foregroundStyle(CupaTheme.secondaryText).lineLimit(2)
                        Spacer(minLength: 0)
                        Text(bean.altitudeMeters.map { "\($0) m" } ?? "Altura sin registrar")
                            .foregroundStyle(accent)
                    }.font(.system(size: 12))
                    CoffeeFreshnessBar(result: freshness, compact: true)
                }.contentShape(Rectangle())
            }
            .accessibilityIdentifier("coffee.row.\(bean.id.uuidString)")
            Button { selectedBean = bean } label: {
                HStack {
                    Text("Cómo lo preparo").font(.system(size: 12, weight: .semibold))
                    Spacer()
                    Image(systemName: "chevron.right").font(.system(size: 14))
                }.foregroundStyle(accent).frame(minHeight: 40).contentShape(Rectangle())
            }.accessibilityIdentifier("coffee.preparation.\(bean.id.uuidString)")
            HStack(spacing: 0) {
                Button { editedBean = bean } label: {
                    Image(systemName: "pencil").frame(width: 44, height: 44)
                }.accessibilityLabel("Editar")
                Spacer(minLength: 0)
                Button { lab.load(bean: bean); selection = .lab } label: {
                    Image(systemName: "flask").frame(width: 44, height: 44)
                }.accessibilityLabel("Usar en Laboratorio")
                Spacer(minLength: 0)
                Button { preparation.selectBean(bean); selection = .brew } label: {
                    Label("Preparar", systemImage: "mug")
                        .font(.system(size: 12, weight: .medium)).padding(.horizontal, 14)
                        .frame(minHeight: 44).foregroundStyle(CupaTheme.onAccent)
                        .background(CupaTheme.forest, in: Capsule())
                }
                if !bean.isSample {
                    Spacer(minLength: 0)
                    Button { pendingDeleteBean = bean } label: {
                        Image(systemName: "trash").foregroundStyle(CupaTheme.terracottaText)
                            .frame(width: 44, height: 44)
                    }.accessibilityLabel("Borrar")
                }
            }.foregroundStyle(accent)
        }
        .buttonStyle(.plain)
        .padding(16)
        .background {
            ZStack {
                CupaTheme.card
                LinearGradient(colors: [accent.opacity(0.10), .clear], startPoint: .topLeading, endPoint: .bottomTrailing)
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .overlay(RoundedRectangle(cornerRadius: 24).stroke(accent.opacity(0.22), lineWidth: 1))
        .shadow(color: CupaTheme.warmShadow.opacity(0.08), radius: 4, y: 2)
        .listRowInsets(EdgeInsets(top: 6, leading: 0, bottom: 6, trailing: 0))
        .listRowSeparator(.hidden)
        .listRowBackground(Color.clear)
    }
    private func delete(_ bean: CoffeeBeanRecord) -> String? {
        guard !bean.isSample else { return "Ronpotrero es el café de muestra y siempre estará disponible." }
        bean.markDeleted()
        return save()
    }

    @discardableResult private func save() -> String? {
        do { try modelContext.save(); return nil }
        catch { modelContext.rollback(); return error.localizedDescription }
    }
}

private struct CoffeeBeanDetail: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.managedObjectContext) private var modelContext
    @ObservedObject private var record: CoffeeBeanRecord
    private let onPrepare: () -> Void
    private let onLab: () -> Void
    private let onDelete: () -> String?
    @FetchRequest private var brews: FetchedResults<BrewSessionRecord>
    @FetchRequest private var cups: FetchedResults<CupSessionRecord>
    @State private var showEditor = false
    @State private var actionError: String?
    @State private var confirmingFinished = false
    @State private var confirmingDelete = false

    init(record: CoffeeBeanRecord, onPrepare: @escaping () -> Void, onLab: @escaping () -> Void, onDelete: @escaping () -> String?) {
        _record = ObservedObject(wrappedValue: record)
        self.onPrepare = onPrepare
        self.onLab = onLab
        self.onDelete = onDelete
        _brews = FetchRequest(
            sortDescriptors: [NSSortDescriptor(keyPath: \BrewSessionRecord.completedAt, ascending: false)],
            predicate: LocalDataScope.visiblePredicate(additional: NSPredicate(format: "beanId == %@", record.id as CVarArg)),
            animation: .default
        )
        _cups = FetchRequest(
            sortDescriptors: [NSSortDescriptor(keyPath: \CupSessionRecord.brewDate, ascending: false)],
            predicate: LocalDataScope.visiblePredicate(additional: NSPredicate(format: "beanId == %@", record.id as CVarArg)),
            animation: .default
        )
    }

    var body: some View {
        NavigationStack {
            List {
                Section("Café") {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(record.name).font(.title3.bold())
                        Text("\(record.origin.isEmpty ? "Origen sin registrar" : record.origin) · \(record.altitudeMeters.map { "\($0) m" } ?? "Altura sin registrar")")
                            .font(.subheadline).foregroundStyle(CupaTheme.secondaryText)
                        CoffeeFreshnessBar(result: freshness, compact: true)
                    }
                    .padding(.vertical, 4)
                }

                Section("Cómo lo preparo") {
                    InventoryBeanBrewSettings(bean: record)
                }

                Section {
                    DisclosureGroup("Ficha completa") {
                        Text(record.brand.isEmpty ? "Sin tostador" : record.brand)
                        if !details.isEmpty { Text(details) }
                        Label("\(record.remainingQuantityGrams.formatted(.number.precision(.fractionLength(0...1)))) g disponibles", systemImage: "scalemass")
                        if !record.notes.isEmpty { Text(record.notes) }
                        if record.isSample { Text("Muestra · Datos ficticios") }
                        if record.syncStatus != .synced { inventorySyncBadge(record.syncStatus) }
                        if let warning = freshness.openWarning { Text(warning).foregroundStyle(CupaTheme.terracottaText) }
                    }.font(.subheadline)
                }

                Section("Acciones") {
                    HStack {
                        Button { onPrepare(); dismiss() } label: { Label("Preparar", systemImage: "mug") }
                            .buttonStyle(.borderedProminent).tint(CupaTheme.forest).foregroundStyle(CupaTheme.onAccent)
                        Spacer()
                        Button { onLab(); dismiss() } label: { Label("Llevar a Lab", systemImage: "flask") }
                            .buttonStyle(.bordered).tint(CupaTheme.terracotta)
                    }
                    if record.inventoryStatus == .closed {
                        Button("Abrir bolsa hoy", systemImage: "shippingbox.and.arrow.backward") { markOpened() }
                    }
                    if !record.isSample && record.inventoryStatus != .finished {
                        Button("Marcar como terminado", systemImage: "checkmark.circle") { confirmingFinished = true }
                            .foregroundStyle(CupaTheme.terracottaText)
                    }
                }

                Section("Uso") {
                    LabeledContent("Preparaciones", value: "\(brews.count)")
                    LabeledContent("Tazas catadas", value: "\(cups.count)")
                }

                Section("Preparaciones recientes") {
                    if brews.isEmpty {
                        Text("Todavía no hay preparaciones vinculadas con este café.")
                            .foregroundStyle(CupaTheme.secondaryText)
                    } else {
                        ForEach(brews) { brew in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(brew.techniqueNameSnapshot.isEmpty ? (brew.methodNameSnapshot.isEmpty ? "Preparación" : brew.methodNameSnapshot) : brew.techniqueNameSnapshot)
                                    .font(.headline)
                                if !brew.methodNameSnapshot.isEmpty && brew.methodNameSnapshot != brew.techniqueNameSnapshot {
                                    Text(brew.methodNameSnapshot).font(.subheadline).foregroundStyle(CupaTheme.secondaryText)
                                }
                                Text("\(brew.doseGrams.formatted(.number.precision(.fractionLength(0...1)))) g → \(brew.waterMl) ml · 1:\(brew.ratio.formatted(.number.precision(.fractionLength(0...1))))")
                                    .font(.subheadline)
                                Text(brew.completedAt.formatted(date: .abbreviated, time: .shortened))
                                    .font(.caption).foregroundStyle(CupaTheme.secondaryText)
                            }
                            .padding(.vertical, 3)
                        }
                    }
                }

                Section("Tazas y catas") {
                    if cups.isEmpty {
                        Text("Todavía no hay tazas catadas con este café.")
                            .foregroundStyle(CupaTheme.secondaryText)
                    } else {
                        ForEach(cups) { cup in
                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Text(cup.techniqueNameSnapshot.isEmpty ? "Cata" : cup.techniqueNameSnapshot).font(.headline)
                                    Spacer()
                                    Label(cup.rating.formatted(.number.precision(.fractionLength(1))), systemImage: "star.fill")
                                        .font(.subheadline).foregroundStyle(CupaTheme.goldText)
                                }
                                Text(cup.cupLifeState.localizedCupLife)
                                    .font(.subheadline).foregroundStyle(CupaTheme.forestText)
                                if !cup.comment.isEmpty { Text(cup.comment).font(.subheadline) }
                                Text((cup.brewDate ?? cup.createdAt).formatted(date: .abbreviated, time: .shortened))
                                    .font(.caption).foregroundStyle(CupaTheme.secondaryText)
                            }
                            .padding(.vertical, 3)
                        }
                    }
                }

                Section {
                    if !record.isSample {
                        Button(role: .destructive) { confirmingDelete = true } label: { Label("Eliminar café", systemImage: "trash") }
                    }
                }
            }
            .brewScrollableCanvas()
            .navigationTitle("Historial del café")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cerrar") { dismiss() } }
                ToolbarItem(placement: .primaryAction) { Button("Editar") { showEditor = true }.accessibilityIdentifier("coffee.detail.edit") }
            }
            .sheet(isPresented: $showEditor) {
                CoffeeBeanEditor(record: record) { draft in
                    draft.apply(to: record)
                    record.markUpdated()
                    return save()
                }
            }
            .alert("No se pudo actualizar el café", isPresented: Binding(get: { actionError != nil }, set: { if !$0 { actionError = nil } })) {
                Button("Aceptar") {}
            } message: { Text(actionError ?? "") }
            .confirmationDialog("¿Marcar este lote como terminado?", isPresented: $confirmingFinished, titleVisibility: .visible) {
                Button("Marcar como terminado", role: .destructive, action: markFinished)
                Button("Cancelar", role: .cancel) {}
            } message: { Text("La cantidad restante cambiará a 0 g. Puedes corregirla después desde Editar.") }
            .confirmationDialog("¿Eliminar este café?", isPresented: $confirmingDelete, titleVisibility: .visible) {
                Button("Eliminar café", role: .destructive) {
                    if let error = onDelete() { actionError = error } else { dismiss() }
                }
                Button("Cancelar", role: .cancel) {}
            } message: { Text("Se retirará del inventario, pero las preparaciones y tazas conservarán sus datos históricos.") }
        }
    }

    private var freshness: CoffeeFreshnessResult {
        CoffeeFreshnessEngine.evaluate(roastDate: record.roastDate, openedDate: record.openedDate)
    }

    private var details: String {
        [record.origin, record.variety, record.process, "Tueste \(record.roastLevel.lowercased())"]
            .filter { !$0.isEmpty }.joined(separator: " · ")
    }

    private func save() -> String? {
        do { try modelContext.save(); return nil }
        catch { modelContext.rollback(); return error.localizedDescription }
    }

    private func markOpened() {
        record.openedDate = .now; record.markUpdated()
        if let error = save() { actionError = error }
    }

    private func markFinished() {
        record.remainingQuantityGrams = 0; record.markUpdated()
        if let error = save() { actionError = error }
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
    var compact = false
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(result.daysFromRoast.map { "Día \($0) desde tostado" } ?? "Sin datos de tueste")
                Spacer()
                Text(compact ? result.state.label : "\(Int((result.progress * 100).rounded(.towardZero)))% est. útil")
            }
            .font(.caption2.bold()).foregroundStyle(CupaTheme.secondaryText)
            if compact {
                GeometryReader { geometry in
                    ZStack(alignment: .leading) {
                        Capsule().fill(LinearGradient(colors: [0x84AD92, 0x3F7A63, 0xC28B46, 0xB76545, 0x8C5A2B].map { Color(hex: UInt($0)) }, startPoint: .leading, endPoint: .trailing))
                        if result.daysFromRoast != nil {
                            Circle().fill(Color(hex: result.state.colorHex)).frame(width: 8, height: 8)
                                .overlay(Circle().stroke(.white, lineWidth: 2))
                                .offset(x: max(0, min(geometry.size.width - 8, geometry.size.width * result.progress - 4)))
                        }
                    }
                }.frame(height: 12)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("Maduración estimada: \(result.state.label)")
            } else { ProgressView(value: result.progress)
                .tint(Color(hex: result.state.colorHex))
                .accessibilityLabel("Maduración estimada")
                .accessibilityValue("\(Int((result.progress * 100).rounded())) por ciento")
            Text(result.openStatusDetails).font(.caption2).foregroundStyle(CupaTheme.secondaryText)
            }
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
        record.name = record.isSample ? "Ronpotrero" : name
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

private struct CoffeeBeanEditorFormDraft: Codable, Equatable {
    var recordId: UUID?
    var name: String
    var brand: String
    var origin: String
    var producer: String
    var variety: String
    var process: String
    var altitude: String
    var roastLevel: String
    var hasRoastDate: Bool
    var roastDate: Date
    var hasOpenedDate: Bool
    var openedDate: Date
    var initialQuantity: String
    var remainingQuantity: String
    var notes: String

    init(record: CoffeeBeanRecord?) {
        recordId = record?.id
        name = record?.name ?? ""
        brand = record?.brand ?? ""
        origin = record?.origin ?? ""
        producer = record?.producer ?? ""
        variety = record?.variety ?? ""
        process = record?.process ?? ""
        altitude = record?.altitudeMeters.map(String.init) ?? ""
        roastLevel = record?.roastLevel ?? "Medio"
        hasRoastDate = record?.roastDate != nil
        roastDate = record?.roastDate ?? .now
        hasOpenedDate = record?.openedDate != nil
        openedDate = record?.openedDate ?? .now
        initialQuantity = record.map { String(format: "%.1f", $0.initialQuantityGrams) } ?? "250.0"
        remainingQuantity = record.map { String(format: "%.1f", $0.remainingQuantityGrams) } ?? "250.0"
        notes = record?.notes ?? ""
    }
}

private struct CoffeeBeanEditor: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.managedObjectContext) private var context
    private let record: CoffeeBeanRecord?
    private let onSave: (CoffeeBeanDraft) -> String?
    @State private var draft: CoffeeBeanEditorFormDraft
    @State private var loaded = false
    @State private var isSaving = false
    @State private var saveError: String?
    @SceneStorage("cupa.coffeeBeanEditorDraft.v1") private var storedDraft: Data?

    init(record: CoffeeBeanRecord?, onSave: @escaping (CoffeeBeanDraft) -> String?) {
        self.record = record
        self.onSave = onSave
        _draft = State(initialValue: CoffeeBeanEditorFormDraft(record: record))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Identidad") {
                    TextField("Nombre del café", text: $draft.name).disabled(record?.isSample == true)
                    TextField("Marca o tostador", text: $draft.brand)
                    TextField("Origen", text: $draft.origin)
                    TextField("Productor", text: $draft.producer)
                    TextField("Variedad", text: $draft.variety)
                    TextField("Proceso", text: $draft.process)
                    TextField("Altitud (msnm)", text: $draft.altitude).keyboardType(.numberPad)
                }
                Section("Tueste y apertura") {
                    Picker("Tueste", selection: $draft.roastLevel) {
                    ForEach(["Claro", "Medio", "Oscuro"], id: \.self) { Text($0) }
                    }
                    Toggle("Con fecha de tueste", isOn: $draft.hasRoastDate)
                    if draft.hasRoastDate { DatePicker("Fecha de tueste", selection: $draft.roastDate, displayedComponents: .date) }
                    Toggle("Bolsa abierta", isOn: $draft.hasOpenedDate)
                    if draft.hasOpenedDate { DatePicker("Fecha de apertura", selection: $draft.openedDate, displayedComponents: .date) }
                }
                Section("Frescura estimada") {
                    HStack { CoffeeFreshnessBadge(state: freshness.state); Spacer(); Text(freshness.openStatusDetails).font(.caption).foregroundStyle(.secondary) }
                    CoffeeFreshnessBar(result: freshness)
                    Text(freshness.recommendation).font(.caption).foregroundStyle(CupaTheme.secondaryText)
                    if let warning = freshness.openWarning {
                        Label(warning, systemImage: "exclamationmark.triangle.fill").font(.caption).foregroundStyle(CupaTheme.terracottaText)
                    }
                }
                Section("Inventario") {
                    TextField("Cantidad inicial (g)", text: $draft.initialQuantity).keyboardType(.decimalPad)
                    TextField("Cantidad restante (g)", text: $draft.remainingQuantity).keyboardType(.decimalPad)
                    if let validationMessage { Text(validationMessage).font(.caption).foregroundStyle(.red) }
                    TextField("Notas", text: $draft.notes, axis: .vertical).lineLimit(3...8)
                }
            }
            .navigationTitle(record == nil ? "Agregar café" : "Editar café")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { clearStoredDraft(); dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isSaving ? "Guardando…" : "Guardar") {
                        guard !isSaving else { return }
                        let values: CoffeeBeanValidatedInput
                        do { values = try validatedInput() }
                        catch { saveError = error.localizedDescription; return }
                        isSaving = true
                        let error = onSave(CoffeeBeanDraft(
                            name: draft.name.trimmingCharacters(in: .whitespacesAndNewlines),
                            brand: draft.brand.trimmingCharacters(in: .whitespacesAndNewlines),
                            origin: draft.origin.trimmingCharacters(in: .whitespacesAndNewlines),
                            producer: draft.producer.trimmingCharacters(in: .whitespacesAndNewlines),
                            variety: draft.variety.trimmingCharacters(in: .whitespacesAndNewlines),
                            process: draft.process.trimmingCharacters(in: .whitespacesAndNewlines),
                            altitudeMeters: values.altitudeMeters,
                            roastLevel: draft.roastLevel,
                            roastDate: draft.hasRoastDate ? draft.roastDate : nil,
                            openedDate: draft.hasOpenedDate ? draft.openedDate : nil,
                            initialQuantityGrams: values.initialQuantityGrams,
                            remainingQuantityGrams: values.remainingQuantityGrams,
                            notes: draft.notes.trimmingCharacters(in: .whitespacesAndNewlines)
                        ))
                        if let error { isSaving = false; saveError = error }
                        else { clearStoredDraft(); dismiss() }
                    }
                    .disabled(draft.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || validationMessage != nil || isSaving)
                }
            }
            .brewScrollableCanvas()
            .onAppear(perform: load)
            .onChange(of: draft) { _, value in
                guard loaded else { return }
                storedDraft = storingEditorDraft(value, ownerId: context.activeOwnerId, in: storedDraft)
            }
            .alert("No se pudo guardar el café", isPresented: Binding(get: { saveError != nil }, set: { if !$0 { saveError = nil } })) {
                Button("Aceptar") {}
            } message: { Text(saveError ?? "") }
        }
    }

    private func validatedInput() throws -> CoffeeBeanValidatedInput {
        try CoffeeBeanInputValidator.validate(
            altitudeText: draft.altitude,
            initialQuantityText: draft.initialQuantity,
            remainingQuantityText: draft.remainingQuantity,
            roastDate: draft.hasRoastDate ? draft.roastDate : nil,
            openedDate: draft.hasOpenedDate ? draft.openedDate : nil
        )
    }

    private var validationMessage: String? {
        do { _ = try validatedInput(); return nil }
        catch { return error.localizedDescription }
    }

    private var freshness: CoffeeFreshnessResult {
        CoffeeFreshnessEngine.evaluate(
            roastDate: draft.hasRoastDate ? draft.roastDate : nil,
            openedDate: draft.hasOpenedDate ? draft.openedDate : nil
        )
    }

    private func load() {
        guard !loaded else { return }
        loaded = true
        if let restored: CoffeeBeanEditorFormDraft = scopedEditorDraft(from: storedDraft, ownerId: context.activeOwnerId),
           restored.recordId == record?.id {
            draft = restored
        }
    }

    private func clearStoredDraft() {
        storedDraft = removingEditorDraft(ownerId: context.activeOwnerId, from: storedDraft, as: CoffeeBeanEditorFormDraft.self)
    }
}
