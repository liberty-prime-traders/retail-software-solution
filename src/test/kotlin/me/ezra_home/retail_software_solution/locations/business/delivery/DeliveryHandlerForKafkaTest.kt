package me.ezra_home.retail_software_solution.locations.business.delivery

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.purchase.api.DeliveryHandlerForPurchase
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDeliveryContext
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDeliveryLineStatus
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseLineDto
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import me.ezra_home.retail_software_solution.util.quantity.ExactQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class DeliveryHandlerForKafkaTest {

    private val deliveryId = UUID.randomUUID()
    private val purchaseId = UUID.randomUUID()
    private val purchaseLineId = UUID.randomUUID()
    private val unitId = UUID.randomUUID()
    private val delivery = PurchaseDeliveryEntity(
        purchaseId = purchaseId,
        deliveredAt = OffsetDateTime.parse("2026-01-01T00:00:00Z")
    ).also {
        it.id = deliveryId
        it.referenceNumber = "DEL000001"
    }
    private val revisedLine = deliveryLine("DELN000001", PurchaseDeliveryLineStatus.REVISED)
    private val replacementLine = deliveryLine("DELN000002", PurchaseDeliveryLineStatus.ACTIVE)
    private val purchaseDeliveryRepository = mock(PurchaseDeliveryRepository::class.java)
    private val purchaseDeliveryLineRepository = mock(PurchaseDeliveryLineRepository::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val deliveryHandlerForPurchase = mock(DeliveryHandlerForPurchase::class.java)
    private val deliveryHandlerForKafka = DeliveryHandlerForKafka(
        purchaseDeliveryRepository,
        purchaseDeliveryLineRepository,
        eventPublisher,
        deliveryHandlerForPurchase
    )

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

    private fun deliveryLine(referenceNumber: String, status: PurchaseDeliveryLineStatus) = PurchaseDeliveryLineEntity(
        purchaseDeliveryId = deliveryId,
        purchaseLineId = purchaseLineId,
        userProvidedQuantityDelivered = BigDecimal("2"),
        unitCost = BigDecimal("5.00"),
        unitId = unitId,
        storageUnitsPerUserProvidedUnit = 1L,
        status = status,
    ).also {
        it.id = UUID.randomUUID()
        it.referenceNumber = referenceNumber
    }

    @Test
    fun `a reissued delivery carries its active lines only`() {
        `when`(purchaseDeliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery))
        `when`(purchaseDeliveryLineRepository.findByPurchaseDeliveryIdIn(listOf(deliveryId)))
            .thenReturn(listOf(revisedLine, replacementLine))
        `when`(deliveryHandlerForPurchase.getDeliveryContext(purchaseId)).thenReturn(
            PurchaseDeliveryContext(
                purchaseId = purchaseId,
                purchaseReferenceNumber = "PUR000001",
                supplierId = UUID.randomUUID(),
                purchaseLineById = mapOf(
                    purchaseLineId to PurchaseLineDto(
                        id = purchaseLineId,
                        purchaseId = purchaseId,
                        locationProductId = UUID.randomUUID(),
                        unitCost = BigDecimal("5.00"),
                        storageUnitsPerUserProvidedUnit = 1L,
                        unitId = unitId,
                        storageQuantityExpected = ExactQuantity.ofStorage(BigDecimal("2")),
                        storageQuantityRemaining = ExactQuantity.ZERO
                    )
                )
            )
        )

        deliveryHandlerForKafka.reissue(deliveryId)

        val eventCaptor = ArgumentCaptor.forClass(Any::class.java)
        verify(eventPublisher).publishEvent(eventCaptor.capture() ?: Any())
        val purchaseDeliveredEvent = eventCaptor.value as PurchaseDeliveredEvent
        assertEquals(listOf("DELN000002"), purchaseDeliveredEvent.lines.map { it.lineReferenceNumber })
        assertEquals(0, BigDecimal("10.00").compareTo(purchaseDeliveredEvent.deliveryTotal.toBigDecimal()))
    }
}
