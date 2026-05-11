package me.ezra_home.retail_software_solution.locations.business.sale_discount.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductLineWithPrice
import me.ezra_home.retail_software_solution.locations.business.sale_discount.DiscountAmountCalculator
import me.ezra_home.retail_software_solution.locations.business.sale_discount.SaleDiscountEntity
import me.ezra_home.retail_software_solution.util.business.Currencies
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class NewSaleDiscountValidator {

    fun validateNewDiscounts(
        discountDtos: List<SaleDiscountCreateDto>,
        lines: List<ProductLineWithPrice>,
        productSummaries: Map<UUID, LocationProductSummaryDto>,
        existingDiscounts: List<SaleDiscountEntity> = emptyList(),
        productByLineId: Map<UUID, UUID> = emptyMap()
    ) {
        if (discountDtos.isEmpty()) return
        val lineByProductId = lines.associateBy { it.locationProductId }
        val newAmounts = discountDtos.map { dto ->
            dto.locationProductId?.let {
                if (!lineByProductId.containsKey(it)) {
                    throw RtsGenericException("No sale line found for product ${labelFor(it, productSummaries)}")
                }
            }
            DiscountAmount(
                locationProductId = dto.locationProductId,
                calculatedAmount = DiscountAmountCalculator.calculateAmount(dto, lines)
            )
        }
        val existingAmounts = existingDiscounts.map { discount ->
            val productId = discount.saleLineId?.let {
                productByLineId[it]
                    ?: throw RtsGenericException("Discount references sale line $it which is not in the surviving set")
            }
            DiscountAmount(locationProductId = productId, calculatedAmount = discount.calculatedAmount)
        }
        guardLineTotals(newAmounts, existingAmounts, lines, productSummaries)
        guardOrderTotal(newAmounts, existingAmounts, lines)
    }

    private fun guardLineTotals(
        newDiscounts: List<DiscountAmount>,
        existingDiscounts: List<DiscountAmount>,
        lines: List<ProductLineWithPrice>,
        productSummaries: Map<UUID, LocationProductSummaryDto>,
    ) {
        val lineByProductId = lines.associateBy { it.locationProductId }
        val existingDiscountsByProductId = existingDiscounts.filter { it.isLineLevelDiscount }
            .groupBy { it.locationProductId!! }
            .mapValues { (_, discounts) -> discounts.sumOf { it.calculatedAmount } }

        newDiscounts.filter { it.isLineLevelDiscount }
            .groupBy { it.locationProductId!! }
            .forEach { (locationProductId, newDiscountsForLine) ->
                val line = lineByProductId.getValue(locationProductId)
                val saleLineTotal = line.lineTotal()
                val existingLineDiscountsTotal = existingDiscountsByProductId[locationProductId] ?: BigDecimal.ZERO
                val incomingLineDiscountsTotal = newDiscountsForLine.sumOf { it.calculatedAmount }
                val total = existingLineDiscountsTotal + incomingLineDiscountsTotal
                if (total > saleLineTotal) {
                    throw RtsGenericException(
                        "On ${labelFor(locationProductId, productSummaries)}, total discounts of ${Currencies.format(total)} " +
                                "exceed line total of ${Currencies.format(saleLineTotal)}. " +
                                "Remove or adjust the discounts and resubmit."
                    )
                }
            }
    }

    private fun guardOrderTotal(
        newDiscounts: List<DiscountAmount>,
        existingDiscounts: List<DiscountAmount>,
        lines: List<ProductLineWithPrice>
    ) {
        val incomingOrderDiscountsTotal = newDiscounts.filter { !it.isLineLevelDiscount }.sumOf { it.calculatedAmount }
        if (incomingOrderDiscountsTotal == BigDecimal.ZERO) return
        val saleSubtotal = lines.sumOf { it.lineTotal() }
        val existingLineDiscountTotal = existingDiscounts.filter { it.isLineLevelDiscount }.sumOf { it.calculatedAmount }
        val existingOrderDiscountTotal = existingDiscounts.filter { !it.isLineLevelDiscount }.sumOf { it.calculatedAmount }
        val total = existingOrderDiscountTotal + incomingOrderDiscountsTotal
        val remaining = saleSubtotal - existingLineDiscountTotal
        if (total > remaining) {
            throw RtsGenericException(
                "Order-level discount total of ${Currencies.format(total)} exceeds remaining subtotal " +
                        "${Currencies.format(remaining)} (subtotal ${Currencies.format(saleSubtotal)} " +
                        "minus line discounts ${Currencies.format(existingLineDiscountTotal)}). " +
                        "Remove or adjust the discounts and resubmit."
            )
        }
    }

    private fun labelFor(
        locationProductId: UUID,
        productSummaries: Map<UUID, LocationProductSummaryDto>,
    ): String = productSummaries[locationProductId]?.label ?: locationProductId.toString()
}
