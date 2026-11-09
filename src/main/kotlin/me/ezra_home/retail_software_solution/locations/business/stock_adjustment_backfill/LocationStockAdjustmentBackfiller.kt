package me.ezra_home.retail_software_solution.locations.business.stock_adjustment_backfill

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentBackfillStore
import me.ezra_home.retail_software_solution.util.business.reference_number.ReferenceNumberGenerator
import me.ezra_home.retail_software_solution.util.model.TableName
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class LocationStockAdjustmentBackfiller(
    private val stockAdjustmentBackfillStore: StockAdjustmentBackfillStore,
    private val referenceNumberGenerator: ReferenceNumberGenerator,
) {

    fun insertNextBatchOfCostRecords(): Int {
        val candidates = stockAdjustmentBackfillStore.findAdjustmentsMissingCostRecord(BATCH_SIZE)
        if (candidates.isEmpty()) return 0
        val referenceNumbers = referenceNumberGenerator.generateBulk(TableName.STOCK_ADJUSTMENT_COST, candidates.size)
        stockAdjustmentBackfillStore.insertCostRecords(candidates, referenceNumbers)
        return candidates.size
    }

    fun countsAfterInserting(insertedCostRecordCount: Long) = StockAdjustmentBackfillCounts(
        insertedCostRecordCount = insertedCostRecordCount,
        adjustmentCount = stockAdjustmentBackfillStore.countAdjustments(),
        activeCostRecordCount = stockAdjustmentBackfillStore.countActiveCostRecords(),
    )

    private companion object {
        const val BATCH_SIZE = 500
    }
}
