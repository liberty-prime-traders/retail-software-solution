package me.ezra_home.retail_software_solution.organizations.business.unitgroup

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupDto
import me.ezra_home.retail_software_solution.organizations.business.unitgroup.api.UnitGroupInsertDto
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
    fun create(insertDto: UnitGroupInsertDto): UnitGroupDto {
        val saved = unitGroupRepository.saveAndFlush(unitGroupMapper.toEntity(insertDto))
        return unitGroupMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun save(unitGroupDto: UnitGroupDto): UnitGroupDto {
        val saved = unitGroupRepository.save(unitGroupMapper.toEntity(unitGroupDto))
        return unitGroupMapper.toDomainDto(saved)
    }

    @CacheEvict(allEntries = true)
    fun saveAll(entities: Collection<UnitGroupEntity>) {
        unitGroupRepository.saveAll(entities)
    }

    @CacheEvict(allEntries = true)
    fun deleteUnitGroup(id: UUID) {
        unitGroupRepository.deleteById(id)
    }
}
