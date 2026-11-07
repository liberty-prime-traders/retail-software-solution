package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAdjustmentMovementDetails
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockMovementDraft
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import java.math.BigDecimal
import java.util.UUID

object StockMovementCreator {

    fun create(
        stockMovementDraft: StockMovementDraft,
        stockEntryId: UUID,
        locationProductId: UUID,
        quantityBreakdown: QuantityBreakdown,
    ): StockMovementEntity = create(
        stockEntryId = stockEntryId,
        locationProductId = locationProductId,
        movementType = stockMovementDraft.movementType,
        storageMoved = stockMovementDraft.storageMoved,
        balanceAfterMovement = stockMovementDraft.balanceAfterMovement,
        movedCost = stockMovementDraft.movedCost,
        wholeLineQuantityText = stockMovementDraft.wholeLineQuantityText,
        documentReference = stockMovementDraft.documentReference,
        detailReference = stockMovementDraft.detailReference,
        unitId = stockMovementDraft.unitId,
        storageUnitsPerUserProvidedUnit = stockMovementDraft.storageUnitsPerUserProvidedUnit,
        quantityBreakdown = quantityBreakdown,
        context = stockMovementDraft.context,
        stockRevisionReference = stockMovementDraft.stockRevisionReference,
    )

    fun create(
        stockEntryId: UUID,
        locationProductId: UUID,
        movementType: MovementType,
        storageMoved: ExactQuantity,
        balanceAfterMovement: ExactQuantity,
        movedCost: BigDecimal,
        wholeLineQuantityText: String,
        documentReference: String?,
        detailReference: String?,
        unitId: UUID,
        storageUnitsPerUserProvidedUnit: Long,
        quantityBreakdown: QuantityBreakdown,
        adjustmentDetails: StockAdjustmentMovementDetails? = null,
        context: String? = null,
        stockRevisionReference: String? = null,
    ): StockMovementEntity = StockMovementEntity(
        stockEntryId = stockEntryId,
        locationProductId = locationProductId,
        movementType = movementType,
        reasonId = adjustmentDetails?.reasonId,
        storageMovedQuantity = storageMoved.toStorageQuantity(),
        wholeLineQuantityText = wholeLineQuantityText,
        storageRemainingQuantity = balanceAfterMovement.toStorageQuantity(),
        movedCost = movedCost,
        storageMovedBreakdownText = quantityBreakdown.render(storageMoved),
        storageRemainingBreakdownText = quantityBreakdown.render(balanceAfterMovement),
        documentReference = documentReference,
        detailReference = detailReference,
        unitId = unitId,
        storageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnit,
        category = adjustmentDetails?.category,
        note = adjustmentDetails?.note,
        context = context,
        stockRevisionReference = stockRevisionReference,
    )
}
