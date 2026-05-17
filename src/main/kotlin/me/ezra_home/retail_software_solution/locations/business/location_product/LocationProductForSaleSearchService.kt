package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchService
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductForSaleDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryFetcher
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class LocationProductForSaleSearchService(
    private val locationProductCache: LocationProductCache,
    private val stockEntryFetcher: StockEntryFetcher,
    locationProductForSaleFetcher: LocationProductForSaleFetcher
) : ProductSearchService<LocationProductForSaleDto>(
    locationProductForSaleFetcher,
    LocationProductSearchQueryBuilder::buildSearchQuery
) {

    override fun countAllProducts(): Long = locationProductCache.countAllLocationProducts()

    override fun findAllProducts(): List<LocationProductForSaleDto> {
        val dtos = locationProductCache.findAllLocationProducts()
        val entriesByProductId = stockEntryFetcher.fetchAvailableEntriesByProductIds(dtos.map { it.id })
        return dtos.map {
            LocationProductForSaleDto(
                id = it.id,
                referenceNumber = it.referenceNumber,
                productName = it.productName!!,
                productGroupName = it.productGroupName!!,
                baseUnitId = it.baseUnitId!!,
                defaultSalePrice = it.defaultSalePrice,
                stockBatches = entriesByProductId[it.id].orEmpty()
            )
        }
    }
}
