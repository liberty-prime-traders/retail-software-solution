package me.ezra_home.retail_software_solution.organizations.business.unitvalue

import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UnitValueQualifier(private val unitValueCache: UnitValueCache) {

    @UnitName
    fun getUnitName(unitValueId: UUID?): String? {
        return unitValueId?.let {
            unitValueCache.getAllUnitValues().find { it.id == unitValueId }?.name
        }
    }
}
