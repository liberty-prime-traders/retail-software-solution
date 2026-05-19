package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleCommitInput
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentEntity
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentRepository
import me.ezra_home.retail_software_solution.util.business.DateTimes
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
        if (newOnes.isEmpty()) {
            val totalPaid = salePaymentFetcher.calculatePaidAmount(saleId)
            return AppendResult(emptyMap(), resolvePaymentStatus(totalPaid, payableTotal))
        }
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
            idsByClientKey = newIdByClientKey,
            newStatus = resolvePaymentStatus(freshTotalPaid, payableTotal),
        )
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
