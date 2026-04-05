package me.ezra_home.retail_software_solution.cross_tier.product.search.organization

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchService
import me.ezra_home.retail_software_solution.organizations.business.product.public.OrganizationProductService
import me.ezra_home.retail_software_solution.organizations.business.product.public.OrganizationProductResponseDto
import org.springframework.stereotype.Service

@Service
class OrganizationProductSearchService(
  private val organizationProductService: OrganizationProductService,
  organizationProductFetcher: OrganizationProductFetcher
) : ProductSearchService<OrganizationProductResponseDto>(
  organizationProductFetcher,
  OrganizationProductQueryBuilder::buildSearchQuery
) {

  override fun countAllProducts(): Long = organizationProductService.countAllProducts()

  override fun findAllProducts(): List<OrganizationProductResponseDto> = organizationProductService.findAllProducts()
}
