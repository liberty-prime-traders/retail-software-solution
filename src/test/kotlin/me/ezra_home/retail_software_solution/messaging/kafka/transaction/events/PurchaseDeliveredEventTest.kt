package me.ezra_home.retail_software_solution.messaging.kafka.transaction.events

import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

@WithDefaultOrganizationSession
class PurchaseDeliveredEventTest {

    @Test
    fun `the delivery total is the value the producer computed, whatever scale the reading thread has`() {
        val purchaseDeliveredEvent = PurchaseDeliveredEvent(
            eventId = UUID.randomUUID(),
            sourceContext = EventSourceContext.LocationLevel(orgSchema = "org-a", locationSchema = "loc-1"),
            timestamp = Instant.now(),
            correlationId = null,
            purchaseId = UUID.randomUUID(),
            deliveryId = UUID.randomUUID(),
            deliveryReferenceNumber = "DEL-1",
            deliveredAt = OffsetDateTime.now(),
            supplierId = UUID.randomUUID(),
            lines = listOf(
                PurchaseDeliveredLineDto(
                    deliveryLineId = UUID.randomUUID(),
                    lineReferenceNumber = "DEL-1-1",
                    locationProductId = UUID.randomUUID(),
                    userProvidedQuantityDelivered = BigDecimal("2.125"),
                    unitId = UUID.randomUUID(),
                    storageUnitsPerUnit = 1L,
                    unitCost = BigDecimal("5.00")
                )
            ),
            deliveryTotal = BigDecimal("10.63")
        )

        SessionContextProvider.setSession(
            SessionContext(organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC", moneyScale = 4))
        )

        assertEquals(BigDecimal("10.63"), purchaseDeliveredEvent.deliveryTotal)
    }
}
