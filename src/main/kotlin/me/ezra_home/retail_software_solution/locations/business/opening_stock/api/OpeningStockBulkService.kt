package me.ezra_home.retail_software_solution.locations.business.opening_stock.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductUpdateDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueFetcher
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.Optional
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
        openingStockBulkLineDtos.forEachIndexed { index, openingStockBulkLineDto ->
            val referenceNumber = StringUtils.getValueOrNull(openingStockBulkLineDto.locationProductReferenceNumber)
            val unitCode = StringUtils.getValueOrNull(openingStockBulkLineDto.unitCode)
            val locationProductId = referenceNumber?.let { locationProductIdsByReferenceNumber[it] }
            val unitId = unitCode?.let { unitIdsByCode[it] }
            when {
                referenceNumber == null -> errors.add("Row ${index + 1}: Missing product reference number")
                unitCode == null -> errors.add("Row ${index + 1}: Missing unit code")
                locationProductId == null -> errors.add("Row ${index + 1}: Unknown product '$referenceNumber'")
                unitId == null -> errors.add("Row ${index + 1}: Unknown unit code '$unitCode'")
                !referenceNumbersInRequest.add(referenceNumber) ->
                    errors.add("Row ${index + 1}: Product '$referenceNumber' appears more than once in the request")
                else -> openingStockLineDtos.add(toOpeningStockLineDto(openingStockBulkLineDto, locationProductId, unitId))
            }
        }
        if (errors.isNotEmpty()) {
            throw RtsGenericException("Bulk opening stock validation failed", errors)
        }

        val openingStockResponseDtos = openingStockService.declareInitialStock(openingStockLineDtos)
        openingStockBulkLineDtos.forEach { openingStockBulkLineDto ->
            locationProductService.updateProduct(
                LocationProductUpdateDto(
                    id = locationProductIdsByReferenceNumber.getValue(
                        StringUtils.getValueOrException(
                            openingStockBulkLineDto.locationProductReferenceNumber,
                            "Missing product reference number"
                        )
                    ),
                    defaultSalePrice = Optional.of(openingStockBulkLineDto.defaultSalePrice)
                )
            )
        }
        return openingStockResponseDtos
    }

    private fun toOpeningStockLineDto(
        openingStockBulkLineDto: OpeningStockBulkLineDto,
        locationProductId: UUID,
        unitId: UUID
    ) = OpeningStockLineDto(
        locationProductId = locationProductId,
        userProvidedQuantity = openingStockBulkLineDto.userProvidedQuantity,
        unitId = unitId,
        unitCost = openingStockBulkLineDto.unitCost
    )
}
