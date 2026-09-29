package me.ezra_home.retail_software_solution.cucumber.support.context

import io.restassured.response.Response
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ResponseContext {

  var lastResponse: Response? = null

  val lastError: String?
    get() = lastResponse?.takeIf { it.statusCode >= 400 }?.asString()

  fun reset() {
    lastResponse = null
  }

  fun idFromResponse(): UUID {
    return idFromResponse(lastResponse ?: throw IllegalStateException("No response available to extract ID from"))
  }

  companion object {
    fun idFromResponse(response: Response): UUID {
      val id = checkNotNull(response.jsonPath()?.getString("id")) { "Response missing 'id' field" }
      return UUID.fromString(id)
    }
  }

}
