package me.ezra_home.retail_software_solution.cucumber.fixtures.organizations

import me.ezra_home.retail_software_solution.cucumber.context.organizations.CategoryContext
import me.ezra_home.retail_software_solution.cucumber.fixtures.FixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.support.InjectContext
import me.ezra_home.retail_software_solution.organizations.business.product_group.dto.ProductGroupInsertDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProductGroupFixtureBuilder(requestFactory: AuthenticatedRequestFactory, injectContext: InjectContext)
  : FixtureBuilder<ProductGroupInsertDto>(requestFactory, injectContext) {

  override val endpoint = "/secured/product-groups"

  override fun defaultDto() = ProductGroupInsertDto(
    groupName = "Test Group",
    categoryId = UUID.fromString(injectContext.get(CategoryContext.ID))
  )
}
