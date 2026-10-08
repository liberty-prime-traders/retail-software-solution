package me.ezra_home.retail_software_solution.cross_tier.expense.operation

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseAggregate
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseBatchRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentDraft
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseVoidRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ResolvedExpenseRow
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ResolvedSettlement
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpenseVoidRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.PaymentInstruction
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpenseResponseBuilder
import me.ezra_home.retail_software_solution.cross_tier.expense.store.ExpenseStore
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpensePaymentRecordedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpenseRecordedEvent
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeDto
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ExpenseOperationsTest {

    private val expenseRowResolver = mock(ExpenseRowResolver::class.java)
    private val expenseResponseBuilder = mock(ExpenseResponseBuilder::class.java)
    private val fiscalPeriodService = mock(FiscalPeriodService::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val expenseStore = mock(ExpenseStore::class.java)

    private val expenseLookup = ExpenseLookup()
    private val expensePaymentOperations = ExpensePaymentOperations(
        expenseRowResolver, expenseResponseBuilder, expenseLookup, fiscalPeriodService, eventPublisher
    )
    private val expenseOperations = ExpenseOperations(
        expenseRowResolver, expenseResponseBuilder, expenseLookup, expensePaymentOperations, fiscalPeriodService, eventPublisher
    )

    private val expenseDate = LocalDate.of(2026, 3, 10)
    private val orgContext = EventSourceContext.OrgLevel(orgSchema = "org-a")
    private val expenseType = ExpenseTypeDto(
        id = UUID.randomUUID(), createdById = UUID.randomUUID(), createdOn = OffsetDateTime.now(), code = null, name = "Freight",
        expenseAccountCode = "005.005", eligiblePayeeTypes = setOf(ContactType.SUPPLIER),
        eligibleSourceTypes = setOf(ExpenseSourceType.ADHOC, ExpenseSourceType.PURCHASE), systemDefined = false
    )
    private val payee = ContactDto(
        id = UUID.randomUUID(), createdById = UUID.randomUUID(), createdOn = OffsetDateTime.now(),
        referenceNumber = "CONT01", contactTypes = setOf(ContactType.SUPPLIER), companyName = "Acme"
    )

    @BeforeEach
    fun setSession() {
        SessionContextProvider.setSession(
            SessionContext(organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"))
        )
        `when`(expenseStore.sourceContext()).thenReturn(orgContext)
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `an expense with a live payment cannot be voided`() {
        val expenseRecord = stubExpense(amount = "100", activePayments = listOf("40"))

        assertThrows(RtsGenericException::class.java) {
            expenseOperations.voidExpense(expenseStore, ExpenseVoidRequest(expenseRecord.referenceNumber, "entered twice"))
        }
        verify(expenseStore, never()).saveExpenseVoid(expenseRecord.id, "entered twice")
        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `an already voided expense cannot be voided or paid again`() {
        val expenseRecord = stubExpense(amount = "100", activePayments = emptyList(), voided = true)

        assertThrows(RtsGenericException::class.java) {
            expenseOperations.voidExpense(expenseStore, ExpenseVoidRequest(expenseRecord.referenceNumber, "again"))
        }
        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `voiding requires a reason`() {
        assertThrows(RtsGenericException::class.java) {
            expenseOperations.voidExpense(expenseStore, ExpenseVoidRequest("EXPN01", "  "))
        }
    }

    @Test
    fun `a contextual submission locks the source document before looking for its batch`() {
        val sourceDocumentId = UUID.randomUUID()
        val batchRecord = ExpenseBatchRecord(UUID.randomUUID(), "EXBT01", "Purchase PRCH01", OffsetDateTime.now(), UUID.randomUUID())
        `when`(expenseRowResolver.resolve(ExpenseSourceType.PURCHASE, expenseDate, emptyList())).thenReturn(emptyList())
        `when`(expenseStore.findBatchBySource(ExpenseSourceType.PURCHASE, "PRCH01")).thenReturn(batchRecord)
        `when`(expenseStore.loadForExpenses(emptyList())).thenReturn(emptyAggregate())

        expenseOperations.createForSourceDocument(
            expenseStore, ExpenseSourceType.PURCHASE, "PRCH01", sourceDocumentId, expenseDate, emptyList()
        )

        val lockThenFind = inOrder(expenseStore)
        lockThenFind.verify(expenseStore).lockSourceDocument(sourceDocumentId)
        lockThenFind.verify(expenseStore).findBatchBySource(ExpenseSourceType.PURCHASE, "PRCH01")
        verify(expenseStore, never()).createBatch("Purchase PRCH01", ExpenseSourceType.PURCHASE, "PRCH01")
    }

    @Test
    fun `appending to a batch answers with the rows just written and never reloads the whole batch`() {
        val batchRecord = ExpenseBatchRecord(UUID.randomUUID(), "EXBT01", "Purchase PRCH01", OffsetDateTime.now(), UUID.randomUUID())
        `when`(expenseRowResolver.resolve(ExpenseSourceType.PURCHASE, expenseDate, emptyList())).thenReturn(emptyList())
        `when`(expenseStore.findBatchBySource(ExpenseSourceType.PURCHASE, "PRCH01")).thenReturn(batchRecord)
        `when`(expenseStore.loadForExpenses(emptyList())).thenReturn(emptyAggregate())

        expenseOperations.createForSourceDocument(
            expenseStore, ExpenseSourceType.PURCHASE, "PRCH01", UUID.randomUUID(), expenseDate, emptyList()
        )

        verify(expenseStore, never()).loadForBatch(batchRecord.id)
    }

    @Test
    fun `rows are answered in the order they were written whatever order the store loads them in`() {
        val batchRecord = ExpenseBatchRecord(UUID.randomUUID(), "EXBT01", "Freight run", OffsetDateTime.now(), UUID.randomUUID())
        val firstRow = ResolvedExpenseRow(expenseType, payee, BigDecimal("10.0000"), null, expenseDate, null)
        val secondRow = ResolvedExpenseRow(expenseType, payee, BigDecimal("20.0000"), null, expenseDate, null)
        val firstRecord = expenseRecord(BigDecimal("10.0000"), "EXPN01")
        val secondRecord = expenseRecord(BigDecimal("20.0000"), "EXPN02")
        `when`(expenseRowResolver.resolve(ExpenseSourceType.ADHOC, expenseDate, emptyList())).thenReturn(listOf(firstRow, secondRow))
        `when`(expenseStore.createBatch("Freight run", ExpenseSourceType.ADHOC, null)).thenReturn(batchRecord)
        `when`(expenseStore.saveExpense(batchRecord.id, ExpenseSourceType.ADHOC, null, firstRow)).thenReturn(firstRecord)
        `when`(expenseStore.saveExpense(batchRecord.id, ExpenseSourceType.ADHOC, null, secondRow)).thenReturn(secondRecord)
        `when`(expenseStore.loadForExpenses(listOf(firstRecord.id, secondRecord.id))).thenReturn(
            ExpenseAggregate(emptyList(), listOf(secondRecord, firstRecord), emptyList(), emptyList(), emptyList())
        )

        expenseOperations.createBatch(expenseStore, ExpenseSourceType.ADHOC, "Freight run", expenseDate, emptyList())

        val summarizedAggregate = mockingDetails(expenseResponseBuilder).invocations.single().arguments[0] as ExpenseAggregate
        assertEquals(listOf("EXPN01", "EXPN02"), summarizedAggregate.expenses.map { it.referenceNumber })
    }

    @Test
    fun `a settled row publishes the expense event then the payment event with the store's source context`() {
        val resolvedRow = ResolvedExpenseRow(
            expenseType = expenseType, payee = payee, amount = BigDecimal("100.0000"), description = null,
            expenseDate = expenseDate, settlement = resolvedSettlement()
        )
        val batchRecord = ExpenseBatchRecord(UUID.randomUUID(), "EXBT01", "Freight run", OffsetDateTime.now(), UUID.randomUUID())
        val expenseRecord = expenseRecord(BigDecimal("100.0000"))
        `when`(expenseRowResolver.resolve(ExpenseSourceType.ADHOC, expenseDate, emptyList())).thenReturn(listOf(resolvedRow))
        `when`(expenseStore.createBatch("Freight run", ExpenseSourceType.ADHOC, null)).thenReturn(batchRecord)
        `when`(expenseStore.saveExpense(batchRecord.id, ExpenseSourceType.ADHOC, null, resolvedRow)).thenReturn(expenseRecord)
        `when`(expenseStore.savePayments(listOf(ExpensePaymentDraft(expenseRecord.id, BigDecimal("100.0000"), resolvedRow.settlement!!))))
            .thenReturn(listOf(paymentRecord(expenseRecord.id, "100")))
        `when`(expenseStore.loadForExpenses(listOf(expenseRecord.id))).thenReturn(
            ExpenseAggregate(emptyList(), listOf(expenseRecord), emptyList(), emptyList(), emptyList())
        )

        expenseOperations.createBatch(expenseStore, ExpenseSourceType.ADHOC, "Freight run", expenseDate, emptyList())

        val publishedEvents = mockingDetails(eventPublisher).invocations.map { it.arguments[0] }
        assertEquals(2, publishedEvents.size)
        val recorded = assertInstanceOf(ExpenseRecordedEvent::class.java, publishedEvents[0])
        val paid = assertInstanceOf(ExpensePaymentRecordedEvent::class.java, publishedEvents[1])
        assertEquals(orgContext, recorded.sourceContext)
        assertEquals("005.005", recorded.expenseAccountCode)
        assertEquals("001.001", paid.paymentMethodAccountCode)
    }

    private val settlementInstruction = PaymentInstruction(UUID.randomUUID())

    private fun resolvedSettlement() = ResolvedSettlement(settlementInstruction.paymentMethodId, "001.001", null, expenseDate)

    private fun expenseRecord(amount: BigDecimal, referenceNumber: String = "EXPN01") = ExpenseRecord(
        UUID.randomUUID(), referenceNumber, expenseType.id, expenseType.expenseAccountCode, payee.id, amount, expenseDate, null,
        ExpenseSourceType.ADHOC, null, UUID.randomUUID(), OffsetDateTime.now(), UUID.randomUUID()
    )

    private fun paymentRecord(expenseId: UUID, amount: String) = ExpensePaymentRecord(
        UUID.randomUUID(), expenseId, "EXPY01", settlementInstruction.paymentMethodId, "001.001", BigDecimal(amount),
        null, OffsetDateTime.now(), OffsetDateTime.now()
    )

    private fun emptyAggregate() = ExpenseAggregate(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

    private fun stubExpense(amount: String, activePayments: List<String>, voided: Boolean = false): ExpenseRecord {
        val expenseRecord = expenseRecord(BigDecimal(amount))
        `when`(expenseStore.findExpenseByReference("EXPN01")).thenReturn(expenseRecord)
        `when`(expenseStore.loadForExpenses(listOf(expenseRecord.id))).thenReturn(
            ExpenseAggregate(
                batches = emptyList(),
                expenses = listOf(expenseRecord),
                payments = activePayments.map { paymentRecord(expenseRecord.id, it) },
                paymentVoids = emptyList(),
                expenseVoids = if (voided) {
                    listOf(ExpenseVoidRecord(UUID.randomUUID(), expenseRecord.id, "earlier", OffsetDateTime.now()))
                } else emptyList()
            )
        )
        return expenseRecord
    }
}
