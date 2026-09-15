package com.example.data.engine

import com.example.data.remote.dtos.CataFlavorNoteDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object CataDraftEncoding {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val flavorAdapter = moshi.adapter<List<CataFlavorNoteDto>>(
        Types.newParameterizedType(List::class.java, CataFlavorNoteDto::class.java)
    )
    private val descriptorAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    fun flavorNotesJson(raw: String, family: String = "FRUITY"): String =
        flavorAdapter.toJson(tokens(raw).map { CataFlavorNoteDto(note = it, family = family) })

    fun descriptorsJson(raw: String): String = descriptorAdapter.toJson(tokens(raw))

    fun sensoryLevel(value: String): String = when (value.trim().lowercase()) {
        "baja", "ligera", "seca", "corta" -> "LOW"
        "alta", "muy alta", "densa", "larga" -> "HIGH"
        else -> "MEDIUM"
    }

    private fun tokens(raw: String): List<String> = raw
        .split(',', ';', '•', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }
}
