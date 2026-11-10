package me.ezra_home.retail_software_solution.locations.business.supplier_payment.api

import me.ezra_home.retail_software_solution.locations.business.supplier_payment.PaymentsCalculatorService
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentAssembler
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentRepository
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.SupplierPaymentVoidRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class SupplierPaymentDataFetcher(
    private val paymentsCalculatorService: PaymentsCalculatorService,
    private val assembler: SupplierPaymentAssembler,
    private val supplierPaymentRepository: SupplierPaymentRepository,
    private val supplierPaymentVoidRepository: SupplierPaymentVoidRepository
) {

    fun getPaidAmountsByPurchaseIds(purchaseIds: Set<UUID>): Map<UUID, BigDecimal> =
        paymentsCalculatorService.calculatePaidAmountsForPurchases(purchaseIds)

    fun getPaymentsByPurchaseId(purchaseId: UUID): List<SupplierPaymentResponseDto> {
        val payments = supplierPaymentRepository.findByPurchaseId(purchaseId)
        if (payments.isEmpty()) return emptyList()
        val voidsByPaymentId = supplierPaymentVoidRepository
            .findBySupplierPaymentIdIn(payments.map { it.id!! })
            .associateBy { it.supplierPaymentId }
        return assembler.buildResponses(payments, voidsByPaymentId)
    }
}
