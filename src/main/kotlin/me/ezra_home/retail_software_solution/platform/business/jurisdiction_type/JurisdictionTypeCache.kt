package me.ezra_home.retail_software_solution.platform.business.jurisdiction_type

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.platform.model.JurisdictionTypeEntity
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.PLATFORM)
@CacheConfig(cacheNames = [CacheNames.JURISDICTION_TYPE])
internal class JurisdictionTypeCache(
    private val jurisdictionTypeRepository: JurisdictionTypeRepository
) {

    @Cacheable
    fun getAll(): Collection<JurisdictionTypeEntity> = jurisdictionTypeRepository.findAll()

    @CacheEvict(allEntries = true)
    fun upsert(entity: JurisdictionTypeEntity) {
        jurisdictionTypeRepository.save(entity)
    }

    @CacheEvict(allEntries = true)
    fun delete(id: UUID) {
        jurisdictionTypeRepository.deleteById(id)
    }
}
