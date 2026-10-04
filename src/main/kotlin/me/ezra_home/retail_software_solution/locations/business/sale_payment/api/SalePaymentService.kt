package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdater
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentMapper
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentRepository
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentVoidEntity
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentVoidHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentVoidRepository
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentWriter
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
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
    private val fiscalPeriodService: FiscalPeriodService,
) {

    fun recordPayment(salePaymentCreateDto: SalePaymentCreateDto): SalePaymentResponseDto {
        val saleId = salePaymentCreateDto.saleId ?: throw RtsGenericException("saleId is required")
        SalePaymentValidator.guardPositiveAmount(salePaymentCreateDto.amount)
        val effectivePaymentDate = salePaymentCreateDto.paymentDate ?: DateTimes.Offset.Now.organization()
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.atOrganizationZone(effectivePaymentDate))
        val (contactId, receivableTotal, saleStatus) = saleDataFetcher.lockAndGetSaleContext(saleId)
        SalePaymentValidator.guardOpenForPayment(saleStatus)
        val alreadyPaid = salePaymentFetcher.calculatePaidAmount(saleId)
        SalePaymentValidator.guardNotExceedingBalance(salePaymentCreateDto.amount, receivableTotal.subtract(alreadyPaid))
        val writeResult = salePaymentWriter.write(
            saleId = saleId,
            contactId = contactId,
            receivableTotal = receivableTotal,
            newSalePayments = listOf(
                SalePaymentWriter.NewSalePayment(
                    paymentMethodId = salePaymentCreateDto.paymentMethodId,
                    amount = salePaymentCreateDto.amount,
                    reference = salePaymentCreateDto.reference,
                    paymentDate = effectivePaymentDate,
                )
            ),
        )
        val savedSalePayment = writeResult.savedSalePayments.single()
        val updatedSaleVersion = saleUpdater.updatePaymentStatus(saleId, writeResult.newPaymentStatus)
        return SalePaymentMapper.toResponseDto(
            savedSalePayment,
            null,
            paymentMethodService.getNamesById(),
            writeResult.newPaymentStatus,
            updatedSaleVersion,
        )
    }

    fun voidPayment(salePaymentVoidCreateDto: SalePaymentVoidCreateDto): SalePaymentResponseDto {
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.Now.organization())
        val payment = salePaymentRepository.getReferenceById(salePaymentVoidCreateDto.salePaymentId)
        SalePaymentValidator.guardNotAlreadyVoided(
            salePaymentVoidRepository.existsBySalePaymentId(salePaymentVoidCreateDto.salePaymentId),
            payment.requiredReference()
        )

        val (contactId, receivableTotal, saleStatus) = saleDataFetcher.lockAndGetSaleContext(payment.saleId)
        SalePaymentValidator.guardSaleNotVoided(saleStatus)

        val voidEntity = SalePaymentVoidEntity(salePaymentId = salePaymentVoidCreateDto.salePaymentId, reason = salePaymentVoidCreateDto.reason)
        salePaymentVoidRepository.save(voidEntity)

        val totalPaidAfterVoid = salePaymentFetcher.calculatePaidAmount(payment.saleId)
        val newStatus = PaymentStatusResolver.resolve(totalPaidAfterVoid, receivableTotal)
        val updatedSaleVersion = saleUpdater.updatePaymentStatus(payment.saleId, newStatus)
        salePaymentVoidHandlerForKafka.publish(payment, voidEntity, contactId)
        return SalePaymentMapper.toResponseDto(
            payment,
            voidEntity.reason,
            paymentMethodService.getNamesById(),
            newStatus,
            updatedSaleVersion,
        )
    }
}
