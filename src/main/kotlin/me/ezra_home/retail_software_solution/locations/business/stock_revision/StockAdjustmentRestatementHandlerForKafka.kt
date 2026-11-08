package me.ezra_home.retail_software_solution.locations.business.stock_revision

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.stock.api.read.StockMovementFetcher
import me.ezra_home.retail_software_solution.locations.business.stock_adjustment.api.StockAdjustmentAccountCodesFetcher
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockAdjustmentCostRestatedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.StockAdjustmentCostRestatementDto
import me.ezra_home.retail_software_solution.util.business.Money
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class StockAdjustmentRestatementHandlerForKafka(
    stockRevisionRepository: StockRevisionRepository,
    private val stockMovementFetcher: StockMovementFetcher,
    private val stockAdjustmentAccountCodesFetcher: StockAdjustmentAccountCodesFetcher,
    private val eventPublisher: ApplicationEventPublisher
) : StockRevisionEventHandler(stockRevisionRepository) {

    override val eventType = StockAdjustmentCostRestatedEvent::class

    override fun republish(stockRevision: StockRevisionEntity) {
        val revisionMovements = stockMovementFetcher.findOfRevision(stockRevision.requiredReference())
        publish(stockRevision, ConsumerCostChange.ofRevisionMovements(revisionMovements))
    }

    fun publish(stockRevision: StockRevisionEntity, consumerCostChanges: List<ConsumerCostChange>) {
        val adjustmentCostChanges = RestatableConsumers.stockAdjustmentsOf(consumerCostChanges)
        val restatements = restatementsOf(adjustmentCostChanges)
        if (restatements.isEmpty()) return
        eventPublisher.publishEvent(
            StockAdjustmentCostRestatedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = sourceContext(),
                timestamp = Instant.now(),
                correlationId = null,
                stockRevisionId = stockRevision.id!!,
                stockRevisionReferenceNumber = stockRevision.requiredReference(),
                restatements = restatements,
                postingDate = postingDate()
            )
        )
    }

    private fun restatementsOf(adjustmentCostChanges: List<ConsumerCostChange>): List<StockAdjustmentCostRestatementDto> {
        if (adjustmentCostChanges.isEmpty()) return emptyList()
        val accountCodesByReferenceNumber = stockAdjustmentAccountCodesFetcher
            .getByReferenceNumber(adjustmentCostChanges.map { it.documentReference })
        return adjustmentCostChanges
            .groupBy { it.documentReference }
            .map { (adjustmentReferenceNumber, costChanges) ->
                val currentRuleAccountCodes = accountCodesByReferenceNumber.getValue(adjustmentReferenceNumber)
                StockAdjustmentCostRestatementDto(
                    adjustmentReferenceNumber = adjustmentReferenceNumber,
                    currentRuleDebitAccountCode = currentRuleAccountCodes.debitAccountCode,
                    currentRuleCreditAccountCode = currentRuleAccountCodes.creditAccountCode,
                    oldCostTotal = Money.of(costChanges.sumOf { it.oldCost }),
                    newCostTotal = Money.of(costChanges.sumOf { it.newCost }),
                )
            }
            .filter { it.oldCostTotal.toBigDecimal().compareTo(it.newCostTotal.toBigDecimal()) != 0 }
    }
}
