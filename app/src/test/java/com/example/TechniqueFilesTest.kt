package com.example

import com.example.data.database.Technique
import com.example.data.database.TechniqueStep
import com.example.data.engine.TechniqueFiles
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TechniqueFilesTest {
    private fun source(): String {
        val technique = Technique(id = "original", name = "Mi técnica", methodId = "local-private-method", beanId = "private-bean", ownerUserId = "private-account")
        return TechniqueFiles.encode(technique, "Método improvisado", listOf(TechniqueStep(techniqueId = "original", stepNumber = 1,
            title = "Inmersión", durationSeconds = 180, waterAddedMl = 240, waterAccumulatedMl = 240, gesture = "WAIT", stepNote = "Reposar")))
    }
    @Test fun portableFilePreservesContentButNotPrivateReferences() {
        val text = source()
        assertFalse(text.contains("private-account")); assertFalse(text.contains("private-bean"))
        val draft = TechniqueFiles.decode(text)
        assertEquals("Método improvisado", draft.methodName)
        assertEquals(240, draft.technique.waterMl)
        assertEquals("WAIT", draft.steps.single().gesture)
        assertNull(draft.technique.beanId); assertNull(draft.technique.ownerUserId)
        assertNotEquals("original", draft.technique.id)
        assertNotEquals(draft.technique.id, TechniqueFiles.decode(text).technique.id)
        java.io.File("/private/tmp/brew-android-technique.json").writeText(text)
    }
    @Test fun rejectsWrongVersionWaterMismatchFractionalDurationAndOversizedFiles() {
        fun rejects(text: String) { assertTrue(runCatching { TechniqueFiles.decode(text) }.isFailure) }
        rejects(JSONObject(source()).put("version", 2).toString())
        val water = JSONObject(source()); water.getJSONObject("technique").put("waterMl", 100); rejects(water.toString())
        val duration = JSONObject(source()); duration.getJSONObject("technique").getJSONArray("steps").getJSONObject(0).put("durationSeconds", 1.5); rejects(duration.toString())
        rejects(" ".repeat(TechniqueFiles.MAX_BYTES + 1))
    }
    @Test fun readsFileExportedByIOS() {
        val file = java.io.File("/private/tmp/brew-ios-technique.json")
        assumeTrue(file.exists())
        val draft = TechniqueFiles.decode(file.readText())
        assertEquals("Método improvisado", draft.methodName)
        assertEquals(240, draft.technique.waterMl)
        assertEquals(180, draft.steps.single().durationSeconds)
    }
}
