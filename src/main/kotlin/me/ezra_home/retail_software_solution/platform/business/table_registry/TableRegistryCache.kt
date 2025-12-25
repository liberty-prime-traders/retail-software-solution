package me.ezra_home.retail_software_solution.platform.business.table_registry

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.platform.model.TableRegistryEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheConfig(cacheNames = [CacheNames.TABLE_REGISTRY])
class TableRegistryCache(private val repository: TableRegistryRepository) {

    @Cacheable
    fun getAllTables(): Collection<TableRegistryEntity> = repository.findAll()

    @CacheEvict(allEntries = true)
    fun upsertTable(entity: TableRegistryEntity) = repository.save(entity)
}
