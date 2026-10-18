package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import me.ezra_home.retail_software_solution.configuration.serializer.StripZeroesAndRound
import java.math.BigDecimal
import java.util.UUID

// quantity and quantityReceived are whole-line figures repeated on every cost-group row of a dispatched line: never sum them
data class ReconciledTransferLine(
    val dispatchLineRef: String,
    val productLabel: String,
    val quantity: BigDecimal,
    val unitId: UUID,
    val baseUnitId: UUID,
    val conversionFactor: BigDecimal,
    @field:JsonSerialize(using = StripZeroesAndRound::class)
    val unitCost: BigDecimal?,
    val quantityReceived: BigDecimal?,
    val quantityAvailable: BigDecimal? = null,
    val totalCost: BigDecimal?,
    val storageQuantity: BigDecimal? = null,
    val storageBreakdownText: String? = null,
)
