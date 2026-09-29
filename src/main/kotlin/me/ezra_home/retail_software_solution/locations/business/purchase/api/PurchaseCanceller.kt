package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseAssembler
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseValidator
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.PurchasePaymentStatusService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class PurchaseCanceller(
    private val purchaseRepository: PurchaseRepository,
    private val purchaseLineRepository: PurchaseLineRepository,
    private val purchaseAssembler: PurchaseAssembler,
    private val purchasePaymentStatusService: PurchasePaymentStatusService,
    private val purchaseDataFetcher: PurchaseDataFetcher,
) {

    fun cancel(purchaseId: UUID, lines: List<PurchaseCancelLinesDto>): PurchaseResponseDto {
        val purchase = purchaseDataFetcher.lockAndGetPurchase(purchaseId)
        PurchaseValidator.guardCanCancelLines(purchase)
        val existingLines = purchaseLineRepository.findByPurchaseId(purchaseId)
        cancelLines(existingLines, lines)
        purchase.purchaseStatus = resolvePurchaseStatus(existingLines) ?: purchase.purchaseStatus
        purchaseRepository.save(purchase)
        val paymentStatus = purchasePaymentStatusService.patchThenReturnPaymentStatus(purchaseId)
        return purchaseAssembler.buildResponse(purchase, existingLines).copy(paymentStatus = paymentStatus)
    }

    private fun cancelLines(existingLines: List<PurchaseLineEntity>, cancels: List<PurchaseCancelLinesDto>) {
        val linesById = existingLines.associateBy { it.id!! }
        val toSave = cancels.mapNotNull { cancel ->
            linesById[cancel.purchaseLineId]?.also { line ->
                PurchaseValidator.guardCancelQuantity(line, cancel.quantityCanceled)
                line.quantityCanceled = cancel.quantityCanceled
            }
        }
        purchaseLineRepository.saveAll(toSave)
    }

    private fun resolvePurchaseStatus(lines: List<PurchaseLineEntity>): PurchaseStatus? {
        val allAccountedFor = lines.all { (it.quantityDelivered + it.quantityCanceled).compareTo(it.quantityOrdered) == 0 }
        val anyDelivered = lines.any { it.quantityDelivered > BigDecimal.ZERO }
        return when {
            allAccountedFor && !anyDelivered -> PurchaseStatus.CANCELED
            allAccountedFor -> PurchaseStatus.FULLY_DELIVERED
            anyDelivered -> PurchaseStatus.PARTIALLY_DELIVERED
            else -> null
        }
    }
}
