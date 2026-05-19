package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleSaveRequest
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentEntity
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentHandlerForKafka
import me.ezra_home.retail_software_solution.locations.business.sale_payment.SalePaymentRepository
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class SalePaymentAppender(
    private val salePaymentRepository: SalePaymentRepository,
    private val salePaymentFetcher: SalePaymentFetcher,
    private val salePaymentHandlerForKafka: SalePaymentHandlerForKafka,
) {

    fun appendNew(
        saleId: UUID,
        contactId: UUID,
        payableTotal: BigDecimal,
        saleSaveRequest: SaleSaveRequest,
    ): AppendResult {
        val newSalePaymentSaveRequests = saleSaveRequest.salePayments.filter { it.existingId == null }
        if (newSalePaymentSaveRequests.isEmpty()) {
            val totalPaid = salePaymentFetcher.calculatePaidAmount(saleId)
            return AppendResult(emptyMap(), resolvePaymentStatus(totalPaid, payableTotal))
        }
        val newSalePaymentEntities = newSalePaymentSaveRequests.map { salePaymentSaveRequest ->
            SalePaymentEntity(
                saleId = saleId,
                paymentMethodId = salePaymentSaveRequest.paymentMethodId,
                amount = salePaymentSaveRequest.amount,
                reference = salePaymentSaveRequest.reference,
                paymentDate = salePaymentSaveRequest.paymentDate ?: DateTimes.Offset.Now.organization(),
            )
        }
        val savedSalePayments = salePaymentRepository.saveAll(newSalePaymentEntities).toList()
        val newSalePaymentIdsByClientKey = newSalePaymentSaveRequests
            .mapIndexed { salePaymentIndex, salePaymentSaveRequest ->
                salePaymentSaveRequest.clientKey to savedSalePayments[salePaymentIndex].id!!
            }.toMap()
        salePaymentHandlerForKafka.publish(saleId, contactId, savedSalePayments)
        val freshTotalPaid = salePaymentFetcher.calculatePaidAmount(saleId)
        return AppendResult(
            salePaymentIdsByClientKey = newSalePaymentIdsByClientKey,
            newPaymentStatus = resolvePaymentStatus(freshTotalPaid, payableTotal),
        )
    }

    private fun resolvePaymentStatus(paid: BigDecimal, payableTotal: BigDecimal): PaymentStatus = when {
        paid.compareTo(BigDecimal.ZERO) == 0 -> PaymentStatus.UNPAID
        paid > payableTotal -> PaymentStatus.OVERPAID
        paid < payableTotal -> PaymentStatus.PARTIALLY_SETTLED
        else -> PaymentStatus.FULLY_SETTLED
    }

    data class AppendResult(
        val salePaymentIdsByClientKey: Map<UUID, UUID>,
        val newPaymentStatus: PaymentStatus,
    )
}
