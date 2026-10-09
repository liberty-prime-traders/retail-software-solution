package me.ezra_home.retail_software_solution.cross_tier.expense.operation

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseSource
import me.ezra_home.retail_software_solution.cross_tier.expense.model.NewExpenseBatch
import me.ezra_home.retail_software_solution.cross_tier.expense.model.NewExpense
import me.ezra_home.retail_software_solution.cross_tier.expense.model.SourceDocument
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseSubmission
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseAggregate
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseBatchDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentDraft
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseVoidDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ResolvedExpenseRow
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ResolvedSettlement
import me.ezra_home.retail_software_solution.cross_tier.expense.request.ExpenseVoidRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.model.PaymentInstruction
import me.ezra_home.retail_software_solution.cross_tier.expense.response.ExpenseResponseBuilder
import me.ezra_home.retail_software_solution.cross_tier.expense.store.ExpenseStore
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseTier
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpensePaymentRecordedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpenseRecordedEvent
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeDto
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
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val expenseStore = mock(ExpenseStore::class.java)
    private val expenseTier = mock(ExpenseTier::class.java).also { `when`(it.expenseStore).thenReturn(expenseStore) }

    private val expenseOperations = ExpenseOperationsFactory(
        expenseRowResolver, expenseResponseBuilder, eventPublisher
    ).operationsFor(expenseTier).expenseOperations

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
        `when`(expenseTier.sourceContext()).thenReturn(orgContext)
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `an expense with a live payment cannot be voided`() {
        val expenseDto = stubExpense(amount = "100", activePayments = listOf("40"))

        assertThrows(RtsGenericException::class.java) {
            expenseOperations.voidExpense(ExpenseVoidRequest(expenseDto.referenceNumber, "entered twice"))
        }
        verify(expenseStore, never()).saveExpenseVoid(expenseDto.id, "entered twice")
        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `an already voided expense cannot be voided or paid again`() {
        val expenseDto = stubExpense(amount = "100", activePayments = emptyList(), voided = true)

        assertThrows(RtsGenericException::class.java) {
            expenseOperations.voidExpense(ExpenseVoidRequest(expenseDto.referenceNumber, "again"))
        }
        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `voiding requires a reason`() {
        assertThrows(RtsGenericException::class.java) {
            expenseOperations.voidExpense(ExpenseVoidRequest("EXPN01", "  "))
        }
    }

    @Test
    fun `a contextual submission locks the source document before looking for its batch`() {
        val sourceDocumentId = UUID.randomUUID()
        val batchDto = ExpenseBatchDto(UUID.randomUUID(), "EXBT01", "Purchase PRCH01", OffsetDateTime.now(), UUID.randomUUID())
        `when`(expenseRowResolver.resolve(ExpenseSourceType.PURCHASE, ExpenseSubmission(expenseDate, emptyList()))).thenReturn(emptyList())
        `when`(expenseStore.findBatchBySource(ExpenseSourceType.PURCHASE, "PRCH01")).thenReturn(batchDto)
        `when`(expenseStore.loadForExpenses(emptyList())).thenReturn(emptyAggregate())

        expenseOperations.createForSourceDocument(SourceDocument(ExpenseSourceType.PURCHASE, "PRCH01", sourceDocumentId), ExpenseSubmission(expenseDate, emptyList()))

        val lockThenFind = inOrder(expenseTier, expenseStore)
        lockThenFind.verify(expenseTier).lockSourceDocument(sourceDocumentId)
        lockThenFind.verify(expenseStore).findBatchBySource(ExpenseSourceType.PURCHASE, "PRCH01")
        verify(expenseStore, never()).createBatch(NewExpenseBatch("Purchase PRCH01", ExpenseSource(ExpenseSourceType.PURCHASE, "PRCH01")))
    }

    @Test
    fun `appending to a batch answers with the rows just written and never reloads the whole batch`() {
        val batchDto = ExpenseBatchDto(UUID.randomUUID(), "EXBT01", "Purchase PRCH01", OffsetDateTime.now(), UUID.randomUUID())
        `when`(expenseRowResolver.resolve(ExpenseSourceType.PURCHASE, ExpenseSubmission(expenseDate, emptyList()))).thenReturn(emptyList())
        `when`(expenseStore.findBatchBySource(ExpenseSourceType.PURCHASE, "PRCH01")).thenReturn(batchDto)
        `when`(expenseStore.loadForExpenses(emptyList())).thenReturn(emptyAggregate())

        expenseOperations.createForSourceDocument(SourceDocument(ExpenseSourceType.PURCHASE, "PRCH01", UUID.randomUUID()), ExpenseSubmission(expenseDate, emptyList()))

        verify(expenseStore, never()).loadForBatch(batchDto.id)
    }

    @Test
    fun `rows are answered in the order they were written whatever order the store loads them in`() {
        val batchDto = ExpenseBatchDto(UUID.randomUUID(), "EXBT01", "Freight run", OffsetDateTime.now(), UUID.randomUUID())
        val firstRow = ResolvedExpenseRow(expenseType, payee, BigDecimal("10.0000"), null, expenseDate, null)
        val secondRow = ResolvedExpenseRow(expenseType, payee, BigDecimal("20.0000"), null, expenseDate, null)
        val firstRecord = expenseDto(BigDecimal("10.0000"), "EXPN01")
        val secondRecord = expenseDto(BigDecimal("20.0000"), "EXPN02")
        `when`(expenseRowResolver.resolve(ExpenseSourceType.ADHOC, ExpenseSubmission(expenseDate, emptyList()))).thenReturn(listOf(firstRow, secondRow))
        `when`(expenseStore.createBatch(NewExpenseBatch("Freight run", ExpenseSource(ExpenseSourceType.ADHOC, null)))).thenReturn(batchDto)
        `when`(expenseStore.saveExpense(NewExpense(batchDto.id, ExpenseSource(ExpenseSourceType.ADHOC, null), firstRow))).thenReturn(firstRecord)
        `when`(expenseStore.saveExpense(NewExpense(batchDto.id, ExpenseSource(ExpenseSourceType.ADHOC, null), secondRow))).thenReturn(secondRecord)
        `when`(expenseStore.loadForExpenses(listOf(firstRecord.id, secondRecord.id))).thenReturn(
            ExpenseAggregate(emptyList(), listOf(secondRecord, firstRecord), emptyList(), emptyList(), emptyList())
        )

        expenseOperations.createBatch(ExpenseSourceType.ADHOC, "Freight run", ExpenseSubmission(expenseDate, emptyList()))

        val summarizedAggregate = mockingDetails(expenseResponseBuilder).invocations.single().arguments[0] as ExpenseAggregate
        assertEquals(listOf("EXPN01", "EXPN02"), summarizedAggregate.expenses.map { it.referenceNumber })
    }

    @Test
    fun `a settled row publishes the expense event then the payment event with the store's source context`() {
        val resolvedRow = ResolvedExpenseRow(
            expenseType = expenseType, payee = payee, amount = BigDecimal("100.0000"), description = null,
            expenseDate = expenseDate, settlement = resolvedSettlement()
        )
        val batchDto = ExpenseBatchDto(UUID.randomUUID(), "EXBT01", "Freight run", OffsetDateTime.now(), UUID.randomUUID())
        val expenseDto = expenseDto(BigDecimal("100.0000"))
        `when`(expenseRowResolver.resolve(ExpenseSourceType.ADHOC, ExpenseSubmission(expenseDate, emptyList()))).thenReturn(listOf(resolvedRow))
        `when`(expenseStore.createBatch(NewExpenseBatch("Freight run", ExpenseSource(ExpenseSourceType.ADHOC, null)))).thenReturn(batchDto)
        `when`(expenseStore.saveExpense(NewExpense(batchDto.id, ExpenseSource(ExpenseSourceType.ADHOC, null), resolvedRow))).thenReturn(expenseDto)
        `when`(expenseStore.savePayments(listOf(ExpensePaymentDraft(expenseDto.id, BigDecimal("100.0000"), resolvedRow.settlement!!))))
            .thenReturn(listOf(paymentDto(expenseDto.id, "100")))
        `when`(expenseStore.loadForExpenses(listOf(expenseDto.id))).thenReturn(
            ExpenseAggregate(emptyList(), listOf(expenseDto), emptyList(), emptyList(), emptyList())
        )

        expenseOperations.createBatch(ExpenseSourceType.ADHOC, "Freight run", ExpenseSubmission(expenseDate, emptyList()))

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

    private fun expenseDto(amount: BigDecimal, referenceNumber: String = "EXPN01") = ExpenseDto(
        UUID.randomUUID(), referenceNumber, expenseType.id, expenseType.expenseAccountCode, payee.id, amount, expenseDate, null,
        ExpenseSourceType.ADHOC, null, UUID.randomUUID(), OffsetDateTime.now(), UUID.randomUUID()
    )

    private fun paymentDto(expenseId: UUID, amount: String) = ExpensePaymentDto(
        UUID.randomUUID(), expenseId, "EXPY01", settlementInstruction.paymentMethodId, "001.001", BigDecimal(amount),
        null, OffsetDateTime.now(), OffsetDateTime.now()
    )

    private fun emptyAggregate() = ExpenseAggregate(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

    private fun stubExpense(amount: String, activePayments: List<String>, voided: Boolean = false): ExpenseDto {
        val expenseDto = expenseDto(BigDecimal(amount))
        `when`(expenseStore.findExpenseByReference("EXPN01")).thenReturn(expenseDto)
        `when`(expenseStore.loadForExpenses(listOf(expenseDto.id))).thenReturn(
            ExpenseAggregate(
                batches = emptyList(),
                expenses = listOf(expenseDto),
                payments = activePayments.map { paymentDto(expenseDto.id, it) },
                paymentVoids = emptyList(),
                expenseVoids = if (voided) {
                    listOf(ExpenseVoidDto(UUID.randomUUID(), expenseDto.id, "earlier", OffsetDateTime.now()))
                } else emptyList()
            )
        )
        return expenseDto
    }
}
