package me.ezra_home.retail_software_solution.locations.business.sale_payment.api

import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import java.math.BigDecimal

object PaymentStatusResolver {

    fun resolve(paid: BigDecimal, payableTotal: BigDecimal): PaymentStatus = when {
        paid.compareTo(BigDecimal.ZERO) == 0 -> PaymentStatus.UNPAID
        paid > payableTotal -> PaymentStatus.OVERPAID
        paid < payableTotal -> PaymentStatus.PARTIALLY_SETTLED
        else -> PaymentStatus.FULLY_SETTLED
    }
}
