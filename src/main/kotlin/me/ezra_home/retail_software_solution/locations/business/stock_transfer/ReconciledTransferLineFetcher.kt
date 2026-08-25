package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAvailability
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAvailabilityFetcher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

@Component
class ReconciledTransferLineFetcher(
    private val stockTransferDraftLineRepository: StockTransferDraftLineRepository,
    private val stockTransferDispatchLineRepository: StockTransferDispatchLineRepository,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val stockAvailabilityFetcher: StockAvailabilityFetcher
) {

    @TransactionalOnLocationSchema(readOnly = true, propagation = Propagation.MANDATORY)
    fun draftLines(dispatchId: UUID): List<ReconciledTransferLine> {
        val lines = stockTransferDraftLineRepository.findByStockTransferDispatchId(dispatchId)
        val labels = locationProductDataFetcher.findSummaryByIds(lines.map { it.locationProductId }.toSet())
        val availabilityByLocationProductId = stockAvailabilityFetcher.fetch(lines.map { it.locationProductId }.toSet())
        return lines.map { line ->
            val availability = availabilityByLocationProductId[line.locationProductId] ?: StockAvailability.ZERO
            ReconciledTransferLine(
                dispatchLineRef = line.requiredReference(),
                productLabel = labels.getValue(line.locationProductId).label,
                quantity = line.quantity,
                unitId = line.unitId,
                baseUnitId = line.baseUnitId,
                conversionFactor = line.conversionFactor,
                unitCost = null,
                quantityReceived = null,
                quantityAvailable = availability.quantityAvailable
            )
        }
    }

    @TransactionalOnLocationSchema(readOnly = true, propagation = Propagation.MANDATORY)
    fun dispatchLines(dispatchId: UUID, confirmedRefs: Set<String>): List<ReconciledTransferLine> {
        val lines = stockTransferDispatchLineRepository.findByStockTransferDispatchId(dispatchId)
        val labels = locationProductDataFetcher.findSummaryByIds(lines.map { it.locationProductId }.toSet())
        return lines.map { line ->
            ReconciledTransferLine(
                dispatchLineRef = line.requiredReference(),
                productLabel = labels.getValue(line.locationProductId).label,
                quantity = line.quantityDispatched,
                unitId = line.unitId,
                baseUnitId = line.baseUnitId,
                conversionFactor = line.conversionFactor,
                unitCost = line.unitCost,
                quantityReceived = if (line.requiredReference() in confirmedRefs) line.quantityDispatched else null
            )
        }
    }
}
