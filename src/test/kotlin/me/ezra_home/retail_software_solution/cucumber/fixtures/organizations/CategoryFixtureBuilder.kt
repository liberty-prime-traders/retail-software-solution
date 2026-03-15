package me.ezra_home.retail_software_solution.cucumber.fixtures.organizations

import me.ezra_home.retail_software_solution.cucumber.fixtures.FixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.organizations.business.product_category.dto.ProductCategoryInsertDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CategoryFixtureBuilder(requestFactory: AuthenticatedRequestFactory)
  : FixtureBuilder<ProductCategoryInsertDto>(requestFactory) {

  override val endpoint = "/secured/product-category"

  override fun defaultDto() = ProductCategoryInsertDto(
    categoryName = "Category-${UUID.randomUUID().toString().take(8)}"
  )
}
