package me.ezra_home.retail_software_solution.organizations.business.unitgroup

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.organizations.model.UnitGroupEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.UNIT_GROUP])
internal class UnitGroupCache(private val unitGroupRepository: UnitGroupRepository) {

    @Cacheable
    fun getAllUnitGroups(): Collection<UnitGroupEntity> {
        return unitGroupRepository.findAll()
    }

    @CacheEvict(allEntries = true)
    fun upsertUnitGroup(unitGroupEntity: UnitGroupEntity) {
        unitGroupRepository.save(unitGroupEntity)
    }

    @CacheEvict(allEntries = true)
    fun deleteUnitGroup(id: UUID) {
        unitGroupRepository.deleteById(id)
    }
}
