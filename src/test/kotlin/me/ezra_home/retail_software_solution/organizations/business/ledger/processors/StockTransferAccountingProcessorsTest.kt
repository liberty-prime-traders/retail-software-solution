package me.ezra_home.retail_software_solution.organizations.business.ledger.processors

import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferCancelledEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferDispatchedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferReceiptCompletedEvent
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryGroupRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntryRequest
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerPostingService
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerSourceType
import me.ezra_home.retail_software_solution.organizations.business.ledger.helpers.LedgerPostingGate
import me.ezra_home.retail_software_solution.organizations.business.ledger.helpers.StockTransferLedgerPostings
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDomainDto
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class StockTransferAccountingProcessorsTest {

    private val sourceLocationId = UUID.randomUUID()
    private val sourceContext = EventSourceContext.LocationLevel("org-a", "loc-1")
    private val destinationContext = EventSourceContext.LocationLevel("org-a", "loc-2")
    private val postingDate = LocalDate.of(2026, 11, 10)

    private val stockTransferOrderDataFetcher = mock(StockTransferOrderDataFetcher::class.java)
    private val ledgerEntryGroupRepository = mock(LedgerEntryGroupRepository::class.java)
    private val ledgerPostingGate = mock(LedgerPostingGate::class.java)
    private val ledgerPostingService = mock(LedgerPostingService::class.java)
    private val stockTransferLedgerPostings =
        StockTransferLedgerPostings(stockTransferOrderDataFetcher, ledgerEntryGroupRepository)

    private val dispatchProcessor =
        StockTransferDispatchAccountingProcessor(stockTransferLedgerPostings, ledgerPostingGate, ledgerPostingService)
    private val receiptProcessor =
        StockTransferReceiptAccountingProcessor(stockTransferLedgerPostings, ledgerPostingGate, ledgerPostingService)
    private val cancelProcessor =
        StockTransferCancelAccountingProcessor(stockTransferLedgerPostings, ledgerPostingGate, ledgerPostingService)

    private fun givenOrderDispatchedFromSource() {
        `when`(stockTransferOrderDataFetcher.getByReferenceNumber("ORDER-1")).thenReturn(
            StockTransferOrderDomainDto(
                id = UUID.randomUUID(),
                referenceNumber = "ORDER-1",
                sourceLocationId = sourceLocationId,
                destinationLocationId = UUID.randomUUID(),
                status = StockTransferStatus.DISPATCHED,
                notes = null,
                lineCount = 1,
                totalDispatchedCost = BigDecimal("125.50"),
                dispatchedAt = null,
                dispatchedByName = null,
                createdById = UUID.randomUUID(),
                createdOn = OffsetDateTime.now()
            )
        )
    }

    private fun givenDispatchPostedAtSource(posted: Boolean) {
        `when`(
            ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceTypeAndSourceLocationId(
                "DISPATCH-1", LedgerSourceType.STOCK_TRANSFER_DISPATCH, sourceLocationId
            )
        ).thenReturn(posted)
    }

    private fun dispatchedEvent(totalCost: String = "125.50") = StockTransferDispatchedEvent(
        eventId = UUID.randomUUID(),
        sourceContext = sourceContext,
        timestamp = Instant.now(),
        correlationId = null,
        sourceDocumentId = UUID.randomUUID(),
        orderReferenceNumber = "ORDER-1",
        dispatchReferenceNumber = "DISPATCH-1",
        totalCost = Money.of(BigDecimal(totalCost)),
        postingDate = postingDate
    )

    private fun receiptCompletedEvent(totalCost: String = "125.50") = StockTransferReceiptCompletedEvent(
        eventId = UUID.randomUUID(),
        sourceContext = destinationContext,
        timestamp = Instant.now(),
        correlationId = null,
        sourceDocumentId = UUID.randomUUID(),
        orderReferenceNumber = "ORDER-1",
        dispatchReferenceNumber = "DISPATCH-1",
        totalCost = Money.of(BigDecimal(totalCost)),
        receiptReferenceNumber = "RECEIPT-1",
        postingDate = postingDate
    )

    private fun cancelledEvent(totalCost: String = "125.50") = StockTransferCancelledEvent(
        eventId = UUID.randomUUID(),
        sourceContext = sourceContext,
        timestamp = Instant.now(),
        correlationId = null,
        sourceDocumentId = UUID.randomUUID(),
        orderReferenceNumber = "ORDER-1",
        dispatchReferenceNumber = "DISPATCH-1",
        totalCost = Money.of(BigDecimal(totalCost)),
        postingDate = postingDate
    )

    private fun givenGatePosted(
        context: EventSourceContext.LocationLevel,
        sourceType: LedgerSourceType,
        posted: Boolean
    ) {
        `when`(ledgerPostingGate.isPosted(context, "DISPATCH-1", sourceType)).thenReturn(posted)
    }

    private fun summary(entries: List<LedgerEntryRequest>) =
        entries.map { Triple(it.accountCode, it.entryType, it.amount.toBigDecimal().stripTrailingZeros()) }

    @Test
    fun `dispatch moves the dispatched cost from inventory into in transit under the dispatch reference`() {

        val request = dispatchProcessor.prepareLedgerRequest(dispatchedEvent())

        assertEquals(LedgerSourceType.STOCK_TRANSFER_DISPATCH, request.sourceType)
        assertEquals("DISPATCH-1", request.sourceReferenceNumber)
        assertEquals(postingDate, request.postingDate)
        assertEquals(
            listOf(
                Triple(SystemAccount.INVENTORY_IN_TRANSIT.code, EntryType.DEBIT, BigDecimal("125.5")),
                Triple(SystemAccount.INVENTORY.code, EntryType.CREDIT, BigDecimal("125.5")),
            ),
            summary(request.entries)
        )
    }

    @Test
    fun `a dispatch that carried no cost posts nothing`() {
        assertFalse(dispatchProcessor.shouldProcess(dispatchedEvent(totalCost = "0")))
    }

    @Test
    fun `a dispatch already posted is not posted again`() {
        givenGatePosted(sourceContext, LedgerSourceType.STOCK_TRANSFER_DISPATCH, true)

        assertFalse(dispatchProcessor.shouldProcess(dispatchedEvent()))
    }

    @Test
    fun `receipt clears in transit into the destination's inventory`() {
        givenOrderDispatchedFromSource()
        givenDispatchPostedAtSource(true)

        val request = receiptProcessor.prepareLedgerRequest(receiptCompletedEvent())

        assertEquals(LedgerSourceType.STOCK_TRANSFER_RECEIPT, request.sourceType)
        assertEquals("DISPATCH-1", request.sourceReferenceNumber)
        assertEquals(
            listOf(
                Triple(SystemAccount.INVENTORY.code, EntryType.DEBIT, BigDecimal("125.5")),
                Triple(SystemAccount.INVENTORY_IN_TRANSIT.code, EntryType.CREDIT, BigDecimal("125.5")),
            ),
            summary(request.entries)
        )
    }

    @Test
    fun `a receipt whose dispatch has not posted at the source fails so the sweeper retries it`() {
        givenOrderDispatchedFromSource()
        givenDispatchPostedAtSource(false)

        val exception = assertThrows(RtsGenericException::class.java) {
            receiptProcessor.prepareLedgerRequest(receiptCompletedEvent())
        }

        assertTrue(exception.message.contains(LedgerSourceType.STOCK_TRANSFER_DISPATCH.name))
    }

    @Test
    fun `a receipt is posted once`() {
        assertTrue(receiptProcessor.shouldProcess(receiptCompletedEvent()))

        givenGatePosted(destinationContext, LedgerSourceType.STOCK_TRANSFER_RECEIPT, true)

        assertFalse(receiptProcessor.shouldProcess(receiptCompletedEvent()))
    }

    @Test
    fun `a receipt that carried no cost posts nothing`() {
        assertFalse(receiptProcessor.shouldProcess(receiptCompletedEvent(totalCost = "0")))
    }

    @Test
    fun `cancel returns in transit to the source's inventory`() {

        val request = cancelProcessor.prepareLedgerRequest(cancelledEvent())

        assertEquals(LedgerSourceType.STOCK_TRANSFER_CANCEL, request.sourceType)
        assertEquals(
            listOf(
                Triple(SystemAccount.INVENTORY.code, EntryType.DEBIT, BigDecimal("125.5")),
                Triple(SystemAccount.INVENTORY_IN_TRANSIT.code, EntryType.CREDIT, BigDecimal("125.5")),
            ),
            summary(request.entries)
        )
    }

    @Test
    fun `a cancel requires the dispatch posting and fails when the gate reports it missing`() {
        doThrow(RtsGenericException("dispatch posting missing"))
            .`when`(ledgerPostingGate)
            .requirePosted(sourceContext, "DISPATCH-1", LedgerSourceType.STOCK_TRANSFER_DISPATCH)

        assertThrows(RtsGenericException::class.java) { cancelProcessor.prepareLedgerRequest(cancelledEvent()) }
    }

    @Test
    fun `a cancel is posted once`() {
        assertTrue(cancelProcessor.shouldProcess(cancelledEvent()))

        givenGatePosted(sourceContext, LedgerSourceType.STOCK_TRANSFER_CANCEL, true)

        assertFalse(cancelProcessor.shouldProcess(cancelledEvent()))
    }

    @Test
    fun `a cancel that carried no cost posts nothing`() {
        assertFalse(cancelProcessor.shouldProcess(cancelledEvent(totalCost = "0")))
    }
}
