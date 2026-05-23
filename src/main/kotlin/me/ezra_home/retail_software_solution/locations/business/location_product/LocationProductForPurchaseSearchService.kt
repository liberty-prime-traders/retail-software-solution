package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductForPurchaseDto
import me.ezra_home.retail_software_solution.organizations.business.product.api.ProductStatus
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnLocationSchema(readOnly = true)
class LocationProductForPurchaseSearchService(
    private val locationProductCache: LocationProductCache,
    locationProductForPurchaseFetcher: LocationProductForPurchaseFetcher
) : ProductSearchService<LocationProductForPurchaseDto>(
    locationProductForPurchaseFetcher,
    LocationProductSearchQueryBuilder::buildSearchQuery
) {

    override fun countAllProducts(): Long = locationProductCache.countAllLocationProducts()

    override fun findAllProducts(): List<LocationProductForPurchaseDto> {
        val dtos = locationProductCache.findAllLocationProducts()
            .filter { it.status == ProductStatus.ACTIVE }
        return dtos.map {
            LocationProductForPurchaseDto(
                id = it.id,
                referenceNumber = it.referenceNumber,
                productName = it.productName!!,
                productGroupName = it.productGroupName!!,
                baseUnitId = it.baseUnitId!!,
                lastPurchasePrice = it.lastPurchasePrice ?: BigDecimal.ZERO
            )
        }
    }
}
