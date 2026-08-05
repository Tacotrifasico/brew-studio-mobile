package com.example.data.engine

import java.util.UUID

data class RecipeIngredientInput(
    val name: String = "",
    val amount: String = "",
    val unit: String = "G",
    val id: String = UUID.randomUUID().toString()
)

data class RecipeStepInput(
    val instruction: String = "",
    val id: String = UUID.randomUUID().toString()
)

data class RecipeDraft(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val recipeKind: String = "BLACK_COFFEE", // BLACK_COFFEE, MILK_DRINK, COLD_DRINK, SIGNATURE, DESSERT, OTHER
    val intention: String = "",
    val suggestedMethod: String = "",
    val ingredients: List<RecipeIngredientInput> = emptyList(),
    val steps: List<RecipeStepInput> = emptyList(),
    val tags: String = "",
    val isFavorite: Boolean = false
)

object RecipeTextParser {

    fun parse(rawText: String): RecipeDraft {
        if (rawText.isBlank()) return RecipeDraft()

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return RecipeDraft()

        var name = ""
        var recipeKind = "BLACK_COFFEE"
        var intention = ""
        var suggestedMethod = ""
        val ingredients = mutableListOf<RecipeIngredientInput>()
        val steps = mutableListOf<RecipeStepInput>()

        var currentSection = "" // "INGREDIENTS", "STEPS", "NOTES"

        lines.forEachIndexed { index, line ->
            val lower = line.lowercase()

            when {
                // Header/Name detection
                index == 0 && (lower.startsWith("receta:") || lower.startsWith("nombre:")) -> {
                    name = line.substringAfter(":").trim()
                }
                index == 0 -> {
                    name = line.removePrefix("#").removePrefix("*").trim()
                }
                lower.contains("ingrediente") -> {
                    currentSection = "INGREDIENTS"
                }
                lower.contains("paso") || lower.contains("preparación") || lower.contains("instruccion") || lower.contains("pasos") -> {
                    currentSection = "STEPS"
                }
                lower.contains("nota") || lower.contains("intencion") || lower.contains("perfil") || lower.contains("descriptor") -> {
                    currentSection = "NOTES"
                }
                else -> {
                    when (currentSection) {
                        "INGREDIENTS" -> {
                            val parsedIng = parseIngredientLine(line)
                            if (parsedIng != null) ingredients.add(parsedIng)
                        }
                        "STEPS" -> {
                            val stepText = line.replace(Regex("""^\d+[\.\)-]\s*"""), "").trim()
                            if (stepText.isNotBlank()) {
                                steps.add(RecipeStepInput(stepText))
                            }
                        }
                        "NOTES" -> {
                            if (intention.isBlank()) intention = line
                            else intention += " $line"
                        }
                        else -> {
                            // Heuristic detection when no explicit section headers are used
                            if (line.matches(Regex("""^\d+[\.\)-]\s*.*"""))) {
                                val stepText = line.replace(Regex("""^\d+[\.\)-]\s*"""), "").trim()
                                if (stepText.isNotBlank()) steps.add(RecipeStepInput(stepText))
                            } else {
                                val parsedIng = parseIngredientLine(line)
                                if (parsedIng != null) {
                                    ingredients.add(parsedIng)
                                } else if (name.isNotBlank() && index == 1 && intention.isBlank()) {
                                    intention = line
                                }
                            }
                        }
                    }
                }
            }
        }

        // Infer RecipeKind based on full text keywords
        val fullTextLower = rawText.lowercase()
        recipeKind = when {
            fullTextLower.contains("leche") || fullTextLower.contains("latte") || fullTextLower.contains("cappuccino") || fullTextLower.contains("flat white") || fullTextLower.contains("macchiato") -> "MILK_DRINK"
            fullTextLower.contains("hielo") || fullTextLower.contains("cold brew") || fullTextLower.contains("tonic") || fullTextLower.contains("fresco") || fullTextLower.contains("fría") || fullTextLower.contains("frio") -> "COLD_DRINK"
            fullTextLower.contains("postre") || fullTextLower.contains("affogato") || fullTextLower.contains("helado") || fullTextLower.contains("dulce") || fullTextLower.contains("chocolate") -> "DESSERT"
            fullTextLower.contains("autor") || fullTextLower.contains("signature") || fullTextLower.contains("jarabe") || fullTextLower.contains("syrup") || fullTextLower.contains("coctel") -> "SIGNATURE"
            fullTextLower.contains("v60") || fullTextLower.contains("espresso") || fullTextLower.contains("filtrado") || fullTextLower.contains("aeropress") || fullTextLower.contains("chemex") -> "BLACK_COFFEE"
            else -> "OTHER"
        }

        // Infer Suggested Method
        suggestedMethod = when {
            fullTextLower.contains("v60") -> "V60"
            fullTextLower.contains("aeropress") -> "Aeropress"
            fullTextLower.contains("espresso") -> "Espresso"
            fullTextLower.contains("prensa francesa") || fullTextLower.contains("french press") -> "Prensa Francesa"
            fullTextLower.contains("chemex") -> "Chemex"
            fullTextLower.contains("kalita") -> "Kalita Wave"
            else -> ""
        }

        if (ingredients.isEmpty()) {
            ingredients.add(RecipeIngredientInput(name = "Café de especialidad", amount = "15", unit = "G"))
            ingredients.add(RecipeIngredientInput(name = "Agua filtrada", amount = "240", unit = "ML"))
        }

        if (steps.isEmpty()) {
            steps.add(RecipeStepInput(instruction = "Mezclar los ingredientes y servir"))
        }

        return RecipeDraft(
            name = name.ifBlank { "Receta Importada" },
            recipeKind = recipeKind,
            intention = intention,
            suggestedMethod = suggestedMethod,
            ingredients = ingredients,
            steps = steps
        )
    }

    private fun parseIngredientLine(line: String): RecipeIngredientInput? {
        val clean = line.removePrefix("-").removePrefix("*").removePrefix("•").trim()
        if (clean.isBlank()) return null

        val parsed = IngredientSuggestionEngine.parseIngredientsSummary(clean).firstOrNull()
        return if (parsed != null && parsed.name.isNotBlank()) {
            RecipeIngredientInput(
                name = parsed.name,
                amount = parsed.amount,
                unit = if (parsed.unit.isNotBlank()) parsed.unit else "G"
            )
        } else {
            RecipeIngredientInput(name = clean, amount = "", unit = "G")
        }
    }
}
