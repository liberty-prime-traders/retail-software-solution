package me.ezra_home.retail_software_solution.locations.business.supplier_payment

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class PaymentsCalculatorService(
    private val activeSupplierPaymentRepository: ActiveSupplierPaymentRepository
) {

    fun calculatePaidAmountForPurchase(purchaseId: UUID): BigDecimal {
        return calculatePaidAmountsForPurchases(setOf(purchaseId)).getValue(purchaseId)
    }

    fun calculatePaidAmountsForPurchases(purchaseIds: Set<UUID>): Map<UUID, BigDecimal> {
        val paidAmountsByPurchaseId = activeSupplierPaymentRepository.sumAmountsByPurchaseIdIn(purchaseIds)
            .associate { it.purchaseId to it.paidAmount }
        return purchaseIds.associateWith { paidAmountsByPurchaseId[it] ?: BigDecimal.ZERO }
    }

    fun calculatePaidAmountForDelivery(deliveryId: UUID): BigDecimal {
        return activeSupplierPaymentRepository.sumAmountByDeliveryId(deliveryId)
    }
}
