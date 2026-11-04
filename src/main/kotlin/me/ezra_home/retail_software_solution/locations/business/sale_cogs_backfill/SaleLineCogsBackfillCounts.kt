package me.ezra_home.retail_software_solution.locations.business.sale_cogs_backfill

data class SaleLineCogsBackfillCounts(
    val insertedCostRecordCount: Long,
    val saleLinesWithCogsTotalCount: Long,
    val costRecordCount: Long,
)
