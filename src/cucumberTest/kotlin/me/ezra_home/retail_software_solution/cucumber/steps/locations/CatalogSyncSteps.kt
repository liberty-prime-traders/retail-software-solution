package me.ezra_home.retail_software_solution.cucumber.steps.locations

import io.cucumber.java.en.Then
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.TransientKey
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductResponseDto
import me.ezra_home.retail_software_solution.support.TestConstants
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import org.awaitility.Awaitility.await
import java.time.Duration

class CatalogSyncSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext
) {

  @Then("the location catalog should contain product {string}")
  fun verifyLocationCatalogContainsProduct(productName: String) {
    val orgProductId = injectContext.get(TransientKey.PRODUCT)

    val searchBody = PageRequest(
      previousCursor = null,
      requestedSize = 50,
      parameters = ProductSearchParameters(searchText = productName)
    )

    await().alias("Product '$productName' (id=$orgProductId) not found in location catalog")
      .atMost(Duration.ofMillis(TestConstants.Timeouts.KAFKA_SYNC_MS))
      .pollInterval(Duration.ofMillis(TestConstants.Timeouts.KAFKA_POLL_INTERVAL_MS))
      .until {
        apiClient.post("/secured/location-products/search", searchBody)
          .jsonPath()
          .getList("contents", LocationProductResponseDto::class.java)
          ?.any { it.productName == productName && it.orgProductId == orgProductId } == true
      }
  }
}
