package me.ezra_home.retail_software_solution.cross_tier.expense.operation

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseSubmission
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseRowCommand
import me.ezra_home.retail_software_solution.cross_tier.expense.model.PaymentInstruction
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeDto
import me.ezra_home.retail_software_solution.organizations.business.expense_type.api.ExpenseTypeService
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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

class ExpenseRowResolverTest {

    private val expenseTypeService = mock(ExpenseTypeService::class.java)
    private val contactService = mock(ContactService::class.java)
    private val paymentMethodService = mock(PaymentMethodService::class.java)
    private val fiscalPeriodService = mock(FiscalPeriodService::class.java)
    private val expenseRowResolver = ExpenseRowResolver(expenseTypeService, contactService, paymentMethodService, fiscalPeriodService)

    private val batchExpenseDate = LocalDate.of(2026, 3, 10)
    private val supplierContact = contactDto(setOf(ContactType.SUPPLIER))
    private val employeeContact = contactDto(setOf(ContactType.EMPLOYEE))
    private val freightExpenseType = expenseTypeDto(
        eligiblePayeeTypes = setOf(ContactType.SUPPLIER, ContactType.SERVICE_PROVIDER),
        eligibleSourceTypes = setOf(ExpenseSourceType.ADHOC, ExpenseSourceType.PURCHASE)
    )

    @Test
    fun `a payee sharing any contact type with the expense type is accepted`() {
        stub(freightExpenseType, supplierContact)

        val resolvedExpenseRows = expenseRowResolver.resolve(ExpenseSourceType.ADHOC, ExpenseSubmission(batchExpenseDate, listOf(rowCommand(freightExpenseType, supplierContact))))

        assertEquals(1, resolvedExpenseRows.size)
        assertEquals(supplierContact.id, resolvedExpenseRows.single().payee.id)
    }

    @Test
    fun `a payee with no overlapping contact type is rejected`() {
        stub(freightExpenseType, employeeContact)

        assertThrows(RtsGenericException::class.java) {
            expenseRowResolver.resolve(ExpenseSourceType.ADHOC, ExpenseSubmission(batchExpenseDate, listOf(rowCommand(freightExpenseType, employeeContact))))
        }
    }

    @Test
    fun `a type that does not list the source type is rejected`() {
        stub(freightExpenseType, supplierContact)

        assertThrows(RtsGenericException::class.java) {
            expenseRowResolver.resolve(ExpenseSourceType.SALE, ExpenseSubmission(batchExpenseDate, listOf(rowCommand(freightExpenseType, supplierContact))))
        }
    }

    @Test
    fun `a row's own date wins over the batch date and each distinct date is checked once`() {
        stub(freightExpenseType, supplierContact)
        val overrideDate = LocalDate.of(2026, 2, 1)

        val resolvedExpenseRows = expenseRowResolver.resolve(ExpenseSourceType.ADHOC, ExpenseSubmission(batchExpenseDate, listOf(
                rowCommand(freightExpenseType, supplierContact, expenseDateOverride = overrideDate),
                rowCommand(freightExpenseType, supplierContact),
                rowCommand(freightExpenseType, supplierContact)
            )))

        assertEquals(overrideDate, resolvedExpenseRows[0].expenseDate)
        assertEquals(batchExpenseDate, resolvedExpenseRows[1].expenseDate)
        verify(fiscalPeriodService, times(1)).requireOpenForDate(batchExpenseDate)
        verify(fiscalPeriodService, times(1)).requireOpenForDate(overrideDate)
    }

    @Test
    fun `a settlement's payment date follows the row's effective expense date unless it declares its own`() {
        stub(freightExpenseType, supplierContact)
        val paymentMethodId = UUID.randomUUID()
        `when`(paymentMethodService.findAccountCode(paymentMethodId)).thenReturn("001.001")
        val ownPaymentDate = LocalDate.of(2026, 3, 20)

        val resolvedExpenseRows = expenseRowResolver.resolve(ExpenseSourceType.ADHOC, ExpenseSubmission(batchExpenseDate, listOf(
                rowCommand(freightExpenseType, supplierContact, settlement = PaymentInstruction(paymentMethodId)),
                rowCommand(freightExpenseType, supplierContact, settlement = PaymentInstruction(paymentMethodId, paymentDate = ownPaymentDate))
            )))

        assertEquals(batchExpenseDate, resolvedExpenseRows[0].settlement!!.paymentDate)
        assertEquals(ownPaymentDate, resolvedExpenseRows[1].settlement!!.paymentDate)
    }

    @Test
    fun `non-positive amounts are rejected before anything is looked up`() {
        assertThrows(RtsGenericException::class.java) {
            expenseRowResolver.resolve(ExpenseSourceType.ADHOC, ExpenseSubmission(batchExpenseDate, listOf(rowCommand(freightExpenseType, supplierContact, amount = BigDecimal.ZERO))))
        }
        verifyNoInteractions(expenseTypeService)
    }

    @Test
    fun `amounts are rounded to scale 4`() {
        stub(freightExpenseType, supplierContact)

        val resolvedExpenseRows = expenseRowResolver.resolve(ExpenseSourceType.ADHOC, ExpenseSubmission(batchExpenseDate, listOf(rowCommand(freightExpenseType, supplierContact, amount = BigDecimal("12.34567")))))

        assertEquals(BigDecimal("12.3457"), resolvedExpenseRows.single().amount)
    }

    @Test
    fun `an empty or oversized request is rejected`() {
        assertThrows(RtsGenericException::class.java) {
            expenseRowResolver.resolve(ExpenseSourceType.ADHOC, ExpenseSubmission(batchExpenseDate, emptyList()))
        }
        val tooManyRows = List(ExpenseRowResolver.MAXIMUM_ROWS_PER_REQUEST + 1) { rowCommand(freightExpenseType, supplierContact) }
        assertThrows(RtsGenericException::class.java) {
            expenseRowResolver.resolve(ExpenseSourceType.ADHOC, ExpenseSubmission(batchExpenseDate, tooManyRows))
        }
    }

    private fun stub(expenseType: ExpenseTypeDto, contact: ContactDto) {
        `when`(expenseTypeService.getById(expenseType.id)).thenReturn(expenseType)
        `when`(contactService.getContactById(contact.id)).thenReturn(contact)
    }

    private fun rowCommand(
        expenseType: ExpenseTypeDto,
        contact: ContactDto,
        amount: BigDecimal = BigDecimal("50"),
        expenseDateOverride: LocalDate? = null,
        settlement: PaymentInstruction? = null
    ) = ExpenseRowCommand(expenseType.id, contact.id, amount, null, expenseDateOverride, settlement)

    private fun expenseTypeDto(
        eligiblePayeeTypes: Set<ContactType>,
        eligibleSourceTypes: Set<ExpenseSourceType>
    ) = ExpenseTypeDto(
        id = UUID.randomUUID(), createdById = UUID.randomUUID(), createdOn = OffsetDateTime.now(), code = null,
        name = "Freight", expenseAccountCode = "005.005", eligiblePayeeTypes = eligiblePayeeTypes,
        eligibleSourceTypes = eligibleSourceTypes, systemDefined = false
    )

    private fun contactDto(contactTypes: Set<ContactType>) = ContactDto(
        id = UUID.randomUUID(), createdById = UUID.randomUUID(), createdOn = OffsetDateTime.now(),
        referenceNumber = "CONT01", contactTypes = contactTypes, companyName = "Acme"
    )
}
