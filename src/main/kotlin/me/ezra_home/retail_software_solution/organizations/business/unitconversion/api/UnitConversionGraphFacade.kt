package me.ezra_home.retail_software_solution.organizations.business.unitconversion.api

import me.ezra_home.retail_software_solution.organizations.business.unitconversion.UnitConversionGraphBuilder
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class UnitConversionGraphFacade(private val graphBuilder: UnitConversionGraphBuilder) {

    fun getOrLoad(): Map<UUID, Map<UUID, ConversionTargetDto>> = graphBuilder.getOrLoad()

    fun getFactor(fromUnitId: UUID, toUnitId: UUID): BigDecimal {
        if (fromUnitId == toUnitId) return BigDecimal.ONE
        val target = graphBuilder.getOrLoad()[fromUnitId]?.get(toUnitId)
            ?: throw RtsGenericException("No conversion path from unit $fromUnitId to $toUnitId")
        return target.factor
    }

    fun convert(fromUnitId: UUID, toUnitId: UUID, quantity: BigDecimal): BigDecimal {
        if (quantity.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        val target = graphBuilder.getOrLoad()[fromUnitId]?.get(toUnitId)
            ?: throw RtsGenericException("No conversion path from unit $fromUnitId to $toUnitId")
        return Decimals.divideScale4(
            quantity.multiply(target.numerator.toBigDecimal()),
            target.denominator.toBigDecimal()
        )
    }

    fun invalidate() = graphBuilder.invalidate()
}
