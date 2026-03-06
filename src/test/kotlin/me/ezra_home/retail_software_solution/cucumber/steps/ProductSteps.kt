package me.ezra_home.retail_software_solution.cucumber.steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import me.ezra_home.retail_software_solution.cucumber.config.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.config.ProductFixture
import me.ezra_home.retail_software_solution.cucumber.config.TestContext
import me.ezra_home.retail_software_solution.cucumber.config.TestDataManager
import me.ezra_home.retail_software_solution.cucumber.config.TestFixtureBuilder
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class ProductSteps {

  @Autowired
  private lateinit var context: TestContext

  @Autowired
  private lateinit var dataManager: TestDataManager

  @Autowired
  private lateinit var requestFactory: AuthenticatedRequestFactory

  @Autowired
  private lateinit var fixtureBuilder: TestFixtureBuilder

  @Given("the following products exist:")
  fun createProducts(dataTable: DataTable) {
    val fixture = getOrCreateProductFixture()

    dataTable.asMaps().forEach { row ->
      val productData = mapOf(
        "productName" to row["name"],
        "description" to row["description"],
        "productGroupId" to fixture.productGroupId.toString(),
        "baseUnitId" to fixture.baseUnitId.toString(),
        "status" to (row["status"] ?: "ACTIVE")
      )

      val response = requestFactory.jsonRequest()
        .body(productData)
        .post("/secured/products")

      if (response.statusCode == 201) {
        dataManager.track("product", UUID.fromString(response.jsonPath().getString("id")))
      }
    }
  }

  @When("I create a product with name {string} and description {string}")
  fun createProduct(name: String, description: String) {
    val fixture = getOrCreateProductFixture()

    val productData = mapOf(
      "productName" to name,
      "description" to description,
      "productGroupId" to fixture.productGroupId.toString(),
      "baseUnitId" to fixture.baseUnitId.toString()
    )

    context.lastResponse = requestFactory.jsonRequest()
      .body(productData)
      .post("/secured/products")
  }

  @When("I search for products with text {string}")
  fun searchProducts(searchText: String) {
    val searchParams = mapOf(
      "searchText" to searchText,
      "searchStrategy" to "FULLTEXT"
    )

    context.lastResponse = requestFactory.jsonRequest()
      .body(searchParams)
      .post("/secured/products/search")
  }

  private fun getOrCreateProductFixture(): ProductFixture {
    context.get("productFixture", ProductFixture::class.java)?.let { return it }

    val fixture = fixtureBuilder.createProductFixture()
    context.store("productFixture", fixture)
    return fixture
  }
}
