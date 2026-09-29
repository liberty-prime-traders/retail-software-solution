package me.ezra_home.retail_software_solution.locations.business.supplier_payment

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class PaymentsCalculatorService(
    private val supplierPaymentRepository: SupplierPaymentRepository,
    private val supplierPaymentVoidRepository: SupplierPaymentVoidRepository
) {

    fun calculatePaidAmountForPurchase(purchaseId: UUID): BigDecimal {
        val payments = supplierPaymentRepository.findByPurchaseId(purchaseId)
        return calculatePaidAmountForPayments(payments)
    }

    fun calculatePaidAmountForDelivery(deliveryId: UUID): BigDecimal {
        val payments = supplierPaymentRepository.findByDeliveryId(deliveryId)
        return calculatePaidAmountForPayments(payments)
    }

    private fun calculatePaidAmountForPayments(payments: List<SupplierPaymentEntity>): BigDecimal {
        if (payments.isEmpty()) return BigDecimal.ZERO
        val voidedPaymentIds = supplierPaymentVoidRepository
            .findBySupplierPaymentIdIn(payments.map { it.id!! })
            .mapTo(HashSet()) { it.supplierPaymentId }
        return payments.filter { it.id !in voidedPaymentIds }.sumOf { it.amount }
    }
}
