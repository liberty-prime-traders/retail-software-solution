package me.ezra_home.retail_software_solution.locations.business.supplier_payment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchasePaymentCeilingService
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseUpdater
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.PaymentsCalculatorService
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentAssembler
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentMapper
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentRepository
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentVoidHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentVoidMapper
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentVoidRepository
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.DisplayFormatters
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
    private val purchaseUpdater: PurchaseUpdater,
    private val paymentMethodService: PaymentMethodService,
    private val supplierPaymentHandlerForKafka: SupplierPaymentHandlerForKafka,
    private val supplierPaymentVoidHandlerForKafka: SupplierPaymentVoidHandlerForKafka,
    private val assembler: SupplierPaymentAssembler,
    private val purchasePaymentCeilingService: PurchasePaymentCeilingService,
    private val purchasePaymentStatusService: PurchasePaymentStatusService,
    private val paymentsCalculatorService: PaymentsCalculatorService,
    private val purchaseDeliveryDataFetcher: PurchaseDeliveryDataFetcher,
    private val fiscalPeriodService: FiscalPeriodService
) {

    fun getPaymentsByPurchaseId(purchaseId: UUID): List<SupplierPaymentResponseDto> {
        val payments = supplierPaymentRepository.findByPurchaseId(purchaseId)
        if (payments.isEmpty()) return emptyList()
        val voidsByPaymentId = supplierPaymentVoidRepository
            .findBySupplierPaymentIdIn(payments.map { it.id!! })
            .associateBy { it.supplierPaymentId }
        return assembler.buildResponses(payments, voidsByPaymentId)
    }

    fun recordPayment(supplierPaymentCreateDto: SupplierPaymentCreateDto): SupplierPaymentResponseDto {
        if (supplierPaymentCreateDto.amount <= BigDecimal.ZERO) {
            throw RtsGenericException("Payment amount must be greater than zero")
        }
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.atOrganizationZone(supplierPaymentCreateDto.paymentDate))
        purchaseDataFetcher.lockPurchase(supplierPaymentCreateDto.purchaseId)
        validateDeliveryLevelPayment(supplierPaymentCreateDto)
        val ceiling = purchasePaymentCeilingService.computeCeiling(supplierPaymentCreateDto.purchaseId)
        val alreadyPaid = paymentsCalculatorService.calculatePaidAmountForPurchase(supplierPaymentCreateDto.purchaseId)

        if (ceiling.isFullyDelivered) {
            val projected = alreadyPaid + supplierPaymentCreateDto.amount
            if (projected > ceiling.deliveredTotal) {
                val formattedBalance = NumberFormat.getCurrencyInstance().format(ceiling.deliveredTotal - alreadyPaid)
                throw RtsGenericException("Payment of ${supplierPaymentCreateDto.amount} would exceed the remaining balance of $formattedBalance")
            }
        }

        val entity = supplierPaymentMapper.toEntity(
            supplierPaymentCreateDto, paymentMethodService.findAccountCode(supplierPaymentCreateDto.paymentMethodId)
        )
        supplierPaymentRepository.save(entity)

        val newStatus = purchasePaymentStatusService.resolvePaymentStatus(alreadyPaid + supplierPaymentCreateDto.amount, ceiling)
        purchaseUpdater.updatePaymentStatus(supplierPaymentCreateDto.purchaseId, newStatus)
        supplierPaymentHandlerForKafka.publish(entity, purchaseDataFetcher.getSupplierId(supplierPaymentCreateDto.purchaseId))
        return assembler.buildResponse(entity, null, newStatus)
    }

    private fun validateDeliveryLevelPayment(supplierPaymentCreateDto: SupplierPaymentCreateDto) {
        if (supplierPaymentCreateDto.deliveryId != null) {
            val deliveryCeiling = purchaseDeliveryDataFetcher.calculateSingleDeliveryTotal(supplierPaymentCreateDto.deliveryId)
            val alreadyPaidForDelivery = paymentsCalculatorService.calculatePaidAmountForDelivery(supplierPaymentCreateDto.deliveryId)
            val projected = alreadyPaidForDelivery + supplierPaymentCreateDto.amount
            if (projected > deliveryCeiling) {
                val formattedBalance = DisplayFormatters.formatCurrency(deliveryCeiling - alreadyPaidForDelivery)
                throw RtsGenericException("Payment of ${DisplayFormatters.formatCurrency(supplierPaymentCreateDto.amount)} would " +
                        "exceed the remaining delivery balance of $formattedBalance"
                )
            }
        }
    }

    fun voidPayment(supplierPaymentVoidCreateDto: SupplierPaymentVoidCreateDto): SupplierPaymentResponseDto {
        fiscalPeriodService.requireOpenForDate(DateTimes.Local.Now.organization())
        val paymentEntity = supplierPaymentRepository.findById(supplierPaymentVoidCreateDto.supplierPaymentId)
            .orElseThrow { UpdatingNonExistingRecordException() }
        purchaseDataFetcher.lockPurchase(paymentEntity.purchaseId)

        if (supplierPaymentVoidRepository.existsBySupplierPaymentId(supplierPaymentVoidCreateDto.supplierPaymentId)) {
            throw RtsGenericException("Payment ${paymentEntity.referenceNumber} has already been voided")
        }

        val voidEntity = supplierPaymentVoidMapper.toEntity(supplierPaymentVoidCreateDto)
        supplierPaymentVoidRepository.save(voidEntity)

        val newStatus = purchasePaymentStatusService.patchThenReturnPaymentStatus(paymentEntity.purchaseId)

        supplierPaymentVoidHandlerForKafka.publish(voidEntity, paymentEntity, purchaseDataFetcher.getSupplierId(paymentEntity.purchaseId))
        return assembler.buildResponse(paymentEntity, voidEntity, newStatus)
    }

}
