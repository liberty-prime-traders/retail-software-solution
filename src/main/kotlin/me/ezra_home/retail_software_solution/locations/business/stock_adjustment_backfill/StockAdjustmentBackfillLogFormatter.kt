package me.ezra_home.retail_software_solution.locations.business.stock_adjustment_backfill

object StockAdjustmentBackfillLogFormatter {

    fun formatLocation(result: StockAdjustmentBackfillResult): String = listOf(
        "  Location ................ ${result.locationName}",
        "  Cost records inserted ... ${result.counts.insertedCostRecordCount}",
        "  Adjustments ............. ${result.counts.adjustmentCount}",
        "  Active cost records ..... ${result.counts.activeCostRecordCount}",
        "  Reconciled .............. ${if (result.isReconciled) "YES" else "NO - counts differ, re-run the backfill"}",
    ).joinToString("\n")

    fun formatSummary(results: List<StockAdjustmentBackfillResult>, failedLocationNames: List<String>): String {
        val nameWidth = (results.map { it.locationName } + failedLocationNames).maxOfOrNull { it.length } ?: 0
        val resultRows = results.map { result ->
            val status = if (result.isReconciled) "OK      " else "MISMATCH"
            "  $status  ${result.locationName.padEnd(nameWidth)}  adjustments=${result.counts.adjustmentCount}" +
                "  costRecords=${result.counts.activeCostRecordCount}"
        }
        val failedRows = failedLocationNames.map { "  FAILED    ${it.padEnd(nameWidth)}  see the error logged above" }
        val unreconciledCount = results.count { !it.isReconciled }
        val header = "  ${results.size + failedLocationNames.size} location(s): ${results.size - unreconciledCount} reconciled, " +
            "$unreconciledCount mismatched, ${failedLocationNames.size} failed"
        return (listOf(header) + resultRows + failedRows).joinToString("\n")
    }
}
