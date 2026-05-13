package me.ezra_home.retail_software_solution.organizations.business.unitconversion.api

import me.ezra_home.retail_software_solution.organizations.business.unitconversion.UnitConversionGraphBuilder
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class UnitConversionGraphFacade(private val graphBuilder: UnitConversionGraphBuilder) {

    fun getOrLoad(): UnitConversionGraph = graphBuilder.getOrLoad()

    fun getFactor(fromUnitId: UUID, toUnitId: UUID): BigDecimal {
        return graphBuilder.getOrLoad().getFactor(fromUnitId, toUnitId)
    }

    fun invalidate() = graphBuilder.invalidate()
}
