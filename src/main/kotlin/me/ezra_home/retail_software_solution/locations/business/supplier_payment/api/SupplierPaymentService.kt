package me.ezra_home.retail_software_solution.locations.business.supplier_payment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchasePaymentCeilingService
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseUpdater
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.PaymentsCalculatorService
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentAssembler
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentEntity
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentMapper
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentRepository
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentVoidEntity
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentVoidHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentVoidMapper
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentVoidRepository
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
import me.ezra_home.retail_software_solution.util.business.Currencies
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SupplierPaymentService(
    private val supplierPaymentRepository: SupplierPaymentRepository,
    private val supplierPaymentVoidRepository: SupplierPaymentVoidRepository,
    private val supplierPaymentMapper: SupplierPaymentMapper,
    private val supplierPaymentVoidMapper: SupplierPaymentVoidMapper,
    private val purchaseDataFetcher: PurchaseDataFetcher,
    private val purchaseUpdater: PurchaseUpdater,
    private val paymentMethodService: PaymentMethodService,
    private val supplierPaymentHandlerForKafka: SupplierPaymentHandlerForKafka,
    private val supplierPaymentVoidHandlerForKafka: SupplierPaymentVoidHandlerForKafka,
    private val assembler: SupplierPaymentAssembler,
    private val purchasePaymentCeilingService: PurchasePaymentCeilingService,
    private val purchasePaymentStatusService: PurchasePaymentStatusService,
    private val paymentsCalculatorService: PaymentsCalculatorService,
    private val purchaseDeliveryDataFetcher: PurchaseDeliveryDataFetcher
) {

    fun getPaymentsByPurchaseId(purchaseId: UUID): List<SupplierPaymentResponseDto> {
        val payments = supplierPaymentRepository.findByPurchaseId(purchaseId)
        if (payments.isEmpty()) return emptyList()
        val voidsByPaymentId = supplierPaymentVoidRepository
            .findBySupplierPaymentIdIn(payments.map { it.id!! })
            .associateBy { it.supplierPaymentId }
        return assembler.buildResponses(payments, voidsByPaymentId)
    }

    fun recordPayment(dto: SupplierPaymentCreateDto): SupplierPaymentResponseDto {
        if (dto.amount <= BigDecimal.ZERO) {
            throw RtsGenericException("Payment amount must be greater than zero")
        }
        purchaseDataFetcher.lockAndGetPurchase(dto.purchaseId)
        validateDeliveryLevelPayment(dto)
        val ceiling = purchasePaymentCeilingService.computeCeiling(dto.purchaseId)
        val alreadyPaid = paymentsCalculatorService.calculatePaidAmountForPurchase(dto.purchaseId)

        if (ceiling.isFullyDelivered) {
            val projected = alreadyPaid + dto.amount
            if (projected > ceiling.deliveredTotal) {
                val formattedBalance = NumberFormat.getCurrencyInstance().format(ceiling.deliveredTotal - alreadyPaid)
                throw RtsGenericException("Payment of ${dto.amount} would exceed the remaining balance of $formattedBalance")
            }
        }

        val entity = supplierPaymentMapper.toEntity(dto)
        supplierPaymentRepository.save(entity)

        val newStatus = purchasePaymentStatusService.resolvePaymentStatus(alreadyPaid + dto.amount, ceiling)
        purchaseUpdater.updatePaymentStatus(dto.purchaseId, newStatus)
        publishTransactionToKafka(dto, entity)
        return assembler.buildResponse(entity, null, newStatus)
    }

    private fun validateDeliveryLevelPayment(dto: SupplierPaymentCreateDto) {
        if (dto.deliveryId != null) {
            val deliveryCeiling = purchaseDeliveryDataFetcher.calculateSingleDeliveryTotal(dto.deliveryId)
            val alreadyPaidForDelivery = paymentsCalculatorService.calculatePaidAmountForDelivery(dto.deliveryId)
            val projected = alreadyPaidForDelivery + dto.amount
            if (projected > deliveryCeiling) {
                val formattedBalance = Currencies.format(deliveryCeiling - alreadyPaidForDelivery)
                throw RtsGenericException("Payment of ${Currencies.format(dto.amount)} would " +
                        "exceed the remaining delivery balance of $formattedBalance"
                )
            }
        }
    }

    fun voidPayment(dto: SupplierPaymentVoidCreateDto): SupplierPaymentResponseDto {
        val paymentEntity = supplierPaymentRepository.findById(dto.supplierPaymentId)
            .orElseThrow { UpdatingNonExistingRecordException() }
        purchaseDataFetcher.lockAndGetPurchase(paymentEntity.purchaseId)

        if (supplierPaymentVoidRepository.existsBySupplierPaymentId(dto.supplierPaymentId)) {
            throw RtsGenericException("Payment ${paymentEntity.referenceNumber} has already been voided")
        }

        val voidEntity = supplierPaymentVoidMapper.toEntity(dto)
        supplierPaymentVoidRepository.save(voidEntity)

        val newStatus = purchasePaymentStatusService.patchThenReturnPaymentStatus(paymentEntity.purchaseId)

        publishVoidTransactionToKafka(paymentEntity, voidEntity)
        return assembler.buildResponse(paymentEntity, voidEntity, newStatus)
    }

    private fun publishTransactionToKafka(dto: SupplierPaymentCreateDto, payment: SupplierPaymentEntity) {
        val accountCode = paymentMethodService.findAccountCode(dto.paymentMethodId)
        if (StringUtils.hasValue(accountCode)) {
            val supplierId = purchaseDataFetcher.getSupplierId(dto.purchaseId)
            supplierPaymentHandlerForKafka.publish(payment, supplierId, accountCode!!)
        } else {
            log.debug(
                "Payment method {} has no account code — ledger entry skipped for payment {}",
                dto.paymentMethodId,
                payment.referenceNumber
            )
        }
    }

    private fun publishVoidTransactionToKafka(payment: SupplierPaymentEntity, voidEntity: SupplierPaymentVoidEntity) {
        val accountCode = paymentMethodService.findAccountCode(payment.paymentMethodId)
        if (StringUtils.hasValue(accountCode)) {
            val supplierId = purchaseDataFetcher.getSupplierId(payment.purchaseId)
            supplierPaymentVoidHandlerForKafka.publish(voidEntity, payment, supplierId, accountCode!!)
        } else {
            log.debug(
                "Payment method {} has no account code — ledger entry skipped for void of {}",
                payment.paymentMethodId,
                payment.referenceNumber
            )
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(SupplierPaymentService::class.java)
    }
}
