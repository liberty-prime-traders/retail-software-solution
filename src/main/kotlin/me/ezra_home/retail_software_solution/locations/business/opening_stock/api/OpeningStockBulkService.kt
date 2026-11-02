package me.ezra_home.retail_software_solution.locations.business.opening_stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.quantity.PositiveUserProvidedQuantity
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class OpeningStockBulkService(
    private val openingStockService: OpeningStockService,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val locationProductService: LocationProductService,
    private val unitValueFetcher: UnitValueFetcher
) {

    fun bulkDeclareInitialStock(openingStockBulkLineDtos: List<OpeningStockBulkLineDto>): List<OpeningStockResponseDto> {
        val locationProductIdsByReferenceNumber = locationProductDataFetcher.findIdsByReferenceNumbers(
             StringUtils.dropBlank(openingStockBulkLineDtos.map { it.locationProductReferenceNumber })
        )
        val unitIdsByCode = unitValueFetcher.getAllUnitValues().associate { it.code to it.id }

        val errors = mutableListOf<String>()
        val referenceNumbersInRequest = mutableSetOf<String>()
        val openingStockLineDtos = mutableListOf<OpeningStockLineDto>()
        val defaultSalePricesByLocationProductId = mutableMapOf<UUID, Money>()
        openingStockBulkLineDtos.forEachIndexed { index, openingStockBulkLineDto ->
            val referenceNumber = StringUtils.getValueOrNull(openingStockBulkLineDto.locationProductReferenceNumber)
            val unitCode = StringUtils.getValueOrNull(openingStockBulkLineDto.unitCode)
            val locationProductId = referenceNumber?.let { locationProductIdsByReferenceNumber[it] }
            val unitId = unitCode?.let { unitIdsByCode[it] }
            val costUnitCode = StringUtils.getValueOrNull(openingStockBulkLineDto.costUnitCode)
            val costUnitId = costUnitCode?.let { unitIdsByCode[it] }
            when {
                referenceNumber == null -> errors.add("Row ${index + 1}: Missing product reference number")
                unitCode == null -> errors.add("Row ${index + 1}: Missing unit code")
                locationProductId == null -> errors.add("Row ${index + 1}: Unknown product '$referenceNumber'")
                unitId == null -> errors.add("Row ${index + 1}: Unknown unit code '$unitCode'")
                !openingStockBulkLineDto.userProvidedQuantity.isZero() && openingStockBulkLineDto.unitCost == null ->
                    errors.add("Row ${index + 1}: Missing unit cost")
                costUnitCode != null && costUnitId == null ->
                    errors.add("Row ${index + 1}: Unknown cost unit code '$costUnitCode'")
                !referenceNumbersInRequest.add(referenceNumber) ->
                    errors.add("Row ${index + 1}: Product '$referenceNumber' appears more than once in the request")
                else -> {
                    defaultSalePricesByLocationProductId[locationProductId] = openingStockBulkLineDto.defaultSalePrice
                    if (!openingStockBulkLineDto.userProvidedQuantity.isZero()) {
                        openingStockLineDtos.add(
                            toOpeningStockLineDto(openingStockBulkLineDto, locationProductId, unitId, costUnitId)
                        )
                    }
                }
            }
        }
        if (errors.isNotEmpty()) {
            throw RtsGenericException("Bulk opening stock validation failed", errors)
        }

        val openingStockResponseDtos =
            if (openingStockLineDtos.isEmpty()) emptyList() else openingStockService.declareInitialStock(openingStockLineDtos)
        locationProductService.updateDefaultSalePrices(defaultSalePricesByLocationProductId)
        return openingStockResponseDtos
    }

    private fun toOpeningStockLineDto(
        openingStockBulkLineDto: OpeningStockBulkLineDto,
        locationProductId: UUID,
        unitId: UUID,
        costUnitId: UUID?
    ) = OpeningStockLineDto(
        locationProductId = locationProductId,
        userProvidedQuantity = PositiveUserProvidedQuantity.of(openingStockBulkLineDto.userProvidedQuantity.toBigDecimal()),
        unitId = unitId,
        unitCost = requireNotNull(openingStockBulkLineDto.unitCost),
        costUnitId = costUnitId
    )
}
