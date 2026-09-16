package com.example.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.validation.BrewInputRules
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BrewAggregateRoomTest {
    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `laboratory technique and pours persist replace and delete atomically`() = runBlocking {
        val normalized = requireNotNull(
            BrewInputRules.normalizeTechnique(
                name = "V60 prueba integral",
                coffee = 20f,
                temperature = 93,
                stepTitles = listOf("Preinfusión", "Vertido central", "Vertido final"),
                stepDurations = listOf(40, 50, 70),
                stepWaters = listOf(60, 120, 140)
            )
        )
        val techniqueId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
        val technique = Technique(
            id = techniqueId,
            name = "V60 prueba integral",
            methodId = "11111111-1111-4000-8000-000000000001",
            doseG = 20f,
            waterMl = normalized.waterMl,
            ratio = normalized.ratio,
            temperatureC = 93,
            totalTimeSeconds = normalized.totalTimeSeconds,
            ownerUserId = "owner-a",
            syncStatus = "PENDING_CREATE"
        )
        val originalSteps = listOf(
            TechniqueStep("step-a", techniqueId, 1, "Preinfusión", 40, 60, 60),
            TechniqueStep("step-b", techniqueId, 2, "Vertido central", 50, 120, 180),
            TechniqueStep("step-c", techniqueId, 3, "Vertido final", 70, 140, 320)
        )

        database.techniqueDao().insertTechniqueWithSteps(technique, originalSteps)

        assertEquals(320, database.techniqueDao().getTechniqueById(techniqueId)?.waterMl)
        assertEquals(listOf(60, 180, 320), database.techniqueStepDao().getStepsForTechniqueSync(techniqueId).map { it.waterAccumulatedMl })

        val replacement = listOf(
            TechniqueStep("step-new-a", techniqueId, 1, "Preinfusión larga", 45, 80, 80),
            TechniqueStep("step-new-b", techniqueId, 2, "Vertido único", 115, 240, 320)
        )
        database.techniqueDao().replaceTechniqueWithSteps(
            technique.copy(notes = "Editada sin dejar pasos huérfanos", syncStatus = "PENDING_UPDATE"),
            replacement
        )

        val replacedSteps = database.techniqueStepDao().getStepsForTechniqueSync(techniqueId)
        assertEquals(listOf("step-new-a", "step-new-b"), replacedSteps.map { it.id })
        assertEquals(320, replacedSteps.last().waterAccumulatedMl)

        database.techniqueDao().deleteTechniqueWithSteps(technique)
        assertNull(database.techniqueDao().getTechniqueById(techniqueId))
        assertTrue(database.techniqueStepDao().getStepsForTechniqueSync(techniqueId).isEmpty())
    }

    @Test
    fun `prepared technique snapshot remains linked to its tasting`() = runBlocking {
        val techniqueId = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
        val cupId = "cccccccc-cccc-4ccc-8ccc-cccccccccccc"
        val ownerId = "owner-b"
        database.techniqueDao().insertTechniqueWithSteps(
            Technique(
                id = techniqueId,
                name = "AeroPress invertida",
                methodId = "11111111-1111-4000-8000-000000000002",
                doseG = 18f,
                waterMl = 234,
                ratio = 13f,
                temperatureC = 91,
                totalTimeSeconds = 135,
                ownerUserId = ownerId,
                syncStatus = "PENDING_CREATE"
            ),
            listOf(
                TechniqueStep("step-1", techniqueId, 1, "Agregar agua", 45, 234, 234),
                TechniqueStep("step-2", techniqueId, 2, "Presionar", 90, 0, 234)
            )
        )
        val cup = Cup(
            id = cupId,
            techniqueId = techniqueId,
            methodId = "11111111-1111-4000-8000-000000000002",
            executedDoseG = 18f,
            executedWaterMl = 234,
            executedRatio = 13f,
            executedTemperatureC = 91,
            executedDurationSeconds = 135,
            techniqueNameSnapshot = "AeroPress invertida",
            methodNameSnapshot = "AeroPress",
            rating = 4.5,
            comment = "Dulce y limpia",
            ownerUserId = ownerId,
            syncStatus = "PENDING_CREATE"
        )
        val tasting = Cata(
            id = "dddddddd-dddd-4ddd-8ddd-dddddddddddd",
            cupId = cupId,
            activeFlavorFamily = "FRUITY",
            selectedFlavorNotesJson = "[\"durazno\",\"cítricos\"]",
            textureLevel = "MEDIUM",
            cleanlinessLevel = "HIGH",
            persistenceLevel = "MEDIUM",
            overallScore = 4.5,
            evaluatorNotes = "Dulce y limpia",
            ownerUserId = ownerId,
            syncStatus = "PENDING_CREATE"
        )

        database.cupDao().insertCupWithCata(cup, tasting)

        val storedCup = database.cupDao().getCupById(cupId)
        val storedTasting = database.cataDao().getCataForCupSync(cupId)
        assertNotNull(storedCup)
        assertNotNull(storedTasting)
        assertEquals(techniqueId, storedCup?.techniqueId)
        assertEquals("AeroPress invertida", storedCup?.techniqueNameSnapshot)
        assertEquals(234, storedCup?.executedWaterMl)
        assertEquals(cupId, storedTasting?.cupId)
        assertEquals(ownerId, storedTasting?.ownerUserId)
        assertEquals(1, database.cataDao().getAllCatas().first().count { it.cupId == cupId })
    }

    @Test
    fun `migration six to seven recreates method preferences without duplicates`() {
        val sqlite = database.openHelper.writableDatabase
        sqlite.execSQL("DROP TABLE user_method_preferences")

        MIGRATION_6_7.migrate(sqlite)
        MIGRATION_6_7.migrate(sqlite)

        sqlite.query(
            "SELECT id, userId, methodId, isPinnedToCalculator, isActive, addedAt " +
                "FROM user_method_preferences ORDER BY id"
        ).use { cursor ->
            assertEquals(5, cursor.count)
            var pinnedCount = 0
            while (cursor.moveToNext()) {
                assertEquals("local_user", cursor.getString(1))
                assertTrue(cursor.getString(2).matches(Regex("^[0-9a-f-]{36}$")))
                if (cursor.getInt(3) == 1) pinnedCount++
                assertEquals(1, cursor.getInt(4))
                assertTrue(cursor.getString(5).endsWith("Z"))
            }
            assertEquals(4, pinnedCount)
        }
    }
}
