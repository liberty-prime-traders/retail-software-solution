package me.ezra_home.retail_software_solution.configuration.cache

import me.ezra_home.retail_software_solution.util.enums.SchemaLevel

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheSchemaLevel(
    val schemaLevel: SchemaLevel = SchemaLevel.PLATFORM
)
