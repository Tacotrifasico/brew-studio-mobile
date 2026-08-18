package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Instant
import java.util.UUID

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `user_method_preferences` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL DEFAULT 'local_user',
                `methodId` TEXT NOT NULL,
                `isPinnedToCalculator` INTEGER NOT NULL DEFAULT 1,
                `isActive` INTEGER NOT NULL DEFAULT 1,
                `sourceInstrumentId` TEXT,
                `addedAt` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        db.execSQL("""
            INSERT OR IGNORE INTO user_method_preferences (id, userId, methodId, isPinnedToCalculator, isActive, sourceInstrumentId, addedAt) VALUES
            ('pref-v60', 'local_user', '11111111-1111-4000-8000-000000000001', 1, 1, NULL, '2026-01-01T00:00:00Z'),
            ('pref-aeropress', 'local_user', '11111111-1111-4000-8000-000000000002', 1, 1, NULL, '2026-01-01T00:00:00Z'),
            ('pref-espresso', 'local_user', '11111111-1111-4000-8000-000000000003', 1, 1, NULL, '2026-01-01T00:00:00Z'),
            ('pref-french_press', 'local_user', '11111111-1111-4000-8000-000000000004', 1, 1, NULL, '2026-01-01T00:00:00Z'),
            ('pref-custom', 'local_user', '11111111-1111-4000-8000-000000000005', 0, 1, NULL, '2026-01-01T00:00:00Z')
        """.trimIndent())
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create brew_methods table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `brew_methods` (
                `id` TEXT NOT NULL,
                `code` TEXT NOT NULL,
                `nameKey` TEXT NOT NULL,
                `category` TEXT NOT NULL DEFAULT 'POUR_OVER',
                `defaultRatio` REAL NOT NULL DEFAULT 16.0,
                `ownerUserId` TEXT,
                `schemaVersion` INTEGER NOT NULL DEFAULT 1,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        // Seed core BrewMethod UUIDs
        db.execSQL("""
            INSERT OR IGNORE INTO brew_methods (id, code, nameKey, category, defaultRatio, ownerUserId, schemaVersion) VALUES
            ('11111111-1111-4000-8000-000000000001', 'v60', 'brew_method.v60.name', 'POUR_OVER', 16.0, NULL, 1),
            ('11111111-1111-4000-8000-000000000002', 'aeropress', 'brew_method.aeropress.name', 'HYBRID', 15.0, NULL, 1),
            ('11111111-1111-4000-8000-000000000003', 'espresso', 'brew_method.espresso.name', 'PRESSURE', 2.0, NULL, 1),
            ('11111111-1111-4000-8000-000000000004', 'french_press', 'brew_method.french_press.name', 'IMMERSION', 15.0, NULL, 1),
            ('11111111-1111-4000-8000-000000000005', 'custom', 'brew_method.custom.name', 'OTHER', 15.0, NULL, 1)
        """.trimIndent())

        // 2. Create instruments table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `instruments` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `brand` TEXT NOT NULL DEFAULT '',
                `model` TEXT NOT NULL DEFAULT '',
                `notes` TEXT NOT NULL DEFAULT '',
                `ownerUserId` TEXT,
                `schemaVersion` INTEGER NOT NULL DEFAULT 1,
                `remoteId` TEXT,
                `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED',
                `serverVersion` INTEGER NOT NULL DEFAULT 1,
                `expectedVersion` INTEGER NOT NULL DEFAULT 1,
                `lastSyncedAt` TEXT,
                `createdAt` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `migrationStatus` TEXT NOT NULL DEFAULT 'MIGRATED',
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        // 3. Create grinder_profiles table with FK & Index
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `grinder_profiles` (
                `id` TEXT NOT NULL,
                `instrumentId` TEXT NOT NULL,
                `clickRange` TEXT NOT NULL DEFAULT '',
                `calibrationNotes` TEXT NOT NULL DEFAULT '',
                PRIMARY KEY(`id`),
                FOREIGN KEY(`instrumentId`) REFERENCES `instruments`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_grinder_profiles_instrumentId` ON `grinder_profiles` (`instrumentId`)")

        // 4. Create grinder_method_settings table with FK & Indices
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `grinder_method_settings` (
                `id` TEXT NOT NULL,
                `grinderProfileId` TEXT NOT NULL,
                `methodId` TEXT NOT NULL,
                `settingValue` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`grinderProfileId`) REFERENCES `grinder_profiles`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_grinder_method_settings_grinderProfileId` ON `grinder_method_settings` (`grinderProfileId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_grinder_method_settings_methodId` ON `grinder_method_settings` (`methodId`)")

        // 5. Create recipe child tables: recipe_ingredients, recipe_steps, recipe_tags
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `recipe_ingredients` (
                `id` TEXT NOT NULL,
                `recipeId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `unit` TEXT NOT NULL DEFAULT 'GRAMS',
                `orderIndex` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_recipeId` ON `recipe_ingredients` (`recipeId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `recipe_steps` (
                `id` TEXT NOT NULL,
                `recipeId` TEXT NOT NULL,
                `instruction` TEXT NOT NULL,
                `stepNumber` INTEGER NOT NULL,
                `durationSeconds` INTEGER,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_steps_recipeId` ON `recipe_steps` (`recipeId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `recipe_tags` (
                `id` TEXT NOT NULL,
                `recipeId` TEXT NOT NULL,
                `tag` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_tags_recipeId` ON `recipe_tags` (`recipeId`)")

        // 6. Migrate legacy Equipment -> Instruments (No empty catch blocks, valid UUIDs, ISO dates, NEEDS_REVIEW status)
        val eqCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='equipment'")
        if (eqCursor.moveToFirst()) {
            val cursor = db.query("SELECT * FROM equipment")
            while (cursor.moveToNext()) {
                val rawId = cursor.getStringOrDefault("id", UUID.randomUUID().toString())
                val name = cursor.getStringOrDefault("name", "Equipo")
                val rawType = cursor.getStringOrDefault("type", "OTHER")
                val notes = cursor.getStringOrDefault("notes", "")
                val remoteId = cursor.getStringOrNull("remoteId")
                val syncStatus = cursor.getStringOrDefault("syncStatus", "SYNCED")
                val lastSyncedLong = cursor.getLongOrDefault("lastSyncedAt", 0L)
                val rawCreatedAt = cursor.getStringOrNull("createdAt")
                val rawUpdatedAt = cursor.getStringOrNull("updatedAt")

                val instId = try { UUID.fromString(rawId).toString() } catch (e: Exception) { UUID.nameUUIDFromBytes("eq_$rawId".toByteArray()).toString() }
                val type = when (rawType.uppercase()) {
                    "MOLINO", "GRINDER" -> "GRINDER"
                    "MÉTODO", "METODO", "BREWER_METHOD" -> "BREWER_METHOD"
                    "BÁSCULA", "BASCULA", "SCALE" -> "SCALE"
                    "TETERA", "KETTLE" -> "KETTLE"
                    "FILTROS", "FILTERS" -> "FILTERS"
                    "SERVIDOR", "SERVER" -> "SERVER"
                    "PRENSA", "PRESS" -> "PRESS"
                    "ACCESORIOS", "ACCESSORY" -> "ACCESSORY"
                    else -> "OTHER"
                }
                val lastSyncedAtIso = if (lastSyncedLong > 0) Instant.ofEpochMilli(lastSyncedLong).toString() else null
                val createdAtIso = if (!rawCreatedAt.isNull_or_Empty()) rawCreatedAt else Instant.ofEpochMilli(1700000000000L).toString()
                val updatedAtIso = if (!rawUpdatedAt.isNull_or_Empty()) rawUpdatedAt else Instant.ofEpochMilli(1700000000000L).toString()

                db.execSQL("""
                    INSERT OR REPLACE INTO instruments (id, name, type, brand, model, notes, ownerUserId, schemaVersion, remoteId, syncStatus, serverVersion, expectedVersion, lastSyncedAt, createdAt, updatedAt, migrationStatus)
                    VALUES (?, ?, ?, '', '', ?, NULL, 1, ?, ?, 1, 1, ?, ?, ?, 'NEEDS_REVIEW')
                """.trimIndent(), arrayOf(instId, name, type, notes, remoteId, syncStatus, lastSyncedAtIso, createdAtIso, updatedAtIso))
            }
            cursor.close()
        }
        eqCursor.close()

        // 7. Migrate legacy Grinders -> Instruments & GrinderProfiles & Settings
        val grCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='grinders'")
        if (grCursor.moveToFirst()) {
            val cursor = db.query("SELECT * FROM grinders")
            while (cursor.moveToNext()) {
                val rawId = cursor.getStringOrDefault("id", UUID.randomUUID().toString())
                val brand = cursor.getStringOrDefault("brand", "")
                val model = cursor.getStringOrDefault("model", "")
                val clickRange = cursor.getStringOrDefault("clickRange", "")
                val favClicks = cursor.getStringOrDefault("favoriteClicksByMethod", "")
                val calNotes = cursor.getStringOrDefault("calibrationNotes", "")
                val remoteId = cursor.getStringOrNull("remoteId")
                val syncStatus = cursor.getStringOrDefault("syncStatus", "SYNCED")
                val lastSyncedLong = cursor.getLongOrDefault("lastSyncedAt", 0L)
                val rawCreatedAt = cursor.getStringOrNull("createdAt")
                val rawUpdatedAt = cursor.getStringOrNull("updatedAt")

                val instId = try { UUID.fromString(rawId).toString() } catch (e: Exception) { UUID.nameUUIDFromBytes("gr_$rawId".toByteArray()).toString() }
                val profileId = UUID.nameUUIDFromBytes("prof_$rawId".toByteArray()).toString()
                val name = if (brand.isNotEmpty() || model.isNotEmpty()) "$brand $model".trim() else "Molino"
                val lastSyncedAtIso = if (lastSyncedLong > 0) Instant.ofEpochMilli(lastSyncedLong).toString() else null
                val createdAtIso = if (!rawCreatedAt.isNull_or_Empty()) rawCreatedAt else Instant.ofEpochMilli(1700000000000L).toString()
                val updatedAtIso = if (!rawUpdatedAt.isNull_or_Empty()) rawUpdatedAt else Instant.ofEpochMilli(1700000000000L).toString()

                db.execSQL("""
                    INSERT OR REPLACE INTO instruments (id, name, type, brand, model, notes, ownerUserId, schemaVersion, remoteId, syncStatus, serverVersion, expectedVersion, lastSyncedAt, createdAt, updatedAt, migrationStatus)
                    VALUES (?, ?, 'GRINDER', ?, ?, ?, NULL, 1, ?, ?, 1, 1, ?, ?, ?, 'NEEDS_REVIEW')
                """.trimIndent(), arrayOf(instId, name, brand, model, calNotes, remoteId, syncStatus, lastSyncedAtIso, createdAtIso, updatedAtIso))

                db.execSQL("""
                    INSERT OR REPLACE INTO grinder_profiles (id, instrumentId, clickRange, calibrationNotes)
                    VALUES (?, ?, ?, ?)
                """.trimIndent(), arrayOf(profileId, instId, clickRange, calNotes))

                if (favClicks.isNotEmpty()) {
                    val settingId = UUID.nameUUIDFromBytes("setting_$rawId".toByteArray()).toString()
                    val v60MethodId = "11111111-1111-4000-8000-000000000001"
                    db.execSQL("""
                        INSERT OR REPLACE INTO grinder_method_settings (id, grinderProfileId, methodId, settingValue)
                        VALUES (?, ?, ?, ?)
                    """.trimIndent(), arrayOf(settingId, profileId, v60MethodId, favClicks))
                }
            }
            cursor.close()
        }
        grCursor.close()

        // 8. Recreate beans table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `beans_v6` (
                `id` TEXT NOT NULL,
                `roaster` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `origin` TEXT NOT NULL,
                `altitude` TEXT NOT NULL,
                `process` TEXT NOT NULL,
                `roastDate` TEXT NOT NULL,
                `firstUseDate` TEXT NOT NULL,
                `notes` TEXT NOT NULL,
                `status` TEXT NOT NULL DEFAULT 'OPEN',
                `stockGrams` REAL NOT NULL DEFAULT 0.0,
                `ownerUserId` TEXT,
                `schemaVersion` INTEGER NOT NULL DEFAULT 1,
                `remoteId` TEXT,
                `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED',
                `serverVersion` INTEGER NOT NULL DEFAULT 1,
                `expectedVersion` INTEGER NOT NULL DEFAULT 1,
                `lastSyncedAt` TEXT,
                `createdAt` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `migrationStatus` TEXT NOT NULL DEFAULT 'MIGRATED',
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        val beanTableCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='beans'")
        if (beanTableCursor.moveToFirst()) {
            val cursor = db.query("SELECT * FROM beans")
            while (cursor.moveToNext()) {
                val rawId = cursor.getStringOrDefault("id", UUID.randomUUID().toString())
                val id = try { UUID.fromString(rawId).toString() } catch (e: Exception) { UUID.nameUUIDFromBytes("bean_$rawId".toByteArray()).toString() }
                val roaster = cursor.getStringOrDefault("roaster", "")
                val name = cursor.getStringOrDefault("name", "")
                val origin = cursor.getStringOrDefault("origin", "")
                val altitude = cursor.getStringOrDefault("altitude", "")
                val process = cursor.getStringOrDefault("process", "")
                val roastDate = cursor.getStringOrDefault("roastDate", "2026-01-01")
                val firstUseDate = cursor.getStringOrDefault("firstUseDate", "2026-01-01")
                val notes = cursor.getStringOrDefault("notes", "")
                val stockGrams = if (cursor.getColumnIndex("stockGrams") >= 0 && !cursor.isNull(cursor.getColumnIndex("stockGrams"))) {
                    cursor.getFloat(cursor.getColumnIndex("stockGrams"))
                } else if (cursor.getColumnIndex("bag_size_g") >= 0 && !cursor.isNull(cursor.getColumnIndex("bag_size_g"))) {
                    cursor.getFloat(cursor.getColumnIndex("bag_size_g"))
                } else {
                    250.0f
                }
                val remoteId = cursor.getStringOrNull("remoteId")
                val syncStatus = cursor.getStringOrDefault("syncStatus", "SYNCED")
                val lastSyncedLong = cursor.getLongOrDefault("lastSyncedAt", 0L)
                val rawCreatedAt = cursor.getStringOrNull("createdAt")
                val rawUpdatedAt = cursor.getStringOrNull("updatedAt")

                val lastSyncedAtIso = if (lastSyncedLong > 0) Instant.ofEpochMilli(lastSyncedLong).toString() else null
                val createdAtIso = if (!rawCreatedAt.isNull_or_Empty()) rawCreatedAt else Instant.ofEpochMilli(1700000000000L).toString()
                val updatedAtIso = if (!rawUpdatedAt.isNull_or_Empty()) rawUpdatedAt else Instant.ofEpochMilli(1700000000000L).toString()
                val migrationStatus = "NEEDS_REVIEW"

                db.execSQL("""
                    INSERT OR REPLACE INTO beans_v6 (id, roaster, name, origin, altitude, process, roastDate, firstUseDate, notes, status, stockGrams, ownerUserId, schemaVersion, remoteId, syncStatus, serverVersion, expectedVersion, lastSyncedAt, createdAt, updatedAt, migrationStatus)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'OPEN', ?, NULL, 1, ?, ?, 1, 1, ?, ?, ?, ?)
                """.trimIndent(), arrayOf(id, roaster, name, origin, altitude, process, roastDate, firstUseDate, notes, stockGrams, remoteId, syncStatus, lastSyncedAtIso, createdAtIso, updatedAtIso, migrationStatus))
            }
            cursor.close()
        }
        beanTableCursor.close()

        db.execSQL("DROP TABLE IF EXISTS `beans` ")
        db.execSQL("ALTER TABLE `beans_v6` RENAME TO `beans` ")

        // 9. Recreate techniques table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `techniques_v6` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `methodId` TEXT NOT NULL,
                `recipeId` TEXT,
                `beanId` TEXT,
                `grinderId` TEXT,
                `doseG` REAL NOT NULL DEFAULT 15.0,
                `waterMl` INTEGER NOT NULL DEFAULT 240,
                `ratio` REAL NOT NULL DEFAULT 16.0,
                `temperatureC` INTEGER NOT NULL DEFAULT 93,
                `executionMode` TEXT NOT NULL DEFAULT 'GUIDED',
                `grindValue` REAL,
                `grindDescription` TEXT,
                `grindUnit` TEXT NOT NULL DEFAULT 'CLICKS',
                `notes` TEXT NOT NULL DEFAULT '',
                `totalTimeSeconds` INTEGER NOT NULL DEFAULT 180,
                `author` TEXT,
                `description` TEXT,
                `ownerUserId` TEXT,
                `ownerDisplayName` TEXT,
                `visibility` TEXT NOT NULL DEFAULT 'PRIVATE',
                `isShared` INTEGER NOT NULL DEFAULT 0,
                `originalAuthorUserId` TEXT,
                `originalAuthorName` TEXT,
                `originalEntityId` TEXT,
                `rootEntityId` TEXT,
                `importedFromShareId` TEXT,
                `copyMode` TEXT NOT NULL DEFAULT 'ORIGINAL',
                `originCreatedAt` TEXT,
                `legacyMethodName` TEXT,
                `schemaVersion` INTEGER NOT NULL DEFAULT 1,
                `remoteId` TEXT,
                `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED',
                `serverVersion` INTEGER NOT NULL DEFAULT 1,
                `expectedVersion` INTEGER NOT NULL DEFAULT 1,
                `lastSyncedAt` TEXT,
                `createdAt` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `migrationStatus` TEXT NOT NULL DEFAULT 'MIGRATED',
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        val techTableCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='techniques'")
        if (techTableCursor.moveToFirst()) {
            val cursor = db.query("SELECT * FROM techniques")
            while (cursor.moveToNext()) {
                val rawId = cursor.getStringOrDefault("id", UUID.randomUUID().toString())
                val id = try { UUID.fromString(rawId).toString() } catch (e: Exception) { UUID.nameUUIDFromBytes("tech_$rawId".toByteArray()).toString() }
                val name = cursor.getStringOrDefault("name", "")
                val waterMl = cursor.getIntOrDefault("waterMl", 240)
                val ratio = cursor.getFloatOrDefault("ratio", 16.0f)
                val notes = cursor.getStringOrDefault("notes", "")
                val totalTime = cursor.getIntOrDefault("totalTimeSeconds", 180)
                val remoteId = cursor.getStringOrNull("remoteId")
                val syncStatus = cursor.getStringOrDefault("syncStatus", "SYNCED")
                val lastSyncedLong = cursor.getLongOrDefault("lastSyncedAt", 0L)
                val rawCreatedAt = cursor.getStringOrNull("createdAt")
                val rawUpdatedAt = cursor.getStringOrNull("updatedAt")

                var matchedMethodId: String? = null
                var legacyMethodName: String? = null
                val nameLower = name.lowercase()
                when {
                    nameLower.contains("v60") -> matchedMethodId = "11111111-1111-4000-8000-000000000001"
                    nameLower.contains("aeropress") -> matchedMethodId = "11111111-1111-4000-8000-000000000002"
                    nameLower.contains("espresso") -> matchedMethodId = "11111111-1111-4000-8000-000000000003"
                    nameLower.contains("french") || nameLower.contains("prensa") -> matchedMethodId = "11111111-1111-4000-8000-000000000004"
                    else -> {
                        val customMethodId = UUID.nameUUIDFromBytes("method_$name".toByteArray()).toString()
                        matchedMethodId = customMethodId
                        legacyMethodName = name
                        db.execSQL("""
                            INSERT OR IGNORE INTO brew_methods (id, code, nameKey, category, defaultRatio, ownerUserId, schemaVersion)
                            VALUES (?, ?, ?, 'OTHER', 15.0, NULL, 1)
                        """.trimIndent(), arrayOf(customMethodId, "custom_${name.lowercase().replace(Regex("[^a-z0-9]"), "_")}", name))
                    }
                }

                val lastSyncedAtIso = if (lastSyncedLong > 0) Instant.ofEpochMilli(lastSyncedLong).toString() else null
                val createdAtIso = if (!rawCreatedAt.isNull_or_Empty()) rawCreatedAt else Instant.ofEpochMilli(1700000000000L).toString()
                val updatedAtIso = if (!rawUpdatedAt.isNull_or_Empty()) rawUpdatedAt else Instant.ofEpochMilli(1700000000000L).toString()
                val migrationStatus = "NEEDS_REVIEW"

                db.execSQL("""
                    INSERT OR REPLACE INTO techniques_v6 (id, name, methodId, doseG, waterMl, ratio, temperatureC, notes, totalTimeSeconds, legacyMethodName, ownerUserId, schemaVersion, remoteId, syncStatus, serverVersion, expectedVersion, lastSyncedAt, createdAt, updatedAt, migrationStatus)
                    VALUES (?, ?, ?, 15.0, ?, ?, 93, ?, ?, ?, NULL, 1, ?, ?, 1, 1, ?, ?, ?, ?)
                """.trimIndent(), arrayOf(id, name, matchedMethodId, waterMl, ratio, notes, totalTime, legacyMethodName, remoteId, syncStatus, lastSyncedAtIso, createdAtIso, updatedAtIso, migrationStatus))
            }
            cursor.close()
        }
        techTableCursor.close()

        db.execSQL("DROP TABLE IF EXISTS `techniques` ")
        db.execSQL("ALTER TABLE `techniques_v6` RENAME TO `techniques` ")

        // 10. Recreate technique_steps table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `technique_steps_v6` (
                `id` TEXT NOT NULL,
                `techniqueId` TEXT NOT NULL,
                `stepNumber` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `durationSeconds` INTEGER NOT NULL,
                `waterAddedMl` INTEGER NOT NULL,
                `waterAccumulatedMl` INTEGER NOT NULL,
                `intensity` TEXT NOT NULL DEFAULT 'MEDIUM',
                `gesture` TEXT NOT NULL DEFAULT 'CIRCULAR_POUR',
                `stepNote` TEXT NOT NULL DEFAULT '',
                `coverage` REAL,
                `flow` REAL,
                `secondaryAction` TEXT,
                `position` INTEGER,
                `targetWaterMl` INTEGER,
                `remoteId` TEXT,
                `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED',
                `serverVersion` INTEGER NOT NULL DEFAULT 1,
                `expectedVersion` INTEGER NOT NULL DEFAULT 1,
                `lastSyncedAt` TEXT,
                `createdAt` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`techniqueId`) REFERENCES `techniques`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_technique_steps_techniqueId` ON `technique_steps_v6` (`techniqueId`)")

        val tsTableCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='technique_steps'")
        if (tsTableCursor.moveToFirst()) {
            val cursor = db.query("SELECT * FROM technique_steps")
            while (cursor.moveToNext()) {
                val id = cursor.getStringOrDefault("id", UUID.randomUUID().toString())
                val techId = cursor.getStringOrDefault("techniqueId", "")
                val stepNum = cursor.getIntOrDefault("stepNumber", 1)
                val title = cursor.getStringOrDefault("title", "")
                val dur = cursor.getIntOrDefault("durationSeconds", 30)
                val added = cursor.getIntOrDefault("waterAddedMl", 50)
                val acc = cursor.getIntOrDefault("waterAccumulatedMl", 50)
                val intensity = cursor.getStringOrDefault("intensity", "MEDIUM")
                val gesture = cursor.getStringOrDefault("gesture", "CIRCULAR_POUR")
                val note = cursor.getStringOrDefault("stepNote", "")
                val remoteId = cursor.getStringOrNull("remoteId")
                val syncStatus = cursor.getStringOrDefault("syncStatus", "SYNCED")
                val lastSyncedLong = cursor.getLongOrDefault("lastSyncedAt", 0L)
                val rawCreatedAt = cursor.getStringOrNull("createdAt")
                val rawUpdatedAt = cursor.getStringOrNull("updatedAt")

                val lastSyncedAtIso = if (lastSyncedLong > 0) Instant.ofEpochMilli(lastSyncedLong).toString() else null
                val createdAtIso = if (!rawCreatedAt.isNull_or_Empty()) rawCreatedAt else Instant.ofEpochMilli(1700000000000L).toString()
                val updatedAtIso = if (!rawUpdatedAt.isNull_or_Empty()) rawUpdatedAt else Instant.ofEpochMilli(1700000000000L).toString()

                db.execSQL("""
                    INSERT OR REPLACE INTO technique_steps_v6 (id, techniqueId, stepNumber, title, durationSeconds, waterAddedMl, waterAccumulatedMl, intensity, gesture, stepNote, remoteId, syncStatus, serverVersion, expectedVersion, lastSyncedAt, createdAt, updatedAt)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, ?, ?, ?)
                """.trimIndent(), arrayOf(id, techId, stepNum, title, dur, added, acc, intensity, gesture, note, remoteId, syncStatus, lastSyncedAtIso, createdAtIso, updatedAtIso))
            }
            cursor.close()
        }
        tsTableCursor.close()

        db.execSQL("DROP TABLE IF EXISTS `technique_steps` ")
        db.execSQL("ALTER TABLE `technique_steps_v6` RENAME TO `technique_steps` ")

        // 11. Recreate recipes table & migrate summaries to recipe_ingredients, recipe_steps, recipe_tags
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `recipes_v6` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `recipeKind` TEXT NOT NULL DEFAULT 'BLACK_COFFEE',
                `intention` TEXT NOT NULL DEFAULT '',
                `suggestedMethodId` TEXT,
                `isFavorite` INTEGER NOT NULL DEFAULT 0,
                `ingredientsSummary` TEXT NOT NULL DEFAULT '',
                `stepsSummary` TEXT NOT NULL DEFAULT '',
                `tags` TEXT NOT NULL DEFAULT '',
                `ownerUserId` TEXT,
                `ownerDisplayName` TEXT,
                `visibility` TEXT NOT NULL DEFAULT 'PRIVATE',
                `isShared` INTEGER NOT NULL DEFAULT 0,
                `originalAuthorUserId` TEXT,
                `originalAuthorName` TEXT,
                `originalEntityId` TEXT,
                `rootEntityId` TEXT,
                `importedFromShareId` TEXT,
                `copyMode` TEXT NOT NULL DEFAULT 'ORIGINAL',
                `originCreatedAt` TEXT,
                `legacyMethodName` TEXT,
                `schemaVersion` INTEGER NOT NULL DEFAULT 1,
                `remoteId` TEXT,
                `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED',
                `serverVersion` INTEGER NOT NULL DEFAULT 1,
                `expectedVersion` INTEGER NOT NULL DEFAULT 1,
                `lastSyncedAt` TEXT,
                `createdAt` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `migrationStatus` TEXT NOT NULL DEFAULT 'MIGRATED',
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        val recTableCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='recipes'")
        if (recTableCursor.moveToFirst()) {
            val cursor = db.query("SELECT * FROM recipes")
            while (cursor.moveToNext()) {
                val rawId = cursor.getStringOrDefault("id", UUID.randomUUID().toString())
                val id = try { UUID.fromString(rawId).toString() } catch (e: Exception) { UUID.nameUUIDFromBytes("rec_$rawId".toByteArray()).toString() }
                val name = cursor.getStringOrDefault("name", "")
                val notes = cursor.getStringOrDefault("notes", "")
                val ingSum = cursor.getStringOrDefault("ingredientsSummary", notes)
                val stepSum = cursor.getStringOrDefault("stepsSummary", notes)
                val tagsStr = cursor.getStringOrDefault("tags", "")
                val remoteId = cursor.getStringOrNull("remoteId")
                val syncStatus = cursor.getStringOrDefault("syncStatus", "SYNCED")
                val lastSyncedLong = cursor.getLongOrDefault("lastSyncedAt", 0L)
                val rawCreatedAt = cursor.getStringOrNull("createdAt")
                val rawUpdatedAt = cursor.getStringOrNull("updatedAt")

                val lastSyncedAtIso = if (lastSyncedLong > 0) Instant.ofEpochMilli(lastSyncedLong).toString() else null
                val createdAtIso = if (!rawCreatedAt.isNull_or_Empty()) rawCreatedAt else Instant.ofEpochMilli(1700000000000L).toString()
                val updatedAtIso = if (!rawUpdatedAt.isNull_or_Empty()) rawUpdatedAt else Instant.ofEpochMilli(1700000000000L).toString()
                val migrationStatus = "NEEDS_REVIEW"

                db.execSQL("""
                    INSERT OR REPLACE INTO recipes_v6 (id, name, recipeKind, intention, suggestedMethodId, isFavorite, ingredientsSummary, stepsSummary, tags, ownerUserId, schemaVersion, remoteId, syncStatus, serverVersion, expectedVersion, lastSyncedAt, createdAt, updatedAt, migrationStatus)
                    VALUES (?, ?, 'BLACK_COFFEE', ?, NULL, 0, ?, ?, ?, NULL, 1, ?, ?, 1, 1, ?, ?, ?, ?)
                """.trimIndent(), arrayOf(id, name, notes, ingSum, stepSum, tagsStr, remoteId, syncStatus, lastSyncedAtIso, createdAtIso, updatedAtIso, migrationStatus))

                // Parse ingredients from summary/notes
                val rawIngs = ingSum.split(Regex("[,;\\n]")).map { it.trim() }.filter { it.isNotEmpty() }
                if (rawIngs.isNotEmpty()) {
                    rawIngs.forEachIndexed { index, ingStr ->
                        val ingId = UUID.nameUUIDFromBytes("ing_${id}_$index".toByteArray()).toString()
                        db.execSQL("""
                            INSERT OR REPLACE INTO recipe_ingredients (id, recipeId, name, amount, unit, orderIndex)
                            VALUES (?, ?, ?, 15.0, 'GRAMS', ?)
                        """.trimIndent(), arrayOf(ingId, id, ingStr, index + 1))
                    }
                } else {
                    val ingId = UUID.nameUUIDFromBytes("ing_${id}_0".toByteArray()).toString()
                    db.execSQL("""
                        INSERT OR REPLACE INTO recipe_ingredients (id, recipeId, name, amount, unit, orderIndex)
                        VALUES (?, ?, 'Café', 15.0, 'GRAMS', 1)
                    """.trimIndent(), arrayOf(ingId, id))
                }

                // Parse steps from summary/notes
                val rawSteps = stepSum.split(Regex("[,;\\n]")).map { it.trim() }.filter { it.isNotEmpty() }
                if (rawSteps.isNotEmpty()) {
                    rawSteps.forEachIndexed { index, stepStr ->
                        val stepId = UUID.nameUUIDFromBytes("step_${id}_$index".toByteArray()).toString()
                        db.execSQL("""
                            INSERT OR REPLACE INTO recipe_steps (id, recipeId, instruction, stepNumber)
                            VALUES (?, ?, ?, ?)
                        """.trimIndent(), arrayOf(stepId, id, stepStr, index + 1))
                    }
                } else {
                    val stepId = UUID.nameUUIDFromBytes("step_${id}_0".toByteArray()).toString()
                    db.execSQL("""
                        INSERT OR REPLACE INTO recipe_steps (id, recipeId, instruction, stepNumber)
                        VALUES (?, ?, 'Preparar café', 1)
                    """.trimIndent(), arrayOf(stepId, id))
                }

                // Parse tags
                if (tagsStr.isNotEmpty()) {
                    val rawTags = tagsStr.split(Regex("[,;\\n]")).map { it.trim() }.filter { it.isNotEmpty() }
                    rawTags.forEachIndexed { index, tagStr ->
                        val tagId = UUID.nameUUIDFromBytes("tag_${id}_$index".toByteArray()).toString()
                        db.execSQL("""
                            INSERT OR REPLACE INTO recipe_tags (id, recipeId, tag)
                            VALUES (?, ?, ?)
                        """.trimIndent(), arrayOf(tagId, id, tagStr))
                    }
                }
            }
            cursor.close()
        }
        recTableCursor.close()

        db.execSQL("DROP TABLE IF EXISTS `recipes` ")
        db.execSQL("ALTER TABLE `recipes_v6` RENAME TO `recipes` ")

        // 12. Recreate cups table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `cups_v6` (
                `id` TEXT NOT NULL,
                `recipeId` TEXT,
                `beanId` TEXT,
                `techniqueId` TEXT,
                `methodId` TEXT,
                `grinderId` TEXT,
                `executedDoseG` REAL NOT NULL DEFAULT 15.0,
                `executedWaterMl` INTEGER NOT NULL DEFAULT 240,
                `executedRatio` REAL NOT NULL DEFAULT 16.0,
                `executedTemperatureC` INTEGER NOT NULL DEFAULT 93,
                `executedGrindSetting` TEXT NOT NULL DEFAULT '18',
                `executedDurationSeconds` INTEGER NOT NULL DEFAULT 180,
                `beanNameSnapshot` TEXT NOT NULL DEFAULT '',
                `recipeNameSnapshot` TEXT NOT NULL DEFAULT '',
                `techniqueNameSnapshot` TEXT NOT NULL DEFAULT '',
                `methodNameSnapshot` TEXT NOT NULL DEFAULT '',
                `grinderNameSnapshot` TEXT NOT NULL DEFAULT '',
                `cupLifeSeconds` INTEGER NOT NULL DEFAULT 180,
                `cupLifeState` TEXT NOT NULL DEFAULT 'FRESH',
                `nps` INTEGER,
                `rating` REAL,
                `comment` TEXT NOT NULL DEFAULT '',
                `brewDate` TEXT NOT NULL,
                `recipeSnapshotJson` TEXT NOT NULL DEFAULT '{}',
                `techniqueSnapshotJson` TEXT NOT NULL DEFAULT '{}',
                `beanSnapshotJson` TEXT NOT NULL DEFAULT '{}',
                `grinderSnapshotJson` TEXT NOT NULL DEFAULT '{}',
                `ownerUserId` TEXT,
                `schemaVersion` INTEGER NOT NULL DEFAULT 1,
                `remoteId` TEXT,
                `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED',
                `serverVersion` INTEGER NOT NULL DEFAULT 1,
                `expectedVersion` INTEGER NOT NULL DEFAULT 1,
                `lastSyncedAt` TEXT,
                `createdAt` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `migrationStatus` TEXT NOT NULL DEFAULT 'MIGRATED',
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        val cupTableCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='cups'")
        if (cupTableCursor.moveToFirst()) {
            val cursor = db.query("SELECT * FROM cups")
            while (cursor.moveToNext()) {
                val rawId = cursor.getStringOrDefault("id", UUID.randomUUID().toString())
                val id = try { UUID.fromString(rawId).toString() } catch (e: Exception) { UUID.nameUUIDFromBytes("cup_$rawId".toByteArray()).toString() }
                val waterMl = cursor.getIntOrDefault("waterMl", 240)
                val ratio = cursor.getFloatOrDefault("ratio", 16.0f)
                val clicks = cursor.getIntOrDefault("clicks", 18)
                val temp = cursor.getIntOrDefault("temperature", 93)
                val dur = cursor.getIntOrDefault("durationSeconds", 180)
                val rating = cursor.getDoubleOrNull("rating")
                val notes = cursor.getStringOrDefault("freeNotes", "")
                val beanName = cursor.getStringOrDefault("beanName", "Grano")
                val recipeName = cursor.getStringOrDefault("recipeName", "Receta")
                val techniqueName = cursor.getStringOrDefault("techniqueName", "Técnica")
                val grinderName = cursor.getStringOrDefault("grinderName", "Molino")
                val remoteId = cursor.getStringOrNull("remoteId")
                val syncStatus = cursor.getStringOrDefault("syncStatus", "SYNCED")
                val lastSyncedLong = cursor.getLongOrDefault("lastSyncedAt", 0L)
                val rawCreatedAt = cursor.getStringOrNull("createdAt")
                val rawUpdatedAt = cursor.getStringOrNull("updatedAt")

                val lastSyncedAtIso = if (lastSyncedLong > 0) Instant.ofEpochMilli(lastSyncedLong).toString() else null
                val createdAtIso = if (!rawCreatedAt.isNull_or_Empty()) rawCreatedAt else Instant.ofEpochMilli(1700000000000L).toString()
                val updatedAtIso = if (!rawUpdatedAt.isNull_or_Empty()) rawUpdatedAt else Instant.ofEpochMilli(1700000000000L).toString()
                val migrationStatus = "NEEDS_REVIEW"

                val recipeJson = """{"schemaVersion":1,"name":"$recipeName","doseG":15.0,"waterMl":$waterMl,"ratio":$ratio,"temperatureC":$temp}"""
                val techniqueJson = """{"schemaVersion":1,"name":"$techniqueName","durationSeconds":$dur}"""
                val beanJson = """{"schemaVersion":1,"name":"$beanName"}"""
                val grinderJson = """{"schemaVersion":1,"name":"$grinderName","setting":"$clicks"}"""

                db.execSQL("""
                    INSERT OR REPLACE INTO cups_v6 (id, executedDoseG, executedWaterMl, executedRatio, executedTemperatureC, executedGrindSetting, executedDurationSeconds, rating, comment, brewDate, recipeNameSnapshot, techniqueNameSnapshot, beanNameSnapshot, grinderNameSnapshot, recipeSnapshotJson, techniqueSnapshotJson, beanSnapshotJson, grinderSnapshotJson, ownerUserId, schemaVersion, remoteId, syncStatus, serverVersion, expectedVersion, lastSyncedAt, createdAt, updatedAt, migrationStatus)
                    VALUES (?, 15.0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, 1, ?, ?, 1, 1, ?, ?, ?, ?)
                """.trimIndent(), arrayOf(id, waterMl, ratio, temp, clicks.toString(), dur, rating, notes, createdAtIso, recipeName, techniqueName, beanName, grinderName, recipeJson, techniqueJson, beanJson, grinderJson, remoteId, syncStatus, lastSyncedAtIso, createdAtIso, updatedAtIso, migrationStatus))
            }
            cursor.close()
        }
        cupTableCursor.close()

        db.execSQL("DROP TABLE IF EXISTS `cups` ")
        db.execSQL("ALTER TABLE `cups_v6` RENAME TO `cups` ")

        // 13. Recreate cata_flavor_notes & catas table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `cata_flavor_notes` (
                `id` TEXT NOT NULL,
                `cataId` TEXT NOT NULL,
                `note` TEXT NOT NULL,
                `category` TEXT NOT NULL DEFAULT '',
                PRIMARY KEY(`id`),
                FOREIGN KEY(`cataId`) REFERENCES `catas`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cata_flavor_notes_cataId` ON `cata_flavor_notes` (`cataId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `catas_v6` (
                `id` TEXT NOT NULL,
                `cupId` TEXT,
                `recipeId` TEXT,
                `beanId` TEXT,
                `activeFlavorFamily` TEXT,
                `selectedFlavorNotesJson` TEXT NOT NULL DEFAULT '[]',
                `sensoryWheelDescriptorsJson` TEXT NOT NULL DEFAULT '[]',
                `textureLevel` TEXT,
                `cleanlinessLevel` TEXT,
                `persistenceLevel` TEXT,
                `sweetnessLevel` TEXT,
                `acidityLevel` TEXT,
                `balanceLevel` TEXT,
                `fragranceAromaScore` REAL,
                `flavorScore` REAL,
                `aftertasteScore` REAL,
                `acidityScore` REAL,
                `bodyScore` REAL,
                `uniformityScore` REAL,
                `cleanCupScore` REAL,
                `sweetnessScore` REAL,
                `balanceScore` REAL,
                `overallScore` REAL,
                `totalScaScore` REAL,
                `evaluatorNotes` TEXT NOT NULL DEFAULT '',
                `evaluatedAt` TEXT NOT NULL,
                `ownerUserId` TEXT,
                `schemaVersion` INTEGER NOT NULL DEFAULT 1,
                `remoteId` TEXT,
                `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED',
                `serverVersion` INTEGER NOT NULL DEFAULT 1,
                `expectedVersion` INTEGER NOT NULL DEFAULT 1,
                `lastSyncedAt` TEXT,
                `createdAt` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `migrationStatus` TEXT NOT NULL DEFAULT 'MIGRATED',
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_catas_cupId` ON `catas_v6` (`cupId`)")

        val cataTableCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='catas'")
        if (cataTableCursor.moveToFirst()) {
            val cursor = db.query("SELECT * FROM catas")
            while (cursor.moveToNext()) {
                val rawId = cursor.getStringOrDefault("id", UUID.randomUUID().toString())
                val id = try { UUID.fromString(rawId).toString() } catch (e: Exception) { UUID.nameUUIDFromBytes("cata_$rawId".toByteArray()).toString() }
                val rawCupId = cursor.getStringOrNull("cupId")
                val cupId = rawCupId?.let { try { UUID.fromString(it).toString() } catch (e: Exception) { UUID.nameUUIDFromBytes("cup_$it".toByteArray()).toString() } }
                val score = cursor.getDoubleOrNull("overallScore")
                val notes = cursor.getStringOrDefault("notes", cursor.getStringOrDefault("cata_notes", ""))
                val remoteId = cursor.getStringOrNull("remoteId")
                val syncStatus = cursor.getStringOrDefault("syncStatus", "SYNCED")
                val lastSyncedLong = cursor.getLongOrDefault("lastSyncedAt", 0L)
                val rawCreatedAt = cursor.getStringOrNull("createdAt")
                val rawUpdatedAt = cursor.getStringOrNull("updatedAt")

                val lastSyncedAtIso = if (lastSyncedLong > 0) Instant.ofEpochMilli(lastSyncedLong).toString() else null
                val createdAtIso = if (!rawCreatedAt.isNull_or_Empty()) rawCreatedAt else Instant.ofEpochMilli(1700000000000L).toString()
                val updatedAtIso = if (!rawUpdatedAt.isNull_or_Empty()) rawUpdatedAt else Instant.ofEpochMilli(1700000000000L).toString()
                val migrationStatus = "NEEDS_REVIEW"

                db.execSQL("""
                    INSERT OR REPLACE INTO catas_v6 (id, cupId, overallScore, evaluatorNotes, evaluatedAt, ownerUserId, schemaVersion, remoteId, syncStatus, serverVersion, expectedVersion, lastSyncedAt, createdAt, updatedAt, migrationStatus)
                    VALUES (?, ?, ?, ?, ?, NULL, 1, ?, ?, 1, 1, ?, ?, ?, ?)
                """.trimIndent(), arrayOf(id, cupId, score, notes, createdAtIso, remoteId, syncStatus, lastSyncedAtIso, createdAtIso, updatedAtIso, migrationStatus))

                if (notes.isNotEmpty()) {
                    val fnId = UUID.nameUUIDFromBytes("fn_$id".toByteArray()).toString()
                    db.execSQL("""
                        INSERT OR REPLACE INTO cata_flavor_notes (id, cataId, note, category)
                        VALUES (?, ?, ?, '')
                    """.trimIndent(), arrayOf(fnId, id, notes.take(30)))
                }
            }
            cursor.close()
        }
        cataTableCursor.close()

        db.execSQL("DROP TABLE IF EXISTS `catas` ")
        db.execSQL("ALTER TABLE `catas_v6` RENAME TO `catas` ")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_catas_cupId` ON `catas` (`cupId`)")

        // 14. Recreate lab_experiments table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `lab_experiments_v6` (
                `id` TEXT NOT NULL,
                `methodId` TEXT,
                `beanId` TEXT,
                `grinderId` TEXT,
                `techniqueId` TEXT,
                `coffeeGrams` REAL NOT NULL DEFAULT 15.0,
                `waterMl` INTEGER NOT NULL DEFAULT 240,
                `ratio` REAL NOT NULL DEFAULT 16.0,
                `temperatureC` INTEGER NOT NULL DEFAULT 93,
                `grindSetting` TEXT NOT NULL DEFAULT '18',
                `beanFreshnessDays` INTEGER,
                `estimatedTimeSeconds` INTEGER NOT NULL DEFAULT 180,
                `actualTimeSeconds` INTEGER,
                `experimentHypothesis` TEXT NOT NULL DEFAULT '',
                `experimentNotes` TEXT NOT NULL DEFAULT '',
                `conclusionNotes` TEXT NOT NULL DEFAULT '',
                `resultRating` REAL,
                `resultCupId` TEXT,
                `ownerUserId` TEXT,
                `schemaVersion` INTEGER NOT NULL DEFAULT 1,
                `remoteId` TEXT,
                `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED',
                `serverVersion` INTEGER NOT NULL DEFAULT 1,
                `expectedVersion` INTEGER NOT NULL DEFAULT 1,
                `lastSyncedAt` TEXT,
                `createdAt` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `migrationStatus` TEXT NOT NULL DEFAULT 'MIGRATED',
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

        val labTableCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='lab_experiments'")
        if (labTableCursor.moveToFirst()) {
            val cursor = db.query("SELECT * FROM lab_experiments")
            while (cursor.moveToNext()) {
                val rawId = cursor.getStringOrDefault("id", UUID.randomUUID().toString())
                val id = try { UUID.fromString(rawId).toString() } catch (e: Exception) { UUID.nameUUIDFromBytes("lab_$rawId".toByteArray()).toString() }
                val coffeeGrams = cursor.getFloatOrDefault("coffeeGrams", 15.0f)
                val waterMl = cursor.getIntOrDefault("waterMl", 240)
                val ratio = cursor.getFloatOrDefault("ratio", 16.0f)
                val temp = cursor.getIntOrDefault("temperature", 93)
                val clicks = cursor.getIntOrDefault("clicks", 18)
                val estTime = cursor.getIntOrDefault("estimatedTimeSeconds", 180)
                val notes = cursor.getStringOrDefault("experimentNotes", "")
                val remoteId = cursor.getStringOrNull("remoteId")
                val syncStatus = cursor.getStringOrDefault("syncStatus", "SYNCED")
                val lastSyncedLong = cursor.getLongOrDefault("lastSyncedAt", 0L)
                val rawCreatedAt = cursor.getStringOrNull("createdAt")
                val rawUpdatedAt = cursor.getStringOrNull("updatedAt")

                val lastSyncedAtIso = if (lastSyncedLong > 0) Instant.ofEpochMilli(lastSyncedLong).toString() else null
                val createdAtIso = if (!rawCreatedAt.isNull_or_Empty()) rawCreatedAt else Instant.ofEpochMilli(1700000000000L).toString()
                val updatedAtIso = if (!rawUpdatedAt.isNull_or_Empty()) rawUpdatedAt else Instant.ofEpochMilli(1700000000000L).toString()
                val migrationStatus = "NEEDS_REVIEW"

                db.execSQL("""
                    INSERT OR REPLACE INTO lab_experiments_v6 (id, coffeeGrams, waterMl, ratio, temperatureC, grindSetting, estimatedTimeSeconds, experimentNotes, ownerUserId, schemaVersion, remoteId, syncStatus, serverVersion, expectedVersion, lastSyncedAt, createdAt, updatedAt, migrationStatus)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, 1, ?, ?, 1, 1, ?, ?, ?, ?)
                """.trimIndent(), arrayOf(id, coffeeGrams, waterMl, ratio, temp, clicks.toString(), estTime, notes, remoteId, syncStatus, lastSyncedAtIso, createdAtIso, updatedAtIso, migrationStatus))
            }
            cursor.close()
        }
        labTableCursor.close()

        db.execSQL("DROP TABLE IF EXISTS `lab_experiments` ")
        db.execSQL("ALTER TABLE `lab_experiments_v6` RENAME TO `lab_experiments` ")

        // 15. Clean legacy equipment and grinders tables
        db.execSQL("DROP TABLE IF EXISTS `equipment` ")
        db.execSQL("DROP TABLE IF EXISTS `grinders` ")
    }
}

private fun android.database.Cursor.getStringOrNull(colName: String): String? {
    val idx = this.getColumnIndex(colName)
    return if (idx >= 0 && !this.isNull(idx)) this.getString(idx) else null
}

private fun android.database.Cursor.getStringOrDefault(colName: String, defaultVal: String): String {
    val idx = this.getColumnIndex(colName)
    return if (idx >= 0 && !this.isNull(idx)) this.getString(idx) else defaultVal
}

private fun android.database.Cursor.getFloatOrDefault(colName: String, defaultVal: Float): Float {
    val idx = this.getColumnIndex(colName)
    return if (idx >= 0 && !this.isNull(idx)) this.getFloat(idx) else defaultVal
}

private fun android.database.Cursor.getIntOrDefault(colName: String, defaultVal: Int): Int {
    val idx = this.getColumnIndex(colName)
    return if (idx >= 0 && !this.isNull(idx)) this.getInt(idx) else defaultVal
}

private fun android.database.Cursor.getLongOrDefault(colName: String, defaultVal: Long): Long {
    val idx = this.getColumnIndex(colName)
    return if (idx >= 0 && !this.isNull(idx)) this.getLong(idx) else defaultVal
}

private fun android.database.Cursor.getDoubleOrNull(colName: String): Double? {
    val idx = this.getColumnIndex(colName)
    return if (idx >= 0 && !this.isNull(idx)) this.getDouble(idx) else null
}

private fun String?.isNull_or_Empty(): Boolean {
    return this == null || this.trim().isEmpty()
}

@Database(
    entities = [
        RatioPreset::class,
        RatioLastUsed::class,
        BrewMethod::class,
        UserMethodPreference::class,
        Bean::class,
        Instrument::class,
        GrinderProfile::class,
        GrinderMethodSetting::class,
        Technique::class,
        TechniqueStep::class,
        Recipe::class,
        RecipeIngredient::class,
        RecipeStep::class,
        RecipeTag::class,
        Cup::class,
        Cata::class,
        CataFlavorNote::class,
        LabExperiment::class
    ],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ratioPresetDao(): RatioPresetDao
    abstract fun ratioLastUsedDao(): RatioLastUsedDao
    abstract fun brewMethodDao(): BrewMethodDao
    abstract fun userMethodPreferenceDao(): UserMethodPreferenceDao
    abstract fun beanDao(): BeanDao
    abstract fun instrumentDao(): InstrumentDao
    abstract fun grinderProfileDao(): GrinderProfileDao
    abstract fun grinderMethodSettingDao(): GrinderMethodSettingDao
    abstract fun techniqueDao(): TechniqueDao
    abstract fun techniqueStepDao(): TechniqueStepDao
    abstract fun recipeDao(): RecipeDao
    abstract fun recipeIngredientDao(): RecipeIngredientDao
    abstract fun recipeStepDao(): RecipeStepDao
    abstract fun recipeTagDao(): RecipeTagDao
    abstract fun cataDao(): CataDao
    abstract fun cataFlavorNoteDao(): CataFlavorNoteDao
    abstract fun cupDao(): CupDao
    abstract fun labExperimentDao(): LabExperimentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "brew_studio_database_v2"
                )
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
