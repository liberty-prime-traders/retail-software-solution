package me.ezra_home.retail_software_solution.cucumber.fixtures.organizations

import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectionKeys
import me.ezra_home.retail_software_solution.cucumber.fixtures.FixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import me.ezra_home.retail_software_solution.organizations.business.product_group.dto.ProductGroupInsertDto
import org.springframework.stereotype.Component

@Component
class ProductGroupFixtureBuilder(
  injectContext: InjectContext,
  apiClient: ApiClient
) : FixtureBuilder<ProductGroupInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/product-groups"

  override fun defaultDto() = ProductGroupInsertDto(
    groupName = "Test Group",
    categoryId = injectContext.get(InjectionKeys.CATEGORY)
  )
}
