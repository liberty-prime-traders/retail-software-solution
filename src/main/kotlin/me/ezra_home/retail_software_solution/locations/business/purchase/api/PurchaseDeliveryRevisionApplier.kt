package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseAssembler
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseRepository
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseStatusResolver
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.PurchasePaymentStatusService
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

@Service
class PurchaseDeliveryRevisionApplier(
    private val purchaseRepository: PurchaseRepository,
    private val purchaseLineRepository: PurchaseLineRepository,
    private val purchaseAssembler: PurchaseAssembler,
    private val purchasePaymentStatusService: PurchasePaymentStatusService,
) {

    // The caller holds the purchase lock and has already saved the replacement delivery line, so the ceiling sums
    // see it.
    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun apply(
        purchaseId: UUID,
        purchaseLineId: UUID,
        storageQuantityDelivered: ExactQuantity
    ): PurchaseResponseDto {
        val purchase = purchaseRepository.getReferenceById(purchaseId)
        val purchaseLines = purchaseLineRepository.findByPurchaseId(purchaseId)
        val purchaseLine = purchaseLines.first { it.id == purchaseLineId }
        purchaseLine.storageQuantityDelivered = storageQuantityDelivered.toStorageQuantity()
        purchaseLineRepository.save(purchaseLine)
        purchase.purchaseStatus = PurchaseStatusResolver.resolveAfterQuantityChange(purchaseLines)
        purchaseRepository.save(purchase)

        val purchaseResponse = purchaseAssembler.buildResponse(purchase, purchaseLines)
        return purchaseResponse.copy(
            paymentStatus = purchasePaymentStatusService.patchThenReturnPaymentStatus(
                purchaseId,
                purchaseResponse.paymentCeiling
            )
        )
    }
}
