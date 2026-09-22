package me.ezra_home.retail_software_solution.organizations.business.product.api

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchService
import me.ezra_home.retail_software_solution.organizations.business.product.OrganizationProductQueryBuilder
import org.springframework.stereotype.Service

@Service
class OrganizationProductSearchService(
  private val organizationProductFetcher: OrganizationProductFetcher
)
  :
  ProductSearchService<OrganizationProductResponseDto>(
    organizationProductFetcher,
    OrganizationProductQueryBuilder::buildSearchQuery
  ) {

  override fun countAllProducts(): Long = organizationProductFetcher.countAllProducts()

  override fun findAllProducts(parameters: ProductSearchParameters): List<OrganizationProductResponseDto> =
    organizationProductFetcher.findAllProducts().filter { it.status in parameters.statusList }
}
