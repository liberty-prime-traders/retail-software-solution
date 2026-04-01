package me.ezra_home.retail_software_solution.cucumber.fixtures

import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.getResponseId
import kotlin.test.assertEquals

abstract class FixtureBuilder<INSERT_DTO>(
  protected val requestFactory: AuthenticatedRequestFactory,
  protected val injectContext: InjectContext
) {

  protected abstract val endpoint: String
  protected abstract fun defaultDto(): INSERT_DTO

  protected open fun fromRow(row: Map<String, String>): INSERT_DTO = defaultDto()

  fun create(dto: INSERT_DTO = defaultDto()): String {
    val response = requestFactory.jsonRequest().body(dto).post(endpoint)
    assertEquals(
      200,
      response.statusCode,
      "Failed to create fixture at $endpoint. Response: ${response.asString()}"
    )
    return response.getResponseId()
  }

  fun createFromRow(row: Map<String, String>): String = create(fromRow(row))
}
