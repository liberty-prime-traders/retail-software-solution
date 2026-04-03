package me.ezra_home.retail_software_solution.cucumber.steps.organizations

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectionKeys
import me.ezra_home.retail_software_solution.cucumber.support.context.ResponseContext
import me.ezra_home.retail_software_solution.cucumber.fixtures.organizations.ProductFixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import me.ezra_home.retail_software_solution.organizations.business.product.dto.OrganizationProductInsertDto
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.queries.SearchStrategy

class ProductSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext,
  private val productFixtureBuilder: ProductFixtureBuilder
) {

  @Given("the following products exist:")
  fun createProducts(dataTable: DataTable) {
    dataTable.asMaps().forEach { row ->
      injectContext.store(InjectionKeys.PRODUCT, productFixtureBuilder.createFromRow(row))
    }
  }

  @When("I create a product with name {string} and description {string}")
  fun createProduct(name: String, description: String) {
    val response = apiClient.post(
      "/secured/products",
      OrganizationProductInsertDto(
        productName = name,
        description = description,
        productGroupId = injectContext.get(InjectionKeys.PRODUCT_GROUP),
        baseUnitId = injectContext.get(InjectionKeys.UNIT_VALUE)
      )
    )
    injectContext.store(InjectionKeys.PRODUCT, ResponseContext.idFromResponse(response))
  }

  @When("I search for products with text {string}")
  fun searchProducts(searchText: String) {
    apiClient.post(
      "/secured/products/search",
      PageRequest(
        previousCursor = null,
        requestedSize = 50,
        parameters = ProductSearchParameters(searchText = searchText, searchStrategy = SearchStrategy.FULLTEXT)
      )
    )
  }
}
