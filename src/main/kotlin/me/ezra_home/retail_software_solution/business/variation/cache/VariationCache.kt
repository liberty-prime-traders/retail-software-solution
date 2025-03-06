package me.ezra_home.retail_software_solution.business.variation.cache

import me.ezra_home.retail_software_solution.model.entity.VariationEntity
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class VariationCache {
    private val cache = ConcurrentHashMap<String, VariationEntity>()

    fun put(key: String, variation: VariationEntity) {
        cache[key] = variation
    }

    fun get(key: String): VariationEntity? {
        return cache[key]
    }

    fun remove(key: String) {
        cache.remove(key)
    }

    fun clear() {
        cache.clear()
    }

}