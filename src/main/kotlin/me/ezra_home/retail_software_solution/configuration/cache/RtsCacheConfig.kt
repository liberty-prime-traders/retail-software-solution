package me.ezra_home.retail_software_solution.configuration.cache

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.configuration.util.typeadapters.HibernateProxyTypeAdapter
import me.ezra_home.retail_software_solution.configuration.util.typeadapters.OffsetDateTimeAdapter
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.OffsetDateTime


@Configuration
@EnableCaching
class RtsCacheConfig : CachingConfigurer {

    companion object {
        private val GSON: Gson = GsonBuilder()
            .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY)
            .registerTypeAdapter(OffsetDateTime::class.java, OffsetDateTimeAdapter())
            .create()
    }

    @Bean
    override fun keyGenerator(): KeyGenerator {
        return KeyGenerator { target, method, params ->
            val schemaName = getSchemaName(target)
            val jsonParams = GSON.toJson(params)
            "${schemaName}:${target::class.simpleName ?: "Unknown"}:${method.name}:${jsonParams}"
        }
    }

    private fun getSchemaName(target: Any): String {
        val schemaLevel = target::class.annotations.find { it is CacheSchemaLevel }
            ?.let {it as CacheSchemaLevel}
            ?.schemaLevel
        val default = DataSourceBeanNames.PLATFORM_SCHEMA_NAME
        if (schemaLevel == null) {
            return default
        }
        val schemaName = when (schemaLevel) {
            SchemaLevel.PLATFORM -> default
            SchemaLevel.ORGANIZATION -> SessionContextProvider.getSession().organizationSchemaName
            SchemaLevel.LOCATION -> SessionContextProvider.getSession().locationSchemaName
        }
        return schemaName ?: default
    }
}
