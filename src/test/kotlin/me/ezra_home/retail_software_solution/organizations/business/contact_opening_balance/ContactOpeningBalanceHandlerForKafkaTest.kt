package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance

import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ContactOpeningBalanceUpsertedEvent
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactSubledgerDirection
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.context.ApplicationEventPublisher
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

/** A replayed event must rebuild the original delta from the previous revision, or a retry double-posts or mis-signs the ledger. */
class ContactOpeningBalanceHandlerForKafkaTest {

    private val contactReferenceNumber = "CON000001"
    private val revisionsById = mutableMapOf<UUID, ContactOpeningBalanceEntity>()
    private var previousRevisionLookupCreatedOn: OffsetDateTime? = null
    private var previousRevision: ContactOpeningBalanceEntity? = null

    private val contactOpeningBalanceRepository = Proxy.newProxyInstance(
        ContactOpeningBalanceRepository::class.java.classLoader,
        arrayOf(ContactOpeningBalanceRepository::class.java)
    ) { _, method, arguments ->
        when (method.name) {
            "findById" -> Optional.ofNullable(revisionsById[arguments[0]])
            "findFirstByContactReferenceNumberAndSubledgerDirectionAndCreatedOnLessThanOrderByCreatedOnDesc" -> {
                previousRevisionLookupCreatedOn = arguments[2] as OffsetDateTime
                previousRevision
            }
            else -> throw UnsupportedOperationException(method.name)
        }
    } as ContactOpeningBalanceRepository

    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val contactOpeningBalanceHandlerForKafka =
        ContactOpeningBalanceHandlerForKafka(contactOpeningBalanceRepository, eventPublisher)

    @BeforeEach
    fun setUp() {
        SessionContextProvider.setSession(
            SessionContext(organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"))
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `a first revision replays its whole amount as a raise`() {
        val revisionRow = givenRevision("COB000001", "100.0000", OffsetDateTime.of(2026, 3, 5, 23, 30, 0, 0, ZoneOffset.UTC))

        contactOpeningBalanceHandlerForKafka.reissue(revisionRow.id!!)

        val event = publishedEvent()
        assertEquals("COB000001", event.ledgerSourceReferenceNumber)
        assertEquals(0, BigDecimal("100.0000").compareTo(event.amount))
        assertFalse(event.balanceReduced)
        assertEquals(LocalDate.of(2026, 3, 5), event.postingDate)
        assertEquals(revisionRow.createdOn, previousRevisionLookupCreatedOn)
    }

    @Test
    fun `a downward revision replays the absolute delta flagged as reduced`() {
        previousRevision = ContactOpeningBalanceEntity(contactReferenceNumber, ContactSubledgerDirection.PAYABLE, BigDecimal("130.0000"))
        val revisionRow = givenRevision("COB000002", "90.0000", OffsetDateTime.of(2026, 3, 6, 9, 0, 0, 0, ZoneOffset.UTC))

        contactOpeningBalanceHandlerForKafka.reissue(revisionRow.id!!)

        val event = publishedEvent()
        assertEquals("COB000002", event.ledgerSourceReferenceNumber)
        assertEquals(0, BigDecimal("40.0000").compareTo(event.amount))
        assertTrue(event.balanceReduced)
        assertEquals(LocalDate.of(2026, 3, 6), event.postingDate)
    }

    @Test
    fun `an unknown revision id is rejected without publishing`() {
        assertThrows(RtsGenericException::class.java) {
            contactOpeningBalanceHandlerForKafka.reissue(UUID.randomUUID())
        }
    }

    private fun givenRevision(referenceNumber: String, amount: String, createdOn: OffsetDateTime): ContactOpeningBalanceEntity {
        val revisionRow = ContactOpeningBalanceEntity(contactReferenceNumber, ContactSubledgerDirection.PAYABLE, BigDecimal(amount))
        revisionRow.id = UUID.randomUUID()
        revisionRow.referenceNumber = referenceNumber
        revisionRow.createdOn = createdOn
        revisionsById[revisionRow.id!!] = revisionRow
        return revisionRow
    }

    private fun publishedEvent(): ContactOpeningBalanceUpsertedEvent {
        val eventCaptor = ArgumentCaptor.forClass(Any::class.java)
        verify(eventPublisher).publishEvent(eventCaptor.capture())
        return eventCaptor.value as ContactOpeningBalanceUpsertedEvent
    }
}
