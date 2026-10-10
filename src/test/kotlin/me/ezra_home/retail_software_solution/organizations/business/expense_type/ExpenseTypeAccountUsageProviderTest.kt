package me.ezra_home.retail_software_solution.organizations.business.expense_type

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountUsageType
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ExpenseTypeAccountUsageProviderTest {

    private val expenseTypeRepository = mock(ExpenseTypeRepository::class.java)
    private val expenseTypeAccountUsageProvider = ExpenseTypeAccountUsageProvider(expenseTypeRepository)

    @Test
    fun `reports the names of every expense type backed by the account`() {
        val shippingExpenseType = ExpenseTypeEntity(
            name = "Shipping",
            expenseAccountCode = "005.100",
            eligiblePayeeTypes = setOf(ContactType.SUPPLIER),
            eligibleSourceTypes = setOf(ExpenseSourceType.ADHOC)
        )
        `when`(expenseTypeRepository.findAllByExpenseAccountCode("005.100")).thenReturn(listOf(shippingExpenseType))

        assertEquals(AccountUsageType.EXPENSE_TYPE, expenseTypeAccountUsageProvider.usageType)
        assertEquals(listOf("Shipping"), expenseTypeAccountUsageProvider.getReferences("005.100"))
        assertEquals(emptyList<String>(), expenseTypeAccountUsageProvider.getReferences("005.200"))
    }
}
