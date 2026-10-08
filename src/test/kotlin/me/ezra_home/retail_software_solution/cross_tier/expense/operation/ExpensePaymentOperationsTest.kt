package me.ezra_home.retail_software_solution.cross_tier.expense.operation

import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpensePaymentCreateRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpensePaymentVoidRequest
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpenseResponseBuilder
import me.ezra_home.retail_software_solution.cross_tier.expense.api.PaymentInstruction
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseAggregate
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentVoidRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentDraft
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ResolvedSettlement
import me.ezra_home.retail_software_solution.cross_tier.expense.store.ExpenseStore
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpensePaymentRecordedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpensePaymentVoidedEvent
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertThrows
import org.mockito.Mockito.inOrder
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

    private val expensePaymentOperations = ExpensePaymentOperations(
        expenseRowResolver, mock(ExpenseResponseBuilder::class.java), ExpenseLookup(),
        mock(FiscalPeriodService::class.java), eventPublisher
    )

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
        `when`(expenseStore.sourceContext()).thenReturn(EventSourceContext.OrgLevel(orgSchema = "org-a"))
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `a payment larger than the remaining balance is rejected`() {
        val expenseRecord = stubExpense(amount = "100", activePayments = listOf("60"))

        assertThrows(RtsGenericException::class.java) {
            expensePaymentOperations.recordPayments(
                expenseStore, listOf(ExpensePaymentCreateRequest(expenseRecord.referenceNumber, settlementInstruction, BigDecimal("50")))
            )
        }
        assertNoPaymentsSaved()
    }

    @Test
    fun `payments to one expense are checked together and the error names the total and the excess`() {
        val expenseRecord = stubExpense(amount = "100", activePayments = emptyList())
        val sixtyEach = ExpensePaymentCreateRequest(expenseRecord.referenceNumber, settlementInstruction, BigDecimal("60"))

        val exception = assertThrows(RtsGenericException::class.java) {
            expensePaymentOperations.recordPayments(expenseStore, listOf(sixtyEach, sixtyEach))
        }
        assertTrue(exception.message.contains("120.00"), exception.message)
        assertTrue(exception.message.contains("20.00"), exception.message)
        assertNoPaymentsSaved()
    }

    @Test
    fun `payments to one expense are saved in a single batch`() {
        val expenseRecord = stubExpense(amount = "100", activePayments = emptyList())
        val drafts = listOf(
            ExpensePaymentDraft(expenseRecord.id, BigDecimal("30.0000"), resolvedSettlement()),
            ExpensePaymentDraft(expenseRecord.id, BigDecimal("70.0000"), resolvedSettlement())
        )
        `when`(expenseStore.savePayments(drafts))
            .thenReturn(listOf(paymentRecord(expenseRecord.id, "30"), paymentRecord(expenseRecord.id, "70")))
        val requests = listOf("30", "70").map {
            ExpensePaymentCreateRequest(expenseRecord.referenceNumber, settlementInstruction, BigDecimal(it))
        }

        expensePaymentOperations.recordPayments(expenseStore, requests)

        verify(expenseStore).savePayments(drafts)
        assertEquals(2, mockingDetails(eventPublisher).invocations.size)
    }

    @Test
    fun `an empty payment collection is rejected`() {
        assertThrows(RtsGenericException::class.java) {
            expensePaymentOperations.recordPayments(expenseStore, emptyList())
        }
        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `expenses are locked in id order whatever order the payments arrive in`() {
        val firstExpense = expenseRecord(BigDecimal("100"), "EXPN01")
        val secondExpense = expenseRecord(BigDecimal("100"), "EXPN02")
        val (lowerIdExpense, higherIdExpense) = listOf(firstExpense, secondExpense).sortedBy { it.id }
        `when`(expenseStore.findExpensesByReferences(listOf(higherIdExpense.referenceNumber, lowerIdExpense.referenceNumber)))
            .thenReturn(listOf(firstExpense, secondExpense))
        `when`(expenseStore.savePayments(listOf(higherIdExpense, lowerIdExpense).map {
            ExpensePaymentDraft(it.id, BigDecimal("10.0000"), resolvedSettlement())
        })).thenReturn(listOf(higherIdExpense, lowerIdExpense).map { paymentRecord(it.id, "10") })
        `when`(expenseStore.loadForExpenses(listOf(lowerIdExpense.id, higherIdExpense.id))).thenReturn(
            ExpenseAggregate(emptyList(), listOf(lowerIdExpense, higherIdExpense), emptyList(), emptyList(), emptyList())
        )

        expensePaymentOperations.recordPayments(
            expenseStore,
            listOf(
                ExpensePaymentCreateRequest(higherIdExpense.referenceNumber, settlementInstruction, BigDecimal("10")),
                ExpensePaymentCreateRequest(lowerIdExpense.referenceNumber, settlementInstruction, BigDecimal("10"))
            )
        )

        val locksInOrder = inOrder(expenseStore)
        locksInOrder.verify(expenseStore).lockExpense(lowerIdExpense.id)
        locksInOrder.verify(expenseStore).lockExpense(higherIdExpense.id)
    }

    @Test
    fun `voiding a payment requires a reason`() {
        assertThrows(RtsGenericException::class.java) {
            expensePaymentOperations.voidPayment(expenseStore, ExpensePaymentVoidRequest("EXPY01", ""))
        }
    }

    @Test
    fun `a payment that was already voided cannot be voided again`() {
        val expenseRecord = stubExpense(amount = "100", activePayments = emptyList())
        val paymentRecord = paymentRecord(expenseRecord.id, "40")
        `when`(expenseStore.findPaymentByReference("EXPY01")).thenReturn(paymentRecord)
        `when`(expenseStore.loadForExpenses(listOf(expenseRecord.id))).thenReturn(
            ExpenseAggregate(
                batches = emptyList(),
                expenses = listOf(expenseRecord),
                payments = listOf(paymentRecord),
                paymentVoids = listOf(ExpensePaymentVoidRecord(UUID.randomUUID(), paymentRecord.id, "earlier", OffsetDateTime.now())),
                expenseVoids = emptyList()
            )
        )

        assertThrows(RtsGenericException::class.java) {
            expensePaymentOperations.voidPayment(expenseStore, ExpensePaymentVoidRequest("EXPY01", "wrong method"))
        }
        verify(expenseStore, never()).savePaymentVoid(paymentRecord.id, "wrong method")
    }

    @Test
    fun `reissuing a recorded payment posts to the payment method account snapshotted when it was recorded`() {
        val expenseRecord = expenseRecord(BigDecimal("10.0000"))
        val expensePaymentRecord = paymentRecord(expenseRecord.id, "10")
        `when`(expenseStore.findPaymentById(expensePaymentRecord.id)).thenReturn(expensePaymentRecord)
        `when`(expenseStore.findExpenseById(expenseRecord.id)).thenReturn(expenseRecord)

        expensePaymentOperations.reissuePaymentRecorded(expenseStore, expensePaymentRecord.id)

        val publishedEvent = mockingDetails(eventPublisher).invocations.single().arguments[0] as ExpensePaymentRecordedEvent
        assertEquals(snapshottedPaymentMethodAccountCode, publishedEvent.paymentMethodAccountCode)
    }

    @Test
    fun `reissuing a payment void posts to the payment method account snapshotted when the payment was recorded`() {
        val expenseRecord = expenseRecord(BigDecimal("10.0000"))
        val expensePaymentRecord = paymentRecord(expenseRecord.id, "10")
        val expensePaymentVoidRecord = ExpensePaymentVoidRecord(UUID.randomUUID(), expensePaymentRecord.id, "wrong method", OffsetDateTime.now())
        `when`(expenseStore.findPaymentVoidById(expensePaymentVoidRecord.id)).thenReturn(expensePaymentVoidRecord)
        `when`(expenseStore.findPaymentById(expensePaymentRecord.id)).thenReturn(expensePaymentRecord)
        `when`(expenseStore.findExpenseById(expenseRecord.id)).thenReturn(expenseRecord)

        expensePaymentOperations.reissuePaymentVoided(expenseStore, expensePaymentVoidRecord.id)

        val publishedEvent = mockingDetails(eventPublisher).invocations.single().arguments[0] as ExpensePaymentVoidedEvent
        assertEquals(snapshottedPaymentMethodAccountCode, publishedEvent.paymentMethodAccountCode)
    }

    @Test
    fun `voiding a payment posts to the payment method account snapshotted when it was recorded`() {
        val expenseRecord = stubExpense(amount = "100", activePayments = emptyList())
        val expensePaymentRecord = paymentRecord(expenseRecord.id, "40")
        `when`(expenseStore.findPaymentByReference("EXPY01")).thenReturn(expensePaymentRecord)
        `when`(expenseStore.loadForExpenses(listOf(expenseRecord.id))).thenReturn(
            ExpenseAggregate(emptyList(), listOf(expenseRecord), listOf(expensePaymentRecord), emptyList(), emptyList())
        )
        `when`(expenseStore.savePaymentVoid(expensePaymentRecord.id, "wrong method")).thenReturn(
            ExpensePaymentVoidRecord(UUID.randomUUID(), expensePaymentRecord.id, "wrong method", OffsetDateTime.now())
        )

        expensePaymentOperations.voidPayment(expenseStore, ExpensePaymentVoidRequest("EXPY01", "wrong method"))

        val publishedEvent = mockingDetails(eventPublisher).invocations.single().arguments[0] as ExpensePaymentVoidedEvent
        assertEquals(snapshottedPaymentMethodAccountCode, publishedEvent.paymentMethodAccountCode)
    }

    private fun resolvedSettlement() = ResolvedSettlement(settlementInstruction.paymentMethodId, "001.001", null, expenseDate)

    private fun expenseRecord(amount: BigDecimal, referenceNumber: String = "EXPN01") = ExpenseRecord(
        UUID.randomUUID(), referenceNumber, expenseTypeId, expenseAccountCode, UUID.randomUUID(), amount, expenseDate, null,
        ExpenseSourceType.ADHOC, null, UUID.randomUUID(), OffsetDateTime.now(), UUID.randomUUID()
    )

    private fun paymentRecord(expenseId: UUID, amount: String) = ExpensePaymentRecord(
        UUID.randomUUID(), expenseId, "EXPY01", settlementInstruction.paymentMethodId, snapshottedPaymentMethodAccountCode, BigDecimal(amount),
        null, OffsetDateTime.now(), OffsetDateTime.now()
    )

    private fun assertNoPaymentsSaved() {
        assertTrue(mockingDetails(expenseStore).invocations.none { it.method.name == "savePayments" })
    }

    private fun stubExpense(amount: String, activePayments: List<String>): ExpenseRecord {
        val expenseRecord = expenseRecord(BigDecimal(amount))
        `when`(expenseStore.findExpensesByReferences(listOf("EXPN01"))).thenReturn(listOf(expenseRecord))
        `when`(expenseStore.loadForExpenses(listOf(expenseRecord.id))).thenReturn(
            ExpenseAggregate(
                batches = emptyList(),
                expenses = listOf(expenseRecord),
                payments = activePayments.map { paymentRecord(expenseRecord.id, it) },
                paymentVoids = emptyList(),
                expenseVoids = emptyList()
            )
        )
        return expenseRecord
    }
}
