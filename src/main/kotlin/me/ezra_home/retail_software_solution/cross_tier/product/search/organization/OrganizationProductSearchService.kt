package me.ezra_home.retail_software_solution.cross_tier.product.search.organization

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchService
import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductFetcher
import me.ezra_home.retail_software_solution.organizations.business.product.api.OrganizationProductResponseDto
import org.springframework.stereotype.Service

@Service
class OrganizationProductSearchService(private val organizationProductFetcher: OrganizationProductFetcher)
  : ProductSearchService<OrganizationProductResponseDto>(
  organizationProductFetcher,
  OrganizationProductQueryBuilder::buildSearchQuery
  ) {

  override fun countAllProducts(): Long = organizationProductFetcher.countAllProducts()

  override fun findAllProducts(): List<OrganizationProductResponseDto> = organizationProductFetcher.findAllProducts()
}
