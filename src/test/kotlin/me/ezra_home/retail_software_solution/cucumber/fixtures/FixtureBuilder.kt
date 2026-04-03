package me.ezra_home.retail_software_solution.cucumber.fixtures

import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.ResponseContext
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import java.util.UUID
import kotlin.test.assertEquals

abstract class FixtureBuilder<INSERT_DTO>(
  protected val injectContext: InjectContext,
  protected val apiClient: ApiClient,
) {

  protected abstract val endpoint: String
  protected abstract fun defaultDto(): INSERT_DTO

  protected open fun fromRow(row: Map<String, String>): INSERT_DTO = defaultDto()

  fun create(dto: INSERT_DTO? = null): UUID {
    val resolvedDto = try {
      dto ?: defaultDto()
    } catch (e: IllegalStateException) {
      throw IllegalStateException("${this::class.simpleName} prerequisite missing: ${e.message}", e)
    }
    val response = apiClient.post(endpoint, resolvedDto)
    assertEquals(
      200,
      response.statusCode,
      "Failed to create fixture at $endpoint. Response: ${response.asString()}"
    )
    return ResponseContext.idFromResponse(response)
  }

  fun createFromRow(row: Map<String, String>) = create(fromRow(row))
}
