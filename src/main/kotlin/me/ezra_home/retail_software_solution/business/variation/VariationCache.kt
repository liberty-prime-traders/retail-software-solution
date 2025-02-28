package me.ezra_home.retail_software_solution.business.variation

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.List
import java.util.concurrent.TimeUnit

@Component
class VariationCache(
    private val variationRepository: VariationRepository
) {
    private val cache: Cache<UUID, VariationEntity> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(100)
        .build()

    fun getVariation(variationId: UUID): VariationEntity? {
        return cache.get(variationId) { key ->
            variationRepository.findByVariationIdAndIsActiveTrue(key)
        }
    }

    fun getAllVariations(): List<VariationEntity> {
        return variationRepository.findByIsActiveTrue()
    }

    fun invalidateCache(variationId: UUID) {
        cache.invalidate(variationId)
    }

    fun addToCache(variation: VariationEntity) {
        cache.put(variation.variationId, variation)
    }
}