package me.ezra_home.retail_software_solution.organizations.business.account.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class EntryTypeSignTest {

    private val ten = BigDecimal("10.00")

    @Test
    fun `an entry on the normal side is positive and one on the other side is negative`() {
        assertEquals(ten, EntryType.DEBIT.amountTowards(EntryType.DEBIT, ten))
        assertEquals(ten.negate(), EntryType.CREDIT.amountTowards(EntryType.DEBIT, ten))
        assertEquals(ten, EntryType.CREDIT.amountTowards(EntryType.CREDIT, ten))
        assertEquals(ten.negate(), EntryType.DEBIT.amountTowards(EntryType.CREDIT, ten))
    }

    @Test
    fun `an account type takes the side from its normal balance, contra accounts included`() {
        assertEquals(ten, AccountType.ASSET.amountInNormalDirection(EntryType.DEBIT, ten))
        assertEquals(ten.negate(), AccountType.ASSET.amountInNormalDirection(EntryType.CREDIT, ten))
        assertEquals(ten, AccountType.LIABILITY.amountInNormalDirection(EntryType.CREDIT, ten))
        assertEquals(ten, AccountType.ASSET_CONTRA.amountInNormalDirection(EntryType.CREDIT, ten))
        assertEquals(ten.negate(), AccountType.EXPENSE_CONTRA.amountInNormalDirection(EntryType.DEBIT, ten))
    }

    @Test
    fun `revenue and expense entries move profit and balance sheet entries do not`() {
        assertEquals(ten.negate(), AccountType.EXPENSE.profitChangeOf(EntryType.DEBIT, ten))
        assertEquals(ten, AccountType.EXPENSE.profitChangeOf(EntryType.CREDIT, ten))
        assertEquals(ten, AccountType.REVENUE.profitChangeOf(EntryType.CREDIT, ten))
        assertEquals(ten.negate(), AccountType.REVENUE_CONTRA.profitChangeOf(EntryType.DEBIT, ten))
        assertEquals(ten, AccountType.EXPENSE_CONTRA.profitChangeOf(EntryType.CREDIT, ten))
        assertEquals(BigDecimal.ZERO, AccountType.ASSET.profitChangeOf(EntryType.DEBIT, ten))
        assertEquals(BigDecimal.ZERO, AccountType.LIABILITY.profitChangeOf(EntryType.CREDIT, ten))
        assertEquals(BigDecimal.ZERO, AccountType.EQUITY.profitChangeOf(EntryType.CREDIT, ten))
    }

    @Test
    fun `an account tree node signs the same way as its account type`() {
        val node = AccountTreeNode(
            "001.001.003", "Inventory", null, AccountCategory.ASSET, false, EntryType.DEBIT, true
        )

        assertEquals(ten, node.amountInNormalDirection(EntryType.DEBIT, ten))
        assertEquals(ten.negate(), node.amountInNormalDirection(EntryType.CREDIT, ten))
    }
}
