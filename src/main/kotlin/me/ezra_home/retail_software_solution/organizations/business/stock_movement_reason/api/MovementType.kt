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

    fun isReversal(): Boolean = reversals.containsKey(this) || this == REVERSED_BY_REVISION

    fun increasesStock(): Boolean = stockDirection == StockDirection.INCREASE

    fun decreasesStock(): Boolean = stockDirection == StockDirection.DECREASE

    companion object {
        // REVERSED_BY_REVISION is not listed: it undoes a draw of any type, so it names no single reversed type.
        val reversals: Map<MovementType, MovementType> = mapOf(
            SALE_VOID to SALE,
            TRANSFER_CANCELLED to TRANSFER_OUT
        )

        // CUSTOMER_RETURN is listed only because no logic has implements a flow for it.
        // Once it carries the sale's references it can leave this set and become a `reversals` entry for SALE.
        // SUPPLIER_RETURN stays: it is tied to one delivery's supplier and cost, so a FIFO replay would change whose credit it is.
        val movementsThatCannotBeReversed: Set<MovementType> = setOf(CUSTOMER_RETURN, SUPPLIER_RETURN)
    }
}
