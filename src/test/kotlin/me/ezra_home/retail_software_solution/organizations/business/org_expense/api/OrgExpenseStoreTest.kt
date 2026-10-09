package me.ezra_home.retail_software_solution.organizations.business.org_expense.api

import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseSource

import me.ezra_home.retail_software_solution.cross_tier.expense.model.NewExpense

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ResolvedExpenseRow
import me.ezra_home.retail_software_solution.cross_tier.expense.repository.ExpensePaymentStateRepositoryBase
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeDto
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpenseBatchRepository
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpenseEntity
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpensePaymentRepository
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpensePaymentStateEntity
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpensePaymentStateRepository
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpensePaymentVoidRepository
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpenseRepository
import me.ezra_home.retail_software_solution.organizations.business.org_expense.OrgExpenseVoidRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyIterable
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class OrgExpenseStoreTest {

    private val orgExpenseRepository = mock(OrgExpenseRepository::class.java)
    private val orgExpensePaymentStateRepository = mock(OrgExpensePaymentStateRepository::class.java)

    private val orgExpenseStore = OrgExpenseStore(
        mock(OrgExpenseBatchRepository::class.java), orgExpenseRepository, mock(OrgExpensePaymentRepository::class.java),
        mock(OrgExpensePaymentVoidRepository::class.java), mock(OrgExpenseVoidRepository::class.java),
        orgExpensePaymentStateRepository
    )

    @Test
    fun `saving an org expense creates its payment state as unpaid with nothing paid`() {
        val orgExpenseEntity = OrgExpenseEntity(
            UUID.randomUUID(), "005.005", UUID.randomUUID(), BigDecimal("100.0000"), LocalDate.of(2026, 3, 10),
            null, ExpenseSourceType.ADHOC, null, UUID.randomUUID()
        ).apply {
            id = UUID.randomUUID()
            createdOn = OffsetDateTime.now()
            createdById = UUID.randomUUID()
            referenceNumber = "OXPN000001"
        }
        `when`(orgExpenseRepository.saveAll(anyIterable<OrgExpenseEntity>())).thenReturn(listOf(orgExpenseEntity))
        val expenseType = mock(ExpenseTypeDto::class.java)
        `when`(expenseType.id).thenReturn(UUID.randomUUID())
        `when`(expenseType.expenseAccountCode).thenReturn("005.005")
        val payee = mock(ContactDto::class.java)
        `when`(payee.id).thenReturn(UUID.randomUUID())

        orgExpenseStore.saveExpenses(
            listOf(
                NewExpense(
                    orgExpenseEntity.batchId, ExpenseSource(ExpenseSourceType.ADHOC, null),
                    ResolvedExpenseRow(expenseType, payee, BigDecimal("100.0000"), null, LocalDate.of(2026, 3, 10), null)
                )
            )
        )

        @Suppress("UNCHECKED_CAST")
        val savedStates = ArgumentCaptor.forClass(Iterable::class.java) as ArgumentCaptor<Iterable<OrgExpensePaymentStateEntity>>
        verify(orgExpensePaymentStateRepository).saveAll(savedStates.capture())
        val savedState = savedStates.value.single()
        assertEquals(orgExpenseEntity.id, savedState.expenseId)
        assertEquals(PaymentStatus.UNPAID, savedState.paymentStatus)
        assertEquals(0, BigDecimal.ZERO.compareTo(savedState.amountPaid))
    }

    @Test
    fun `refreshing derives status and paid amount from the active payments`() {
        val expenseDto = ExpenseDto(
            UUID.randomUUID(), "OXPN01", UUID.randomUUID(), "005.005", UUID.randomUUID(), BigDecimal("100"),
            LocalDate.of(2026, 3, 10), null, ExpenseSourceType.ADHOC, null, UUID.randomUUID(), OffsetDateTime.now(), UUID.randomUUID()
        )
        val state = OrgExpensePaymentStateEntity(expenseDto.id)
        `when`(orgExpensePaymentStateRepository.sumActivePaidByExpenseId(anyCollection())).thenReturn(
            listOf(object : ExpensePaymentStateRepositoryBase.ActivePaidAmount {
                override val expenseId = expenseDto.id
                override val amountPaid = BigDecimal("40")
            })
        )
        `when`(orgExpensePaymentStateRepository.findByExpenseIdIn(anyCollection())).thenReturn(listOf(state))

        orgExpenseStore.refreshPaymentStates(listOf(expenseDto))

        assertEquals(PaymentStatus.PARTIALLY_SETTLED, state.paymentStatus)
        assertEquals(0, BigDecimal("40").compareTo(state.amountPaid))
    }
}
