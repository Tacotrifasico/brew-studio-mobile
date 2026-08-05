package com.example.data.contracts

object JsonSchemas {

    val BREW_METHOD_SCHEMA_V6 = """
    {
      "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
      "title": "BrewMethodV6_1",
      "type": "object",
      "required": ["id", "code", "name_key", "category", "default_ratio", "schema_version"],
      "properties": {
        "id": { "type": "string", "pattern": "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}${'$'}" },
        "code": { "type": "string" },
        "name_key": { "type": "string" },
        "category": { "type": "string", "enum": ["POUR_OVER", "IMMERSION", "PRESSURE", "COLD", "HYBRID", "OTHER"] },
        "default_ratio": { "type": "number", "minimum": 0 },
        "owner_user_id": { "type": ["string", "null"] },
        "schema_version": { "type": "integer", "const": 1 }
      },
      "additionalProperties": true
    }
    """.trimIndent()

    val BEAN_SCHEMA_V6 = """
    {
      "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
      "title": "BeanV6_1",
      "type": "object",
      "required": [
        "id", "roaster", "name", "origin", "altitude", "process",
        "roast_date", "first_use_date", "notes", "status", "stock_grams",
        "schema_version", "server_version", "expected_version", "created_at", "updated_at", "migration_status"
      ],
      "properties": {
        "id": { "type": "string", "pattern": "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}${'$'}" },
        "roaster": { "type": "string" },
        "name": { "type": "string" },
        "origin": { "type": "string" },
        "altitude": { "type": "string" },
        "process": { "type": "string" },
        "roast_date": { "type": "string" },
        "first_use_date": { "type": "string" },
        "notes": { "type": "string" },
        "status": { "type": "string", "enum": ["CLOSED", "OPEN", "FINISHED"] },
        "stock_grams": { "type": "number", "minimum": 0 },
        "owner_user_id": { "type": ["string", "null"] },
        "schema_version": { "type": "integer", "const": 1 },
        "server_version": { "type": "integer" },
        "expected_version": { "type": "integer" },
        "last_synced_at": { "type": ["string", "null"] },
        "created_at": { "type": "string" },
        "updated_at": { "type": "string" },
        "migration_status": { "type": "string", "enum": ["MIGRATED", "NEEDS_REVIEW"] }
      },
      "additionalProperties": true
    }
    """.trimIndent()

    val INSTRUMENT_SCHEMA_V6 = """
    {
      "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
      "title": "InstrumentV6_1",
      "type": "object",
      "required": [
        "id", "name", "type", "brand", "model", "notes",
        "schema_version", "server_version", "expected_version", "created_at", "updated_at", "migration_status"
      ],
      "properties": {
        "id": { "type": "string", "pattern": "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}${'$'}" },
        "name": { "type": "string" },
        "type": { "type": "string", "enum": ["GRINDER", "BREWER_METHOD", "SCALE", "KETTLE", "FILTERS", "SERVER", "PRESS", "ACCESSORY", "OTHER"] },
        "brand": { "type": "string" },
        "model": { "type": "string" },
        "notes": { "type": "string" },
        "grinder_profile": { "type": ["object", "null"] },
        "owner_user_id": { "type": ["string", "null"] },
        "schema_version": { "type": "integer", "const": 1 },
        "server_version": { "type": "integer" },
        "expected_version": { "type": "integer" },
        "last_synced_at": { "type": ["string", "null"] },
        "created_at": { "type": "string" },
        "updated_at": { "type": "string" },
        "migration_status": { "type": "string", "enum": ["MIGRATED", "NEEDS_REVIEW"] }
      },
      "additionalProperties": true
    }
    """.trimIndent()

    val RECIPE_SCHEMA_V6 = """
    {
      "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
      "title": "RecipeV6_1",
      "type": "object",
      "required": [
        "id", "name", "recipe_kind", "intention", "is_favorite",
        "ingredients", "steps", "tags", "visibility", "is_shared", "copy_mode",
        "schema_version", "server_version", "expected_version", "created_at", "updated_at", "migration_status"
      ],
      "properties": {
        "id": { "type": "string", "pattern": "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}${'$'}" },
        "name": { "type": "string" },
        "recipe_kind": { "type": "string", "enum": ["BLACK_COFFEE", "MILK_DRINK", "COLD_DRINK", "SIGNATURE", "DESSERT", "OTHER"] },
        "intention": { "type": "string" },
        "suggested_method_id": { "type": ["string", "null"] },
        "is_favorite": { "type": "boolean" },
        "ingredients": { "type": "array" },
        "steps": { "type": "array" },
        "tags": { "type": "array" },
        "owner_user_id": { "type": ["string", "null"] },
        "owner_display_name": { "type": ["string", "null"] },
        "visibility": { "type": "string", "enum": ["PRIVATE", "PUBLIC", "UNLISTED"] },
        "is_shared": { "type": "boolean" },
        "original_author_user_id": { "type": ["string", "null"] },
        "original_author_name": { "type": ["string", "null"] },
        "original_entity_id": { "type": ["string", "null"] },
        "root_entity_id": { "type": ["string", "null"] },
        "imported_from_share_id": { "type": ["string", "null"] },
        "copy_mode": { "type": "string", "enum": ["ORIGINAL", "FORK", "SHARED_COPY"] },
        "origin_created_at": { "type": ["string", "null"] },
        "schema_version": { "type": "integer", "const": 1 },
        "server_version": { "type": "integer" },
        "expected_version": { "type": "integer" },
        "last_synced_at": { "type": ["string", "null"] },
        "created_at": { "type": "string" },
        "updated_at": { "type": "string" },
        "migration_status": { "type": "string", "enum": ["MIGRATED", "NEEDS_REVIEW"] }
      },
      "additionalProperties": true
    }
    """.trimIndent()

    val TECHNIQUE_SCHEMA_V6 = """
    {
      "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
      "title": "TechniqueV6_1",
      "type": "object",
      "required": [
        "id", "name", "method_id", "dose_g", "water_ml", "ratio", "temperature_c",
        "execution_mode", "grind_unit", "total_time_seconds", "steps",
        "schema_version", "server_version", "expected_version", "created_at", "updated_at", "migration_status"
      ],
      "properties": {
        "id": { "type": "string", "pattern": "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}${'$'}" },
        "name": { "type": "string" },
        "method_id": { "type": "string" },
        "recipe_id": { "type": ["string", "null"] },
        "bean_id": { "type": ["string", "null"] },
        "grinder_id": { "type": ["string", "null"] },
        "dose_g": { "type": "number" },
        "water_ml": { "type": "integer" },
        "ratio": { "type": "number" },
        "temperature_c": { "type": "integer" },
        "execution_mode": { "type": "string", "enum": ["GUIDED", "MANUAL", "TIMER_ONLY", "AUTOMATED"] },
        "grind_value": { "type": ["number", "null"] },
        "grind_description": { "type": ["string", "null"] },
        "grind_unit": { "type": "string", "enum": ["CLICKS", "MICRONS", "SETTING_NUMERIC", "DESCRIPTIVE"] },
        "notes": { "type": "string" },
        "total_time_seconds": { "type": "integer" },
        "coverage": { "type": ["number", "null"] },
        "flow": { "type": ["number", "null"] },
        "secondary_action": { "type": ["string", "null"] },
        "position": { "type": ["integer", "null"] },
        "target_water_ml": { "type": ["integer", "null"] },
        "steps": { "type": "array" },
        "owner_user_id": { "type": ["string", "null"] },
        "schema_version": { "type": "integer", "const": 1 },
        "server_version": { "type": "integer" },
        "expected_version": { "type": "integer" },
        "last_synced_at": { "type": ["string", "null"] },
        "created_at": { "type": "string" },
        "updated_at": { "type": "string" },
        "migration_status": { "type": "string", "enum": ["MIGRATED", "NEEDS_REVIEW"] }
      },
      "additionalProperties": true
    }
    """.trimIndent()

    val CUP_SCHEMA_V6 = """
    {
      "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
      "title": "CupV6_1",
      "type": "object",
      "required": [
        "id", "executed_dose_g", "executed_water_ml", "executed_ratio", "executed_temperature_c",
        "executed_grind_setting", "executed_duration_seconds", "cup_life_seconds", "cup_life_state",
        "brew_date", "schema_version", "server_version", "expected_version", "created_at", "updated_at", "migration_status"
      ],
      "properties": {
        "id": { "type": "string", "pattern": "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}${'$'}" },
        "recipe_id": { "type": ["string", "null"] },
        "bean_id": { "type": ["string", "null"] },
        "technique_id": { "type": ["string", "null"] },
        "method_id": { "type": ["string", "null"] },
        "grinder_id": { "type": ["string", "null"] },
        "executed_dose_g": { "type": "number" },
        "executed_water_ml": { "type": "integer" },
        "executed_ratio": { "type": "number" },
        "executed_temperature_c": { "type": "integer" },
        "executed_grind_setting": { "type": "string" },
        "executed_duration_seconds": { "type": "integer" },
        "cup_life_seconds": { "type": "integer" },
        "cup_life_state": { "type": "string", "enum": ["FRESH", "PEAK", "DECLINING", "EXHAUSTED"] },
        "nps": { "type": ["integer", "null"], "minimum": 0, "maximum": 10 },
        "rating": { "type": ["number", "null"], "minimum": 1.0, "maximum": 5.0 },
        "comment": { "type": "string" },
        "brew_date": { "type": "string" },
        "cata": { "type": ["object", "null"] },
        "owner_user_id": { "type": ["string", "null"] },
        "schema_version": { "type": "integer", "const": 1 },
        "server_version": { "type": "integer" },
        "expected_version": { "type": "integer" },
        "last_synced_at": { "type": ["string", "null"] },
        "created_at": { "type": "string" },
        "updated_at": { "type": "string" },
        "migration_status": { "type": "string", "enum": ["MIGRATED", "NEEDS_REVIEW"] }
      },
      "additionalProperties": true
    }
    """.trimIndent()

    val CATA_SCHEMA_V6 = """
    {
      "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
      "title": "CataV6_1",
      "type": "object",
      "required": [
        "id", "selected_flavor_notes", "sensory_wheel_descriptors", "evaluator_notes", "evaluated_at",
        "schema_version", "server_version", "expected_version", "created_at", "updated_at", "migration_status"
      ],
      "properties": {
        "id": { "type": "string", "pattern": "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}${'$'}" },
        "cup_id": { "type": ["string", "null"] },
        "recipe_id": { "type": ["string", "null"] },
        "bean_id": { "type": ["string", "null"] },
        "active_flavor_family": { "type": ["string", "null"], "enum": ["FLORAL", "FRUITY", "CITRIC", "SWEET", "CACAO", "NUTTY", "SPICED", "GREEN", null] },
        "selected_flavor_notes": { "type": "array" },
        "sensory_wheel_descriptors": { "type": "array" },
        "texture_level": { "type": ["string", "null"], "enum": ["LOW", "MEDIUM_LOW", "MEDIUM", "MEDIUM_HIGH", "HIGH", null] },
        "cleanliness_level": { "type": ["string", "null"], "enum": ["LOW", "MEDIUM_LOW", "MEDIUM", "MEDIUM_HIGH", "HIGH", null] },
        "persistence_level": { "type": ["string", "null"], "enum": ["LOW", "MEDIUM_LOW", "MEDIUM", "MEDIUM_HIGH", "HIGH", null] },
        "sweetness_level": { "type": ["string", "null"], "enum": ["LOW", "MEDIUM_LOW", "MEDIUM", "MEDIUM_HIGH", "HIGH", null] },
        "acidity_level": { "type": ["string", "null"], "enum": ["LOW", "MEDIUM_LOW", "MEDIUM", "MEDIUM_HIGH", "HIGH", null] },
        "balance_level": { "type": ["string", "null"], "enum": ["LOW", "MEDIUM_LOW", "MEDIUM", "MEDIUM_HIGH", "HIGH", null] },
        "fragrance_aroma_score": { "type": ["number", "null"] },
        "flavor_score": { "type": ["number", "null"] },
        "aftertaste_score": { "type": ["number", "null"] },
        "acidity_score": { "type": ["number", "null"] },
        "body_score": { "type": ["number", "null"] },
        "uniformity_score": { "type": ["number", "null"] },
        "clean_cup_score": { "type": ["number", "null"] },
        "sweetness_score": { "type": ["number", "null"] },
        "balance_score": { "type": ["number", "null"] },
        "overall_score": { "type": ["number", "null"] },
        "total_sca_score": { "type": ["number", "null"] },
        "evaluator_notes": { "type": "string" },
        "evaluated_at": { "type": "string" },
        "owner_user_id": { "type": ["string", "null"] },
        "schema_version": { "type": "integer", "const": 1 },
        "server_version": { "type": "integer" },
        "expected_version": { "type": "integer" },
        "last_synced_at": { "type": ["string", "null"] },
        "created_at": { "type": "string" },
        "updated_at": { "type": "string" },
        "migration_status": { "type": "string", "enum": ["MIGRATED", "NEEDS_REVIEW"] }
      },
      "additionalProperties": true
    }
    """.trimIndent()

    val LAB_EXPERIMENT_SCHEMA_V6 = """
    {
      "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
      "title": "LabExperimentV6_1",
      "type": "object",
      "required": [
        "id", "coffee_grams", "water_ml", "ratio", "temperature_c", "grind_setting",
        "estimated_time_seconds", "experiment_hypothesis", "experiment_notes", "conclusion_notes",
        "schema_version", "server_version", "expected_version", "created_at", "updated_at", "migration_status"
      ],
      "properties": {
        "id": { "type": "string", "pattern": "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}${'$'}" },
        "method_id": { "type": ["string", "null"] },
        "bean_id": { "type": ["string", "null"] },
        "grinder_id": { "type": ["string", "null"] },
        "technique_id": { "type": ["string", "null"] },
        "coffee_grams": { "type": "number" },
        "water_ml": { "type": "integer" },
        "ratio": { "type": "number" },
        "temperature_c": { "type": "integer" },
        "grind_setting": { "type": "string" },
        "bean_freshness_days": { "type": ["integer", "null"] },
        "estimated_time_seconds": { "type": "integer" },
        "actual_time_seconds": { "type": ["integer", "null"] },
        "experiment_hypothesis": { "type": "string" },
        "experiment_notes": { "type": "string" },
        "conclusion_notes": { "type": "string" },
        "result_rating": { "type": ["number", "null"] },
        "result_cup_id": { "type": ["string", "null"] },
        "owner_user_id": { "type": ["string", "null"] },
        "schema_version": { "type": "integer", "const": 1 },
        "server_version": { "type": "integer" },
        "expected_version": { "type": "integer" },
        "last_synced_at": { "type": ["string", "null"] },
        "created_at": { "type": "string" },
        "updated_at": { "type": "string" },
        "migration_status": { "type": "string", "enum": ["MIGRATED", "NEEDS_REVIEW"] }
      },
      "additionalProperties": true
    }
    """.trimIndent()
}
