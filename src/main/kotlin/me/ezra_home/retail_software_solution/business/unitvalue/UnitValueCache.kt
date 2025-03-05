package me.ezra_home.retail_software_solution.business.unitvalue

import me.ezra_home.retail_software_solution.business.util.exceptions.QueriedByEmptyIdException
import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import me.ezra_home.retail_software_solution.model.entity.UnitValueEntity
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.UNITVALUE])
class UnitValueCache(private val unitValueRepository: UnitValueRepository) {

    @Cacheable
    fun getByUnitGroupId(unitGroupId: UUID?): Collection<UnitValueEntity> {
        if (unitGroupId == null) {
            throw QueriedByEmptyIdException()
        }
        return unitValueRepository.findByUnitGroupId(unitGroupId)
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
