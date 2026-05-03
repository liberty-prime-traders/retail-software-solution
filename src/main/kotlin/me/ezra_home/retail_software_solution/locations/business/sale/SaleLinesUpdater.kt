package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineUpdateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.ConversionTargetDto
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

data class SaleLineUpdateResult(
    val lines: List<SaleLineEntity>,
    val baseQtyByProductId: Map<UUID, BigDecimal>
)

@Service
@TransactionalOnLocationSchema
class SaleLinesUpdater(
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
    private val saleValidator: SaleValidator,
    private val locationProductService: LocationProductService,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val saleStockReserver: SaleStockReserver,
    private val saleLineRepository: SaleLineRepository
) {

    fun applyLineUpdates(saleId: UUID, dto: SaleUpdateDto): SaleLineUpdateResult {
        val existing = saleLineRepository.findBySaleId(saleId).associateBy { it.id!! }
        val allProductIds = dto.linesToAdd.map { it.locationProductId } + dto.linesToUpdate.map { it.locationProductId }
        val productSummaries = locationProductDataFetcher.findSummaryByIds(allProductIds)
        val baseUnitsByLocationProductId = productSummaries.values.associate { it.id to it.baseUnitId }
        val graph = unitConversionGraphFacade.getOrLoad()

        val additions = computeAdditions(saleId, dto.linesToAdd, baseUnitsByLocationProductId, graph)
        val updates: UpdateResult = computeUpdates(dto.linesToUpdate, existing, baseUnitsByLocationProductId, graph)

        val newLines = additions.map { (entity, _) -> entity }
        val deletedIds = updates.toDelete.mapTo(HashSet()) { it.id }
        val updatedIds = updates.toUpdate.mapTo(HashSet()) { it.id }
        val resultingLines = existing.values.filter { it.id !in deletedIds && it.id !in updatedIds } + updates.toUpdate + newLines
        SaleValidator.guardNoDuplicateProducts(resultingLines.map { it.locationProductId })

        val baseQuantitiesForSurvivingLines = (additions + updates.updatedWithNewBaseQty).associate {
            (entity, qty) -> entity.locationProductId to qty
        }
        val alreadyReserved = updates.toUpdate.associate {
            it.locationProductId to saleStockReserver.getReserved(it.locationProductId)
        }
        saleValidator.guardStockForDraftUpdates(baseQuantitiesForSurvivingLines, alreadyReserved, productSummaries)

        updates.toDelete.forEach { saleStockReserver.clearByLines(listOf(it.id!!), it.locationProductId) }
        saleLineRepository.deleteAll(updates.toDelete)
        saleLineRepository.saveAll(updates.toUpdate + newLines)
        saleStockReserver.syncUpdatedReservations(
            updates.updatedWithNewBaseQty,
            newLines,
            baseQuantitiesForSurvivingLines,
            saleId
        )

        return SaleLineUpdateResult(saleLineRepository.findBySaleId(saleId), baseQuantitiesForSurvivingLines)
    }

    private fun computeAdditions(
        saleId: UUID,
        linesToAdd: List<SaleLineCreateDto>,
        baseUnitsByLocationProductId: Map<UUID, UUID>,
        graph: Map<UUID, Map<UUID, ConversionTargetDto>>
    ): List<Pair<SaleLineEntity, BigDecimal>> {
        if (linesToAdd.isEmpty()) return emptyList()
        locationProductService.guardAllActive(linesToAdd.map { it.locationProductId })
        return linesToAdd.map { lineDto ->
            val baseUnitId = baseUnitsByLocationProductId[lineDto.locationProductId]!!
            val baseQty = unitConversionGraphFacade.convert(lineDto.unitId, baseUnitId, lineDto.quantity)
            val factor = graph[lineDto.unitId]?.get(baseUnitId)?.factor!!
            SaleLineEntity(saleId, lineDto.locationProductId, lineDto.quantity, lineDto.unitId, lineDto.unitPrice, factor) to baseQty
        }
    }

    private data class UpdateResult(
        val toDelete: List<SaleLineEntity>,
        val toUpdate: List<SaleLineEntity>,
        val updatedWithNewBaseQty: List<Pair<SaleLineEntity, BigDecimal>>
    )

    private fun computeUpdates(
        linesToUpdate: List<SaleLineUpdateDto>,
        existing: Map<UUID, SaleLineEntity>,
        baseUnitsByLocationProductId: Map<UUID, UUID>,
        graph: Map<UUID, Map<UUID, ConversionTargetDto>>
    ): UpdateResult {
        val toDelete = mutableListOf<SaleLineEntity>()
        val toUpdate = mutableListOf<SaleLineEntity>()
        val updatedWithNewBaseQty = mutableListOf<Pair<SaleLineEntity, BigDecimal>>()

        for (lineDto in linesToUpdate) {
            val entity = existing[lineDto.id] ?: continue
            val baseUnitId = baseUnitsByLocationProductId[lineDto.locationProductId]!!
            val baseQty = unitConversionGraphFacade.convert(lineDto.unitId, baseUnitId, lineDto.quantity)

            if (baseQty <= BigDecimal.ZERO) {
                toDelete.add(entity)
            } else {
                entity.quantity = lineDto.quantity
                entity.unitPrice = lineDto.unitPrice
                if (entity.unitId != lineDto.unitId) {
                    entity.unitId = lineDto.unitId
                    entity.conversionFactor = graph[lineDto.unitId]?.get(baseUnitId)?.factor!!
                }
                toUpdate.add(entity)
                updatedWithNewBaseQty.add(entity to baseQty)
            }
        }

        return UpdateResult(toDelete, toUpdate, updatedWithNewBaseQty)
    }
}
