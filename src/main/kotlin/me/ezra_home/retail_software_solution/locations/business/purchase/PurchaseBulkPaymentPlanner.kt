package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.locations.business.purchase.api.payment.PurchasePayableCalculator
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher.PurchaseInfo
import me.ezra_home.retail_software_solution.locations.business.purchase.api.bulk_payment.PurchaseBulkPaymentCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.payment.PurchasePaymentCeilingService
import me.ezra_home.retail_software_solution.locations.business.purchase.api.payment.PurchasePaymentCeilingService.PaymentCeiling
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentDataFetcher
import me.ezra_home.retail_software_solution.util.business.DisplayFormatters
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

data class PlannedPurchasePayment(val purchaseId: UUID, val amount: BigDecimal)

data class PurchaseBulkPaymentPlan(
  val plannedPayments: List<PlannedPurchasePayment>,
  val purchaseEntities: List<PurchaseEntity>,
  val purchaseInfosByPurchaseId: Map<UUID, PurchaseInfo>,
  val ceilingsByPurchaseId: Map<UUID, PaymentCeiling>,
  val alreadyPaidByPurchaseId: Map<UUID, BigDecimal>
)

@Component
class PurchaseBulkPaymentPlanner(
  private val purchaseRepository: PurchaseRepository,
  private val purchasePaymentCeilingService: PurchasePaymentCeilingService,
  private val supplierPaymentDataFetcher: SupplierPaymentDataFetcher
) {

  fun plan(purchaseBulkPaymentCreateDto: PurchaseBulkPaymentCreateDto): PurchaseBulkPaymentPlan {
    val purchaseEntitiesOldestFirst = requirePayablePurchases(purchaseBulkPaymentCreateDto)
      .sortedWith(PurchaseOrdering.OLDEST_FIRST)
    val purchaseIdsOldestFirst = purchaseEntitiesOldestFirst.map { it.id!! }
    val purchaseIds = purchaseIdsOldestFirst.toSet()
    val ceilingsByPurchaseId = purchasePaymentCeilingService.computeCeilings(purchaseIds)
    val alreadyPaidByPurchaseId = supplierPaymentDataFetcher.getPaidAmountsByPurchaseIds(purchaseIds)
    val payablesByPurchaseId = purchaseIdsOldestFirst.associateWith { purchaseId ->
      PurchasePayableCalculator.calculatePayable(
        ceilingsByPurchaseId.getValue(purchaseId).amount, alreadyPaidByPurchaseId.getValue(purchaseId)
      )
    }

    val amount = purchaseBulkPaymentCreateDto.amount.toBigDecimal()
    val totalPayable = payablesByPurchaseId.values.sumOf { it }
    if (amount > totalPayable) {
      throw RtsGenericException(
        "Amount ${DisplayFormatters.formatCurrency(amount)} exceeds the total payable of " +
          DisplayFormatters.formatCurrency(totalPayable)
      )
    }

    var remainingAmount = amount
    val plannedPayments = mutableListOf<PlannedPurchasePayment>()
    for ((purchaseId, payable) in payablesByPurchaseId) {
      if (remainingAmount.signum() == 0) break
      if (payable.signum() == 0) continue
      val allocatedAmount = remainingAmount.min(payable)
      plannedPayments.add(PlannedPurchasePayment(purchaseId, allocatedAmount))
      remainingAmount -= allocatedAmount
    }
    return PurchaseBulkPaymentPlan(
      plannedPayments,
      purchaseEntitiesOldestFirst,
      purchaseEntitiesOldestFirst.associate { it.id!! to PurchaseInfo.from(it) },
      ceilingsByPurchaseId,
      alreadyPaidByPurchaseId
    )
  }

  private fun requirePayablePurchases(
    purchaseBulkPaymentCreateDto: PurchaseBulkPaymentCreateDto
  ): List<PurchaseEntity> {
    val purchases = purchaseRepository.findAllById(purchaseBulkPaymentCreateDto.purchaseIds)

    val foundPurchaseIds = purchases.map { it.id!! }.toSet()
    val missingPurchaseIds = purchaseBulkPaymentCreateDto.purchaseIds.filterNot { it in foundPurchaseIds }
    val foreignPurchaseIds = purchases
      .filter { it.supplierId != purchaseBulkPaymentCreateDto.supplierId }
      .map { it.id!! }
    if (missingPurchaseIds.isNotEmpty() || foreignPurchaseIds.isNotEmpty()) {
      throw RtsGenericException(
        "Purchases not found or not owned by the supplier: " +
          (missingPurchaseIds + foreignPurchaseIds).joinToString()
      )
    }
    val unpayableStatusPurchaseIds = purchases
      .filter { it.purchaseStatus !in PayablePurchaseFinder.PAYABLE_PURCHASE_STATUSES }
      .map { it.id!! }
    if (unpayableStatusPurchaseIds.isNotEmpty()) {
      throw RtsGenericException(
        "Draft or canceled purchases cannot be paid: ${unpayableStatusPurchaseIds.joinToString()}"
      )
    }
    return purchases
  }
}
