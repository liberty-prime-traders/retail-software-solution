package me.ezra_home.retail_software_solution.locations.business.sale_payment

import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.util.business.PaymentStatusResolver
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentFetcher
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
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
    private val paymentMethodService: PaymentMethodService,
) {

    fun write(
        saleId: UUID,
        contactId: UUID,
        receivableTotal: BigDecimal,
        newSalePayments: List<NewSalePayment>,
    ): SalePaymentWriteResult {
        val alreadyPaid = salePaymentFetcher.calculatePaidAmount(saleId)
        if (newSalePayments.isEmpty()) {
            return SalePaymentWriteResult(emptyList(), PaymentStatusResolver.resolve(alreadyPaid, receivableTotal))
        }
        val entitiesToSave = newSalePayments.map { newSalePayment ->
            SalePaymentEntity(
                saleId = saleId,
                paymentMethodId = newSalePayment.paymentMethodId,
                paymentMethodAccountCode = paymentMethodService.findAccountCode(newSalePayment.paymentMethodId),
                amount = newSalePayment.amount,
                reference = newSalePayment.reference,
                paymentDate = newSalePayment.paymentDate ?: DateTimes.Offset.Now.organization(),
            )
        }
        val savedSalePayments = salePaymentRepository.saveAll(entitiesToSave).toList()
        salePaymentHandlerForKafka.publish(saleId, contactId, savedSalePayments)
        val totalPaid = PaymentStatusResolver.paidTotalAfterRecording(alreadyPaid, savedSalePayments.map { it.amount })
        return SalePaymentWriteResult(savedSalePayments, PaymentStatusResolver.resolve(totalPaid, receivableTotal))
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
