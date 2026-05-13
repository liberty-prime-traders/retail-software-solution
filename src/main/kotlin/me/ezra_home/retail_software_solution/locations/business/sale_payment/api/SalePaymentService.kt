package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdater
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentEntity
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentMapper
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentRepository
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentValidator
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentVoidEntity
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentVoidHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentVoidRepository
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SalePaymentService(
    private val salePaymentRepository: SalePaymentRepository,
    private val salePaymentVoidRepository: SalePaymentVoidRepository,
    private val saleDataFetcher: SaleDataFetcher,
    private val saleUpdater: SaleUpdater,
    private val salePaymentFetcher: SalePaymentFetcher,
    private val salePaymentHandlerForKafka: SalePaymentHandlerForKafka,
    private val salePaymentVoidHandlerForKafka: SalePaymentVoidHandlerForKafka,
    private val paymentMethodService: PaymentMethodService
) {

    fun recordPaymentsSubmittedWithSale(
        saleId: UUID,
        contactId: UUID,
        payments: List<SalePaymentCreateDto>,
        saleTotal: BigDecimal,
        isNewSale: Boolean,
        publishKafka: Boolean,
    ): PaymentStatus? {
        if (payments.isEmpty()) return null
        val alreadyPaid = if (isNewSale) BigDecimal.ZERO else salePaymentFetcher.calculatePaidAmount(saleId)
        return doRecordPayments(saleId, contactId, payments, saleTotal, alreadyPaid, publishKafka)
    }

    private fun doRecordPayments(
        saleId: UUID,
        contactId: UUID,
        payments: List<SalePaymentCreateDto>,
        saleTotal: BigDecimal,
        alreadyPaid: BigDecimal,
        publishKafka: Boolean,
    ): PaymentStatus {
        payments.forEach { SalePaymentValidator.guardPositiveAmount(it.amount) }
        val totalPaid = alreadyPaid + payments.sumOf { it.amount }
        SalePaymentValidator.guardNotExceedingSaleTotal(totalPaid, saleTotal)
        val entities = payments.map { dto ->
            SalePaymentEntity(
                saleId = saleId,
                paymentMethodId = dto.paymentMethodId,
                amount = dto.amount,
                reference = dto.reference,
                paymentDate = dto.paymentDate ?: DateTimes.Offset.Now.organization()
            )
        }
        salePaymentRepository.saveAll(entities)
        if (publishKafka) salePaymentHandlerForKafka.publish(saleId, contactId, entities)
        return resolvePaymentStatus(totalPaid, saleTotal)
    }

    fun publishKafkaForExistingPayments(saleId: UUID) {
        salePaymentHandlerForKafka.publishExistingForSale(saleId)
    }

    fun recordPayment(dto: SalePaymentCreateDto): SalePaymentResponseDto {
        val saleId = dto.saleId ?: throw RtsGenericException("saleId is required")
        SalePaymentValidator.guardPositiveAmount(dto.amount)
        val (contactId, saleTotal, saleStatus) = saleDataFetcher.getSaleContext(saleId)
        SalePaymentValidator.guardOpenForPayment(saleStatus)
        val alreadyPaid = salePaymentFetcher.calculatePaidAmount(saleId)
        SalePaymentValidator.guardNotExceedingBalance(dto.amount, saleTotal.subtract(alreadyPaid))
        val entity = SalePaymentEntity(
            saleId = saleId,
            paymentMethodId = dto.paymentMethodId,
            amount = dto.amount,
            reference = dto.reference,
            paymentDate = dto.paymentDate ?: DateTimes.Offset.Now.organization()
        )
        salePaymentRepository.save(entity)
        val newStatus = resolvePaymentStatus(alreadyPaid + dto.amount, saleTotal)
        saleUpdater.updatePaymentStatus(saleId, newStatus)
        salePaymentHandlerForKafka.publish(saleId, contactId, listOf(entity))
        return SalePaymentMapper.toResponseDto(
            entity,
            null,
            paymentMethodService.getNamesById(),
            newStatus
        )
    }

    fun voidPayment(dto: SalePaymentVoidCreateDto): SalePaymentResponseDto {
        val payment = salePaymentRepository.getReferenceById(dto.salePaymentId)
        SalePaymentValidator.guardNotAlreadyVoided(
            salePaymentVoidRepository.existsBySalePaymentId(dto.salePaymentId),
            payment.requiredReference()
        )

        val (contactId, saleTotal, saleStatus) = saleDataFetcher.getSaleContext(payment.saleId)
        SalePaymentValidator.guardSaleNotVoided(saleStatus)

        val voidEntity = SalePaymentVoidEntity(salePaymentId = dto.salePaymentId, reason = dto.reason)
        salePaymentVoidRepository.save(voidEntity)

        val totalPaidAfterVoid = salePaymentFetcher.calculatePaidAmount(payment.saleId)
        val newStatus = resolvePaymentStatus(totalPaidAfterVoid, saleTotal)
        saleUpdater.updatePaymentStatus(payment.saleId, newStatus)
        salePaymentVoidHandlerForKafka.publish(payment, voidEntity, contactId)
        return SalePaymentMapper.toResponseDto(
            payment,
            voidEntity.reason,
            paymentMethodService.getNamesById(),
            newStatus
        )
    }

    fun resolvePaymentStatus(paid: BigDecimal, total: BigDecimal): PaymentStatus = when {
        paid.compareTo(BigDecimal.ZERO) == 0 -> PaymentStatus.UNPAID
        paid > total -> PaymentStatus.OVERPAID
        paid < total -> PaymentStatus.PARTIALLY_SETTLED
        else -> PaymentStatus.FULLY_SETTLED
    }
}
