package me.ezra_home.retail_software_solution.locations.business.stock

import java.math.BigDecimal

data class FifoRestorationResult(
    val movements: List<StockMovementEntity>,
    val touchedEntries: List<StockEntryEntity>,
    val endingBalance: BigDecimal
)
