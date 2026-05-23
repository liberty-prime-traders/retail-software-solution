package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchExecutor
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductForPurchaseDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Component

@Component
class LocationProductForPurchaseSearchExecutor(
    @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_ENTITY_MANAGER_FACTORY)
    emf: LocalContainerEntityManagerFactoryBean,
) : ProductSearchExecutor<LocationProductEntity, LocationProductForPurchaseDto>(emf, LocationProductEntity::class.java) {

    override fun map(entities: List<LocationProductEntity>): List<LocationProductForPurchaseDto> {
        return entities.map {
            LocationProductForPurchaseDto(
                id = it.id!!,
                referenceNumber = it.requiredReference(),
                productName = it.productName,
                productGroupName = it.productGroupName,
                baseUnitId = it.baseUnitId,
                lastPurchasePrice = it.lastPurchasePrice!!,
            )
        }
    }
}
