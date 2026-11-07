package me.ezra_home.retail_software_solution.locations.business.sale_cogs_backfill.api

import me.ezra_home.retail_software_solution.configuration.session.ServiceAccountContext
import me.ezra_home.retail_software_solution.configuration.session.withLocationSession
import me.ezra_home.retail_software_solution.locations.business.sale_cogs_backfill.LocationSaleLineCogsBackfiller
import me.ezra_home.retail_software_solution.locations.business.sale_cogs_backfill.SaleLineCogsBackfillLogFormatter
import me.ezra_home.retail_software_solution.locations.business.sale_cogs_backfill.SaleLineCogsBackfillResult
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationDto
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.util.async.AsyncExecutor
import me.ezra_home.retail_software_solution.util.enums.ServiceAccount
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean

@Service
class SaleLineCogsBackfiller(
    private val locationService: LocationService,
    private val locationSaleLineCogsBackfiller: LocationSaleLineCogsBackfiller,
    private val asyncExecutor: AsyncExecutor,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // Per application instance; across instances the unique index on ACTIVE rows keeps an overlap harmless.
    private val backfillRunning = AtomicBoolean(false)

    fun startBackfillAllLocations() {
        if (!backfillRunning.compareAndSet(false, true)) throw RtsGenericException(
            "The sale line COGS backfill is already running."
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

    // Every batch commits on its own, so a failing location keeps the batches already copied and is skipped; one bad
    // location never blocks the rest and a re-run resumes from whatever is still missing.
    private fun backfillAllLocations() {
        val locations = locationService.getAllLocationDtos()
        log.info("Sale line COGS backfill started for {} location(s)", locations.size)

        val results = mutableListOf<SaleLineCogsBackfillResult>()
        val failedLocationNames = mutableListOf<String>()
        locations.forEach { location ->
            try {
                val result = backfillLocation(location)
                results.add(result)
                log.info("Sale line COGS backfill location finished\n{}", SaleLineCogsBackfillLogFormatter.formatLocation(result))
            } catch (exception: Exception) {
                failedLocationNames.add(location.name!!)
                log.error(
                    "Sale line COGS backfill FAILED for location '{}'; batches already committed are kept, re-run to resume",
                    location.name, exception
                )
            }
        }

        log.info("Sale line COGS backfill finished\n{}", SaleLineCogsBackfillLogFormatter.formatSummary(results, failedLocationNames))
    }

    private fun backfillLocation(location: LocationDto): SaleLineCogsBackfillResult =
        withLocationSession(location) {
            var insertedCostRecordCount = 0L
            do {
                val insertedInBatch = locationSaleLineCogsBackfiller.backfillNextBatch()
                insertedCostRecordCount += insertedInBatch
            } while (insertedInBatch > 0)
            SaleLineCogsBackfillResult(
                locationId = location.id,
                locationName = location.name!!,
                counts = locationSaleLineCogsBackfiller.countsAfterInserting(insertedCostRecordCount),
            )
        }
}
