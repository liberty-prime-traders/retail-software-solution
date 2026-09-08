package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import java.math.BigDecimal
import java.util.UUID

data class StockTransferDispatchedLineDto(
    val dispatchLineReferenceNumber: String,
    val orgProductId: UUID,
    val quantityDispatched: BigDecimal,
    val unitId: UUID,
    val baseUnitId: UUID,
    val conversionNumerator: Long,
    val conversionDenominator: Long,
    val unitCost: BigDecimal
)
