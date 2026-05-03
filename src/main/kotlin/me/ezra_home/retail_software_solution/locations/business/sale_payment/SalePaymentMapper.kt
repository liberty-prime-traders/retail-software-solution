package me.ezra_home.retail_software_solution.locations.business.sale_payment

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.sale_payment.api.SalePaymentResponseDto

object SalePaymentMapper {

    fun toResponseDto(
        payment: SalePaymentEntity,
        voidedReason: String?,
        updatedStatus: PaymentStatus?
    ) = SalePaymentResponseDto(
        id = payment.id!!,
        referenceNumber = payment.referenceNumber!!,
        saleId = payment.saleId,
        paymentMethodId = payment.paymentMethodId,
        amount = payment.amount,
        reference = payment.reference,
        paymentDate = payment.paymentDate,
        createdOn = payment.createdOn.toString(),
        voidedReason = voidedReason,
        updatedSalePaymentStatus = updatedStatus
    )
}
