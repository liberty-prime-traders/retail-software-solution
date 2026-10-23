package me.ezra_home.retail_software_solution.locations.business.supplier_payment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchasePaymentCeilingService
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchasePaymentCeilingService.PaymentCeiling
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

    private fun patchPaymentStatus(purchaseId: UUID, totalPaid: BigDecimal): PaymentStatus {
        val ceiling = purchasePaymentCeilingService.computeCeiling(purchaseId)
        val status = resolvePaymentStatus(totalPaid, ceiling)
        purchaseUpdater.updatePaymentStatus(purchaseId, status)
        return status
    }

    fun resolvePaymentStatus(totalPaid: BigDecimal, ceiling: PaymentCeiling): PaymentStatus {
        return when {
            totalPaid.compareTo(BigDecimal.ZERO) == 0 -> PaymentStatus.UNPAID
            totalPaid > ceiling.amount -> PaymentStatus.OVERPAID
            totalPaid.compareTo(ceiling.amount) == 0 -> PaymentStatus.FULLY_SETTLED
            else -> PaymentStatus.PARTIALLY_SETTLED
        }
    }
}
