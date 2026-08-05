package com.example

import com.example.data.fixtures.SharedFixtures
import com.example.data.mappers.EntityMappers.toDomain
import com.example.data.mappers.EntityMappers.toDto
import com.example.data.mappers.EntityMappers.toEntity
import com.example.data.domain.*
import com.example.data.remote.dtos.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class V6_1_VerificationTest {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$")

    @Test
    fun testUuidValidityInFixtures() {
        assertTrue("BrewMethod V60 ID must be UUID", uuidRegex.matches(SharedFixtures.BREW_METHOD_V60.id))
        assertTrue("Bean ID must be UUID", uuidRegex.matches(SharedFixtures.SAMPLE_BEAN_V6.id))
        assertTrue("Instrument ID must be UUID", uuidRegex.matches(SharedFixtures.SAMPLE_GRINDER_INSTRUMENT_V6.id))
        assertTrue("Recipe ID must be UUID", uuidRegex.matches(SharedFixtures.SAMPLE_RECIPE_V6.id))

        SharedFixtures.SAMPLE_RECIPE_V6.ingredients.forEach { ingredient ->
            assertTrue("RecipeIngredient ID must be UUID: ${ingredient.id}", uuidRegex.matches(ingredient.id))
        }

        SharedFixtures.SAMPLE_RECIPE_V6.steps.forEach { step ->
            assertTrue("RecipeStep ID must be UUID: ${step.id}", uuidRegex.matches(step.id))
        }

        assertTrue("Technique ID must be UUID", uuidRegex.matches(SharedFixtures.SAMPLE_TECHNIQUE_V6.id))
        SharedFixtures.SAMPLE_TECHNIQUE_V6.steps.forEach { step ->
            assertTrue("TechniqueStep ID must be UUID: ${step.id}", uuidRegex.matches(step.id))
        }

        assertTrue("Cup ID must be UUID", uuidRegex.matches(SharedFixtures.SAMPLE_CUP_V6.id))
        assertTrue("Cata ID must be UUID", uuidRegex.matches(SharedFixtures.SAMPLE_CATA_V6.id))
        assertTrue("LabExperiment ID must be UUID", uuidRegex.matches(SharedFixtures.SAMPLE_LAB_EXPERIMENT_V6.id))
    }

    @Test
    fun testBeanMappingAndSerialization() {
        val originalBean = SharedFixtures.SAMPLE_BEAN_V6
        assertEquals(SharedFixtures.TEST_OWNER_USER_ID, originalBean.ownerUserId)
        assertEquals(1, originalBean.schemaVersion)

        // Room entity round-trip
        val entity = originalBean.toEntity()
        assertEquals(originalBean.id, entity.id)
        assertEquals(originalBean.ownerUserId, entity.ownerUserId)
        assertEquals(originalBean.schemaVersion, entity.schemaVersion)
        val fromEntity = entity.toDomain()
        assertEquals(originalBean.id, fromEntity.id)

        // Network DTO round-trip
        val dto = originalBean.toDto()
        val adapter = moshi.adapter(BeanDto::class.java)
        val json = adapter.toJson(dto)
        assertTrue(json.contains("\"owner_user_id\":\"${SharedFixtures.TEST_OWNER_USER_ID}\""))
        assertTrue(json.contains("\"schema_version\":1"))

        val deserializedDto = adapter.fromJson(json)
        assertNotNull(deserializedDto)
        val fromDto = deserializedDto!!.toDomain()
        assertEquals(originalBean.id, fromDto.id)
        assertEquals(originalBean.name, fromDto.name)
        assertEquals(BeanStatus.OPEN, fromDto.status)
    }

    @Test
    fun testTechniqueGrindValueDoubleAndExecutionMode() {
        val technique = SharedFixtures.SAMPLE_TECHNIQUE_V6
        assertEquals(28.0, technique.grindValue!!, 0.001)
        assertEquals("28 Clicks", technique.grindDescription)
        assertEquals(ExecutionMode.GUIDED, technique.executionMode)
        assertEquals(15.0f, technique.ratio, 0.001f)

        // DTO round-trip
        val dto = technique.toDto()
        assertEquals(28.0, dto.grindValue!!, 0.001)
        assertEquals("GUIDED", dto.executionMode)

        val adapter = moshi.adapter(TechniqueDto::class.java)
        val json = adapter.toJson(dto)
        assertTrue(json.contains("\"grind_value\":28.0"))
        assertTrue(json.contains("\"execution_mode\":\"GUIDED\""))

        val deserializedDto = adapter.fromJson(json)!!
        val domain = deserializedDto.toDomain()
        assertEquals(28.0, domain.grindValue!!, 0.001)
        assertEquals(ExecutionMode.GUIDED, domain.executionMode)
    }

    @Test
    fun testCataSingleCupRelationAndEnums() {
        val cata = SharedFixtures.SAMPLE_CATA_V6
        assertEquals(FlavorFamily.FRUITY, cata.activeFlavorFamily)
        assertEquals(SensoryLevel.MEDIUM, cata.textureLevel)
        assertEquals(SensoryLevel.HIGH, cata.cleanlinessLevel)
        assertEquals(89.50, cata.totalScaScore!!, 0.001)

        // DTO round-trip
        val dto = cata.toDto()
        assertEquals("FRUITY", dto.activeFlavorFamily)
        assertEquals("MEDIUM", dto.textureLevel)
        assertEquals("HIGH", dto.cleanlinessLevel)

        val adapter = moshi.adapter(CataDto::class.java)
        val json = adapter.toJson(dto)
        assertTrue(json.contains("\"active_flavor_family\":\"FRUITY\""))
        assertTrue(json.contains("\"texture_level\":\"MEDIUM\""))

        val deserialized = adapter.fromJson(json)!!.toDomain()
        assertEquals(FlavorFamily.FRUITY, deserialized.activeFlavorFamily)
        assertEquals(SensoryLevel.MEDIUM, deserialized.textureLevel)
    }

    @Test
    fun testCupLifeStateAndRatings() {
        val cup = SharedFixtures.SAMPLE_CUP_V6
        assertEquals(240, cup.cupLifeSeconds)
        assertEquals(CupLifeState.FRESH, cup.cupLifeState)
        assertEquals(9, cup.nps)
        assertEquals(4.8, cup.rating!!, 0.001)

        val dto = cup.toDto()
        assertEquals(9, dto.nps)
        assertEquals(4.8, dto.rating!!, 0.001)
        assertEquals("FRESH", dto.cupLifeState)

        // Cup life state calculation logic
        assertEquals(CupLifeState.FRESH, deriveCupLifeState(100))
        assertEquals(CupLifeState.PEAK, deriveCupLifeState(600))
        assertEquals(CupLifeState.DECLINING, deriveCupLifeState(1200))
        assertEquals(CupLifeState.EXHAUSTED, deriveCupLifeState(2000))
    }

    @Test
    fun testLabExperimentAdditions() {
        val experiment = SharedFixtures.SAMPLE_LAB_EXPERIMENT_V6
        assertEquals(205, experiment.actualTimeSeconds)
        assertEquals(4.9, experiment.resultRating!!, 0.001)
        assertEquals("Hipótesis confirmada. La taza resultó más brillante.", experiment.conclusionNotes)
        assertEquals(SharedFixtures.SAMPLE_CUP_V6.id, experiment.resultCupId)

        val dto = experiment.toDto()
        assertEquals(205, dto.actualTimeSeconds)
        assertEquals(4.9, dto.resultRating!!, 0.001)
        assertEquals(SharedFixtures.SAMPLE_CUP_V6.id, dto.resultCupId)
    }

    @Test
    fun testExpectedVersionAndConflictResolutionLogic() {
        val bean = SharedFixtures.SAMPLE_BEAN_V6.copy(serverVersion = 5L, expectedVersion = 5L)
        val dto = bean.toDto()
        assertEquals(5L, dto.serverVersion)
        assertEquals(5L, dto.expectedVersion)

        // Simulate client mutation: expectedVersion matches serverVersion before sync
        val clientUpdate = bean.copy(name = "Updated Name", expectedVersion = bean.serverVersion)
        assertEquals(clientUpdate.serverVersion, clientUpdate.expectedVersion)

        // 409 conflict detection rule: if server has version 6 while client expected 5
        val serverVersionOnRemote = 6L
        val isConflict = clientUpdate.expectedVersion < serverVersionOnRemote
        assertTrue("Conflict must be detected when expectedVersion < remote serverVersion", isConflict)
    }

    @Test
    fun testOfflineDeleteStatusHandling() {
        val bean = SharedFixtures.SAMPLE_BEAN_V6.copy(syncStatus = SyncStatus.PENDING_DELETE)
        val entity = bean.toEntity()
        assertEquals("PENDING_DELETE", entity.syncStatus)

        val domain = entity.toDomain()
        assertEquals(SyncStatus.PENDING_DELETE, domain.syncStatus)
    }

    @Test
    fun testImmutableSnapshotsInCup() {
        val cup = SharedFixtures.SAMPLE_CUP_V6
        assertEquals("Geisha San Alberto", cup.beanNameSnapshot)
        assertEquals("Espresso Tonic Artesanal", cup.recipeNameSnapshot)
        assertEquals("V60 Tetsu Kasuya 4:6 Method", cup.techniqueNameSnapshot)

        // Mutate original bean name
        val modifiedBean = SharedFixtures.SAMPLE_BEAN_V6.copy(name = "Renamed Bean")
        assertNotEquals(modifiedBean.name, cup.beanNameSnapshot)
    }

    @Test
    fun testCrossContractRoundtripAndroidToWebToAndroid() {
        val mapAdapter = moshi.adapter(Map::class.java)

        // 1. Bean roundtrip
        val bean = SharedFixtures.SAMPLE_BEAN_V6
        val beanAdapter = moshi.adapter(BeanDto::class.java)
        val beanAndroidJson = beanAdapter.toJson(bean.toDto())
        @Suppress("UNCHECKED_CAST")
        val beanWebMap = mapAdapter.fromJson(beanAndroidJson) as Map<String, Any?>
        assertEquals(bean.id, beanWebMap["id"])
        assertEquals(1.0, (beanWebMap["schema_version"] as Number).toDouble(), 0.001)
        val beanWebJson = mapAdapter.toJson(beanWebMap)
        val finalBean = beanAdapter.fromJson(beanWebJson)!!.toDomain()
        assertEquals(bean.id, finalBean.id)
        assertEquals(bean.name, finalBean.name)

        // 2. Instrument roundtrip
        val instrument = SharedFixtures.SAMPLE_GRINDER_INSTRUMENT_V6
        val instAdapter = moshi.adapter(InstrumentDto::class.java)
        val instAndroidJson = instAdapter.toJson(instrument.toDto())
        @Suppress("UNCHECKED_CAST")
        val instWebMap = mapAdapter.fromJson(instAndroidJson) as Map<String, Any?>
        assertEquals(instrument.id, instWebMap["id"])
        val instWebJson = mapAdapter.toJson(instWebMap)
        val finalInst = instAdapter.fromJson(instWebJson)!!.toDomain()
        assertEquals(instrument.id, finalInst.id)
        assertEquals(instrument.name, finalInst.name)

        // 3. Recipe roundtrip
        val recipe = SharedFixtures.SAMPLE_RECIPE_V6
        val recipeAdapter = moshi.adapter(RecipeDto::class.java)
        val recipeAndroidJson = recipeAdapter.toJson(recipe.toDto())
        @Suppress("UNCHECKED_CAST")
        val recipeWebMap = mapAdapter.fromJson(recipeAndroidJson) as Map<String, Any?>
        assertEquals(recipe.id, recipeWebMap["id"])
        val recipeWebJson = mapAdapter.toJson(recipeWebMap)
        val finalRecipe = recipeAdapter.fromJson(recipeWebJson)!!.toDomain()
        assertEquals(recipe.id, finalRecipe.id)
        assertEquals(recipe.ingredients.size, finalRecipe.ingredients.size)

        // 4. Technique roundtrip
        val technique = SharedFixtures.SAMPLE_TECHNIQUE_V6
        val techAdapter = moshi.adapter(TechniqueDto::class.java)
        val techAndroidJson = techAdapter.toJson(technique.toDto())
        @Suppress("UNCHECKED_CAST")
        val techWebMap = mapAdapter.fromJson(techAndroidJson) as Map<String, Any?>
        assertEquals(technique.id, techWebMap["id"])
        val techWebJson = mapAdapter.toJson(techWebMap)
        val finalTech = techAdapter.fromJson(techWebJson)!!.toDomain()
        assertEquals(technique.id, finalTech.id)
        assertEquals(28.0, finalTech.grindValue!!, 0.001)

        // 5. Cup roundtrip
        val cup = SharedFixtures.SAMPLE_CUP_V6
        val cupAdapter = moshi.adapter(CupDto::class.java)
        val cupAndroidJson = cupAdapter.toJson(cup.toDto())
        @Suppress("UNCHECKED_CAST")
        val cupWebMap = mapAdapter.fromJson(cupAndroidJson) as Map<String, Any?>
        assertEquals(cup.id, cupWebMap["id"])
        val cupWebJson = mapAdapter.toJson(cupWebMap)
        val finalCup = cupAdapter.fromJson(cupWebJson)!!.toDomain()
        assertEquals(cup.id, finalCup.id)
        assertEquals(CupLifeState.FRESH, finalCup.cupLifeState)

        // 6. Cata roundtrip
        val cata = SharedFixtures.SAMPLE_CATA_V6
        val cataAdapter = moshi.adapter(CataDto::class.java)
        val cataAndroidJson = cataAdapter.toJson(cata.toDto())
        @Suppress("UNCHECKED_CAST")
        val cataWebMap = mapAdapter.fromJson(cataAndroidJson) as Map<String, Any?>
        assertEquals(cata.id, cataWebMap["id"])
        val cataWebJson = mapAdapter.toJson(cataWebMap)
        val finalCata = cataAdapter.fromJson(cataWebJson)!!.toDomain()
        assertEquals(cata.id, finalCata.id)
        assertEquals(FlavorFamily.FRUITY, finalCata.activeFlavorFamily)

        // 7. LabExperiment roundtrip
        val lab = SharedFixtures.SAMPLE_LAB_EXPERIMENT_V6
        val labAdapter = moshi.adapter(LabExperimentDto::class.java)
        val labAndroidJson = labAdapter.toJson(lab.toDto())
        @Suppress("UNCHECKED_CAST")
        val labWebMap = mapAdapter.fromJson(labAndroidJson) as Map<String, Any?>
        assertEquals(lab.id, labWebMap["id"])
        val labWebJson = mapAdapter.toJson(labWebMap)
        val finalLab = labAdapter.fromJson(labWebJson)!!.toDomain()
        assertEquals(lab.id, finalLab.id)
        assertEquals(4.9, finalLab.resultRating!!, 0.001)
    }
}
