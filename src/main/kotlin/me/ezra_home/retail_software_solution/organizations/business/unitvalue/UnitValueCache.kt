package me.ezra_home.retail_software_solution.organizations.business.unitvalue

import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.organizations.model.UnitValueEntity
import me.ezra_home.retail_software_solution.util.exceptions.QueriedByEmptyIdException
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.UNIT_VALUE])
internal class UnitValueCache(private val unitValueRepository: UnitValueRepository) {

    @Cacheable
    fun getByUnitGroupId(unitGroupId: UUID?): Collection<UnitValueEntity> {
        if (unitGroupId == null) {
            throw QueriedByEmptyIdException()
        }
        return unitValueRepository.findByUnitGroupId(unitGroupId)
    }

    @Cacheable
    fun getAllUnitValues(): Collection<UnitValueEntity> {
        return unitValueRepository.findAll()
    }

    @CacheEvict(allEntries = true)
    fun upsertUnitValue(unitValueEntity: UnitValueEntity) {
        unitValueRepository.save(unitValueEntity)
    }

    @CacheEvict(allEntries = true)
    fun deleteUnitValue(id: UUID?) {
        if (id != null) {
            unitValueRepository.deleteById(id)
        }
    }
}
