package me.ezra_home.retail_software_solution.cucumber.steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import me.ezra_home.retail_software_solution.cucumber.support.AuthContext
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.support.DtoConverter
import me.ezra_home.retail_software_solution.cucumber.support.InjectContext
import me.ezra_home.retail_software_solution.cucumber.context.organizations.ProductContext
import me.ezra_home.retail_software_solution.cucumber.fixtures.organizations.ProductFixture
import me.ezra_home.retail_software_solution.cucumber.fixtures.organizations.ProductFixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.ResponseContext
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants
import me.ezra_home.retail_software_solution.organizations.business.product.dto.OrganizationProductInsertDto

class ProductSteps(
  private val responseContext: ResponseContext,
  private val requestFactory: AuthenticatedRequestFactory,
  private val fixtureBuilder: ProductFixtureBuilder,
  private val dtoConverter: DtoConverter,
  private val injectContext: InjectContext,
  private val authContext: AuthContext
) {

  private var productFixture: ProductFixture? = null

  @Given("the following products exist:")
  fun createProducts(dataTable: DataTable) {
    val fixture = getOrCreateProductFixture()

    dtoConverter.fromTable(dataTable, OrganizationProductInsertDto::class.java)
      .map { it.copy(productGroupId = fixture.productGroupId, baseUnitId = fixture.baseUnitId) }
      .forEach { dto ->
        val response = requestFactory.jsonRequest().body(dto).post("/secured/products")
        response.jsonPath().getString("id")?.let { injectContext.store(ProductContext.ID, it) }
      }
  }

  @When("I create a product with name {string} and description {string}")
  fun createProduct(name: String, description: String) {
    val fixture = getOrCreateProductFixture()
    val dto = OrganizationProductInsertDto(
      productName = name,
      description = description,
      productGroupId = fixture.productGroupId,
      baseUnitId = fixture.baseUnitId
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

  private fun getOrCreateProductFixture(): ProductFixture {
    if (productFixture != null) return productFixture!!

    return synchronized(authContext) {
      val originalToken = authContext.authToken
      try {
        if (originalToken == null) {
          authContext.authToken = TestConstants.Tokens.ORG_USER
        }
        fixtureBuilder.create().also { productFixture = it }
      } finally {
        authContext.authToken = originalToken
      }
    }
  }
}
