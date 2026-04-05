package me.ezra_home.retail_software_solution.organizations.business.org_table_registry

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.organizations.model.OrgTableRegistryEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
@CacheConfig(cacheNames = [CacheNames.ORG_TABLE_REGISTRY])
internal class OrgTableRegistryCache(private val repository: OrgTableRegistryRepository) {

    @Cacheable
    fun getAllTables(): Collection<OrgTableRegistryEntity> = repository.findAll()

    @CacheEvict(allEntries = true)
    fun upsertTable(entity: OrgTableRegistryEntity) {
        repository.save(entity)
    }
}
