package me.ezra_home.retail_software_solution.locations.business.last_purchase_cost_backfill

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.delivery.api.LastPurchaseCostBackfillStore
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import org.springframework.stereotype.Service
import java.util.UUID

// Short-lived: deleted with the rest of the last purchase cost backfill once every organization has run it.
@Service
@TransactionalOnLocationSchema
class LocationLastPurchaseCostBackfiller(
    private val lastPurchaseCostBackfillStore: LastPurchaseCostBackfillStore,
    private val locationProductService: LocationProductService,
) {

    // One call is one transaction, so the caller loops with the last id it got back until it returns null.
    fun backfillNextBatch(afterLocationProductId: UUID): LastPurchaseCostBatch? {
        val lastPurchaseCostUpdateDtos = lastPurchaseCostBackfillStore.findLastDeliveredCostsAfter(
            afterLocationProductId,
            BATCH_SIZE
        )
        if (lastPurchaseCostUpdateDtos.isEmpty()) return null
        locationProductService.updateLastPurchaseCosts(lastPurchaseCostUpdateDtos)
        return LastPurchaseCostBatch(
            updatedProductCount = lastPurchaseCostUpdateDtos.size,
            lastLocationProductId = lastPurchaseCostUpdateDtos.last().locationProductId,
        )
    }

    private companion object {
        const val BATCH_SIZE = 500
    }
}
