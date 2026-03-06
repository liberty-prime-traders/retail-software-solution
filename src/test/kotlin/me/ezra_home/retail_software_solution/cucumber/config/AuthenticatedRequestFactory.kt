package me.ezra_home.retail_software_solution.cucumber.config

import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import me.ezra_home.retail_software_solution.configuration.security.RtsHeaders
import org.springframework.stereotype.Component

@Component
class AuthenticatedRequestFactory(
  private val context: TestContext
) {

  fun jsonRequest(): RequestSpecification {
    val request = given()
      .baseUri(context.baseUrl)
      .contentType(ContentType.JSON)

    context.authToken?.let { request.header("Authorization", "Bearer $it") }
    context.currentOrganizationId?.let { request.header(RtsHeaders.ORGANIZATION_ID_HEADER, it.toString()) }
    context.currentLocationId?.let { request.header(RtsHeaders.LOCATION_ID_HEADER, it.toString()) }
    return request
  }
}
