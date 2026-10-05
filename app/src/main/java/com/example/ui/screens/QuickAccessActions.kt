package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.database.Recipe
import com.example.ui.theme.*
import com.example.ui.viewmodel.BaristaCalcViewModel

/** Same IDs, default order and minimum selection as iOS QuickAccessAction. */
enum class QuickAccessAction(val id: String, val title: String) {
    ADD_COFFEE("add_coffee", "Agregar grano"),
    ADD_EQUIPMENT("add_equipment", "Agregar instrumento"),
    SHARE_RECIPE("share_recipe", "Compartir receta"),
    ADD_RECIPE("add_recipe", "Nueva receta"),
    ADD_TECHNIQUE("add_technique", "Nueva técnica"),
    ADD_GRINDER("add_grinder", "Agregar molino");
    companion object {
        val defaults = setOf("add_coffee", "add_equipment", "share_recipe", "add_recipe")
        fun normalize(ids: Set<String>): Set<String> = ids.intersect(entries.map { it.id }.toSet()).ifEmpty { defaults }
        fun toggle(ids: Set<String>, id: String): Set<String> {
            val current = normalize(ids)
            if (entries.none { it.id == id }) return current
            return if (id in current) { if (current.size > 1) current - id else current } else current + id
        }
    }
}

// Share only the chosen recipe, never account IDs, tokens or sync metadata.
fun recipeShareText(recipe: Recipe, methodName: String? = recipe.legacyMethodName ?: recipe.suggestedMethodId): String = listOf(
    "Brew Studio · ${recipe.name}", recipe.intention,
    methodName?.takeIf { it.isNotBlank() }?.takeUnless { runCatching { java.util.UUID.fromString(it) }.isSuccess }
        ?.let { "Método: $it" }.orEmpty(),
    "Ingredientes", recipe.ingredientsSummary, "Pasos", recipe.stepsSummary,
    recipe.tags.takeIf { it.isNotBlank() }?.let { "Etiquetas: $it" }.orEmpty()
).filter { it.isNotBlank() }.joinToString("\n\n")

@Composable
fun QuickAccessActionHost(action: String?, viewModel: BaristaCalcViewModel,
    onDismiss: () -> Unit, onChangeAction: (String) -> Unit, onShareText: ((String) -> Unit)? = null) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    when (action) {
        "add_coffee" -> AddEditBeanSheet(null, viewModel, onDismiss)
        "add_equipment", "add_grinder", "add_recipe", "add_technique" -> Dialog(
            onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize(), color = MainBackground) {
                if (action == "add_technique") CreateTechniqueFormView(viewModel, onDone = onDismiss)
                else AddingFormSelector(category = when (action) {
                    "add_equipment" -> "Equipos"; "add_grinder" -> "Molinos"; else -> "Recetas"
                }, viewModel = viewModel, onCompleted = onDismiss)
            }
        }
        "share_recipe" -> AlertDialog(onDismissRequest = onDismiss, title = { Text("Compartir receta") },
            text = {
                if (state.recipesList.isEmpty()) Text("Aún no tienes recetas. Crea una para poder compartirla.")
                else Column {
                    Text("Elige una receta para compartir desde tu teléfono.")
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(state.recipesList, key = { it.id }) { recipe ->
                            TextButton(onClick = {
                                val methodName = state.userMethods.firstOrNull { it.methodId == recipe.suggestedMethodId }?.name
                                    ?: recipe.legacyMethodName ?: recipe.suggestedMethodId
                                val text = recipeShareText(recipe, methodName)
                                if (onShareText != null) onShareText(text)
                                else context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
                                    putExtra(Intent.EXTRA_SUBJECT, recipe.name)
                                }, "Compartir receta"))
                                onDismiss()
                            }, modifier = Modifier.fillMaxWidth()) { Text(recipe.name, color = TextPrincipal) }
                        }
                    }
                }
            }, confirmButton = {
                if (state.recipesList.isEmpty()) TextButton(onClick = { onChangeAction("add_recipe") }) { Text("Nueva receta") }
            }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }, containerColor = SurfaceCard)
    }
}
