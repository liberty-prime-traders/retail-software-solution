package me.ezra_home.retail_software_solution.locations.business.supplier_payment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseService
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
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
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
    private val purchaseService: PurchaseService,
    private val paymentMethodService: PaymentMethodService,
    private val supplierPaymentHandlerForKafka: SupplierPaymentHandlerForKafka,
    private val supplierPaymentVoidHandlerForKafka: SupplierPaymentVoidHandlerForKafka,
    private val assembler: SupplierPaymentAssembler
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

        val purchaseTotal = purchaseDataFetcher.calculatePurchaseTotal(dto.purchaseId)
        val alreadyPaid = calculatePaidAmount(dto.purchaseId)
        val remainingBalance = purchaseTotal.subtract(alreadyPaid)

        if (dto.amount > remainingBalance) {
            val formattedBalance = NumberFormat.getCurrencyInstance().format(remainingBalance)
            throw RtsGenericException("Payment of $${dto.amount} would exceed remaining balance of  $formattedBalance")
        }

        val entity = supplierPaymentMapper.toEntity(dto)
        supplierPaymentRepository.save(entity)

        val newStatus = resolvePaymentStatus(alreadyPaid + dto.amount, purchaseTotal)
        purchaseService.updatePaymentStatus(dto.purchaseId, newStatus)
        publishTransactionToKafka(dto, entity)
        return assembler.buildResponse(entity, null, newStatus)
    }

    fun voidPayment(dto: SupplierPaymentVoidCreateDto): SupplierPaymentResponseDto {
        val paymentEntity = supplierPaymentRepository.findById(dto.supplierPaymentId)
            .orElseThrow { UpdatingNonExistingRecordException() }

        if (supplierPaymentVoidRepository.existsBySupplierPaymentId(dto.supplierPaymentId)) {
            throw RtsGenericException("Payment ${paymentEntity.referenceNumber} has already been voided")
        }

        val voidEntity = supplierPaymentVoidMapper.toEntity(dto)
        supplierPaymentVoidRepository.save(voidEntity)

        val purchaseTotal = purchaseDataFetcher.calculatePurchaseTotal(paymentEntity.purchaseId)
        val paidAfterVoid = calculatePaidAmount(paymentEntity.purchaseId)
        val newStatus = resolvePaymentStatus(paidAfterVoid, purchaseTotal)
        purchaseService.updatePaymentStatus(paymentEntity.purchaseId, newStatus)

        publishVoidTransactionToKafka(paymentEntity, voidEntity)
        return assembler.buildResponse(paymentEntity, voidEntity, newStatus)
    }

    private fun calculatePaidAmount(purchaseId: UUID): BigDecimal {
        val payments = supplierPaymentRepository.findByPurchaseId(purchaseId)
        if (payments.isEmpty()) return BigDecimal.ZERO
        val voidedPaymentIds = supplierPaymentVoidRepository
            .findBySupplierPaymentIdIn(payments.map { it.id!! })
            .mapTo(HashSet()) { it.supplierPaymentId }
        return payments.filter { it.id !in voidedPaymentIds }.sumOf { it.amount }
    }

    private fun resolvePaymentStatus(paidAmount: BigDecimal, purchaseTotal: BigDecimal): PaymentStatus {
        return when {
            paidAmount.compareTo(BigDecimal.ZERO) == 0 -> PaymentStatus.UNPAID
            paidAmount < purchaseTotal -> PaymentStatus.PARTIALLY_SETTLED
            else -> PaymentStatus.FULLY_SETTLED
        }
    }

    private fun publishTransactionToKafka(dto: SupplierPaymentCreateDto, payment: SupplierPaymentEntity) {
        val accountCode = paymentMethodService.findAccountCode(dto.paymentMethodId)
        if (StringUtils.hasValue(accountCode)) {
            val supplierId = purchaseDataFetcher.getSupplierId(dto.purchaseId)
            supplierPaymentHandlerForKafka.publish(payment, supplierId, accountCode!!)
        }
    }

    private fun publishVoidTransactionToKafka(payment: SupplierPaymentEntity, voidEntity: SupplierPaymentVoidEntity) {
        val accountCode = paymentMethodService.findAccountCode(payment.paymentMethodId)
        if (StringUtils.hasValue(accountCode)) {
            val supplierId = purchaseDataFetcher.getSupplierId(payment.purchaseId)
            supplierPaymentVoidHandlerForKafka.publish(voidEntity, payment, supplierId, accountCode!!)
        }
    }
}
