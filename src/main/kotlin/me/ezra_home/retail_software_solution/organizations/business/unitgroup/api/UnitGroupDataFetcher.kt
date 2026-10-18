package me.ezra_home.retail_software_solution.organizations.business.unitgroup.api

import me.ezra_home.retail_software_solution.organizations.business.unitgroup.UnitGroupCache
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UnitGroupDataFetcher(private val unitGroupCache: UnitGroupCache) {

    fun exists(unitGroupId: UUID?): Boolean =
        unitGroupId != null && unitGroupCache.getAllUnitGroups().any { it.id == unitGroupId }

    fun isMiscellaneous(unitGroupId: UUID?): Boolean =
        unitGroupCache.getAllUnitGroups().any { it.id == unitGroupId && it.code == SystemUnitGroup.MISC.code }
}
