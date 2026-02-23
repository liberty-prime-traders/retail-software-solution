package me.ezra_home.retail_software_solution.cucumber.config

import io.restassured.response.Response
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TestContext {

  var baseUrl: String = ""
  var authToken: String? = null
  var currentUserId: UUID? = null
  var currentOrganizationId: UUID? = null
  var currentLocationId: UUID? = null
  var lastResponse: Response? = null
  val testData: MutableMap<String, Any> = mutableMapOf()

  fun reset() {
    authToken = null
    currentUserId = null
    currentOrganizationId = null
    currentLocationId = null
    lastResponse = null
    testData.clear()
  }

  fun store(key: String, value: Any) {
    testData[key] = value
  }

  fun <T> get(key: String, type: Class<T>): T? {
    return testData[key]?.let { type.cast(it) }
  }
}
