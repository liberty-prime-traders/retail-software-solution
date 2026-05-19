package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.AdjustmentAmountCalculator
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionTotals
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class SaleSessionTotalsCalculator {

    fun recompute(session: SaleSession): SaleSession {
        val totals = compute(session.lines, session.adjustments, session.totalPaid())
        return session.copy(totals = totals)
    }

    fun compute(
        lines: List<SaleSessionLine>,
        adjustments: List<SaleSessionAdjustment>,
        paymentTotal: BigDecimal,
    ): SaleSessionTotals {
        val subtotal = lines.sumOf { it.lineTotal() }
        val productByLineKey = lines.associate { it.identity.key() to it.locationProductId }
        val amountsByAdjustment = adjustments.associate { adj ->
            adj.identity.key() to calculateAmount(adj, productByLineKey, lines)
        }
        val lineLevelDiscount = sumOf(adjustments, AdjustmentDirection.DISCOUNT, lineLevel = true, amountsByAdjustment)
        val orderLevelDiscount = sumOf(adjustments, AdjustmentDirection.DISCOUNT, lineLevel = false, amountsByAdjustment)
        val lineLevelSurcharge = sumOf(adjustments, AdjustmentDirection.SURCHARGE, lineLevel = true, amountsByAdjustment)
        val orderLevelSurcharge = sumOf(adjustments, AdjustmentDirection.SURCHARGE, lineLevel = false, amountsByAdjustment)
        val payableTotal = subtotal - lineLevelDiscount - orderLevelDiscount + lineLevelSurcharge + orderLevelSurcharge
        return SaleSessionTotals(
            subtotal = subtotal,
            lineLevelDiscountTotal = lineLevelDiscount,
            orderLevelDiscountTotal = orderLevelDiscount,
            lineLevelSurchargeTotal = lineLevelSurcharge,
            orderLevelSurchargeTotal = orderLevelSurcharge,
            paymentTotal = paymentTotal,
            payableTotal = payableTotal,
            balance = payableTotal - paymentTotal,
        )
    }

    fun calculatedAmount(adjustment: SaleSessionAdjustment, lines: List<SaleSessionLine>): BigDecimal {
        val productByLineKey = lines.associate { it.identity.key() to it.locationProductId }
        return calculateAmount(adjustment, productByLineKey, lines)
    }

    private fun calculateAmount(
        adjustment: SaleSessionAdjustment,
        productByLineKey: Map<UUID, UUID>,
        lines: List<SaleSessionLine>,
    ): BigDecimal {
        val productId = adjustment.lineIdentity?.let { productByLineKey[it.key()] }
        val adjustmentCreateDto = SaleAdjustmentCreateDto(
            locationProductId = productId,
            direction = adjustment.direction,
            calculationMethod = adjustment.calculationMethod,
            value = adjustment.value,
            adjustmentReasonId = adjustment.adjustmentReasonId,
            note = adjustment.note,
            approvedById = adjustment.approvedById,
        )
        return AdjustmentAmountCalculator.calculateAmount(adjustmentCreateDto, lines)
    }

    private fun sumOf(
        adjustments: List<SaleSessionAdjustment>,
        direction: AdjustmentDirection,
        lineLevel: Boolean,
        amounts: Map<UUID, BigDecimal>,
    ): BigDecimal = adjustments
        .filter { it.direction == direction && (it.lineIdentity != null) == lineLevel }
        .sumOf { amounts.getValue(it.identity.key()) }
}
