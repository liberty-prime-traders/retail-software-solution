package me.ezra_home.retail_software_solution.locations.business.stock.api.revision

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockInboundLayers
import me.ezra_home.retail_software_solution.locations.business.stock.StockReplacementLayerSpecs
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.FifoLayer
import me.ezra_home.retail_software_solution.locations.business.stock.api.fifo.StockEntryFifoDto
import me.ezra_home.retail_software_solution.locations.business.stock.stockEntryFifoComparator
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class StockReplacementLayerPreviewer(
    private val stockEntryRepository: StockEntryRepository,
) {

    fun previewReplacement(
        stockReplacementLayerRequest: StockReplacementLayerRequest
    ): ReplacementLayerPreview {
        val revisedStockEntry = findRevisedStockEntry(stockReplacementLayerRequest.revisedStockEntryId)
        val replacementEntry = StockInboundLayers.newLayer(
            StockReplacementLayerSpecs.of(stockReplacementLayerRequest, revisedStockEntry)
        )
        val otherEntriesInDrawOrder = stockEntryRepository
            .findFifoEntriesForProducts(listOf(stockReplacementLayerRequest.locationProductId))
            .filter { it.id != revisedStockEntry.id }
            .sortedWith(stockEntryFifoComparator)
        val (entriesDrawnBefore, entriesDrawnAfter) = otherEntriesInDrawOrder.partition {
            stockEntryFifoComparator.compare(it, replacementEntry) <= 0
        }

        val unsavedReplacement = unsavedFifoDtoOf(replacementEntry, stockReplacementLayerRequest.detailReference)
        val fifoEntries = buildList {
            addAll(entriesDrawnBefore.map { it.toFifoDto() })
            add(unsavedReplacement)
            addAll(entriesDrawnAfter.map { it.toFifoDto() })
        }
        return ReplacementLayerPreview(fifoEntries, unsavedReplacement.fifoLayer.stockEntryId)
    }

    private fun findRevisedStockEntry(revisedStockEntryId: UUID): StockEntryEntity =
        stockEntryRepository.findById(revisedStockEntryId).orElseThrow {
            RtsGenericException("The stock of this delivery could not be found. Please contact support.")
        }

    private fun unsavedFifoDtoOf(unsavedEntry: StockEntryEntity, referenceNumber: String): StockEntryFifoDto {
        val unsavedFifoLayer = FifoLayer(
            stockEntryId = UUID.randomUUID(),
            storageQuantityRemaining = unsavedEntry.storageQuantityRemaining,
            costRemaining = unsavedEntry.costRemaining,
            storageBatchSize = unsavedEntry.storageBatchSize,
            batchCost = unsavedEntry.batchCost,
        )
        return StockEntryFifoDto(
            referenceNumber = referenceNumber,
            locationProductId = unsavedEntry.locationProductId,
            unitCost = unsavedEntry.unitCost,
            fifoLayer = unsavedFifoLayer,
        )
    }
}
