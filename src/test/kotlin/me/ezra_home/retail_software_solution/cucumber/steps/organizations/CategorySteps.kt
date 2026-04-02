package me.ezra_home.retail_software_solution.cucumber.steps.organizations

import io.cucumber.java.en.Given
import me.ezra_home.retail_software_solution.cucumber.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.context.InjectionKeys
import me.ezra_home.retail_software_solution.cucumber.fixtures.organizations.CategoryFixtureBuilder

class CategorySteps(
  private val categoryFixtureBuilder: CategoryFixtureBuilder,
  private val injectContext: InjectContext
) {

  @Given("a category exists")
  fun createCategory() {
    injectContext.store(InjectionKeys.CATEGORY, categoryFixtureBuilder.create())
  }
}
