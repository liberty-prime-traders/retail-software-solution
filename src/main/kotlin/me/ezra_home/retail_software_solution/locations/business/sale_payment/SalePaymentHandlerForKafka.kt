package me.ezra_home.retail_software_solution.locations.business.sale_payment

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SalePaymentLineDto
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SalePaymentRecordedEvent
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
import me.ezra_home.retail_software_solution.util.business.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SalePaymentHandlerForKafka(
    private val saleDataFetcher: SaleDataFetcher,
    private val salePaymentRepository: SalePaymentRepository,
    private val salePaymentVoidRepository: SalePaymentVoidRepository,
    private val paymentMethodService: PaymentMethodService,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = SalePaymentRecordedEvent::class

    override fun reissue(sourceDocumentId: UUID) = publishExistingForSale(sourceDocumentId)

    fun publishExistingForSale(saleId: UUID) {
        val contactId = saleDataFetcher.getSaleContactId(saleId)
        val payments = salePaymentRepository.findBySaleId(saleId)
        if (payments.isEmpty()) return
        val voidedIds = salePaymentVoidRepository.findBySalePaymentIdIn(payments.map { it.id!! })
            .mapTo(HashSet()) { it.salePaymentId }
        val lines = payments.filter { it.id !in voidedIds }.mapNotNull { payment ->
            val accountCode = paymentMethodService.findAccountCode(payment.paymentMethodId)
            if (!StringUtils.hasValue(accountCode)) return@mapNotNull null
            SalePaymentLineDto(
                paymentReferenceNumber = payment.requiredReference(),
                paymentMethodAccountCode = accountCode!!,
                amount = payment.amount,
                paymentDate = payment.paymentDate
            )
        }
        if (lines.isEmpty()) return
        publishEvent(saleId, contactId, lines)
    }

    fun publish(saleId: UUID, contactId: UUID, payments: List<SalePaymentEntity>) {
        val lines = payments.mapNotNull { payment ->
            val accountCode = paymentMethodService.findAccountCode(payment.paymentMethodId)

            if (!StringUtils.hasValue(accountCode)) {
                log.debug(
                    "Payment method {} has no account code — ledger entry skipped for payment {}",
                    payment.paymentMethodId, payment.referenceNumber
                )
                return@mapNotNull null
            }

            SalePaymentLineDto(
                paymentReferenceNumber = payment.requiredReference(),
                paymentMethodAccountCode = accountCode!!,
                amount = payment.amount,
                paymentDate = payment.paymentDate
            )
        }

        if (lines.isNotEmpty()) {
            publishEvent(saleId, contactId, lines)
        }
    }

    private fun publishEvent(saleId: UUID, contactId: UUID, payments: List<SalePaymentLineDto>) {
        eventPublisher.publishEvent(
            SalePaymentRecordedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = SessionContextProvider.getLocationSchema()
                ),
                timestamp = Instant.now(),
                correlationId = null,
                sourceDocumentId = saleId,
                contactId = contactId,
                payments = payments
            )
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(SalePaymentHandlerForKafka::class.java)
    }
}
