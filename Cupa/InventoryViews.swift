import CoreData
import SwiftUI

struct GrinderInventoryView: View {
    @Environment(\.managedObjectContext) private var context
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \GrinderRecord.updatedAt, ascending: false)],
        predicate: LocalDataScope.visiblePredicate(), animation: .default
    ) private var grinders: FetchedResults<GrinderRecord>
    @State private var adding = false
    @State private var editing: GrinderRecord?
    @State private var selected: GrinderRecord?
    @State private var pendingEdit: GrinderRecord?
    @State private var errorMessage: String?

    var body: some View {
        List {
            if grinders.isEmpty {
                ContentUnavailableView("Sin molinos", systemImage: "gearshape.2", description: Text("Registra un molino y su rango real de calibración."))
                    .listRowBackground(Color.clear)
            } else {
                ForEach(grinders) { grinder in
                    Button { selected = grinder } label: {
                        VStack(alignment: .leading, spacing: 5) {
                            HStack {
                                Text(grinder.name).font(.headline)
                                Spacer()
                                inventorySyncBadge(grinder.syncStatus)
                            }
                            Text([grinder.brand, grinder.model].filter { !$0.isEmpty }.joined(separator: " · "))
                                .font(.subheadline).foregroundStyle(CupaTheme.secondaryText)
                            Text("\(grinder.minimumSetting)–\(grinder.maximumSetting) \(grinder.scaleUnit.lowercased()) · \(grinder.grinderType.capitalized)")
                                .font(.caption).foregroundStyle(CupaTheme.forestText)
                        }.padding(.vertical, 5)
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("grinders.row.\(grinder.id.uuidString)")
                }
            }
        }
        .brewScrollableCanvas()
        .toolbar {
            Button { adding = true } label: { Image(systemName: "plus") }
                .accessibilityLabel("Agregar molino")
                .accessibilityIdentifier("grinders.add")
        }
        .sheet(isPresented: $adding) {
            GrinderEditor(record: nil) { draft in _ = draft.insert(in: context); return save() }
        }
        .sheet(item: $editing) { record in
            GrinderEditor(record: record) { draft in draft.apply(to: record); record.markUpdated(); return save() }
        }
        .sheet(item: $selected, onDismiss: {
            if let pendingEdit { editing = pendingEdit; self.pendingEdit = nil }
        }) { record in
            GrinderDetailView(
                grinder: record,
                onEdit: { pendingEdit = record; selected = nil },
                onDelete: { if delete(record) { selected = nil } }
            )
        }
        .alert("No se pudo guardar el molino", isPresented: Binding(get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })) {
            Button("Aceptar") {}
        } message: { Text(errorMessage ?? "") }
    }

    private func delete(_ grinder: GrinderRecord) -> Bool { grinder.markDeleted(); return save() }
    @discardableResult private func save() -> Bool {
        do { try context.save(); return true }
        catch { context.rollback(); errorMessage = error.localizedDescription; return false }
    }
}

struct EquipmentInventoryView: View {
    @Environment(\.managedObjectContext) private var context
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \EquipmentRecord.updatedAt, ascending: false)],
        predicate: LocalDataScope.visiblePredicate(), animation: .default
    ) private var equipment: FetchedResults<EquipmentRecord>
    @State private var adding = false
    @State private var editing: EquipmentRecord?
    @State private var selected: EquipmentRecord?
    @State private var pendingEdit: EquipmentRecord?
    @State private var errorMessage: String?

    var body: some View {
        List {
            if equipment.isEmpty {
                ContentUnavailableView("Sin equipos", systemImage: "wrench.and.screwdriver", description: Text("Agrega métodos, teteras, básculas y accesorios del taller."))
                    .listRowBackground(Color.clear)
            } else {
                ForEach(equipment) { item in
                    Button { selected = item } label: {
                        VStack(alignment: .leading, spacing: 5) {
                            HStack {
                                Image(systemName: item.isFavorite ? "star.fill" : equipmentIcon(item.equipmentType))
                                    .foregroundStyle(item.isFavorite ? CupaTheme.goldText : CupaTheme.forestText)
                                Text(item.name).font(.headline)
                                Spacer()
                                inventorySyncBadge(item.syncStatus)
                            }
                            Text(equipmentTypeLabel(item.equipmentType) + (item.capacityMl.map { " · \($0) ml" } ?? ""))
                                .font(.subheadline).foregroundStyle(CupaTheme.secondaryText)
                            if !item.configuration.isEmpty { Text(item.configuration).font(.caption).foregroundStyle(CupaTheme.forestText) }
                        }.padding(.vertical, 5).opacity(item.isActive ? 1 : 0.55)
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("equipment.row.\(item.id.uuidString)")
                    .swipeActions(edge: .leading, allowsFullSwipe: false) {
                        if item.isBrewingMethod {
                            Button { toggleCalculatorPin(item) } label: {
                                Label(item.isFavorite ? "Quitar de calculadora" : "Mostrar en calculadora", systemImage: item.isFavorite ? "pin.slash" : "pin")
                            }
                            .tint(CupaTheme.gold)
                        }
                    }
                }
            }
        }
        .brewScrollableCanvas()
        .toolbar {
            Button { adding = true } label: { Image(systemName: "plus") }
                .accessibilityLabel("Agregar equipo")
                .accessibilityIdentifier("equipment.add")
        }
        .sheet(isPresented: $adding) {
            EquipmentEditor(record: nil) { draft in _ = draft.insert(in: context); return save() }
        }
        .sheet(item: $editing) { record in
            EquipmentEditor(record: record) { draft in draft.apply(to: record); record.markUpdated(); return save() }
        }
        .sheet(item: $selected, onDismiss: {
            if let pendingEdit { editing = pendingEdit; self.pendingEdit = nil }
        }) { record in
            EquipmentDetailView(
                equipment: record,
                onEdit: { pendingEdit = record; selected = nil },
                onDelete: { if delete(record) { selected = nil } }
            )
        }
        .alert("No se pudo guardar el equipo", isPresented: Binding(get: { errorMessage != nil }, set: { if !$0 { errorMessage = nil } })) {
            Button("Aceptar") {}
        } message: { Text(errorMessage ?? "") }
    }

    private func delete(_ item: EquipmentRecord) -> Bool { item.markDeleted(); return save() }
    private func toggleCalculatorPin(_ item: EquipmentRecord) { item.isFavorite.toggle(); item.markUpdated(); save() }
    @discardableResult private func save() -> Bool {
        do { try context.save(); return true }
        catch { context.rollback(); errorMessage = error.localizedDescription; return false }
    }
}

private struct GrinderDraft {
    var name: String; var brand: String; var model: String; var type: String; var unit: String
    var minimum: Int; var maximum: Int; var calibration: String; var notes: String

    func insert(in context: NSManagedObjectContext) -> GrinderRecord {
        GrinderRecord(context: context, name: resolvedName, brand: brand, model: model, grinderType: type, scaleUnit: unit, minimumSetting: minimum, maximumSetting: maximum, calibrationNotes: calibration, notes: notes)
    }
    func apply(to record: GrinderRecord) {
        record.name = resolvedName; record.brand = brand; record.model = model; record.grinderType = type; record.scaleUnit = unit
        record.minimumSetting = Int64(minimum); record.maximumSetting = Int64(maximum); record.calibrationNotes = calibration; record.notes = notes
    }
    private var resolvedName: String { name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? [brand, model].filter { !$0.isEmpty }.joined(separator: " ") : name }
}

private struct GrinderEditorFormDraft: Codable, Equatable {
    var recordId: UUID?
    var name: String
    var brand: String
    var model: String
    var type: String
    var unit: String
    var minimum: Int
    var maximum: Int
    var calibration: String
    var notes: String

    init(record: GrinderRecord?) {
        recordId = record?.id
        name = record?.name ?? ""
        brand = record?.brand ?? ""
        model = record?.model ?? ""
        type = record?.grinderType ?? "MANUAL"
        unit = record?.scaleUnit ?? "CLICKS"
        minimum = Int(record?.minimumSetting ?? 0)
        maximum = Int(record?.maximumSetting ?? 40)
        calibration = record?.calibrationNotes ?? ""
        notes = record?.notes ?? ""
    }
}

private struct GrinderEditor: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.managedObjectContext) private var context
    let record: GrinderRecord?
    let onSave: (GrinderDraft) -> Bool
    @State private var draft: GrinderEditorFormDraft
    @State private var loaded = false
    @State private var isSaving = false
    @SceneStorage("cupa.grinderEditorDraft.v1") private var storedDraft: Data?

    init(record: GrinderRecord?, onSave: @escaping (GrinderDraft) -> Bool) {
        self.record = record; self.onSave = onSave
        _draft = State(initialValue: GrinderEditorFormDraft(record: record))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Identidad") {
                    TextField("Nombre visible", text: $draft.name); TextField("Marca", text: $draft.brand); TextField("Modelo", text: $draft.model)
                    Picker("Tipo", selection: $draft.type) { Text("Manual").tag("MANUAL"); Text("Eléctrico").tag("ELECTRIC") }
                }
                Section("Escala") {
                    Picker("Unidad", selection: $draft.unit) { ForEach(["CLICKS", "MICRONS", "SETTING_NUMERIC", "DESCRIPTIVE"], id: \.self) { Text(grinderScaleLabel($0)).tag($0) } }
                    Stepper("Mínimo: \(draft.minimum)", value: $draft.minimum, in: 0...10_000)
                    Stepper("Máximo: \(draft.maximum)", value: $draft.maximum, in: draft.minimum...10_000)
                    TextField("Notas de calibración", text: $draft.calibration, axis: .vertical).lineLimit(2...5)
                    TextField("Notas", text: $draft.notes, axis: .vertical).lineLimit(2...5)
                }
            }
            .brewScrollableCanvas()
            .onAppear(perform: load)
            .onChange(of: draft.minimum) { _, newValue in draft.maximum = max(draft.maximum, newValue) }
            .onChange(of: draft) { _, value in
                guard loaded else { return }
                storedDraft = storingEditorDraft(value, ownerId: context.activeOwnerId, in: storedDraft)
            }
            .navigationTitle(record == nil ? "Agregar molino" : "Editar molino")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { clearStoredDraft(); dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isSaving ? "Guardando…" : "Guardar") {
                        guard !isSaving else { return }
                        isSaving = true
                        let saved = onSave(GrinderDraft(
                            name: draft.name.trimmingCharacters(in: .whitespacesAndNewlines),
                            brand: draft.brand.trimmingCharacters(in: .whitespacesAndNewlines),
                            model: draft.model.trimmingCharacters(in: .whitespacesAndNewlines),
                            type: draft.type,
                            unit: draft.unit,
                            minimum: draft.minimum,
                            maximum: max(draft.minimum, draft.maximum),
                            calibration: draft.calibration.trimmingCharacters(in: .whitespacesAndNewlines),
                            notes: draft.notes.trimmingCharacters(in: .whitespacesAndNewlines)
                        ))
                        if saved { clearStoredDraft(); dismiss() } else { isSaving = false }
                    }
                        .disabled((draft.model.trimmingCharacters(in: .whitespaces).isEmpty && draft.name.trimmingCharacters(in: .whitespaces).isEmpty) || isSaving)
                }
            }
        }
    }

    private func load() {
        guard !loaded else { return }
        loaded = true
        if let restored: GrinderEditorFormDraft = scopedEditorDraft(from: storedDraft, ownerId: context.activeOwnerId),
           restored.recordId == record?.id {
            draft = restored
        }
    }

    private func clearStoredDraft() {
        storedDraft = removingEditorDraft(ownerId: context.activeOwnerId, from: storedDraft, as: GrinderEditorFormDraft.self)
    }
}

private struct GrinderDetailView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var grinder: GrinderRecord
    let onEdit: () -> Void
    let onDelete: () -> Void
    @State private var confirmingDelete = false

    var body: some View {
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 6) {
                        Label(grinder.name, systemImage: grinder.grinderType == "ELECTRIC" ? "bolt.fill" : "gearshape.2.fill")
                            .font(.title3.bold()).foregroundStyle(CupaTheme.forestText)
                        if !identity.isEmpty { Text(identity).font(.subheadline).foregroundStyle(CupaTheme.secondaryText) }
                    }.padding(.vertical, 4)
                }
                Section("Especificaciones") {
                    inventoryDetailRow("Tipo", grinder.grinderType == "ELECTRIC" ? "Eléctrico" : "Manual")
                    inventoryDetailRow("Escala", "\(grinder.minimumSetting)–\(grinder.maximumSetting) \(grinderScaleLabel(grinder.scaleUnit))")
                    if !grinder.calibrationNotes.isEmpty { inventoryDetailRow("Calibración", grinder.calibrationNotes) }
                    if !grinder.notes.isEmpty { inventoryDetailRow("Notas", grinder.notes) }
                }
                Section("Registro") {
                    inventoryDetailRow("Creado", grinder.createdAt.formatted(date: .abbreviated, time: .shortened))
                    inventoryDetailRow("Actualizado", grinder.updatedAt.formatted(date: .abbreviated, time: .shortened))
                    inventoryDetailRow("Sincronización", inventorySyncStatusLabel(grinder.syncStatus))
                }
                Section {
                    Button(role: .destructive) { confirmingDelete = true } label: { Label("Eliminar molino", systemImage: "trash") }
                }
            }
            .brewScrollableCanvas()
            .navigationTitle("Detalle del molino")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cerrar") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Editar", action: onEdit).accessibilityIdentifier("grinders.detail.edit")
                }
            }
            .confirmationDialog("¿Eliminar este molino?", isPresented: $confirmingDelete, titleVisibility: .visible) {
                Button("Eliminar molino", role: .destructive, action: onDelete)
                Button("Cancelar", role: .cancel) {}
            } message: {
                Text("Se retirará del inventario y de futuras selecciones. Las preparaciones guardadas conservarán su nombre y ajuste históricos.")
            }
        }
    }

    private var identity: String { [grinder.brand, grinder.model].filter { !$0.isEmpty }.joined(separator: " · ") }
}

private struct EquipmentDraft {
    var name: String; var type: String; var brand: String; var model: String; var capacity: Int?
    var configuration: String; var notes: String; var favorite: Bool; var active: Bool
    func insert(in context: NSManagedObjectContext) -> EquipmentRecord { EquipmentRecord(context: context, name: name, equipmentType: type, brand: brand, model: model, capacityMl: capacity, configuration: configuration, notes: notes, isFavorite: favorite, isActive: active) }
    func apply(to record: EquipmentRecord) { record.name = name; record.equipmentType = type; record.brand = brand; record.model = model; record.capacityMl = capacity; record.configuration = configuration; record.notes = notes; record.isFavorite = favorite; record.isActive = active }
}

private struct EquipmentEditorFormDraft: Codable, Equatable {
    var recordId: UUID?
    var name: String
    var type: String
    var brand: String
    var model: String
    var capacity: String
    var configuration: String
    var notes: String
    var favorite: Bool
    var active: Bool

    init(record: EquipmentRecord?) {
        recordId = record?.id
        name = record?.name ?? ""
        type = record?.equipmentType ?? "BREWER_METHOD"
        brand = record?.brand ?? ""
        model = record?.model ?? ""
        capacity = record?.capacityMl.map(String.init) ?? ""
        configuration = record?.configuration ?? ""
        notes = record?.notes ?? ""
        favorite = record?.isFavorite ?? true
        active = record?.isActive ?? true
    }
}

private struct EquipmentEditor: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.managedObjectContext) private var context
    let record: EquipmentRecord?; let onSave: (EquipmentDraft) -> Bool
    @State private var draft: EquipmentEditorFormDraft
    @State private var loaded = false
    @State private var isSaving = false
    @SceneStorage("cupa.equipmentEditorDraft.v1") private var storedDraft: Data?

    init(record: EquipmentRecord?, onSave: @escaping (EquipmentDraft) -> Bool) {
        self.record = record; self.onSave = onSave
        _draft = State(initialValue: EquipmentEditorFormDraft(record: record))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Equipo") {
                    TextField("Nombre o descripción", text: $draft.name)
                    Picker("Categoría", selection: $draft.type) { ForEach(equipmentTypes) { Text($0.label).tag($0.code) } }
                    TextField("Marca", text: $draft.brand); TextField("Modelo", text: $draft.model)
                    TextField("Capacidad (ml)", text: $draft.capacity).keyboardType(.numberPad)
                    if !capacityIsValid { Text("Escribe una capacidad entera mayor que cero.").font(.caption).foregroundStyle(.red) }
                }
                Section("Configuración") {
                    TextField("Configuración o especificaciones", text: $draft.configuration, axis: .vertical).lineLimit(2...5)
                    TextField("Notas", text: $draft.notes, axis: .vertical).lineLimit(2...5)
                    Toggle(draft.type == "BREWER_METHOD" ? "Mostrar en calculadora" : "Favorito", isOn: $draft.favorite)
                    Toggle("Equipo activo", isOn: $draft.active)
                }
            }
            .brewScrollableCanvas()
            .onAppear(perform: load)
            .onChange(of: draft.type) { oldValue, newValue in
                if oldValue == "BREWER_METHOD" || newValue == "BREWER_METHOD" { draft.favorite = newValue == "BREWER_METHOD" }
            }
            .onChange(of: draft) { _, value in
                guard loaded else { return }
                storedDraft = storingEditorDraft(value, ownerId: context.activeOwnerId, in: storedDraft)
            }
            .navigationTitle(record == nil ? "Agregar equipo" : "Editar equipo")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { clearStoredDraft(); dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isSaving ? "Guardando…" : "Guardar") {
                        guard !isSaving else { return }
                        isSaving = true
                        let saved = onSave(EquipmentDraft(
                            name: draft.name.trimmingCharacters(in: .whitespacesAndNewlines),
                            type: draft.type,
                            brand: draft.brand.trimmingCharacters(in: .whitespacesAndNewlines),
                            model: draft.model.trimmingCharacters(in: .whitespacesAndNewlines),
                            capacity: parsedCapacity,
                            configuration: draft.configuration.trimmingCharacters(in: .whitespacesAndNewlines),
                            notes: draft.notes.trimmingCharacters(in: .whitespacesAndNewlines),
                            favorite: draft.favorite,
                            active: draft.active
                        ))
                        if saved { clearStoredDraft(); dismiss() } else { isSaving = false }
                    }
                        .disabled(draft.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || !capacityIsValid || isSaving)
                }
            }
        }
    }

    private var normalizedCapacity: String { draft.capacity.trimmingCharacters(in: .whitespacesAndNewlines) }
    private var parsedCapacity: Int? { normalizedCapacity.isEmpty ? nil : Int(normalizedCapacity) }
    private var capacityIsValid: Bool { normalizedCapacity.isEmpty || (parsedCapacity ?? 0) > 0 }

    private func load() {
        guard !loaded else { return }
        loaded = true
        if let restored: EquipmentEditorFormDraft = scopedEditorDraft(from: storedDraft, ownerId: context.activeOwnerId),
           restored.recordId == record?.id {
            draft = restored
        }
    }

    private func clearStoredDraft() {
        storedDraft = removingEditorDraft(ownerId: context.activeOwnerId, from: storedDraft, as: EquipmentEditorFormDraft.self)
    }
}

private struct EquipmentDetailView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var equipment: EquipmentRecord
    let onEdit: () -> Void
    let onDelete: () -> Void
    @State private var confirmingDelete = false

    var body: some View {
        NavigationStack {
            List {
                Section {
                    VStack(alignment: .leading, spacing: 6) {
                        Label(equipment.name, systemImage: equipmentIcon(equipment.equipmentType))
                            .font(.title3.bold()).foregroundStyle(CupaTheme.forestText)
                        Text(equipmentTypeLabel(equipment.equipmentType)).font(.subheadline).foregroundStyle(CupaTheme.secondaryText)
                    }.padding(.vertical, 4)
                }
                Section("Especificaciones") {
                    if !equipment.brand.isEmpty { inventoryDetailRow("Marca", equipment.brand) }
                    if !equipment.model.isEmpty { inventoryDetailRow("Modelo", equipment.model) }
                    if let capacity = equipment.capacityMl { inventoryDetailRow("Capacidad", "\(capacity) ml") }
                    if !equipment.configuration.isEmpty { inventoryDetailRow("Configuración", equipment.configuration) }
                    if !equipment.notes.isEmpty { inventoryDetailRow("Notas", equipment.notes) }
                }
                Section("Disponibilidad") {
                    inventoryDetailRow(equipment.isBrewingMethod ? "Calculadora" : "Favorito", equipment.isFavorite ? "Sí" : "No")
                    inventoryDetailRow("Equipo activo", equipment.isActive ? "Sí" : "No")
                    inventoryDetailRow("Sincronización", inventorySyncStatusLabel(equipment.syncStatus))
                }
                Section {
                    Button(role: .destructive) { confirmingDelete = true } label: { Label("Eliminar equipo", systemImage: "trash") }
                }
            }
            .brewScrollableCanvas()
            .navigationTitle("Detalle del equipo")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cerrar") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Editar", action: onEdit).accessibilityIdentifier("equipment.detail.edit")
                }
            }
            .confirmationDialog("¿Eliminar este equipo?", isPresented: $confirmingDelete, titleVisibility: .visible) {
                Button("Eliminar equipo", role: .destructive, action: onDelete)
                Button("Cancelar", role: .cancel) {}
            } message: {
                Text("Se retirará del inventario y de la calculadora. Las preparaciones guardadas conservarán sus referencias y nombres históricos.")
            }
        }
    }
}

private struct EquipmentTypeOption: Identifiable {
    let code: String; let label: String
    var id: String { code }
}
private let equipmentTypes = [
    EquipmentTypeOption(code: "BREWER_METHOD", label: "Método"), EquipmentTypeOption(code: "KETTLE", label: "Tetera"),
    EquipmentTypeOption(code: "SCALE", label: "Báscula"), EquipmentTypeOption(code: "FILTERS", label: "Filtros"),
    EquipmentTypeOption(code: "SERVER", label: "Servidor"), EquipmentTypeOption(code: "PRESS", label: "Prensa"),
    EquipmentTypeOption(code: "ACCESSORY", label: "Accesorio"), EquipmentTypeOption(code: "OTHER", label: "Otro")
]

private func equipmentTypeLabel(_ code: String) -> String { equipmentTypes.first { $0.code == code }?.label ?? code.capitalized }
private func grinderScaleLabel(_ code: String) -> String {
    switch code { case "CLICKS": "clics"; case "MICRONS": "micras"; case "SETTING_NUMERIC": "niveles"; default: "descriptiva" }
}
func inventorySyncStatusLabel(_ status: SyncStatus) -> String {
    switch status {
    case .synced: "Sincronizado"
    case .pendingCreate: "Guardado local"
    case .pendingUpdate: "Cambios pendientes"
    case .pendingDelete: "Eliminación pendiente"
    case .conflict: "Conflicto · reintentar"
    case .error: "Reintento pendiente"
    }
}
@ViewBuilder private func inventoryDetailRow(_ title: String, _ value: String) -> some View {
    HStack(alignment: .top) {
        Text(title)
        Spacer()
        Text(value).multilineTextAlignment(.trailing).fontWeight(.semibold).foregroundStyle(CupaTheme.forestText)
    }
}
private func equipmentIcon(_ code: String) -> String {
    switch code { case "KETTLE": "kettle"; case "SCALE": "scalemass"; case "BREWER_METHOD": "mug"; default: "wrench.and.screwdriver" }
}
@ViewBuilder func inventorySyncBadge(_ status: SyncStatus) -> some View {
    if status != .synced {
        let needsAttention = status == .error || status == .conflict
        Label(
            inventorySyncStatusLabel(status),
            systemImage: needsAttention ? "exclamationmark.arrow.triangle.2.circlepath" : "icloud.and.arrow.up"
        )
        .font(.caption2.bold())
        .foregroundStyle(needsAttention ? CupaTheme.terracottaText : CupaTheme.goldText)
        .padding(.horizontal, 7)
        .padding(.vertical, 4)
        .background(CupaTheme.backgroundAlt, in: Capsule())
        .accessibilityLabel(inventorySyncStatusLabel(status))
    }
}
