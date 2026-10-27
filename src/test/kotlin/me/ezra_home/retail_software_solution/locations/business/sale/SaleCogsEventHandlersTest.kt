package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleConfirmedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleVoidedEvent
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class SaleCogsEventHandlersTest {

    private val saleId = UUID.randomUUID()
    private val firstProductId = UUID.randomUUID()
    private val secondProductId = UUID.randomUUID()
    private val saleRepository = mock(SaleRepository::class.java)
    private val saleLineRepository = mock(SaleLineRepository::class.java)
    private val saleVoidRepository = mock(SaleVoidRepository::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val saleDataFetcher = SaleDataFetcher(
        saleRepository = saleRepository,
        saleLineRepository = saleLineRepository,
        saleAssembler = mock(SaleAssembler::class.java),
        locationProductDataFetcher = mock(LocationProductDataFetcher::class.java),
        entityAdvisoryLock = mock(EntityAdvisoryLock::class.java),
        saleVoidRepository = saleVoidRepository,
        userQualifier = mock(UserQualifier::class.java),
    )
    private val saleConfirmedHandler = SaleConfirmedHandlerForKafka(saleRepository, saleDataFetcher, eventPublisher)
    private val saleVoidHandler = SaleVoidHandlerForKafka(saleRepository, saleVoidRepository, saleDataFetcher, eventPublisher)

    @BeforeEach
    fun setUp() {
        SessionContextProvider.setSession(
            SessionContext(
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1")
            )
        )
        val saleEntity = SaleEntity(contactId = UUID.randomUUID(), status = SaleStatus.CONFIRMED)
            .also {
                it.id = saleId
                it.referenceNumber = "SALE-1"
                it.dateSold = OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.UTC)
            }
        `when`(saleRepository.getReferenceById(saleId)).thenReturn(saleEntity)
        `when`(saleLineRepository.findBySaleId(saleId)).thenReturn(
            listOf(
                saleLine(firstProductId, quantity = "1.0000", storageUnitsPerUserProvidedUnit = 12L, cogsTotal = BigDecimal("30.0000")),
                saleLine(secondProductId, quantity = "1.0000", storageUnitsPerUserProvidedUnit = 1L, cogsTotal = null),
            )
        )
        val saleVoidEntity = mock(SaleVoidEntity::class.java)
        `when`(saleVoidEntity.requiredCreatedOn()).thenReturn(OffsetDateTime.of(2026, 1, 16, 10, 0, 0, 0, ZoneOffset.UTC))
        `when`(saleVoidRepository.findBySaleId(saleId)).thenReturn(saleVoidEntity)
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `confirmed event carries the sum of stored line costs with null treated as zero`() {
        saleConfirmedHandler.reissue(saleId)

        val event = capturePublished(SaleConfirmedEvent::class.java)
        assertEquals(Money.of(BigDecimal("30.00")), event.cogsTotal)
    }

    @Test
    fun `voided event carries the same sum of stored line costs`() {
        saleVoidHandler.reissue(saleId)

        val event = capturePublished(SaleVoidedEvent::class.java)
        assertEquals(Money.of(BigDecimal("30.00")), event.cogsTotal)
    }

    @Test
    fun `reissue reproduces the cost fields of the originally published event`() {
        val saleEntity = saleRepository.getReferenceById(saleId)
        saleConfirmedHandler.publish(saleEntity)
        saleConfirmedHandler.reissue(saleId)

        val captor = ArgumentCaptor.forClass(Any::class.java)
        verify(eventPublisher, times(2)).publishEvent(captor.capture())
        val (original, reissued) = captor.allValues.map { it as SaleConfirmedEvent }
        assertEquals(original.cogsTotal, reissued.cogsTotal)
    }

    private fun <T : Any> capturePublished(eventClass: Class<T>): T {
        val captor = ArgumentCaptor.forClass(Any::class.java)
        verify(eventPublisher).publishEvent(captor.capture())
        return eventClass.cast(captor.value)
    }

    private fun saleLine(locationProductId: UUID, quantity: String, storageUnitsPerUserProvidedUnit: Long, cogsTotal: BigDecimal?) =
        SaleLineEntity(
            saleId = saleId,
            locationProductId = locationProductId,
            userProvidedQuantity = BigDecimal(quantity),
            unitId = UUID.randomUUID(),
            lineTotal = BigDecimal("10.0000").multiply(BigDecimal(quantity)),
            defaultSalePrice = BigDecimal("10.0000"),
            storageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnit,
            cogsTotal = cogsTotal,
        )
}
