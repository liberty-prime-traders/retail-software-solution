package me.ezra_home.retail_software_solution.cucumber.steps.organizations

import io.cucumber.java.en.Given
import me.ezra_home.retail_software_solution.cucumber.context.organizations.CategoryContext
import me.ezra_home.retail_software_solution.cucumber.fixtures.organizations.CategoryFixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.context.InjectContext

class CategorySteps(
  private val categoryFixtureBuilder: CategoryFixtureBuilder,
  private val injectContext: InjectContext
) {

  @Given("a category exists")
  fun createCategory() {
    injectContext.store(CategoryContext.ID, categoryFixtureBuilder.create())
  }
}
