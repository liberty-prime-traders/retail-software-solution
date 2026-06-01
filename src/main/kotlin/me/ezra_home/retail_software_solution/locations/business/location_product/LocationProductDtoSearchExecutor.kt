package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchExecutor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Component

@Component
class LocationProductDtoSearchExecutor(
    @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_ENTITY_MANAGER_FACTORY)
    emf: LocalContainerEntityManagerFactoryBean,
    private val locationProductMapper: LocationProductMapper,
) : ProductSearchExecutor<LocationProductEntity, LocationProductDto>(emf, LocationProductEntity::class.java) {

    override fun map(entities: List<LocationProductEntity>): List<LocationProductDto> =
        entities.map { locationProductMapper.toDomainDto(it) }
}
