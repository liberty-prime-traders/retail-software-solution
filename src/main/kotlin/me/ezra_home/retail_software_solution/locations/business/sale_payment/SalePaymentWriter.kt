package me.ezra_home.retail_software_solution.locations.business.sale_payment

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.PaymentStatusResolver
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentFetcher
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Service
class SalePaymentWriter(
    private val salePaymentRepository: SalePaymentRepository,
    private val salePaymentFetcher: SalePaymentFetcher,
    private val salePaymentHandlerForKafka: SalePaymentHandlerForKafka,
) {

    fun write(
        saleId: UUID,
        contactId: UUID,
        payableTotal: BigDecimal,
        newSalePayments: List<NewSalePayment>,
    ): SalePaymentWriteResult {
        if (newSalePayments.isEmpty()) {
            val totalPaid = salePaymentFetcher.calculatePaidAmount(saleId)
            return SalePaymentWriteResult(emptyList(), PaymentStatusResolver.resolve(totalPaid, payableTotal))
        }
        val entitiesToSave = newSalePayments.map { newSalePayment ->
            SalePaymentEntity(
                saleId = saleId,
                paymentMethodId = newSalePayment.paymentMethodId,
                amount = newSalePayment.amount,
                reference = newSalePayment.reference,
                paymentDate = newSalePayment.paymentDate ?: DateTimes.Offset.Now.organization(),
            )
        }
        val savedSalePayments = salePaymentRepository.saveAll(entitiesToSave).toList()
        salePaymentHandlerForKafka.publish(saleId, contactId, savedSalePayments)
        val freshTotalPaid = salePaymentFetcher.calculatePaidAmount(saleId)
        return SalePaymentWriteResult(savedSalePayments, PaymentStatusResolver.resolve(freshTotalPaid, payableTotal))
    }

    data class NewSalePayment(
        val paymentMethodId: UUID,
        val amount: BigDecimal,
        val reference: String?,
        val paymentDate: OffsetDateTime?,
    )

    data class SalePaymentWriteResult(
        val savedSalePayments: List<SalePaymentEntity>,
        val newPaymentStatus: PaymentStatus,
    )
}
