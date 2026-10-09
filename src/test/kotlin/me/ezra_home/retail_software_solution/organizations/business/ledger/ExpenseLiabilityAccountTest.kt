package me.ezra_home.retail_software_solution.organizations.business.ledger

import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.ExpenseLiabilityAccount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExpenseLiabilityAccountTest {

    @Test
    fun `the wages expense account is owed through wages payable and every other through trade payables`() {
        assertEquals("002.003", ExpenseLiabilityAccount.forExpenseAccount("005.002"))
        assertEquals("002.001.001", ExpenseLiabilityAccount.forExpenseAccount("005.003"))
    }
}
