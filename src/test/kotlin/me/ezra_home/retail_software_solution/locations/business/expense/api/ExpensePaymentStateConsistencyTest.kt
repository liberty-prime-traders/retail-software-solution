package me.ezra_home.retail_software_solution.locations.business.expense.api

import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.api.ExpenseResponseBuilder
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseAggregate
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseBatchRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentStateRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentVoidRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseRecord
import me.ezra_home.retail_software_solution.locations.business.expense.ExpenseBatchRepository
import me.ezra_home.retail_software_solution.locations.business.expense.ExpensePaymentRepository
import me.ezra_home.retail_software_solution.locations.business.expense.ExpensePaymentStateEntity
import me.ezra_home.retail_software_solution.cross_tier.expense.repository.ExpensePaymentStateRepositoryBase
import me.ezra_home.retail_software_solution.locations.business.expense.ExpensePaymentStateRepository
import me.ezra_home.retail_software_solution.locations.business.expense.ExpensePaymentVoidRepository
import me.ezra_home.retail_software_solution.locations.business.expense.ExpenseRepository
import me.ezra_home.retail_software_solution.locations.business.expense.ExpenseVoidRepository
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeResponseDto
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeService
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

// The stored state backs both the search filters and every read. This pins the refresh against an
// independent derivation from payments and voids, and checks the builder reports the stored row as is.
class ExpensePaymentStateConsistencyTest {

    private val expenseTypeId = UUID.randomUUID()
    private val paymentMethodId = UUID.randomUUID()
    private val creatorId = UUID.randomUUID()
    private val batchRecord = ExpenseBatchRecord(UUID.randomUUID(), "EXBT01", "Batch", OffsetDateTime.now(), creatorId)

    private val expensePaymentStateRepository = mock(ExpensePaymentStateRepository::class.java)
    private val locationExpenseStore = LocationExpenseStore(
        mock(ExpenseBatchRepository::class.java), mock(ExpenseRepository::class.java), mock(ExpensePaymentRepository::class.java),
        mock(ExpensePaymentVoidRepository::class.java), mock(ExpenseVoidRepository::class.java),
        expensePaymentStateRepository, mock(EntityAdvisoryLock::class.java)
    )
    private val expenseResponseBuilder = responseBuilder()

    @BeforeEach
    fun setSession() {
        SessionContextProvider.setSession(
            SessionContext(organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"))
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `the refreshed state matches the active payments for every payment and void combination and the builder reports it unchanged`() {
        val scenarios = listOf(
            emptyList(),
            listOf(PaymentScenario("40", voided = false)),
            listOf(PaymentScenario("100", voided = false)),
            listOf(PaymentScenario("60", voided = true)),
            listOf(PaymentScenario("100", voided = true)),
            listOf(PaymentScenario("40", voided = false), PaymentScenario("60", voided = true)),
            listOf(PaymentScenario("30", voided = false), PaymentScenario("30", voided = false), PaymentScenario("40", voided = true)),
            listOf(PaymentScenario("100", voided = true), PaymentScenario("100", voided = false))
        )

        scenarios.forEach { paymentScenarios ->
            val expenseRecord = expenseRecord("100")
            val expenseAggregate = aggregate(expenseRecord, paymentScenarios)
            val storedState = refreshedState(expenseRecord, expenseAggregate)

            val expectedPaid = paymentScenarios.filterNot { it.voided }.sumOf { BigDecimal(it.amount) }
            val expectedStatus = when {
                expectedPaid.signum() == 0 -> PaymentStatus.UNPAID
                expectedPaid >= expenseRecord.amount -> PaymentStatus.FULLY_SETTLED
                else -> PaymentStatus.PARTIALLY_SETTLED
            }
            assertEquals(expectedStatus, storedState.paymentStatus, "status for $paymentScenarios")
            assertEquals(0, expectedPaid.compareTo(storedState.amountPaid), "amount paid for $paymentScenarios")

            val storedStateRecord = ExpensePaymentStateRecord(expenseRecord.id, storedState.paymentStatus, storedState.amountPaid)
            val builtSummary = expenseResponseBuilder.buildSummary(expenseAggregate.copy(paymentStates = listOf(storedStateRecord)), expenseRecord.id)
            assertEquals(storedState.paymentStatus, builtSummary.status, "built status for $paymentScenarios")
            assertEquals(0, storedState.amountPaid.compareTo(builtSummary.amountPaid), "built amount paid for $paymentScenarios")
            assertEquals(0, (expenseRecord.amount - storedState.amountPaid).compareTo(builtSummary.balanceRemaining), "balance for $paymentScenarios")
        }
    }

    private data class PaymentScenario(val amount: String, val voided: Boolean)

    // Models the sumActivePaidByExpenseId JPQL: a payment counts unless a void row points at it.
    private fun refreshedState(expenseRecord: ExpenseRecord, expenseAggregate: ExpenseAggregate): ExpensePaymentStateEntity {
        val voidedPaymentIds = expenseAggregate.paymentVoids.map { it.paymentId }.toSet()
        val activePaid = expenseAggregate.payments.filter { it.id !in voidedPaymentIds }.sumOf { it.amount }
        val activePaidAmounts = if (activePaid.signum() == 0) emptyList() else listOf(
            object : ExpensePaymentStateRepositoryBase.ActivePaidAmount {
                override val expenseId = expenseRecord.id
                override val amountPaid = activePaid
            }
        )
        val storedState = ExpensePaymentStateEntity(expenseRecord.id)
        `when`(expensePaymentStateRepository.sumActivePaidByExpenseId(anyCollection())).thenReturn(activePaidAmounts)
        `when`(expensePaymentStateRepository.findByExpenseIdIn(anyCollection())).thenReturn(listOf(storedState))

        locationExpenseStore.refreshPaymentStates(listOf(expenseRecord))
        return storedState
    }

    private fun aggregate(expenseRecord: ExpenseRecord, paymentScenarios: List<PaymentScenario>): ExpenseAggregate {
        val payments = paymentScenarios.map {
            ExpensePaymentRecord(
                UUID.randomUUID(), expenseRecord.id, "EXPY01", paymentMethodId, "001.001", BigDecimal(it.amount),
                null, OffsetDateTime.now(), OffsetDateTime.now()
            )
        }
        val paymentVoids = payments.zip(paymentScenarios).filter { (_, scenario) -> scenario.voided }.map { (payment, _) ->
            ExpensePaymentVoidRecord(UUID.randomUUID(), payment.id, "wrong", OffsetDateTime.now())
        }
        return ExpenseAggregate(listOf(batchRecord), listOf(expenseRecord), payments, paymentVoids, emptyList())
    }

    private fun expenseRecord(amount: String) = ExpenseRecord(
        UUID.randomUUID(), "EXPN01", expenseTypeId, "005.005", UUID.randomUUID(), BigDecimal(amount),
        LocalDate.of(2026, 3, 10), null, ExpenseSourceType.ADHOC, null, batchRecord.id, OffsetDateTime.now(), creatorId
    )

    private fun responseBuilder(): ExpenseResponseBuilder {
        val expenseTypeService = mock(ExpenseTypeService::class.java)
        `when`(expenseTypeService.getAll(null)).thenReturn(
            listOf(ExpenseTypeResponseDto(expenseTypeId, null, "Freight", "005.005", emptySet(), emptySet(), false))
        )
        val paymentMethodService = mock(PaymentMethodService::class.java)
        `when`(paymentMethodService.getNamesById()).thenReturn(mapOf(paymentMethodId to "Cash"))
        val userQualifier = mock(UserQualifier::class.java)
        `when`(userQualifier.getCreatorFullName(creatorId)).thenReturn("Creator")
        return ExpenseResponseBuilder(expenseTypeService, mock(ContactService::class.java), paymentMethodService, userQualifier)
    }
}
