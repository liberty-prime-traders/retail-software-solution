package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.organizations.business.stock_item_source.api.StockItemSource
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import java.math.BigDecimal
import java.util.UUID

data class InboundLayerSpec(
    val locationProductId: UUID,
    val sourceType: StockItemSource,
    val movementType: MovementType,
    val documentReference: String,
    val detailReference: String?,
    val storageQuantity: ExactQuantity,
    val batchCost: BigDecimal,
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUnit: Long,
)

object StockInboundLayers {

    // The rate is for display and grouping, never multiplied back into a total
    fun newLayer(inboundLayerSpec: InboundLayerSpec): StockEntryEntity {
        val storageQuantity = inboundLayerSpec.storageQuantity.toStorageQuantity()
        val unitCost = if (inboundLayerSpec.storageQuantity.isPositive()) {
            Decimals.Scale10.divide(inboundLayerSpec.batchCost, storageQuantity)
        } else {
            BigDecimal.ZERO
        }
        return StockEntryEntity(
            locationProductId = inboundLayerSpec.locationProductId,
            sourceType = inboundLayerSpec.sourceType,
            documentReference = inboundLayerSpec.documentReference,
            detailReference = inboundLayerSpec.detailReference,
            storageBatchSize = storageQuantity,
            storageQuantityRemaining = storageQuantity,
            priority = 0,
            unitCost = unitCost,
            batchCost = inboundLayerSpec.batchCost,
            costRemaining = inboundLayerSpec.batchCost,
        )
    }

    fun inboundMovement(
        inboundLayerSpec: InboundLayerSpec,
        stockEntry: StockEntryEntity,
        balanceAfterMovement: ExactQuantity,
        quantityBreakdown: QuantityBreakdown
    ): StockMovementEntity = StockMovementEntity(
        stockEntryId = stockEntry.id!!,
        locationProductId = inboundLayerSpec.locationProductId,
        movementType = inboundLayerSpec.movementType,
        storageMovedQuantity = inboundLayerSpec.storageQuantity.toStorageQuantity(),
        userProvidedMovedQuantity = inboundLayerSpec.userProvidedQuantity,
        storageRemainingQuantity = balanceAfterMovement.toStorageQuantity(),
        movedCost = inboundLayerSpec.batchCost,
        storageMovedBreakdownText = quantityBreakdown.render(inboundLayerSpec.storageQuantity),
        storageRemainingBreakdownText = quantityBreakdown.render(balanceAfterMovement),
        documentReference = inboundLayerSpec.documentReference,
        detailReference = inboundLayerSpec.detailReference,
        unitId = inboundLayerSpec.unitId,
        storageUnitsPerUnit = inboundLayerSpec.storageUnitsPerUnit,
    )
}
