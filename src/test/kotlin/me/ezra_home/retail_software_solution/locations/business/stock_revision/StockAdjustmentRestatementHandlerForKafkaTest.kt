package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockMovementFetcher
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentAccountCodes
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentAccountCodesFetcher
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockAdjustmentCostRestatedEvent
import me.ezra_home.retail_software_solution.organizations.business.stock_movement_reason.api.MovementType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.util.UUID

class StockAdjustmentRestatementHandlerForKafkaTest {

    private val stockAdjustmentAccountCodesFetcher = mock(StockAdjustmentAccountCodesFetcher::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val stockAdjustmentRestatementHandlerForKafka = StockAdjustmentRestatementHandlerForKafka(
        mock(StockRevisionRepository::class.java), mock(StockMovementFetcher::class.java),
        StockAdjustmentRestatements(stockAdjustmentAccountCodesFetcher), eventPublisher
    )
    private val stockRevision = StockRevisionEntity(
        sourceType = StockRevisionSourceType.DELIVERY_LINE,
        sourceReferenceNumber = "DELN000002",
        revisedStockEntryId = UUID.randomUUID(),
        replacementStockEntryId = UUID.randomUUID(),
        reason = "Supplier corrected the invoice"
    ).also {
        it.id = UUID.randomUUID()
        it.referenceNumber = "STRV000001"
    }

    @BeforeEach
    fun setSession() {
        SessionContextProvider.setSession(
            SessionContext(
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1")
            )
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    private fun costChange(movementType: MovementType, documentReference: String, oldCost: String, newCost: String) =
        ConsumerCostChange(movementType, documentReference, null, BigDecimal(oldCost), BigDecimal(newCost))

    @Test
    fun `each adjustment's cost is totalled on its own and sales are left out`() {
        val internalUseAccounts = StockAdjustmentAccountCodes("INT", "INV")
        `when`(stockAdjustmentAccountCodesFetcher.getByReferenceNumber(listOf("ADJ1", "ADJ1", "ADJ2")))
            .thenReturn(mapOf("ADJ1" to internalUseAccounts, "ADJ2" to internalUseAccounts))

        stockAdjustmentRestatementHandlerForKafka.publish(
            stockRevision,
            listOf(
                costChange(MovementType.STOCK_ADJUSTMENT_DECREASE, "ADJ1", "3.00", "2.50"),
                costChange(MovementType.STOCK_ADJUSTMENT_DECREASE, "ADJ1", "2.00", "2.00"),
                costChange(MovementType.STOCK_ADJUSTMENT_DECREASE, "ADJ2", "10.00", "9.00"),
                costChange(MovementType.SALE, "SAL1", "75.40", "74.10"),
            )
        )

        val eventCaptor = ArgumentCaptor.forClass(Any::class.java)
        verify(eventPublisher).publishEvent(eventCaptor.capture() ?: Any())
        val restatements = (eventCaptor.value as StockAdjustmentCostRestatedEvent).restatements
        assertEquals(listOf("ADJ1", "ADJ2"), restatements.map { it.adjustmentReferenceNumber })
        assertEquals("INT", restatements.first().currentRuleDebitAccountCode)
        assertEquals(BigDecimal("5.00"), restatements.first().oldCostTotal.toBigDecimal())
        assertEquals(BigDecimal("4.50"), restatements.first().newCostTotal.toBigDecimal())
    }

    @Test
    fun `no restated adjustment cost publishes nothing`() {
        stockAdjustmentRestatementHandlerForKafka.publish(
            stockRevision, listOf(costChange(MovementType.SALE, "SAL1", "75.40", "74.10"))
        )

        verify(eventPublisher, never()).publishEvent(any(Any::class.java) ?: Any())
    }
}
