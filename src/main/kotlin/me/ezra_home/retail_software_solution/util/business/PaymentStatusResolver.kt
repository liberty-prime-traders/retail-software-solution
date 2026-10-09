package me.ezra_home.retail_software_solution.util.business

import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import java.math.BigDecimal

object PaymentStatusResolver {

    fun resolve(paid: BigDecimal, totalDue: BigDecimal): PaymentStatus = when {
        paid.compareTo(BigDecimal.ZERO) == 0 -> PaymentStatus.UNPAID
        paid > totalDue -> PaymentStatus.OVERPAID
        paid < totalDue -> PaymentStatus.PARTIALLY_SETTLED
        else -> PaymentStatus.FULLY_SETTLED
    }
}
