package me.ezra_home.retail_software_solution.cucumber.steps.organizations

import io.cucumber.java.en.Given
import me.ezra_home.retail_software_solution.cucumber.fixtures.organizations.ProductGroupFixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.TransientKey

class ProductGroupSteps(
  private val productGroupFixtureBuilder: ProductGroupFixtureBuilder,
  private val injectContext: InjectContext
) {

  @Given("a product group exists")
  fun createProductGroup() {
    injectContext.store(TransientKey.PRODUCT_GROUP, productGroupFixtureBuilder.create())
  }
}
