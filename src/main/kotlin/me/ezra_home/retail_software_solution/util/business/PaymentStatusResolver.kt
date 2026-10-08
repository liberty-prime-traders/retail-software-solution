package me.ezra_home.retail_software_solution.util.business

import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import java.math.BigDecimal

object PaymentStatusResolver {

    fun resolve(paid: BigDecimal, receivableTotal: BigDecimal): PaymentStatus = when {
        paid.compareTo(BigDecimal.ZERO) == 0 -> PaymentStatus.UNPAID
        paid > receivableTotal -> PaymentStatus.OVERPAID
        paid < receivableTotal -> PaymentStatus.PARTIALLY_SETTLED
        else -> PaymentStatus.FULLY_SETTLED
    }
}
