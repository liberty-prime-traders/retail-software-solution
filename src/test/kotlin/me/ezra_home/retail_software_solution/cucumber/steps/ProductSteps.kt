package me.ezra_home.retail_software_solution.cucumber.steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import me.ezra_home.retail_software_solution.cucumber.config.TestContext
import me.ezra_home.retail_software_solution.cucumber.config.TestDataManager
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class ProductSteps {

  @Autowired
  private lateinit var context: TestContext

  @Autowired
  private lateinit var dataManager: TestDataManager

  @Given("the following products exist:")
  fun createProducts(dataTable: DataTable) {
    dataTable.asMaps().forEach { row ->
      val productData = mapOf(
        "productName" to row["name"],
        "description" to row["description"],
        "productGroupId" to UUID.randomUUID().toString(),
        "baseUnitId" to UUID.randomUUID().toString(),
        "status" to (row["status"] ?: "ACTIVE")
      )

      val response = given()
        .baseUri(context.baseUrl)
        .contentType(ContentType.JSON)
        .header("Authorization", "Bearer ${context.authToken}")
        .body(productData)
        .post("/secured/products")

      if (response.statusCode == 201) {
        dataManager.track("product", UUID.fromString(response.jsonPath().getString("id")))
      }
    }
  }

  @When("I create a product with name {string} and description {string}")
  fun createProduct(name: String, description: String) {
    val productData = mapOf(
      "productName" to name,
      "description" to description,
      "productGroupId" to UUID.randomUUID().toString(),
      "baseUnitId" to UUID.randomUUID().toString()
    )

    context.lastResponse = given()
      .baseUri(context.baseUrl)
      .contentType(ContentType.JSON)
      .header("Authorization", "Bearer ${context.authToken}")
      .body(productData)
      .post("/secured/products")
  }

  @When("I search for products with text {string}")
  fun searchProducts(searchText: String) {
    val searchParams = mapOf(
      "searchText" to searchText,
      "searchStrategy" to "FULLTEXT"
    )

    context.lastResponse = given()
      .baseUri(context.baseUrl)
      .contentType(ContentType.JSON)
      .header("Authorization", "Bearer ${context.authToken}")
      .body(searchParams)
      .post("/secured/products/search")
  }
}
