package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockAvailabilityFetcher
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockBreakdownProvider
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.quantity.BaseUnitDisplay
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import java.math.RoundingMode
import java.util.UUID

@Component
class ReconciledTransferLineFetcher(
    private val stockTransferDraftLineRepository: StockTransferDraftLineRepository,
    private val stockTransferDispatchLineRepository: StockTransferDispatchLineRepository,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val stockAvailabilityFetcher: StockAvailabilityFetcher,
    private val stockBreakdownProvider: StockBreakdownProvider,
    private val unitConversionGraphFacade: UnitConversionGraphFacade
) {

    @TransactionalOnLocationSchema(readOnly = true, propagation = Propagation.MANDATORY)
    fun draftLines(dispatchId: UUID): List<ReconciledTransferLine> {
        val lines = stockTransferDraftLineRepository.findByStockTransferDispatchId(dispatchId)
        val summaries = locationProductDataFetcher.findSummaryByIds(lines.map { it.locationProductId }.toSet())
        val availabilityByLocationProductId = stockAvailabilityFetcher.fetch(lines.map { it.locationProductId }.toSet())
        val breakdownByLocationProductId = stockBreakdownProvider.forLocationProducts(lines.map { it.locationProductId }.toSet())
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        return lines.map { line ->
            val summary = summaries.getValue(line.locationProductId)
            val storageUnitsPerBaseUnit = unitConversionGraph.getSizeInRoot(summary.baseUnitId)
            val availability = availabilityByLocationProductId.getValue(line.locationProductId)
            val storageQuantity = line.storageQuantity()
            ReconciledTransferLine(
                dispatchLineRef = line.requiredReference(),
                productLabel = summary.label,
                quantity = line.userProvidedQuantity,
                unitId = line.unitId,
                baseUnitId = summary.baseUnitId,
                conversionFactor = BaseUnitDisplay.conversionFactor(line.storageUnitsPerUserProvidedUnit, storageUnitsPerBaseUnit),
                unitCost = null,
                totalCost = null,
                quantityReceived = null,
                quantityAvailable = BaseUnitDisplay.storageQuantityInBaseUnit(availability.quantityAvailable, storageUnitsPerBaseUnit, RoundingMode.DOWN),
                storageQuantity = storageQuantity.toStorageQuantity(),
                storageBreakdownText = breakdownByLocationProductId.getValue(line.locationProductId).render(storageQuantity)
            )
        }
    }

    @TransactionalOnLocationSchema(readOnly = true, propagation = Propagation.MANDATORY)
    fun dispatchLines(dispatchId: UUID, confirmedRefs: Set<String>): List<ReconciledTransferLine> {
        val lines = stockTransferDispatchLineRepository.findByStockTransferDispatchId(dispatchId)
        val summaries = locationProductDataFetcher.findSummaryByIds(lines.map { it.locationProductId }.toSet())
        val breakdownByLocationProductId = stockBreakdownProvider.forLocationProducts(lines.map { it.locationProductId }.toSet())
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        return lines.map { line ->
            val summary = summaries.getValue(line.locationProductId)
            val storageUnitsPerBaseUnit = unitConversionGraph.getSizeInRoot(summary.baseUnitId)
            ReconciledTransferLine(
                dispatchLineRef = line.requiredReference(),
                productLabel = summary.label,
                quantity = line.wholeLineUserProvidedQuantity,
                unitId = line.unitId,
                baseUnitId = summary.baseUnitId,
                conversionFactor = BaseUnitDisplay.conversionFactor(line.storageUnitsPerUserProvidedUnit, storageUnitsPerBaseUnit),
                unitCost = line.unitCost,
                totalCost = line.costDispatched,
                quantityReceived = if (line.requiredReference() in confirmedRefs) line.wholeLineUserProvidedQuantity else null,
                storageQuantity = line.storageQuantityDispatched,
                storageBreakdownText = breakdownByLocationProductId.getValue(line.locationProductId)
                    .render(ExactQuantity.ofStorage(line.storageQuantityDispatched))
            )
        }
    }
}
