package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import java.math.BigDecimal
import java.util.UUID

// wholeLineUserProvidedQuantity, unitId and storageUnitsPerUserProvidedUnit are the draft line's whole-line values, repeated on every cost group: never sum them
data class StockTransferDispatchedLineDto(
    val dispatchLineReferenceNumber: String,
    val orgProductId: UUID,
    val wholeLineUserProvidedQuantity: BigDecimal,
    val storageQuantityDispatched: BigDecimal,
    val costDispatched: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long
)
