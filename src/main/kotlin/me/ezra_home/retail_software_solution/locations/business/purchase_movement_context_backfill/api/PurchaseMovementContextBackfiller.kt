package me.ezra_home.retail_software_solution.locations.business.purchase_movement_context_backfill.api

import me.ezra_home.retail_software_solution.configuration.session.ServiceAccountContext
import me.ezra_home.retail_software_solution.configuration.session.withLocationSession
import me.ezra_home.retail_software_solution.locations.business.purchase_movement_context_backfill.LocationPurchaseMovementContextBackfiller
import me.ezra_home.retail_software_solution.locations.business.purchase_movement_context_backfill.PurchaseMovementContextBackfillLogFormatter
import me.ezra_home.retail_software_solution.locations.business.purchase_movement_context_backfill.PurchaseMovementContextBackfillResult
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationDto
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.util.async.AsyncExecutor
import me.ezra_home.retail_software_solution.util.enums.ServiceAccount
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

// Short-lived: deleted with the rest of the purchase movement context backfill once every organization has run it.
@Service
class PurchaseMovementContextBackfiller(
    private val locationService: LocationService,
    private val locationPurchaseMovementContextBackfiller: LocationPurchaseMovementContextBackfiller,
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
        log.info("Purchase movement context backfill started for {} location(s)", locations.size)

        val results = mutableListOf<PurchaseMovementContextBackfillResult>()
        val failedLocationNames = mutableListOf<String>()
        locations.forEach { location ->
            try {
                val result = backfillLocation(location)
                results.add(result)
                log.info(
                    "Purchase movement context backfill location finished\n{}",
                    PurchaseMovementContextBackfillLogFormatter.formatLocation(result)
                )
            } catch (exception: Exception) {
                failedLocationNames.add(location.name!!)
                log.error(
                    "Purchase movement context backfill FAILED for location '{}'; " +
                        "batches already committed are kept, re-run to resume",
                    location.name, exception
                )
            }
        }

        log.info(
            "Purchase movement context backfill finished\n{}",
            PurchaseMovementContextBackfillLogFormatter.formatSummary(results, failedLocationNames)
        )
    }

    private fun backfillLocation(location: LocationDto): PurchaseMovementContextBackfillResult =
        withLocationSession(location) {
            var updatedMovementCount = 0L
            var lastStockMovementId = UUID(0L, 0L)
            while (true) {
                val batch = locationPurchaseMovementContextBackfiller.backfillNextBatch(lastStockMovementId) ?: break
                updatedMovementCount += batch.updatedMovementCount
                lastStockMovementId = batch.lastStockMovementId
            }
            PurchaseMovementContextBackfillResult(
                locationId = location.id,
                locationName = location.name!!,
                counts = locationPurchaseMovementContextBackfiller.countsAfterUpdating(updatedMovementCount),
            )
        }
}
