package me.ezra_home.retail_software_solution.locations.business.expense.api

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ResolvedExpenseRow
import me.ezra_home.retail_software_solution.locations.business.expense.ExpenseBatchRepository
import me.ezra_home.retail_software_solution.locations.business.expense.ExpenseEntity
import me.ezra_home.retail_software_solution.locations.business.expense.ExpensePaymentRepository
import me.ezra_home.retail_software_solution.locations.business.expense.ExpensePaymentStateEntity
import me.ezra_home.retail_software_solution.cross_tier.expense.repository.ExpensePaymentStateRepositoryBase
import me.ezra_home.retail_software_solution.locations.business.expense.ExpensePaymentStateRepository
import me.ezra_home.retail_software_solution.locations.business.expense.ExpensePaymentVoidRepository
import me.ezra_home.retail_software_solution.locations.business.expense.ExpenseRepository
import me.ezra_home.retail_software_solution.locations.business.expense.ExpenseVoidRepository
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class LocationExpenseStoreTest {

    private val expenseRepository = mock(ExpenseRepository::class.java)
    private val expensePaymentStateRepository = mock(ExpensePaymentStateRepository::class.java)

    private val locationExpenseStore = LocationExpenseStore(
        mock(ExpenseBatchRepository::class.java), expenseRepository, mock(ExpensePaymentRepository::class.java),
        mock(ExpensePaymentVoidRepository::class.java), mock(ExpenseVoidRepository::class.java),
        expensePaymentStateRepository, mock(EntityAdvisoryLock::class.java)
    )

    @Test
    fun `saving an expense creates its payment state as unpaid with nothing paid`() {
        val expenseEntity = persistedExpenseEntity()
        `when`(expenseRepository.save(any(ExpenseEntity::class.java))).thenReturn(expenseEntity)

        locationExpenseStore.saveExpense(expenseEntity.batchId, ExpenseSourceType.ADHOC, null, resolvedExpenseRow())

        val savedState = ArgumentCaptor.forClass(ExpensePaymentStateEntity::class.java)
        verify(expensePaymentStateRepository).save(savedState.capture())
        assertEquals(expenseEntity.id, savedState.value.expenseId)
        assertEquals(PaymentStatus.UNPAID, savedState.value.paymentStatus)
        assertEquals(0, BigDecimal.ZERO.compareTo(savedState.value.amountPaid))
    }

    @Test
    fun `refreshing derives each expense's status and paid amount from its active payments`() {
        val partiallyPaidExpense = expenseRecord("100")
        val fullyPaidExpense = expenseRecord("50")
        val partiallyPaidState = ExpensePaymentStateEntity(partiallyPaidExpense.id)
        val fullyPaidState = ExpensePaymentStateEntity(fullyPaidExpense.id)
        stubActivePaid(partiallyPaidExpense.id to "40", fullyPaidExpense.id to "50")
        stubStates(partiallyPaidState, fullyPaidState)

        locationExpenseStore.refreshPaymentStates(listOf(partiallyPaidExpense, fullyPaidExpense))

        assertEquals(PaymentStatus.PARTIALLY_SETTLED, partiallyPaidState.paymentStatus)
        assertEquals(0, BigDecimal("40").compareTo(partiallyPaidState.amountPaid))
        assertEquals(PaymentStatus.FULLY_SETTLED, fullyPaidState.paymentStatus)
        assertEquals(0, BigDecimal("50").compareTo(fullyPaidState.amountPaid))
    }

    @Test
    fun `an expense whose payments are all voided drops back to unpaid`() {
        val expenseRecord = expenseRecord("100")
        val paidState = ExpensePaymentStateEntity(expenseRecord.id).apply {
            paymentStatus = PaymentStatus.FULLY_SETTLED
            amountPaid = BigDecimal("100")
        }
        stubActivePaid()
        stubStates(paidState)

        locationExpenseStore.refreshPaymentStates(listOf(expenseRecord))

        assertEquals(PaymentStatus.UNPAID, paidState.paymentStatus)
        assertEquals(0, BigDecimal.ZERO.compareTo(paidState.amountPaid))
    }

    @Test
    fun `refreshing nothing does not touch the database`() {
        locationExpenseStore.refreshPaymentStates(emptyList())

        verify(expensePaymentStateRepository, never()).findByExpenseIdIn(anyCollection())
    }

    private fun stubActivePaid(vararg activePaidByExpenseId: Pair<UUID, String>) {
        val activePaidAmounts = activePaidByExpenseId.map { (expenseId, amount) ->
            object : ExpensePaymentStateRepositoryBase.ActivePaidAmount {
                override val expenseId = expenseId
                override val amountPaid = BigDecimal(amount)
            }
        }
        `when`(expensePaymentStateRepository.sumActivePaidByExpenseId(anyCollection())).thenReturn(activePaidAmounts)
    }

    private fun stubStates(vararg states: ExpensePaymentStateEntity) {
        `when`(expensePaymentStateRepository.findByExpenseIdIn(anyCollection())).thenReturn(states.toList())
    }

    private fun expenseRecord(amount: String) = ExpenseRecord(
        UUID.randomUUID(), "EXPN01", UUID.randomUUID(), "005.005", UUID.randomUUID(), BigDecimal(amount),
        LocalDate.of(2026, 3, 10), null, ExpenseSourceType.ADHOC, null, UUID.randomUUID(), OffsetDateTime.now(), UUID.randomUUID()
    )

    private fun persistedExpenseEntity() = ExpenseEntity(
        expenseTypeId = UUID.randomUUID(),
        expenseAccountCode = "005.005",
        payeeContactId = UUID.randomUUID(),
        amount = BigDecimal("100.0000"),
        expenseDate = LocalDate.of(2026, 3, 10),
        description = null,
        sourceType = ExpenseSourceType.ADHOC,
        sourceReference = null,
        batchId = UUID.randomUUID()
    ).apply {
        id = UUID.randomUUID()
        createdOn = OffsetDateTime.now()
        createdById = UUID.randomUUID()
        referenceNumber = "EXPN000001"
    }

    private fun resolvedExpenseRow(): ResolvedExpenseRow {
        val expenseType = mock(ExpenseTypeDto::class.java)
        `when`(expenseType.id).thenReturn(UUID.randomUUID())
        `when`(expenseType.expenseAccountCode).thenReturn("005.005")
        val payee = mock(ContactDto::class.java)
        `when`(payee.id).thenReturn(UUID.randomUUID())
        return ResolvedExpenseRow(expenseType, payee, BigDecimal("100.0000"), null, LocalDate.of(2026, 3, 10), null)
    }
}
