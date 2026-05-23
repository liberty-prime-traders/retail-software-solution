package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentValidator
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

    fun validate(saleSession: SaleSession) {
        guardPositiveLineQuantities(saleSession.saleLines)
        guardNoDuplicateProducts(saleSession.saleLines)
        guardAdjustmentReferences(saleSession)
        guardAdjustmentReasons(saleSession.saleAdjustments)
        guardPositivePayments(saleSession.salePayments.map { it.amount })
        guardDiscountCeilings(saleSession)
    }

    fun guardMutable(saleSession: SaleSession) {
        if (!saleSession.mutable()) {
            throw RtsGenericException(
                "Sale session is read-only because the underlying sale is ${saleSession.originalStatus}"
            )
        }
    }

    fun canAddPayments(saleSession: SaleSession) {
        if (!saleSession.canAddPayments()) {
            throw RtsGenericException(
                "Cannot add a payment because the sale is ${saleSession.originalStatus}"
            )
        }
    }

    fun guardNonEmptyLines(saleSession: SaleSession) {
        if (saleSession.saleLines.isEmpty()) {
            throw RtsGenericException("Sale must have at least one line")
        }
    }

    fun guardWalkInFullyCovered(saleSession: SaleSession) {
        if (saleSession.header.contactId != SystemContact.WALK_IN.id) return
        if (saleSession.totals.paymentTotal < saleSession.totals.payableTotal) {
            throw RtsGenericException("Walk-in sales require full payment coverage")
        }
    }

    fun guardPaymentsWithinTotal(saleSession: SaleSession) {
        if (saleSession.totals.payableTotal.signum() <= 0) return
        SalePaymentValidator.guardNotExceedingSaleTotal(
            totalSubmitted = saleSession.totals.paymentTotal,
            saleTotal = saleSession.totals.payableTotal,
        )
    }

    private fun guardPositiveLineQuantities(saleSessionLines: List<SaleSessionLine>) {
        if (saleSessionLines.any { it.quantity.signum() <= 0 }) {
            throw RtsGenericException("Line quantity must be positive")
        }
    }

    private fun guardNoDuplicateProducts(saleSessionLines: List<SaleSessionLine>) {
        val locationProductIds = saleSessionLines.map { it.locationProductId }
        if (locationProductIds.size != locationProductIds.toSet().size) {
            throw RtsGenericException("Duplicate products are not allowed in a sale")
        }
    }

    private fun guardAdjustmentReferences(saleSession: SaleSession) {
        val saleSessionLineKeys = saleSession.saleLines.mapTo(HashSet()) { it.identity.key() }
        saleSession.saleAdjustments.forEach { saleSessionAdjustment ->
            val targetLineKey = saleSessionAdjustment.relatedSaleLineIdentity?.key() ?: return@forEach
            if (targetLineKey !in saleSessionLineKeys) {
                throw RtsGenericException("Adjustment references a line that is not on the sale")
            }
        }
    }

    private fun guardAdjustmentReasons(saleSessionAdjustments: List<SaleSessionAdjustment>) {
        saleSessionAdjustments.forEach { saleSessionAdjustment ->
            adjustmentReasonService.requireCanApply(
                saleSessionAdjustment.adjustmentReasonId,
                saleSessionAdjustment.direction,
            )
        }
    }

    private fun guardPositivePayments(paymentAmounts: List<BigDecimal>) {
        paymentAmounts.forEach { SalePaymentValidator.guardPositiveAmount(it) }
    }

    private fun guardDiscountCeilings(saleSession: SaleSession) {
        val totals = saleSessionTotalsCalculator.compute(
            saleSession.saleLines, saleSession.saleAdjustments, saleSession.totalPaid()
        )
        guardLineDiscountCeilings(saleSession)
        val subtotal = totals.subtotal
        val remainingSubtotalAfterLineDiscounts = subtotal - totals.lineLevelDiscountTotal
        if (totals.orderLevelDiscountTotal > remainingSubtotalAfterLineDiscounts) {
            throw RtsGenericException(
                "Order-level discount total of ${Currencies.format(totals.orderLevelDiscountTotal)} exceeds " +
                        "remaining subtotal ${Currencies.format(remainingSubtotalAfterLineDiscounts)} " +
                        "(subtotal ${Currencies.format(subtotal)} " +
                        "minus line discounts ${Currencies.format(totals.lineLevelDiscountTotal)})."
            )
        }
    }

    private fun guardLineDiscountCeilings(saleSession: SaleSession) {
        val saleSessionLinesByKey = saleSession.saleLines.associateBy { it.identity.key() }
        val lineDiscountsByLineKey = saleSession.saleAdjustments
            .filter { it.direction == AdjustmentDirection.DISCOUNT && it.relatedSaleLineIdentity != null }
            .groupBy { it.relatedSaleLineIdentity!!.key() }
        lineDiscountsByLineKey.forEach { (targetLineKey, lineDiscounts) ->
            val targetSaleSessionLine = saleSessionLinesByKey[targetLineKey]
                ?: throw RtsGenericException("Adjustment references a line that is not on the sale")
            val totalDiscountAmount = lineDiscounts.sumOf {
                saleSessionTotalsCalculator.calculatedAmount(it, saleSession.saleLines)
            }
            val lineTotal = targetSaleSessionLine.lineTotal
            if (totalDiscountAmount > lineTotal) {
                throw RtsGenericException(
                    "On ${targetSaleSessionLine.productLabel}, total discounts of " +
                            "${Currencies.format(totalDiscountAmount)} exceed " +
                            "line total of ${Currencies.format(lineTotal)}."
                )
            }
        }
    }

}
