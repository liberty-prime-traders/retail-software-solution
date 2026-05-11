package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.ConversionTargetDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraph
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SaleLinesUpdater(
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
    private val saleValidator: SaleValidator,
    private val locationProductService: LocationProductService,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val saleStockReserver: SaleStockReserver,
    private val saleLineRepository: SaleLineRepository,
) {

    fun applyLineUpdates(saleId: UUID, dto: SaleUpdateDto): SaleLineUpdateResult {
        val existing = saleLineRepository.findBySaleId(saleId).associateBy { it.id!! }
        val allProductIds = dto.linesToAdd.map { it.locationProductId } +
                dto.linesToUpdate.map { it.locationProductId } +
                existing.values.map { it.locationProductId }
        val productSummaries = locationProductDataFetcher.findSummaryByIds(allProductIds.toHashSet())
        val baseUnitsByLocationProductId = productSummaries.values.associate { it.id to it.baseUnitId }
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()

        val newLines = buildAdditions(saleId, dto.linesToAdd, productSummaries, baseUnitsByLocationProductId, unitConversionGraph)
        val updatedLines = applyUpdates(dto.linesToUpdate, existing, baseUnitsByLocationProductId, unitConversionGraph)

        val updatedIds = updatedLines.mapTo(HashSet()) { it.id }
        val resultingLines = existing.values.filter { it.id !in updatedIds } + updatedLines + newLines
        SaleValidator.guardNoDuplicateProducts(resultingLines.map { it.locationProductId })

        val baseQuantityRequestedByProductId = (newLines + updatedLines).associate { it.locationProductId to it.baseQty() }
        saleValidator.guardStockForDraftUpdates(saleId, baseQuantityRequestedByProductId, productSummaries)

        saleLineRepository.saveAll(updatedLines + newLines)
        saleStockReserver.syncUpdatedReservations(updatedLines, newLines, saleId)

        return SaleLineUpdateResult(resultingLines, productSummaries)
    }

    private fun buildAdditions(
        saleId: UUID,
        linesToAdd: List<SaleLineCreateDto>,
        productSummaries: Map<UUID, LocationProductSummaryDto>,
        baseUnitsByLocationProductId: Map<UUID, UUID>,
        unitConversionGraph: UnitConversionGraph,
    ): List<SaleLineEntity> {
        if (linesToAdd.isEmpty()) return emptyList()
        locationProductService.guardAllActive(linesToAdd.map { it.locationProductId })
        return linesToAdd.map { lineDto ->
            val baseUnitId = baseUnitsByLocationProductId.getValue(lineDto.locationProductId)
            val factor = unitConversionGraph.getFactor(lineDto.unitId, baseUnitId)
            val unitPrice = productSummaries.getValue(lineDto.locationProductId).unitPrice
                ?: throw RtsGenericException("Product ${lineDto.locationProductId} has no unit price")
            SaleLineEntity(saleId, lineDto.locationProductId, lineDto.quantity, lineDto.unitId, unitPrice, factor)
        }
    }

    private fun applyUpdates(
        linesToUpdate: List<SaleLineUpdateDto>,
        existing: Map<UUID, SaleLineEntity>,
        baseUnitsByLocationProductId: Map<UUID, UUID>,
        unitConversionGraph: UnitConversionGraph,
    ): List<SaleLineEntity> {
        val updated = mutableListOf<SaleLineEntity>()

        for (lineDto in linesToUpdate) {
            val entity = existing[lineDto.id] ?: continue
            val baseUnitId = baseUnitsByLocationProductId.getValue(lineDto.locationProductId)
            entity.quantity = lineDto.quantity
            if (entity.unitId != lineDto.unitId) {
                entity.unitId = lineDto.unitId
                entity.conversionFactor = unitConversionGraph.getFactor(lineDto.unitId, baseUnitId)
            }
            if (entity.baseQty().signum() <= 0) {
                throw RtsGenericException("Line quantity must be positive; use linesToRemove to delete a line")
            }
            updated.add(entity)
        }

        return updated
    }
}
