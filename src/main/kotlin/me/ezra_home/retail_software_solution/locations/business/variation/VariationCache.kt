package me.ezra_home.retail_software_solution.locations.business.variation

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.locations.model.VariationEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID


@Component
@CacheConfig(cacheNames = [CacheNames.VARIATION])
class VariationCache(private val variationRepository: VariationRepository) {

    @Cacheable
    fun getAllVariations(): Collection<VariationEntity> {
        return variationRepository.findAll()
    }

    @CacheEvict(allEntries = true)
    fun upsertVariation(variationEntity: VariationEntity) {
        variationRepository.save(variationEntity)
    }

    @CacheEvict(allEntries = true)
    fun deleteVariation(id: UUID?) {
        if (id != null) {
            variationRepository.deleteById(id)
        }
    }
}
