package me.ezra_home.retail_software_solution.locations.business.reports.payments

import me.ezra_home.retail_software_solution.locations.business.reports.payments.api.PaymentSearchParameters
import me.ezra_home.retail_software_solution.locations.business.reports.payments.api.PaymentSearchService
import me.ezra_home.retail_software_solution.locations.business.reports.payments.api.PaymentType
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodDataFetcher
import me.ezra_home.retail_software_solution.util.business.mappers.NameResolution
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import me.ezra_home.retail_software_solution.util.ui_models.BalanceEffect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class PaymentSearchServiceTest {

  private val cashMethodId = UUID.randomUUID()
  private val bankMethodId = UUID.randomUUID()
  private val customerId = UUID.randomUUID()
  private val supplierId = UUID.randomUUID()

  private val searchCalls = mutableListOf<Pair<List<PaymentSource>, PaymentSearchParameters>>()
  private val summaryCalls = mutableListOf<Pair<PaymentSource, PaymentSearchParameters>>()

  private var searchRows = listOf<PaymentRow>()
  private var summaryRows = mapOf<PaymentSource, List<PaymentMethodTotals>>()

  // Typed overrides fail to compile when a fetcher signature changes, unlike answers routed by method name.
  private inner class FakePaymentSearchFetcher : PaymentSearchFetcher(mock(PaymentSearchExecutor::class.java)) {

    override fun search(
      paymentSources: List<PaymentSource>,
      paymentSearchParameters: PaymentSearchParameters,
      keysetSearchCursor: KeysetSearchCursor?,
      requestedSize: Int
    ): List<PaymentRow> {
      searchCalls.add(paymentSources to paymentSearchParameters)
      return searchRows.filter { paymentRow -> paymentSources.any { it.type == paymentRow.type } }
    }

    override fun summarize(
      paymentSource: PaymentSource,
      paymentSearchParameters: PaymentSearchParameters
    ): List<PaymentMethodTotals> {
      summaryCalls.add(paymentSource to paymentSearchParameters)
      return summaryRows[paymentSource].orEmpty()
    }
  }

  private val paymentMethodDataFetcher = mock(PaymentMethodDataFetcher::class.java).also {
    `when`(it.getNamesById()).thenReturn(mapOf(cashMethodId to "CASH", bankMethodId to "BANK"))
  }

  private val nameResolution = mock(NameResolution::class.java).also {
    `when`(it.organizationContacts(anyCollection<UUID>()))
      .thenReturn(mapOf(customerId to "Jane Customer", supplierId to "Acme Supplier"))
  }

  private val paymentSearchService =
    PaymentSearchService(FakePaymentSearchFetcher(), paymentMethodDataFetcher, nameResolution)

  private fun totals(methodId: UUID, active: String, voided: String, activeCount: Long, voidedCount: Long) =
    PaymentMethodTotals(methodId, BigDecimal(active), BigDecimal(voided), activeCount, voidedCount)

  private fun paymentRow(
    type: PaymentType,
    referenceNumber: String,
    recordedOn: OffsetDateTime,
    voidReason: String? = null
  ) = PaymentRow(
    id = UUID.randomUUID(),
    type = type,
    parentReferenceNumber = referenceNumber,
    contactId = if (type == PaymentType.SALE) customerId else supplierId,
    paymentMethodId = if (type == PaymentType.SALE) cashMethodId else bankMethodId,
    amount = BigDecimal("100.00"),
    paymentDate = recordedOn,
    recordedOn = recordedOn,
    voidReason = voidReason
  )

  private fun at(hour: Int): OffsetDateTime = OffsetDateTime.of(2026, 3, 10, hour, 0, 0, 0, ZoneOffset.UTC)

  private fun searchParameters(
    types: Set<PaymentType> = PaymentType.entries.toSet(),
    contactIds: List<UUID> = emptyList(),
    referenceNumbersByType: Map<PaymentType, List<String>> = emptyMap()
  ) = PaymentSearchParameters(
    types = types,
    recordedFrom = at(0),
    recordedBefore = at(23),
    contactIds = contactIds,
    referenceNumbersByType = referenceNumbersByType
  )

  private fun pageRequest(parameters: PaymentSearchParameters, size: Int = 10) =
    PageRequest(previousCursor = "", requestedSize = size, parameters = parameters)

  private fun summarizedSources() = summaryCalls.map { it.first }

  @Test
  fun `sales only summary lists only the sale type and never queries supplier payments`() {
    summaryRows = mapOf(PaymentSource.SALE to listOf(totals(cashMethodId, "300.00", "0", 3, 0)))

    val summary = paymentSearchService.summarize(searchParameters(types = setOf(PaymentType.SALE)))

    val sales = summary.summaries.single()
    assertEquals(PaymentType.SALE, sales.type)
    assertEquals(BalanceEffect.ADDS, sales.balanceEffect)
    assertEquals(BigDecimal("300.00"), sales.activeTotal)
    assertEquals(listOf(PaymentSource.SALE), summarizedSources())
  }

  @Test
  fun `purchases only summary lists only the purchase type and never queries sale payments`() {
    summaryRows = mapOf(PaymentSource.PURCHASE to listOf(totals(bankMethodId, "50.00", "0", 1, 0)))

    val summary = paymentSearchService.summarize(searchParameters(types = setOf(PaymentType.PURCHASE)))

    val purchases = summary.summaries.single()
    assertEquals(PaymentType.PURCHASE, purchases.type)
    assertEquals(BalanceEffect.SUBTRACTS, purchases.balanceEffect)
    assertEquals(BigDecimal("50.00"), purchases.activeTotal)
    assertEquals(listOf(PaymentSource.PURCHASE), summarizedSources())
  }

  @Test
  fun `no checked type yields no summaries and no queries`() {
    val summary = paymentSearchService.summarize(searchParameters(types = emptySet()))

    assertTrue(summary.summaries.isEmpty())
    assertTrue(summaryCalls.isEmpty())
  }

  @Test
  fun `both types keep their totals separate and each total equals the sum of its methods`() {
    summaryRows = mapOf(
      PaymentSource.SALE to listOf(
        totals(cashMethodId, "200.00", "0", 2, 0),
        totals(bankMethodId, "100.00", "20.00", 1, 1)
      ),
      PaymentSource.PURCHASE to listOf(totals(bankMethodId, "50.00", "0", 1, 0))
    )

    val summary = paymentSearchService.summarize(searchParameters())

    val (sales, purchases) = summary.summaries
    assertEquals(listOf(PaymentType.SALE, PaymentType.PURCHASE), summary.summaries.map { it.type })
    assertEquals(BigDecimal("300.00"), sales.activeTotal)
    assertEquals(BigDecimal("20.00"), sales.voidedTotal)
    assertEquals(3L, sales.activeCount)
    assertEquals(1L, sales.voidedCount)
    assertEquals(sales.activeTotal, sales.methods.sumOf { it.activeTotal })
    assertEquals(listOf("BANK", "CASH"), sales.methods.map { it.paymentMethodName })
    assertEquals(BigDecimal("50.00"), purchases.activeTotal)
  }

  @Test
  fun `a method with only voided payments still appears in each type`() {
    summaryRows = mapOf(
      PaymentSource.SALE to listOf(totals(cashMethodId, "0", "75.00", 0, 1)),
      PaymentSource.PURCHASE to listOf(totals(bankMethodId, "0", "6000.00", 0, 1))
    )

    val (sales, purchases) = paymentSearchService.summarize(searchParameters()).summaries

    assertEquals(BigDecimal("75.00"), sales.methods.single().voidedTotal)
    assertEquals(BigDecimal("6000.00"), purchases.methods.single().voidedTotal)
    assertEquals(1L, purchases.voidedCount)
  }

  @Test
  fun `a reference filter on one type drops the types that have none`() {
    paymentSearchService.summarize(
      searchParameters(referenceNumbersByType = mapOf(PaymentType.SALE to listOf("SL000001")))
    )
    assertEquals(listOf(PaymentSource.SALE), summarizedSources())

    paymentSearchService.summarize(
      searchParameters(referenceNumbersByType = mapOf(PaymentType.PURCHASE to listOf("PU000001")))
    )
    assertEquals(listOf(PaymentSource.SALE, PaymentSource.PURCHASE), summarizedSources())
  }

  @Test
  fun `reference filters on both types keep both types each with its own references`() {
    val referenceNumbersByType = mapOf(
      PaymentType.SALE to listOf("SL000001"),
      PaymentType.PURCHASE to listOf("PU000001")
    )

    paymentSearchService.summarize(searchParameters(referenceNumbersByType = referenceNumbersByType))

    assertEquals(listOf(PaymentSource.SALE, PaymentSource.PURCHASE), summarizedSources())
    assertEquals(referenceNumbersByType, summaryCalls.first().second.referenceNumbersByType)
  }

  @Test
  fun `blank reference numbers are dropped and an all-blank type does not filter the others out`() {
    paymentSearchService.summarize(
      searchParameters(referenceNumbersByType = mapOf(PaymentType.SALE to listOf("  ", "")))
    )

    assertEquals(listOf(PaymentSource.SALE, PaymentSource.PURCHASE), summarizedSources())
  }

  @Test
  fun `a reference filter does not revive an unchecked type`() {
    paymentSearchService.summarize(
      searchParameters(
        types = setOf(PaymentType.SALE),
        referenceNumbersByType = mapOf(PaymentType.PURCHASE to listOf("PU000001"))
      )
    )

    assertTrue(summaryCalls.isEmpty())
  }

  @Test
  fun `list maps rows with own reference numbers, balance effects and contact names`() {
    searchRows = listOf(
      paymentRow(PaymentType.SALE, "SP000002", at(10)),
      paymentRow(PaymentType.PURCHASE, "SUP000001", at(9))
    )

    val page = paymentSearchService.search(pageRequest(searchParameters()))

    assertEquals(listOf("SP000002", "SUP000001"), page.contents.map { it.parentReferenceNumber })
    assertEquals(listOf(PaymentType.SALE, PaymentType.PURCHASE), page.contents.map { it.type })
    assertEquals(listOf(BalanceEffect.ADDS, BalanceEffect.SUBTRACTS), page.contents.map { it.balanceEffect })
    assertEquals(listOf("Jane Customer", "Acme Supplier"), page.contents.map { it.contactName })
    assertFalse(page.hasMore)
  }

  @Test
  fun `list trims the extra row, flags hasMore and cursors on the last returned row`() {
    searchRows = listOf(
      paymentRow(PaymentType.SALE, "SP000003", at(10)),
      paymentRow(PaymentType.PURCHASE, "SUP000002", at(9)),
      paymentRow(PaymentType.SALE, "SP000001", at(6))
    )

    val page = paymentSearchService.search(pageRequest(searchParameters(), size = 2))

    assertEquals(listOf("SP000003", "SUP000002"), page.contents.map { it.parentReferenceNumber })
    assertTrue(page.hasMore)
    val keysetSearchCursor = KeysetSearchCursor.decode(page.currentCursor)
    assertNotNull(keysetSearchCursor)
    assertEquals(page.contents.last().id, keysetSearchCursor!!.id)
  }

  @Test
  fun `list fails for a payment whose method no longer exists, so the bad delete gets fixed`() {
    searchRows = listOf(paymentRow(PaymentType.SALE, "SP000001", at(10)).copy(paymentMethodId = UUID.randomUUID()))

    assertThrows(NoSuchElementException::class.java) {
      paymentSearchService.search(pageRequest(searchParameters()))
    }
  }

  @Test
  fun `list marks voided rows with their reason for both types`() {
    searchRows = listOf(
      paymentRow(PaymentType.SALE, "SP000001", at(10), voidReason = "Wrong amount"),
      paymentRow(PaymentType.PURCHASE, "SUP000001", at(9), voidReason = "Duplicate")
    )

    val page = paymentSearchService.search(pageRequest(searchParameters()))

    assertTrue(page.contents.all { it.voided })
    assertEquals(listOf("Wrong amount", "Duplicate"), page.contents.map { it.voidedReason })
  }

  @Test
  fun `list queries every selected source in one call and applies reference filters like the summary`() {
    paymentSearchService.search(pageRequest(searchParameters()))
    paymentSearchService.search(pageRequest(searchParameters(types = setOf(PaymentType.PURCHASE))))
    paymentSearchService.search(
      pageRequest(searchParameters(referenceNumbersByType = mapOf(PaymentType.SALE to listOf("SL000001"))))
    )

    assertEquals(
      listOf(
        listOf(PaymentSource.SALE, PaymentSource.PURCHASE),
        listOf(PaymentSource.PURCHASE),
        listOf(PaymentSource.SALE)
      ),
      searchCalls.map { it.first }
    )
  }

  @Test
  fun `the contact filter reaches the query for every selected type`() {
    val contactIds = listOf(customerId, supplierId)

    paymentSearchService.search(pageRequest(searchParameters(contactIds = contactIds)))

    assertEquals(contactIds, searchCalls.single().second.contactIds)
  }
}
