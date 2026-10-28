package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentValidator
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionTotals
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentReasonService
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.SystemAdjustmentReason
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.business.DisplayFormatters
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SaleSessionValidator(
    private val adjustmentReasonService: AdjustmentReasonService,
) {

    fun validate(saleSession: SaleSession) {
        guardNoDuplicateProducts(saleSession.saleLines)
        guardAdjustmentReferences(saleSession)
        guardAdjustmentReasons(saleSession.saleAdjustments)
        guardLineOverrideExclusivity(saleSession)
        val totals = SaleSessionTotalsCalculator.compute(
            saleSession.saleLines, saleSession.saleAdjustments, saleSession.totalPaid()
        )
        guardAmountsWithinMaximum(saleSession, totals)
        guardDiscountCeilings(saleSession, totals)
    }

    private fun guardAmountsWithinMaximum(saleSession: SaleSession, totals: SaleSessionTotals) {
        saleSession.saleLines.forEach {
            Decimals.requireWithinMaximum(it.lineTotal, Decimals.MAXIMUM_TRANSACTION_AMOUNT, "Line total of ${it.productLabel}")
        }
        saleSession.saleAdjustments.forEach {
            val adjustmentAmount = SaleSessionTotalsCalculator.calculateAdjustmentAmount(it, saleSession.saleLines)
            Decimals.requireWithinMaximum(adjustmentAmount, Decimals.MAXIMUM_TRANSACTION_AMOUNT, "Adjustment amount")
        }
        Decimals.requireWithinMaximum(totals.subtotal, Decimals.MAXIMUM_TRANSACTION_AMOUNT, "Sale subtotal")
        Decimals.requireWithinMaximum(totals.receivableTotal, Decimals.MAXIMUM_TRANSACTION_AMOUNT, "Sale total")
    }

    private fun guardLineOverrideExclusivity(saleSession: SaleSession) {
        val priceOverrideReasonId = adjustmentReasonService.getSystemReasonId(SystemAdjustmentReason.PRICE_OVERRIDE)
        val lineKeysWithOverride = saleSession.saleAdjustments
            .filter { it.isPriceOverride(priceOverrideReasonId) }
            .mapTo(HashSet()) { it.requiredRelatedSaleLineKey() }
        if (lineKeysWithOverride.isEmpty()) return
        val saleSessionLinesByKey = saleSession.saleLines.associateBy { it.identity.key() }
        saleSession.saleAdjustments.forEach { saleSessionAdjustment ->
            val targetLineKey = saleSessionAdjustment.relatedSaleLineIdentity?.key() ?: return@forEach
            if (saleSessionAdjustment.adjustmentReasonId == priceOverrideReasonId) return@forEach
            if (targetLineKey !in lineKeysWithOverride) return@forEach
            val productLabel = saleSessionLinesByKey[targetLineKey]?.productLabel ?: "this line"
            throw RtsGenericException(
                "$productLabel has both a price override and another adjustment — " +
                " Remove the override and use adjustments to update price"
            )
        }
    }

    fun guardReasonIsNotPriceOverride(adjustmentReasonId: UUID) {
        if (adjustmentReasonId == adjustmentReasonService.getSystemReasonId(SystemAdjustmentReason.PRICE_OVERRIDE)) {
            throw RtsGenericException("The Price Override reason is applied by changing a line's price, not by adding an adjustment")
        }
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
        if (saleSession.totals.paymentTotal < saleSession.totals.receivableTotal) {
            throw RtsGenericException("Walk-in sales require full payment coverage")
        }
    }

    fun guardPaymentsWithinTotal(saleSession: SaleSession) {
        if (saleSession.totals.receivableTotal.signum() <= 0) return
        SalePaymentValidator.guardNotExceedingSaleTotal(
            totalSubmitted = saleSession.totals.paymentTotal,
            receivableTotal = saleSession.totals.receivableTotal,
        )
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

    private fun guardDiscountCeilings(saleSession: SaleSession, totals: SaleSessionTotals) {
        guardLineDiscountCeilings(saleSession)
        val remainingSubtotalAfterLineDiscounts = totals.subtotal - totals.lineLevelDiscountTotal
        if (totals.orderLevelDiscountTotal > remainingSubtotalAfterLineDiscounts) {
            throw RtsGenericException(
                "Order-level discount total of ${DisplayFormatters.formatCurrency(totals.orderLevelDiscountTotal)} exceeds " +
                        "remaining subtotal ${DisplayFormatters.formatCurrency(remainingSubtotalAfterLineDiscounts)} " +
                        "(subtotal ${DisplayFormatters.formatCurrency(totals.subtotal)} " +
                        "minus line discounts ${DisplayFormatters.formatCurrency(totals.lineLevelDiscountTotal)})."
            )
        }
    }

    private fun guardLineDiscountCeilings(saleSession: SaleSession) {
        val saleSessionLinesByKey = saleSession.saleLines.associateBy { it.identity.key() }
        val lineDiscountsByLineKey = saleSession.saleAdjustments
            .filter { it.direction == AdjustmentDirection.DISCOUNT && it.relatedSaleLineIdentity != null }
            .groupBy { it.requiredRelatedSaleLineKey() }
        lineDiscountsByLineKey.forEach { (targetLineKey, lineDiscounts) ->
            val targetSaleSessionLine = saleSessionLinesByKey[targetLineKey]
                ?: throw RtsGenericException("Adjustment references a line that is not on the sale")
            val totalDiscountAmount = lineDiscounts.sumOf {
                SaleSessionTotalsCalculator.calculateAdjustmentAmount(it, saleSession.saleLines)
            }
            val lineTotal = targetSaleSessionLine.lineTotal
            if (totalDiscountAmount > lineTotal) {
                throw RtsGenericException(
                    "On ${targetSaleSessionLine.productLabel}, total discounts of " +
                            "${DisplayFormatters.formatCurrency(totalDiscountAmount)} exceed " +
                            "line total of ${DisplayFormatters.formatCurrency(lineTotal)}."
                )
            }
        }
    }

}
