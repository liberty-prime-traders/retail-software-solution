package me.ezra_home.retail_software_solution.cross_tier.expense.search

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpenseResponseBuilder
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpenseSummaryResponse
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseAggregate
import me.ezra_home.retail_software_solution.cross_tier.expense.store.ExpenseStore
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeResponseDto
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.paging.PageRequest
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ExpenseSearchOperationsTest {

    private val expenseSearchPort = mock(ExpenseSearchPort::class.java)
    private val expenseTypeService = mock(ExpenseTypeService::class.java)
    private val expenseStore = mock(ExpenseStore::class.java)
    private val expenseResponseBuilder = mock(ExpenseResponseBuilder::class.java)

    private val expenseSearchOperations = ExpenseSearchOperations(expenseTypeService, expenseResponseBuilder)

    private val freightTypeId = UUID.randomUUID()
    private val wagesTypeId = UUID.randomUUID()
    private val baseCreatedOn = OffsetDateTime.parse("2030-01-01T00:00:00Z")
    private val rangedParameters = ExpenseSearchParameters(createdFrom = baseCreatedOn, createdBefore = baseCreatedOn.plusDays(30))

    init {
        `when`(expenseTypeService.getAll(null)).thenReturn(listOf(expenseType(freightTypeId, "Freight"), expenseType(wagesTypeId, "Wages")))
    }

    @Test
    fun `a page is trimmed to the requested size and reports more when the extra row came back`() {
        val rows = listOf(rawRow("EXPN03", 3), rawRow("EXPN02", 2), rawRow("EXPN01", 1))
        `when`(expenseSearchPort.search(rangedParameters, null, 2)).thenReturn(rows)
        stubSummaries(rows.take(2))

        val page = expenseSearchOperations.search(expenseSearchPort, expenseStore, PageRequest("", 2, rangedParameters))

        assertTrue(page.hasMore)
        assertEquals(listOf("EXPN03", "EXPN02"), page.contents.map { it.reference })
        assertEquals(KeysetSearchCursor(rows[1].createdOn, rows[1].id).encode(), page.currentCursor)
    }

    @Test
    fun `blank and null reference filters are dropped before the query runs`() {
        val rows = listOf(rawRow("EXPN01", 1))
        // A JSON body can carry a null element even though the Kotlin type says List<String>.
        @Suppress("UNCHECKED_CAST")
        val dirtyParameters = rangedParameters.copy(
            expenseReferenceNumbers = listOf<String?>("EXPN01", " ", "", null) as List<String>,
            sourceReferences = listOf(" PRCH01 ", "")
        )
        `when`(expenseSearchPort.search(
            rangedParameters.copy(expenseReferenceNumbers = listOf("EXPN01"), sourceReferences = listOf("PRCH01")), null, 5
        )).thenReturn(rows)
        stubSummaries(rows)

        val page = expenseSearchOperations.search(expenseSearchPort, expenseStore, PageRequest("", 5, dirtyParameters))

        assertEquals(listOf("EXPN01"), page.contents.map { it.reference })
    }

    @Test
    fun `a page that exactly fills the request reports nothing more`() {
        val rows = listOf(rawRow("EXPN02", 2), rawRow("EXPN01", 1))
        `when`(expenseSearchPort.search(rangedParameters, null, 2)).thenReturn(rows)
        stubSummaries(rows)

        val page = expenseSearchOperations.search(expenseSearchPort, expenseStore, PageRequest("", 2, rangedParameters))

        assertFalse(page.hasMore)
        assertEquals(2, page.contents.size)
    }

    @Test
    fun `an empty page keeps the incoming cursor and loads nothing`() {
        val incomingCursor = KeysetSearchCursor(baseCreatedOn, UUID.randomUUID()).encode()
        `when`(expenseSearchPort.search(rangedParameters, KeysetSearchCursor.decode(incomingCursor), 5)).thenReturn(emptyList())

        val page = expenseSearchOperations.search(expenseSearchPort, expenseStore, PageRequest(incomingCursor, 5, rangedParameters))

        assertEquals(incomingCursor, page.currentCursor)
        assertTrue(page.contents.isEmpty())
        verifyNoInteractions(expenseStore)
    }

    @Test
    fun `the page is loaded in one batch and returned in query order whatever order the builder answers in`() {
        val rows = listOf(rawRow("EXPN02", 2), rawRow("EXPN01", 1))
        `when`(expenseSearchPort.search(rangedParameters, null, 10)).thenReturn(rows)
        stubSummaries(rows, builderOrder = rows.reversed())

        val page = expenseSearchOperations.search(expenseSearchPort, expenseStore, PageRequest("", 10, rangedParameters))

        assertEquals(listOf("EXPN02", "EXPN01"), page.contents.map { it.reference })
        verify(expenseStore, times(1)).loadForExpenses(rows.map { it.id })
        verify(expenseResponseBuilder, times(1)).buildSummaries(aggregate)
    }

    @Test
    fun `an invalid page size or malformed cursor is rejected before any query runs`() {
        assertThrows(RtsGenericException::class.java) { expenseSearchOperations.search(expenseSearchPort, expenseStore, PageRequest("", 501, rangedParameters)) }
        assertThrows(RtsGenericException::class.java) { expenseSearchOperations.search(expenseSearchPort, expenseStore, PageRequest("not-a-cursor", 10, rangedParameters)) }

        verifyNoInteractions(expenseSearchPort)
    }

    @Test
    fun `a search or summary without a created range is rejected before any query runs`() {
        val unranged = ExpenseSearchParameters(voided = false)

        assertThrows(RtsGenericException::class.java) { expenseSearchOperations.search(expenseSearchPort, expenseStore, PageRequest("", 10, unranged)) }
        assertThrows(RtsGenericException::class.java) { expenseSearchOperations.summarize(expenseSearchPort, unranged) }

        verifyNoInteractions(expenseSearchPort)
    }

    @Test
    fun `an empty summary still returns every bucket zero filled in a fixed order`() {
        val parameters = rangedParameters.copy(voided = false)
        `when`(expenseSearchPort.summarize(parameters)).thenReturn(emptyList())

        val summary = expenseSearchOperations.summarize(expenseSearchPort, parameters)

        assertEquals(ExpenseSummaryBucket.entries, summary.byStatus.map { it.bucket })
        assertTrue(summary.byStatus.all { it.expenseCount == 0L && it.amountTotal.signum() == 0 })
        assertTrue(summary.byExpenseType.isEmpty())
        assertEquals(0L, summary.expenseCount)
        assertEquals(0L, summary.voidedCount)
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.amountTotal))
    }

    @Test
    fun `each type reports live figures and voided figures apart, and totals are summed across types`() {
        val parameters = rangedParameters.copy(expenseTypeIds = listOf(freightTypeId, wagesTypeId))
        `when`(expenseSearchPort.summarize(parameters)).thenReturn(
            listOf(
                summaryRow(freightTypeId, false, PaymentStatus.UNPAID, count = 2, amount = "300", paid = "0", outstanding = "300"),
                summaryRow(freightTypeId, false, PaymentStatus.PARTIALLY_SETTLED, count = 1, amount = "200", paid = "50", outstanding = "150"),
                summaryRow(freightTypeId, true, PaymentStatus.FULLY_SETTLED, count = 1, amount = "100", paid = "0", outstanding = "0"),
                summaryRow(wagesTypeId, false, PaymentStatus.FULLY_SETTLED, count = 1, amount = "500", paid = "500", outstanding = "0"),
                summaryRow(wagesTypeId, true, PaymentStatus.UNPAID, count = 3, amount = "900", paid = "0", outstanding = "0")
            )
        )
        `when`(expenseTypeService.getAll(null)).thenReturn(listOf(expenseType(wagesTypeId, "Wages"), expenseType(freightTypeId, "Freight")))

        val summary = expenseSearchOperations.summarize(expenseSearchPort, parameters)

        val byBucket = summary.byStatus.associateBy { it.bucket }
        assertEquals(2L, byBucket.getValue(ExpenseSummaryBucket.UNPAID).expenseCount)
        assertEquals(1L, byBucket.getValue(ExpenseSummaryBucket.PARTIAL).expenseCount)
        assertEquals(1L, byBucket.getValue(ExpenseSummaryBucket.PAID).expenseCount)
        assertEquals(4L, byBucket.getValue(ExpenseSummaryBucket.VOIDED).expenseCount)
        assertEquals(0, BigDecimal("1000").compareTo(byBucket.getValue(ExpenseSummaryBucket.VOIDED).amountTotal))
        assertEquals(0, BigDecimal.ZERO.compareTo(byBucket.getValue(ExpenseSummaryBucket.VOIDED).paidTotal))

        assertEquals(listOf("Freight", "Wages"), summary.byExpenseType.map { it.expenseTypeName })
        val freight = summary.byExpenseType.first()
        assertEquals(3L, freight.expenseCount)
        assertEquals(0, BigDecimal("500").compareTo(freight.amountTotal))
        assertEquals(0, BigDecimal("50").compareTo(freight.paidTotal))
        assertEquals(0, BigDecimal("450").compareTo(freight.outstandingTotal))
        assertEquals(1L, freight.voidedCount)
        assertEquals(0, BigDecimal("100").compareTo(freight.voidedAmountTotal))

        assertEquals(4L, summary.expenseCount)
        assertEquals(0, BigDecimal("1000").compareTo(summary.amountTotal))
        assertEquals(0, BigDecimal("550").compareTo(summary.paidTotal))
        assertEquals(0, BigDecimal("450").compareTo(summary.outstandingTotal))
        assertEquals(4L, summary.voidedCount)
        assertEquals(0, BigDecimal("1000").compareTo(summary.voidedAmountTotal))
    }

    @Test
    fun `a type whose matches are all voided still appears with zero live figures`() {
        val parameters = rangedParameters.copy(voided = true)
        `when`(expenseSearchPort.summarize(parameters)).thenReturn(listOf(summaryRow(wagesTypeId, true, PaymentStatus.UNPAID, count = 2, amount = "70", paid = "0", outstanding = "0")))
        `when`(expenseTypeService.getAll(null)).thenReturn(listOf(expenseType(wagesTypeId, "Wages")))

        val wages = expenseSearchOperations.summarize(expenseSearchPort, parameters).byExpenseType.single()

        assertEquals(0L, wages.expenseCount)
        assertEquals(2L, wages.voidedCount)
        assertEquals(0, BigDecimal("70").compareTo(wages.voidedAmountTotal))
    }

    private fun expenseType(id: UUID, name: String) = ExpenseTypeResponseDto(id, null, name, "005.005", emptySet(), emptySet(), false)

    private val aggregate = ExpenseAggregate(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

    private fun stubSummaries(rows: List<ExpenseSearchRawRow>, builderOrder: List<ExpenseSearchRawRow> = rows) {
        `when`(expenseStore.loadForExpenses(rows.map { it.id })).thenReturn(aggregate)
        `when`(expenseResponseBuilder.buildSummaries(aggregate)).thenReturn(builderOrder.map { summaryResponse(it.referenceNumber) })
    }

    private fun summaryResponse(reference: String) = ExpenseSummaryResponse(
        reference, "Freight", UUID.randomUUID(), "Acme", BigDecimal("100"), LocalDate.of(2030, 1, 1), null, ExpenseSourceType.ADHOC, null,
        "EXBT01", "Batch", PaymentStatus.UNPAID, BigDecimal.ZERO, BigDecimal("100"), false, null, baseCreatedOn, "Sam", emptyList()
    )

    private fun rawRow(referenceNumber: String, createdHoursAfterBase: Long) =
        ExpenseSearchRawRow(UUID.randomUUID(), referenceNumber, baseCreatedOn.plusHours(createdHoursAfterBase))

    private fun summaryRow(
        expenseTypeId: UUID,
        voided: Boolean,
        paymentStatus: PaymentStatus,
        count: Long,
        amount: String,
        paid: String,
        outstanding: String
    ) = ExpenseSummaryRawRow(expenseTypeId, voided, paymentStatus, count, BigDecimal(amount), BigDecimal(paid), BigDecimal(outstanding))
}
