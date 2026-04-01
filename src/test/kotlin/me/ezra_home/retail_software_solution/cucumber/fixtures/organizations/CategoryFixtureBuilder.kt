package me.ezra_home.retail_software_solution.cucumber.fixtures.organizations

import me.ezra_home.retail_software_solution.cucumber.fixtures.FixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.context.InjectContext
import me.ezra_home.retail_software_solution.organizations.business.product_category.dto.ProductCategoryInsertDto
import org.springframework.stereotype.Component

@Component
class CategoryFixtureBuilder(requestFactory: AuthenticatedRequestFactory, injectContext: InjectContext)
  : FixtureBuilder<ProductCategoryInsertDto>(requestFactory, injectContext) {

  override val endpoint = "/secured/product-category"

  override fun defaultDto() = ProductCategoryInsertDto(categoryName = "Test Category")
}
