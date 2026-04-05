package me.ezra_home.retail_software_solution.organizations.business.unitvalue

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.dto.UnitValueDto
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.util.exceptions.QueriedByEmptyIdException
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.UNIT_VALUE])
class UnitValueCache(
    private val unitValueRepository: UnitValueRepository,
    private val unitValueMapper: UnitValueMapper
) {

    @Cacheable
    fun getByUnitGroupId(unitGroupId: UUID?): Collection<UnitValueDto> {
        if (unitGroupId == null) {
            throw QueriedByEmptyIdException()
        }
        return unitValueRepository.findByUnitGroupId(unitGroupId).map { unitValueMapper.toDomainDto(it) }
    }

    @Cacheable
    fun getAllUnitValues(): Collection<UnitValueDto> {
        return unitValueRepository.findAll().map { unitValueMapper.toDomainDto(it) }
    }

    @CacheEvict(allEntries = true)
    fun upsertUnitValue(unitValueDto: UnitValueDto) {
        unitValueRepository.save(unitValueMapper.toEntity(unitValueDto))
    }

    @CacheEvict(allEntries = true)
    fun deleteUnitValue(id: UUID?) {
        if (id != null) {
            unitValueRepository.deleteById(id)
        }
    }
}
