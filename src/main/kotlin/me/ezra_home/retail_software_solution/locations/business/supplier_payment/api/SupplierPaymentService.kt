package me.ezra_home.retail_software_solution.locations.business.supplier_payment.api

import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher.PurchaseInfo
import me.ezra_home.retail_software_solution.locations.business.purchase.api.payment.PurchasePaymentCeilingService
import me.ezra_home.retail_software_solution.locations.business.purchase.api.payment.PurchasePayableCalculator
import me.ezra_home.retail_software_solution.locations.business.purchase.api.payment.PurchasePaymentCeilingService.PaymentCeiling
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseUpdater
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentEntity
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.PaymentsCalculatorService
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentAssembler
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentMapper
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentRepository
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentVoidHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentVoidMapper
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentVoidRepository
import me.ezra_home.retail_software_solution.organizations.business.fiscal_period.api.FiscalPeriodService
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodDataFetcher
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.DisplayFormatters
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.math.BigDecimal
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
    private val paymentMethodDataFetcher: PaymentMethodDataFetcher,
    private val supplierPaymentHandlerForKafka: SupplierPaymentHandlerForKafka,
    private val supplierPaymentVoidHandlerForKafka: SupplierPaymentVoidHandlerForKafka,
    private val assembler: SupplierPaymentAssembler,
    private val purchasePaymentCeilingService: PurchasePaymentCeilingService,
    private val purchasePaymentStatusService: PurchasePaymentStatusService,
    private val paymentsCalculatorService: PaymentsCalculatorService,
    private val purchaseDeliveryDataFetcher: PurchaseDeliveryDataFetcher,
    private val fiscalPeriodService: FiscalPeriodService
) {

    fun recordPayment(supplierPaymentCreateDto: SupplierPaymentCreateDto): SupplierPaymentResponseDto {
        val purchaseId = supplierPaymentCreateDto.purchaseId
        purchaseDataFetcher.lockPurchase(purchaseId)
        return recordPaymentsForLockedPurchases(
            listOf(supplierPaymentCreateDto),
            mapOf(purchaseId to purchasePaymentCeilingService.computeCeiling(purchaseId)),
            mapOf(purchaseId to paymentsCalculatorService.calculatePaidAmountForPurchase(purchaseId)),
            purchaseDataFetcher.findPurchaseInfoByIds(setOf(purchaseId))
        ).single()
    }

    fun recordPaymentsForLockedPurchases(
        supplierPaymentCreateDtos: List<SupplierPaymentCreateDto>,
        ceilingsByPurchaseId: Map<UUID, PaymentCeiling>,
        alreadyPaidByPurchaseId: Map<UUID, BigDecimal>,
        purchaseInfosByPurchaseId: Map<UUID, PurchaseInfo>
    ): List<SupplierPaymentResponseDto> {
        requireOnePaymentPerPurchase(supplierPaymentCreateDtos)
        requireOpenFiscalPeriods(supplierPaymentCreateDtos)
        val entities = buildEntities(supplierPaymentCreateDtos, ceilingsByPurchaseId, alreadyPaidByPurchaseId)
        supplierPaymentRepository.saveAll(entities)
        val paymentStatusesByPurchaseId = resolvePaymentStatuses(
            supplierPaymentCreateDtos, ceilingsByPurchaseId, alreadyPaidByPurchaseId
        )
        purchaseUpdater.updatePaymentStatuses(paymentStatusesByPurchaseId)
        val updatedPurchaseInfosByPurchaseId = purchaseInfosByPurchaseId.mapValues { (purchaseId, purchaseInfo) ->
            purchaseInfo.copy(paymentStatus = paymentStatusesByPurchaseId[purchaseId] ?: purchaseInfo.paymentStatus)
        }
        publishRecordedEvents(entities, updatedPurchaseInfosByPurchaseId)
        return assembler.buildResponses(entities, emptyMap(), updatedPurchaseInfosByPurchaseId)
    }

    private fun requireOnePaymentPerPurchase(supplierPaymentCreateDtos: List<SupplierPaymentCreateDto>) {
        val purchaseIds = supplierPaymentCreateDtos.map { it.purchaseId }
        if (purchaseIds.size != purchaseIds.toSet().size) {
            throw RtsGenericException("A batch can hold only one payment per purchase")
        }
    }

    private fun requireOpenFiscalPeriods(supplierPaymentCreateDtos: List<SupplierPaymentCreateDto>) {
        supplierPaymentCreateDtos.map { it.paymentDate }.distinct().forEach {
            fiscalPeriodService.requireOpenForDate(DateTimes.Local.atOrganizationZone(it))
        }
    }

    private fun buildEntities(
        supplierPaymentCreateDtos: List<SupplierPaymentCreateDto>,
        ceilingsByPurchaseId: Map<UUID, PaymentCeiling>,
        alreadyPaidByPurchaseId: Map<UUID, BigDecimal>
    ): List<SupplierPaymentEntity> {
        val accountCodesByPaymentMethodId = paymentMethodDataFetcher.findAccountCodesByIds(
            supplierPaymentCreateDtos.map { it.paymentMethodId }.toSet()
        )
        return supplierPaymentCreateDtos.map { supplierPaymentCreateDto ->
            validateDeliveryLevelPayment(supplierPaymentCreateDto)
            requireWithinDeliveredBalance(
                ceilingsByPurchaseId.getValue(supplierPaymentCreateDto.purchaseId),
                alreadyPaidByPurchaseId.getValue(supplierPaymentCreateDto.purchaseId),
                supplierPaymentCreateDto.amount.toBigDecimal()
            )
            supplierPaymentMapper.toEntity(
                supplierPaymentCreateDto,
                accountCodesByPaymentMethodId.getValue(supplierPaymentCreateDto.paymentMethodId)
            )
        }
    }

    private fun resolvePaymentStatuses(
        supplierPaymentCreateDtos: List<SupplierPaymentCreateDto>,
        ceilingsByPurchaseId: Map<UUID, PaymentCeiling>,
        alreadyPaidByPurchaseId: Map<UUID, BigDecimal>
    ): Map<UUID, PaymentStatus> = supplierPaymentCreateDtos.associate { supplierPaymentCreateDto ->
        val purchaseId = supplierPaymentCreateDto.purchaseId
        purchaseId to purchasePaymentStatusService.resolvePaymentStatus(
            alreadyPaidByPurchaseId.getValue(purchaseId) + supplierPaymentCreateDto.amount.toBigDecimal(),
            ceilingsByPurchaseId.getValue(purchaseId)
        )
    }

    private fun publishRecordedEvents(
        entities: List<SupplierPaymentEntity>,
        purchaseInfosByPurchaseId: Map<UUID, PurchaseInfo>
    ) {
        entities.forEach {
            supplierPaymentHandlerForKafka.publish(it, purchaseInfosByPurchaseId.getValue(it.purchaseId).supplierId)
        }
    }

    private fun requireWithinDeliveredBalance(ceiling: PaymentCeiling, alreadyPaid: BigDecimal, amount: BigDecimal) {
        val payable = PurchasePayableCalculator.calculatePayable(ceiling.amount, alreadyPaid)
        if (ceiling.isFullyDelivered && amount > payable) {
            val formattedAmount = DisplayFormatters.formatCurrency(amount)
            val formattedBalance = DisplayFormatters.formatCurrency(payable)
            throw RtsGenericException(
                "Payment of $formattedAmount would exceed the remaining balance of $formattedBalance"
            )
        }
    }

    private fun validateDeliveryLevelPayment(supplierPaymentCreateDto: SupplierPaymentCreateDto) {
        val deliveryId = supplierPaymentCreateDto.deliveryId
        if (deliveryId != null) {
            val deliveryCeiling = purchaseDeliveryDataFetcher.calculateSingleDeliveryTotal(deliveryId)
            val alreadyPaidForDelivery = paymentsCalculatorService.calculatePaidAmountForDelivery(deliveryId)
            val amount = supplierPaymentCreateDto.amount.toBigDecimal()
            if (alreadyPaidForDelivery + amount > deliveryCeiling) {
                val formattedAmount = DisplayFormatters.formatCurrency(amount)
                val formattedBalance = DisplayFormatters.formatCurrency(deliveryCeiling - alreadyPaidForDelivery)
                throw RtsGenericException(
                    "Payment of $formattedAmount would exceed the remaining delivery balance of $formattedBalance"
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

        val paidTotalBeforeVoid = paymentsCalculatorService.calculatePaidAmountForPurchase(paymentEntity.purchaseId)
        val voidEntity = supplierPaymentVoidMapper.toEntity(supplierPaymentVoidCreateDto)
        supplierPaymentVoidRepository.save(voidEntity)

        val newStatus = purchasePaymentStatusService.patchThenReturnPaymentStatusAfterVoid(
            paymentEntity.purchaseId, paidTotalBeforeVoid, paymentEntity.amount
        )

        supplierPaymentVoidHandlerForKafka.publish(
            voidEntity, paymentEntity, purchaseDataFetcher.getSupplierId(paymentEntity.purchaseId)
        )
        return assembler.buildResponse(paymentEntity, voidEntity, newStatus)
    }

}
