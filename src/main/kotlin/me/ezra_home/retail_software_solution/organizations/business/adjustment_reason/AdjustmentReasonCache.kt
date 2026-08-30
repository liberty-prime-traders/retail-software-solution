package me.ezra_home.retail_software_solution.organizations.business.adjustment_reason

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentReasonInsertDto
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ADJUSTMENT_REASON])
class AdjustmentReasonCache(
    private val repository: AdjustmentReasonRepository,
    private val mapper: AdjustmentReasonMapper
) {

    @Cacheable
    fun getAll(): Collection<AdjustmentReasonDto> =
        repository.findAll().map { mapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun create(insertDto: AdjustmentReasonInsertDto): AdjustmentReasonDto {
        val saved = repository.save(mapper.toEntity(insertDto))
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun saveAll(entities: Collection<AdjustmentReasonEntity>) {
        repository.saveAll(entities)
    }
}
