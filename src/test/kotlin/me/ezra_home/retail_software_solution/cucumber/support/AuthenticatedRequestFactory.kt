package me.ezra_home.retail_software_solution.cucumber.support

import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import me.ezra_home.retail_software_solution.configuration.security.RtsHeaders
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AuthenticatedRequestFactory(
  @param:Value("\${local.server.port}")
  private val port: Int
) {

  fun jsonRequest(): RequestSpecification {
    val request = given()
      .baseUri("http://localhost:$port")
      .contentType(ContentType.JSON)

    AuthContext.authToken?.let { request.header("Authorization", "Bearer $it") }
    AuthContext.currentOrganizationId?.let { request.header(RtsHeaders.ORGANIZATION_ID_HEADER, it.toString()) }
    AuthContext.currentLocationId?.let { request.header(RtsHeaders.LOCATION_ID_HEADER, it.toString()) }
    return request
  }
}
