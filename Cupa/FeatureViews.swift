import SwiftUI
import CoreData

struct HomeView: View {
    @Binding var selection: CupaTab

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
                        Label("Mi perfil", systemImage: "person.crop.circle")
                            .font(.subheadline.weight(.semibold))
                        Spacer()
                        Image(systemName: "bell")
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
    }
}

struct BrewView: View {
    @Binding var selection: CupaTab
    @ObservedObject var calculator: CalculatorModel
    @State private var isTimerRunning = false
    @State private var elapsed = 0
    @State private var timer: Timer?

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
                                calculatorInput("CAFÉ (g)", text: Binding(
                                    get: { calculator.coffeeInput },
                                    set: { calculator.changeCoffee($0) }
                                ), minus: { calculator.adjustCoffee(-1) }, plus: { calculator.adjustCoffee(1) })

                                calculatorInput("RATIO (1:x)", text: Binding(
                                    get: { calculator.ratioInput },
                                    set: { calculator.changeRatio($0) }
                                ), minus: { calculator.adjustRatio(-0.1) }, plus: { calculator.adjustRatio(0.1) })

                                calculatorInput("AGUA (ml)", text: Binding(
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
                                Button { calculator.toggleFavorite() } label: {
                                    Image(systemName: calculator.isCurrentFavorite ? "heart.fill" : "heart")
                                        .foregroundStyle(calculator.isCurrentFavorite ? CupaTheme.terracotta : CupaTheme.secondaryText)
                                }
                            }
                            .foregroundStyle(CupaTheme.secondaryText)

                            HStack {
                                Button { selection = .lab } label: { Label("Laboratorio", systemImage: "flask") }
                                    .buttonStyle(.bordered)
                                Button("Preparar con estos datos") { resetTimer() }
                                    .buttonStyle(.borderedProminent)
                                    .tint(CupaTheme.forest)
                            }
                        }
                    }

                    CupaCard {
                        VStack(spacing: 14) {
                            Text(timeString)
                                .font(.system(size: 52, weight: .bold, design: .rounded))
                                .monospacedDigit()
                            HStack {
                                Button(isTimerRunning ? "Pausar" : "Iniciar") { toggleTimer() }
                                    .buttonStyle(.borderedProminent)
                                    .tint(CupaTheme.forest)
                                Button("Reiniciar") { resetTimer() }
                                    .buttonStyle(.bordered)
                            }
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
                .padding()
            }
        }
        .navigationTitle("Preparar")
        .navigationBarTitleDisplayMode(.inline)
        .onDisappear { timer?.invalidate() }
    }

    private var timeString: String {
        String(format: "%02d:%02d", elapsed / 60, elapsed % 60)
    }

    private var categoryColor: Color {
        switch calculator.category {
        case .espresso: Color(hex: 0xB85D42)
        case .intense: CupaTheme.gold
        case .balance: CupaTheme.forest
        case .clarity: Color(hex: 0x2E5A44)
        }
    }

    private func calculatorInput(_ title: String, text: Binding<String>, minus: @escaping () -> Void, plus: @escaping () -> Void) -> some View {
        VStack(spacing: 7) {
            Text(title).font(.system(size: 9, weight: .bold)).foregroundStyle(CupaTheme.secondaryText)
            TextField("", text: text)
                .keyboardType(.decimalPad)
                .multilineTextAlignment(.center)
                .font(.headline.monospacedDigit())
                .onSubmit { calculator.validateInputs() }
            HStack {
                Button(action: minus) { Image(systemName: "minus.circle.fill") }
                Spacer()
                Button(action: plus) { Image(systemName: "plus.circle.fill") }
            }
            .foregroundStyle(categoryColor)
        }
        .padding(9)
        .background(CupaTheme.backgroundAlt.opacity(0.7))
        .clipShape(RoundedRectangle(cornerRadius: 15))
    }

    private func toggleTimer() {
        isTimerRunning.toggle()
        timer?.invalidate()
        if isTimerRunning {
            timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in elapsed += 1 }
        }
    }

    private func resetTimer() {
        timer?.invalidate()
        timer = nil
        elapsed = 0
        isTimerRunning = false
    }
}

struct TastingView: View {
    @State private var aroma = 3.0
    @State private var sweetness = 3.0
    @State private var acidity = 3.0
    @State private var bodyLevel = 3.0
    @State private var notes = ""
    @State private var saved = false

    var body: some View {
        ZStack {
            CupaTheme.background.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 20) {
                    SectionHeader(eyebrow: "Cata", title: "Mapa sensorial", subtitle: "Registra cómo cambia la taza mientras se enfría.")
                    CupaCard {
                        VStack(spacing: 18) {
                            sensorySlider("Aroma", value: $aroma)
                            sensorySlider("Dulzor", value: $sweetness)
                            sensorySlider("Acidez", value: $acidity)
                            sensorySlider("Cuerpo", value: $bodyLevel)
                        }
                    }
                    CupaCard {
                        TextField("Notas: cacao, frutos rojos…", text: $notes, axis: .vertical)
                            .lineLimit(4...8)
                    }
                    Button(saved ? "Cata guardada" : "Guardar cata") { saved = true }
                        .buttonStyle(.borderedProminent)
                        .tint(CupaTheme.forest)
                        .disabled(saved)
                }
                .padding()
            }
        }
        .navigationTitle("Cata")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func sensorySlider(_ title: String, value: Binding<Double>) -> some View {
        VStack(spacing: 8) {
            HStack { Text(title); Spacer(); Text("\(Int(value.wrappedValue))/5").bold() }
            Slider(value: value, in: 1...5, step: 1).tint(CupaTheme.terracotta)
        }
    }
}

struct LabView: View {
    @State private var temperature = 92.0
    @State private var grind = 50.0
    @State private var agitation = 35.0

    var body: some View {
        ZStack {
            CupaTheme.background.ignoresSafeArea()
            ScrollView {
                VStack(spacing: 20) {
                    SectionHeader(eyebrow: "Laboratorio", title: "Explora variables", subtitle: "Cambia una variable a la vez y compara el resultado.")
                    CupaCard {
                        VStack(spacing: 20) {
                            labSlider("Temperatura", value: $temperature, range: 80...100, suffix: "°C")
                            labSlider("Molienda", value: $grind, range: 0...100, suffix: "%")
                            labSlider("Agitación", value: $agitation, range: 0...100, suffix: "%")
                        }
                    }
                    CupaCard {
                        VStack(alignment: .leading, spacing: 10) {
                            Label("Hipótesis", systemImage: "lightbulb")
                                .font(.headline).foregroundStyle(CupaTheme.gold)
                            Text(hypothesis)
                                .foregroundStyle(CupaTheme.secondaryText)
                        }
                    }
                }
                .padding()
            }
        }
        .navigationTitle("Laboratorio")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var hypothesis: String {
        temperature > 94
            ? "Una temperatura alta puede aumentar la extracción. Vigila amargor y astringencia."
            : "Esta temperatura favorece una extracción suave. Prueba una molienda más fina si falta dulzor."
    }

    private func labSlider(_ title: String, value: Binding<Double>, range: ClosedRange<Double>, suffix: String) -> some View {
        VStack(spacing: 8) {
            HStack { Text(title); Spacer(); Text("\(Int(value.wrappedValue))\(suffix)").bold() }
            Slider(value: value, in: range).tint(CupaTheme.forest)
        }
    }
}

struct StorageView: View {
    @Environment(\.managedObjectContext) private var modelContext
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \CoffeeBeanRecord.updatedAt, ascending: false)],
        predicate: NSPredicate(format: "deletedAt == nil"),
        animation: .default
    ) private var beans: FetchedResults<CoffeeBeanRecord>
    @State private var showAddBean = false
    @State private var editingBean: CoffeeBeanRecord?

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
                            Button { editingBean = bean } label: {
                                VStack(alignment: .leading, spacing: 6) {
                                    HStack {
                                        Text(bean.name).font(.headline)
                                        Spacer()
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
        .navigationTitle("Almacén")
        .toolbar {
            Button { showAddBean = true } label: { Image(systemName: "plus") }
        }
        .sheet(isPresented: $showAddBean) {
            CoffeeBeanEditor(record: nil) { draft in
                _ = draft.makeRecord(in: modelContext)
                try? modelContext.save()
            }
        }
        .sheet(item: $editingBean) { bean in
            CoffeeBeanEditor(record: bean) { draft in
                draft.apply(to: bean)
                bean.markUpdated()
                try? modelContext.save()
            }
        }
    }

    private func softDelete(at offsets: IndexSet) {
        for index in offsets { beans[index].markDeleted() }
        try? modelContext.save()
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
    private let onSave: (CoffeeBeanDraft) -> Void
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

    init(record: CoffeeBeanRecord?, onSave: @escaping (CoffeeBeanDraft) -> Void) {
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
                        onSave(CoffeeBeanDraft(
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
                        ))
                        dismiss()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
    }

    private func parseDecimal(_ text: String) -> Double {
        Double(text.replacingOccurrences(of: ",", with: ".")) ?? 0
    }
}
