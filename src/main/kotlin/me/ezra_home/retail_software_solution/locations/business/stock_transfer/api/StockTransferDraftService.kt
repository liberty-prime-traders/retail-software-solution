package me.ezra_home.retail_software_solution.locations.business.stock_transfer.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductUnitRequestDto
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchEntity
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDraftDispatchFetcher
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferResponseAssembler
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDraftLineEntity
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDraftLineRepository
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.ReconciledTransferLineFetcher
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDataFetcher
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class StockTransferDraftService(
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher,
    private val stockTransferDraftDispatchFetcher: StockTransferDraftDispatchFetcher,
    private val stockTransferDraftLineRepository: StockTransferDraftLineRepository,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val stockTransferResponseAssembler: StockTransferResponseAssembler,
    private val reconciledTransferLineFetcher: ReconciledTransferLineFetcher
) {

    fun applyLineChanges(orderRef: String, lineRequestDto: StockTransferLineRequestDto): StockTransferResponse {
        val dispatch = stockTransferDraftDispatchFetcher.requireDraftDispatch(orderRef)
        val existingLines = stockTransferDraftLineRepository.findByStockTransferDispatchId(dispatch.id!!)
        val existingLinesByRef = existingLines.associateBy { it.requiredReference() }
        guardUpdatesTargetExistingLines(lineRequestDto.updates, existingLinesByRef)

        val locationProductSummariesById = locationProductDataFetcher.findSummaryByIds(
            existingLines.map { it.locationProductId } + lineRequestDto.additions.map { it.locationProductId }
        )
        guardNoDuplicateAdditionProducts(lineRequestDto.additions, existingLines, locationProductSummariesById)

        val conversionFactorsByLocationProductId = locationProductDataFetcher.getConversionFactors(
            conversionFactorRequests(lineRequestDto, existingLinesByRef)
        )

        val newDraftLines = lineRequestDto.additions.map { addition ->
            StockTransferDraftLineEntity(
                stockTransferDispatchId = dispatch.id!!,
                locationProductId = addition.locationProductId,
                quantity = addition.quantityDispatched,
                unitId = addition.unitId,
                conversionFactor = conversionFactorsByLocationProductId.getValue(addition.locationProductId),
                baseUnitId = locationProductSummariesById.getValue(addition.locationProductId).baseUnitId
            )
        }

        val updatedExistingLines = lineRequestDto.updates.map { update ->
            val existingLine = existingLinesByRef.getValue(update.lineRef)
            val newUnitId = update.unitId ?: existingLine.unitId
            existingLine.conversionFactor = if (newUnitId != existingLine.unitId) {
                conversionFactorsByLocationProductId.getValue(existingLine.locationProductId)
            } else {
                existingLine.conversionFactor
            }
            existingLine.quantity = update.quantityDispatched ?: existingLine.quantity
            existingLine.unitId = newUnitId
            existingLine
        }

        stockTransferDraftLineRepository.saveAll(newDraftLines)
        stockTransferDraftLineRepository.saveAll(updatedExistingLines)
        return buildDraftResponse(orderRef, dispatch)
    }

    private fun conversionFactorRequests(
        lineRequestDto: StockTransferLineRequestDto,
        existingLinesByRef: Map<String, StockTransferDraftLineEntity>
    ): List<LocationProductUnitRequestDto> = buildList {
        lineRequestDto.additions.forEach { addition ->
            add(LocationProductUnitRequestDto(addition.locationProductId, addition.unitId))
        }
        lineRequestDto.updates.forEach { update ->
            val existingLine = existingLinesByRef.getValue(update.lineRef)
            val newUnitId = update.unitId ?: existingLine.unitId
            if (newUnitId != existingLine.unitId) {
                add(LocationProductUnitRequestDto(existingLine.locationProductId, newUnitId))
            }
        }
    }

    private fun guardUpdatesTargetExistingLines(
        updates: List<StockTransferLineUpdateDto>,
        existingLinesByRef: Map<String, StockTransferDraftLineEntity>
    ) {
        val missingRefs = updates.map { it.lineRef }.filterNot { existingLinesByRef.containsKey(it) }
        if (missingRefs.isNotEmpty()) {
            throw RtsGenericException("Line(s) not found on this draft: ${missingRefs.joinToString()}")
        }
    }

    private fun guardNoDuplicateAdditionProducts(
        additions: List<StockTransferLineInsertDto>,
        existingLines: List<StockTransferDraftLineEntity>,
        locationProductSummariesById: Map<UUID, LocationProductSummaryDto>
    ) {
        val existingProductIds = existingLines.map { it.locationProductId }.toSet()
        val seenAdditionProductIds = mutableSetOf<UUID>()
        additions.forEach { addition ->
            val locationProductId = addition.locationProductId
            if (locationProductId in existingProductIds || !seenAdditionProductIds.add(locationProductId)) {
                val label = locationProductSummariesById.getValue(locationProductId).label
                throw RtsGenericException("Product $label already exists on this draft")
            }
        }
    }

    fun removeLine(orderRef: String, lineRef: String): StockTransferResponse {
        val dispatch = stockTransferDraftDispatchFetcher.requireDraftDispatch(orderRef)
        val draftLine = requireDraftLine(lineRef, dispatch.id!!)
        stockTransferDraftLineRepository.delete(draftLine)
        return buildDraftResponse(orderRef, dispatch)
    }

    private fun buildDraftResponse(orderRef: String, dispatch: StockTransferDispatchEntity): StockTransferResponse {
        val order = stockTransferOrderDataFetcher.getByReferenceNumber(orderRef)
        val draftLines = reconciledTransferLineFetcher.draftLines(dispatch.id!!)
        return stockTransferResponseAssembler.buildDispatchOnly(order, dispatch, draftLines)
    }

    private fun requireDraftLine(lineRef: String, dispatchId: UUID): StockTransferDraftLineEntity {
        val draftLine = stockTransferDraftLineRepository.findByReferenceNumber(lineRef)
            ?: throw RtsGenericException("Draft line $lineRef not found")
        if (draftLine.stockTransferDispatchId != dispatchId) {
            throw RtsGenericException("Line $lineRef does not belong to the specified dispatch")
        }
        return draftLine
    }

}
