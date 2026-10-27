package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LinePricing
import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductLineWithPrice
import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal

object AdjustmentAmountCalculator {

    fun calculateAmount(saleAdjustmentCreateDto: SaleAdjustmentCreateDto, productLinesWithPrice: List<ProductLineWithPrice>): BigDecimal =
        when (saleAdjustmentCreateDto.calculationMethod) {
            PriceAdjustmentMethod.UNIT_PRICE_OVERRIDE -> {
                val overriddenLine = productLinesWithPrice.first { it.locationProductId == saleAdjustmentCreateDto.locationProductId }
                Decimals.MoneyScale.round(
                    LinePricing.overrideDifference(overriddenLine.lineTotal, saleAdjustmentCreateDto.value, overriddenLine.userProvidedQuantity).abs()
                )
            }
            PriceAdjustmentMethod.PERCENTAGE -> {
                val basePrice = if (saleAdjustmentCreateDto.locationProductId != null) {
                    productLinesWithPrice.first { it.locationProductId == saleAdjustmentCreateDto.locationProductId }.lineTotal
                } else {
                    productLinesWithPrice.sumOf { it.lineTotal }
                }
                Decimals.MoneyScale.divide(saleAdjustmentCreateDto.value.multiply(basePrice), BigDecimal(100))
            }
        }
}
