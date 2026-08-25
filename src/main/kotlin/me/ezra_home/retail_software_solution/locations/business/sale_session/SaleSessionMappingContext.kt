package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import java.math.BigDecimal
import java.util.UUID

data class SaleSessionMappingContext(
    val contactLabel: String,
    val walkInCustomer: Boolean,
    val showActiveUserWarning: Boolean,
    val showUnreservedChangesWarning: Boolean,
)

data class AdjustmentMappingContext(
    private val adjustmentReasonNamesById: Map<UUID, String>,
    private val saleSessionTotalsCalculator: SaleSessionTotalsCalculator,
    private val saleSessionLines: List<SaleSessionLine>,
) {
    fun reasonLabel(adjustmentReasonId: UUID): String? = adjustmentReasonNamesById[adjustmentReasonId]
    fun calculatedAmount(saleSessionAdjustment: SaleSessionAdjustment): BigDecimal =
        saleSessionTotalsCalculator.calculatedAmount(saleSessionAdjustment, saleSessionLines)
}

data class LineMappingContext(
    private val unitPriceOverrideByLineKey: Map<UUID, BigDecimal>,
    private val netUnitPriceByLineKey: Map<UUID, BigDecimal>,
) {
    fun unitPriceOverrideFor(saleSessionLine: SaleSessionLine): BigDecimal =
        unitPriceOverrideByLineKey.getValue(saleSessionLine.identity.key())

    fun netUnitPriceFor(saleSessionLine: SaleSessionLine): BigDecimal =
        netUnitPriceByLineKey.getValue(saleSessionLine.identity.key())
}
