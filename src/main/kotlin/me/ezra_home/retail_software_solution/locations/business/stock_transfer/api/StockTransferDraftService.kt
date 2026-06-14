package me.ezra_home.retail_software_solution.locations.business.stock_transfer.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductUnitRequestDto
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchEntity
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDispatchRepository
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferResponseAssembler
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDraftLineEntity
import me.ezra_home.retail_software_solution.locations.business.stock_transfer.StockTransferDraftLineRepository
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class StockTransferDraftService(
    private val stockTransferOrderService: StockTransferOrderService,
    private val stockTransferDispatchRepository: StockTransferDispatchRepository,
    private val stockTransferDraftLineRepository: StockTransferDraftLineRepository,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val stockTransferResponseAssembler: StockTransferResponseAssembler
) {

    fun addLine(orderRef: String, stockTransferLineInsertDto: StockTransferLineInsertDto): StockTransferResponse {
        val dispatch = requireDraftDispatch(orderRef)
        val locationProductSummary = getLocationProductSummary(stockTransferLineInsertDto.locationProductId)
        guardNoDuplicateDraftProduct(dispatch.id!!, locationProductSummary)

        val locationProductId = locationProductSummary.id
        val conversionFactor = getConversionFactor(locationProductId, stockTransferLineInsertDto.unitId)

        stockTransferDraftLineRepository.save(
            StockTransferDraftLineEntity(
                stockTransferDispatchId = dispatch.id!!,
                locationProductId = locationProductId,
                quantity = stockTransferLineInsertDto.quantityDispatched,
                unitId = stockTransferLineInsertDto.unitId,
                conversionFactor = conversionFactor,
                baseUnitId = locationProductSummary.baseUnitId
            )
        )
        return buildResponse(orderRef)
    }

    private fun getLocationProductSummary(locationProductId: UUID): LocationProductSummaryDto {
        return locationProductDataFetcher.findSummaryByIds(listOf(locationProductId))
            .getValue(locationProductId)
    }

    private fun getConversionFactor(locationProductId: UUID, unitId: UUID): BigDecimal {
        return locationProductDataFetcher.getConversionFactors(
            listOf(LocationProductUnitRequestDto(locationProductId, unitId))
        ).getValue(locationProductId)
    }

    fun updateLine(orderRef: String, lineRef: String, stockTransferLineUpdateDto: StockTransferLineUpdateDto): StockTransferResponse {
        val dispatch = requireDraftDispatch(orderRef)
        val draftLine = requireDraftLine(lineRef, dispatch.id!!)

        val newUnitId = stockTransferLineUpdateDto.unitId ?: draftLine.unitId

        val conversionFactor = if (newUnitId != draftLine.unitId) {
            getConversionFactor(draftLine.locationProductId, newUnitId)
        } else {
            draftLine.conversionFactor
        }

        draftLine.quantity = stockTransferLineUpdateDto.quantityDispatched ?: draftLine.quantity
        draftLine.unitId = newUnitId
        draftLine.conversionFactor = conversionFactor
        stockTransferDraftLineRepository.save(draftLine)
        return buildResponse(orderRef)
    }


    fun removeLine(orderRef: String, lineRef: String): StockTransferResponse {
        val dispatch = requireDraftDispatch(orderRef)
        val draftLine = requireDraftLine(lineRef, dispatch.id!!)
        stockTransferDraftLineRepository.delete(draftLine)
        return buildResponse(orderRef)
    }

    private fun buildResponse(orderRef: String): StockTransferResponse =
        stockTransferResponseAssembler.build(stockTransferOrderService.getByReferenceNumber(orderRef))

    private fun requireDraftDispatch(orderRef: String): StockTransferDispatchEntity {
        val dispatch = stockTransferDispatchRepository.findByStockTransferOrderRef(orderRef)
            ?: throw RtsGenericException("Dispatch not found for order $orderRef")
        if (dispatch.status != StockTransferStatus.DRAFT) {
            throw RtsGenericException("Operation only allowed in DRAFT status. Current: ${dispatch.status}")
        }
        return dispatch
    }

    private fun requireDraftLine(lineRef: String, dispatchId: UUID): StockTransferDraftLineEntity {
        val draftLine = stockTransferDraftLineRepository.findByReferenceNumber(lineRef)
            ?: throw RtsGenericException("Draft line $lineRef not found")
        if (draftLine.stockTransferDispatchId != dispatchId) {
            throw RtsGenericException("Line $lineRef does not belong to the specified dispatch")
        }
        return draftLine
    }

    private fun guardNoDuplicateDraftProduct(dispatchId: UUID, locationProductSummary: LocationProductSummaryDto) {
        stockTransferDraftLineRepository
            .existsByStockTransferDispatchIdAndLocationProductId(dispatchId, locationProductSummary.id)
            .let { exists ->
                if (exists) throw RtsGenericException("Product ${locationProductSummary.label} already exists on this draft")
            }
    }

}
