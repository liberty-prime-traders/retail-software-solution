package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
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
    private val salePaymentVoidHandlerForKafka: SalePaymentVoidHandlerForKafka
) {

    fun recordPaymentsForNewSale(
        saleId: UUID,
        contactId: UUID,
        payments: List<SalePaymentCreateDto>,
        saleTotal: BigDecimal
    ) {
        if (payments.isEmpty()) return
        payments.forEach { SalePaymentValidator.guardPositiveAmount(it.amount) }
        val totalSubmitted = payments.sumOf { it.amount }
        SalePaymentValidator.guardNotExceedingSaleTotal(totalSubmitted, saleTotal)
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
        saleUpdater.updatePaymentStatus(saleId, SalePaymentValidator.resolvePaymentStatus(totalSubmitted, saleTotal))
        salePaymentHandlerForKafka.publish(saleId, contactId, entities)
    }

    fun recordPayment(dto: SalePaymentCreateDto): SalePaymentResponseDto {
        val saleId = dto.saleId ?: throw RtsGenericException("saleId is required")
        SalePaymentValidator.guardPositiveAmount(dto.amount)
        val (contactId, saleTotal) = saleDataFetcher.getSaleContext(saleId)
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
        val newStatus = SalePaymentValidator.resolvePaymentStatus(alreadyPaid + dto.amount, saleTotal)
        saleUpdater.updatePaymentStatus(saleId, newStatus)
        salePaymentHandlerForKafka.publish(saleId, contactId, listOf(entity))
        return SalePaymentMapper.toResponseDto(entity, null, newStatus)
    }

    fun voidPayment(dto: SalePaymentVoidCreateDto): SalePaymentResponseDto {
        val payment = salePaymentRepository.getReferenceById(dto.salePaymentId)
        SalePaymentValidator.guardNotAlreadyVoided(
            salePaymentVoidRepository.existsBySalePaymentId(dto.salePaymentId),
            payment.referenceNumber!!
        )
        val voidEntity = SalePaymentVoidEntity(salePaymentId = dto.salePaymentId, reason = dto.reason)
        salePaymentVoidRepository.save(voidEntity)
        val (contactId, saleTotal, saleStatus) = saleDataFetcher.getSaleContext(payment.saleId)
        SalePaymentValidator.guardSaleNotVoided(saleStatus)
        val paidAfterVoid = salePaymentFetcher.calculatePaidAmount(payment.saleId)
        val newStatus = SalePaymentValidator.resolvePaymentStatus(paidAfterVoid, saleTotal)
        saleUpdater.updatePaymentStatus(payment.saleId, newStatus)
        salePaymentVoidHandlerForKafka.publish(payment, voidEntity, contactId)
        return SalePaymentMapper.toResponseDto(payment, voidEntity.reason, newStatus)
    }
}
