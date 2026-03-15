package me.ezra_home.retail_software_solution.cucumber.fixtures

import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

abstract class FixtureBuilder<INSERT_DTO>(protected val requestFactory: AuthenticatedRequestFactory) {

  protected abstract val endpoint: String
  protected abstract fun defaultDto(): INSERT_DTO

  fun create(dto: INSERT_DTO = defaultDto()): UUID {
    val response = requestFactory.jsonRequest().body(dto).post(endpoint)
    assertEquals(200, response.statusCode, "Failed to create fixture at $endpoint. Response: ${response.asString()}")
    val id = response.jsonPath().getString("id")
    assertNotNull(id, "Fixture response missing id")
    return UUID.fromString(id)
  }

  fun delete(id: UUID) {
    val response = requestFactory.jsonRequest().delete("$endpoint/$id")
    assertEquals(
      204, response.statusCode,
      "Failed to delete fixture at $endpoint/$id. Response: ${response.asString()}"
    )
  }
}
