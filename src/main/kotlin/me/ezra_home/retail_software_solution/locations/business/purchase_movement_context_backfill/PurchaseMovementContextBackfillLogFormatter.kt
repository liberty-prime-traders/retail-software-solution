package me.ezra_home.retail_software_solution.locations.business.purchase_movement_context_backfill

object PurchaseMovementContextBackfillLogFormatter {

    fun formatLocation(result: PurchaseMovementContextBackfillResult): String = listOf(
        "  Location ............ ${result.locationName}",
        "  Updated this run .... ${result.counts.updatedMovementCount}",
        "  Still missing ....... ${result.counts.movementsStillMissingContextCount}",
        "  Complete ............ ${if (result.isComplete) "YES" else "NO - movements still lack a context, re-run the backfill"}",
    ).joinToString("\n")

    fun formatSummary(results: List<PurchaseMovementContextBackfillResult>, failedLocationNames: List<String>): String {
        val nameWidth = (results.map { it.locationName } + failedLocationNames).maxOfOrNull { it.length } ?: 0
        val resultRows = results.map { result ->
            val status = if (result.isComplete) "OK        " else "INCOMPLETE"
            "  $status  ${result.locationName.padEnd(nameWidth)}  updated=${result.counts.updatedMovementCount}" +
                "  missing=${result.counts.movementsStillMissingContextCount}"
        }
        val failedRows = failedLocationNames.map { "  FAILED      ${it.padEnd(nameWidth)}  see the error logged above" }
        val incompleteCount = results.count { !it.isComplete }
        val header = "  ${results.size + failedLocationNames.size} location(s): ${results.size - incompleteCount} complete, " +
            "$incompleteCount incomplete, ${failedLocationNames.size} failed"
        return (listOf(header) + resultRows + failedRows).joinToString("\n")
    }
}
