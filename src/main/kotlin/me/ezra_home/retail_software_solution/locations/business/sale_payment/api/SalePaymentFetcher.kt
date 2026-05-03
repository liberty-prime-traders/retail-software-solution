package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentMapper
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentRepository
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentVoidRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SalePaymentFetcher(
    private val salePaymentRepository: SalePaymentRepository,
    private val salePaymentVoidRepository: SalePaymentVoidRepository
) {

    fun calculatePaidAmount(saleId: UUID): BigDecimal {
        val payments = salePaymentRepository.findBySaleId(saleId)
        if (payments.isEmpty()) return BigDecimal.ZERO
        val voidedIds = voidedPaymentIds(payments.map { it.id!! })
        return payments.filter { it.id !in voidedIds }.sumOf { it.amount }
    }

    fun getPaymentsBySaleId(saleId: UUID): List<SalePaymentResponseDto> {
        return getPaymentsBySaleIds(listOf(saleId))[saleId] ?: emptyList()
    }

    fun getPaymentsBySaleIds(saleIds: List<UUID>): Map<UUID, List<SalePaymentResponseDto>> {
        val payments = salePaymentRepository.findBySaleIdIn(saleIds)
        if (payments.isEmpty()) return emptyMap()
        val voidsBySalePaymentId = salePaymentVoidRepository
            .findBySalePaymentIdIn(payments.map { it.id!! })
            .associateBy { it.salePaymentId }
        return payments.groupBy({ it.saleId }) { payment ->
            SalePaymentMapper.toResponseDto(payment, voidsBySalePaymentId[payment.id!!]?.reason, null)
        }
    }

    fun hasActivePayments(saleId: UUID): Boolean {
        val payments = salePaymentRepository.findBySaleId(saleId)
        if (payments.isEmpty()) return false
        val voidedIds = voidedPaymentIds(payments.map { it.id!! })
        return payments.any { it.id !in voidedIds }
    }

    private fun voidedPaymentIds(paymentIds: List<UUID>): Set<UUID> =
        salePaymentVoidRepository.findBySalePaymentIdIn(paymentIds)
            .mapTo(HashSet()) { it.salePaymentId }
}
