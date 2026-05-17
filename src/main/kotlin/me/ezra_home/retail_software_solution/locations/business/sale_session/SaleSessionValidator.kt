package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentReasonService
import me.ezra_home.retail_software_solution.util.business.Currencies
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class SaleSessionValidator(
    private val adjustmentReasonService: AdjustmentReasonService,
    private val saleSessionTotalsCalculator: SaleSessionTotalsCalculator,
) {

    fun validate(session: SaleSession) {
        guardPositiveLineQuantities(session.lines)
        guardNoDuplicateProducts(session.lines)
        guardAdjustmentReferences(session)
        guardAdjustmentReasons(session.adjustments)
        guardPositivePayments(session.payments.map { it.amount })
        guardDiscountCeilings(session)
    }

    fun guardWalkInFullyCovered(session: SaleSession) {
        if (session.header.contactId != SystemContact.WALK_IN.id) return
        if (session.totals.paymentTotal < session.totals.payableTotal) {
            throw RtsGenericException("Walk-in sales require full payment coverage")
        }
    }

    fun guardPaymentsWithinTotal(session: SaleSession) {
        if (session.totals.payableTotal.signum() <= 0) return
        if (session.totals.paymentTotal > session.totals.payableTotal) {
            throw RtsGenericException(
                "Payments of ${Currencies.format(session.totals.paymentTotal)} exceed payable total " +
                        "${Currencies.format(session.totals.payableTotal)}"
            )
        }
    }

    private fun guardPositiveLineQuantities(lines: List<SaleSessionLine>) {
        if (lines.any { it.quantity.signum() <= 0 }) {
            throw RtsGenericException("Line quantity must be positive")
        }
    }

    private fun guardNoDuplicateProducts(lines: List<SaleSessionLine>) {
        val productIds = lines.map { it.locationProductId }
        if (productIds.size != productIds.toSet().size) {
            throw RtsGenericException("Duplicate products are not allowed in a sale")
        }
    }

    private fun guardAdjustmentReferences(session: SaleSession) {
        val lineKeys = session.lines.mapTo(HashSet()) { it.id.key() }
        session.adjustments.forEach { adj ->
            val lineKey = adj.lineId?.key() ?: return@forEach
            if (lineKey !in lineKeys) {
                throw RtsGenericException("Adjustment references a line that is not on the sale")
            }
        }
    }

    private fun guardAdjustmentReasons(adjustments: List<SaleSessionAdjustment>) {
        adjustments.forEach { adj ->
            adjustmentReasonService.requireCanApply(adj.adjustmentReasonId, adj.direction)
        }
    }

    private fun guardPositivePayments(amounts: List<BigDecimal>) {
        if (amounts.any { it.signum() <= 0 }) {
            throw RtsGenericException("Payment amount must be positive")
        }
    }

    private fun guardDiscountCeilings(session: SaleSession) {
        val totals = saleSessionTotalsCalculator.compute(
            session.lines, session.adjustments, session.payments.sumOf { it.amount },
        )
        guardLineDiscountCeilings(session)
        val subtotal = totals.subtotal
        val remaining = subtotal - totals.lineLevelDiscountTotal
        if (totals.orderLevelDiscountTotal > remaining) {
            throw RtsGenericException(
                "Order-level discount total of ${Currencies.format(totals.orderLevelDiscountTotal)} exceeds " +
                        "remaining subtotal ${Currencies.format(remaining)} (subtotal ${Currencies.format(subtotal)} " +
                        "minus line discounts ${Currencies.format(totals.lineLevelDiscountTotal)})."
            )
        }
    }

    private fun guardLineDiscountCeilings(session: SaleSession) {
        val linesByKey = session.lines.associateBy { it.id.key() }
        val discountsByLine = session.adjustments
            .filter { it.direction == AdjustmentDirection.DISCOUNT && it.lineId != null }
            .groupBy { it.lineId!!.key() }
        discountsByLine.forEach { (lineKey, discounts) ->
            val line = linesByKey[lineKey]
                ?: throw RtsGenericException("Adjustment references a line that is not on the sale")
            val totalDiscount = discounts.sumOf { saleSessionTotalsCalculator.calculatedAmount(it, session.lines) }
            val lineTotal = line.lineTotal()
            if (totalDiscount > lineTotal) {
                throw RtsGenericException(
                    "On ${line.productLabel}, total discounts of ${Currencies.format(totalDiscount)} exceed " +
                            "line total of ${Currencies.format(lineTotal)}."
                )
            }
        }
    }

}
