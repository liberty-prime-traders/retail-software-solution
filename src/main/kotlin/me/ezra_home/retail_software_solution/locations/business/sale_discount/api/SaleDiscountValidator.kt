package me.ezra_home.retail_software_solution.locations.business.sale_discount.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.SaleLineEntity
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_discount.SaleDiscountEntity
import me.ezra_home.retail_software_solution.locations.business.sale_discount.SaleDiscountRepository
import me.ezra_home.retail_software_solution.util.business.Currencies
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SaleDiscountValidator(
    private val saleDiscountRepository: SaleDiscountRepository,
) {

    fun guardDiscountsBelongToSale(saleId: UUID, discountIds: Collection<UUID>) {
        if (discountIds.isEmpty()) return
        val owned = saleDiscountRepository.findIdsBySaleIdAndIdIn(saleId, discountIds).toSet()
        val invalid = discountIds.filterNot { it in owned }
        if (invalid.isNotEmpty()) {
            throw RtsGenericException("Discount ids do not belong to this sale: $invalid")
        }
    }

    fun assertDiscountsStillFitAfterLineChanges(
        existing: List<SaleDiscountEntity>,
        lines: List<SaleLineEntity>,
        productSummaries: Map<UUID, LocationProductSummaryDto>,
    ) {
        val lineById = lines.filter { it.id != null }.associateBy { it.id!! }
        existing.filter { it.saleLineId != null }
            .groupBy { it.saleLineId!! }
            .forEach { (lineId, discountsForLine) ->
                val line = lineById[lineId]
                    ?: throw RtsGenericException("Discount references sale line $lineId which is not in the surviving set")
                val totalDiscount = discountsForLine.sumOf { it.calculatedAmount }
                val lineTotal = line.lineTotal()
                if (totalDiscount > lineTotal) {
                    throw RtsGenericException(
                        "Discount of ${Currencies.format(totalDiscount)} on ${labelFor(line.locationProductId, productSummaries)} " +
                                "exceeds new line total ${Currencies.format(lineTotal)} after the quantity change. " +
                                "Remove or adjust the discount and resubmit."
                    )
                }
            }
        val orderLevelTotal = existing.filter { it.saleLineId == null }.sumOf { it.calculatedAmount }
        if (orderLevelTotal.signum() == 0) return
        val lineLevelTotal = existing.filter { it.saleLineId != null }.sumOf { it.calculatedAmount }
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
        fun guardIsDraft(sale: SaleEntity) {
            if (sale.status != SaleStatus.DRAFT) {
                throw RtsGenericException("Discounts can only be modified on DRAFT sales")
            }
        }
    }
}
