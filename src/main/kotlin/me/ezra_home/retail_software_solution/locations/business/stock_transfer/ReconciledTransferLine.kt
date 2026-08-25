package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal
import java.util.UUID

data class ReconciledTransferLine(
    val dispatchLineRef: String,
    val productLabel: String,
    val quantity: BigDecimal,
    val unitId: UUID,
    val baseUnitId: UUID,
    val conversionFactor: BigDecimal,
    val unitCost: BigDecimal?,
    val quantityReceived: BigDecimal?,
    val quantityAvailable: BigDecimal? = null,
) {
    val totalCost: BigDecimal? = unitCost?.let { Decimals.multiplyScale4(quantity, it) }
}
