package me.ezra_home.retail_software_solution.organizations.business.ledger

import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockAdjustmentCostRestatedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockAdjustmentCostRestatementDto
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.helpers.LedgerPostingGate
import me.ezra_home.retail_software_solution.organizations.business.ledger.helpers.PostedStockAdjustmentAccounts
import me.ezra_home.retail_software_solution.organizations.business.ledger.helpers.PostedStockAdjustmentAccountsReader
import me.ezra_home.retail_software_solution.organizations.business.ledger.processors.StockAdjustmentRestatementAccountingProcessor
import me.ezra_home.retail_software_solution.util.business.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class StockAdjustmentRestatementEntriesTest {

    private val postedStockAdjustmentAccountsReader = mock(PostedStockAdjustmentAccountsReader::class.java)
    private val stockAdjustmentRestatementAccountingProcessor = StockAdjustmentRestatementAccountingProcessor(
        mock(LedgerPostingGate::class.java), postedStockAdjustmentAccountsReader, mock(LedgerPostingService::class.java)
    )

    // The rule accounts are deliberately different from the posted ones: only a posting with no entries uses them.
    private fun restatement(
        adjustmentReferenceNumber: String, oldCostTotal: String, newCostTotal: String
    ) = StockAdjustmentCostRestatementDto(
        adjustmentReferenceNumber, "RULE-DR", "RULE-CR",
        Money.of(BigDecimal(oldCostTotal)), Money.of(BigDecimal(newCostTotal))
    )

    private fun givenPosted(vararg accountsByAdjustment: Pair<String, PostedStockAdjustmentAccounts>) {
        `when`(postedStockAdjustmentAccountsReader.getByAdjustmentReferenceNumber(accountsByAdjustment.map { it.first }.toSet()))
            .thenReturn(accountsByAdjustment.toMap())
    }

    private fun entriesOf(vararg restatements: StockAdjustmentCostRestatementDto) =
        stockAdjustmentRestatementAccountingProcessor.prepareLedgerRequest(
            StockAdjustmentCostRestatedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(orgSchema = "org-a", locationSchema = "loc-1"),
                timestamp = Instant.now(),
                correlationId = null,
                stockRevisionId = UUID.randomUUID(),
                stockRevisionReferenceNumber = "STRV000001",
                restatements = restatements.toList(),
                postingDate = LocalDate.parse("2026-11-08")
            )
        ).entries.map { Triple(it.accountCode, it.entryType, it.amount.toBigDecimal()) }

    @Test
    fun `a cost that rises posts the adjustment's own legs for the increase`() {
        givenPosted("ADJ1" to PostedStockAdjustmentAccounts("EXP", "INV"))
        assertEquals(
            listOf(
                Triple("EXP", EntryType.DEBIT, BigDecimal("1.50")),
                Triple("INV", EntryType.CREDIT, BigDecimal("1.50")),
            ),
            entriesOf(restatement("ADJ1", "5.00", "6.50"))
        )
    }

    @Test
    fun `a cost that falls posts the legs reversed`() {
        givenPosted("ADJ1" to PostedStockAdjustmentAccounts("EXP", "INV"))
        assertEquals(
            listOf(
                Triple("EXP", EntryType.CREDIT, BigDecimal("0.50")),
                Triple("INV", EntryType.DEBIT, BigDecimal("0.50")),
            ),
            entriesOf(restatement("ADJ1", "5.00", "4.50"))
        )
    }

    @Test
    fun `restatements of several adjustment types are netted by account`() {
        givenPosted(
            "ADJ1" to PostedStockAdjustmentAccounts("EXP", "INV"),
            "ADJ2" to PostedStockAdjustmentAccounts("SHR", "INV")
        )
        assertEquals(
            listOf(
                Triple("EXP", EntryType.CREDIT, BigDecimal("0.50")),
                Triple("INV", EntryType.DEBIT, BigDecimal("0.20")),
                Triple("SHR", EntryType.DEBIT, BigDecimal("0.30")),
            ),
            entriesOf(
                restatement("ADJ1", "5.00", "4.50"),
                restatement("ADJ2", "2.00", "2.30")
            )
        )
    }

    @Test
    fun `an adjustment that posted nothing uses the current rule accounts`() {
        `when`(postedStockAdjustmentAccountsReader.getByAdjustmentReferenceNumber(setOf("ADJ1", "ADJ2")))
            .thenReturn(mapOf("ADJ1" to PostedStockAdjustmentAccounts("EXP", "INV")))

        assertEquals(
            listOf(
                Triple("EXP", EntryType.DEBIT, BigDecimal("1.00")),
                Triple("RULE-CR", EntryType.CREDIT, BigDecimal("3.00")),
                Triple("RULE-DR", EntryType.DEBIT, BigDecimal("3.00")),
                Triple("INV", EntryType.CREDIT, BigDecimal("1.00")),
            ).sortedBy { it.first },
            entriesOf(restatement("ADJ1", "5.00", "6.00"), restatement("ADJ2", "0.00", "3.00"))
                .sortedBy { it.first }
        )
    }
}
