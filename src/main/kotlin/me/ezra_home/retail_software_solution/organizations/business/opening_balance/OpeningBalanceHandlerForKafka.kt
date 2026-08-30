package me.ezra_home.retail_software_solution.organizations.business.opening_balance

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.OpeningBalanceUpsertedEvent
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountDataFetcher
import me.ezra_home.retail_software_solution.organizations.business.account.api.EntryType
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Component
@TransactionalOnOrganizationSchema(readOnly = true)
class OpeningBalanceHandlerForKafka(
    private val openingBalanceRepository: OpeningBalanceRepository,
    private val accountDataFetcher: AccountDataFetcher,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = OpeningBalanceUpsertedEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        val row = openingBalanceRepository.findById(sourceDocumentId)
            .orElseThrow { RtsGenericException("Opening balance $sourceDocumentId not found") }
        val previous = openingBalanceRepository.findFirstByAccountCodeAndCreatedOnLessThanOrderByCreatedOnDesc(
            row.accountCode,
            row.requiredCreatedOn()
        )
        val delta = row.amount - (previous?.amount ?: BigDecimal.ZERO)
        if (delta.compareTo(BigDecimal.ZERO) == 0) return

        val account = accountDataFetcher.getByCode(row.accountCode)
        OpeningBalanceAccountValidator.requireLeafActive(account)
        val accountEntryType = directionFor(account.normalBalanceEntryType, delta)
        publish(row, accountEntryType, delta.abs(), DateTimes.Local.atOrganizationZone(row.requiredCreatedOn()))
    }

    fun directionFor(normalBalanceEntryType: EntryType, delta: BigDecimal): EntryType {
        val isCorrectionReducingBalance = delta < BigDecimal.ZERO
        return if (isCorrectionReducingBalance) normalBalanceEntryType.opposite() else normalBalanceEntryType
    }

    fun publish(openingBalance: OpeningBalanceEntity, accountEntryType: EntryType, amount: BigDecimal, postingDate: LocalDate) {
        eventPublisher.publishEvent(
            OpeningBalanceUpsertedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.OrgLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema()
                ),
                timestamp = Instant.now(),
                correlationId = null,
                openingBalanceId = requireNotNull(openingBalance.id),
                ledgerSourceReferenceNumber = openingBalance.requiredReference(),
                accountCode = openingBalance.accountCode,
                accountEntryType = accountEntryType,
                amount = amount,
                postingDate = postingDate
            )
        )
    }
}
