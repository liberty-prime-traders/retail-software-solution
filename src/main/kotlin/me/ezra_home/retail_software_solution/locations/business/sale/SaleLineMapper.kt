package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineResponseDto
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.util.UUID

object SaleLineMapper {

    fun toLineEntities(
        saleId: UUID,
        dtoLines: List<SaleLineCreateDto>,
        validatedSaleLines: ValidatedSaleLines,
    ): List<SaleLineEntity> =
        dtoLines.map { line ->
            val unitPrice = validatedSaleLines.productSummaries.getValue(line.locationProductId).unitPrice!!
            SaleLineEntity(
                saleId,
                line.locationProductId,
                line.quantity,
                line.unitId,
                unitPrice,
                validatedSaleLines.factorByProductId.getValue(line.locationProductId),
            )
        }

    fun toResponseLines(
        saleLineEntities: List<SaleLineEntity>,
        productSummaries: Map<UUID, LocationProductSummaryDto>
    ): List<SaleLineResponseDto> {
        return saleLineEntities.map { line ->
            SaleLineResponseDto(
                id = line.id!!,
                referenceNumber = line.requiredReference(),
                locationProductId = line.locationProductId,
                quantity = line.quantity,
                unitId = line.unitId,
                conversionFactor = line.conversionFactor,
                unitPrice = line.unitPrice,
                lineTotal = line.lineTotal(),
                locationProduct = productSummaries.getValue(line.locationProductId)
            )
        }
    }
}
