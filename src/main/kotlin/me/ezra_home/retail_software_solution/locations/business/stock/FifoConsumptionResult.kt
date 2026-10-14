package me.ezra_home.retail_software_solution.locations.business.stock

import java.math.BigDecimal

data class FifoConsumptionResult(
    val movements: List<StockMovementEntity>,
    val touchedEntries: List<StockEntryEntity>,
    val cost: BigDecimal,
    val endingBalance: BigDecimal
)
