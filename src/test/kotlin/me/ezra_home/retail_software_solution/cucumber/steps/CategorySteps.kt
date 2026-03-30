package me.ezra_home.retail_software_solution.cucumber.steps

import io.cucumber.java.en.Given
import me.ezra_home.retail_software_solution.cucumber.context.organizations.CategoryContext
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.support.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.getResponseId
import me.ezra_home.retail_software_solution.organizations.business.product_category.dto.ProductCategoryInsertDto
import kotlin.test.assertEquals

class CategorySteps(
  private val requestFactory: AuthenticatedRequestFactory,
  private val injectContext: InjectContext
) {

  @Given("a category exists")
  fun createCategory() {
    val response = requestFactory.jsonRequest()
      .body(ProductCategoryInsertDto(categoryName = "Test Category"))
      .post("/secured/product-category")
    assertEquals(200, response.statusCode, "Failed to create category. Response: ${response.asString()}")
    injectContext.store(CategoryContext.ID, response.getResponseId())
  }
}
