package me.ezra_home.retail_software_solution.cucumber.steps.rest

import io.cucumber.java.en.When
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import me.ezra_home.retail_software_solution.cucumber.context.InjectContext

class RequestSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext
) {

  @When("I send a GET request to {string}")
  fun sendGet(endpoint: String) {
    apiClient.get(injectContext.inject(endpoint))
  }

  @When("I send a POST request to {string}")
  fun sendPost(endpoint: String) {
    apiClient.post(injectContext.inject(endpoint))
  }

  @When("I send a POST request to {string} with body:")
  fun sendPostWithBody(endpoint: String, body: String) {
    apiClient.post(injectContext.inject(endpoint), injectContext.inject(body))
  }

  @When("I send a PUT request to {string} with body:")
  fun sendPutWithBody(endpoint: String, body: String) {
    apiClient.put(injectContext.inject(endpoint), injectContext.inject(body))
  }

  @When("I send a DELETE request to {string}")
  fun sendDelete(endpoint: String) {
    apiClient.delete(injectContext.inject(endpoint))
  }
}
