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
            when (val bulkRow = resolveBulkRow(index, openingStockBulkLineDto, locationProductIdsByReferenceNumber, unitIdsByCode)) {
                is BulkRow.Invalid -> errors.add(bulkRow.message)
                is BulkRow.Resolved -> when {
                    !referenceNumbersInRequest.add(bulkRow.referenceNumber) ->
                        errors.add("Row ${index + 1}: Product '${bulkRow.referenceNumber}' appears more than once in the request")
                    else -> {
                        defaultSalePricesByLocationProductId[bulkRow.locationProductId] = bulkRow.defaultSalePrice
                        bulkRow.openingStockLineDto?.let(openingStockLineDtos::add)
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

    private fun resolveBulkRow(
        index: Int,
        openingStockBulkLineDto: OpeningStockBulkLineDto,
        locationProductIdsByReferenceNumber: Map<String, UUID>,
        unitIdsByCode: Map<String, UUID>
    ): BulkRow {
        val row = "Row ${index + 1}"
        val referenceNumber = StringUtils.getValueOrNull(openingStockBulkLineDto.locationProductReferenceNumber)
        val unitCode = StringUtils.getValueOrNull(openingStockBulkLineDto.unitCode)
        val costUnitCode = StringUtils.getValueOrNull(openingStockBulkLineDto.costUnitCode)
        val locationProductId = referenceNumber?.let { locationProductIdsByReferenceNumber[it] }
        val unitId = unitCode?.let { unitIdsByCode[it] }
        val costUnitId = costUnitCode?.let { unitIdsByCode[it] }
        val isPriceOnly = openingStockBulkLineDto.userProvidedQuantity.isZero()
        val unitCost = openingStockBulkLineDto.unitCost
        return when {
            referenceNumber == null -> BulkRow.Invalid("$row: Missing product reference number")
            unitCode == null -> BulkRow.Invalid("$row: Missing unit code")
            locationProductId == null -> BulkRow.Invalid("$row: Unknown product '$referenceNumber'")
            unitId == null -> BulkRow.Invalid("$row: Unknown unit code '$unitCode'")
            !isPriceOnly && unitCost == null -> BulkRow.Invalid("$row: Missing unit cost")
            costUnitCode != null && costUnitId == null -> BulkRow.Invalid("$row: Unknown cost unit code '$costUnitCode'")
            else -> BulkRow.Resolved(
                referenceNumber = referenceNumber,
                locationProductId = locationProductId,
                defaultSalePrice = openingStockBulkLineDto.defaultSalePrice,
                openingStockLineDto = unitCost?.takeUnless { isPriceOnly }?.let { positiveUnitCost ->
                    OpeningStockLineDto(
                        locationProductId = locationProductId,
                        userProvidedQuantity = PositiveUserProvidedQuantity.of(openingStockBulkLineDto.userProvidedQuantity.toBigDecimal()),
                        unitId = unitId,
                        unitCost = positiveUnitCost,
                        costUnitId = costUnitId
                    )
                }
            )
        }
    }

    private sealed interface BulkRow {
        data class Invalid(val message: String) : BulkRow

        data class Resolved(
            val referenceNumber: String,
            val locationProductId: UUID,
            val defaultSalePrice: Money,
            val openingStockLineDto: OpeningStockLineDto?
        ) : BulkRow
    }
}
