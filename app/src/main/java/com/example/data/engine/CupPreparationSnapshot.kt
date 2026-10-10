package com.example.data.engine

import com.example.data.database.TechniqueStep
import org.json.JSONArray
import org.json.JSONObject

/** Local immutable executed sequence. Never replace it with the current technique. */
object CupPreparationSnapshot {
    fun encode(steps: List<TechniqueStep>): String = JSONArray().apply {
        steps.forEach { step -> put(JSONObject().apply {
            put("id", step.id); put("techniqueId", step.techniqueId); put("number", step.stepNumber)
            put("title", step.title); put("durationSeconds", step.durationSeconds)
            put("waterAddedMl", step.waterAddedMl); put("waterAccumulatedMl", step.waterAccumulatedMl)
            put("gesture", step.gesture); put("intensity", step.intensity); put("note", step.stepNote)
            step.coverage?.let { put("coverage", it) }; step.flow?.let { put("flow", it) }
            step.secondaryAction?.let { put("secondaryAction", it) }
        }) }
    }.toString()

    fun decode(json: String): List<TechniqueStep> {
        val array = JSONArray(json)
        require(array.length() in 1..128)
        return (0 until array.length()).map { index ->
            val step = array.getJSONObject(index)
            TechniqueStep(id = step.getString("id"), techniqueId = step.optString("techniqueId"),
                stepNumber = step.getInt("number"), title = step.getString("title"),
                durationSeconds = step.getInt("durationSeconds"), waterAddedMl = step.getInt("waterAddedMl"),
                waterAccumulatedMl = step.getInt("waterAccumulatedMl"), gesture = step.getString("gesture"),
                intensity = step.getString("intensity"), stepNote = step.optString("note"),
                coverage = if (step.has("coverage")) step.getDouble("coverage").toFloat() else null,
                flow = if (step.has("flow")) step.getDouble("flow").toFloat() else null,
                secondaryAction = if (step.has("secondaryAction")) step.getString("secondaryAction") else null)
        }.also { steps -> require(steps.all { it.durationSeconds > 0 && it.waterAddedMl >= 0 }) }
    }
}
