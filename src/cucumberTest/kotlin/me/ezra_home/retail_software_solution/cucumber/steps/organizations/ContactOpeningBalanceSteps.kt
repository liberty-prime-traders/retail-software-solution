package me.ezra_home.retail_software_solution.cucumber.steps.organizations

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import me.ezra_home.retail_software_solution.cucumber.fixtures.organizations.ContactFixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.TransientKey
import java.math.BigDecimal
import kotlin.test.assertEquals

class ContactOpeningBalanceSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext,
  private val contactFixtureBuilder: ContactFixtureBuilder
) {

  @Given("a contact exists")
  fun createContact() {
    injectContext.store(TransientKey.CONTACT, contactFixtureBuilder.create())
  }

  @When("I set the contact's opening balances to payable {string} and receivable {string}")
  fun setOpeningBalances(payable: String, receivable: String) {
    apiClient.post(
      "/secured/contact-opening-balances/${injectContext.get(TransientKey.CONTACT)}",
      mapOf("payable" to BigDecimal(payable), "receivable" to BigDecimal(receivable))
    )
  }

  @Then("the contact's listed net opening balance should be {string} with effect {string}")
  fun verifyListedOpeningBalance(expectedNetOpeningBalance: String, expectedEffect: String) {
    val contactId = injectContext.get(TransientKey.CONTACT)
    val listResponse = apiClient.get("/secured/contacts")
    val contactJsonPath = "find { it.id == '$contactId' }"
    val actualNetOpeningBalance = BigDecimal(listResponse.jsonPath().getString("$contactJsonPath.netOpeningBalance"))
    assertEquals(0, BigDecimal(expectedNetOpeningBalance).compareTo(actualNetOpeningBalance), "net opening balance was $actualNetOpeningBalance")
    assertEquals(expectedEffect, listResponse.jsonPath().getString("$contactJsonPath.netOpeningBalanceEffect"))
  }
}
