package me.ezra_home.retail_software_solution.locations.business.supplier_payment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.purchase.api.payment.PurchasePaymentCeilingService
import me.ezra_home.retail_software_solution.locations.business.purchase.api.payment.PurchasePaymentCeilingService.PaymentCeiling
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseUpdater
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.PaymentsCalculatorService
import me.ezra_home.retail_software_solution.util.business.PaymentStatusResolver
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class PurchasePaymentStatusService(
    private val purchasePaymentCeilingService: PurchasePaymentCeilingService,
    private val purchaseUpdater: PurchaseUpdater,
    private val paymentsCalculatorService: PaymentsCalculatorService
) {

    fun patchThenReturnPaymentStatus(purchaseId: UUID): PaymentStatus =
        patchPaymentStatus(purchaseId, paymentsCalculatorService.calculatePaidAmountForPurchase(purchaseId))

    fun patchThenReturnPaymentStatusAfterVoid(purchaseId: UUID, paidTotalBeforeVoid: BigDecimal, voidedAmount: BigDecimal): PaymentStatus =
        patchPaymentStatus(purchaseId, PaymentStatusResolver.paidTotalAfterVoid(paidTotalBeforeVoid, voidedAmount))

    fun patchThenReturnPaymentStatus(purchaseId: UUID, paymentCeiling: BigDecimal): PaymentStatus =
        patchPaymentStatus(
            purchaseId,
            paymentsCalculatorService.calculatePaidAmountForPurchase(purchaseId),
            paymentCeiling
        )

    private fun patchPaymentStatus(
        purchaseId: UUID,
        totalPaid: BigDecimal,
        paymentCeiling: BigDecimal = purchasePaymentCeilingService.computeCeiling(purchaseId).amount
    ): PaymentStatus {
        val status = resolvePaymentStatus(totalPaid, paymentCeiling)
        purchaseUpdater.updatePaymentStatus(purchaseId, status)
        return status
    }

    fun resolvePaymentStatus(totalPaid: BigDecimal, ceiling: PaymentCeiling): PaymentStatus =
        resolvePaymentStatus(totalPaid, ceiling.amount)

    private fun resolvePaymentStatus(totalPaid: BigDecimal, paymentCeiling: BigDecimal): PaymentStatus {
        return when {
            totalPaid.compareTo(BigDecimal.ZERO) == 0 -> PaymentStatus.UNPAID
            totalPaid > paymentCeiling -> PaymentStatus.OVERPAID
            totalPaid.compareTo(paymentCeiling) == 0 -> PaymentStatus.FULLY_SETTLED
            else -> PaymentStatus.PARTIALLY_SETTLED
        }
    }
}
