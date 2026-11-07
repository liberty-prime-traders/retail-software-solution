package me.ezra_home.retail_software_solution.locations.business.delivery.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryLineEntity
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryLineRepository
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryRepository
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryRevisionValidator
import me.ezra_home.retail_software_solution.locations.business.purchase.api.DeliveryHandlerForPurchase
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDeliveryLineStatus
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Service

// Writes the line rows only; stock, ledger and purchase totals are the caller's. The caller must hold the product lock for the checks to hold.
@Service
@TransactionalOnLocationSchema
class PurchaseDeliveryLineReplacer(
    private val purchaseDeliveryLineRepository: PurchaseDeliveryLineRepository,
    private val purchaseDeliveryRepository: PurchaseDeliveryRepository,
    private val purchaseDeliveryRevisionValidator: PurchaseDeliveryRevisionValidator,
    private val deliveryHandlerForPurchase: DeliveryHandlerForPurchase,
) {

    fun replace(purchaseDeliveryLineReplaceRequest: PurchaseDeliveryLineReplaceRequest): ReplacementDeliveryLineDto {
        val revisedLine = purchaseDeliveryLineRepository.getReferenceById(purchaseDeliveryLineReplaceRequest.revisedDeliveryLineId)
        val delivery = purchaseDeliveryRepository.getReferenceById(revisedLine.purchaseDeliveryId)
        val purchaseLineDto = deliveryHandlerForPurchase.getDeliveryContext(delivery.purchaseId)
            .purchaseLineById.getValue(revisedLine.purchaseLineId)
        purchaseDeliveryRevisionValidator.validate(
            delivery.requiredReference(),
            purchaseLineDto,
            revisedLine,
            ExactQuantity.ofStorageFromUserProvided(
                purchaseDeliveryLineReplaceRequest.userProvidedQuantityDelivered.toBigDecimal(),
                revisedLine.storageUnitsPerUserProvidedUnit
            )
        )

        revisedLine.status = PurchaseDeliveryLineStatus.REVISED
        val replacementLine = purchaseDeliveryLineRepository.save(
            PurchaseDeliveryLineEntity(
                purchaseDeliveryId = revisedLine.purchaseDeliveryId,
                purchaseLineId = revisedLine.purchaseLineId,
                userProvidedQuantityDelivered = purchaseDeliveryLineReplaceRequest.userProvidedQuantityDelivered.toBigDecimal(),
                unitCost = purchaseDeliveryLineReplaceRequest.unitCost.toBigDecimal(),
                unitId = revisedLine.unitId,
                storageUnitsPerUserProvidedUnit = revisedLine.storageUnitsPerUserProvidedUnit,
            )
        )
        purchaseDeliveryLineRepository.save(revisedLine)

        return ReplacementDeliveryLineDto(
            deliveryReferenceNumber = delivery.requiredReference(),
            revisedLineReferenceNumber = revisedLine.requiredReference(),
            replacementLineReferenceNumber = replacementLine.requiredReference(),
            purchaseLineId = replacementLine.purchaseLineId,
            userProvidedQuantityDelivered = replacementLine.userProvidedQuantityDelivered,
            unitCost = replacementLine.unitCost,
            unitId = replacementLine.unitId,
            storageUnitsPerUserProvidedUnit = replacementLine.storageUnitsPerUserProvidedUnit,
        )
    }
}
