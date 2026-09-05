import CoreData
import SwiftUI

struct RecipeInventoryView: View {
    @Environment(\.managedObjectContext) private var context
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \RecipeRecord.isFavorite, ascending: false), NSSortDescriptor(keyPath: \RecipeRecord.updatedAt, ascending: false)],
        predicate: LocalDataScope.visiblePredicate(), animation: .default
    ) private var recipes: FetchedResults<RecipeRecord>
    @FetchRequest(sortDescriptors: [], predicate: LocalDataScope.visiblePredicate()) private var ingredients: FetchedResults<RecipeIngredientRecord>
    @FetchRequest(sortDescriptors: [], predicate: LocalDataScope.visiblePredicate()) private var recipeSteps: FetchedResults<RecipeStepRecord>
    @State private var search = ""; @State private var kind = "ALL"; @State private var adding = false
    @State private var selectedRecipe: RecipeRecord?; @State private var editing: RecipeRecord?; @State private var pendingEdit: RecipeRecord?
    @State private var importing = false; @State private var importedDraft: RecipeDraftModel?
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
                    Button { selectedRecipe = recipe } label: {
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
                }
            }
        }
        .searchable(text: $search, prompt: "Buscar en recetario")
        .brewScrollableCanvas()
        .toolbar {
            Button { importedDraft = nil; importing = true } label: { Image(systemName: "wand.and.stars") }.accessibilityLabel("Importar receta desde texto").accessibilityIdentifier("recipes.import")
            Button { importedDraft = nil; adding = true } label: { Image(systemName: "plus") }.accessibilityLabel("Agregar receta").accessibilityIdentifier("recipes.add")
        }
        .sheet(isPresented: $adding) { RecipeEditorView(recipe: nil, initialDraft: importedDraft) }
        .sheet(item: $editing) { RecipeEditorView(recipe: $0) }
        .sheet(item: $selectedRecipe, onDismiss: {
            if let pendingEdit { editing = pendingEdit; self.pendingEdit = nil }
        }) { recipe in
            RecipeDetailView(
                recipe: recipe,
                ingredients: ingredients.filter { $0.recipeId == recipe.id },
                steps: recipeSteps.filter { $0.recipeId == recipe.id },
                onFavorite: { toggleFavorite(recipe) },
                onDuplicate: { duplicate(recipe); selectedRecipe = nil },
                onEdit: { pendingEdit = recipe; selectedRecipe = nil },
                onDelete: { delete(recipe); selectedRecipe = nil }
            )
        }
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
    private func delete(_ recipe: RecipeRecord) { do { try RecipeTechniqueRepository(context: context).deleteRecipe(recipe) } catch { errorMessage = error.localizedDescription } }
    private func matchesSearch(_ recipe: RecipeRecord) -> Bool {
        guard !search.isEmpty else { return true }
        return recipe.name.localizedCaseInsensitiveContains(search) || recipe.tags.localizedCaseInsensitiveContains(search) ||
            recipe.intention.localizedCaseInsensitiveContains(search) ||
            ingredients.contains { $0.recipeId == recipe.id && $0.name.localizedCaseInsensitiveContains(search) } ||
            recipeSteps.contains { $0.recipeId == recipe.id && $0.instruction.localizedCaseInsensitiveContains(search) }
    }
}

private struct RecipeDetailView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var recipe: RecipeRecord
    let ingredients: [RecipeIngredientRecord]
    let steps: [RecipeStepRecord]
    let onFavorite: () -> Void
    let onDuplicate: () -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void
    @State private var confirmingDelete = false

    var body: some View {
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 10) {
                        HStack(spacing: 10) {
                            Image(systemName: recipe.isFavorite ? "star.fill" : "book.pages")
                                .font(.title2).foregroundStyle(recipe.isFavorite ? CupaTheme.gold : CupaTheme.forest)
                            VStack(alignment: .leading, spacing: 3) {
                                Text(recipe.name).font(.title3.bold())
                                Text(recipeKindLabel(recipe.recipeKind) + (recipe.suggestedMethodName.isEmpty ? "" : " · \(recipe.suggestedMethodName)"))
                                    .font(.subheadline).foregroundStyle(CupaTheme.secondaryText)
                            }
                        }
                        if !recipe.intention.isEmpty {
                            Text("“\(recipe.intention)”").font(.body.italic()).foregroundStyle(CupaTheme.secondaryText)
                        }
                        if !recipe.tags.isEmpty { Label(recipe.tags, systemImage: "tag").font(.caption).foregroundStyle(CupaTheme.forest) }
                    }.padding(.vertical, 4)
                }

                Section("Ingredientes") {
                    ForEach(ingredients) { ingredient in
                        HStack {
                            Text(ingredient.name)
                            Spacer()
                            Text(quantity(ingredient)).foregroundStyle(CupaTheme.forest).fontWeight(.semibold)
                        }
                    }
                }

                Section("Pasos de preparación") {
                    ForEach(Array(steps.enumerated()), id: \.element.id) { index, step in
                        HStack(alignment: .top, spacing: 12) {
                            Text("\(index + 1)").font(.caption.bold()).foregroundStyle(CupaTheme.onAccent)
                                .frame(minWidth: 28, minHeight: 28).background(CupaTheme.forest, in: Circle())
                            VStack(alignment: .leading, spacing: 3) {
                                Text(step.instruction)
                                if let seconds = step.durationSeconds {
                                    Label(formatDuration(seconds), systemImage: "timer").font(.caption).foregroundStyle(CupaTheme.secondaryText)
                                }
                            }
                        }.padding(.vertical, 3)
                    }
                }

                Section {
                    Button(action: onFavorite) { Label(recipe.isFavorite ? "Quitar de favoritas" : "Marcar como favorita", systemImage: recipe.isFavorite ? "star.slash" : "star") }
                    Button(action: onDuplicate) { Label("Duplicar receta", systemImage: "plus.square.on.square") }
                    Button(role: .destructive) { confirmingDelete = true } label: { Label("Eliminar receta", systemImage: "trash") }
                }
            }
            .brewScrollableCanvas()
            .navigationTitle("Detalle de receta")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cerrar") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Editar", action: onEdit).accessibilityIdentifier("recipes.detail.edit") }
            }
            .confirmationDialog("¿Eliminar esta receta?", isPresented: $confirmingDelete, titleVisibility: .visible) {
                Button("Eliminar receta", role: .destructive, action: onDelete)
                Button("Cancelar", role: .cancel) {}
            } message: {
                Text("Se quitará del recetario; el historial ya guardado conservará sus datos.")
            }
        }
    }

    private func quantity(_ ingredient: RecipeIngredientRecord) -> String {
        "\(ingredient.amount.formatted(.number.precision(.fractionLength(0...2)))) \(unitLabel(ingredient.unit))"
    }
}

struct TechniqueInventoryView: View {
    @Environment(\.managedObjectContext) private var context
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \TechniqueRecord.updatedAt, ascending: false)], predicate: LocalDataScope.visiblePredicate(), animation: .default)
    private var techniques: FetchedResults<TechniqueRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \TechniqueStepRecord.stepNumber, ascending: true)], predicate: LocalDataScope.visiblePredicate())
    private var techniqueSteps: FetchedResults<TechniqueStepRecord>
    @FetchRequest(sortDescriptors: [], predicate: LocalDataScope.visiblePredicate()) private var recipes: FetchedResults<RecipeRecord>
    @FetchRequest(sortDescriptors: [], predicate: LocalDataScope.visiblePredicate()) private var beans: FetchedResults<CoffeeBeanRecord>
    @FetchRequest(sortDescriptors: [], predicate: LocalDataScope.visiblePredicate()) private var grinders: FetchedResults<GrinderRecord>
    @Binding var selection: CupaTab
    @ObservedObject var preparation: PreparationModel
    @State private var search = ""; @State private var mode = "ALL"; @State private var adding = false
    @State private var selectedTechnique: TechniqueRecord?; @State private var editing: TechniqueRecord?; @State private var pendingEdit: TechniqueRecord?
    @State private var errorMessage: String?

    private var visible: [TechniqueRecord] { techniques.filter { (mode == "ALL" || $0.executionMode == mode) && (search.isEmpty || $0.name.localizedCaseInsensitiveContains(search) || $0.methodName.localizedCaseInsensitiveContains(search)) } }

    var body: some View {
        List {
            Section { Picker("Ejecución", selection: $mode) { Text("Todas").tag("ALL"); ForEach(executionModes, id: \.0) { Text($0.1).tag($0.0) } }.pickerStyle(.menu) }
            if visible.isEmpty {
                ContentUnavailableView("Sin técnicas", systemImage: "list.number", description: Text("Crea una secuencia ejecutable desde cero o desde una receta."))
                    .listRowBackground(Color.clear)
            } else {
                ForEach(visible) { technique in
                    Button { selectedTechnique = technique } label: {
                        VStack(alignment: .leading, spacing: 6) {
                            HStack { Text(technique.name).font(.headline); Spacer(); if technique.syncStatus != .synced { syncIndicator } }
                            Text("\(technique.methodName) · 1:\(technique.ratio.formatted(.number.precision(.fractionLength(0...1)))) · \(formatDuration(Int(technique.totalTimeSeconds)))")
                                .font(.subheadline).foregroundStyle(CupaTheme.secondaryText)
                            Text("\(technique.doseGrams.formatted(.number.precision(.fractionLength(0...1)))) g · \(technique.waterMl) ml · \(technique.temperatureC)°C · \(executionModeLabel(technique.executionMode))")
                                .font(.caption).foregroundStyle(CupaTheme.forest)
                        }.padding(.vertical, 4)
                    }.buttonStyle(.plain)
                }
            }
        }
        .searchable(text: $search, prompt: "Buscar técnica o método")
        .brewScrollableCanvas()
        .toolbar { Button { adding = true } label: { Image(systemName: "plus") }.accessibilityLabel("Agregar técnica").accessibilityIdentifier("techniques.add") }
        .sheet(isPresented: $adding) { TechniqueEditorView(technique: nil) }
        .sheet(item: $editing) { TechniqueEditorView(technique: $0) }
        .sheet(item: $selectedTechnique, onDismiss: {
            if let pendingEdit { editing = pendingEdit; self.pendingEdit = nil }
        }) { technique in
            TechniqueDetailView(
                technique: technique,
                steps: techniqueSteps.filter { $0.techniqueId == technique.id },
                recipeName: recipes.first { $0.id == technique.recipeId }?.name,
                beanName: beans.first { $0.id == technique.beanId }?.name,
                grinderName: grinders.first { $0.id == technique.grinderId }?.name,
                onPrepare: { prepare(technique); selectedTechnique = nil },
                onEdit: { pendingEdit = technique; selectedTechnique = nil },
                onDelete: { delete(technique); selectedTechnique = nil }
            )
        }
        .alert("No se pudo guardar", isPresented: Binding(get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })) { Button("Aceptar") {} } message: { Text(errorMessage ?? "Error desconocido") }
    }
    private func prepare(_ technique: TechniqueRecord) {
        do {
            preparation.load(technique: technique, steps: try RecipeTechniqueRepository(context: context).techniqueSteps(techniqueId: technique.id))
            selection = .brew
        } catch { errorMessage = error.localizedDescription }
    }
    private func delete(_ technique: TechniqueRecord) { do { try RecipeTechniqueRepository(context: context).deleteTechnique(technique) } catch { errorMessage = error.localizedDescription } }
}

private struct TechniqueDetailView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var technique: TechniqueRecord
    let steps: [TechniqueStepRecord]
    let recipeName: String?
    let beanName: String?
    let grinderName: String?
    let onPrepare: () -> Void
    let onEdit: () -> Void
    let onDelete: () -> Void
    @State private var confirmingDelete = false

    var body: some View {
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 10) {
                        HStack(spacing: 12) {
                            Image(systemName: "list.number").font(.title2).foregroundStyle(CupaTheme.forest)
                            VStack(alignment: .leading, spacing: 3) {
                                Text(technique.name).font(.title3.bold())
                                Text("\(technique.methodName) · \(executionModeLabel(technique.executionMode))")
                                    .font(.subheadline).foregroundStyle(CupaTheme.secondaryText)
                            }
                        }
                        if !technique.techniqueDescription.isEmpty { Text(technique.techniqueDescription).foregroundStyle(CupaTheme.secondaryText) }
                    }.padding(.vertical, 4)
                }

                Section("Parámetros") {
                    detailRow("Café", "\(technique.doseGrams.formatted(.number.precision(.fractionLength(0...1)))) g")
                    detailRow("Agua", "\(technique.waterMl) ml")
                    detailRow("Proporción", "1:\(technique.ratio.formatted(.number.precision(.fractionLength(0...1))))")
                    detailRow("Temperatura", "\(technique.temperatureC) °C")
                    detailRow("Duración", formatDuration(Int(technique.totalTimeSeconds)))
                    detailRow("Molienda", technique.grindDescription.isEmpty ? "\(technique.grindValue.formatted(.number.precision(.fractionLength(0...1)))) \(technique.grindUnit.lowercased())" : technique.grindDescription)
                }

                if recipeName != nil || beanName != nil || grinderName != nil {
                    Section("Inventario vinculado") {
                        if let recipeName { detailRow("Receta", recipeName) }
                        if let beanName { detailRow("Café", beanName) }
                        if let grinderName { detailRow("Molino", grinderName) }
                    }
                }

                Section("Secuencia") {
                    ForEach(steps) { step in
                        HStack(alignment: .top, spacing: 12) {
                            Text("\(step.stepNumber)").font(.caption.bold()).foregroundStyle(CupaTheme.onAccent)
                                .frame(minWidth: 28, minHeight: 28).background(CupaTheme.forest, in: Circle())
                            VStack(alignment: .leading, spacing: 5) {
                                Text(step.title).fontWeight(.semibold)
                                HStack(spacing: 12) {
                                    Label(formatDuration(Int(step.durationSeconds)), systemImage: "timer")
                                    Label("\(step.waterAddedMl) ml · \(step.waterAccumulatedMl) total", systemImage: "drop")
                                }.font(.caption).foregroundStyle(CupaTheme.secondaryText)
                                Text("\(gestureLabel(step.gesture)) · \(step.intensity.capitalized)")
                                    .font(.caption.bold()).foregroundStyle(CupaTheme.forest)
                                if step.coverage != nil || step.flow != nil {
                                    HStack(spacing: 12) {
                                        if let coverage = step.coverage { Label("\(coverage.formatted(.number.precision(.fractionLength(0...1))))%", systemImage: "circle.dotted") }
                                        if let flow = step.flow { Label("\(flow.formatted(.number.precision(.fractionLength(0...1)))) ml/s", systemImage: "water.waves") }
                                    }.font(.caption).foregroundStyle(CupaTheme.secondaryText)
                                }
                                if !step.stepNote.isEmpty { Text(step.stepNote).font(.caption).foregroundStyle(CupaTheme.secondaryText) }
                                if let secondary = step.secondaryAction, !secondary.isEmpty {
                                    Label(secondary, systemImage: "arrow.triangle.branch").font(.caption).foregroundStyle(CupaTheme.secondaryText)
                                }
                            }
                        }.padding(.vertical, 4)
                    }
                }

                if !technique.notes.isEmpty { Section("Notas") { Text(technique.notes) } }

                Section {
                    Button(action: onPrepare) { Label("Preparar con esta técnica", systemImage: "play.fill") }
                        .accessibilityIdentifier("techniques.detail.prepare")
                    Button(role: .destructive) { confirmingDelete = true } label: { Label("Eliminar técnica", systemImage: "trash") }
                }
            }
            .brewScrollableCanvas()
            .navigationTitle("Detalle de técnica")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cerrar") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) { Button("Editar", action: onEdit).accessibilityIdentifier("techniques.detail.edit") }
            }
            .confirmationDialog("¿Eliminar esta técnica?", isPresented: $confirmingDelete, titleVisibility: .visible) {
                Button("Eliminar técnica", role: .destructive, action: onDelete)
                Button("Cancelar", role: .cancel) {}
            } message: {
                Text("Se quitará de la biblioteca; las preparaciones guardadas conservarán sus snapshots.")
            }
        }
    }

    private func detailRow(_ title: String, _ value: String) -> some View {
        HStack { Text(title); Spacer(); Text(value).foregroundStyle(CupaTheme.forest).fontWeight(.semibold) }
    }

    private func gestureLabel(_ code: String) -> String {
        code.replacingOccurrences(of: "_", with: " ").capitalized
    }
}

private struct RecipeEditorView: View {
    @Environment(\.dismiss) private var dismiss; @Environment(\.managedObjectContext) private var context
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \EquipmentRecord.name, ascending: true)], predicate: LocalDataScope.visiblePredicate(additional: NSPredicate(format: "equipmentType == 'BREWER_METHOD'"))) private var methods: FetchedResults<EquipmentRecord>
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
            .brewScrollableCanvas()
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
            .brewScrollableCanvas()
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
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \RecipeRecord.name, ascending: true)], predicate: LocalDataScope.visiblePredicate()) private var recipes: FetchedResults<RecipeRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \CoffeeBeanRecord.name, ascending: true)], predicate: LocalDataScope.visiblePredicate()) private var beans: FetchedResults<CoffeeBeanRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \GrinderRecord.name, ascending: true)], predicate: LocalDataScope.visiblePredicate()) private var grinders: FetchedResults<GrinderRecord>
    @FetchRequest(sortDescriptors: [NSSortDescriptor(keyPath: \EquipmentRecord.name, ascending: true)], predicate: LocalDataScope.visiblePredicate(additional: NSPredicate(format: "equipmentType == 'BREWER_METHOD'"))) private var methods: FetchedResults<EquipmentRecord>
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
            .brewScrollableCanvas()
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
