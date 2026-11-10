package me.ezra_home.retail_software_solution.locations.business.stock_revision.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryDataFetcher
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryLineReplacer
import me.ezra_home.retail_software_solution.locations.business.delivery.api.ReplacementDeliveryLineDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductStockBreakdownProvider
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.delivery.PurchaseDeliveryRevisionApplier
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseResponseDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.DeliveryLineStockInspector
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.StockReplacementLayerRecorder
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.StockRevisionCommitter
import me.ezra_home.retail_software_solution.locations.business.stock_revision.DeliveryReplacementLayerRequests
import me.ezra_home.retail_software_solution.locations.business.stock_revision.NetConsumptionCalculator
import me.ezra_home.retail_software_solution.locations.business.stock_revision.ReplayPlanner
import me.ezra_home.retail_software_solution.locations.business.stock_revision.DeliveryLineRevisionAssessor
import me.ezra_home.retail_software_solution.locations.business.stock_revision.ResetPlanner
import me.ezra_home.retail_software_solution.locations.business.stock_revision.StockRevisionEntity
import me.ezra_home.retail_software_solution.locations.business.stock_revision.StockRevisionRepository
import me.ezra_home.retail_software_solution.locations.business.stock_revision.StockRevisionSourceType
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.BlockedChangeException
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.business.reference_number.ReferenceNumberGenerator
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class DeliveryLineRevisioner(
    private val purchaseDeliveryDataFetcher: PurchaseDeliveryDataFetcher,
    private val purchaseDataFetcher: PurchaseDataFetcher,
    private val purchaseDeliveryLineReplacer: PurchaseDeliveryLineReplacer,
    private val deliveryLineStockInspector: DeliveryLineStockInspector,
    private val stockRevisionCommitter: StockRevisionCommitter,
    private val stockReplacementLayerRecorder: StockReplacementLayerRecorder,
    private val netConsumptionCalculator: NetConsumptionCalculator,
    private val resetPlanner: ResetPlanner,
    private val replayPlanner: ReplayPlanner,
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val referenceNumberGenerator: ReferenceNumberGenerator,
    private val stockRevisionRepository: StockRevisionRepository,
    private val productStockBreakdownProvider: ProductStockBreakdownProvider,
    private val purchaseDeliveryRevisionApplier: PurchaseDeliveryRevisionApplier,
    private val deliveryLineRevisionAssessor: DeliveryLineRevisionAssessor,
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
        reviseStock(replacementLine, locationProductId, reason)
        return applyToPurchase(purchaseId, replacementLine)
    }

    // The id lookups read scalars, so no entity is cached before the locks.
    // Purchase first, the order a delivery takes, so the two cannot deadlock.
    private fun lockPurchaseThenProduct(purchaseId: UUID, locationProductId: UUID) {
        purchaseDataFetcher.lockPurchase(purchaseId)
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductId)
    }

    private fun reviseStock(replacementLine: ReplacementDeliveryLineDto, locationProductId: UUID, reason: String) {
        val quantityBreakdown = productStockBreakdownProvider.forLocationProduct(locationProductId)
        val stockRevisionReferenceNumber = referenceNumberGenerator.generateSingle(TableName.STOCK_REVISION)
        val revisedStockEntryId = deliveryLineStockInspector.getStockEntryId(
            replacementLine.deliveryReferenceNumber, replacementLine.revisedLineReferenceNumber
        )
        val consumptionsOnRevisedLayer = netConsumptionCalculator.calculateNetConsumptions(revisedStockEntryId)

        stockRevisionCommitter.commitReset(
            resetPlanner.plan(
                revisedStockEntryId,
                consumptionsOnRevisedLayer,
                stockRevisionReferenceNumber,
                quantityBreakdown
            ),
            quantityBreakdown
        )
        val replacementStockEntryId = recordReplacementLayer(
            replacementLine, locationProductId, stockRevisionReferenceNumber, revisedStockEntryId
        )
        stockRevisionCommitter.commitReplay(
            replayPlanner.planReplayForExecution(
                consumptionsOnRevisedLayer, stockRevisionReferenceNumber, quantityBreakdown
            ),
            replacementStockEntryId,
            quantityBreakdown
        )

        stockRevisionRepository.save(
            StockRevisionEntity(
                sourceType = StockRevisionSourceType.DELIVERY_LINE,
                sourceReferenceNumber = replacementLine.replacementLineReferenceNumber,
                revisedStockEntryId = revisedStockEntryId,
                replacementStockEntryId = replacementStockEntryId,
                reason = reason,
            ).also { it.referenceNumber = stockRevisionReferenceNumber }
        )
    }

    private fun recordReplacementLayer(
        replacementLine: ReplacementDeliveryLineDto,
        locationProductId: UUID,
        stockRevisionReferenceNumber: String,
        revisedStockEntryId: UUID
    ): UUID = stockReplacementLayerRecorder.record(
        DeliveryReplacementLayerRequests.of(
            locationProductId = locationProductId,
            deliveryReferenceNumber = replacementLine.deliveryReferenceNumber,
            replacementLineReferenceNumber = replacementLine.replacementLineReferenceNumber,
            stockRevisionReferenceNumber = stockRevisionReferenceNumber,
            userProvidedQuantityDelivered = replacementLine.userProvidedQuantityDelivered,
            unitId = replacementLine.unitId,
            storageUnitsPerUserProvidedUnit = replacementLine.storageUnitsPerUserProvidedUnit,
            unitCost = replacementLine.unitCost,
            revisedStockEntryId = revisedStockEntryId,
        )
    )

    private fun applyToPurchase(purchaseId: UUID, replacementLine: ReplacementDeliveryLineDto): PurchaseResponseDto =
        purchaseDeliveryRevisionApplier.apply(
            purchaseId = purchaseId,
            purchaseLineId = replacementLine.purchaseLineId,
            storageQuantityDelivered = purchaseDeliveryDataFetcher.calculateStorageQuantityDelivered(
                replacementLine.purchaseLineId
            ),
        )
}
