package me.ezra_home.retail_software_solution.locations.business.sale_discount

import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductLineWithPrice
import me.ezra_home.retail_software_solution.locations.business.sale_discount.api.SaleDiscountCreateDto
import me.ezra_home.retail_software_solution.platform.business.tax_type.api.CalculationMethod
import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal

object DiscountAmountCalculator {

    fun calculateAmount(dto: SaleDiscountCreateDto, lines: List<ProductLineWithPrice>): BigDecimal {
        return when (dto.calculationMethod) {
            CalculationMethod.FIXED_VALUE -> dto.value
            CalculationMethod.PERCENTAGE -> {
                val basePrice = if (dto.locationProductId != null) {
                    lines.first { it.locationProductId == dto.locationProductId }.lineTotal()
                } else {
                    lines.sumOf { it.lineTotal() }
                }
                Decimals.multiplyScale4(Decimals.divideScale4(dto.value, BigDecimal(100)), basePrice)
            }
        }
    }
}
