package me.ezra_home.retail_software_solution.locations.business.sale_cogs_backfill

object SaleLineCogsBackfillLogFormatter {

    fun formatLocation(result: SaleLineCogsBackfillResult): String = listOf(
        "  Location ............ ${result.locationName}",
        "  Inserted this run ... ${result.counts.insertedCostRecordCount}",
        "  Lines with cost ..... ${result.counts.saleLinesWithCogsTotalCount}",
        "  Cost records ........ ${result.counts.costRecordCount}",
        "  Reconciled .......... ${if (result.isReconciled) "YES" else "NO - counts differ, re-run the backfill"}",
    ).joinToString("\n")

    fun formatSummary(results: List<SaleLineCogsBackfillResult>, failedLocationNames: List<String>): String {
        val nameWidth = (results.map { it.locationName } + failedLocationNames).maxOfOrNull { it.length } ?: 0
        val resultRows = results.map { result ->
            val status = if (result.isReconciled) "OK      " else "MISMATCH"
            "  $status  ${result.locationName.padEnd(nameWidth)}  inserted=${result.counts.insertedCostRecordCount}" +
                "  lines=${result.counts.saleLinesWithCogsTotalCount}  records=${result.counts.costRecordCount}"
        }
        val failedRows = failedLocationNames.map { "  FAILED    ${it.padEnd(nameWidth)}  see the error logged above" }
        val unreconciledCount = results.count { !it.isReconciled }
        val header = "  ${results.size + failedLocationNames.size} location(s): ${results.size - unreconciledCount} reconciled, " +
            "$unreconciledCount mismatched, ${failedLocationNames.size} failed"
        return (listOf(header) + resultRows + failedRows).joinToString("\n")
    }
}
