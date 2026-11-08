package me.ezra_home.retail_software_solution.locations.business.stock_revision.api

import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockAdjustmentCostRestatementDto
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.PostedStockAdjustmentAccounts
import me.ezra_home.retail_software_solution.util.business.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class RevisionLedgerEntriesTest {

    private fun legs(ledgerEntryRequests: List<LedgerEntryRequest>) =
        ledgerEntryRequests.map { Triple(it.accountCode, it.entryType, it.amount.toBigDecimal()) }

    private fun amount(value: String) = BigDecimal(value)

    @Test
    fun `a dearer line debits inventory and credits trade payables by the change`() {
        assertEquals(
            listOf(
                Triple(SystemAccount.INVENTORY.code, EntryType.DEBIT, amount("20.00")),
                Triple(SystemAccount.TRADE_PAYABLES.code, EntryType.CREDIT, amount("20.00")),
            ),
            legs(RevisionLedgerEntries.purchaseDeliveryRevision(amount("100.00"), amount("120.00")))
        )
    }

    @Test
    fun `a cheaper line reverses the legs`() {
        assertEquals(
            listOf(
                Triple(SystemAccount.TRADE_PAYABLES.code, EntryType.DEBIT, amount("20.00")),
                Triple(SystemAccount.INVENTORY.code, EntryType.CREDIT, amount("20.00")),
            ),
            legs(RevisionLedgerEntries.purchaseDeliveryRevision(amount("120.00"), amount("100.00")))
        )
    }

    @Test
    fun `an unchanged cost posts nothing`() {
        assertTrue(RevisionLedgerEntries.purchaseDeliveryRevision(amount("100.00"), amount("100.00")).isEmpty())
        assertTrue(RevisionLedgerEntries.saleCogsRestatement(amount("40.00"), amount("40.00")).isEmpty())
        assertTrue(RevisionLedgerEntries.stockAdjustmentRestatement(emptyList()).isEmpty())
    }

    @Test
    fun `sales that cost more debit cost of goods sold and those that cost less credit it`() {
        assertEquals(
            listOf(
                Triple(SystemAccount.COST_OF_GOODS_SOLD.code, EntryType.DEBIT, amount("8.00")),
                Triple(SystemAccount.INVENTORY.code, EntryType.CREDIT, amount("8.00")),
            ),
            legs(RevisionLedgerEntries.saleCogsRestatement(amount("40.00"), amount("48.00")))
        )
        assertEquals(
            listOf(
                Triple(SystemAccount.INVENTORY.code, EntryType.DEBIT, amount("8.00")),
                Triple(SystemAccount.COST_OF_GOODS_SOLD.code, EntryType.CREDIT, amount("8.00")),
            ),
            legs(RevisionLedgerEntries.saleCogsRestatement(amount("48.00"), amount("40.00")))
        )
    }

    @Test
    fun `adjustments are netted by account and listed in account code order`() {
        assertEquals(
            listOf(
                Triple("EXPENSE", EntryType.CREDIT, amount("0.50")),
                Triple("INVENTORY", EntryType.DEBIT, amount("0.20")),
                Triple("SHRINKAGE", EntryType.DEBIT, amount("0.30")),
            ),
            legs(
                RevisionLedgerEntries.stockAdjustmentRestatement(
                    listOf(
                        AdjustmentCostChange("EXPENSE", "INVENTORY", amount("-0.50")),
                        AdjustmentCostChange("SHRINKAGE", "INVENTORY", amount("0.30")),
                    )
                )
            )
        )
    }

    @Test
    fun `an account whose legs cancel out posts nothing`() {
        assertEquals(
            listOf(
                Triple("EXPENSE", EntryType.DEBIT, amount("1.00")),
                Triple("SHRINKAGE", EntryType.CREDIT, amount("1.00")),
            ),
            legs(
                RevisionLedgerEntries.stockAdjustmentRestatement(
                    listOf(
                        AdjustmentCostChange("EXPENSE", "INVENTORY", amount("1.00")),
                        AdjustmentCostChange("INVENTORY", "SHRINKAGE", amount("1.00")),
                    )
                )
            )
        )
    }

    @Test
    fun `an adjustment that costs more lowers profit and one that costs less raises it`() {
        val internalUseUp = AdjustmentCostChange(
            SystemAccount.INTERNAL_USE_EXPENSE.code, SystemAccount.INVENTORY.code, amount("2.00")
        )
        val shrinkageDown = AdjustmentCostChange(
            SystemAccount.SHRINKAGE_AND_LOSSES.code, SystemAccount.INVENTORY.code, amount("-1.00")
        )

        assertEquals(0, amount("-2.00").compareTo(internalUseUp.netProfitChange()))
        assertEquals(0, amount("1.00").compareTo(shrinkageDown.netProfitChange()))
    }

    @Test
    fun `an adjustment between balance sheet accounts does not move profit`() {
        val adjustmentCostChange = AdjustmentCostChange(
            SystemAccount.INVENTORY.code, SystemAccount.OPENING_BALANCE_EQUITY.code, amount("3.00")
        )

        assertEquals(0, BigDecimal.ZERO.compareTo(adjustmentCostChange.netProfitChange()))
    }

    private fun restatement(oldCostTotal: String, newCostTotal: String) = StockAdjustmentCostRestatementDto(
        adjustmentReferenceNumber = "ADJ1",
        currentRuleDebitAccountCode = "RULE-DR",
        currentRuleCreditAccountCode = "RULE-CR",
        oldCostTotal = Money.of(amount(oldCostTotal)),
        newCostTotal = Money.of(amount(newCostTotal)),
    )

    @Test
    fun `the posted accounts are used when there are any, and the rule accounts when there are none`() {
        val posted = RevisionLedgerEntries.adjustmentCostChangeOf(
            restatement("5.00", "4.00"), PostedStockAdjustmentAccounts("POSTED-DR", "POSTED-CR")
        )
        val fallback = RevisionLedgerEntries.adjustmentCostChangeOf(restatement("5.00", "4.00"), null)

        assertEquals(AdjustmentCostChange("POSTED-DR", "POSTED-CR", amount("-1.00")), posted)
        assertEquals(AdjustmentCostChange("RULE-DR", "RULE-CR", amount("-1.00")), fallback)
    }
}
