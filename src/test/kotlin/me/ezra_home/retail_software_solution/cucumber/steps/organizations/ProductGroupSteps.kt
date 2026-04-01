package me.ezra_home.retail_software_solution.cucumber.steps.organizations

import io.cucumber.java.en.Given
import me.ezra_home.retail_software_solution.cucumber.context.organizations.ProductGroupContext
import me.ezra_home.retail_software_solution.cucumber.fixtures.organizations.ProductGroupFixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.context.InjectContext

class ProductGroupSteps(
  private val productGroupFixtureBuilder: ProductGroupFixtureBuilder,
  private val injectContext: InjectContext
) {

  @Given("a product group exists")
  fun createProductGroup() {
    injectContext.store(ProductGroupContext.ID, productGroupFixtureBuilder.create())
  }
}
