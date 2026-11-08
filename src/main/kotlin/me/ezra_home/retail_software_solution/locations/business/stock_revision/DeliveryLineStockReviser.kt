package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.delivery.api.ReplacementDeliveryLineDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductStockBreakdownProvider
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.DeliveryLineStockInspector
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.StockReplacementLayerRecorder
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.StockRevisionCommitter
import me.ezra_home.retail_software_solution.util.business.reference_number.ReferenceNumberGenerator
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

data class StockRevisionOutcome(
    val stockRevision: StockRevisionEntity,
    val consumerCostChanges: List<ConsumerCostChange>,
)

@Component
@TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
class DeliveryLineStockReviser(
    private val deliveryLineStockInspector: DeliveryLineStockInspector,
    private val stockRevisionCommitter: StockRevisionCommitter,
    private val stockReplacementLayerRecorder: StockReplacementLayerRecorder,
    private val netConsumptionCalculator: NetConsumptionCalculator,
    private val resetPlanner: ResetPlanner,
    private val replayPlanner: ReplayPlanner,
    private val referenceNumberGenerator: ReferenceNumberGenerator,
    private val stockRevisionRepository: StockRevisionRepository,
    private val productStockBreakdownProvider: ProductStockBreakdownProvider,
) {

    fun revise(
        replacementLine: ReplacementDeliveryLineDto,
        locationProductId: UUID,
        reason: String
    ): StockRevisionOutcome {
        val quantityBreakdown = productStockBreakdownProvider.forLocationProduct(locationProductId)
        val stockRevisionReferenceNumber = referenceNumberGenerator.generateSingle(TableName.STOCK_REVISION)
        val revisedStockEntryId = deliveryLineStockInspector.getStockEntryId(
            replacementLine.deliveryReferenceNumber, replacementLine.revisedLineReferenceNumber
        )
        val consumptionsOnRevisedLayer = netConsumptionCalculator.calculateNetConsumptions(revisedStockEntryId)
        RestatableConsumers.requireAllRestatable(consumptionsOnRevisedLayer)

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
        val consumerReplayPlans = replayPlanner.planReplayForExecution(
            consumptionsOnRevisedLayer, stockRevisionReferenceNumber, quantityBreakdown
        )
        stockRevisionCommitter.commitReplay(consumerReplayPlans, replacementStockEntryId, quantityBreakdown)

        val stockRevision = stockRevisionRepository.save(
            StockRevisionEntity(
                sourceType = StockRevisionSourceType.DELIVERY_LINE,
                sourceReferenceNumber = replacementLine.replacementLineReferenceNumber,
                revisedStockEntryId = revisedStockEntryId,
                replacementStockEntryId = replacementStockEntryId,
                reason = reason,
            ).also { it.referenceNumber = stockRevisionReferenceNumber }
        )
        return StockRevisionOutcome(stockRevision, ConsumerCostChange.ofPlans(consumerReplayPlans))
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
}
