package me.ezra_home.retail_software_solution.platform.business.table_registry

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.platform.model.TableRegistryEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.TABLE_REGISTRY])
class TableRegistryCache(
    private val repository: TableRegistryRepository
) {

    @Cacheable
    fun getAllTableRegistries(): Collection<TableRegistryEntity> = repository.findAll()

    @Cacheable
    fun findByTableName(tableName: String): TableRegistryEntity? = repository.findByTableName(tableName)

    @CacheEvict(allEntries = true)
    fun upsertTableRegistry(entity: TableRegistryEntity) = repository.save(entity)

    @CacheEvict(allEntries = true)
    fun deleteTableRegistry(id: UUID) {
        repository.deleteById(id)
    }
}
