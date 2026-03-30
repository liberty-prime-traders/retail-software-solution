package me.ezra_home.retail_software_solution.cucumber.steps

import io.cucumber.java.en.Given
import me.ezra_home.retail_software_solution.cucumber.context.organizations.CategoryContext
import me.ezra_home.retail_software_solution.cucumber.context.organizations.ProductGroupContext
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.support.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.getResponseId
import me.ezra_home.retail_software_solution.organizations.business.product_group.dto.ProductGroupInsertDto
import java.util.UUID
import kotlin.test.assertEquals

class ProductGroupSteps(
  private val requestFactory: AuthenticatedRequestFactory,
  private val injectContext: InjectContext
) {

  @Given("a product group exists")
  fun createProductGroup() {
    val categoryId = UUID.fromString(injectContext.get(CategoryContext.ID))
    val response = requestFactory.jsonRequest()
      .body(ProductGroupInsertDto(groupName = "Test Group", categoryId = categoryId))
      .post("/secured/product-groups")
    assertEquals(200, response.statusCode, "Failed to create product group. Response: ${response.asString()}")
    injectContext.store(ProductGroupContext.ID, response.getResponseId())
  }
}
