package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockBalanceFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockEntryFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.revision.StockEntryResetPlan
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryStatus
import me.ezra_home.retail_software_solution.util.exceptions.BlockedChangeException
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import me.ezra_home.retail_software_solution.util.quantity.QuantityBreakdown
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class ResetPlanner(
    private val stockEntryFetcher: StockEntryFetcher,
    private val stockBalanceFetcher: StockBalanceFetcher
) {

    fun plan(
        stockEntryId: UUID,
        stockEntryConsumptions: List<StockEntryConsumption>,
        stockRevisionReferenceNumber: String,
        quantityBreakdown: QuantityBreakdown
    ): StockEntryResetPlan {
        val stockEntryDto = stockEntryFetcher.get(stockEntryId)
        if (stockEntryDto.status == StockEntryStatus.REPLACED) {
            throw BlockedChangeException.contactSupport(
                listOf(
                    BlockingProblem(
                        documentReference = stockEntryDto.referenceNumber,
                        description = "This stock record was already replaced by an earlier change.",
                        effect = "Change the latest delivery line instead."
                    )
                )
            )
        }
        val locationProductId = stockEntryDto.locationProductId
        val productBalance = stockBalanceFetcher.getLatestBalances(
            listOf(locationProductId)
        ).getValue(locationProductId)

        val restoration = StockEntryRestorer.restore(
            stockEntryConsumptions, stockEntryDto, stockRevisionReferenceNumber,
            ExactQuantity.ofStorage(productBalance)
        )
        val drainDraft = StockEntryDrainer.drain(
            stockEntryDto = stockEntryDto,
            restoredQuantity = restoration.restoredQuantity,
            restoredCost = restoration.restoredCost,
            stockRevisionReferenceNumber = stockRevisionReferenceNumber,
            balanceBeforeDrain = restoration.endingBalance,
            quantityBreakdown = quantityBreakdown
        )
        return StockEntryResetPlan(
            stockEntryId,
            locationProductId,
            restoration.reversalDrafts + listOfNotNull(drainDraft)
        )
    }
}
