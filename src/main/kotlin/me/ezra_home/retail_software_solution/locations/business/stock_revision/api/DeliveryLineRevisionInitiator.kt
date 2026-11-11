package me.ezra_home.retail_software_solution.locations.business.stock_revision.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryDataFetcher
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryLineReplacer
import me.ezra_home.retail_software_solution.locations.business.delivery.api.ReplacementDeliveryLineDto
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.delivery.PurchaseDeliveryRevisionApplier
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseResponseDto
import me.ezra_home.retail_software_solution.locations.business.stock_revision.DeliveryLineRevisionAssessor
import me.ezra_home.retail_software_solution.locations.business.stock_revision.DeliveryLineStockReviser
import me.ezra_home.retail_software_solution.locations.business.stock_revision.RevisionCostPropagator
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.BlockedChangeException
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class DeliveryLineRevisionInitiator(
    private val purchaseDeliveryDataFetcher: PurchaseDeliveryDataFetcher,
    private val purchaseDataFetcher: PurchaseDataFetcher,
    private val purchaseDeliveryLineReplacer: PurchaseDeliveryLineReplacer,
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val purchaseDeliveryRevisionApplier: PurchaseDeliveryRevisionApplier,
    private val deliveryLineRevisionAssessor: DeliveryLineRevisionAssessor,
    private val deliveryLineStockReviser: DeliveryLineStockReviser,
    private val revisionCostPropagator: RevisionCostPropagator,
) {

    fun initiateDeliveryLineRevision(deliveryLineRevisionRequest: DeliveryLineRevisionRequest): PurchaseResponseDto {
        val reason = StringUtils.getValueOrException(
            deliveryLineRevisionRequest.reason,
            "Enter the reason for this change."
        )
        val revisedDeliveryLineId = deliveryLineRevisionRequest.revisedDeliveryLineId
        val (purchaseId, purchaseLineId) = purchaseDeliveryDataFetcher.getOwners(revisedDeliveryLineId)
        val locationProductId = purchaseDataFetcher.getLocationProductIdOfLine(purchaseLineId)
        lockPurchaseThenProduct(purchaseId, locationProductId)

        val assessment = deliveryLineRevisionAssessor.assess(deliveryLineRevisionRequest)
        BlockedChangeException.throwIfAny(assessment.blockingProblems)

        val replacementLine = purchaseDeliveryLineReplacer.replace(assessment.validatedDeliveryLineRevision)
        val stockRevisionOutcome = deliveryLineStockReviser.revise(replacementLine, locationProductId, reason)
        revisionCostPropagator.propagate(stockRevisionOutcome, purchaseId, replacementLine)
        return applyToPurchase(purchaseId, replacementLine)
    }

    // The id lookups read scalars, so no entity is cached before the locks.
    // Purchase first, the order a delivery takes, so the two cannot deadlock.
    private fun lockPurchaseThenProduct(purchaseId: UUID, locationProductId: UUID) {
        purchaseDataFetcher.lockPurchase(purchaseId)
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductId)
    }

    private fun applyToPurchase(purchaseId: UUID, replacementLine: ReplacementDeliveryLineDto): PurchaseResponseDto =
        purchaseDeliveryRevisionApplier.apply(
            purchaseId = purchaseId,
            purchaseLineId = replacementLine.purchaseLineId,
            storageQuantityDelivered = purchaseDeliveryDataFetcher.calculateStorageQuantityDelivered(
                replacementLine.purchaseLineId
            ),
        )
}
