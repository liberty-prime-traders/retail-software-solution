package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCommitInput
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentEntity
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentRepository
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentValidator
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class SalePaymentCommitter(
    private val salePaymentRepository: SalePaymentRepository,
    private val salePaymentFetcher: SalePaymentFetcher,
    private val salePaymentHandlerForKafka: SalePaymentHandlerForKafka,
) {

    fun appendNew(
        saleId: UUID,
        contactId: UUID,
        payableTotal: BigDecimal,
        input: SaleCommitInput,
    ): AppendResult {
        val newOnes = input.payments.filter { it.existingId == null }
        val existingIdByClientKey = input.payments
            .filter { it.existingId != null }
            .associate { it.clientKey to it.existingId!! }
        if (newOnes.isEmpty()) {
            val totalPaid = salePaymentFetcher.calculatePaidAmount(saleId)
            return AppendResult(existingIdByClientKey, resolvePaymentStatus(totalPaid, payableTotal))
        }
        newOnes.forEach { SalePaymentValidator.guardPositiveAmount(it.amount) }
        val entities = newOnes.map { dto ->
            SalePaymentEntity(
                saleId = saleId,
                paymentMethodId = dto.paymentMethodId,
                amount = dto.amount,
                reference = dto.reference,
                paymentDate = dto.paymentDate ?: DateTimes.Offset.Now.organization(),
            )
        }
        val saved = salePaymentRepository.saveAll(entities).toList()
        val newIdByClientKey = newOnes.mapIndexed { index, dto -> dto.clientKey to saved[index].id!! }.toMap()
        salePaymentHandlerForKafka.publish(saleId, contactId, saved)
        val freshTotalPaid = salePaymentFetcher.calculatePaidAmount(saleId)
        return AppendResult(
            idsByClientKey = newIdByClientKey + existingIdByClientKey,
            newStatus = resolvePaymentStatus(freshTotalPaid, payableTotal),
        )
    }

    fun guardWithinTotal(saleId: UUID, payableTotal: BigDecimal, input: SaleCommitInput) {
        val newOnes = input.payments.filter { it.existingId == null }
        if (newOnes.isEmpty()) return
        val alreadyPaid = if (input.saleId == null) BigDecimal.ZERO else salePaymentFetcher.calculatePaidAmount(saleId)
        val totalPaid = alreadyPaid + newOnes.sumOf { it.amount }
        SalePaymentValidator.guardNotExceedingSaleTotal(totalPaid, payableTotal)
    }

    fun guardFullCoverage(saleId: UUID, payableTotal: BigDecimal, input: SaleCommitInput) {
        val newOnes = input.payments.filter { it.existingId == null }
        val alreadyPaid = if (input.saleId == null) BigDecimal.ZERO else salePaymentFetcher.calculatePaidAmount(saleId)
        val totalPaid = alreadyPaid + newOnes.sumOf { it.amount }
        if (totalPaid < payableTotal) {
            throw RtsGenericException("Walk-in sales require full payment coverage")
        }
    }

    fun ensureRemovalsRejected(input: SaleCommitInput) {
        val saleId = input.saleId ?: return
        val existingIds = salePaymentRepository.findBySaleId(saleId).mapNotNull { it.id }.toHashSet()
        val incomingIds = input.payments.mapNotNull { it.existingId }.toHashSet()
        val missing = existingIds - incomingIds
        if (missing.isNotEmpty()) {
            throw RtsGenericException("Existing payments cannot be removed via a sale session: $missing")
        }
    }

    private fun resolvePaymentStatus(paid: BigDecimal, payableTotal: BigDecimal): PaymentStatus = when {
        paid.compareTo(BigDecimal.ZERO) == 0 -> PaymentStatus.UNPAID
        paid > payableTotal -> PaymentStatus.OVERPAID
        paid < payableTotal -> PaymentStatus.PARTIALLY_SETTLED
        else -> PaymentStatus.FULLY_SETTLED
    }

    data class AppendResult(
        val idsByClientKey: Map<UUID, UUID>,
        val newStatus: PaymentStatus,
    )
}
