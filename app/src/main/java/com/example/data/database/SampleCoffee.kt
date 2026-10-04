package com.example.data.database

import java.time.LocalDate

/** Local-only sample, same stable ID/data as iOS. Never overwrite its saved method profiles. */
object SampleCoffee {
    const val ID = "524f4e50-4f54-4520-8000-000000000001"
    fun bean(today: LocalDate = LocalDate.now()): Bean {
        var profiles = "{}"
        listOf(BeanBrewProfile("V60", 22, 92), BeanBrewProfile("AeroPress", 18, 88),
            BeanBrewProfile("Prensa francesa", 28, 94)).forEach { profiles = BeanBrewProfiles.write(profiles, it) }
        return Bean(id = ID, name = "Ronpotrero", roaster = "Tostadores del Potrero (muestra)",
            origin = "Chiapas, México", altitude = "1700", process = "Lavado",
            roastDate = today.minusDays(7).toString(), firstUseDate = today.toString(),
            notes = "Café de muestra · Datos ficticios. Finca El Potrero; Bourbon; tueste medio. Chocolate, panela y naranja.",
            stockGrams = 250f, status = "abierto", brewProfilesJSON = profiles, ownerUserId = null, syncStatus = "SYNCED")
    }
}

val Bean.isSample: Boolean get() = id == SampleCoffee.ID
