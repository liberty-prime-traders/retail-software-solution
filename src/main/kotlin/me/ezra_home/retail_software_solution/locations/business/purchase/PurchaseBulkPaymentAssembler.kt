package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.locations.business.purchase.api.payment.PurchasePayableCalculator
import me.ezra_home.retail_software_solution.locations.business.purchase.api.bulk_payment.PurchaseBulkPaymentResponseDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.payment.PurchasePaymentCeilingService.PaymentCeiling
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentResponseDto
import java.math.BigDecimal
import java.util.UUID

object PurchaseBulkPaymentAssembler {

  fun buildResponses(
    purchaseEntities: List<PurchaseEntity>,
    ceilingsByPurchaseId: Map<UUID, PaymentCeiling>,
    paidTotalsByPurchaseId: Map<UUID, BigDecimal>,
    newSupplierPaymentsByPurchaseId: Map<UUID, SupplierPaymentResponseDto> = emptyMap()
  ): List<PurchaseBulkPaymentResponseDto> =
    purchaseEntities.map { purchaseEntity ->
      val purchaseId = purchaseEntity.id!!
      val ceiling = ceilingsByPurchaseId.getValue(purchaseId)
      val paidTotal = paidTotalsByPurchaseId.getValue(purchaseId)
      val newPayment = newSupplierPaymentsByPurchaseId[purchaseId]
      PurchaseBulkPaymentResponseDto(
        id = purchaseId,
        referenceNumber = purchaseEntity.requiredReference(),
        purchaseStatus = purchaseEntity.purchaseStatus,
        dateOrdered = purchaseEntity.dateOrdered,
        orderedTotal = ceiling.orderedTotal,
        paymentCeiling = ceiling.amount,
        paidTotal = paidTotal,
        payable = PurchasePayableCalculator.calculatePayable(ceiling.amount, paidTotal),
        paymentStatus = newPayment?.updatedPurchasePaymentStatus ?: purchaseEntity.paymentStatus,
        appliedAmount = newPayment?.amount ?: BigDecimal.ZERO,
        supplierPaymentId = newPayment?.id,
        paymentReferenceNumber = newPayment?.referenceNumber
      )
    }
}
