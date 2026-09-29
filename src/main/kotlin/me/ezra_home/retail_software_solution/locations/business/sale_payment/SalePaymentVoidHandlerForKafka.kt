package me.ezra_home.retail_software_solution.locations.business.sale_payment

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SalePaymentVoidedEvent
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SalePaymentVoidHandlerForKafka(
    private val saleDataFetcher: SaleDataFetcher,
    private val salePaymentRepository: SalePaymentRepository,
    private val salePaymentVoidRepository: SalePaymentVoidRepository,
    private val paymentMethodService: PaymentMethodService,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = SalePaymentVoidedEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        val contactId = saleDataFetcher.getSaleContactId(sourceDocumentId)
        val payments = salePaymentRepository.findBySaleId(sourceDocumentId)
        if (payments.isEmpty()) return
        val voidsByPaymentId = salePaymentVoidRepository.findBySalePaymentIdIn(payments.map { it.id!! })
            .associateBy { it.salePaymentId }
        val allPaymentMethods = paymentMethodService.getAllPaymentMethods().associateBy { it.id }
        payments.forEach { payment ->
            val voidEntity = voidsByPaymentId[payment.id!!] ?: return@forEach
            val accountCode = allPaymentMethods[payment.paymentMethodId]?.accountCode
            if (StringUtils.hasValue(accountCode)) {
                publishEvent(payment, voidEntity, contactId, accountCode!!)
            }
        }
    }

    fun publish(payment: SalePaymentEntity, voidEntity: SalePaymentVoidEntity, contactId: UUID) {
        val accountCode = paymentMethodService.findAccountCode(payment.paymentMethodId)
        if (!StringUtils.hasValue(accountCode)) {
            log.debug("Payment method {} has no account code — ledger entry skipped for void of {}", payment.paymentMethodId, payment.referenceNumber)
            return
        }
        publishEvent(payment, voidEntity, contactId, accountCode!!)
    }

    private fun publishEvent(payment: SalePaymentEntity, voidEntity: SalePaymentVoidEntity, contactId: UUID, accountCode: String) {
        eventPublisher.publishEvent(
            SalePaymentVoidedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = SessionContextProvider.getLocationSchema()
                ),
                timestamp = Instant.now(),
                correlationId = null,
                sourceDocumentId = payment.saleId,
                contactId = contactId,
                paymentReferenceNumber = payment.requiredReference(),
                paymentMethodAccountCode = accountCode,
                amount = payment.amount,
                voidedOn = voidEntity.createdOn?.let { DateTimes.Local.atOrganizationZone(it) }
                    ?: DateTimes.Local.Now.organization()
            )
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(SalePaymentVoidHandlerForKafka::class.java)
    }
}
