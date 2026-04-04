package me.ezra_home.retail_software_solution.cucumber.support.cleanup

import jakarta.annotation.PostConstruct
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.beans.factory.getBeansWithAnnotation
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheConfig
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils

@Component
class CacheCleaner(
    private val applicationContext: ApplicationContext,
    private val cacheManager: CacheManager,
) {
    private lateinit var transientCacheNames: Set<String>

    @PostConstruct
    fun resolveTransientCaches() {
        transientCacheNames = applicationContext.getBeansWithAnnotation<CacheSchemaLevel>().values
            .map { ClassUtils.getUserClass(it) }
            .filter { type ->
                val level = type.getAnnotation(CacheSchemaLevel::class.java)?.schemaLevel
                level == SchemaLevel.ORGANIZATION || level == SchemaLevel.LOCATION
            }
            .flatMap { type ->
                type.getAnnotation(CacheConfig::class.java)?.cacheNames?.toList() ?: emptyList()
            }
            .toSet()
    }

    fun clearAllCaches() {
        transientCacheNames.forEach { cacheManager.getCache(it)?.clear() }
    }
}
