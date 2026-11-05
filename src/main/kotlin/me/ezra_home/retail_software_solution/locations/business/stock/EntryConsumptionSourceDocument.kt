package me.ezra_home.retail_software_solution.locations.business.stock

import java.util.UUID

// Each document family has its own reference sequence, so entry, document and detail identify a draw whatever its type; a reversal copies all three from its draw.
data class EntryConsumptionSourceDocument(
    val stockEntryId: UUID,
    val documentReference: String?,
    val detailReference: String?,
) {
    val documentAndDetailReference: String = "$documentReference/$detailReference"

    companion object {
        fun of(movement: StockMovementEntity) = EntryConsumptionSourceDocument(
            movement.stockEntryId,
            movement.documentReference,
            movement.detailReference
        )
    }
}
