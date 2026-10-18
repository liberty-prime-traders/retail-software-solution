package me.ezra_home.retail_software_solution.organizations.business.unitconversion.api

import me.ezra_home.retail_software_solution.organizations.business.unitconversion.UnitConversionGraphBuilder
import org.springframework.stereotype.Service

@Service
class UnitConversionGraphFacade(private val graphBuilder: UnitConversionGraphBuilder) {

    fun getOrLoad(): UnitConversionGraph = graphBuilder.getOrLoad()

    fun invalidate() = graphBuilder.invalidate()
}
