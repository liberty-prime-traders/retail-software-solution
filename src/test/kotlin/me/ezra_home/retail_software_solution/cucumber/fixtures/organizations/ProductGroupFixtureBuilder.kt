package me.ezra_home.retail_software_solution.cucumber.fixtures.organizations

import me.ezra_home.retail_software_solution.cucumber.fixtures.FixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.organizations.business.product_group.dto.ProductGroupInsertDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProductGroupFixtureBuilder(
  requestFactory: AuthenticatedRequestFactory,
  private val categoryFixtureBuilder: CategoryFixtureBuilder
) : FixtureBuilder<ProductGroupInsertDto>(requestFactory) {

  override val endpoint = "/secured/product-groups"

  override fun defaultDto() = ProductGroupInsertDto(
    groupName = "Group-${UUID.randomUUID().toString().take(8)}",
    categoryId = categoryFixtureBuilder.create()
  )
}
