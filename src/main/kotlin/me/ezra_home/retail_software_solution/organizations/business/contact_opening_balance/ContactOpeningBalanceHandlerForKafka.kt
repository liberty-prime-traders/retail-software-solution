package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ContactOpeningBalanceUpsertedEvent
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Component
class ContactOpeningBalanceHandlerForKafka(
    private val contactOpeningBalanceRepository: ContactOpeningBalanceRepository,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = ContactOpeningBalanceUpsertedEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun reissue(sourceDocumentId: UUID) {
        val revisionRow = contactOpeningBalanceRepository.findById(sourceDocumentId)
            .orElseThrow { RtsGenericException("Contact opening balance $sourceDocumentId not found") }
        val previousRevisionRow = contactOpeningBalanceRepository
            .findFirstByContactReferenceNumberAndSubledgerDirectionAndCreatedOnLessThanOrderByCreatedOnDesc(
                revisionRow.contactReferenceNumber,
                revisionRow.subledgerDirection,
                revisionRow.requiredCreatedOn()
            )
        val signedDelta = revisionRow.amount - (previousRevisionRow?.amount ?: BigDecimal.ZERO)
        publish(revisionRow, signedDelta, DateTimes.Local.atOrganizationZone(revisionRow.requiredCreatedOn()))
    }

    fun publish(contactOpeningBalance: ContactOpeningBalanceEntity, signedDelta: BigDecimal, postingDate: LocalDate) {
        eventPublisher.publishEvent(
            ContactOpeningBalanceUpsertedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.OrgLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema()
                ),
                timestamp = Instant.now(),
                correlationId = null,
                contactOpeningBalanceId = requireNotNull(contactOpeningBalance.id),
                ledgerSourceReferenceNumber = contactOpeningBalance.requiredReference(),
                contactReferenceNumber = contactOpeningBalance.contactReferenceNumber,
                subledgerDirection = contactOpeningBalance.subledgerDirection,
                amount = signedDelta.abs(),
                balanceReduced = signedDelta < BigDecimal.ZERO,
                postingDate = postingDate
            )
        )
    }
}
