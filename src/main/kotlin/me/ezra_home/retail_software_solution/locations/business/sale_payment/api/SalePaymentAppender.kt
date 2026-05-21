package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleSaveRequest
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class SalePaymentAppender(
    private val salePaymentWriter: SalePaymentWriter,
) {

    fun appendNew(
        saleId: UUID,
        contactId: UUID,
        payableTotal: BigDecimal,
        saleSaveRequest: SaleSaveRequest,
    ): AppendResult {
        val newSalePaymentSaveRequests = saleSaveRequest.salePayments.filter { it.existingId == null }
        val newSalePayments = newSalePaymentSaveRequests.map { salePaymentSaveRequest ->
            SalePaymentWriter.NewSalePayment(
                paymentMethodId = salePaymentSaveRequest.paymentMethodId,
                amount = salePaymentSaveRequest.amount,
                reference = salePaymentSaveRequest.reference,
                paymentDate = salePaymentSaveRequest.paymentDate,
            )
        }
        val writeResult = salePaymentWriter.write(saleId, contactId, payableTotal, newSalePayments)
        val salePaymentIdsByClientKey = newSalePaymentSaveRequests
            .mapIndexed { paymentIndex, salePaymentSaveRequest ->
                salePaymentSaveRequest.clientKey to writeResult.savedSalePayments[paymentIndex].id!!
            }.toMap()
        return AppendResult(
            salePaymentIdsByClientKey = salePaymentIdsByClientKey,
            newPaymentStatus = writeResult.newPaymentStatus,
        )
    }

    data class AppendResult(
        val salePaymentIdsByClientKey: Map<UUID, UUID>,
        val newPaymentStatus: PaymentStatus,
    )
}
