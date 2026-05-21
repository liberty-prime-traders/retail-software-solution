package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdater
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentMapper
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentRepository
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentValidator
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentVoidEntity
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentVoidHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentVoidRepository
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class SalePaymentService(
    private val salePaymentRepository: SalePaymentRepository,
    private val salePaymentVoidRepository: SalePaymentVoidRepository,
    private val saleDataFetcher: SaleDataFetcher,
    private val saleUpdater: SaleUpdater,
    private val salePaymentFetcher: SalePaymentFetcher,
    private val salePaymentWriter: SalePaymentWriter,
    private val salePaymentVoidHandlerForKafka: SalePaymentVoidHandlerForKafka,
    private val paymentMethodService: PaymentMethodService,
) {

    fun recordPayment(dto: SalePaymentCreateDto): SalePaymentResponseDto {
        val saleId = dto.saleId ?: throw RtsGenericException("saleId is required")
        SalePaymentValidator.guardPositiveAmount(dto.amount)
        val (contactId, saleTotal, saleStatus) = saleDataFetcher.lockAndGetSaleContext(saleId)
        SalePaymentValidator.guardOpenForPayment(saleStatus)
        val alreadyPaid = salePaymentFetcher.calculatePaidAmount(saleId)
        SalePaymentValidator.guardNotExceedingBalance(dto.amount, saleTotal.subtract(alreadyPaid))
        val writeResult = salePaymentWriter.write(
            saleId = saleId,
            contactId = contactId,
            payableTotal = saleTotal,
            newSalePayments = listOf(
                SalePaymentWriter.NewSalePayment(
                    paymentMethodId = dto.paymentMethodId,
                    amount = dto.amount,
                    reference = dto.reference,
                    paymentDate = dto.paymentDate,
                )
            ),
        )
        val savedSalePayment = writeResult.savedSalePayments.single()
        saleUpdater.updatePaymentStatus(saleId, writeResult.newPaymentStatus)
        return SalePaymentMapper.toResponseDto(
            savedSalePayment,
            null,
            paymentMethodService.getNamesById(),
            writeResult.newPaymentStatus,
        )
    }

    fun voidPayment(dto: SalePaymentVoidCreateDto): SalePaymentResponseDto {
        val payment = salePaymentRepository.getReferenceById(dto.salePaymentId)
        SalePaymentValidator.guardNotAlreadyVoided(
            salePaymentVoidRepository.existsBySalePaymentId(dto.salePaymentId),
            payment.requiredReference()
        )

        val (contactId, saleTotal, saleStatus) = saleDataFetcher.lockAndGetSaleContext(payment.saleId)
        SalePaymentValidator.guardSaleNotVoided(saleStatus)

        val voidEntity = SalePaymentVoidEntity(salePaymentId = dto.salePaymentId, reason = dto.reason)
        salePaymentVoidRepository.save(voidEntity)

        val totalPaidAfterVoid = salePaymentFetcher.calculatePaidAmount(payment.saleId)
        val newStatus = PaymentStatusResolver.resolve(totalPaidAfterVoid, saleTotal)
        saleUpdater.updatePaymentStatus(payment.saleId, newStatus)
        salePaymentVoidHandlerForKafka.publish(payment, voidEntity, contactId)
        return SalePaymentMapper.toResponseDto(
            payment,
            voidEntity.reason,
            paymentMethodService.getNamesById(),
            newStatus
        )
    }
}
