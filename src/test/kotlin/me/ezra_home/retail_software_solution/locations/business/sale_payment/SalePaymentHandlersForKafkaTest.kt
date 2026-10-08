package me.ezra_home.retail_software_solution.locations.business.sale_payment

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SalePaymentRecordedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SalePaymentVoidedEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class SalePaymentHandlersForKafkaTest {

    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val salePaymentHandlerForKafka = SalePaymentHandlerForKafka(
        mock(SaleDataFetcher::class.java), mock(SalePaymentRepository::class.java),
        mock(SalePaymentVoidRepository::class.java), eventPublisher
    )
    private val salePaymentVoidHandlerForKafka = SalePaymentVoidHandlerForKafka(
        mock(SaleDataFetcher::class.java), mock(SalePaymentRepository::class.java),
        mock(SalePaymentVoidRepository::class.java), eventPublisher
    )
    private val saleId = UUID.randomUUID()
    private val contactId = UUID.randomUUID()

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

    @Test
    fun `a recorded payment is posted to the account copied when it was recorded`() {
        salePaymentHandlerForKafka.publish(saleId, contactId, listOf(payment("001.099")))

        val publishedEvent = mockingDetails(eventPublisher).invocations.single().arguments[0] as SalePaymentRecordedEvent
        assertEquals("001.099", publishedEvent.payments.single().paymentMethodAccountCode)
    }

    @Test
    fun `a payment void is posted to the account copied when the payment was recorded`() {
        salePaymentVoidHandlerForKafka.publish(payment("001.099"), voidOf(), contactId)

        val publishedEvent = mockingDetails(eventPublisher).invocations.single().arguments[0] as SalePaymentVoidedEvent
        assertEquals("001.099", publishedEvent.paymentMethodAccountCode)
    }

    @Test
    fun `a payment recorded without an account code publishes neither the payment nor its void`() {
        salePaymentHandlerForKafka.publish(saleId, contactId, listOf(payment(null)))
        salePaymentVoidHandlerForKafka.publish(payment(null), voidOf(), contactId)

        verifyNoInteractions(eventPublisher)
    }

    private fun payment(paymentMethodAccountCode: String?) = SalePaymentEntity(
        saleId = saleId,
        paymentMethodId = UUID.randomUUID(),
        paymentMethodAccountCode = paymentMethodAccountCode,
        amount = BigDecimal("25.0000"),
        paymentDate = OffsetDateTime.now()
    ).apply {
        id = UUID.randomUUID()
        referenceNumber = "SPAY01"
    }

    private fun voidOf() = SalePaymentVoidEntity(UUID.randomUUID(), "wrong method").apply {
        id = UUID.randomUUID()
        createdOn = OffsetDateTime.now()
    }
}
