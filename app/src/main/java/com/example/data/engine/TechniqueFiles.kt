package com.example.data.engine

import com.example.data.database.Technique
import com.example.data.database.TechniqueStep
import com.example.data.validation.BrewInputRules
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Portable public content only. Never exports owner IDs, auth tokens or local inventory links. */
object TechniqueFiles {
    const val MAX_BYTES = 1_048_576
    fun readLimited(input: java.io.InputStream): String {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= MAX_BYTES) { "El archivo supera 1 MB." }
            output.write(buffer, 0, count)
        }
        return output.toString("UTF-8")
    }
    data class Draft(val technique: Technique, val methodName: String, val steps: List<TechniqueStep>)

    fun encode(technique: Technique, methodName: String, steps: List<TechniqueStep>): String {
        require(steps.isNotEmpty() && steps.all { it.techniqueId == technique.id } && steps.sumOf { it.waterAddedMl } == technique.waterMl) {
            "Los pasos todavía no están disponibles o no corresponden a esta técnica."
        }
        val body = JSONObject().put("name", technique.name).put("methodName", methodName)
            .put("doseGrams", technique.doseG.toDouble()).put("waterMl", technique.waterMl)
            .put("temperatureC", technique.temperatureC).put("executionMode", technique.executionMode)
            .put("grindValue", technique.grindValue ?: 0.0).put("grindUnit", technique.grindUnit)
            .put("grindDescription", technique.grindDescription ?: "").put("notes", technique.notes)
            .put("description", technique.description ?: "")
        val list = JSONArray()
        steps.sortedBy { it.stepNumber }.forEach { step ->
            list.put(JSONObject().put("title", step.title).put("durationSeconds", step.durationSeconds)
                .put("waterAddedMl", step.waterAddedMl).put("gesture", step.gesture).put("intensity", step.intensity)
                .put("note", step.stepNote).put("coverage", step.coverage ?: JSONObject.NULL)
                .put("flow", step.flow ?: JSONObject.NULL).put("secondaryAction", step.secondaryAction ?: JSONObject.NULL))
        }
        body.put("steps", list)
        return JSONObject().put("format", "brew-studio").put("version", 1).put("kind", "technique").put("technique", body).toString(2)
    }

    fun decode(text: String): Draft {
        fun integer(obj: JSONObject, key: String): Int {
            val number = obj.get(key) as? Number ?: error("$key debe ser numérico.")
            val value = number.toDouble()
            require(value.isFinite() && value == kotlin.math.floor(value) && value in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble())
            return value.toInt()
        }
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "El archivo supera 1 MB." }
        val root = JSONObject(text)
        require(root.getString("format") == "brew-studio" && integer(root, "version") == 1 && root.getString("kind") == "technique") { "Archivo de técnica no compatible." }
        val body = root.getJSONObject("technique")
        val name = body.getString("name").trim(); val method = body.getString("methodName").trim()
        require(name.isNotBlank() && name.length <= 200 && method.isNotBlank() && method.length <= 200) { "Falta el nombre de la técnica o método." }
        val id = UUID.randomUUID().toString()
        var accumulated = 0
        val array = body.getJSONArray("steps")
        require(array.length() in 1..128) { "La técnica necesita entre 1 y 128 pasos." }
        val steps = (0 until array.length()).map { index ->
            val step = array.getJSONObject(index)
            val water = integer(step, "waterAddedMl"); val duration = integer(step, "durationSeconds")
            require(water in 0..2000 && duration in 1..172800) { "Revisa el agua y duración de los pasos." }
            accumulated += water
            TechniqueStep(techniqueId = id, stepNumber = index + 1, title = step.getString("title"), durationSeconds = duration,
                waterAddedMl = water, waterAccumulatedMl = accumulated, gesture = step.getString("gesture"), intensity = step.getString("intensity"),
                stepNote = step.getString("note"), coverage = if (step.isNull("coverage")) null else step.getDouble("coverage").toFloat(),
                flow = if (step.isNull("flow")) null else step.getDouble("flow").toFloat(),
                secondaryAction = if (step.isNull("secondaryAction")) null else step.getString("secondaryAction"), syncStatus = "PENDING_CREATE")
        }
        val dose = body.getDouble("doseGrams").toFloat(); val temp = integer(body, "temperatureC")
        val water = integer(body, "waterMl")
        val grind = body.getDouble("grindValue")
        require(grind.isFinite() && grind in 0.0..50000.0 && steps.all {
            (it.coverage == null || (it.coverage.isFinite() && it.coverage in 0f..100f)) &&
            (it.flow == null || (it.flow.isFinite() && it.flow in 0f..200f))
        }) { "La molienda, cobertura o flujo no son válidos." }
        require(dose.isFinite() && BrewInputRules.validCoffee(dose) && BrewInputRules.validWater(water) &&
            temp in 0..100 && accumulated == water && BrewInputRules.validRatio(water / dose) && steps.all { it.title.isNotBlank() }) { "Las cantidades o pasos no son válidos." }
        return Draft(Technique(id = id, name = name, methodId = "", legacyMethodName = method, doseG = dose, waterMl = water, ratio = water / dose,
            temperatureC = temp, executionMode = body.getString("executionMode"), grindValue = grind,
            grindUnit = body.getString("grindUnit"), grindDescription = body.getString("grindDescription"), notes = body.getString("notes"),
            description = body.getString("description"), totalTimeSeconds = steps.sumOf { it.durationSeconds }, copyMode = "IMPORT", syncStatus = "PENDING_CREATE"), method, steps)
    }
}
