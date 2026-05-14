package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductSummaryDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineCreateDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineResponseDto
import java.util.UUID

object SaleLineMapper {

    fun toLineEntities(
        saleId: UUID,
        dtoLines: List<SaleLineCreateDto>,
        insertContext: SaleLinesInsertContext,
    ): List<SaleLineEntity> =
        dtoLines.map { line ->
            val unitPrice = insertContext.productSummaries.getValue(line.locationProductId).unitPrice!!
            SaleLineEntity(
                saleId,
                line.locationProductId,
                line.quantity,
                line.unitId,
                unitPrice,
                insertContext.factorByProductId.getValue(line.locationProductId),
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
