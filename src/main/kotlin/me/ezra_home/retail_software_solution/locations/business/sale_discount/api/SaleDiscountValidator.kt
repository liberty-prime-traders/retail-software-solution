package me.ezra_home.retail_software_solution.locations.business.sale_discount.api

import me.ezra_home.retail_software_solution.locations.business.sale.SaleEntity
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_discount.DiscountAmountCalculator
import me.ezra_home.retail_software_solution.locations.business.sale_discount.SaleDiscountEntity
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.math.BigDecimal
import java.util.UUID

object SaleDiscountValidator {

    fun guardIsDraft(sale: SaleEntity) {
        if (sale.status != SaleStatus.DRAFT) {
            throw RtsGenericException("Discounts can only be modified on DRAFT sales")
        }
    }

    fun validateNewDiscounts(
        discountDtos: List<SaleDiscountCreateDto>,
        lines: List<LinePricing>,
        existingDiscounts: List<SaleDiscountEntity> = emptyList(),
        productByLineId: Map<UUID, UUID> = emptyMap()
    ) {
        if (discountDtos.isEmpty()) return
        val lineByProductId = lines.associateBy { it.locationProductId }
        val newAmounts = discountDtos.map { dto ->
            dto.locationProductId?.let {
                if (!lineByProductId.containsKey(it)) {
                    throw RtsGenericException("No sale line found for product $it")
                }
            }
            DiscountAmount(
                locationProductId = dto.locationProductId,
                calculatedAmount = DiscountAmountCalculator.calculateAmount(dto, lines)
            )
        }
        val existingAmounts = existingDiscounts.map { discount ->
            DiscountAmount(
                locationProductId = discount.saleLineId?.let { productByLineId[it] },
                calculatedAmount = discount.calculatedAmount
            )
        }
        guardLineTotals(newAmounts, existingAmounts, lines)
        guardOrderTotal(newAmounts, existingAmounts, lines)
    }

    private fun guardLineTotals(
        newDiscounts: List<DiscountAmount>,
        existingDiscounts: List<DiscountAmount>,
        lines: List<LinePricing>
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
                if (existingLineDiscountsTotal + incomingLineDiscountsTotal > saleLineTotal) {
                    throw RtsGenericException("Discount total exceeds line total")
                }
            }
    }

    private fun guardOrderTotal(
        newDiscounts: List<DiscountAmount>,
        existingDiscounts: List<DiscountAmount>,
        lines: List<LinePricing>
    ) {
        val incomingOrderDiscountsTotal = newDiscounts.filter { !it.isLineLevelDiscount }.sumOf { it.calculatedAmount }
        if (incomingOrderDiscountsTotal == BigDecimal.ZERO) return
        val saleSubtotal = lines.sumOf { it.lineTotal() }
        val existingLineDiscountTotal = existingDiscounts.filter { it.isLineLevelDiscount }.sumOf { it.calculatedAmount }
        val existingOrderDiscountTotal = existingDiscounts.filter { !it.isLineLevelDiscount }.sumOf { it.calculatedAmount }
        if (existingOrderDiscountTotal + incomingOrderDiscountsTotal > saleSubtotal - existingLineDiscountTotal) {
            throw RtsGenericException("Order-level discount total exceeds order subtotal after line discounts")
        }
    }
}

data class DiscountAmount(
    val locationProductId: UUID?,
    val calculatedAmount: BigDecimal,
    val isLineLevelDiscount: Boolean = locationProductId != null
)

interface LinePricing {
    val locationProductId: UUID
    val quantity: BigDecimal
    val unitPrice: BigDecimal

    fun lineTotal(): BigDecimal = Decimals.multiplyScale4(quantity, unitPrice)
}
