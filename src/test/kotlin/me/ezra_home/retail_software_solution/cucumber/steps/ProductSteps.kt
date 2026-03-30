package me.ezra_home.retail_software_solution.cucumber.steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import me.ezra_home.retail_software_solution.cucumber.context.organizations.ProductContext
import me.ezra_home.retail_software_solution.cucumber.context.organizations.ProductGroupContext
import me.ezra_home.retail_software_solution.cucumber.context.organizations.UnitContext
import me.ezra_home.retail_software_solution.cucumber.fixtures.organizations.ProductFixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.support.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.ResponseContext
import me.ezra_home.retail_software_solution.organizations.business.product.dto.OrganizationProductInsertDto
import java.util.UUID

class ProductSteps(
  private val responseContext: ResponseContext,
  private val requestFactory: AuthenticatedRequestFactory,
  private val injectContext: InjectContext,
  private val productFixtureBuilder: ProductFixtureBuilder
) {

  @Given("the following products exist:")
  fun createProducts(dataTable: DataTable) {
    dataTable.asMaps().forEach { row ->
      val id = productFixtureBuilder.createFromRow(row)
      injectContext.store(ProductContext.ID, id)
    }
  }

  @When("I create a product with name {string} and description {string}")
  fun createProduct(name: String, description: String) {
    val dto = OrganizationProductInsertDto(
      productName = name,
      description = description,
      productGroupId = resolveProductGroupId(),
      baseUnitId = resolveBaseUnitId()
    )
    responseContext.lastResponse = requestFactory.jsonRequest().body(dto).post("/secured/products")
    responseContext.lastResponse?.jsonPath()?.getString("id")?.let { injectContext.store(ProductContext.ID, it) }
  }

  @When("I search for products with text {string}")
  fun searchProducts(searchText: String) {
    responseContext.lastResponse = requestFactory.jsonRequest()
      .body(mapOf("searchText" to searchText, "searchStrategy" to "FULLTEXT"))
      .post("/secured/products/search")
  }

  private fun resolveProductGroupId(): UUID = UUID.fromString(injectContext.get(ProductGroupContext.ID))

  private fun resolveBaseUnitId(): UUID = UUID.fromString(injectContext.get(UnitContext.VALUE_ID))
}
