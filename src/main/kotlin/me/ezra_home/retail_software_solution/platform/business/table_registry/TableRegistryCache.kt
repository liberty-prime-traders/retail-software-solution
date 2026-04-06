package me.ezra_home.retail_software_solution.platform.business.table_registry

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.platform.business.table_registry.api.TableRegistryDto
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheConfig(cacheNames = [CacheNames.TABLE_REGISTRY])
class TableRegistryCache(
    private val repository: TableRegistryRepository,
    private val mapper: TableRegistryMapper
) {

    @Cacheable
    fun getAllTables(): Collection<TableRegistryDto> = repository.findAll().map { mapper.toDomainDto(it) }

    @CacheEvict(allEntries = true)
    fun upsertTable(dto: TableRegistryDto) = repository.save(mapper.toEntity(dto))
}
