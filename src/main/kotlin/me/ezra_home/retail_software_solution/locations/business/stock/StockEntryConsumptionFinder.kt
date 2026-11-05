package me.ezra_home.retail_software_solution.locations.business.stock

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class StockEntryConsumptionFinder(
    private val stockMovementRepository: StockMovementRepository,
    private val stockEntryRepository: StockEntryRepository
) {

       fun netConsumptions(stockEntryIds: Collection<UUID>): List<EntryConsumption> {
        if (stockEntryIds.isEmpty()) return emptyList()
        val movementsOnEntries = stockMovementRepository.findByStockEntryIdInOrderByCreatedOnAscIdAsc(stockEntryIds)
        val consumingMovements = movementsOnEntries.filter { it.movementType.decreasesStock() }
        val reversalsBySourceDocument = movementsOnEntries
            .filter { it.movementType.isReversal() }
            .groupBy { EntryConsumptionSourceDocument.of(it) }
        val consumptions = consumingMovements.map { consumingMovement ->
            val consumingSourceDocument = EntryConsumptionSourceDocument.of(consumingMovement)
            netConsumedAfterReversals(consumingMovement, reversalsBySourceDocument[consumingSourceDocument].orEmpty())
        }
        rejectInconsistentHistory(reversalsBySourceDocument.keys, consumptions)
        return consumptions.filter { it.netConsumedStorageQuantity.isPositive() }
    }

    private fun netConsumedAfterReversals(consumingMovement: StockMovementEntity, reversals: List<StockMovementEntity>) = EntryConsumption(
        consumingMovement = consumingMovement,
        netConsumedStorageQuantity = reversals.fold(ExactQuantity.ofStorage(consumingMovement.storageMovedQuantity)) { net, reversal ->
            net - ExactQuantity.ofStorage(reversal.storageMovedQuantity)
        },
        netConsumedCost = reversals.fold(consumingMovement.movedCost) { net, reversal -> net.subtract(reversal.movedCost) },
    )

    private fun rejectInconsistentHistory(reversedSourceDocuments: Set<EntryConsumptionSourceDocument>, consumptions: List<EntryConsumption>) {
        val consumedSourceDocuments = consumptions.map { it.sourceDocument }.toSet()
        val reversalsWithoutDraw = reversedSourceDocuments
            .filter { it !in consumedSourceDocuments }
            .map { HistoryProblem(it.stockEntryId, "reversal for ${it.documentAndDetailReference} has no matching draw") }
        val overReversedDraws = consumptions
            .filter { it.netConsumedStorageQuantity < ExactQuantity.ZERO }
            .map {
                HistoryProblem(
                    it.stockEntryId,
                    "more reversed than drawn for ${it.documentAndDetailReference}"
                )
            }
        val problems = reversalsWithoutDraw + overReversedDraws
        if (problems.isEmpty()) return
        val referenceByStockEntryId = stockEntryRepository.findAllById(problems.map { it.stockEntryId }.toSet())
            .associate { it.id!! to it.requiredReference() }
        val errors = problems.map { "Stock entry ${referenceByStockEntryId.getValue(it.stockEntryId)}: ${it.description}." }
        throw RtsGenericException(errors.joinToString(" "), payload = errors)
    }

    private data class HistoryProblem(val stockEntryId: UUID, val description: String)
}
