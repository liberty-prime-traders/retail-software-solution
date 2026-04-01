package me.ezra_home.retail_software_solution.cucumber.context

import io.restassured.response.Response
import org.springframework.stereotype.Component

@Component
class ResponseContext {

  var lastResponse: Response? = null

  val lastError: String?
    get() = lastResponse?.takeIf { it.statusCode >= 400 }?.asString()

  fun reset() {
    lastResponse = null
  }
}
