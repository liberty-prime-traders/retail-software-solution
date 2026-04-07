package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchExecutor
import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.unitvalue.api.UnitValueService
import org.hibernate.query.results.Builders.entity
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Component

@Component
class OrganizationProductSearchExecutor(
  @Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_ENTITY_MANAGER_FACTORY)
  emf: LocalContainerEntityManagerFactoryBean,
  private val mapper: OrganizationProductMapper,
  private val unitValueService: UnitValueService
) : ProductSearchExecutor<OrganizationProductEntity, OrganizationProductResponseDto>(emf, OrganizationProductEntity::class.java) {

  override fun map(entities: List<OrganizationProductEntity>): List<OrganizationProductResponseDto> {
    val unitNamesById = unitValueService.getUnitNamesById()
    return entities.map { entity ->
      mapper.toResponseDto(mapper.toDomainDto(entity), unitNamesById[entity.baseUnitId])
    }
  }
}

