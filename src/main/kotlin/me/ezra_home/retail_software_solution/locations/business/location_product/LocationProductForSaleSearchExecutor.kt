package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchExecutor
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductForSaleDto
import me.ezra_home.retail_software_solution.locations.business.stock.api.StockEntryFetcher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Component

@Component
class LocationProductForSaleSearchExecutor(
    @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_ENTITY_MANAGER_FACTORY)
    emf: LocalContainerEntityManagerFactoryBean,
    private val stockEntryFetcher: StockEntryFetcher
) : ProductSearchExecutor<LocationProductEntity, LocationProductForSaleDto>(emf, LocationProductEntity::class.java) {

    override fun map(entities: List<LocationProductEntity>): List<LocationProductForSaleDto> {
        val sellable = entities.filter { it.defaultSalePrice != null }
        val entriesByProductId = stockEntryFetcher.fetchAvailableEntriesByProductIds(
            sellable.mapNotNull { it.id }
        )
        return sellable.map {
            LocationProductForSaleDto(
                id = it.id!!,
                referenceNumber = it.requiredReference(),
                productName = it.productName,
                productGroupName = it.productGroupName,
                defaultSalePrice = it.defaultSalePrice,
                stockBatches = entriesByProductId[it.id].orEmpty()
            )
        }
    }
}
