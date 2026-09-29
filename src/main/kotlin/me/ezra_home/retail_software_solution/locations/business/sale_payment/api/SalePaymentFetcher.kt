package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentRepository
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentVoidRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SalePaymentSnapshot(
    val id: UUID,
    val paymentMethodId: UUID,
    val amount: BigDecimal,
    val reference: String?,
    val paymentDate: OffsetDateTime?,
    val voidedReason: String?,
)

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SalePaymentFetcher(
    private val salePaymentRepository: SalePaymentRepository,
    private val salePaymentVoidRepository: SalePaymentVoidRepository
) {

    fun calculatePaidAmount(saleId: UUID): BigDecimal {
        return calculatePaidAmounts(listOf(saleId))[saleId] ?: BigDecimal.ZERO
    }

    fun calculatePaidAmounts(saleIds: List<UUID>): Map<UUID, BigDecimal> {
        val payments = salePaymentRepository.findBySaleIdIn(saleIds)
        if (payments.isEmpty()) return emptyMap()
        val voidedIds = voidedPaymentIds(payments.map { it.id!! })
        return payments
            .filter { it.id !in voidedIds }
            .groupBy { it.saleId }
            .mapValues { (_, activePayments) -> activePayments.sumOf { it.amount } }
    }


    fun getPaymentSnapshots(saleId: UUID): List<SalePaymentSnapshot> {
        val payments = salePaymentRepository.findBySaleId(saleId)
        if (payments.isEmpty()) return emptyList()
        val voidReasonsById = salePaymentVoidRepository
            .findBySalePaymentIdIn(payments.map { it.id!! })
            .associate { it.salePaymentId to it.reason }
        return payments.map { entity ->
            SalePaymentSnapshot(
                id = entity.id!!,
                paymentMethodId = entity.paymentMethodId,
                amount = entity.amount,
                reference = entity.reference,
                paymentDate = entity.paymentDate,
                voidedReason = voidReasonsById[entity.id!!],
            )
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
