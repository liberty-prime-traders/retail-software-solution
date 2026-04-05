package me.ezra_home.retail_software_solution.platform.business.tax_type

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxTypeDto
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.PLATFORM)
@CacheConfig(cacheNames = [CacheNames.TAX_TYPE])
class TaxTypeCache(
    private val taxTypeRepository: TaxTypeRepository,
    private val mapper: TaxTypeMapper
) {

    @Cacheable
    fun getAll(): Collection<TaxTypeDto> = taxTypeRepository.findAll().map { mapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun upsert(dto: TaxTypeDto) {
        taxTypeRepository.save(mapper.toEntity(dto))
    }

    @CacheEvict(allEntries = true)
    fun delete(id: UUID) {
        taxTypeRepository.deleteById(id)
    }
}
