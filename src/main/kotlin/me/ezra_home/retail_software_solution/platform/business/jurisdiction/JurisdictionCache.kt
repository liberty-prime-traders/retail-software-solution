package me.ezra_home.retail_software_solution.platform.business.jurisdiction

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.platform.model.JurisdictionEntity
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.PLATFORM)
@CacheConfig(cacheNames = [CacheNames.JURISDICTION])
internal class JurisdictionCache(
    private val jurisdictionRepository: JurisdictionRepository
) {

    @Cacheable
    fun getAll(): Collection<JurisdictionEntity> = jurisdictionRepository.findAll()

    @CacheEvict(allEntries = true)
    fun upsert(entity: JurisdictionEntity) {
        jurisdictionRepository.save(entity)
    }

    @CacheEvict(allEntries = true)
    fun delete(id: UUID) {
        jurisdictionRepository.deleteById(id)
    }
}
