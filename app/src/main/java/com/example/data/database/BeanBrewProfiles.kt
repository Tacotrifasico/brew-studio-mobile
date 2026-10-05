package com.example.data.database

import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

/** Shared iOS/Android wire format. Temperature is ALWAYS Celsius, regardless of display units.
 * AXCIS: map this bean-owned JSON object to beans.brew_profiles; never put it in notes.
 */
data class BeanBrewProfile(val methodName: String, val clicks: Int = 18, val temperatureC: Int = 93)

object BeanBrewProfiles {
    /** Configurable starting points, not universal grinder calibration or saved favorites. */
    fun startingPoint(method: String): BeanBrewProfile {
        val (clicks, temperature) = when (key(method)) {
            "v60" -> 22 to 92
            "aeropress" -> 18 to 88
            "prensa francesa" -> 28 to 94
            "chemex" -> 26 to 93
            "espresso" -> 8 to 93
            "moka" -> 12 to 90
            "cold brew" -> 32 to 20
            else -> 18 to 93
        }
        return BeanBrewProfile(method, clicks, temperature)
    }
    fun resolve(json: String, method: String): BeanBrewProfile = read(json, method) ?: startingPoint(method)

    fun key(method: String): String = Normalizer.normalize(method.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "").lowercase(Locale.ROOT)

    fun read(json: String, method: String): BeanBrewProfile? = runCatching {
        val item = JSONObject(json).optJSONObject(key(method)) ?: return null
        BeanBrewProfile(item.getString("methodName"), item.getInt("clicks").coerceIn(1, 200),
            item.getInt("temperatureC").coerceIn(1, 100))
    }.getOrNull()

    fun write(json: String, profile: BeanBrewProfile): String {
        // Refuse malformed data instead of silently erasing associations for other methods.
        val root = JSONObject(json)
        root.put(key(profile.methodName), JSONObject().put("methodName", profile.methodName)
            .put("clicks", profile.clicks.coerceIn(1, 200))
            .put("temperatureC", profile.temperatureC.coerceIn(1, 100)))
        return root.toString()
    }
}
