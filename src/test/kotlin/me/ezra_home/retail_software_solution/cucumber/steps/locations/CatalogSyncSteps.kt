package me.ezra_home.retail_software_solution.cucumber.steps.locations

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.cucumber.context.AuthContext
import me.ezra_home.retail_software_solution.cucumber.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.context.InjectionKeys
import me.ezra_home.retail_software_solution.cucumber.fixtures.locations.LocationFixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants
import me.ezra_home.retail_software_solution.locations.business.location_product.dto.LocationProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.location.LocationType
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationInsertDto
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import org.awaitility.Awaitility.await
import java.time.Duration

class CatalogSyncSteps(
  private val requestFactory: AuthenticatedRequestFactory,
  private val locationFixtureBuilder: LocationFixtureBuilder,
  private val injectContext: InjectContext,
  private val authContext: AuthContext
) {

  @Given("a location exists for catalog sync")
  fun createLocationForCatalogSync() {
    val locationId = locationFixtureBuilder.create(
      LocationInsertDto(
        locationType = LocationType.SHOP,
        name = "Catalog Sync Location",
        description = "Location used in cucumber kafka consumer flow"
      )
    )
    injectContext.store(InjectionKeys.CATALOG_SYNC_LOCATION, locationId)
  }

  @Given("I use the catalog sync location context")
  fun useCatalogSyncLocationContext() {
    authContext.currentLocationId = injectContext.get(InjectionKeys.CATALOG_SYNC_LOCATION)
  }

  @Then("the location catalog should contain product {string}")
  fun verifyLocationCatalogContainsProduct(productName: String) {
    val productId = injectContext.get(InjectionKeys.PRODUCT)

    val searchBody = PageRequest(
      previousCursor = null,
      requestedSize = 50,
      parameters = ProductSearchParameters(searchText = productName)
    )

    await().alias("Product '$productName' (id=$productId) not found in location catalog")
      .atMost(Duration.ofMillis(TestConstants.Timeouts.KAFKA_SYNC_MS))
      .pollInterval(Duration.ofMillis(TestConstants.Timeouts.KAFKA_POLL_INTERVAL_MS))
      .until {
        requestFactory.jsonRequest()
          .body(searchBody)
          .post("/secured/location-products/search")
          .jsonPath()
          .getList("contents", LocationProductResponseDto::class.java)
          ?.any { it.productName == productName && it.id == productId } == true
      }
  }
}
