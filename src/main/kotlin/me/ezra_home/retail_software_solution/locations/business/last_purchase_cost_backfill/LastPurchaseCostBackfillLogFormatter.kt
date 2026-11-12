package me.ezra_home.retail_software_solution.locations.business.last_purchase_cost_backfill

object LastPurchaseCostBackfillLogFormatter {

    fun formatLocation(result: LastPurchaseCostBackfillResult): String = listOf(
        "  Location ............ ${result.locationName}",
        "  Updated this run .... ${result.updatedProductCount}",
    ).joinToString("\n")

    fun formatSummary(results: List<LastPurchaseCostBackfillResult>, failedLocationNames: List<String>): String {
        val nameWidth = (results.map { it.locationName } + failedLocationNames).maxOfOrNull { it.length } ?: 0
        val resultRows = results.map { result ->
            "  OK      ${result.locationName.padEnd(nameWidth)}  updated=${result.updatedProductCount}"
        }
        val failedRows = failedLocationNames.map { "  FAILED  ${it.padEnd(nameWidth)}  see the error logged above" }
        val header = "  ${results.size + failedLocationNames.size} location(s): " +
            "${results.size} complete, ${failedLocationNames.size} failed"
        return buildList {
            add(header)
            addAll(resultRows)
            addAll(failedRows)
        }.joinToString("\n")
    }
}
