package com.example.data.engine

import com.example.data.remote.dtos.CataFlavorNoteDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class CataDraftEncodingTest {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    @Test fun notesBecomeValidBackendJsonWithoutDuplicates() {
        val type = Types.newParameterizedType(List::class.java, CataFlavorNoteDto::class.java)
        val decoded = moshi.adapter<List<CataFlavorNoteDto>>(type)
            .fromJson(CataDraftEncoding.flavorNotesJson("Fresa, cacao; FRESA"))!!
        assertEquals(listOf("Fresa", "cacao"), decoded.map { it.note })
        assertEquals(listOf("FRUITY", "FRUITY"), decoded.map { it.family })
    }

    @Test fun expectedDescriptorsAndSensoryLevelsMatchContract() {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val decoded = moshi.adapter<List<String>>(type)
            .fromJson(CataDraftEncoding.descriptorsJson("Floral • panela\nCítrico"))!!
        assertEquals(listOf("Floral", "panela", "Cítrico"), decoded)
        assertEquals("LOW", CataDraftEncoding.sensoryLevel("corta"))
        assertEquals("MEDIUM", CataDraftEncoding.sensoryLevel("sedosa"))
        assertEquals("HIGH", CataDraftEncoding.sensoryLevel("muy alta"))
    }
}
