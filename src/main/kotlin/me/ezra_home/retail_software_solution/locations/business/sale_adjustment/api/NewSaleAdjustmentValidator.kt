package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductLineWithPrice
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.AdjustmentAmountCalculator
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.SaleAdjustmentEntity
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentReasonService
import me.ezra_home.retail_software_solution.util.business.Currencies
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class NewSaleAdjustmentValidator(
    private val adjustmentReasonService: AdjustmentReasonService,
) {

    fun validateNewAdjustments(
        adjustmentDtos: List<SaleAdjustmentCreateDto>,
        lines: List<ProductLineWithPrice>,
        productSummaries: Map<UUID, LocationProductSummaryDto>,
        existingAdjustments: List<SaleAdjustmentEntity> = emptyList(),
        productBySaleLineId: Map<UUID, UUID> = emptyMap(),
    ) {
        if (adjustmentDtos.isEmpty() && existingAdjustments.isEmpty()) return
        val lineByProductId = lines.associateBy { it.locationProductId }
        adjustmentDtos.forEach { dto ->
            dto.locationProductId?.let {
                if (!lineByProductId.containsKey(it)) {
                    throw RtsGenericException(
                        "No sale line found for product ${labelFor(it, productSummaries)}"
                    )
                }
            }
            val reason = adjustmentReasonService.getById(dto.adjustmentReasonId)
            adjustmentReasonService.requireCanApply(reason, dto.direction)
        }
        existingAdjustments.forEach { adjustment ->
            adjustment.saleLineId?.let {
                productBySaleLineId[it]
                    ?: throw RtsGenericException(
                        "Adjustment references sale line $it which is not on the current sale"
                    )
            }
        }
    }

    fun guardAdjustmentCeilings(
        adjustmentDtos: List<SaleAdjustmentCreateDto>,
        lines: List<ProductLineWithPrice>,
        productSummaries: Map<UUID, LocationProductSummaryDto>,
        existingAdjustments: List<SaleAdjustmentEntity> = emptyList(),
        productByLineId: Map<UUID, UUID> = emptyMap(),
    ) {
        if (adjustmentDtos.isEmpty()) return
        val newDiscountAmounts = adjustmentDtos
            .filter { it.direction == AdjustmentDirection.DISCOUNT }
            .map { dto ->
                AdjustmentAmount(
                    locationProductId = dto.locationProductId,
                    calculatedAmount = AdjustmentAmountCalculator.calculateAmount(dto, lines)
                )
            }
        val existingDiscountAmounts = existingAdjustments
            .filter { it.direction == AdjustmentDirection.DISCOUNT }
            .map { entity ->
                val productId = entity.saleLineId?.let { productByLineId[it] }
                AdjustmentAmount(locationProductId = productId, calculatedAmount = entity.calculatedAmount)
            }
        guardLineTotals(newDiscountAmounts, existingDiscountAmounts, lines, productSummaries)
        guardOrderTotal(newDiscountAmounts, existingDiscountAmounts, lines)
    }

    private fun guardLineTotals(
        newDiscounts: List<AdjustmentAmount>,
        existingDiscounts: List<AdjustmentAmount>,
        lines: List<ProductLineWithPrice>,
        productSummaries: Map<UUID, LocationProductSummaryDto>,
    ) {
        val lineByProductId = lines.associateBy { it.locationProductId }
        val existingByProductId = existingDiscounts.filter { it.isLineLevel }
            .groupBy { it.locationProductId!! }
            .mapValues { (_, amounts) -> amounts.sumOf { it.calculatedAmount } }

        newDiscounts.filter { it.isLineLevel }
            .groupBy { it.locationProductId!! }
            .forEach { (locationProductId, newForLine) ->
                val line = lineByProductId.getValue(locationProductId)
                val saleLineTotal = line.lineTotal()
                val existingTotal = existingByProductId[locationProductId] ?: BigDecimal.ZERO
                val incomingTotal = newForLine.sumOf { it.calculatedAmount }
                val total = existingTotal + incomingTotal
                if (total > saleLineTotal) {
                    throw RtsGenericException(
                        "On ${labelFor(locationProductId, productSummaries)}, total discounts of " +
                                "${Currencies.format(total)} exceed line total of " +
                                "${Currencies.format(saleLineTotal)}. " +
                                "Remove or adjust the discounts then resubmit."
                    )
                }
            }
    }

    private fun guardOrderTotal(
        newDiscounts: List<AdjustmentAmount>,
        existingDiscounts: List<AdjustmentAmount>,
        lines: List<ProductLineWithPrice>,
    ) {
        val incomingOrderTotal = newDiscounts.filter { !it.isLineLevel }.sumOf { it.calculatedAmount }
        if (incomingOrderTotal == BigDecimal.ZERO) return
        val saleSubtotal = lines.sumOf { it.lineTotal() }
        val existingLineTotal = existingDiscounts.filter { it.isLineLevel }.sumOf { it.calculatedAmount }
        val existingOrderTotal = existingDiscounts.filter { !it.isLineLevel }.sumOf { it.calculatedAmount }
        val total = existingOrderTotal + incomingOrderTotal
        val remaining = saleSubtotal - existingLineTotal
        if (total > remaining) {
            throw RtsGenericException(
                "Order-level discount total of ${Currencies.format(total)} exceeds remaining subtotal " +
                        "${Currencies.format(remaining)} (subtotal ${Currencies.format(saleSubtotal)} " +
                        "minus line discounts ${Currencies.format(existingLineTotal)}). " +
                        "Remove or adjust the discounts and resubmit."
            )
        }
    }

    private fun labelFor(
        locationProductId: UUID,
        productSummaries: Map<UUID, LocationProductSummaryDto>,
    ): String = productSummaries[locationProductId]?.label ?: locationProductId.toString()
}

private data class AdjustmentAmount(
    val locationProductId: UUID?,
    val calculatedAmount: BigDecimal,
) {
    val isLineLevel: Boolean get() = locationProductId != null
}
