import CoreData
import SwiftUI

struct RecipeInventoryView: View {
    @Environment(\.managedObjectContext) private var context
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \RecipeRecord.isFavorite, ascending: false), NSSortDescriptor(keyPath: \RecipeRecord.updatedAt, ascending: false)],
        predicate: NSPredicate(format: "deletedAt == nil"), animation: .default
    ) private var recipes: FetchedResults<RecipeRecord>
    @FetchRequest(sortDescriptors: [], predicate: NSPredicate(format: "deletedAt == nil")) private var ingredients: FetchedResults<RecipeIngredientRecord>
    @FetchRequest(sortDescriptors: [], predicate: NSPredicate(format: "deletedAt == nil")) private var recipeSteps: FetchedResults<RecipeStepRecord>
    @State private var search = ""; @State private var kind = "ALL"; @State private var adding = false
    @State private var editing: RecipeRecord?; @State private var importing = false; @State private var importedDraft: RecipeDraftModel?
    @State private var errorMessage: String?

    private var visible: [RecipeRecord] {
        recipes.filter {
            (kind == "ALL" || (kind == "FAVORITES" ? $0.isFavorite : $0.recipeKind == kind)) && matchesSearch($0)
        }
    }

    var body: some View {
        List {
            Section {
                Picker("Tipo", selection: $kind) {
                    Text("Todas").tag("ALL")
                    Text("Favoritas").tag("FAVORITES")
                    ForEach(recipeKinds) { Text($0.label).tag($0.code) }
                }.pickerStyle(.menu)
            }
            if visible.isEmpty {
                ContentUnavailableView("Sin recetas", systemImage: "book.closed", description: Text(search.isEmpty ? "Crea una fórmula con ingredientes y pasos propios." : "No hay resultados para esta búsqueda."))
                    .listRowBackground(Color.clear)
            } else {
                ForEach(visible) { recipe in
                    Button { editing = recipe } label: {
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Image(systemName: recipe.isFavorite ? "star.fill" : "book.pages").foregroundStyle(recipe.isFavorite ? CupaTheme.gold : CupaTheme.forest)
                                Text(recipe.name).font(.headline)
                                Spacer(); if recipe.syncStatus != .synced { syncIndicator }
                            }
                            Text(recipeKindLabel(recipe.recipeKind) + (recipe.suggestedMethodName.isEmpty ? "" : " · \(recipe.suggestedMethodName)"))
                                .font(.subheadline).foregroundStyle(CupaTheme.secondaryText)
                            if !recipe.intention.isEmpty { Text(recipe.intention).font(.caption).lineLimit(2).foregroundStyle(CupaTheme.secondaryText) }
                        }.padding(.vertical, 4)
                    }.buttonStyle(.plain)
                    .swipeActions(edge: .leading) {
                        Button { duplicate(recipe) } label: { Label("Duplicar", systemImage: "plus.square.on.square") }.tint(CupaTheme.forest)
                    }
                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                        Button { toggleFavorite(recipe) } label: { Label(recipe.isFavorite ? "Quitar favorita" : "Favorita", systemImage: recipe.isFavorite ? "star.slash" : "star") }.tint(CupaTheme.gold)
                    }
                }.onDelete(perform: delete)
            }
        }
        .searchable(text: $search, prompt: "Buscar nombre o etiqueta")
        .scrollContentBackground(.hidden)
        .toolbar {
            Button { importedDraft = nil; importing = true } label: { Image(systemName: "wand.and.stars") }.accessibilityLabel("Importar receta desde texto").accessibilityIdentifier("recipes.import")
            Button { importedDraft = nil; adding = true } label: { Image(systemName: "plus") }.accessibilityLabel("Agregar receta").accessibilityIdentifier("recipes.add")
        }
        .sheet(isPresented: $adding) { RecipeEditorView(recipe: nil, initialDraft: importedDraft) }
        .sheet(item: $editing) { RecipeEditorView(recipe: $0) }
        .sheet(isPresented: $importing, onDismiss: {
            if importedDraft != nil { adding = true }
        }) {
            RecipeImporterView { draft in
                importedDraft = draft; importing = false
            }
        }
        .alert("No se pudo guardar", isPresented: Binding(get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })) { Button("Aceptar") {} } message: { Text(errorMessage ?? "Error desconocido") }
    }

    private func duplicate(_ recipe: RecipeRecord) { do { _ = try RecipeTechniqueRepository(context: context).duplicateRecipe(recipe) } catch { errorMessage = error.localizedDescription } }
    private func toggleFavorite(_ recipe: RecipeRecord) { do { try RecipeTechniqueRepository(context: context).toggleFavorite(recipe) } catch { errorMessage = error.localizedDescription } }
    private func delete(_ offsets: IndexSet) { do { for index in offsets { try RecipeTechniqueRepository(context: context).deleteRecipe(visible[index]) } } catch { errorMessage = error.localizedDescription } }
    private func matchesSearch(_ recipe: RecipeRecord) -> Bool {
        guard !search.isEmpty else { return true }
        return recipe.name.localizedCaseInsensitiveContains(search) || recipe.tags.localizedCaseInsensitiveContains(search) ||
            recipe.intention.localizedCaseInsensitiveContains(search) ||
            ingredients.contains { $0.recipeId == recipe.id && $0.name.localizedCaseInsensitiveContains(search) } ||
            recipeSteps.contains { $0.recipeId == recipe.id && $0.instruction.localizedCaseInsensitiveContains(search) }
    }
}

struct TechniqueInventoryView: View {
    @Environment(\.managedObjectContext) private var context
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \TechniqueRecord.updatedAt, ascending: false)], predicate: NSPredicate(format: "deletedAt == nil"), animation: .default)
    private var techniques: FetchedResults<TechniqueRecord>
    @State private var search = ""; @State private var mode = "ALL"; @State private var adding = false
    @State private var editing: TechniqueRecord?; @State private var errorMessage: String?

    private var visible: [TechniqueRecord] { techniques.filter { (mode == "ALL" || $0.executionMode == mode) && (search.isEmpty || $0.name.localizedCaseInsensitiveContains(search) || $0.methodName.localizedCaseInsensitiveContains(search)) } }

    var body: some View {
        List {
            Section { Picker("Ejecución", selection: $mode) { Text("Todas").tag("ALL"); ForEach(executionModes, id: \.0) { Text($0.1).tag($0.0) } }.pickerStyle(.menu) }
            if visible.isEmpty {
                ContentUnavailableView("Sin técnicas", systemImage: "list.number", description: Text("Crea una secuencia ejecutable desde cero o desde una receta."))
                    .listRowBackground(Color.clear)
            } else {
                ForEach(visible) { technique in
                    Button { editing = technique } label: {
                        VStack(alignment: .leading, spacing: 6) {
                            HStack { Text(technique.name).font(.headline); Spacer(); if technique.syncStatus != .synced { syncIndicator } }
                            Text("\(technique.methodName) · 1:\(technique.ratio.formatted(.number.precision(.fractionLength(0...1)))) · \(formatDuration(Int(technique.totalTimeSeconds)))")
                                .font(.subheadline).foregroundStyle(CupaTheme.secondaryText)
                            Text("\(technique.doseGrams.formatted(.number.precision(.fractionLength(0...1)))) g · \(technique.waterMl) ml · \(technique.temperatureC)°C · \(executionModeLabel(technique.executionMode))")
                                .font(.caption).foregroundStyle(CupaTheme.forest)
                        }.padding(.vertical, 4)
                    }.buttonStyle(.plain)
                }.onDelete(perform: delete)
            }
        }
        .searchable(text: $search, prompt: "Buscar técnica o método")
        .scrollContentBackground(.hidden)
        .toolbar { Button { adding = true } label: { Image(systemName: "plus") }.accessibilityLabel("Agregar técnica").accessibilityIdentifier("techniques.add") }
        .sheet(isPresented: $adding) { TechniqueEditorView(technique: nil) }
        .sheet(item: $editing) { TechniqueEditorView(technique: $0) }
        .alert("No se pudo guardar", isPresented: Binding(get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })) { Button("Aceptar") {} } message: { Text(errorMessage ?? "Error desconocido") }
    }
    private func delete(_ offsets: IndexSet) { do { for index in offsets { try RecipeTechniqueRepository(context: context).deleteTechnique(visible[index]) } } catch { errorMessage = error.localizedDescription } }
}

private struct RecipeEditorView: View {
    @Environment(\.dismiss) private var dismiss; @Environment(\.managedObjectContext) private var context
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \EquipmentRecord.name, ascending: true)], predicate: NSPredicate(format: "deletedAt == nil AND equipmentType == 'BREWER_METHOD'")) private var methods: FetchedResults<EquipmentRecord>
    let recipe: RecipeRecord?
    var initialDraft: RecipeDraftModel? = nil
    @State private var draft = RecipeDraftModel(); @State private var loaded = false; @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Form {
                Section("Datos principales") {
                    TextField("Nombre de la receta", text: $draft.name)
                    Picker("Categoría", selection: $draft.recipeKind) { ForEach(recipeKinds) { Text($0.label).tag($0.code) } }
                    TextField("Intención o perfil", text: $draft.intention, axis: .vertical).lineLimit(2...5)
                    Picker("Método sugerido", selection: $draft.suggestedMethodId) {
                        Text("Sin método").tag(Optional<UUID>.none)
                        ForEach(methods) { Text($0.name).tag(Optional($0.id)) }
                    }
                    Toggle("Favorita", isOn: $draft.isFavorite)
                    TextField("Etiquetas separadas por comas", text: $draft.tags)
                }
                Section("Ingredientes") {
                    ForEach($draft.ingredients) { $ingredient in
                        VStack(spacing: 8) {
                            TextField("Ingrediente", text: $ingredient.name)
                            HStack {
                                TextField("Cantidad", value: $ingredient.amount, format: .number).keyboardType(.decimalPad)
                                Picker("Unidad", selection: $ingredient.unit) { ForEach(ingredientUnits, id: \.self) { Text(unitLabel($0)).tag($0) } }.labelsHidden()
                            }
                        }
                    }.onDelete { draft.ingredients.remove(atOffsets: $0) }.onMove { draft.ingredients.move(fromOffsets: $0, toOffset: $1) }
                    Button { draft.ingredients.append(.init()) } label: { Label("Agregar ingrediente", systemImage: "plus") }
                }
                Section("Instrucciones") {
                    ForEach($draft.steps) { $step in
                        VStack(spacing: 8) {
                            TextField("Instrucción", text: $step.instruction, axis: .vertical).lineLimit(2...5)
                            OptionalDurationField(seconds: $step.durationSeconds)
                        }
                    }.onDelete { draft.steps.remove(atOffsets: $0) }.onMove { draft.steps.move(fromOffsets: $0, toOffset: $1) }
                    Button { draft.steps.append(.init()) } label: { Label("Agregar paso", systemImage: "plus") }
                }
            }
            .environment(\.editMode, .constant(.active))
            .navigationTitle(recipe == nil ? "Nueva receta" : "Editar receta")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Guardar", action: save).disabled(!canSave) }
            }
            .onAppear(perform: load)
            .alert("No se pudo guardar", isPresented: Binding(get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })) { Button("Aceptar") {} } message: { Text(errorMessage ?? "") }
        }
    }

    private var canSave: Bool { !draft.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && draft.ingredients.contains { !$0.name.trimmingCharacters(in: .whitespaces).isEmpty } && draft.steps.contains { !$0.instruction.trimmingCharacters(in: .whitespaces).isEmpty } }
    private func load() {
        guard !loaded else { return }; loaded = true
        if let recipe { do { draft = try RecipeTechniqueRepository(context: context).recipeDraft(for: recipe) } catch { errorMessage = error.localizedDescription } }
        else if let initialDraft { draft = initialDraft }
    }
    private func save() {
        if let selected = methods.first(where: { $0.id == draft.suggestedMethodId }) { draft.suggestedMethodName = selected.name }
        do { _ = try RecipeTechniqueRepository(context: context).saveRecipe(draft); dismiss() } catch { errorMessage = error.localizedDescription }
    }
}

private struct RecipeImporterView: View {
    @Environment(\.dismiss) private var dismiss
    let onParsed: (RecipeDraftModel) -> Void
    @State private var rawText = ""

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text("Pega una receta de un blog, mensaje o nota. Cupa detectará localmente el nombre, categoría, método, ingredientes y pasos; podrás corregirlos antes de guardar.")
                        .font(.subheadline).foregroundStyle(CupaTheme.secondaryText)
                    TextEditor(text: $rawText).frame(minHeight: 220).accessibilityIdentifier("recipes.import.text")
                } footer: {
                    Text("Ejemplo: Receta: Espresso tonic · Ingredientes: 30 ml espresso… · Pasos: 1. Servir hielo…")
                }
            }
            .navigationTitle("Importar receta")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Procesar") { onParsed(RecipeTextParser.parse(rawText)) }
                        .disabled(rawText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                        .accessibilityIdentifier("recipes.import.process")
                }
            }
        }
    }
}

private struct TechniqueEditorView: View {
    @Environment(\.dismiss) private var dismiss; @Environment(\.managedObjectContext) private var context
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \RecipeRecord.name, ascending: true)], predicate: NSPredicate(format: "deletedAt == nil")) private var recipes: FetchedResults<RecipeRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \CoffeeBeanRecord.name, ascending: true)], predicate: NSPredicate(format: "deletedAt == nil")) private var beans: FetchedResults<CoffeeBeanRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \GrinderRecord.name, ascending: true)], predicate: NSPredicate(format: "deletedAt == nil")) private var grinders: FetchedResults<GrinderRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \EquipmentRecord.name, ascending: true)], predicate: NSPredicate(format: "deletedAt == nil AND equipmentType == 'BREWER_METHOD'")) private var methods: FetchedResults<EquipmentRecord>
    let technique: TechniqueRecord?
    @State private var draft = TechniqueDraftModel(); @State private var loaded = false; @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Form {
                Section("Técnica") {
                    TextField("Nombre", text: $draft.name)
                    Picker("Método", selection: $draft.methodId) { Text("V60 / genérico").tag(Optional<UUID>.none); ForEach(methods) { Text($0.name).tag(Optional($0.id)) } }
                    Picker("Partir de receta", selection: $draft.recipeId) { Text("Desde cero").tag(Optional<UUID>.none); ForEach(recipes) { Text($0.name).tag(Optional($0.id)) } }
                    if draft.recipeId != nil { Button("Cargar cantidades de la receta", action: importRecipeQuantities) }
                    Picker("Modo", selection: $draft.executionMode) { ForEach(executionModes, id: \.0) { Text($0.1).tag($0.0) } }
                    TextField("Descripción", text: $draft.techniqueDescription, axis: .vertical).lineLimit(2...4)
                }
                Section("Preparación") {
                    HStack { TextField("Café (g)", value: $draft.doseGrams, format: .number).keyboardType(.decimalPad); TextField("Agua (ml)", value: $draft.waterMl, format: .number).keyboardType(.numberPad) }
                    HStack { TextField("Ratio", value: $draft.ratio, format: .number).keyboardType(.decimalPad); TextField("Temperatura °C", value: $draft.temperatureC, format: .number).keyboardType(.numberPad) }
                    Picker("Grano", selection: $draft.beanId) { Text("Sin asignar").tag(Optional<UUID>.none); ForEach(beans) { Text($0.name).tag(Optional($0.id)) } }
                    Picker("Molino", selection: $draft.grinderId) { Text("Sin asignar").tag(Optional<UUID>.none); ForEach(grinders) { Text($0.name).tag(Optional($0.id)) } }
                    HStack { TextField("Valor molienda", value: $draft.grindValue, format: .number).keyboardType(.decimalPad); Picker("Unidad", selection: $draft.grindUnit) { ForEach(grindUnits, id: \.self) { Text($0.capitalized).tag($0) } } }
                    TextField("Descripción de molienda", text: $draft.grindDescription)
                    TextField("Notas", text: $draft.notes, axis: .vertical).lineLimit(2...5)
                }
                Section("Pasos") {
                    ForEach($draft.steps) { $step in TechniqueStepDraftEditor(step: $step) }
                        .onDelete { draft.steps.remove(atOffsets: $0) }.onMove { draft.steps.move(fromOffsets: $0, toOffset: $1) }
                    Button { draft.steps.append(.init()) } label: { Label("Agregar paso", systemImage: "plus") }
                }
            }
            .environment(\.editMode, .constant(.active))
            .navigationTitle(technique == nil ? "Nueva técnica" : "Editar técnica")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Guardar", action: save).disabled(!canSave) }
            }
            .onAppear(perform: load)
            .alert("No se pudo guardar", isPresented: Binding(get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })) { Button("Aceptar") {} } message: { Text(errorMessage ?? "") }
        }
    }

    private var canSave: Bool { !draft.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && draft.doseGrams > 0 && draft.waterMl > 0 && draft.steps.contains { !$0.title.trimmingCharacters(in: .whitespaces).isEmpty } }
    private func load() { guard !loaded else { return }; loaded = true; if let technique { do { draft = try RecipeTechniqueRepository(context: context).techniqueDraft(for: technique) } catch { errorMessage = error.localizedDescription } } }
    private func save() {
        if let method = methods.first(where: { $0.id == draft.methodId }) { draft.methodName = method.name }
        do { _ = try RecipeTechniqueRepository(context: context).saveTechnique(draft); dismiss() } catch { errorMessage = error.localizedDescription }
    }
    private func importRecipeQuantities() {
        guard let recipeId = draft.recipeId else { return }
        do {
            let ingredients = try RecipeTechniqueRepository(context: context).ingredients(recipeId: recipeId)
            if let coffee = ingredients.first(where: { $0.name.localizedCaseInsensitiveContains("café") || $0.name.localizedCaseInsensitiveContains("coffee") }) { draft.doseGrams = coffee.amount }
            if let water = ingredients.first(where: { $0.name.localizedCaseInsensitiveContains("agua") || $0.name.localizedCaseInsensitiveContains("water") }) { draft.waterMl = Int(water.amount) }
            if draft.doseGrams > 0 { draft.ratio = Double(draft.waterMl) / draft.doseGrams }
        } catch { errorMessage = error.localizedDescription }
    }
}

private struct TechniqueStepDraftEditor: View {
    @Binding var step: TechniqueStepDraft
    var body: some View {
        DisclosureGroup(step.title.isEmpty ? "Nuevo paso" : step.title) {
            TextField("Título", text: $step.title)
            HStack { TextField("Duración (s)", value: $step.durationSeconds, format: .number).keyboardType(.numberPad); TextField("Agua agregada (ml)", value: $step.waterAddedMl, format: .number).keyboardType(.numberPad) }
            Picker("Gesto", selection: $step.gesture) { ForEach(gestures, id: \.self) { Text($0.replacingOccurrences(of: "_", with: " ").capitalized).tag($0) } }
            Picker("Intensidad", selection: $step.intensity) { Text("Baja").tag("LOW"); Text("Media").tag("MEDIUM"); Text("Alta").tag("HIGH") }
            HStack { OptionalDoubleField(title: "Cobertura %", value: $step.coverage); OptionalDoubleField(title: "Flujo ml/s", value: $step.flow) }
            TextField("Acción secundaria", text: Binding(get: { step.secondaryAction ?? "" }, set: { step.secondaryAction = $0.isEmpty ? nil : $0 }))
            TextField("Notas del paso", text: $step.note, axis: .vertical).lineLimit(2...4)
        }
    }
}

private struct OptionalDurationField: View {
    @Binding var seconds: Int?
    var body: some View { TextField("Duración opcional (s)", text: Binding(get: { seconds.map(String.init) ?? "" }, set: { seconds = Int($0) })).keyboardType(.numberPad) }
}
private struct OptionalDoubleField: View {
    let title: String; @Binding var value: Double?
    var body: some View { TextField(title, text: Binding(get: { value.map { String(format: "%.1f", $0) } ?? "" }, set: { value = Double($0.replacingOccurrences(of: ",", with: ".")) })).keyboardType(.decimalPad) }
}

private struct CodeLabel: Identifiable { let code: String; let label: String; var id: String { code } }
private let recipeKinds = [CodeLabel(code: "BLACK_COFFEE", label: "Café negro"), CodeLabel(code: "MILK_DRINK", label: "Leche"), CodeLabel(code: "COLD_DRINK", label: "Bebida fría"), CodeLabel(code: "SIGNATURE", label: "Autor"), CodeLabel(code: "DESSERT", label: "Postre"), CodeLabel(code: "OTHER", label: "Otro")]
private let ingredientUnits = ["GRAMS", "MILLILITERS", "UNITS", "TEASPOONS", "TABLESPOONS", "OUNCES", "OTHER"]
private let executionModes = [("GUIDED", "Guiada"), ("MANUAL", "Manual"), ("TIMER_ONLY", "Sólo cronómetro"), ("AUTOMATED", "Automática")]
private let grindUnits = ["CLICKS", "MICRONS", "SETTING_NUMERIC", "DESCRIPTIVE"]
private let gestures = ["BLOOM", "CIRCULAR_POUR", "CENTER_POUR", "SWIRL", "STIR", "PRESS", "WAIT"]
private func recipeKindLabel(_ code: String) -> String { recipeKinds.first { $0.code == code }?.label ?? code }
private func unitLabel(_ code: String) -> String { ["GRAMS": "g", "MILLILITERS": "ml", "UNITS": "u", "TEASPOONS": "cdta", "TABLESPOONS": "cda", "OUNCES": "oz", "OTHER": "otra"][code] ?? code }
private func executionModeLabel(_ code: String) -> String { executionModes.first { $0.0 == code }?.1 ?? code }
private func formatDuration(_ seconds: Int) -> String { String(format: "%d:%02d", seconds / 60, seconds % 60) }
private var syncIndicator: some View { Image(systemName: "arrow.triangle.2.circlepath").font(.caption).foregroundStyle(CupaTheme.gold).accessibilityLabel("Pendiente de sincronización") }
