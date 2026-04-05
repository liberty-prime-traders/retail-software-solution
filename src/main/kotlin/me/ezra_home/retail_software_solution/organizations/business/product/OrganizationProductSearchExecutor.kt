package me.ezra_home.retail_software_solution.organizations.business.product

import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchExecutor
import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductResponseDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Component

@Component
class OrganizationProductSearchExecutor(
  @param:Qualifier(DataSourceBeanNames.ORGANIZATION_SCHEMA_ENTITY_MANAGER_FACTORY)
  emf: LocalContainerEntityManagerFactoryBean,
  private val mapper: OrganizationProductMapper
) : ProductSearchExecutor<OrganizationProductEntity, OrganizationProductResponseDto>(emf, OrganizationProductEntity::class.java) {

  override fun map(entity: OrganizationProductEntity) =
    mapper.toResponseDtoWithoutTags(mapper.toDomainDto(entity))
}
