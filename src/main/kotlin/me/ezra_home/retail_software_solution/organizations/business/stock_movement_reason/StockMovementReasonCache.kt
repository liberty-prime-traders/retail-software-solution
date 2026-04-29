package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.StockMovementReasonInsertDto
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.STOCK_MOVEMENT_REASON])
class StockMovementReasonCache(
    private val repository: StockMovementReasonRepository,
    private val mapper: StockMovementReasonMapper
) {

    @Cacheable
    fun getAll(): Collection<StockMovementReasonDto> =
        repository.findAll().map { mapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun create(insertDto: StockMovementReasonInsertDto): StockMovementReasonDto {
        val saved = repository.saveAndFlush(mapper.toEntity(insertDto))
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(dto: StockMovementReasonDto): StockMovementReasonDto {
        val saved = repository.save(mapper.toEntity(dto))
        return mapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun saveAll(entities: Collection<StockMovementReasonEntity>) {
        repository.saveAll(entities)
    }
}
