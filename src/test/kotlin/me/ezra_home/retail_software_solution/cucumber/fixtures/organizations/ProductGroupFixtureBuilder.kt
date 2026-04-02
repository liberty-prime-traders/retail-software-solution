package me.ezra_home.retail_software_solution.cucumber.fixtures.organizations

import me.ezra_home.retail_software_solution.cucumber.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.context.InjectionKeys
import me.ezra_home.retail_software_solution.cucumber.fixtures.FixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.organizations.business.product_group.dto.ProductGroupInsertDto
import org.springframework.stereotype.Component

@Component
class ProductGroupFixtureBuilder(requestFactory: AuthenticatedRequestFactory, injectContext: InjectContext)
  : FixtureBuilder<ProductGroupInsertDto>(requestFactory, injectContext) {

  override val endpoint = "/secured/product-groups"

  override fun defaultDto() = ProductGroupInsertDto(
    groupName = "Test Group",
    categoryId = injectContext.get(InjectionKeys.CATEGORY)
  )
}
