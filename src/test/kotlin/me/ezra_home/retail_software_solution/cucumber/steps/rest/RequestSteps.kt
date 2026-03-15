package me.ezra_home.retail_software_solution.cucumber.steps.rest

import io.cucumber.java.en.When
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.support.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.ResponseContext

class RequestSteps(
  private val responseContext: ResponseContext,
  private val requestFactory: AuthenticatedRequestFactory,
  private val injectContext: InjectContext
) {

  @When("I send a GET request to {string}")
  fun sendGet(endpoint: String) {
    responseContext.lastResponse = requestFactory.jsonRequest().get(injectContext.inject(endpoint))
  }

  @When("I send a POST request to {string}")
  fun sendPost(endpoint: String) {
    responseContext.lastResponse = requestFactory.jsonRequest().post(injectContext.inject(endpoint))
  }

  @When("I send a POST request to {string} with body:")
  fun sendPostWithBody(endpoint: String, body: String) {
    responseContext.lastResponse = requestFactory.jsonRequest()
      .body(injectContext.inject(body))
      .post(injectContext.inject(endpoint))
  }

  @When("I send a PUT request to {string} with body:")
  fun sendPutWithBody(endpoint: String, body: String) {
    responseContext.lastResponse = requestFactory.jsonRequest()
      .body(injectContext.inject(body))
      .put(injectContext.inject(endpoint))
  }

  @When("I send a DELETE request to {string}")
  fun sendDelete(endpoint: String) {
    responseContext.lastResponse = requestFactory.jsonRequest().delete(injectContext.inject(endpoint))
  }
}
