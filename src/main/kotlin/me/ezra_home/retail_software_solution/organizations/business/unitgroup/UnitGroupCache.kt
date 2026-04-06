package me.ezra_home.retail_software_solution.organizations.business.unitgroup

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupDto
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.UNIT_GROUP])
class UnitGroupCache(
    private val unitGroupRepository: UnitGroupRepository,
    private val unitGroupMapper: UnitGroupMapper
) {

    @Cacheable
    fun getAllUnitGroups(): Collection<UnitGroupDto> {
        return unitGroupRepository.findAll().map { unitGroupMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun upsertUnitGroup(unitGroupDto: UnitGroupDto) {
        unitGroupRepository.save(unitGroupMapper.toEntity(unitGroupDto))
    }

    @CacheEvict(allEntries = true)
    fun deleteUnitGroup(id: UUID) {
        unitGroupRepository.deleteById(id)
    }
}
