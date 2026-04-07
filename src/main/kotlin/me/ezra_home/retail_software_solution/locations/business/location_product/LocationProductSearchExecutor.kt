package me.ezra_home.retail_software_solution.locations.business.location_product

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchExecutor
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Component

@Component
class LocationProductSearchExecutor(
  @Qualifier(DataSourceBeanNames.LOCATION_SCHEMA_ENTITY_MANAGER_FACTORY)
  emf: LocalContainerEntityManagerFactoryBean,
  private val mapper: LocationProductMapper,
  private val unitValueService: UnitValueService
) : ProductSearchExecutor<LocationProductEntity, LocationProductResponseDto>(emf, LocationProductEntity::class.java) {


  override fun map(entities: List<LocationProductEntity>): List<LocationProductResponseDto> {
    val unitNamesById = unitValueService.getUnitNamesById()
    return entities.map { entity ->
      mapper.toDto(mapper.toDomainDto(entity), unitNamesById[entity.baseUnitId])
    }
  }
}
