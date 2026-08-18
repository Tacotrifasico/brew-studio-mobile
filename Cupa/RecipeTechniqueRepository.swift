import CoreData
import Foundation

struct RecipeIngredientDraft: Identifiable, Equatable {
    var id = UUID(); var name = ""; var amount = 0.0; var unit = "GRAMS"
}

struct RecipeStepDraft: Identifiable, Equatable {
    var id = UUID(); var instruction = ""; var durationSeconds: Int?
}

struct RecipeDraftModel: Equatable {
    var id = UUID(); var name = ""; var recipeKind = "BLACK_COFFEE"; var intention = ""
    var suggestedMethodId: UUID?; var suggestedMethodName = ""; var isFavorite = false; var tags = ""
    var ingredients: [RecipeIngredientDraft] = []; var steps: [RecipeStepDraft] = []
}

struct TechniqueStepDraft: Identifiable, Equatable {
    var id = UUID(); var title = ""; var durationSeconds = 30; var waterAddedMl = 0
    var intensity = "MEDIUM"; var gesture = "CIRCULAR_POUR"; var note = ""
    var coverage: Double?; var flow: Double?; var secondaryAction: String?
}

struct TechniqueDraftModel: Equatable {
    var id = UUID(); var name = ""; var methodId: UUID?; var methodName = "V60"
    var recipeId: UUID?; var beanId: UUID?; var grinderId: UUID?
    var doseGrams = 15.0; var waterMl = 240; var ratio = 16.0; var temperatureC = 93
    var executionMode = "GUIDED"; var grindValue = 18.0; var grindDescription = "18 Clicks"; var grindUnit = "CLICKS"
    var notes = ""; var techniqueDescription = ""; var steps: [TechniqueStepDraft] = []
}

@MainActor
final class RecipeTechniqueRepository {
    private let context: NSManagedObjectContext
    init(context: NSManagedObjectContext) { self.context = context }

    func recipeDraft(for recipe: RecipeRecord) throws -> RecipeDraftModel {
        RecipeDraftModel(
            id: recipe.id, name: recipe.name, recipeKind: recipe.recipeKind, intention: recipe.intention,
            suggestedMethodId: recipe.suggestedMethodId, suggestedMethodName: recipe.suggestedMethodName,
            isFavorite: recipe.isFavorite, tags: recipe.tags,
            ingredients: try ingredients(recipeId: recipe.id).map { .init(id: $0.id, name: $0.name, amount: $0.amount, unit: $0.unit) },
            steps: try recipeSteps(recipeId: recipe.id).map { .init(id: $0.id, instruction: $0.instruction, durationSeconds: $0.durationSeconds) }
        )
    }

    @discardableResult func saveRecipe(_ draft: RecipeDraftModel) throws -> RecipeRecord {
        let recipe = try recipe(id: draft.id) ?? RecipeRecord(
            context: context, id: draft.id, name: draft.name, recipeKind: draft.recipeKind,
            intention: draft.intention, suggestedMethodId: draft.suggestedMethodId,
            suggestedMethodName: draft.suggestedMethodName, isFavorite: draft.isFavorite, tags: draft.tags
        )
        if !recipe.isInserted {
            recipe.name = draft.name; recipe.recipeKind = draft.recipeKind; recipe.intention = draft.intention
            recipe.suggestedMethodId = draft.suggestedMethodId; recipe.suggestedMethodName = draft.suggestedMethodName
            recipe.isFavorite = draft.isFavorite; recipe.tags = draft.tags; recipe.markUpdated()
        }
        try reconcileIngredients(draft.ingredients, recipeId: recipe.id)
        try reconcileRecipeSteps(draft.steps, recipeId: recipe.id)
        try saveContext()
        return recipe
    }

    @discardableResult func duplicateRecipe(_ source: RecipeRecord) throws -> RecipeRecord {
        var draft = try recipeDraft(for: source)
        let sourceId = source.id; draft.id = UUID(); draft.name = "Copia de \(source.name)"
        draft.ingredients = draft.ingredients.map { var item = $0; item.id = UUID(); return item }
        draft.steps = draft.steps.map { var item = $0; item.id = UUID(); return item }
        let copy = RecipeRecord(
            context: context, id: draft.id, name: draft.name, recipeKind: draft.recipeKind, intention: draft.intention,
            suggestedMethodId: draft.suggestedMethodId, suggestedMethodName: draft.suggestedMethodName,
            isFavorite: draft.isFavorite, tags: draft.tags, originalEntityId: sourceId,
            rootEntityId: source.rootEntityId ?? sourceId, copyMode: "FORK"
        )
        try reconcileIngredients(draft.ingredients, recipeId: copy.id)
        try reconcileRecipeSteps(draft.steps, recipeId: copy.id)
        try saveContext(); return copy
    }

    func deleteRecipe(_ recipe: RecipeRecord) throws {
        recipe.markDeleted()
        try ingredients(recipeId: recipe.id).forEach { $0.markDeleted() }
        try recipeSteps(recipeId: recipe.id).forEach { $0.markDeleted() }
        try saveContext()
    }

    func techniqueDraft(for technique: TechniqueRecord) throws -> TechniqueDraftModel {
        TechniqueDraftModel(
            id: technique.id, name: technique.name, methodId: technique.methodId, methodName: technique.methodName,
            recipeId: technique.recipeId, beanId: technique.beanId, grinderId: technique.grinderId,
            doseGrams: technique.doseGrams, waterMl: Int(technique.waterMl), ratio: technique.ratio,
            temperatureC: Int(technique.temperatureC), executionMode: technique.executionMode,
            grindValue: technique.grindValue, grindDescription: technique.grindDescription, grindUnit: technique.grindUnit,
            notes: technique.notes, techniqueDescription: technique.techniqueDescription,
            steps: try techniqueSteps(techniqueId: technique.id).map {
                .init(id: $0.id, title: $0.title, durationSeconds: Int($0.durationSeconds), waterAddedMl: Int($0.waterAddedMl), intensity: $0.intensity, gesture: $0.gesture, note: $0.stepNote, coverage: $0.coverage, flow: $0.flow, secondaryAction: $0.secondaryAction)
            }
        )
    }

    @discardableResult func saveTechnique(_ draft: TechniqueDraftModel) throws -> TechniqueRecord {
        let totalTime = draft.steps.reduce(0) { $0 + max(0, $1.durationSeconds) }
        let technique = try technique(id: draft.id) ?? TechniqueRecord(
            context: context, id: draft.id, name: draft.name, methodId: draft.methodId, methodName: draft.methodName,
            recipeId: draft.recipeId, beanId: draft.beanId, grinderId: draft.grinderId,
            doseGrams: draft.doseGrams, waterMl: draft.waterMl, ratio: draft.ratio, temperatureC: draft.temperatureC,
            executionMode: draft.executionMode, grindValue: draft.grindValue, grindDescription: draft.grindDescription,
            grindUnit: draft.grindUnit, notes: draft.notes, techniqueDescription: draft.techniqueDescription, totalTimeSeconds: totalTime
        )
        if !technique.isInserted {
            technique.name = draft.name; technique.methodId = draft.methodId; technique.methodName = draft.methodName
            technique.recipeId = draft.recipeId; technique.beanId = draft.beanId; technique.grinderId = draft.grinderId
            technique.doseGrams = draft.doseGrams; technique.waterMl = Int64(draft.waterMl); technique.ratio = draft.ratio
            technique.temperatureC = Int64(draft.temperatureC); technique.executionMode = draft.executionMode
            technique.grindValue = draft.grindValue; technique.grindDescription = draft.grindDescription; technique.grindUnit = draft.grindUnit
            technique.notes = draft.notes; technique.techniqueDescription = draft.techniqueDescription; technique.totalTimeSeconds = Int64(totalTime)
            technique.markUpdated()
        }
        try reconcileTechniqueSteps(draft.steps, techniqueId: technique.id)
        try saveContext(); return technique
    }

    func deleteTechnique(_ technique: TechniqueRecord) throws {
        technique.markDeleted(); try techniqueSteps(techniqueId: technique.id).forEach { $0.markDeleted() }; try saveContext()
    }

    func ingredients(recipeId: UUID) throws -> [RecipeIngredientRecord] {
        try fetch(RecipeIngredientRecord.self, entity: "RecipeIngredientRecord", predicate: NSPredicate(format: "recipeId == %@ AND deletedAt == nil", recipeId as CVarArg), sortKey: "orderIndex")
    }
    func recipeSteps(recipeId: UUID) throws -> [RecipeStepRecord] {
        try fetch(RecipeStepRecord.self, entity: "RecipeStepRecord", predicate: NSPredicate(format: "recipeId == %@ AND deletedAt == nil", recipeId as CVarArg), sortKey: "stepNumber")
    }
    func techniqueSteps(techniqueId: UUID) throws -> [TechniqueStepRecord] {
        try fetch(TechniqueStepRecord.self, entity: "TechniqueStepRecord", predicate: NSPredicate(format: "techniqueId == %@ AND deletedAt == nil", techniqueId as CVarArg), sortKey: "stepNumber")
    }

    private func recipe(id: UUID) throws -> RecipeRecord? { try object(RecipeRecord.self, entity: "RecipeRecord", id: id) }
    private func technique(id: UUID) throws -> TechniqueRecord? { try object(TechniqueRecord.self, entity: "TechniqueRecord", id: id) }

    private func reconcileIngredients(_ drafts: [RecipeIngredientDraft], recipeId: UUID) throws {
        let current = try ingredients(recipeId: recipeId); let incoming = Set(drafts.map(\.id))
        current.filter { !incoming.contains($0.id) }.forEach { $0.markDeleted() }
        for (index, draft) in drafts.enumerated() where !draft.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            if let item = current.first(where: { $0.id == draft.id }) {
                item.name = draft.name; item.amount = draft.amount; item.unit = draft.unit; item.orderIndex = Int64(index); item.markUpdated()
            } else { _ = RecipeIngredientRecord(context: context, id: draft.id, recipeId: recipeId, name: draft.name, amount: draft.amount, unit: draft.unit, orderIndex: index) }
        }
    }

    private func reconcileRecipeSteps(_ drafts: [RecipeStepDraft], recipeId: UUID) throws {
        let current = try recipeSteps(recipeId: recipeId); let incoming = Set(drafts.map(\.id))
        current.filter { !incoming.contains($0.id) }.forEach { $0.markDeleted() }
        for (index, draft) in drafts.enumerated() where !draft.instruction.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            if let item = current.first(where: { $0.id == draft.id }) {
                item.instruction = draft.instruction; item.stepNumber = Int64(index + 1); item.durationSeconds = draft.durationSeconds; item.markUpdated()
            } else { _ = RecipeStepRecord(context: context, id: draft.id, recipeId: recipeId, instruction: draft.instruction, stepNumber: index + 1, durationSeconds: draft.durationSeconds) }
        }
    }

    private func reconcileTechniqueSteps(_ drafts: [TechniqueStepDraft], techniqueId: UUID) throws {
        let current = try techniqueSteps(techniqueId: techniqueId); let incoming = Set(drafts.map(\.id)); var accumulated = 0
        current.filter { !incoming.contains($0.id) }.forEach { $0.markDeleted() }
        for (index, draft) in drafts.enumerated() where !draft.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            accumulated += max(0, draft.waterAddedMl)
            if let item = current.first(where: { $0.id == draft.id }) {
                item.stepNumber = Int64(index + 1); item.title = draft.title; item.durationSeconds = Int64(max(0, draft.durationSeconds))
                item.waterAddedMl = Int64(max(0, draft.waterAddedMl)); item.waterAccumulatedMl = Int64(accumulated)
                item.intensity = draft.intensity; item.gesture = draft.gesture; item.stepNote = draft.note
                item.coverage = draft.coverage; item.flow = draft.flow; item.secondaryAction = draft.secondaryAction; item.markUpdated()
            } else {
                _ = TechniqueStepRecord(context: context, id: draft.id, techniqueId: techniqueId, stepNumber: index + 1, title: draft.title, durationSeconds: max(0, draft.durationSeconds), waterAddedMl: max(0, draft.waterAddedMl), waterAccumulatedMl: accumulated, intensity: draft.intensity, gesture: draft.gesture, stepNote: draft.note, coverage: draft.coverage, flow: draft.flow, secondaryAction: draft.secondaryAction)
            }
        }
    }

    private func object<T: NSManagedObject>(_ type: T.Type, entity: String, id: UUID) throws -> T? {
        let request = NSFetchRequest<T>(entityName: entity); request.predicate = NSPredicate(format: "id == %@", id as CVarArg); request.fetchLimit = 1
        return try context.fetch(request).first
    }
    private func fetch<T: NSManagedObject>(_ type: T.Type, entity: String, predicate: NSPredicate, sortKey: String) throws -> [T] {
        let request = NSFetchRequest<T>(entityName: entity); request.predicate = predicate; request.sortDescriptors = [NSSortDescriptor(key: sortKey, ascending: true)]
        return try context.fetch(request)
    }
    private func saveContext() throws { if context.hasChanges { try context.save() } }
}
