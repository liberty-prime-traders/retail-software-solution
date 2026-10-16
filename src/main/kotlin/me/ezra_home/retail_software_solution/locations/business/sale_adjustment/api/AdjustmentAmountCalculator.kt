package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductLineWithPrice
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal

object AdjustmentAmountCalculator {

    fun calculateAmount(dto: SaleAdjustmentCreateDto, lines: List<ProductLineWithPrice>): BigDecimal =
        when (dto.calculationMethod) {
            CalculationMethod.FIXED_VALUE -> {
                if (dto.locationProductId != null) {
                    val targetLine = lines.first { it.locationProductId == dto.locationProductId }
                    Decimals.Scale4.multiply(dto.value, targetLine.quantity)
                } else {
                    dto.value
                }
            }
            CalculationMethod.PERCENTAGE -> {
                val basePrice = if (dto.locationProductId != null) {
                    lines.first { it.locationProductId == dto.locationProductId }.lineTotal
                } else {
                    lines.sumOf { it.lineTotal }
                }
                Decimals.Scale4.multiply(Decimals.Scale4.divide(dto.value, BigDecimal(100)), basePrice)
            }
        }
}
