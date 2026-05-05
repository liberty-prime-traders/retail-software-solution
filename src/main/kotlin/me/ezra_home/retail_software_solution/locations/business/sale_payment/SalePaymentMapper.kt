package me.ezra_home.retail_software_solution.locations.business.sale_payment

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentResponseDto
import java.util.UUID

object SalePaymentMapper {

    fun toResponseDto(
        payment: SalePaymentEntity,
        voidedReason: String?,
        paymentMethodNamesById: Map<UUID, String> = emptyMap(),
        updatedStatus: PaymentStatus? = null
    ) = SalePaymentResponseDto(
        id = payment.id!!,
        referenceNumber = payment.referenceNumber!!,
        saleId = payment.saleId,
        amount = payment.amount,
        reference = payment.reference,
        paymentDate = payment.paymentDate,
        paymentMethodName = paymentMethodNamesById[payment.paymentMethodId] ?: "",
        voidedReason = voidedReason,
        updatedSalePaymentStatus = updatedStatus
    )
}
