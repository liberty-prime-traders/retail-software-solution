package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentSummaryDto
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class SaleTotalsApplier {

    fun applyTotals(
        sale: SaleEntity,
        lines: List<SaleLineEntity>,
        adjustments: List<SaleAdjustmentSummaryDto>,
    ) {
        sale.subtotal = lines.sumOf { it.lineTotal() }
        sale.lineLevelDiscountTotal = sumOf(adjustments, AdjustmentDirection.DISCOUNT, lineLevel = true)
        sale.orderLevelDiscountTotal = sumOf(adjustments, AdjustmentDirection.DISCOUNT, lineLevel = false)
        sale.lineLevelSurchargeTotal = sumOf(adjustments, AdjustmentDirection.SURCHARGE, lineLevel = true)
        sale.orderLevelSurchargeTotal = sumOf(adjustments, AdjustmentDirection.SURCHARGE, lineLevel = false)
    }

    private fun sumOf(
        adjustments: List<SaleAdjustmentSummaryDto>,
        direction: AdjustmentDirection,
        lineLevel: Boolean,
    ): BigDecimal = adjustments
        .filter { it.direction == direction && (it.saleLineId != null) == lineLevel }
        .sumOf { it.calculatedAmount }
}
