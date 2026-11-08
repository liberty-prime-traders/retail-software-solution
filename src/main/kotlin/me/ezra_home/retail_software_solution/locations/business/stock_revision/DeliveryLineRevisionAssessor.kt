package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryLineReplaceRequest
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryLineReplacer
import me.ezra_home.retail_software_solution.locations.business.delivery.api.ValidatedDeliveryLineRevisionDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.ProductStockBreakdownProvider
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.DeliveryLineStockInspector
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.StockReplacementLayerPreviewer
import me.ezra_home.retail_software_solution.locations.business.stock_revision.api.DeliveryLineRevisionRequest
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class DeliveryLineRevisionAssessor(
    private val purchaseDeliveryLineReplacer: PurchaseDeliveryLineReplacer,
    private val deliveryLineStockInspector: DeliveryLineStockInspector,
    private val productStockBreakdownProvider: ProductStockBreakdownProvider,
    private val netConsumptionCalculator: NetConsumptionCalculator,
    private val stockReplacementLayerPreviewer: StockReplacementLayerPreviewer,
    private val replayPlanner: ReplayPlanner,
    private val reservationShortfallChecker: ReservationShortfallChecker,
) {

    fun assess(deliveryLineRevisionRequest: DeliveryLineRevisionRequest): DeliveryLineRevisionAssessment {
        val validatedDeliveryLineRevision = purchaseDeliveryLineReplacer.validateRevisionReportingProblems(
            PurchaseDeliveryLineReplaceRequest(
                revisedDeliveryLineId = deliveryLineRevisionRequest.revisedDeliveryLineId,
                userProvidedQuantityDelivered = deliveryLineRevisionRequest.userProvidedQuantityDelivered,
                unitCost = deliveryLineRevisionRequest.unitCost,
            )
        )
        val quantityBreakdown = productStockBreakdownProvider.forLocationProduct(
            validatedDeliveryLineRevision.locationProductId
        )
        if (validatedDeliveryLineRevision.problems.isNotEmpty()) {
            return DeliveryLineRevisionAssessment(
                validatedDeliveryLineRevision, quantityBreakdown,
                ReplayResult(emptyList(), emptyList()), emptyList()
            )
        }
        val replayResult = replayOnRevisedLayer(validatedDeliveryLineRevision, quantityBreakdown)
        val reservationProblems = if (replayResult.problems.isEmpty()) {
            reservationShortfallChecker.problemsAfterPendingChange(
                validatedDeliveryLineRevision.locationProductId,
                validatedDeliveryLineRevision.storageQuantityChange,
                quantityBreakdown
            )
        } else {
            emptyList()
        }
        return DeliveryLineRevisionAssessment(
            validatedDeliveryLineRevision, quantityBreakdown, replayResult, reservationProblems
        )
    }

    private fun replayOnRevisedLayer(
        validatedDeliveryLineRevision: ValidatedDeliveryLineRevisionDto,
        quantityBreakdown: QuantityBreakdown
    ): ReplayResult {
        val revisedStockEntryId = deliveryLineStockInspector.getStockEntryId(
            validatedDeliveryLineRevision.deliveryReferenceNumber,
            validatedDeliveryLineRevision.revisedLineReferenceNumber
        )
        return replayPlanner.planReplayForPreview(
            netConsumptionCalculator.calculateNetConsumptions(revisedStockEntryId),
            stockReplacementLayerPreviewer.fifoEntriesAfterReplacement(
                replacementLayerRequestOf(validatedDeliveryLineRevision, revisedStockEntryId)
            ),
            ASSESSMENT_REFERENCE,
            quantityBreakdown
        )
    }

    private fun replacementLayerRequestOf(
        validatedDeliveryLineRevision: ValidatedDeliveryLineRevisionDto,
        revisedStockEntryId: UUID
    ) = DeliveryReplacementLayerRequests.of(
        locationProductId = validatedDeliveryLineRevision.locationProductId,
        deliveryReferenceNumber = validatedDeliveryLineRevision.deliveryReferenceNumber,
        replacementLineReferenceNumber = ASSESSMENT_REFERENCE,
        stockRevisionReferenceNumber = ASSESSMENT_REFERENCE,
        userProvidedQuantityDelivered = validatedDeliveryLineRevision.userProvidedQuantityDelivered,
        unitId = validatedDeliveryLineRevision.unitId,
        storageUnitsPerUserProvidedUnit = validatedDeliveryLineRevision.storageUnitsPerUserProvidedUnit,
        unitCost = validatedDeliveryLineRevision.unitCost,
        revisedStockEntryId = revisedStockEntryId,
    )

    private companion object {
        const val ASSESSMENT_REFERENCE = "PREVIEW"
    }
}
