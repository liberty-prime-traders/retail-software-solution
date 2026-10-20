package me.ezra_home.retail_software_solution.locations.business.supplier_payment

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class PaymentsCalculatorService(
    private val activeSupplierPaymentRepository: ActiveSupplierPaymentRepository
) {

    fun calculatePaidAmountForPurchase(purchaseId: UUID): BigDecimal {
        return activeSupplierPaymentRepository.findByPurchaseId(purchaseId).sumOf { it.amount }
    }

    fun calculatePaidAmountForDelivery(deliveryId: UUID): BigDecimal {
        return activeSupplierPaymentRepository.findByDeliveryId(deliveryId).sumOf { it.amount }
    }
}
