package me.ezra_home.retail_software_solution.cucumber.support

import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import me.ezra_home.retail_software_solution.configuration.security.RtsHeaders
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class AuthenticatedRequestFactory(
    private val authContext: AuthContext,
    private val environment: Environment
) {

  fun jsonRequest(): RequestSpecification {
    val port = environment.getProperty("local.server.port", Int::class.java)
      ?: error("Property 'local.server.port' must be set for tests")
    val request = given()
      .baseUri("http://localhost:$port")
      .contentType(ContentType.JSON)

    authContext.authToken?.let { request.header("Authorization", "Bearer $it") }
    authContext.currentOrganizationId?.let { request.header(RtsHeaders.ORGANIZATION_ID_HEADER, it.toString()) }
    authContext.currentLocationId?.let { request.header(RtsHeaders.LOCATION_ID_HEADER, it.toString()) }
    return request
  }
}
