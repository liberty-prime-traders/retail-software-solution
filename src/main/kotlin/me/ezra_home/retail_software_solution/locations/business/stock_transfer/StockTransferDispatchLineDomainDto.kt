package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class StockTransferDispatchLineDomainDto(
    val id: UUID,
    val referenceNumber: String,
    val stockTransferDispatchId: UUID,
    val locationProductId: UUID,
    val wholeLineUserProvidedQuantity: BigDecimal,
    val storageQuantityDispatched: BigDecimal,
    val unitId: UUID,
    val unitCost: BigDecimal,
    val costDispatched: BigDecimal,
    val storageUnitsPerUserProvidedUnit: Long,
    val createdById: UUID,
    val createdOn: OffsetDateTime
)
