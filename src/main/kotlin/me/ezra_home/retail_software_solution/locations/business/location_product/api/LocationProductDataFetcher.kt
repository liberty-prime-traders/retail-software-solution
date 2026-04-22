package me.ezra_home.retail_software_solution.locations.business.location_product.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.location_product.LocationProductRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class LocationProductDataFetcher(
    private val locationProductRepository: LocationProductRepository
) {

    fun findSummaryByIds(ids: Collection<UUID>): Map<UUID, LocationProductSummaryDto> =
        locationProductRepository.findAllById(ids).associate { entity ->
            entity.id!! to LocationProductSummaryDto(
                id = entity.id!!,
                referenceNumber = entity.referenceNumber!!,
                productName = entity.productName,
                productGroupName = entity.productGroupName,
                baseUnitId = entity.baseUnitId
            )
        }

    fun getBaseUnitIds(locationProductIds: Collection<UUID>): Map<UUID, UUID> =
        locationProductRepository.findAllById(locationProductIds)
            .associate { it.id!! to it.baseUnitId }
}
