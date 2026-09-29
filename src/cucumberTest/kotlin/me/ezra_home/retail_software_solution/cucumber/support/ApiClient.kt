package me.ezra_home.retail_software_solution.cucumber.support

import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import me.ezra_home.retail_software_solution.cucumber.support.context.ResponseContext
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component

@Component
class ApiClient(
  private val requestFactory: AuthenticatedRequestFactory,
  private val responseContext: ResponseContext
) {

  fun get(url: String, queryParams: Map<String, Any> = emptyMap()): Response =
    requestFactory.jsonRequest().applyQuery(queryParams).get(url).also { responseContext.lastResponse = it }

  fun post(url: String, body: Any? = null, queryParams: Map<String, Any> = emptyMap()): Response {
    val spec = requestFactory.jsonRequest().applyQuery(queryParams)
    body?.let { spec.body(it) }
    return spec.post(url).also { responseContext.lastResponse = it }
  }

  fun put(url: String, body: Any? = null, queryParams: Map<String, Any> = emptyMap()): Response {
    val spec = requestFactory.jsonRequest().applyQuery(queryParams)
    body?.let { spec.body(it) }
    return spec.put(url).also { responseContext.lastResponse = it }
  }

  fun patch(url: String, body: Any? = null, queryParams: Map<String, Any> = emptyMap()): Response {
    val spec = requestFactory.jsonRequest().applyQuery(queryParams)
    body?.let { spec.body(it) }
    return spec.patch(url).also { responseContext.lastResponse = it }
  }

  fun delete(url: String, queryParams: Map<String, Any> = emptyMap()): Response =
    requestFactory.jsonRequest().applyQuery(queryParams).delete(url).also { responseContext.lastResponse = it }

  fun exchange(
    method: HttpMethod,
    url: String,
    body: Any? = null,
    queryParams: Map<String, Any> = emptyMap(),
  ): Response = when (method) {
    HttpMethod.GET -> get(url, queryParams)
    HttpMethod.POST -> post(url, body, queryParams)
    HttpMethod.PUT -> put(url, body, queryParams)
    HttpMethod.PATCH -> patch(url, body, queryParams)
    HttpMethod.DELETE -> delete(url, queryParams)
    else -> error("Unsupported HTTP method for cucumber steps: $method")
  }

  private fun RequestSpecification.applyQuery(queryParams: Map<String, Any>): RequestSpecification =
    if (queryParams.isEmpty()) this else queryParams(queryParams)
}
