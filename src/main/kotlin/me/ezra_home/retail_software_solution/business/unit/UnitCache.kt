package me.ezra_home.retail_software_solution.business.unit

import java.util.UUID
import me.ezra_home.retail_software_solution.model.entity.UnitEntity
import me.ezra_home.retail_software_solution.configuration.cache.CacheNames
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
@CacheConfig(cacheNames = [CacheNames.UNIT])
class UnitCache(private val unitRepository: UnitRepository) {

    @Cacheable
    fun getAllUnits(): Collection<UnitEntity> = unitRepository.findAll()

    @CacheEvict(allEntries = true)
    fun upsertUnit(unitEntity: UnitEntity): UnitEntity = unitRepository.save(unitEntity)

    @CacheEvict(allEntries = true)
    fun deleteUnit(id: UUID) {
        unitRepository.deleteById(id)
    }
}