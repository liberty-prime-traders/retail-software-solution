package me.ezra_home.retail_software_solution.cucumber.support

import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import me.ezra_home.retail_software_solution.configuration.security.RtsHeaders
import me.ezra_home.retail_software_solution.cucumber.support.context.AuthContext
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.PersistentKey
import me.ezra_home.retail_software_solution.cucumber.support.context.TransientKey
import org.springframework.core.env.Environment
import org.springframework.core.env.getRequiredProperty
import org.springframework.stereotype.Component

@Component
class AuthenticatedRequestFactory(
  private val environment: Environment,
  private val authContext: AuthContext,
  private val injectContext: InjectContext
) {

  fun jsonRequest(): RequestSpecification {
    val port = environment.getRequiredProperty<Int>("local.server.port")
    val request = given()
      .baseUri("http://localhost:$port")
      .contentType(ContentType.JSON)

    authContext.authToken?.let { request.header(TestConstants.Tokens.TOKEN_HEADER, it) }
    injectContext.find(PersistentKey.ORGANIZATION)?.let { request.header(RtsHeaders.ORGANIZATION_ID_HEADER, it.toString()) }
    injectContext.find(TransientKey.LOCATION)?.let { request.header(RtsHeaders.LOCATION_ID_HEADER, it.toString()) }
    return request
  }
}
