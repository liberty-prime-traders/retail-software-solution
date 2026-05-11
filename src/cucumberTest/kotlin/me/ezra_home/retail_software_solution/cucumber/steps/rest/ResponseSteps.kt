package me.ezra_home.retail_software_solution.cucumber.steps.rest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import io.restassured.response.Response
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.ResponseContext
import me.ezra_home.retail_software_solution.cucumber.support.rest.JsonSubsetMatcher
import me.ezra_home.retail_software_solution.cucumber.support.rest.OrderOption
import me.ezra_home.retail_software_solution.cucumber.support.rest.RestVerificationOption
import me.ezra_home.retail_software_solution.cucumber.support.rest.SubsetOptions
import org.hamcrest.Matchers.hasSize
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ResponseSteps(
  private val responseContext: ResponseContext,
  private val injectContext: InjectContext
) {
  private val objectMapper = jacksonObjectMapper()

  @Then("the response status should be {int}")
  fun verifyStatus(expectedStatus: Int) {
    val actualStatus = responseContext.lastResponse?.statusCode
    assertEquals(
      expectedStatus, actualStatus,
      "Expected status $expectedStatus but got $actualStatus. Response: ${responseContext.lastResponse?.asString()}"
    )
  }

  @Then("the response should contain field {string}")
  fun verifyFieldExists(fieldName: String) {
    assertNotNull(responseContext.lastResponse?.jsonPath()?.get<Any>(fieldName))
  }

  @Then("the response field {string} should be {string}")
  fun verifyFieldValue(fieldName: String, expectedValue: String) {
    assertEquals(expectedValue, responseContext.lastResponse?.jsonPath()?.getString(fieldName))
  }

  @Then("the response should match json:")
  fun verifyResponseMatchesJson(expectedJson: String) {
    val expected = objectMapper.readTree(injectContext.inject(expectedJson))
    val actual = responseBodyAsJson()
    JsonSubsetMatcher.assertJsonSubset(expected, actual)
  }

  @Then("the response should contain:")
  fun verifyFields(table: DataTable) {
    table.asMaps().forEach { row ->
      val field = row["field"]!!
      val expected = injectContext.inject(row["value"]!!)
      assertEquals(
        expected,
        responseContext.lastResponse?.jsonPath()?.getString(field),
        "Response field '$field' mismatch"
      )
    }
  }

  @Then("the response should contain {int} items")
  fun verifyListSize(size: Int) {
    responseContext.lastResponse?.then()?.body("$", hasSize<Any>(size))
  }

  @Then("the response should be an empty list")
  fun verifyEmptyList() {
    responseContext.lastResponse?.then()?.body("$", hasSize<Any>(0))
  }

  @Then("the response error message should be {string}")
  fun verifyErrorMessage(expectedMessage: String) {
    assertNotNull(responseContext.lastError, "Expected an error response but status was ${responseContext.lastResponse?.statusCode}")
    val actualMessage = responseContext.lastResponse?.jsonPath()?.getString("message")
    val injectedExpectedMessage = injectContext.inject(expectedMessage)
    assertEquals(
      injectedExpectedMessage,
      actualMessage,
      "Expected error message '$injectedExpectedMessage' but got '$actualMessage'"
    )
  }

  @Then("the response error field {string} should be {string}")
  fun verifyErrorField(fieldName: String, expectedValue: String) {
    assertNotNull(responseContext.lastError, "Expected an error response but status was ${responseContext.lastResponse?.statusCode}")
    assertEquals(expectedValue, responseContext.lastResponse?.jsonPath()?.getString(fieldName))
  }

  @Then("response returns with status {int}")
  fun checkResponseStatus(expected: Int) {
    assertStatus(expected, null)
  }

  @Then("response returns with status {int} with message: {}")
  fun checkResponseStatusWithMessage(expected: Int, message: String) {
    assertStatus(expected, injectContext.inject(message))
  }

  @Then("response returns with status {int} without message: {}")
  fun checkResponseStatusWithoutMessage(expected: Int) {
    assertStatus(expected, null)
  }

  @Then("response list size is {int}")
  fun checkResponseListSize(size: Int) {
    val actual = responseBodyAsJson()
    assertTrue(actual is ArrayNode, "Response is not a collection: $actual")
    assertEquals(size, actual.size(), "Response size is not $size")
  }

  @Then("response contains{restVerificationOption}details:")
  fun verifyDetails(option: RestVerificationOption, payload: String) {
    verifyDetails(option, payload, exactLists = false, inOrder = false)
  }

  @Then("response contains{restVerificationOption}details with exact lists{orderOption}:")
  fun verifyDetailsExactLists(option: RestVerificationOption, order: OrderOption, payload: String) {
    verifyDetails(option, payload, exactLists = true, inOrder = order.inOrder())
  }

  private fun verifyDetails(option: RestVerificationOption, payload: String, exactLists: Boolean, inOrder: Boolean) {
    val expected = objectMapper.readTree(injectContext.inject(payload))
    val actual = responseBodyAsJson()
    val options = SubsetOptions(
      exactLists = exactLists,
      inOrder = inOrder,
    )

    val matched = if (actual is ArrayNode && !expected.isArray) {
      anyArrayItemMatches(actual, expected, options)
    } else {
      JsonSubsetMatcher.isJsonSubset(expected, actual, options)
    }

    when (option) {
      RestVerificationOption.DEFAULT, RestVerificationOption.ITEM_WITH -> assertTrue(
        matched,
        "Detail did not match response.\n\nDetail: $expected\n\nResponse: $actual",
      )
      RestVerificationOption.NO_ITEM_WITH -> assertTrue(
        !matched,
        "Detail matched response when it should not.\n\nDetail: $expected\n\nResponse: $actual",
      )
    }
  }

  private fun anyArrayItemMatches(
    array: ArrayNode,
    expected: JsonNode,
    options: SubsetOptions,
  ): Boolean = (0 until array.size()).any { JsonSubsetMatcher.isJsonSubset(expected, array[it], options) }

  private fun responseBodyAsJson(): JsonNode {
    val body = checkNotNull(responseContext.lastResponse?.asString()) {
      "Expected a response body but no response was captured"
    }
    return objectMapper.readTree(body)
  }

  private fun assertStatus(expectedCode: Int, message: String?) {
    val response: Response = checkNotNull(responseContext.lastResponse) { "no response" }
    assertEquals(
      expectedCode,
      response.statusCode,
      "Status was ${response.statusCode} not $expectedCode. Body: ${response.asString()}",
    )
    if (!message.isNullOrBlank()) assertMessageMatches(message, response)
  }

  private fun assertMessageMatches(expected: String, response: Response) {
    val responseMessage = response.jsonPath().getString("message")?.trim()
      ?: response.asString()?.trim().orEmpty()
    val escaped = expected.replace("{", "\\{").replace("\"", "")
    val matchesRegex = runCatching { Regex(escaped).containsMatchIn(responseMessage) }.getOrDefault(false)
    assertTrue(
      matchesRegex || responseMessage == expected,
      "Unexpected error message: $responseMessage should be: $expected",
    )
  }
}
