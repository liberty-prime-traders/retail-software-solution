package me.ezra_home.retail_software_solution.locations.business.stock.api

object StockMovementContexts {

    fun restoredForStockRevision(
        stockRevisionReferenceNumber: String
    ) = "Restored for stock revision $stockRevisionReferenceNumber"

    fun drainedByStockRevision(stockRevisionReferenceNumber: String, revisedSourceText: String) =
        "Drained by stock revision $stockRevisionReferenceNumber, which revised $revisedSourceText"

    fun replayedAfterStockRevision(
        stockRevisionReferenceNumber: String
    ) = "Replayed after stock revision $stockRevisionReferenceNumber"

    fun replacementFromStockRevision(
        stockRevisionReferenceNumber: String
    ) = "Replacement from stock revision $stockRevisionReferenceNumber"

    fun purchase(purchaseReferenceNumber: String) = "Purchase $purchaseReferenceNumber"
}
