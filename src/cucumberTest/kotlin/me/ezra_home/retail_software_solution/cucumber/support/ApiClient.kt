package me.ezra_home.retail_software_solution.cucumber.support

import io.restassured.response.Response
import me.ezra_home.retail_software_solution.cucumber.support.context.ResponseContext
import org.springframework.stereotype.Component

@Component
class ApiClient(
  private val requestFactory: AuthenticatedRequestFactory,
  private val responseContext: ResponseContext
) {

  fun get(url: String): Response =
    requestFactory.jsonRequest().get(url).also { responseContext.lastResponse = it }

  fun post(url: String, body: Any? = null): Response {
    val spec = requestFactory.jsonRequest()
    body?.let { spec.body(it) }
    return spec.post(url).also { responseContext.lastResponse = it }
  }

  fun put(url: String, body: Any? = null): Response {
    val spec = requestFactory.jsonRequest()
    body?.let { spec.body(it) }
    return spec.put(url).also { responseContext.lastResponse = it }
  }

  fun delete(url: String): Response =
    requestFactory.jsonRequest().delete(url).also { responseContext.lastResponse = it }
}
