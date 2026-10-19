package me.ezra_home.retail_software_solution.locations.business.stock_adjustment

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockAdjustmentRecordedEvent
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class StockAdjustmentHandlerForKafka(
    private val stockAdjustmentRepository: StockAdjustmentRepository,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = StockAdjustmentRecordedEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        val stockAdjustment = stockAdjustmentRepository.findById(sourceDocumentId)
            .orElseThrow { RtsGenericException("Stock adjustment $sourceDocumentId not found") }
        publish(stockAdjustment)
    }

    // A zero-cost adjustment moves no value, and the ledger rejects zero-amount entries.
    fun publish(stockAdjustment: StockAdjustmentEntity) {
        if (stockAdjustment.totalCost.signum() == 0) return
        val ledgerAccounts = StockAdjustmentCategoryRules.ledgerAccounts(stockAdjustment.category, stockAdjustment.direction)
        eventPublisher.publishEvent(
            StockAdjustmentRecordedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = SessionContextProvider.getLocationSchema()
                ),
                timestamp = Instant.now(),
                correlationId = null,
                stockAdjustmentId = requireNotNull(stockAdjustment.id),
                ledgerSourceReferenceNumber = stockAdjustment.requiredReference(),
                debitAccountCode = ledgerAccounts.debit.code,
                creditAccountCode = ledgerAccounts.credit.code,
                totalCost = stockAdjustment.totalCost,
                postingDate = DateTimes.Local.atOrganizationZone(stockAdjustment.requiredCreatedOn())
            )
        )
    }
}
