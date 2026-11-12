package me.ezra_home.retail_software_solution.locations.business.last_purchase_cost_backfill.api

import me.ezra_home.retail_software_solution.configuration.session.ServiceAccountContext
import me.ezra_home.retail_software_solution.configuration.session.withLocationSession
import me.ezra_home.retail_software_solution.locations.business.last_purchase_cost_backfill.LocationLastPurchaseCostBackfiller
import me.ezra_home.retail_software_solution.locations.business.last_purchase_cost_backfill.LastPurchaseCostBackfillLogFormatter
import me.ezra_home.retail_software_solution.locations.business.last_purchase_cost_backfill.LastPurchaseCostBackfillResult
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationDto
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.util.async.AsyncExecutor
import me.ezra_home.retail_software_solution.util.enums.ServiceAccount
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

// Short-lived: deleted with the rest of the last purchase cost backfill once every organization has run it.
@Service
class LastPurchaseCostBackfiller(
    private val locationService: LocationService,
    private val locationLastPurchaseCostBackfiller: LocationLastPurchaseCostBackfiller,
    private val asyncExecutor: AsyncExecutor,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun startBackfillAllLocations() {
        asyncExecutor.execute {
            ServiceAccountContext.runWithServiceAccount(ServiceAccount.RECORD_INITIALIZER) {
                backfillAllLocations()
            }
        }
    }

    private fun backfillAllLocations() {
        val locations = locationService.getAllLocationDtos()
        log.info("Last purchase cost backfill started for {} location(s)", locations.size)

        val results = mutableListOf<LastPurchaseCostBackfillResult>()
        val failedLocationNames = mutableListOf<String>()
        locations.forEach { location ->
            try {
                val result = backfillLocation(location)
                results.add(result)
                log.info(
                    "Last purchase cost backfill location finished\n{}",
                    LastPurchaseCostBackfillLogFormatter.formatLocation(result)
                )
            } catch (exception: Exception) {
                failedLocationNames.add(location.name!!)
                log.error(
                    "Last purchase cost backfill FAILED for location '{}'; " +
                        "batches already committed are kept, re-run to resume",
                    location.name, exception
                )
            }
        }

        log.info(
            "Last purchase cost backfill finished\n{}",
            LastPurchaseCostBackfillLogFormatter.formatSummary(results, failedLocationNames)
        )
    }

    private fun backfillLocation(location: LocationDto): LastPurchaseCostBackfillResult =
        withLocationSession(location) {
            var updatedProductCount = 0L
            var lastLocationProductId = UUID(0L, 0L)
            while (true) {
                val batch = locationLastPurchaseCostBackfiller.backfillNextBatch(lastLocationProductId) ?: break
                updatedProductCount += batch.updatedProductCount
                lastLocationProductId = batch.lastLocationProductId
            }
            LastPurchaseCostBackfillResult(
                locationId = location.id,
                locationName = location.name!!,
                updatedProductCount = updatedProductCount,
            )
        }
}
