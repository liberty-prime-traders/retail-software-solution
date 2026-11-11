package me.ezra_home.retail_software_solution.locations.business.stock_transfer

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockTransferDispatchedEvent
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferOrderDomainDto
import me.ezra_home.retail_software_solution.organizations.business.stock_transfer.api.StockTransferStatus
import me.ezra_home.retail_software_solution.util.business.Money
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class StockTransferDispatchedHandlerForKafkaTest {

    private val orderId = UUID.randomUUID()
    private val sourceLocationId = UUID.randomUUID()

    private val stockTransferSchemaGateway = mock(StockTransferSchemaGateway::class.java)
    private val stockTransferOrderDataFetcher = mock(StockTransferOrderDataFetcher::class.java)
    private val locationService = mock(LocationService::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val stockTransferDispatchedHandlerForKafka = StockTransferDispatchedHandlerForKafka(
        stockTransferSchemaGateway, stockTransferOrderDataFetcher, locationService, eventPublisher
    )

    @BeforeEach
    fun setUpSession() {
        SessionContextProvider.setSession(
            SessionContext(
                organization = OrgSession(UUID.randomUUID(), "org-a", "America/Chicago"),
                location = LocationSession(UUID.randomUUID(), "loc-source", "America/Chicago")
            )
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    private fun publishedEvent(): StockTransferDispatchedEvent {
        val captor = ArgumentCaptor.forClass(Any::class.java)
        verify(eventPublisher).publishEvent(captor.capture())
        return captor.value as StockTransferDispatchedEvent
    }

    @Test
    fun `a reissued dispatched event is dated by the dispatch in the organization's timezone, not the reissue day`() {
        // 03:30 UTC on 2 November is still 1 November in Chicago (CST, UTC-6)
        val dispatchedAt = OffsetDateTime.of(2026, 11, 2, 3, 30, 0, 0, ZoneOffset.UTC)
        `when`(stockTransferOrderDataFetcher.getById(orderId)).thenReturn(
            StockTransferOrderDomainDto(
                id = orderId,
                referenceNumber = "ORD-1",
                sourceLocationId = sourceLocationId,
                destinationLocationId = UUID.randomUUID(),
                status = StockTransferStatus.DISPATCHED,
                notes = null,
                lineCount = 1,
                totalDispatchedCost = BigDecimal("10.0000"),
                dispatchedAt = dispatchedAt,
                dispatchedByName = null,
                createdById = UUID.randomUUID(),
                createdOn = dispatchedAt
            )
        )
        `when`(locationService.getSchemaByLocationId(sourceLocationId)).thenReturn("loc-source")
        `when`(stockTransferSchemaGateway.readDispatchAndLines("ORD-1")).thenReturn(
            DispatchWithLines(
                StockTransferDispatchEntity("ORD-1").also { it.referenceNumber = "DISPATCH-1" },
                emptyList()
            )
        )

        stockTransferDispatchedHandlerForKafka.reissue(orderId)

        val event = publishedEvent()
        assertEquals(LocalDate.of(2026, 11, 1), event.postingDate)
        assertEquals("DISPATCH-1", event.dispatchReferenceNumber)
        assertEquals(Money.of(BigDecimal("10.0000")), event.totalCost)
    }
}
