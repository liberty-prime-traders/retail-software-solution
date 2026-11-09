package me.ezra_home.retail_software_solution.locations.business.stock_adjustment_backfill.api

import me.ezra_home.retail_software_solution.configuration.session.ServiceAccountContext
import me.ezra_home.retail_software_solution.configuration.session.withLocationSession
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment_backfill.LocationStockAdjustmentBackfiller
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment_backfill.StockAdjustmentBackfillLogFormatter
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment_backfill.StockAdjustmentBackfillResult
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationDto
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.util.async.AsyncExecutor
import me.ezra_home.retail_software_solution.util.enums.ServiceAccount
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean

@Service
class StockAdjustmentBackfiller(
    private val locationService: LocationService,
    private val locationStockAdjustmentBackfiller: LocationStockAdjustmentBackfiller,
    private val asyncExecutor: AsyncExecutor,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // Per application instance; across instances the unique index on ACTIVE rows keeps an overlap harmless.
    private val backfillRunning = AtomicBoolean(false)

    fun startBackfillAllLocations() {
        if (!backfillRunning.compareAndSet(false, true)) throw RtsGenericException(
            "The stock adjustment backfill is already running."
        )
        try {
            asyncExecutor.execute {
                try {
                    ServiceAccountContext.runWithServiceAccount(ServiceAccount.RECORD_INITIALIZER) {
                        backfillAllLocations()
                    }
                } finally {
                    backfillRunning.set(false)
                }
            }
        } catch (exception: RuntimeException) {
            backfillRunning.set(false)
            throw exception
        }
    }

    // Every batch commits on its own, so a failing location keeps the batches already done and is skipped; one bad
    // location never blocks the rest and a re-run resumes from whatever is still missing.
    private fun backfillAllLocations() {
        val locations = locationService.getAllLocationDtos()
        log.info("Stock adjustment backfill started for {} location(s)", locations.size)

        val results = mutableListOf<StockAdjustmentBackfillResult>()
        val failedLocationNames = mutableListOf<String>()
        locations.forEach { location ->
            try {
                val result = backfillLocation(location)
                results.add(result)
                log.info("Stock adjustment backfill location finished\n{}", StockAdjustmentBackfillLogFormatter.formatLocation(result))
            } catch (exception: Exception) {
                failedLocationNames.add(location.name!!)
                log.error(
                    "Stock adjustment backfill FAILED for location '{}'; batches already committed are kept, re-run to resume",
                    location.name, exception
                )
            }
        }

        log.info("Stock adjustment backfill finished\n{}", StockAdjustmentBackfillLogFormatter.formatSummary(results, failedLocationNames))
    }

    private fun backfillLocation(location: LocationDto): StockAdjustmentBackfillResult =
        withLocationSession(location) {
            var insertedCostRecordCount = 0L
            do {
                val insertedInBatch = locationStockAdjustmentBackfiller.insertNextBatchOfCostRecords()
                insertedCostRecordCount += insertedInBatch
            } while (insertedInBatch > 0)

            StockAdjustmentBackfillResult(
                locationId = location.id,
                locationName = location.name!!,
                counts = locationStockAdjustmentBackfiller.countsAfterInserting(insertedCostRecordCount),
            )
        }
}
