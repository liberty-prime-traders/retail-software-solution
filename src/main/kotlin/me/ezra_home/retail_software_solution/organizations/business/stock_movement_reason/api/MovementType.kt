package me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api

import me.ezra_home.retail_software_solution.util.enums.HasCode

enum class MovementType(
    override val code: String,
    val stockDirection: StockDirection,
    val reasonRequired: Boolean = false
) : HasCode {

    SALE("SAL", StockDirection.DECREASE),
    SALE_VOID("SLV", StockDirection.INCREASE),
    CUSTOMER_RETURN("CR", StockDirection.INCREASE),
    SUPPLIER_RETURN("SR", StockDirection.DECREASE),
    STOCK_ADJUSTMENT_INCREASE("ADI", StockDirection.INCREASE, reasonRequired = true),
    STOCK_ADJUSTMENT_DECREASE("ADC", StockDirection.DECREASE, reasonRequired = true),
    TRANSFER_IN("TI", StockDirection.INCREASE),
    TRANSFER_OUT("TO", StockDirection.DECREASE),
    TRANSFER_CANCELLED("TOC", StockDirection.INCREASE),
    PURCHASE_RECEIVED("PR", StockDirection.INCREASE),
    OPENING_STOCK("OPS", StockDirection.INCREASE),
    REVERSED_BY_REVISION("RBR", StockDirection.INCREASE),
    STOCK_REVISION_ZEROING("SRZ", StockDirection.DECREASE);

    fun isReversal(): Boolean = reversals.contains(this)

    fun reversalTypes(): Set<MovementType> =
        if (isConsumption()) setOfNotNull(ownReversals[this], REVERSED_BY_REVISION) else emptySet()

    fun withReversalTypes(): Set<MovementType> = setOf(this) + reversalTypes()

    fun isConsumption(): Boolean = decreasesStock() && this != STOCK_REVISION_ZEROING

    fun decreasesStock(): Boolean = stockDirection == StockDirection.DECREASE

    companion object {
        private val ownReversals = mapOf(SALE to SALE_VOID, TRANSFER_OUT to TRANSFER_CANCELLED)
        private val reversals = ownReversals.values.toSet() + REVERSED_BY_REVISION

        // CUSTOMER_RETURN has no flow yet; SUPPLIER_RETURN stays because a replay would change whose credit it is.
        val movementsThatCannotBeReversed: Set<MovementType> = setOf(CUSTOMER_RETURN, SUPPLIER_RETURN)
    }
}
