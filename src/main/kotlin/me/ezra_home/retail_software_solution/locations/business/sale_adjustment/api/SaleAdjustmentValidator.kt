package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.SaleAdjustmentEntity
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.SaleAdjustmentRepository
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.util.business.Currencies
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleAdjustmentValidator(
    private val saleAdjustmentRepository: SaleAdjustmentRepository,
) {

    fun guardAdjustmentsBelongToSale(saleId: UUID, adjustmentIds: Collection<UUID>) {
        if (adjustmentIds.isEmpty()) return
        val owned = saleAdjustmentRepository.findIdsBySaleIdAndIdIn(saleId, adjustmentIds).toSet()
        val invalid = adjustmentIds.filterNot { it in owned }
        if (invalid.isNotEmpty()) {
            throw RtsGenericException("Adjustment ids do not belong to this sale: $invalid")
        }
    }

    fun assertAdjustmentsStillFitAfterLineChanges(
        existing: List<SaleAdjustmentEntity>,
        lines: List<SaleLineSummaryDto>,
        productSummaries: Map<UUID, LocationProductSummaryDto>,
    ) {
        // Only DISCOUNT ceilings are enforced. SURCHARGE has no ceiling — TODO(phase 2).
        val discounts = existing.filter { it.direction == AdjustmentDirection.DISCOUNT }
        val lineById = lines.filter { it.id != null }.associateBy { it.id!! }
        discounts.filter { it.saleLineId != null }
            .groupBy { it.saleLineId!! }
            .forEach { (lineId, discountsForLine) ->
                val line = lineById[lineId]
                    ?: throw RtsGenericException(
                        "Adjustment references sale line $lineId which is not in the surviving set"
                    )
                val totalDiscount = discountsForLine.sumOf { it.calculatedAmount }
                val lineTotal = line.lineTotal()
                if (totalDiscount > lineTotal) {
                    throw RtsGenericException(
                        "Discount of ${Currencies.format(totalDiscount)} on " +
                                "${labelFor(line.locationProductId, productSummaries)} exceeds new line total " +
                                "${Currencies.format(lineTotal)} after the quantity change. " +
                                "Remove or adjust the discount and resubmit."
                    )
                }
            }
        val orderLevelTotal = discounts.filter { it.saleLineId == null }.sumOf { it.calculatedAmount }
        if (orderLevelTotal.signum() == 0) return
        val lineLevelTotal = discounts.filter { it.saleLineId != null }.sumOf { it.calculatedAmount }
        val subtotal = lines.sumOf { it.lineTotal() }
        val remaining = subtotal - lineLevelTotal
        if (orderLevelTotal > remaining) {
            throw RtsGenericException(
                "Order-level discount of ${Currencies.format(orderLevelTotal)} exceeds remaining subtotal " +
                        "${Currencies.format(remaining)} (subtotal ${Currencies.format(subtotal)} " +
                        "minus line discounts ${Currencies.format(lineLevelTotal)}) after the line changes. " +
                        "Remove or adjust the discount and resubmit."
            )
        }
    }

    private fun labelFor(
        locationProductId: UUID,
        productSummaries: Map<UUID, LocationProductSummaryDto>,
    ): String = productSummaries[locationProductId]?.label ?: locationProductId.toString()

    companion object {
        fun guardIsDraft(status: SaleStatus) {
            if (status != SaleStatus.DRAFT) {
                throw RtsGenericException("Adjustments can only be modified on DRAFT sales")
            }
        }
    }
}
