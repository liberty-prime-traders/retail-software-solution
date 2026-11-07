package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.ConsumerReplayPlan
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockEntryFetcher
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.springframework.stereotype.Component

// Reads the layers as they stand, so the replacement layer must already be saved.
@Component
@TransactionalOnLocationSchema(readOnly = true)
class ReplayPlanner(
    private val stockEntryFetcher: StockEntryFetcher,
    private val locationProductDataFetcher: LocationProductDataFetcher
) {

    fun plan(
        consumptionsOnReplacedLayer: List<StockEntryConsumption>,
        stockRevisionReferenceNumber: String,
        quantityBreakdown: QuantityBreakdown
    ): List<ConsumerReplayPlan> {
        if (consumptionsOnReplacedLayer.isEmpty()) return emptyList()
        val locationProductId = consumptionsOnReplacedLayer.first().locationProductId
        val replayResult = ConsumerReplayer.replay(
            ReplayRequest(
                stockRevisionReferenceNumber = stockRevisionReferenceNumber,
                consumptionsOnReplacedLayer = consumptionsOnReplacedLayer,
                productBeingReplayed = ProductBeingReplayed(
                    productLabel = locationProductDataFetcher.findSummaryByIds(
                        listOf(locationProductId)
                    ).getValue(locationProductId).label,
                    quantityBreakdown = quantityBreakdown,
                    fifoEntriesAvailableForConsumption = stockEntryFetcher.findWithStockLeftInDrawOrder(
                        locationProductId
                    ),
                )
            )
        )
        if (replayResult.errors.isNotEmpty()) throw RtsGenericException(
            replayResult.errors.joinToString(" "),
            payload = replayResult.errors
        )
        return replayResult.consumerReplayPlans
    }
}
