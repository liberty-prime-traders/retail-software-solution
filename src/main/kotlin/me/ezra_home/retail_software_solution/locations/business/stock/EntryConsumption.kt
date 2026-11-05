package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import java.math.BigDecimal
import java.util.UUID

data class EntryConsumption(
    val consumingMovement: StockMovementEntity,
    val netConsumedStorageQuantity: ExactQuantity,
    val netConsumedCost: BigDecimal
) {
    val sourceDocument: EntryConsumptionSourceDocument = EntryConsumptionSourceDocument.of(consumingMovement)
    val stockEntryId: UUID = consumingMovement.stockEntryId
    val documentReference: String? = consumingMovement.documentReference
    val detailReference: String? = consumingMovement.detailReference
    val documentAndDetailReference: String = sourceDocument.documentAndDetailReference
}
