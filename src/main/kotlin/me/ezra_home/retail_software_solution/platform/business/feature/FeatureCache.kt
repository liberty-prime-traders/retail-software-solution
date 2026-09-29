package me.ezra_home.retail_software_solution.platform.business.feature

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.platform.business.feature.api.FeatureDto
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.PLATFORM)
@CacheConfig(cacheNames = [CacheNames.FEATURE])
class FeatureCache(
    private val featureRepository: FeatureRepository,
    private val featureMapper: FeatureMapper
) {

    @Cacheable
    fun getAll(): Collection<FeatureDto> = featureRepository.findAll().map {
        featureMapper.toDomainDto(it)
    }

    @CacheEvict(allEntries = true)
    fun update(updated: FeatureDto) {
        featureRepository.save(featureMapper.toEntity(updated))
    }
}
