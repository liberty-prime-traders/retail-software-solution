package me.ezra_home.retail_software_solution.business.unitgroup

import me.ezra_home.retail_software_solution.business.util.UsageCountManager
import me.ezra_home.retail_software_solution.model.entity.UnitGroupEntity
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UnitGroupUsageCountManager(private val unitGroupCache: UnitGroupCache) : UsageCountManager {

    override fun incrementUsageCount(id: UUID?) {
        unitGroupCache.getAllUnitGroups().find { it.id == id }?.let {
            it.usageCount.plus(1L)
            unitGroupCache.upsertUnitGroup(it)
        }
    }

    override fun decrementUsageCount(id: UUID?) {
        unitGroupCache.getAllUnitGroups().find { it.id == id }?.let {
            it.usageCount.minus(1L)
            unitGroupCache.upsertUnitGroup(it)
        }
    }

    fun incrementUsageCount(unitGroupEntity: UnitGroupEntity) {
        incrementUsageCount(unitGroupEntity.id!!)
    }

    fun decrementUsageCount(unitGroupEntity: UnitGroupEntity) {
        decrementUsageCount(unitGroupEntity.id!!)
    }
}
