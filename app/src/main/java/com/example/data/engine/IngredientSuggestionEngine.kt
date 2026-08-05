package com.example.data.engine

import com.example.data.database.Bean
import com.example.data.database.Recipe
import com.example.data.database.RecipeIngredient

data class IngredientSuggestion(
    val name: String,
    val defaultUnit: String, // "G", "ML", "UNIT", "TSP", "TBSP", "OZ", "OTHER"
    val usageCount: Int,
    val isFromStorageBean: Boolean = false,
    val beanDetails: String? = null
)

data class ParsedIngredient(
    val name: String,
    val amount: String = "",
    val unit: String = "",
    val displayQuantity: String = ""
)

object IngredientSuggestionEngine {

    fun buildIndex(
        recipes: List<Recipe>,
        savedIngredients: List<RecipeIngredient> = emptyList(),
        beans: List<Bean> = emptyList()
    ): List<IngredientSuggestion> {
        val ingredientUsage = mutableMapOf<String, Int>()
        val ingredientUnits = mutableMapOf<String, MutableMap<String, Int>>()
        val displayNameMap = mutableMapOf<String, String>()

        // 1. Process structured database RecipeIngredients
        savedIngredients.forEach { ing ->
            val cleanName = ing.name.trim()
            if (cleanName.isNotBlank()) {
                val key = cleanName.lowercase()
                displayNameMap.putIfAbsent(key, cleanName)
                ingredientUsage[key] = (ingredientUsage[key] ?: 0) + 1
                val unitMap = ingredientUnits.getOrPut(key) { mutableMapOf() }
                val stdUnit = normalizeUnit(ing.unit)
                unitMap[stdUnit] = (unitMap[stdUnit] ?: 0) + 1
            }
        }

        // 2. Process ingredientsSummary string from saved recipes
        recipes.forEach { recipe ->
            parseIngredientsSummary(recipe.ingredientsSummary).forEach { ing ->
                val cleanName = ing.name.trim()
                if (cleanName.isNotBlank()) {
                    val key = cleanName.lowercase()
                    displayNameMap.putIfAbsent(key, cleanName)
                    ingredientUsage[key] = (ingredientUsage[key] ?: 0) + 1
                    if (ing.unit.isNotBlank()) {
                        val unitMap = ingredientUnits.getOrPut(key) { mutableMapOf() }
                        val stdUnit = normalizeUnit(ing.unit)
                        unitMap[stdUnit] = (unitMap[stdUnit] ?: 0) + 1
                    }
                }
            }
        }

        // 3. Assemble indexed suggestions map
        val suggestionsMap = mutableMapOf<String, IngredientSuggestion>()

        ingredientUsage.forEach { (key, count) ->
            val unitMap = ingredientUnits[key]
            val mostFrequentUnit = unitMap?.maxByOrNull { it.value }?.key ?: inferUnitFromName(key)
            val displayName = displayNameMap[key] ?: key.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

            suggestionsMap[key] = IngredientSuggestion(
                name = displayName,
                defaultUnit = mostFrequentUnit,
                usageCount = count,
                isFromStorageBean = false
            )
        }

        // 4. Index Beans from Almacén (Storage) with high priority
        beans.forEach { bean ->
            val beanFullName = if (bean.roaster.isNotBlank()) "${bean.roaster} ${bean.name}".trim() else bean.name.trim()
            if (beanFullName.isNotBlank()) {
                val key = beanFullName.lowercase()
                val existing = suggestionsMap[key]
                val details = listOfNotNull(bean.origin.takeIf { it.isNotBlank() }, bean.process.takeIf { it.isNotBlank() }).joinToString(" · ")
                
                suggestionsMap[key] = IngredientSuggestion(
                    name = beanFullName,
                    defaultUnit = "G",
                    usageCount = (existing?.usageCount ?: 0) + 100, // boost usage for active inventory
                    isFromStorageBean = true,
                    beanDetails = details.ifBlank { "De tu almacén" }
                )
            }
        }

        return suggestionsMap.values.toList()
    }

    fun getSuggestions(
        query: String,
        index: List<IngredientSuggestion>,
        limit: Int = 5
    ): List<IngredientSuggestion> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isBlank()) return emptyList()

        return index
            .filter { it.name.lowercase().contains(trimmed) }
            .sortedWith(
                compareByDescending<IngredientSuggestion> { it.isFromStorageBean }
                    .thenByDescending { it.name.lowercase().startsWith(trimmed) }
                    .thenByDescending { it.usageCount }
                    .thenBy { it.name.length }
            )
            .take(limit)
    }

    fun parseIngredientsSummary(summary: String): List<ParsedIngredient> {
        if (summary.isBlank()) return emptyList()
        return summary.split(",").mapNotNull { part ->
            val clean = part.trim()
            if (clean.isBlank()) null
            else parseSingleIngredient(clean)
        }
    }

    private fun parseSingleIngredient(clean: String): ParsedIngredient {
        // Pattern 1: Name (Amount Unit) -> e.g., "Café (30 ML)" or "Canela (al gusto)"
        val parensRegex = Regex("""^(.+?)\s*\((.*?)\)$""")
        val parensMatch = parensRegex.find(clean)
        if (parensMatch != null) {
            val ingName = parensMatch.groupValues[1].trim()
            val inside = parensMatch.groupValues[2].trim()

            val amtUnitRegex = Regex("""^(\d+(?:[\.,]\d+)?|\d+/\d+)?\s*([a-zA-ZáéíóúÁÉÍÓÚ%]+)?$""")
            val insideMatch = amtUnitRegex.find(inside)
            return if (insideMatch != null && (insideMatch.groupValues[1].isNotBlank() || insideMatch.groupValues[2].isNotBlank())) {
                val amt = insideMatch.groupValues[1].trim()
                val rawUnit = insideMatch.groupValues[2].trim()
                val unit = normalizeUnit(rawUnit)
                val disp = listOf(amt, rawUnit.ifBlank { unit }).filter { it.isNotBlank() }.joinToString(" ")
                ParsedIngredient(name = ingName, amount = amt, unit = unit, displayQuantity = disp)
            } else {
                ParsedIngredient(name = ingName, amount = inside, unit = "", displayQuantity = inside)
            }
        }

        // Pattern 2: "30 ML de Café" or "30 g Café"
        val startAmtRegex = Regex("""^(\d+(?:[\.,]\d+)?|\d+/\d+)\s*([a-zA-ZáéíóúÁÉÍÓÚ%]+)?\s+(?:de\s+)?(.+)$""")
        val startMatch = startAmtRegex.find(clean)
        if (startMatch != null) {
            val amt = startMatch.groupValues[1].trim()
            val rawUnit = startMatch.groupValues[2].trim()
            val ingName = startMatch.groupValues[3].trim()
            val unit = normalizeUnit(rawUnit)
            val disp = listOf(amt, rawUnit.ifBlank { unit }).filter { it.isNotBlank() }.joinToString(" ")
            return ParsedIngredient(name = ingName, amount = amt, unit = unit, displayQuantity = disp)
        }

        // Pattern 3: "Café 30 ML" or "Café 15g"
        val endAmtRegex = Regex("""^(.+?)\s+(\d+(?:[\.,]\d+)?|\d+/\d+)\s*([a-zA-ZáéíóúÁÉÍÓÚ%]+)?$""")
        val endMatch = endAmtRegex.find(clean)
        if (endMatch != null) {
            val ingName = endMatch.groupValues[1].trim()
            val amt = endMatch.groupValues[2].trim()
            val rawUnit = endMatch.groupValues[3].trim()
            val unit = normalizeUnit(rawUnit)
            val disp = listOf(amt, rawUnit.ifBlank { unit }).filter { it.isNotBlank() }.joinToString(" ")
            return ParsedIngredient(name = ingName, amount = amt, unit = unit, displayQuantity = disp)
        }

        // Pattern 4: Plain name without quantity
        return ParsedIngredient(name = clean, amount = "", unit = "", displayQuantity = "")
    }

    fun auditCorruptedRecipes(recipes: List<Recipe>): Int {
        val knownUnits = setOf("g", "ml", "oz", "unit", "unid", "tsp", "tbsp", "cda", "cdta", "taza", "piezas", "g.", "ml.")
        return recipes.count { recipe ->
            val parsed = parseIngredientsSummary(recipe.ingredientsSummary)
            parsed.any { ing ->
                val nameLower = ing.name.trim().lowercase()
                val isNumericName = nameLower.toFloatOrNull() != null
                val isUnitName = knownUnits.contains(nameLower)
                isNumericName || isUnitName
            }
        }
    }

    fun normalizeUnit(unitStr: String): String {
        val u = unitStr.trim().uppercase()
        return when {
            u.contains("G") || u.contains("GRAM") -> "G"
            u.contains("ML") || u.contains("MILI") || u.contains("L") -> "ML"
            u.contains("UNIT") || u.contains("UNID") || u.contains("PIEZA") -> "UNIT"
            u.contains("TSP") || u.contains("CUCHARADITA") || u.contains("CDTA") -> "TSP"
            u.contains("TBSP") || u.contains("CUCHARADA") || u.contains("CDA") -> "TBSP"
            u.contains("OZ") || u.contains("ONZA") -> "OZ"
            else -> "OTHER"
        }
    }

    private fun inferUnitFromName(lowercaseName: String): String {
        return when {
            lowercaseName.contains("agua") || lowercaseName.contains("leche") || lowercaseName.contains("jarabe") || lowercaseName.contains("sirope") || lowercaseName.contains("tónica") || lowercaseName.contains("jugo") -> "ML"
            lowercaseName.contains("hielo") || lowercaseName.contains("unidad") || lowercaseName.contains("galleta") || lowercaseName.contains("canela") -> "UNIT"
            else -> "G"
        }
    }
}
