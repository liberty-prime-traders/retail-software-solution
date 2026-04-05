package me.ezra_home.retail_software_solution.platform.business.jurisdiction

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.PLATFORM)
@CacheConfig(cacheNames = [CacheNames.JURISDICTION])
class JurisdictionCache(
    private val jurisdictionRepository: JurisdictionRepository,
    private val mapper: JurisdictionMapper
) {

    @Cacheable
    fun getAll(): Collection<JurisdictionDto> = jurisdictionRepository.findAll().map { mapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun upsert(dto: JurisdictionDto) {
        jurisdictionRepository.save(mapper.toEntity(dto))
    }

    @CacheEvict(allEntries = true)
    fun delete(id: UUID) {
        jurisdictionRepository.deleteById(id)
    }
}
