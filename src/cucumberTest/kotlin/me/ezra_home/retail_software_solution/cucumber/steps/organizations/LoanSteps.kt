package me.ezra_home.retail_software_solution.cucumber.steps.organizations

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.restassured.response.Response
import me.ezra_home.retail_software_solution.cucumber.support.ApiClient
import me.ezra_home.retail_software_solution.cucumber.support.AuthenticatedRequestFactory
import me.ezra_home.retail_software_solution.cucumber.support.context.ResponseContext
import me.ezra_home.retail_software_solution.cucumber.support.database.LoanTestDatabase
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactInsertDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import me.ezra_home.retail_software_solution.organizations.business.contact.api.IdentityType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoanSteps(
  private val apiClient: ApiClient,
  private val authenticatedRequestFactory: AuthenticatedRequestFactory,
  private val responseContext: ResponseContext,
  private val loanTestDatabase: LoanTestDatabase
) {

  private val cashCode = "001.001.001"
  private val loansPayableCode = "002.004"

  private var lenderReference = ""
  private var nonLenderReference = ""
  private var loanReference = ""
  private var loanId = ""
  private var repaymentIds = emptyList<String>()
  private var concurrentRoundOutcomes = emptyList<List<Int>>()
  private lateinit var lastSearchResponse: Response

  @Given("the chart of accounts is active with open fiscal periods")
  fun activateChartOfAccounts() {
    check(apiClient.post("/secured/org-features/activate", listOf("CHART_OF_ACCOUNTS")).statusCode == 200) { responseContext.lastError.orEmpty() }
    check(apiClient.post("/secured/fiscal-periods/nudge").statusCode == 200) { responseContext.lastError.orEmpty() }
  }

  @Given("a lender exists")
  fun createLender() {
    lenderReference = createContact(ContactType.LENDER, "First Bank")
  }

  @Given("a contact exists that is not a lender")
  fun createNonLender() {
    nonLenderReference = createContact(ContactType.CUSTOMER, "Plain Customer")
  }

  @Given("I record a loan of {string} from the lender into cash dated today")
  @When("I record a loan of {string} from the lender into cash dated today")
  fun recordLoanToday(amount: String) = recordLoan(lenderReference, cashCode, amount, today())

  @When("I record a loan of {string} from the lender into cash dated {string}")
  fun recordLoanOnDate(amount: String, loanDate: String) = recordLoan(lenderReference, cashCode, amount, LocalDate.parse(loanDate))

  @When("I record a loan of {string} from the lender into account {string} dated today")
  fun recordLoanIntoAccount(amount: String, accountCode: String) = recordLoan(lenderReference, accountCode, amount, today())

  @When("I record a loan of {string} from the contact that is not a lender into cash dated today")
  fun recordLoanFromNonLender(amount: String) = recordLoan(nonLenderReference, cashCode, amount, today())

  @When("I record a loan of {string} from an unknown lender into cash dated today")
  fun recordLoanFromUnknownLender(amount: String) = recordLoan("CT999999", cashCode, amount, today())

  @Given("I repay {string} of the loan from cash")
  @When("I repay {string} of the loan from cash")
  fun repayToday(amount: String) = repay(amount, today())

  @When("I repay {string} of the loan from cash dated {string}")
  fun repayOnDate(amount: String, repaymentDate: String) = repay(amount, LocalDate.parse(repaymentDate))

  @Given("I void the loan")
  @When("I void the loan")
  fun voidLoan() {
    remember(apiClient.post("/secured/loans/void", mapOf("loanId" to loanId, "reason" to "test")))
  }

  @Given("I void the latest repayment of the loan")
  @When("I void the latest repayment of the loan")
  fun voidLatestRepayment() {
    remember(apiClient.post("/secured/loans/repayments/void", mapOf("repaymentId" to repaymentIds.last(), "reason" to "test")))
  }

  @When("{int} rounds of two simultaneous repayments of {string} are submitted against fresh loans of {string}")
  fun raceTwoRepayments(rounds: Int, repaymentAmount: String, loanAmount: String) {
    concurrentRoundOutcomes = (1..rounds).map {
      recordLoan(lenderReference, cashCode, loanAmount, today())
      val freshLoanId = loanId
      submitSimultaneously(
        { post("/secured/loans/repayments", repaymentBody(freshLoanId, repaymentAmount)) },
        { post("/secured/loans/repayments", repaymentBody(freshLoanId, repaymentAmount)) }
      )
    }
  }

  @When("{int} rounds of a repayment of {string} racing a loan void are submitted against fresh loans of {string}")
  fun raceRepaymentAgainstVoid(rounds: Int, repaymentAmount: String, loanAmount: String) {
    concurrentRoundOutcomes = (1..rounds).map {
      recordLoan(lenderReference, cashCode, loanAmount, today())
      val freshLoanId = loanId
      submitSimultaneously(
        { post("/secured/loans/repayments", repaymentBody(freshLoanId, repaymentAmount)) },
        { post("/secured/loans/void", mapOf("loanId" to freshLoanId, "reason" to "race")) }
      )
    }
  }

  @When("I search the loans for the lender with status {string}")
  fun searchByLenderAndStatus(status: String) {
    search(mapOf("lenderContactReferences" to listOf(lenderReference), "statuses" to listOf(status)))
  }

  @When("I search the loans for an unknown lender")
  fun searchByUnknownLender() {
    search(mapOf("lenderContactReferences" to listOf("CT999999")))
  }

  @When("I search the loans without a created range")
  fun searchWithoutRange() {
    apiClient.post("/secured/loans/search", mapOf("parameters" to emptyMap<String, Any>(), "previousCursor" to "", "requestedSize" to 50))
  }

  @When("I search the loans over a range longer than a year")
  fun searchOverLongRange() {
    val parameters = mapOf(
      "createdFrom" to OffsetDateTime.now(ZoneOffset.UTC).minusYears(2).toString(),
      "createdBefore" to OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).toString()
    )
    apiClient.post("/secured/loans/search", mapOf("parameters" to parameters, "previousCursor" to "", "requestedSize" to 50))
  }

  @Then("the loan should be {string} with {string} repaid and {string} outstanding")
  fun verifyLoan(status: String, amountRepaid: String, outstanding: String) {
    val loanJson = apiClient.get("/secured/loans/$loanReference").jsonPath()
    assertEquals(status, loanJson.getString("status"))
    assertAmount(amountRepaid, loanJson.getString("amountRepaid"), "amount repaid")
    assertAmount(outstanding, loanJson.getString("outstanding"), "outstanding")
  }

  @Then("account {string} should have a balance of {string}")
  fun verifyAccountBalance(accountCode: String, expectedBalance: String) {
    assertAmount(expectedBalance, accountBalance(accountCode).toPlainString(), "balance of $accountCode")
  }

  @Then("the lender's payable balance should be {string} and receivable balance {string}")
  fun verifyLenderBalances(payable: String, receivable: String) {
    val contactsJson = apiClient.get("/secured/contacts").jsonPath()
    assertAmount(payable, contactsJson.getString("find { it.referenceNumber == '$lenderReference' }.payableBalance"), "payable")
    assertAmount(receivable, contactsJson.getString("find { it.referenceNumber == '$lenderReference' }.receivableBalance"), "receivable")
  }

  @Then("loans payable should equal the sum of outstanding loans")
  fun verifyLoansPayableMatchesOutstanding() {
    val loansJson = searchAllLoans().jsonPath()
    val outstandingTotal = loansJson.getList<Any>("contents.outstanding").fold(BigDecimal.ZERO) { total, outstanding -> total + BigDecimal(outstanding.toString()) }
    assertEquals(0, outstandingTotal.compareTo(accountBalance(loansPayableCode)), "outstanding $outstandingTotal vs loans payable ${accountBalance(loansPayableCode)}")
  }

  @Then("the error should mention {string}")
  fun verifyErrorMentions(text: String) {
    assertTrue(responseContext.lastError.orEmpty().contains(text), "Expected error to mention $text but was: ${responseContext.lastError}")
  }

  @Then("the error should mention the loan's active repayment")
  fun verifyErrorNamesRepayment() {
    val repaymentReference = apiClient.get("/secured/loans/$loanReference").jsonPath().getString("repayments[0].referenceNumber")
    verifyErrorMentions(repaymentReference)
  }

  @Then("the loan search should return {int} loans")
  fun verifySearchCount(expectedCount: Int) {
    assertEquals(expectedCount, lastSearchResponse.jsonPath().getList<Any>("contents").size)
  }

  @Then("there should be {int} loans")
  fun verifyLoanCount(expectedCount: Int) {
    assertEquals(expectedCount, searchAllLoans().jsonPath().getList<Any>("contents").size)
  }

  @Then("the loan's ledger groups should have a null source location")
  fun verifyNullSourceLocation() {
    assertEquals(1, loanTestDatabase.countLedgerGroups(loanReference, "LN"))
    assertEquals(0, loanTestDatabase.countLedgerGroupsWithLocation(loanReference))
  }

  @Then("no loan ledger groups should exist")
  fun verifyNoLoanLedgerGroups() {
    assertEquals(0, loanTestDatabase.countLedgerGroupsOfType("LN"))
  }

  @Then("exactly one repayment should succeed in every round")
  fun verifyOneRepaymentSucceedsPerRound() = verifyOneSuccessPerRound()

  @Then("exactly one request should succeed in every round")
  fun verifyOneRequestSucceedsPerRound() = verifyOneSuccessPerRound()

  private fun verifyOneSuccessPerRound() {
    concurrentRoundOutcomes.forEachIndexed { roundIndex, statusCodes ->
      assertEquals(1, statusCodes.count { it == 200 }, "round ${roundIndex + 1} statuses: $statusCodes")
      assertTrue(statusCodes.all { it == 200 || it == 400 }, "round ${roundIndex + 1} statuses: $statusCodes")
    }
  }

  private fun createContact(contactType: ContactType, companyName: String): String {
    val response = apiClient.post(
      "/secured/contacts",
      ContactInsertDto(contactTypes = setOf(contactType), identityType = IdentityType.ORGANIZATION, companyName = "$companyName ${UUID.randomUUID()}")
    )
    check(response.statusCode == 200) { "Failed to create contact: ${response.asString()}" }
    return response.jsonPath().getString("referenceNumber")
  }

  private fun recordLoan(lenderContactReference: String, assetAccountCode: String, amount: String, loanDate: LocalDate) {
    remember(
      apiClient.post(
        "/secured/loans",
        mapOf(
          "lenderContactReference" to lenderContactReference,
          "assetAccountCode" to assetAccountCode,
          "amount" to BigDecimal(amount),
          "loanDate" to loanDate.toString()
        )
      )
    )
  }

  private fun repay(amount: String, repaymentDate: LocalDate) {
    remember(apiClient.post("/secured/loans/repayments", repaymentBody(loanId, amount, repaymentDate)))
  }

  private fun repaymentBody(forLoanId: String, amount: String, repaymentDate: LocalDate = today()) = mapOf(
    "loanId" to forLoanId,
    "assetAccountCode" to cashCode,
    "amount" to BigDecimal(amount),
    "repaymentDate" to repaymentDate.toString()
  )

  private fun remember(response: Response) {
    if (response.statusCode != 200) return
    val loanJson = response.jsonPath()
    loanId = loanJson.getString("id")
    loanReference = loanJson.getString("referenceNumber")
    repaymentIds = loanJson.getList<String>("repayments.findAll { it.void == null }.id")
  }

  private fun search(parameters: Map<String, Any>) {
    val boundedParameters = parameters + searchRange()
    lastSearchResponse = apiClient.post("/secured/loans/search", mapOf("parameters" to boundedParameters, "previousCursor" to "", "requestedSize" to 50))
  }

  private fun searchAllLoans(): Response = apiClient.post(
    "/secured/loans/search",
    mapOf("parameters" to searchRange(), "previousCursor" to "", "requestedSize" to 200)
  )

  private fun searchRange() = mapOf(
    "createdFrom" to OffsetDateTime.now(ZoneOffset.UTC).minusDays(1).toString(),
    "createdBefore" to OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).toString()
  )

  private fun accountBalance(accountCode: String): BigDecimal =
    BigDecimal(apiClient.get("/secured/accounts").jsonPath().getString("**.find { it.code == '$accountCode' }.currentBalance"))

  private fun post(url: String, body: Any): Int = authenticatedRequestFactory.jsonRequest().body(body).post(url).statusCode

  private fun submitSimultaneously(first: () -> Int, second: () -> Int): List<Int> {
    val barrier = CyclicBarrier(2)
    val executor = Executors.newFixedThreadPool(2)
    try {
      val futures = listOf(first, second).map { request ->
        executor.submit<Int> {
          barrier.await()
          request()
        }
      }
      return futures.map { it.get(60, TimeUnit.SECONDS) }
    } finally {
      executor.shutdownNow()
    }
  }

  private fun assertAmount(expected: String, actual: String?, label: String) {
    assertEquals(0, BigDecimal(expected).compareTo(BigDecimal(requireNotNull(actual) { "$label missing" })), "$label expected $expected but was $actual")
  }

  private fun today(): LocalDate = LocalDate.now(ZoneOffset.UTC)
}
