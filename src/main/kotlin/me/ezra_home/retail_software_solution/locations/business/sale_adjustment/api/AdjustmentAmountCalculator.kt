package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductLineWithPrice
import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal

object AdjustmentAmountCalculator {

    fun calculateAmount(dto: SaleAdjustmentCreateDto, lines: List<ProductLineWithPrice>): BigDecimal =
        when (dto.calculationMethod) {
            PriceAdjustmentMethod.FIXED_LINE_TOTAL -> Decimals.Scale2.round(dto.value)
            PriceAdjustmentMethod.PERCENTAGE -> {
                val basePrice = if (dto.locationProductId != null) {
                    lines.first { it.locationProductId == dto.locationProductId }.lineTotal
                } else {
                    lines.sumOf { it.lineTotal }
                }
                Decimals.Scale2.divide(dto.value.multiply(basePrice), BigDecimal(100))
            }
        }
}
