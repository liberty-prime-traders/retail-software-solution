package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.springframework.stereotype.Component
import java.util.UUID

// Knows no products, so product code can depend on it without a cycle
@Component
class QuantityBreakdownFactory(
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
) {

    fun forBaseUnits(baseUnitIds: Collection<UUID>): Map<UUID, QuantityBreakdown> {
        if (baseUnitIds.isEmpty()) return emptyMap()
        val breakdownUnits = unitConversionGraphFacade.getOrLoad().getBreakdownUnits()
        return baseUnitIds.toSet().associateWith { QuantityBreakdown.forBaseUnit(breakdownUnits, it) }
    }
}
