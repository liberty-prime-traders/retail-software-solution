package me.ezra_home.retail_software_solution.locations.business.delivery.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryLineEntity
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryLineRepository
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryRepository
import me.ezra_home.retail_software_solution.locations.business.delivery.PurchaseDeliveryRevisionValidator
import me.ezra_home.retail_software_solution.locations.business.purchase.api.delivery.DeliveryHandlerForPurchase
import me.ezra_home.retail_software_solution.locations.business.purchase.api.delivery.PurchaseDeliveryLineStatus
import me.ezra_home.retail_software_solution.util.exceptions.BlockedChangeException
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

// Writes the line rows only; stock, ledger and purchase totals are the caller's. The caller must hold the product lock
// for the checks to hold.
@Service
@TransactionalOnLocationSchema
class PurchaseDeliveryLineReplacer(
    private val purchaseDeliveryLineRepository: PurchaseDeliveryLineRepository,
    private val purchaseDeliveryRepository: PurchaseDeliveryRepository,
    private val purchaseDeliveryRevisionValidator: PurchaseDeliveryRevisionValidator,
    private val deliveryHandlerForPurchase: DeliveryHandlerForPurchase,
) {

    @TransactionalOnLocationSchema(readOnly = true)
    fun validateRevisionReportingProblems(
        purchaseDeliveryLineReplaceRequest: PurchaseDeliveryLineReplaceRequest
    ): ValidatedDeliveryLineRevisionDto {
        val revisedLine = requiredRevisedLine(purchaseDeliveryLineReplaceRequest.revisedDeliveryLineId)
        val delivery = purchaseDeliveryRepository.getReferenceById(revisedLine.purchaseDeliveryId)
        val purchaseLineDto = deliveryHandlerForPurchase.getDeliveryContext(delivery.purchaseId)
            .purchaseLineById.getValue(revisedLine.purchaseLineId)
        val replacementUserProvidedQuantity =
            purchaseDeliveryLineReplaceRequest.userProvidedQuantityDelivered.toBigDecimal()
        val replacementStorageQuantity = ExactQuantity.ofStorageFromUserProvided(
            replacementUserProvidedQuantity, revisedLine.storageUnitsPerUserProvidedUnit
        )
        val problems = purchaseDeliveryRevisionValidator.problemsOf(
            delivery.requiredReference(), purchaseLineDto,
            revisedLine, replacementStorageQuantity
        )
        return ValidatedDeliveryLineRevisionDto(
            revisedDeliveryLineId = purchaseDeliveryLineReplaceRequest.revisedDeliveryLineId,
            deliveryReferenceNumber = delivery.requiredReference(),
            revisedLineReferenceNumber = revisedLine.requiredReference(),
            purchaseLineId = revisedLine.purchaseLineId,
            locationProductId = purchaseLineDto.locationProductId,
            unitId = revisedLine.unitId,
            storageUnitsPerUserProvidedUnit = revisedLine.storageUnitsPerUserProvidedUnit,
            revisedUserProvidedQuantityDelivered = revisedLine.userProvidedQuantityDelivered,
            revisedUnitCost = revisedLine.unitCost,
            userProvidedQuantityDelivered = replacementUserProvidedQuantity,
            unitCost = purchaseDeliveryLineReplaceRequest.unitCost.toBigDecimal(),
            problems = problems,
        )
    }

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun replace(validatedDeliveryLineRevision: ValidatedDeliveryLineRevisionDto): ReplacementDeliveryLineDto {
        BlockedChangeException.throwIfAny(validatedDeliveryLineRevision.problems)
        val revisedLine = requiredRevisedLine(validatedDeliveryLineRevision.revisedDeliveryLineId)

        revisedLine.status = PurchaseDeliveryLineStatus.REVISED
        val replacementLine = purchaseDeliveryLineRepository.save(
            PurchaseDeliveryLineEntity(
                purchaseDeliveryId = revisedLine.purchaseDeliveryId,
                purchaseLineId = revisedLine.purchaseLineId,
                userProvidedQuantityDelivered = validatedDeliveryLineRevision.userProvidedQuantityDelivered,
                unitCost = validatedDeliveryLineRevision.unitCost,
                unitId = revisedLine.unitId,
                storageUnitsPerUserProvidedUnit = revisedLine.storageUnitsPerUserProvidedUnit,
            )
        )
        purchaseDeliveryLineRepository.save(revisedLine)

        return ReplacementDeliveryLineDto(
            deliveryReferenceNumber = validatedDeliveryLineRevision.deliveryReferenceNumber,
            revisedLineReferenceNumber = revisedLine.requiredReference(),
            replacementLineReferenceNumber = replacementLine.requiredReference(),
            purchaseLineId = replacementLine.purchaseLineId,
            userProvidedQuantityDelivered = replacementLine.userProvidedQuantityDelivered,
            unitCost = replacementLine.unitCost,
            unitId = replacementLine.unitId,
            storageUnitsPerUserProvidedUnit = replacementLine.storageUnitsPerUserProvidedUnit,
        )
    }

    private fun requiredRevisedLine(revisedDeliveryLineId: UUID): PurchaseDeliveryLineEntity =
        purchaseDeliveryLineRepository.findById(revisedDeliveryLineId).orElseThrow {
            RtsGenericException("That delivery line could not be found. Refresh the page and try again.")
        }
}
