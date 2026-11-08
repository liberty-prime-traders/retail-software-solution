package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.ConsumerReplayPlan
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.StockEntryFifoDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockEntryFetcher
import me.ezra_home.retail_software_solution.util.exceptions.BlockedChangeException
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.springframework.stereotype.Component

@Component
@TransactionalOnLocationSchema(readOnly = true)
class ReplayPlanner(
    private val stockEntryFetcher: StockEntryFetcher,
    private val locationProductDataFetcher: LocationProductDataFetcher
) {

    fun planReplayForExecution(
        consumptionsOnReplacedLayer: List<StockEntryConsumption>,
        stockRevisionReferenceNumber: String,
        quantityBreakdown: QuantityBreakdown
    ): List<ConsumerReplayPlan> {
        if (consumptionsOnReplacedLayer.isEmpty()) return emptyList()
        val locationProductId = consumptionsOnReplacedLayer.first().locationProductId
        val replayResult = planReplayForPreview(
            consumptionsOnReplacedLayer,
            stockEntryFetcher.findWithStockLeftInDrawOrder(locationProductId),
            stockRevisionReferenceNumber,
            quantityBreakdown
        )
        BlockedChangeException.throwIfAny(replayResult.problems)
        return replayResult.consumerReplayPlans
    }

    fun planReplayForPreview(
        consumptionsOnReplacedLayer: List<StockEntryConsumption>,
        fifoEntriesAvailableForConsumption: List<StockEntryFifoDto>,
        stockRevisionReferenceNumber: String,
        quantityBreakdown: QuantityBreakdown
    ): ReplayResult {
        if (consumptionsOnReplacedLayer.isEmpty()) return ReplayResult(emptyList(), emptyList())
        val locationProductId = consumptionsOnReplacedLayer.first().locationProductId
        return ConsumerReplayer.replay(
            ReplayRequest(
                stockRevisionReferenceNumber = stockRevisionReferenceNumber,
                consumptionsOnReplacedLayer = consumptionsOnReplacedLayer,
                productBeingReplayed = ProductBeingReplayed(
                    productLabel = locationProductDataFetcher.findSummaryByIds(
                        listOf(locationProductId)
                    ).getValue(locationProductId).label,
                    quantityBreakdown = quantityBreakdown,
                    fifoEntriesAvailableForConsumption = fifoEntriesAvailableForConsumption,
                )
            )
        )
    }
}
