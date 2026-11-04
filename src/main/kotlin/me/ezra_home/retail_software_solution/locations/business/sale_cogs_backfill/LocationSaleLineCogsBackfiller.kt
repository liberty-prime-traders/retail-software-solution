package me.ezra_home.retail_software_solution.locations.business.sale_cogs_backfill

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineCogsBackfillStore
import me.ezra_home.retail_software_solution.util.business.reference_number.ReferenceNumberGenerator
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class LocationSaleLineCogsBackfiller(
    private val saleLineCogsBackfillStore: SaleLineCogsBackfillStore,
    private val referenceNumberGenerator: ReferenceNumberGenerator,
) {

    // One call is one transaction, so the caller loops until it returns 0 and no batch holds more than BATCH_SIZE
    // managed entities. Each batch removes its own rows from the candidate query, so the first page is always
    // the next batch. The line is the key, so a partly copied sale gets its remaining lines on a re-run.
    fun backfillNextBatch(): Int {
        val candidates = saleLineCogsBackfillStore.findLinesMissingCogsRecord(BATCH_SIZE)
        if (candidates.isEmpty()) return 0
        val referenceNumbers = referenceNumberGenerator.generateBulk(TableName.SALE_LINE_COGS, candidates.size)
        saleLineCogsBackfillStore.insertCogsRecords(candidates, referenceNumbers)
        return candidates.size
    }

    fun countsAfterInserting(insertedCostRecordCount: Long) = SaleLineCogsBackfillCounts(
        insertedCostRecordCount = insertedCostRecordCount,
        saleLinesWithCogsTotalCount = saleLineCogsBackfillStore.countSaleLinesWithCogsTotal(),
        costRecordCount = saleLineCogsBackfillStore.countCogsRecords(),
    )

    private companion object {
        const val BATCH_SIZE = 500
    }
}
