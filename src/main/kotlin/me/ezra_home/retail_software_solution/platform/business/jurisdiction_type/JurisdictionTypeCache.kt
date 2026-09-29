package me.ezra_home.retail_software_solution.platform.business.jurisdiction_type

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.api.JurisdictionTypeDto
import me.ezra_home.retail_software_solution.platform.business.jurisdiction_type.api.JurisdictionTypeInsertDto
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.PLATFORM)
@CacheConfig(cacheNames = [CacheNames.JURISDICTION_TYPE])
class JurisdictionTypeCache(
    private val jurisdictionTypeRepository: JurisdictionTypeRepository,
    private val mapper: JurisdictionTypeMapper
) {

    @Cacheable
    fun getAll(): Collection<JurisdictionTypeDto> = jurisdictionTypeRepository.findAll().map { mapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun create(insertDto: JurisdictionTypeInsertDto): JurisdictionTypeDto {
        val saved = jurisdictionTypeRepository.save(mapper.toEntity(insertDto))
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(dto: JurisdictionTypeDto): JurisdictionTypeDto {
        val saved = jurisdictionTypeRepository.save(mapper.toEntity(dto))
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun delete(id: UUID) {
        jurisdictionTypeRepository.deleteById(id)
    }
}
