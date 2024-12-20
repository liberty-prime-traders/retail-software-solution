package me.ezra_home.retail_software_solution.configuration.cache

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import me.ezra_home.retail_software_solution.configuration.util.typeadapters.HibernateProxyTypeAdapter
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
@EnableCaching
class RtsCacheConfig : CachingConfigurer {

    companion object {
        private val GSON: Gson = GsonBuilder()
            .registerTypeAdapterFactory(HibernateProxyTypeAdapter.FACTORY)
            .create()
    }

    @Bean
    override fun keyGenerator(): KeyGenerator {
        return KeyGenerator { target, method, params ->
            val jsonParams = GSON.toJson(params)
            "${target::class.simpleName ?: "Unknown"}:${method.name}:${jsonParams}"
        }
    }
}
