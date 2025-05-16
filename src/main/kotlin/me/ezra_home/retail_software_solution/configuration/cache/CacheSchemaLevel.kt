package me.ezra_home.retail_software_solution.configuration.cache

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheSchemaLevel(
    val schemaLevel: SchemaLevel = SchemaLevel.PLATFORM
)
