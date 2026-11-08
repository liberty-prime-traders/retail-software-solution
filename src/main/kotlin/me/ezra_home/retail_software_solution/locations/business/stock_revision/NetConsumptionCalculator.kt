package me.ezra_home.retail_software_solution.locations.business.stock_revision

import java.math.BigDecimal
import java.util.UUID
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockMovementDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockMovementFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockEntryFetcher
import me.ezra_home.retail_software_solution.util.exceptions.BlockedChangeException
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Component

@Component
@TransactionalOnLocationSchema(readOnly = true)
class NetConsumptionCalculator(
    private val stockMovementFetcher: StockMovementFetcher,
    private val stockEntryFetcher: StockEntryFetcher
) {

    fun calculateNetConsumptions(stockEntryId: UUID): List<StockEntryConsumption> {
        val movementsOnEntry = stockMovementFetcher.findOnEntry(stockEntryId)
        val drawsByOrigin = movementsOnEntry
            .filter { it.movementType.isConsumption() }
            .groupBy { StockMovementOrigin.of(it) }
        val reversalsByOrigin = movementsOnEntry
            .filter { it.movementType.isReversal() }
            .groupBy { StockMovementOrigin.of(it) }
        val (
            consistentDraws,
            contradictingDraws
        ) = drawsByOrigin.entries.partition { drawsDescribeOneConsumer(it.value) }
        val firstConsumptionOrderByOrigin = firstConsumptionOrderByOrigin(drawsByOrigin)
        val netConsumptions = consistentDraws.map { (stockMovementOrigin, draws) ->
            netConsumptionOf(
                stockEntryId, draws, reversalsByOrigin[stockMovementOrigin].orEmpty(),
                firstConsumptionOrderByOrigin.getValue(stockMovementOrigin)
            )
        }
        rejectInconsistentHistory(
            stockEntryId,
            reversalsByOrigin.keys,
            netConsumptions,
            contradictingDraws.map { it.key }
        )
        return netConsumptions.filter { it.netConsumedStorageQuantity.isPositive() }.sortedBy { it.orderOfConsumption }
    }

    /**
     * Most origins draw once from a stock layer. However, replays that happen during revision could have the same
     * origin (For example, same sale and same line) to draw from a layer it already consumed before.
     * This method checks that the provided draws, all originate from the same origin/consumer.
     */
    private fun drawsDescribeOneConsumer(draws: List<StockMovementDto>): Boolean =
        draws.map { StockConsumer.of(it) }.distinct().size == 1

    // A replayed draw is stamped with the replay time, so the order of a consumer is its first draw on any layer of the
    // product.
    private fun firstConsumptionOrderByOrigin(
        drawsByOrigin: Map<StockMovementOrigin, List<StockMovementDto>>
    ): Map<StockMovementOrigin, OrderOfConsumption> {
        val firstDraw = drawsByOrigin.values.firstOrNull()?.first() ?: return emptyMap()
        val documentReferences = drawsByOrigin.keys.mapNotNull { it.documentReference }.toSet()
        val allConsumptionsForDocument = stockMovementFetcher
            .findOfDocuments(firstDraw.locationProductId, documentReferences)
            .filter { it.movementType.isConsumption() }
            .groupBy { StockMovementOrigin.of(it) }
        return drawsByOrigin.mapValues { (stockMovementOrigin, draws) ->
            (allConsumptionsForDocument[stockMovementOrigin].orEmpty() + draws).minOf { OrderOfConsumption.of(it) }
        }
    }

    private fun netConsumptionOf(
        stockEntryId: UUID,
        draws: List<StockMovementDto>,
        reversals: List<StockMovementDto>,
        orderOfConsumption: OrderOfConsumption
    ): StockEntryConsumption {
        val totalDrawnStorageQuantity = totalStorageQuantityOf(draws)
        val totalDrawnCost = totalCostOf(draws)
        val totalReversedStorageQuantity = totalStorageQuantityOf(reversals)
        val totalReversedCost = totalCostOf(reversals)
        val earliestDraw = draws.minBy { OrderOfConsumption.of(it) }
        return StockEntryConsumption(
            stockEntryId = stockEntryId,
            locationProductId = earliestDraw.locationProductId,
            consumer = StockConsumer.of(earliestDraw),
            quantityAsEntered = earliestDraw.quantityAsEntered,
            orderOfConsumption = orderOfConsumption,
            netConsumedStorageQuantity = totalDrawnStorageQuantity - totalReversedStorageQuantity,
            netConsumedCost = totalDrawnCost.subtract(totalReversedCost),
        )
    }

    private fun totalStorageQuantityOf(movements: List<StockMovementDto>): ExactQuantity =
        movements.fold(ExactQuantity.ZERO) { total, movement -> total + ExactQuantity.ofStorage(
            movement.storageMovedQuantity
        ) }

    private fun totalCostOf(movements: List<StockMovementDto>): BigDecimal = movements.sumOf { it.movedCost }

    private fun rejectInconsistentHistory(
        stockEntryId: UUID,
        reversedOrigins: Set<StockMovementOrigin>,
        consumptions: List<StockEntryConsumption>,
        originsWithContradictingDraws: List<StockMovementOrigin>
    ) {
        val consumedOrigins = consumptions.map { it.stockMovementOrigin }.toSet() + originsWithContradictingDraws
        val originsReversedWithoutDraw = reversedOrigins.filter { it !in consumedOrigins }
        val originsOverReversed = consumptions
            .filter { it.netConsumedStorageQuantity < ExactQuantity.ZERO }
            .map { it.stockMovementOrigin }
        if (originsReversedWithoutDraw.isEmpty() && originsOverReversed.isEmpty() &&
            originsWithContradictingDraws.isEmpty()
        ) return
        val stockRecordReference = stockEntryFetcher.get(stockEntryId).referenceNumber
        val effect = "Please contact support and quote stock record $stockRecordReference."
        fun problemOf(stockMovementOrigin: StockMovementOrigin, description: String) = BlockingProblem(
            documentReference = stockMovementOrigin.requiredDocumentReference(),
            description = "${stockMovementOrigin.displayText} $description",
            effect = effect
        )
        throw BlockedChangeException.contactSupport(
            originsReversedWithoutDraw.map {
                problemOf(it, "shows stock put back that was never taken from this delivery.")
            } + originsOverReversed.map {
                problemOf(it, "shows more stock put back than was taken from this delivery.")
            } + originsWithContradictingDraws.map {
                problemOf(it, "took stock from this delivery in two different ways.")
            }
        )
    }
}
