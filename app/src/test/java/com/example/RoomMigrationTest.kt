package com.example

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.database.AppDatabase
import com.example.data.database.MIGRATION_5_6
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigrationTest {

    private val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testMigration5To6WithPopulatedV5Database() {
        val dbName = "test_v5_to_v6.db"
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)

        // 1. Create and populate V5 schema database using MigrationTestHelper
        val dbV5 = helper.createDatabase(dbName, 5)
        populateV5SampleData(dbV5)

        // Verify pre-migration counts
        verifyV5DataBeforeMigration(dbV5)

        // 2. Execute migration to V6 and validate schema
        val dbV6 = helper.runMigrationsAndValidate(dbName, 6, true, MIGRATION_5_6)

        // 3. Verify V6 migration correctness and exact data preservation
        verifyV6DataAfterMigration(dbV6)

        dbV6.close()
    }

    private fun verifyV5DataBeforeMigration(db: SupportSQLiteDatabase) {
        val beanCursor = db.query("SELECT COUNT(*) FROM beans")
        assertTrue(beanCursor.moveToFirst())
        assertEquals(2, beanCursor.getInt(0))
        beanCursor.close()

        val equipCursor = db.query("SELECT COUNT(*) FROM equipment")
        assertTrue(equipCursor.moveToFirst())
        assertEquals(2, equipCursor.getInt(0))
        equipCursor.close()

        val grinderCursor = db.query("SELECT COUNT(*) FROM grinders")
        assertTrue(grinderCursor.moveToFirst())
        assertEquals(2, grinderCursor.getInt(0))
        grinderCursor.close()

        val techCursor = db.query("SELECT COUNT(*) FROM techniques")
        assertTrue(techCursor.moveToFirst())
        assertEquals(5, techCursor.getInt(0))
        techCursor.close()

        val recipeCursor = db.query("SELECT COUNT(*) FROM recipes")
        assertTrue(recipeCursor.moveToFirst())
        assertEquals(1, recipeCursor.getInt(0))
        recipeCursor.close()

        val cupCursor = db.query("SELECT COUNT(*) FROM cups")
        assertTrue(cupCursor.moveToFirst())
        assertEquals(1, cupCursor.getInt(0))
        cupCursor.close()

        val cataCursor = db.query("SELECT COUNT(*) FROM catas")
        assertTrue(cataCursor.moveToFirst())
        assertEquals(1, cataCursor.getInt(0))
        cataCursor.close()

        val labCursor = db.query("SELECT COUNT(*) FROM lab_experiments")
        assertTrue(labCursor.moveToFirst())
        assertEquals(1, labCursor.getInt(0))
        labCursor.close()
    }

    private fun verifyV6DataAfterMigration(db: SupportSQLiteDatabase) {
        // 1. Verify legacy tables 'equipment' and 'grinders' were dropped
        var tableCheck = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='equipment'")
        assertFalse("Legacy equipment table must be dropped", tableCheck.moveToFirst())
        tableCheck.close()

        tableCheck = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='grinders'")
        assertFalse("Legacy grinders table must be dropped", tableCheck.moveToFirst())
        tableCheck.close()

        // 2. Verify Equipment & Grinders migrated to 'instruments', 'grinder_profiles', and 'grinder_method_settings'
        val instCursor = db.query("SELECT id, name, type, migrationStatus, createdAt, updatedAt FROM instruments")
        var instCount = 0
        while (instCursor.moveToNext()) {
            instCount++
            val id = instCursor.getString(0)
            val name = instCursor.getString(1)
            val type = instCursor.getString(2)
            val status = instCursor.getString(3)
            val createdAt = instCursor.getString(4)
            val updatedAt = instCursor.getString(5)

            assertTrue("Instrument ID must be a valid UUID: $id", uuidRegex.matches(id))
            assertEquals("NEEDS_REVIEW", status)
            assertValidIsoDate(createdAt)
            assertValidIsoDate(updatedAt)
            assertTrue("Instrument name must not be empty", name.isNotEmpty())
            assertTrue("Instrument type must be valid", type in listOf("BREWER", "KETTLE", "GRINDER", "SCALE", "OTHER"))
        }
        instCursor.close()
        assertEquals("Should have 4 instruments (2 equipment + 2 grinders)", 4, instCount)

        val profileCursor = db.query("SELECT id, instrumentId, clickRange FROM grinder_profiles")
        var profileCount = 0
        while (profileCursor.moveToNext()) {
            profileCount++
            val profId = profileCursor.getString(0)
            val instId = profileCursor.getString(1)
            assertTrue("Grinder profile ID must be valid UUID", uuidRegex.matches(profId))
            assertTrue("Instrument ID reference must be valid UUID", uuidRegex.matches(instId))
        }
        profileCursor.close()
        assertEquals("Should have 2 grinder profiles", 2, profileCount)

        // 3. Verify Techniques migration (Unknown technique handling)
        val techCursor = db.query("SELECT id, name, methodId, legacyMethodName, migrationStatus FROM techniques")
        var techCount = 0
        while (techCursor.moveToNext()) {
            techCount++
            val id = techCursor.getString(0)
            val name = techCursor.getString(1)
            val methodId = techCursor.getString(2)
            val legacyMethodName = if (!techCursor.isNull(3)) techCursor.getString(3) else null
            val status = techCursor.getString(4)

            assertTrue("Technique ID must be valid UUID: $id", uuidRegex.matches(id))

            if (name.contains("Kalita Wave")) {
                // Requirement 2: Unknown technique must NOT convert automatically to V60 or grouped dummy ID.
                // Must create a custom BrewMethod using deterministic UUID v5, retain legacyMethodName and be marked NEEDS_REVIEW
                val expectedCustomMethodId = UUID.nameUUIDFromBytes("method_Kalita Wave Slow Drip".toByteArray()).toString()
                assertNotEquals("Unknown technique must not be assigned to V60", "11111111-1111-4000-8000-000000000001", methodId)
                assertEquals(expectedCustomMethodId, methodId)
                assertEquals("Kalita Wave Slow Drip", legacyMethodName)
                assertEquals("NEEDS_REVIEW", status)

                // Verify custom brew method was inserted into brew_methods table
                val bmCursor = db.query("SELECT id, nameKey, category FROM brew_methods WHERE id = ?", arrayOf(expectedCustomMethodId))
                assertTrue("Custom brew method must be created in brew_methods table", bmCursor.moveToFirst())
                assertEquals(expectedCustomMethodId, bmCursor.getString(0))
                assertEquals("Kalita Wave Slow Drip", bmCursor.getString(1))
                assertEquals("OTHER", bmCursor.getString(2))
                bmCursor.close()
            } else if (name.contains("V60")) {
                assertEquals("11111111-1111-4000-8000-000000000001", methodId)
            } else if (name.contains("Aeropress")) {
                assertEquals("11111111-1111-4000-8000-000000000002", methodId)
            } else if (name.contains("Espresso")) {
                assertEquals("11111111-1111-4000-8000-000000000003", methodId)
            } else if (name.contains("Prensa")) {
                assertEquals("11111111-1111-4000-8000-000000000004", methodId)
            }
        }
        techCursor.close()
        assertEquals("Should preserve all 5 techniques", 5, techCount)

        // 4. Verify Recipe child tables migration (ingredients, steps, tags)
        val ingCursor = db.query("SELECT id, recipeId, name, orderIndex FROM recipe_ingredients ORDER BY orderIndex")
        val ingNames = mutableListOf<String>()
        while (ingCursor.moveToNext()) {
            val ingId = ingCursor.getString(0)
            val recipeId = ingCursor.getString(1)
            val name = ingCursor.getString(2)
            assertTrue("Ingredient ID must be valid UUID", uuidRegex.matches(ingId))
            assertTrue("Recipe ID must be valid UUID", uuidRegex.matches(recipeId))
            ingNames.add(name)
        }
        ingCursor.close()
        assertEquals(2, ingNames.size)
        assertEquals("Café Especial 15g", ingNames[0])
        assertEquals("Agua Mineral 240g", ingNames[1])

        val stepCursor = db.query("SELECT id, recipeId, instruction, stepNumber FROM recipe_steps ORDER BY stepNumber")
        val stepInstructions = mutableListOf<String>()
        while (stepCursor.moveToNext()) {
            val stepId = stepCursor.getString(0)
            val recipeId = stepCursor.getString(1)
            val instruction = stepCursor.getString(2)
            assertTrue("Step ID must be valid UUID", uuidRegex.matches(stepId))
            assertTrue("Recipe ID must be valid UUID", uuidRegex.matches(recipeId))
            stepInstructions.add(instruction)
        }
        stepCursor.close()
        assertEquals(3, stepInstructions.size)
        assertEquals("Verter 50g para preinfusión", stepInstructions[0])
        assertEquals("Verter hasta 150g", stepInstructions[1])
        assertEquals("Verter hasta 240g", stepInstructions[2])

        val tagCursor = db.query("SELECT id, recipeId, tag FROM recipe_tags")
        val tags = mutableListOf<String>()
        while (tagCursor.moveToNext()) {
            val tagId = tagCursor.getString(0)
            val tag = tagCursor.getString(2)
            assertTrue("Tag ID must be valid UUID", uuidRegex.matches(tagId))
            tags.add(tag)
        }
        tagCursor.close()
        assertEquals(3, tags.size)
        assertTrue(tags.contains("Filtro"))
        assertTrue(tags.contains("Afrutado"))
        assertTrue(tags.contains("Especial"))

        // 5. Verify Cups snapshots
        val cupCursor = db.query("SELECT id, executedWaterMl, executedRatio, rating, comment, recipeSnapshotJson, techniqueSnapshotJson, beanSnapshotJson, grinderSnapshotJson, createdAt, updatedAt FROM cups")
        assertTrue("Cup must exist after migration", cupCursor.moveToFirst())
        val cupId = cupCursor.getString(0)
        val waterMl = cupCursor.getInt(1)
        val rating = cupCursor.getDouble(3)
        val comment = cupCursor.getString(4)
        val recipeJson = cupCursor.getString(5)
        val techniqueJson = cupCursor.getString(6)
        val beanJson = cupCursor.getString(7)
        val grinderJson = cupCursor.getString(8)
        val createdAt = cupCursor.getString(9)
        val updatedAt = cupCursor.getString(10)
        cupCursor.close()

        assertTrue("Cup ID must be valid UUID", uuidRegex.matches(cupId))
        assertEquals(240, waterMl)
        assertEquals(4.8, rating, 0.01)
        assertEquals("Taza muy limpia y brillante", comment)

        assertValidIsoDate(createdAt)
        assertValidIsoDate(updatedAt)

        // Verify full versioned JSON snapshots
        assertTrue("Recipe snapshot must contain version", recipeJson.contains("\"schemaVersion\":1"))
        assertTrue("Recipe snapshot must contain recipe name", recipeJson.contains("V60 Specialty Recipe"))

        assertTrue("Technique snapshot must contain version", techniqueJson.contains("\"schemaVersion\":1"))
        assertTrue("Technique snapshot must contain technique name", techniqueJson.contains("V60 4:6 Method"))

        assertTrue("Bean snapshot must contain version", beanJson.contains("\"schemaVersion\":1"))
        assertTrue("Bean snapshot must contain bean name", beanJson.contains("Geisha Panama"))

        assertTrue("Grinder snapshot must contain version", grinderJson.contains("\"schemaVersion\":1"))
        assertTrue("Grinder snapshot must contain grinder name", grinderJson.contains("Comandante C40 MK4"))

        // 6. Verify Beans, Catas & Lab Experiments
        val beanCursor = db.query("SELECT id, roaster, name, stockGrams, createdAt, updatedAt FROM beans")
        var beanCount = 0
        while (beanCursor.moveToNext()) {
            beanCount++
            val bId = beanCursor.getString(0)
            val bRoaster = beanCursor.getString(1)
            val bName = beanCursor.getString(2)
            val bStock = beanCursor.getFloat(3)
            val bCreatedAt = beanCursor.getString(4)
            val bUpdatedAt = beanCursor.getString(5)

            assertTrue("Bean ID must be valid UUID", uuidRegex.matches(bId))
            assertTrue("Roaster must not be empty", bRoaster.isNotEmpty())
            assertTrue("Name must not be empty", bName.isNotEmpty())
            assertTrue("Stock grams must be preserved", bStock > 0f)
            assertValidIsoDate(bCreatedAt)
            assertValidIsoDate(bUpdatedAt)
        }
        beanCursor.close()
        assertEquals("Both beans must be preserved", 2, beanCount)

        val cataCursor = db.query("SELECT id, overallScore, evaluatorNotes, evaluatedAt FROM catas")
        assertTrue("Cata record must exist", cataCursor.moveToFirst())
        val cataId = cataCursor.getString(0)
        val score = cataCursor.getDouble(1)
        val cataNotes = cataCursor.getString(2)
        val evaluatedAt = cataCursor.getString(3)
        cataCursor.close()

        assertTrue("Cata ID must be valid UUID", uuidRegex.matches(cataId))
        assertEquals(8.75, score, 0.01)
        assertEquals("Cata sensorial excelente", cataNotes)
        assertValidIsoDate(evaluatedAt)

        val cataNotesCursor = db.query("SELECT id, cataId, note FROM cata_flavor_notes")
        assertTrue("Cata flavor note must exist", cataNotesCursor.moveToFirst())
        val fnId = cataNotesCursor.getString(0)
        val fnCataId = cataNotesCursor.getString(1)
        val fnNote = cataNotesCursor.getString(2)
        cataNotesCursor.close()

        assertTrue("Flavor note ID must be valid UUID", uuidRegex.matches(fnId))
        assertEquals(cataId, fnCataId)
        assertTrue("Flavor note must match migrated text", fnNote.contains("Cata sensorial excelente"))

        val labCursor = db.query("SELECT id, coffeeGrams, waterMl, temperatureC, grindSetting, experimentNotes, createdAt, updatedAt FROM lab_experiments")
        assertTrue("Lab experiment record must exist", labCursor.moveToFirst())
        val expId = labCursor.getString(0)
        val expCoffee = labCursor.getFloat(1)
        val expWater = labCursor.getInt(2)
        val expTemp = labCursor.getInt(3)
        val expGrind = labCursor.getString(4)
        val expNotes = labCursor.getString(5)
        val expCreatedAt = labCursor.getString(6)
        val expUpdatedAt = labCursor.getString(7)
        labCursor.close()

        assertTrue("Experiment ID must be valid UUID", uuidRegex.matches(expId))
        assertEquals(15.0f, expCoffee, 0.01f)
        assertEquals(240, expWater)
        assertEquals(92, expTemp)
        assertEquals("22", expGrind)
        assertEquals("Prueba de temperatura a 92C", expNotes)
        assertValidIsoDate(expCreatedAt)
        assertValidIsoDate(expUpdatedAt)
    }

    private fun assertValidIsoDate(dateStr: String?) {
        assertNotNull("Date string should not be null", dateStr)
        assertFalse("Date string should not be empty", dateStr!!.isEmpty())
        assertFalse("Date string should not be a raw number string like 1700000000000", dateStr.matches(Regex("^\\d{10,15}$")))
        try {
            Instant.parse(dateStr)
        } catch (e: Exception) {
            fail("Date '$dateStr' is not a valid ISO-8601 string: ${e.message}")
        }
    }

    private fun populateV5SampleData(db: SupportSQLiteDatabase) {
        db.execSQL("""
            INSERT INTO beans (id, roaster, name, origin, altitude, process, roastDate, firstUseDate, notes, status, stockGrams, remoteId, syncStatus, lastSyncedAt, createdAt, updatedAt)
            VALUES 
            ('10000000-0000-4000-8000-000000000001', 'Café de Origen', 'Geisha Panama', 'Panamá', '1800m', 'Washed', '2026-01-01', '2026-01-05', 'Notas florales', 'OPEN', 200.0, NULL, 'SYNCED', 1700000000000, '2026-01-05T10:00:00Z', '2026-01-05T10:00:00Z'),
            ('20000000-0000-4000-8000-000000000002', 'Boutique Roasters', 'Sidra Huila', 'Colombia', '1700m', 'Natural', '2026-01-10', '2026-01-12', 'Frutas rojas', 'OPEN', 150.0, NULL, 'SYNCED', NULL, '2026-01-12T10:00:00Z', '2026-01-12T10:00:00Z')
        """.trimIndent())

        db.execSQL("""
            INSERT INTO equipment (id, name, type, notes, remoteId, syncStatus, lastSyncedAt, createdAt, updatedAt)
            VALUES 
            ('30000000-0000-4000-8000-000000000001', 'Hario V60 Plastic 02', 'BREWER', 'Dripper cónico de plástico', NULL, 'SYNCED', NULL, '2026-01-01T08:00:00Z', '2026-01-01T08:00:00Z'),
            ('30000000-0000-4000-8000-000000000002', 'Fellow Stagg EKG', 'KETTLE', 'Tetera cuello de cisne', NULL, 'SYNCED', NULL, '2026-01-01T08:00:00Z', '2026-01-01T08:00:00Z')
        """.trimIndent())

        db.execSQL("""
            INSERT INTO grinders (id, brand, model, clickRange, favoriteClicksByMethod, calibrationNotes, remoteId, syncStatus, lastSyncedAt, createdAt, updatedAt)
            VALUES 
            ('40000000-0000-4000-8000-000000000001', 'Comandante', 'C40 MK4', '0-60', '24 Clicks', 'Standard', NULL, 'SYNCED', NULL, '2026-01-01T08:00:00Z', '2026-01-01T08:00:00Z'),
            ('40000000-0000-4000-8000-000000000002', 'Timemore', 'C3', '0-30', '16 Clicks', 'Fine', NULL, 'SYNCED', NULL, '2026-01-01T08:00:00Z', '2026-01-01T08:00:00Z')
        """.trimIndent())

        db.execSQL("""
            INSERT INTO techniques (id, name, waterMl, ratio, notes, totalTimeSeconds, remoteId, syncStatus, lastSyncedAt, createdAt, updatedAt)
            VALUES
            ('50000000-0000-4000-8000-000000000001', 'V60 4:6 Method', 240, 16.0, 'Método de Tetsu Kasuya', 180, NULL, 'SYNCED', NULL, '2026-01-01T08:00:00Z', '2026-01-01T08:00:00Z'),
            ('50000000-0000-4000-8000-000000000002', 'Aeropress Inverted', 200, 15.0, 'Método invertido intenso', 120, NULL, 'SYNCED', NULL, '2026-01-01T08:00:00Z', '2026-01-01T08:00:00Z'),
            ('50000000-0000-4000-8000-000000000003', 'Espresso Extraction', 36, 2.0, 'Extracción doble 1:2', 28, NULL, 'SYNCED', NULL, '2026-01-01T08:00:00Z', '2026-01-01T08:00:00Z'),
            ('50000000-0000-4000-8000-000000000004', 'Prensa Francesa Smooth', 300, 15.0, 'Inmersión completa de 4 min', 240, NULL, 'SYNCED', NULL, '2026-01-01T08:00:00Z', '2026-01-01T08:00:00Z'),
            ('50000000-0000-4000-8000-000000000005', 'Kalita Wave Slow Drip', 250, 16.0, 'Método con fondo plano', 210, NULL, 'SYNCED', NULL, '2026-01-01T08:00:00Z', '2026-01-01T08:00:00Z')
        """.trimIndent())

        db.execSQL("""
            INSERT INTO recipes (id, name, notes, ingredientsSummary, stepsSummary, tags, remoteId, syncStatus, lastSyncedAt, createdAt, updatedAt)
            VALUES 
            ('60000000-0000-4000-8000-000000000001', 'V60 Specialty Recipe', 'Perfil de alta extracción', 'Café Especial 15g, Agua Mineral 240g', 'Verter 50g para preinfusión; Verter hasta 150g; Verter hasta 240g', 'Filtro, Afrutado, Especial', NULL, 'SYNCED', NULL, '2026-01-01T08:00:00Z', '2026-01-01T08:00:00Z')
        """.trimIndent())

        db.execSQL("""
            INSERT INTO cups (id, waterMl, ratio, clicks, temperature, durationSeconds, rating, freeNotes, beanName, recipeName, techniqueName, grinderName, remoteId, syncStatus, lastSyncedAt, createdAt, updatedAt)
            VALUES 
            ('70000000-0000-4000-8000-000000000001', 240, 16.0, 24, 93, 180, 4.8, 'Taza muy limpia y brillante', 'Geisha Panama', 'V60 Specialty Recipe', 'V60 4:6 Method', 'Comandante C40 MK4', NULL, 'SYNCED', NULL, '2026-01-05T12:00:00Z', '2026-01-05T12:00:00Z')
        """.trimIndent())

        db.execSQL("""
            INSERT INTO catas (id, cupId, overallScore, notes, cata_notes, remoteId, syncStatus, lastSyncedAt, createdAt, updatedAt)
            VALUES 
            ('80000000-0000-4000-8000-000000000001', '70000000-0000-4000-8000-000000000001', 8.75, 'Cata sensorial excelente', 'Fresa, Jazmín', NULL, 'SYNCED', NULL, '2026-01-05T12:30:00Z', '2026-01-05T12:30:00Z')
        """.trimIndent())

        db.execSQL("""
            INSERT INTO lab_experiments (id, coffeeGrams, waterMl, ratio, temperature, clicks, estimatedTimeSeconds, experimentNotes, remoteId, syncStatus, lastSyncedAt, createdAt, updatedAt)
            VALUES 
            ('90000000-0000-4000-8000-000000000001', 15.0, 240, 16.0, 92, 22, 180, 'Prueba de temperatura a 92C', NULL, 'SYNCED', NULL, '2026-01-06T09:00:00Z', '2026-01-06T09:00:00Z')
        """.trimIndent())
    }
}

