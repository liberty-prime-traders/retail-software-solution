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

    fun recompute(saleSession: SaleSession): SaleSession {
        val totals = compute(saleSession.saleLines, saleSession.saleAdjustments, saleSession.totalPaid())
        return saleSession.copy(totals = totals)
    }

    fun compute(
        saleSessionLines: List<SaleSessionLine>,
        saleSessionAdjustments: List<SaleSessionAdjustment>,
        paymentTotal: BigDecimal,
    ): SaleSessionTotals {
        val subtotal = saleSessionLines.sumOf { it.lineTotal }
        val locationProductIdBySaleSessionLineKey = saleSessionLines.associate { it.identity.key() to it.locationProductId }
        val calculatedAmountByAdjustmentKey = saleSessionAdjustments.associate { saleSessionAdjustment ->
            saleSessionAdjustment.identity.key() to calculateAdjustmentAmount(
                saleSessionAdjustment,
                locationProductIdBySaleSessionLineKey,
                saleSessionLines,
            )
        }
        val lineLevelDiscount = sumAdjustmentAmounts(
            saleSessionAdjustments, AdjustmentDirection.DISCOUNT, lineLevel = true, calculatedAmountByAdjustmentKey
        )
        val orderLevelDiscount = sumAdjustmentAmounts(
            saleSessionAdjustments, AdjustmentDirection.DISCOUNT, lineLevel = false, calculatedAmountByAdjustmentKey
        )
        val lineLevelSurcharge = sumAdjustmentAmounts(
            saleSessionAdjustments, AdjustmentDirection.SURCHARGE, lineLevel = true, calculatedAmountByAdjustmentKey
        )
        val orderLevelSurcharge = sumAdjustmentAmounts(
            saleSessionAdjustments, AdjustmentDirection.SURCHARGE, lineLevel = false, calculatedAmountByAdjustmentKey
        )
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

    fun calculatedAmount(
        saleSessionAdjustment: SaleSessionAdjustment,
        saleSessionLines: List<SaleSessionLine>,
    ): BigDecimal {
        val locationProductIdBySaleSessionLineKey = saleSessionLines.associate { it.identity.key() to it.locationProductId }
        return calculateAdjustmentAmount(saleSessionAdjustment, locationProductIdBySaleSessionLineKey, saleSessionLines)
    }

    private fun calculateAdjustmentAmount(
        saleSessionAdjustment: SaleSessionAdjustment,
        locationProductIdBySaleSessionLineKey: Map<UUID, UUID>,
        saleSessionLines: List<SaleSessionLine>,
    ): BigDecimal {
        val locationProductId = saleSessionAdjustment.relatedSaleLineIdentity?.let {
            locationProductIdBySaleSessionLineKey[it.key()]
        }
        val adjustmentCreateDto = SaleAdjustmentCreateDto(
            locationProductId = locationProductId,
            direction = saleSessionAdjustment.direction,
            calculationMethod = saleSessionAdjustment.calculationMethod,
            value = saleSessionAdjustment.value,
            adjustmentReasonId = saleSessionAdjustment.adjustmentReasonId,
            note = saleSessionAdjustment.note,
            approvedById = saleSessionAdjustment.approvedById,
        )
        return AdjustmentAmountCalculator.calculateAmount(adjustmentCreateDto, saleSessionLines)
    }

    private fun sumAdjustmentAmounts(
        saleSessionAdjustments: List<SaleSessionAdjustment>,
        direction: AdjustmentDirection,
        lineLevel: Boolean,
        calculatedAmountByAdjustmentKey: Map<UUID, BigDecimal>,
    ): BigDecimal = saleSessionAdjustments
        .filter { it.direction == direction && (it.relatedSaleLineIdentity != null) == lineLevel }
        .sumOf { calculatedAmountByAdjustmentKey.getValue(it.identity.key()) }
}
