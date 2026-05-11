package me.ezra_home.retail_software_solution.locations.business.stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockEntryRepository
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementEntity
import me.ezra_home.retail_software_solution.locations.business.stock.StockMovementRepository
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

data class SaleLineStockRequest(
    val saleLineId: UUID,
    val locationProductId: UUID,
    val baseQuantity: BigDecimal,
    val unitId: UUID,
    val conversionFactor: BigDecimal
)

private data class BatchAllocation(val stockEntryId: UUID, val quantityTaken: BigDecimal)

@Service
@TransactionalOnLocationSchema
class SaleStockUpdater(
    private val stockEntryRepository: StockEntryRepository,
    private val stockMovementRepository: StockMovementRepository
) {

    fun consumeStock(saleLineStockRequests: List<SaleLineStockRequest>, saleRefNumber: String) {
        saleLineStockRequests.forEach { consumeLine(it, saleRefNumber) }
    }

    private fun consumeLine(line: SaleLineStockRequest, saleRefNumber: String) {
        val batches = allocateBatches(line)
        recordSaleMovements(line, batches, saleRefNumber)
    }

    private fun allocateBatches(line: SaleLineStockRequest): List<BatchAllocation> {
        var remaining = line.baseQuantity
        val batches = mutableListOf<BatchAllocation>()
        val modifiedEntries = mutableListOf<StockEntryEntity>()
        for (entry in stockEntryRepository.findFifoEntriesForProduct(line.locationProductId)) {
            if (remaining <= BigDecimal.ZERO) break
            val taken = remaining.min(entry.quantityRemaining)
            entry.quantityRemaining = entry.quantityRemaining.subtract(taken)
            modifiedEntries.add(entry)
            batches.add(BatchAllocation(entry.id!!, taken))
            remaining = remaining.subtract(taken)
        }
        if (remaining > BigDecimal.ZERO) {
            throw RtsGenericException("Insufficient stock for product ${line.locationProductId}")
        }
        stockEntryRepository.saveAll(modifiedEntries)
        return batches
    }

    private fun recordSaleMovements(line: SaleLineStockRequest, batches: List<BatchAllocation>, saleRefNumber: String) {
        var runningBalance = stockMovementRepository.findLatestBalance(line.locationProductId) ?: BigDecimal.ZERO
        val movements = batches.map { batch ->
            runningBalance = runningBalance.subtract(batch.quantityTaken)
            StockMovementEntity(
                stockEntryId = batch.stockEntryId,
                locationProductId = line.locationProductId,
                movementType = MovementType.SALE,
                movedQuantity = batch.quantityTaken,
                remainingQuantity = runningBalance,
                externalReferenceNumber = saleRefNumber,
                unitId = line.unitId,
                conversionFactor = line.conversionFactor
            )
        }
        stockMovementRepository.saveAll(movements)
    }

    fun restoreStock(lines: List<SaleLineStockRequest>, saleRefNumber: String) {
        val saleMovements = stockMovementRepository.findByExternalReferenceNumberAndMovementType(saleRefNumber, MovementType.SALE)
        val movementsByProduct = saleMovements.groupBy { it.locationProductId }
        lines.forEach { line ->
            val batchMovements = movementsByProduct[line.locationProductId] ?: return@forEach
            val stockEntriesById = stockEntryRepository.findAllById(batchMovements.map { it.stockEntryId })
                .associateBy { it.id!! }
            var runningBalance = stockMovementRepository.findLatestBalance(line.locationProductId) ?: BigDecimal.ZERO
            batchMovements.forEach { movement ->
                stockEntriesById[movement.stockEntryId]!!.quantityRemaining =
                    stockEntriesById[movement.stockEntryId]!!.quantityRemaining.add(movement.movedQuantity)
            }
            stockEntryRepository.saveAll(stockEntriesById.values)
            val movements = batchMovements.map { movement ->
                runningBalance = runningBalance.add(movement.movedQuantity)
                StockMovementEntity(
                    stockEntryId = movement.stockEntryId,
                    locationProductId = line.locationProductId,
                    movementType = MovementType.SALE_VOID,
                    movedQuantity = movement.movedQuantity,
                    remainingQuantity = runningBalance,
                    externalReferenceNumber = saleRefNumber,
                    unitId = movement.unitId,
                    conversionFactor = movement.conversionFactor
                )
            }
            stockMovementRepository.saveAll(movements)
        }
    }
}
