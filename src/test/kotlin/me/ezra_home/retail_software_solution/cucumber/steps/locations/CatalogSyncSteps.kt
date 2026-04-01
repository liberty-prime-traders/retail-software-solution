package me.ezra_home.retail_software_solution.cucumber.steps.locations

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.cucumber.context.locations.LocationContext
import me.ezra_home.retail_software_solution.cucumber.context.organizations.ProductContext
import me.ezra_home.retail_software_solution.cucumber.fixtures.locations.LocationFixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.context.AuthContext
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.TestConstants
import me.ezra_home.retail_software_solution.locations.business.location_product.dto.LocationProductResponseDto
import me.ezra_home.retail_software_solution.organizations.business.location.dto.LocationInsertDto
import me.ezra_home.retail_software_solution.util.enums.LocationType
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import java.util.UUID
import kotlin.test.fail

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
    injectContext.store(LocationContext.CATALOG_SYNC_LOCATION_ID, locationId)
  }

  @Given("I use the catalog sync location context")
  fun useCatalogSyncLocationContext() {
    authContext.currentLocationId = UUID.fromString(injectContext.get(LocationContext.CATALOG_SYNC_LOCATION_ID))
  }

  @Then("the location catalog should contain product {string}")
  fun verifyLocationCatalogContainsProduct(productName: String) {
    val productId = injectContext.get(ProductContext.ID)

    val searchBody = PageRequest(
      previousCursor = null,
      requestedSize = 50,
      parameters = ProductSearchParameters(searchText = productName)
    )

    val timeoutAt = System.currentTimeMillis() + TestConstants.Timeouts.KAFKA_SYNC_MS
    while (System.currentTimeMillis() < timeoutAt) {
      val found = requestFactory.jsonRequest()
        .body(searchBody)
        .post("/secured/location-products/search")
        .jsonPath()
        .getList("contents", LocationProductResponseDto::class.java)
        ?.any { it.productName == productName && it.id?.toString() == productId } == true

      if (found) return
      Thread.sleep(TestConstants.Timeouts.KAFKA_POLL_INTERVAL_MS)
    }

    fail("Product '$productName' (id=$productId) was not found in location catalog within timeout")
  }
}
