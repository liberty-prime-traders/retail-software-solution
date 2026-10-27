package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@WithDefaultOrganizationSession
class SaleEventAmountsTest {

    private val locationLevelContext = EventSourceContext.LocationLevel(orgSchema = "org-a", locationSchema = "loc-1")

    @Test
    fun `confirmed and voided events derive the amount owed and the gross revenue`() {
        val saleConfirmedEvent = saleConfirmedEvent(taxableAmount = Money.of(BigDecimal("90.00")), taxBilled = Money.of(BigDecimal("4.50")))
        val saleVoidedEvent = saleVoidedEvent(taxableAmount = Money.of(BigDecimal("90.00")), taxBilled = Money.of(BigDecimal("4.50")))

        assertEquals(Money.of(BigDecimal("94.50")), saleConfirmedEvent.amountOwed)
        assertEquals(Money.of(BigDecimal("100.00")), saleConfirmedEvent.grossRevenue)
        assertEquals(Money.of(BigDecimal("94.50")), saleVoidedEvent.amountOwed)
        assertEquals(Money.of(BigDecimal("100.00")), saleVoidedEvent.grossRevenue)
    }

    @Test
    fun `a receivable plus tax above the ceiling fails when the event is built, never in the ledger consumer`() {
        val taxableAtCeiling = Money.of(Decimals.MAXIMUM_TRANSACTION_AMOUNT)
        val oneCent = Money.of(BigDecimal("0.01"))

        assertThrows(RtsGenericException::class.java) { saleConfirmedEvent(taxableAmount = taxableAtCeiling, taxBilled = oneCent) }
        assertThrows(RtsGenericException::class.java) { saleVoidedEvent(taxableAmount = taxableAtCeiling, taxBilled = oneCent) }
    }

    @Test
    fun `a taxable amount plus discount above the ceiling fails when the event is built`() {
        assertThrows(RtsGenericException::class.java) {
            saleConfirmedEvent(taxableAmount = Money.of(Decimals.MAXIMUM_TRANSACTION_AMOUNT), discountTotal = Money.of(BigDecimal("0.01")))
        }
    }

    private fun saleConfirmedEvent(
        taxableAmount: Money,
        taxBilled: Money = Money.ZERO,
        discountTotal: Money = Money.of(BigDecimal("10.00")),
    ) = SaleConfirmedEvent(
        eventId = UUID.randomUUID(),
        sourceContext = locationLevelContext,
        timestamp = Instant.now(),
        correlationId = null,
        sourceDocumentId = UUID.randomUUID(),
        contactId = UUID.randomUUID(),
        saleReferenceNumber = "SALE-1",
        taxableAmount = taxableAmount,
        taxBilled = taxBilled,
        discountTotal = discountTotal,
        dateSold = LocalDate.of(2026, 1, 15),
        cogsTotal = Money.ZERO
    )

    private fun saleVoidedEvent(
        taxableAmount: Money,
        taxBilled: Money = Money.ZERO,
        discountTotal: Money = Money.of(BigDecimal("10.00")),
    ) = SaleVoidedEvent(
        eventId = UUID.randomUUID(),
        sourceContext = locationLevelContext,
        timestamp = Instant.now(),
        correlationId = null,
        sourceDocumentId = UUID.randomUUID(),
        contactId = UUID.randomUUID(),
        saleReferenceNumber = "SALE-1",
        taxableAmount = taxableAmount,
        taxBilled = taxBilled,
        discountTotal = discountTotal,
        dateSold = LocalDate.of(2026, 1, 15),
        dateVoided = LocalDate.of(2026, 1, 16),
        cogsTotal = Money.ZERO
    )
}
