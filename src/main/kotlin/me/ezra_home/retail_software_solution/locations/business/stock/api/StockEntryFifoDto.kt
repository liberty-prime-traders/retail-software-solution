package me.ezra_home.retail_software_solution.locations.business.stock.api

import java.math.BigDecimal
import java.util.UUID

data class StockEntryFifoDto(
    val locationProductId: UUID,
    val unitCost: BigDecimal,
    override val storageQuantityRemaining: BigDecimal,
    override val costRemaining: BigDecimal,
    override val storageBatchSize: BigDecimal,
    override val batchCost: BigDecimal
) : FifoLayer
