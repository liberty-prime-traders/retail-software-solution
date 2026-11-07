package me.ezra_home.retail_software_solution.locations.business.stock_revision.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryDataFetcher
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryLineReplaceRequest
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryLineReplacer
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.DeliveryLineStockInspector
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBreakdownProvider
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockReplacementLayerRecorder
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockReplacementLayerRequest
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockRevisionCommitter
import me.ezra_home.retail_software_solution.locations.business.stock_revision.NetConsumptionCalculator
import me.ezra_home.retail_software_solution.locations.business.stock_revision.ReplayPlanner
import me.ezra_home.retail_software_solution.locations.business.stock_revision.ResetPlanner
import me.ezra_home.retail_software_solution.locations.business.stock_revision.StockRevisionEntity
import me.ezra_home.retail_software_solution.locations.business.stock_revision.StockRevisionRepository
import me.ezra_home.retail_software_solution.locations.business.stock_revision.StockRevisionSourceType
import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.business.reference_number.ReferenceNumberGenerator
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.stereotype.Service

// Reset, replacement layer and replay commit together, so the whole revision runs in this one transaction.
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
    private val stockBreakdownProvider: StockBreakdownProvider,
) {

    fun revise(deliveryLineRevisionRequest: DeliveryLineRevisionRequest): DeliveryLineRevisionResponseDto {
        StringUtils.requireHasValue(deliveryLineRevisionRequest.reason, "A reason is required to revise a delivery line.")
        val locationProductId = purchaseDataFetcher.getLocationProductIdOfLine(
            purchaseDeliveryDataFetcher.getPurchaseLineId(deliveryLineRevisionRequest.revisedDeliveryLineId)
        )
        // The id lookups read scalars, so no entity is cached before the locks. Purchase first, the order a delivery takes, so the two cannot deadlock.
        purchaseDataFetcher.lockPurchase(purchaseDeliveryDataFetcher.getPurchaseId(deliveryLineRevisionRequest.revisedDeliveryLineId))
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductId)
        val quantityBreakdown = stockBreakdownProvider.forLocationProducts(listOf(locationProductId)).getValue(locationProductId)

        val stockRevisionReferenceNumber = referenceNumberGenerator.generateSingle(TableName.STOCK_REVISION)
        val replacementLine = purchaseDeliveryLineReplacer.replace(
            PurchaseDeliveryLineReplaceRequest(
                revisedDeliveryLineId = deliveryLineRevisionRequest.revisedDeliveryLineId,
                userProvidedQuantityDelivered = deliveryLineRevisionRequest.userProvidedQuantityDelivered,
                unitCost = deliveryLineRevisionRequest.unitCost,
            )
        )
        val revisedStockEntryId = deliveryLineStockInspector.getStockEntryId(
            replacementLine.deliveryReferenceNumber, replacementLine.revisedLineReferenceNumber
        )

        val consumptionsOnRevisedLayer = netConsumptionCalculator.calculateNetConsumptions(revisedStockEntryId)
        stockRevisionCommitter.commitReset(
            resetPlanner.plan(revisedStockEntryId, consumptionsOnRevisedLayer, stockRevisionReferenceNumber, quantityBreakdown),
            quantityBreakdown
        )
        val replacementStockEntryId = stockReplacementLayerRecorder.record(
            StockReplacementLayerRequest(
                locationProductId = locationProductId,
                sourceType = StockItemSource.PURCHASE,
                movementType = MovementType.PURCHASE_RECEIVED,
                documentReference = replacementLine.deliveryReferenceNumber,
                detailReference = replacementLine.replacementLineReferenceNumber,
                stockRevisionReferenceNumber = stockRevisionReferenceNumber,
                userProvidedQuantity = replacementLine.userProvidedQuantityDelivered,
                unitId = replacementLine.unitId,
                storageUnitsPerUserProvidedUnit = replacementLine.storageUnitsPerUserProvidedUnit,
                batchCost = Decimals.MoneyScale.multiply(replacementLine.unitCost, replacementLine.userProvidedQuantityDelivered),
                revisedStockEntryId = revisedStockEntryId,
            )
        )
        val consumerReplayPlans = replayPlanner.plan(consumptionsOnRevisedLayer, stockRevisionReferenceNumber, quantityBreakdown)
        val consumerReplays = stockRevisionCommitter.commitReplay(
            consumerReplayPlans, replacementStockEntryId, quantityBreakdown
        )

        stockRevisionRepository.save(
            StockRevisionEntity(
                sourceType = StockRevisionSourceType.DELIVERY_LINE,
                sourceReferenceNumber = replacementLine.replacementLineReferenceNumber,
                revisedStockEntryId = revisedStockEntryId,
                replacementStockEntryId = replacementStockEntryId,
                reason = deliveryLineRevisionRequest.reason.trim(),
            ).also { it.referenceNumber = stockRevisionReferenceNumber }
        )
        return DeliveryLineRevisionResponseDto(stockRevisionReferenceNumber, replacementLine, consumerReplays)
    }
}
