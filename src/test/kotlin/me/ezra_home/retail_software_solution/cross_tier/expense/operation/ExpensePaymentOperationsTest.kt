package me.ezra_home.retail_software_solution.cross_tier.expense.operation

import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.request.ExpensePaymentCreateRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.request.ExpensePaymentVoidRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.response.ExpenseResponseBuilder
import me.ezra_home.retail_software_solution.cross_tier.expense.response.ExpenseSummaryResponse
import me.ezra_home.retail_software_solution.cross_tier.expense.model.PaymentInstruction
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseAggregate
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentVoidDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentDraft
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ResolvedSettlement
import me.ezra_home.retail_software_solution.cross_tier.expense.model.SettledExpense
import me.ezra_home.retail_software_solution.cross_tier.expense.store.ExpenseStore
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseTier
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpensePaymentRecordedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpensePaymentVoidedEvent
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ExpensePaymentOperationsTest {

    private val expenseRowResolver = mock(ExpenseRowResolver::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val expenseStore = mock(ExpenseStore::class.java)
    private val expenseResponseBuilder = mock(ExpenseResponseBuilder::class.java)

    private val expenseTier = mock(ExpenseTier::class.java).also { `when`(it.expenseStore).thenReturn(expenseStore) }

    private val tierExpenseOperations = ExpenseOperationsFactory(
        expenseRowResolver, expenseResponseBuilder, eventPublisher
    ).operationsFor(expenseTier)
    private val expensePaymentOperations = tierExpenseOperations.expensePaymentOperations
    private val expenseReissueOperations = tierExpenseOperations.expenseReissueOperations

    private val expenseDate = LocalDate.of(2026, 3, 10)
    private val settlementInstruction = PaymentInstruction(UUID.randomUUID())
    private val expenseTypeId = UUID.randomUUID()
    private val expenseAccountCode = "005.005"
    private val snapshottedPaymentMethodAccountCode = "001.099"

    @BeforeEach
    fun setSession() {
        SessionContextProvider.setSession(
            SessionContext(organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"))
        )
        `when`(expenseRowResolver.resolveSettlement(settlementInstruction, DateTimes.Local.Now.organization()))
            .thenReturn(resolvedSettlement())
        `when`(expenseTier.sourceContext()).thenReturn(EventSourceContext.OrgLevel(orgSchema = "org-a"))
        `when`(expenseResponseBuilder.buildSummaries(anyAggregate())).thenAnswer { invocation ->
            (invocation.arguments[0] as ExpenseAggregate).expenses.map { summaryResponse(it.referenceNumber) }
        }
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `a payment larger than the remaining balance is rejected`() {
        val expenseDto = stubExpense(amount = "100", activePayments = listOf("60"))

        assertThrows(RtsGenericException::class.java) {
            expensePaymentOperations.recordPayments(
                listOf(ExpensePaymentCreateRequest(expenseDto.referenceNumber, settlementInstruction, BigDecimal("50")))
            )
        }
        assertNoPaymentsSaved()
    }

    @Test
    fun `payments to one expense are checked together and the error names the total and the excess`() {
        val expenseDto = stubExpense(amount = "100", activePayments = emptyList())
        val sixtyEach = ExpensePaymentCreateRequest(expenseDto.referenceNumber, settlementInstruction, BigDecimal("60"))

        val exception = assertThrows(RtsGenericException::class.java) {
            expensePaymentOperations.recordPayments(listOf(sixtyEach, sixtyEach))
        }
        assertTrue(exception.message.contains("120.00"), exception.message)
        assertTrue(exception.message.contains("20.00"), exception.message)
        assertNoPaymentsSaved()
    }

    @Test
    fun `payments to one expense are saved in a single batch`() {
        val expenseDto = stubExpense(amount = "100", activePayments = emptyList())
        val drafts = listOf(
            ExpensePaymentDraft(expenseDto.id, BigDecimal("30.0000"), resolvedSettlement()),
            ExpensePaymentDraft(expenseDto.id, BigDecimal("70.0000"), resolvedSettlement())
        )
        `when`(expenseStore.savePayments(drafts))
            .thenReturn(listOf(paymentDto(expenseDto.id, "30"), paymentDto(expenseDto.id, "70")))
        val requests = listOf("30", "70").map {
            ExpensePaymentCreateRequest(expenseDto.referenceNumber, settlementInstruction, BigDecimal(it))
        }

        expensePaymentOperations.recordPayments(requests)

        verify(expenseStore).savePayments(drafts)
        assertEquals(2, mockingDetails(eventPublisher).invocations.size)
    }

    @Test
    fun `an empty payment collection is rejected`() {
        assertThrows(RtsGenericException::class.java) {
            expensePaymentOperations.recordPayments(emptyList())
        }
        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `a bulk request locks its expenses in one call and answers once, in request order`() {
        val firstExpense = expenseDto(BigDecimal("100"), "EXPN01")
        val secondExpense = expenseDto(BigDecimal("100"), "EXPN02")
        val (lowerIdExpense, higherIdExpense) = listOf(firstExpense, secondExpense).sortedBy { it.id }
        `when`(expenseStore.findExpensesByReferences(listOf(higherIdExpense.referenceNumber, lowerIdExpense.referenceNumber)))
            .thenReturn(listOf(firstExpense, secondExpense))
        `when`(expenseStore.savePayments(listOf(higherIdExpense, lowerIdExpense).map {
            ExpensePaymentDraft(it.id, BigDecimal("10.0000"), resolvedSettlement())
        })).thenReturn(listOf(higherIdExpense, lowerIdExpense).map { paymentDto(it.id, "10") })
        `when`(expenseStore.loadForExpenses(anyCollection())).thenReturn(
            ExpenseAggregate(emptyList(), listOf(lowerIdExpense, higherIdExpense), emptyList(), emptyList(), emptyList())
        )

        val summaries = expensePaymentOperations.recordPayments(
            listOf(
                ExpensePaymentCreateRequest(higherIdExpense.referenceNumber, settlementInstruction, BigDecimal("10")),
                ExpensePaymentCreateRequest(lowerIdExpense.referenceNumber, settlementInstruction, BigDecimal("10"))
            )
        )

        verify(expenseTier).lockExpenses(listOf(firstExpense.id, secondExpense.id))
        assertEquals(listOf(higherIdExpense.referenceNumber, lowerIdExpense.referenceNumber), summaries.map { it.reference })
        assertEquals(1, mockingDetails(expenseResponseBuilder).invocations.count { it.method.name == "buildSummaries" })
    }

    @Test
    fun `voiding a payment requires a reason`() {
        assertThrows(RtsGenericException::class.java) {
            expensePaymentOperations.voidPayment(ExpensePaymentVoidRequest("EXPY01", ""))
        }
    }

    @Test
    fun `a payment that was already voided cannot be voided again`() {
        val expenseDto = stubExpense(amount = "100", activePayments = emptyList())
        val paymentDto = paymentDto(expenseDto.id, "40")
        `when`(expenseStore.findPaymentByReference("EXPY01")).thenReturn(paymentDto)
        `when`(expenseStore.loadForExpenses(listOf(expenseDto.id))).thenReturn(
            ExpenseAggregate(
                batches = emptyList(),
                expenses = listOf(expenseDto),
                payments = listOf(paymentDto),
                paymentVoids = listOf(ExpensePaymentVoidDto(UUID.randomUUID(), paymentDto.id, "earlier", OffsetDateTime.now())),
                expenseVoids = emptyList()
            )
        )

        assertThrows(RtsGenericException::class.java) {
            expensePaymentOperations.voidPayment(ExpensePaymentVoidRequest("EXPY01", "wrong method"))
        }
        verify(expenseStore, never()).savePaymentVoid(paymentDto.id, "wrong method")
    }

    @Test
    fun `reissuing a recorded payment posts to the payment method account snapshotted when it was recorded`() {
        val expenseDto = expenseDto(BigDecimal("10.0000"))
        val expensePaymentDto = paymentDto(expenseDto.id, "10")
        `when`(expenseStore.findPaymentById(expensePaymentDto.id)).thenReturn(expensePaymentDto)
        `when`(expenseStore.findExpenseById(expenseDto.id)).thenReturn(expenseDto)

        expenseReissueOperations.reissuePaymentRecorded(expensePaymentDto.id)

        val publishedEvent = mockingDetails(eventPublisher).invocations.single().arguments[0] as ExpensePaymentRecordedEvent
        assertEquals(snapshottedPaymentMethodAccountCode, publishedEvent.paymentMethodAccountCode)
    }

    @Test
    fun `reissuing a payment void posts to the payment method account snapshotted when the payment was recorded`() {
        val expenseDto = expenseDto(BigDecimal("10.0000"))
        val expensePaymentDto = paymentDto(expenseDto.id, "10")
        val expensePaymentVoidDto = ExpensePaymentVoidDto(UUID.randomUUID(), expensePaymentDto.id, "wrong method", OffsetDateTime.now())
        `when`(expenseStore.findPaymentVoidById(expensePaymentVoidDto.id)).thenReturn(expensePaymentVoidDto)
        `when`(expenseStore.findPaymentById(expensePaymentDto.id)).thenReturn(expensePaymentDto)
        `when`(expenseStore.findExpenseById(expenseDto.id)).thenReturn(expenseDto)

        expenseReissueOperations.reissuePaymentVoided(expensePaymentVoidDto.id)

        val publishedEvent = mockingDetails(eventPublisher).invocations.single().arguments[0] as ExpensePaymentVoidedEvent
        assertEquals(snapshottedPaymentMethodAccountCode, publishedEvent.paymentMethodAccountCode)
    }

    @Test
    fun `voiding a payment posts to the payment method account snapshotted when it was recorded`() {
        val expenseDto = stubExpense(amount = "100", activePayments = emptyList())
        val expensePaymentDto = paymentDto(expenseDto.id, "40")
        `when`(expenseStore.findPaymentByReference("EXPY01")).thenReturn(expensePaymentDto)
        `when`(expenseStore.loadForExpenses(listOf(expenseDto.id))).thenReturn(
            ExpenseAggregate(emptyList(), listOf(expenseDto), listOf(expensePaymentDto), emptyList(), emptyList())
        )
        `when`(expenseStore.savePaymentVoid(expensePaymentDto.id, "wrong method")).thenReturn(
            ExpensePaymentVoidDto(UUID.randomUUID(), expensePaymentDto.id, "wrong method", OffsetDateTime.now())
        )

        expensePaymentOperations.voidPayment(ExpensePaymentVoidRequest("EXPY01", "wrong method"))

        val publishedEvent = mockingDetails(eventPublisher).invocations.single().arguments[0] as ExpensePaymentVoidedEvent
        assertEquals(snapshottedPaymentMethodAccountCode, publishedEvent.paymentMethodAccountCode)
    }

    @Test
    fun `recording payments refreshes the payment state of each paid expense after the payments are saved`() {
        val expenseDto = stubExpense(amount = "100", activePayments = emptyList())
        `when`(expenseStore.savePayments(listOf(ExpensePaymentDraft(expenseDto.id, BigDecimal("30.0000"), resolvedSettlement()))))
            .thenReturn(listOf(paymentDto(expenseDto.id, "30")))

        expensePaymentOperations.recordPayments(
            listOf(ExpensePaymentCreateRequest(expenseDto.referenceNumber, settlementInstruction, BigDecimal("30")))
        )

        assertRefreshedAfter("savePayments", listOf(expenseDto))
    }

    @Test
    fun `settling new expenses saves their payments in one batch and refreshes the payment states once`() {
        val firstExpense = expenseDto(BigDecimal("100"), "EXPN01")
        val secondExpense = expenseDto(BigDecimal("50"), "EXPN02")
        val drafts = listOf(firstExpense, secondExpense).map { ExpensePaymentDraft(it.id, it.amount, resolvedSettlement()) }
        `when`(expenseStore.savePayments(drafts))
            .thenReturn(listOf(paymentDto(firstExpense.id, "100"), paymentDto(secondExpense.id, "50")))

        expensePaymentOperations.settleNewExpenses(
            listOf(firstExpense, secondExpense).map { SettledExpense(it, resolvedSettlement()) }
        )

        verify(expenseStore).savePayments(drafts)
        assertEquals(2, mockingDetails(eventPublisher).invocations.size)
        assertRefreshedAfter("savePayments", listOf(firstExpense, secondExpense))
    }

    @Test
    fun `settling no expenses touches nothing`() {
        expensePaymentOperations.settleNewExpenses(emptyList())

        verifyNoInteractions(eventPublisher)
        assertTrue(mockingDetails(expenseStore).invocations.none { it.method.name == "savePayments" || it.method.name == "refreshPaymentStates" })
    }

    @Test
    fun `voiding a payment refreshes the payment state after the void is saved`() {
        val expenseDto = stubExpense(amount = "100", activePayments = emptyList())
        val expensePaymentDto = paymentDto(expenseDto.id, "40")
        `when`(expenseStore.findPaymentByReference("EXPY01")).thenReturn(expensePaymentDto)
        `when`(expenseStore.loadForExpenses(listOf(expenseDto.id))).thenReturn(
            ExpenseAggregate(emptyList(), listOf(expenseDto), listOf(expensePaymentDto), emptyList(), emptyList())
        )
        `when`(expenseStore.savePaymentVoid(expensePaymentDto.id, "wrong method")).thenReturn(
            ExpensePaymentVoidDto(UUID.randomUUID(), expensePaymentDto.id, "wrong method", OffsetDateTime.now())
        )

        expensePaymentOperations.voidPayment(ExpensePaymentVoidRequest("EXPY01", "wrong method"))

        assertRefreshedAfter("savePaymentVoid", listOf(expenseDto))
    }

    @Test
    fun `a rejected payment leaves the payment state untouched`() {
        val expenseDto = stubExpense(amount = "100", activePayments = listOf("60"))

        assertThrows(RtsGenericException::class.java) {
            expensePaymentOperations.recordPayments(
                listOf(ExpensePaymentCreateRequest(expenseDto.referenceNumber, settlementInstruction, BigDecimal("50")))
            )
        }

        assertTrue(mockingDetails(expenseStore).invocations.none { it.method.name == "refreshPaymentStates" })
    }

    private fun assertRefreshedAfter(writeMethodName: String, expectedExpenseRecords: List<ExpenseDto>) {
        val invocations = mockingDetails(expenseStore).invocations
        val writeIndex = invocations.indexOfFirst { it.method.name == writeMethodName }
        val refreshInvocations = invocations.filter { it.method.name == "refreshPaymentStates" }
        assertEquals(1, refreshInvocations.size)
        assertEquals(expectedExpenseRecords, (refreshInvocations.single().arguments[0] as Collection<*>).toList())
        assertTrue(writeIndex in 0 until invocations.indexOf(refreshInvocations.single()))
    }

    private fun resolvedSettlement() = ResolvedSettlement(settlementInstruction.paymentMethodId, "001.001", null, expenseDate)

    private fun expenseDto(amount: BigDecimal, referenceNumber: String = "EXPN01") = ExpenseDto(
        UUID.randomUUID(), referenceNumber, expenseTypeId, expenseAccountCode, UUID.randomUUID(), amount, expenseDate, null,
        ExpenseSourceType.ADHOC, null, UUID.randomUUID(), OffsetDateTime.now(), UUID.randomUUID()
    )

    private fun paymentDto(expenseId: UUID, amount: String) = ExpensePaymentDto(
        UUID.randomUUID(), expenseId, "EXPY01", settlementInstruction.paymentMethodId, snapshottedPaymentMethodAccountCode, BigDecimal(amount),
        null, OffsetDateTime.now(), OffsetDateTime.now()
    )

    // any() answers null, which Kotlin's non-null parameter check rejects before the stub is recorded.
    private fun anyAggregate(): ExpenseAggregate {
        any(ExpenseAggregate::class.java)
        return ExpenseAggregate(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    }

    private fun summaryResponse(reference: String) = ExpenseSummaryResponse(
        reference, "Freight", UUID.randomUUID(), "Acme", BigDecimal("100"), expenseDate, null, ExpenseSourceType.ADHOC, null,
        "EXBT01", "Batch", PaymentStatus.UNPAID, BigDecimal.ZERO, BigDecimal("100"), false, null, OffsetDateTime.now(), "Sam", emptyList()
    )

    private fun assertNoPaymentsSaved() {
        assertTrue(mockingDetails(expenseStore).invocations.none { it.method.name == "savePayments" })
    }

    private fun stubExpense(amount: String, activePayments: List<String>): ExpenseDto {
        val expenseDto = expenseDto(BigDecimal(amount))
        `when`(expenseStore.findExpensesByReferences(listOf("EXPN01"))).thenReturn(listOf(expenseDto))
        `when`(expenseStore.loadForExpenses(listOf(expenseDto.id))).thenReturn(
            ExpenseAggregate(
                batches = emptyList(),
                expenses = listOf(expenseDto),
                payments = activePayments.map { paymentDto(expenseDto.id, it) },
                paymentVoids = emptyList(),
                expenseVoids = emptyList()
            )
        )
        return expenseDto
    }
}
