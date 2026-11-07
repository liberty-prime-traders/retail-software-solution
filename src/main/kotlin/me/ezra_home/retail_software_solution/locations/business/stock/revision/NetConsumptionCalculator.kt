package me.ezra_home.retail_software_solution.locations.business.stock.revision

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class NetConsumptionCalculator(
    private val stockMovementRepository: StockMovementRepository,
    private val stockEntryRepository: StockEntryRepository
) {

    fun calculateNetConsumptions(stockEntryId: UUID): List<StockEntryConsumption> {
        val movementsOnEntry = stockMovementRepository.findByStockEntryId(stockEntryId)
        val drawsByOrigin = movementsOnEntry
            .filter { it.movementType.isConsumption() }
            .groupBy { StockMovementOrigin.of(it) }
        val reversalsByOrigin = movementsOnEntry
            .filter { it.movementType.isReversal() }
            .groupBy { StockMovementOrigin.of(it) }
        val (consistentDraws, contradictingDraws) = drawsByOrigin.entries.partition { drawsDescribeOneConsumer(it.value) }
        val firstConsumptionOrderByOrigin = firstConsumptionOrderByOrigin(drawsByOrigin)
        val netConsumptions = consistentDraws.map { (stockMovementOrigin, draws) ->
            netConsumptionOf(
                stockEntryId, draws, reversalsByOrigin[stockMovementOrigin].orEmpty(),
                firstConsumptionOrderByOrigin.getValue(stockMovementOrigin)
            )
        }
        rejectInconsistentHistory(stockEntryId, reversalsByOrigin.keys, netConsumptions, contradictingDraws.map { it.key })
        return netConsumptions.filter { it.netConsumedStorageQuantity.isPositive() }.sortedBy { it.orderOfConsumption }
    }

    /**
     * Most origins draw once from a stock layer. However, replays that happen during revision could have the same
     * origin (For example, same sale and same line) to draw from a layer it already consumed before.
     * This method checks that the provided draws, all originate from the same origin/consumer.
     */
    private fun drawsDescribeOneConsumer(draws: List<StockMovementEntity>): Boolean =
        draws.map { StockConsumer.of(it) }.distinct().size == 1

    // A replayed draw is stamped with the replay time, so the order of a consumer is its first draw on any layer of the product.
    private fun firstConsumptionOrderByOrigin(
        drawsByOrigin: Map<StockMovementOrigin, List<StockMovementEntity>>
    ): Map<StockMovementOrigin, OrderOfConsumption> {
        val firstDraw = drawsByOrigin.values.firstOrNull()?.first() ?: return emptyMap()
        val documentReferences = drawsByOrigin.keys.mapNotNull { it.documentReference }.toSet()
        val allConsumptionsForDocument = stockMovementRepository.findByLocationProductIdAndDocumentReferenceIn(firstDraw.locationProductId, documentReferences)
            .filter { it.movementType.isConsumption() }
            .groupBy { StockMovementOrigin.of(it) }
        return drawsByOrigin.mapValues { (stockMovementOrigin, draws) ->
            (allConsumptionsForDocument[stockMovementOrigin].orEmpty() + draws).minOf { OrderOfConsumption.of(it) }
        }
    }

    private fun netConsumptionOf(
        stockEntryId: UUID,
        draws: List<StockMovementEntity>,
        reversals: List<StockMovementEntity>,
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
            quantityAsEntered = earliestDraw.quantityAsEntered(),
            orderOfConsumption = orderOfConsumption,
            netConsumedStorageQuantity = totalDrawnStorageQuantity - totalReversedStorageQuantity,
            netConsumedCost = totalDrawnCost.subtract(totalReversedCost),
        )
    }

    private fun totalStorageQuantityOf(movements: List<StockMovementEntity>): ExactQuantity =
        movements.fold(ExactQuantity.ZERO) { total, movement -> total + ExactQuantity.ofStorage(movement.storageMovedQuantity) }

    private fun totalCostOf(movements: List<StockMovementEntity>): BigDecimal = movements.sumOf { it.movedCost }

    private fun rejectInconsistentHistory(
        stockEntryId: UUID,
        reversedOrigins: Set<StockMovementOrigin>,
        consumptions: List<StockEntryConsumption>,
        originsWithContradictingDraws: List<StockMovementOrigin>
    ) {
        val consumedOrigins = consumptions.map { it.stockMovementOrigin }.toSet() + originsWithContradictingDraws
        val reversalsWithoutDraw = reversedOrigins
            .filter { it !in consumedOrigins }
            .map { "reversal for ${it.displayText} has no matching draw" }
        val overReversedDraws = consumptions
            .filter { it.netConsumedStorageQuantity < ExactQuantity.ZERO }
            .map { "more reversed than drawn for ${it.stockMovementOrigin.displayText}" }
        val contradictingDraws = originsWithContradictingDraws.map {
            "draws for ${it.displayText} differ in movement type or adjustment details"
        }
        val problems = reversalsWithoutDraw + overReversedDraws + contradictingDraws
        if (problems.isEmpty()) return
        val reference = stockEntryRepository.findById(stockEntryId).orElseThrow().requiredReference()
        val errors = problems.map { "Stock entry $reference: $it." }
        throw RtsGenericException(errors.joinToString(" "), payload = errors)
    }
}
